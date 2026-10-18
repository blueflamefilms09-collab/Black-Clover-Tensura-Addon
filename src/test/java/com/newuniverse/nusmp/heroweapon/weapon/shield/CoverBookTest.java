package com.newuniverse.nusmp.heroweapon.weapon.shield;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Why: Protect and Second Shield are promises the wielder makes to other people, and a bug in the bookkeeping is either a free invulnerability or a protection that
 * silently never works. The book is tested with plain ids: who is covered, that a recast replaces, that the numbers are bounded, that time ends everything, that a
 * ward soaks exactly its bank, and that the redirect arithmetic can never take the wielder below the floor.
 */
class CoverBookTest {
    private static List<UUID> ids(int n) {
        List<UUID> out = new ArrayList<>();
        for (int i = 0; i < n; i++) out.add(UUID.nameUUIDFromBytes(("ally" + i).getBytes()));
        return out;
    }

    private static final UUID W = UUID.nameUUIDFromBytes("wielder".getBytes()), W2 = UUID.nameUUIDFromBytes("other wielder".getBytes());

    @Test
    void coverBindsTheAlliesForTheTimeAndThenEnds() {
        CoverBook b = new CoverBook();
        List<UUID> a = ids(3);
        assertEquals(3, b.cover(W, a, 100, 160, 0.4));
        CoverBook.Cover c = b.coverOf(a.get(0), 150);
        assertNotNull(c);
        assertEquals(W, c.wielder());
        assertEquals(0.4, c.share(), 1e-9);
        assertNotNull(b.coverOf(a.get(2), 259));
        assertNull(b.coverOf(a.get(0), 260), "ended exactly at the time");
        assertEquals(2, b.coverCount(), "the expired entry was dropped when asked");
    }

    @Test
    void theWielderIsNeverCoveredByThemselvesAndTheCrowdIsBounded() {
        CoverBook b = new CoverBook();
        List<UUID> many = ids(20);
        many.add(0, W);
        assertEquals(ShieldNumbers.COVER_MAX_ALLIES, b.cover(W, many, 0, 100, 0.4));
        assertNull(b.coverOf(W, 1), "not covered by themselves");
        assertEquals(ShieldNumbers.COVER_MAX_ALLIES, b.coverCount());
    }

    @Test
    void aRecastReplacesTheOldCoverOfThatWielderOnly() {
        CoverBook b = new CoverBook();
        List<UUID> first = ids(4), second = ids(8).subList(4, 8);
        b.cover(W, first, 0, 100, 0.4);
        b.cover(W2, ids(2).subList(0, 1), 0, 100, 0.3);                // W2 covers ally 0 too, replacing W's cover of it
        assertEquals(W2, b.coverOf(first.get(0), 10).wielder(), "an ally has one cover: the newer");
        b.cover(W, second, 5, 100, 0.4);
        assertNull(b.coverOf(first.get(1), 10), "W's first cover ended when W recast");
        assertNotNull(b.coverOf(second.get(0), 10));
        assertNotNull(b.coverOf(first.get(0), 10), "W2's cover is not touched by W's recast");
    }

    @Test
    void nothingIsBoundForNothing() {
        CoverBook b = new CoverBook();
        assertEquals(0, b.cover(W, ids(3), 0, 0, 0.4));
        assertEquals(0, b.cover(W, ids(3), 0, 100, 0.0));
        assertEquals(0, b.coverCount());
    }

    @Test
    void aWardSoaksExactlyItsBankAndThenIsGone() {
        CoverBook b = new CoverBook();
        UUID ally = ids(1).get(0);
        b.ward(W, ally, 0, 200, 10);
        assertEquals(4.0, b.soak(ally, 5, 4), 1e-9);
        assertEquals(6.0, b.wardOf(ally, 6).bank(), 1e-9);
        assertEquals(6.0, b.soak(ally, 7, 9), 1e-9, "only what is left is soaked");
        assertNull(b.wardOf(ally, 8));
        assertEquals(0.0, b.soak(ally, 9, 5), 1e-9);
    }

