package com.newuniverse.nusmp.labyrinth.trial;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The numbers of a trial chamber: its box, its party scaling, its barrier columns and the doorway rule. */
class TrialMathTest {
    @Test
    void theBoxReachesRadiusToEachSideHeightDownAndTwoUp() {
        TrialMath.Box b = TrialMath.box(100, 64, -20, 8, 8);
        assertEquals(new TrialMath.Box(92, 56, -28, 108, 66, -12), b);
        assertTrue(b.contains(92, 56, -28));
        assertTrue(b.contains(108, 66, -12));
        assertFalse(b.contains(91, 60, -20));
        assertFalse(b.contains(100, 67, -20));
        assertFalse(b.contains(100, 55, -20));
        assertEquals(17 * 11 * 17, b.volume());
    }

    @Test
    void anEntityIsInsideByTheBlockItsFeetAreIn() {
        TrialMath.Box b = TrialMath.box(0, 10, 0, 2, 3);
        assertTrue(b.contains(2.9, 12.0, -2.0));
        assertFalse(b.contains(3.0, 12.0, 0.0));
        assertTrue(b.contains(-1.9, 7.0, 0.5));      // feet at y=7 = minY
        assertFalse(b.contains(-2.5, 7.0, 0.5), "floor(-2.5) = -3 is outside");
        assertFalse(b.contains(-2.5, 6.99, 0.5));    // floor(6.99) = 6 < minY
        assertTrue(b.contains(0.5, 12.5, 0.5));      // y=12 = cy + ABOVE
        assertFalse(b.contains(0.5, 13.0, 0.5));
    }

    @Test
    void templateValuesAreClamped() {
        assertEquals(TrialMath.MIN_RADIUS, TrialMath.clampRadius(0));
        assertEquals(TrialMath.MIN_RADIUS, TrialMath.clampRadius(-5));
        assertEquals(TrialMath.MAX_RADIUS, TrialMath.clampRadius(200));
        assertEquals(9, TrialMath.clampRadius(9));
        assertEquals(TrialMath.MIN_HEIGHT, TrialMath.clampHeight(0));
        assertEquals(TrialMath.MAX_HEIGHT, TrialMath.clampHeight(1000));
        assertEquals(TrialMath.MIN_RESET_SECONDS * 20, TrialMath.resetTicks(1));
        assertEquals(60 * 20, TrialMath.resetTicks(60));
        assertEquals(TrialMath.MAX_RESET_SECONDS * 20, TrialMath.resetTicks(99999));
        TrialMath.Box b = TrialMath.box(0, 0, 0, 0, 0);
        assertEquals(new TrialMath.Box(-1, -1, -1, 1, 2, 1), b, "a zero radius or height still watches something");
    }

    @Test
    void mobCountGrowsByHalfTheBasePerExtraPlayerAndIsCapped() {
        assertEquals(3, TrialMath.scaledCount(3, 1));
        assertEquals(3, TrialMath.scaledCount(3, 0), "no player counts as one");
        assertEquals(3, TrialMath.scaledCount(3, -4));
        assertEquals(4, TrialMath.scaledCount(3, 2));    // 3 + floor(1.5)
        assertEquals(6, TrialMath.scaledCount(3, 3));    // 3 + 3
        assertEquals(7, TrialMath.scaledCount(3, 4));    // 3 + floor(4.5)
        assertEquals(9, TrialMath.scaledCount(3, 5));    // 3 + 6
        assertEquals(12, TrialMath.scaledCount(3, 8));   // 3 + floor(10.5) = 13 -> cap
        assertEquals(12, TrialMath.scaledCount(3, 100));
        assertEquals(1, TrialMath.scaledCount(1, 1));
        assertEquals(1, TrialMath.scaledCount(1, 2), "a single boss stays single for two players");
        assertEquals(2, TrialMath.scaledCount(1, 3));
        assertEquals(12, TrialMath.scaledCount(12, 1));
        assertEquals(12, TrialMath.scaledCount(40, 1), "a base over the cap is capped even for one player");
        assertEquals(0, TrialMath.scaledCount(0, 5));
        assertEquals(0, TrialMath.scaledCount(-2, 5));
    }

