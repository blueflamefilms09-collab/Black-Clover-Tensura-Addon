package com.newuniverse.nusmp.elite.forge;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: the owner must be able to switch the whole Forge & Custom Crafting System off, and to tune the few numbers that are a matter of taste (what changing a
 * specialization costs, how strong a quality is on the item), without touching code. Own SERVER spec file ({@code nusmp-elite-forge-server.toml}); the master switch
 * {@code enabled} makes the stations inert (they say the fires are cold), refuses every forge packet and leaves the {@code EliteHooks} answering their defaults. The
 * registry (blocks, items, components) cannot depend on a config and stays registered either way. Reads fall back to the defaults before the config is loaded.
 */
public final class ForgeConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.BooleanValue ENABLED;
    public static final ModConfigSpec.BooleanValue ENFORCE_TIER;
    public static final ModConfigSpec.BooleanValue LUCK;
    public static final ModConfigSpec.BooleanValue ANNOUNCE_MYTHICAL;
    public static final ModConfigSpec.IntValue SPEC_CHANGE_LEVELS;
    public static final ModConfigSpec.IntValue ENGRAVE_LEVELS;
    public static final ModConfigSpec.IntValue STATION_RANGE;
    public static final ModConfigSpec.DoubleValue STAT_SCALE;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("forge");
        ENABLED = b.comment("Master switch of the Forge & Custom Crafting System (elite/forge). false: the four stations do nothing, no forge packet is accepted and the",
                "forge hooks of other systems answer 0. The blocks and items stay registered so a world with stations in it still loads.")
                .define("enabled", true);
        ENFORCE_TIER = b.comment("A recipe needs a station of its tier or better. false: every station can forge every recipe (quality is still capped by the station).")
                .define("enforceStationTier", true);
        LUCK = b.comment("The quality roll adds a small random luck (+-4 points). false: the roll is fully decided by skill, station, materials and specialization.")
                .define("luck", true);
        ANNOUNCE_MYTHICAL = b.comment("Tell the whole server when somebody forges a Mythical item.")
                .define("announceMythical", true);
        SPEC_CHANGE_LEVELS = b.comment("Experience levels it costs to CHANGE a specialization (the first choice is free; creative players pay nothing).")
                .defineInRange("specializationChangeLevels", 20, 0, 100);
        ENGRAVE_LEVELS = b.comment("Experience levels it costs to engrave one more Tensura engraving on the forged item in the main hand at a station (creative players pay nothing).")
                .defineInRange("engraveLevels", 5, 0, 100);
        STATION_RANGE = b.comment("How close (blocks) a player must stay to the station while forging.")
                .defineInRange("stationRange", 8, 3, 32);
        STAT_SCALE = b.comment("Scales the attack damage / armour / durability bonus a quality gives (1.0 = the documented values: Mythical +30% damage or armour, +85% durability).")
                .defineInRange("qualityStatScale", 1.0, 0.0, 3.0);
        b.pop();
        SPEC = b.build();
    }

    private ForgeConfig() {}

    private static <T> T get(ModConfigSpec.ConfigValue<T> v) {
        try { return v.get(); } catch (IllegalStateException notLoaded) { return v.getDefault(); }
    }

    public static boolean enabled() { return get(ENABLED); }

    public static boolean enforceTier() { return get(ENFORCE_TIER); }

    public static boolean luck() { return get(LUCK); }

    public static boolean announceMythical() { return get(ANNOUNCE_MYTHICAL); }

    public static int specChangeLevels() { return get(SPEC_CHANGE_LEVELS); }

    public static int engraveLevels() { return get(ENGRAVE_LEVELS); }

    public static int stationRange() { return get(STATION_RANGE); }

    public static double statScale() { return get(STAT_SCALE); }
}
