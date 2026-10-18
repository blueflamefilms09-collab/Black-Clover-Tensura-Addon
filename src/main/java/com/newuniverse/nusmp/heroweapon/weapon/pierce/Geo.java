package com.newuniverse.nusmp.heroweapon.weapon.pierce;

/**
 * Why: the Legendary Spear and the Legendary Bow both fight along lines and sweeps (a thrust that runs eight blocks, a fan of arrows, the Minute Hand that
 * turns round its wielder like the hand of a clock), and getting those shapes wrong is invisible until someone plays: an arrow that misses a foe standing in
 * plain view, a sweep that skips a target when the hand steps past it. The maths is therefore kept free of any game type, so it is checked by plain unit tests
 * with the edge cases (a zero-length segment, a bearing that wraps past north, a step across the 360 degree seam). All distances are blocks, all angles
 * degrees, bearings are measured from +x towards +z (the way Minecraft's yaw turns: yaw 0 looks along +z, so a bearing is yaw plus ninety).
 */
public final class Geo {
    private Geo() {}

    /** The closest point of the segment a-b to p, as the share t of the way from a to b (0 at a, 1 at b). A zero-length segment gives 0. */
    public static double segmentParam(double ax, double ay, double az, double bx, double by, double bz, double px, double py, double pz) {
        double vx = bx - ax, vy = by - ay, vz = bz - az;
        double len2 = vx * vx + vy * vy + vz * vz;
        if (len2 < 1.0E-12) return 0.0;
        double t = ((px - ax) * vx + (py - ay) * vy + (pz - az) * vz) / len2;
        return Math.max(0.0, Math.min(1.0, t));
    }

    /** The bearing of the horizontal vector (dx, dz) in degrees, 0 (inclusive) to 360 (exclusive), measured from +x towards +z. The zero vector gives 0. */
    public static double bearing(double dx, double dz) {
        if (dx == 0.0 && dz == 0.0) return 0.0;
        double a = Math.toDegrees(Math.atan2(dz, dx));
        a = a % 360.0;
        return a < 0.0 ? a + 360.0 : a;
    }

    /** The unit horizontal vector (x, z) of a bearing. */
    public static double[] direction(double bearingDeg) {
        double r = Math.toRadians(bearingDeg);
        return new double[]{Math.cos(r), Math.sin(r)};
    }

    /** The signed shortest turn from bearing {@code from} to bearing {@code to}, in (-180, 180]. Positive turns towards +z. */
    public static double turn(double from, double to) {
        double d = (to - from) % 360.0;
        if (d > 180.0) d -= 360.0;
        else if (d <= -180.0) d += 360.0;
        return d;
    }

    /**
     * True when the bearing {@code at} lies on the arc a clock hand swept while it moved from bearing {@code prev} to bearing {@code cur} (the short way,
     * so a step is expected to be well below 180 degrees), widened by {@code half} degrees at both ends: the hand has some width, so a foe just behind
     * or just ahead of the line is still caught. A foe is therefore caught on the step that passes it, and (with a step no wider than the arc) on at least
     * one step of a full turn.
     */
    public static boolean swept(double prev, double cur, double at, double half) {
        double step = turn(prev, cur);
        double rel = turn(prev, at);
        double h = Math.max(0.0, half);
        if (step >= 0.0) return rel >= -h && rel <= step + h;
        return rel <= h && rel >= step - h;
    }

    /** The horizontal vector (x, z) turned by {@code deg} degrees (positive: from +x towards +z). */
    public static double[] rotate(double x, double z, double deg) {
        double a = Math.toRadians(deg), c = Math.cos(a), s = Math.sin(a);
        return new double[]{x * c - z * s, x * s + z * c};
    }

    /**
     * {@code n} angle offsets spread evenly over {@code spread} degrees and centred on 0: the arrows of a fan. One arrow flies straight (offset 0); two
     * arrows fly at plus and minus half the spread.
     */
    public static double[] fan(int n, double spread) {
        int count = Math.max(1, n);
        double[] out = new double[count];
        if (count == 1) return out;
        for (int i = 0; i < count; i++) out[i] = -spread / 2.0 + spread * i / (count - 1);
        return out;
    }

    public static double clamp(double v, double lo, double hi) { return v < lo ? lo : Math.min(v, hi); }
}
