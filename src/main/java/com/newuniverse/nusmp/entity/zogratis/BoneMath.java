package com.newuniverse.nusmp.entity.zogratis;

import java.util.ArrayList;
import java.util.List;

/**
 * The arithmetic behind Zenon's Bone Magic (pure, no Minecraft types, unit tested): where a spear grows out of his body, how a volley
 * fans out, how many there are at each power, the cone of a bone blade, the plan of a bone garden and the cells of its growths, and
 * the size of the bone armour. {@code ZenonMoves} applies the numbers to the world; nothing here knows what a block or an entity is.
 */
public final class BoneMath {
    private BoneMath() {}

    // ---------------------------------------------------------------- spears
    public static final int MAX_SPEARS = 12, GROW_TICKS = 5, SPEAR_STAGGER = 2, SPEAR_LIFE = 34, MAX_HITS_PER_VICTIM = 3;
    public static final double SPEAR_SPEED = 1.6, SPEAR_REACH = 0.45, MAX_FAN = 0.42;

    /** Spears in a volley for a power with 'slots' casts: 3 at the opening, then 5, 7, 9 (never over {@link #MAX_SPEARS}). */
    public static int spearCount(int slots) { return Math.max(1, Math.min(MAX_SPEARS, 3 + 2 * (Math.max(1, slots) - 1))); }

    /**
     * Where spear 'i' of 'n' grows, relative to the boss's feet, as {x, y, z}: an arch behind the shoulders (the middle spears highest
     * and furthest back), mirrored left and right of where the boss faces ('yawDeg', Minecraft's yaw).
     */
    public static double[] growthPoint(int i, int n, double yawDeg) {
        int count = Math.max(1, n);
        double u = count == 1 ? 0 : Math.max(0, Math.min(count - 1, i)) / (double) (count - 1) * 2 - 1;      // -1 .. 1 across the arch
        double lateral = u * 1.15, forward = -0.35 - 0.25 * (1 - u * u), up = 1.55 + 0.45 * (1 - u * u);
        double yaw = Math.toRadians(yawDeg);
        double fx = -Math.sin(yaw), fz = Math.cos(yaw), rx = Math.cos(yaw), rz = Math.sin(yaw);
        return new double[]{rx * lateral + fx * forward, up, rz * lateral + fz * forward};
    }

    /** The yaw offset in radians of spear 'i' of 'n' from the aim: a narrow fan (at most {@link #MAX_FAN} across), so a volley is not one line. */
    public static double spread(int i, int n) {
        if (n <= 1) return 0;
        double total = Math.min(MAX_FAN, 0.06 * (n - 1));
        return (Math.max(0, Math.min(n - 1, i)) / (double) (n - 1) - 0.5) * total;
    }

    public static int flightTicks(double distance, double speed) { return (int) Math.max(1, Math.ceil(Math.max(0, distance) / Math.max(0.1, speed))); }

    // ---------------------------------------------------------------- blades
    public static final double BLADE_REACH = 4.2, BLADE_HALF_ARC = Math.toRadians(60), DASH_STOP = 2.4, BODY = 0.6;
    public static final int BLADE_GAP = 6;

    /** Slashes of the bone blade combo: 3 at the opening, 4 and 5 at the higher powers. */
    public static int bladeSlashes(int slots) { return 3 + Math.max(0, Math.min(2, slots - 2)); }

    /** Is a foe at (px, pz) inside a slash from (ox, oz) along (dirX, dirZ): within 'reach' and within 'halfArc' of the direction? Right on top of the origin always is. */
    public static boolean inCone(double ox, double oz, double dirX, double dirZ, double px, double pz, double reach, double halfArc) {
        double dx = px - ox, dz = pz - oz, d = Math.sqrt(dx * dx + dz * dz);
        if (d > reach) return false;
        if (d < BODY) return true;
        double len = Math.sqrt(dirX * dirX + dirZ * dirZ);
        if (len < 1e-9) return false;
        double cos = (dx * dirX + dz * dirZ) / (d * len);
        return cos >= Math.cos(Math.max(0, Math.min(Math.PI, halfArc)));
    }

