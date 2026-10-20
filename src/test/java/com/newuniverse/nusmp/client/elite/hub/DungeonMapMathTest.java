package com.newuniverse.nusmp.client.elite.hub;

import com.newuniverse.nusmp.client.elite.hub.DungeonMapMath.Cam;
import com.newuniverse.nusmp.client.elite.hub.DungeonMapMath.Frame;
import com.newuniverse.nusmp.client.ui.scale.GuiScaleMath;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the Dungeon Map's pan, zoom, clamp, frame layout, legend wrapping, room colours and the backdrop under the GUI size option are plain maths, so a wrong sign or a missing limit (a map dragged
 * out of sight, a zoom that overshoots, a backdrop that leaves the HUD showing round a scaled screen) is caught without a game.
 */
class DungeonMapMathTest {
    private static final int[] B = {0, 0, 199, 99};            // a floor 200 x 100 blocks
    private static final Frame F = DungeonMapMath.frame(854, 480, 1);

    @Test
    void frameSitsBetweenTheBars() {
        assertEquals(DungeonMapMath.TITLE_H, F.top());
        assertEquals(480 - DungeonMapMath.bottomBarHeight(1), F.bottom());
        assertTrue(F.height() > 300 && F.width() == 854);
        assertTrue(DungeonMapMath.frame(100, 40, 3).height() >= 16, "a tiny window still gets a frame");
        assertTrue(DungeonMapMath.bottomBarHeight(2) > DungeonMapMath.bottomBarHeight(1));
    }

    @Test
    void fitShowsTheWholeFloorCentred() {
        Cam c = DungeonMapMath.fit(B, F);
        assertEquals(100.0, c.cx(), 1e-9);
        assertEquals(50.0, c.cz(), 1e-9);
        double left = DungeonMapMath.screenX(c, F, 0), right = DungeonMapMath.screenX(c, F, 200);
        double top = DungeonMapMath.screenZ(c, F, 0), bottom = DungeonMapMath.screenZ(c, F, 100);
        assertTrue(left >= F.left() && right <= F.right() && top >= F.top() && bottom <= F.bottom(), "the whole floor is inside the frame");
        assertEquals(c.zoom(), DungeonMapMath.fitZoom(B, F), 1e-12);
    }

    @Test
    void screenAndWorldAreInverse() {
        Cam c = new Cam(37.5, -12.25, 3.5);
        for (double w : new double[] {-100, 0, 12.5, 999}) {
            assertEquals(w, DungeonMapMath.worldX(c, F, DungeonMapMath.screenX(c, F, w)), 1e-9);
            assertEquals(w, DungeonMapMath.worldZ(c, F, DungeonMapMath.screenZ(c, F, w)), 1e-9);
        }
        assertEquals(F.midX(), DungeonMapMath.screenX(c, F, 37.5), 1e-9, "the camera centre is the middle of the frame");
    }

    @Test
    void dragMovesTheMapWithThePointer() {
        Cam c = DungeonMapMath.fit(B, F);
        double wx = 80, before = DungeonMapMath.screenX(c, F, wx);
        Cam d = DungeonMapMath.pan(c, 30, -12, B, F);
        assertEquals(before + 30, DungeonMapMath.screenX(d, F, wx), 1e-6, "dragging right moves the map right");
        assertEquals(DungeonMapMath.screenZ(c, F, 40) - 12, DungeonMapMath.screenZ(d, F, 40), 1e-6, "dragging up moves the map up");
        assertEquals(c.zoom(), d.zoom(), 0.0);
    }

    @Test
    void panIsHeldToTheFloor() {
        Cam c = DungeonMapMath.fit(B, F);
        Cam far = DungeonMapMath.pan(c, -1e9, 1e9, B, F);
        assertEquals(B[2] + 1 + DungeonMapMath.PAN_PAD, far.cx(), 1e-9);
        assertEquals(B[1] - DungeonMapMath.PAN_PAD, far.cz(), 1e-9);
        Cam other = DungeonMapMath.pan(c, 1e9, -1e9, B, F);
        assertEquals(B[0] - DungeonMapMath.PAN_PAD, other.cx(), 1e-9);
        assertEquals(B[3] + 1 + DungeonMapMath.PAN_PAD, other.cz(), 1e-9);
    }

