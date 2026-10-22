package com.newuniverse.nusmp.spiritdark;

/**
 * Why: the destination search of the Shadow Step, pure (no game classes) so every rule is unit tested on a fake grid: the spot must be inside a loaded chunk, inside the world's height, have two
 * free cells for the body, stand on something solid (never over the void or a lava lake), not be a hazard cell (fire, lava, magma, cactus ...), be reachable by a clear line from where the owner
 * stands, and be between {@link SpiritDarkRules#STEP_RANGE_MIN} and {@link SpiritDarkRules#STEP_RANGE_MAX} blocks away. It prefers the farthest spot toward where the owner looks, and among
 * spots it prefers darker ones (light level at or below {@link SpiritDarkRules#DARK_LIGHT_MAX}). The server adds its own last checks (the player's real hit box, the world border) before moving.
 *
 * <p>Cost, bounded: at most {@code FAN.length * DISTANCES.length} = 24 candidate columns, each scanning {@link #SCAN_UP} + {@link #SCAN_DOWN} + 1 cells, so at most about 24 * 8 * 4 probe reads,
 * and at most {@link #MAX_LINE_CHECKS} line-of-sight clips; it runs once per Shadow Step (nine seconds apart per player), never on a tick.</p>
 */
public final class ShadowStep {
    private ShadowStep() {}

    /** What the search may ask of the world (the server backs it with the level; the tests with a grid). All cells are integer block coordinates. */
    public interface Probe {
        /** Is the chunk of this cell loaded (never loads one)? */
        boolean loaded(int x, int y, int z);

        /** Can the body occupy the cell: no collision shape and no fluid that harms (water is allowed, lava is not)? */
        boolean passable(int x, int y, int z);

        /** Does the cell hold something that harms by standing in or on it (fire, lava, magma, cactus, a berry bush, a wither rose ...)? */
        boolean hazard(int x, int y, int z);

        /** Is the cell a solid floor to stand on (a full collision top, not a fluid)? */
        boolean floor(int x, int y, int z);

        /** The light level (0..15) of the cell. */
        int light(int x, int y, int z);

        /** Is the straight line between two points free of blocks that stop movement? */
        boolean lineClear(double x0, double y0, double z0, double x1, double y1, double z1);

        /** The lowest and highest y a body may stand at in this world (the world's own height limits). */
        int minY();

        int maxY();
    }

    /** The result: where to stand (feet), how far, the light there. */
    public record Spot(double x, double y, double z, double distance, int light) {}

    /** Yaw offsets (degrees) of the candidate directions around the look, and the distances tried (blocks, farthest first). */
    public static final double[] FAN = {0.0, 16.0, -16.0, 32.0, -32.0, 48.0, -48.0, 64.0};
    public static final double[] DISTANCES = {14.0, 12.0, 10.0, 8.0, 6.0, 4.0};
    /** How far above and below the expected height a column is scanned for a floor. */
    public static final int SCAN_UP = 3, SCAN_DOWN = 4;
    /** At most this many sight-line clips are made. */
    public static final int MAX_LINE_CHECKS = 5;
    /** Score weights: distance counts 1 per block, a dark spot gains {@link #DARK_BONUS}, every light level costs {@link #LIGHT_COST}, a turned direction costs {@link #TURN_COST} per degree. */
    public static final double DARK_BONUS = 5.0, LIGHT_COST = 0.25, TURN_COST = 0.04;

    private static final Spot[] NONE = new Spot[0];

