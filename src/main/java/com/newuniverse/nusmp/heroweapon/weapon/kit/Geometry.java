package com.newuniverse.nusmp.heroweapon.weapon.kit;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Staff, the Gauntlet, the Projectile and the Carriage all answer the same few geometric questions (which foes lie along a thrown line, which lie in a
 * cone in front of the wielder, in what order does a ricochet visit its targets, how does a carriage turn towards where its driver looks) and every one of those
 * answers decides who gets hurt, so they live here as plain double arithmetic with no game classes: a test can pin them and a later edit that breaks them fails
 * at once. The game glue (the {@code Foes} and {@code Rays} classes) only turns Minecraft's vectors into the numbers these methods take.
 * <p>
 * Yaw follows Minecraft: yaw 0 looks south (+z), 90 looks west (-x); a positive turn goes from south towards west.
 */
public final class Geometry {
    private Geometry() {}

    /** The parameter t in [0, 1] of the point of the segment a to b that lies nearest to p (0 when the segment is a point). */
    public static double segmentT(double ax, double ay, double az, double bx, double by, double bz, double px, double py, double pz) {
        double dx = bx - ax, dy = by - ay, dz = bz - az;
        double len2 = dx * dx + dy * dy + dz * dz;
        if (len2 < 1.0e-12) return 0.0;
        double t = ((px - ax) * dx + (py - ay) * dy + (pz - az) * dz) / len2;
        return t < 0.0 ? 0.0 : Math.min(t, 1.0);
    }

    /** The squared distance from p to the segment a to b. */
    public static double distToSegmentSq(double ax, double ay, double az, double bx, double by, double bz, double px, double py, double pz) {
        double t = segmentT(ax, ay, az, bx, by, bz, px, py, pz);
        double cx = ax + (bx - ax) * t - px, cy = ay + (by - ay) * t - py, cz = az + (bz - az) * t - pz;
        return cx * cx + cy * cy + cz * cz;
    }

    /** True when the direction d lies within {@code halfAngleDeg} of the direction l (neither needs to be a unit vector). A zero d (on the apex) counts as inside. */
    public static boolean inCone(double dx, double dy, double dz, double lx, double ly, double lz, double halfAngleDeg) {
        double dl = Math.sqrt(dx * dx + dy * dy + dz * dz), ll = Math.sqrt(lx * lx + ly * ly + lz * lz);
        if (dl < 1.0e-9) return true;
        if (ll < 1.0e-9) return false;
        double cos = (dx * lx + dy * ly + dz * lz) / (dl * ll);
        return cos >= Math.cos(Math.toRadians(halfAngleDeg));
    }

    /** Turns the horizontal vector (x, z) by {@code degrees} of yaw (positive: from south towards west) and returns {x', z'}. */
    public static double[] turnYaw(double x, double z, double degrees) {
        double r = Math.toRadians(degrees), c = Math.cos(r), s = Math.sin(r);
        return new double[]{x * c - z * s, x * s + z * c};
    }

    /** The yaw in degrees (Minecraft's sense) of the horizontal direction (dx, dz); 0 for a zero vector. */
    public static double yawOf(double dx, double dz) {
        if (dx * dx + dz * dz < 1.0e-12) return 0.0;
        return Math.toDegrees(-Math.atan2(dx, dz));
    }

    /** An angle brought into [-180, 180). */
    public static double wrapDegrees(double a) {
        double w = a % 360.0;
        if (w >= 180.0) w -= 360.0;
        if (w < -180.0) w += 360.0;
        return w;
    }

    /** Moves {@code current} towards {@code target} (the short way round) by at most {@code maxStep} degrees. */
    public static double approachAngle(double current, double target, double maxStep) {
        double d = wrapDegrees(target - current), step = Math.abs(maxStep);
        if (d > step) d = step;
        if (d < -step) d = -step;
        return current + d;
    }

    /** {@code count} angles in degrees spread evenly over {@code totalDeg} and centred on 0 (one angle: just 0). Used for a fan of knives. */
    public static double[] fan(int count, double totalDeg) {
        int n = Math.max(1, count);
        double[] out = new double[n];
        if (n == 1) return out;
        double step = totalDeg / (n - 1);
        for (int i = 0; i < n; i++) out[i] = -totalDeg / 2.0 + i * step;
        return out;
    }

    /**
     * The order in which a thrown blade that jumps from foe to foe visits {@code points}: always the nearest one not yet visited, from the start point, and only
     * while the next one lies within {@code maxJump} of the last; at most {@code maxVisits} indices. A point never appears twice.
     */
    public static List<Integer> chain(double sx, double sy, double sz, double[][] points, int maxVisits, double maxJump) {
        List<Integer> order = new ArrayList<>();
        boolean[] used = new boolean[points.length];
        double x = sx, y = sy, z = sz;
        double limit2 = maxJump * maxJump;
        while (order.size() < maxVisits) {
            int best = -1;
            double bestD = Double.MAX_VALUE;
            for (int i = 0; i < points.length; i++) {
                if (used[i]) continue;
                double dx = points[i][0] - x, dy = points[i][1] - y, dz = points[i][2] - z, d = dx * dx + dy * dy + dz * dz;
                if (d < bestD) {
                    bestD = d;
                    best = i;
                }
            }
            if (best < 0 || bestD > limit2) break;
            used[best] = true;
            order.add(best);
            x = points[best][0];
            y = points[best][1];
            z = points[best][2];
        }
        return order;
    }

    /** Clamps v into [lo, hi]. */
    public static double clamp(double v, double lo, double hi) { return v < lo ? lo : Math.min(v, hi); }
}