    @Test
    void zoomKeepsThePointUnderThePointerAndHasLimits() {
        Cam c = DungeonMapMath.fit(B, F);
        double mx = 600, my = 200;
        double wx = DungeonMapMath.worldX(c, F, mx), wz = DungeonMapMath.worldZ(c, F, my);
        Cam z = DungeonMapMath.zoomAbout(c, mx, my, 3, B, F);
        assertTrue(z.zoom() > c.zoom());
        assertEquals(wx, DungeonMapMath.worldX(z, F, mx), 1e-6);
        assertEquals(wz, DungeonMapMath.worldZ(z, F, my), 1e-6);
        Cam max = c, min = c;
        for (int i = 0; i < 200; i++) {
            max = DungeonMapMath.zoomAbout(max, mx, my, 1, B, F);
            min = DungeonMapMath.zoomAbout(min, mx, my, -1, B, F);
        }
        assertEquals(DungeonMapMath.MAX_ZOOM, max.zoom(), 1e-9);
        assertEquals(DungeonMapMath.minZoom(DungeonMapMath.fitZoom(B, F)), min.zoom(), 1e-9);
        assertTrue(min.zoom() > 0);
    }

    @Test
    void aHugeFloorMayZoomOutToItsFit() {
        int[] huge = {0, 0, 19999, 19999};
        double fit = DungeonMapMath.fitZoom(huge, F);
        assertTrue(fit < DungeonMapMath.DEFAULT_MIN_ZOOM);
        assertEquals(fit, DungeonMapMath.minZoom(fit), 1e-12, "the minimum never exceeds the fit, so the floor can always be fitted");
        assertEquals(fit, DungeonMapMath.clampZoom(0.0001, fit), 1e-12);
        assertEquals(DungeonMapMath.ABS_MIN_ZOOM, DungeonMapMath.fitZoom(new int[] {0, 0, 10_000_000, 10_000_000}, F), 1e-12);
    }

    @Test
    void emptyAndNanAreSafe() {
        int[] none = {0, 0, 0, 0};
        Cam c = DungeonMapMath.fit(none, F);
        assertTrue(c.zoom() >= DungeonMapMath.ABS_MIN_ZOOM && c.zoom() <= DungeonMapMath.MAX_ZOOM);
        Cam n = DungeonMapMath.clamp(new Cam(Double.NaN, Double.NaN, Double.NaN), B, F);
        assertTrue(!Double.isNaN(n.cx()) && !Double.isNaN(n.cz()) && !Double.isNaN(n.zoom()));
    }

    @Test
    void centreOnMeZoomsInAndCentres() {
        Cam fit = DungeonMapMath.fit(B, F);
        Cam me = DungeonMapMath.centreOn(fit, 120, 30, 2.0, B, F);
        assertEquals(120, me.cx(), 1e-9);
        assertEquals(30, me.cz(), 1e-9);
        assertEquals(Math.max(fit.zoom(), 2.0), me.zoom(), 1e-9);
        assertEquals(F.midX(), DungeonMapMath.screenX(me, F, 120), 1e-9);
        Cam out = DungeonMapMath.centreOn(fit, 1e6, 1e6, 2.0, B, F);
        assertEquals(B[2] + 1 + DungeonMapMath.PAN_PAD, out.cx(), 1e-9, "a player outside the floor is clamped, not lost");
    }

    @Test
    void guiScaleMouseConversionRoundTrips() {
        // GuiScale scales the screen round the window's middle and hands the screen toLocal(mouse): a click on a scaled point must land on the same local point
        double s = 0.85, px = 854 / 2.0;
        double local = 600;
        double mouse = GuiScaleMath.toScreen(local, px, s);
        assertEquals(local, GuiScaleMath.toLocal(mouse, px, s), 1e-9);
        assertEquals(10.0, GuiScaleMath.deltaToLocal(8.5, s), 1e-9, "a drag of 8.5 window pixels is 10 screen pixels at 85%");
        // and the map under that conversion: a drag of 8.5 px moves the map by 10 local px, so the point under the pointer stays under it
        Cam c = DungeonMapMath.fit(B, F);
        Cam d = DungeonMapMath.pan(c, GuiScaleMath.deltaToLocal(8.5, s), 0, B, F);
        assertEquals(DungeonMapMath.screenX(c, F, 90) + 10, DungeonMapMath.screenX(d, F, 90), 1e-6);
    }

