package com.newuniverse.nusmp.skilltree.loot;

import com.newuniverse.nusmp.dungeon.sentient.Dice;
import org.junit.jupiter.api.Test;

import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: which route a dropped book opens is weighted by each route's own {@code loot_weight}, and the three promises of that field are tested at their edges: a weight
 * of 0 (or less) is NEVER picked, a list with nothing positive answers "none" instead of guessing an index, and one dice value maps to exactly one index at the
 * exact boundaries between the weights. A long seeded run then checks the proportions, and a dice that misbehaves by returning exactly 1.0 must not walk off the end of
 * the array.
 */
class RoutePickTest {
    private static Dice fixed(double v) { return () -> v; }

    @Test
    void noPositiveWeightMeansNone() {
        assertEquals(-1, RoutePick.pick(new int[0], fixed(0.5)));
        assertEquals(-1, RoutePick.pick(new int[] {0, 0, 0}, fixed(0.5)));
        assertEquals(-1, RoutePick.pick(new int[] {-2, 0, -7}, fixed(0.5)));
        assertEquals(-1, RoutePick.pick(null, fixed(0.5)));
        assertEquals(-1, RoutePick.pick(new int[] {1}, null));
    }

    @Test
    void aSinglePositiveWeightIsAlwaysChosen() {
        for (double d : new double[] {0.0, 0.5, 0.9999999}) {
            assertEquals(2, RoutePick.pick(new int[] {0, -1, 4, 0}, fixed(d)));
            assertEquals(0, RoutePick.pick(new int[] {9}, fixed(d)));
        }
    }

    @Test
    void theBoundariesBetweenWeights() {
        int[] w = {1, 1};
        assertEquals(0, RoutePick.pick(w, fixed(0.0)));
        assertEquals(0, RoutePick.pick(w, fixed(0.499)));
        assertEquals(1, RoutePick.pick(w, fixed(0.5)));
        assertEquals(1, RoutePick.pick(w, fixed(0.9999)));
        int[] u = {1, 0, 3};
        assertEquals(0, RoutePick.pick(u, fixed(0.24)));
        assertEquals(2, RoutePick.pick(u, fixed(0.25)));
        assertEquals(2, RoutePick.pick(u, fixed(0.99)));
    }

    @Test
    void aDiceThatReturnsExactlyOneStaysInRange() {
        assertEquals(2, RoutePick.pick(new int[] {1, 0, 3}, fixed(1.0)));
        assertEquals(1, RoutePick.pick(new int[] {0, 5, 0}, fixed(1.0)));
    }

    @Test
    void zeroWeightsNeverComeUpAndTheRestFollowTheirProportions() {
        Random random = new Random(1_62_3L);
        Dice dice = random::nextDouble;
        int[] weights = {0, 1, 0, 3, -5, 2};
        int n = 300_000;
        int[] count = new int[weights.length];
        for (int i = 0; i < n; i++) count[RoutePick.pick(weights, dice)]++;
        assertEquals(0, count[0]);
        assertEquals(0, count[2]);
        assertEquals(0, count[4]);
        double[] p = {0, 1.0 / 6, 0, 3.0 / 6, 0, 2.0 / 6};
        for (int i : new int[] {1, 3, 5}) {
            double sigma = Math.sqrt(n * p[i] * (1 - p[i]));
            assertTrue(Math.abs(count[i] - n * p[i]) < 5 * sigma, "index " + i + ": " + count[i] + " of " + n);
        }
    }
}
