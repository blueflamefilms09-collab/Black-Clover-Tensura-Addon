package com.newuniverse.nusmp.arc.midnight;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.balance.BalanceLaw;
import com.newuniverse.nusmp.blackclover.MagicType;
import com.newuniverse.nusmp.book.MirrorWorks;
import com.newuniverse.nusmp.entity.zogratis.DanteMoves;
import com.newuniverse.nusmp.entity.zogratis.Tells;
import com.newuniverse.nusmp.entity.zogratis.ZogratisBoss;
import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.VfxSpawn;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.IntConsumer;

/**
 * Why: the four Midnight Sun bosses strike with the same handful of shapes (a ring on the floor, a lane, a cone, a bolt, a mark that lands later) and must
 * all do four things the same way: never take the server tick down when one effect throws, test the foes with the same geometry the telegraph drew
 * ({@link MidnightMath}), let a raised shield and anti-magic cut a blow the way the rest of the mod's bosses allow, and never flood the clients with effect
 * packets. This class is those four things as small static functions, so a boss class reads as the story of its fight (what each move is) and not as
 * bookkeeping. It is glue (it touches the game), kept free of fight rules: what a move means lives in the boss, how a shape is tested lives here.
 * Every function runs on the server thread only.
 */
final class MidnightFx {
    private MidnightFx() {}

    /** The warning colour of a hard lock: the lane or ring that has stopped following its target. */
    static final int WARN = 0xFF4A4A;
    /** Where a player's last turning-back of an attack is noted (game time), so a mirror is a skill and not a wall. */
    static final String K_TURNED = "nusmp_midnight_turned";
    private static final String K_FULL_REFLECT = "nusmp_full_reflect";

    // ================================================================ guards
    /**
     * Zone steps and delayed tasks run inside the server tick without a net under them, so every one of the Midnight Sun bosses' goes through here: a step
     * that throws is logged once and that zone or task stops, it never takes the tick down.
     */
    static IntConsumer safe(String what, IntConsumer step) {
        boolean[] dead = {false};
        return age -> {
            if (dead[0]) return;
            try {
                step.accept(age);
            } catch (Throwable t) {
                dead[0] = true;
                LogUtils.getLogger().warn("[nusmp] a Midnight Sun boss's {} stopped: {}", what, t.toString());
            }
        };
    }

    /** {@link #safe(String, IntConsumer)} for a delayed task. */
    static Runnable safe(String what, Runnable task) {
        IntConsumer guarded = safe(what, age -> task.run());
        return () -> guarded.accept(0);
    }

