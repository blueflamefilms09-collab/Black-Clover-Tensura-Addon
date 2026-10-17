package com.newuniverse.nusmp.avatar.menu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * The screen layout swept over every window size a player could have: nothing leaves the window, the three columns and the two bars never overlap, the five
 * round buttons sit inside the top bar and never touch each other, and every icon grid keeps at least three columns and answers hit tests consistently.
 */
class ForgeLayoutTest {
    private static boolean inside(ForgeLayout.Rect r, int w, int h) { return r.isEmpty() || (r.x() >= 0 && r.y() >= 0 && r.right() <= w && r.bottom() <= h); }

    @Test
    void everyWindowSizeKeepsEverythingInsideTheWindow() {
        int checked = 0;
        for (int w = 160; w <= 1920; w += 7) {
            for (int h = 120; h <= 1080; h += 11) {
                ForgeLayout.Layout l = ForgeLayout.compute(w, h);
                String at = w + "x" + h;
                assertTrue(inside(l.top(), l.width(), l.height()), "top " + at);
                assertTrue(inside(l.left(), l.width(), l.height()), "left " + at);
                assertTrue(inside(l.center(), l.width(), l.height()), "center " + at);
                assertTrue(inside(l.right(), l.width(), l.height()), "right " + at);
                assertTrue(inside(l.bottom(), l.width(), l.height()), "bottom " + at);
                for (ForgeLayout.Rect c : l.categories()) assertTrue(inside(c, l.width(), l.height()), "category " + at);
                checked++;
            }
        }
        assertTrue(checked > 10000);
    }

    @Test
    void theColumnsAndBarsNeverOverlap() {
        for (int w = 160; w <= 1920; w += 13) {
            for (int h = 120; h <= 1080; h += 17) {
                ForgeLayout.Layout l = ForgeLayout.compute(w, h);
                String at = w + "x" + h;
                assertFalse(l.left().overlaps(l.center()), "left/center " + at);
                assertFalse(l.center().overlaps(l.right()), "center/right " + at);
                assertFalse(l.left().overlaps(l.right()), "left/right " + at);
                for (ForgeLayout.Rect col : new ForgeLayout.Rect[]{l.left(), l.center(), l.right()}) {
                    assertFalse(col.overlaps(l.top()), "column/top " + at);
                    assertFalse(col.overlaps(l.bottom()), "column/bottom " + at);
                }
                assertFalse(l.top().overlaps(l.bottom()), "top/bottom " + at);
            }
        }
    }

    @Test
    void theAvatarKeepsAStageOfAtLeastEightyPixelsAndTheModuleANormalWidth() {
        for (int w = 320; w <= 1920; w += 5) {
            ForgeLayout.Layout l = ForgeLayout.compute(w, 480);
            assertTrue(l.center().w() >= 80, "center width at " + w + " = " + l.center().w());
            assertTrue(l.left().w() >= 110, "module width at " + w + " = " + l.left().w());
            assertTrue(l.center().h() >= 40);
        }
    }

    @Test
    void theLedgerSitsBesideTheAvatarOnlyInAWideWindow() {
        assertFalse(ForgeLayout.compute(419, 480).ledgerBeside());
        assertTrue(ForgeLayout.compute(419, 480).right().isEmpty());
        assertTrue(ForgeLayout.compute(420, 480).ledgerBeside());
        assertFalse(ForgeLayout.compute(420, 480).right().isEmpty());
    }

    @Test
    void theFiveRoundButtonsAreCentredInTheTopBarInOrderAndNeverTouch() {
        for (int w = 160; w <= 1920; w += 9) {
            for (int h = 120; h <= 1080; h += 23) {
                ForgeLayout.Layout l = ForgeLayout.compute(w, h);
                ForgeLayout.Rect[] c = l.categories();
                assertEquals(5, c.length);
                String at = w + "x" + h;
                for (int i = 0; i < 5; i++) {
                    assertTrue(c[i].w() == c[i].h() && c[i].w() >= 16, "round button is a square of at least 16 at " + at);
                    assertTrue(c[i].y() >= 0 && c[i].bottom() <= l.top().h(), "inside the top bar at " + at);
                    if (i > 0) {
                        assertTrue(c[i].x() >= c[i - 1].right() + 2, "gap between buttons at " + at);
                        assertEquals(c[i - 1].y(), c[i].y(), "same row at " + at);
                    }
                }
                int mid = (c[0].x() + c[4].right()) / 2;
                assertTrue(Math.abs(mid - w / 2) <= 1, "centred at " + at + " (mid " + mid + ")");
            }
        }
    }

