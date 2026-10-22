package com.newuniverse.nusmp.fairytail.storm;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The worst a crowd of storms can cost the server in one tick and in one second (CLAUDE.md rule 11): the budgets hold whatever is asked. */
class StormBudgetTest {
    @Test
    void scansAreCappedPerTickAndRefillTheNextTick() {
        StormBudget.reset();
        int granted = 0;
        for (int i = 0; i < 500; i++) if (StormBudget.scan(1000)) granted++;
        assertEquals(StormBudget.SCANS_PER_TICK, granted, "a thousand storms ask, six scans run");
        assertTrue(StormBudget.scan(1001), "the next tick starts with a fresh allowance");
    }

    @Test
    void hailVolleysAreCappedPerTick() {
        StormBudget.reset();
        int granted = 0;
        for (int i = 0; i < 100; i++) if (StormBudget.hail(50)) granted++;
        assertEquals(StormBudget.HAIL_PER_TICK, granted);
    }

    @Test
    void boltsAreOnePerTickAndFewPerSecondOverTheWholeServer() {
        StormBudget.reset();
        int inSecond = 0;
        for (long t = 2000; t < 2020; t++) {
            int thisTick = 0;
            for (int ask = 0; ask < 50; ask++) if (StormBudget.bolt(t)) { thisTick++; inSecond++; }
            assertTrue(thisTick <= StormBudget.BOLTS_PER_TICK, "tick " + t);
        }
        assertEquals(StormBudget.BOLTS_PER_SECOND, inSecond, "twenty ticks of a crowd of storms: six bolts");
        assertTrue(StormBudget.bolt(2020), "the next second has its own allowance");
    }

    @Test
    void theWorstCaseOfOneTickIsAHandfulOfOperations() {
        assertTrue(StormBudget.worstCaseOperationsPerTick() <= 12, "scans + volleys + bolts per tick: " + StormBudget.worstCaseOperationsPerTick());
        // and the per-creature caps of one storm bound the damage work too: a creature can take at most 3 + 8 + 6 landed hits from one storm
        assertEquals(17, StormMath.MAX_BOLTS_PER_TARGET + StormMath.MAX_HAIL_PER_TARGET + StormMath.MAX_PULSES_PER_TARGET);
        assertTrue(StormMath.MAX_FOES <= 16, "an area query is cut at sixteen creatures");
    }

    @Test
    void thePerPlayerAndGlobalLimitsAreSmall() {
        assertTrue(StormCaps.MAX_PER_PLAYER <= 2 && StormCaps.MAX_GLOBAL <= 64);
    }
}
