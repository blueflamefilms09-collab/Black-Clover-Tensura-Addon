package com.newuniverse.nusmp.corpse;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The ledger of the dead: the 60 second window, the cap, oldest-first eviction, pets. */
class CorpseLedgerTest {
    private static CorpseLedger.Fallen f(String type, long tick, boolean pet) {
        return new CorpseLedger.Fallen(type, type, 20, 3, 0.2, "minecraft:overworld", 0, 64, 0, tick, pet);
    }

    @Test
    void windowIsSixtySeconds() {
        CorpseLedger l = new CorpseLedger(24);
        l.record(f("a", 0, false));
        assertEquals(1, l.fresh(1200).size(), "exactly 60 s old is still there");
        assertEquals(0, l.fresh(1201).size(), "older is forgotten");
        assertTrue(l.isEmpty());
    }

    @Test
    void capEvictsOldestFirst() {
        CorpseLedger l = new CorpseLedger(3);
        for (int i = 0; i < 5; i++) l.record(f("m" + i, i, false));
        assertEquals(3, l.size());
        List<CorpseLedger.Fallen> fresh = l.fresh(10);
        assertEquals("m4", fresh.get(0).type());
        assertEquals("m2", fresh.get(2).type());
        l.setCap(1);
        assertEquals("m4", l.entries().get(0).type());
    }

    @Test
    void takeRemovesTheNewestFirst() {
        CorpseLedger l = new CorpseLedger(24);
        l.record(f("old", 0, false));
        l.record(f("new", 100, false));
        List<CorpseLedger.Fallen> got = l.take(200, 1);
        assertEquals(1, got.size());
        assertEquals("new", got.get(0).type());
        assertEquals(1, l.size());
        assertTrue(l.take(5000, 5).isEmpty(), "the other one expired");
    }

    @Test
    void petsOnlyAndWithinTheirWindow() {
        CorpseLedger l = new CorpseLedger(24);
        l.record(f("wolf", 0, true));
        l.record(f("zombie", 400, false));
        l.record(f("cat", 500, true));
        List<CorpseLedger.Fallen> pets = l.takePets(600, 300);
        assertEquals(1, pets.size(), "the wolf fell 600 ticks ago, outside 300");
        assertEquals("cat", pets.get(0).type());
        assertEquals(2, l.size());
    }

    @Test
    void restoreKeepsTheCap() {
        CorpseLedger l = new CorpseLedger(2);
        l.restore(List.of(f("a", 1, false), f("b", 2, false), f("c", 3, false)));
        assertEquals(2, l.size());
        assertEquals("b", l.entries().get(0).type());
    }
}
