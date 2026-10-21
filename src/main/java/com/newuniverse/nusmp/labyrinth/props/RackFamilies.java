package com.newuniverse.nusmp.labyrinth.props;

import java.util.Locale;
import java.util.Map;

/**
 * Why: the loot-filled racks are themed: the rack in a forge hall shows hammers and axes, the one in a crypt shows rusted blades and bone, the one in a church shows blessed steel. There are
 * 27 region themes and six loot tables ({@code data/nusmp/loot_table/racks/<family>.json}); this class is the mapping between them, so a new theme added later falls into {@link #PLAIN} (a table
 * of ordinary gear) instead of into nothing. Pure: the theme id is the lower-case id of {@code RegionTheme}.
 */
public final class RackFamilies {
    private RackFamilies() {}

    public static final String PLAIN = "plain", ARMOURY = "armoury", CRYPT = "crypt", SACRED = "sacred", ARCANE = "arcane", WILD = "wild";
    public static final String[] ALL = {PLAIN, ARMOURY, CRYPT, SACRED, ARCANE, WILD};

    private static final Map<String, String> BY_THEME = Map.ofEntries(
            Map.entry("gravity", ARMOURY), Map.entry("dwarven_forgehold", ARMOURY), Map.entry("infernal_foundry", ARMOURY), Map.entry("flame", ARMOURY),
            Map.entry("storm", ARMOURY), Map.entry("mountain_monastery", ARMOURY),
            Map.entry("necropolis", CRYPT), Map.entry("drowned_crypt", CRYPT), Map.entry("vampire_castle", CRYPT), Map.entry("blood", CRYPT),
            Map.entry("abyss", CRYPT), Map.entry("shadow", CRYPT), Map.entry("haunted_manor", CRYPT),
            Map.entry("holy_church", SACRED), Map.entry("celestial_court", SACRED), Map.entry("radiant", SACRED), Map.entry("sky_citadel", SACRED),
            Map.entry("sunken_temple", SACRED),
            Map.entry("cursed_archive", ARCANE), Map.entry("spatial", ARCANE), Map.entry("astral", ARCANE), Map.entry("crystal_geode", ARCANE),
            Map.entry("feywild_grove", ARCANE),
            Map.entry("spider_warrens", WILD), Map.entry("fungal_underdark", WILD), Map.entry("verdant", WILD), Map.entry("frost", WILD));

    /** The family of a theme id; any unknown or empty id is {@link #PLAIN}. */
    public static String family(String themeId) {
        if (themeId == null) return PLAIN;
        String f = BY_THEME.get(themeId.trim().toLowerCase(Locale.ROOT));
        return f == null ? PLAIN : f;
    }

    /** The path of the family's loot table under the mod's namespace. */
    public static String tablePath(String family) { return "racks/" + (family == null || family.isEmpty() ? PLAIN : family); }

    public static boolean known(String themeId) { return themeId != null && BY_THEME.containsKey(themeId.trim().toLowerCase(Locale.ROOT)); }
}
