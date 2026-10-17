package com.newuniverse.nusmp.arc.sds.kit;

/**
 * Why: the four Sins (Merlin, Gowther, King, Diane) all aim things on the flat ground plane: a fan of bolts, a cone in front of a hand, a lane along a
 * spear, a ring of marks round a target, an arc that a shield covers. The arithmetic is small but easy to get subtly wrong (angle wrap-around, a segment
 * shorter than a block), so it lives here once, pure and tested, and the game glue only turns entities into numbers. Angles are radians measured with
 * {@code atan2(dz, dx)} (the same convention for every method); distances are blocks.
 */
public final class FourSinsMath {
    private FourSinsMath() {}

    /** The bearing of the vector (dx, dz): 0 along +x, increasing towards +z. */
    public static double bearing(double dx, double dz) { return Math.atan2(dz, dx); }

    /** The smallest signed difference {@code a - b} wrapped into [-pi, pi]. */
    public static double angleDiff(double a, double b) {
        double d = (a - b) % (Math.PI * 2);
        if (d > Math.PI) d -= Math.PI * 2;
        else if (d < -Math.PI) d += Math.PI * 2;
        return d;
    }

    /** True when {@code bearing} lies within {@code halfArc} of {@code facing} (both radians). */
    public static boolean inArc(double facing, double bearing, double halfArc) { return Math.abs(angleDiff(bearing, facing)) <= halfArc; }

    /**
     * The angles of {@code count} rays fanned over {@code spread} radians in total, centred on zero. One ray is straight ahead; several are evenly
     * spaced with the outer two on the edges of the spread.
     */
    public static double[] fan(int count, double spread) {
        int n = Math.max(1, count);
        double[] out = new double[n];
        if (n == 1) return out;
        for (int i = 0; i < n; i++) out[i] = -spread / 2 + spread * i / (n - 1);
        return out;
    }

    /** Distance from the point (px, pz) to the segment (ax, az)-(bx, bz), on the ground plane. */
    public static double distToSegment(double px, double pz, double ax, double az, double bx, double bz) {
        double dx = bx - ax, dz = bz - az;
        double len2 = dx * dx + dz * dz;
        double t = len2 < 1e-9 ? 0 : ((px - ax) * dx + (pz - az) * dz) / len2;
        t = Math.max(0, Math.min(1, t));
        return Math.hypot(px - (ax + dx * t), pz - (az + dz * t));
    }

    /** Distance from the point P to the segment A-B in three dimensions (a spear thrown downwards from the air is a lane in 3D, not on the ground plane). */
    public static double distToSegment3(double px, double py, double pz, double ax, double ay, double az, double bx, double by, double bz) {
        double t = along3(px, py, pz, ax, ay, az, bx, by, bz);
        return Math.sqrt(sq(px - (ax + (bx - ax) * t)) + sq(py - (ay + (by - ay) * t)) + sq(pz - (az + (bz - az) * t)));
    }

    /** How far along the segment A to B (0 at A, 1 at B, clamped) the foot of the perpendicular from P falls, in three dimensions. */
    public static double along3(double px, double py, double pz, double ax, double ay, double az, double bx, double by, double bz) {
        double dx = bx - ax, dy = by - ay, dz = bz - az;
        double len2 = dx * dx + dy * dy + dz * dz;
        if (len2 < 1e-9) return 0;
        return Math.max(0, Math.min(1, ((px - ax) * dx + (py - ay) * dy + (pz - az) * dz) / len2));
    }

    private static double sq(double v) { return v * v; }

    /** How far along the segment A to B (0 at A, 1 at B, clamped) the foot of the perpendicular from P falls. */
    public static double along(double px, double pz, double ax, double az, double bx, double bz) {
        double dx = bx - ax, dz = bz - az;
        double len2 = dx * dx + dz * dz;
        if (len2 < 1e-9) return 0;
        return Math.max(0, Math.min(1, ((px - ax) * dx + (pz - az) * dz) / len2));
    }

    /**
     * True when P is inside the cone with its tip at (ox, oz), pointing along (dirX, dirZ), reaching {@code range} blocks and opening {@code halfAngle}
     * radians to each side. A point on the tip counts as inside.
     */
    public static boolean inCone(double ox, double oz, double dirX, double dirZ, double px, double pz, double range, double halfAngle) {
        double dx = px - ox, dz = pz - oz;
        double d = Math.hypot(dx, dz);
        if (d > range) return false;
        if (d < 1e-6) return true;
        return inArc(bearing(dirX, dirZ), bearing(dx, dz), halfAngle);
    }

    /** The points of a ring of {@code count} marks of radius {@code radius} round (cx, cz), starting at angle {@code phase}: {x, z} pairs. */
    public static double[][] ring(int count, double cx, double cz, double radius, double phase) {
        int n = Math.max(1, count);
        double[][] out = new double[n][2];
        for (int i = 0; i < n; i++) {
            double a = phase + Math.PI * 2 * i / n;
            out[i][0] = cx + Math.cos(a) * radius;
            out[i][1] = cz + Math.sin(a) * radius;
        }
        return out;
    }

    public static double clamp(double v, double lo, double hi) { return v < lo ? lo : Math.min(v, hi); }

    public static float clamp(float v, float lo, float hi) { return v < lo ? lo : Math.min(v, hi); }

    public static double lerp(double a, double b, double t) { return a + (b - a) * clamp(t, 0, 1); }

    /**
     * Marks for a volley of ground strikes: the first on the primary target, then one on each other foe, then the rest on a ring round the primary,
     * never closer than {@code spacing} to a mark already chosen (a candidate that would stack is nudged round the ring until it fits, or dropped).
     *
     * @param primary the primary target {x, z}
     * @param others  the other foes {x, z}
     * @param count   how many marks are wanted (at least 1)
     * @param ringRadius radius of the ring round the primary for the spare marks
     * @param spacing nearest two marks may be
     * @param phase   starting angle of the ring (a random number for variety, a constant for a test)
     */
    public static double[][] spreadMarks(double[] primary, double[][] others, int count, double ringRadius, double spacing, double phase) {
        java.util.List<double[]> out = new java.util.ArrayList<>();
        out.add(new double[]{primary[0], primary[1]});
        for (double[] o : others) {
            if (out.size() >= count) break;
            if (clear(out, o[0], o[1], spacing)) out.add(new double[]{o[0], o[1]});
        }
        int guard = 0;
        while (out.size() < count && guard++ < count * 12) {
            double a = phase + guard * 2.399963;               // the golden angle: successive marks never line up
            double r = ringRadius * (0.55 + 0.45 * ((guard % 3) / 2.0));
            double x = primary[0] + Math.cos(a) * r, z = primary[1] + Math.sin(a) * r;
            if (clear(out, x, z, spacing)) out.add(new double[]{x, z});
        }
        return out.toArray(new double[0][]);
    }

    private static boolean clear(java.util.List<double[]> marks, double x, double z, double spacing) {
        for (double[] m : marks) if (Math.hypot(m[0] - x, m[1] - z) < spacing) return false;
        return true;
    }
}
