package com.newuniverse.nusmp.arc.goku;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** 4.102.0: Goku's EP ladder (1 trillion in Base, 500 trillion in the last form) and the one-hundredth-of-the-player floor. */
class GokuEpTest {
    private static final double S = 1.0e12, E = 5.0e14;

    @Test void baseAndLastAreExact() {
        assertEquals(S, GokuEp.ep(0, 9, S, E), S * 1e-9);
        assertEquals(E, GokuEp.ep(9, 9, S, E), E * 1e-9);
        assertEquals(E, GokuEp.ep(10, 10, S, E), E * 1e-9);
    }

    @Test void everyStepMultipliesByTheSameFactorAndNeverFalls() {
        double prev = GokuEp.ep(0, 9, S, E), f = Math.pow(E / S, 1.0 / 9);
        for (int i = 1; i <= 9; i++) {
            double v = GokuEp.ep(i, 9, S, E);
            assertTrue(v > prev);
            assertEquals(f, v / prev, 1e-9);
            prev = v;
        }
    }

    @Test void badNumbersFallBackToTheDefaults() {
        assertEquals(S, GokuEp.ep(0, 9, Double.NaN, Double.NaN), S * 1e-9);
        assertEquals(E, GokuEp.ep(99, 9, -1, 0), E * 1e-9);
        assertEquals(S, GokuEp.ep(-3, 0 + 9, S, E), S * 1e-9);
    }

    @Test void playerShareOnlyRaisesTheLadder() {
        assertEquals(S, GokuEp.withPlayers(S, 5.0e13, 0.01), 1.0);            // 1/100 of 50 T = 0.5 T, below the ladder
        assertEquals(2.0e13, GokuEp.withPlayers(S, 2.0e15, 0.01), 1.0);        // 1/100 of 2 quadrillion = 20 T, above it
        assertEquals(S, GokuEp.withPlayers(S, 2.0e15, 0.0), 0.0);              // off
        assertEquals(S, GokuEp.withPlayers(S, Double.NaN, 0.01), 0.0);
        assertEquals(S, GokuEp.withPlayers(S, -5, 0.01), 0.0);
    }
}
