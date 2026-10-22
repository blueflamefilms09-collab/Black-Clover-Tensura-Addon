package com.newuniverse.nusmp.spiritspace;

/**
 * Why: a portal must never drop its owner inside a wall, over the void or into lava. The decision of "is this a safe place to stand" is pure here (it asks a {@link Probe} about single blocks), so it is
 * unit tested with a fake world; the server fills the probe from blocks that are ALREADY loaded (it never loads or generates a chunk). The search is bounded: at most {@link #VERTICAL} blocks up and
 * down in the waypoint's own column, then the eight neighbours of that column and the sixteen at distance two, nothing farther, so one portal costs at most {@link #MAX_COLUMNS} columns of
 * {@code 2 * VERTICAL + 1} cells each.
 */
public final class SpaceLanding {
    private SpaceLanding() {}

    /** What the world answers about one block position. Unloaded cells must answer "not safe" everywhere (solid false, passable false, hazard true). */
    public interface Probe {
        /** A block a player can stand on (a full top face). */
        boolean solid(int x, int y, int z);

        /** Empty enough to stand in: no collision shape and no fluid. */
        boolean passable(int x, int y, int z);

        /** A block that hurts or kills on touch or when stood next to or on: lava, fire, cactus, magma, berry bushes, campfires, powder snow, unloaded chunks. */
        boolean hazard(int x, int y, int z);

        /** The lowest and the highest y a player may be put at (feet). */
        int minY();

        int maxY();
    }

    public static final int VERTICAL = 4, MAX_COLUMNS = 25;
    /** Offsets of the columns tried, nearest first: the column itself, its eight neighbours, then the ring at distance two. */
    private static final int[][] COLUMNS = buildColumns();

    private static int[][] buildColumns() {
        int[][] c = new int[MAX_COLUMNS][];
        int n = 0;
        for (int r = 0; r <= 2; r++)
            for (int dx = -r; dx <= r; dx++)
                for (int dz = -r; dz <= r; dz++)
                    if (Math.max(Math.abs(dx), Math.abs(dz)) == r) c[n++] = new int[]{dx, dz};
        return c;
    }

    /** The probes order in y: 0, +1, -1, +2, -2 ... up to {@link #VERTICAL}. */
    private static int dyAt(int i) { return i == 0 ? 0 : ((i & 1) == 1 ? (i + 1) / 2 : -(i / 2)); }

    /** Is a player standing with the feet in the cell (x, y, z) safe: stone under the feet, two cells of room, nothing hazardous in them or under them or at the sides of the feet. */
    public static boolean safe(Probe p, int x, int y, int z) {
        if (y < p.minY() + 1 || y > p.maxY() - 2) return false;
        if (!p.solid(x, y - 1, z) || p.hazard(x, y - 1, z)) return false;
        if (!p.passable(x, y, z) || !p.passable(x, y + 1, z)) return false;
        if (p.hazard(x, y, z) || p.hazard(x, y + 1, z)) return false;
        return !(p.hazard(x + 1, y, z) || p.hazard(x - 1, y, z) || p.hazard(x, y, z + 1) || p.hazard(x, y, z - 1));
    }

    /** The nearest safe feet cell to (x, y, z), as {x, y, z}, or null when there is none inside the bounded search. */
    public static int[] find(Probe p, int x, int y, int z) {
        for (int[] col : COLUMNS) {
            for (int i = 0; i <= 2 * VERTICAL; i++) {
                int cy = y + dyAt(i);
                if (safe(p, x + col[0], cy, z + col[1])) return new int[]{x + col[0], cy, z + col[1]};
            }
        }
        return null;
    }

    /** The most cells one search can ask the probe about: columns times vertical steps times the cells {@link #safe} reads (it reads at most eleven). */
    public static int maxProbeCalls() { return MAX_COLUMNS * (2 * VERTICAL + 1) * 11; }
}
