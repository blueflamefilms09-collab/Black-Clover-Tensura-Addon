package com.newuniverse.nusmp.arc.goku;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** 4.103.1: the gentle EP ladder (1 billion to 10 billion, never above the ceiling) that replaces 1 trillion to 500 trillion by default. */
class GokuEpTameTest {
    private static final double CEIL = 1.0e11, CLIMB = 10.0;

    @Test void newDefaultsAreTheGentleLadder() {
        assertEquals(1.0e9, GokuBossConfig.DEFAULT_EP_START, 0.0);
        assertEquals(1.0e10, GokuBossConfig.DEFAULT_EP_END, 0.0);
        assertEquals(1.0e9, GokuEp.tamed(0, 9, 1e9, 1e10, 0, CEIL, CLIMB), 1.0);
        assertEquals(1.0e10, GokuEp.tamed(9, 9, 1e9, 1e10, 0, CEIL, CLIMB), 10.0);
    }

    @Test void anOldConfigOfOneTrillionToFiveHundredTrillionIsPulledDown() {
        double base = GokuEp.tamed(0, 9, 1e12, 5e14, 0, CEIL, CLIMB), last = GokuEp.tamed(9, 9, 1e12, 5e14, 0, CEIL, CLIMB);
        assertEquals(CEIL, last, CEIL * 1e-9);                       // the last form sits on the ceiling
        assertEquals(CLIMB, last / base, 1e-6);                      // and the climb is 10x, not 500x
    }

    @Test void strongPlayersRaiseBaseButNeverPastTheCeiling() {
        assertEquals(5.0e9, GokuEp.tamed(0, 9, 1e9, 1e10, 5.0e9, CEIL, CLIMB), 1.0);
        double huge = GokuEp.tamed(9, 9, 1e9, 1e10, 1.0e17, CEIL, CLIMB);
        assertEquals(CEIL, huge, CEIL * 1e-9);
        for (int i = 0; i <= 10; i++) assertTrue(GokuEp.tamed(i, 10, 1e12, 5e14, 1.0e17, CEIL, CLIMB) <= CEIL);
    }

    @Test void everyStepRisesAndBadNumbersFallBack() {
        double prev = 0;
        for (int i = 0; i <= 9; i++) { double v = GokuEp.tamed(i, 9, 1e9, 1e10, 0, CEIL, CLIMB); assertTrue(v > prev); prev = v; }
        assertEquals(1.0e9, GokuEp.tamed(0, 9, Double.NaN, -1, Double.NaN, Double.NaN, Double.NaN), 1.0);
        assertEquals(1.0e10, GokuEp.tamed(9, 9, Double.NaN, Double.NaN, 0, 0, 0), 10.0);
    }

    @Test void theCeilingCapsTheOneHundredthFloorToo() {
        assertEquals(CEIL, GokuEp.withPlayersCapped(1e9, 1.0e17, 0.01, CEIL), 1.0);
        assertEquals(2.0e9, GokuEp.withPlayersCapped(1e9, 2.0e11, 0.01, CEIL), 1.0);
        assertEquals(1.0e9, GokuEp.withPlayersCapped(1e9, 2.0e11, 0.0, CEIL), 0.0);
    }

    @Test void theClassicLadderIsStillThere() {
        assertEquals(1.0e12, GokuEp.ep(0, 9, 1e12, 5e14), 1e3);
        assertEquals(5.0e14, GokuEp.ep(9, 9, 1e12, 5e14), 1e5);
    }
}
