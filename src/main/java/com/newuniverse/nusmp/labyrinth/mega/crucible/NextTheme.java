package com.newuniverse.nusmp.labyrinth.mega.crucible;

import com.newuniverse.nusmp.labyrinth.mega.MegaData;
import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import com.newuniverse.nusmp.labyrinth.mega.Sector;
import com.newuniverse.nusmp.labyrinth.mega.ThemeOverride;
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
 * Why: the relic a party chooses in a sector's Crucible decides the theme (and the ruleset) of the NEXT sector, "the next ten floors", and the generator that
 * builds the next sector needs one question answered: "what comes after this one?". This is that answer. It is registered as the {@link ThemeOverride} of the
 * Mega-Dungeon ({@code MegaDungeon.setThemeOverride}, done by {@code ThemeRegistry.init}), so the generator asks it through the contract without importing
 * this package. It answers null while the vault has not been decided (UNOPENED or ROLLED: the party has not chosen, so there is nothing to decide by and the
 * seeded draw applies), and from the moment a relic is taken (CHOSEN, and RESOLVED after the catastrophe) the {@link RelicThemes.Road} of that relic: its theme
 * and its ruleset id, by the explicit table ({@code relic_themes.json}), the themes' own keywords and the card name of the relic as the catalogue knows it. The
 * answer is a pure function of the saved sector, so it is the same before and after a restart and the same on every call.
 * <p>
 * The ruleset id needs no field of its own in the sector: the relic that was taken is saved in the Crucible of the region it was taken in, so the ruleset of
 * the NEXT region is derived from that ({@link #rulesetOf}), whenever anybody asks, for as long as the world exists. It is reported only when the region
 * really wears the theme the relic chose (the generator may have built it before the choice was made, by the seeded draw: the relic then shaped nothing).
 * <p>
 * A vault that was RESOLVED empty (the world had no relic of those grades left, so nothing was ever chosen) has no relic to read, so the sector itself
 * stands in: the theme comes from the hash of the sector's id and seed, which still never lands on the baseline and still never changes. A Crucible that
 * is switched off ({@code crucibleEnabled = false}) never leaves UNOPENED, so the planner must not wait for it then. The answer may equal the previous
 * sector's own theme (a Frost relic taken in a Frost sector): the relic's identity decides, and {@code RegionTheme.pick}'s never-twice rule is not applied here.
 */
public final class NextTheme {
    private NextTheme() {}

    /**
     * The override the Mega-Dungeon asks. {@link #themeAfter} is the contract; {@link #rulesetAfter} is the same question for the ruleset, written as a plain
     * public method so that it becomes the implementation of a {@code rulesetAfter(Sector)} default method the day the contract's interface gains one (see the
     * theme documentation, "what the contract could add"), and does no harm until then.
     */
    private static final class Hook implements ThemeOverride {
        @Override
        public RegionTheme themeAfter(Sector previous) { return nextThemeFor(previous); }

        /** The ruleset id of the sector after {@code previous}, or null when its Crucible has not been decided. */
        public String rulesetAfter(Sector previous) { return nextRulesetFor(previous); }
    }

    /** The one instance {@code ThemeRegistry.init} hands to {@code MegaDungeon.setThemeOverride}. */
    public static final ThemeOverride HOOK = new Hook();

    /**
     * The road the sector after {@code previous} takes, or null when its Crucible has not been decided yet (or {@code previous} is null). Never throws.
     */
    public static RelicThemes.Road roadAfter(Sector previous) {
        if (previous == null) return null;
        Phase phase = Phase.of(previous.crucible.phase.name());
        if (phase != Phase.CHOSEN && phase != Phase.RESOLVED) return null;
        String chosen = previous.crucible.chosen == null ? "" : previous.crucible.chosen.trim();
        if (chosen.isEmpty()) {
            RegionTheme t = RelicThemes.themeOf("empty_vault_" + previous.id + "_" + Long.toHexString(previous.seed), "", Map.of(), Map.of());
            return new RelicThemes.Road(t, t.id());
        }
        return roadOfRelic(chosen);
    }

    /** The road a relic leads to (what the offer shows and what the pick decides), by the loaded tables. */
    public static RelicThemes.Road roadOfRelic(String relicId) {
        return RelicThemes.roadOf(relicId, displayName(relicId), RelicThemeMap.explicit(), RelicThemeMap.rulesets(), keywords());
    }

    /** The theme of the sector that follows {@code previous}, or null when its Crucible has not been decided yet (or {@code previous} is null). Never throws. */
    public static RegionTheme nextThemeFor(Sector previous) {
        RelicThemes.Road road = roadAfter(previous);
        return road == null ? null : road.theme();
    }

    /** The ruleset id of the sector that follows {@code previous}, or null when its Crucible has not been decided yet (or {@code previous} is null). Never throws. */
    public static String nextRulesetFor(Sector previous) {
        RelicThemes.Road road = roadAfter(previous);
        return road == null ? null : road.ruleset();
    }

    /**
     * The ruleset of a sector, from saved data alone: the road the relic taken in the previous sector of its chain chose, when this sector wears the theme
     * that road names; otherwise "" (the first sector of a chain, a sector built before the choice, a vault nobody decided). "" means "the theme's own rules".
     * Other systems (the encounter director, the factions) may call this to key their data on a relic's road without any change to {@code Sector}.
     */
    public static String rulesetOf(MegaData data, Sector sector) {
        if (data == null || sector == null || sector.chainIndex <= 0) return "";
        Sector previous = data.chainSector(sector.party, sector.polarity, sector.chainIndex - 1);       // the floor before in the SAME party's chain (1.58.0: every party has chains of its own)
        RelicThemes.Road road = roadAfter(previous);
        return road != null && road.theme() == sector.theme ? road.ruleset() : "";
    }

    /** A road as a player reads it: "Frost", or "Frost: The Long Winter" when the ruleset has a name of its own; used by the offer and the pick messages and the status command. */
    public static String describe(RelicThemes.Road road) {
        if (road == null) return "";
        String theme = ThemeDefs.get(road.theme()).name();
        String rules = RelicThemeMap.rulesetName(road.ruleset());
        return rules == null || rules.isEmpty() || rules.equals(theme) ? theme : theme + ": " + rules;
    }

    /** The name of a ruleset as a player reads it: the file's name for it, else the theme's own name ("Frost" for the ruleset {@code frost}). */
    public static String rulesetName(String rulesetId, RegionTheme theme) {
        String named = RelicThemeMap.rulesetName(rulesetId);
        if (named != null && !named.isEmpty()) return named;
        return ThemeDefs.get(theme).name();
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
