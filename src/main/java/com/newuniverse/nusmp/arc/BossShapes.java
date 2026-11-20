package com.newuniverse.nusmp.arc;

/**
 * Why: the geometry of the redesigned Goku and Sinbad abilities (docs/boss_redesign_goku_sinbad.md) as pure functions (no game class, so the unit test and the preview script read the very same
 * numbers): rings, spirals, orbits, the six-sector sigil and the out-and-back path of a thrown disc. Every function returns a flat array of at most {@link #MAX_POINTS} points (x, y, z triples, relative
 * to the centre), so whatever a move asks for, the particle packets it can cause are bounded before any server code runs.
 */
public final class BossShapes {
    private BossShapes() {}

    /** The most points any shape returns. */
    public static final int MAX_POINTS = 32;

    private static int clamp(int n) { return Math.max(1, Math.min(MAX_POINTS, n)); }

    /** {@code n} points of a flat ring of {@code radius} at height {@code y}, turned by {@code phase} radians. */
    public static double[] ring(double radius, double y, int n, double phase) {
        n = clamp(n);
        double[] out = new double[n * 3];
        for (int i = 0; i < n; i++) {
            double a = phase + i * Math.PI * 2.0 / n;
            out[i * 3] = Math.cos(a) * radius;
            out[i * 3 + 1] = y;
            out[i * 3 + 2] = Math.sin(a) * radius;
        }
        return out;
    }

    /** A spiral from (r0, y0) to (r1, y1) with {@code turns} turns: the thread of a charge, an energy column. */
    public static double[] spiral(double r0, double r1, double y0, double y1, int n, double turns, double phase) {
        n = clamp(n);
        double[] out = new double[n * 3];
        for (int i = 0; i < n; i++) {
            double k = n == 1 ? 0.0 : i / (double) (n - 1);
            double a = phase + k * turns * Math.PI * 2.0;
            double r = r0 + (r1 - r0) * k;
            out[i * 3] = Math.cos(a) * r;
            out[i * 3 + 1] = y0 + (y1 - y0) * k;
            out[i * 3 + 2] = Math.sin(a) * r;
        }
        return out;
    }

    /** Point {@code i} of {@code n} orbiting motes round the centre, each bobbing a little on its own phase. */
    public static double[] orbit(int i, int n, double radius, double y, double phase, double bob) {
        double a = phase + i * Math.PI * 2.0 / Math.max(1, n);
        return new double[]{Math.cos(a) * radius, y + Math.sin(phase * 2.0 + i) * bob, Math.sin(a) * radius};
    }

    /** The sector (0 .. sectors-1, counted counter-clockwise from +x turned by {@code phase}) of the offset (dx, dz) from the centre of a sigil. */
    public static int sector(double dx, double dz, int sectors, double phase) {
        sectors = Math.max(1, sectors);
        double a = Math.atan2(dz, dx) - phase;
        double t = a / (Math.PI * 2.0);
        t -= Math.floor(t);
        return Math.min(sectors - 1, (int) (t * sectors));
    }

    /** Whether a sector is one of the lit (dangerous) ones: every second sector, starting with {@code pattern} 0 or 1. */
    public static boolean lit(int sector, int pattern) { return ((sector + pattern) & 1) == 0; }

    /** Whether a point at (dx, dz) from the sigil centre is hit: inside the radius and in a lit sector. */
    public static boolean sigilHit(double dx, double dz, double radius, int sectors, double phase, int pattern) {
        if (dx * dx + dz * dz > radius * radius) return false;
        return lit(sector(dx, dz, sectors, phase), pattern);
    }

    /** {@code n} points filling one sector of a sigil: rows from 25% to 95% of the radius, spread across the sector's angle. */
    public static double[] sectorFill(double radius, int sectors, int sector, double phase, int n, double y) {
        n = clamp(n);
        sectors = Math.max(1, sectors);
        double a0 = phase + sector * Math.PI * 2.0 / sectors, span = Math.PI * 2.0 / sectors;
        int rows = Math.max(1, (int) Math.round(Math.sqrt(n / 2.0)));
        double[] out = new double[n * 3];
        for (int i = 0; i < n; i++) {
            int row = i % rows;
            double r = radius * (0.25 + 0.70 * (rows == 1 ? 1.0 : row / (double) (rows - 1)));
            double k = ((i / rows) + 0.5) / Math.ceil(n / (double) rows);
            double a = a0 + span * Math.min(0.95, Math.max(0.05, k));
            out[i * 3] = Math.cos(a) * r;
            out[i * 3 + 1] = y;
            out[i * 3 + 2] = Math.sin(a) * r;
        }
        return out;
    }

    /** The point on the rim at the middle of a sector (relative), for its marker column. */
    public static double[] sectorMid(double radius, int sectors, int sector, double phase) {
        double a = phase + (sector + 0.5) * Math.PI * 2.0 / Math.max(1, sectors);
        return new double[]{Math.cos(a) * radius, 0.0, Math.sin(a) * radius};
    }

    /** The distance of a thrown disc from its thrower after {@code tick} ticks: out to {@code reach} in {@code outTicks}, then back to 0 in the same time. 0 before the throw and after the return. */
    public static double discDistance(double reach, int tick, int outTicks) {
        if (tick <= 0 || outTicks <= 0 || tick >= outTicks * 2) return 0.0;
        double k = tick <= outTicks ? tick / (double) outTicks : (outTicks * 2 - tick) / (double) outTicks;
        double ease = 1.0 - (1.0 - k) * (1.0 - k);     // fast out, slowing at the far end
        return reach * ease;
    }

    /** The {@code n} points of a flat spinning disc of {@code radius} (a ring plus the hub), turned by {@code spin}. */
    public static double[] disc(double radius, double y, int n, double spin) {
        n = clamp(n);
        double[] out = new double[n * 3];
        int rim = n - 1;
        for (int i = 0; i < rim; i++) {
            double a = spin + i * Math.PI * 2.0 / rim;
            out[i * 3] = Math.cos(a) * radius;
            out[i * 3 + 1] = y;
            out[i * 3 + 2] = Math.sin(a) * radius;
        }
        out[rim * 3] = 0.0;
        out[rim * 3 + 1] = y;
        out[rim * 3 + 2] = 0.0;
        return out;
    }

    /** Whether a viewer looking along (vx, vy, vz) faces the offset (dx, dy, dz) to a source: the cosine of the angle between them is above {@code minCos}. A zero offset counts as facing. */
    public static boolean facing(double vx, double vy, double vz, double dx, double dy, double dz, double minCos) {
        double dl = Math.sqrt(dx * dx + dy * dy + dz * dz), vl = Math.sqrt(vx * vx + vy * vy + vz * vz);
        if (dl < 1e-6 || vl < 1e-6) return true;
        return (vx * dx + vy * dy + vz * dz) / (dl * vl) > minCos;
    }

    /** The {@code n} pillar angles (radians) of a cage round a centre, the first at {@code phase}. */
    public static double pillarAngle(int i, int n, double phase) { return phase + i * Math.PI * 2.0 / Math.max(1, n); }

    /** The size of the cage's afterimage ring at a step of the telegraph: grows from 0.25 to 1 over the wind-up. */
    public static double grow(int elapsed, int total) { return 0.25 + 0.75 * Math.max(0.0, Math.min(1.0, elapsed / (double) Math.max(1, total))); }
}
