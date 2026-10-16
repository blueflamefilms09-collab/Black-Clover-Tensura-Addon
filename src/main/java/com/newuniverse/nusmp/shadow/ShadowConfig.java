package com.newuniverse.nusmp.shadow;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Why: Nacht Faust's Shadow Travel and Devil Unite modes carry balance numbers (how dark a shadow must be, how far the jump reaches, whether
 * the new abilities also ride on the old quick Unite pages) that the owner must be able to tune or switch off without a rebuild. Own spec file
 * ({@code nusmp-shadow-server.toml}, SERVER); the lead registers it with
 * {@code modContainer.registerConfig(ModConfig.Type.SERVER, ShadowConfig.SPEC, "nusmp-shadow-server.toml")}. Plain define / defineInRange only.
 * Every read falls back to the default before the config is loaded, so the rules hold from the first tick.
 */
public final class ShadowConfig {
    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.IntValue MAX_LIGHT, TRAVEL_RANGE, SEARCH_CAP, FOLLOWERS, PACK_MAX;
    public static final ModConfigSpec.DoubleValue MANA_PER_BLOCK, DODGE_VS_BOSS;
    public static final ModConfigSpec.BooleanValue REQUIRE_SHADOW, UNITE_OVERLAYS, UNITE_EXTRAS_ON_OLD_PAGES, UNITE_SIGHT;

    static {
        ModConfigSpec.Builder b = new ModConfigSpec.Builder();
        b.push("shadowTravel");
        MAX_LIGHT = b.comment("A spot is shadow when its light level (the brighter of block light and sky light after the time of day) is at most this at the feet AND the head.",
                        "3 means 'light level below 4'.")
                .defineInRange("maxLight", ShadowLight.DEFAULT_MAX_LIGHT, ShadowLight.MIN_MAX_LIGHT, ShadowLight.MAX_MAX_LIGHT);
        TRAVEL_RANGE = b.comment("How far (blocks) Shadow Travel may reach.").defineInRange("travelRange", 48, 8, 128);
        SEARCH_CAP = b.comment("How many candidate cells one cast may check when the aimed spot is not shadow (bounds the work per cast).")
                .defineInRange("searchCap", ShadowSearch.DEFAULT_CAP, 50, 2000);
        FOLLOWERS = b.comment("How many sneaking allies next to the caster follow through the shadow.").defineInRange("followers", 2, 0, 6);
        MANA_PER_BLOCK = b.comment("Extra magicules, as a percent of the caster's maximum, for every block travelled (on top of the page cost; capped at 8 percent).")
                .defineInRange("manaPerBlock", 0.10, 0.0, 1.0);
        REQUIRE_SHADOW = b.comment("The caster must stand in shadow (or be in a Unite mode, which brings its own darkness). false = cast from anywhere.")
                .define("requireShadow", true);
        b.pop();
        b.push("uniteModes");
        UNITE_OVERLAYS = b.comment("Show the four Devil Unite armour overlays (also on the old quick pages Canis, Gallus and Canis x Felis).").define("overlays", true);
        UNITE_EXTRAS_ON_OLD_PAGES = b.comment("The old quick pages Unite Canis, Unite Gallus and Canis x Felis also bring the new mode's own abilities (Canis: shadow pack and spikes).",
                        "false = those pages stay exactly the plain 30 second buffs they were (only the overlay is added).")
                .define("extrasOnOldPages", true);
        DODGE_VS_BOSS = b.comment("Unite Felis dodge chance against bosses (against everything else it is 20 percent).").defineInRange("dodgeVsBoss", 0.10, 0.0, 0.25);
        UNITE_SIGHT = b.comment("Unite Felis outlines living things within 24 blocks for the wearer only.").define("felisSight", true);
        PACK_MAX = b.comment("The most shadow wolves one Unite Canis may call (3 at low power, 4 and 5 with Existence Points).").defineInRange("packMax", 5, 0, 8);
        b.pop();
        SPEC = b.build();
    }

    private ShadowConfig() {}

    private static <T> T get(ModConfigSpec.ConfigValue<T> v, T fallback) {
        try { return v.get(); } catch (RuntimeException notLoaded) { return fallback; }
    }

    public static int maxLight() { return ShadowLight.clampMax(get(MAX_LIGHT, ShadowLight.DEFAULT_MAX_LIGHT)); }
    public static int travelRange() { return get(TRAVEL_RANGE, 48); }
    public static int searchCap() { return get(SEARCH_CAP, ShadowSearch.DEFAULT_CAP); }
    public static int followers() { return get(FOLLOWERS, 2); }
    public static double manaPerBlock() { return get(MANA_PER_BLOCK, 0.10); }
    public static boolean requireShadow() { return get(REQUIRE_SHADOW, true); }
    public static boolean overlays() { return get(UNITE_OVERLAYS, true); }
    public static boolean extrasOnOldPages() { return get(UNITE_EXTRAS_ON_OLD_PAGES, true); }
    public static double dodgeVsBoss() { return get(DODGE_VS_BOSS, 0.10); }
    public static boolean felisSight() { return get(UNITE_SIGHT, true); }
    public static int packMax() { return get(PACK_MAX, 5); }
}
