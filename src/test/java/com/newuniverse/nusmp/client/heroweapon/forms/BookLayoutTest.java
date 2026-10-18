package com.newuniverse.nusmp.client.heroweapon.forms;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Why: a layout that overlaps or runs off the edge on one window size is invisible to the author, so the Weapon Book's rectangles are checked for a grid of sizes,
 * from a small laptop at a large GUI scale to a big monitor.
 */
class BookLayoutTest {
    private static final int[][] SIZES = {{284, 160}, {320, 180}, {320, 240}, {427, 240}, {480, 270}, {640, 360}, {854, 480}, {960, 540}, {1280, 720}, {1920, 1080}};

    @Test
    void everyPartLiesInsideThePanelAndTheScreen() {
        for (int[] s : SIZES) {
            BookLayout l = BookLayout.of(s[0], s[1]);
            String at = s[0] + "x" + s[1];
            BookLayout.Rect screen = new BookLayout.Rect(0, 0, s[0], s[1]);
            assertTrue(l.panel().inside(screen), at + " panel " + l.panel());
            for (BookLayout.Rect r : new BookLayout.Rect[]{l.header(), l.list(), l.pane(), l.footerLeft(), l.footerRight()}) assertTrue(r.inside(l.panel()), at + " part " + r);
        }
    }

    @Test
    void theHeaderTheTwoPanesAndTheFooterDoNotOverlap() {
        for (int[] s : SIZES) {
            BookLayout l = BookLayout.of(s[0], s[1]);
            String at = s[0] + "x" + s[1];
            assertFalse(l.list().overlaps(l.pane()), at + " list and pane");
            assertFalse(l.header().overlaps(l.list()) || l.header().overlaps(l.pane()), at + " header over the body");
            assertFalse(l.footerLeft().overlaps(l.list()) || l.footerRight().overlaps(l.pane()), at + " footer over the body");
            assertFalse(l.footerLeft().overlaps(l.footerRight()), at + " footer halves");
        }
    }

    @Test
    void thePanesKeepAUsableSize() {
        for (int[] s : SIZES) {
            BookLayout l = BookLayout.of(s[0], s[1]);
            String at = s[0] + "x" + s[1];
            assertTrue(l.list().w() >= BookLayout.MIN_LIST_W && l.list().w() <= BookLayout.MAX_LIST_W, at + " list width " + l.list().w());
            assertTrue(l.pane().w() >= 120, at + " pane width " + l.pane().w());
            assertTrue(l.pane().h() >= 80, at + " pane height " + l.pane().h());
            assertTrue(l.visibleRows() >= 5, at + " rows " + l.visibleRows());
        }
    }

    @Test
    void theButtonsFitTheFooterSideBySide() {
        for (int[] s : SIZES) {
            BookLayout l = BookLayout.of(s[0], s[1]);
            for (int n = 1; n <= 3; n++) {
                BookLayout.Rect prev = null;
                for (int i = 0; i < n; i++) {
                    BookLayout.Rect b = l.button(i, n);
                    assertTrue(b.inside(l.footerRight()), s[0] + "x" + s[1] + " button " + i + " of " + n);
                    assertTrue(b.w() >= 40, "a button needs room for its label: " + b);
                    if (prev != null) assertFalse(prev.overlaps(b), "buttons overlap");
                    prev = b;
                }
            }
        }
    }

    @Test
    void thePanelIsCentredAndCapped() {
        BookLayout big = BookLayout.of(1920, 1080);
        assertEquals(BookLayout.MAX_PANEL_W, big.panel().w());
        assertEquals(BookLayout.MAX_PANEL_H, big.panel().h());
        assertEquals((1920 - BookLayout.MAX_PANEL_W) / 2, big.panel().x());
        assertEquals((1080 - BookLayout.MAX_PANEL_H) / 2, big.panel().y());
        BookLayout tiny = BookLayout.of(200, 120);
        assertTrue(tiny.panel().inside(new BookLayout.Rect(0, 0, 200, 120)), "a window smaller than the minimum still gets a panel inside it");
    }

    @Test
    void rectHelpers() {
        BookLayout.Rect r = new BookLayout.Rect(10, 20, 30, 40);
        assertTrue(r.contains(10, 20) && r.contains(39.9, 59.9));
        assertFalse(r.contains(40, 20) || r.contains(10, 60) || r.contains(9.9, 30));
        assertTrue(r.overlaps(new BookLayout.Rect(39, 59, 5, 5)));
        assertFalse(r.overlaps(new BookLayout.Rect(40, 20, 5, 5)));
        assertEquals(40, r.right());
        assertEquals(60, r.bottom());
    }
}
