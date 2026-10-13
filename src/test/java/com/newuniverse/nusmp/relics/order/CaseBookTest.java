package com.newuniverse.nusmp.relics.order;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The waiting list of the Order: one place per relic id, oldest first, bounded, and nothing is stored while a relic cools down. */
class CaseBookTest {
    @Test
    void theOldestWaitingRelicComesFirst() {
        CaseBook<String> b = new CaseBook<>(4);
        assertTrue(b.offer("a", "A1", true));
        assertTrue(b.offer("b", "B1", true));
        assertEquals("A1", b.poll(id -> true));
        assertEquals("B1", b.poll(id -> true));
        assertNull(b.poll(id -> true));
    }

    @Test
    void aRelicWaitsOnceAndKeepsItsPlaceWithTheNewerDetails() {
        CaseBook<String> b = new CaseBook<>(4);
        b.offer("a", "old", true);
        b.offer("b", "B", true);
        assertTrue(b.offer("a", "new", true));
        assertEquals(2, b.size());
        assertEquals("new", b.poll(id -> true), "still first");
    }

    @Test
    void nothingIsStoredWhileTheRelicCoolsDown() {
        CaseBook<String> b = new CaseBook<>(4);
        assertFalse(b.offer("a", "A", false));
        assertEquals(0, b.size());
    }

    @Test
    void theListIsBoundedButAWaitingRelicCanStillBeUpdated() {
        CaseBook<Integer> b = new CaseBook<>(2);
        assertTrue(b.offer("a", 1, true));
        assertTrue(b.offer("b", 2, true));
        assertFalse(b.offer("c", 3, true), "full");
        assertTrue(b.offer("a", 9, true));
        assertEquals(2, b.size());
    }

    @Test
    void pollSkipsRelicsThatAreNotReadyAndLeavesThemWaiting() {
        CaseBook<String> b = new CaseBook<>(4);
        b.offer("a", "A", true);
        b.offer("b", "B", true);
        Set<String> ready = Set.of("b");
        assertEquals("B", b.poll(ready::contains));
        assertTrue(b.waiting("a"));
        assertNull(b.poll(ready::contains));
    }

    @Test
    void forgetDropsAWaitingRelic() {
        CaseBook<String> b = new CaseBook<>(4);
        b.offer("a", "A", true);
        assertEquals("A", b.forget("a"));
        assertFalse(b.waiting("a"));
        assertNull(b.forget("a"));
    }

    @Test
    void emptyIdsAreRefused() {
        CaseBook<String> b = new CaseBook<>(4);
        assertFalse(b.offer("", "x", true));
        assertFalse(b.offer(null, "x", true));
    }
}
