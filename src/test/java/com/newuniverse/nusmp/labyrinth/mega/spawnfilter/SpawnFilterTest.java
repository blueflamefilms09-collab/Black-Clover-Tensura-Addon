package com.newuniverse.nusmp.labyrinth.mega.spawnfilter;

import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The blacklist parser, the themed palette, the combined verdict, and the shipped data: the Gorgon is banned and in no dungeon tag, and the palettes made
 * from the shipped faction files keep drowned things out of a dry crypt. Pure (java.nio and regular expressions); working directory = project root.
 */
class SpawnFilterTest {
    private static final Path DATA = Path.of("src/main/resources/data/nusmp");
    private static final String GORGON = "iceandfire:gorgon";

    // ---------------------------------------------------------------- the blacklist
    @Test
    void theDefaultListNamesTheGorgonAndNothingElse() {
        assertEquals(List.of(GORGON), SpawnBlacklist.DEFAULT);
        SpawnBlacklist b = SpawnBlacklist.parse(SpawnBlacklist.DEFAULT);
        assertTrue(b.matches(GORGON));
        assertFalse(b.matches("iceandfire:fire_dragon"));
        assertFalse(b.matches("minecraft:zombie"));
        assertEquals(1, b.size());
    }

    @Test
    void entriesMatchExactlyByNamespaceAndByBarePath() {
        SpawnBlacklist b = SpawnBlacklist.parse(List.of("tensura:*", "Lich", " IceAndFire:Gorgon "));
        assertTrue(b.matches("tensura:orc"));
        assertTrue(b.matches("tensura:slime"));
        assertTrue(b.matches("othermod:lich"), "a bare path matches in any namespace");
        assertTrue(b.matches("ICEANDFIRE:GORGON"), "case does not matter");
        assertFalse(b.matches("iceandfire:hydra"));
        assertFalse(b.matches("othermod:liches"));
    }

    @Test
    void junkIsSkippedNeverAnError() {
        SpawnBlacklist b = SpawnBlacklist.parse(java.util.Arrays.asList("", "   ", "# a comment", "has space", "bad:ent ry", ":", "a:", ":b", 7, null, "ok:fine"));
        assertEquals(1, b.size());
        assertTrue(b.matches("ok:fine"));
        assertFalse(b.matches(null));
        assertFalse(b.matches(""));
        assertTrue(SpawnBlacklist.parse(null).isEmpty());
        assertTrue(SpawnBlacklist.none().isEmpty());
        assertFalse(SpawnBlacklist.none().matches(GORGON));
    }

