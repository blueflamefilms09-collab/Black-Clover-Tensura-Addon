package com.newuniverse.nusmp.client.mega.system;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the owner moves and resizes the System window with the mouse and expects it to stay where it was put on any screen. The risks are all arithmetic: a drag
 * that lands the window somewhere other than under the mouse, an anchor that is not the nearest one so a window left at the right edge jumps when the screen gets
 * narrower, a resize whose opposite corner slides, a scale outside 0.5 to 2.5, a wheel notch that moves the window instead of growing it round the pointer. Each is
 * pinned here over every corner, a grid of screens, panels and scales, with the same functions the edit screen calls.
 */
class OverlayDragTest {
    private static final int M = OverlayStyle.MARGIN;
    private static final int PW = 164, PH = 90;

    // ---------------------------------------------------------------- the nine anchors
    @Test void aWindowDroppedInACornerIsAnchoredToThatCorner() {
        assertEquals(new OverlayDrag.Place(0, false, false, 0, 0), OverlayDrag.snap(640, 360, PW, PH, M, M, M));
        OverlayDrag.Place tr = OverlayDrag.snap(640, 360, PW, PH, M, 640 - PW - M, M);
        assertEquals(1, tr.anchor());
        assertFalse(tr.centerX());
        assertEquals(0, tr.offX());
        OverlayDrag.Place br = OverlayDrag.snap(640, 360, PW, PH, M, 640 - PW - M, 360 - PH - M);
        assertEquals(3, br.anchor());
        OverlayDrag.Place bl = OverlayDrag.snap(640, 360, PW, PH, M, M, 360 - PH - M);
        assertEquals(2, bl.anchor());
    }

    @Test void aWindowDroppedInTheMiddleIsAnchoredToTheMiddle() {
        OverlayDrag.Place c = OverlayDrag.snap(640, 360, PW, PH, M, (640 - PW) / 2, (360 - PH) / 2);
        assertTrue(c.centerX() && c.centerY());
        assertEquals(0, c.offX());
        assertEquals(0, c.offY());
        OverlayDrag.Place top = OverlayDrag.snap(640, 360, PW, PH, M, (640 - PW) / 2 + 10, M);
        assertTrue(top.centerX());
        assertFalse(top.centerY());
        assertEquals(10, top.offX());
    }

    @Test void placeAndSnapAreInversesOverAGridOfScreensPanelsAndSpots() {
        for (int sw : new int[]{320, 427, 640, 1000}) {
            for (int sh : new int[]{180, 240, 360, 540}) {
                for (int pw : new int[]{120, 164, 230}) {
                    for (int ph : new int[]{60, 90, 140}) {
                        for (int x = -20; x <= sw + 20; x += 37) {
                            for (int y = -20; y <= sh + 20; y += 31) {
                                OverlayDrag.Place p = OverlayDrag.snap(sw, sh, pw, ph, M, x, y);
                                int wantX = OverlayMath.clamp(x, 0, sw - pw), wantY = OverlayMath.clamp(y, 0, sh - ph);
                                assertEquals(wantX, OverlayDrag.placeX(p, sw, pw, M), "x for " + sw + "x" + sh + " panel " + pw + "x" + ph + " at " + x + "," + y);
                                assertEquals(wantY, OverlayDrag.placeY(p, sh, ph, M), "y for " + sw + "x" + sh + " panel " + pw + "x" + ph + " at " + x + "," + y);
                            }
                        }
                    }
                }
            }
        }
    }

    @Test void theWindowIsAlwaysOnTheScreenAfterAnyPlaceAndScreenChange() {
        OverlayDrag.Place[] places = {
                new OverlayDrag.Place(0, false, false, 4000, 4000), new OverlayDrag.Place(3, false, false, -4000, -4000), new OverlayDrag.Place(1, true, true, 4096, -4096),
                new OverlayDrag.Place(2, true, false, -999, 999), OverlayDrag.Place.DEFAULT};
        for (OverlayDrag.Place p : places) {
            for (int sw : new int[]{200, 320, 640, 1920}) {
                for (int sh : new int[]{120, 240, 480}) {
                    int x = OverlayDrag.placeX(p, sw, PW, M), y = OverlayDrag.placeY(p, sh, PH, M);
                    assertTrue(x >= 0 && x + PW <= Math.max(sw, PW), p + " x " + x + " on " + sw);
                    assertTrue(y >= 0 && y + PH <= Math.max(sh, PH), p + " y " + y + " on " + sh);
                }
            }
        }
    }

