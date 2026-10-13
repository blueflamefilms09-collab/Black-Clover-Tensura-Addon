package com.newuniverse.nusmp.dungeon.nexus;

import com.newuniverse.nusmp.dungeon.DungeonTier;

/** Finding the nearest cell that holds a Nexus to a position (the admin commands use it), and the question which cells those are. Pure; the cell test is passed in. */
public final class NexusCells {
    private NexusCells() {}

    /** Is this 128-block cell one the caller is looking for? (isNexusCell below in the game; or DungeonTier.isCityCell for the core cells alone.) */
    public interface CellTest { boolean test(int cellX, int cellZ); }

    /**
     * Does this 128-block cell hold a Nexus: is it one of the Variable Core's cells (a core cell) or one of the floor bosses' arenas (a Goliath's
     * Pit or a Charybdis Basin)? The two kinds never overlap. This is the question the builder, the lookups by position and the admin command ask;
     * what is only about the deepest floor (the seals, the descent gates, the other zones' ways) still asks {@link DungeonTier#isCityCell}.
     */
    public static boolean isNexusCell(int cellX, int cellZ) { return DungeonTier.isCityCell(cellX, cellZ) || DungeonTier.arenaOf(cellX, cellZ) != DungeonTier.ARENA_NONE; }

    /** How far above or below an arena's floor (blocks) a player may be and still start its building: the Upper tier alone is more than 100 deep. */
    public static final int ARENA_REACH_Y = 70;

    /** Is a player at height playerY near enough, in height, to an arena whose floor is at floorY to start building it? */
    public static boolean arenaInReach(int playerY, int floorY) { return Math.abs((long) playerY - floorY) <= ARENA_REACH_Y; }

    /**
     * The nearest cell to column (x, z) that passes the test, looking at the cells within `rings` of the column's own and measuring to the
     * cell's middle; {cellX, cellZ}, or null if there is none. Ties go to the first found, scanning x then z from the low side.
     */
    public static int[] nearest(int x, int z, int rings, CellTest test) {
        int cx = Math.floorDiv(x, NexusZone.CELL), cz = Math.floorDiv(z, NexusZone.CELL);
        int[] best = null;
        long bestD = Long.MAX_VALUE;
        for (int dx = -rings; dx <= rings; dx++) {
            for (int dz = -rings; dz <= rings; dz++) {
                if (!test.test(cx + dx, cz + dz)) continue;
                long ddx = NexusZone.centre(cx + dx) - x, ddz = NexusZone.centre(cz + dz) - z;
                long d = ddx * ddx + ddz * ddz;
                if (d < bestD) {
                    bestD = d;
                    best = new int[]{cx + dx, cz + dz};
                }
            }
        }
        return best;
    }

    /** The key of a cell as the saved data uses it. */
    public static long key(int cellX, int cellZ) { return ((long) cellX << 32) | (cellZ & 0xFFFFFFFFL); }

    public static int cellX(long key) { return (int) (key >> 32); }

    public static int cellZ(long key) { return (int) key; }
}
