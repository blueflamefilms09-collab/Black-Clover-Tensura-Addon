package com.newuniverse.nusmp.heroweapon.weapon.glassworld;

/**
 * Why: a dash, a pull and a cone are plain arithmetic, and getting them wrong is the kind of mistake nobody can see until a player flies into the sky or is
 * pulled the wrong way. They live here, with no game imports, so they are tested with numbers: the speed of a dash that should cover a distance, the pitch that
 * must not launch the wielder, the stop short of a wall, the pull toward a point that must never overshoot, the test of a point against a cone. The game glue
 * ({@link GlassKit}) feeds in the vectors and applies the results.
 */
public final class Geo {
    private Geo() {}

    /** A velocity in blocks per tick. */
    public record Impulse(double x, double y, double z) {
        public double horizontal() { return Math.sqrt(x * x + z * z); }
    }

    /**
     * The velocity of a dash along a look direction: a fixed horizontal {@code speed}, the climb or fall of the look clamped to {@code maxUp} and
     * {@code maxDown} (so looking at the sky does not launch the wielder), and a small {@code lift} added on the ground so friction does not eat the dash.
     * Looking straight up or down gives no horizontal part.
     */
    public static Impulse dash(double lookX, double lookY, double lookZ, double speed, double maxUp, double maxDown, double lift, boolean onGround) {
        double h = Math.sqrt(lookX * lookX + lookZ * lookZ);
        double vy = clamp(lookY * speed, -Math.abs(maxDown), Math.abs(maxUp)) + (onGround ? lift : 0.0);
        if (h < 1e-6) return new Impulse(0.0, vy, 0.0);
        return new Impulse(lookX / h * speed, vy, lookZ / h * speed);
    }

    /** The horizontal speed that carries a dash about {@code distance} blocks (air drag 0.91 per tick), kept between 0.2 and 1.4 blocks per tick. */
    public static double dashSpeedFor(double distance) {
        return clamp(0.16 * distance, 0.2, 1.4);
    }

    /** How far a dash of {@code wanted} blocks may go when the first block is {@code wallDistance} away: it stops {@code margin} short of the wall, never below 0. */
    public static double stopShort(double wanted, double wallDistance, double margin) {
        return Math.max(0.0, Math.min(wanted, wallDistance - margin));
    }

    /**
     * The velocity that pulls something from where it is to a point: {@code (dx, dy, dz)} is the vector from the thing to the point; the speed is
     * {@code gain} times the distance, at most {@code maxSpeed}, so a near thing is drawn gently and the pull never overshoots the point by much. Zero when
     * it is already there (within 0.3 blocks).
     */
    public static Impulse pull(double dx, double dy, double dz, double gain, double maxSpeed) {
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (d < 0.3) return new Impulse(0.0, 0.0, 0.0);
        double v = Math.min(Math.abs(maxSpeed), d * gain);
        return new Impulse(dx / d * v, dy / d * v, dz / d * v);
    }

    /** The cosine of the half angle of a cone given in degrees. */
    public static double cosHalf(double halfAngleDegrees) {
        return Math.cos(Math.toRadians(clamp(halfAngleDegrees, 0.0, 180.0)));
    }

    /**
     * True when the point at {@code (dx, dy, dz)} from the apex lies inside the cone around {@code (lx, ly, lz)} whose half angle has the cosine
     * {@code cosHalf}. The apex itself counts as inside.
     */
    public static boolean inCone(double dx, double dy, double dz, double lx, double ly, double lz, double cosHalf) {
        double dl = Math.sqrt(dx * dx + dy * dy + dz * dz), ll = Math.sqrt(lx * lx + ly * ly + lz * lz);
        if (dl < 1e-6) return true;
        if (ll < 1e-6) return false;
        return (dx * lx + dy * ly + dz * lz) / (dl * ll) >= cosHalf;
    }

    /** The horizontal unit vector pointing from {@code (fromX, fromZ)} away from {@code (otherX, otherZ)}, or {@code (fallbackX, fallbackZ)} (normalised) when they coincide. */
    public static double[] awayFrom(double fromX, double fromZ, double otherX, double otherZ, double fallbackX, double fallbackZ) {
        double ax = fromX - otherX, az = fromZ - otherZ;
        double l = Math.sqrt(ax * ax + az * az);
        if (l < 1e-6) {
            double fl = Math.sqrt(fallbackX * fallbackX + fallbackZ * fallbackZ);
            if (fl < 1e-6) return new double[] {0.0, 1.0};
            return new double[] {fallbackX / fl, fallbackZ / fl};
        }
        return new double[] {ax / l, az / l};
    }

    private static double clamp(double v, double lo, double hi) { return Math.max(lo, Math.min(hi, v)); }
}