    @Test void aCornerPlaceKeepsItsDistanceFromTheEdgeWhenTheScreenChanges() {
        // dropped 40 window pixels from the right edge and 30 from the bottom, on a 640x360 window space
        OverlayDrag.Place p = OverlayDrag.snap(640, 360, PW, PH, M, 640 - PW - 40, 360 - PH - 30);
        assertEquals(3, p.anchor());
        for (int sw : new int[]{427, 500, 800, 1280}) {
            for (int sh : new int[]{240, 300, 450, 720}) {
                assertEquals(sw - PW - 40, OverlayDrag.placeX(p, sw, PW, M), "right-hand distance kept on " + sw);
                assertEquals(sh - PH - 30, OverlayDrag.placeY(p, sh, PH, M), "bottom distance kept on " + sh);
            }
        }
    }

    @Test void aCentredPlaceStaysCentredWhenTheScreenChanges() {
        OverlayDrag.Place p = OverlayDrag.snap(640, 360, PW, PH, M, (640 - PW) / 2, (360 - PH) / 2);
        for (int sw : new int[]{428, 500, 800, 1280}) {
            int x = OverlayDrag.placeX(p, sw, PW, M);
            assertEquals((sw - PW) / 2, x);
            assertTrue(Math.abs((x + PW / 2.0) - sw / 2.0) <= 1.0, "centre of the window at the centre of the screen");
        }
    }

    @Test void aGuiScaleChangeKeepsTheWindowWhereItWasPutOnTheScreen() {
        // a 1280x720 screen at GUI scale 2 (640x360), the window dropped near the right edge; then GUI scale 3 (427x240)
        OverlayDrag.Place p = OverlayDrag.snapGui(640, 360, 1f, PW, PH, M, 640 - PW - 20, 50);
        assertEquals(1, p.anchor());
        double leftBefore = OverlayDrag.leftGui(p, 640, 1f, PW, M);
        double leftAfter = OverlayDrag.leftGui(p, 427, 1f, PW, M);
        assertEquals(640 - PW - 20, leftBefore, 1.0);
        assertEquals(427 - PW - 20, leftAfter, 1.0, "still 20 pixels from the right edge");
        assertTrue(leftAfter >= 0 && leftAfter + PW <= 427);
    }

    // ---------------------------------------------------------------- the drag
    @Test void aDragPutsTheGrabbedPointUnderTheMouse() {
        double grabX = 30, grabY = 12;
        for (float scale : new float[]{0.5f, 1f, 1.5f, 2.5f}) {
            for (double mx : new double[]{100, 250.5, 400}) {
                for (double my : new double[]{60, 120, 200}) {
                    OverlayDrag.Place p = OverlayDrag.drag(640, 360, scale, PW, PH, M, mx, my, grabX, grabY);
                    double left = OverlayDrag.leftGui(p, 640, scale, PW, M), top = OverlayDrag.topGui(p, 360, scale, PH, M);
                    double wantLeft = Math.max(0, Math.min(640 - PW * scale, mx - grabX)), wantTop = Math.max(0, Math.min(360 - PH * scale, my - grabY));
                    assertEquals(wantLeft, left, scale + 0.5, "left at scale " + scale + " mouse " + mx);
                    assertEquals(wantTop, top, scale + 0.5, "top at scale " + scale + " mouse " + my);
                }
            }
        }
    }

    @Test void aDragPastTheScreenIsStoppedAtTheEdges() {
        OverlayDrag.Place p = OverlayDrag.drag(640, 360, 1f, PW, PH, M, 5000, 5000, 0, 0);
        assertEquals(640 - PW, OverlayDrag.placeX(p, 640, PW, M));
        assertEquals(360 - PH, OverlayDrag.placeY(p, 360, PH, M));
        OverlayDrag.Place q = OverlayDrag.drag(640, 360, 1f, PW, PH, M, -5000, -5000, 0, 0);
        assertEquals(0, OverlayDrag.placeX(q, 640, PW, M));
        assertEquals(0, OverlayDrag.placeY(q, 360, PH, M));
    }

    // ---------------------------------------------------------------- the size
    @Test void theScaleRangeIsHalfToTwoAndAHalf() {
        assertEquals(0.5f, OverlayDrag.MIN_SCALE);
        assertEquals(2.5f, OverlayDrag.MAX_SCALE);
        assertEquals(0.5f, OverlayMath.clampScale(0.1));
        assertEquals(2.5f, OverlayMath.clampScale(7.0));
        assertEquals(1.0f, OverlayMath.clampScale(Double.NaN));
        assertEquals(2.5f, OverlayMath.clampScale(2.5));
    }

