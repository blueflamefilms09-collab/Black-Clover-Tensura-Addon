package com.newuniverse.nusmp.saiyan;

import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.particle.fx.Fx;
import com.newuniverse.nusmp.particle.fx.FxSlot;
import com.newuniverse.nusmp.particle.fx.FxThemes;
import com.newuniverse.nusmp.vfx.VfxShape;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Why: the techniques of the Dragon Ball rework (phases 3 and 4) and the Giant Ki Avatar (techniques III), next to everything else (nothing existing is changed). PHYSICAL: Meteor Strike, Vanishing Step, Aerial
 * Rush, Warrior's Counter, Ground Breaker, Combat Instinct. KI: Charged Ki Sphere, Ki Wave, Ki Reinforcement, Afterimage Technique. TRAINING: Gravity Training. AVATAR: the Giant Ki Avatar. Every rule is decided
 * here on the server; the client only sends the one-byte ask of {@link SaiyanNet2} (the same packet as the older keys, so no new packet exists). State lives in the {@link State} of the player's run: dropped on
 * death, logout and change of dimension. Hurt goes through the balance law, Ki techniques as Tensura magic ({@link SaiyanDamage}); costs are the existing stamina and Ki (magicule) path.
 * WORST CASE per Saiyan: Meteor Strike one UUID lookup and one blow; Vanishing Step one raycast and at most 16 collision checks, once per use; Aerial Rush one entity query and at most 3 blows; Ground Breaker
 * one entity query and at most 8 blows; Charged Ki Sphere at most 60 ticks of one block read and one entity query every second tick, then one blast of at most 8 targets; Ki Wave at most 100 ticks, a ray every 4th
 * tick and at most one packet every 20 ticks; Giant Ki Avatar at most 900 ticks, a blast of at most 6 foes once a second and ONE effect packet; toggles cost a few comparisons a second. No entity is ever
 * spawned and no block is ever changed.
 */
final class SaiyanTechniques4 {
    private SaiyanTechniques4() {}

    /** Per-player technique state (not saved). */
    static final class State {
        UUID strikeTarget;
        long strikeAt;
        long counterUntil, instinctUntil, breakerUntil;
        double breakerFromY;
        boolean reinforce, afterimage;
        int gravityLevel;
        double gx = Double.NaN, gz = Double.NaN;
        Vec3 spherePos, sphereDir;
        int sphereLeft;
        long waveStart = -1, waveBegin, waveEnd;
        long avatarUntil, avatarRose;
        long ritualStart = -1;
        /** The race stage whose buff is on the player now (0 = none), so a switched-off race gives it back. */
        int raceApplied;

        /** True while something that needs the per-tick call runs (so the call is one comparison when nothing does). */
        boolean busy(long now) {
            return strikeTarget != null || breakerUntil > now || sphereLeft > 0 || waveStart >= 0 || waveEnd > 0 || avatarUntil > 0 || ritualStart >= 0;
        }

        boolean toggled() { return reinforce || afterimage || gravityLevel > 0 || instinctUntil > 0 || counterUntil > 0; }

        void clear() {
            strikeTarget = null;
            counterUntil = 0;
            instinctUntil = 0;
            breakerUntil = 0;
            reinforce = false;
            afterimage = false;
            gravityLevel = 0;
            sphereLeft = 0;
            spherePos = null;
            sphereDir = null;
            waveStart = -1;
            waveEnd = 0;
            avatarUntil = 0;
            ritualStart = -1;
        }
    }

    /** Stops everything that is running (the system or the techniques were switched off, the race was taken away) and takes the modifiers off. */
    static void cancel(ServerPlayer p, SaiyanManager.Run r) {
        SaiyanRitual.end(p);
        r.tech4.clear();
        SaiyanAttributes.applyTech(p, 0.0, 0.0, 0.0);
        r.dirty = true;
    }

    // ------------------------------------------------------------------ the two factors the form engine multiplies in
    /** Outgoing damage factor of Ki Reinforcement (1.0 when it is off). */
    static double damageMul(SaiyanManager.Run r) { return r.tech4.reinforce ? SaiyanPhaseRules.reinforceOut(SaiyanPhaseConfig.reinforceDamage()) : 1.0; }

    /** Share of incoming damage that still goes through with Ki Reinforcement on (1.0 when it is off). */
    static double takenMul(SaiyanManager.Run r) { return r.tech4.reinforce ? SaiyanPhaseRules.reinforceTaken(SaiyanPhaseConfig.reinforceDefense()) : 1.0; }

