package com.newuniverse.nusmp.compat.maps;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Why: waypoint lines are printed by the server on its own (a rift opens, a gate is found), so they must never become spam: at most {@code max} lines per player in any
 * window of {@code windowTicks}, and a place is told once ({@link #remember}). PURE (a clock value is passed in), so it is unit-tested. Server thread only.
 */
public final class ShareLimiter {
    private final int max;
    private final long windowTicks;
    private final Map<UUID, Deque<Long>> sent = new HashMap<>();

    public ShareLimiter(int max, long windowTicks) {
        this.max = Math.max(1, max);
        this.windowTicks = Math.max(1L, windowTicks);
    }

    public int max() { return max; }

    /** Takes a slot for {@code who} at game time {@code now}; false (and nothing taken) when the window is full. A clock that went backwards (another world) clears that player's history. */
    public synchronized boolean tryAcquire(UUID who, long now) {
        Deque<Long> q = sent.computeIfAbsent(who, k -> new ArrayDeque<>());
        if (!q.isEmpty() && now < q.peekLast()) q.clear();
        while (!q.isEmpty() && now - q.peekFirst() >= windowTicks) q.pollFirst();
        if (q.size() >= max) return false;
        q.addLast(now);
        return true;
    }

    /** Would {@link #tryAcquire} succeed now? (Takes nothing.) */
    public synchronized boolean wouldAllow(UUID who, long now) {
        Deque<Long> q = sent.get(who);
        if (q == null || q.isEmpty() || now < q.peekLast()) return true;
        int live = 0;
        for (long t : q) if (now - t < windowTicks) live++;
        return live < max;
    }

    public synchronized void forget(UUID who) { sent.remove(who); }

    public synchronized void clear() { sent.clear(); }

    /**
     * Adds {@code key} to a player's list of places already told (oldest dropped past {@code cap}); true when it was NEW. Pure list logic: the caller stores the list
     * (the mod keeps it in the player's persistent data, so a place is told once per player for good).
     */
    public static boolean remember(List<String> known, String key, int cap) {
        if (key == null || known.contains(key)) return false;
        known.add(key);
        while (known.size() > Math.max(1, cap)) known.remove(0);
        return true;
    }
}
