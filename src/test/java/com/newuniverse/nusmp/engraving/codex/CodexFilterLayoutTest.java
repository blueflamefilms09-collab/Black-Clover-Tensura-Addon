package com.newuniverse.nusmp.engraving.codex;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The search, the group filter, the pagination and the layout of the Codex, with the shipped data (pure, no game). */
class CodexFilterLayoutTest {

    private static CodexEntries.Book book() throws Exception { return CodexTestData.book(CodexTestData.english()); }

    private static int len(String s) { return s.length() * 6; }          // a monospace stand-in font: 6 pixels a character

    // ---------------------------------------------------------------- filter
    @Test
    void anEmptySearchShowsEverythingInTheBooksOrder() throws Exception {
        CodexEntries.Book b = book();
        int[] all = CodexFilter.select(b.entries(), null, "");
        assertEquals(b.entries().size(), all.length);
        for (int i = 0; i < all.length; i++) assertEquals(i, all[i]);
        assertEquals(b.entries().size(), CodexFilter.select(b.entries(), "", "   ").length, "spaces are no query");
    }

    @Test
    void aGroupShowsOnlyItsOwnEntries() throws Exception {
        CodexEntries.Book b = book();
        int total = 0;
        for (CodexEntries.Group g : b.groups()) {
            int[] in = CodexFilter.select(b.entries(), g.id(), "");
            assertEquals(g.count(), in.length, g.id());
            for (int i : in) assertEquals(g.id(), b.entries().get(i).groupId());
            total += in.length;
        }
        assertEquals(b.entries().size(), total);
        assertEquals(0, CodexFilter.select(b.entries(), "no_such_group", "").length);
    }

    @Test
    void theSearchIsCaseInsensitiveAndEveryWordMustMatch() throws Exception {
        CodexEntries.Book b = book();
        int[] falna = CodexFilter.select(b.entries(), null, "FALNA");
        assertTrue(falna.length >= 1);
        assertEquals("nusmp:fr_falna", b.entries().get(falna[0]).key(), "the engraving named Falna comes first");
        int[] both = CodexFilter.select(b.entries(), null, "falna danmachi");
        assertTrue(both.length >= 1 && both.length <= falna.length);
        assertEquals(0, CodexFilter.select(b.entries(), null, "falna bleach").length, "an AND search: Falna is not a Bleach engraving");
        assertEquals(0, CodexFilter.select(b.entries(), null, "zzzzqqqq").length);
    }

    @Test
    void theSearchLooksInTheEffectTextTheFactsAndTheCarriers() throws Exception {
        CodexEntries.Book b = book();
        int[] cooldown = CodexFilter.select(b.entries(), null, "once every 5 s");
        assertTrue(cooldown.length > 3, "the effect text is searched");
        int[] cap = CodexFilter.select(b.entries(), null, "never goes above");
        assertTrue(cap.length > 3, "the facts are searched");
        int[] item = CodexFilter.select(b.entries(), null, "demon_slayer_sword");
        boolean anti = false;
        for (int i : item) anti |= b.entries().get(i).key().equals("nusmp:anti_magic");
        assertTrue(anti, "the items that carry an engraving are searched");
    }

    @Test
    void nameMatchesComeBeforeTextMatchesAndBothKeepTheBooksOrder() throws Exception {
        CodexEntries.Book b = book();
        int[] r = CodexFilter.select(b.entries(), null, "swift");
        assertTrue(r.length > 1);
        assertEquals("tensura:swift", b.entries().get(r[0]).key());
        boolean seenText = false;
        int lastName = -1, lastText = -1;
        for (int i : r) {
            boolean byName = b.entries().get(i).name().toLowerCase().contains("swift");
            if (byName) { assertFalse(seenText, "a name match after a text match"); assertTrue(i > lastName); lastName = i; }
            else { seenText = true; assertTrue(i > lastText); lastText = i; }
        }
    }

