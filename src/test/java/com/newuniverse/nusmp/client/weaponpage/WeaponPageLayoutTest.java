package com.newuniverse.nusmp.client.weaponpage;

import com.newuniverse.nusmp.client.heroweapon.forms.BookLayout.Rect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Weapon page must fit every spread the Codex can give it and every number of weapons, options and bars: nothing runs outside the area, nothing overlaps, a click lands on the row that was drawn,
 * and the height it asks for is exactly what its last rectangle needs. Pure: no game types.
 */
class WeaponPageLayoutTest {
    private static final int[] WIDTHS = {120, 150, 188, 240, 320, 400, 700};
    private static final int[] CHIPS = {0, 1, 2, 3, 6};

    private static boolean insideX(Rect r, WeaponPageLayout l) { return r.x() >= l.x() && r.right() <= l.x() + l.w(); }

    @Test
    void everyRectangleStaysInsideTheAreaAndNothingOverlaps() {
        for (int w : WIDTHS) {
            WeaponPageLayout l = WeaponPageLayout.of(10, 20, w);
            assertTrue(l.leftW() > 0 && l.rightW() > 0 && l.leftW() + WeaponPageLayout.GAP + l.rightW() == l.w(), "columns at " + w);
            for (int chips : CHIPS) {
                for (int i = 0; i < chips; i++) {
                    Rect c = l.chip(i, chips);
                    assertTrue(insideX(c, l) || w < 6 * (WeaponPageLayout.MIN_CHIP_W + WeaponPageLayout.CHIP_GAP), "chip " + i + "/" + chips + " at " + w);
                    assertTrue(c.w() >= WeaponPageLayout.MIN_CHIP_W && c.w() <= WeaponPageLayout.MAX_CHIP_W);
                    if (i > 0) assertFalse(c.overlaps(l.chip(i - 1, chips)), "chips overlap");
                }
                int options = 24, bars = 4, extras = 5;
                Rect head = l.head(chips);
                assertTrue(insideX(head, l) && head.right() <= l.rightX(), "head");
                for (int i = 0; i < options; i++) {
                    Rect o = l.option(i, chips);
                    assertTrue(insideX(o, l) && o.right() <= l.rightX(), "option column");
                    assertFalse(o.overlaps(head), "option over the head");
                    if (i > 0) assertFalse(o.overlaps(l.option(i - 1, chips)), "options overlap");
                }
                for (int i = 0; i < bars; i++) {
                    Rect b = l.bar(i, chips);
                    assertTrue(insideX(b, l) && b.x() >= l.x() + l.leftW(), "bar column");
                    if (i > 0) assertFalse(b.overlaps(l.bar(i - 1, chips)), "bars overlap");
                }
                for (int i = 0; i < extras; i++) {
                    Rect e = l.extra(i, bars, chips);
                    assertTrue(insideX(e, l), "extra column");
                    assertFalse(e.overlaps(l.bar(bars - 1, chips)), "extra over a bar");
                    if (i > 0) assertFalse(e.overlaps(l.extra(i - 1, bars, chips)), "extras overlap");
                }
                assertTrue(l.extraHeadingY(bars, chips) + WeaponPageLayout.HEADING_H <= l.extra(0, bars, chips).y(), "the heading sits above the first extra");
            }
        }
    }

    @Test
    void theHeightIsWhatTheLastRectangleNeeds() {
        WeaponPageLayout l = WeaponPageLayout.of(0, 0, 300);
        for (int chips : CHIPS) {
            for (int options : new int[] {0, 1, 9, 24}) {
                for (int bars : new int[] {1, 4}) {
                    for (int extras : new int[] {0, 3}) {
                        int h = l.height(chips, options, bars, extras);
                        int leftEnd = options == 0 ? l.head(chips).bottom() : l.option(options - 1, chips).bottom() + 1;
                        int rightEnd = extras == 0 ? l.bar(bars - 1, chips).bottom() : l.extra(extras - 1, bars, chips).bottom();
                        assertTrue(h >= Math.max(leftEnd, rightEnd) - l.y(), "height " + h + " too small for " + chips + "/" + options + "/" + bars + "/" + extras);
                        assertTrue(h <= Math.max(leftEnd, rightEnd) - l.y() + 6, "height " + h + " has a gap for " + chips + "/" + options + "/" + bars + "/" + extras);
                    }
                }
            }
        }
    }

    @Test
    void aClickLandsOnTheRowThatWasDrawn() {
        WeaponPageLayout l = WeaponPageLayout.of(5, 7, 250);
        for (int chips : new int[] {1, 3}) {
            for (int i = 0; i < 9; i++) {
                Rect o = l.option(i, chips);
                assertEquals(i, l.optionAt(o.x() + 1, o.y() + 1, 9, chips));
                assertEquals(i, l.optionAt(o.right() - 1, o.bottom() - 1, 9, chips));
            }
            assertEquals(-1, l.optionAt(l.x() + 2, l.head(chips).y() + 3, 9, chips), "the head is not a row");
            assertEquals(-1, l.optionAt(l.rightX() + 2, l.option(0, chips).y() + 2, 9, chips), "the right column is not a row");
            assertEquals(-1, l.optionAt(l.x() + 2, l.option(9, chips).y() + 2, 9, chips), "past the last row");
        }
        for (int n : new int[] {2, 6}) {
            for (int i = 0; i < n; i++) {
                Rect c = l.chip(i, n);
                assertEquals(i, l.chipAt(c.x() + 1, c.y() + 1, n));
            }
            assertEquals(-1, l.chipAt(l.x() + 1, l.y() + WeaponPageLayout.CHIP_H + 2, n));
        }
    }
}
