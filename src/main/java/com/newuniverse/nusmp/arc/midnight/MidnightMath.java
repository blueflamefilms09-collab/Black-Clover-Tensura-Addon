package com.newuniverse.nusmp.arc.midnight;

/**
 * Why: every strike of the four Midnight Sun bosses is a shape on the floor (a lane, a cone, a ring, a fan of bolts) that is drawn as a telegraph and
 * then tested against the foes when it lands. The drawing and the testing must use the SAME numbers, or the party is hit where nothing was shown, which is
 * the one unfair thing a boss can do. Keeping the geometry in one pure class with no game types means the two halves call the same function, and a
 * unit test can prove the edges (the end of a lane, the rim of a ring, the corner of a cone) without a running game.
 * <p>
 * All lengths are blocks, all angles radians, all positions horizontal (x, z) unless a name says otherwise; a boss's strike also checks the height
 * window with {@link #withinHeight}.
 */
public final class MidnightMath {
    private MidnightMath() {}

    /** A player's walking speed in blocks per tick (4.3 blocks a second): every mark that gives {@code ticks} of warning must be leavable at a walk. */
    public static final double WALK_BLOCKS_PER_TICK = 0.216;

    /** Shortest distance from the point (px, pz) to the segment from (ax, az) to (bx, bz). A zero-length segment is the distance to its point. */
    public static double distToSegment(double px, double pz, double ax, double az, double bx, double bz) {
        double dx = bx - ax, dz = bz - az;
        double len2 = dx * dx + dz * dz;
        if (len2 < 1e-12) return Math.hypot(px - ax, pz - az);
        double t = clamp(((px - ax) * dx + (pz - az) * dz) / len2, 0, 1);
        return Math.hypot(px - (ax + dx * t), pz - (az + dz * t));
    }

    /** How far along the segment (0 at a, 1 at b) the point's nearest approach lies; used to order the foes a beam meets from the boss outwards. */
    public static double alongSegment(double px, double pz, double ax, double az, double bx, double bz) {
        double dx = bx - ax, dz = bz - az;
        double len2 = dx * dx + dz * dz;
        if (len2 < 1e-12) return 0;
        return clamp(((px - ax) * dx + (pz - az) * dz) / len2, 0, 1);
    }

    /**
     * Is a body at offset (dx, dz) from the cone's tip inside a cone that points along (dirX, dirZ), reaches {@code reach} blocks and opens
     * {@code halfAngle} radians each side? A body's own radius widens the reach and the angle a little, so a foe whose centre is just outside is still
     * struck when the drawn edge touches its body. A body on the tip is always inside.
     */
    public static boolean inCone(double dx, double dz, double dirX, double dirZ, double reach, double halfAngle, double bodyRadius) {
        double d = Math.hypot(dx, dz);
        if (d <= Math.max(0.0, bodyRadius)) return true;
        if (d > reach + bodyRadius) return false;
        double dl = Math.hypot(dirX, dirZ);
        if (dl < 1e-9) return false;
        double cos = (dx * dirX + dz * dirZ) / (d * dl);
        double angle = Math.acos(clamp(cos, -1, 1));
        double widen = Math.asin(clamp(bodyRadius / d, 0, 1));
        return angle <= halfAngle + widen + 1e-9;
    }

    /** Is the horizontal distance from (cx, cz) to (x, z) within {@code radius} plus the body's own radius? */
    public static boolean inRing(double cx, double cz, double x, double z, double radius, double bodyRadius) {
        double dx = x - cx, dz = z - cz;
        double r = radius + Math.max(0, bodyRadius);
        return dx * dx + dz * dz <= r * r;
    }

    /** The vertical window of a strike on the floor: a foe far above or far below the mark is not under it. */
    public static boolean withinHeight(double dy, double below, double above) { return dy >= -below && dy <= above; }

    /**
     * Offsets (radians) of {@code n} bolts spread evenly over {@code totalSpread} and centred on the aim: one bolt is straight ahead, two are at plus
     * and minus half the step, and so on. The sum of the offsets is zero, so a fan never leans to one side.
     */
    public static double[] fan(int n, double totalSpread) {
        int k = Math.max(1, n);
        double[] out = new double[k];
        if (k == 1) return out;
        double step = totalSpread / (k - 1);
        for (int i = 0; i < k; i++) out[i] = (i - (k - 1) / 2.0) * step;
        return out;
    }

    /** {@code n} points (x, z offsets) evenly round a ring of {@code radius}, the first at angle {@code phase}. */
    public static double[][] ring(int n, double radius, double phase) {
        int k = Math.max(1, n);
        double[][] out = new double[k][2];
        for (int i = 0; i < k; i++) {
            double a = phase + i * Math.PI * 2 / k;
            out[i][0] = Math.cos(a) * radius;
            out[i][1] = Math.sin(a) * radius;
        }
        return out;
    }

    /**
     * Is the viewer (looking along (viewX, viewZ)) facing the source at offset (toX, toZ) from the viewer, within {@code maxAngle} radians? A flash only
     * blinds those who look at it. A viewer standing on the source counts as looking at it.
     */
    public static boolean looksToward(double viewX, double viewZ, double toX, double toZ, double maxAngle) {
        double vl = Math.hypot(viewX, viewZ), tl = Math.hypot(toX, toZ);
        if (tl < 1e-6) return true;
        if (vl < 1e-6) return false;
        double cos = (viewX * toX + viewZ * toZ) / (vl * tl);
        return Math.acos(clamp(cos, -1, 1)) <= maxAngle;
    }

    /**
     * The spot {@code dist} blocks behind a body that faces (faceX, faceZ): where a light-speed step lands to strike a foe's back. A body with no facing
     * (a zero vector) is struck from the south, so the answer is always defined.
     */
    public static double[] behind(double x, double z, double faceX, double faceZ, double dist) {
        double l = Math.hypot(faceX, faceZ);
        if (l < 1e-9) return new double[]{x, z + dist};
        return new double[]{x - faceX / l * dist, z - faceZ / l * dist};
    }

    /** Ticks at which {@code count} things happen, the first after {@code first} ticks and each {@code gap} ticks after the one before. */
    public static int[] schedule(int count, int first, int gap) {
        int[] out = new int[Math.max(0, count)];
        for (int i = 0; i < out.length; i++) out[i] = Math.max(0, first) + i * Math.max(0, gap);
        return out;
    }

    /**
     * The share of a blow that gets through a foe's guard: a raised shield halves it, anti-magic in hand cuts a spell to 55%, both together to 30%.
     * {@code spell} says the blow is magic (a claw or a swing is not cut by anti-magic, only by the shield).
     */
    public static double guardFactor(boolean blocking, boolean antiMagic, boolean spell) {
        double f = 1.0;
        if (blocking) f *= 0.5;
        if (antiMagic && spell) f *= blocking ? 0.6 : 0.55;
        return f;
    }

    /** The farthest a foe can walk away from a mark during {@code ticks} of warning, in blocks. A mark of radius r is fair when this is at least r. */
    public static double escapeReach(int ticks) { return ticks * WALK_BLOCKS_PER_TICK; }

    public static double clamp(double v, double lo, double hi) { return v < lo ? lo : v > hi ? hi : v; }

    public static int clamp(int v, int lo, int hi) { return v < lo ? lo : v > hi ? hi : v; }

    public static double lerp(double a, double b, double t) { return a + (b - a) * clamp(t, 0, 1); }
}
