package com.newuniverse.nusmp.heroweapon.gem;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: how a player is chosen by a hero weapon is the part of the job the owner did not specify, so the numbers that shape it are a server owner's to change
 * without a rebuild: how long the ritual is held, how still the player must stand, how much open space it needs, whether the survival route exists at all.
 * Its own file, {@code nusmp-heroweapon-gem-server.toml}, section {@code [gem]}, registered by {@link GemSystem#init}. Every value is a plain {@code define}
 * / {@code defineInRange} (never {@code defineInList}: a list-validated value throws while the server is still loading) and every reader goes through
 * {@link #get}, which answers the default while the spec is not loaded yet (the item's use duration is read by the client before any server config exists).
 */
public final class GemConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue RITUAL_SECONDS, OPEN_ABOVE;
    public static final ModConfigSpec.DoubleValue MOVE_TOLERANCE, SPEED_TOLERANCE;
    public static final ModConfigSpec.BooleanValue RECIPES, HOLY_FREE_ONLY, EFFECTS;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("gem");
        RITUAL_SECONDS = b.comment("Seconds a player holds right click with a Calling Gem or a Vassal Seal for the weapon to choose them.")
                .defineInRange("ritualSeconds", 5, 1, 60);
        RECIPES = b.comment("The survival route: when true anyone can use a gem. When false the gems still exist but only operators and creative players can use them, so a",
                "crafted gem does nothing for survival players. The crafting recipes themselves cannot read this setting (a recipe is loaded before the server config is),",
                "so to remove them as well, override data/nusmp/recipe/calling_gem.json, gem_fragment.json and vassal_seal_*.json in a data pack.")
                .define("recipes", true);
        MOVE_TOLERANCE = b.comment("How far, in blocks, the player may drift from the spot where the ritual began before it breaks.")
                .defineInRange("moveTolerance", 1.5, 0.0, 16.0);
        SPEED_TOLERANCE = b.comment("The fastest horizontal speed, in blocks per tick, at which the ritual may still begin (walking is about 0.22, sneaking 0.065).")
                .defineInRange("speedTolerance", 0.1, 0.0, 1.0);
        OPEN_ABOVE = b.comment("Blocks of clear space the ritual needs above the player's head (a sky is not required, a cavern will do). 0 switches the rule off.")
                .defineInRange("openAbove", 4, 0, 16);
        HOLY_FREE_ONLY = b.comment("The Holy Calling Gem only chooses among the Legendary Weapons that nobody holds right now. When false it may also pick one that is held, which",
                "only succeeds if the core's own rule (holyUnique in nusmp-heroweapon-server.toml) lets two players share it.")
                .define("holyGemFreeOnly", true);
        EFFECTS = b.comment("The ritual circle, the pillar of light and the sounds. The countdown, the title and the messages stay.")
                .define("effects", true);
        b.pop();
        SPEC = b.build();
    }

    private GemConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    /** The length of the ritual in ticks. */
    public static int ritualTicks() { return get(RITUAL_SECONDS) * 20; }

    public static boolean recipes() { return get(RECIPES); }

    public static double moveTolerance() { return get(MOVE_TOLERANCE); }

    public static double speedTolerance() { return get(SPEED_TOLERANCE); }

    public static int openAbove() { return get(OPEN_ABOVE); }

    public static boolean holyFreeOnly() { return get(HOLY_FREE_ONLY); }

    public static boolean effects() { return get(EFFECTS); }
}
