package com.newuniverse.nusmp.labyrinth.mega.theme;

import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The relic-to-theme mapping, rule by rule: the table always wins, then the theme with the most distinct keywords (a tie to the first theme of the
 * enum), then a stable hash that never lands on the baseline; and that no input, however odd, throws or returns null.
 */
class RelicThemesTest {
    private static Map<RegionTheme, List<String>> kw(Object... pairs) {
        Map<RegionTheme, List<String>> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            @SuppressWarnings("unchecked") List<String> words = (List<String>) pairs[i + 1];
            m.put((RegionTheme) pairs[i], words);
        }
        return m;
    }

    private static Map<String, String> table(String... pairs) {
        Map<String, String> m = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) m.put(pairs[i], pairs[i + 1]);
        return m;
    }

    // ---------------------------------------------------------------- (a) the table
    @Test
    void anExplicitEntryWinsOverKeywordsAndHash() {
        Map<RegionTheme, List<String>> frost = kw(RegionTheme.FROST, List.of("frost"));
        assertEquals(RegionTheme.FROST, RelicThemes.themeOf("frostfang", "Frostfang", Map.of(), frost), "without the table the keyword decides");
        assertEquals(RegionTheme.FLAME, RelicThemes.themeOf("frostfang", "Frostfang", table("frostfang", "flame"), frost), "the table wins over a keyword hit");
        assertEquals(RegionTheme.BLOOD, RelicThemes.themeOf("unheard_of", "", table("unheard_of", "blood"), Map.of()), "and over the hash");
    }

    @Test
    void theRelicKeyIsNormalised() {
        assertEquals("frostfang", RelicThemes.key("  NUSMP:FrostFang "));
        assertEquals("frostfang", RelicThemes.key("frostfang"));
        assertEquals("", RelicThemes.key(null));
        assertEquals("", RelicThemes.key("  "));
        assertEquals(RegionTheme.BLOOD, RelicThemes.themeOf("NUSMP:Frostfang", "", table("frostfang", "blood"), Map.of()), "the id may carry a namespace and capitals");
        assertEquals(RegionTheme.BLOOD, RelicThemes.themeOf("frostfang", "", table("Nusmp:FrostFang", "BLOOD"), Map.of()), "and so may the table's key and value");
    }

    @Test
    void anEntryThatNamesNoThemeIsIgnored() {
        Map<RegionTheme, List<String>> frost = kw(RegionTheme.FROST, List.of("frost"));
        assertEquals(RegionTheme.FROST, RelicThemes.themeOf("frostfang", "Frostfang", table("frostfang", "nonsense"), frost));
        assertEquals(RegionTheme.FROST, RelicThemes.themeOf("frostfang", "Frostfang", table("frostfang", ""), frost));
        Map<String, String> nulls = new HashMap<>();
        nulls.put("frostfang", null);
        nulls.put(null, "blood");
        assertEquals(RegionTheme.FROST, RelicThemes.themeOf("frostfang", "Frostfang", nulls, frost));
    }

    @Test
    void theBaselineIsReachableOnlyFromTheTable() {
        assertEquals(RegionTheme.GRAVITY, RelicThemes.themeOf("dante_gravity_crown", "Dante's Gravity Crown", table("dante_gravity_crown", "gravity"), Map.of()));
        Map<RegionTheme, List<String>> heavy = kw(RegionTheme.GRAVITY, List.of("stone", "gravity", "hammer"));
        assertNull(RelicThemes.byKeywords("the_mind_stone", "The Mind Stone", heavy), "the baseline's keywords are never read");
        assertNotEquals(RegionTheme.GRAVITY, RelicThemes.themeOf("the_mind_stone", "The Mind Stone", Map.of(), heavy));
        assertNotEquals(RegionTheme.GRAVITY, RelicThemes.themeOf("time_hammer", "Time hammer", Map.of(), heavy));
    }

    // ---------------------------------------------------------------- (b) the keywords
    @Test
    void theThemeWithTheMostDistinctKeywordsWins() {
        Map<RegionTheme, List<String>> words = kw(RegionTheme.BLOOD, List.of("ember"), RegionTheme.FLAME, List.of("ember", "sword"), RegionTheme.FROST, List.of("blade"));
        assertEquals(RegionTheme.FLAME, RelicThemes.themeOf("ember_sword", "", Map.of(), words), "two keywords beat one");
        assertEquals(RegionTheme.BLOOD, RelicThemes.themeOf("ember_staff", "", Map.of(), words), "one hit each: the earlier theme");
        assertEquals(RegionTheme.FROST, RelicThemes.themeOf("blade_of_nothing", "", Map.of(), words), "one hit is enough when it is the only one");
    }

    @Test
    void aTieGoesToTheThemeFirstInTheEnumWhateverTheMapOrder() {
        List<String> zeta = List.of("zeta");
        Map<RegionTheme, List<String>> frostFirst = new LinkedHashMap<>();
        frostFirst.put(RegionTheme.SHADOW, zeta);
        frostFirst.put(RegionTheme.FROST, zeta);
        frostFirst.put(RegionTheme.FLAME, zeta);
        assertEquals(RegionTheme.FLAME, RelicThemes.themeOf("zeta", "", Map.of(), frostFirst), "FLAME comes before FROST and SHADOW in RegionTheme");
        Map<RegionTheme, List<String>> sorted = new EnumMap<>(RegionTheme.class);
        sorted.putAll(frostFirst);
        assertEquals(RegionTheme.FLAME, RelicThemes.themeOf("zeta", "", Map.of(), sorted));
    }

    @Test
    void aKeywordCountsOnceNoMatterHowOftenItIsListedOrHit() {
        Map<RegionTheme, List<String>> words = kw(RegionTheme.FROST, List.of("frost", "FROST", " frost ", "frost"), RegionTheme.FLAME, List.of("fire"));
        assertEquals(1, RelicThemes.hits(List.of("frostfang", "frost", "frozen"), words.get(RegionTheme.FROST)), "listed four times, hitting two words: one hit");
        assertEquals(RegionTheme.FLAME, RelicThemes.themeOf("frost_fire", "", Map.of(), words), "one hit each, FLAME first");
    }

    @Test
    void aKeywordMustBeginAWordNotHideInsideOne() {
        Map<RegionTheme, List<String>> ice = kw(RegionTheme.FROST, List.of("ice"));
        assertNull(RelicThemes.byKeywords("voice_of_the_mountain", "Voice of the Mountain", ice), "ice is inside voice");
        assertEquals(RegionTheme.FROST, RelicThemes.byKeywords("ice_dagger", "Ice Dagger", ice));
        assertEquals(RegionTheme.FROST, RelicThemes.byKeywords("x", "Iceberg Lance", ice), "a prefix of a word is a hit");
    }

    @Test
    void theCardNameCountsAsWellAsTheId() {
        Map<RegionTheme, List<String>> storm = kw(RegionTheme.STORM, List.of("thunder"));
        assertEquals(RegionTheme.STORM, RelicThemes.byKeywords("x", "Thunder Axe", storm));
        assertEquals(RegionTheme.STORM, RelicThemes.byKeywords("thunder_axe", "", storm));
        assertEquals(RegionTheme.STORM, RelicThemes.byKeywords("thunder_axe", null, storm));
        assertNull(RelicThemes.byKeywords("axe", "Axe", storm));
        assertNull(RelicThemes.byKeywords("", "", storm));
    }

    // ---------------------------------------------------------------- (c) the hash
    @Test
    void theHashIsPinnedSoNoRelicChangesThemeByAccident() {
        // characterisation values (FNV-1a over the key, then splitmix64's finaliser), cross-checked against an independent implementation
        assertEquals(0xF52A15E9A9B5E89BL, RelicThemes.hash(""));
        assertEquals(0x16D81979B9303A64L, RelicThemes.hash("frostfang"));
        assertEquals(0x816D04E56E3DEC66L, RelicThemes.hash("the_reaver"));
        assertEquals(0x48D11A6FDC408694L, RelicThemes.hash("bloodbound_katana"));
        assertEquals(0x58854B0A643A458AL, RelicThemes.hash("z"));
    }

    @Test
    void theHashIsStableAndNeverTheBaseline() {
        for (int i = 0; i < 4000; i++) {
            String id = "relic_" + i;
            RegionTheme t = RelicThemes.themeOf(id, "", Map.of(), Map.of());
            assertEquals(t, RelicThemes.themeOf(id, "", Map.of(), Map.of()), id + " must always lead to the same theme");
            assertTrue(t != RegionTheme.GRAVITY, id + " reached the baseline by hash");
            assertEquals(t, RelicThemes.byHash(RelicThemes.key(id)));
        }
    }

    @Test
    void theHashSpreadsRelicsOverEveryOtherTheme() {
        Map<RegionTheme, Integer> seen = new EnumMap<>(RegionTheme.class);
        int n = 20000;
        for (int i = 0; i < n; i++) seen.merge(RelicThemes.byHash("relic_" + i), 1, Integer::sum);
        int others = RegionTheme.values().length - 1;
        assertEquals(others, seen.size(), "every theme but the baseline is reachable");
        double mean = n / (double) others;
        for (Map.Entry<RegionTheme, Integer> e : seen.entrySet()) assertTrue(Math.abs(e.getValue() - mean) < mean * 0.15, e.getKey() + " got " + e.getValue() + " of " + n + " (mean " + mean + ")");
    }

    // ---------------------------------------------------------------- never throws
    @Test
    void nothingEverThrowsOrReturnsNull() {
        assertNotNull(RelicThemes.themeOf(null, null, null, null));
        assertEquals(RelicThemes.themeOf(null, null, null, null), RelicThemes.themeOf("", "", Map.of(), Map.of()), "no id is the same as an empty one");
        assertNotEquals(RegionTheme.GRAVITY, RelicThemes.themeOf(null, null, null, null));
        List<String> dirty = new ArrayList<>();
        dirty.add(null);
        dirty.add("");
        dirty.add("   ");
        Map<RegionTheme, List<String>> words = new HashMap<>();
        words.put(RegionTheme.FROST, dirty);
        words.put(RegionTheme.FLAME, null);
        words.put(null, List.of("x"));
        assertNotNull(RelicThemes.themeOf("x", "x", new HashMap<>(), words));
        assertNotNull(RelicThemes.themeOf("\u0000￿ weird 日本語", "\n\t", Map.of(), words));
        assertNotNull(RelicThemes.themeOf("x".repeat(100000), "y".repeat(100000), Map.of(), words));
        assertEquals(0, RelicThemes.hits(List.of(), List.of("a")));
        assertEquals(0, RelicThemes.hits(List.of("a"), null));
    }

    // ---------------------------------------------------------------- the road: a theme and a ruleset
    @Test
    void theRulesetIsTheThemeUnlessTheRelicNamesAVariant() {
        Map<RegionTheme, List<String>> frost = kw(RegionTheme.FROST, List.of("frost"));
        RelicThemes.Road plain = RelicThemes.roadOf("frostfang", "Frostfang", Map.of(), Map.of(), frost);
        assertEquals(RegionTheme.FROST, plain.theme());
        assertEquals("frost", plain.ruleset(), "no variant: the ruleset is the theme's own id");
        RelicThemes.Road variant = RelicThemes.roadOf("frostfang", "Frostfang", Map.of(), table("frostfang", "the_long_winter"), frost);
        assertEquals(RegionTheme.FROST, variant.theme(), "a variant never changes the theme");
        assertEquals("the_long_winter", variant.ruleset());
        assertEquals(new RelicThemes.Road(RegionTheme.BLOOD, "blood"), RelicThemes.roadOf("x", "", table("x", "blood"), null, Map.of()), "no ruleset table at all is fine");
    }

    @Test
    void aRulesetVariantIsReadLikeAThemeEntryAndBadOnesAreIgnored() {
        Map<RegionTheme, List<String>> frost = kw(RegionTheme.FROST, List.of("frost"));
        assertEquals("hunt_2", RelicThemes.roadOf("NUSMP:FrostFang", "Frostfang", Map.of(), table("frostfang", " Hunt_2 "), frost).ruleset(), "key and id are normalised");
        assertEquals("hunt_2", RelicThemes.roadOf("frostfang", "Frostfang", Map.of(), table("Nusmp:FrostFang", "hunt_2"), frost).ruleset(), "so is a namespaced key in the table");
        for (String bad : new String[] {"", "  ", "has space", "Bad-Dash", "x".repeat(41), "with:colon", "ünï"}) {
            assertEquals("frost", RelicThemes.roadOf("frostfang", "Frostfang", Map.of(), table("frostfang", bad), frost).ruleset(), "'" + bad + "' is not a ruleset id");
        }
        Map<String, String> nulls = new HashMap<>();
        nulls.put("frostfang", null);
        nulls.put(null, "x");
        assertEquals("frost", RelicThemes.roadOf("frostfang", "Frostfang", Map.of(), nulls, frost).ruleset());
        assertTrue(RelicThemes.validRuleset("a_1"));
        assertFalse(RelicThemes.validRuleset(null));
        assertFalse(RelicThemes.validRuleset(""));
    }

    @Test
    void theRoadNeverThrowsOrReturnsNull() {
        assertNotNull(RelicThemes.roadOf(null, null, null, null, null));
        assertEquals(RelicThemes.themeOf(null, null, null, null), RelicThemes.roadOf(null, null, null, null, null).theme());
        assertEquals(RelicThemes.roadOf(null, null, null, null, null).theme().id(), RelicThemes.roadOf(null, null, null, null, null).ruleset());
        assertNotNull(RelicThemes.roadOf("x".repeat(50000), "y", Map.of(), Map.of("x", "z"), Map.of()));
    }

    // ---------------------------------------------------------------- the file
    @Test
    void parseReadsTheRulesetVariantsAndTheirNames() {
        RelicThemes.Parsed p = RelicThemes.parse(MiniJson.parse("{\"explicit\": {\"frostfang\": \"frost\"}, \"rulesets\": {\"Nusmp:Frostfang\": \"The_Hunt\"}, "
                + "\"ruleset_names\": {\"the_hunt\": \"The Frozen Hunt\", \"frost\": \"The Long Winter\"}}"));
        assertEquals(List.of(), p.warnings());
        assertEquals(Map.of("frostfang", "the_hunt"), p.rulesets());
        assertEquals("The Frozen Hunt", p.names().get("the_hunt"));
        assertEquals("The Long Winter", p.names().get("frost"));
    }

    @Test
    void parseWarnsAboutBadRulesetEntriesAndKeepsTheGoodOnes() {
        RelicThemes.Parsed p = RelicThemes.parse(MiniJson.parse("{\"explicit\": {\"a\": \"frost\"}, \"rulesets\": {\"good\": \"fine_id\", \"bad\": \"Not An Id\", \"\": \"x\", \"num\": 7}, "
                + "\"ruleset_names\": {\"fine_id\": \"Fine\", \"Bad Id\": \"x\", \"empty\": \"  \", \"long\": \"" + "n".repeat(61) + "\"}}"));
        assertEquals(Map.of("good", "fine_id"), p.rulesets());
        assertEquals(Map.of("fine_id", "Fine"), p.names());
        assertEquals(6, p.warnings().size(), p.warnings().toString());
        RelicThemes.Parsed shapes = RelicThemes.parse(MiniJson.parse("{\"explicit\": {}, \"rulesets\": [1], \"ruleset_names\": \"x\"}"));
        assertEquals(2, shapes.warnings().size(), shapes.warnings().toString());
        assertTrue(shapes.rulesets().isEmpty() && shapes.names().isEmpty());
    }

    @Test
    void aFileFromBeforeRulesetsStillReads() {
        RelicThemes.Parsed p = RelicThemes.parse(MiniJson.parse("{\"explicit\": {\"a\": \"frost\"}}"));
        assertEquals(List.of(), p.warnings());
        assertTrue(p.rulesets().isEmpty() && p.names().isEmpty());
        RelicThemes.Parsed legacy = new RelicThemes.Parsed(Map.of("a", "frost"), List.of("w"));
        assertTrue(legacy.rulesets().isEmpty() && legacy.names().isEmpty());
        assertEquals(List.of("w"), legacy.warnings());
    }

    @Test
    void parseReadsTheTableAndNormalisesIt() {
        RelicThemes.Parsed p = RelicThemes.parse(MiniJson.parse("{\"_why\": \"x\", \"explicit\": {\"Frostfang\": \"FROST\", \"nusmp:the_reaver\": \"blood\", \"dante_gravity_crown\": \"gravity\"}}"));
        assertEquals(List.of(), p.warnings());
        assertEquals("frost", p.explicit().get("frostfang"));
        assertEquals("blood", p.explicit().get("the_reaver"));
        assertEquals("gravity", p.explicit().get("dante_gravity_crown"));
        assertEquals(3, p.explicit().size());
    }

    @Test
    void parseWarnsAboutEveryBadEntryAndKeepsTheGoodOnes() {
        RelicThemes.Parsed p = RelicThemes.parse(MiniJson.parse("{\"explicit\": {\"good\": \"frost\", \"bad_theme\": \"lava\", \"not_text\": 7, \"\": \"blood\", \"Good\": \"flame\"}}"));
        assertEquals(4, p.warnings().size(), p.warnings().toString());
        assertEquals("flame", p.explicit().get("good"), "a repeated key keeps the last entry");
        assertEquals(1, p.explicit().size());
        assertTrue(p.warnings().get(0).startsWith("bad_theme"), p.warnings().get(0));
    }

    @Test
    void parseNeverThrows() {
        assertEquals(1, RelicThemes.parse("not an object").warnings().size());
        assertEquals(1, RelicThemes.parse(null).warnings().size());
        assertEquals(1, RelicThemes.parse(MiniJson.parse("{}")).warnings().size(), "no explicit object is worth a warning");
        assertEquals(1, RelicThemes.parse(MiniJson.parse("{\"explicit\": [1, 2]}")).warnings().size());
        assertTrue(RelicThemes.parse(MiniJson.parse("{\"explicit\": {}}")).warnings().isEmpty(), "an empty table is a legal table");
        assertTrue(RelicThemes.parse(MiniJson.parse("{\"explicit\": {}}")).explicit().isEmpty());
    }

    @Test
    void parseChecksValuesAgainstTheGivenThemeList() {
        RelicThemes.Parsed p = RelicThemes.parse(MiniJson.parse("{\"explicit\": {\"a\": \"holy\", \"b\": \"frost\"}}"), List.of("frost", "holy"));
        assertEquals(List.of(), p.warnings());
        assertEquals(2, p.explicit().size());
        assertEquals(1, RelicThemes.parse(MiniJson.parse("{\"explicit\": {\"a\": \"holy\"}}"), List.of("frost")).warnings().size());
    }
}
