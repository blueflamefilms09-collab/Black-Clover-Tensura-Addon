package com.newuniverse.nusmp.client.hub;

import com.newuniverse.nusmp.client.heroweapon.forms.BookLayout.Rect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the hub is a fixed stage scaled to the window, so the proof that it fits "any window" is (1) the stage always fits inside the window at the scale chosen, over a grid of window sizes from a tiny
 * laptop at a huge GUI scale to a 4K monitor, and (2) every part lies inside the stage and overlaps nothing on it. Pure: no game types.
 */
class HubLayoutTest {
    private static final int[][] WINDOWS = {{1, 1}, {120, 70}, {240, 135}, {284, 160}, {320, 180}, {400, 240}, {427, 240}, {480, 270}, {640, 360}, {854, 480}, {960, 540}, {1280, 720}, {1920, 1080}, {3840, 2160}, {1000, 200}, {200, 1000}};
    private static final Rect STAGE = new Rect(0, 0, HubLayout.STAGE_W, HubLayout.STAGE_H);

    @Test
    void theScaledStageAlwaysFitsTheWindowOnceTheWindowIsBigEnough() {
        for (int[] win : WINDOWS) {
            HubLayout.View v = HubLayout.view(win[0], win[1]);
            assertTrue(v.scale() >= HubLayout.MIN_SCALE && v.scale() <= HubLayout.MAX_SCALE, "scale " + v.scale());
            assertEquals(v.scale(), Math.round(v.scale() * 8f) / 8f, 1e-6, "scale snaps to eighths");
            float w = HubLayout.STAGE_W * v.scale(), h = HubLayout.STAGE_H * v.scale();
            boolean clamped = v.scale() == HubLayout.MIN_SCALE || v.scale() == HubLayout.MAX_SCALE;
            if (!clamped) {
                assertTrue(w <= win[0] + 1e-3 && h <= win[1] + 1e-3, "stage " + w + "x" + h + " does not fit " + win[0] + "x" + win[1]);
                assertTrue(v.ox() >= -1e-3 && v.oy() >= -1e-3, "stage starts outside the window");
                assertTrue(w >= win[0] - HubLayout.STAGE_W * 0.125f - 1e-3 || h >= win[1] - HubLayout.STAGE_H * 0.125f - 1e-3, "the stage should use the window (within one snap step)");
            }
        }
    }

    @Test
    void aClickMapsBackToTheStagePointItWasDrawnAt() {
        for (int[] win : WINDOWS) {
            HubLayout.View v = HubLayout.view(win[0], win[1]);
            for (boolean home : new boolean[]{true, false}) {
                for (int i = 0; i < HubNodes.COUNT; i++) {
                    float sx = HubLayout.nodeX(home, i), sy = HubLayout.nodeY(home, i);
                    double wx = v.ox() + sx * v.scale(), wy = v.oy() + sy * v.scale();
                    assertEquals(i, HubLayout.nodeAt(home, v.sx(wx), v.sy(wy)), "node " + i + " at " + win[0] + "x" + win[1]);
                }
            }
        }
    }

    @Test
    void nodesStayInsideTheStageAndNeverOverlapOrTouchTheCore() {
        for (boolean home : new boolean[]{true, false}) {
            int r = HubLayout.nodeR(home);
            for (int i = 0; i < HubNodes.COUNT; i++) {
                Rect n = new Rect(Math.round(HubLayout.nodeX(home, i)) - r, Math.round(HubLayout.nodeY(home, i)) - r, 2 * r, 2 * r);
                assertTrue(n.inside(STAGE), "node " + i + " leaves the stage (home=" + home + ")");
                if (!home) assertTrue(n.right() <= HubLayout.PANEL.x(), "rail node " + i + " runs into the page panel");
                for (int j = i + 1; j < HubNodes.COUNT; j++) {
                    Rect o = new Rect(Math.round(HubLayout.nodeX(home, j)) - r, Math.round(HubLayout.nodeY(home, j)) - r, 2 * r, 2 * r);
                    assertFalse(n.overlaps(o), "nodes " + i + " and " + j + " overlap (home=" + home + ")");
                }
                assertEquals(-1, HubLayout.nodeAt(home, HubLayout.ringCx(home), HubLayout.ringCy(home)), "the core is not a node");
                assertTrue(HubLayout.onCore(home, HubLayout.ringCx(home), HubLayout.ringCy(home)));
                assertFalse(HubLayout.onCore(home, HubLayout.nodeX(home, i), HubLayout.nodeY(home, i)), "a node is not the core");
            }
        }
    }

