package com.newuniverse.nusmp.client.ui.move;

import com.newuniverse.nusmp.client.mega.system.OverlayDrag;
import com.newuniverse.nusmp.client.ui.move.GuiMoveMath.Place;
import com.newuniverse.nusmp.client.ui.move.GuiMoveMath.Rect;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the layout arithmetic of GUI Move cannot be seen until the game runs, so it is pinned to the pixel here: anchors, the 24-pixel clamp, snapping, scale limits and the
 * order of the two sizes, the migration of the System window's old offsets, the stability of a saved place when the window or the GUI scale changes, and the mouse
 * transform of a moved element.
 */
class GuiMoveMathTest {
    private static final double EPS = 1e-9;

    private static final int[][] SCREENS = {{427, 240}, {480, 270}, {640, 360}, {854, 480}, {960, 540}, {1920, 1080}, {200, 120}};
    private static final float[] TOTALS = {0.5f, 0.75f, 1f, 1.25f, 2f, 2.5f};

    // ---------------------------------------------------------------- anchors
    @Test
    void aDroppedElementTakesTheNearestOfTheNineAnchors() {
        int w = 100, h = 40, margin = 6;
        Place tl = GuiMoveMath.placeFromGui(480, 270, 1f, w, h, margin, 10, 10);
        assertEquals(0, tl.anchor());
        assertFalse(tl.centerX());
        assertFalse(tl.centerY());
        Place br = GuiMoveMath.placeFromGui(480, 270, 1f, w, h, margin, 480 - w - 6, 270 - h - 6);
        assertEquals(3, br.anchor());
        assertEquals(0, br.offX());
        assertEquals(0, br.offY());
        Place centre = GuiMoveMath.placeFromGui(480, 270, 1f, w, h, margin, (480 - w) / 2.0, (270 - h) / 2.0);
        assertTrue(centre.centerX() && centre.centerY());
        assertEquals(0, centre.offX());
        assertEquals(0, centre.offY());
        Place topCentre = GuiMoveMath.placeFromGui(480, 270, 1f, w, h, margin, (480 - w) / 2.0 + 12, 6);
        assertEquals(0, topCentre.anchor() & 2);
        assertTrue(topCentre.centerX());
        assertFalse(topCentre.centerY());
        assertEquals(12, topCentre.offX());
        assertEquals(0, topCentre.offY());
    }

    @Test
    void aPlaceGivesTheDroppedCornerBackToTheRoundingOfOnePixel() {
        for (int[] s : SCREENS) for (float t : TOTALS) for (int w : new int[]{16, 64, 164, 300}) for (int h : new int[]{10, 40, 120}) {
            for (double fx = 0; fx <= 1.0; fx += 0.125) for (double fy = 0; fy <= 1.0; fy += 0.125) {
                double left = fx * (s[0] - w * t), top = fy * (s[1] - h * t);
                double l = GuiMoveMath.clampVisible(left, w * (double) t, s[0]), tp = GuiMoveMath.clampVisible(top, h * (double) t, s[1]);
                Place p = GuiMoveMath.placeFromGui(s[0], s[1], t, w, h, 6, left, top);
                assertEquals(l, GuiMoveMath.leftGui(p, s[0], t, w, 6), t + 1e-6, "x for " + s[0] + "x" + s[1] + " t" + t + " w" + w + " at " + left);
                assertEquals(tp, GuiMoveMath.topGui(p, s[1], t, h, 6), t + 1e-6, "y for " + s[0] + "x" + s[1] + " t" + t + " h" + h + " at " + top);
            }
        }
    }

    // ---------------------------------------------------------------- never off screen
    @Test
    void atLeast24PixelsAlwaysStayOnScreenWhateverIsAskedOrSaved() {
        double[] wild = {-1e9, -5000, -300, -1, 0, 1, 50, 479, 480, 5000, 1e9, Double.NaN};
        for (int[] s : SCREENS) for (float t : TOTALS) for (int w : new int[]{8, 24, 100, 540}) for (double x : wild) {
            double size = w * (double) t;
            double pos = GuiMoveMath.clampVisible(x, size, s[0]);
            double visible = Math.min(pos + size, s[0]) - Math.max(pos, 0);
            assertTrue(visible >= Math.min(GuiMoveMath.MIN_VISIBLE, size) - 1e-9, "visible " + visible + " of " + size + " at " + x + " on " + s[0]);
        }
        // a saved place with absurd offsets (a hand-edited file) is held on the screen too
        for (int[] s : SCREENS) for (int off : new int[]{-100000, -4096, 4096, 100000}) for (int anchor = 0; anchor < 4; anchor++) for (boolean c : new boolean[]{false, true}) {
            Place p = GuiMoveMath.sanitize(new Place(anchor, c, c, off, -off));
            Rect r = GuiMoveMath.rect(p, s[0], s[1], 1.5f, 164, 116, 6);
            assertTrue(Math.min(r.right(), s[0]) - Math.max(r.x(), 0) >= 24 - 1e-9 && Math.min(r.bottom(), s[1]) - Math.max(r.y(), 0) >= 24 - 1e-9, "rect " + r + " on " + s[0] + "x" + s[1]);
        }
    }

