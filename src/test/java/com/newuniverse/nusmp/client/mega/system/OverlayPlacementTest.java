package com.newuniverse.nusmp.client.mega.system;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the owner can now move the System window with the keyboard and the mouse, and the whole risk of that feature is arithmetic nobody can see until the game
 * runs: a window that jumps the wrong way when it is nudged from a right-hand corner, one that can be pushed off the screen and lost, one that stays off screen after
 * the GUI scale was made bigger, a reset that does not put it back, a corner cycle that skips a corner. Each rule is pinned here over every corner and a grid of
 * screens, panels and offsets: the window is ALWAYS on the screen, a nudge moves it one pixel in the direction pressed whatever the corner, a drag lands where the
 * mouse is, and reset and cycle do what their keys say.
 */
class OverlayPlacementTest {
    private static final int M = OverlayStyle.MARGIN;

    // ---------------------------------------------------------------- where the window sits
    @Test void withNoOffsetEachCornerHoldsTheStandardMargin() {
        assertEquals(M, OverlayMath.placeX(0, 640, 164, M, 0));
        assertEquals(M, OverlayMath.placeY(0, 360, 80, M, 0));
        assertEquals(640 - 164 - M, OverlayMath.placeX(1, 640, 164, M, 0));
        assertEquals(M, OverlayMath.placeY(1, 360, 80, M, 0));
        assertEquals(M, OverlayMath.placeX(2, 640, 164, M, 0));
        assertEquals(360 - 80 - M, OverlayMath.placeY(2, 360, 80, M, 0));
        assertEquals(640 - 164 - M, OverlayMath.placeX(3, 640, 164, M, 0));
        assertEquals(360 - 80 - M, OverlayMath.placeY(3, 360, 80, M, 0));
    }

    @Test void theOffsetMovesTheWindowInwardsFromItsCorner() {
        assertEquals(M + 10, OverlayMath.placeX(0, 640, 164, M, 10));
        assertEquals(640 - 164 - M - 10, OverlayMath.placeX(1, 640, 164, M, 10), "a right-hand corner counts from the right edge");
        assertEquals(360 - 80 - M - 7, OverlayMath.placeY(3, 360, 80, M, 7), "a bottom corner counts from the bottom edge");
        assertEquals(M + 7, OverlayMath.placeY(1, 360, 80, M, 7));
    }

    @Test void aNegativeOffsetGoesTowardsTheEdgeAndStopsThere() {
        assertEquals(M - 3, OverlayMath.placeX(0, 640, 164, M, -3));
        assertEquals(0, OverlayMath.placeX(0, 640, 164, M, -300), "never past the left edge");
        assertEquals(640 - 164, OverlayMath.placeX(1, 640, 164, M, -300), "never past the right edge");
        assertEquals(0, OverlayMath.placeY(0, 360, 80, M, -300));
        assertEquals(360 - 80, OverlayMath.placeY(3, 360, 80, M, -300));
    }

    @Test void aHugeOffsetStopsAtTheFarEdge() {
        assertEquals(640 - 164, OverlayMath.placeX(0, 640, 164, M, 5000));
        assertEquals(0, OverlayMath.placeX(1, 640, 164, M, 5000));
        assertEquals(360 - 80, OverlayMath.placeY(0, 360, 80, M, 5000));
        assertEquals(0, OverlayMath.placeY(3, 360, 80, M, 5000));
    }

    @Test void aPanelBiggerThanTheScreenSitsAtTheOrigin() {
        for (int a = 0; a < 4; a++) {
            assertEquals(0, OverlayMath.placeX(a, 100, 164, M, 20));
            assertEquals(0, OverlayMath.placeY(a, 50, 80, M, 20));
        }
    }

    @Test void theWindowIsAlwaysOnTheScreenWhateverTheCornerScreenPanelAndOffset() {
        int[] screens = {200, 320, 427, 640, 1000};
        int[] panels = {120, 164, 220};
        int[] offs = {-5000, -400, -50, -6, -1, 0, 1, 25, 300, 4096, 5000};
        for (int a = 0; a < 4; a++) {
            for (int sw : screens) {
                for (int pw : panels) {
                    for (int off : offs) {
                        int x = OverlayMath.placeX(a, sw, pw, M, off), y = OverlayMath.placeY(a, sw, pw, M, off);
                        assertTrue(x >= 0 && (pw > sw || x + pw <= sw), "x " + x + " panel " + pw + " screen " + sw + " corner " + a + " offset " + off);
                        assertTrue(y >= 0 && (pw > sw || y + pw <= sw), "y " + y + " panel " + pw + " screen " + sw + " corner " + a + " offset " + off);
                    }
                }
            }
        }
    }