    @Test
    void everyLabelStaysOnTheStageAndClearsEveryNodeAndTheBottomLine() {
        Rect line = HubLayout.line();
        assertTrue(line.inside(STAGE));
        for (int w : new int[]{1, 30, 60, 92, 500}) {
            for (int i = 0; i < HubNodes.COUNT; i++) {
                Rect l = HubLayout.label(i, w);
                assertTrue(l.inside(STAGE), "label " + i + " at width " + w + " leaves the stage: " + l);
                assertTrue(l.w() <= HubLayout.LABEL_MAX_W);
                assertFalse(l.overlaps(line), "label " + i + " overlaps the bottom line");
                for (int j = 0; j < HubNodes.COUNT; j++) {
                    int r = HubLayout.HOME_NODE;
                    Rect n = new Rect(Math.round(HubLayout.nodeX(true, j)) - r, Math.round(HubLayout.nodeY(true, j)) - r, 2 * r, 2 * r);
                    assertFalse(l.overlaps(n), "label " + i + " overlaps node " + j);
                    if (j != i) assertFalse(l.overlaps(HubLayout.label(j, w)), "labels " + i + " and " + j + " overlap");
                }
            }
        }
    }

    @Test
    void thePagePanelHoldsItsTitleAndBodyAndClearsTheRail() {
        Rect p = HubLayout.PANEL;
        assertTrue(p.inside(STAGE));
        assertTrue(HubLayout.TITLE.inside(p) && HubLayout.BODY.inside(p), "title and body inside the panel");
        assertFalse(HubLayout.TITLE.overlaps(HubLayout.BODY));
        assertTrue(HubLayout.SCROLLBAR_X >= HubLayout.BODY.right() && HubLayout.SCROLLBAR_X + HubLayout.SCROLLBAR_W <= p.right(), "the scrollbar sits between the body and the frame");
        assertFalse(HubLayout.line().overlaps(p), "the bottom line is under the panel");
    }

    @Test
    void labelSidesFollowThePositionOnTheRing() {
        assertEquals(0, HubLayout.labelSide(0), "the top node's label is above it");
        assertEquals(1, HubLayout.labelSide(1), "a node on the right puts its label to the right");
        assertEquals(3, HubLayout.labelSide(HubNodes.COUNT / 2), "the bottom node's label is below it");
        assertEquals(2, HubLayout.labelSide(HubNodes.COUNT - 1), "a node on the left puts its label to the left");
    }

    // ================================================================ Hub v2: the framed window, the icon strip, the Codex layout
    private static final int[] SIZES = {40, 50, 62, 75, 99, 100};

    @Test
    void v2WindowFitsTheScreenSnapsToEighthsAndHundredIsTheOldFit() {
        for (int[] win : WINDOWS) {
            HubLayout.View old = HubLayout.view(win[0], win[1]);
            assertEquals(old, HubLayout.view(win[0], win[1], 100), "100 percent = the old full fit at " + win[0] + "x" + win[1]);
            for (int pct : SIZES) {
                HubLayout.View v = HubLayout.view(win[0], win[1], pct);
                assertTrue(v.scale() >= HubLayout.MIN_SCALE, "scale " + v.scale());
                assertEquals(v.scale(), Math.round(v.scale() * 8f) / 8f, 1e-6, "scale snaps to eighths");
                assertTrue(v.scale() <= old.scale() + 1e-6, "a smaller window is never bigger than the full fit (" + pct + "% at " + win[0] + "x" + win[1] + ")");
                if (v.scale() > HubLayout.MIN_SCALE) {
                    assertTrue(HubLayout.windowW(v) <= win[0] + 1 && HubLayout.windowH(v) <= win[1] + 1, "window does not fit " + win[0] + "x" + win[1] + " at " + pct + "%");
                    assertTrue(v.ox() >= -1e-3 && v.oy() >= -1e-3, "window starts outside the screen");
                }
                assertEquals((win[0] - HubLayout.STAGE_W * v.scale()) / 2f, v.ox(), 1e-3, "centred");
            }
        }
    }

    @Test
    void v2DefaultWindowIsAboutSixtyTwoPercentAndNeverMoreThanTheStage() {
        for (int[] win : WINDOWS) {
            if (win[0] < 640 || win[1] < 360) continue;
            HubLayout.View v = HubLayout.view(win[0], win[1], HubLayout.DEFAULT_SIZE);
            assertEquals(1.0f, v.scale(), 1e-6, "62% is capped to the 400 x 240 stage at " + win[0] + "x" + win[1]);
            assertTrue(HubLayout.windowW(v) <= 400 && HubLayout.windowH(v) <= 240);
        }
        HubLayout.View small = HubLayout.view(427, 240, HubLayout.DEFAULT_SIZE);                      // a 1280x720 window at GUI scale 3
        assertTrue(small.scale() >= HubLayout.MIN_READ_SCALE - 1e-6, "text stays readable when the screen allows it");
        assertTrue(HubLayout.windowW(small) < 427 && HubLayout.windowH(small) < 240, "and the window is smaller than the screen");
    }

    @Test
    void v2BiggerSettingNeverShrinksTheWindow() {
        for (int[] win : WINDOWS) {
            float last = 0f;
            for (int pct = HubLayout.MIN_SIZE; pct <= HubLayout.MAX_SIZE; pct += 5) {
                float s = HubLayout.view(win[0], win[1], pct).scale();
                assertTrue(s >= last - 1e-6, "scale shrank from " + last + " to " + s + " at " + pct + "% on " + win[0] + "x" + win[1]);
                last = s;
            }
        }
    }

