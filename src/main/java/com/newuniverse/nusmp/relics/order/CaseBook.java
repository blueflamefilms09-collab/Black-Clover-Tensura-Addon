package com.newuniverse.nusmp.relics.order;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * The waiting list of the Order (pure, unit tested): one entry per relic id, oldest first, a bounded size. The Order handles one case at a time;
 * every duplicate that is found while it is busy or while the relic's cooldown runs waits here (or is simply found again by the next scan).
 */
public final class CaseBook<T> {
    private final int capacity;
    private final LinkedHashMap<String, T> waiting = new LinkedHashMap<>();

    public CaseBook(int capacity) { this.capacity = Math.max(1, capacity); }

    /**
     * Puts a relic id on the list. 'allowed' is false while the relic is cooling down: then nothing is stored (the next scan finds the duplicate
     * again once the cooldown is over). A relic already waiting keeps its place and takes the newer details. False if nothing was stored.
     */
    public boolean offer(String relicId, T what, boolean allowed) {
        if (relicId == null || relicId.isEmpty() || !allowed) return false;
        if (waiting.containsKey(relicId)) {
            waiting.put(relicId, what);
            return true;
        }
        if (waiting.size() >= capacity) return false;
        waiting.put(relicId, what);
        return true;
    }

    /** Removes and returns the oldest waiting entry whose relic id passes the test (its cooldown is over), or null. */
    public T poll(Predicate<String> ready) {
        for (Map.Entry<String, T> e : waiting.entrySet()) {
            if (ready.test(e.getKey())) {
                T v = e.getValue();
                waiting.remove(e.getKey());
                return v;
            }
        }
        return null;
    }

    public boolean waiting(String relicId) { return waiting.containsKey(relicId); }

    /** Drops a relic id from the list (its duplicate was dealt with some other way). */
    public T forget(String relicId) { return waiting.remove(relicId); }

    public int size() { return waiting.size(); }

    public void clear() { waiting.clear(); }

    public List<String> ids() { return new ArrayList<>(waiting.keySet()); }
}
