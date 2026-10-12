package com.newuniverse.nusmp.entity.zogratis;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The Soul Paladins' ledger: bounded, down-and-rise timers, the soul cut. */
class PaladinLedgerTest {
    @Test
    void addIsBoundedByTheCapAndRefusesRepeats() {
        PaladinLedger<String> l = new PaladinLedger<>();
        assertTrue(l.add("a", 2));
        assertFalse(l.add("a", 2), "already there");
        assertTrue(l.add("b", 2));
        assertFalse(l.add("c", 2), "the cap");
        assertEquals(2, l.size());
        assertTrue(l.add("c", 3), "a larger cap (more power) makes room");
        assertFalse(l.add(null, 9));
        assertFalse(l.add("d", 0));
    }

    @Test
    void aStruckDownPaladinKneelsAndRisesWhenItsTimerRunsOut() {
        PaladinLedger<String> l = new PaladinLedger<>();
        l.add("a", 4);
        assertEquals(PaladinLedger.State.UP, l.stateOf("a"));
        assertTrue(l.down("a", 1000, 200));
        assertEquals(PaladinLedger.State.DOWNED, l.stateOf("a"));
        assertFalse(l.down("a", 1010, 200), "already down: the timer does not restart");
        assertEquals(190, l.riseIn("a", 1010));
        assertTrue(l.due(1199).isEmpty());
        assertEquals(List.of("a"), l.due(1200));
        assertTrue(l.raise("a"));
        assertEquals(PaladinLedger.State.UP, l.stateOf("a"));
        assertFalse(l.raise("a"), "it was not down");
        assertEquals(1, l.falls("a"));
        assertTrue(l.down("a", 2000, 100));
        assertEquals(2, l.falls("a"));
    }

    @Test
    void countsStandingAndDowned() {
        PaladinLedger<Integer> l = new PaladinLedger<>();
        for (int i = 0; i < 4; i++) l.add(i, 4);
        l.down(1, 0, 50);
        l.down(3, 10, 50);
        assertEquals(2, l.standing());
        assertEquals(2, l.downed());
        assertEquals(List.of(1, 3), l.downedIds(), "oldest first");
        assertEquals(List.of(0, 1, 2, 3), l.ids());
    }

    @Test
    void dueListsOnlyTheOnesWhoseTimeIsUp() {
        PaladinLedger<Integer> l = new PaladinLedger<>();
        for (int i = 0; i < 3; i++) l.add(i, 4);
        l.down(0, 0, 100);
        l.down(1, 0, 300);
        l.down(2, 50, 100);
        assertEquals(List.of(0), l.due(100));
        assertEquals(List.of(0, 2), l.due(150));
        assertEquals(List.of(0, 1, 2), l.due(300));
    }

    @Test
    void cuttingTheSoulRemovesItForGood() {
        PaladinLedger<String> l = new PaladinLedger<>();
        l.add("a", 4);
        l.down("a", 0, 100);
        assertTrue(l.remove("a"));
        assertNull(l.stateOf("a"));
        assertTrue(l.due(1_000_000).isEmpty(), "it never rises");
        assertFalse(l.remove("a"));
        assertEquals(0, l.riseIn("a", 5));
        assertTrue(l.add("a", 4), "room again");
    }

    @Test
    void clearForgetsEveryone() {
        PaladinLedger<String> l = new PaladinLedger<>();
        l.add("a", 4);
        l.add("b", 4);
        l.clear();
        assertTrue(l.isEmpty());
    }
}
