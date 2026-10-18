package com.newuniverse.nusmp.heroweapon.forms;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Why: the two-second lock after a technique stops a form swap from doubling up benefits; the arithmetic must be exact at the edges and a player who never cast
 * must never be locked.
 */
class FormLocksTest {
    @Test
    void theLockCountsDownAndEndsExactly() {
        assertEquals(40, FormLocks.remaining(100, 100, 40));
        assertEquals(1, FormLocks.remaining(100, 139, 40));
        assertEquals(0, FormLocks.remaining(100, 140, 40));
        assertEquals(0, FormLocks.remaining(100, 5000, 40));
    }

    @Test
    void aZeroLockAndABackwardsClockNeverLock() {
        assertEquals(0, FormLocks.remaining(100, 100, 0));
        assertEquals(0, FormLocks.remaining(100, 100, -5));
        assertEquals(0, FormLocks.remaining(5000, 100, 40), "the world clock restarted");
    }

    @Test
    void aPlayerWhoNeverCastIsNeverLockedAndForgettingUnlocks() {
        UUID a = UUID.nameUUIDFromBytes("a".getBytes()), b = UUID.nameUUIDFromBytes("b".getBytes());
        FormLocks.clear();
        assertEquals(0, FormLocks.remaining(a, 10, 40));
        FormLocks.technique(a, 10);
        assertEquals(30, FormLocks.remaining(a, 20, 40));
        assertEquals(0, FormLocks.remaining(b, 20, 40), "another player is not affected");
        FormLocks.forget(a);
        assertEquals(0, FormLocks.remaining(a, 20, 40));
        FormLocks.technique(b, 1);
        FormLocks.clear();
        assertEquals(0, FormLocks.remaining(b, 2, 40));
    }
}