    @Test void aBiggerGuiScaleNeverLeavesTheWindowOffScreen() {
        int offX = 400, offY = 200;                                           // saved on a big screen
        int bigW = OverlayMath.windowSize(1280, 1f), bigH = OverlayMath.windowSize(720, 1f);
        int smallW = OverlayMath.windowSize(1280 / 4, 1f), smallH = OverlayMath.windowSize(720 / 4, 1f);      // the same window at GUI scale 4
        assertEquals(1280, bigW);
        assertEquals(320, smallW);
        for (int a = 0; a < 4; a++) {
            int x = OverlayMath.placeX(a, smallW, 164, M, offX), y = OverlayMath.placeY(a, smallH, 80, M, offY);
            assertTrue(x >= 0 && x + 164 <= smallW && y >= 0 && y + 80 <= smallH, "corner " + a + " at " + x + "," + y);
            assertTrue(OverlayMath.placeX(a, bigW, 164, M, offX) + 164 <= bigW);
        }
    }

    @Test void theWindowSizeFollowsTheScaleOption() {
        assertEquals(640, OverlayMath.windowSize(640, 1f));
        assertEquals(320, OverlayMath.windowSize(640, 2f));
        assertEquals(1280, OverlayMath.windowSize(640, 0.5f));
        assertEquals(1, OverlayMath.windowSize(0, 1f));
        assertEquals(6400, OverlayMath.windowSize(640, 0f), "a scale of 0 is guarded");
    }

    // ---------------------------------------------------------------- inverse
    @Test void offsetAndPlaceAreInverses() {
        for (int a = 0; a < 4; a++) {
            for (int x = 0; x <= 640 - 164; x += 37) {
                int off = OverlayMath.offsetFromX(a, 640, 164, M, x);
                assertEquals(x, OverlayMath.placeX(a, 640, 164, M, off), "corner " + a + " x " + x);
            }
            for (int y = 0; y <= 360 - 80; y += 23) {
                int off = OverlayMath.offsetFromY(a, 360, 80, M, y);
                assertEquals(y, OverlayMath.placeY(a, 360, 80, M, off), "corner " + a + " y " + y);
            }
        }
    }

    // ---------------------------------------------------------------- nudge
    @Test void aNudgeMovesTheWindowOnePixelInTheDirectionPressedFromEveryCorner() {
        for (int a = 0; a < 4; a++) {
            int x0 = OverlayMath.placeX(a, 640, 164, M, 0), y0 = OverlayMath.placeY(a, 360, 80, M, 0);
            // start from the middle so no edge is in the way
            OverlayMath.Offset mid = OverlayMath.offsetFor(a, 640, 360, 164, 80, M, 200, 100);
            int mx = OverlayMath.placeX(a, 640, 164, M, mid.x()), my = OverlayMath.placeY(a, 360, 80, M, mid.y());
            assertEquals(200, mx);
            assertEquals(100, my);
            OverlayMath.Offset right = OverlayMath.nudge(a, 640, 360, 164, 80, M, mid.x(), mid.y(), 1, 0);
            assertEquals(201, OverlayMath.placeX(a, 640, 164, M, right.x()), "right from corner " + a);
            assertEquals(100, OverlayMath.placeY(a, 360, 80, M, right.y()));
            OverlayMath.Offset left = OverlayMath.nudge(a, 640, 360, 164, 80, M, mid.x(), mid.y(), -1, 0);
            assertEquals(199, OverlayMath.placeX(a, 640, 164, M, left.x()), "left from corner " + a);
            OverlayMath.Offset down = OverlayMath.nudge(a, 640, 360, 164, 80, M, mid.x(), mid.y(), 0, 1);
            assertEquals(101, OverlayMath.placeY(a, 360, 80, M, down.y()), "down from corner " + a);
            OverlayMath.Offset up = OverlayMath.nudge(a, 640, 360, 164, 80, M, mid.x(), mid.y(), 0, -10);
            assertEquals(90, OverlayMath.placeY(a, 360, 80, M, up.y()), "up by ten from corner " + a);
            assertTrue(x0 >= 0 && y0 >= 0);
        }
    }

    @Test void theSignOfTheStoredOffsetFollowsTheCorner() {
        assertEquals(1, OverlayMath.nudge(0, 640, 360, 164, 80, M, 0, 0, 1, 0).x(), "left corner: moving right adds");
        assertEquals(-1, OverlayMath.nudge(1, 640, 360, 164, 80, M, 0, 0, 1, 0).x(), "right corner: moving right goes towards the edge, the offset shrinks");
        assertEquals(1, OverlayMath.nudge(3, 640, 360, 164, 80, M, 0, 0, -1, 0).x());
        assertEquals(-1, OverlayMath.nudge(2, 640, 360, 164, 80, M, 0, 0, 0, 1).y(), "bottom corner: moving down goes towards the edge");
    }

