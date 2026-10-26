package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.antimagic.AntiMagic;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.heroweapon.weapon.kit.Foes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingKnockBackEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

import java.util.List;

/**
 * Why: the passives of Hercules act inside the game's own events (a blow about to land, a fall, a knock-back, an effect about to land, a block being mined), so this is the one subscriber for them, wired once
 * from NUSMP with {@link #register}. It reads the player's {@link JudgmentState} mask (one int, no skill walk) through {@link JudgmentState#onFor}; every handler returns on its first line for a creature that is
 * not an owner of Hercules with the right toggle on. Server side only. Rules (docs/veldanava_skills.md):
 * <ul>
 *   <li><b>Adamantine Body</b>: a physical blow takes a tenth of its damage, a magical one half; no fire, no fall damage, no knock-back, no Temporal Stutter.</li>
 *   <li><b>God of Strength</b>: a melee blow is multiplied ({@link HerculesRules#STRENGTH}, x4.5 mastered, then the Balance Law); every block of hardness up to 50 breaks in one hit (setting
 *       {@code herculesBlockBreak}); a melee blow throws a shockwave that hurts up to six more enemies within three blocks (setting {@code herculesShockwave}).</li>
 *   <li><b>Unstoppable Momentum</b>: at each stage a melee blow takes back part of what the armour saved (the speeds are attribute modifiers, set once a second by the skill).</li>
 *   <li><b>Colossal Vigor</b>: a melee blow heals the owner for a share of the damage (the calm regeneration is the skill's second).</li>
 *   <li><b>Invictus</b>: no blow can leave less than one Hit Point; a melee blow grows as the health falls.</li>
 *   <li><b>Earth-Shattering Tremor</b>: the landing of the leap is a tremor ({@link HerculesSkill#land}).</li>
 * </ul>
 * Everything is dropped at death, logout and server stop. No tick hook.
 */
public final class HerculesEvents {
    private HerculesEvents() {}

    private static final int CODE = HerculesRules.CODE;

    public static void register(IEventBus bus) {
        bus.addListener(HerculesEvents::onEffect);
        bus.addListener(HerculesEvents::onIncoming);
        bus.addListener(EventPriority.LOWEST, HerculesEvents::onDamagePre);
        bus.addListener(HerculesEvents::onDamagePost);
        bus.addListener(HerculesEvents::onKnockBack);
        bus.addListener(HerculesEvents::onFall);
        bus.addListener(HerculesEvents::onBreakSpeed);
        bus.addListener(HerculesEvents::onDeath);
        bus.addListener(HerculesEvents::onLogout);
        bus.addListener(HerculesEvents::onServerStopping);
    }

    private static boolean serverSide(LivingEntity e) { return !e.level().isClientSide; }

    private static String damagePath(DamageSource src) {
        return src.typeHolder().unwrapKey().map(k -> k.location().getPath()).orElse("");
    }

    /** A plain melee blow of a player: the player's own attack, not a spell, not a thrown thing. */
    static boolean melee(DamageSource src, ServerPlayer atk) {
        return src.getDirectEntity() == atk && src.is(DamageTypes.PLAYER_ATTACK) && !AntiMagic.isMagic(src);
    }

