package com.newuniverse.nusmp.perf;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: "send on change, plus a slow refresh" is the rule for every state a client mirrors; {@link DiffSend} is only the decision, so this pins it: the first look always sends, an unchanged
 * state is not repeated, a changed one is, an unchanged one is repeated once the refresh period has passed (and never when the refresh is off), keys do not disturb each other, a forgotten key
 * starts again, and a clock that runs backwards (a dimension change, a world reload) sends instead of staying silent.
 */
class DiffSendTest {
    @Test
    void theFirstLookSendsAndAnUnchangedStateDoesNot() {
        DiffSend d = new DiffSend();
        assertTrue(d.shouldSend(1, 42, 100, 200));
        assertFalse(d.shouldSend(1, 42, 101, 200));
        assertFalse(d.shouldSend(1, 42, 299, 200));
        assertEquals(1L, d.sent());
        assertEquals(2L, d.skipped());
    }

    @Test
    void aChangeSendsAtOnce() {
        DiffSend d = new DiffSend();
        d.shouldSend(1, 42, 100, 200);
        assertTrue(d.shouldSend(1, 43, 101, 200));
        assertFalse(d.shouldSend(1, 43, 102, 200));
    }

    @Test
    void anUnchangedStateIsRepeatedAfterTheRefreshPeriod() {
        DiffSend d = new DiffSend();
        assertTrue(d.shouldSend(7, 5, 1000, 200));
        assertFalse(d.shouldSend(7, 5, 1199, 200));
        assertTrue(d.shouldSend(7, 5, 1200, 200));          // exactly the period later
        assertFalse(d.shouldSend(7, 5, 1399, 200));         // and the period counts from the repeat
        assertTrue(d.shouldSend(7, 5, 1400, 200));
    }

    @Test
    void aRefreshOfZeroOrLessNeverRepeats() {
        DiffSend d = new DiffSend();
        assertTrue(d.shouldSend(7, 5, 0, 0));
        assertFalse(d.shouldSend(7, 5, 1_000_000, 0));
        assertFalse(d.shouldSend(7, 5, 2_000_000, -1));
    }

    @Test
    void keysAreIndependent() {
        DiffSend d = new DiffSend();
        assertTrue(d.shouldSend(1, 9, 10, 0));
        assertTrue(d.shouldSend(2, 9, 10, 0));
        assertFalse(d.shouldSend(1, 9, 11, 0));
        assertTrue(d.shouldSend(2, 8, 11, 0));
        assertEquals(2, d.size());
    }

    @Test
    void aForgottenKeyStartsAgain() {
        DiffSend d = new DiffSend();
        d.shouldSend(1, 9, 10, 0);
        d.forget(1);
        assertTrue(d.shouldSend(1, 9, 11, 0));
        d.clear();
        assertEquals(0, d.size());
        assertTrue(d.shouldSend(1, 9, 12, 0));
    }

    @Test
    void aClockThatRunsBackwardsSendsInsteadOfStayingSilent() {
        DiffSend d = new DiffSend();
        assertTrue(d.shouldSend(1, 9, 5000, 0));
        assertFalse(d.shouldSend(1, 9, 5001, 0));
        assertTrue(d.shouldSend(1, 9, 3, 0));                // the game time restarted
    }

    @Test
    void signaturesSeparateDifferentStates() {
        assertFalse(DiffSend.sign(1, 2, 3, 4) == DiffSend.sign(1, 2, 3, 5));
        assertFalse(DiffSend.sign(1, 2, 3, 4) == DiffSend.sign(4, 3, 2, 1));
        assertEquals(DiffSend.sign(1, 2, 3, 4), DiffSend.sign(1, 2, 3, 4));
        assertFalse(DiffSend.sign(0.5) == DiffSend.sign(0.5000001));
        assertEquals(DiffSend.sign(0.25), DiffSend.sign(0.25));
    }
}
