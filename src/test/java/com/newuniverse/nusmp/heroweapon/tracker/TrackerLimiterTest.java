package com.newuniverse.nusmp.heroweapon.tracker;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the server answers a player at most once per two seconds and keeps a bounded table; both are pure rules. */
class TrackerLimiterTest {
    private static UUID id(int n) { return new UUID(0L, n); }

    @Test
    void oneAnswerPerGapPerPlayerAndTheRestAreDropped() {
        TrackerLimiter l = new TrackerLimiter();
        assertEquals(40, TrackerLimiter.GAP_TICKS);
        assertTrue(l.allow(id(1), 1000));
        assertFalse(l.allow(id(1), 1000));
        assertFalse(l.allow(id(1), 1039));
        assertTrue(l.allow(id(1), 1040));
        assertFalse(l.allow(id(1), 1041));
    }

    @Test
    void aDroppedRequestDoesNotExtendTheWait() {
        TrackerLimiter l = new TrackerLimiter();
        assertTrue(l.allow(id(1), 0));
        for (int t = 1; t < 40; t++) assertFalse(l.allow(id(1), t));          // a client hammering the request
        assertTrue(l.allow(id(1), 40));                                       // still answered exactly 40 ticks after the last answer
    }

    @Test
    void playersDoNotShareAGap() {
        TrackerLimiter l = new TrackerLimiter();
        assertTrue(l.allow(id(1), 5));
        assertTrue(l.allow(id(2), 5));
        assertFalse(l.allow(id(1), 6));
        assertFalse(l.allow(id(2), 6));
    }

    @Test
    void aWorldClockSetBackDoesNotLockAPlayerOut() {
        TrackerLimiter l = new TrackerLimiter();
        assertTrue(l.allow(id(1), 100000));
        assertTrue(l.allow(id(1), 10));
    }

    @Test
    void forgetAndClearEmptyTheTableAndANullPlayerIsRefused() {
        TrackerLimiter l = new TrackerLimiter();
        l.allow(id(1), 0);
        l.allow(id(2), 0);
        assertEquals(2, l.size());
        l.forget(id(1));
        assertEquals(1, l.size());
        assertTrue(l.allow(id(1), 1));                                        // a returning player is answered at once
        l.clear();
        assertEquals(0, l.size());
        assertFalse(l.allow(null, 0));
    }

    @Test
    void theTableStaysBoundedWhenManyPlayersAskOnce() {
        TrackerLimiter l = new TrackerLimiter();
        for (int i = 0; i < 5000; i++) l.allow(id(i), i * 100L);              // each asks long after the one before: old entries are swept
        assertTrue(l.size() <= TrackerLimiter.SWEEP_AT + 1, "size " + l.size());
    }
}
