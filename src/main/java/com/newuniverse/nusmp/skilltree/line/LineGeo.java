package com.newuniverse.nusmp.skilltree.line;

import com.newuniverse.nusmp.skilltree.ability.AbilityKit;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Why: half of the 26 arts need the same four geometric answers (the creature the player is looking at, the point their look ends on, the nearest few of a crowd, a safe place to stand after a move)
 * and the answers must be the same everywhere or two arts of the same family would aim differently. They are written once here on top of {@link AbilityKit}'s own shape helpers (ring, line, cone), so
 * the allies/foes rule ({@code isAlliedTo}, spectators, the dead) is the one every other tree technique uses. Block checks use the collision clip of the level (a wall stops a beam, a blink and a dash)
 * and never change a block. All functions are static and take what they need; none keeps anything.
 */
public final class LineGeo {
    private LineGeo() {}

    /** Where the player's look ends within {@code range}: the first solid block, or the point {@code range} away. */
    public static Vec3 lookEnd(ServerPlayer p, double range) {
        Vec3 from = p.getEyePosition(), to = from.add(p.getViewVector(1f).scale(range));
        var hit = p.serverLevel().clip(new ClipContext(from, to, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        return hit.getType() == HitResult.Type.MISS ? to : hit.getLocation();
    }

    /** The nearest living foe the player is looking at within {@code range} (a thin beam, stopped by walls), or null. */
    public static LivingEntity lookTarget(ServerPlayer p, double range) {
        Vec3 from = p.getEyePosition(), end = lookEnd(p, range);
        LivingEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (LivingEntity e : AbilityKit.along(p, from, end, 0.6)) {
            double d = e.distanceToSqr(from);
            if (d < bestD) {
                bestD = d;
                best = e;
            }
        }
        return best;
    }

    /** The nearest {@code n} of {@code list} to {@code from}, nearest first (the list is not changed). */
    public static List<LivingEntity> nearest(List<LivingEntity> list, Vec3 from, int n) {
        List<LivingEntity> copy = new ArrayList<>(list);
        copy.sort(Comparator.comparingDouble(e -> e.distanceToSqr(from)));
        return copy.size() <= n ? copy : new ArrayList<>(copy.subList(0, Math.max(0, n)));
    }

    /** The horizontal part of a vector, normalised (zero stays zero). */
    public static Vec3 flat(Vec3 v) {
        Vec3 h = new Vec3(v.x, 0, v.z);
        return h.lengthSqr() < 1e-6 ? Vec3.ZERO : h.normalize();
    }

    /** Pushes {@code target} away from the player: the game's own knockback, so armour and knockback resistance still count. */
    public static void shove(ServerPlayer p, LivingEntity target, double strength) {
        if (strength > 0) target.knockback(strength, p.getX() - target.getX(), p.getZ() - target.getZ());
    }

    /**
     * The furthest free place on the segment from the player toward {@code to}: stops at a wall, and backs off until the player's box fits. Never inside a block, never farther than asked.
     */
    public static Vec3 freeAlong(ServerPlayer p, Vec3 to) {
        ServerLevel level = p.serverLevel();
        Vec3 from = p.position();
        Vec3 eyeFrom = p.getEyePosition(), eyeTo = to.add(0, p.getEyeHeight(), 0);
        var hit = level.clip(new ClipContext(eyeFrom, eyeTo, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 target = hit.getType() == HitResult.Type.MISS ? to : hit.getLocation().subtract(0, p.getEyeHeight(), 0);
        Vec3 dir = target.subtract(from);
        double len = dir.length();
        if (len < 1e-4) return from;
        Vec3 step = dir.scale(1.0 / len);
        for (double d = len; d > 0.2; d -= 0.5) {
            Vec3 c = from.add(step.scale(d));
            AABB box = p.getBoundingBox().move(c.x - p.getX(), c.y - p.getY(), c.z - p.getZ());
            if (level.noCollision(p, box)) return c;
        }
        return from;
    }

    /** Moves the player to a place (no fall damage from the move) and keeps them looking the same way. */
    public static void moveTo(ServerPlayer p, Vec3 to) {
        p.teleportTo(to.x, to.y, to.z);
        p.fallDistance = 0f;
        p.setDeltaMovement(Vec3.ZERO);
        p.hurtMarked = true;
    }
}
