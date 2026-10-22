package com.newuniverse.nusmp.spiritcontrol;

import java.util.List;
import java.util.Set;

/**
 * Why: the numbers of every spirit the control plane drives, in one pure place: its id, its grimoire magics, its special abilities (what the old extra keys did) and its small list of combat spells (built from
 * the spells the spirit packages and the element pages already have: Salamander fire bolts, Sylph wind blades, Undine water spheres, Gnome stone shots, the Light beam, the Darkness mark and lance, the Space
 * rift). Base damage is about a starter page (7); a cast also costs mana units, split between the spirit and the user ({@link ManaSplit}). The shape names are VfxShape constants (the self-test checks them).
 */
public final class SpiritCatalog {
    private SpiritCatalog() {}

    /** A catalogue row; also the {@link SpiritInfo} the handles delegate to. */
    public record Entry(String id, String display, Set<String> magics, List<SpiritAbilityDef> abilities, List<SpiritSpellDef> spells) implements SpiritInfo {}

    private static SpiritSpellDef spell(String id, String label, float raw, double units, int cd, double range, SpiritSpellDef.Effect fx, String shape, int color, String sound) {
        return new SpiritSpellDef(id, label, raw, units, cd, range, fx, shape, color, sound);
    }

    private static final int FIRE = 0xFFFF6A1E, WATER = 0xFF4FA8FF, WIND = 0xFF8CFFC2, EARTH = 0xFFB08850, LIGHT = 0xFFFFE08A, DARK = 0xFF5B3A8A, SPACE = 0xFF8A5CFF, ANTI = 0xFF2A0A30;

    public static final List<SpiritSpellDef> FIRE_SPELLS = List.of(
            spell("flame_bolt", "Flame Bolt", 7f, 8, 30, 20, SpiritSpellDef.Effect.IGNITE, "FLAME_TRAIL", FIRE, "fire"),
            spell("fire_ball", "Fire Ball", 12f, 16, 80, 18, SpiritSpellDef.Effect.IGNITE, "FLAMEFX_BURST", FIRE, "fire"));
    public static final List<SpiritSpellDef> WIND_SPELLS = List.of(
            spell("wind_blade", "Wind Blade", 7f, 8, 30, 20, SpiritSpellDef.Effect.NONE, "WIND_SLASH", WIND, "wind"),
            spell("gale_gust", "Gale Gust", 10f, 14, 70, 16, SpiritSpellDef.Effect.KNOCKUP, "WIND_GALE", WIND, "wind"));
    public static final List<SpiritSpellDef> WATER_SPELLS = List.of(
            spell("water_sphere", "Water Sphere", 7f, 8, 30, 20, SpiritSpellDef.Effect.NONE, "WATER_SPLASH", WATER, "water"),
            spell("tidal_splash", "Tidal Splash", 11f, 15, 80, 16, SpiritSpellDef.Effect.SLOW, "WATER_RING", WATER, "water"));
    public static final List<SpiritSpellDef> EARTH_SPELLS = List.of(
            spell("stone_shot", "Stone Shot", 8f, 9, 35, 18, SpiritSpellDef.Effect.NONE, "EARTH_SPIKES", EARTH, "earth"),
            spell("earth_spikes", "Earth Spikes", 12f, 16, 90, 14, SpiritSpellDef.Effect.SLOW, "EARTHFAM_BURST", EARTH, "earth"));
    public static final List<SpiritSpellDef> LIGHT_SPELLS = List.of(
            spell("light_beam", "Light Beam", 8f, 9, 30, 22, SpiritSpellDef.Effect.NONE, "LIGHT_BLADE", LIGHT, "light"),
            spell("glow_mark", "Glow Mark", 5f, 10, 70, 20, SpiritSpellDef.Effect.GLOW, "LIGHT_FLARE", LIGHT, "light"));
    public static final List<SpiritSpellDef> DARK_SPELLS = List.of(
            spell("shadow_mark", "Shadow Mark", 5f, 10, 60, 20, SpiritSpellDef.Effect.WEAKEN, "DARKFX_BURST", DARK, "dark"),
            spell("shadow_lance", "Shadow Lance", 9f, 11, 35, 20, SpiritSpellDef.Effect.NONE, "DARK_THRUST", DARK, "dark"));
    public static final List<SpiritSpellDef> SPACE_SPELLS = List.of(
            spell("rift_cut", "Rift Cut", 8f, 10, 40, 20, SpiritSpellDef.Effect.SLOW, "SPATIAL_RIFT", SPACE, "space"),
            spell("gravity_tug", "Gravity Tug", 10f, 14, 80, 16, SpiritSpellDef.Effect.KNOCKUP, "SPATIAL_RIFT", SPACE, "space"));
    public static final List<SpiritSpellDef> ANTI_SPELLS = List.of(
            spell("null_slash", "Null Slash", 8f, 9, 40, 18, SpiritSpellDef.Effect.NONE, "ANTI_MAGIC_SLASH", ANTI, "dark"));

