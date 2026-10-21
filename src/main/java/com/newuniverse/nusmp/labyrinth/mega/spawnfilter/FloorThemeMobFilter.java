package com.newuniverse.nusmp.labyrinth.mega.spawnfilter;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/**
 * Why: a floor of the Mega-Dungeon has a Region Theme (a Necropolis, a Sunken Temple, a Spider Warren ...), and a creature that does not belong to it breaks
 * the picture: drowned things walking a dry crypt, slimes in a forge of fire. The shipped data already says who belongs where: every faction file
 * ({@code data/nusmp/mega/factions/*.json}) lists a weight per theme, and a weight of 0 means "this faction is never native here". This class is the PALETTE
 * of one theme made from those weights: the factions whose weight is above zero and at least the configured minimum are the floor's palette, and a creature
 * of any other faction is out of place. A creature that belongs to no faction (a vanilla zombie, a one-off boss) is never filtered, and a theme whose palette
 * would be empty (a broken data set: every faction at 0) filters nothing, because a datapack typo must never empty a floor. Pure: the game glue only feeds
 * it the weights it reads from the loaded factions.
 */
public final class FloorThemeMobFilter {
    private FloorThemeMobFilter() {}

    /** The palette of one theme: the factions allowed on it, or "open" (everything is allowed). */
    public record Palette(String themeId, Set<String> allowed, boolean open) {
        public Palette {
            allowed = Collections.unmodifiableSet(new LinkedHashSet<>(allowed));
        }

        /** The palette that allows everything (no theme known, no data, or a broken data set). */
        public static Palette open(String themeId) { return new Palette(themeId == null ? "" : themeId, Set.of(), true); }

        /** Is a creature of this faction (null = no faction) at home on this floor? */
        public boolean allows(String factionId) { return open || factionId == null || allowed.contains(factionId); }
    }

    /**
     * Builds the palette of a theme from the weight each faction gives it. A faction is native when its weight is above zero AND at least {@code minWeight}
     * (0 = every faction with any weight; 1 = only those that like the theme at least as much as the neutral default). An unknown theme (null or empty), no
     * factions at all, or no faction reaching the bar gives the open palette.
     */
    public static Palette palette(String themeId, Map<String, Double> weightByFaction, double minWeight) {
        if (themeId == null || themeId.isEmpty() || weightByFaction == null || weightByFaction.isEmpty()) return Palette.open(themeId);
        double bar = Double.isNaN(minWeight) ? 0.0 : Math.max(0.0, minWeight);
        Set<String> home = new LinkedHashSet<>();
        for (Map.Entry<String, Double> e : weightByFaction.entrySet()) {
            double w = e.getValue() == null ? 0.0 : e.getValue();
            if (w > 0.0 && w >= bar) home.add(e.getKey());
        }
        return home.isEmpty() ? Palette.open(themeId) : new Palette(themeId, home, false);
    }

    /** The decision for one creature: false = it does not belong on this floor and its spawn is refused. */
    public static boolean allows(Palette palette, String factionId) { return palette == null || palette.allows(factionId); }
}
