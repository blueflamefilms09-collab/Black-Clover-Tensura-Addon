package com.newuniverse.nusmp.labyrinth.mega.guard;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the Guardian's traits are drawn from a few small shapes (the cells it blinks to behind its target, the lightning chain from player to player, the
 * ring of stars and the trail of fire cells), and a chain that revisits a player, a ring that is not even or a trail that never forgets would hit a party
 * unfairly or grow without bound; this test pins the geometry of {@link TraitMath} with no game running.
 */
class TraitMathTest {
    @Test
    void theFirstBlinkSpotIsRightBehindTheTarget() {
        double[][] s = TraitMath.blinkSpots(10.0, 20.0, 0.0, 1.0, 1.8);        // the target looks towards +z
        assertEquals(4, s.length);
        assertEquals(10.0, s[0][0], 1e-9);
        assertEquals(20.0 - 1.8, s[0][1], 1e-9, "behind = against the look");
        assertEquals(10.0 + 1.8, s[1][0], 1e-9, "then to one side");
        assertEquals(20.0, s[1][1], 1e-9);
        assertEquals(10.0 - 1.8, s[2][0], 1e-9, "then to the other");
        assertEquals(20.0 + 1.8, s[3][1], 1e-9, "the front is the last resort");
        for (double[] p : s) assertEquals(1.8, Math.hypot(p[0] - 10.0, p[1] - 20.0), 1e-9, "every spot is the same distance away");
    }

    @Test
    void aTargetLookingStraightUpCountsAsFacingPlusX() {
        double[][] s = TraitMath.blinkSpots(0.0, 0.0, 0.0, 0.0, 2.0);
        assertEquals(-2.0, s[0][0], 1e-9);
        assertEquals(0.0, s[0][1], 1e-9);
        for (double[] p : s) assertFalse(Double.isNaN(p[0]) || Double.isNaN(p[1]));
    }

    @Test
    void theChainVisitsTheNearestPlayerThenHopsOn() {
        double[][] pts = {{20, 0, 0}, {3, 0, 0}, {8, 0, 0}, {40, 0, 0}};
        int[] order = TraitMath.chainOrder(0, 0, 0, pts, 8.0, 6.0, 3);
        assertArrayEq(new int[]{1, 2}, order);                          // 3 first, then 8 (5 away); 20 is 12 away from 8: out of hop reach
        int[] more = TraitMath.chainOrder(0, 0, 0, new double[][]{{3, 0, 0}, {8, 0, 0}, {13, 0, 0}, {18, 0, 0}}, 8.0, 6.0, 3);
        assertArrayEq(new int[]{0, 1, 2}, more);
        int[] capped = TraitMath.chainOrder(0, 0, 0, new double[][]{{3, 0, 0}, {8, 0, 0}, {13, 0, 0}, {18, 0, 0}}, 8.0, 6.0, 2);
        assertEquals(2, capped.length, "at most maxTargets");
    }

    @Test
    void theChainNeverRevisitsAndNeedsSomeoneInReach() {
        assertEquals(0, TraitMath.chainOrder(0, 0, 0, new double[][]{{30, 0, 0}}, 8.0, 6.0, 3).length, "nobody in reach: no chain");
        assertEquals(0, TraitMath.chainOrder(0, 0, 0, new double[0][], 8.0, 6.0, 3).length);
        assertEquals(0, TraitMath.chainOrder(0, 0, 0, new double[][]{{1, 0, 0}}, 8.0, 6.0, 0).length);
        int[] two = TraitMath.chainOrder(0, 0, 0, new double[][]{{1, 0, 0}, {1, 0, 0}}, 8.0, 6.0, 5);
        assertArrayEq(new int[]{0, 1}, two);                            // a tie goes to the lower index; each is visited once
    }

    @Test
    void eachHopIsWeakerThanTheLast() {
        assertEquals(10.0, TraitMath.chainDamage(10.0, 0), 1e-9);
        assertEquals(7.5, TraitMath.chainDamage(10.0, 1), 1e-9);
        assertEquals(10.0 * 0.75 * 0.75, TraitMath.chainDamage(10.0, 2), 1e-9);
        assertEquals(10.0, TraitMath.chainDamage(10.0, -3), 1e-9, "a negative hop is the first");
    }

    @Test
    void theStarRingIsEvenAndTurns() {
        double[][] ring = TraitMath.starRing(5.0, -5.0, 7.0, 8, 0.0);
        assertEquals(8, ring.length);
        for (double[] p : ring) assertEquals(7.0, Math.hypot(p[0] - 5.0, p[1] + 5.0), 1e-9, "all on the ring");
        assertEquals(12.0, ring[0][0], 1e-9);
        assertEquals(-5.0, ring[0][1], 1e-9);
        double step = Math.hypot(ring[0][0] - ring[1][0], ring[0][1] - ring[1][1]);
        for (int k = 1; k < 8; k++) {
            double d = Math.hypot(ring[k][0] - ring[(k + 1) % 8][0], ring[k][1] - ring[(k + 1) % 8][1]);
            assertEquals(step, d, 1e-9, "equal gaps");
        }
        double[][] turned = TraitMath.starRing(5.0, -5.0, 7.0, 8, Math.PI / 8);
        assertNotEquals(ring[0][0], turned[0][0]);
        assertEquals(0, TraitMath.starRing(0, 0, 3.0, 0, 0.0).length);
        assertEquals(0, TraitMath.starRing(0, 0, 3.0, -2, 0.0).length);
    }

    @Test
    void theFireTrailKeepsFewCellsAndForgetsTheOldOnes() {
        TraitMath.FireTrail t = new TraitMath.FireTrail();
        for (int i = 0; i < 40; i++) t.add(i, 64, 0, 1000 + i, 24);
        assertEquals(24, t.size(), "never more than max");
        assertEquals(16, t.cell(0)[0], "the oldest cells were dropped first");
        t.add(39, 64, 0, 5000, 24);
        assertEquals(24, t.size(), "walking on a cell again refreshes it, it is not added twice");
        t.prune(1030);
        assertTrue(t.size() >= 1 && t.size() < 24, "burnt-out cells are forgotten: " + t.size());
        t.prune(10_000);
        assertEquals(0, t.size());
    }

    @Test
    void aPlayerBurnsOnlyInsideALiveCell() {
        TraitMath.FireTrail t = new TraitMath.FireTrail();
        t.add(10, 64, 10, 200, 24);
        assertTrue(t.burnsAt(10.5, 64.0, 10.5, 0.0, 100), "in the middle");
        assertTrue(t.burnsAt(10.99, 64.5, 10.01, 0.0, 100), "still in the cell");
        assertFalse(t.burnsAt(11.6, 64.0, 10.5, 0.0, 100), "beside it");
        assertTrue(t.burnsAt(11.4, 64.0, 10.5, 0.9, 100), "a wider trail reaches further");
        assertFalse(t.burnsAt(10.5, 67.5, 10.5, 0.0, 100), "not three blocks above the floor");
        assertFalse(t.burnsAt(10.5, 62.0, 10.5, 0.0, 100), "not below it");
        assertFalse(t.burnsAt(10.5, 64.0, 10.5, 0.0, 200), "burnt out exactly at its expiry");
        t.clear();
        assertFalse(t.burnsAt(10.5, 64.0, 10.5, 0.0, 100));
    }

    private static void assertArrayEq(int[] expected, int[] actual) {
        assertEquals(expected.length, actual.length, "length");
        for (int i = 0; i < expected.length; i++) assertEquals(expected[i], actual[i], "index " + i);
    }
}