    /**
     * Picks the destination. {@code (fx, fy, fz)} are the owner's feet, {@code eyeHeight} the eye above the feet, {@code yaw} the look's yaw in degrees (0 faces +z), {@code lookY} the look's
     * vertical component (-1..1; a look up or down tilts the expected height). Returns null when nothing valid exists.
     */
    public static Spot pick(Probe w, double fx, double fy, double fz, double eyeHeight, double yaw, double lookY, double maxRange) {
        double range = Math.max(0.0, Math.min(maxRange, SpiritDarkRules.STEP_RANGE_MAX));
        Spot[] best = new Spot[MAX_LINE_CHECKS];
        double[] bestScore = new double[MAX_LINE_CHECKS];
        int have = 0;
        for (double off : FAN) {
            double a = Math.toRadians(yaw + off), dx = -Math.sin(a), dz = Math.cos(a);
            for (double dist : DISTANCES) {
                if (dist > range || dist < SpiritDarkRules.STEP_RANGE_MIN) continue;
                int x = (int) Math.floor(fx + dx * dist), z = (int) Math.floor(fz + dz * dist);
                int y0 = (int) Math.floor(fy + Math.max(-1.0, Math.min(1.0, lookY)) * dist * 0.5);
                int y = standingY(w, x, y0, z);
                if (y == Integer.MIN_VALUE) continue;
                int light = w.light(x, y, z);
                double real = Math.sqrt((x + 0.5 - fx) * (x + 0.5 - fx) + (y - fy) * (y - fy) + (z + 0.5 - fz) * (z + 0.5 - fz));
                if (real > range + 0.75 || real < SpiritDarkRules.STEP_RANGE_MIN - 0.75) continue;
                double score = real + (light <= SpiritDarkRules.DARK_LIGHT_MAX ? DARK_BONUS : 0.0) - light * LIGHT_COST - Math.abs(off) * TURN_COST;
                Spot s = new Spot(x + 0.5, y, z + 0.5, real, light);
                // keep the best MAX_LINE_CHECKS by score (insertion into a tiny sorted array)
                int at = have;
                while (at > 0 && bestScore[at - 1] < score) at--;
                if (at >= MAX_LINE_CHECKS) continue;
                for (int k = Math.min(have, MAX_LINE_CHECKS - 1); k > at; k--) { best[k] = best[k - 1]; bestScore[k] = bestScore[k - 1]; }
                best[at] = s;
                bestScore[at] = score;
                if (have < MAX_LINE_CHECKS) have++;
            }
        }
        for (int i = 0; i < have; i++) {
            Spot s = best[i];
            if (w.lineClear(fx, fy + eyeHeight, fz, s.x(), s.y() + eyeHeight, s.z())) return s;
        }
        return null;
    }

    /** The y of the lowest free cell of a body standing in column (x, z) near y0, or MIN_VALUE: two free cells over a solid floor, none of the three a hazard, the chunk loaded, inside the height limits. */
    static int standingY(Probe w, int x, int y0, int z) {
        for (int y = y0 + SCAN_UP; y >= y0 - SCAN_DOWN; y--) {
            if (y <= w.minY() + 1 || y + 1 >= w.maxY()) continue;
            if (!w.loaded(x, y, z) || !w.loaded(x, y - 1, z)) continue;
            if (!w.floor(x, y - 1, z) || w.hazard(x, y - 1, z)) continue;
            if (!w.passable(x, y, z) || !w.passable(x, y + 1, z) || w.hazard(x, y, z) || w.hazard(x, y + 1, z)) continue;
            return y;
        }
        return Integer.MIN_VALUE;
    }

    /** Is this destination still acceptable a moment later (used by the self-test and the server's last look): inside the range, free, over a floor, loaded. */
    public static boolean stillValid(Probe w, Spot s, double fx, double fy, double fz) {
        int x = (int) Math.floor(s.x()), y = (int) Math.floor(s.y()), z = (int) Math.floor(s.z());
        double dx = s.x() - fx, dy = s.y() - fy, dz = s.z() - fz, d = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (d > SpiritDarkRules.STEP_RANGE_MAX + 0.75 || y <= w.minY() + 1 || y + 1 >= w.maxY()) return false;
        return w.loaded(x, y, z) && w.floor(x, y - 1, z) && !w.hazard(x, y - 1, z) && w.passable(x, y, z) && w.passable(x, y + 1, z) && !w.hazard(x, y, z) && !w.hazard(x, y + 1, z);
    }
}
