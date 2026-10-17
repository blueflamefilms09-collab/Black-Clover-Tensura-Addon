package com.newuniverse.nusmp.labyrinth.mega.gen;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** The theme rule of templates: free unless a prefix claims them, longest prefix wins, ids are case-insensitive. */
class TemplateThemesTest {
    @Test
    void aTemplateNobodyClaimsIsFreeForEveryTheme() {
        TemplateThemes t = TemplateThemes.of(Map.of("nusmp:mega/hall/holy_church_", List.of("holy_church")));
        assertTrue(t.allows("nusmp:mega/hall/crypt_a", "gravity"));
        assertTrue(t.allows("nusmp:mega/hall/crypt_a", "holy_church"));
        assertFalse(t.restricted("nusmp:mega/hall/crypt_a"));
        assertEquals(List.of(), t.themesOf("nusmp:mega/hall/crypt_a"));
    }

    @Test
    void aClaimedTemplateIsDrawnOnlyInItsThemes() {
        TemplateThemes t = TemplateThemes.of(Map.of("nusmp:mega/hall/holy_church_", List.of("holy_church", "celestial_court")));
        assertTrue(t.allows("nusmp:mega/hall/holy_church_nave", "holy_church"));
        assertTrue(t.allows("nusmp:mega/hall/holy_church_nave", "CELESTIAL_COURT"));
        assertFalse(t.allows("nusmp:mega/hall/holy_church_nave", "cursed_archive"));
        assertFalse(t.allows("nusmp:mega/hall/holy_church_nave", null));
        assertTrue(t.restricted("nusmp:mega/hall/holy_church_nave"));
    }

    @Test
    void theLongestPrefixWins() {
        TemplateThemes t = TemplateThemes.of(Map.of("nusmp:mega/hall/archive_", List.of("cursed_archive"), "nusmp:mega/hall/archive_light", List.of("holy_church")));
        assertTrue(t.allows("nusmp:mega/hall/archive_stacks", "cursed_archive"));
        assertFalse(t.allows("nusmp:mega/hall/archive_stacks", "holy_church"));
        assertTrue(t.allows("nusmp:mega/hall/archive_lightwell", "holy_church"));
        assertFalse(t.allows("nusmp:mega/hall/archive_lightwell", "cursed_archive"));
    }

    @Test
    void filesAddUpAndNullsAreHarmless() {
        TemplateThemes t = TemplateThemes.none().with("holy_church", List.of("nusmp:mega/hall/church_")).with("celestial_court", List.of("nusmp:mega/hall/church_", " "));
        assertEquals(List.of("celestial_court", "holy_church"), t.themesOf("nusmp:mega/hall/church_apse"));
        assertEquals(1, t.size());
        assertFalse(TemplateThemes.of(null).restricted("x"));
        assertFalse(TemplateThemes.none().allows(null, "gravity"));
        assertEquals(0, TemplateThemes.of(Map.of(" ", List.of("a"))).size());
    }
}
