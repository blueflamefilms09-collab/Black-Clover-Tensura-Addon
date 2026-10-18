package com.newuniverse.nusmp.heroweapon.weapon.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * Why: marks, prisms and buffs must fade on their own and must never pile up, so the table that holds them is pinned: an entry is active strictly before
 * its end, a repeated key keeps the later end, a full table never grows and gives up the entry that would end soonest, and pruning only removes what is over.
 */
class ExpiryTableTest {
    @Test
    void anEntryIsActiveStrictlyBeforeItsEnd() {
        ExpiryTable<String> t = new ExpiryTable<>(8);
        t.put("a", 100, 0);
        assertTrue(t.active("a", 0));
        assertTrue(t.active("a", 99));
        assertFalse(t.active("a", 100), "at the end tick it is over");
        assertFalse(t.active("a", 500));
        assertFalse(t.active("never", 0));
        assertEquals(40L, t.remaining("a", 60));
        assertEquals(0L, t.remaining("a", 150));
        assertEquals(0L, t.remaining("never", 0));
    }

    @Test
    void aRepeatedKeyKeepsTheLaterEnd() {
        ExpiryTable<String> t = new ExpiryTable<>(8);
        t.put("a", 100, 0);
        t.put("a", 60, 10);
        assertEquals(90L, t.remaining("a", 10), "a shorter second put does not cut the first short");
        t.put("a", 200, 10);
        assertEquals(190L, t.remaining("a", 10), "a longer second put extends it");
        assertEquals(1, t.size());
    }

    @Test
    void aFullTableNeverGrowsAndDropsTheEntryEndingSoonest() {
        ExpiryTable<Integer> t = new ExpiryTable<>(3);
        t.put(1, 50, 0);
        t.put(2, 90, 0);
        t.put(3, 70, 0);
        t.put(4, 80, 0);
        assertEquals(3, t.size());
        assertFalse(t.active(1, 0), "the entry that would have ended first made room");
        assertTrue(t.active(2, 0));
        assertTrue(t.active(3, 0));
        assertTrue(t.active(4, 0));
    }

    @Test
    void pruningRemovesOnlyWhatIsOver() {
        ExpiryTable<Integer> t = new ExpiryTable<>(10);
        t.put(1, 10, 0);
        t.put(2, 20, 0);
        t.put(3, 30, 0);
        assertEquals(2, t.prune(20), "10 and 20 are over at tick 20");
        assertEquals(1, t.size());
        assertEquals(java.util.List.of(3), t.activeKeys(20));
        t.remove(3);
        assertEquals(0, t.size());
        t.put(5, 99, 0);
        t.clear();
        assertEquals(0, t.size());
        t.put(null, 99, 0);
        assertEquals(0, t.size(), "a null key is ignored");
    }
}
