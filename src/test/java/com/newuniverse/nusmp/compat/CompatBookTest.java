package com.newuniverse.nusmp.compat;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The Compatibility Codex text: every page fits a vanilla book page, the live status flips with the mod list, nothing is overclaimed. */
class CompatBookTest {
    private static final Set<String> ALL = Set.of("neoforge", "tensura", "manascore_skill", "manascore_storage", "architectury", "geckolib",
            "curios", "bettercombat", "ftbteams", "trnightmare", "iceandfire", "tensuranoindex");

    @Test
    void everyPageFitsABookPage() {
        for (Set<String> mods : List.of(ALL, Set.<String>of(), Set.of("tensura"))) {
            List<String> pages = CompatBook.pages(mods::contains);
            assertFalse(pages.isEmpty());
            assertTrue(pages.size() <= 100, "a written book holds at most 100 pages");
            for (String page : pages) {
                assertTrue(page.length() <= CompatBook.MAX_CHARS, "page too long (" + page.length() + "): " + page);
                String[] lines = page.split("\n", -1);
                assertTrue(lines.length <= CompatBook.MAX_LINES, "too many lines: " + page);
                for (String line : lines) assertTrue(line.length() <= CompatBook.LINE_CHARS, "line too wide: '" + line + "'");
                assertFalse(page.isBlank());
            }
        }
    }

    @Test
    void liveStatusFollowsTheModList() {
        String with = String.join("\n", CompatBook.pages(ALL::contains)).replaceAll("\\s+", " ");
        String without = String.join("\n", CompatBook.pages(id -> false)).replaceAll("\\s+", " ");
        assertTrue(with.contains("Curios API: Loaded"), with);
        assertTrue(with.contains("TENSURA NO INDEX Loaded"));
        assertTrue(with.contains("is ON"));
        assertTrue(without.contains("Curios API: Not installed"));
        assertTrue(without.contains("TENSURA NO INDEX Not installed"));
        assertFalse(without.contains(": Loaded") || without.contains("INDEX Loaded") || without.contains("is ON"), "with no mods nothing may read Loaded or ON");
        assertTrue(without.contains("off (the other mod is not here)"));
    }

    @Test
    void nullPredicateMeansNothingInstalled() {
        assertEquals(CompatBook.pages(id -> false), CompatBook.pages(null));
    }

    @Test
    void honestyAndBridgeSectionsArePresentAndFirstPageIsTheCover() {
        List<String> pages = CompatBook.pages(ALL::contains);
        assertTrue(pages.get(0).startsWith(CompatBook.TITLE));
        String all = String.join("\n", pages).replaceAll("\\s+", " ");
        assertTrue(all.contains("Not one line of this was run"));
        assertTrue(all.contains("no_max_ep_plunder"));
        assertTrue(all.contains("REQUIRED") && all.contains("OPTIONAL") && all.contains("TENSURA NO INDEX") && all.contains("Not covered"));
        assertTrue(CompatBook.TITLE.length() <= 32, "a book title is capped at 32 characters");
    }

    @Test
    void sameInputSamePages() {
        assertEquals(CompatBook.pages(ALL::contains), CompatBook.pages(ALL::contains));
    }

    @Test
    void wrapCutsOnWordsAndLongWords() {
        assertEquals(List.of("aaa bbb", "ccc"), CompatBook.wrap("aaa bbb ccc", 7));
        assertEquals(List.of("abcde", "fghij", "k"), CompatBook.wrap("abcdefghijk", 5));
        assertEquals(List.of("a", "", "b"), CompatBook.wrap("a\n\nb", 5));
    }

    @Test
    void aLongBlockSplitsOverPagesWithoutLosingText() {
        String text = "word ".repeat(300).trim();
        List<String> pages = CompatBook.paginate(List.of(new CompatBook.Block(true, text)));
        assertTrue(pages.size() > 1);
        String joined = String.join(" ", pages).replaceAll("\\s+", " ").trim();
        assertEquals(text, joined);
        for (String p : pages) assertTrue(p.length() <= CompatBook.MAX_CHARS);
    }
}
