package com.newuniverse.nusmp.thread;

import com.newuniverse.nusmp.blackclover.TimeStop;
import com.newuniverse.nusmp.boss.rules.BossIdentity;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import org.joml.Vector3f;

/**
 * Why: this is where Rouge's fate rewrite actually happens. A protected ally's deadly or heavy blow arrives as a damage event, an
 * arrow arrives as a projectile impact, and a death that slipped through arrives as a death event; each is cut off here, the caster
 * is charged through the mana system, the ally is stepped out of the way, and Rouge taps them. The numbers and the verdict come from
 * the pure {@link FateRules}; who is guarded comes from the cat ({@link RougeEntity#isProtecting}); this class only gathers facts
 * from the event, pays, and plays the result.
 * <p>
 * Rules it keeps: the void, /kill and anything that bypasses invulnerability are never rewritten; neither is a victim caught in
 * stopped time or carrying the {@link #UNFATED_TAG} flag (any other feature can set that persistent-data game time to say "fate
 * cannot be rewritten for this one until then"). The same attacker is skipped for 10 ticks after a save. Boss blows cost double and
 * start an 8 s cooldown for that victim. If the caster cannot pay the whole price, Rouge fails and unravels.
 * Wire in NUSMP (HIGH priority so it runs before other damage tweaks): {@code NeoForge.EVENT_BUS.addListener(EventPriority.HIGH,
 * RougeFateEvents::onIncoming)}, the same for {@code onProjectile} and {@code onDeath}.
 */
public final class RougeFateEvents {
    private RougeFateEvents() {}

    /** Persistent-data game time on a victim: until then Rouge cannot rewrite their fate. */
    public static final String UNFATED_TAG = "nusmp_unfated_until";
    private static final String MARK_ATTACKER = "nusmp_fate_att", MARK_UNTIL = "nusmp_fate_until", BOSS_COOLDOWN = "nusmp_fate_boss_cd";
    private static final double CAT_RANGE_SQR = 40.0 * 40.0;

