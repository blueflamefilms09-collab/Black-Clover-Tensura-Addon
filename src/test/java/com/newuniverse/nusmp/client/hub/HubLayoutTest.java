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
}
