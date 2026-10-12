package com.newuniverse.nusmp.dungeon.nexus;

/**
 * The four faces of the Variable Core, the Living Dungeon's deepest floor (docs/living_dungeon.md, "The Variable Core"). The Dungeon's
 * mana is so dense down there that the final floor has no fixed identity: each core cell of 128 x 128 blocks pulls one of these four
 * climactic zones out of the multiverse. The choice is a stable hash of the cell, so the server and every client agree on it without
 * any saved data (the client can colour its fog by zone).
 */
public enum NexusZone {
    MONARCH_VAULT("The Monarch's Vault"),
    UNDERWORLD_GATE("The Underworld Gate"),
    TEMPEST_MIRAGE("The Tempest Mirage"),
    GOLDEN_CITY("The Golden City");

    public final String label;

    NexusZone(String label) { this.label = label; }

    /** Width of one core cell in blocks (the same grid as DungeonTier.CITY_CELL). */
    public static final int CELL = 128;

    /** The zone of the core cell (cellX, cellZ): an even, stable pick. */
    public static NexusZone of(int cellX, int cellZ) {
        long h = cellX * 0x9E3779B97F4A7C15L ^ cellZ * 0xC2B2AE3D27D4EB4FL ^ 11L * 0x165667B19E3779F9L;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return values()[(int) Math.floorMod(h, (long) values().length)];
    }

    /** The zone of the core cell that holds this column. */
    public static NexusZone at(int x, int z) { return of(Math.floorDiv(x, CELL), Math.floorDiv(z, CELL)); }

    /** The block coordinate of the middle of core cell index {@code cell} (x or z). */
    public static int centre(int cell) { return cell * CELL + CELL / 2; }
}