    // ------------------------------------------------------------------ entry
    private static boolean open(ServerPlayer p, SaiyanState s) {
        if (!SaiyanPhaseConfig.enabled() || p.isSpectator() || !p.isAlive()) return false;
        if (!s.awakened) {
            SaiyanAbilities.say(p, "not_awakened");
            return false;
        }
        return true;
    }

    /** One ask of the {@link SaiyanNet2} range (already rate limited by the manager). */
    static boolean action(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, int op, long now) {
        if (op == SaiyanNet2.OP_BRANCH) return SaiyanPhaseConfig.branchKey() && SaiyanFormEngine.cycle(p, s, r, now, 1);
        if (!open(p, s)) return false;
        return switch (op) {
            case SaiyanNet2.OP_STRIKE -> strike(p, s, r, now);
            case SaiyanNet2.OP_VANISH -> vanish(p, s, r, now);
            case SaiyanNet2.OP_RUSH -> rush(p, s, r, now);
            case SaiyanNet2.OP_COUNTER -> counter(p, s, r, now);
            case SaiyanNet2.OP_BREAKER -> breaker(p, s, r, now);
            case SaiyanNet2.OP_INSTINCT -> instinct(p, s, r, now);
            case SaiyanNet2.OP_SPHERE -> sphere(p, s, r, now);
            case SaiyanNet2.OP_WAVE_START -> waveStart(p, s, r, now);
            case SaiyanNet2.OP_REINFORCE -> reinforce(p, r);
            case SaiyanNet2.OP_AVATAR -> avatar(p, s, r, now);
            case SaiyanNet2.OP_AFTERIMAGE -> afterimage(p, r);
            case SaiyanNet2.OP_GRAVITY -> gravity(p, s, r, now);
            default -> false;
        };
    }

    // ------------------------------------------------------------------ helpers
    private static List<LivingEntity> foes(ServerPlayer p, AABB box, int max) {
        List<LivingEntity> out = new ArrayList<>();
        int seen = 0;
        for (LivingEntity e : p.serverLevel().getEntitiesOfClass(LivingEntity.class, box, x -> x != p && x.isAlive() && !(x instanceof ArmorStand))) {
            if (++seen > SaiyanAbilities.MAX_CANDIDATES) break;
            if (SaiyanAbilities.allowed(p, e)) out.add(e);
        }
        out.sort(Comparator.comparingDouble((LivingEntity e) -> e.distanceToSqr(p)));
        return out.size() > max ? new ArrayList<>(out.subList(0, max)) : out;
    }

    private static void shove(LivingEntity e, Vec3 dir, double k, double up) {
        if (k <= 0.0 || BalanceLaw.isBoss(e)) return;
        e.setDeltaMovement(e.getDeltaMovement().add(dir.x * k, up, dir.z * k));
        if (e instanceof ServerPlayer) e.hurtMarked = true;
    }

    private static double blowRaw(SaiyanState s, SaiyanManager.Run r, double base) {
        return base * SaiyanRules.damageFactor(SaiyanConfig.bonuses(s.battlePower).strength()) * SaiyanFormEngine.damageFactor(s, r);
    }

    private static void sound(ServerLevel level, double x, double y, double z, net.minecraft.sounds.SoundEvent ev, float vol, float pitch) {
        level.playSound(null, x, y, z, ev, SoundSource.PLAYERS, vol, pitch);
    }

    private static void spark(ServerLevel level, int rgb, double x, double y, double z, int n) {
        Fx.sprayTinted(level, FxThemes.WORLD, FxSlot.SPARK, rgb & 0xFFFFFF, 1.8f, x, y, z, n, 0.4, 0.5, 0.4, 0.15);
    }

    private static int color(ServerPlayer p) { return SaiyanKiFx.kiColor(p, SaiyanKiFx.BASE, true); }

    private static int adapted(SaiyanState s) { return SaiyanPhaseRules.adaptedLevel(s.gravitySecs, SaiyanPhaseConfig.gravitySecondsPerLevel()); }

    // ================================================================== Meteor Strike
    private static boolean strike(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        State t = r.tech4;
        if (t.strikeTarget != null || !s.ready("strike", now)) return false;
        LivingEntity e = SaiyanTechniques3.lockFoe(p, SaiyanPhaseConfig.strikeRange());
        if (e == null) {
            SaiyanAbilities.say(p, "t4_none");
            return false;
        }
        if (!SaiyanAbilities.pay(p, s, SaiyanPhaseConfig.strikeStamina(), SaiyanPhaseConfig.strikeCost())) return false;
        t.strikeTarget = e.getUUID();
        t.strikeAt = now + SaiyanPhaseConfig.strikeWind();
        s.startCooldown("strike", now, SaiyanPhaseConfig.strikeCooldown());
        s.use("strike", 1);
        r.lastCombat = now;
        SaiyanKiFx.send(p.serverLevel(), VfxShape.KI_STRIKE, p.position().add(0.0, 1.0, 0.0), e.position().add(0.0, 1.0, 0.0), color(p), SaiyanPhaseConfig.strikeWind() + 6, 1f);
        sound(p.serverLevel(), p.getX(), p.getY(), p.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, 0.8f, 0.7f);
        return true;
    }