    @Test
    void anElementSmallerThan24KeepsAllOfItself() {
        assertEquals(0.0, GuiMoveMath.clampVisible(-500, 10, 400), EPS);                 // a 10 pixel element cannot hang over an edge at all
        assertEquals(390.0, GuiMoveMath.clampVisible(9999, 10, 400), EPS);
        assertEquals(-76.0, GuiMoveMath.clampVisible(-500, 100, 400), EPS);               // a 100 pixel one may hang out by 76, 24 stay
        assertEquals(376.0, GuiMoveMath.clampVisible(9999, 100, 400), EPS);
    }

    // ---------------------------------------------------------------- a resize keeps a place
    @Test
    void aRightAnchoredElementKeepsItsDistanceFromTheRightEdgeWhenTheWindowChanges() {
        int w = 100, h = 30;
        Place p = GuiMoveMath.placeFromGui(480, 270, 1f, w, h, 6, 480 - w - 20, 40);        // 20 pixels from the right edge
        assertTrue(p.right());
        for (int[] s : SCREENS) {
            if (s[0] < 200) continue;
            double right = GuiMoveMath.leftGui(p, s[0], 1f, w, 6) + w;
            assertEquals(s[0] - 20, right, 1.0, "right edge on " + s[0]);
        }
    }

    @Test
    void aCentredElementStaysCentredAndABottomOneStaysAtTheBottom() {
        int w = 206, h = 68;
        Place p = GuiMoveMath.placeFromGui(480, 270, 1f, w, h, 62, (480 - w) / 2.0, 270 - 62 - h);          // the Djinn panel's default spot
        assertTrue(p.centerX() && p.bottom());
        for (int[] s : new int[][]{{427, 240}, {854, 480}, {1920, 1080}}) {
            Rect r = GuiMoveMath.rect(p, s[0], s[1], 1f, w, h, 62);
            assertEquals((s[0] - w) / 2.0, r.x(), 1.0);
            assertEquals(s[1] - 62 - h, r.y(), 1.0);
        }
    }

    @Test
    void aWindowThatShrinksNeverLeavesAnElementOutOfReach() {
        int w = 164, h = 116;
        Place p = GuiMoveMath.placeFromGui(1920, 1080, 1f, w, h, 6, 1700, 900);
        for (int[] s : SCREENS) {
            Rect r = GuiMoveMath.rect(p, s[0], s[1], 1f, w, h, 6);
            assertTrue(r.right() >= 24 && r.x() <= s[0] - 24 && r.bottom() >= 24 && r.y() <= s[1] - 24, "out of reach on " + s[0] + "x" + s[1] + ": " + r);
        }
    }

    @Test
    void theGuiScaleChangingMovesTheGuiPixelsButNotTheMeaningOfThePlace() {
        // the same window, the player raises the Minecraft GUI scale: 854x480 -> 427x240 GUI pixels. A right-anchored element keeps its margin in its own pixels.
        int w = 120, h = 30;
        Place p = GuiMoveMath.placeFromGui(854, 480, 1f, w, h, 6, 854 - w - 30, 100);
        Rect big = GuiMoveMath.rect(p, 427, 240, 1f, w, h, 6);
        assertEquals(427 - 30, big.right(), 1.0);
    }

    // ---------------------------------------------------------------- the two sizes
    @Test
    void theTwoSizesAreOneProductAppliedOnce() {
        assertEquals(1.0f, GuiMoveMath.total(1f, 1f), EPS);
        assertEquals(2.25f, GuiMoveMath.total(1.5f, 1.5f), EPS);
        assertEquals(GuiMoveMath.total(0.5f, 2f), GuiMoveMath.total(2f, 0.5f), EPS);         // the order of the two does not matter
        assertEquals(1.0f, GuiMoveMath.total(0.5f, 2f), EPS);
        assertEquals(GuiMoveMath.MAX_TOTAL, GuiMoveMath.total(2.5f, 2.5f), EPS);             // held
        assertEquals(GuiMoveMath.MIN_TOTAL, GuiMoveMath.total(0.5f, 0.1f), EPS);
        assertEquals(1.5f, GuiMoveMath.total(Float.NaN, 1.5f), EPS);                         // garbage reads as 1
        assertEquals(1.5f, GuiMoveMath.total(1.5f, Float.POSITIVE_INFINITY), EPS);
    }

