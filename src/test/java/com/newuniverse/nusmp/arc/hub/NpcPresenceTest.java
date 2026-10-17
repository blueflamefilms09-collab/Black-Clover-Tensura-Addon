package com.newuniverse.nusmp.arc.hub;

import org.junit.jupiter.api.Test;

import java.util.List;

import static com.newuniverse.nusmp.arc.hub.NpcPresence.Act.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the pass that keeps the friendly characters standing runs every five seconds on a world that is saved, restarted and half unloaded at any time; these
 * tests pin the two rules that matter (never make a second one, never act on what the pass cannot see) and the wish (always, no prerequisites, or at least one
 * online player has opened the character).
 */
class NpcPresenceTest {
    @Test
    void aCharacterIsWantedWhenAlwaysThereOrFreeOrOpenedBySomeone() {
        assertTrue(NpcPresence.wanted(true, List.of("boss"), 0), "the config can bring everybody");
        assertTrue(NpcPresence.wanted(false, List.of(), 0), "no prerequisites: always there");
        assertTrue(NpcPresence.wanted(false, null, 0), "a null list is no prerequisites");
        assertTrue(NpcPresence.wanted(false, List.of("boss"), 1));
        assertTrue(NpcPresence.wanted(false, List.of("boss"), 3));
        assertFalse(NpcPresence.wanted(false, List.of("boss"), 0), "nobody online has opened it");
    }

    @Test
    void aWantedCharacterIsMadeOnceKeptOnceAndDedupedWhenTwoStand() {
        assertEquals(SPAWN, NpcPresence.decide(true, true, 0));
        assertEquals(KEEP, NpcPresence.decide(true, true, 1));
        assertEquals(DEDUPE, NpcPresence.decide(true, true, 2));
        assertEquals(DEDUPE, NpcPresence.decide(true, true, 5));
    }

    @Test
    void anUnwantedCharacterIsRemovedOnlyWhenSomeoneStandsThere() {
        assertEquals(REMOVE, NpcPresence.decide(false, true, 1));
        assertEquals(REMOVE, NpcPresence.decide(false, true, 3));
        assertEquals(KEEP, NpcPresence.decide(false, true, 0));
    }

    @Test
    void nothingIsDoneForWhatCannotBeSeen() {
        for (boolean wanted : new boolean[] {true, false}) {
            for (int standing = 0; standing < 4; standing++) assertEquals(KEEP, NpcPresence.decide(wanted, false, standing), "unseen: wanted=" + wanted + " standing=" + standing);
        }
    }

    @Test
    void aRestartNeverMakesTheSecondOne() {
        // the first pass after a restart sees the saved one standing (1): keep; if the chunk was not loaded it sees nothing and must not make one
        assertEquals(KEEP, NpcPresence.decide(true, true, 1));
        assertEquals(KEEP, NpcPresence.decide(true, false, 0));
    }
}
