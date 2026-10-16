package com.newuniverse.nusmp.soul;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The cap by mastery, the eviction of the oldest, restoring after a load in any order, and the release of everyone when the owner dies. */
class PaladinRosterTest {
    private static UUID id(int n) { return new UUID(0, n); }

    @Test
    void theLimitIsThreePlusOnePerQuarterMasteryUpToSix() {
        assertEquals(3, PaladinRoster.limit(0));
        assertEquals(3, PaladinRoster.limit(24.9));
        assertEquals(4, PaladinRoster.limit(25));
        assertEquals(5, PaladinRoster.limit(50));
        assertEquals(6, PaladinRoster.limit(75));
        assertEquals(6, PaladinRoster.limit(100), "3 + 4 = 7 is cut to the ceiling of six");
        assertEquals(3, PaladinRoster.limit(-40));
        assertEquals(3, PaladinRoster.limit(Double.NaN));
    }

    @Test
    void theConfigCanLowerTheLimitButNeverRaiseItPastSix() {
        assertEquals(2, PaladinRoster.limit(100, 1, 100, 6));
        assertEquals(4, PaladinRoster.limit(100, 3, 25, 4));
        assertEquals(6, PaladinRoster.limit(100, 9, 25, 99));
        assertEquals(1, PaladinRoster.limit(0, 0, 25, 6), "at least one");
    }

    @Test
    void theOldestIsEvictedWhenTheOwnerIsOverTheLimit() {
        PaladinRoster<UUID> r = new PaladinRoster<>();
        UUID owner = id(100);
        assertTrue(r.add(owner, id(1), 10, 3).isEmpty());
        assertTrue(r.add(owner, id(2), 20, 3).isEmpty());
        assertTrue(r.add(owner, id(3), 30, 3).isEmpty());
        List<UUID> gone = r.add(owner, id(4), 40, 3);
        assertEquals(List.of(id(1)), gone);
        assertEquals(3, r.count(owner));
        assertFalse(r.contains(id(1)));
        assertTrue(r.contains(id(4)), "the new one is never the one that goes");
    }

    @Test
    void loadingInAnyOrderStillEvictsByConversionTime() {
        PaladinRoster<UUID> r = new PaladinRoster<>();
        UUID owner = id(100);
        r.add(owner, id(3), 30, 2);
        r.add(owner, id(1), 10, 2);          // restored later but older: it is the one that goes
        assertTrue(r.contains(id(3)));
        assertTrue(r.contains(id(1)), "two fit: nobody is evicted yet");
        assertEquals(2, r.count(owner));
        List<UUID> third = r.add(owner, id(2), 20, 2);
        assertEquals(1, third.size());
        assertEquals(id(1), third.get(0));
    }

    @Test
    void ownersAreIndependent() {
        PaladinRoster<UUID> r = new PaladinRoster<>();
        r.add(id(100), id(1), 1, 1);
        r.add(id(200), id(2), 2, 1);
        assertEquals(1, r.count(id(100)));
        assertEquals(1, r.count(id(200)));
        assertEquals(id(200), r.ownerOf(id(2)));
        assertEquals(2, r.owners().size());
    }

    @Test
    void whenTheOwnerDiesEveryPaladinIsReleased() {
        PaladinRoster<UUID> r = new PaladinRoster<>();
        r.add(id(100), id(1), 1, 6);
        r.add(id(100), id(2), 2, 6);
        r.add(id(200), id(9), 3, 6);
        List<UUID> freed = r.releaseOwner(id(100));
        assertEquals(List.of(id(1), id(2)), freed);
        assertEquals(0, r.count(id(100)));
        assertTrue(r.contains(id(9)), "someone else's Paladin stays");
        assertTrue(r.releaseOwner(id(100)).isEmpty(), "twice is safe");
    }

    @Test
    void aDeadPaladinFreesItsPlaceAndRemoveIsSafeTwice() {
        PaladinRoster<UUID> r = new PaladinRoster<>();
        r.add(id(100), id(1), 1, 3);
        assertEquals(id(100), r.remove(id(1)));
        assertNull(r.remove(id(1)));
        assertEquals(0, r.size());
    }

    @Test
    void shrinkingTheLimitTrimsTheOldest() {
        PaladinRoster<UUID> r = new PaladinRoster<>();
        for (int i = 1; i <= 5; i++) r.add(id(100), id(i), i, 6);
        List<UUID> gone = r.trim(id(100), 3);
        assertEquals(List.of(id(1), id(2)), gone);
        assertEquals(List.of(id(3), id(4), id(5)), r.of(id(100)));
    }

    @Test
    void nullsAndZeroAreHandled() {
        PaladinRoster<UUID> r = new PaladinRoster<>();
        assertTrue(r.add(null, id(1), 1, 3).isEmpty());
        assertTrue(r.add(id(100), null, 1, 3).isEmpty());
        assertEquals(0, r.size());
        assertEquals(List.of(id(1)), r.add(id(100), id(1), 1, 0), "a limit of zero lets no one stay");
    }
}
