package com.newuniverse.nusmp.arc.sds;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** Ban's Snatch: a share of a player's attack for a while, capped on every side, and a way back for the player who fights for it. */
class SnatchLedgerTest {
    private static final UUID A = new UUID(0, 1), B = new UUID(0, 2), C = new UUID(0, 3);

    @Test
    void oneSnatchTakesAShareThatGrowsWithThePowerStage() {
        assertEquals(0.12, SnatchLedger.takeOf(0), 1e-9);
        assertEquals(0.15, SnatchLedger.takeOf(1), 1e-9);
        assertEquals(0.24, SnatchLedger.takeOf(4), 1e-9);
        assertEquals(0.12, SnatchLedger.takeOf(-3), 1e-9);
        assertEquals(0.24, SnatchLedger.takeOf(99), 1e-9);
        assertTrue(SnatchLedger.takeOf(4) < SnatchLedger.TAKE_MAX_PER_VICTIM);
    }

    @Test
    void whatIsTakenIsAppliedBothWaysAndEndsOnItsOwn() {
        SnatchLedger l = new SnatchLedger();
        assertEquals(0.15, l.steal(A, 100, 0.15), 1e-9);
        assertEquals(0.15, l.taken(A, 100), 1e-9);
        assertEquals(0.85, l.damageFactor(A, 100), 1e-9, "the player does 15% less to Ban");
        assertEquals(0.15 * SnatchLedger.BORROW_RATE, l.bonus(100), 1e-9, "and Ban gains 80% of it");
        assertEquals(1.0, l.damageFactor(B, 100), 1e-9, "everyone else is untouched");
        assertTrue(l.taken(A, 100 + SnatchLedger.DURATION - 1) > 0);
        assertEquals(0.0, l.taken(A, 100 + SnatchLedger.DURATION), 1e-9, "it ends after 8 seconds");
        assertEquals(1.0, l.damageFactor(A, 100 + SnatchLedger.DURATION), 1e-9);
        assertEquals(0.0, l.bonus(100 + SnatchLedger.DURATION), 1e-9);
        assertEquals(0, l.victims(100 + SnatchLedger.DURATION));
    }

    @Test
    void aPlayerCanBeSnatchedTwiceAtMostAndNeverBeyondTheCeiling() {
        SnatchLedger l = new SnatchLedger();
        assertEquals(0.15, l.steal(A, 0, 0.15), 1e-9);
        assertEquals(0.15, l.steal(A, 10, 0.15), 1e-9);
        assertEquals(0.30, l.taken(A, 10), 1e-9);
        assertEquals(0.0, l.steal(A, 20, 0.15), 0.0, "a third one fizzles");
        SnatchLedger tight = new SnatchLedger();
        assertEquals(0.24, tight.steal(A, 0, 0.24), 1e-9);
        assertEquals(0.06, tight.steal(A, 5, 0.24), 1e-9, "the ceiling of 0.30 cuts the second");
        assertEquals(0.30, tight.taken(A, 5), 1e-9);
        SnatchLedger low = new SnatchLedger(0.10);
        assertEquals(0.10, low.steal(A, 0, 0.24), 1e-9, "a server can lower the ceiling");
        assertEquals(0.05, new SnatchLedger(-4).steal(A, 0, 0.24), 1e-9, "and not below a floor");
        assertEquals(0.24, new SnatchLedger(9).steal(A, 0, 0.24), 1e-9);
        SnatchLedger high = new SnatchLedger(9.0);
        high.steal(A, 0, 0.30);
        high.steal(A, 1, 0.30);
        assertEquals(0.40, high.taken(A, 2), 1e-9, "the ceiling is capped at 0.60 and Ban's own cap of 0.40 binds first");
    }