    @Test
    void aLongQueryIsCutAndNoQueryCrashesTheFilter() throws Exception {
        CodexEntries.Book b = book();
        String huge = "a ".repeat(500);
        assertTrue(CodexFilter.words(huge).length <= CodexFilter.MAX_QUERY);
        assertEquals(0, CodexFilter.words(null).length);
        assertEquals(0, CodexFilter.words("   ").length);
        assertEquals(2, CodexFilter.words("  Fire   Wall ").length);
        assertEquals("fire", CodexFilter.words("  Fire   Wall ")[0]);
        CodexFilter.select(b.entries(), null, huge);
        CodexFilter.select(b.entries(), null, null);
        CodexFilter.select(List.of(), null, "x");
    }

    // ---------------------------------------------------------------- pagination
    @Test
    void entriesPackIntoPagesWithoutOverflow() {
        int[] heights = {80, 80, 80, 80, 80, 80, 80};
        List<int[]> pages = CodexFilter.paginate(heights, 200, 8);                  // 80 + 8 + 80 = 168 fits, a third does not
        assertEquals(4, pages.size());
        int next = 0;
        for (int[] p : pages) {
            assertEquals(next, p[0], "pages are runs of consecutive items");
            int used = 0;
            for (int i = 0; i < p[1]; i++) used += heights[p[0] + i] + (i > 0 ? 8 : 0);
            assertTrue(used <= 200 || p[1] == 1, "a page overflows: " + used);
            next += p[1];
        }
        assertEquals(heights.length, next, "every item is on a page");
    }

    @Test
    void anOversizeEntryGetsAPageOfItsOwnAndAnEmptyListGetsOnePage() {
        List<int[]> pages = CodexFilter.paginate(new int[]{50, 400, 50}, 200, 8);
        assertEquals(3, pages.size());
        assertEquals(1, pages.get(1)[1]);
        List<int[]> none = CodexFilter.paginate(new int[0], 200, 8);
        assertEquals(1, none.size());
        assertEquals(0, none.get(0)[1]);
        assertEquals(1, CodexFilter.spreads(0));
        assertEquals(1, CodexFilter.spreads(1));
        assertEquals(1, CodexFilter.spreads(2));
        assertEquals(2, CodexFilter.spreads(3));
        assertEquals(12, CodexFilter.spreads(24));
    }

    @Test
    void theWholeBookPaginatesAtSeveralPageSizes() throws Exception {
        CodexEntries.Book b = book();
        for (int[] size : new int[][]{{150, 150}, {230, 190}, {260, 260}}) {                       // small, normal and large windows: {page height, page width}
            int[] heights = new int[b.entries().size()];
            for (int i = 0; i < heights.length; i++) heights[i] = CodexLayout.lay(i, b.entries().get(i), size[1], size[0], 6, null, CodexFilterLayoutTest::len).height();
            List<int[]> pages = CodexFilter.paginate(heights, size[0], 8);
            int sum = 0;
            for (int[] p : pages) sum += p[1];
            assertEquals(heights.length, sum);
            for (int h : heights) assertTrue(h <= size[0], "an entry is taller than its page: " + h + " > " + size[0]);
            assertTrue(pages.size() >= heights.length / 5, "pages: " + pages.size());
        }
    }

    // ---------------------------------------------------------------- layout
    @Test
    void wrapBreaksAtWordsAndNeverExceedsTheWidth() {
        List<String> lines = CodexLayout.wrap("one two three four five six seven", 6 * 10, CodexFilterLayoutTest::len);
        for (String l : lines) assertTrue(len(l) <= 60, "'" + l + "'");
        assertEquals("one two three four five six seven", String.join(" ", lines));
        List<String> longWord = CodexLayout.wrap("abcdefghijklmnopqrstuvwxyz", 6 * 8, CodexFilterLayoutTest::len);
        assertTrue(longWord.size() >= 4);
        for (String l : longWord) assertTrue(len(l) <= 48);
        assertEquals("abcdefghijklmnopqrstuvwxyz", String.join("", longWord));
        assertEquals(List.of(""), CodexLayout.wrap("", 100, CodexFilterLayoutTest::len));
        assertEquals(List.of(""), CodexLayout.wrap(null, 100, CodexFilterLayoutTest::len));
    }

