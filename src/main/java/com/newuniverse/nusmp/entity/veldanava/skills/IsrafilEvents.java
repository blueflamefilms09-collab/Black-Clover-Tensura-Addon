package com.newuniverse.nusmp.entity.veldanava.skills;

import com.newuniverse.nusmp.antimagic.AntiMagic;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.ProjectileImpactEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.entity.living.LivingHealEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

/**
 * Why: the passives of Israfil act inside the game's own events (a fall, a blow, a projectile, an effect about to land), so this is the one subscriber for them, wired once from NUSMP with
 * {@link #register}. It reads the player's {@link JudgmentState} mask (one int, no skill walk) through {@link JudgmentState#onFor}; every handler returns on its first line for a creature that is not
 * an owner of Israfil. Server side only. Rules (docs/veldanava_skills.md):
 * <ul>
 *   <li><b>Spatial Domination</b>: no fall damage; a melee blow is dodged by a blink to a free spot three to seven blocks away (once every six seconds); no Movement Interference or Gravity Crush lands.
 *       Mastered: a physical blow that still lands does half damage, and a projectile about to hit is bent away.</li>
 *   <li><b>Trial of Endurance</b>: a share of every blow taken is stored (at most {@link IsrafilRules#STORE_MAX}); the store gives temporary maximum health and armour toughness, and whoever dealt the blow
 *       owes Temporal Debt.</li>
 *   <li><b>Labyrinthian Mind</b>: no mind-altering effect lands ({@link IsrafilRules#mindEffect}).</li>
 *   <li>Nihility Collapse: no healing for a creature that carries the effect. Rift Walk: ended on death, logout and server stop, and restored on login.</li>
 * </ul>
 */
public final class IsrafilEvents {
    private IsrafilEvents() {}

    private static final int CODE = VeldanavaGuard.ISRAFIL;
    private static final String BENT = "nusmp_israfil_bent";

    public static void register(IEventBus bus) {
        bus.addListener(IsrafilEvents::onFall);
        bus.addListener(IsrafilEvents::onIncoming);
        bus.addListener(IsrafilEvents::onDamaged);
        bus.addListener(IsrafilEvents::onEffect);
        bus.addListener(IsrafilEvents::onImpact);
        bus.addListener(IsrafilEvents::onHeal);
        bus.addListener(IsrafilEvents::onDeath);
        bus.addListener(IsrafilEvents::onLogout);
        bus.addListener(IsrafilEvents::onLogin);
        bus.addListener(IsrafilEvents::onServerTick);
        bus.addListener(IsrafilEvents::onServerStopping);
    }

    private static boolean serverSide(LivingEntity e) { return !e.level().isClientSide; }

    private static boolean spatial(LivingEntity e) { return JudgmentState.onFor(e, CODE, 0); }

    // ---------------------------------------------------------------- Spatial Domination
    static void onFall(LivingFallEvent e) {
        if (serverSide(e.getEntity()) && JudgmentConfig.enabled() && spatial(e.getEntity())) e.setDamageMultiplier(0f);
    }

