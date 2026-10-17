package com.newuniverse.nusmp.labyrinth.mega.theme;

import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: {@code relic_themes.json} is a hand-made table of about a hundred and forty relic ids, and an id with a typo is a relic that silently falls to the
 * hash instead of its place on the map. This test reads the very file that ships: it must parse without a warning against the eleven shipped themes, its
 * keys must be normalised and unique, every key must be the id of a relic that exists in the sources (a quoted literal under {@code relics/}), every
 * theme must be the target of some relic, and the keywords the theme files carry for the fallback must not be listed by two themes. Pure: reads files
 * from the project root.
 */
class RelicThemesDataTest {
    private static final Path FILE = Path.of("src/main/resources/data/nusmp/mega/relic_themes.json");
    private static final Path THEMES = Path.of("src/main/resources/data/nusmp/mega/themes");
    private static final Path RELICS = Path.of("src/main/java/com/newuniverse/nusmp/relics");
    /** The themes the mod ships (the same list as {@code ThemeDataTest}): the file is checked against these, whatever the enum on the class path holds. */
    private static final List<String> SHIPPED = List.of("gravity", "blood", "spatial", "flame", "frost", "verdant", "storm", "shadow", "astral",
            "radiant", "abyss", "feywild_grove", "dwarven_forgehold", "crystal_geode", "sunken_temple", "holy_church", "celestial_court",
            "sky_citadel", "mountain_monastery", "cursed_archive", "necropolis", "vampire_castle", "haunted_manor");

    private static String raw() throws IOException { return Files.readString(FILE, StandardCharsets.UTF_8); }

    @Test
    void theShippedTableParsesWithoutAWarning() throws IOException {
        RelicThemes.Parsed p = RelicThemes.parse(MiniJson.parse(raw()), SHIPPED);
        assertEquals(List.of(), p.warnings());
        assertTrue(p.explicit().size() >= 120, "the table lists the existing relics of Unique and above: " + p.explicit().size());
        assertTrue(raw().contains("\"_why\": \""), "every data file says why it is built that way");
    }

    @Test
    void everyKeyIsNormalisedAndListedOnce() throws IOException {
        String raw = raw();
        int from = raw.indexOf("\"explicit\": {");
        assertTrue(from > 0, "the table has an explicit object");
        String block = raw.substring(from, raw.indexOf("\n  }", from));                  // only the explicit block: the ruleset names above it are indented the same way
        Matcher m = Pattern.compile("^\\s{4}\"([^\"]+)\"\\s*:\\s*\"[^\"]+\"\\s*,?\\s*$", Pattern.MULTILINE).matcher(block);
        Set<String> seen = new HashSet<>();
        int entries = 0;
        while (m.find()) {
            entries++;
            String key = m.group(1);
            assertEquals(RelicThemes.key(key), key, "key '" + key + "' must be lower case with no namespace");
            assertTrue(key.matches("[a-z0-9_]+"), "key '" + key + "' is not a relic id");
            assertTrue(seen.add(key), "relic '" + key + "' is listed twice");
        }
        assertEquals(RelicThemes.parse(MiniJson.parse(raw), SHIPPED).explicit().size(), entries, "every line of the table is one entry");
    }

    @Test
    void everyShippedThemeHasARulesetNameAndEveryVariantIsAValidId() throws IOException {
        RelicThemes.Parsed p = RelicThemes.parse(MiniJson.parse(raw()), SHIPPED);
        for (String theme : SHIPPED) {
            String name = p.names().get(theme);
            assertNotNull(name, "the ruleset of the theme '" + theme + "' (its own id) has no name in ruleset_names");
            assertTrue(name.length() >= 4 && name.length() <= 40, theme + ": '" + name + "'");
        }
        assertEquals(new TreeSet<>(SHIPPED), new TreeSet<>(p.names().keySet()), "a name for a ruleset nobody plays would never be read");
        for (Map.Entry<String, String> v : p.rulesets().entrySet()) {
            assertTrue(RelicThemes.validRuleset(v.getValue()), v.getKey() + ": '" + v.getValue() + "' is not a ruleset id");
            assertTrue(p.explicit().containsKey(v.getKey()), v.getKey() + " names a ruleset variant but is not placed in 'explicit' (a variant belongs to a hand-placed relic)");
        }
    }