    private static void strikeTick(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        State t = r.tech4;
        if (now < t.strikeAt) return;
        ServerLevel level = p.serverLevel();
        UUID id = t.strikeTarget;
        t.strikeTarget = null;
        if (!(level.getEntity(id) instanceof LivingEntity e) || !e.isAlive() || !p.isAlive() || !SaiyanAbilities.allowed(p, e) || p.distanceTo(e) > SaiyanPhaseConfig.strikeRange() + 3.0) return;
        Vec3 dir = e.position().subtract(p.position());
        dir = dir.lengthSqr() < 1.0e-6 ? p.getLookAngle() : dir.normalize();
        p.setDeltaMovement(dir.x * 1.2, Math.max(0.1, dir.y * 0.4), dir.z * 1.2);
        p.hurtMarked = true;
        r.safeFallUntil = now + 40;
        double growth = SaiyanRules.growth(s.battlePower);
        if (SaiyanAbilities.hurtBlow(p, e, blowRaw(s, r, SaiyanPhaseConfig.strikeDamage()), growth) > 0f) shove(e, dir, SaiyanPhaseConfig.strikeKnockback(), 0.3);
        SaiyanCombat.trainOn(s, e, SaiyanTraining.Cat.MARTIAL, now);
        SaiyanKiFx.send(level, VfxShape.KI_SHOCKWAVE, e.position(), e.position(), color(p), 12, 2f);
        spark(level, color(p), e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ(), 12);
        sound(level, e.getX(), e.getY(), e.getZ(), SoundEvents.GENERIC_EXPLODE.value(), 0.7f, 1.3f);
        r.lastCombat = now;
    }

    // ================================================================== Vanishing Step
    private static boolean vanish(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        if (!s.ready("vanish", now)) return false;
        ServerLevel level = p.serverLevel();
        Vec3 look = p.getLookAngle();
        Vec3 dir = new Vec3(look.x, Math.max(-0.3, Math.min(0.3, look.y)), look.z);
        if (dir.lengthSqr() < 1.0e-6) return false;
        dir = dir.normalize();
        double dist = SaiyanPhaseConfig.vanishDistance();
        Vec3 eye = p.getEyePosition();
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(dir.scale(dist)), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        if (hit.getType() != HitResult.Type.MISS) dist = Math.max(0.0, hit.getLocation().distanceTo(eye) - 0.7);
        Vec3 start = p.position(), best = null;
        for (double d = Math.floor(dist); d >= 1.0; d -= 1.0) {
            Vec3 to = start.add(dir.scale(d));
            AABB box = p.getBoundingBox().move(dir.scale(d));
            if (level.getWorldBorder().isWithinBounds(box) && level.noCollision(p, box)) {
                best = to;
                break;
            }
        }
        if (best == null) {
            SaiyanAbilities.say(p, "t4_blocked");
            return false;
        }
        if (!SaiyanAbilities.pay(p, s, SaiyanPhaseConfig.vanishStamina(), SaiyanPhaseConfig.vanishCost())) return false;
        SaiyanKiFx.send(level, VfxShape.KI_AFTERIMAGE, start, best, color(p), 14, 1f);
        p.teleportTo(best.x, best.y, best.z);
        p.fallDistance = 0.0f;
        s.startCooldown("vanish", now, SaiyanPhaseConfig.vanishCooldown());
        s.use("vanish", 1);
        sound(level, best.x, best.y, best.z, SoundEvents.AMETHYST_BLOCK_RESONATE, 0.5f, 1.8f);
        return true;
    }

