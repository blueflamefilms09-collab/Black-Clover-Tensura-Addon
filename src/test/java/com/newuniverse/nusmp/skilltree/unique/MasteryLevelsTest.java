package com.newuniverse.nusmp.skilltree.unique;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: with no Skill Points, the level every node counts at and the awakening of the later arts both come from the skill's mastery, and a wrong formula is silent (a passive at level 0 does nothing, an art
 * index out of range throws in the middle of a key press). So the formulas are pinned at their edges: the level is never below 1 or above the node's maximum for any input including NaN and values outside 0..1,
 * the signature art is awake from the start and the last art needs less than full mastery, the thresholds never decrease, and a press of the key wraps into the arts.
 */
class MasteryLevelsTest {
    @Test
    void theFractionIsClampedAndNeverNaN() {
        assertEquals(0.0, MasteryLevels.fraction(0, 100));
        assertEquals(0.5, MasteryLevels.fraction(50, 100));
        assertEquals(1.0, MasteryLevels.fraction(500, 100));
        assertEquals(0.0, MasteryLevels.fraction(-5, 100));
        assertEquals(0.0, MasteryLevels.fraction(5, 0), "no maximum means no mastery");
        assertEquals(0.0, MasteryLevels.fraction(Double.NaN, 100));
        assertEquals(0.0, MasteryLevels.fraction(5, Double.POSITIVE_INFINITY));
    }

    @Test
    void theLevelStartsAtOneAndReachesTheMaximumOnlyWhenMastered() {
        assertEquals(1, MasteryLevels.level(0.0, 5));
        assertEquals(5, MasteryLevels.level(1.0, 5));
        assertEquals(1, MasteryLevels.level(0.19, 5));
        assertEquals(2, MasteryLevels.level(0.2, 5));
        assertEquals(3, MasteryLevels.level(0.5, 5));
        assertEquals(4, MasteryLevels.level(0.79, 5));
        assertEquals(1, MasteryLevels.level(0.99, 1), "a node of one level is always level 1");
    }

    @Test
    void theLevelStaysInRangeForAnyInput() {
        for (int max = -2; max <= 12; max++) {
            for (double f : new double[] {-1, 0, 0.3, 0.999999, 1, 7, Double.NaN, Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY}) {
                int lv = MasteryLevels.level(f, max);
                assertTrue(lv >= 1 && lv <= Math.max(1, max), "level " + lv + " for fraction " + f + " and maximum " + max);
            }
        }
    }

    @Test
    void theLevelNeverFallsAsMasteryRises() {
        for (int max = 1; max <= 10; max++) {
            int last = 0;
            for (int i = 0; i <= 200; i++) {
                int lv = MasteryLevels.level(i / 200.0, max);
                assertTrue(lv >= last, "max " + max + " at " + i);
                last = lv;
            }
        }
    }

    @Test
    void theSignatureArtIsAwakeFromTheStartAndTheLastNeedsLessThanFullMastery() {
        for (int n = 1; n <= 14; n++) {
            assertTrue(MasteryLevels.awake(0, n, 0.0), "the first art of " + n + " is awake at no mastery");
            assertEquals(0, MasteryLevels.percentNeeded(0, n));
            if (n > 1) assertTrue(MasteryLevels.threshold(n - 1, n) < 1.0, "the last art of " + n + " needs less than full mastery");
            assertTrue(MasteryLevels.awake(n - 1, n, 1.0), "everything is awake when mastered");
        }
    }

    @Test
    void thresholdsRiseWithTheIndex() {
        for (int n = 2; n <= 14; n++) {
            double last = -1;
            for (int k = 0; k < n; k++) {
                double t = MasteryLevels.threshold(k, n);
                assertTrue(t >= last, n + " arts, art " + k);
                last = t;
            }
        }
        assertEquals(50, MasteryLevels.percentNeeded(5, 10));
        assertFalse(MasteryLevels.awake(5, 10, 0.49));
        assertTrue(MasteryLevels.awake(5, 10, 0.5));
    }

    @Test
    void anArtOutOfRangeIsNeverAwake() {
        assertFalse(MasteryLevels.awake(-1, 3, 1.0));
        assertFalse(MasteryLevels.awake(3, 3, 1.0));
        assertTrue(MasteryLevels.awake(0, 3, Double.NaN), "the signature art needs no mastery even when the number is broken");
    }

    @Test
    void aPressWrapsIntoTheArts() {
        assertEquals(-1, MasteryLevels.wrapMode(0, 0));
        assertEquals(0, MasteryLevels.wrapMode(5, 1));
        assertEquals(2, MasteryLevels.wrapMode(-1, 3));
        assertEquals(1, MasteryLevels.wrapMode(4, 3));
        assertEquals(0, MasteryLevels.wrapMode(-9, 3));
    }
}
