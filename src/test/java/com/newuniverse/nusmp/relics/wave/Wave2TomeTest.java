package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/** The Tome of "Wait, That's Illegal": the d20 table and the ledger of failures. */
class Wave2TomeTest {
    @Test
    void theTableFollowsTheCard() {
        for (int r = 1; r <= 3; r++) assertEquals(Wave2Tome.Outcome.DENIED, Wave2Tome.outcome(r), "" + r);
        for (int r = 4; r <= 9; r++) assertEquals(Wave2Tome.Outcome.PENDING, Wave2Tome.outcome(r), "" + r);
        for (int r = 10; r <= 15; r++) assertEquals(Wave2Tome.Outcome.REROLL, Wave2Tome.outcome(r), "" + r);
        for (int r = 16; r <= 19; r++) assertEquals(Wave2Tome.Outcome.GRANTED, Wave2Tome.outcome(r), "" + r);
        assertEquals(Wave2Tome.Outcome.EXPEDITED, Wave2Tome.outcome(20));
        assertEquals(Wave2Tome.Outcome.DENIED, Wave2Tome.outcome(-4), "clamped");
        assertEquals(Wave2Tome.Outcome.EXPEDITED, Wave2Tome.outcome(400), "clamped");
    }

    @Test
    void advantageNeverRollsWorseOnAverage() {
        Random plain = new Random(1), adv = new Random(1);
        long a = 0, b = 0;
        int n = 20000;
        for (int i = 0; i < n; i++) {
            int x = Wave2Tome.d20(plain::nextInt, false), y = Wave2Tome.d20(adv::nextInt, true);
            assertTrue(x >= 1 && x <= 20 && y >= 1 && y <= 20);
            a += x;
            b += y;
        }
        assertEquals(10.5, a / (double) n, 0.2);
        assertEquals(13.8, b / (double) n, 0.25);
        assertTrue(b > a);
    }

    @Test
    void theAuditorIsFivePercent() {
        int hits = 0;
        for (int d = 1; d <= 100; d++) if (Wave2Tome.auditorNotices(d)) hits++;
        assertEquals(5, hits);
    }

    @Test
    void theImpNeverTakesTheLastHealth() {
        assertEquals(6f, Wave2Tome.fine(6, 20f));
        assertEquals(1f, Wave2Tome.fine(1, 20f));
        assertEquals(2f, Wave2Tome.fine(6, 3f));
        assertEquals(0f, Wave2Tome.fine(6, 1f));
        assertEquals(0f, Wave2Tome.fine(6, 0.5f));
        assertEquals(6f, Wave2Tome.fine(99, 20f), "clamped to a d6");
        assertTrue(Wave2Tome.rerollUndoes(0));
        assertFalse(Wave2Tome.rerollUndoes(1));
    }

    @Test
    void theLedgerKeepsTheLatestFailurePerPlayer() {
        Wave2Tome.Ledger l = new Wave2Tome.Ledger();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID(), src = UUID.randomUUID();
        l.record(a, "minecraft:poison", 0, 200, 100, src);
        l.record(a, "minecraft:slowness", 1, 100, 150, null);
        l.record(b, "minecraft:weakness", 0, 300, 120, null);
        assertEquals(2, l.size(), "one entry per player");
        Wave2Tome.Failure f = l.latest(List.of(a, b), 160, 200);
        assertEquals("minecraft:slowness", f.effectId());
        assertEquals(a, f.target());
        assertEquals("minecraft:weakness", l.latest(List.of(b), 160, 200).effectId());
        assertNull(l.latest(List.of(UUID.randomUUID()), 160, 200), "nobody else");
        assertNull(l.latest(List.of(a), 400, 200), "too old");
        assertNull(l.latest(List.of(a), 100, 200), "from the future");
        l.remove(f);
        assertNull(l.latest(List.of(a), 160, 200), "appealed failures are gone");
        assertNotNull(l.latest(List.of(b), 160, 200));
    }

    @Test
    void theLedgerIsBoundedAndPrunes() {
        Wave2Tome.Ledger l = new Wave2Tome.Ledger();
        for (int i = 0; i < 200; i++) l.record(UUID.randomUUID(), "minecraft:poison", 0, 20, i, null);
        assertEquals(Wave2Tome.Ledger.CAPACITY, l.size());
        l.prune(199, 10);
        assertTrue(l.size() <= 11, "only the last ten ticks remain: " + l.size());
        l.record(null, "x", 0, 1, 1, null);
        l.record(UUID.randomUUID(), null, 0, 1, 1, null);
        assertTrue(l.size() <= 11, "nulls are ignored");
        l.clear();
        assertEquals(0, l.size());
    }
}