    @Test void aNudgeCannotPushTheWindowOffTheScreen() {
        OverlayMath.Offset o = OverlayMath.Offset.ZERO;
        for (int i = 0; i < 100; i++) o = OverlayMath.nudge(0, 640, 360, 164, 80, M, o.x(), o.y(), -10, -10);
        assertEquals(0, OverlayMath.placeX(0, 640, 164, M, o.x()));
        assertEquals(0, OverlayMath.placeY(0, 360, 80, M, o.y()));
        assertEquals(-M, o.x(), "the offset stops exactly where the window touches the edge, so the next press the other way moves it at once");
        OverlayMath.Offset back = OverlayMath.nudge(0, 640, 360, 164, 80, M, o.x(), o.y(), 1, 0);
        assertEquals(1, OverlayMath.placeX(0, 640, 164, M, back.x()));
        for (int i = 0; i < 100; i++) o = OverlayMath.nudge(0, 640, 360, 164, 80, M, o.x(), o.y(), 10, 10);
        assertEquals(640 - 164, OverlayMath.placeX(0, 640, 164, M, o.x()));
        assertEquals(360 - 80, OverlayMath.placeY(0, 360, 80, M, o.y()));
    }

    @Test void aNudgeStartsFromWhereTheWindowReallyIsNotFromAStaleOffset() {
        // saved as 3000 on a big screen; on this 320x180 screen the window sits at the far edge, and one step left must move it one pixel from there
        int farX = 320 - 164;
        OverlayMath.Offset o = OverlayMath.nudge(0, 320, 180, 164, 80, M, 3000, 3000, -1, -1);
        assertEquals(farX - 1, OverlayMath.placeX(0, 320, 164, M, o.x()));
        assertEquals(180 - 80 - 1, OverlayMath.placeY(0, 180, 80, M, o.y()));
    }

    // ---------------------------------------------------------------- drag
    @Test void aDragPutsTheTopLeftWhereTheMouseSays() {
        for (int a = 0; a < 4; a++) {
            OverlayMath.Offset o = OverlayMath.offsetFor(a, 640, 360, 164, 80, M, 123, 77);
            assertEquals(123, OverlayMath.placeX(a, 640, 164, M, o.x()), "corner " + a);
            assertEquals(77, OverlayMath.placeY(a, 360, 80, M, o.y()), "corner " + a);
        }
    }

    @Test void aDragPastTheScreenIsStoppedAtTheEdges() {
        for (int a = 0; a < 4; a++) {
            OverlayMath.Offset o = OverlayMath.offsetFor(a, 640, 360, 164, 80, M, -500, -500);
            assertEquals(0, OverlayMath.placeX(a, 640, 164, M, o.x()));
            assertEquals(0, OverlayMath.placeY(a, 360, 80, M, o.y()));
            OverlayMath.Offset p = OverlayMath.offsetFor(a, 640, 360, 164, 80, M, 9000, 9000);
            assertEquals(640 - 164, OverlayMath.placeX(a, 640, 164, M, p.x()));
            assertEquals(360 - 80, OverlayMath.placeY(a, 360, 80, M, p.y()));
        }
    }

    @Test void theStoredOffsetStaysInTheConfigsRange() {
        OverlayMath.Offset o = OverlayMath.offsetFor(0, 100000, 100000, 164, 80, M, 90000, 90000);
        assertEquals(OverlayMath.MAX_OFFSET, o.x());
        assertEquals(OverlayMath.MAX_OFFSET, o.y());
        assertEquals(-OverlayMath.MAX_OFFSET, OverlayMath.clampOffset(-99999));
        assertEquals(5, OverlayMath.clampOffset(5));
    }

    // ---------------------------------------------------------------- reset and the corners
    @Test void resetPutsTheWindowBackInTheDefaultCorner() {
        assertEquals(0, OverlayMath.DEFAULT_ANCHOR);
        assertEquals(0, OverlayMath.Offset.ZERO.x());
        assertEquals(0, OverlayMath.Offset.ZERO.y());
        assertEquals(M, OverlayMath.placeX(OverlayMath.DEFAULT_ANCHOR, 640, 164, M, OverlayMath.Offset.ZERO.x()));
        assertEquals(M, OverlayMath.placeY(OverlayMath.DEFAULT_ANCHOR, 360, 80, M, OverlayMath.Offset.ZERO.y()));
    }

