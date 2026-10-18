package com.newuniverse.nusmp.heroweapon.weapon.kit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Why: several techniques leave something behind that must fade by itself (a mark on a foe, a prism in the air, a buff on the wielder, a pinned target) and a
 * server must never accumulate such things without end: a mark on a mob that left the world, a player who logged out, a chunk that unloaded. This is the one
 * small table all of them use: a key (usually a UUID) maps to the game time at which it stops being active, the table has a hard size cap (when it is full
 * the entry that would expire soonest is dropped to make room), expired entries are pruned on every write, and nothing in it needs the world. It is plain
 * Java so a test can pin the edges (an entry is active strictly before its end time, a full table never grows, a repeated key keeps the later end).
 * Server thread only, like every caller.
 */
public final class ExpiryTable<K> {
    private final Map<K, Long> until = new HashMap<>();
    private final int cap;

    /** A table that never holds more than {@code cap} entries (at least 1). */
    public ExpiryTable(int cap) { this.cap = Math.max(1, cap); }

    /** Marks {@code key} active until the game time {@code untilTick} (exclusive). A repeated key keeps the later end. */
    public void put(K key, long untilTick, long now) {
        if (key == null) return;
        prune(now);
        Long old = until.get(key);
        if (old != null) {
            until.put(key, Math.max(old, untilTick));
            return;
        }
        if (until.size() >= cap) dropSoonest();
        until.put(key, untilTick);
    }

    /** True while the game time is before the entry's end. */
    public boolean active(K key, long now) {
        Long end = until.get(key);
        return end != null && end > now;
    }

    /** Ticks left on the entry, 0 when absent or over. */
    public long remaining(K key, long now) {
        Long end = until.get(key);
        return end == null ? 0L : Math.max(0L, end - now);
    }

    /** Forgets one key. */
    public void remove(K key) { until.remove(key); }

    /** Forgets every entry that is over; returns how many went. */
    public int prune(long now) {
        int n = 0;
        for (Iterator<Map.Entry<K, Long>> it = until.entrySet().iterator(); it.hasNext(); ) {
            if (it.next().getValue() <= now) {
                it.remove();
                n++;
            }
        }
        return n;
    }

    /** How many entries the table holds (some may be over but not yet pruned). */
    public int size() { return until.size(); }

    /** The keys that are active now (a copy). */
    public List<K> activeKeys(long now) {
        List<K> out = new ArrayList<>();
        for (Map.Entry<K, Long> e : until.entrySet()) if (e.getValue() > now) out.add(e.getKey());
        return out;
    }

    public void clear() { until.clear(); }

    private void dropSoonest() {
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
