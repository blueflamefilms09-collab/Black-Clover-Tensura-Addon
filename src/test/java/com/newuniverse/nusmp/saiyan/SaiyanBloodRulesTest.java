package com.newuniverse.nusmp.saiyan;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The numbers of the Saiyan-blooded evolution (docs/saiyan_prodigy.md): weights, progress, the deeds and their bounds. Pure. */
class SaiyanBloodRulesTest {
    @Test
    void theWeightsAddUpToOneHundredWhateverIsSwitchedOff() {
        for (double ep : new double[] {0, 20000}) for (int k : new int[] {0, 25}) for (int n : new int[] {0, 3}) {
            double[] w = SaiyanBloodRules.weights(ep, k, n);
            assertEquals(100.0, w[0] + w[1] + w[2], 1e-9, ep + "/" + k + "/" + n);
        }
        double[] all = SaiyanBloodRules.weights(20000, 25, 3);
        assertEquals(40.0, all[0], 1e-9);
        assertEquals(40.0, all[1], 1e-9);
        assertEquals(20.0, all[2], 1e-9);
        double[] two = SaiyanBloodRules.weights(20000, 25, 0);
        assertEquals(50.0, two[0], 1e-9);
        assertEquals(50.0, two[1], 1e-9);
    }

    @Test
    void progressGrowsWithEachDeedAndOpensAtOneHundred() {
        assertEquals(0.0, SaiyanBloodRules.progress(0, 20000, 0, 25, 0, 3), 1e-9);
        assertEquals(40.0, SaiyanBloodRules.progress(20000, 20000, 0, 25, 0, 3), 1e-9);
        assertEquals(40.0, SaiyanBloodRules.progress(0, 20000, 25, 25, 0, 3), 1e-9);
        assertEquals(20.0, SaiyanBloodRules.progress(0, 20000, 0, 25, 3, 3), 1e-9);
        assertTrue(SaiyanBloodRules.ready(SaiyanBloodRules.progress(20000, 20000, 25, 25, 3, 3)));
        assertFalse(SaiyanBloodRules.ready(SaiyanBloodRules.progress(19999, 20000, 25, 25, 3, 3)));
        assertEquals(100.0, SaiyanBloodRules.progress(1e12, 20000, 99999, 25, 999, 3), 1e-9, "a deed over its need adds nothing past its share");
        assertTrue(SaiyanBloodRules.ready(SaiyanBloodRules.progress(0, 0, 0, 0, 0, 0)), "everything switched off: nothing stands in the way");
    }

    @Test
    void sharesAreClamped() {
        assertEquals(0.0, SaiyanBloodRules.share(-5, 10));
        assertEquals(1.0, SaiyanBloodRules.share(50, 10));
        assertEquals(1.0, SaiyanBloodRules.share(0, 0));
        assertEquals(0.5, SaiyanBloodRules.share(5, 10));
        assertEquals(0.0, SaiyanBloodRules.share(Double.NaN, 10));
    }

    @Test
    void aWorthyKillIsAHostileCreatureThatCouldHurt() {
        assertTrue(SaiyanBloodRules.worthyKill(true, 20.0, false));
        assertTrue(SaiyanBloodRules.worthyKill(true, 10.0, false));
        assertFalse(SaiyanBloodRules.worthyKill(true, 9.9, false), "a weak creature");
        assertFalse(SaiyanBloodRules.worthyKill(false, 300.0, false), "a farm animal");
        assertFalse(SaiyanBloodRules.worthyKill(true, 300.0, true), "a player or a pet");
    }

    @Test
    void theEdgeOfDeathCountsOncePerTenMinutes() {
        assertTrue(SaiyanBloodRules.nearDeath(4.0, 20.0));
        assertTrue(SaiyanBloodRules.nearDeath(0.5, 20.0));
        assertFalse(SaiyanBloodRules.nearDeath(4.1, 20.0));
        assertFalse(SaiyanBloodRules.nearDeath(0.0, 20.0), "dead is not survived");
        assertFalse(SaiyanBloodRules.nearDeath(5.0, 0.0));
        assertTrue(SaiyanBloodRules.nearGapOver(1000, -1));
        assertFalse(SaiyanBloodRules.nearGapOver(1000 + SaiyanBloodRules.NEAR_GAP_TICKS - 1, 1000));
        assertTrue(SaiyanBloodRules.nearGapOver(1000 + SaiyanBloodRules.NEAR_GAP_TICKS, 1000));
        assertTrue(SaiyanBloodRules.nearGapOver(5, 9999), "a clock that went back (a restored world) does not block it for good");
    }

    @Test
    void aCounterNeverGrowsPastItsCap() {
        assertEquals(1, SaiyanBloodRules.bump(0));
        assertEquals(1, SaiyanBloodRules.bump(-7));
        assertEquals(SaiyanBloodRules.COUNT_CAP, SaiyanBloodRules.bump(SaiyanBloodRules.COUNT_CAP));
    }
}
