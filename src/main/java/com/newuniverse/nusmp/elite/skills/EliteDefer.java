package com.newuniverse.nusmp.elite.skills;

import com.mojang.logging.LogUtils;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Why: a few elite effects must happen just AFTER the blow that caused them, not inside it: Void Edge's extra damage cannot be dealt from inside the damage event of the blow it rides on (the
 * target's invulnerability frames would swallow one of the two hits), so it is queued for the next tick. The shared zone scheduler of the rebuilt skills holds at most four entries per owner
 * and is meant for lingering zones, so this is a separate, tiny, in-memory queue run once per server tick; a task that throws is logged and dropped, and the queue is capped so a hit-spamming
 * player cannot grow it without bound. Nothing survives a restart, which is right for something one tick long.
 */
public final class EliteDefer {
    private EliteDefer() {}

    private static final Logger LOG = LogUtils.getLogger();
    static final int MAX_QUEUED = 256;

    private record Task(long due, Runnable run) {}

    private static final List<Task> QUEUE = new ArrayList<>();
    private static long now;

    /** Runs {@code run} after {@code ticks} server ticks (at least 1). Returns false (and drops the task) if the queue is full. */
    public static synchronized boolean later(int ticks, Runnable run) {
        if (run == null || QUEUE.size() >= MAX_QUEUED) return false;
        QUEUE.add(new Task(now + Math.max(1, ticks), run));
        return true;
    }

    /** Once per server tick. */
    public static synchronized void tick() {
        now++;
        if (QUEUE.isEmpty()) return;
        List<Runnable> due = new ArrayList<>();
        for (Iterator<Task> it = QUEUE.iterator(); it.hasNext(); ) {
            Task t = it.next();
            if (t.due() <= now) { due.add(t.run()); it.remove(); }
        }
        for (Runnable r : due) {
            try {
                r.run();
            } catch (RuntimeException | LinkageError e) {
                LOG.warn("[nusmp] An elite deferred task failed", e);
            }
        }
    }

    public static synchronized int pending() { return QUEUE.size(); }

    public static synchronized void clear() { QUEUE.clear(); }
}