    @Test
    void theScaleStaysInItsLimitsAndStepsInTwentieths() {
        assertEquals(0.5f, GuiMoveMath.clampScale(0.1), EPS);
        assertEquals(2.5f, GuiMoveMath.clampScale(9), EPS);
        assertEquals(1f, GuiMoveMath.clampScale(Double.NaN), EPS);
        assertEquals(1.05f, GuiMoveMath.stepScale(1f, 1, false), 1e-6);
        assertEquals(1.25f, GuiMoveMath.stepScale(1f, 1, true), 1e-6);
        assertEquals(0.95f, GuiMoveMath.stepScale(1f, -3, false), 1e-6);                     // only the sign counts
        assertEquals(0.5f, GuiMoveMath.stepScale(0.5f, -1, true), 1e-6);
        assertEquals(2.5f, GuiMoveMath.stepScale(2.5f, 1, false), 1e-6);
        assertEquals(1f, GuiMoveMath.stepScale(1f, 0, false), 1e-6);
        float s = 1f;
        for (int i = 0; i < 100; i++) s = GuiMoveMath.stepScale(s, 1, false);
        assertEquals(2.5f, s, 1e-6);
        for (int i = 0; i < 200; i++) s = GuiMoveMath.stepScale(s, -1, false);
        assertEquals(0.5f, s, 1e-6);
    }

    @Test
    void theWheelKeepsThePointUnderTheMouseStill() {
        double left = 200, top = 100;
        int w = 100, h = 40;
        double mx = 230, my = 110;
        GuiMoveMath.Resized z = GuiMoveMath.wheel(left, top, w, h, 1f, 1f, 1, false, mx, my);
        assertEquals(1.05f, z.moveScale(), 1e-6);
        double fx = (mx - left) / (w * 1.0), fy = (my - top) / (h * 1.0);
        assertEquals(mx, z.left() + fx * w * 1.05, 1e-4);
        assertEquals(my, z.top() + fy * h * 1.05, 1e-4);
        // with a GUI Size of 1.5 the point still holds (the factor of the sliders is part of the total)
        GuiMoveMath.Resized y = GuiMoveMath.wheel(left, top, w, h, 1f, 1.5f, -1, true, mx, my);
        assertEquals(0.75f, y.moveScale(), 1e-6);
        double t0 = 1.5, t1 = 0.75 * 1.5;
        assertEquals(mx, y.left() + ((mx - left) / (w * t0)) * w * t1, 1e-4);
    }

    @Test
    void aNudgeMovesFromWhereTheElementReallyIsAndStaysOnTheScreen() {
        int w = 100, h = 40;
        Place p = GuiMoveMath.placeFromGui(480, 270, 1f, w, h, 6, 100, 80);
        double x0 = GuiMoveMath.leftGui(p, 480, 1f, w, 6), y0 = GuiMoveMath.topGui(p, 270, 1f, h, 6);
        Place q = GuiMoveMath.nudge(p, 480, 270, 1f, w, h, 6, 8, -1);
        assertEquals(x0 + 8, GuiMoveMath.leftGui(q, 480, 1f, w, 6), 1.0);
        assertEquals(y0 - 1, GuiMoveMath.topGui(q, 270, 1f, h, 6), 1.0);
        Place far = GuiMoveMath.nudge(p, 480, 270, 1f, w, h, 6, 100000, 100000);
        Rect r = GuiMoveMath.rect(far, 480, 270, 1f, w, h, 6);
        assertTrue(r.x() <= 480 - 24 && r.y() <= 270 - 24);
    }

    // ---------------------------------------------------------------- snapping
    @Test
    void anEdgeSnapsToTheScreenAndToOtherElementsOnlyWithinTheDistance() {
        Rect other = new Rect(300, 50, 100, 40);
        Rect near = new Rect(2, 4, 80, 30);                                      // left edge 2 px from the screen's left, top 4 from the top
        GuiMoveMath.Snapped s = GuiMoveMath.snap(near, new Rect[]{other}, 480, 270, GuiMoveMath.SNAP_DISTANCE);
        assertEquals(0.0, s.x(), EPS);
        assertEquals(0.0, s.y(), EPS);
        assertTrue(s.snappedX() && s.snappedY());
        assertEquals(0.0, s.guideX(), EPS);
        Rect nextToOther = new Rect(400 + 3, 200, 60, 30);                       // 3 px right of the other's right edge
        GuiMoveMath.Snapped o = GuiMoveMath.snap(nextToOther, new Rect[]{other}, 480, 270, GuiMoveMath.SNAP_DISTANCE);
        assertEquals(400.0, o.x(), EPS);
        assertEquals(200.0, o.y(), EPS);
        assertFalse(o.snappedY());
        Rect far = new Rect(100, 150, 60, 30);                                   // nothing within 6
        GuiMoveMath.Snapped f = GuiMoveMath.snap(far, new Rect[]{other}, 480, 270, GuiMoveMath.SNAP_DISTANCE);
        assertEquals(100.0, f.x(), EPS);
        assertEquals(150.0, f.y(), EPS);
        assertFalse(f.snappedX() || f.snappedY());
        assertTrue(Double.isNaN(f.guideX()));
    }

