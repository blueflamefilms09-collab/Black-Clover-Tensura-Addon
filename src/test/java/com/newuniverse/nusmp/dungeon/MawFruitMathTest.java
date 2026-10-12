package com.newuniverse.nusmp.dungeon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The Maw reward: the sooner the roots are cut, the more fruit and experience. */
class MawFruitMathTest {
    private static final long TOTAL = 20 * 1200L;      // the default 20 minutes

    @Test
    void anInstantCutIsWorthThreeFruit() {
        assertEquals(3, MawFruitMath.count(TOTAL, TOTAL));
        assertEquals(100, MawFruitMath.xp(TOTAL, TOTAL));
    }

    @Test
    void aLastMinuteCutIsWorthOneFruit() {
        assertEquals(1, MawFruitMath.count(0, TOTAL));
        assertEquals(30, MawFruitMath.xp(0, TOTAL));
    }

    @Test
    void theThresholdsFallAtAThirdAndTwoThirds() {
        assertEquals(3, MawFruitMath.count(16000, TOTAL));    // 66.7% left
        assertEquals(2, MawFruitMath.count(15000, TOTAL));    // 62.5%
        assertEquals(2, MawFruitMath.count(8000, TOTAL));     // 33.3%
        assertEquals(1, MawFruitMath.count(7000, TOTAL));     // 29.2%
    }

    @Test
    void theRewardNeverGrowsAsTheTimeRunsOut() {
        int fruit = 3, xp = 100;
        for (long left = TOTAL; left >= 0; left -= 600) {
            assertTrue(MawFruitMath.count(left, TOTAL) <= fruit, "fruit at " + left);
            assertTrue(MawFruitMath.xp(left, TOTAL) <= xp, "xp at " + left);
            fruit = MawFruitMath.count(left, TOTAL);
            xp = MawFruitMath.xp(left, TOTAL);
            assertTrue(fruit >= 1 && fruit <= 3);
            assertTrue(xp >= 30 && xp <= 100);
        }
    }

    @Test
    void oddInputsAreKeptInRange() {
        assertEquals(1.0, MawFruitMath.share(TOTAL * 2, TOTAL));
        assertEquals(0.0, MawFruitMath.share(-50, TOTAL));
        assertEquals(0.0, MawFruitMath.share(100, 0));
        assertEquals(1, MawFruitMath.count(100, 0));
        assertEquals(30, MawFruitMath.xp(100, -1));
        assertEquals(3, MawFruitMath.count(TOTAL * 3, TOTAL));
    }
}
