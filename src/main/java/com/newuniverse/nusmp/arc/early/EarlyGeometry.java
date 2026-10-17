package com.newuniverse.nusmp.arc.early;

/**
 * Why: the early bosses all aim the same few shapes at the floor (a cone in front of a blade, a corridor along a chain or a lance, a ring that runs out
 * from a stomp, a fan of bolts, a triangle of fangs round a target), and every one of those is a few lines of arithmetic that is easy to get subtly wrong
 * (a corridor that ends where the segment ends, a cone that wraps at 180 degrees, a ring wave that skips a fast foe). They live here, pure, so one test
 * pins them for all five fights, and the game-side helpers ({@link EarlyMoves}) only feed them positions. Everything works on the horizontal plane
 * (x, z); the callers handle height.
 */
public final class EarlyGeometry {
    private EarlyGeometry() {}

    /** A foe whose feet are this far above the floor has jumped a low ring wave (the same figure as the Zogratis ring waves). */
    public static final double JUMP_HEIGHT = 0.75;
    /** Blocks a ring wave travels per tick, the radius it starts at, and how thick it is (a foe within this of its edge is hit). */
    public static final double WAVE_SPEED = 1.1, WAVE_START = 0.5, WAVE_THICK = 1.3;

    public static double clamp01(double v) { return v < 0 ? 0 : v > 1 ? 1 : v; }

    /** Position along the segment a-b (0 at a, 1 at b) of the point nearest to (px, pz); 0 for a segment with no length. */
    public static double segmentT(double px, double pz, double ax, double az, double bx, double bz) {
        double dx = bx - ax, dz = bz - az, len2 = dx * dx + dz * dz;
        if (len2 < 1e-9) return 0;
        return clamp01(((px - ax) * dx + (pz - az) * dz) / len2);
    }

    /** Horizontal distance from the point to the segment a-b (to the nearer end when the point is past it). */
    public static double segmentDistance(double px, double pz, double ax, double az, double bx, double bz) {
        double t = segmentT(px, pz, ax, az, bx, bz);
        return Math.hypot(px - (ax + t * (bx - ax)), pz - (az + t * (bz - az)));
    }

    /** Is the point inside the corridor of half width {@code halfWidth} round the segment a-b (the ends are round)? */
    public static boolean inCorridor(double px, double pz, double ax, double az, double bx, double bz, double halfWidth) {
        return segmentDistance(px, pz, ax, az, bx, bz) <= halfWidth;
    }

    /** The angle in radians (0 to pi) between the facing (fx, fz) and the direction (dx, dz); pi for a direction with no length. */
    public static double angleBetween(double fx, double fz, double dx, double dz) {
        double fl = Math.hypot(fx, fz), dl = Math.hypot(dx, dz);
        if (fl < 1e-9 || dl < 1e-9) return Math.PI;
        double cos = (fx * dx + fz * dz) / (fl * dl);
        return Math.acos(Math.max(-1, Math.min(1, cos)));
    }

    /** Is the point inside the cone from (ox, oz) facing (fx, fz), {@code reach} long and {@code halfAngle} radians either side? A point on the origin is in. */
    public static boolean inCone(double ox, double oz, double fx, double fz, double reach, double halfAngle, double px, double pz) {
        double dx = px - ox, dz = pz - oz;
        if (Math.hypot(dx, dz) > reach) return false;
        if (Math.hypot(dx, dz) < 1e-6) return true;
        return angleBetween(fx, fz, dx, dz) <= halfAngle;
    }

    /** The vector (x, z) turned by {@code angle} radians (positive turns from +x towards +z). */
    public static double[] rotate(double x, double z, double angle) {
        double c = Math.cos(angle), s = Math.sin(angle);
        return new double[]{x * c - z * s, x * s + z * c};
    }

    /** {@code n} angles centred on 0 and spread over {@code spread} radians in all (one angle is exactly 0 when n is odd). */
    public static double[] fan(int n, double spread) {
        int count = Math.max(1, n);
        double[] out = new double[count];
        if (count == 1) return out;
        for (int i = 0; i < count; i++) out[i] = -spread / 2 + spread * i / (count - 1);
        return out;
    }

    /** The corners of a regular polygon round (cx, cz): {@code sides} points {@code radius} away, the first at angle {@code phase}. */
    public static double[][] polygon(double cx, double cz, double radius, int sides, double phase) {
        int n = Math.max(1, sides);
        double[][] out = new double[n][2];
        for (int i = 0; i < n; i++) {
            double a = phase + i * Math.PI * 2 / n;
            out[i][0] = cx + Math.cos(a) * radius;
            out[i][1] = cz + Math.sin(a) * radius;
        }
        return out;
    }

    /** The radius of a ring wave {@code age} ticks after it left the boss. */
    public static double waveRadius(int age) { return WAVE_START + WAVE_SPEED * Math.max(0, age); }

    /** Ticks a ring wave needs to run out to {@code reach} (and a few more so the last foes are not missed). */
    public static int waveTicks(double reach) { return (int) Math.ceil((Math.max(1, reach) - WAVE_START) / WAVE_SPEED) + 3; }

    /** Is a foe at {@code dist} from the middle inside a wave whose edge is at {@code wave} (thick {@value #WAVE_THICK}, so a fast foe is not skipped)? */
    public static boolean ringHits(double dist, double wave) { return dist <= wave + 0.4 && dist >= wave - WAVE_THICK; }

    /** Has this foe jumped the wave (feet at least {@value #JUMP_HEIGHT} blocks off the floor)? */
    public static boolean jumpsRing(double feetAboveFloor) { return feetAboveFloor >= JUMP_HEIGHT; }

    /** The share of a blast that reaches a foe {@code dist} away: 1 up to {@code inner}, falling in a straight line to {@code floor} at {@code outer}, 0 beyond. */
    public static double falloff(double dist, double inner, double outer, double floor) {
        if (dist > outer) return 0;
        if (dist <= inner || outer <= inner) return 1;
        return 1 - (1 - floor) * (dist - inner) / (outer - inner);
    }
}
