package com.newuniverse.nusmp.devil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why (5.7.0): "a 2 percent chance a devil asks" must really be 2 percent, rare in time, and only for a free player. */
class DevilCallingRulesTest {
    @Test
    void theRollIsTwoPercent() {
        int wins = 0;
        for (int i = 0; i < 10_000; i++) if (DevilCallingRules.wins(2.0, i / 10_000.0)) wins++;
        assertEquals(200, wins);
        assertFalse(DevilCallingRules.wins(0.0, 0.0));
        assertTrue(DevilCallingRules.wins(100.0, 0.999999));
        assertTrue(DevilCallingRules.wins(Double.NaN, 0.0), "a broken value falls back to the 2 percent default");
    }

    @Test
    void aNewPlayersClockStartsAtTheirFirstSweep() {
        assertFalse(DevilCallingRules.due(0, 1_000_000, 24_000));
        assertFalse(DevilCallingRules.due(1000, 1000 + 23_999, 24_000));
        assertTrue(DevilCallingRules.due(1000, 1000 + 24_000, 24_000));
        assertFalse(DevilCallingRules.due(5000, 4000, 24_000), "a clock that went back never fires");
    }

    @Test
    void onlyAFreePlayerIsAsked() {
        assertTrue(DevilCallingRules.eligible(true, false, false, false, false));
        assertFalse(DevilCallingRules.eligible(false, false, false, false, false));
        assertFalse(DevilCallingRules.eligible(true, true, false, false, false));
        assertFalse(DevilCallingRules.eligible(true, false, true, false, false));
        assertFalse(DevilCallingRules.eligible(true, false, false, true, false));
        assertFalse(DevilCallingRules.eligible(true, false, false, false, true));
    }

    @Test
    void theBoundsHold() {
        assertEquals(24_000L, DevilCallingRules.intervalTicks(20));
        assertEquals(1200L, DevilCallingRules.intervalTicks(-5));
        assertEquals(1440 * 1200L, DevilCallingRules.intervalTicks(Integer.MAX_VALUE));
        assertEquals(100.0, DevilCallingRules.clampPercent(500));
        assertEquals(0.0, DevilCallingRules.clampPercent(-1));
        assertEquals(-1, DevilCallingRules.pick(0, 0.5));
        for (int n = 1; n < 30; n++) for (double r : new double[] {0, 0.5, 0.999999, 1.5, -1}) {
            int k = DevilCallingRules.pick(n, r);
            assertTrue(k >= 0 && k < n);
        }
    }
}