    @Test
    void aTinyOrNonsenseWindowStillGivesALayout() {
        ForgeLayout.Layout a = ForgeLayout.compute(0, 0);
        assertEquals(160, a.width());
        assertEquals(120, a.height());
        ForgeLayout.Layout b = ForgeLayout.compute(-50, 9999);
        assertEquals(160, b.width());
        assertNotNull(b.center());
        assertEquals(5, b.categories().length);
    }

    @Test
    void theLeftBodyIsInsideTheModule() {
        for (int w = 200; w <= 1600; w += 31) {
            ForgeLayout.Layout l = ForgeLayout.compute(w, 360);
            ForgeLayout.Rect b = l.leftBody();
            assertTrue(b.x() >= l.left().x() && b.right() <= l.left().right() && b.y() >= l.left().y() && b.bottom() <= l.left().bottom(), "body inside module at " + w);
        }
    }

    @Test
    void theBottomBarButtonsFitInsideItAndNeverTouchEachOther() {
        for (int w = 160; w <= 1920; w += 3) {
            for (int h = 120; h <= 1080; h += 41) {
                ForgeLayout.Layout l = ForgeLayout.compute(w, h);
                String at = w + "x" + h;
                ForgeLayout.Rect[] bs = {l.back(), l.next(), l.finalizeButton()};
                for (ForgeLayout.Rect b : bs) {
                    assertFalse(b.isEmpty(), "button has a size at " + at);
                    assertTrue(b.x() >= 0 && b.right() <= l.width() && b.y() >= l.bottom().y() && b.bottom() <= l.bottom().bottom(), "button inside the bar at " + at);
                    assertTrue(b.h() >= 14, "button tall enough to click at " + at);
                }
                assertFalse(bs[0].overlaps(bs[1]), "back/next " + at);
                assertFalse(bs[1].overlaps(bs[2]), "next/finalize " + at);
                assertFalse(bs[0].overlaps(bs[2]), "back/finalize " + at);
                if (!l.message().isEmpty()) {
                    assertFalse(l.message().overlaps(bs[0]) || l.message().overlaps(bs[1]) || l.message().overlaps(bs[2]), "message clear of buttons at " + at);
                }
            }
        }
    }

    @Test
    void theCostPanelAndTheStageShareTheCentreColumnWithoutOverlap() {
        for (int w = 160; w <= 1920; w += 11) {
            for (int h = 120; h <= 1080; h += 29) {
                ForgeLayout.Layout l = ForgeLayout.compute(w, h);
                String at = w + "x" + h;
                assertFalse(l.ledger().isEmpty(), "a cost panel exists at " + at);
                assertFalse(l.stage().isEmpty(), "a stage exists at " + at);
                assertTrue(inside(l.ledger(), l.width(), l.height()) && inside(l.stage(), l.width(), l.height()), "inside the window at " + at);
                assertFalse(l.ledger().overlaps(l.stage()), "ledger/stage at " + at);
                assertFalse(l.ledger().overlaps(l.left()), "ledger/module at " + at);
                assertFalse(l.stage().overlaps(l.left()), "stage/module at " + at);
                assertFalse(l.ledger().overlaps(l.top()) || l.ledger().overlaps(l.bottom()), "ledger clear of the bars at " + at);
                assertEquals(l.ledgerBeside(), l.ledger() == l.right() || l.ledger().equals(l.right()), "panel sits in the right column exactly when there is one at " + at);
            }
        }
    }

