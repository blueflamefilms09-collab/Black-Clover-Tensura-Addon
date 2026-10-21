package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the ledger is the director's whole memory of a player, and its arithmetic is what makes the ambush FAIR: heat that rises per signal and cools with time, one
 * signal of a kind per second (a long run is a signal a second, not twenty), a cap so an hour of running does not store a day of ambushes, a cooldown after an ambush, a
 * budget of so many per window, the earlier strikes that raise the next squad's band, and a half-kept heat when no place could be found so the next suspicious act tries
 * again soon. Time is a parameter, so every test is exact.
 */
class AmbushLedgerTest {
    private static final AmbushRules.Config C = AmbushRules.Config.DEFAULT;

    @Test
    void aSignalOfAKindIsAcceptedOncePerSecond() {
        AmbushLedger l = new AmbushLedger();
        assertEquals(4.0, l.add(AmbushRules.Signal.TELEPORT, 0, 100, C));
        assertEquals(0.0, l.add(AmbushRules.Signal.TELEPORT, 0, 110, C), "ten ticks later: the same signal is not counted twice");
        assertEquals(4.0, l.add(AmbushRules.Signal.TELEPORT, 0, 120, C));
        assertEquals(5.0, l.add(AmbushRules.Signal.ESCAPED_LOCKDOWN, 0, 110, C), "another kind is counted at once");
        assertEquals(AmbushRules.Signal.ESCAPED_LOCKDOWN, l.lastSignal());
    }

    @Test
    void heatIsCappedAtTwiceTheThreshold() {
        AmbushLedger l = new AmbushLedger();
        for (int i = 0; i < 200; i++) l.add(AmbushRules.Signal.TELEPORT, 0, 20L * i, C);
        assertTrue(l.heat(20L * 200, C) <= 2.0 * C.threshold() + 1e-9);
    }

    @Test
    void anAmbushSpendsTheHeatAndStartsTheCooldown() {
        AmbushLedger l = new AmbushLedger();
        l.add(AmbushRules.Signal.TELEPORT, 0, 0, C);
        l.add(AmbushRules.Signal.ESCAPED_LOCKDOWN, 0, 0, C);
        assertTrue(l.heat(0, C) >= C.threshold());
        l.fired(100, C);
        assertEquals(0.0, l.heat(100, C));
        assertEquals(C.cooldownTicks(), l.cooldownLeft(100));
        assertEquals(C.cooldownTicks() - 50, l.cooldownLeft(150));
        assertEquals(0L, l.cooldownLeft(100 + C.cooldownTicks() + 1));
    }

    @Test
    void theBudgetCountsInsideItsWindowOnly() {
        AmbushLedger l = new AmbushLedger();
        for (int i = 0; i < 3; i++) l.fired(1000L * i, C);
        assertEquals(3, l.firedInWindow(3000, C));
        assertEquals(2, l.firedInWindow(C.budgetWindowTicks() + 500, C), "the first one fell out of the window");
        assertEquals(0, l.firedInWindow(C.budgetWindowTicks() * 3, C));
    }

    @Test
    void earlierStrikesRaiseTheNextBand() {
        AmbushLedger l = new AmbushLedger();
        assertEquals(0, l.strikes(0, C));
        l.fired(0, C);
        l.fired(2000, C);
        assertEquals(2, l.strikes(2500, C));
        assertEquals(1, l.strikes(C.strikeWindowTicks() + 100, C), "an old strike is forgiven");
        assertEquals(0, l.strikes(C.strikeWindowTicks() * 4, C));
    }

    @Test
    void whenNoPlaceIsFoundHalfTheThresholdIsKept() {
        AmbushLedger l = new AmbushLedger();
        l.add(AmbushRules.Signal.TELEPORT, 0, 0, C);
        l.add(AmbushRules.Signal.ESCAPED_LOCKDOWN, 0, 0, C);
        l.noPlace(0, C);
        assertEquals(C.threshold() * 0.5, l.heat(0, C), 1e-9);
        assertEquals(0L, l.cooldownLeft(0), "a failed attempt costs no cooldown");
        assertEquals(0, l.firedInWindow(0, C), "and no budget");
        AmbushLedger cold = new AmbushLedger();
        cold.add(AmbushRules.Signal.FAST_TRAVEL, 0, 0, C);
        cold.noPlace(0, C);
        assertEquals(1.5, cold.heat(0, C), 1e-9, "a cold ledger is not warmed by a failed attempt");
    }

    @Test
    void theHistoryIsBounded() {
        AmbushLedger l = new AmbushLedger();
        for (int i = 0; i < 500; i++) l.fired(i, C);
        assertTrue(l.strikes(500, C) <= 64);
    }
}