    // ================================================================ the three doors
    public static void onIncoming(LivingIncomingDamageEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide || !ThreadConfig.rougeEntity() || !RougeManager.anyActive()) return;
        if (attempt(victim, e.getSource(), e.getAmount(), false)) e.setCanceled(true);
    }

    public static void onProjectile(ProjectileImpactEvent e) {
        Projectile proj = e.getProjectile();
        if (proj.level().isClientSide || !ThreadConfig.rougeEntity() || !RougeManager.anyActive()) return;
        if (!(e.getRayTraceResult() instanceof EntityHitResult hit) || !(hit.getEntity() instanceof LivingEntity victim)) return;
        if (!(proj instanceof AbstractArrow arrow)) return;                   // anything else is judged when its damage arrives
        double speed = arrow.getDeltaMovement().length();
        float dmg = Mth.ceil(Mth.clamp(speed * arrow.getBaseDamage(), 0.0, 2.147483647E9));
        if (arrow.isCritArrow()) dmg += dmg / 2f + 1f;
        DamageSource src = proj.damageSources().arrow(arrow, arrow.getOwner());
        if (attempt(victim, src, dmg, false)) {
            e.setCanceled(true);
            Vec3 v = arrow.getDeltaMovement();                                 // the arrow glances off and the shot "misses"
            arrow.setDeltaMovement(v.scale(-0.25).add((arrow.getRandom().nextDouble() - 0.5) * 0.2, 0.15, (arrow.getRandom().nextDouble() - 0.5) * 0.2));
            arrow.hurtMarked = true;
        }
    }

    /** The last safety net: a death still arriving while Rouge is up and mana remains is cancelled, health set to 1. */
    public static void onDeath(LivingDeathEvent e) {
        LivingEntity victim = e.getEntity();
        if (victim.level().isClientSide || !ThreadConfig.rougeEntity() || !RougeManager.anyActive()) return;
        if (attempt(victim, e.getSource(), Float.MAX_VALUE / 4f, true)) {
            e.setCanceled(true);
            victim.setHealth(Math.max(1f, victim.getHealth()));
        }
    }

    // ================================================================ the decision and its price
    static boolean unsavableType(DamageSource s) {
        return s.is(DamageTypes.FELL_OUT_OF_WORLD) || s.is(DamageTypes.GENERIC_KILL) || s.is(DamageTypeTags.BYPASSES_INVULNERABILITY);
    }

    /** True if this blow was rewritten (the caller cancels it). */
    private static boolean attempt(LivingEntity victim, DamageSource src, float amount, boolean deathNet) {
        long now = victim.level().getGameTime();
        RougeEntity cat = null;
        for (RougeEntity r : RougeManager.cats()) {
            if (r.level() != victim.level() || r.state() == RougeEntity.UNRAVEL || r.distanceToSqr(victim) > CAT_RANGE_SQR) continue;
            if (r.isProtecting(victim)) { cat = r; break; }
        }
        if (cat == null) return false;
        ServerPlayer caster = cat.owner();
        if (caster == null) return false;

        Entity attacker = src.getEntity() != null ? src.getEntity() : src.getDirectEntity();
        boolean boss = attacker != null && BossIdentity.isBoss(attacker);
        var data = victim.getPersistentData();
        String who = attacker != null ? attacker.getUUID().toString() : "env:" + src.getMsgId();
        boolean unsavable = unsavableType(src) || TimeStop.isFrozen(victim) || data.getLong(UNFATED_TAG) > now;
        boolean repeat = FateRules.sameAttacker(data.getString(MARK_ATTACKER), data.getLong(MARK_UNTIL), who, now);
        FateRules.Hit hit = new FateRules.Hit(amount, src.is(DamageTypeTags.BYPASSES_ARMOR), victim.getArmorValue(), victim.getAttributeValue(Attributes.ARMOR_TOUGHNESS),
                victim.getHealth(), victim.getAbsorptionAmount(), victim.getMaxHealth(), unsavable, boss, repeat, now, data.getLong(BOSS_COOLDOWN));
        FateRules.Params params = ThreadConfig.fateParams();
        FateRules.Verdict verdict = FateRules.judge(hit, params);
        if (verdict == FateRules.Verdict.IGNORE || verdict == FateRules.Verdict.BOSS_COOLDOWN) return false;
        if (verdict == FateRules.Verdict.REPEAT_FREE) return true;

        double cost = FateRules.cost(RougeManager.maxMana(caster), params, boss);
        if (!RougeManager.pay(caster, cost)) {
            caster.displayClientMessage(Component.literal("Rouge cannot pay the price of fate. She unravels.").withStyle(ChatFormatting.DARK_RED), true);
            cat.beginUnravel(caster);
            return false;
        }
        data.putString(MARK_ATTACKER, who);
        data.putLong(MARK_UNTIL, now + params.repeatTicks());
        if (boss) data.putLong(BOSS_COOLDOWN, FateRules.bossCooldownEnd(now, params));

        Vec3 from = attacker != null ? attacker.position() : null;
        cat.tap(victim);
        if (!deathNet) nudge(victim, from);
        sparkle(victim);
        if (victim instanceof ServerPlayer saved) com.newuniverse.nusmp.boss.god.DodgeEvents.record(saved);      // 1.45.0: a save is a dodge, for the god-tier Lucius's Time Stasis Traps
        Component line = Component.literal(deathNet ? "Rouge pulls the thread back. That never happened." : "Rouge rewrites fate: the attack misses.").withStyle(ChatFormatting.RED);
        caster.displayClientMessage(line, true);
        com.newuniverse.nusmp.quests.NusmpEvents.fire(caster, "rouge.saved", "caster");
        if (victim instanceof ServerPlayer vp && vp != caster) vp.displayClientMessage(line, true);
        if (victim instanceof ServerPlayer sv && sv != caster) com.newuniverse.nusmp.quests.NusmpEvents.fire(sv, "rouge.saved", "saved");
        return true;
    }

    /** About a block and a half away from the attacker, only to a spot that is safe to stand on. */
    private static void nudge(LivingEntity victim, Vec3 attackerAt) {
        if (!(victim.level() instanceof ServerLevel sl) || victim.isPassenger() || victim.isFallFlying()) return;
        Vec3 spot = SafeSpot.firstOk(sl, victim, SafeSpot.awayFrom(victim, attackerAt, 1.5));
        if (spot == null) return;
        victim.teleportTo(spot.x, spot.y, spot.z);
        victim.fallDistance = 0f;
    }

    /** A burst of red dust and white glints where the fate was rewritten (the thread VFX itself comes from the tap). */
    private static void sparkle(LivingEntity victim) {
        if (!(victim.level() instanceof ServerLevel sl)) return;
        sl.sendParticles(new DustParticleOptions(new Vector3f(1f, 0.2f, 0.33f), 1.0f), victim.getX(), victim.getY() + victim.getBbHeight() * 0.5, victim.getZ(),
                18, 0.35, 0.5, 0.35, 0.02);
        sl.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, victim.getX(), victim.getY() + victim.getBbHeight() * 0.6, victim.getZ(), 6, 0.3, 0.4, 0.3, 0.02);
    }
}
