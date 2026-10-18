package com.newuniverse.nusmp.heroweapon.weapon.glassworld;

import java.util.HashMap;
import java.util.Map;

/**
 * Why: several techniques of the Fan, the Scythe, the Katana and the Book work for a while and then stop (a veil of mist for four seconds, a floating scythe
 * for six, a parry window of fourteen ticks, a marked foe that must not be marked again for two seconds). Each is "this key is active until game tick T", and the
 * rule that matters is the same everywhere: it must be bounded (a table that only ever grows is a leak), it must answer by the game clock and not by a counter
 * that a lag spike could skip, and it must forget when asked. This is that table, with no game imports so it is tested on its own. The key is anything with a
 * proper equals (a player's UUID, a foe's UUID); the clock is the level's game time.
 */
public final class Expiry<K> {
    private final Map<K, Long> until = new HashMap<>();
    private final int cap;

    /** @param cap the most keys held at once; the key that expires first is dropped when a new one would pass it */
    public Expiry(int cap) {
        this.cap = Math.max(1, cap);
    }

    /** Makes {@code key} active until game tick {@code untilTick} (exclusive), replacing any time held. */
    public void set(K key, long untilTick) {
        if (key == null) return;
        if (!until.containsKey(key) && until.size() >= cap) evictOne();
        until.put(key, untilTick);
    }

    /** Makes {@code key} active until at least {@code untilTick}: a later time already held stays. */
    public void extend(K key, long untilTick) {
        if (key == null) return;
        Long held = until.get(key);
        set(key, held == null ? untilTick : Math.max(held, untilTick));
    }

    /** True while {@code now < until}. */
    public boolean active(K key, long now) {
        Long t = until.get(key);
        return t != null && now < t;
    }

    /** Ticks left (0 when it is not active). */
    public long remaining(K key, long now) {
        Long t = until.get(key);
        return t == null ? 0 : Math.max(0, t - now);
    }

    public void clear(K key) { until.remove(key); }

    public void clearAll() { until.clear(); }

    /** Drops every key whose time has passed; returns how many were dropped. */
    public int prune(long now) {
        int before = until.size();
        until.values().removeIf(t -> now >= t);
        return before - until.size();
    }

    public int size() { return until.size(); }

    private void evictOne() {
        K soonest = null;
        long best = Long.MAX_VALUE;
        for (Map.Entry<K, Long> e : until.entrySet()) {
            if (e.getValue() < best) {
                best = e.getValue();
                soonest = e.getKey();
            }
        }
        if (soonest != null) until.remove(soonest);
    }
}