    // ================================================================ geometry on the world
    static Vec3 rotateY(Vec3 v, double angle) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        return new Vec3(v.x * cos - v.z * sin, v.y, v.x * sin + v.z * cos);
    }

    /** The horizontal unit vector from 'a' towards 'b' (the fallback if they are on top of each other, then south). */
    static Vec3 flatDir(Vec3 a, Vec3 b, Vec3 fallback) {
        Vec3 d = b.subtract(a).multiply(1, 0, 1);
        if (d.lengthSqr() < 1e-4) {
            d = fallback == null ? Vec3.ZERO : fallback.multiply(1, 0, 1);
            if (d.lengthSqr() < 1e-4) d = new Vec3(0, 0, 1);
        }
        return d.normalize();
    }

    /** Where a straight line from 'a' to 'b' first meets a wall ('b' itself if the way is clear). */
    static Vec3 clipWall(ServerLevel sl, Entity by, Vec3 a, Vec3 b) {
        BlockHitResult hit = sl.clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, by));
        return hit.getType() == HitResult.Type.MISS ? b : hit.getLocation();
    }

    /** How far the feet of 'e' are above the floor under them (0 when they stand on it): a wave of fire is jumped, a lane is not. */
    static double floorHeight(Level level, LivingEntity e) {
        if (e.onGround()) return 0;
        return Math.max(0, e.getY() - ZogratisBoss.ground(level, e.position()).y);
    }

    /** Is there a roof or ledge within {@code height} blocks above the foe's head? */
    static boolean covered(ServerLevel sl, LivingEntity e, double height) {
        Vec3 from = e.getEyePosition().add(0, 0.3, 0);
        BlockHitResult hit = sl.clip(new ClipContext(from, from.add(0, height, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e));
        return hit.getType() != HitResult.Type.MISS;
    }

    /** Is there a wall between the point and the foe's eyes? */
    static boolean walled(ServerLevel sl, Vec3 from, LivingEntity e) {
        BlockHitResult hit = sl.clip(new ClipContext(from, e.getEyePosition(), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, e));
        return hit.getType() != HitResult.Type.MISS;
    }

    // ================================================================ who stands in a shape
    /** Foes inside a ring of {@code radius} on the floor at {@code c} (a window of 3.5 below and 4.5 above, the body's width counted). */
    static List<LivingEntity> inRing(ZogratisBoss boss, Vec3 c, double radius) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : boss.foesAround(c, radius + 3)) {
            if (MidnightMath.inRing(c.x, c.z, e.getX(), e.getZ(), radius, e.getBbWidth() / 2) && MidnightMath.withinHeight(e.getY() - c.y, 3.5, 4.5)) out.add(e);
        }
        return out;
    }

    /** Foes within {@code halfWidth} of the lane from 'a' to 'b' (height window of 3.5 round the lane), the nearest to 'a' first. */
    static List<LivingEntity> inLane(ZogratisBoss boss, Vec3 a, Vec3 b, double halfWidth) {
        Vec3 mid = a.add(b).scale(0.5);
        double reach = a.distanceTo(b) / 2 + halfWidth + 2;
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : boss.foesAround(mid, reach)) {
            double d = MidnightMath.distToSegment(e.getX(), e.getZ(), a.x, a.z, b.x, b.z);
            double t = MidnightMath.alongSegment(e.getX(), e.getZ(), a.x, a.z, b.x, b.z);
            double laneY = a.y + (b.y - a.y) * t;
            if (d <= halfWidth + e.getBbWidth() / 2 && Math.abs(e.getY() + e.getBbHeight() / 2 - laneY) <= 3.5) out.add(e);
        }
        out.sort(Comparator.comparingDouble(e -> MidnightMath.alongSegment(e.getX(), e.getZ(), a.x, a.z, b.x, b.z)));
        return out;
    }

    /** Foes inside a cone whose tip is {@code tip}, pointing along the horizontal part of {@code dir}. */
    static List<LivingEntity> inCone(ZogratisBoss boss, Vec3 tip, Vec3 dir, double reach, double halfAngle) {
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity e : boss.foesAround(tip, reach + 2)) {
            if (MidnightMath.inCone(e.getX() - tip.x, e.getZ() - tip.z, dir.x, dir.z, reach, halfAngle, e.getBbWidth() / 2)
                    && MidnightMath.withinHeight(e.getY() - tip.y, 3.0, 3.5)) out.add(e);
        }
        return out;
    }

    // ================================================================ blows
    /** Is this foe holding anti-magic (a demon sword or the Anti-Magic Lord)? */
    static boolean antiMagic(LivingEntity e) { return e instanceof Player p && DanteMoves.antiMagicHeld(p); }

    /**
     * A blow of {@code base} on {@code v} through the boss's damage law, cut by the foe's guard: a raised shield halves any blow, anti-magic in hand cuts a
     * spell ({@code spell}) to 55%. The whole group's {@code damageScale} dial is applied here. Returns whether it landed.
     */
    static boolean hit(ZogratisBoss boss, LivingEntity v, float base, boolean spell) {
        if (boss == null || v == null || !v.isAlive()) return false;
        double g = MidnightMath.guardFactor(v.isBlocking(), antiMagic(v), spell);
        return boss.strike(v, (float) (base * MidnightConfig.damageScale() * g), spell);
    }

    /** A blow whose guard has been counted by the caller (the Verdict and the Evil Heart cut by cover and shelter themselves). */
    static boolean hitRaw(ZogratisBoss boss, LivingEntity v, float base, boolean spell) {
        if (boss == null || v == null || !v.isAlive()) return false;
        return boss.strike(v, base * MidnightConfig.damageScale(), spell);
    }

    /** Throws the foe away from {@code from} (strength as the framework's knockback) with a little lift if asked. */
    static void shove(LivingEntity v, Vec3 from, double strength, double lift) {
        Vec3 d = v.position().subtract(from).multiply(1, 0, 1);
        if (d.lengthSqr() < 1e-4) d = new Vec3(0, 0, 1);
        d = d.normalize();
        if (v instanceof Player p && p.isFallFlying()) p.stopFallFlying();
        v.knockback(strength, -d.x, -d.z);
        if (lift > 0) {
            v.setDeltaMovement(v.getDeltaMovement().add(0, lift, 0));
            v.hurtMarked = true;
        }
    }

    /** Throws the foe sideways to the lane it stood in (strength as the framework's knockback). */
    static void shoveAside(LivingEntity v, Vec3 laneFrom, Vec3 laneTo, double strength) {
        Vec3 along = laneTo.subtract(laneFrom).multiply(1, 0, 1);
        if (along.lengthSqr() < 1e-4) return;
        along = along.normalize();
        Vec3 rel = v.position().subtract(laneFrom).multiply(1, 0, 1);
        double side = rel.x * -along.z + rel.z * along.x;
        double s = side >= 0 ? 1 : -1;
        v.knockback(strength, -(-along.z * s), -(along.x * s));
    }

    /**
     * Does the shared control rule let this boss hold someone now? True (and the twelve-second cooldown starts) at most once per twelve seconds per boss, and
     * only if the server allows the group's control effects. The balance law owns the numbers; this is only the door.
     */
    static boolean controlAllowed(ZogratisBoss boss) { return MidnightConfig.controls() && BalanceLaw.beginControl(boss); }

    /** Roots {@code v} for {@code ticks} (capped and diminished by the balance law). Call only after {@link #controlAllowed} said yes. */
    static void root(LivingEntity v, int ticks) {
        int t = BalanceLaw.controlTicks(v, ticks);
        v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, t, 6));
        v.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, t + 20, 0));
    }

    /** What a hit of a magic does besides damage: short, so no magic holds anyone for long. Blindness and levitation obey the control rule. */
    static void rider(MagicType.HitEffect effect, LivingEntity v, ZogratisBoss boss) {
        if (effect == null || v == null || !v.isAlive()) return;
        switch (effect) {
            case BURN -> v.igniteForSeconds(2);
            case FREEZE -> v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 2));
            case SHOCK, WEAKEN -> v.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0));
            case SLOW -> v.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 1));
            case POISON -> v.addEffect(new MobEffectInstance(MobEffects.POISON, 50, 0));
            case WITHER -> v.addEffect(new MobEffectInstance(MobEffects.WITHER, 40, 0));
            case BLIND -> { if (controlAllowed(boss)) v.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BalanceLaw.controlTicks(v, 40), 0)); }
            case LEVITATE -> { if (controlAllowed(boss)) v.addEffect(new MobEffectInstance(MobEffects.LEVITATION, BalanceLaw.controlTicks(v, 25), 0)); }
            case PUSH -> shove(v, boss.position(), 0.7, 0);
            case PULL -> v.knockback(0.5, v.getX() - boss.getX(), v.getZ() - boss.getZ());
            default -> { }
        }
    }

    // ================================================================ turning an attack back
    /**
     * What, if anything, lets this foe turn an attack back right now: a Full Reflection mirror, a live Mirror Magic array, or (for a flying attack only) an
     * anti-magic swing; the foe must be a player who has not turned one back in the last five seconds, and the server must allow it.
     */
    static ReflectRules.Kind reflectKind(LivingEntity e, boolean projectile) {
        if (!(e instanceof ServerPlayer p) || !MidnightConfig.reflects()) return ReflectRules.Kind.NONE;
        long now = p.level().getGameTime();
        if (now < p.getPersistentData().getLong(K_TURNED) + ReflectRules.PLAYER_COOLDOWN) return ReflectRules.Kind.NONE;
        boolean full = p.getPersistentData().getLong(K_FULL_REFLECT) > now;
        boolean mirrors = MirrorWorks.mirrorsLeft(p) > 0;
        return ReflectRules.decide(full, mirrors, DanteMoves.antiMagicHeld(p), p.swinging, projectile);
    }

    /** Notes that this player has just turned an attack back. */
    static void markTurned(ServerPlayer p) { p.getPersistentData().putLong(K_TURNED, p.level().getGameTime()); }

    // ================================================================ effects
    /** One effect packet at most per the boss's per-tick allowance; colours are 0xRRGGBB. */
    static void fx(ZogratisBoss boss, VfxShape shape, Vec3 from, Vec3 to, int rgb, int ticks, float power) {
        if (!(boss.level() instanceof ServerLevel sl)) return;
        if (boss instanceof MidnightBoss mb && !mb.fxAllowed()) return;
        VfxSpawn.send(sl, shape, from, to, 0xFF000000 | (rgb & 0xFFFFFF), Math.max(1, ticks), power);
    }

    static void sound(Level level, Vec3 at, SoundEvent ev, float volume, float pitch) {
        if (level instanceof ServerLevel sl) sl.playSound(null, BlockPos.containing(at), ev, SoundSource.HOSTILE, volume, pitch);
    }

    /** A line on the action bar of every player within {@code r} blocks of {@code at}. */
    static void bar(ServerLevel sl, Vec3 at, double r, Component text) {
        for (ServerPlayer p : sl.players()) if (p.distanceToSqr(at) < r * r) p.displayClientMessage(text, true);
    }

    /** The two edges and the rim of a cone, drawn in dust: what the party reads as "this is where it will land". */
    static void coneTell(ServerLevel sl, Vec3 tip, Vec3 dir, double reach, double halfAngle, int rgb) {
        Vec3 d = dir.multiply(1, 0, 1);
        if (d.lengthSqr() < 1e-4) return;
        d = d.normalize();
        Vec3 left = rotateY(d, halfAngle), right = rotateY(d, -halfAngle);
        Tells.line(sl, tip, tip.add(left.scale(reach)), rgb);
        Tells.line(sl, tip, tip.add(right.scale(reach)), rgb);
        Vec3 prev = tip.add(left.scale(reach));
        int steps = 6;
        for (int i = 1; i <= steps; i++) {
            Vec3 p = tip.add(rotateY(d, halfAngle - 2 * halfAngle * i / steps).scale(reach));
            Tells.line(sl, prev, p, rgb);
            prev = p;
        }
    }
}