    @Test
    void banHoldsAtMostFortyPercentOfEverybodyTogether() {
        SnatchLedger l = new SnatchLedger();
        assertEquals(0.24, l.steal(A, 0, 0.24), 1e-9);
        assertEquals(0.16, l.steal(B, 1, 0.24), 1e-9, "only 0.16 is left of the 0.40 he can hold");
        assertEquals(0.0, l.steal(C, 2, 0.24), 0.0, "nothing is left for the third");
        assertEquals(0.40, l.borrowed(3), 1e-9);
        assertEquals(0.32, l.bonus(3), 1e-9, "at most +32% damage");
        assertEquals(2, l.victims(3));
        assertTrue(l.bonus(3) < 0.35, "the cap on his gain");
        // when the first runs out, the room comes back
        assertEquals(0.24, l.steal(C, SnatchLedger.DURATION + 1, 0.24), 1e-9);
    }

    @Test
    void aRefreshRestartsTheClock() {
        SnatchLedger l = new SnatchLedger();
        l.steal(A, 0, 0.12);
        l.steal(A, 100, 0.12);
        assertTrue(l.taken(A, 100 + SnatchLedger.DURATION - 1) > 0, "the second Snatch runs the full eight seconds");
        assertEquals(0.0, l.taken(A, 100 + SnatchLedger.DURATION), 1e-9);
    }

    @Test
    void threeRealBlowsInTimeWrenchTheStrengthBack() {
        SnatchLedger l = new SnatchLedger();
        l.steal(A, 0, 0.24);
        float max = 1000f;
        assertFalse(l.hitBack(A, 10, 10f, max));
        assertFalse(l.hitBack(A, 20, 10f, max));
        assertTrue(l.hitBack(A, 30, 10f, max), "the third blow inside the window wrenches it back");
        assertEquals(0.0, l.taken(A, 31), 1e-9);
        assertEquals(1.0, l.damageFactor(A, 31), 1e-9);
        assertEquals(0.0, l.bonus(31), 1e-9, "and Ban loses what he borrowed");
        assertFalse(l.hitBack(A, 40, 10f, max), "nothing left to retake");
    }

    @Test
    void feebleOrSlowBlowsDoNotCount() {
        SnatchLedger l = new SnatchLedger();
        l.steal(A, 0, 0.24);
        float max = 1000f;
        for (int i = 0; i < 20; i++) assertFalse(l.hitBack(A, 10 + i, 3f, max), "a tick of poison (under 0.4% of his health) is not a blow");
        assertEquals(0.24, l.taken(A, 40), 1e-9);
        assertFalse(l.hitBack(A, 50, 10f, max));
        assertFalse(l.hitBack(A, 50 + SnatchLedger.RETAKE_WINDOW + 5, 10f, max), "too slow: the count starts again");
        assertFalse(l.hitBack(A, 50 + SnatchLedger.RETAKE_WINDOW + 10, 10f, max));
        assertTrue(l.hitBack(A, 50 + SnatchLedger.RETAKE_WINDOW + 15, 10f, max));
        assertFalse(new SnatchLedger().hitBack(A, 5, 100f, max), "nothing taken, nothing to retake");
        SnatchLedger z = new SnatchLedger();
        z.steal(A, 0, 0.2);
        assertFalse(z.hitBack(A, 5, 100f, 0f), "a nonsense maximum retakes nothing");
    }

    @Test
    void aSecondSnatchRestartsTheRetakeCount() {
        SnatchLedger l = new SnatchLedger();
        l.steal(A, 0, 0.12);
        assertFalse(l.hitBack(A, 5, 10f, 1000f));
        assertFalse(l.hitBack(A, 6, 10f, 1000f));
        l.steal(A, 7, 0.12);
        assertFalse(l.hitBack(A, 8, 10f, 1000f), "the count began again");
        assertFalse(l.hitBack(A, 9, 10f, 1000f));
        assertTrue(l.hitBack(A, 10, 10f, 1000f));
    }

    @Test
    void releaseAndClearForgetEverything() {
        SnatchLedger l = new SnatchLedger();
        l.steal(A, 0, 0.12);
        l.steal(B, 0, 0.12);
        l.release(A);
        assertEquals(0.0, l.taken(A, 1), 0.0);
        assertEquals(0.12, l.taken(B, 1), 1e-9);
        l.clear();
        assertEquals(0, l.victims(1));
        assertEquals(0.0, l.borrowed(1), 0.0);
    }
}
