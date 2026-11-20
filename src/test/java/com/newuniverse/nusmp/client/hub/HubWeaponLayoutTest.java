package com.newuniverse.nusmp.client.hub;

import com.newuniverse.nusmp.client.heroweapon.forms.BookLayout.Rect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Weapons tab must fit any number of weapons (0 to 6), options (0 to 24), bars (0 to 8) and other tracks (0 to 24) at any width the hub gives it, with nothing overlapping and nothing outside the width,
 * a click landing on the row that was drawn, and the height being exactly what the last section needs. The old page could overlap on a narrow spread; this is the proof the new one cannot. Pure: no game types.
 */
class HubWeaponLayoutTest {
    private static final int[] WIDTHS = {44, 90, 140, 244, 320, 700};
    private static final int[] CHIPS = {0, 1, 2, 3, 6}, OPTIONS = {0, 1, 5, 24}, BARS = {0, 1, 4, 8}, EXTRAS = {0, 1, 3, 8, 24};

    private static void collect(HubWeaponLayout l, java.util.List<Rect> out) {
        if (l.hasChips()) for (int i = 0; i < l.chips(); i++) out.add(l.chip(i));
        out.add(l.head());
        for (int i = 0; i < l.options(); i++) out.add(l.option(i));
        for (int i = 0; i < l.bars(); i++) out.add(l.bar(i));
        for (int i = 0; i < l.extras(); i++) out.add(l.extra(i));
    }

    @Test
    void nothingOverlapsAndEverythingIsInsideTheWidthForEveryCount() {
        for (int w : WIDTHS) for (int c : CHIPS) for (int o : OPTIONS) for (int b : BARS) for (int e : EXTRAS) {
            HubWeaponLayout l = HubWeaponLayout.of(w, c, o, b, e);
            java.util.List<Rect> all = new java.util.ArrayList<>();
            collect(l, all);
            Rect area = new Rect(0, 0, l.w(), l.height());
            String why = " w=" + w + " chips=" + c + " options=" + o + " bars=" + b + " extras=" + e;
            for (int i = 0; i < all.size(); i++) {
                assertTrue(all.get(i).inside(area), "rect " + i + " " + all.get(i) + " outside " + area + why);
                for (int j = i + 1; j < all.size(); j++) assertFalse(all.get(i).overlaps(all.get(j)), "rects " + i + " and " + j + " overlap" + why);
            }
            if (o > 0) assertTrue(l.optionsHeadingY() + HubWeaponLayout.HEADING_H <= l.option(0).y(), "options heading clear of the first row" + why);
            if (b > 0) assertTrue(l.barsHeadingY() + HubWeaponLayout.HEADING_H <= l.bar(0).y(), "bars heading clear of the first bar" + why);
            if (e > 0) assertTrue(l.extrasHeadingY() + HubWeaponLayout.HEADING_H <= l.extra(0).y(), "extras heading clear of the first bar" + why);
            if (b > 0 && o > 0) assertTrue(l.option(o - 1).bottom() < l.barsHeadingY(), "bars heading under the last option" + why);
            if (e > 0 && b > 0) assertTrue(l.bar(b - 1).bottom() < l.extrasHeadingY(), "extras heading under the last bar" + why);
        }
    }

    @Test
    void chipsKeepTheirSizeAndWrapInsteadOfShrinkingToNothing() {
        for (int w : WIDTHS) for (int c : new int[]{2, 3, 6}) {
            HubWeaponLayout l = HubWeaponLayout.of(w, c, 0, 0, 0);
            assertTrue(l.chipW() >= HubWeaponLayout.MIN_CHIP_W && l.chipW() <= HubWeaponLayout.MAX_CHIP_W, "chip width " + l.chipW() + " at " + w);
            assertEquals(c, l.chipRows() * l.chipsPerRow() >= c ? c : -1, "all chips have a slot");
            assertTrue(l.chipRows() >= 1 && l.chipsH() >= HubWeaponLayout.CHIP_H);
        }
        assertEquals(1, HubWeaponLayout.of(244, 3, 0, 0, 0).chipRows(), "three chips share one row at the hub's width");
        assertEquals(0, HubWeaponLayout.of(244, 1, 0, 0, 0).chipsH(), "one weapon shows no chips");
    }

    @Test
    void aClickLandsOnTheRowThatWasDrawn() {
        HubWeaponLayout l = HubWeaponLayout.of(244, 3, 24, 4, 3);
        for (int i = 0; i < 3; i++) { Rect r = l.chip(i); assertEquals(i, l.chipAt(r.x() + r.w() / 2.0, r.y() + r.h() / 2.0)); }
        for (int i = 0; i < 24; i++) { Rect r = l.option(i); assertEquals(i, l.optionAt(r.x() + r.w() / 2.0, r.y() + r.h() / 2.0)); }
        assertEquals(-1, l.optionAt(5, l.head().y() + 5), "the head is not an option");
        assertEquals(-1, HubWeaponLayout.of(244, 1, 3, 0, 0).chipAt(2, 2), "no chips with one weapon");
    }

    @Test
    void theHeightIsWhatTheLastSectionNeeds() {
        for (int c : CHIPS) for (int o : OPTIONS) for (int b : BARS) for (int e : EXTRAS) {
            HubWeaponLayout l = HubWeaponLayout.of(244, c, o, b, e);
            int last = e > 0 ? l.extra(e - 1).bottom() : b > 0 ? l.bar(b - 1).bottom() : o > 0 ? l.option(o - 1).bottom() : l.head().bottom();
            assertTrue(l.height() >= last && l.height() <= last + 3 + HubWeaponLayout.GAP, "height " + l.height() + " vs last bottom " + last);
        }
        assertTrue(HubWeaponLayout.of(244, 6, 24, 8, 24).height() > 160, "the biggest page is taller than the body and scrolls");
    }
}
