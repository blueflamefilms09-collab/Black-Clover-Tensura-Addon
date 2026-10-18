package com.newuniverse.nusmp.client.skilltree.shop;

import com.newuniverse.nusmp.client.skilltree.shop.ShopLayout.Rect;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.function.ToIntFunction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: a button outside the panel or two lists on top of each other only shows up at the window size nobody tried, so the layout is walked across every GUI size a player can
 * plausibly have (a 1280x720 window at scale 3 is 427x240; the same window at scale 4 is 320x180; a 1920x1080 window at scale 1 is 1920x1080) and the invariants are asserted at
 * each: everything inside the panel, the panel on the screen, no two blocks overlapping, every list showing at least one row, rows and buttons inside their lists.
 */
class ShopLayoutTest {
    private static final int[][] SIZES = {{300, 176}, {320, 180}, {400, 225}, {427, 240}, {480, 270}, {640, 360}, {854, 480}, {960, 540}, {1280, 720}, {1920, 1080}, {500, 200}, {330, 300}};

    @Test
    void thePanelIsOnTheScreenAndEverythingIsInsideIt() {
        for (int[] s : SIZES) {
            ShopLayout l = ShopLayout.of(s[0], s[1]);
            String at = s[0] + "x" + s[1];
            assertTrue(l.panel.x() >= 0 && l.panel.y() >= 0 && l.panel.right() <= s[0] && l.panel.bottom() <= s[1], "the panel is on the screen at " + at + ": " + l.panel);
            Rect[] parts = {l.portrait, l.purse, l.closeButton, l.speech, l.shelfBox, l.shelfList, l.shelfBar, l.goodsBox, l.bulkRow, l.goodsList, l.goodsBar};
            for (Rect r : parts) {
                assertTrue(r.w() > 0 && r.h() > 0, "positive size " + r + " at " + at);
                assertTrue(r.inside(l.panel), r + " is outside the panel at " + at);
            }
        }
    }

    @Test
    void noTwoBlocksOverlap() {
        for (int[] s : SIZES) {
            ShopLayout l = ShopLayout.of(s[0], s[1]);
            String at = s[0] + "x" + s[1];
            Rect[] blocks = {l.portrait, l.purse, l.shelfBox, l.goodsBox, l.speech, l.closeButton};
            for (int i = 0; i < blocks.length; i++)
                for (int j = i + 1; j < blocks.length; j++)
                    assertFalse(blocks[i].overlaps(blocks[j]), blocks[i] + " overlaps " + blocks[j] + " at " + at);
            assertTrue(l.shelfList.inside(l.shelfBox) && l.shelfBar.inside(l.shelfBox), "shelf parts inside the shelf box at " + at);
            assertTrue(l.bulkRow.inside(l.goodsBox) && l.goodsList.inside(l.goodsBox) && l.goodsBar.inside(l.goodsBox), "goods parts inside the goods box at " + at);
            assertFalse(l.shelfList.overlaps(l.shelfBar), "the shelf and its bar at " + at);
            assertFalse(l.goodsList.overlaps(l.goodsBar), "the goods and their bar at " + at);
            assertFalse(l.bulkRow.overlaps(l.goodsList), "the bulk buttons and the goods at " + at);
            assertFalse(l.portrait.overlaps(l.shelfBox), "the header and the body at " + at);
        }
    }

    @Test
    void everyListShowsAtLeastOneRowAndTheCommonSizeShowsMany() {
        for (int[] s : SIZES) {
            ShopLayout l = ShopLayout.of(s[0], s[1]);
            assertTrue(l.shelfVisible >= 1 && l.goodsVisible >= 1, "rows at " + s[0] + "x" + s[1]);
            assertTrue(l.shelfVisible * ShopLayout.SHELF_ROW <= l.shelfList.h(), "the visible shelf rows fit their list");
            assertTrue(l.goodsVisible * ShopLayout.GOODS_ROW <= l.goodsList.h(), "the visible goods rows fit their list");
        }
        ShopLayout common = ShopLayout.of(427, 240);
        assertTrue(common.shelfVisible >= 5, "at the common size the shelf shows five or more rows, got " + common.shelfVisible);
        assertTrue(common.goodsVisible >= 5, "at the common size the goods show five or more rows, got " + common.goodsVisible);
        assertTrue(ShopLayout.of(1920, 1080).shelfVisible > common.shelfVisible, "a bigger window shows more rows");
    }

