package com.newuniverse.nusmp.relics.wave;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The bounded job queue behind the telegraphed pulses of the wave 18 charms. */
class Wave18QueueTest {
    @Test
    void jobsRunWhenDueOldestFirst() {
        Wave18Queue q = new Wave18Queue();
        List<Integer> ran = new ArrayList<>();
        q.at(30, () -> ran.add(3));
        q.at(10, () -> ran.add(1));
        q.at(20, () -> ran.add(2));
        assertEquals(0, q.tick(5));
        assertEquals(2, q.tick(25));
        assertEquals(List.of(1, 2), ran);
        assertEquals(1, q.size());
        assertEquals(1, q.tick(99));
        assertEquals(List.of(1, 2, 3), ran);
        assertEquals(0, q.size());
    }

    @Test
    void aFullQueueRefusesAndAFailingJobDoesNotStopTheOthers() {
        Wave18Queue q = new Wave18Queue();
        for (int i = 0; i < Wave18Queue.CAPACITY; i++) assertTrue(q.at(1, () -> { }));
        assertFalse(q.at(1, () -> { }), "a full queue refuses a new job");
        assertFalse(q.at(1, null));
        q.clear();
        assertEquals(0, q.size());
        List<Integer> ran = new ArrayList<>();
        q.at(1, () -> { throw new IllegalStateException("target gone"); });
        q.at(1, () -> ran.add(7));
        assertEquals(2, q.tick(1));
        assertEquals(List.of(7), ran);
    }

    @Test
    void atMostTheCapRunPerTickAndTheRestWait() {
        Wave18Queue q = new Wave18Queue();
        int[] n = {0};
        for (int i = 0; i < Wave18Queue.MAX_PER_TICK + 5; i++) q.at(1, () -> n[0]++);
        assertEquals(Wave18Queue.MAX_PER_TICK, q.tick(1));
        assertEquals(5, q.size());
        assertEquals(5, q.tick(2));
        assertEquals(Wave18Queue.MAX_PER_TICK + 5, n[0]);
    }

    @Test
    void aJobMayQueueItsNextPulse() {
        Wave18Queue q = new Wave18Queue();
        int[] pulses = {0};
        Runnable[] job = new Runnable[1];
        job[0] = () -> { if (++pulses[0] < 4) q.at(pulses[0] * 10L, job[0]); };
        q.at(0, job[0]);
        for (long t = 0; t < 60; t++) q.tick(t);
        assertEquals(4, pulses[0]);
        assertEquals(0, q.size());
    }
}
