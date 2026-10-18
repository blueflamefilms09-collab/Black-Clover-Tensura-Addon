package com.newuniverse.nusmp.heroweapon.weapon.pierce;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Why: the thrusts, the arrow fans and the Minute Hand all rest on these few formulas, and every one of them has an edge where a naive version fails
 * silently in play (a target at the end of a segment, a bearing that wraps, a step across the 360 degree seam). The tests pin the edges.
 */
class GeoTest {
    @Test
    void segmentParamRunsFromZeroToOneAndIsClamped() {
        assertEquals(0.5, Geo.segmentParam(0, 0, 0, 10, 0, 0, 5, 7, 7), 1e-9);
        assertEquals(0.0, Geo.segmentParam(0, 0, 0, 10, 0, 0, -9, 0, 0), 1e-9);
        assertEquals(1.0, Geo.segmentParam(0, 0, 0, 10, 0, 0, 99, 0, 0), 1e-9);
        assertEquals(0.0, Geo.segmentParam(1, 1, 1, 1, 1, 1, 9, 9, 9), 1e-9);
    }

    @Test
    void bearingsRunFromXTowardsZAndWrap() {
        assertEquals(0.0, Geo.bearing(1, 0), 1e-9);
        assertEquals(90.0, Geo.bearing(0, 1), 1e-9);
        assertEquals(180.0, Geo.bearing(-1, 0), 1e-9);
        assertEquals(270.0, Geo.bearing(0, -1), 1e-9);
        assertEquals(0.0, Geo.bearing(0, 0), 1e-9);
        assertTrue(Geo.bearing(1, -1e-9) > 359.0, "just below the +x axis is just below 360");
        double[] d = Geo.direction(90.0);
        assertEquals(0.0, d[0], 1e-9);
        assertEquals(1.0, d[1], 1e-9);
    }

    @Test
    void turnTakesTheShortWayAndKeepsItsSign() {
        assertEquals(20.0, Geo.turn(350, 10), 1e-9, "across the seam, forwards");
        assertEquals(-20.0, Geo.turn(10, 350), 1e-9, "across the seam, backwards");
        assertEquals(180.0, Geo.turn(0, 180), 1e-9, "half a turn is +180, never -180");
        assertEquals(180.0, Geo.turn(180, 0), 1e-9);
        assertEquals(0.0, Geo.turn(77, 77), 1e-9);
        assertEquals(30.0, Geo.turn(0, 390), 1e-9, "more than a full turn is folded");
    }

    @Test
    void theHandCatchesWhatItPassesIncludingAcrossTheSeam() {
        // a step from 350 to 14 degrees (24 degrees forwards across north) with a hand 10 degrees either side
        assertTrue(Geo.swept(350, 14, 0, 10), "in the middle of the step");
        assertTrue(Geo.swept(350, 14, 355, 10), "near the start");
        assertTrue(Geo.swept(350, 14, 20, 10), "just ahead of the new position, within the hand's width");
        assertTrue(Geo.swept(350, 14, 342, 10), "just behind the old position, within the hand's width");
        assertFalse(Geo.swept(350, 14, 40, 10), "well ahead");
        assertFalse(Geo.swept(350, 14, 300, 10), "well behind");
        assertFalse(Geo.swept(350, 14, 180, 10), "the opposite side");
        // a backwards step
        assertTrue(Geo.swept(14, 350, 0, 10));
        assertFalse(Geo.swept(14, 350, 40, 10));
    }

    @Test
    void aFullTurnInStepsCatchesEveryBearingAtLeastOnce() {
        int steps = 15;
        for (int b = 0; b < 360; b++) {
            int caught = 0;
            for (int s = 1; s <= steps; s++) if (Geo.swept(s * 24.0 - 24.0, s * 24.0, b, 4.0)) caught++;
            assertTrue(caught >= 1, "bearing " + b + " was never caught");
        }
    }

    @Test
    void rotateTurnsTowardsZ() {
        double[] r = Geo.rotate(1, 0, 90);
        assertEquals(0.0, r[0], 1e-9);
        assertEquals(1.0, r[1], 1e-9);
        double[] same = Geo.rotate(3, 4, 0);
        assertEquals(3.0, same[0], 1e-9);
        assertEquals(4.0, same[1], 1e-9);
    }

    @Test
    void aFanIsCentredAndEven() {
        double[] one = Geo.fan(1, 40);
        assertEquals(1, one.length);
        assertEquals(0.0, one[0], 1e-9);
        double[] two = Geo.fan(2, 40);
        assertEquals(-20.0, two[0], 1e-9);
        assertEquals(20.0, two[1], 1e-9);
        double[] seven = Geo.fan(7, 36);
        assertEquals(7, seven.length);
        assertEquals(-18.0, seven[0], 1e-9);
        assertEquals(0.0, seven[3], 1e-9);
        assertEquals(18.0, seven[6], 1e-9);
        assertEquals(1, Geo.fan(0, 40).length, "never an empty fan");
    }

    @Test
    void clampKeepsAValueInside() {
        assertEquals(5.0, Geo.clamp(9, 0, 5), 1e-9);
        assertEquals(0.0, Geo.clamp(-9, 0, 5), 1e-9);
        assertEquals(3.0, Geo.clamp(3, 0, 5), 1e-9);
    }
}