    @Test
    void everyLaidOutEntryFitsItsPageAndIsMeasuredHonestly() throws Exception {
        CodexEntries.Book b = book();
        for (int pageH : new int[]{120, 180, 240}) {
            for (int i = 0; i < b.entries().size(); i++) {
                CodexEntries.Entry e = b.entries().get(i);
                CodexLayout.Laid l = CodexLayout.lay(i, e, 170, pageH, 6, null, CodexFilterLayoutTest::len);
                assertTrue(l.height() <= pageH || l.text().size() <= 2, e.key() + " height " + l.height() + " for page " + pageH);
                assertEquals(CodexLayout.heightOf(l.scope().size(), l.text().size(), l.facts().size(), l.found().size()), l.height());
                assertFalse(l.text().isEmpty(), e.key());
                if (e.foundOn().size() > 6) assertTrue(String.join(" ", l.found()).contains("more"), e.key() + ": the '+M more' must never be cut off: " + l.found());
                if (e.foundOn().isEmpty()) assertTrue(String.join(" ", l.found()).contains("no item"), e.key());
                for (String s : l.text()) assertTrue(len(s) <= 170, e.key() + " text line too wide");
                for (String s : l.facts()) assertTrue(len(s) <= 170, e.key() + " fact line too wide");
                assertTrue(l.text().size() <= CodexLayout.MAX_TEXT && l.facts().size() <= CodexLayout.MAX_FACTS && l.found().size() <= CodexLayout.MAX_FOUND);
            }
        }
    }

    @Test
    void aCutBlockEndsWithTheEllipsisAndBulletsIndentTheirContinuation() throws Exception {
        CodexEntries.Book b = book();
        CodexEntries.Entry e = b.entries().stream().filter(x -> x.key().equals("nusmp:fr_grimoire_surge")).findFirst().orElseThrow();
        CodexLayout.Laid roomy = CodexLayout.lay(0, e, 300, 400, 6, null, CodexFilterLayoutTest::len);
        assertFalse(roomy.cut(), "nothing is cut when there is room");
        for (String f : roomy.facts()) assertTrue(f.startsWith("- ") || f.startsWith("  "), f);
        CodexLayout.Laid tight = CodexLayout.lay(0, e, 150, 90, 6, null, CodexFilterLayoutTest::len);
        assertTrue(tight.cut(), "a short page cuts");
        assertTrue(tight.height() <= 90 || tight.text().size() <= 2);
        Set<String> kinds = new HashSet<>();
        for (CodexEntries.Entry x : b.entries()) kinds.add(x.kind().name());
        assertEquals(3, kinds.size());
    }

    @Test
    void thereAreAboutAHundredEntriesSoTheOneTimeLayoutIsCheap() throws Exception {
        CodexEntries.Book b = book();
        assertTrue(b.entries().size() >= 100 && b.entries().size() <= 400, "entries: " + b.entries().size());
        long t0 = System.nanoTime();
        for (int i = 0; i < b.entries().size(); i++) CodexLayout.lay(i, b.entries().get(i), 170, 200, 6, null, CodexFilterLayoutTest::len);
        long ms = (System.nanoTime() - t0) / 1_000_000L;
        assertTrue(ms < 2000, "laying out " + b.entries().size() + " entries took " + ms + " ms");
        long t1 = System.nanoTime();
        for (int k = 0; k < 200; k++) CodexFilter.select(b.entries(), null, "fire damage");
        long per = (System.nanoTime() - t1) / 200 / 1000L;
        assertTrue(per < 20_000, "one search took " + per + " microseconds");
        assertTrue(Arrays.stream(CodexFilter.select(b.entries(), null, "fire damage")).count() >= 0);
    }
}