    // ---------------------------------------------------------------- the palette
    private static Map<String, Double> weights(Object... pairs) {
        Map<String, Double> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) m.put((String) pairs[i], ((Number) pairs[i + 1]).doubleValue());
        return m;
    }

    @Test
    void aWeightOfZeroKeepsAFactionOutAndAnyOtherWeightLetsItIn() {
        FloorThemeMobFilter.Palette p = FloorThemeMobFilter.palette("necropolis", weights("aquatic", 0, "undead", 5, "goblin", 0.5), 0.0);
        assertFalse(p.open());
        assertFalse(p.allows("aquatic"));
        assertTrue(p.allows("undead"));
        assertTrue(p.allows("goblin"));
        assertTrue(p.allows(null), "a creature with no faction is never filtered");
        assertFalse(p.allows("some_unknown_faction"), "a faction the floor does not list is out of place");
    }

    @Test
    void theMinimumWeightRaisesTheBar() {
        Map<String, Double> w = weights("aquatic", 0, "undead", 5, "goblin", 0.5, "beast", 1);
        FloorThemeMobFilter.Palette p = FloorThemeMobFilter.palette("x", w, 1.0);
        assertTrue(p.allows("undead"));
        assertTrue(p.allows("beast"));
        assertFalse(p.allows("goblin"));
        assertFalse(p.allows("aquatic"));
    }

    @Test
    void aBrokenOrUnknownThemeFiltersNothing() {
        assertTrue(FloorThemeMobFilter.palette("x", weights("a", 0, "b", 0), 0).open(), "every faction at 0 must not empty a floor");
        assertTrue(FloorThemeMobFilter.palette("x", weights("a", 1), 5.0).open(), "nobody reaches the bar");
        assertTrue(FloorThemeMobFilter.palette(null, weights("a", 1), 0).open());
        assertTrue(FloorThemeMobFilter.palette("", weights("a", 1), 0).open());
        assertTrue(FloorThemeMobFilter.palette("x", Map.of(), 0).open());
        assertTrue(FloorThemeMobFilter.palette("x", null, 0).open());
        assertTrue(FloorThemeMobFilter.allows(null, "aquatic"), "no palette = no filter");
        assertTrue(FloorThemeMobFilter.Palette.open("x").allows("aquatic"));
    }

    // ---------------------------------------------------------------- the verdict
    private static final FloorThemeMobFilter.Palette DRY = FloorThemeMobFilter.palette("necropolis", weights("aquatic", 0, "undead", 5), 0.0);

    private static SpawnFilterRules.Verdict judge(boolean banned, boolean admin, boolean bypass, boolean exempt, boolean fresh, boolean themeOn, String faction) {
        return SpawnFilterRules.judge(banned, admin, bypass, exempt, fresh, themeOn, DRY, faction);
    }

    @Test
    void theBanIsAbsoluteExceptForAnOperatorWhenTheConfigSaysSo() {
        assertEquals(SpawnFilterRules.Verdict.DENY_BANNED, judge(true, false, false, false, true, true, null));
        assertEquals(SpawnFilterRules.Verdict.DENY_BANNED, judge(true, false, true, true, false, false, null), "loaded from a save, exempt, theme off: still banned");
        assertEquals(SpawnFilterRules.Verdict.DENY_BANNED, judge(true, true, false, false, true, true, null), "an operator without the bypass: never means never");
        assertEquals(SpawnFilterRules.Verdict.ALLOW, judge(true, true, true, false, true, true, null), "an operator with the bypass");
        assertEquals(SpawnFilterRules.Verdict.DENY_BANNED, judge(true, false, true, false, true, true, null), "the bypass is for operators only");
    }

    @Test
    void theThemeFilterOnlyTouchesAFreshFactionSpawnThatDoesNotBelong() {
        assertEquals(SpawnFilterRules.Verdict.DENY_THEME, judge(false, false, false, false, true, true, "aquatic"));
        assertEquals(SpawnFilterRules.Verdict.ALLOW, judge(false, false, false, false, true, true, "undead"));
        assertEquals(SpawnFilterRules.Verdict.ALLOW, judge(false, false, false, false, true, true, null), "no faction");
        assertEquals(SpawnFilterRules.Verdict.ALLOW, judge(false, false, false, false, true, false, "aquatic"), "switch off");
        assertEquals(SpawnFilterRules.Verdict.ALLOW, judge(false, true, false, false, true, true, "aquatic"), "an operator's creature");
        assertEquals(SpawnFilterRules.Verdict.ALLOW, judge(false, false, false, true, true, true, "aquatic"), "owned, named or a boss");
        assertEquals(SpawnFilterRules.Verdict.ALLOW, judge(false, false, false, false, false, true, "aquatic"), "loaded from a save");
        assertEquals(SpawnFilterRules.Verdict.ALLOW, SpawnFilterRules.judge(false, false, false, false, true, true, null, "aquatic"), "no floor, no palette");
    }

    // ---------------------------------------------------------------- the shipped data
    private static String read(Path p) throws IOException { return Files.readString(p, StandardCharsets.UTF_8); }

    @Test
    void theGorgonIsInNoDungeonTierNorFactionTagAndIsBanned() throws IOException {
        Path tags = DATA.resolve("tags/entity_type");
        for (String t : List.of("dungeon/tier1.json", "dungeon/tier2.json", "dungeon/tier3.json", "dungeon/legendary.json", "dungeon/grazers.json")) {
            assertFalse(read(tags.resolve(t)).contains(GORGON), t + " still lists the Gorgon");
        }
        try (Stream<Path> faction = Files.list(tags.resolve("mega"))) {
            for (Path f : (Iterable<Path>) faction::iterator) assertFalse(read(f).contains(GORGON), f + " still lists the Gorgon");
        }
        assertTrue(read(tags.resolve("dungeon/banned.json")).contains(GORGON), "nusmp:dungeon/banned must name the Gorgon");
        assertTrue(read(tags.resolve("dungeon/excluded.json")).contains(GORGON), "the veto tag keeps the Gorgon out of the tiers and the encounters");
    }

    private static final Pattern WEIGHT = Pattern.compile("\"([a-z_]+)\"\\s*:\\s*([0-9]+(?:\\.[0-9]+)?)");

    /** The theme weights of a faction file, read from its themeWeights block. */
    private static Map<String, Double> themeWeights(String faction) throws IOException {
        String text = read(DATA.resolve("mega/factions/" + faction + ".json"));
        int from = text.indexOf("\"themeWeights\"");
        assertTrue(from >= 0, faction + " has no themeWeights");
        int open = text.indexOf('{', from), close = text.indexOf('}', open);
        Matcher m = WEIGHT.matcher(text.substring(open, close));
        Map<String, Double> out = new LinkedHashMap<>();
        while (m.find()) out.put(m.group(1), Double.parseDouble(m.group(2)));
        return out;
    }

    @Test
    void thePalettesOfTheShippedThemesKeepTheDrownedOutOfTheDryAndInTheWet() throws IOException {
        List<String> factions = List.of("aquatic", "beast", "construct", "cultist", "demon", "elemental", "goblin", "insect", "ogre", "slime", "undead");
        Map<String, Map<String, Double>> byFaction = new LinkedHashMap<>();
        for (String f : factions) byFaction.put(f, themeWeights(f));
        List<String> problems = new ArrayList<>();
        for (RegionTheme t : RegionTheme.values()) {
            Map<String, Double> w = new LinkedHashMap<>();
            for (String f : factions) w.put(f, byFaction.get(f).getOrDefault(t.id(), 1.0));
            FloorThemeMobFilter.Palette p = FloorThemeMobFilter.palette(t.id(), w, 0.0);
            if (p.open()) problems.add(t.id() + ": no palette (every faction at 0?)");
            else if (p.allowed().size() < 2) problems.add(t.id() + ": only " + p.allowed() + " is native, a floor needs a choice");
        }
        assertTrue(problems.isEmpty(), String.join("; ", problems));
        Map<String, Double> necro = new LinkedHashMap<>(), temple = new LinkedHashMap<>();
        for (String f : factions) {
            necro.put(f, byFaction.get(f).getOrDefault("necropolis", 1.0));
            temple.put(f, byFaction.get(f).getOrDefault("sunken_temple", 1.0));
        }
        assertFalse(FloorThemeMobFilter.palette("necropolis", necro, 0.0).allows("aquatic"), "drowned things in a dry crypt");
        assertTrue(FloorThemeMobFilter.palette("necropolis", necro, 0.0).allows("undead"));
        assertTrue(FloorThemeMobFilter.palette("sunken_temple", temple, 0.0).allows("aquatic"));
    }
}