    @Test
    void aWardEndsWithTimeAndAnEmptyBlowIsIgnored() {
        CoverBook b = new CoverBook();
        UUID ally = ids(1).get(0);
        b.ward(W, ally, 0, 200, 10);
        assertEquals(0.0, b.soak(ally, 5, 0), 1e-9);
        assertEquals(0.0, b.soak(ally, 5, -3), 1e-9);
        assertEquals(10.0, b.wardOf(ally, 6).bank(), 1e-9, "a zero blow costs nothing");
        assertEquals(0.0, b.soak(ally, 200, 5), 1e-9, "expired");
        assertNull(b.wardOf(ally, 200));
    }

    @Test
    void aWielderKeepsAtMostThreeWardsAndTheOneThatEndsFirstGivesWay() {
        CoverBook b = new CoverBook();
        List<UUID> a = ids(5);
        b.ward(W, a.get(0), 0, 100, 5);                                // ends at 100
        b.ward(W, a.get(1), 10, 100, 5);                               // 110
        b.ward(W, a.get(2), 20, 100, 5);                               // 120
        b.ward(W, a.get(3), 30, 100, 5);                               // 130: the first (100) is dropped
        assertNull(b.wardOf(a.get(0), 31));
        assertNotNull(b.wardOf(a.get(1), 31));
        assertNotNull(b.wardOf(a.get(2), 31));
        assertNotNull(b.wardOf(a.get(3), 31));
        assertEquals(3, b.wardCount());
        b.ward(W2, a.get(4), 31, 100, 5);
        assertEquals(4, b.wardCount(), "another wielder's wards do not count against this one");
    }

    @Test
    void aSecondWardOnTheSameAllyReplacesTheFirst() {
        CoverBook b = new CoverBook();
        UUID ally = ids(1).get(0);
        b.ward(W, ally, 0, 100, 5);
        b.ward(W, ally, 10, 100, 9);
        assertEquals(9.0, b.wardOf(ally, 11).bank(), 1e-9);
        assertEquals(1, b.wardCount());
    }

    @Test
    void forgetAndClearEmptyTheBook() {
        CoverBook b = new CoverBook();
        List<UUID> a = ids(3);
        b.cover(W, a, 0, 100, 0.4);
        b.ward(W, a.get(0), 0, 100, 5);
        b.ward(W2, a.get(1), 0, 100, 5);
        b.forget(W);
        assertEquals(0, b.coverCount());
        assertEquals(1, b.wardCount());
        b.clear();
        assertEquals(0, b.wardCount());
    }

    @Test
    void aClockThatWentFarBackDropsTheEntryInsteadOfKeepingItForHours() {
        CoverBook b = new CoverBook();
        UUID ally = ids(1).get(0);
        b.ward(W, ally, 1_000_000, 200, 10);
        assertNull(b.wardOf(ally, 10), "the clock went back by a million ticks");
        b.cover(W, List.of(ally), 1_000_000, 160, 0.4);
        assertNull(b.coverOf(ally, 10));
    }

    @Test
    void theRedirectIsTheShareButNeverMoreThanTheWielderCanGive() {
        assertEquals(4.0, CoverBook.redirect(10, 0.4, 20, 20, 0.25), 1e-9, "40 percent of 10, plenty of room");
        assertEquals(4.0, CoverBook.redirect(10, 0.4, 10, 20, 0.25), 1e-9, "health 10 of 20 with a floor of 5: room for 5, the share of 4 is smaller and wins");
        assertEquals(2.0, CoverBook.redirect(10, 0.4, 7, 20, 0.25), 1e-9, "health 7: room for 2, which is smaller than the share");
    }

    @Test
    void theRedirectNeverTakesTheWielderUnderTheFloor() {
        // health 7 of 20, floor 25 percent = 5: room 2
        assertEquals(2.0, CoverBook.redirect(10, 0.9, 7, 20, 0.25), 1e-9);
        assertEquals(0.0, CoverBook.redirect(10, 0.9, 5, 20, 0.25), 1e-9, "at the floor: nothing");
        assertEquals(0.0, CoverBook.redirect(10, 0.9, 3, 20, 0.25), 1e-9, "under the floor: nothing");
        assertEquals(0.0, CoverBook.redirect(0, 0.9, 20, 20, 0.25), 1e-9);
        assertEquals(0.0, CoverBook.redirect(10, 0.0, 20, 20, 0.25), 1e-9);
        assertEquals(0.0, CoverBook.redirect(Double.NaN, 0.4, 20, 20, 0.25), 1e-9);
        assertFalse(CoverBook.redirect(10, 5.0, 20, 20, 0.25) > 10.0, "a share above one is still at most the blow");
    }
}
