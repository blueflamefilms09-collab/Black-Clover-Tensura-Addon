package com.newuniverse.nusmp.entity.zogratis;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The block ledger's core: bounded, the first old value wins, oldest first, nothing lost. */
class LedgerQueueTest {
    @Test
    void firstOldValueWinsAndPlacedFollows() {
        LedgerQueue<Long, String> q = new LedgerQueue<>(10);
        assertTrue(q.record(1L, "stone", "air"));
        assertTrue(q.record(1L, "air", "bone"), "the same block changed again");
        assertEquals(1, q.size());
        LedgerQueue.Entry<Long, String> e = q.takeAll().get(0);
        assertEquals("stone", e.old(), "back to what it was before the fight");
        assertEquals("bone", e.placed());
        assertTrue(q.isEmpty());
    }

    @Test
    void isBoundedAndRefusesNewKeysWhenFull() {
        LedgerQueue<Long, String> q = new LedgerQueue<>(3);
        assertTrue(q.record(1L, "a", "x"));
        assertTrue(q.record(2L, "b", "x"));
        assertTrue(q.record(3L, "c", "x"));
        assertTrue(q.isFull());
        assertFalse(q.record(4L, "d", "x"), "a full ledger means: do not make the change");
        assertEquals(3, q.size());
        assertTrue(q.record(2L, "x", "y"), "a key already in the ledger can still change");
        assertFalse(q.contains(4L));
    }

    @Test
    void takesOldestFirstInBatches() {
        LedgerQueue<Long, Integer> q = new LedgerQueue<>(100);
        for (long i = 0; i < 10; i++) q.record(i, (int) i, -1);
        List<LedgerQueue.Entry<Long, Integer>> first = q.take(4);
        assertEquals(List.of(0L, 1L, 2L, 3L), first.stream().map(LedgerQueue.Entry::key).toList());
        assertEquals(6, q.size());
        List<LedgerQueue.Entry<Long, Integer>> rest = q.takeAll();
        assertEquals(6, rest.size());
        assertEquals(4L, rest.get(0).key());
        assertEquals(9L, rest.get(5).key());
        assertTrue(q.take(5).isEmpty());
    }

    @Test
    void putBackKeepsAnEntryForLater() {
        LedgerQueue<Long, String> q = new LedgerQueue<>(5);
        q.record(1L, "a", "x");
        q.record(2L, "b", "x");
        var batch = q.take(2);
        q.putBack(batch.get(0));
        assertEquals(1, q.size());
        assertTrue(q.contains(1L));
        q.putBack(batch.get(0));
        assertEquals(1, q.size(), "putting back twice does not duplicate");
        assertEquals(List.of(1L, 2L), batch.stream().map(LedgerQueue.Entry::key).toList());
    }

    @Test
    void everyRecordedChangeComesBack() {
        LedgerQueue<Long, Integer> q = new LedgerQueue<>(500);
        int recorded = 0;
        for (long i = 0; i < 1000; i++) if (q.record(i % 700, (int) i, 0)) recorded++;
        assertEquals(500, q.size());
        int back = 0;
        while (!q.isEmpty()) back += q.take(64).size();
        assertEquals(500, back);
        assertTrue(recorded >= 500);
    }

    @Test
    void takeWhereSkipsWhatIsNotReadyAndKeepsIt() {
        LedgerQueue<Long, Integer> q = new LedgerQueue<>(100);
        for (long i = 0; i < 10; i++) q.record(i, (int) i, -1);
        var odd = q.takeWhere(k -> k % 2 == 1, 3);
        assertEquals(List.of(1L, 3L, 5L), odd.stream().map(LedgerQueue.Entry::key).toList());
        assertEquals(7, q.size(), "the evens and the odd ones past the limit stay");
        assertTrue(q.contains(0L) && q.contains(7L) && q.contains(9L));
        assertTrue(q.takeWhere(k -> false, 10).isEmpty());
        assertEquals(7, q.size(), "nothing ready: nothing lost");
        assertEquals(7, q.takeWhere(k -> true, 100).size());
        assertTrue(q.isEmpty());
    }

    @Test
    void snapshotDoesNotDrain() {
        LedgerQueue<Long, String> q = new LedgerQueue<>(5);
        q.record(1L, "a", "x");
        assertEquals(1, q.snapshot().size());
        assertEquals(1, q.size());
        q.clear();
        assertTrue(q.isEmpty());
        assertEquals(5, q.capacity());
    }
}
