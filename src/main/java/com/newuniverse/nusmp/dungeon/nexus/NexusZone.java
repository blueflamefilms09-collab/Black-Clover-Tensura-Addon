package com.newuniverse.nusmp.dungeon.nexus;

import com.newuniverse.nusmp.dungeon.DungeonTier;

/**
 * The faces of the Dungeon's Nexuses (docs/living_dungeon.md, "The Variable Core"). The first four are the climactic zones of the deepest
 * floor: the Dungeon's mana is so dense down there that the final floor has no fixed identity, and each core cell of 128 x 128 blocks pulls
 * one of them out of the multiverse. The last two are the arenas of the upper floors' bosses ({@link DungeonTier#arenaOf}): a Goliath's Pit
 * in the Upper Catacombs and a Charybdis Basin in the Deep Waterways, each in a cell that is not a core cell. The choice is a stable hash of
 * the cell, so the server and every client agree on it without any saved data (the client can colour its fog by zone). New zones are only
 * ever appended: the ordinal is saved (the collapse) and the admin command words follow it.
 */
public enum NexusZone {
    MONARCH_VAULT("The Monarch's Vault"),
    UNDERWORLD_GATE("The Underworld Gate"),
    TEMPEST_MIRAGE("The Tempest Mirage"),
    GOLDEN_CITY("The Golden City"),
    /** The Upper Catacombs' floor boss arena: a colosseum pit round a seal of bedrock (dungeon/floor/goliath). */
    GOLIATH_PIT("The Goliath's Pit"),
    /** The Deep Waterways' floor boss arena: a drowned basin (dungeon/floor/charybdis). */
    CHARYBDIS_BASIN("The Charybdis Basin");

    public final String label;

    NexusZone(String label) { this.label = label; }

    /** Width of one core cell in blocks (the same grid as DungeonTier.CITY_CELL). */
    public static final int CELL = 128;

    /** The four zones a core cell can be, in their original order: the hash picks among exactly these (the pick of every core cell stays what it was). */
    private static final NexusZone[] CORE = {MONARCH_VAULT, UNDERWORLD_GATE, TEMPEST_MIRAGE, GOLDEN_CITY};

    /** The four zones of the deepest floor (a copy; the arena zones are not among them). */
    public static NexusZone[] coreZones() { return CORE.clone(); }

    /** Is this one of the floor boss arenas (a pit or a basin), not a zone of the Variable Core? */
    public boolean isArena() { return this == GOLIATH_PIT || this == CHARYBDIS_BASIN; }

    /** Is this one of the four zones of the Variable Core? */
    public boolean isCore() { return !isArena(); }

    /**
     * The zone of the cell (cellX, cellZ): an arena cell is its arena; every other cell, a core cell included, takes an even, stable pick of
     * the four core zones (the very hash and modulo of 1.21.0, so no core cell changed its zone).
     */
    public static NexusZone of(int cellX, int cellZ) {
        switch (DungeonTier.arenaOf(cellX, cellZ)) {
            case DungeonTier.ARENA_GOLIATH -> { return GOLIATH_PIT; }
            case DungeonTier.ARENA_CHARYBDIS -> { return CHARYBDIS_BASIN; }
            default -> { }
        }
        long h = cellX * 0x9E3779B97F4A7C15L ^ cellZ * 0xC2B2AE3D27D4EB4FL ^ 11L * 0x165667B19E3779F9L;
        h ^= h >>> 31;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 29;
        return CORE[(int) Math.floorMod(h, (long) CORE.length)];
    }

    /** The zone of the cell that holds this column. */
    public static NexusZone at(int x, int z) { return of(Math.floorDiv(x, CELL), Math.floorDiv(z, CELL)); }

    /** The block coordinate of the middle of cell index {@code cell} (x or z). */
    public static int centre(int cell) { return cell * CELL + CELL / 2; }
}
