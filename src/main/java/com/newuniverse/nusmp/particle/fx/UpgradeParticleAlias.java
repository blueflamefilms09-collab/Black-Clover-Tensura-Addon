package com.newuniverse.nusmp.particle.fx;

import java.util.Locale;
import java.util.Map;

/**
 * Why: the effect tables of the upgraded weapons and relics ({@code assets/nusmp/visual_upgrade/**.json}) name their particles with a short token, and until 1.110.0 half of the loaders turned
 * the token into a VANILLA particle (a registry lookup of {@code minecraft:flame}, a coloured vanilla dust): white square puffs, a flame sprite that fills the first-person view, cyan dust.
 * This pure class (no game types, so a test can walk the shipped tables with it) holds the one decision every loader now uses: a token becomes the id of one of the mod's own particles
 * ({@link FxCatalog}), never a vanilla one.
 * <ul>
 *   <li>{@code "fx"}, {@code "dust"}, empty or null: the hue-themed custom mote dyed with the layer's colour ({@link FxRelic#dust}); {@link #resolve} answers {@code null} for them.</li>
 *   <li>a catalog id ({@code flame_tongue}, {@code nusmp:ember}): itself.</li>
 *   <li>a vanilla particle name ({@code minecraft:end_rod}, {@code flame}, ...): the nearest custom particle of the {@link #ALIAS} table. Every vanilla particle of 1.21.1 is in the table, and
 *       an unknown name falls back to {@link #FALLBACK}, so no token can ever reach the game as a vanilla particle.</li>
 * </ul>
 */
public final class UpgradeParticleAlias {
    private UpgradeParticleAlias() {}

    /** What an unknown name becomes: the signature light mote of the library. */
    public static final String FALLBACK = "mote";

    /** The tokens that mean "the hue-themed custom mote dyed with the layer colour". */
    public static final String FX = "fx";

    /** Every vanilla particle of 1.21.1 (path) and the custom particle that stands for it in an upgrade effect. */
    private static final Map<String, String> ALIAS = Map.ofEntries(
            // lights and glints
            Map.entry("end_rod", "mote"), Map.entry("glow", "mote_soft"), Map.entry("glow_squid_ink", "spirit_firefly"), Map.entry("firework", "glint"),
            Map.entry("totem_of_undying", "glint"), Map.entry("enchanted_hit", "glint"), Map.entry("wax_on", "glint"), Map.entry("wax_off", "glint"), Map.entry("scrape", "glint"),
            Map.entry("happy_villager", "gem_sparkle"), Map.entry("heart", "petal"), Map.entry("note", "music_note"), Map.entry("enchant", "rune_sol"), Map.entry("witch", "mana_mote"),
            Map.entry("effect", "mana_mote"), Map.entry("instant_effect", "mana_mote"), Map.entry("entity_effect", "mana_mote"), Map.entry("elder_guardian", "mote"),
            Map.entry("trial_spawner_detection", "mana_mote"), Map.entry("trial_spawner_detection_ominous", "void_speck"), Map.entry("vault_connection", "mana_mote"),
            Map.entry("ominous_spawning", "void_speck"), Map.entry("raid_omen", "void_speck"), Map.entry("trial_omen", "void_speck"),
            // sparks and lightning
            Map.entry("crit", "spark"), Map.entry("damage_indicator", "spark"), Map.entry("angry_villager", "spark"), Map.entry("electric_spark", "electric_orb"),
            // fire
            Map.entry("flame", "flame_tongue"), Map.entry("small_flame", "flame_tongue"), Map.entry("soul_fire_flame", "flame_soul"),
            Map.entry("lava", "lava_blob"), Map.entry("falling_lava", "lava_blob"), Map.entry("dripping_lava", "lava_blob"), Map.entry("landing_lava", "lava_blob"),
            Map.entry("dripping_dripstone_lava", "lava_blob"), Map.entry("falling_dripstone_lava", "lava_blob"),
            // smoke
            Map.entry("cloud", "smoke_wisp"), Map.entry("smoke", "smoke_wisp"), Map.entry("white_smoke", "smoke_wisp"), Map.entry("poof", "smoke_wisp"), Map.entry("sneeze", "smoke_wisp"),
            Map.entry("campfire_cosy_smoke", "smoke_wisp"), Map.entry("campfire_signal_smoke", "smoke_wisp"), Map.entry("large_smoke", "soot_puff"), Map.entry("squid_ink", "soot_puff"),
            // void, souls, breath
            Map.entry("portal", "void_speck"), Map.entry("reverse_portal", "void_speck"), Map.entry("dragon_breath", "nebula_puff"), Map.entry("soul", "soul_wisp"),
            Map.entry("sculk_soul", "soul_wisp"), Map.entry("sculk_charge", "void_speck"), Map.entry("sculk_charge_pop", "void_speck"), Map.entry("shriek", "void_speck"),
            Map.entry("vibration", "mote"),
            // water, ice
            Map.entry("snowflake", "snowflake"), Map.entry("bubble", "bubble"), Map.entry("bubble_pop", "bubble"), Map.entry("bubble_column_up", "bubble"), Map.entry("nautilus", "bubble"),
            Map.entry("dolphin", "bubble"), Map.entry("underwater", "bubble"), Map.entry("fishing", "bubble"), Map.entry("splash", "droplet"), Map.entry("current_down", "droplet"),
            Map.entry("rain", "droplet"), Map.entry("dripping_water", "droplet"), Map.entry("falling_water", "droplet"), Map.entry("dripping_dripstone_water", "droplet"),
            Map.entry("falling_dripstone_water", "droplet"), Map.entry("spit", "droplet"), Map.entry("dripping_honey", "droplet"), Map.entry("falling_honey", "droplet"),
            Map.entry("landing_honey", "droplet"), Map.entry("falling_nectar", "droplet"), Map.entry("dripping_obsidian_tear", "droplet"), Map.entry("falling_obsidian_tear", "droplet"),
            Map.entry("landing_obsidian_tear", "droplet"),
            // plants, ash, earth
            Map.entry("cherry_leaves", "petal_cherry"), Map.entry("composter", "leaf_green"), Map.entry("spore_blossom_air", "spore"), Map.entry("falling_spore_blossom", "spore"),
            Map.entry("crimson_spore", "spore"), Map.entry("warped_spore", "spore"), Map.entry("mycelium", "spore"), Map.entry("ash", "ash_flake"), Map.entry("white_ash", "ash_flake"),
            Map.entry("dust_plume", "sand_grain"), Map.entry("dust_pillar", "sand_grain"), Map.entry("falling_dust", "sand_grain"), Map.entry("block", "earth_chip"),
            Map.entry("block_marker", "earth_chip"), Map.entry("item", "earth_chip"), Map.entry("item_slime", "droplet"), Map.entry("item_snowball", "snowflake"), Map.entry("item_cobweb", "thread"),
            Map.entry("egg_crack", "earth_chip"), Map.entry("infested", "spore"),
            // shapes
            Map.entry("explosion", "impact_burst"), Map.entry("explosion_emitter", "impact_burst"), Map.entry("flash", "circle_flash"), Map.entry("sweep_attack", "slash_streak"),
            Map.entry("sonic_boom", "shock_ring"), Map.entry("gust", "wind_swirl"), Map.entry("small_gust", "wind_swirl"), Map.entry("gust_emitter_large", "wind_swirl"),
            Map.entry("gust_emitter_small", "wind_swirl"));