    public static final Entry SALAMANDER = new Entry("salamander", "Salamander", Set.of("FLAME"),
            List.of(new SpiritAbilityDef("dragon", "Salamander Dragon", 6.0)), FIRE_SPELLS);
    public static final Entry SYLPH = new Entry("sylph", "Sylph", Set.of("WIND"),
            List.of(new SpiritAbilityDef("powered", "Sylph Powered Form", 6.0)), WIND_SPELLS);
    public static final Entry UNDINE = new Entry("undine", "Undine", Set.of("WATER"),
            List.of(new SpiritAbilityDef("full", "Undine Full Form", 6.0)), WATER_SPELLS);
    public static final Entry GNOME = new Entry("gnome", "Gnome", Set.of("EARTH"),
            List.of(new SpiritAbilityDef("giant", "Gnome Giant", 6.0)), EARTH_SPELLS);
    public static final Entry LIGHT_SPIRIT = new Entry("spirit_light", "Spirit of Light", Set.of("LIGHT"),
            List.of(new SpiritAbilityDef("beam", "Light Beam", 3.0), new SpiritAbilityDef("pulse", "Glowing Pulse", 4.0), new SpiritAbilityDef("power", "Seraph Form", 5.0)), LIGHT_SPELLS);
    public static final Entry DARK_SPIRIT = new Entry("spirit_dark", "Spirit of Darkness", Set.of("DARK"),
            List.of(new SpiritAbilityDef("step", "Shadow Step", 3.0), new SpiritAbilityDef("power", "Demon Form", 5.0)), DARK_SPELLS);
    public static final Entry SPACE_SPIRIT = new Entry("spirit_space", "Spirit of Space", Set.of("SPATIAL"),
            List.of(new SpiritAbilityDef("portal", "Spirit Portal", 0.0, 0, true), new SpiritAbilityDef("gravity", "Gravity Pulse", 0.0),
                    new SpiritAbilityDef("cut", "Spatial Cut", 0.0), new SpiritAbilityDef("set", "Mark Waypoint", 0.0, 0, true)), SPACE_SPELLS);
    /** The old generic Spirit Lord (the orb of the Spirit Lord skill and the true bodies of the Spirit Charm); its spells follow the element of the bond. */
    public static final Entry LORD = new Entry("lord", "Spirit Lord", Set.of("FLAME", "WATER", "WIND", "EARTH", "ANTI_MAGIC"), List.of(), FIRE_SPELLS);

    /** The spells of the Spirit Lord of this bond type (Salamander, Undine, Sylph, Gnome, Anti); anything else casts the fire list. */
    public static List<SpiritSpellDef> lordSpells(String type) {
        if (type == null) return FIRE_SPELLS;
        return switch (type.toLowerCase(java.util.Locale.ROOT)) {
            case "undine" -> WATER_SPELLS;
            case "sylph" -> WIND_SPELLS;
            case "gnome" -> EARTH_SPELLS;
            case "anti", "anti-magic" -> ANTI_SPELLS;
            default -> FIRE_SPELLS;
        };
    }

    /** The grimoire magic a Spirit Lord of this bond type belongs to, or null. */
    public static String lordMagic(String type) {
        if (type == null) return null;
        return switch (type.toLowerCase(java.util.Locale.ROOT)) {
            case "salamander" -> "FLAME";
            case "undine" -> "WATER";
            case "sylph" -> "WIND";
            case "gnome" -> "EARTH";
            case "anti", "anti-magic" -> "ANTI_MAGIC";
            default -> null;
        };
    }

    public static List<Entry> all() { return List.of(SALAMANDER, SYLPH, UNDINE, GNOME, LIGHT_SPIRIT, DARK_SPIRIT, SPACE_SPIRIT, LORD); }
}
