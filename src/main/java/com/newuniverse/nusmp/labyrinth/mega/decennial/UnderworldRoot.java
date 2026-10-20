package com.newuniverse.nusmp.labyrinth.mega.decennial;

import java.util.List;
import java.util.Map;

/**
 * Why: the owner's Tree of Qliphoth canon says the Underworld is a ROOT that grows deeper through seven gates, and that every gate leaves the floors below it darker and more rooted than the gate
 * before. The Evil half of the Decennial roster is cut into those seven bands ({@link DecennialRules#bandOf}); the boss halls of the gates and the throne are built by the arena themes; and THIS class
 * is the part of the story that reaches the ordinary floors. The mega-dungeon already asks one question when it starts a new decade of floors: "which theme must the floors after this boss wear?"
 * ({@code MegaDungeon.themeOverride}). The relic taken in a Crucible answers it first; when it has no opinion, {@link DecennialThemes} answers for the Abyssal road with this table: past the first
 * gate, the next ten floors wear one of the existing dark and overgrown themes (no new theme is added), and the deeper the band the darker the pair of themes it draws from. The answer depends on the
 * floor of the boss just beaten alone, so it is the same on every server and in every party's chain. This part is pure (theme ids as text) so a test can pin it.
 */
public final class UnderworldRoot {
    private UnderworldRoot() {}

    /** The pair of existing themes each band (2..7) wears after its gate: rooted and overgrown first, darker and deeper in the later bands. Band 1 has no entry: the first fourteen decades are the ordinary Abyss. */
    private static final Map<Integer, List<String>> BAND_THEMES = Map.of(
            2, List.of("verdant", "shadow"),
            3, List.of("fungal_underdark", "verdant"),
            4, List.of("shadow", "fungal_underdark"),
            5, List.of("abyss", "shadow"),
            6, List.of("shadow", "abyss"),
            7, List.of("abyss", "fungal_underdark"));

    /** Every theme id the table can answer (a test checks each one is a real region theme). */
    public static List<String> allThemeIds() {
        return BAND_THEMES.values().stream().flatMap(List::stream).distinct().toList();
    }

    /**
     * The theme id for the floors after the boss floor {@code lastFloor} (a signed floor number), or null when the story has no opinion: not an Abyssal boss floor, not a tenth floor, or the next
     * level is still in the first band.
     */
    public static String themeIdAfter(int lastFloor) {
        if (lastFloor >= 0) return null;
        int level = DecennialRules.levelOfFloor(lastFloor);
        if (level >= 0) return null;                                    // not a boss floor (levelOfFloor answers 0)
        int depth = -level;
        int next = Math.min(DecennialRules.LEVELS_PER_SIDE, depth + 1);
        int band = DecennialRules.bandOf(next);
        List<String> pair = BAND_THEMES.get(band);
        if (pair == null) return null;
        return pair.get(depth & 1);
    }
}