    @Test void aWheelNotchStepsTheScaleByAnExactTwentiethAndStopsAtTheEnds() {
        assertEquals(1.05f, OverlayDrag.stepScale(1.0f, 1, false), 1e-6);
        assertEquals(0.95f, OverlayDrag.stepScale(1.0f, -1, false), 1e-6);
        assertEquals(1.25f, OverlayDrag.stepScale(1.0f, 3, true), 1e-6, "only the sign counts; Shift steps a quarter");
        assertEquals(2.5f, OverlayDrag.stepScale(2.5f, 1, false));
        assertEquals(2.5f, OverlayDrag.stepScale(2.45f, 1, true));
        assertEquals(0.5f, OverlayDrag.stepScale(0.5f, -1, false));
        assertEquals(1.0f, OverlayDrag.stepScale(1.0f, 0, false));
        float s = 1.0f;
        for (int i = 0; i < 100; i++) s = OverlayDrag.stepScale(s, 1, false);
        assertEquals(2.5f, s, 1e-6, "a hundred notches up end at the top without float drift past it");
        for (int i = 0; i < 100; i++) s = OverlayDrag.stepScale(s, -1, false);
        assertEquals(0.5f, s, 1e-6);
    }

    @Test void resizingRoundAPivotKeepsThePivotWhereItIs() {
        for (double fx : new double[]{0, 0.25, 0.5, 1}) {
            for (double fy : new double[]{0, 0.5, 1}) {
                double left = 100, top = 60;
                float oldS = 1.0f, newS = 1.8f;
                double pxBefore = left + fx * PW * oldS, pyBefore = top + fy * PH * oldS;
                OverlayDrag.Resized r = OverlayDrag.around(left, top, PW, PH, oldS, newS, fx, fy);
                assertEquals(newS, r.scale(), 1e-6);
                assertEquals(pxBefore, r.left() + fx * PW * r.scale(), 1e-6);
                assertEquals(pyBefore, r.top() + fy * PH * r.scale(), 1e-6);
            }
        }
        assertEquals(2.5f, OverlayDrag.around(0, 0, PW, PH, 1f, 9f, 0, 0).scale(), "the new scale is clamped");
    }

    @Test void aWheelNotchOverTheWindowGrowsItRoundThePointUnderTheMouse() {
        double left = 200, top = 100;
        double mx = left + 0.5 * PW, my = top + 0.5 * PH;
        OverlayDrag.Resized r = OverlayDrag.wheel(left, top, PW, PH, 1f, 1, false, mx, my);
        assertEquals(1.05f, r.scale(), 1e-6);
        assertEquals(mx, r.left() + 0.5 * PW * r.scale(), 1e-6, "the point under the mouse did not move");
        assertEquals(my, r.top() + 0.5 * PH * r.scale(), 1e-6);
        OverlayDrag.Resized outside = OverlayDrag.wheel(left, top, PW, PH, 1f, 1, false, -500, -500);
        assertEquals(left, outside.left(), 1e-6, "a mouse outside pivots on the nearest corner of the window");
        assertEquals(top, outside.top(), 1e-6);
    }

    @Test void draggingACornerKeepsTheOppositeCornerFixedForEveryCorner() {
        double left = 150, top = 80;
        float scale = 1.0f;
        double right = left + PW * scale, bottom = top + PH * scale;
        double[][] fixed = {{right, bottom}, {left, bottom}, {right, top}, {left, top}};         // the opposite of 0 TL, 1 TR, 2 BL, 3 BR
        double[][] mouse = {{100, 40}, {400, 30}, {90, 300}, {420, 280}};
        for (int c = 0; c < 4; c++) {
            OverlayDrag.Resized r = OverlayDrag.corner(c, mouse[c][0], mouse[c][1], left, top, PW, PH, scale);
            double w = PW * (double) r.scale(), h = PH * (double) r.scale();
            double ox = (c == 0 || c == 2) ? r.left() + w : r.left();
            double oy = (c == 0 || c == 1) ? r.top() + h : r.top();
            assertEquals(fixed[c][0], ox, 1e-6, "fixed x of the corner opposite " + c);
            assertEquals(fixed[c][1], oy, 1e-6, "fixed y of the corner opposite " + c);
            assertTrue(r.scale() >= 0.5f && r.scale() <= 2.5f);
        }
    }

    @Test void draggingACornerOutMakesTheWindowBiggerAndInSmaller() {
        double left = 150, top = 80;
        float grown = OverlayDrag.corner(3, left + PW * 1.5, top + PH * 1.5, left, top, PW, PH, 1f).scale();
        float shrunk = OverlayDrag.corner(3, left + PW * 0.6, top + PH * 0.6, left, top, PW, PH, 1f).scale();
        assertEquals(1.5f, grown, 0.011f);
        assertEquals(0.6f, shrunk, 0.011f);
        assertEquals(2.5f, OverlayDrag.corner(3, 9999, 9999, left, top, PW, PH, 1f).scale());
        assertEquals(0.5f, OverlayDrag.corner(3, left, top, left, top, PW, PH, 1f).scale(), "dragging onto the fixed corner gives the smallest size");
        assertEquals(1.5f, OverlayDrag.corner(0, left + PW - PW * 1.5, top + PH - PH * 1.5, left, top, PW, PH, 1f).scale(), 0.011f, "the top-left handle works the other way round");
    }

