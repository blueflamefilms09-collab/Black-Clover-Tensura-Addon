package com.newuniverse.nusmp.relics.wave;

import java.util.ArrayList;
import java.util.List;

/**
 * Wave 18: a small bounded list of "do this at tick X" jobs for the telegraphed things (a bag's pulses, the Elephant's room, a flood of kittens).
 * Pure Java. A job holds ids, never entities; a full queue refuses a new job; at most {@link #MAX_PER_TICK} jobs run per call, and a job that throws
 * is dropped without stopping the others.
 */
final class Wave18Queue {
    /** Jobs waiting at most, and jobs run per call at most. */
    static final int CAPACITY = 192, MAX_PER_TICK = 24;

    private record Job(long due, Runnable run) {}

    private final List<Job> jobs = new ArrayList<>();

    /** Queues a job; false (and nothing queued) when the queue is full. */
    synchronized boolean at(long dueTick, Runnable run) {
        if (run == null || jobs.size() >= CAPACITY) return false;
        jobs.add(new Job(dueTick, run));
        return true;
    }

    int size() { synchronized (this) { return jobs.size(); } }

    synchronized void clear() { jobs.clear(); }

    /** Runs the jobs that are due at {@code now} (oldest due first), at most {@link #MAX_PER_TICK}; returns how many ran. */
    int tick(long now) {
        List<Job> due = new ArrayList<>();
        synchronized (this) {
            if (jobs.isEmpty()) return 0;
            for (Job j : jobs) if (j.due() <= now) due.add(j);
            due.sort((a, b) -> Long.compare(a.due(), b.due()));
            if (due.size() > MAX_PER_TICK) due = new ArrayList<>(due.subList(0, MAX_PER_TICK));
            jobs.removeAll(due);
        }
        int ran = 0;
        for (Job j : due) {
            try {
                j.run().run();
            } catch (RuntimeException failed) {
                // a job whose target went away is dropped
            }
            ran++;
        }
        return ran;
    }
}
