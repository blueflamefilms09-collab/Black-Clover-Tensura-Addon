package com.newuniverse.nusmp.relics.upgrade;

import java.util.Locale;
import java.util.Map;

/**
 * Why: the effect tables of the upgraded relics ({@code assets/nusmp/visual_upgrade/<id>.json}, {@link UpgradeFxData}) name their particles with the old vanilla ids ({@code minecraft:end_rod},
 * {@code minecraft:flame}, ...) or {@code "dust"}. Since 1.108.0 those never reach the game as vanilla particles any more: the client layers ({@code client/relics/fx/UpgradeFxClient}) and the act
 * bursts ({@link UpgradeActFx}) turn every name into the mod's own custom particle through {@code particle.fx.Fx} / {@code FxRelic}. This pure class (no game types, so a test can check it
 * against the shipped tables) holds the one decision: a data name becomes the name of an {@code FxOld} constant (the slot of the theme that replaces that vanilla particle), or {@code null} for
 * "dust" (a custom mote dyed with the layer's colour). Ids of other namespaces are the mod's own particle types and are looked up in the registry by the callers.
 */
public final class UpgradeParticleNames {
    private UpgradeParticleNames() {}

    /** Names that have no constant of their own in {@code FxOld}: the constant whose slot says the same thing (a note is a glyph, a dolphin spray is a bubble, ...). */
    private static final Map<String, String> ALIAS = Map.ofEntries(
            Map.entry("wax_on", "GLOW"), Map.entry("wax_off", "GLOW"), Map.entry("scrape", "GLOW"),
            Map.entry("glow_squid_ink", "SQUID_INK"),
            Map.entry("note", "ENCHANT"), Map.entry("dragon_breath", "PORTAL"), Map.entry("nautilus", "BUBBLE"), Map.entry("dolphin", "BUBBLE"),
            Map.entry("angry_villager", "DAMAGE_INDICATOR"), Map.entry("totem_of_undying", "FIREWORK"), Map.entry("spore_blossom_air", "HAPPY_VILLAGER"),
            Map.entry("cherry_leaves", "HEART"), Map.entry("dripping_water", "CURRENT_DOWN"), Map.entry("falling_water", "CURRENT_DOWN"), Map.entry("rain", "CURRENT_DOWN"),
            Map.entry("white_ash", "ASH"), Map.entry("campfire_signal_smoke", "CAMPFIRE_COSY_SMOKE"), Map.entry("trial_spawner_detection", "ENCHANT"));

    /** What an unknown vanilla name becomes: the signature mote of the theme (the old end rod). */
    public static final String DEFAULT = "END_ROD";

    /** The path of a particle name without its namespace ({@code minecraft:flame} gives {@code flame}). */
    public static String path(String particle) {
        if (particle == null) return "";
        int c = particle.indexOf(':');
        return (c >= 0 ? particle.substring(c + 1) : particle).trim().toLowerCase(Locale.ROOT);
    }

    /** True for a bare name or one in the {@code minecraft} namespace: the old vanilla names. */
    public static boolean isVanilla(String particle) {
        if (particle == null) return true;
        int c = particle.indexOf(':');
        return c < 0 || particle.substring(0, c).trim().equalsIgnoreCase("minecraft");
    }

    /** True for "dust", an empty name or none: a custom mote dyed with the layer's colour. */
    public static boolean isDust(String particle) {
        String p = path(particle);
        return p.isEmpty() || p.equals("dust");
    }

    /**
     * The name of the {@code FxOld} constant that replaces a vanilla particle name (see the alias table; an unknown name gives {@link #DEFAULT}); {@code null} for "dust". The callers
     * pass only vanilla names here ({@link #isVanilla}); a name of another namespace is the mod's own type.
     */
    public static String oldName(String particle) {
        if (isDust(particle)) return null;
        String p = path(particle);
        return ALIAS.getOrDefault(p, p.toUpperCase(Locale.ROOT));
    }
}
