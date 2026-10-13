package com.newuniverse.nusmp.dungeon.nexus;

/**
 * The Dungeon's spatial distortions (docs/living_dungeon.md section 3): pockets where space and gravity are warped, corridors overlap and
 * compasses fail. A pure function of the position, so the server (warp traps, block rejection), the Anti-Bird Compass and the clients
 * agree without saved data. About one cell in five is distorted; the strength fades from 1 at the middle of a pocket to 0 at its edge.
 */
public final class SpatialDistortion {
    private SpatialDistortion() {}

    public static final int CELL_XZ = 40, CELL_Y = 24;
    private static final int PERCENT = 20;

    private static long hash(int cx, int cy, int cz) {
        long h = cx * 0x9E3779B97F4A7C15L ^ cy * 0xC2B2AE3D27D4EB4FL ^ cz * 0x165667B19E3779F9L ^ 0x5851F42D4C957F2DL;
        h ^= h >>> 32;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return h;
    }

    /** Is the cell holding this position a distorted pocket? */
    public static boolean at(int x, int y, int z) {
        return Math.floorMod(hash(Math.floorDiv(x, CELL_XZ), Math.floorDiv(y, CELL_Y), Math.floorDiv(z, CELL_XZ)), 100L) < PERCENT;
    }

    /** 0 outside a pocket, rising to 1 at its middle. */
    public static double strength(int x, int y, int z) {
        if (!at(x, y, z)) return 0;
        double dx = (Math.floorMod(x, CELL_XZ) + 0.5) / CELL_XZ - 0.5, dy = (Math.floorMod(y, CELL_Y) + 0.5) / CELL_Y - 0.5, dz = (Math.floorMod(z, CELL_XZ) + 0.5) / CELL_XZ - 0.5;
        double d = Math.sqrt(dx * dx + dy * dy + dz * dz) / 0.8660254;                // 0 at the middle, 1 at a corner
        return Math.max(0, 1 - d * d);
    }
}
