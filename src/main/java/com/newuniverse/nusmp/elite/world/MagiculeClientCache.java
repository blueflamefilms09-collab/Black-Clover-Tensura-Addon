package com.newuniverse.nusmp.elite.world;

/**
 * Why: what the client knows about the magicule around the player, as told by the throttled sync ({@link MagiculeSync}): the player's own density (with the depth bonus), and the stored
 * density of the 3 x 3 chunks around, as bytes. It is plain data with no client classes in it, so a common class (the gauge item's tooltip) can read it safely; on a dedicated server it
 * simply stays "not valid". The HUD layer and the tooltip are the only readers.
 */
public final class MagiculeClientCache {
    private MagiculeClientCache() {}

    private static volatile boolean valid;
    private static volatile int chunkX, chunkZ, own;
    private static volatile byte[] grid = new byte[9];

    /** Called by the sync handler on the client thread. {@code ownQ} is negative when the world engine is not active where the player stands. */
    public static void accept(int cx, int cz, int ownQ, byte[] cells) {
        if (ownQ < 0) {
            valid = false;
            return;
        }
        chunkX = cx;
        chunkZ = cz;
        own = ownQ;
        grid = cells == null || cells.length != 9 ? new byte[9] : cells.clone();
        valid = true;
    }

    public static void clear() { valid = false; }

    public static boolean valid() { return valid; }

    /** The player's own density, 0..1. */
    public static double own() { return own / 255.0; }

    public static int chunkX() { return chunkX; }

    public static int chunkZ() { return chunkZ; }

    /** The stored density of the cell {@code (dx, dz)} round the player's chunk, dx and dz in -1..1, 0..1. */
    public static double cell(int dx, int dz) {
        int i = (dz + 1) * 3 + (dx + 1);
        if (i < 0 || i > 8) return 0.0;
        return (grid[i] & 0xFF) / 255.0;
    }
}
