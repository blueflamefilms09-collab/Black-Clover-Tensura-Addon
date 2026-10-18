package com.newuniverse.nusmp.skilltree.ft.kit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

/**
 * Why: several Fairy Tail techniques are not one instant (a hammer that is shown before it falls, a tornado that walks forward, a floor that bites every half second, a
 * Secret Art that winds up, pulses and bursts), and the server must run the second half of them some ticks after the key was released without a thread, a sleeping
 * task or an entity per effect. This is the whole timing rule in one pure class (no Minecraft type, so a test can run it): a list of tasks ordered by the game tick at which
 * they are due, earliest first, and tasks due on the same tick in the order they were added, so a windup always runs before the strike that was scheduled after it. It has a
 * hard size limit because a technique that schedules from a loop must never be able to grow the server's memory: when the queue is full the new task is refused and the
 * caller drops the effect, which is the safe direction (an effect that does not happen) and not the unsafe one (a lag spike). It is generic over the task type so the
 * glue ({@link FtClock}) stores {@code Runnable}s while the tests store plain strings. Server thread only; it keeps no reference to a world.
 */
public final class FtTaskQueue<T> {
    private record Entry<T>(long due, long seq, T task) {}

    private final PriorityQueue<Entry<T>> heap = new PriorityQueue<>(Comparator.comparingLong((Entry<T> e) -> e.due()).thenComparingLong(e -> e.seq()));
    private final int cap;
    private long seq;

    /** A queue that refuses more than {@code cap} waiting tasks (at least 1). */
    public FtTaskQueue(int cap) { this.cap = Math.max(1, cap); }

    /** Adds a task due at game tick {@code due}; false (and nothing added) when the task is null or the queue is full. */
    public boolean add(long due, T task) {
        if (task == null || heap.size() >= cap) return false;
        heap.add(new Entry<>(due, seq++, task));
        return true;
    }

    /** Removes and returns every task due at or before {@code now}: earliest first, equal ticks in the order they were added. */
    public List<T> popDue(long now) {
        List<T> out = new ArrayList<>();
        while (!heap.isEmpty() && heap.peek().due() <= now) out.add(heap.poll().task());
        return out;
    }

    public int size() { return heap.size(); }

    public boolean isEmpty() { return heap.isEmpty(); }

    /** Forgets every waiting task (the server stopped: effects of a world that is gone must not run in the next one). */
    public void clear() { heap.clear(); }
}
