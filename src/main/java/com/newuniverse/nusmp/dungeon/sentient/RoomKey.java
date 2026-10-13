package com.newuniverse.nusmp.dungeon.sentient;

/**
 * What the Dungeon calls a room: a chunk column cut into bands of 16 blocks of height (chunk x, chunk z, floor(y / 16)). A room is one long,
 * so it can be saved and looked up cheaply; the box of the room is its chunk column over its band. Pure.
 */
public final class RoomKey {
    private RoomKey() {}

    /** The height of one band. */
    public static final int BAND = 16;
    private static final long MASK26 = (1L << 26) - 1, MASK12 = (1L << 12) - 1;

    public static long of(int chunkX, int chunkZ, int band) {
        return ((chunkX & MASK26) << 38) | ((chunkZ & MASK26) << 12) | (band & MASK12);
    }

    /** The room that holds the block (x, y, z). */
    public static long at(int x, int y, int z) { return of(x >> 4, z >> 4, Math.floorDiv(y, BAND)); }

    public static int chunkX(long key) { return (int) (key >> 38); }

    public static int chunkZ(long key) { return (int) ((key << 26) >> 38); }

    public static int band(long key) { return (int) ((key << 52) >> 52); }

    public static int minX(long key) { return chunkX(key) << 4; }

    public static int maxX(long key) { return minX(key) + 15; }

    public static int minZ(long key) { return chunkZ(key) << 4; }

    public static int maxZ(long key) { return minZ(key) + 15; }

    public static int minY(long key) { return band(key) * BAND; }

    public static int maxY(long key) { return minY(key) + BAND - 1; }

    /** Is the block inside the box of the room? */
    public static boolean contains(long key, int x, int y, int z) {
        return x >= minX(key) && x <= maxX(key) && y >= minY(key) && y <= maxY(key) && z >= minZ(key) && z <= maxZ(key);
    }
}
