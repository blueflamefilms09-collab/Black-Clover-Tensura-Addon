package com.newuniverse.nusmp.arc.goku;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class GokuAdaptMathTest {
    @Test void stagesFollowMahoragaThresholds() {
        assertEquals(0, GokuAdaptMath.stage(0));
        assertEquals(1, GokuAdaptMath.stage(1));
        assertEquals(3, GokuAdaptMath.stage(3));
        assertEquals(5, GokuAdaptMath.stage(5));
        assertEquals(5, GokuAdaptMath.stage(500));
    }

    @Test void reductionGrowsSlowlyAndStopsAtTheCap() {
        assertEquals(0.0, GokuAdaptMath.reduction(0, 1, 3, 25), 1e-9);
        assertEquals(0.01, GokuAdaptMath.reduction(1, 1, 3, 25), 1e-9);     // 1% from the first hit
        assertEquals(0.01, GokuAdaptMath.reduction(3, 1, 3, 25), 1e-9);
        assertEquals(0.02, GokuAdaptMath.reduction(4, 1, 3, 25), 1e-9);     // +1% per 3 further hits
        assertEquals(0.25, GokuAdaptMath.reduction(1000, 1, 3, 25), 1e-9);  // unmastered cap
        assertEquals(0.50, GokuAdaptMath.reduction(1000, 1, 3, 50), 1e-9);  // mastered cap
    }

    @Test void neverImmuneWhateverTheSettings() {
        for (int hits : new int[] {1, 50, 100000}) {
            double m = GokuAdaptMath.multiplier(hits, 99, 1, 99);
            assertTrue(m >= 1.0 - GokuAdaptMath.CEILING - 1e-9, "hits " + hits + " gave multiplier " + m);
            assertTrue(m > 0.0);
        }
        assertEquals(1.0, GokuAdaptMath.multiplier(0, 99, 1, 99), 1e-9);
    }

    @Test void saneOnBadInput() {
        assertEquals(0.0, GokuAdaptMath.reduction(-5, 1, 3, 25), 1e-9);
        assertEquals(0.01, GokuAdaptMath.reduction(1, 1, 0, 25), 1e-9);     // hitsPerPct 0 does not divide by zero
    }

    @Test void oldGreatSageCurveIsStillThere() {
        assertEquals(0.75, GokuOmegaMath.HARD_ADAPT_CAP, 1e-9);             // kept behind mahoragaAdapt=false
    }
}
