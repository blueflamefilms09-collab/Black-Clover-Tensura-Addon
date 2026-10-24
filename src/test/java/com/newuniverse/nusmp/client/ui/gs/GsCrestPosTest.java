package com.newuniverse.nusmp.client.ui.gs;

import com.newuniverse.nusmp.client.ui.gs.GsCrestPos.Corner;
import com.newuniverse.nusmp.client.ui.gs.GsCrestPos.Place;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the player drags the crest anywhere; whatever is stored, the crest must stay fully on screen at every window size and GUI scale. */
class GsCrestPosTest {
    private static final int W = 32;

    @Test
    void theDefaultIsTheTopRightCornerWithAMargin() {
        int[] xy = GsCrestPos.resolve(Place.DEFAULT, 427, 240, W, W);
        assertArrayEquals(new int[]{427 - W - GsCrestPos.MARGIN, GsCrestPos.MARGIN}, xy);
    }

    @Test
    void eachCornerCountsInwardFromItsOwnEdges() {
        assertArrayEquals(new int[]{6, 6}, GsCrestPos.resolve(new Place(Corner.TOP_LEFT, 0, 0), 400, 300, W, W));
        assertArrayEquals(new int[]{400 - W - 6, 300 - W - 6}, GsCrestPos.resolve(new Place(Corner.BOTTOM_RIGHT, 0, 0), 400, 300, W, W));
        assertArrayEquals(new int[]{6 + 10, 300 - W - 6 - 20}, GsCrestPos.resolve(new Place(Corner.BOTTOM_LEFT, 10, 20), 400, 300, W, W));
        assertArrayEquals(new int[]{400 - W - 6 - 10, 6 + 20}, GsCrestPos.resolve(new Place(Corner.TOP_RIGHT, 10, 20), 400, 300, W, W));
    }

    @Test
    void aHugeOrNegativeOffsetIsClampedIntoTheScreen() {
        for (Corner c : Corner.values()) {
            for (int off : new int[]{-100000, -500, 0, 500, 100000, Integer.MIN_VALUE, Integer.MAX_VALUE}) {
                int[] xy = GsCrestPos.resolve(new Place(c, off, off), 320, 180, W, W);
                assertTrue(xy[0] >= 0 && xy[0] + W <= 320, c + " " + off + " x=" + xy[0]);
                assertTrue(xy[1] >= 0 && xy[1] + W <= 180, c + " " + off + " y=" + xy[1]);
            }
        }
    }

    @Test
    void aScreenSmallerThanTheCrestStillGivesNonNegativeCoordinates() {
        int[] xy = GsCrestPos.resolve(new Place(Corner.BOTTOM_RIGHT, 50, 50), 20, 20, W, W);
        assertEquals(0, xy[0]);
        assertEquals(0, xy[1]);
    }

    @Test
    void aDroppedCrestStoresTheNearestCornerAndResolvesBackToTheSameSpot() {
        int sw = 427, sh = 240;
        int[][] drops = {{10, 10}, {395, 5}, {5, 200}, {390, 200}, {200, 100}, {0, 0}, {395, 208}};
        for (int[] d : drops) {
            Place p = GsCrestPos.fromPosition(d[0], d[1], sw, sh, W, W);
            int[] back = GsCrestPos.resolve(p, sw, sh, W, W);
            assertArrayEquals(d, back, "drop " + d[0] + "," + d[1] + " stored as " + p);
        }
        assertEquals(Corner.TOP_LEFT, GsCrestPos.fromPosition(10, 10, sw, sh, W, W).corner());
        assertEquals(Corner.TOP_RIGHT, GsCrestPos.fromPosition(395, 5, sw, sh, W, W).corner());
        assertEquals(Corner.BOTTOM_LEFT, GsCrestPos.fromPosition(5, 200, sw, sh, W, W).corner());
        assertEquals(Corner.BOTTOM_RIGHT, GsCrestPos.fromPosition(390, 200, sw, sh, W, W).corner());
    }

    @Test
    void aDropOutsideTheScreenIsPulledBackIn() {
        Place p = GsCrestPos.fromPosition(-50, 9999, 300, 200, W, W);
        int[] xy = GsCrestPos.resolve(p, 300, 200, W, W);
        assertEquals(0, xy[0]);
        assertEquals(200 - W, xy[1]);
    }

    @Test
    void theStoredPlaceKeepsTheCornerWhenTheWindowChanges() {
        Place p = new Place(Corner.BOTTOM_RIGHT, 12, 30);
        int[] small = GsCrestPos.resolve(p, 320, 180, W, W);
        int[] big = GsCrestPos.resolve(p, 640, 360, W, W);
        assertEquals(320 - W - 6 - 12, small[0]);
        assertEquals(640 - W - 6 - 12, big[0]);
        assertEquals(180 - W - 6 - 30, small[1]);
        assertEquals(360 - W - 6 - 30, big[1]);
    }

    @Test
    void cornerTextParsesAndSanitizeHoldsTheRange() {
        assertEquals(Corner.BOTTOM_LEFT, Corner.parse("bottom_left"));
        assertEquals(Corner.BOTTOM_LEFT, Corner.parse(" Bottom-Left "));
        assertEquals(Corner.TOP_RIGHT, Corner.parse("middle"));
        assertEquals(Corner.TOP_RIGHT, Corner.parse(null));
        Place p = GsCrestPos.sanitize(new Place(null, 99999, -99999));
        assertEquals(Corner.TOP_RIGHT, p.corner());
        assertEquals(GsCrestPos.MAX_OFFSET, p.dx());
        assertEquals(-GsCrestPos.MAX_OFFSET, p.dy());
    }
}