    @Test
    void everyKeyNamesARelicThatExists() throws IOException {
        StringBuilder all = new StringBuilder();
        try (var files = Files.walk(RELICS)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".java")).toList()) all.append(Files.readString(p, StandardCharsets.UTF_8)).append('\n');
        }
        String sources = all.toString();
        assertTrue(sources.length() > 200000, "the relic sources look truncated");
        List<String> missing = new ArrayList<>();
        for (String id : RelicThemes.parse(MiniJson.parse(raw()), SHIPPED).explicit().keySet()) if (!sources.contains("\"" + id + "\"")) missing.add(id);
        assertEquals(List.of(), missing, "these ids appear nowhere in the relic sources (a typo?)");
    }

    @Test
    void everyShippedThemeIsTheTargetOfSomeRelic() throws IOException {
        Map<String, Integer> count = new TreeMap<>();
        for (String theme : RelicThemes.parse(MiniJson.parse(raw()), SHIPPED).explicit().values()) count.merge(theme, 1, Integer::sum);
        assertEquals(new TreeSet<>(SHIPPED), count.keySet(), "a theme nobody leads to would never be chosen");
        for (Map.Entry<String, Integer> e : count.entrySet()) assertTrue(e.getValue() >= 5, e.getKey() + " is the road of only " + e.getValue() + " relics");
        int most = 0;
        int total = 0;
        for (int n : count.values()) {
            most = Math.max(most, n);
            total += n;
        }
        assertTrue(most * 4 <= total, "one theme takes more than a quarter of the relics: " + count);
    }

    /**
     * The Crucible only offers relics of the minimum grade (UNIQUE by default), so a theme whose explicit relics are all of a lower grade could never be chosen by
     * a vault; the in-game self-test {@code theme.relic_themes} fails on it, and this test says so before the smoke test is needed.
     */
    @Test
    void everyShippedThemeHasARelicOfUniqueOrHigher() throws IOException {
        Map<String, String> grade = new TreeMap<>();
        Pattern def = Pattern.compile("RelicDef\\.of\\(\"([a-z0-9_]+)\"\\s*,\\s*\"[^\"]*\"\\s*,\\s*Grade\\.([A-Z]+)");
        try (var files = Files.walk(RELICS)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                Matcher m = def.matcher(Files.readString(p, StandardCharsets.UTF_8));
                while (m.find()) grade.put(m.group(1), m.group(2));
            }
        }
        assertTrue(grade.size() > 200, "the relic definitions were not found: " + grade.size());
        Set<String> high = Set.of("UNIQUE", "LEGEND", "GOD", "GENESIS");
        Map<String, Integer> perTheme = new TreeMap<>();
        for (Map.Entry<String, String> e : RelicThemes.parse(MiniJson.parse(raw()), SHIPPED).explicit().entrySet())
            if (high.contains(grade.getOrDefault(e.getKey(), ""))) perTheme.merge(e.getValue(), 1, Integer::sum);
        for (String theme : SHIPPED)
            assertTrue(perTheme.getOrDefault(theme, 0) >= 1, theme + " has no explicit relic of grade UNIQUE or higher: a vault could never lead to it");
    }

    @Test
    void noKeywordIsListedByTwoThemes() throws IOException {
        Map<String, String> owner = new TreeMap<>();
        try (var files = Files.list(THEMES)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".json")).sorted().toList()) {
                String id = p.getFileName().toString().replace(".json", "");
                ThemeDef d = ThemeDef.parse(id, MiniJson.parse(Files.readString(p, StandardCharsets.UTF_8))).def();
                for (String k : d.keywords()) {
                    String before = owner.put(k, id);
                    assertNull(before, "keyword '" + k + "' is listed by both " + before + " and " + id);
                }
            }
        }
        assertTrue(owner.size() > 100, "the themes carry " + owner.size() + " keywords in all");
    }

    @Test
    void aRelicNamedByTwoOfAThemesKeywordsLeadsToThatTheme() throws IOException {
        Map<RegionTheme, List<String>> words = new EnumMap<>(RegionTheme.class);
        try (var files = Files.list(THEMES)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".json")).sorted().toList()) {
                String id = p.getFileName().toString().replace(".json", "");
                RegionTheme t = RelicThemes.themeById(id);
                if (t == null) continue;                                          // a theme file whose enum entry is not on this class path yet
                words.put(t, ThemeDef.parse(id, MiniJson.parse(Files.readString(p, StandardCharsets.UTF_8))).def().keywords());
            }
        }
        assertTrue(words.size() >= 9);
        for (Map.Entry<RegionTheme, List<String>> e : words.entrySet()) {
            if (e.getKey() == RegionTheme.GRAVITY) continue;                      // the baseline is never chosen by keyword
            List<String> k = e.getValue();
            String name = k.get(0) + " of " + k.get(1);
            assertEquals(e.getKey(), RelicThemes.themeOf(name.replace(' ', '_'), name, Map.of(), words), "'" + name + "' should lead to " + e.getKey());
        }
    }
}