    static void onIncoming(LivingIncomingDamageEvent e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !serverSide(p) || !JudgmentConfig.enabled() || e.getAmount() <= 0f || !spatial(p)) return;
        DamageSource src = e.getSource();
        if (src.getDirectEntity() == null || AntiMagic.isMagic(src) || JudgmentDamage.trueActive()) return;
        if (src.getDirectEntity() instanceof LivingEntity attacker && attacker != p && blink(p, attacker)) {
            e.setCanceled(true);
            return;
        }
        if (JudgmentState.mastered(p)) e.setAmount((float) (e.getAmount() * IsrafilRules.DEFLECT_PHYSICAL));
    }

    /** The natural blink: a melee blow is dodged by stepping to a free spot three to seven blocks away. At most once every {@link IsrafilRules#BLINK_GAP_TICKS} ticks. */
    private static boolean blink(ServerPlayer p, LivingEntity attacker) {
        ServerLevel l = p.serverLevel();
        long now = l.getGameTime();
        if (now < p.getPersistentData().getLong(IsrafilState.BLINK_AT) || p.distanceToSqr(attacker) > 36.0) return false;
        Vec3 spot = freeSpot(p, l);
        if (spot == null) return false;
        p.getPersistentData().putLong(IsrafilState.BLINK_AT, now + IsrafilRules.BLINK_GAP_TICKS);
        TrialFx.shift(l, p.position(), spot);
        IsrafilState.place(p, l, spot);
        return true;
    }

    /** A free place to stand: five tries at random directions, each tried at four heights (nothing in the way, solid ground below). */
    private static Vec3 freeSpot(ServerPlayer p, ServerLevel l) {
        AABB box = p.getBoundingBox();
        for (int attempt = 0; attempt < 5; attempt++) {
            double angle = p.getRandom().nextDouble() * Math.PI * 2.0;
            double dist = IsrafilRules.BLINK_MIN + p.getRandom().nextDouble() * (IsrafilRules.BLINK_MAX - IsrafilRules.BLINK_MIN);
            double dx = Math.cos(angle) * dist, dz = Math.sin(angle) * dist;
            for (int dy : new int[]{0, 1, -1, 2}) {
                AABB at = box.move(dx, dy, dz);
                if (l.noCollision(p, at) && !l.noCollision(p, at.move(0.0, -0.6, 0.0))) return p.position().add(dx, dy, dz);
            }
        }
        return null;
    }

    /** A projectile about to hit an owner of a mastered Spatial Domination is bent away (once per projectile) instead of landing. */
    static void onImpact(ProjectileImpactEvent e) {
        Projectile pr = e.getProjectile();
        if (pr.level().isClientSide() || !(e.getRayTraceResult() instanceof EntityHitResult hit) || !(hit.getEntity() instanceof ServerPlayer p)) return;
        if (!JudgmentConfig.enabled() || !spatial(p) || !JudgmentState.mastered(p) || pr.getOwner() == p || pr.getPersistentData().getBoolean(BENT)) return;
        pr.getPersistentData().putBoolean(BENT, true);
        e.setCanceled(true);
        Vec3 v = pr.getDeltaMovement();
        Vec3 away = pr.position().subtract(p.getBoundingBox().getCenter()).normalize();
        Vec3 side = new Vec3(-v.z, 0.0, v.x).normalize().scale(p.getRandom().nextBoolean() ? 1.0 : -1.0);
        Vec3 dir = v.normalize().add(away.scale(0.6)).add(side.scale(0.8)).normalize();
        pr.setDeltaMovement(dir.scale(Math.max(0.5, v.length())));
        pr.setPos(pr.position().add(dir.scale(0.8)));
        pr.hurtMarked = true;
    }

    // ---------------------------------------------------------------- Trial of Endurance
    static void onDamaged(LivingDamageEvent.Post e) {
        if (!(e.getEntity() instanceof ServerPlayer p) || !serverSide(p) || !JudgmentConfig.enabled() || !JudgmentState.onFor(p, CODE, 1)) return;
        double blow = e.getNewDamage();
        if (blow <= 0.0) return;
        double store = IsrafilState.store(p);
        double add = IsrafilRules.stored(blow, store);
        if (add > 0.0) {
            IsrafilState.setStore(p, store + add);
            IsrafilState.applyStore(p, store + add);
        }
        if (e.getSource().getEntity() instanceof LivingEntity attacker && attacker != p && attacker.isAlive()) TemporalDebt.add(attacker, IsrafilRules.debtOnAttacker(blow));
    }

    // ---------------------------------------------------------------- Labyrinthian Mind and entrapment
    static void onEffect(MobEffectEvent.Applicable e) {
        LivingEntity who = e.getEntity();
        if (!(who instanceof ServerPlayer) || !serverSide(who) || !JudgmentConfig.enabled()) return;
        var effect = e.getEffectInstance().getEffect();
        if (JudgmentState.onFor(who, CODE, 2) && effect.unwrapKey().map(k -> IsrafilRules.mindEffect(k.location().getPath())).orElse(false)) {
            e.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
            return;
        }
        if (spatial(who) && (effect.value() == JudgmentEffects.MOVEMENT_INTERFERENCE.get() || effect.value() == JudgmentEffects.GRAVITY_CRUSH.get())) e.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
    }

    // ---------------------------------------------------------------- Nihility Collapse: no healing
    static void onHeal(LivingHealEvent e) {
        LivingEntity who = e.getEntity();
        if (serverSide(who) && JudgmentEffects.has(who, JudgmentEffects.NIHILITY_COLLAPSE)) e.setCanceled(true);
    }

    // ---------------------------------------------------------------- Rift Walk housekeeping
    static void onDeath(LivingDeathEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) {
            RiftWalk.end(p, true, null);
            IsrafilState.setStore(p, 0.0);
        }
    }

    static void onLogout(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) RiftWalk.end(p, true, null);
    }

    static void onLogin(PlayerEvent.PlayerLoggedInEvent e) {
        if (e.getEntity() instanceof ServerPlayer p) RiftWalk.restoreStale(p);
    }

    static void onServerTick(ServerTickEvent.Post e) { RiftWalk.onServerTick(e.getServer()); }

    static void onServerStopping(ServerStoppingEvent e) { RiftWalk.endAll(e.getServer()); }

    /** Tests only: a hit entity helper kept here so the scan test can see every use of a player's displacement in one file. */
    static boolean isOwner(Entity e) { return e instanceof LivingEntity le && JudgmentState.skill(le) == CODE; }
}