    @Test void theCornerKeyWalksClockwiseThroughAllFourAndComesBack() {
        assertEquals(1, OverlayMath.nextAnchor(0), "top-left to top-right");
        assertEquals(3, OverlayMath.nextAnchor(1), "top-right to bottom-right");
        assertEquals(2, OverlayMath.nextAnchor(3), "bottom-right to bottom-left");
        assertEquals(0, OverlayMath.nextAnchor(2), "bottom-left to top-left");
        int a = 0;
        boolean[] seen = new boolean[4];
        for (int i = 0; i < 4; i++) {
            seen[a] = true;
            a = OverlayMath.nextAnchor(a);
        }
        assertEquals(0, a, "four steps are a full round");
        for (boolean s : seen) assertTrue(s);
        for (int bad : new int[]{-1, 4, 99, Integer.MIN_VALUE}) {
            assertTrue(OverlayMath.nextAnchor(bad) >= 0 && OverlayMath.nextAnchor(bad) <= 3);
            assertEquals(0, OverlayMath.safeAnchor(bad));
        }
    }

    @Test void theSidesOfTheCornersAreRight() {
        assertFalse(OverlayMath.rightAnchored(0));
        assertTrue(OverlayMath.rightAnchored(1));
        assertFalse(OverlayMath.rightAnchored(2));
        assertTrue(OverlayMath.rightAnchored(3));
        assertFalse(OverlayMath.bottomAnchored(0));
        assertFalse(OverlayMath.bottomAnchored(1));
        assertTrue(OverlayMath.bottomAnchored(2));
        assertTrue(OverlayMath.bottomAnchored(3));
    }

    // ---------------------------------------------------------------- the mouse and the toasts
    @Test void aPointIsInsideTheRectangleOnItsLeftAndTopEdgesButNotItsRightAndBottom() {
        assertTrue(OverlayMath.contains(10, 20, 100, 50, 10, 20));
        assertTrue(OverlayMath.contains(10, 20, 100, 50, 109.9, 69.9));
        assertFalse(OverlayMath.contains(10, 20, 100, 50, 110, 30));
        assertFalse(OverlayMath.contains(10, 20, 100, 50, 50, 70));
        assertFalse(OverlayMath.contains(10, 20, 100, 50, 9.9, 30));
        assertFalse(OverlayMath.contains(10, 20, 100, 50, 50, 19.9));
    }

    @Test void theToastsGoUnderThePanelOnlyWhenThePanelSitsInTheirColumnAtTheTop() {
        int col = OverlayStyle.TOAST_COLUMN_W;
        assertEquals(6 + 80 + 8, OverlayMath.toastTop(6, 6, 164, 80, 360, col, M, 8), "the default top-left corner: below the window, as before");
        assertEquals(M + 2, OverlayMath.toastTop(470, 6, 164, 80, 360, col, M, 8), "a window on the right leaves the column free");
        assertEquals(M + 2, OverlayMath.toastTop(6, 274, 164, 80, 360, col, M, 8), "a window in the lower half leaves the top free");
        assertEquals(M + 2, OverlayMath.toastTop(col + M, 6, 164, 80, 640, col, M, 8), "a window just past the column does not touch it");
        assertEquals(40 + 80 + 8, OverlayMath.toastTop(100, 40, 164, 80, 360, col, M, 8), "a moved window still in the column pushes the toasts below it");
    }

    @Test void theToastsStepBelowTheEditHintBoxWhenItWouldCoverThem() {
        int col = OverlayStyle.TOAST_COLUMN_W, stack = OverlayStyle.TOAST_MAX * (OverlayStyle.TOAST_H + OverlayStyle.TOAST_GAP);
        // the toasts start at 94 under a window at the top-left; the hint box sits right there
        assertEquals(90 + 40 + 4, OverlayMath.toastClear(94, 6, 90, 300, 40, col, stack, M, 4), "a box in the column at the toasts' height: they start under it");
        assertEquals(94, OverlayMath.toastClear(94, 6, 400, 300, 40, col, stack, M, 4), "a box far below the stack leaves them alone");
        assertEquals(94, OverlayMath.toastClear(94, 6, 20, 300, 40, col, stack, M, 4), "a box wholly above the first toast leaves them alone");
        assertEquals(94, OverlayMath.toastClear(94, 500, 90, 300, 40, col, stack, M, 4), "a box on the right-hand side is not in their column");
        assertEquals(94, OverlayMath.toastClear(94, 6, 90, 0, 0, col, stack, M, 4), "an empty box (no hint lines) never moves them");
    }
}