    @Test
    void theScreenZoomTurnsAnyWindowIntoACanvasNearTheDesignSize() {
        assertEquals(1, ForgeLayout.uiScale(427, 240));
        assertEquals(1, ForgeLayout.uiScale(480, 270));
        assertEquals(1, ForgeLayout.uiScale(640, 360));
        assertEquals(2, ForgeLayout.uiScale(854, 480));
        assertEquals(2, ForgeLayout.uiScale(960, 540));
        assertEquals(3, ForgeLayout.uiScale(1280, 720));
        assertEquals(4, ForgeLayout.uiScale(1920, 1080));
        assertEquals(4, ForgeLayout.uiScale(5000, 3000), "capped");
        assertEquals(1, ForgeLayout.uiScale(0, 0));
        assertEquals(1, ForgeLayout.uiScale(-5, 400));
        for (int w = 320; w <= 3840; w += 13) {
            for (int h = 200; h <= 2160; h += 29) {
                int z = ForgeLayout.uiScale(w, h);
                assertTrue(z >= 1 && z <= 4);
                if (z < 4) {
                    // the canvas is never smaller than the design size for a window that could hold it, and never so big that it is a window of its own
                    float cw = w / (float) z, ch = h / (float) z;
                    assertTrue(Math.min(cw / 430f, ch / 243f) < 2.0f, "canvas " + cw + "x" + ch + " from " + w + "x" + h + " at " + z);
                }
                assertTrue(z == 1 || Math.min(w / (float) z / 430f, h / (float) z / 243f) >= 0.88f, "a zoomed canvas keeps at least 88% of the design size: " + w + "x" + h + " at " + z);
            }
        }
    }

    @Test
    void iconSizesGrowWithTheWindowButStayInTheirBounds() {
        int prevIcon = 0, prevSlot = 0;
        for (int w = 320; w <= 1920; w += 16) {
            int h = w * 9 / 16;
            ForgeLayout.Layout l = ForgeLayout.compute(w, h);
            assertTrue(l.icon() >= 26 && l.icon() <= 46, "icon " + l.icon() + " at " + w);
            assertTrue(l.slot() >= 30 && l.slot() <= 64, "slot " + l.slot() + " at " + w);
            assertTrue(l.icon() >= prevIcon && l.slot() >= prevSlot, "never shrinks as the window grows");
            assertTrue(l.slot() >= l.icon());
            prevIcon = l.icon();
            prevSlot = l.slot();
        }
        assertEquals(26, ForgeLayout.compute(320, 240).icon());
        assertEquals(46, ForgeLayout.compute(1920, 1080).icon());
    }

    @Test
    void aSlotRowIsCentredRoundAndInsideTheBody() {
        for (int w = 40; w <= 400; w += 7) {
            for (int n = 1; n <= 4; n++) {
                ForgeLayout.Rect body = new ForgeLayout.Rect(10, 20, w, 200);
                ForgeLayout.Rect[] row = ForgeLayout.slotRow(body, n, 40);
                assertEquals(n, row.length);
                for (int i = 0; i < n; i++) {
                    assertEquals(row[i].w(), row[i].h());
                    assertTrue(row[i].w() >= 20 && row[i].w() <= 64);
                    assertEquals(body.y(), row[i].y());
                    if (w >= n * 20 + (n - 1) * 2) assertTrue(row[i].x() >= body.x() && row[i].right() <= body.right(), "slot " + i + " inside at width " + w + " n " + n);
                    if (i > 0) assertTrue(row[i].x() >= row[i - 1].right() + 1, "slots do not touch");
                }
            }
        }
        ForgeLayout.Rect[] two = ForgeLayout.slotRow(new ForgeLayout.Rect(0, 0, 200, 100), 2, 40);
        assertEquals(100, (two[0].x() + two[1].right()) / 2, 1, "centred");
        assertEquals(two[0].h() + 12, ForgeLayout.slotRowHeight(two));
        assertEquals(0, ForgeLayout.slotRowHeight(new ForgeLayout.Rect[0]));
    }

    @Test
    void theAreaBelowASlotRowIsTheRestOfTheBody() {
        ForgeLayout.Rect body = new ForgeLayout.Rect(10, 20, 150, 200);
        ForgeLayout.Rect g = ForgeLayout.below(body, 52);
        assertEquals(body.x(), g.x());
        assertEquals(body.w(), g.w());
        assertEquals(body.y() + 56, g.y());
        assertEquals(body.bottom(), g.bottom());
        ForgeLayout.Rect none = ForgeLayout.below(new ForgeLayout.Rect(0, 0, 50, 30), 100);
        assertEquals(0, none.h());
        assertTrue(none.y() <= 30);
    }