    @Test
    void v2SizeStepsCycleAndStayInTheOptionRange() {
        int s = 50;
        for (int i = 0; i < 12; i++) {
            int n = HubLayout.nextSize(s);
            assertTrue(n >= HubLayout.MIN_SIZE && n <= HubLayout.MAX_SIZE);
            assertTrue(n != s);
            s = n;
        }
        assertEquals(62, HubLayout.nextSize(50));
        assertEquals(100, HubLayout.nextSize(75));
        assertEquals(50, HubLayout.nextSize(100));
        assertEquals(75, HubLayout.nextSize(63), "a size that is not a step goes to the next bigger step");
    }

    @Test
    void v2StripNodesAreInsideTheStageNeverOverlapAndClearTheBackButtonAndPanel() {
        int r = HubLayout.STRIP_NODE;
        Rect back = new Rect(HubLayout.BACK_CX - HubLayout.BACK_R, HubLayout.BACK_CY - HubLayout.BACK_R, 2 * HubLayout.BACK_R, 2 * HubLayout.BACK_R);
        for (int i = 0; i < HubNodes.COUNT; i++) {
            Rect n = new Rect(Math.round(HubLayout.stripX(i)) - r, Math.round(HubLayout.stripY(i)) - r, 2 * r, 2 * r);
            assertTrue(n.inside(STAGE), "strip node " + i + " leaves the stage");
            assertTrue(n.right() <= HubLayout.PANEL2.x(), "strip node " + i + " runs into the page panel");
            assertFalse(n.overlaps(back), "strip node " + i + " overlaps the back button");
            assertFalse(n.overlaps(HubLayout.line()), "strip node " + i + " overlaps the bottom line");
            for (int j = i + 1; j < HubNodes.COUNT; j++) {
                Rect o = new Rect(Math.round(HubLayout.stripX(j)) - r, Math.round(HubLayout.stripY(j)) - r, 2 * r, 2 * r);
                assertFalse(n.overlaps(o), "strip nodes " + i + " and " + j + " overlap");
            }
            assertEquals(i, HubLayout.stripNodeAt(HubLayout.stripX(i), HubLayout.stripY(i)));
            assertFalse(HubLayout.onBack(HubLayout.stripX(i), HubLayout.stripY(i)), "a node is not the back button");
            for (int[] win : WINDOWS) {
                HubLayout.View v = HubLayout.view(win[0], win[1], HubLayout.DEFAULT_SIZE);
                double wx = v.ox() + HubLayout.stripX(i) * v.scale(), wy = v.oy() + HubLayout.stripY(i) * v.scale();
                assertEquals(i, HubLayout.stripNodeAt(v.sx(wx), v.sy(wy)), "click on strip node " + i + " at " + win[0] + "x" + win[1]);
            }
        }
        assertTrue(back.inside(STAGE) && HubLayout.onBack(HubLayout.BACK_CX, HubLayout.BACK_CY));
        assertEquals(-1, HubLayout.stripNodeAt(HubLayout.BACK_CX, HubLayout.BACK_CY), "the back button is not a node");
    }

    @Test
    void v2PagePanelHoldsEveryBodyAndTheTitleAndTheScrollbars() {
        Rect p = HubLayout.PANEL2;
        assertTrue(p.inside(STAGE));
        assertTrue(HubLayout.TITLE2.inside(p));
        assertFalse(HubLayout.TITLE2.overlaps(HubLayout.BODY2));
        assertFalse(HubLayout.line().overlaps(p));
        for (Rect r : new Rect[] {HubLayout.BODY2, HubLayout.PLAYER, HubLayout.STATUS_LIST, HubLayout.SUBRAIL, HubLayout.CODEX_BODY}) {
            assertTrue(r.inside(p), r + " is outside the panel");
            assertFalse(r.overlaps(HubLayout.TITLE2), r + " overlaps the title");
        }
        for (HubNodes.Tab t : HubNodes.Tab.values()) {
            Rect b = HubLayout.body(t);
            assertTrue(b.right() + HubLayout.SCROLLBAR2_GAP + HubLayout.SCROLLBAR2_W <= p.right(), "the scrollbar of " + t + " leaves the panel");
        }
        assertFalse(HubLayout.PLAYER.overlaps(HubLayout.STATUS_LIST), "the player panel and the status list overlap");
        assertFalse(HubLayout.SUBRAIL.overlaps(HubLayout.CODEX_BODY), "the Codex rail and its body overlap");
        assertTrue(HubLayout.CODEX_BODY.x() - HubLayout.SUBRAIL.right() >= 4, "room for the divider line");
        assertTrue(HubLayout.CODEX_BODY.w() >= 240 && HubLayout.STATUS_LIST.w() >= 230, "the old pages need about the width of the book's column pair");
        assertEquals(HubLayout.BODY2, HubLayout.body(HubNodes.Tab.WEAPONS));
        assertEquals(HubLayout.CODEX_BODY, HubLayout.body(HubNodes.Tab.CODEX));
        assertEquals(HubLayout.STATUS_LIST, HubLayout.body(HubNodes.Tab.STATUS));
        assertTrue(HubLayout.NAME_X > HubLayout.BACK_CX + HubLayout.BACK_R, "the hub name clears the back button");
    }
}