    /** The dye of a tintable replacement whose vanilla particle had a colour of its own (used only while the layer itself has no colour, or white). */
    private static final Map<String, Integer> DYE = Map.ofEntries(
            Map.entry("soul_fire_flame", 0x5EC8FF), Map.entry("happy_villager", 0x6BD66B), Map.entry("heart", 0xFF6B9A), Map.entry("witch", 0xA040FF), Map.entry("enchant", 0xC8B0FF),
            Map.entry("glow_squid_ink", 0x40E0D0), Map.entry("electric_spark", 0xBFEFFF), Map.entry("soul", 0x7FDFFF), Map.entry("sculk_soul", 0x5FB8C0), Map.entry("note", 0xFFD070),
            Map.entry("angry_villager", 0xFF5040), Map.entry("damage_indicator", 0xFF5040), Map.entry("composter", 0x7FBF60));

    /** The vanilla names the table answers for, for the tests. */
    public static java.util.Set<String> vanillaNames() {
        return ALIAS.keySet();
    }

    /** The custom particles the table points at, for the tests. */
    public static java.util.Collection<String> targets() {
        return ALIAS.values();
    }

    /** The path of a token without its namespace ({@code minecraft:flame} gives {@code flame}), trimmed, lower case; "" for null. */
    public static String path(String token) {
        if (token == null) return "";
        int c = token.indexOf(':');
        return (c >= 0 ? token.substring(c + 1) : token).trim().toLowerCase(Locale.ROOT);
    }

    /** True for the tokens of the hue-themed dyed mote: {@code fx}, {@code dust}, empty, null. */
    public static boolean isThemed(String token) {
        String p = path(token);
        return p.isEmpty() || p.equals(FX) || p.equals("dust");
    }

    /**
     * The catalog id of the custom particle a token stands for, or {@code null} for the hue-themed mote ({@link #isThemed}). Never a vanilla particle and never an id that is not in the catalog:
     * a catalog id answers itself, a vanilla name its alias, anything else {@link #FALLBACK}.
     */
    public static String resolve(String token) {
        if (isThemed(token)) return null;
        String p = path(token);
        if (FxCatalog.has(p) && !ALIAS.containsKey(p)) return p;
        String a = ALIAS.get(p);
        if (a != null) return a;
        return FALLBACK;
    }

    /**
     * The colour to dye the replacement with: the layer's own colour when it has one that is not plain white; else the colour the vanilla particle had (a witch is purple, a happy villager
     * green); else the layer colour as it is (-1 = none).
     */
    public static int dye(String token, int layerRgb) {
        int c = layerRgb < 0 ? -1 : layerRgb & 0xFFFFFF;
        if (c >= 0 && c != 0xFFFFFF) return c;
        Integer d = DYE.get(path(token));
        return d != null ? d : c;
    }
}