    @Test void theLargerAxisOfTheMouseLeadsTheResize() {
        double left = 0, top = 0;
        float a = OverlayDrag.corner(3, PW * 2.0, PH * 1.0, left, top, PW, PH, 1f).scale();
        float b = OverlayDrag.corner(3, PW * 1.0, PH * 2.0, left, top, PW, PH, 1f).scale();
        assertEquals(2.0f, a, 0.011f);
        assertEquals(2.0f, b, 0.011f);
    }

    @Test void aResizedWindowIsStoredAsAPlaceThatPutsItWhereTheResizeLeftIt() {
        // drag the bottom-right handle of a window sitting in the top-left corner out to scale 2
        double left = M * 1.0, top = M * 1.0;
        OverlayDrag.Resized r = OverlayDrag.corner(3, left + PW * 2, top + PH * 2, left, top, PW, PH, 1f);
        assertEquals(2.0f, r.scale(), 0.011f);
        OverlayDrag.Place p = OverlayDrag.snapGui(640, 360, r.scale(), PW, PH, M, r.left(), r.top());
        double nowLeft = OverlayDrag.leftGui(p, 640, r.scale(), PW, M), nowTop = OverlayDrag.topGui(p, 360, r.scale(), PH, M);
        assertEquals(r.left(), nowLeft, r.scale() + 0.5);
        assertEquals(r.top(), nowTop, r.scale() + 0.5);
    }

    @Test void aBigWindowOnASmallScreenIsKeptAtTheOrigin() {
        OverlayDrag.Resized r = OverlayDrag.corner(3, 5000, 5000, 6, 6, PW, PH, 1f);
        OverlayDrag.Place p = OverlayDrag.snapGui(300, 160, r.scale(), PW, PH, M, r.left(), r.top());
        assertEquals(0, OverlayDrag.leftGui(p, 300, r.scale(), PW, M), 0.001, "wider than the screen: the left edge wins");
        assertEquals(0, OverlayDrag.topGui(p, 160, r.scale(), PH, M), 0.001);
    }

    // ---------------------------------------------------------------- the handles
    @Test void aCornerHandleIsFoundWithinItsReachAndTheNearestWins() {
        assertEquals(0, OverlayDrag.cornerAt(102, 82, 100, 80, 200, 100, 6));
        assertEquals(1, OverlayDrag.cornerAt(298, 83, 100, 80, 200, 100, 6));
        assertEquals(2, OverlayDrag.cornerAt(101, 181, 100, 80, 200, 100, 6));
        assertEquals(3, OverlayDrag.cornerAt(300, 180, 100, 80, 200, 100, 6));
        assertEquals(-1, OverlayDrag.cornerAt(200, 130, 100, 80, 200, 100, 6), "the middle of the window is not a handle");
        assertEquals(-1, OverlayDrag.cornerAt(107.1, 80, 100, 80, 200, 100, 6), "just out of reach");
        assertEquals(0, OverlayDrag.cornerAt(100, 80, 100, 80, 4, 4, 6), "on a tiny window the nearest of overlapping handles is chosen");
        assertEquals(3, OverlayDrag.cornerAt(104, 84, 100, 80, 4, 4, 6));
    }

    @Test void theCentreFlagsDefaultToOffSoEveryOldViewDrawsAsBefore() {
        OverlayLayout.View old = new OverlayLayout.View(640, 360, 1f, 1, 0.67f, 0f, 12, 5);
        assertFalse(old.centerX());
        assertFalse(old.centerY());
        assertEquals(OverlayMath.placeX(1, old.w(), PW, M, 12), OverlayDrag.placeX(old.anchor(), old.centerX(), old.w(), PW, M, old.offX()));
        for (int a = 0; a < 4; a++) {
            for (int off : new int[]{-30, 0, 17, 5000}) {
                assertEquals(OverlayMath.placeX(a, 640, PW, M, off), OverlayDrag.placeX(a, false, 640, PW, M, off));
                assertEquals(OverlayMath.placeY(a, 360, PH, M, off), OverlayDrag.placeY(a, false, 360, PH, M, off));
                assertEquals(OverlayMath.offsetFromX(a, 640, PW, M, 77), OverlayDrag.offsetFromX(a, false, 640, PW, M, 77));
                assertEquals(OverlayMath.offsetFromY(a, 360, PH, M, 77), OverlayDrag.offsetFromY(a, false, 360, PH, M, 77));
            }
        }
    }
}
