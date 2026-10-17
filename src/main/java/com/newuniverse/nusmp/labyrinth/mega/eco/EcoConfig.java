package com.newuniverse.nusmp.labyrinth.mega.eco;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server config of the faction layer of the Mega-Dungeon (turf wars and faction drops), {@code world/serverconfig/nusmp-labyrinth-mega-eco-server.toml},
 * section {@code [eco]}. Its own file (the pattern of {@code DndConfig} and {@code MegaConfig}) so this builder's switches never share a spec with
 * another builder's, and a server owner can turn the quarrels off without touching the economy. Every value is a plain {@code define} /
 * {@code defineInRange} (never {@code defineInList}: a list-validated value throws while the server is still loading) and every reader goes through
 * {@link #get}, which answers the default while the spec is not loaded yet (a datapack reload and block registration run before SERVER configs are
 * read). {@code range} and {@code nativeBonus} are the FALLBACK for a faction file that gives no {@code aggro} numbers; a file that gives its own wins,
 * and both are clamped by hard ceilings in {@link FactionDef} so a datapack cannot ask for a world-sized entity query.
 */
public final class EcoConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue TURF_WARS, FACTION_DROPS;
    public static final ModConfigSpec.IntValue SCAN_EVERY, RANGE, NATIVE_BONUS, PLAYER_YIELD, MAX_EXTRA_MEAT;
    public static final ModConfigSpec.DoubleValue STONE_THREAT_BONUS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("eco");
        TURF_WARS = b.comment("Monsters of rival factions in the Mega-Dungeon fight each other over territory when they cross paths (a goblin and an ogre in the same hall), and a monster never turns on its own faction or an ally. false = no quarrels, no target guard.")
                .define("turfWarsEnabled", true);
        SCAN_EVERY = b.comment("Ticks between two looks for a rival by one monster (20 = once a second). Higher is cheaper and lazier.")
                .defineInRange("scanEveryTicks", 20, 10, 200);
        RANGE = b.comment("Blocks within which a monster notices a rival, for a faction whose file has no aggro.range. A faction file's own number wins.")
                .defineInRange("range", 14, 4, 48);
        NATIVE_BONUS = b.comment("Extra blocks of notice when the rival stands inside the room that is native to the monster's faction, for a faction whose file has no aggro.nativeBonus.")
                .defineInRange("nativeBonus", 8, 0, 32);
        PLAYER_YIELD = b.comment("A monster with a player (survival or adventure) this close ignores the quarrel and keeps its mind on the player. 0 = quarrels go on in front of players.")
                .defineInRange("playerYieldRadius", 12, 0, 48);
        FACTION_DROPS = b.comment("Faction drops on top of the base economy: the faction's own raw cut and a stone bonus that grows with the sector's threat. false = only the base economy (dnd.drops) pays.")
                .define("factionDropsEnabled", true);
        MAX_EXTRA_MEAT = b.comment("The most raw cuts the faction adds to one kill, however deep the sector and however high the Looting. 0 = no extra cuts.")
                .defineInRange("maxExtraMeat", 2, 0, 8);
        STONE_THREAT_BONUS = b.comment("Chance per threat level of one extra Magic Stone (or, in deeper sectors, a rarer crystal) on a player kill: 0.02 gives 2 percent in the first sector, 10 percent at threat 5, at most 50 percent before the crystal rarity divides it. 0 = no bonus.")
                .defineInRange("stoneThreatBonus", 0.02, 0.0, 0.25);
        b.pop();
        SPEC = b.build();
    }

    private EcoConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