    // ---------------------------------------------------------------- Adamantine Body: no Temporal Stutter
    static void onEffect(MobEffectEvent.Applicable e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !serverSide(p) || !JudgmentConfig.enabled() || !JudgmentState.onFor(p, CODE, 0)) return;
        MobEffectInstance inst = e.getEffectInstance();
        if (inst != null && inst.getEffect().is(JudgmentEffects.TEMPORAL_STUTTER)) e.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }

    // ---------------------------------------------------------------- Adamantine Body: the blow about to land
    static void onIncoming(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !serverSide(p) || !JudgmentConfig.enabled() || e.getAmount() <= 0f || JudgmentState.skill(p) != CODE || !JudgmentState.onFor(p, CODE, 0)) return;
        DamageSource src = e.getSource();
        if (src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) return;
        if (HerculesRules.fire(damagePath(src))) {
            e.setCanceled(true);
            return;
        }
        if (JudgmentDamage.trueActive()) return;                         // a true hit (Sandalphon's) steps past the body
        float before = e.getAmount();
        float after = HerculesRules.adamantine(before, AntiMagic.isMagic(src));
        e.setAmount(after);
        if (before - after >= HerculesRules.BODY_FX_MIN && HerculesState.take(p.getUUID(), HerculesState.GAP_BODY, p.level().getGameTime(), HerculesRules.BODY_FX_GAP_TICKS))
            HerculesFx.body(p.serverLevel(), p);
    }

    static void onKnockBack(LivingKnockBackEvent e) {
        if (e.getEntity() instanceof ServerPlayer p && serverSide(p) && JudgmentConfig.enabled() && JudgmentState.onFor(p, CODE, 0)) e.setCanceled(true);
    }

    // ---------------------------------------------------------------- the landing of the leap, and no fall damage
    static void onFall(LivingFallEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !serverSide(p) || !JudgmentConfig.enabled()) return;
        if (HerculesState.leaping(p.getUUID()) && HerculesState.takeLeap(p) && JudgmentState.skill(p) == CODE) {
            e.setDamageMultiplier(0f);
            HerculesSkill.land(p);
            return;
        }
        if (JudgmentState.onFor(p, CODE, 0)) e.setDamageMultiplier(0f);
    }

    // ---------------------------------------------------------------- the owner's blows, after armour and the other mods have had their say
    static void onDamagePre(LivingDamageEvent.Pre e) {
        LivingEntity who = e.getEntity();
        if (!serverSide(who) || !JudgmentConfig.enabled() || e.getNewDamage() <= 0f) return;
        DamageSource src = e.getSource();
        if (src.getEntity() instanceof ServerPlayer atk && JudgmentState.skill(atk) == CODE && !HerculesDamage.own() && melee(src, atk)) {
            boolean mastered = JudgmentState.mastered(atk);
            float base = e.getNewDamage();
            float dmg = base;
            if (JudgmentState.onFor(atk, CODE, 2)) dmg += HerculesRules.armourPierce(e.getOriginalDamage(), base, HerculesState.stage(atk.getUUID()), mastered);
            if (JudgmentState.onFor(atk, CODE, 1)) dmg = HerculesRules.strength(dmg, mastered);
            if (HerculesState.invictus(atk)) dmg *= (float) HerculesRules.invictusMultiplier(atk.getHealth(), atk.getMaxHealth(), mastered);
            if (dmg > base) {
                float grown = base + BalanceLaw.cap(who, dmg - base);
                e.setNewDamage(who instanceof Player ? BalanceLaw.cap(who, grown) : grown);
            }
        }
        if (who instanceof ServerPlayer p && JudgmentState.skill(p) == CODE && HerculesState.invictus(p) && !src.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            float left = HerculesRules.invictusFloor(e.getNewDamage(), p.getHealth(), p.getAbsorptionAmount());
            if (left != e.getNewDamage()) e.setNewDamage(left);
        }
    }

    // ---------------------------------------------------------------- the blow has landed: life steal and the shockwave
    static void onDamagePost(LivingDamageEvent.Post e) {
        LivingEntity who = e.getEntity();
        if (!serverSide(who) || !JudgmentConfig.enabled() || e.getNewDamage() <= 0f) return;
        DamageSource src = e.getSource();
        if (!(src.getEntity() instanceof ServerPlayer atk) || JudgmentState.skill(atk) != CODE || HerculesDamage.own() || !melee(src, atk) || who == atk) return;
        float dealt = e.getNewDamage();
        if (JudgmentState.onFor(atk, CODE, 3)) {
            float h = HerculesRules.lifesteal(dealt, atk.getMaxHealth(), JudgmentState.mastered(atk));
            if (h > 0f) atk.heal(h);
        }
        if (JudgmentState.onFor(atk, CODE, 1) && JudgmentConfig.herculesShockwave() && Foes.isFoe(atk, who)
                && HerculesState.take(atk.getUUID(), HerculesState.GAP_SHOCK, atk.level().getGameTime(), HerculesRules.SHOCK_GAP_TICKS)) shock(atk, who, dealt);
    }

    /** The shockwave of a heavy blow: the (at most six) other foes within three blocks of the one struck take a share of the blow. One box query. */
    private static void shock(ServerPlayer atk, LivingEntity centre, float blow) {
        ServerLevel l = atk.serverLevel();
        double r = HerculesRules.SHOCK_RADIUS, r2 = r * r;
        List<LivingEntity> near = l.getEntitiesOfClass(LivingEntity.class, new AABB(centre.position(), centre.position()).inflate(r, 2.0, r),
                x -> x != atk && x != centre && x.isAlive() && !x.isSpectator() && x.distanceToSqr(centre) <= r2 && Foes.isFoe(atk, x));
        int n = 0;
        for (LivingEntity x : near) {
            if (n++ >= HerculesRules.SHOCK_MAX) break;
            HerculesDamage.raw(atk, x, HerculesRules.shockDamage(blow));
        }
        HerculesFx.shockwave(l, centre.position());
    }

    // ---------------------------------------------------------------- God of Strength: one hit breaks the block
    static void onBreakSpeed(PlayerEvent.BreakSpeed e) {
        Player p = e.getEntity();
        if (p.level().isClientSide || !(p instanceof ServerPlayer) || !JudgmentConfig.enabled() || !JudgmentConfig.herculesBreak() || !JudgmentState.onFor(p, CODE, 1) || e.getPosition().isEmpty()) return;
        if (DungeonGuard.on(p.level())) return;
        BlockPos pos = e.getPosition().get();
        float hardness = e.getState().getDestroySpeed(p.level(), pos);
        if (HerculesRules.instantBreak(hardness) && e.getNewSpeed() < HerculesRules.instantSpeed(hardness)) e.setNewSpeed(HerculesRules.instantSpeed(hardness));
    }

    // ---------------------------------------------------------------- housekeeping
    static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) HerculesState.leave(p);
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) HerculesState.leave(p);
    }

    static void onServerStopping(ServerStoppingEvent e) { HerculesState.clearAll(); }
}
