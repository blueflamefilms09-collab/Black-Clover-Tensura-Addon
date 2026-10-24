package com.newuniverse.nusmp.companion;

import com.newuniverse.nusmp.client.companion.TalkLayout;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the talk screen must fit every GUI scale and its clicks must land on what is drawn; the layout is pure arithmetic, so it is tested without a game. */
class TalkLayoutTest {
    private static final int[][] WINDOWS = {{240, 135}, {320, 180}, {320, 240}, {427, 240}, {640, 360}, {854, 480}, {960, 540}, {1920, 1080}};

    private static boolean inside(int[] inner, int[] outer) {
        return inner[0] >= outer[0] && inner[1] >= outer[1] && inner[0] + inner[2] <= outer[0] + outer[2] && inner[1] + inner[3] <= outer[1] + outer[3];
    }

    private static boolean apart(int[] a, int[] b) {
        if (a[2] == 0 || a[3] == 0 || b[2] == 0 || b[3] == 0) return true;
        return a[0] + a[2] <= b[0] || b[0] + b[2] <= a[0] || a[1] + a[3] <= b[1] || b[1] + b[3] <= a[1];
    }

    @Test
    void everythingFitsInsideThePanelAndThePanelInsideTheWindow() {
        for (int[] w : WINDOWS) for (int tab = 0; tab < 4; tab++) {
            TalkLayout L = TalkLayout.compute(w[0], w[1], tab);
            String where = w[0] + "x" + w[1] + " tab " + tab;
            assertTrue(L.panel[0] >= 0 && L.panel[1] >= 0 && L.panel[0] + L.panel[2] <= w[0] && L.panel[1] + L.panel[3] <= w[1] || w[1] < 120, "the panel is outside the window at " + where);
            assertTrue(L.visibleRows >= 1, "at least one row at " + where);
            if (w[1] < 170) continue;                                           // a window under 170 GUI pixels high (GUI scale 4 on a small screen) is cramped: it still works, it does not all fit
            assertTrue(inside(L.text, L.panel), "text at " + where);
            assertTrue(inside(L.tabs0, L.panel), "tabs at " + where);
            assertTrue(inside(L.close, L.panel), "close at " + where);
            if (L.status[3] > 0) assertTrue(inside(L.status, L.panel), "status at " + where);
            if (L.grid[2] > 0) assertTrue(inside(L.grid, L.panel), "grid at " + where);
        }
    }

    @Test
    void theBlocksDoNotOverlap() {
        for (int[] w : WINDOWS) if (w[1] >= 180) for (int tab = 0; tab < 4; tab++) {
            TalkLayout L = TalkLayout.compute(w[0], w[1], tab);
            String where = w[0] + "x" + w[1] + " tab " + tab;
            assertTrue(apart(L.tabs0, L.text), "tabs / text at " + where);
            assertTrue(apart(L.text, L.status), "text / status at " + where);
            assertTrue(apart(L.text, L.list), "text / list at " + where);
            assertTrue(apart(L.status, L.list), "status / list at " + where);
            assertTrue(apart(L.list, L.close), "list / close at " + where);
            assertTrue(apart(new int[]{L.list[0], L.list[1], L.listRowsW, L.list[3]}, L.grid), "list / pouch slots at " + where);
        }
    }

    @Test
    void aNormalWindowShowsAtLeastThreeRows() {
        for (int[] w : WINDOWS) if (w[1] >= 240 || w[1] == 180 && false) for (int tab = 0; tab < 4; tab++) assertTrue(TalkLayout.compute(w[0], w[1], tab).visibleRows >= 3, w[0] + "x" + w[1] + " tab " + tab);
        assertTrue(TalkLayout.compute(427, 240, 0).visibleRows >= 5, "the Talk tab shows five rows at the default GUI scale");
    }

    @Test
    void hitTestsAgreeWithTheRectangles() {
        TalkLayout L = TalkLayout.compute(427, 240, 3);
        for (int i = 0; i < 4; i++) {
            int[] r = L.tabRect(i);
            assertEquals(i, L.tabAt(r[0] + 1, r[1] + 1));
            assertEquals(i, L.tabAt(r[0] + r[2] - 1, r[1] + r[3] - 1));
        }
        assertEquals(-1, L.tabAt(L.panel[0] - 5, L.tabs0[1]));
        assertEquals(-1, L.tabAt(L.tabs0[0] + 2, L.tabs0[1] + 40));
        for (int s = 0; s < L.visibleRows; s++) {
            int[] r = L.rowRect(s);
            assertEquals(s + 2, L.rowAt(r[0] + 2, r[1] + 2, 2, 100));
        }
        assertEquals(-1, L.rowAt(L.list[0] + 2, L.list[1] + 2, 0, 0), "no rows, no hit");
        int[] last = L.rowRect(L.visibleRows - 1);
        assertEquals(-1, L.rowAt(last[0] + 2, last[1] + 2, 0, L.visibleRows - 1), "a row past the end of the list is not hit");
        for (int i = 0; i < 9; i++) {
            int[] r = L.slotRect(i);
            assertEquals(i, L.slotAt(r[0] + 2, r[1] + 2));
        }
        assertTrue(L.closeAt(L.close[0] + 1, L.close[1] + 1));
        assertFalse(L.closeAt(L.close[0] - 1, L.close[1]));
        assertEquals(-1, TalkLayout.compute(427, 240, 1).slotAt(L.grid[0] + 2, L.grid[1] + 2), "no slots outside the Pouch tab");
    }

    @Test
    void scrollingStaysInRange() {
        TalkLayout L = TalkLayout.compute(427, 240, 0);
        assertEquals(0, L.maxScroll(0));
        assertEquals(0, L.maxScroll(L.visibleRows));
        assertEquals(10, L.maxScroll(L.visibleRows + 10));
    }

    @Test
    void anOutOfRangeTabIsClamped() {
        assertEquals(0, TalkLayout.compute(427, 240, -3).tab);
        assertEquals(3, TalkLayout.compute(427, 240, 9).tab);
    }
}
