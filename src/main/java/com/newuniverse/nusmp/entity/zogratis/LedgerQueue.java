package com.newuniverse.nusmp.entity.zogratis;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The pure core of {@link BlockLedger}: a bounded list of changes, one entry per key, oldest first. The first 'old' value recorded
 * for a key is kept for good (so a block changed twice still goes back to what it was before the fight), 'placed' is the latest
 * value the boss put there (so a block someone else has since replaced can be left alone).
 */
public final class LedgerQueue<K, V> {
    public record Entry<K, V>(K key, V old, V placed) {}

    private final int capacity;
    private final Map<K, Entry<K, V>> entries = new LinkedHashMap<>();

    public LedgerQueue(int capacity) { this.capacity = Math.max(1, capacity); }

    /** Notes that 'key' was changed from 'old' to 'placed'. False if the ledger is full and the key is new (the change must not be made). */
    public boolean record(K key, V old, V placed) {
        Entry<K, V> e = entries.get(key);
        if (e != null) {
            entries.put(key, new Entry<>(key, e.old(), placed));
            return true;
        }
        if (entries.size() >= capacity) return false;
        entries.put(key, new Entry<>(key, old, placed));
        return true;
    }

    public boolean contains(K key) { return entries.containsKey(key); }
    public int size() { return entries.size(); }
    public boolean isEmpty() { return entries.isEmpty(); }
    public boolean isFull() { return entries.size() >= capacity; }
    public int capacity() { return capacity; }

    /** Removes and returns up to 'max' entries, oldest first. */
    public List<Entry<K, V>> take(int max) {
        List<Entry<K, V>> out = new ArrayList<>();
        var it = entries.values().iterator();
        while (it.hasNext() && out.size() < max) {
            out.add(it.next());
            it.remove();
        }
        return out;
    }

    public List<Entry<K, V>> takeAll() { return take(Integer.MAX_VALUE); }

    /** Puts an entry back at the end (a block in a chunk that was not loaded yet). */
    public void putBack(Entry<K, V> e) { entries.putIfAbsent(e.key(), e); }

    /** A copy, oldest first (for saving). */
    public List<Entry<K, V>> snapshot() { return new ArrayList<>(entries.values()); }

    public void clear() { entries.clear(); }
}
