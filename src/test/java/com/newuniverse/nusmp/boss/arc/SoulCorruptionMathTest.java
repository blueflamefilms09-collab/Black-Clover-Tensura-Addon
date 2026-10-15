package com.newuniverse.nusmp.boss.arc;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Soul Corruption: 10/20/30% of max health, refresh raises the level, never into the four-heart floor. */
class SoulCorruptionMathTest {
    @Test
    void levelsTakeTenTwentyThirtyPercent() {
        assertEquals(2.0, SoulCorruptionMath.reduction(20.0, 0), 1e-9);
        assertEquals(4.0, SoulCorruptionMath.reduction(20.0, 1), 1e-9);
        assertEquals(6.0, SoulCorruptionMath.reduction(20.0, 2), 1e-9);
        assertEquals(60.0, SoulCorruptionMath.reduction(200.0, 2), 1e-9);
    }

    @Test
    void refreshRaisesTheLevelToTwo() {
        assertEquals(0, SoulCorruptionMath.nextAmplifier(-1));
        assertEquals(1, SoulCorruptionMath.nextAmplifier(0));
        assertEquals(2, SoulCorruptionMath.nextAmplifier(1));
        assertEquals(2, SoulCorruptionMath.nextAmplifier(2));
        assertEquals(2, SoulCorruptionMath.nextAmplifier(50));
    }

    @Test
    void neverBelowFourHearts() {
        assertEquals(0.0, SoulCorruptionMath.reduction(8.0, 2), 1e-9);
        assertEquals(2.0, SoulCorruptionMath.reduction(10.0, 2), 1e-9);
        assertEquals(0.0, SoulCorruptionMath.reduction(4.0, 2), 1e-9);
        for (double base = 1; base < 400; base += 3) for (int a = 0; a <= 2; a++) {
            double left = base - SoulCorruptionMath.reduction(base, a);
            assertTrue(left >= Math.min(base, SoulCorruptionMath.FLOOR_HEALTH) - 1e-9, base + " amp " + a);
        }
    }

    @Test
    void junkIsSafe() {
        assertEquals(0.0, SoulCorruptionMath.reduction(0.0, 1));
        assertEquals(0.0, SoulCorruptionMath.reduction(Double.NaN, 1));
        assertEquals(0.3, SoulCorruptionMath.share(99), 1e-9);
        assertEquals(0.1, SoulCorruptionMath.share(-4), 1e-9);
        assertEquals(1200, SoulCorruptionMath.DURATION_TICKS);
    }
}