    @Test
    void aBarrierColumnFillsTheAirOverARuneUpToFour() {
        assertEquals(0, TrialMath.columnHeight(0));
        assertEquals(0, TrialMath.columnHeight(-1));
        assertEquals(1, TrialMath.columnHeight(1));
        assertEquals(3, TrialMath.columnHeight(3));
        assertEquals(TrialMath.MAX_COLUMN, TrialMath.columnHeight(4));
        assertEquals(TrialMath.MAX_COLUMN, TrialMath.columnHeight(9));
    }

    @Test
    void aCellIsInTheColumnOnlyStraightOverTheRune() {
        assertTrue(TrialMath.inColumn(5, 10, 5, 3, 5, 11, 5));
        assertTrue(TrialMath.inColumn(5, 10, 5, 3, 5, 13, 5));
        assertFalse(TrialMath.inColumn(5, 10, 5, 3, 5, 14, 5));
        assertFalse(TrialMath.inColumn(5, 10, 5, 3, 5, 10, 5), "the rune cell itself is not a barrier cell");
        assertFalse(TrialMath.inColumn(5, 10, 5, 3, 6, 11, 5));
        assertFalse(TrialMath.inColumn(5, 10, 5, 0, 5, 11, 5), "a column of height 0 holds nothing");
    }

    @Test
    void aPlayersHitboxTouchingAColumnCellBlocksActivation() {
        // the rune at (5, 10, 5), a column 2 high: x in [5,6), y in [11,13), z in [5,6)
        assertTrue(TrialMath.overlapsColumn(5, 10, 5, 2, 5.2, 11.0, 5.2, 5.8, 12.8, 5.8), "standing in the doorway");
        assertTrue(TrialMath.overlapsColumn(5, 10, 5, 2, 4.7, 11.0, 5.2, 5.3, 12.8, 5.8), "straddling the doorway's edge");
        assertFalse(TrialMath.overlapsColumn(5, 10, 5, 2, 6.0, 11.0, 5.2, 6.6, 12.8, 5.8), "touching the face exactly is outside");
        assertFalse(TrialMath.overlapsColumn(5, 10, 5, 2, 5.2, 13.0, 5.2, 5.8, 14.8, 5.8), "standing on top of the column");
        assertFalse(TrialMath.overlapsColumn(5, 10, 5, 2, 5.2, 9.0, 5.2, 5.8, 10.9, 5.8), "below the rune");
        assertFalse(TrialMath.overlapsColumn(5, 10, 5, 0, 5.2, 11.0, 5.2, 5.8, 12.8, 5.8), "no column, no doorway");
    }

    @Test
    void abandonedAfterTheResetTime() {
        assertFalse(TrialMath.abandoned(1000, 500, 1200));
        assertFalse(TrialMath.abandoned(1699, 500, 1200));
        assertTrue(TrialMath.abandoned(1700, 500, 1200));
        assertTrue(TrialMath.abandoned(5000, 500, 1200));
    }

    @Test
    void eachArchwayTicksOnceEveryTenTicksOnItsOwnPhase() {
        for (long seed : new long[]{0L, 1L, 7L, 8L, -1L, 123456789L}) {
            int hits = 0;
            for (long t = 0; t < 100; t++) if (TrialMath.dueThisTick(t, seed)) hits++;
            assertEquals(10, hits, "seed " + seed);
        }
        assertTrue(TrialMath.dueThisTick(0, 0));
        assertFalse(TrialMath.dueThisTick(1, 0));
        assertTrue(TrialMath.dueThisTick(3, 7), "phase 7: 3 + 7 = 10");
        assertTrue(TrialMath.dueThisTick(-10, 0), "negative game time never throws");
    }

    @Test
    void theFallbackRingIsEightSpotsTwoBlocksOut() {
        assertEquals(8, TrialMath.FALLBACK_OFFSETS.length);
        for (int[] o : TrialMath.FALLBACK_OFFSETS) assertEquals(2, Math.max(Math.abs(o[0]), Math.abs(o[1])));
    }
}
