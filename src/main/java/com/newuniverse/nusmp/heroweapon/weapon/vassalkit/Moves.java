package com.newuniverse.nusmp.heroweapon.weapon.vassalkit;

import com.newuniverse.nusmp.thread.SafeSpot;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the Harpoon reels things in, the Chain pulls and swings, the Mirror and the Sewing Set step through space, and all of them must move a creature without
 * ever putting it inside a wall, over lava or on a fire (the owner forbids changing blocks, so moving people is the only way these weapons "reposition" a fight).
 * Pulls are velocity impulses repeated every tick by the caller (the same method the mod's Chain Magic uses, so the client is told through the hurt-marked flag);
 * dashes and steps are teleports to the farthest spot {@link SafeSpot} accepts along the way, never through a wall. Server thread only.
 */
public final class Moves {
    private Moves() {}

    /**
     * One tick of a pull of {@code e} towards {@code to}: sets its velocity, gently as it nears the goal. {@code weight} scales the speed (1 for a light
     * creature, small for a heavy one or a boss). Returns true once it is within {@code stopDist} of the goal (the caller ends the pull).
     */
    public static boolean pullStep(Entity e, Vec3 to, double maxSpeed, double stopDist, double weight) {
        Vec3 d = to.subtract(e.position());
        double dist = d.length();
        double speed = Geo.pullSpeed(dist, stopDist, maxSpeed) * weight;
        if (speed <= 0.0 || dist < 1e-6) return true;
        Vec3 v = d.scale(speed / dist);
        e.setDeltaMovement(v.x, Math.min(0.5, v.y * 0.6 + 0.08), v.z);
        e.hurtMarked = true;
        e.fallDistance = 0f;
        return false;
    }

    /**
     * Teleports the wielder along the horizontal part of {@code dir} as far as {@code maxDist} blocks, to the farthest spot a creature may stand on, stopping short
     * of a wall. Returns where they landed, or null when there is no room even one block ahead (nothing moves then).
     */
    public static Vec3 dash(ServerPlayer p, Vec3 dir, double maxDist) {
        Vec3 flat = new Vec3(dir.x, 0, dir.z);
        if (flat.lengthSqr() < 1e-6) return null;
        flat = flat.normalize();
        Vec3 from = p.position(), head = from.add(0, p.getEyeHeight() * 0.9, 0);
        Vec3 wall = Foes.wallEnd(p, head, head.add(flat.scale(maxDist + 0.6)));
        double reach = Math.min(maxDist, Math.max(0.0, wall.distanceTo(head) - 0.7));
        for (double d = Math.floor(reach); d >= 1.0; d -= 1.0) {
            Vec3 spot = SafeSpot.firstOk(p.serverLevel(), p, from.add(flat.scale(d)));
            if (spot == null) continue;
            p.teleportTo(spot.x, spot.y, spot.z);
            p.fallDistance = 0f;
            return spot;
        }
        return null;
    }

    /** A spot next to {@code center} where {@code subject} may stand (a ring of eight is tried), or null. */
    public static Vec3 landingNear(ServerPlayer p, Entity subject, Vec3 center, double radius, double phase) {
        return SafeSpot.firstOk(p.serverLevel(), subject, SafeSpot.ringAround(center, radius, phase));
    }
}