    @Test
    void theMiddleOfAnElementSnapsToTheMiddleOfTheScreenAndTheNearestLineWins() {
        Rect r = new Rect(180 + 4, 100, 120, 30);                                // centre at 244 vs the screen's 240
        GuiMoveMath.Snapped s = GuiMoveMath.snap(r, null, 480, 270, GuiMoveMath.SNAP_DISTANCE);
        assertEquals(180.0, s.x(), EPS);
        assertEquals(240.0, s.guideX(), EPS);
        // two lines in reach: the nearer one
        Rect a = new Rect(100, 0, 50, 10), b = new Rect(106, 0, 50, 10);
        Rect moving = new Rect(103.5, 120, 30, 10);
        GuiMoveMath.Snapped t = GuiMoveMath.snap(moving, new Rect[]{a, null, b}, 480, 270, 6);
        assertEquals(2.5, Math.abs(t.x() - 103.5), EPS);
    }

    // ---------------------------------------------------------------- the System window's old offsets
    @Test
    void theOldSystemPlaceLandsOnExactlyTheSameSpot() {
        int panelW = 164, panelH = 90, margin = 6;
        int[][] screens = {{427, 240}, {854, 480}, {960, 540}};
        float[] scales = {0.75f, 1f, 1.5f, 2f};
        for (int anchor = 0; anchor < 4; anchor++) for (boolean cx : new boolean[]{false, true}) for (boolean cy : new boolean[]{false, true}) for (int ox : new int[]{0, 5, 40}) for (int oy : new int[]{0, 7, 30}) {
            OverlayDrag.Place old = new OverlayDrag.Place(anchor, cx, cy, ox, oy);
            Place nw = GuiMoveMath.fromLegacy(old.anchor(), old.centerX(), old.centerY(), old.offX(), old.offY());
            assertEquals(old.anchor(), nw.anchor());
            for (int[] s : screens) for (float scale : scales) {
                double oldLeft = OverlayDrag.leftGui(old, s[0], scale, panelW, margin), oldTop = OverlayDrag.topGui(old, s[1], scale, panelH, margin);
                // the old layout keeps the panel fully inside; compare where that holds
                if (oldLeft <= scale || oldTop <= scale || oldLeft + panelW * scale >= s[0] - scale || oldTop + panelH * scale >= s[1] - scale) continue;
                assertEquals(oldLeft, GuiMoveMath.leftGui(nw, s[0], scale, panelW, margin), 1e-9, "left " + old + " " + s[0] + " " + scale);
                assertEquals(oldTop, GuiMoveMath.topGui(nw, s[1], scale, panelH, margin), 1e-9, "top " + old + " " + s[1] + " " + scale);
            }
        }
    }

    @Test
    void aLegacyPlaceFromAHandEditedFileIsForcedIntoRange() {
        Place p = GuiMoveMath.fromLegacy(9, true, false, 999999, -999999);
        assertEquals(0, p.anchor());
        assertEquals(GuiMoveMath.MAX_OFFSET, p.offX());
        assertEquals(-GuiMoveMath.MAX_OFFSET, p.offY());
        assertEquals(Place.DEFAULT, GuiMoveMath.sanitize(null));
    }

    // ---------------------------------------------------------------- a moved element's mouse
    @Test
    void theMouseOfAMovedElementFollowsIt() {
        double defX = 100, defY = 50, newX = 300, newY = 20;
        float t = 1.5f;
        // the element's default corner is now at (300, 20): the mouse there is at the element's default corner
        assertEquals(defX, GuiMoveMath.toLocal(newX, newX, defX, t), EPS);
        assertEquals(defY, GuiMoveMath.toLocal(newY, newY, defY, t), EPS);
        for (double m = -20; m < 600; m += 31.7) {
            assertEquals(m, GuiMoveMath.toScreen(GuiMoveMath.toLocal(m, newX, defX, t), newX, defX, t), 1e-9);
            assertEquals(m, GuiMoveMath.toLocal(GuiMoveMath.toScreen(m, newX, defX, t), newX, defX, t), 1e-9);
        }
        // one element pixel is t screen pixels
        assertEquals(10.0, GuiMoveMath.toLocal(newX + 15, newX, defX, t) - defX, EPS);
        // an element that was not moved (same corner, size 1) is the identity
        assertEquals(77.0, GuiMoveMath.toLocal(77, 40, 40, 1f), EPS);
    }
}