    // ================================================================ the icon grids
    @Test
    void aGridKeepsAtLeastThreeColumnsWheneverTheWidthAllowsIt() {
        for (int w = 40; w <= 400; w++) {
            ForgeLayout.Grid g = ForgeLayout.Grid.fit(new ForgeLayout.Rect(10, 10, w, 120), 28, 4, 30);
            if (w >= 3 * 12 + 2 * 4) assertTrue(g.cols() >= 3, "cols " + g.cols() + " at width " + w);
            assertTrue(g.cols() >= 1);
            int used = g.cols() * g.cell() + (g.cols() - 1) * g.gap();
            assertTrue(used <= Math.max(w, g.cell()), "grid fits its width " + w + " (used " + used + ")");
        }
    }

    @Test
    void theRowsAreTheCeilingOfCountOverColumns() {
        ForgeLayout.Grid g = ForgeLayout.Grid.fit(new ForgeLayout.Rect(0, 0, 130, 100), 28, 4, 30);
        assertEquals(4, g.cols());
        assertEquals(8, g.rows());
        assertEquals(0, ForgeLayout.Grid.fit(new ForgeLayout.Rect(0, 0, 130, 100), 28, 4, 0).rows());
        assertEquals(0, ForgeLayout.Grid.fit(new ForgeLayout.Rect(0, 0, 130, 100), 28, 4, -5).count());
    }

    @Test
    void scrollingIsClampedToTheRowsThatDoNotFit() {
        ForgeLayout.Grid g = ForgeLayout.Grid.fit(new ForgeLayout.Rect(0, 0, 130, 100), 28, 4, 30);
        assertEquals(3, g.visibleRows());
        assertEquals(5, g.maxScroll());
        assertEquals(0, g.clampScroll(-3));
        assertEquals(5, g.clampScroll(99));
        assertEquals(2, g.clampScroll(2));
        ForgeLayout.Grid few = ForgeLayout.Grid.fit(new ForgeLayout.Rect(0, 0, 130, 100), 28, 4, 5);
        assertEquals(0, few.maxScroll());
    }

    @Test
    void everyVisibleCellIsFoundAtItsOwnCentreAndGapsAndOutsideFindNothing() {
        for (int w = 90; w <= 300; w += 10) {
            for (int count : new int[]{1, 2, 7, 30, 42}) {
                ForgeLayout.Grid g = ForgeLayout.Grid.fit(new ForgeLayout.Rect(20, 30, w, 110), 30, 4, count);
                for (int scroll = 0; scroll <= g.maxScroll(); scroll++) {
                    for (int i = 0; i < count; i++) {
                        ForgeLayout.Rect r = g.cellRect(i, scroll);
                        int row = i / g.cols() - scroll;
                        boolean visible = row >= 0 && row < g.visibleRows();
                        int found = g.indexAt(r.cx(), r.cy(), scroll);
                        if (visible) assertEquals(i, found, "cell " + i + " scroll " + scroll + " width " + w + " count " + count);
                        else assertEquals(-1, found, "hidden cell " + i);
                    }
                    assertEquals(-1, g.indexAt(5, 5, scroll), "outside");
                    assertEquals(-1, g.indexAt(1000, 1000, scroll), "outside");
                }
            }
        }
    }

    @Test
    void aPointInTheGapBetweenTwoCellsHitsNeither() {
        ForgeLayout.Grid g = ForgeLayout.Grid.fit(new ForgeLayout.Rect(0, 0, 200, 100), 30, 6, 12);
        ForgeLayout.Rect a = g.cellRect(0, 0);
        assertEquals(-1, g.indexAt(a.right() + 2, a.cy(), 0), "in the gap");
        assertEquals(0, g.indexAt(a.right() - 1, a.cy(), 0), "last pixel of the cell");
    }

    @Test
    void aCellNeverLeavesTheAreaHorizontally() {
        for (int w = 40; w <= 360; w += 3) {
            ForgeLayout.Rect area = new ForgeLayout.Rect(15, 15, w, 90);
            ForgeLayout.Grid g = ForgeLayout.Grid.fit(area, 26, 4, 25);
            for (int i = 0; i < 25; i++) {
                ForgeLayout.Rect r = g.cellRect(i, 0);
                if (w >= 3 * 12 + 2 * 4) assertTrue(r.x() >= area.x() && r.right() <= area.right(), "cell " + i + " inside area at width " + w);
            }
        }
    }
}
