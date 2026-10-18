package com.newuniverse.nusmp.elite.world.entity;

import com.newuniverse.nusmp.particle.rukh.RukhFx;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the telegraphs and the geometry the two calamity bosses share. A telegraph is the promise of the fight: every blow is marked on the ground with Rukh (and only Rukh: the owner's rule for
 * boss effects) for the whole wind-up, in the SHAPE of the blow (a ring, a cone, a line), so a player can always read where not to stand. The geometry (is this point inside the cone, is the
 * line from the boss to the player free of blocks) is pure arithmetic kept here so the bosses read like their move lists.
 */
public final class BossFx {
    private BossFx() {}

    /** A ring of Rukh on the ground, {@code points} particles (at most 12 are asked per call so the Rukh budget of a level is never drained by one telegraph). */
    public static void ring(ServerLevel l, RukhFx.Alignment a, Vec3 c, double radius, int points) {
        int n = Math.max(3, Math.min(12, points));
        double phase = (l.getGameTime() % 20) / 20.0 * (Math.PI * 2 / n);
        for (int i = 0; i < n; i++) {
            double ang = phase + i * (Math.PI * 2 / n);
            RukhFx.spray(l, a, c.x + Math.cos(ang) * radius, c.y + 0.12, c.z + Math.sin(ang) * radius, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** The two edges and the arc of a cone on the ground. {@code half} is the half angle in degrees. */
    public static void cone(ServerLevel l, RukhFx.Alignment a, Vec3 origin, double dirX, double dirZ, double range, double half) {
        double base = Math.atan2(dirZ, dirX), h = Math.toRadians(half);
        for (int k = 0; k < 3; k++) {
            double ang = base - h + k * h;
            for (int s = 1; s <= 2; s++) {
                double r = range * s / 2.0;
                RukhFx.spray(l, a, origin.x + Math.cos(ang) * r, origin.y + 0.12, origin.z + Math.sin(ang) * r, 1, 0.0, 0.0, 0.0, 0.0);
            }
        }
        for (int k = 0; k < 4; k++) {
            double ang = base - h + k * (2 * h / 3.0);
            RukhFx.spray(l, a, origin.x + Math.cos(ang) * range, origin.y + 0.12, origin.z + Math.sin(ang) * range, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** A line of Rukh from a point along a direction. */
    public static void line(ServerLevel l, RukhFx.Alignment a, Vec3 from, double dirX, double dirZ, double length, int points) {
        double len = Math.hypot(dirX, dirZ);
        if (len < 1e-6) return;
        double ux = dirX / len, uz = dirZ / len;
        int n = Math.max(2, Math.min(12, points));
        for (int i = 1; i <= n; i++) {
            double d = length * i / n;
            RukhFx.spray(l, a, from.x + ux * d, from.y + 0.12, from.z + uz * d, 1, 0.0, 0.0, 0.0, 0.0);
        }
    }

    /** Is {@code p} inside the cone of a given half angle (degrees) and range, from {@code origin} facing {@code (dirX, dirZ)}? Horizontal geometry only. */
    public static boolean inCone(Vec3 origin, double dirX, double dirZ, Vec3 p, double range, double halfDegrees) {
        double dx = p.x - origin.x, dz = p.z - origin.z;
        double dist = Math.hypot(dx, dz);
        if (dist > range) return false;
        if (dist < 0.8) return true;                                      // inside the boss's own reach
        double len = Math.hypot(dirX, dirZ);
        if (len < 1e-6) return false;
        double cos = (dx * dirX + dz * dirZ) / (dist * len);
        return cos >= Math.cos(Math.toRadians(halfDegrees));
    }

    /** Is there a clear line between two entities' eyes (no block in between)? Used by the Lich's nova: cover works. */
    public static boolean clearLine(ServerLevel l, Entity from, Entity to) {
        Vec3 a = from.getEyePosition(), b = to.getEyePosition();
        return l.clip(new ClipContext(a, b, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, from)).getType() == HitResult.Type.MISS;
    }

    /** The unit horizontal direction from a point to another (a zero vector when they coincide). */
    public static Vec3 dirTo(Vec3 from, Vec3 to) {
        double dx = to.x - from.x, dz = to.z - from.z, len = Math.hypot(dx, dz);
        return len < 1e-6 ? Vec3.ZERO : new Vec3(dx / len, 0, dz / len);
    }
}
