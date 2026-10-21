package com.newuniverse.nusmp.labyrinth.mega.foundry;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.particle.rukh.RukhFx;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Why: this is the WORLD ADAPTER of the ability library: {@link FoundryAbility} says what an ability is (pure data and rules), this class does it in the game, one small method per
 * {@link FoundryAbility.Kind}. Every effect is the same shape: the mob has already stood through the wind-up (the cast clip played, a Rukh gathered; see
 * {@code FoundryGoals.AbilityGoal}), {@link #fire} applies the effect once (or starts a timer that {@link #tick} runs: dashes, leaps, auras), and the effect shows itself ONLY with
 * the Light or Dark Rukh ({@link RukhFx}: vanilla particles are purged in this mod) and a sound. Damage is the mob's own attack damage times the ability's power, so it scales
 * with tier and floor like everything else; effects on players are the vanilla ones (slowness, weakness, poison), short and capped by the library's clamps. Nothing here throws into
 * the game loop: {@link #fire} and {@link #tick} swallow a runtime exception (logged once) and the mob simply loses that cast.
 */
public final class FoundryAbilityRunner {
    private FoundryAbilityRunner() {}

    private static final Logger LOG = LogUtils.getLogger();
    private static final AtomicBoolean WARNED = new AtomicBoolean();
    private static final Map<String, SoundEvent> SOUNDS = new HashMap<>();

    /** The timers of the abilities in progress of one mob (server side only; not saved). */
    static final class State {
        int dashLeft, leapLeft, leapAge, guardLeft, enragedLeft, auraLeft, thornLeft, regenLeft, markLeft;
        Vec3 dashDir = Vec3.ZERO;
        double dashPow, leapPow, leapRadius, thornPow;
        boolean dashHit;
        FoundryAbility aura;
        /** 1.107.0: where the telegraphed attack was aimed when its aim froze (set by {@code TelegraphedAttackGoal} just before {@link #fire}, consumed by it); null = aim at the target's live position as before. */
        Vec3 aim;
        long markedUntil;
        java.util.UUID marked;
    }

    // ================================================================ what the mob knows
    /** Everything the ability rules ask about the mob's surroundings, as plain numbers. */
    static FoundryAbility.Situation situation(FoundryMobEntity m) {
        LivingEntity t = m.foe();
        double dist = t == null ? -1.0 : Math.sqrt(m.distanceToSqr(t));
        double share = m.getMaxHealth() <= 0f ? 1.0 : m.getHealth() / m.getMaxHealth();
        int allies = 0;
        double weakest = 1.0;
        int minions = 0;
        if (m.level() instanceof ServerLevel level) {
            for (FoundryMobEntity f : level.getEntitiesOfClass(FoundryMobEntity.class, m.getBoundingBox().inflate(12.0), e -> e != m && e.isAlive() && m.friend(e))) {
                allies++;
                weakest = Math.min(weakest, f.getMaxHealth() <= 0f ? 1.0 : f.getHealth() / f.getMaxHealth());
                if (m.getUUID().equals(f.owner())) minions++;
            }
        }
        State s = m.fx;
        return new FoundryAbility.Situation(dist, share, t != null, allies, weakest, minions, s.guardLeft > 0, s.enragedLeft > 0, s.auraLeft > 0, s.markLeft > 0 && t != null,
                s.thornLeft > 0, s.regenLeft > 0);
    }

    // ================================================================ firing
    /** Applies one ability. {@code target} may be null for the abilities that need none. */
    static void fire(FoundryMobEntity m, FoundryAbility a, LivingEntity target) {
        if (!(m.level() instanceof ServerLevel level)) return;
        try {
            sound(level, m, a);
            RukhFx.Alignment al = RukhFx.Alignment.of(a.dark(m.road() == com.newuniverse.nusmp.labyrinth.mega.Polarity.ABYSSAL));
            double attack = m.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
            double dmg = a.damage(attack);
            Vec3 aim = m.fx.aim;                       // 1.107.0: the frozen aim of a telegraphed attack (what the floor warned about is what gets hit)
            m.fx.aim = null;
            switch (a.kind()) {
                case DASH -> dash(m, a, target, dmg, false, aim);
                case DIVE -> dash(m, a, target, dmg, true, aim);
                case LEAP -> leap(m, a, target, dmg, aim);
                case GUARD -> {
                    m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, a.dur(), Math.max(0, a.amp())));
                    m.fx.guardLeft = a.dur();
                    RukhFx.gather(level, m, al, 8);
                }
                case VOLLEY -> volley(m, a, target, dmg, level, aim);
                case HEAL -> heal(m, a, level, al);
                case SUMMON -> summon(m, a, level, al);
                case DRAIN -> {
                    if (target != null) {
                        magic(level, m, target, dmg);
                        m.heal((float) (dmg * 0.6));
                        RukhFx.stream(level, target, m, al, 6);
                    }
                }
                case ROOT -> {
                    if (target != null) {
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, a.dur(), Math.max(0, a.amp())));
                        if (dmg > 0.0) magic(level, m, target, dmg);
                        RukhFx.gather(level, target, al, 8);
                    }
                }
                case BLINK -> blink(m, a, target, dmg, level, al);
                case ENRAGE -> {
                    m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, a.dur(), Math.max(0, a.amp() - 1)));
                    m.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, a.dur(), 0));
                    m.fx.enragedLeft = a.dur();
                    RukhFx.burst(level, m, al, 10);
                }
                case AURA -> {
                    m.fx.auraLeft = a.dur();
                    m.fx.aura = a;
                    RukhFx.burst(level, m, al, 8);
                }
                case SLAM -> {
                    m.swing(InteractionHand.MAIN_HAND);
                    for (LivingEntity v : foesAround(level, m, m.position(), a.r())) {
                        hit(level, m, v, dmg, 1.0);
                        if (a.amp() > 0) v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, a.amp() - 1));
                    }
                    RukhFx.burst(level, m.position().add(0, 0.2, 0), al, 12, Math.max(1.0, a.r() * 0.5));
                }
                case BOLT -> {
                    if (target != null && m.hasLineOfSight(target)) {
                        magic(level, m, target, dmg);
                        if (a.id().contains("fire")) target.setRemainingFireTicks(60);
                        RukhFx.stream(level, m, target, al, 6);
                    }
                }
                case CLEAVE -> cleave(m, a, level, dmg, al, aim);
                case CURSE -> {
                    Vec3 at = aim != null ? aim : target != null ? target.position() : m.position();
                    for (LivingEntity v : foesAround(level, m, at, Math.max(1.0, a.r()))) {
                        v.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, a.dur(), Math.max(0, a.amp())));
                        v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, a.dur() / 2, 0));
                        RukhFx.gather(level, v, al, 6);
                    }
                }
                case WARD -> {
                    for (LivingEntity f : alliesAround(level, m, a.r())) {
                        f.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, a.dur(), Math.max(0, a.amp() - 1)));
                        RukhFx.gather(level, f, al, 4);
                    }
                }
                case SPIT -> {
                    if (target != null) {
                        magic(level, m, target, dmg);
                        target.addEffect(new MobEffectInstance(MobEffects.POISON, a.dur(), 0));
                        RukhFx.stream(level, m, target, al, 5);
                    }
                }
                case PULL -> {
                    if (target != null) {
                        Vec3 d = m.position().subtract(target.position());
                        Vec3 v = d.lengthSqr() < 1.0E-4 ? Vec3.ZERO : d.normalize().scale(1.1);
                        target.setDeltaMovement(v.x, 0.3, v.z);
                        target.hurtMarked = true;
                        if (dmg > 0.0) magic(level, m, target, dmg);
                        RukhFx.stream(level, target, m, al, 6);
                    }
                }
                case BASH -> {
                    if (target != null) {
                        m.swing(InteractionHand.MAIN_HAND);
                        hit(level, m, target, dmg, 1.4);
                        target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, Math.max(20, a.dur()), Math.max(0, a.amp())));
                    }
                }
                case RALLY -> {
                    for (LivingEntity f : alliesAround(level, m, a.r())) {
                        f.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, a.dur(), 0));
                        f.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, a.dur(), 0));
                        RukhFx.burst(level, f, al, 4);
                    }
                }
                case REGEN -> {
                    m.addEffect(new MobEffectInstance(MobEffects.REGENERATION, a.dur(), Math.max(0, a.amp())));
                    m.fx.regenLeft = a.dur();
                    RukhFx.gather(level, m, al, 6);
                }
                case THORNS -> {
                    m.fx.thornLeft = a.dur();
                    m.fx.thornPow = dmg;
                    RukhFx.burst(level, m, al, 8);
                }
                case MARK -> {
                    if (target != null) {
                        target.addEffect(new MobEffectInstance(MobEffects.GLOWING, a.dur(), 0));
                        m.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, a.dur(), 0));
                        m.fx.markLeft = a.dur();
                        m.fx.marked = target.getUUID();
                        RukhFx.gather(level, target, al, 6);
                    }
                }
                case BURST -> burst(m, a, level, dmg, al);
            }
        } catch (RuntimeException | LinkageError ex) {
            if (WARNED.compareAndSet(false, true)) LOG.warn("[nusmp] a foundry ability failed once and is skipped for that cast: {}", ex.toString());
        }
    }

    // ================================================================ the kinds that need more than a line
    private static void dash(FoundryMobEntity m, FoundryAbility a, LivingEntity target, double dmg, boolean vertical, Vec3 aim) {
        if (target == null) return;
        Vec3 goal = aim != null ? aim : target.position();
        Vec3 d = goal.add(0, target.getBbHeight() * 0.4, 0).subtract(m.position().add(0, m.getBbHeight() * 0.4, 0));
        if (!vertical) d = new Vec3(d.x, 0.0, d.z);
        if (d.lengthSqr() < 1.0E-4) return;
        m.fx.dashDir = d.normalize();
        m.fx.dashLeft = Math.max(4, a.dur());
        m.fx.dashPow = dmg;
        m.fx.dashHit = false;
        m.getNavigation().stop();
    }

    private static void leap(FoundryMobEntity m, FoundryAbility a, LivingEntity target, double dmg, Vec3 aim) {
        if (target == null) return;
        Vec3 d = (aim != null ? aim : target.position()).subtract(m.position());
        double dist = Math.sqrt(d.x * d.x + d.z * d.z);
        if (dist < 1.0E-3) return;
        double speed = Math.min(1.3, 0.35 + dist * 0.09);
        m.setDeltaMovement(d.x / dist * speed, 0.55, d.z / dist * speed);
        m.hurtMarked = true;
        m.fx.leapLeft = 50;
        m.fx.leapAge = 0;
        m.fx.leapPow = dmg;
        m.fx.leapRadius = Math.max(1.5, a.r());
    }

    private static void volley(FoundryMobEntity m, FoundryAbility a, LivingEntity target, double dmg, ServerLevel level, Vec3 aim) {
        if (target == null) return;
        Vec3 centre = aim != null ? aim : target.position();
        boolean rain = a.n() >= 6;
        int n = Math.max(1, a.n());
        for (int i = 0; i < n; i++) {
            Arrow arrow = new Arrow(level, m, new ItemStack(Items.ARROW), null);
            arrow.setBaseDamage(Math.max(0.5, dmg / 2.0));
            arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
            if (rain) {
                double rx = (m.getRandom().nextDouble() - 0.5) * 6.0, rz = (m.getRandom().nextDouble() - 0.5) * 6.0;
                arrow.setPos(centre.x + rx, centre.y + 9.0, centre.z + rz);
                arrow.shoot(0.0, -1.0, 0.0, 1.5f, 3.0f);
            } else {
                double dx = target.getX() - m.getX(), dz = target.getZ() - m.getZ();
                double dy = target.getY(0.3333) - arrow.getY();
                double flat = Math.sqrt(dx * dx + dz * dz);
                arrow.setPos(m.getX(), m.getEyeY() - 0.1, m.getZ());
                arrow.shoot(dx, dy + flat * 0.2, dz, 1.6f, n > 1 ? 9.0f : 4.0f);
            }
            level.addFreshEntity(arrow);
        }
        m.swing(InteractionHand.MAIN_HAND);
    }

    private static void heal(FoundryMobEntity m, FoundryAbility a, ServerLevel level, RukhFx.Alignment al) {
        List<LivingEntity> who = new ArrayList<>(alliesAround(level, m, a.r()));
        who.add(m);
        for (LivingEntity f : who) {
            f.heal((float) (a.pow() * f.getMaxHealth()));
            RukhFx.burst(level, f, al, 5);
        }
    }

    private static void summon(FoundryMobEntity m, FoundryAbility a, ServerLevel level, RukhFx.Alignment al) {
        FoundryDef d = m.def();
        if (d == null) return;
        int alive = 0;
        for (FoundryMobEntity f : level.getEntitiesOfClass(FoundryMobEntity.class, m.getBoundingBox().inflate(40.0), e -> e.isAlive() && m.getUUID().equals(e.owner()))) alive++;
        int room = FoundryConfig.get(FoundryConfig.MINION_CAP) - alive;
        if (room <= 0) return;
        List<FoundryDef> pool = FoundryData.roster().minions(d.road(), Math.max(1, d.tier() - 1), d.faction());
        pool.removeIf(x -> x.id().equals(d.id()));
        if (pool.isEmpty()) pool = FoundryData.roster().minions(d.road(), Math.max(1, d.tier() - 1), null);
        if (pool.isEmpty()) return;
        for (int i = 0; i < Math.min(room, Math.max(1, a.n())); i++) {
            FoundryDef pick = pool.get(m.getRandom().nextInt(pool.size()));
            FoundryMobEntity e = FoundryRegistry.MOB.get().create(level);
            if (e == null) continue;
            double ang = m.getRandom().nextDouble() * Math.PI * 2.0, rad = 1.5 + m.getRandom().nextDouble() * Math.max(1.0, a.r());
            e.moveTo(m.getX() + Math.cos(ang) * rad, m.getY(), m.getZ() + Math.sin(ang) * rad, m.getRandom().nextFloat() * 360f, 0f);
            if (!level.noCollision(e)) {
                e.discard();
                continue;
            }
            e.setFoundry(pick, m.floor());
            e.makeMinion(m.getUUID());
            if (m.foe() != null) e.setTarget(m.foe());
            level.addFreshEntity(e);
            RukhFx.burst(level, e, al, 6);
        }
    }

    private static void blink(FoundryMobEntity m, FoundryAbility a, LivingEntity target, double dmg, ServerLevel level, RukhFx.Alignment al) {
        Vec3 from = m.position();
        RukhFx.burst(level, m, al, 8);
        boolean ok = false;
        for (int tries = 0; tries < 10 && !ok; tries++) {
            double x, z;
            if (a.id().equals("shadow_step") && target != null) {
                Vec3 look = target.getLookAngle();
                double len = Math.sqrt(look.x * look.x + look.z * look.z);
                double bx = len < 1.0E-3 ? 0.0 : look.x / len, bz = len < 1.0E-3 ? 1.0 : look.z / len;
                double off = 1.5 + tries * 0.15;
                x = target.getX() - bx * off + (m.getRandom().nextDouble() - 0.5) * 0.6;
                z = target.getZ() - bz * off + (m.getRandom().nextDouble() - 0.5) * 0.6;
            } else {
                double ang = m.getRandom().nextDouble() * Math.PI * 2.0;
                Vec3 centre = a.id().equals("phase_step") || target == null ? from : target.position();
                double rad = Math.max(2.0, a.r());
                if (a.id().equals("phase_step") && target != null) {
                    Vec3 away = from.subtract(target.position());
                    double len = Math.sqrt(away.x * away.x + away.z * away.z);
                    ang = len < 1.0E-3 ? ang : Math.atan2(away.z, away.x) + (m.getRandom().nextDouble() - 0.5) * 1.2;
                }
                x = centre.x + Math.cos(ang) * rad;
                z = centre.z + Math.sin(ang) * rad;
            }
            double y = target != null && !a.id().equals("phase_step") ? target.getY() : from.y;
            ok = teleport(m, level, x, y, z);
        }
        if (!ok) return;
        RukhFx.burst(level, m, al, 8);
        if (a.pow() > 0.0 && target != null && m.distanceToSqr(target) < 9.0) {
            m.swing(InteractionHand.MAIN_HAND);
            hit(level, m, target, dmg, 0.6);
        }
    }

    /** Moves the mob when the place has room and a floor under it (any height within 3 blocks); puts it back otherwise. No vanilla particles: {@code teleportTo} only moves. */
    private static boolean teleport(FoundryMobEntity m, ServerLevel level, double x, double y, double z) {
        Vec3 old = m.position();
        for (int dy = 0; dy >= -3; dy--) {
            m.teleportTo(x, y + dy, z);
            if (level.noCollision(m) && !level.noCollision(m.getBoundingBox().move(0, -0.6, 0))) return true;
        }
        for (int dy = 1; dy <= 2; dy++) {
            m.teleportTo(x, y + dy, z);
            if (level.noCollision(m) && !level.noCollision(m.getBoundingBox().move(0, -0.6, 0))) return true;
        }
        m.teleportTo(old.x, old.y, old.z);
        return false;
    }

    private static void cleave(FoundryMobEntity m, FoundryAbility a, ServerLevel level, double dmg, RukhFx.Alignment al, Vec3 aim) {
        m.swing(InteractionHand.MAIN_HAND);
        Vec3 look = m.getLookAngle();
        if (aim != null) {                                    // the cone the floor showed points at the frozen aim, whichever way the body has turned since
            double ax = aim.x - m.getX(), az = aim.z - m.getZ(), al2 = Math.sqrt(ax * ax + az * az);
            if (al2 > 1.0E-3) look = new Vec3(ax / al2, 0.0, az / al2);
        }
        double half = Math.toRadians(Math.max(20, Math.min(360, a.amp())) / 2.0);
        for (LivingEntity v : foesAround(level, m, m.position(), a.r())) {
            if (a.amp() < 360) {
                Vec3 d = v.position().subtract(m.position());
                double len = Math.sqrt(d.x * d.x + d.z * d.z);
                double ll = Math.sqrt(look.x * look.x + look.z * look.z);
                if (len > 1.0E-3 && ll > 1.0E-3) {
                    double cos = (d.x * look.x + d.z * look.z) / (len * ll);
                    if (Math.acos(Math.max(-1.0, Math.min(1.0, cos))) > half) continue;
                }
            }
            hit(level, m, v, dmg, 0.9);
        }
        RukhFx.burst(level, m.position().add(look.x * 1.2, 1.0, look.z * 1.2), al, 8, 0.8);
    }

    private static void burst(FoundryMobEntity m, FoundryAbility a, ServerLevel level, double dmg, RukhFx.Alignment al) {
        for (LivingEntity v : foesAround(level, m, m.position(), a.r())) hit(level, m, v, dmg, 1.2);
        RukhFx.burst(level, m.position().add(0, 0.8, 0), al, 12, Math.max(1.0, a.r() * 0.5));
        sound(level, m, a);
    }

    // ================================================================ timers, called every tick of the mob
    static void tick(FoundryMobEntity m, ServerLevel level) {
        State s = m.fx;
        try {
            if (s.guardLeft > 0) s.guardLeft--;
            if (s.enragedLeft > 0) s.enragedLeft--;
            if (s.thornLeft > 0) s.thornLeft--;
            if (s.regenLeft > 0) s.regenLeft--;
            if (s.markLeft > 0) s.markLeft--;
            if (s.dashLeft > 0) {
                s.dashLeft--;
                Vec3 d = s.dashDir;
                m.setDeltaMovement(d.x * 1.05, d.y != 0.0 ? d.y * 0.9 : m.getDeltaMovement().y, d.z * 1.05);
                m.hurtMarked = true;
                if (!s.dashHit) {
                    for (LivingEntity v : level.getEntitiesOfClass(LivingEntity.class, m.getBoundingBox().inflate(0.5), e -> e != m && isFoe(m, e))) {
                        hit(level, m, v, s.dashPow, 1.3);
                        s.dashHit = true;
                        s.dashLeft = 0;
                        break;
                    }
                }
            }
            if (s.leapLeft > 0) {
                s.leapLeft--;
                s.leapAge++;
                if (s.leapAge > 4 && m.onGround()) {
                    s.leapLeft = 0;
                    for (LivingEntity v : foesAround(level, m, m.position(), s.leapRadius)) hit(level, m, v, s.leapPow, 1.0);
                    RukhFx.burst(level, m.position().add(0, 0.2, 0), RukhFx.Alignment.of(m.road() == com.newuniverse.nusmp.labyrinth.mega.Polarity.ABYSSAL), 8, Math.max(1.0, s.leapRadius * 0.5));
                }
            }
            if (s.auraLeft > 0) {
                s.auraLeft--;
                FoundryAbility a = s.aura;
                if (a != null && s.auraLeft % 10 == 0) {
                    double attack = m.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
                    RukhFx.Alignment al = RukhFx.Alignment.of(a.dark(m.road() == com.newuniverse.nusmp.labyrinth.mega.Polarity.ABYSSAL));
                    for (LivingEntity v : foesAround(level, m, m.position(), a.r())) {
                        magic(level, m, v, a.damage(attack) * 0.35);
                        if (a.id().contains("burning")) v.setRemainingFireTicks(40);
                        else if (a.id().contains("frost")) v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, Math.max(0, a.amp())));
                        else if (a.id().contains("miasma")) v.addEffect(new MobEffectInstance(MobEffects.POISON, 40, 0));
                        else v.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 40, 0));
                    }
                    RukhFx.burst(level, m.position().add(0, 0.2, 0), al, 6, Math.max(1.0, a.r() * 0.5));
                }
            }
        } catch (RuntimeException ex) {
            if (WARNED.compareAndSet(false, true)) LOG.warn("[nusmp] a foundry ability timer failed once: {}", ex.toString());
        }
    }

    /** Thorns: while the skin is up, a melee attacker takes a share of the blow back. */
    static void onHurt(FoundryMobEntity m, DamageSource source) {
        if (m.fx.thornLeft > 0 && source.getEntity() instanceof LivingEntity attacker && attacker != m && attacker.distanceToSqr(m) < 36.0 && m.level() instanceof ServerLevel level) {
            attacker.hurt(level.damageSources().thorns(m), (float) Math.max(1.0, m.fx.thornPow));
        }
    }

    /** Death burst: the passive ability goes off when the mob dies. */
    static void onDeath(FoundryMobEntity m, ServerLevel level) {
        int i = m.brain().passive();
        if (i < 0) return;
        try {
            FoundryAbility a = m.brain().abilities().get(i);
            burst(m, a, level, a.damage(m.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE)), RukhFx.Alignment.of(a.dark(m.road() == com.newuniverse.nusmp.labyrinth.mega.Polarity.ABYSSAL)));
        } catch (RuntimeException ex) {
            if (WARNED.compareAndSet(false, true)) LOG.warn("[nusmp] a foundry death burst failed once: {}", ex.toString());
        }
    }

    // ================================================================ basic attacks of the shooters
    /** The plain ranged attack of an archer: one arrow. */
    static void shootArrow(FoundryMobEntity m, LivingEntity target) {
        if (!(m.level() instanceof ServerLevel level)) return;
        Arrow arrow = new Arrow(level, m, new ItemStack(Items.ARROW), null);
        double attack = m.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        arrow.setBaseDamage(Math.max(0.5, attack / 2.0));
        arrow.pickup = AbstractArrow.Pickup.DISALLOWED;
        double dx = target.getX() - m.getX(), dz = target.getZ() - m.getZ(), dy = target.getY(0.3333) - arrow.getY();
        arrow.setPos(m.getX(), m.getEyeY() - 0.1, m.getZ());
        arrow.shoot(dx, dy + Math.sqrt(dx * dx + dz * dz) * 0.2, dz, 1.6f, 6.0f);
        level.addFreshEntity(arrow);
        level.playSound(null, m.getX(), m.getY(), m.getZ(), net.minecraft.sounds.SoundEvents.SKELETON_SHOOT, SoundSource.HOSTILE, 1.0f, 1.0f);
        m.swing(InteractionHand.MAIN_HAND);
    }

    /** The plain ranged attack of a caster: a bolt of its Rukh that never misses what it can see. */
    static void shootBolt(FoundryMobEntity m, LivingEntity target) {
        if (!(m.level() instanceof ServerLevel level)) return;
        double attack = m.getAttributeValue(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        magic(level, m, target, attack * 0.8);
        RukhFx.stream(level, m, target, RukhFx.Alignment.of(m.road() == com.newuniverse.nusmp.labyrinth.mega.Polarity.ABYSSAL), 5);
        level.playSound(null, m.getX(), m.getY(), m.getZ(), net.minecraft.sounds.SoundEvents.EVOKER_CAST_SPELL, SoundSource.HOSTILE, 0.7f, 1.3f);
        m.swing(InteractionHand.MAIN_HAND);
    }

    // ================================================================ helpers
    /** Is this a living thing the mob is against: a player who can be hurt, or its own target. */
    static boolean isFoe(FoundryMobEntity m, LivingEntity e) {
        if (e == m || !e.isAlive() || m.friend(e)) return false;
        if (e instanceof Player p) return !p.isCreative() && !p.isSpectator();
        return e == m.getTarget();
    }

    static List<LivingEntity> foesAround(ServerLevel level, FoundryMobEntity m, Vec3 at, double r) {
        AABB box = new AABB(at.x - r, at.y - 1.0, at.z - r, at.x + r, at.y + 3.0, at.z + r);
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, x -> isFoe(m, x))) if (e.position().distanceToSqr(at) <= r * r + 4.0) out.add(e);
        return out;
    }

    static List<LivingEntity> alliesAround(ServerLevel level, FoundryMobEntity m, double r) {
        List<LivingEntity> out = new ArrayList<>();
        for (FoundryMobEntity f : level.getEntitiesOfClass(FoundryMobEntity.class, m.getBoundingBox().inflate(r), e -> e != m && e.isAlive() && m.friend(e))) out.add(f);
        out.add(m);
        return out;
    }

    private static void hit(ServerLevel level, FoundryMobEntity m, LivingEntity v, double dmg, double knock) {
        v.hurt(level.damageSources().mobAttack(m), (float) dmg);
        Vec3 d = v.position().subtract(m.position());
        double len = Math.sqrt(d.x * d.x + d.z * d.z);
        if (len > 1.0E-3 && knock > 0.0) v.knockback(knock * 0.6, -d.x / len, -d.z / len);
    }

    private static void magic(ServerLevel level, FoundryMobEntity m, LivingEntity v, double dmg) {
        v.hurt(level.damageSources().indirectMagic(m, m), (float) dmg);
    }

    private static void sound(ServerLevel level, FoundryMobEntity m, FoundryAbility a) {
        if (a.sound().isEmpty()) return;
        SoundEvent e;
        synchronized (SOUNDS) {
            e = SOUNDS.computeIfAbsent(a.sound(), id -> {
                ResourceLocation rl = ResourceLocation.tryParse(id);
                return rl == null ? null : BuiltInRegistries.SOUND_EVENT.getOptional(rl).orElse(null);
            });
        }
        if (e != null) level.playSound(null, m.getX(), m.getY(), m.getZ(), e, SoundSource.HOSTILE, 1.0f, 0.9f + m.getRandom().nextFloat() * 0.2f);
    }
}
