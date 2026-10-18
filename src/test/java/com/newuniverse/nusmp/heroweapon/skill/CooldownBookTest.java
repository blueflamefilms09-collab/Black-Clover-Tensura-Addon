package com.newuniverse.nusmp.heroweapon.skill;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Why: the cooldown book is the only place that says when a technique may be cast again, so its arithmetic is pinned: a cooldown counts down in game time and
 * ends exactly on time, two players never share one, two skills of one player do not either, the global lock is separate, a clock set back never locks a
 * weapon for good, and forgetting a player forgets all of them.
 */
class CooldownBookTest {
    private static final UUID A = UUID.nameUUIDFromBytes("a".getBytes()), B = UUID.nameUUIDFromBytes("b".getBytes());

    @Test
    void aCooldownCountsDownAndEndsOnTime() {
        CooldownBook book = new CooldownBook();
        assertTrue(book.ready(A, "shield.air_strike", 100));
        book.start(A, "shield.air_strike", 100, 60);
        assertEquals(60, book.left(A, "shield.air_strike", 100));
        assertEquals(1, book.left(A, "shield.air_strike", 159));
        assertEquals(0, book.left(A, "shield.air_strike", 160));
        assertTrue(book.ready(A, "shield.air_strike", 160));
    }

    @Test
    void playersAndSkillsDoNotShareCooldowns() {
        CooldownBook book = new CooldownBook();
        book.start(A, "sword.slash", 0, 100);
        assertTrue(book.ready(B, "sword.slash", 1), "another player");
        assertTrue(book.ready(A, "sword.dash", 1), "another skill");
        assertFalse(book.ready(A, "sword.slash", 1));
    }

    @Test
    void theGlobalLockIsItsOwnClock() {
        CooldownBook book = new CooldownBook();
        book.lock(A, 10, 40);
        assertEquals(40, book.lockLeft(A, 10));
        assertEquals(0, book.lockLeft(B, 10));
        assertEquals(0, book.lockLeft(A, 50));
        assertTrue(book.ready(A, "x.y", 11), "a lock is not a cooldown");
        book.lock(A, 100, 0);
        assertEquals(0, book.lockLeft(A, 100));
    }

    @Test
    void aClockSetBackNeverLocksAWeaponForGood() {
        CooldownBook book = new CooldownBook();
        book.start(A, "bow.volley", 1_000_000, 600);
        assertEquals(0, book.left(A, "bow.volley", 5), "the world was rewound by a million ticks");
        book.lock(A, 1_000_000, 40);
        assertEquals(0, book.lockLeft(A, 5));
    }

    @Test
    void anAbsurdCooldownIsClamped() {
        CooldownBook book = new CooldownBook();
        book.start(A, "x.y", 0, Integer.MAX_VALUE);
        assertEquals(CooldownBook.MAX_TICKS, book.left(A, "x.y", 0));
        book.start(A, "x.z", 0, -50);
        assertEquals(0, book.left(A, "x.z", 0));
    }

    @Test
    void startingAgainReplacesAndZeroClears() {
        CooldownBook book = new CooldownBook();
        book.start(A, "x.y", 0, 100);
        book.start(A, "x.y", 50, 20);
        assertEquals(20, book.left(A, "x.y", 50));
        book.start(A, "x.y", 50, 0);
        assertTrue(book.ready(A, "x.y", 50));
    }

    @Test
    void runningListsOnlyWhatIsStillRunning() {
        CooldownBook book = new CooldownBook();
        book.start(A, "a.one", 0, 100);
        book.start(A, "a.two", 0, 10);
        assertEquals(2, book.running(A, 5).size());
        assertEquals(1, book.running(A, 50).size());
        assertEquals(Integer.valueOf(50), book.running(A, 50).get("a.one"));
        assertTrue(book.running(B, 5).isEmpty());
    }

    @Test
    void forgetAndClearForgetEverything() {
        CooldownBook book = new CooldownBook();
        book.start(A, "x.y", 0, 100);
        book.lock(A, 0, 40);
        book.start(B, "x.y", 0, 100);
        book.forget(A);
        assertTrue(book.ready(A, "x.y", 1));
        assertEquals(0, book.lockLeft(A, 1));
        assertFalse(book.ready(B, "x.y", 1));
        book.clear();
        assertTrue(book.ready(B, "x.y", 1));
    }
}