    // ---------------------------------------------------------------- the garden
    public static final double GARDEN_RADIUS = 2.3, GROWTH_RING = 2.9;
    public static final int MAX_SPOTS = 9, GROWTH_COLUMNS = 6, GROWTH_LIFE = 160, GROWTH_CELL_CAP = 160;

    /** Spots of a bone garden for a power with 'slots' casts: 5 at the opening, up to 8. */
    public static int gardenCount(int slots) { return Math.max(1, Math.min(MAX_SPOTS, 4 + Math.max(1, slots))); }

    /**
     * Where the bone erupts: the first spot on the primary target at (tx, tz), then on the other foes ('foes' = {x, z} pairs in priority
     * order, skipped when closer than 1.6 radii to a spot already placed), then the rest on a ring round the target at 2.6 radii (turned by
     * 'phase'), then on an outer ring. No two spots are closer than 1.6 radii, so no foe is under two at once; at most 'n' (never over
     * {@link #MAX_SPOTS}). Each result is {x, z}.
     */
    public static List<double[]> gardenPlan(int n, double tx, double tz, double[][] foes, double radius, double phase) {
        int want = Math.max(0, Math.min(MAX_SPOTS, n));
        List<double[]> out = new ArrayList<>();
        if (want == 0) return out;
        double apart = radius * 1.6;
        out.add(new double[]{tx, tz});
        if (foes != null) {
            for (double[] f : foes) {
                if (out.size() >= want) break;
                if (f == null || f.length < 2 || near(out, f[0], f[1], apart)) continue;
                out.add(new double[]{f[0], f[1]});
            }
        }
        for (int ring = 1; ring <= 2 && out.size() < want; ring++) {
            double r = radius * (ring == 1 ? 2.6 : 4.4) + 0.5;
            int slots = ring == 1 ? 6 : 10;
            for (int k = 0; k < slots && out.size() < want; k++) {
                double a = phase + (k + 0.5 * ring) * Math.PI * 2 / slots;
                double x = tx + Math.cos(a) * r, z = tz + Math.sin(a) * r;
                if (!near(out, x, z, apart)) out.add(new double[]{x, z});
            }
        }
        return out;
    }

    private static boolean near(List<double[]> marks, double x, double z, double minDist) {
        for (double[] m : marks) {
            double dx = m[0] - x, dz = m[1] - z;
            if (dx * dx + dz * dz < minDist * minDist) return true;
        }
        return false;
    }

    /** The {x, z} of the bone columns that grow round a garden spot at (sx, sz): {@link #GROWTH_COLUMNS} of them on a ring of {@link #GROWTH_RING} blocks, turned by 'phase'. */
    public static List<double[]> columnBases(double sx, double sz, double phase) {
        List<double[]> out = new ArrayList<>();
        for (int k = 0; k < GROWTH_COLUMNS; k++) {
            double a = phase + k * Math.PI * 2 / GROWTH_COLUMNS;
            out.add(new double[]{sx + Math.cos(a) * GROWTH_RING, sz + Math.sin(a) * GROWTH_RING});
        }
        return out;
    }

    /** Height in blocks of the k-th column of a spot: 1, 2, 3, 1, 2, 3. */
    public static int columnHeight(int k) { return 1 + Math.floorMod(k, 3); }

    /** Blocks one garden spot's columns hold in all (12). */
    public static int cellsPerSpot() {
        int sum = 0;
        for (int k = 0; k < GROWTH_COLUMNS; k++) sum += columnHeight(k);
        return sum;
    }

    // ---------------------------------------------------------------- the armour
    public static final int SHELL_TICKS = 160;
    public static final double SHARD_RADIUS = 4.0;

    /** How much the bone armour takes before it shatters: 5% of the boss's max health at the opening, 9% at the Gates. */
    public static double shellPool(double maxHealth, int power) {
        double t = Math.max(0, Math.min(PowerCurve.FULL, power)) / 100.0;
        return Math.max(1, maxHealth * (0.04 + 0.05 * t));
    }
}