    @Test
    void backdropCoversTheWholeWindowAtEveryScale() {
        for (double s : new double[] {0.5, 0.85, 1.0, 1.25, 2.0}) {
            int[] r = DungeonMapMath.backdrop(854, 480, s);
            double l = GuiScaleMath.toScreen(r[0], 427, s), t = GuiScaleMath.toScreen(r[1], 240, s);
            double rr = GuiScaleMath.toScreen(r[2], 427, s), b = GuiScaleMath.toScreen(r[3], 240, s);
            assertTrue(l <= 0 && t <= 0 && rr >= 854 && b >= 480, "scale " + s + " leaves the window uncovered: " + l + "," + t + " " + rr + "," + b);
        }
        int[] one = DungeonMapMath.backdrop(854, 480, 1.0);
        assertTrue(one[0] >= -2 && one[2] <= 856, "at normal size the backdrop is only the screen");
        int[] bad = DungeonMapMath.backdrop(854, 480, Double.NaN);
        assertTrue(bad[0] <= 0 && bad[2] >= 854, "a bad scale counts as 1");
    }

    @Test
    void legendWrapsInsteadOfRunningOffTheWindow() {
        int[] w = {60, 80, 50, 70, 90};
        int[][] one = DungeonMapMath.legendLayout(w, 10, 1000, 10);
        assertEquals(1, DungeonMapMath.legendRows(one));
        assertEquals(10, one[0][0]);
        assertEquals(10 + 60 + 10, one[1][0]);
        int[][] wrap = DungeonMapMath.legendLayout(w, 10, 200, 10);
        assertTrue(DungeonMapMath.legendRows(wrap) > 1);
        for (int i = 0; i < w.length; i++) assertTrue(wrap[i][0] + w[i] <= 200 || wrap[i][0] == 10, "entry " + i + " runs past the window");
        int[][] narrow = DungeonMapMath.legendLayout(new int[] {500}, 10, 100, 10);
        assertEquals(1, DungeonMapMath.legendRows(narrow), "one wide entry stays on the first row");
        assertEquals(1, DungeonMapMath.legendRows(DungeonMapMath.legendLayout(new int[0], 10, 100, 10)));
    }

    @Test
    void roomsOutsideTheFrameAreCulled() {
        assertTrue(DungeonMapMath.visible(F, 100, 100, 150, 150));
        assertTrue(!DungeonMapMath.visible(F, -50, 100, 0, 150));
        assertTrue(!DungeonMapMath.visible(F, 100, 0, 150, F.top()));
        assertTrue(!DungeonMapMath.visible(F, 100, F.bottom(), 150, F.bottom() + 40));
        assertTrue(DungeonMapMath.visible(F, -50, 100, 1, 150), "a room that just reaches in is drawn");
    }

    @Test
    void everyRoomKindHasAnOpaqueColourAndTheDimIsDarker() {
        for (RoomKind k : RoomKind.values()) {
            int c = DungeonMapMath.colour(k.ordinal());
            assertEquals(0xFF, c >>> 24, k + " must be opaque");
            int d = DungeonMapMath.dim(c);
            assertEquals(0xFF, d >>> 24);
            assertTrue(((d >> 16 & 255) + (d >> 8 & 255) + (d & 255)) < ((c >> 16 & 255) + (c >> 8 & 255) + (c & 255)), k + " dimmed must be darker");
        }
        assertEquals(DungeonMapMath.colour(RoomKind.HALL.ordinal()), DungeonMapMath.colour(9999), "an unknown kind reads as a hall");
        assertEquals(DungeonMapMath.colour(RoomKind.HALL.ordinal()), DungeonMapMath.colour(-1));
    }
}
