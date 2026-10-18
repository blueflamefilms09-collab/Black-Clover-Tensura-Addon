package com.newuniverse.nusmp.skilltree.ft.kit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Why: the timing rule of every delayed Fairy Tail technique is this one queue, and its three promises are what make a wind-up safe: nothing runs before its tick, tasks run earliest first and equal
 * ticks in the order they were added (so a windup scheduled before a strike always runs before it), and a full queue refuses a new task instead of growing. Each is pinned here without a server.
 */
class FtTaskQueueTest {
    @Test
    void tasksComeOutEarliestFirstAndEqualTicksInTheOrderAdded() {
        FtTaskQueue<String> q = new FtTaskQueue<>(10);
        q.add(30, "c");
        q.add(10, "a1");
        q.add(20, "b");
        q.add(10, "a2");
        List<String> due = q.popDue(25);
        assertEquals(List.of("a1", "a2", "b"), due, "earliest first, a tie keeps the order of adding");
        assertEquals(1, q.size());
        assertEquals(List.of("c"), q.popDue(30));
        assertTrue(q.isEmpty());
    }

    @Test
    void nothingRunsBeforeItsTick() {
        FtTaskQueue<String> q = new FtTaskQueue<>(10);
        q.add(100, "later");
        assertTrue(q.popDue(99).isEmpty(), "one tick early is too early");
        assertEquals(1, q.size(), "an early look must not remove the task");
        assertEquals(List.of("later"), q.popDue(100));
    }

    @Test
    void aFullQueueRefusesAndANullTaskIsIgnored() {
        FtTaskQueue<String> q = new FtTaskQueue<>(2);
        assertTrue(q.add(1, "x"));
        assertTrue(q.add(1, "y"));
        assertFalse(q.add(1, "z"), "a third task does not fit");
        assertFalse(q.add(1, null), "a null task is never stored");
        assertEquals(2, q.size());
        q.popDue(1);
        assertTrue(q.add(2, "again"), "room is free again once tasks have run");
    }

    @Test
    void clearForgetsEverything() {
        FtTaskQueue<String> q = new FtTaskQueue<>(5);
        q.add(1, "x");
        q.add(2, "y");
        q.clear();
        assertTrue(q.isEmpty());
        assertTrue(q.popDue(Long.MAX_VALUE).isEmpty());
    }

    @Test
    void aTaskAddedWhileOthersRunIsNotRunInTheSameDrain() {
        FtTaskQueue<String> q = new FtTaskQueue<>(5);
        q.add(5, "first");
        List<String> now = q.popDue(5);
        q.add(5, "added during the run");
        assertEquals(List.of("first"), now, "the drain already took its list");
        assertEquals(List.of("added during the run"), q.popDue(6), "the late one runs on the next drain");
    }
}