    @Test
    void rowsAndTheirButtonsStayInsideTheirLists() {
        for (int[] s : SIZES) {
            ShopLayout l = ShopLayout.of(s[0], s[1]);
            int shelfText = s[0] >= 320 ? 100 : 70, goodsText = s[0] >= 320 ? 60 : 40;       // below a 320-wide GUI the panel cannot keep its proportions; the names are trimmed harder
            for (int i = 0; i < l.shelfVisible; i++) {
                Rect row = l.shelfRow(i, 0);
                assertTrue(row.inside(l.shelfList), "shelf row " + i + " at " + s[0]);
                assertTrue(ShopLayout.buyButton(row).inside(row), "buy button in row " + i);
                assertTrue(ShopLayout.icon(row).inside(row), "icon in row " + i);
                assertFalse(ShopLayout.icon(row).overlaps(ShopLayout.buyButton(row)), "icon and buy button in row " + i);
                assertTrue(ShopLayout.buyButton(row).x() - ShopLayout.icon(row).right() >= shelfText, "room for a name between icon and button: " + (ShopLayout.buyButton(row).x() - ShopLayout.icon(row).right()));
            }
            for (int i = 0; i < l.goodsVisible; i++) {
                Rect row = l.goodsRow(i, 0);
                assertTrue(row.inside(l.goodsList), "goods row " + i + " at " + s[0]);
                assertTrue(ShopLayout.sellButton(row).inside(row), "sell button in row " + i);
                assertFalse(ShopLayout.icon(row).overlaps(ShopLayout.sellButton(row)), "icon and sell button in row " + i);
                assertTrue(ShopLayout.sellButton(row).x() - ShopLayout.icon(row).right() >= goodsText, "room for a name between icon and sell button: " + (ShopLayout.sellButton(row).x() - ShopLayout.icon(row).right()));
            }
        }
    }

    @Test
    void clickingMapsToTheRowUnderTheMouseAndNothingOutside() {
        ShopLayout l = ShopLayout.of(427, 240);
        Rect list = l.shelfList;
        int h = ShopLayout.SHELF_ROW;
        assertEquals(0, ShopLayout.rowAt(list, h, 0, 8, list.x() + 3, list.y()));
        assertEquals(0, ShopLayout.rowAt(list, h, 0, 8, list.x() + 3, list.y() + h - 1));
        assertEquals(1, ShopLayout.rowAt(list, h, 0, 8, list.x() + 3, list.y() + h));
        assertEquals(3, ShopLayout.rowAt(list, h, 2, 8, list.x() + 3, list.y() + h + 1), "scrolled by two, the second visible row is index 3");
        assertEquals(-1, ShopLayout.rowAt(list, h, 0, 8, list.x() - 1, list.y() + 3));
        assertEquals(-1, ShopLayout.rowAt(list, h, 0, 8, list.x() + 3, list.y() - 1));
        assertEquals(-1, ShopLayout.rowAt(list, h, 0, 8, list.right(), list.y() + 3));
        assertEquals(-1, ShopLayout.rowAt(list, h, 0, 2, list.x() + 3, list.y() + 2 * h + 1), "below the last row of a short list is no row");
        assertEquals(-1, ShopLayout.rowAt(list, h, 0, 0, list.x() + 3, list.y() + 1), "an empty list has no rows");
    }

    @Test
    void scrollingIsClampedToTheRowsThatExist() {
        assertEquals(0, ShopLayout.maxScroll(3, 5));
        assertEquals(4, ShopLayout.maxScroll(9, 5));
        assertEquals(0, ShopLayout.clampScroll(-4, 9, 5));
        assertEquals(4, ShopLayout.clampScroll(99, 9, 5));
        assertEquals(2, ShopLayout.clampScroll(2, 9, 5));
        assertEquals(0, ShopLayout.clampScroll(2, 0, 5));
    }

    @Test
    void theScrollbarThumbTracksTheScroll() {
        assertNull(ShopLayout.thumb(5, 5, 0, 100), "everything fits: no bar");
        assertNull(ShopLayout.thumb(3, 5, 0, 100));
        int[] top = ShopLayout.thumb(10, 5, 0, 100);
        assertNotNull(top);
        assertEquals(0, top[0]);
        assertEquals(50, top[1]);
        int[] bottom = ShopLayout.thumb(10, 5, 5, 100);
        assertEquals(50, bottom[0]);
        int[] mid = ShopLayout.thumb(10, 5, 2, 100);
        assertTrue(mid[0] > 0 && mid[0] < 50);
        int[] clamped = ShopLayout.thumb(10, 5, 99, 100);
        assertEquals(50, clamped[0], "a scroll past the end is drawn at the end");
        assertTrue(ShopLayout.thumb(1000, 5, 0, 100)[1] >= 8, "the thumb never shrinks below a grabbable size");
    }

