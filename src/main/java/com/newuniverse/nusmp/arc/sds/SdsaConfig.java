package com.newuniverse.nusmp.arc.sds;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the few rules of Meliodas, Escanor and Ban that a server owner may reasonably want to change without editing code, {@code nusmp-arc-sdsa-server.toml},
 * section {@code [sdsa]}: whether Meliodas has his Full Counter at all, how fast Escanor's sun runs during a fight and where it starts (the day cycle of the
 * game is twenty minutes, a boss fight a few, so the sun is sped up inside the fight; a server that wants the plain world clock sets the speed to 1), the floor of
 * his sun once the Gates are open, whether his heat burns, how many lives Ban's Fountain gives and how much of a player's strength one Snatch may hold. Its own
 * file (not more keys in {@code ArcConfig}) so the builders of the other characters never edit one spec. Every value is a plain {@code define} /
 * {@code defineInRange} (never {@code defineInList}: a list-validated value throws while the server is still loading) and every reader goes through
 * {@link #get}, which answers the default while the spec is not loaded yet.
 */
public final class SdsaConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue FULL_COUNTER, HEAT_AURA, FOUNTAIN;
    public static final ModConfigSpec.DoubleValue SUN_SPEED, SUN_GATES_FLOOR;
    public static final ModConfigSpec.ConfigValue<String> SUN_START;
    public static final ModConfigSpec.IntValue FOUNTAIN_LIVES, SNATCH_CAP_PERCENT;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("sdsa");
        FULL_COUNTER = b.comment("Meliodas raises his Full Counter (throws magic back at the caster). False: he never does; the rest of his fight is unchanged.")
                .define("meliodasFullCounter", true);
        SUN_SPEED = b.comment("How many times faster than the game's day the sun of Escanor's fight runs once the fight has begun (1 = the plain world clock, 0 = it stays at the hour the fight began).")
                .defineInRange("escanorSunSpeed", 3.0, 0.0, 12.0);
        SUN_START = b.comment("Where Escanor's sun starts when a fight begins: 'world' (the hour the world's clock shows, the default), 'dawn', 'noon' or 'midnight'. Anything else is 'world'.")
                .define("escanorSunStart", "world");
        SUN_GATES_FLOOR = b.comment("The lowest Sunshine (0 to 1) Escanor has once the Gates are open: Rhitta pours out the light it stored, so the last quarter of the fight is never a walkover at night.")
                .defineInRange("escanorGatesSunFloor", 0.60, 0.0, 1.0);
        HEAT_AURA = b.comment("The heat of Escanor's body burns players who stand close to him while the sun is high. False: he has no aura.")
                .define("escanorHeatAura", true);
        FOUNTAIN = b.comment("Ban's Fountain of Youth: he falls and rises again instead of dying (at most as many lives as below). False: he dies like any boss.")
                .define("banFountain", true);
        FOUNTAIN_LIVES = b.comment("How many lives Ban has when the Fountain is on (1 to 3; 1 means he never rises).")
                .defineInRange("banFountainLives", 3, 1, 3);
        SNATCH_CAP_PERCENT = b.comment("The most of one player's attack (percent) that Ban's Snatch may hold at once.")
                .defineInRange("banSnatchCapPercent", 30, 5, 60);
        b.pop();
        SPEC = b.build();
    }

    private SdsaConfig() {}

    /** The value, or its default while the config is not loaded yet. */
    public static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean fullCounter() { return get(FULL_COUNTER); }

    public static double sunSpeed() { return get(SUN_SPEED); }

    public static SunClock.Start sunStart() { return SunClock.Start.parse(get(SUN_START)); }

    public static double gatesSunFloor() { return get(SUN_GATES_FLOOR); }

    public static boolean heatAura() { return get(HEAT_AURA); }

    /** The lives of Ban: 1 when the Fountain is off. */
    public static int fountainLives() { return get(FOUNTAIN) ? get(FOUNTAIN_LIVES) : 1; }

    /** The share (0.05 to 0.60) of one player's attack that one Snatch may hold. */
    public static double snatchCap() { return get(SNATCH_CAP_PERCENT) / 100.0; }
}
