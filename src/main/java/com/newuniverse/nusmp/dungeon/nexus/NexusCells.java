package com.newuniverse.nusmp.dungeon.nexus;

/** Finding the nearest core cell to a position (the admin commands use it). Pure; the cell test is passed in. */
public final class NexusCells {
    private NexusCells() {}

    /** Is this 128-block cell one of the Variable Core's? (DungeonTier.isCityCell in the game; or that and more.) */
    public interface CellTest { boolean test(int cellX, int cellZ); }

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