    @Test
    void bulkButtonsStayInsideTheirStripAndTheTightFormKeepsAllThree() {
        int[] widths = {34, 22, 45};                                    // "Stones", "Meat", "Materials" in the game's font
        Rect strip = new Rect(10, 20, 119, 14);                         // the strip at the narrowest panel
        List<Rect> roomy = ShopLayout.bulkRects(strip, widths, ShopLayout.BULK_PAD, ShopLayout.BULK_GAP);
        assertEquals(2, roomy.size(), "roomy padding has to drop the last button at 119 pixels");
        List<Rect> tight = ShopLayout.bulkRects(strip, widths, ShopLayout.BULK_PAD_TIGHT, ShopLayout.BULK_GAP_TIGHT);
        assertEquals(3, tight.size(), "tight padding keeps all three");
        for (Rect r : tight) assertTrue(r.inside(strip), r + " is outside the strip");
        for (int i = 1; i < tight.size(); i++) assertFalse(tight.get(i - 1).overlaps(tight.get(i)), "two bulk buttons overlap");
        assertEquals(0, ShopLayout.bulkRects(strip, new int[]{200}, 8, 3).size(), "a button wider than the strip is left out, not drawn out of its box");
        assertEquals(0, ShopLayout.bulkRects(strip, new int[0], 8, 3).size());
    }

    @Test
    void theBubbleHoldsAtLeastTwoLinesAtEverySize() {
        for (int[] s : SIZES) {
            Rect sp = ShopLayout.of(s[0], s[1]).speech;
            int lines = ShopLayout.speechLines(sp);
            assertTrue(lines >= 2, "two lines of speech at " + s[0] + "x" + s[1] + ", got " + lines);
            assertTrue(4 + 9 * (lines - 1) + 8 <= sp.h() - 2, "the last of the " + lines + " lines stays inside the bubble at " + s[0] + "x" + s[1]);
        }
        assertEquals(1, ShopLayout.speechLines(new Rect(0, 0, 100, 5)), "a tiny bubble still gets one line");
        assertEquals(1, ShopLayout.speechLines(new Rect(0, 0, 100, 0)));
    }

    /** A font where every letter is 6 pixels wide, so the expected lines can be worked out by hand. */
    private static final ToIntFunction<String> SIX = s -> s.length() * 6;

    @Test
    void wrappingBreaksOnSpacesAndBreaksLongWordsByLetters() {
        assertEquals(List.of("aaa", "bbb", "ccc"), ShopLayout.wrap("aaa bbb ccc", 18, 9, SIX), "18 pixels hold exactly one three-letter word");
        assertEquals(List.of("aaa bbb", "ccc"), ShopLayout.wrap("aaa bbb ccc", 42, 9, SIX));
        assertEquals(List.of("abcd", "efgh", "ij"), ShopLayout.wrap("abcdefghij", 24, 9, SIX), "a word wider than a line is broken by letters");
        assertEquals(List.of("aaa", "bbb"), ShopLayout.wrap("  aaa   bbb  ", 18, 9, SIX), "runs of spaces collapse and the ends are trimmed");
    }

    @Test
    void wrappingCutsTheLastLineWithDotsWhenTheTextIsTooLong() {
        assertEquals(List.of("aaa ..."), ShopLayout.wrap("aaa bbb ccc", 42, 1, SIX));
        assertEquals(List.of("aaa bbb", "ccc"), ShopLayout.wrap("aaa bbb ccc", 42, 2, SIX), "no dots when it fits");
        assertEquals(List.of("aaa", "..."), ShopLayout.wrap("aaa bbb ccc", 18, 2, SIX), "when not even a letter and the dots fit, the dots alone are left");
    }

    @Test
    void wrappingNeverOverflowsTheLineOrTheLineCount() {
        String sentence = "Show me the coin first, dearie. Then the shelves. Those are the rules, and the rules are older than you. Hee hee hee.";
        for (int w = 18; w <= 400; w += 7) {
            for (int max = 1; max <= 3; max++) {
                List<String> lines = ShopLayout.wrap(sentence, w, max, SIX);
                assertTrue(lines.size() >= 1 && lines.size() <= max, "line count " + lines.size() + " at width " + w + ", max " + max);
                for (String line : lines) assertTrue(SIX.applyAsInt(line) <= w, "'" + line + "' is wider than " + w);
            }
        }
    }

    @Test
    void wrappingBlankOrImpossibleInputGivesNoLines() {
        assertTrue(ShopLayout.wrap(null, 100, 2, SIX).isEmpty());
        assertTrue(ShopLayout.wrap("", 100, 2, SIX).isEmpty());
        assertTrue(ShopLayout.wrap("   ", 100, 2, SIX).isEmpty());
        assertTrue(ShopLayout.wrap("hello", 0, 2, SIX).isEmpty());
        assertTrue(ShopLayout.wrap("hello", 100, 0, SIX).isEmpty());
    }
}