    // ================================================================== Aerial Rush
    private static boolean rush(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        if (!s.ready("rush", now)) return false;
        Vec3 from = p.position().add(0.0, 1.0, 0.0), dir = p.getLookAngle(), end = from.add(dir.scale(7.0));
        List<LivingEntity> hit = new ArrayList<>();
        for (LivingEntity e : foes(p, new AABB(from, end).inflate(1.6), SaiyanPhaseRules.MAX_RUSH_HITS * 4)) {
            Vec3 c = e.getBoundingBox().getCenter();
            double along = c.subtract(from).dot(dir);
            if (along < 0.0 || along > 7.5 || c.subtract(from.add(dir.scale(along))).length() > 1.8) continue;
            hit.add(e);
            if (hit.size() >= SaiyanPhaseRules.MAX_RUSH_HITS) break;
        }
        if (hit.isEmpty()) {
            SaiyanAbilities.say(p, "t4_none");
            return false;
        }
        if (!SaiyanAbilities.pay(p, s, SaiyanPhaseConfig.rushStamina(), SaiyanPhaseConfig.rushCost())) return false;
        p.setDeltaMovement(dir.x * 1.6, dir.y * 1.0 + 0.1, dir.z * 1.6);
        p.hurtMarked = true;
        r.safeFallUntil = now + 40;
        double growth = SaiyanRules.growth(s.battlePower);
        for (LivingEntity e : hit) {
            SaiyanAbilities.hurtBlow(p, e, blowRaw(s, r, SaiyanPhaseConfig.rushDamage()), growth);
            SaiyanCombat.trainOn(s, e, SaiyanTraining.Cat.MARTIAL, now);
        }
        LivingEntity first = hit.get(0);
        SaiyanKiFx.send(p.serverLevel(), VfxShape.KI_RUSH, first.position(), first.position(), color(p), 16, hit.size());
        s.startCooldown("rush", now, SaiyanPhaseConfig.rushCooldown());
        s.use("rush", 1);
        r.lastCombat = now;
        sound(p.serverLevel(), p.getX(), p.getY(), p.getZ(), SoundEvents.FIREWORK_ROCKET_BLAST, 0.9f, 0.8f);
        return true;
    }

    // ================================================================== Warrior's Counter and the Afterimage (a blow is caught or softened)
    private static boolean counter(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        State t = r.tech4;
        if (t.counterUntil > now || !s.ready("counter", now)) return false;
        if (!SaiyanAbilities.pay(p, s, SaiyanPhaseConfig.counterStamina(), SaiyanPhaseConfig.counterCost())) return false;
        t.counterUntil = now + SaiyanPhaseConfig.counterWindow();
        s.startCooldown("counter", now, SaiyanPhaseConfig.counterCooldown());
        s.use("counter", 1);
        spark(p.serverLevel(), color(p), p.getX(), p.getY() + 1.0, p.getZ(), 8);
        SaiyanAbilities.say(p, "t4_counter_on");
        return true;
    }

    /** Called from the defence of a Saiyan, after the dodge. True = the blow was cancelled (a counter caught it). An Afterimage only softens one blow in five (never a full dodge, so the dodge cap is untouched). */
    static boolean defend(LivingIncomingDamageEvent e, ServerPlayer v, SaiyanState s, SaiyanManager.Run r, long now) {
        State t = r.tech4;
        if (t.counterUntil <= 0 && !t.afterimage) return false;
        if (!(e.getSource().getEntity() instanceof LivingEntity a) || a == v) return false;
        if (SaiyanPhaseRules.counterArmed(now, t.counterUntil)) {
            t.counterUntil = 0;
            double give = SaiyanPhaseRules.counterReflect(e.getAmount(), SaiyanPhaseConfig.counterFactor());
            e.setCanceled(true);
            if (SaiyanAbilities.allowed(v, a)) SaiyanAbilities.hurtBlow(v, a, give, SaiyanRules.growth(s.battlePower));
            spark(v.serverLevel(), color(v), a.getX(), a.getY() + a.getBbHeight() * 0.5, a.getZ(), 14);
            SaiyanAbilities.say(v, "t4_counter_hit");
            return true;
        }
        if (t.afterimage && v.getRandom().nextFloat() < 0.2f) {
            e.setAmount(e.getAmount() * 0.5f);
            SaiyanKiFx.send(v.serverLevel(), VfxShape.KI_AFTERIMAGE, v.position(), v.position().add(v.getLookAngle().scale(-0.8)), color(v), 8, 1f);
        }
        return false;
    }

    // ================================================================== Ground Breaker
    private static boolean breaker(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        State t = r.tech4;
        if (t.breakerUntil > now || !s.ready("breaker", now)) return false;
        if (!SaiyanAbilities.pay(p, s, SaiyanPhaseConfig.breakerStamina(), SaiyanPhaseConfig.breakerCost())) return false;
        s.startCooldown("breaker", now, SaiyanPhaseConfig.breakerCooldown());
        s.use("breaker", 1);
        r.safeFallUntil = now + 100;
        if (p.onGround()) {
            slam(p, s, r, 0.0, now);
        } else {
            t.breakerUntil = now + 60;
            t.breakerFromY = p.getY();
            Vec3 dm = p.getDeltaMovement();
            p.setDeltaMovement(dm.x * 0.3, -2.0, dm.z * 0.3);
            p.hurtMarked = true;
        }
        return true;
    }

