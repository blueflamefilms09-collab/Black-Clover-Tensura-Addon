package com.newuniverse.nusmp.skilltree.ft.dragon;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Why: the delayed follow-ups of the Dragon Slayer techniques (the inhale before a roar, the pulses of the Secret Art, the bolts of Thunder Palace) stand on this queue, so its
 * promises are pinned without a server: nothing comes out before its tick, what is due comes out earliest first and in the order it was added when the ticks are equal, a
 * drained item does not come out twice, a full queue refuses instead of growing or throwing, and a null item is never queued.
 */
class DelayQueueTest {
    @Test
    void nothingComesOutEarlyAndEverythingComesOutOnce() {
        DelayQueue<String> q = new DelayQueue<>(10);
        assertTrue(q.add(100, "a"));
        assertTrue(q.add(105, "b"));
        assertEquals(List.of(), q.drainDue(99));
        assertEquals(List.of("a"), q.drainDue(100));
        assertEquals(List.of(), q.drainDue(100), "a drained item does not come out twice");
        assertEquals(1, q.size());
        assertEquals(List.of("b"), q.drainDue(500));
        assertEquals(0, q.size());
    }

    @Test
    void dueItemsComeOutEarliestFirstAndTiesKeepTheirOrder() {
        DelayQueue<String> q = new DelayQueue<>(10);
        q.add(30, "late");
        q.add(10, "first-tie-1");
        q.add(20, "middle");
        q.add(10, "first-tie-2");
        assertEquals(List.of("first-tie-1", "first-tie-2", "middle", "late"), q.drainDue(30));
    }

    @Test
    void aFullQueueRefusesAndANullIsNeverQueued() {
        DelayQueue<String> q = new DelayQueue<>(2);
        assertTrue(q.add(1, "a"));
        assertTrue(q.add(1, "b"));
        assertFalse(q.add(1, "c"), "the third item does not fit");
        assertFalse(new DelayQueue<String>(5).add(1, null));
        assertEquals(2, q.size());
        q.drainDue(1);
        assertTrue(q.add(2, "c"), "room again once the queue was drained");
    }

    @Test
    void clearForgetsEverything() {
        DelayQueue<String> q = new DelayQueue<>(5);
        q.add(1, "a");
        q.add(2, "b");
        q.clear();
        assertEquals(0, q.size());
        assertEquals(List.of(), q.drainDue(1000));
    }
}
