package com.newuniverse.nusmp.perf;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

/**
 * Why: a lot of per-frame and per-tick code builds the same small immutable thing again and again from the same key (a texture id from a name, a model spec from a mob id): a string
 * concatenation, a validation, an allocation, a hash, each time. This is the one shared way to build it once: a lookup that returns the stored value, and builds and stores it on the first
 * miss. It is bounded: a key set that is not really finite (a name with a counter in it) cannot grow it without end, because when it holds more than {@code max} entries it is emptied and
 * starts again (the values are cheap to rebuild, so a rare flush costs nothing). Thread-safe (a concurrent map), pure (no game classes). The builder must not return null.
 */
public final class BoundedCache<K, V> {
    private final ConcurrentHashMap<K, V> map = new ConcurrentHashMap<>();
    private final int max;
    private long hits, misses;                       // statistics only: a racy increment may lose a count, which does not matter, and a volatile write per frame lookup would cost more than the lookup

    public BoundedCache(int max) { this.max = Math.max(1, max); }

    /** The value stored for {@code key}, built by {@code builder} on the first look. */
    public V get(K key, Function<K, V> builder) {
        V v = map.get(key);
        if (v != null) {
            hits++;
            return v;
        }
        misses++;
        v = builder.apply(key);
        if (map.size() >= max) map.clear();
        V raced = map.putIfAbsent(key, v);
        return raced != null ? raced : v;
    }

    public int size() { return map.size(); }

    public long hits() { return hits; }

    public long misses() { return misses; }

    public void clear() { map.clear(); }
}
