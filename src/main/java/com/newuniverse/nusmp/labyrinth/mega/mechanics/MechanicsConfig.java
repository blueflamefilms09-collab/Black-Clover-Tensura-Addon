package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import net.neoforged.neoforge.common.ModConfigSpec;

import java.util.EnumSet;
import java.util.Set;

/**
 * Why: server config of the dungeon mechanics library, {@code world/serverconfig/nusmp-labyrinth-mega-mechanics-server.toml}, section {@code [mechanics]}. Its own file (the
 * pattern of {@code HubConfig}) so no two builders edit one spec. Everything is a plain {@code define} / {@code defineInRange} (never {@code defineInList}: a list-validated
 * value throws while the server is still loading), and the blacklist is a comma-separated STRING for the same reason. Every reader goes through {@link #get}, which answers
 * the default while the spec is not loaded yet. {@code enabled = false} switches the whole layer off: the Director plans and fires nothing and the dungeon is exactly as it was.
 */
public final class MechanicsConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, FLOOR, PUZZLE, HAZARD, COMBAT, RISK, BOSS, TRAVEL, PARTY, CREATIVE_ACTIVATES;
    public static final ModConfigSpec.IntValue PER_FLOOR, PER_ROOM;
    public static final ModConfigSpec.DoubleValue DIFFICULTY;
    public static final ModConfigSpec.ConfigValue<String> BLACKLIST;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("mechanics");
        ENABLED = b.comment("The dungeon mechanics library: floor modifiers, room puzzles, hazards, combat gimmicks, risk-and-reward, boss rules, traversal and party mechanics attached to floors, rooms and arenas. false = none of them is ever planned or fired.")
                .define("enabled", true);
        PER_FLOOR = b.comment("The most mechanics one floor carries (floor-wide ones and room / zone ones together; a boss arena may add up to two more of its own). 0 = none.")
                .defineInRange("perFloor", 4, 0, 12);
        PER_ROOM = b.comment("The most mechanics one room may carry.")
                .defineInRange("perRoom", 1, 1, 3);
        DIFFICULTY = b.comment("Multiplier on the damage, counts and threat budget of the mechanics (1.0 = as designed).")
                .defineInRange("difficulty", 1.0, 0.25, 4.0);
        FLOOR = b.comment("Allow the floor-modifier category.").define("floorModifiers", true);
        PUZZLE = b.comment("Allow the room-puzzle category.").define("roomPuzzles", true);
        HAZARD = b.comment("Allow the hazards-and-traps category.").define("hazards", true);
        COMBAT = b.comment("Allow the combat-gimmick category.").define("combatGimmicks", true);
        RISK = b.comment("Allow the risk-and-reward category.").define("riskReward", true);
        BOSS = b.comment("Allow the boss-mechanic category.").define("bossMechanics", true);
        TRAVEL = b.comment("Allow the traversal category.").define("traversal", true);
        PARTY = b.comment("Allow the social-and-party category.").define("party", true);
        BLACKLIST = b.comment("Mechanic ids that are never drawn, comma-separated (for example \"hazard.spore_bloom,floor.rim_curse.e_wither\").")
                .define("blacklist", "");
        CREATIVE_ACTIVATES = b.comment("Creative-mode and spectating players start mechanics too (they are never hurt by them). false = only survival and adventure players wake a mechanic.")
                .define("creativeActivates", false);
        b.pop();
        SPEC = b.build();
    }

    private MechanicsConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(ENABLED); }

    public static double difficulty() { return Scaling.clampDifficulty(get(DIFFICULTY)); }

    /** The categories the config allows. */
    public static Set<MechCategory> categories() {
        Set<MechCategory> out = EnumSet.noneOf(MechCategory.class);
        if (get(FLOOR)) out.add(MechCategory.FLOOR);
        if (get(PUZZLE)) out.add(MechCategory.PUZZLE);
        if (get(HAZARD)) out.add(MechCategory.HAZARD);
        if (get(COMBAT)) out.add(MechCategory.COMBAT);
        if (get(RISK)) out.add(MechCategory.RISK);
        if (get(BOSS)) out.add(MechCategory.BOSS);
        if (get(TRAVEL)) out.add(MechCategory.TRAVEL);
        if (get(PARTY)) out.add(MechCategory.PARTY);
        return out;
    }

    /** The blacklist as a set of lower-case ids. */
    public static Set<String> blacklist() { return Blacklist.parse(get(BLACKLIST)); }

    /** The planner's view of the config. */
    public static Planner.Config planner() {
        return new Planner.Config(get(PER_FLOOR), get(PER_ROOM), difficulty(), categories(), blacklist());
    }
}
