package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.RegionTheme;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: a theme builder ships one {@code template_themes/<theme>.json}, and a mistake in it (a theme id that does not exist, a prefix with the wrong spelling, a typo in a
 * key) must be loud at build time and in the log, never silent in the game: the wrong id would make the rooms unreachable, the wrong prefix would leave a Holy Church nave
 * free to appear in a Spider Warren. The parser is pure, so every kind of mistake is written out here and compared word for word.
 */
class TemplateThemeFilesTest {
    private static Map<String, Object> file(String theme, Object templates) {
        Map<String, Object> m = new LinkedHashMap<>();
        if (theme != null) m.put("theme", theme);
        if (templates != null) m.put("templates", templates);
        return m;
    }

    @Test
    void aGoodFileClaimsItsPrefixesForItsTheme() {
        List<String> problems = new ArrayList<>();
        TemplateThemes t = TemplateThemeFiles.parse(List.of(file("frost", List.of("mega/hall/holy_church_", "nusmp:mega/set/cathedral_"))), problems);
        assertEquals(List.of(), problems);
        assertTrue(t.allows("nusmp:mega/hall/holy_church_nave", "frost"));
        assertFalse(t.allows("nusmp:mega/hall/holy_church_nave", "blood"));
        assertTrue(t.allows("nusmp:mega/set/cathedral_rotunda", "frost"), "a prefix that already names its namespace is kept");
        assertFalse(t.allows("nusmp:mega/set/cathedral_rotunda", "gravity"));
        assertTrue(t.allows("nusmp:mega/hall/crypt_a", "gravity"), "a template nobody claims stays free");
        assertEquals(2, t.size());
    }

    @Test
    void thePrefixIsGivenTheNamespaceWhenTheFileOmitsIt() {
        assertEquals("nusmp:mega/hall/x_", TemplateThemeFiles.normalise("mega/hall/x_"));
        assertEquals("nusmp:mega/hall/x_", TemplateThemeFiles.normalise("  mega/hall/x_ "));
        assertEquals("other:mega/hall/x_", TemplateThemeFiles.normalise("other:mega/hall/x_"));
    }

    @Test
    void everyThemeOfTheEnumIsATheme() {
        for (RegionTheme r : RegionTheme.values()) {
            assertTrue(TemplateThemeFiles.isTheme(r.id()), r.id());
            assertTrue(TemplateThemeFiles.isTheme(r.id().toUpperCase()), "case does not matter: " + r.id());
        }
        assertFalse(TemplateThemeFiles.isTheme("frozt"));
        assertFalse(TemplateThemeFiles.isTheme(""));
        assertFalse(TemplateThemeFiles.isTheme(null));
    }

    @Test
    void aThemeThatDoesNotExistIsReportedAndClaimsNothing() {
        List<String> problems = new ArrayList<>();
        TemplateThemes t = TemplateThemeFiles.parse(List.of(file("frozt", List.of("mega/hall/holy_church_"))), problems);
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("'frozt' is not a theme id"), problems.get(0));
        assertFalse(t.restricted("nusmp:mega/hall/holy_church_nave"), "a rule for a theme that cannot exist would make the rooms unreachable, so none is made");
        assertEquals(0, t.size());
    }

    @Test
    void aFileWithoutAThemeOrWithoutPrefixesIsReported() {
        List<String> problems = new ArrayList<>();
        TemplateThemeFiles.parse(List.of(file(null, List.of("mega/hall/x_")), file("   ", List.of("mega/hall/x_"))), problems);
        assertEquals(2, problems.size());
        assertTrue(problems.get(0).contains("names no theme"), problems.get(0));
        problems.clear();
        TemplateThemeFiles.parse(List.of(file("frost", null)), problems);
        assertTrue(problems.stream().anyMatch(p -> p.contains("has no 'templates' list")), problems.toString());
        assertTrue(problems.stream().anyMatch(p -> p.contains("claims no template")), problems.toString());
        problems.clear();
        TemplateThemeFiles.parse(List.of(file("frost", List.of())), problems);
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("claims no template"), problems.get(0));
    }

    @Test
    void aPrefixThatIsNotTextIsReportedAndTheRestStillCounts() {
        List<String> problems = new ArrayList<>();
        List<Object> prefixes = new ArrayList<>(List.of("mega/hall/holy_church_"));
        prefixes.add(7);
        prefixes.add(" ");
        TemplateThemes t = TemplateThemeFiles.parse(List.of(file("frost", prefixes)), problems);
        assertEquals(2, problems.size(), problems.toString());
        assertTrue(t.allows("nusmp:mega/hall/holy_church_nave", "frost"));
        assertFalse(t.allows("nusmp:mega/hall/holy_church_nave", "gravity"));
    }

    @Test
    void anUnknownKeyIsReportedButCommentsAndRelativeWeightsAreNot() {
        Map<String, Object> f = file("frost", List.of("mega/hall/holy_church_"));
        f.put("relative", Map.of("mega/hall/holy_church_", 8));
        f.put("_why", "a comment");
        List<String> problems = new ArrayList<>();
        TemplateThemeFiles.parse(List.of(f), problems);
        assertEquals(List.of(), problems);
        f.put("tempaltes", List.of("x"));
        TemplateThemeFiles.parse(List.of(f), problems);
        assertEquals(1, problems.size());
        assertTrue(problems.get(0).contains("unknown key 'tempaltes'"), problems.get(0));
    }

    @Test
    void aFileThatIsNotAnObjectAndNullInputAreHarmless() {
        List<String> problems = new ArrayList<>();
        TemplateThemes t = TemplateThemeFiles.parse(List.of("just text", List.of(1)), problems);
        assertEquals(2, problems.size());
        assertEquals(0, t.size());
        assertEquals(0, TemplateThemeFiles.parse(null, problems).size());
        assertEquals(0, TemplateThemeFiles.parse(List.of(), problems).size());
    }

    @Test
    void severalFilesAddUpAndTwoThemesMayShareATemplate() {
        List<String> problems = new ArrayList<>();
        TemplateThemes t = TemplateThemeFiles.parse(List.of(
                file("frost", List.of("mega/hall/chapel_")),
                file("astral", List.of("mega/hall/chapel_", "mega/hall/court_"))), problems);
        assertEquals(List.of(), problems);
        assertTrue(t.allows("nusmp:mega/hall/chapel_a", "frost"));
        assertTrue(t.allows("nusmp:mega/hall/chapel_a", "astral"));
        assertFalse(t.allows("nusmp:mega/hall/chapel_a", "gravity"));
        assertTrue(t.allows("nusmp:mega/hall/court_a", "astral"));
        assertFalse(t.allows("nusmp:mega/hall/court_a", "frost"));
    }
}
