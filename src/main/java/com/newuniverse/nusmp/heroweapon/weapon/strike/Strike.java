package com.newuniverse.nusmp.heroweapon.weapon.strike;

import com.mojang.logging.LogUtils;
import com.newuniverse.nusmp.book.SpellRuntime;
import com.newuniverse.nusmp.heroweapon.skill.HeroCtx;
import com.newuniverse.nusmp.relics.Fx;
import com.newuniverse.nusmp.relics.djinn.weapon.WeaponGeo;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Why: the Hammer, the Claw, the Axe and the Whip are four different weapons, but each of their techniques does the same handful of server-side chores, and every one of those
 * chores has a way to go wrong that the owner's rules forbid: picking targets (never a creative or spectator player, an ally, a pet or a summon: the rule the relics already use,
 * {@link Fx#foesAround}), hitting (only through {@link HeroCtx#hit}, with the foe's hurt timer reset so a technique lands on top of a melee blow, and never recursing into the
 * weapon's own melee hook), acting later (a tell now, the blow a few ticks after, which must survive the wielder dying, leaving or putting the weapon away), moving the wielder
 * (no fall damage from the technique's own leap) and shoving foes. Writing these once, here, keeps the four weapons honest and short. Server thread only.
 */
public final class Strike {
    private Strike() {}

    private static final org.slf4j.Logger LOG = LogUtils.getLogger();

    /** True while one of the wielder's techniques is dealing damage: the weapon's own melee hook must ignore the blow it would otherwise count as a swing. */
    private static boolean inHit;

    public static boolean inHit() { return inHit; }

    // ------------------------------------------------------------------ direction and place
    /** The horizontal direction the wielder faces (never zero, even when looking straight up or down). */
    public static Vec3 flatLook(ServerPlayer p) { return Vec3.directionFromRotation(0f, p.getYRot()); }

    /** The middle of a body. */
    public static Vec3 center(LivingEntity t) { return t.getBoundingBox().getCenter(); }

    /** True for a foe standing on something (or swimming): ground waves and falling blows do not reach what flies or has been thrown into the air. */
    public static boolean grounded(LivingEntity t) { return t.onGround() || t.isInWater(); }

    /** The end of the segment from {@code from} to {@code to}, shortened to the first block in the way (a technique does not pass through a wall). */
    public static Vec3 stopAtWall(ServerPlayer p, Vec3 from, Vec3 to) {
        HitResult hit = p.serverLevel().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
    }

    /** The point on the ground under {@code at} (up to 24 blocks down), or {@code at} itself when there is none. */
    public static Vec3 groundUnder(ServerPlayer p, Vec3 at) {
        HitResult hit = p.serverLevel().clip(new ClipContext(at.add(0, 0.5, 0), at.add(0, -24, 0), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return hit.getType() == HitResult.Type.MISS ? at : hit.getLocation();
    }

    // ------------------------------------------------------------------ who is a foe
    private static List<LivingEntity> nearestFirst(List<LivingEntity> in, Vec3 ref, int max) {
        in.sort(Comparator.comparingDouble(t -> t.position().distanceToSqr(ref)));
        return in.size() > max ? new ArrayList<>(in.subList(0, max)) : in;
    }

    /**
     * Foes within {@code radius} of {@code centre} (nearest to the centre first, at most {@code max}). A foe is what the relics' acts strike: a hostile monster, a mob that
     * hunts the wielder, a player the wielder may harm; never a creative or spectator player, an ally, a pet or a summon.
     */
    public static List<LivingEntity> foesNear(ServerPlayer p, Vec3 centre, double radius, int max) {
        double reach = Math.min(32.0, radius + centre.distanceTo(p.position()));
        double r2 = radius * radius, tall = 2.5 + radius * 0.3;
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity t : Fx.foesAround(p, reach)) if (t.position().distanceToSqr(centre) <= r2 && Math.abs(t.getY() - centre.y) <= tall) out.add(t);
        return nearestFirst(out, centre, max);
    }

    /** Foes within {@code radius} of the wielder. */
    public static List<LivingEntity> foesAround(ServerPlayer p, double radius, int max) { return foesNear(p, p.position(), radius, max); }

    /** Foes in front of the wielder: within {@code range} and {@code halfAngleDeg} of the way they face (a flat cone, with a vertical allowance). */
    public static List<LivingEntity> foesCone(ServerPlayer p, double range, double halfAngleDeg, int max) {
        Vec3 flat = flatLook(p);
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity t : Fx.foesAround(p, range + 2.0)) {
            if (WeaponGeo.inCone(p.getX(), p.getZ(), flat.x, flat.z, t.getX(), t.getZ(), range + t.getBbWidth() * 0.5, halfAngleDeg)
                    && Math.abs(t.getY() - p.getY()) <= 2.5 + range * 0.3) out.add(t);
        }
        return nearestFirst(out, p.position(), max);
    }

    /** Foes within {@code halfWidth} of the segment from {@code a} to {@code b}, nearest to {@code a} first. */
    public static List<LivingEntity> foesLine(ServerPlayer p, Vec3 a, Vec3 b, double halfWidth, int max) {
        double reach = Math.min(32.0, p.position().distanceTo(a.add(b).scale(0.5)) + a.distanceTo(b) / 2 + halfWidth + 2.0);
        List<LivingEntity> out = new ArrayList<>();
        for (LivingEntity t : Fx.foesAround(p, reach)) {
            Vec3 m = center(t);
            if (WeaponGeo.segmentDistance(a.x, a.y, a.z, b.x, b.y, b.z, m.x, m.y, m.z) <= halfWidth + t.getBbWidth() * 0.5) out.add(t);
        }
        return nearestFirst(out, a, max);
    }

    /** The foe the wielder is aiming at within {@code range}: in line of sight, within {@code halfAngleDeg} of the look, the closer and more central the better. Null when none. */
    public static LivingEntity pick(ServerPlayer p, double range, double halfAngleDeg) {
        Vec3 eye = p.getEyePosition(), look = p.getLookAngle();
        double min = Math.cos(Math.toRadians(halfAngleDeg));
        LivingEntity best = null;
        double bestScore = Double.MAX_VALUE;
        for (LivingEntity t : Fx.foesAround(p, range + 2.0)) {
            Vec3 to = center(t).subtract(eye);
            double dist = to.length();
            if (dist > range + t.getBbWidth() * 0.5) continue;
            double cos = dist < 1.0E-4 ? 1.0 : to.scale(1.0 / dist).dot(look);
            if (cos < min || !p.hasLineOfSight(t)) continue;
            double score = dist * (2.0 - cos);
            if (score < bestScore) {
                bestScore = score;
                best = t;
            }
        }
        return best;
    }

    // ------------------------------------------------------------------ hurting and moving foes
    /**
     * A technique's hit on one foe, through the balance law. The foe's hurt timer is reset first so the blow lands on top of a melee swing; {@code rapid} is true for one hit of
     * several in a quick row, where a PLAYER keeps the timer (so a flurry cannot strip a player's health faster than the cap allows). False when nothing was hurt.
     */
    public static boolean hit(HeroCtx c, LivingEntity t, float raw, boolean rapid) {
        if (t == null || !t.isAlive()) return false;
        if (t instanceof Player pl && (pl.isCreative() || pl.isSpectator())) return false;
        boolean was = inHit;
        inHit = true;
        try {
            if (!rapid || !(t instanceof Player)) t.invulnerableTime = 0;
            return c.hit(t, raw);
        } finally {
            inHit = was;
        }
    }

    /** The same hit without the shove a blow gives (a bleed or a wound must not push the foe about every second). */
    public static boolean hitStill(HeroCtx c, LivingEntity t, float raw) {
        Vec3 v = t.getDeltaMovement();
        boolean landed = hit(c, t, raw, true);
        if (landed) t.setDeltaMovement(v);
        return landed;
    }

    /** Pushes a foe away from {@code from} (horizontal) and tells its client about it. */
    public static void shove(HeroCtx c, LivingEntity t, Vec3 from, double strength) {
        c.shove(t, from, strength);
        t.hurtMarked = true;
    }

    /** Throws a body upwards by {@code dy}. */
    public static void lift(LivingEntity t, double dy) {
        Vec3 v = t.getDeltaMovement();
        t.setDeltaMovement(v.x, Math.max(v.y, 0.0) + dy, v.z);
        t.hurtMarked = true;
    }

    /** A mob effect by id (the relics' helper: a missing effect is skipped, the time is capped). */
    public static void effect(LivingEntity t, String effectId, int ticks, int amp) { Fx.addEffect(t, effectId, ticks, amp); }

    // ------------------------------------------------------------------ moving the wielder
    /** Launches the wielder: {@code horizontal} blocks per tick along {@code dir} (flat) and {@code vertical} up; the technique's own leap never hurts them. */
    public static void leap(ServerPlayer p, Vec3 dir, double horizontal, double vertical) {
        Vec3 d = new Vec3(dir.x, 0, dir.z);
        d = d.lengthSqr() < 1.0E-6 ? flatLook(p) : d.normalize();
        p.setDeltaMovement(d.x * horizontal, vertical, d.z * horizontal);
        p.hurtMarked = true;
        p.fallDistance = 0f;
    }

    // ------------------------------------------------------------------ acting later
    /**
     * Runs {@code task} after {@code delay} ticks with a fresh context of the wielder, but only if the wielder is still there: alive, in the same world and still bonded. The tell
     * of a technique is drawn at once and its blow is this; a wielder who died, left or lost the weapon in between simply does not strike.
     */
    public static void later(ServerPlayer p, int delay, Consumer<HeroCtx> task) {
        ServerLevel level = p.serverLevel();
        SpellRuntime.later(level, Math.max(1, delay), () -> {
            HeroCtx cx = live(p, level);
            if (cx == null) return;
            try {
                task.accept(cx);
            } catch (RuntimeException ex) {
                LOG.warn("[nusmp] a delayed hero technique failed", ex);
            }
        });
    }

    /** Runs {@code step(ctx, age)} every {@code interval} ticks for {@code ticks} ticks, as long as the wielder is still there (a step after they left does nothing). */
    public static void zone(ServerPlayer p, int ticks, int interval, BiConsumer<HeroCtx, Integer> step) {
        ServerLevel level = p.serverLevel();
        SpellRuntime.zone(level, ticks, interval, age -> {
            HeroCtx cx = live(p, level);
            if (cx == null) return;
            try {
                step.accept(cx, age);
            } catch (RuntimeException ex) {
                LOG.warn("[nusmp] a hero technique step failed", ex);
            }
        });
    }

    private static HeroCtx live(ServerPlayer p, ServerLevel level) {
        if (p.isRemoved() || !p.isAlive() || p.serverLevel() != level) return null;
        return HeroCtx.of(p);
    }
}
