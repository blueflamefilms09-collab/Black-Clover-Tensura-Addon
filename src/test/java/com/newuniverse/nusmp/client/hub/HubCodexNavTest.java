package com.newuniverse.nusmp.client.hub;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Codex lives in the hub behind a rail, and the one promise of the replacement is that no page of the old book is lost. Pure: the rail rows are plain arithmetic, and a source scan of CodexPages checks that
 * every stock page gets a real id (not the catch-all) so it has its own icon and place.
 */
class HubCodexNavTest {
    private static final Path PAGES = Path.of("src/main/java/com/newuniverse/nusmp/client/ui/CodexPages.java");

    @Test
    void everyPageIndexAppearsExactlyOnceInTheRows() {
        List<String> ids = List.of("overview", "status", "weapons", "relics", "bestiary", "spirit_lords", "guide", "spells", "blocks", "entities", "magic", "effects", "beastwill", "weapon", "data", "data", "mystery");
        List<HubCodexNav.Row> rows = HubCodexNav.rows(ids);
        Set<Integer> seen = new HashSet<>();
        for (HubCodexNav.Row r : rows) if (!r.header()) assertTrue(seen.add(r.page()), "page " + r.page() + " listed twice");
        assertEquals(ids.size(), seen.size(), "a page went missing from the rail");
        for (int i = 0; i < ids.size(); i++) assertTrue(HubCodexNav.rowOfPage(rows, i) >= 0, "page " + i + " has no row");
    }

    @Test
    void unknownPagesLandUnderReferenceNotInTheVoid() {
        assertEquals(HubCodexNav.Cat.REFERENCE, HubCodexNav.catOf("something_new"));
        assertEquals("data", HubCodexNav.idOf("Mystery Section", "EntryListPage"));
        assertEquals("data", HubCodexNav.idOf(null, null));
        List<HubCodexNav.Row> rows = HubCodexNav.rows(List.of("mystery"));
        assertEquals(2, rows.size());
        assertTrue(rows.get(0).header() && rows.get(0).cat() == HubCodexNav.Cat.REFERENCE && rows.get(1).page() == 0);
    }

    @Test
    void headersOnlyForCategoriesThatHavePagesAndInCategoryOrder() {
        List<HubCodexNav.Row> rows = HubCodexNav.rows(List.of("spells", "status", "magic"));
        List<HubCodexNav.Cat> heads = new ArrayList<>();
        for (HubCodexNav.Row r : rows) if (r.header()) heads.add(r.cat());
        assertEquals(List.of(HubCodexNav.Cat.YOU, HubCodexNav.Cat.MAGIC), heads);
        assertEquals(1, rows.get(1).page(), "status first under You");
        assertEquals(0, rows.get(3).page(), "spells before magic in rail order");
        assertEquals(2, rows.get(4).page());
    }

    @Test
    void clicksMapBackToRowsAndStepWrapsOverHeaders() {
        List<HubCodexNav.Row> rows = HubCodexNav.rows(List.of("overview", "status", "spells", "wiki"));
        int h = HubCodexNav.height(rows);
        assertEquals(-1, HubCodexNav.rowAt(rows, -1));
        assertEquals(-1, HubCodexNav.rowAt(rows, h));
        for (int i = 0; i < rows.size(); i++) {
            int y = HubCodexNav.rowY(rows, i);
            assertEquals(i, HubCodexNav.rowAt(rows, y));
            assertEquals(i, HubCodexNav.rowAt(rows, y + HubCodexNav.rowH(rows.get(i)) - 1));
        }
        int p = 0;
        Set<Integer> visited = new HashSet<>();
        for (int k = 0; k < 4; k++) { visited.add(p); p = HubCodexNav.step(rows, p, 1); }
        assertEquals(4, visited.size(), "stepping forward visits every page and wraps");
        assertEquals(3, HubCodexNav.step(rows, 0, -1));
        assertEquals(0, HubCodexNav.step(HubCodexNav.rows(List.of("status")), 0, 1), "a single page steps to itself");
    }

    @Test
    void everyStockPageOfCodexPagesGetsItsOwnIdAndTheCustomClassesToo() throws IOException {
        String src = Files.readString(PAGES);
        List<String> titles = new ArrayList<>();
        Matcher m = Pattern.compile("register\\(new (?:CatalogPage|TextPage|EntryListPage)\\(\"([^\"]+)\"").matcher(src);
        while (m.find()) titles.add(m.group(1));
        m = Pattern.compile("String title\\(\\) \\{ return \"([^\"]+)\"; \\}").matcher(src);
        while (m.find()) titles.add(m.group(1));
        assertTrue(titles.size() >= 12, "expected the 12 stock pages, found " + titles);
        for (String t : titles) assertNotEquals("data", HubCodexNav.idOf(t, "x"), "stock page '" + t + "' falls into the catch-all");
        assertEquals("beastwill", HubCodexNav.idOf("Beast Will", "BeastWillCodexPage"));
        assertEquals("weapon", HubCodexNav.idOf("Weapon", "WeaponCodexPage"));
        assertTrue(src.contains("BeastWillCodexPage") && src.contains("WeaponCodexPage"));
        assertEquals("rules", HubCodexNav.idOf("Rules", "EntryListPage"));
        assertEquals("wiki", HubCodexNav.idOf("Wiki", "EntryListPage"));
    }

    @Test
    void everyNavItemHasACategoryAndNoIdRepeats() {
        Set<String> ids = new HashSet<>();
        for (HubCodexNav.Item i : HubCodexNav.NAV) {
            assertTrue(ids.add(i.id()), "duplicate nav id " + i.id());
            assertEquals(i.cat(), HubCodexNav.catOf(i.id()));
        }
        assertTrue(ids.contains(HubCodexNav.DATA));
    }
}
