package com.newuniverse.nusmp.client.ui.codex;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The two-column flow of the generated pages and the way a rule value is written. */
class CodexLayoutTest {
    @Test
    void blocksGoToTheShorterColumnAndTiesToTheLeft() {
        CodexLayout.Placement p = CodexLayout.place(new int[]{50, 10, 10, 10}, 0);
        assertEquals(0, p.column()[0]);
        assertEquals(1, p.column()[1]);
        assertEquals(1, p.column()[2]);
        assertEquals(1, p.column()[3]);
        assertEquals(0, p.y()[0]);
        assertEquals(0, p.y()[1]);
        assertEquals(10, p.y()[2]);
        assertEquals(20, p.y()[3]);
        assertEquals(50, p.total());
    }

    @Test
    void gapIsAddedAfterEveryBlockAndTotalIsTheTallerColumn() {
        CodexLayout.Placement p = CodexLayout.place(new int[]{10, 10, 10}, 4);
        assertEquals(14, p.y()[2]);
        assertEquals(28, p.total());
    }

    @Test
    void emptyAndNegativeHeights() {
        assertEquals(0, CodexLayout.place(new int[0], 3).total());
        CodexLayout.Placement p = CodexLayout.place(new int[]{-5, 10}, 0);
        assertEquals(0, p.y()[0]);
        assertEquals(10, p.total());
    }

    @Test
    void ruleValuesSayDefaultWhenTheyAreNotLive() {
        assertEquals("ON", CodexRuleValue.bool(true, true));
        assertEquals("OFF (default)", CodexRuleValue.bool(false, false));
        assertEquals("6%", CodexRuleValue.percent(0.06, true));
        assertEquals("6% (default)", CodexRuleValue.percent(0.06, false));
        assertEquals("2.5 x", CodexRuleValue.number(2.5, "x", true));
        assertEquals("20 s (default)", CodexRuleValue.number(20, "s", false));
        assertEquals("3", CodexRuleValue.number(3, "", true));
        assertEquals("0.12", CodexRuleValue.trim(0.123456));
    }

    @Test
    void contentsListIsReadDownTheLeftColumnThenTheRight() {
        assertEquals(19, CodexLayout.tocRows(37));
        assertEquals(0, CodexLayout.tocRows(0));
        assertEquals(0, CodexLayout.tocIndexAt(37, 5, 3, 100, 11));        // top of the left column
        assertEquals(1, CodexLayout.tocIndexAt(37, 5, 12, 100, 11));       // second line
        assertEquals(19, CodexLayout.tocIndexAt(37, 105, 0, 100, 11));     // top of the right column
        assertEquals(36, CodexLayout.tocIndexAt(37, 105, 17 * 11, 100, 11));
        assertEquals(-1, CodexLayout.tocIndexAt(37, 105, 18 * 11, 100, 11)); // the right column is one entry shorter
        assertEquals(-1, CodexLayout.tocIndexAt(37, -1, 0, 100, 11));
        assertEquals(-1, CodexLayout.tocIndexAt(37, 5, 19 * 11, 100, 11));
        assertEquals(-1, CodexLayout.tocIndexAt(0, 5, 0, 100, 11));
    }
}
