package com.newuniverse.nusmp.balance;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LateGameScalingTest {
    private static double f(double mm) { return LateGameScaling.factor(true, mm, LateGameScaling.DEFAULT_BASELINE, LateGameScaling.DEFAULT_CAP); }

    @Test
    void offIsTheOldDamage() {
        assertEquals(1.0, LateGameScaling.factor(false, 1e9, 10_000, 6), 0.0);
        assertEquals(1.0, LateGameScaling.factor(true, 1e9, 10_000, 1), 0.0, "a cap of 1 is off too");
    }

    @Test
    void theBaselineAndBelowAreX1() {
        assertEquals(1.0, f(10_000), 1e-12);
        assertEquals(1.0, f(500), 0.0);
        assertEquals(1.0, f(0), 0.0);
    }

    @Test
    void theCurveRisesSmoothlyAndStopsAtTheCap() {
        assertEquals(2.25, f(100_000), 1e-9);
        assertEquals(3.5, f(1_000_000), 1e-9);
        assertEquals(6.0, f(1e8), 1e-9);
        assertEquals(6.0, f(1e15), 0.0);
        double last = 1.0;
        for (double mm = 1; mm < 1e12; mm *= 1.7) {
            double v = f(mm);
            assertTrue(v >= last && v <= 6.0, "monotonic and capped at " + mm);
            last = v;
        }
    }

    @Test
    void badInputsAreSafe() {
        assertEquals(1.0, f(Double.NaN), 0.0);
        assertEquals(1.0, f(-5e6), 0.0);
        assertEquals(6.0, f(Double.POSITIVE_INFINITY), 0.0);
        assertEquals(1.0, LateGameScaling.factor(true, 1e9, Double.NaN, 6), 0.0);
        assertEquals(1.0, LateGameScaling.factor(true, 1e9, -1, 6), 0.0);
        assertEquals(1.0, LateGameScaling.factor(true, 1e9, 10_000, Double.NaN), 0.0);
    }
}
