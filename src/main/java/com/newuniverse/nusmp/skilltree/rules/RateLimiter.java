package com.newuniverse.nusmp.skilltree.rules;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Map;

/**
 * Why: a purchase packet comes from the client, and a modified client can send thousands of them in one tick (each one re-checked on the server, so the damage is only CPU and log spam, but a
 * server owner is entitled to a cap). The owner's design says "a rate limit of 10 purchases per second", so this is a sliding window over the SERVER'S OWN TICK COUNT: not the wall clock, so
 * it behaves the same on a lagging server and in a unit test, and a burst inside one tick is counted packet by packet. Attempts that are refused are not recorded, so a flood is held at the
 * cap and never extends its own lock-out. Pure (the key is any object, in practice a player's UUID); the packet handler owns the one instance and clears it on logout and server stop, which is
 * what keeps this static state from outliving a server.
 *
 * @param <K> the key the limit is counted per
 */
public final class RateLimiter<K> {
    /** One second of game time. */
    public static final long WINDOW_TICKS = 20;

    private final Map<K, ArrayDeque<Long>> stamps = new HashMap<>();

    /**
     * Asks for one action at server tick {@code nowTick}. True (and the action is recorded) when fewer than {@code perSecond} actions of this key happened in the last
     * {@link #WINDOW_TICKS} ticks; false (nothing recorded) otherwise. A {@code perSecond} below 1 is treated as 1.
     */
    public boolean allow(K key, long nowTick, int perSecond) {
        int cap = Math.max(1, perSecond);
        ArrayDeque<Long> q = stamps.computeIfAbsent(key, k -> new ArrayDeque<>());
        while (!q.isEmpty() && (nowTick - q.peekFirst() >= WINDOW_TICKS || q.peekFirst() > nowTick)) q.pollFirst();      // old, or from a clock that has since been reset
        if (q.size() >= cap) return false;
        q.addLast(nowTick);
        return true;
    }

    public void forget(K key) { stamps.remove(key); }

    public void clear() { stamps.clear(); }

    /** How many keys are being counted (for the tests: the map must not grow without bound). */
    public int size() { return stamps.size(); }
}
