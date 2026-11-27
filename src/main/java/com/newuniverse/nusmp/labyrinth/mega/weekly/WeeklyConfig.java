package com.newuniverse.nusmp.labyrinth.mega.weekly;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: server settings of the weekly dungeon modifiers, {@code nusmp-labyrinth-mega-weekly-server.toml}, section {@code [weekly]}. They change gameplay, so they are SERVER settings (rule 9);
 * every reader goes through {@link #get}, which answers the default while the spec is not loaded yet.
 */
public final class WeeklyConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED, FIRST_CLEAR;
    public static final ModConfigSpec.ConfigValue<String> FIRST_CLEAR_ITEM;
    public static final ModConfigSpec.IntValue FLOOR_ONE, FLOOR_TWO, FLOOR_THREE, STRENGTH, PAR_MINUTES;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("weekly");
        ENABLED = b.comment("5.5.0: every real-world week the creatures of the deeper dungeon floors get 1 to 3 shared twists (thicker hide, faster, harder hitting, armoured, sharper senses). False: none, as before.")
                .define("enabled", true);
        FLOOR_ONE = b.comment("From this floor on, creatures carry one weekly twist (floor 30 = the third Landing of ten-floor regions).")
                .defineInRange("floorOne", 30, 1, 10000);
        FLOOR_TWO = b.comment("From this floor on, two weekly twists.")
                .defineInRange("floorTwo", 70, 1, 10000);
        FLOOR_THREE = b.comment("From this floor on, three weekly twists.")
                .defineInRange("floorThree", 100, 1, 10000);
        STRENGTH = b.comment("How strong the twists are, in percent of their listed size (100 = as listed, 50 = half, 200 = double). 0 = no effect.")
                .defineInRange("strengthPercent", 100, 0, 400);
        FIRST_CLEAR = b.comment("5.5.0: the first time a player sees a dungeon Guardian fall (while standing in its sector) they get experience levels and a few of firstClearItem, once per player and sector, growing with the floor. False: no prize, as before.")
                .define("firstClear", true);
        PAR_MINUTES = b.comment("5.6.0: a first clear made within this many minutes of entering the floor pays half as many levels again (par time). 0 = no par bonus.")
                .defineInRange("parMinutes", 15, 0, 240);
        FIRST_CLEAR_ITEM = b.comment("The item id of the first-clear prize (an unknown id falls back to minecraft:diamond).")
                .define("firstClearItem", "minecraft:diamond");
        b.pop();
        SPEC = b.build();
    }

    private WeeklyConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }
}
