package com.newuniverse.nusmp.perf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: a budget decides how much of a tick's work is thinned out, so its arithmetic is pinned: grants within a tick add up to the cap and no further, the allowance refills on a new tick, a
 * cap of 0 means unlimited, an all-or-nothing request never half-spends, the statistics add up, and the shared server budgets follow {@link PerfSettings} as it changes.
 */
class PerfBudgetTest {
    @Test
    void grantsWithinATickAddUpToTheCapAndNoFurther() {
        PerfBudget b = new PerfBudget("t", 10);
        assertEquals(6, b.take(1, 6));
        assertEquals(4, b.take(1, 6));
        assertEquals(0, b.take(1, 1));
        assertEquals(10, b.used());
        assertEquals(0, b.remaining(1));
    }

    @Test
    void theAllowanceRefillsOnANewTick() {
        PerfBudget b = new PerfBudget("t", 10);
        b.take(1, 10);
        assertEquals(10, b.take(2, 10));
        assertEquals(10, b.remaining(3));
    }

    @Test
    void aCapOfZeroOrLessIsUnlimited() {
        PerfBudget b = new PerfBudget("t", 0);
        assertEquals(100000, b.take(1, 100000));
        assertEquals(Integer.MAX_VALUE, b.remaining(1));
        b.setCap(-5);
        assertEquals(7, b.take(1, 7));
        assertTrue(b.tryTake(1, 1_000_000));
    }

    @Test
    void anAllOrNothingRequestNeverHalfSpends() {
        PerfBudget b = new PerfBudget("t", 10);
        assertTrue(b.tryTake(1, 4));
        assertFalse(b.tryTake(1, 7));
        assertEquals(4, b.used());
        assertTrue(b.tryTake(1, 6));
        assertFalse(b.tryTake(1, 1));
        assertTrue(b.tryTake(1, 0));
    }

    @Test
    void zeroAndNegativeRequestsCostNothing() {
        PerfBudget b = new PerfBudget("t", 10);
        assertEquals(0, b.take(1, 0));
        assertEquals(0, b.take(1, -4));
        assertEquals(0, b.used());
        assertEquals(0L, b.asked());
    }

    @Test
    void theStatisticsAddUp() {
        PerfBudget b = new PerfBudget("t", 10);
        b.take(1, 8);
        b.take(1, 8);                                // 2 granted of 8
        b.take(2, 3);
        assertEquals(19L, b.asked());
        assertEquals(13L, b.granted());
        assertEquals(6L, b.denied());
        assertEquals(10L, b.peak());
        b.resetStats();
        assertEquals(0L, b.asked());
        assertEquals(0L, b.peak());
    }

    @Test
    void theCapCanChangeBetweenCalls() {
        PerfBudget b = new PerfBudget("t", 10);
        assertEquals(10, b.take(1, 50));
        b.setCap(20);
        assertEquals(10, b.take(1, 50));             // the same tick: 10 already spent, 10 of the new 20 left
        assertEquals(20, b.take(2, 50));
    }

    @Test
    void theSharedBudgetsFollowTheSettings() {
        PerfSettings.resetDefaults();
        try {
            PerfSettings.setMaxParticlesPerTick(100);
            assertEquals(100, PerfBudgets.particles(5000, 1000));
            assertEquals(0, PerfBudgets.particles(5000, 10));
            assertEquals(60, PerfBudgets.particles(5001, 60));
            PerfSettings.setMaxParticlesPerTick(0);                 // unlimited
            assertEquals(5000, PerfBudgets.particles(5001, 5000));
            PerfSettings.setMaxEffectPacketsPerTick(3);
            assertTrue(PerfBudgets.effectPacket(9000));
            assertTrue(PerfBudgets.effectPacket(9000));
            assertTrue(PerfBudgets.effectPacket(9000));
            assertFalse(PerfBudgets.effectPacket(9000));
            assertTrue(PerfBudgets.effectPacket(9001));
            PerfSettings.setClientParticlesPerTick(5);
            assertEquals(5, PerfBudgets.clientParticles(1, 50));
            PerfBudgets.resetStats();
            assertEquals(0L, PerfBudgets.PARTICLES.asked());
        } finally {
            PerfSettings.resetDefaults();
        }
    }
}
