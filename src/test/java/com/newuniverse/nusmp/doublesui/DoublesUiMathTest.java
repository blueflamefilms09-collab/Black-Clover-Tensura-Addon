package com.newuniverse.nusmp.doublesui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The selection cycle and the stack layout of the Real Double HUD (docs/doubles_ui.md): pure arithmetic, no Minecraft class. */
class DoublesUiMathTest {
    private static final int AUTO = DoublesUiMath.AUTO;

    @Test
    void theCycleWrapsThroughNone() {
        assertEquals(AUTO, DoublesUiMath.step(0, 1, 0));
        assertEquals(AUTO, DoublesUiMath.step(AUTO, 1, 0), "nothing to choose from");
        assertEquals(0, DoublesUiMath.step(AUTO, 1, 1));
        assertEquals(AUTO, DoublesUiMath.step(0, 1, 1), "one double: none, 0, none");
        int cur = AUTO;
        int[] seen = new int[4];
        for (int i = 0; i < 4; i++) { cur = DoublesUiMath.step(cur, 1, 3); seen[i] = cur; }
        assertEquals(0, seen[0]); assertEquals(1, seen[1]); assertEquals(2, seen[2]); assertEquals(AUTO, seen[3]);
        assertEquals(2, DoublesUiMath.step(AUTO, -1, 3), "previous from none reaches the last");
        assertEquals(AUTO, DoublesUiMath.step(0, -1, 3));
        assertEquals(0, DoublesUiMath.step(99, 1, 3), "a stale index reads as none");
    }

    @Test
    void emptyOneAndManyCollapseCorrectly() {
        assertEquals(0, DoublesUiMath.shown(0, 4));
        assertEquals(0, DoublesUiMath.hidden(0, 4));
        assertEquals(0, DoublesUiMath.totalHeight(0, 4, false));
        assertEquals(1, DoublesUiMath.shown(1, 4));
        assertEquals(0, DoublesUiMath.hidden(1, 4));
        assertEquals(4, DoublesUiMath.shown(9, 4));
        assertEquals(5, DoublesUiMath.hidden(9, 4));
        assertEquals(1, DoublesUiMath.shown(9, 0), "a limit of 0 still shows one");
        assertEquals(DoublesUiMath.MAX_ROWS, DoublesUiMath.shown(99, 99), "never more than 16");
        assertEquals(0, DoublesUiMath.hidden(99, 99), "hidden counts only up to the cap of 16");
        assertTrue(DoublesUiMath.totalHeight(9, 4, true) > DoublesUiMath.totalHeight(4, 4, true), "the '+N more' line adds height");
    }

    @Test
    void cardsNeverOverlapForAnyCountLimitOrSelection() {
        for (int n = 1; n <= DoublesUiMath.MAX_ROWS; n++)
            for (int max = 1; max <= DoublesUiMath.MAX_ROWS; max++)
                for (int active = -1; active < n; active++) {
                    boolean has = active >= 0;
                    int s = DoublesUiMath.shown(n, max);
                    int bottom = DoublesUiMath.HEADER_H;
                    boolean[] used = new boolean[n];
                    for (int slot = 0; slot < s; slot++) {
                        int top = DoublesUiMath.cardTop(slot, has);
                        assertTrue(top > bottom, "slot " + slot + " touches the one above (n=" + n + ")");
                        bottom = top + DoublesUiMath.cardHeight(slot, has);
                        int src = DoublesUiMath.source(slot, active, n);
                        assertFalse(used[src], "a double is drawn twice");
                        used[src] = true;
                    }
                    int total = DoublesUiMath.totalHeight(n, max, has);
                    assertTrue(total >= bottom);
                    if (DoublesUiMath.hidden(n, max) > 0) assertTrue(DoublesUiMath.cardTop(s, has) + DoublesUiMath.MORE_H <= total, "the '+N more' line is outside the stack");
                    if (has) assertEquals(active, DoublesUiMath.source(0, active, n), "the selected one is on top");
                }
    }

    @Test
    void theSelectedDoubleIsAlwaysAmongTheShownEvenWhenItIsTheLastOne() {
        assertEquals(7, DoublesUiMath.source(0, 7, 8));
        assertEquals(0, DoublesUiMath.source(1, 7, 8));
        assertEquals(6, DoublesUiMath.source(7, 7, 8));
    }

    @Test
    void barsAndTimersStayInRange() {
        assertEquals(0f, DoublesUiMath.frac(5f, 0f));
        assertEquals(0f, DoublesUiMath.frac(Float.NaN, 10f));
        assertEquals(1f, DoublesUiMath.frac(50f, 10f));
        assertEquals(0f, DoublesUiMath.frac(-3f, 10f));
        assertEquals(0.5f, DoublesUiMath.frac(5f, 10f));
        assertEquals(900, DoublesUiMath.lifeLeft(1200, 100, 400));
        assertEquals(0, DoublesUiMath.lifeLeft(100, 0, 5000));
        assertEquals(100, DoublesUiMath.lifeLeft(100, 50, 10), "a clock that went back never adds time");
    }
}
