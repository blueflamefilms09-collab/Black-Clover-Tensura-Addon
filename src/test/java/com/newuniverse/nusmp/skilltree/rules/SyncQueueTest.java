package com.newuniverse.nusmp.skilltree.rules;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the promise to the client is "one snapshot per tick per player, and always the last state". That depends on three small behaviours that are easy to get wrong and invisible when they
 * are: marking the same player many times queues it once; draining returns the players in the order they were first marked and leaves the queue empty; and a mark made WHILE the drained list is
 * being sent goes to the next drain instead of being lost or looping forever. They are proved here without a server.
 */
class SyncQueueTest {
    @Test
    void manyMarksOfOnePlayerAreOneSend() {
        SyncQueue<String> q = new SyncQueue<>();
        assertTrue(q.mark("a"));
        assertFalse(q.mark("a"), "already queued");
        assertFalse(q.mark("a"));
        assertEquals(1, q.size());
        assertEquals(List.of("a"), q.drain());
    }

    @Test
    void drainGivesTheFirstMarkedFirstAndEmptiesTheQueue() {
        SyncQueue<String> q = new SyncQueue<>();
        q.mark("c");
        q.mark("a");
        q.mark("b");
        q.mark("a");
        assertEquals(List.of("c", "a", "b"), q.drain());
        assertTrue(q.isEmpty());
        assertTrue(q.drain().isEmpty(), "a second drain in the same tick finds nothing");
    }

    @Test
    void aMarkMadeWhileSendingLandsInTheNextDrain() {
        SyncQueue<String> q = new SyncQueue<>();
        q.mark("a");
        q.mark("b");
        List<String> sent = new ArrayList<>();
        for (String k : q.drain()) {
            sent.add(k);
            q.mark(k);                                         // the send touched the player again (a skill was learned, say)
            q.mark("late");
        }
        assertEquals(List.of("a", "b"), sent, "the loop did not run over the new marks");
        assertEquals(List.of("a", "late", "b"), q.drain(), "they are all sent in the next flush, once each, in the order they were first marked");
    }

    @Test
    void forgetAndClearAndNullAreSafe() {
        SyncQueue<String> q = new SyncQueue<>();
        assertFalse(q.mark(null));
        q.mark("a");
        q.mark("b");
        q.forget("a");
        q.forget("never queued");
        assertEquals(List.of("b"), q.drain());
        q.mark("c");
        q.clear();
        assertTrue(q.isEmpty());
    }
}
