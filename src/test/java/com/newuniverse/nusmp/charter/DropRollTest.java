package com.newuniverse.nusmp.charter;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

/** The decision table of the first-kill drop. */
class DropRollTest {
    @Test
    void aClaimedGrimoireNeverRollsAgain() {
        AtomicInteger draws = new AtomicInteger();
        for (double chance : new double[]{0.0, 0.25, 1.0}) {
            assertEquals(DropRoll.Result.LOCKED, DropRoll.decide(true, true, true, chance, () -> { draws.incrementAndGet(); return 0.0; }));
        }
        assertEquals(0, draws.get(), "the roll is not even drawn when the lock is set");
    }

    @Test
    void noEligibleKillerMeansNoRollAndNoLock() {
        AtomicInteger draws = new AtomicInteger();
        assertEquals(DropRoll.Result.NO_KILLER, DropRoll.decide(true, false, false, 1.0, () -> { draws.incrementAndGet(); return 0.0; }));
        assertEquals(0, draws.get());
    }

    @Test
    void theRollDecidesAgainstTheChance() {
        assertEquals(DropRoll.Result.DROP, DropRoll.decide(true, false, true, 0.25, () -> 0.0));
        assertEquals(DropRoll.Result.DROP, DropRoll.decide(true, false, true, 0.25, () -> 0.2499));
        assertEquals(DropRoll.Result.MISS, DropRoll.decide(true, false, true, 0.25, () -> 0.25));
        assertEquals(DropRoll.Result.MISS, DropRoll.decide(true, false, true, 0.25, () -> 0.99));
        assertEquals(DropRoll.Result.DROP, DropRoll.decide(true, false, true, 1.0, () -> 0.999999));
        assertEquals(DropRoll.Result.MISS, DropRoll.decide(true, false, true, 0.0, () -> 0.0));
    }

    @Test
    void switchedOffNothingHappens() {
        assertEquals(DropRoll.Result.DISABLED, DropRoll.decide(false, false, true, 1.0, () -> 0.0));
        assertEquals(DropRoll.Result.DISABLED, DropRoll.decide(false, true, true, 1.0, () -> 0.0));
    }

    @Test
    void theConfigMultiplierIsClamped() {
        assertEquals(0.25, DropRoll.chance(0.25, 1.0), 1e-9);
        assertEquals(0.5, DropRoll.chance(0.25, 2.0), 1e-9);
        assertEquals(1.0, DropRoll.chance(0.4, 10.0), 1e-9);
        assertEquals(0.0, DropRoll.chance(0.4, 0.0), 1e-9);
        assertEquals(0.0, DropRoll.chance(0.4, -3.0), 1e-9);
        assertEquals(0.0, DropRoll.chance(0.4, Double.NaN), 1e-9);
    }

    @Test
    void theObservedRateMatchesTheChance() {
        java.util.Random r = new java.util.Random(7);
        int hits = 0, n = 100000;
        for (int i = 0; i < n; i++) if (DropRoll.decide(true, false, true, 0.25, r::nextDouble) == DropRoll.Result.DROP) hits++;
        assertEquals(0.25, hits / (double) n, 0.01);
    }
}
