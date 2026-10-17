package com.newuniverse.nusmp.labyrinth.mega.crucible;

import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.crucible.CrucibleRules.Phase;
import com.newuniverse.nusmp.labyrinth.mega.theme.RelicThemeMap;
import com.newuniverse.nusmp.labyrinth.mega.theme.RelicThemes;
import com.newuniverse.nusmp.labyrinth.mega.theme.ThemeDefs;
import com.newuniverse.nusmp.relics.RelicDef;
import com.newuniverse.nusmp.relics.Relics;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the relic a party chooses in a sector's Crucible decides the theme of the NEXT sector, and the planner that generates the next sector needs one
 * question answered: "what theme comes after this one?". This is that answer, for the planner to call (the lead wires it in): null while the vault has
 * not been decided (UNOPENED or ROLLED: the party has not chosen, so there is nothing to decide by and the next sector must wait), and from the moment
 * a relic is taken (CHOSEN, and RESOLVED after the catastrophe) the theme of that relic by {@link RelicThemes#themeOf}, with the explicit table
 * ({@code relic_themes.json}), the themes' own keywords and the card name of the relic as the catalogue knows it. The answer is a pure function of the
 * saved sector, so it is the same before and after a restart and the same on every call.
 * <p>
 * A vault that was RESOLVED empty (the world had no relic of those grades left, so nothing was ever chosen) has no relic to read, so the sector itself
 * stands in: the theme comes from the hash of the sector's id and seed, which still never lands on the baseline and still never changes. A Crucible that
 * is switched off ({@code crucibleEnabled = false}) never leaves UNOPENED, so the planner must not wait for it then. The answer may equal the previous
 * sector's own theme (a Frost relic taken in a Frost sector): the relic's identity decides, and {@code RegionTheme.pick}'s never-twice rule is not applied here.
 */
public final class NextTheme {
    private NextTheme() {}

    /**
     * The theme of the sector that follows {@code previous}, or null when its Crucible has not been decided yet (or {@code previous} is null).
     * Never throws.
     */
    public static RegionTheme nextThemeFor(Sector previous) {
        if (previous == null) return null;
        Phase phase = Phase.of(previous.crucible.phase.name());
        if (phase != Phase.CHOSEN && phase != Phase.RESOLVED) return null;
        String chosen = previous.crucible.chosen == null ? "" : previous.crucible.chosen.trim();
        if (chosen.isEmpty()) return RelicThemes.themeOf("empty_vault_" + previous.id + "_" + Long.toHexString(previous.seed), "", Map.of(), Map.of());
        return RelicThemes.themeOf(chosen, displayName(chosen), RelicThemeMap.explicit(), keywords());
    }

    /** The card name of a relic as the catalogue knows it, or "" when the catalogue has no such relic (then only the id is read). */
    static String displayName(String relicId) {
        try {
            RelicDef def = Relics.byId(relicId);
            return def == null || def.cardName() == null ? "" : def.cardName();
        } catch (RuntimeException | LinkageError notThere) {
            return "";
        }
    }

    /** Every theme's relic keywords, as the loaded theme files carry them. */
    static Map<RegionTheme, List<String>> keywords() {
        Map<RegionTheme, List<String>> out = new EnumMap<>(RegionTheme.class);
        for (RegionTheme t : RegionTheme.values()) out.put(t, ThemeDefs.get(t).keywords());
        return out;
    }
}
