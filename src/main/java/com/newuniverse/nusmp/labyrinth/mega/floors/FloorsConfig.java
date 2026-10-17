package com.newuniverse.nusmp.labyrinth.mega.floors;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server config of the floors and the Directional Lock, {@code world/serverconfig/nusmp-labyrinth-mega-floors-server.toml}, section {@code [floors]}.
 * Its own file (the pattern of {@code MegaConfig} and {@code DndConfig}) so this builder's switches never share a spec with another builder's, and an
 * owner who dislikes the lock can turn it off without touching anything else of the Mega-Dungeon. Every value is a plain {@code define} /
 * {@code defineInRange} (never {@code defineInList}: a list-validated value throws while the server is still loading) and every reader goes through
 * {@link #get}, which answers the default while the spec is not loaded yet (block registration, datapack reloads and the first sector all run before SERVER
 * configs are read). The lead registers {@link #SPEC} in the mod constructor with the file name above.
 */
public final class FloorsConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue LOCK_ENABLED, HARD_LOCK, OPS_BYPASS;
    public static final ModConfigSpec.IntValue LOCK_AT_FLOOR, PER_REGION, PER_SECTOR, TRACK_EVERY_TICKS;
    /** How the rooms of a sector are cut into floors: "even" (default) or "arena" (the design brief's rule); see {@code FloorBands.Strategy}. */
    public static final ModConfigSpec.ConfigValue<String> BANDING;

    public static final String BANDING_EVEN = "even", BANDING_ARENA = "arena";

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("floors");
        LOCK_ENABLED = b.comment("The Directional Lock: once a player has cleared floor lockAtFloor of the Holy road (+) or the Abyssal road (-), the opposite road is closed to them (the way back to Floor 0 stays open). false = both roads are always open and nobody is ever locked.")
                .define("lockEnabled", true);
        LOCK_AT_FLOOR = b.comment("How deep the first road must be cleared before it locks: 1 = clearing Floor +1 or -1 (the owner's rule), 3 = the lock closes when floor 3 is cleared, and until then both roads can still be tried. A floor is cleared by moving on from it to a deeper one.")
                .defineInRange("lockAtFloor", 1, 1, 10);
        HARD_LOCK = b.comment("Every second, a locked player who stands in a sector of the OTHER road is sent out of the dungeon. This catches what the doors cannot: an operator's teleport, a relic, an old Gate or Maw. false = only the doors (the Hub's gates and the chain travel) enforce the lock.")
                .define("hardLock", true);
        OPS_BYPASS = b.comment("Operators (permission level 2 or more) pass the lock and the hard guard, so they can test both roads. false = the lock binds everyone.")
                .define("opsBypass", true);
        PER_REGION = b.comment("How many floors one sector has (the floor of the Guardian's arena is the last one, so this is also the distance between two bosses). It is stored in each sector the first time it is numbered and a sector of a road that already has numbered sectors takes THEIR length, so changing this key in a world that has opened a road does not renumber it or overlap its floors: it only matters for a road that has not been opened yet.")
                .defineInRange("perRegion", 10, 2, 20);
        PER_SECTOR = b.comment("1.58.0, the floor model: how many floors one SECTOR holds when a road is opened. 1 (default) = every floor is a level of its own: the party starts at the foot of a stair and must find the stair that leads on (the way a D&D dungeon works). A value above 1 keeps the old model, where a sector is that many floors laid flat in one big sector and banded by distance (then perRegion and banding below apply). Read when the FIRST sector of a road is created; a road that already exists keeps the length of its sectors, so changing this key never renumbers a world.")
                .defineInRange("perSector", 1, 1, 20);
        BANDING = b.comment("How a sector's rooms are cut into its floors. \"even\" (default): rooms are ranked by how many doorways they are from the Landing and cut into runs of equal size, so every floor from 1 to perRegion-1 holds about the same number of rooms and the last floor is the boss chain alone. \"arena\": the floor is the doorway distance over the distance to the Guardian's arena, which only fills the floors when the arena is perRegion-1 or more doorways away (the generated sectors are shallower, so floors are left empty and about half the rooms land on the last one). Read when a sector is numbered; a sector already numbered keeps its floors until /multiverse mega floors assign is run.")
                .define("banding", BANDING_EVEN, o -> o instanceof String s && (s.equalsIgnoreCase(BANDING_EVEN) || s.equalsIgnoreCase(BANDING_ARENA)));
        TRACK_EVERY_TICKS = b.comment("Ticks between two looks at which floor each player stands on (10 = twice a second). Lower is quicker to notice a new floor, higher is cheaper.")
                .defineInRange("trackEveryTicks", 10, 5, 100);
        b.pop();
        SPEC = b.build();
    }

    private FloorsConfig() {}

    /** The value, or its default while the server config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
