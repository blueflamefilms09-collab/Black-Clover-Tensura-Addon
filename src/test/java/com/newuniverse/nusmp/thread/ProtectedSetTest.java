package com.newuniverse.nusmp.thread;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The hand-picked ally list: cap, toggle, saved form; and who is under Rouge's watch. */
class ProtectedSetTest {
    private static UUID id(int n) { return new UUID(0L, n); }

    @Test
    void toggleAddsRemovesAndHonoursTheCap() {
        List<UUID> list = new ArrayList<>();
        assertEquals(ProtectedSet.Toggle.ADDED, ProtectedSet.toggle(list, id(1), 2));
        assertEquals(ProtectedSet.Toggle.ADDED, ProtectedSet.toggle(list, id(2), 2));
        assertEquals(ProtectedSet.Toggle.FULL, ProtectedSet.toggle(list, id(3), 2), "the third does not fit");
        assertEquals(2, list.size());
        assertEquals(ProtectedSet.Toggle.REMOVED, ProtectedSet.toggle(list, id(1), 2), "the same name again removes it");
        assertEquals(ProtectedSet.Toggle.ADDED, ProtectedSet.toggle(list, id(3), 2), "room again");
        assertEquals(List.of(id(2), id(3)), list);
    }

    @Test
    void aZeroCapNamesNobodyButStillLetsYouRemove() {
        List<UUID> list = new ArrayList<>(List.of(id(1)));
        assertEquals(ProtectedSet.Toggle.REMOVED, ProtectedSet.toggle(list, id(1), 0));
        assertEquals(ProtectedSet.Toggle.FULL, ProtectedSet.toggle(list, id(2), 0));
        assertEquals(ProtectedSet.Toggle.FULL, ProtectedSet.toggle(list, id(2), -3));
    }

    @Test
    void theSavedFormRoundTripsAndSurvivesDamage() {
        List<UUID> list = List.of(id(7), id(8), id(9));
        assertEquals(list, ProtectedSet.parse(ProtectedSet.format(list)));
        assertEquals("", ProtectedSet.format(List.of()));
        assertEquals(List.of(), ProtectedSet.parse(""));
        assertEquals(List.of(), ProtectedSet.parse(null));
        String damaged = id(1) + ", nonsense ,," + id(2) + "," + id(1);
        assertEquals(List.of(id(1), id(2)), ProtectedSet.parse(damaged), "bad pieces dropped, repeats collapsed, order kept");
    }

    @Test
    void loweringTheCapCutsTheListToTheOldestEntries() {
        List<UUID> list = List.of(id(1), id(2), id(3), id(4));
        assertEquals(List.of(id(1), id(2)), ProtectedSet.clamp(list, 2));
        assertEquals(list, ProtectedSet.clamp(list, 10));
        assertEquals(List.of(), ProtectedSet.clamp(list, 0));
    }

    @Test
    void whoIsProtected() {
        // (isCaster, whitelisted, ownedPet, squadMate, attackingCaster, isRouge)
        assertTrue(ProtectedSet.isProtected(true, false, false, false, false, false), "the caster");
        assertTrue(ProtectedSet.isProtected(false, true, false, false, false, false), "a chosen name");
        assertTrue(ProtectedSet.isProtected(false, false, true, false, false, false), "her pet");
        assertTrue(ProtectedSet.isProtected(false, false, false, true, false, false), "a squad mate");
        assertFalse(ProtectedSet.isProtected(false, false, false, false, false, false), "a stranger");
        assertFalse(ProtectedSet.isProtected(false, true, true, true, true, false), "an enemy is never protected, however it got listed");
        assertFalse(ProtectedSet.isProtected(true, true, true, true, false, true), "Rouge never guards herself");
    }
}