    private static void breakerTick(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        State t = r.tech4;
        if (!p.isAlive() || now >= t.breakerUntil) {
            t.breakerUntil = 0;
            return;
        }
        if (p.onGround()) {
            double fall = Math.max(0.0, t.breakerFromY - p.getY());
            t.breakerUntil = 0;
            slam(p, s, r, fall, now);
        }
    }

    /** The shockwave: foes inside the radius (at most 8) take the blow, a fall makes it wider and harder; no block is touched. */
    private static void slam(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, double fall, long now) {
        ServerLevel level = p.serverLevel();
        double rad = SaiyanPhaseRules.slamRadius(SaiyanPhaseConfig.breakerRadius(), fall);
        double dmg = SaiyanPhaseConfig.breakerDamage() * SaiyanPhaseRules.slamFactor(fall);
        double growth = SaiyanRules.growth(s.battlePower);
        Vec3 at = p.position();
        for (LivingEntity e : foes(p, p.getBoundingBox().inflate(rad, 2.0, rad), SaiyanPhaseRules.MAX_SLAM_TARGETS)) {
            double d = Math.sqrt(sq(e.getX() - at.x) + sq(e.getZ() - at.z));
            if (d > rad) continue;
            if (SaiyanAbilities.hurtBlow(p, e, blowRaw(s, r, dmg), growth) > 0f) {
                Vec3 away = new Vec3(e.getX() - at.x, 0.0, e.getZ() - at.z);
                shove(e, away.lengthSqr() < 1.0e-6 ? p.getLookAngle() : away.normalize(), 0.8, 0.35);
            }
            SaiyanCombat.trainOn(s, e, SaiyanTraining.Cat.PHYSICAL, now);
        }
        p.fallDistance = 0.0f;
        SaiyanKiFx.send(level, VfxShape.KI_SHOCKWAVE, at, at, color(p), 18, (float) rad);
        sound(level, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), 0.9f, 0.7f);
        r.lastCombat = now;
    }

    private static double sq(double v) { return v * v; }

    // ================================================================== Combat Instinct
    private static boolean instinct(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        State t = r.tech4;
        if (t.instinctUntil > now || !s.ready("instinct", now)) return false;
        if (!SaiyanAbilities.pay(p, s, SaiyanPhaseConfig.instinctStamina(), SaiyanPhaseConfig.instinctCost())) return false;
        t.instinctUntil = now + 20L * SaiyanPhaseConfig.instinctSeconds();
        s.startCooldown("instinct", now, SaiyanPhaseConfig.instinctCooldown());
        s.use("instinct", 1);
        refreshTech(p, s, r, now);
        spark(p.serverLevel(), color(p), p.getX(), p.getY() + 1.0, p.getZ(), 10);
        SaiyanAbilities.say(p, "t4_instinct_on");
        return true;
    }

    /** Puts the timed modifiers (Combat Instinct speed and attack speed, the gravity slowdown) on the player; nothing changes when the values are the same. */
    static void refreshTech(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        State t = r.tech4;
        boolean inst = now < t.instinctUntil;
        double pen = t.gravityLevel > 0 ? SaiyanPhaseRules.gravityPenalty(t.gravityLevel, adapted(s)) : 0.0;
        SaiyanAttributes.applyTech(p, (inst ? SaiyanPhaseConfig.instinctSpeed() : 0.0) - pen, inst ? SaiyanPhaseConfig.instinctAttackSpeed() : 0.0, -pen);
    }

    // ================================================================== Charged Ki Sphere
    private static boolean sphere(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        State t = r.tech4;
        if (t.sphereLeft > 0 || !s.ready("sphere", now)) return false;
        if (!SaiyanAbilities.pay(p, s, SaiyanPhaseConfig.sphereStamina(), SaiyanPhaseConfig.sphereCost())) return false;
        double speed = SaiyanPhaseConfig.sphereSpeed();
        int travel = SaiyanPhaseRules.sphereTravel(SaiyanPhaseConfig.sphereRange(), speed);
        Vec3 dir = p.getLookAngle().normalize(), start = SaiyanAbilities.hand(p);
        t.spherePos = start;
        t.sphereDir = dir;
        t.sphereLeft = travel;
        s.startCooldown("sphere", now, SaiyanPhaseConfig.sphereCooldown());
        s.use("sphere", 1);
        r.lastCombat = now;
        SaiyanKiFx.send(p.serverLevel(), VfxShape.KI_SPHERE, start, start.add(dir.scale(speed * travel)), color(p), travel + 4, (float) SaiyanPhaseConfig.sphereRadius());
        sound(p.serverLevel(), p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_ACTIVATE, 0.7f, 1.6f);
        return true;
    }

    private static void sphereTick(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        State t = r.tech4;
        ServerLevel level = p.serverLevel();
        if (!p.isAlive() || t.spherePos == null) {
            t.sphereLeft = 0;
            return;
        }
        Vec3 next = t.spherePos.add(t.sphereDir.scale(SaiyanPhaseConfig.sphereSpeed()));
        BlockPos bp = BlockPos.containing(next);
        boolean stop = !level.hasChunkAt(bp) || level.getBlockState(bp).blocksMotion();
        t.sphereLeft--;
        if (!stop) {
            t.spherePos = next;
            if ((t.sphereLeft & 1) == 0) {
                List<LivingEntity> near = foes(p, AABB.ofSize(next, 2.4, 2.4, 2.4), 1);
                stop = !near.isEmpty();
            }
        }
        if (stop || t.sphereLeft <= 0) {
            detonate(p, s, r, t.spherePos, now);
            t.sphereLeft = 0;
            t.spherePos = null;
        }
    }

    private static void detonate(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, Vec3 at, long now) {
        ServerLevel level = p.serverLevel();
        double rad = SaiyanPhaseConfig.sphereRadius();
        for (LivingEntity e : foes(p, AABB.ofSize(at, rad * 2.0, rad * 2.0, rad * 2.0), SaiyanPhaseRules.MAX_SLAM_TARGETS)) {
            double d = e.getBoundingBox().getCenter().distanceTo(at);
            if (d > rad + 0.5) continue;
            double fall = 1.0 - 0.5 * Math.min(1.0, d / Math.max(0.5, rad));
            if (SaiyanDamage.ki(p, e, SaiyanPhaseConfig.sphereDamage() * fall * SaiyanDamage.formFactor(p), SaiyanRules.growth(s.battlePower)) > 0f)
                shove(e, e.position().subtract(at).multiply(1.0, 0.0, 1.0).normalize(), 0.6, 0.3);
            SaiyanCombat.trainOn(s, e, SaiyanTraining.Cat.KI, now);
        }
        SaiyanKiFx.send(level, VfxShape.KI_SHOCKWAVE, at, at, color(p), 16, (float) rad);
        sound(level, at.x, at.y, at.z, SoundEvents.GENERIC_EXPLODE.value(), 1.0f, 0.9f);
        r.lastCombat = now;
    }

    // ================================================================== Ki Wave (hold, then release: a channelled beam)
    private static boolean waveStart(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        State t = r.tech4;
        if (t.waveStart >= 0 || t.waveEnd > now || !s.ready("wave", now)) return false;
        t.waveStart = now;
        SaiyanKiFx.send(p.serverLevel(), VfxShape.KI_CHARGE, SaiyanAbilities.hand(p), SaiyanAbilities.hand(p), color(p), 24, 0.5f);
        return true;
    }

    /** The release is never rate limited (so a charge can not be stuck); it fizzles if the charge was short and pays the Ki only when the beam really fires. */
    static boolean waveRelease(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        State t = r.tech4;
        if (t.waveStart < 0) return false;
        int charge = (int) Math.max(0, Math.min(100, now - t.waveStart));
        t.waveStart = -1;
        int ticks = SaiyanPhaseRules.waveTicks(charge, SaiyanPhaseConfig.waveMinCharge(), SaiyanPhaseConfig.waveMaxChannel());
        if (ticks <= 0) {
            SaiyanAbilities.say(p, "t4_wave_fizzle");
            return true;
        }
        if (!SaiyanPhaseConfig.enabled() || !p.isAlive() || !s.ready("wave", now)) return true;
        if (!SaiyanKi.pay(p, SaiyanPhaseConfig.waveCost())) {
            SaiyanAbilities.say(p, "no_ki");
            return true;
        }
        t.waveBegin = now;
        t.waveEnd = now + ticks;
        s.startCooldown("wave", now, SaiyanPhaseConfig.waveCooldown());
        s.use("wave", 1);
        r.lastCombat = now;
        beamFx(p);
        sound(p.serverLevel(), p.getX(), p.getY(), p.getZ(), SoundEvents.BEACON_POWER_SELECT, 1.0f, 0.6f);
        return true;
    }

    private static void beamFx(ServerPlayer p) {
        Vec3 from = SaiyanAbilities.hand(p), to = from.add(p.getLookAngle().scale(SaiyanPhaseConfig.waveRange()));
        SaiyanKiFx.send(p.serverLevel(), VfxShape.KI_BEAM, from, to, color(p), 24, 0.8f);
    }

    private static void waveTick(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        State t = r.tech4;
        if (t.waveStart >= 0 && now - t.waveStart > 100) t.waveStart = -1;        // a charge nobody released
        if (t.waveEnd <= 0) return;
        if (!p.isAlive() || p.isSpectator() || now >= t.waveEnd) {
            t.waveEnd = 0;
            return;
        }
        if (((now - t.waveBegin) % 20L) == 10L) beamFx(p);
        if (!SaiyanPhaseRules.waveBeat(now, t.waveBegin)) return;
        SaiyanAbilities.Hit h = SaiyanAbilities.ray(p, p.getLookAngle(), SaiyanPhaseConfig.waveRange());
        if (h.target() != null) {
            SaiyanDamage.ki(p, h.target(), SaiyanPhaseConfig.waveDamage() * SaiyanDamage.formFactor(p), SaiyanRules.growth(s.battlePower));
            SaiyanCombat.trainOn(s, h.target(), SaiyanTraining.Cat.KI, now);
        }
        r.lastCombat = now;
    }

    // ================================================================== toggles
    private static boolean reinforce(ServerPlayer p, SaiyanManager.Run r) {
        State t = r.tech4;
        t.reinforce = !t.reinforce;
        SaiyanAbilities.say(p, t.reinforce ? "t4_reinforce_on" : "t4_reinforce_off");
        return true;
    }

    private static boolean afterimage(ServerPlayer p, SaiyanManager.Run r) {
        State t = r.tech4;
        t.afterimage = !t.afterimage;
        SaiyanAbilities.say(p, t.afterimage ? "t4_afterimage_on" : "t4_afterimage_off");
        return true;
    }

    private static boolean gravity(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        State t = r.tech4;
        if (!SaiyanPhaseConfig.gravityEnabled()) return false;
        if (t.gravityLevel > 0) {
            t.gravityLevel = 0;
            refreshTech(p, s, r, now);
            SaiyanAbilities.say(p, "t4_gravity_off");
        } else {
            t.gravityLevel = SaiyanPhaseRules.trainingLevel(adapted(s));
            t.gx = p.getX();
            t.gz = p.getZ();
            refreshTech(p, s, r, now);
            SaiyanAbilities.say(p, "t4_gravity_on");
        }
        return true;
    }

    // ================================================================== Giant Ki Avatar
    private static boolean avatar(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        State t = r.tech4;
        if (t.avatarUntil > now) {                                  // pressed again while it stands: it is sent away
            t.avatarUntil = 0;
            SaiyanAbilities.say(p, "t4_avatar_off");
            return true;
        }
        if (!SaiyanPhaseConfig.avatarEnabled() || !s.ready("avatar", now)) return false;
        SaiyanForms.Form f = SaiyanFormEngine.active(r);
        if (!SaiyanPhaseRules.avatarForm(f == null ? "" : f.id(), SaiyanPhaseConfig.avatarNeedsForm())) {
            SaiyanAbilities.say(p, "t4_avatar_form");
            return false;
        }
        if (!SaiyanKi.pay(p, SaiyanPhaseConfig.avatarCost())) {
            SaiyanAbilities.say(p, "no_ki");
            return false;
        }
        int ticks = SaiyanPhaseConfig.avatarTicks();
        t.avatarRose = now;
        t.avatarUntil = now + ticks;
        s.startCooldown("avatar", now, SaiyanPhaseConfig.avatarCooldown());
        s.use("avatar", 1);
        r.lastCombat = now;
        SaiyanKiFx.follow(p.serverLevel(), VfxShape.GIANT_AVATAR, p, p.position(), color(p), ticks, (float) SaiyanPhaseConfig.avatarScale());
        sound(p.serverLevel(), p.getX(), p.getY(), p.getZ(), SoundEvents.ENDER_DRAGON_FLAP, 0.8f, 1.2f);
        SaiyanAbilities.say(p, "t4_avatar_on");
        return true;
    }

    /** Once a second the avatar swings: foes in front of the Saiyan, at most six, take one blow each. The avatar itself is only a drawn effect (one packet): it is never an entity. */
    private static void avatarTick(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        State t = r.tech4;
        if (!p.isAlive() || p.isSpectator() || !SaiyanPhaseConfig.avatarEnabled() || !SaiyanPhaseRules.avatarStands(now, t.avatarUntil)) {
            t.avatarUntil = 0;
            return;
        }
        if (!SaiyanPhaseRules.avatarSwing(now, t.avatarRose)) return;
        double reach = SaiyanPhaseConfig.avatarReach();
        Vec3 look = p.getLookAngle(), c = p.position().add(look.x * reach * 0.5, 0.0, look.z * reach * 0.5);
        int hits = 0;
        for (LivingEntity e : foes(p, AABB.ofSize(c, reach * 2.0, reach, reach * 2.0), SaiyanPhaseRules.MAX_AVATAR_HITS)) {
            Vec3 to = e.position().subtract(p.position());
            double d = to.length();
            if (d > reach + 1.0 || (d > 2.0 && to.scale(1.0 / d).dot(look) < 0.2)) continue;
            if (SaiyanDamage.ki(p, e, SaiyanPhaseConfig.avatarDamage() * SaiyanDamage.formFactor(p), SaiyanRules.growth(s.battlePower)) > 0f) shove(e, look, 0.9, 0.3);
            hits++;
        }
        if (hits > 0) {
            SaiyanKiFx.send(p.serverLevel(), VfxShape.KI_SHOCKWAVE, c, c, color(p), 14, (float) (reach * 0.5));
            sound(p.serverLevel(), c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE.value(), 0.8f, 0.6f);
            r.lastCombat = now;
        }
    }

    // ================================================================== every tick while something runs, and once a second
    static void tick(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        State t = r.tech4;
        if (t.strikeTarget != null) strikeTick(p, s, r, now);
        if (t.breakerUntil > 0) breakerTick(p, s, r, now);
        if (t.sphereLeft > 0) sphereTick(p, s, r, now);
        if (t.waveStart >= 0 || t.waveEnd > 0) waveTick(p, s, r, now);
        if (t.avatarUntil > 0) avatarTick(p, s, r, now);
        if (t.ritualStart >= 0) SaiyanRitual.tick(p, s, r, now);
    }

    /** Once a second: the race stage and its buff, the upkeep of the toggles, the end of the timed ones, the gravity training. */
    static void second(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        SaiyanRace.second(p, s, r);
        State t = r.tech4;
        if (!t.toggled() && t.avatarUntil <= 0 && t.waveEnd <= 0) return;
        if (!SaiyanPhaseConfig.enabled()) {
            cancel(p, r);
            return;
        }
        if (t.reinforce && SaiyanPhaseConfig.reinforceDrain() > 0.0 && !SaiyanKi.pay(p, SaiyanPhaseConfig.reinforceDrain())) {
            t.reinforce = false;
            SaiyanAbilities.say(p, "t4_reinforce_off");
            r.dirty = true;
        }
        if (t.afterimage && SaiyanPhaseConfig.afterimageCost() > 0.0 && !SaiyanKi.pay(p, SaiyanPhaseConfig.afterimageCost())) {
            t.afterimage = false;
            SaiyanAbilities.say(p, "t4_afterimage_off");
            r.dirty = true;
        }
        if (t.waveEnd > now && SaiyanPhaseConfig.waveChannelCost() > 0.0 && !SaiyanKi.pay(p, SaiyanPhaseConfig.waveChannelCost())) t.waveEnd = 0;
        if (t.avatarUntil > now && SaiyanPhaseConfig.avatarDrain() > 0.0 && !SaiyanKi.pay(p, SaiyanPhaseConfig.avatarDrain())) {
            t.avatarUntil = 0;
            SaiyanAbilities.say(p, "t4_avatar_off");
            r.dirty = true;
        }
        if (t.instinctUntil > 0 && now >= t.instinctUntil) {
            t.instinctUntil = 0;
            SaiyanAbilities.say(p, "t4_instinct_off");
        }
        if (t.counterUntil > 0 && now >= t.counterUntil) t.counterUntil = 0;
        if (t.gravityLevel > 0) gravitySecond(p, s, r, now);
        refreshTech(p, s, r, now);
    }

    private static void gravitySecond(ServerPlayer p, SaiyanState s, SaiyanManager.Run r, long now) {
        State t = r.tech4;
        if (!SaiyanPhaseConfig.gravityEnabled() || p.isSpectator()) {
            t.gravityLevel = 0;
            return;
        }
        double cost = SaiyanPhaseRules.gravityStamina(t.gravityLevel);
        if (!p.isCreative()) {
            if (s.stamina < cost) {
                t.gravityLevel = 0;
                SaiyanAbilities.say(p, "t4_gravity_off");
                return;
            }
            s.stamina = SaiyanRules.stamina(s.stamina, -cost, SaiyanManager.staminaMax(s));
        }
        boolean moved = !Double.isNaN(t.gx) && sq(p.getX() - t.gx) + sq(p.getZ() - t.gz) > 1.0;
        t.gx = p.getX();
        t.gz = p.getZ();
        if (!SaiyanPhaseRules.gravityCounts(moved, now, r.lastCombat)) return;
        s.gravitySecs = Math.min(10_000_000, s.gravitySecs + 1);
        s.train(SaiyanTraining.Cat.PHYSICAL, SaiyanPhaseRules.gravityTrain(t.gravityLevel), now, SaiyanConfig.trainingScale());
        r.dirty = true;
    }
}
