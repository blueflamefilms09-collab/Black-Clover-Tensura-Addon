package com.newuniverse.nusmp.skilltree.loot;

import java.util.function.Function;

/**
 * Why: the cumulative weights of 283 lines must be built when the set of lines CHANGES (a datapack reload, a client sync), not on every drop, and the registry already has the right
 * notion of a version: {@code RouteRegistry} swaps one immutable {@code RouteIndex} snapshot as a whole, so "the snapshot is the same object" means "nothing changed". This keeps ONE
 * (key, value) pair in a volatile field and rebuilds only when the key is no longer the same object (identity, not equals: two equal snapshots cost one rebuild, a changed one can never
 * be missed). A race at most builds the value twice (both results are equal and immutable); no lock is taken on the loot path. Generic and pure so the "built once per version" promise is
 * unit tested with a counter instead of a registry.
 */
public final class VersionedCache<K, V> {
    private record Entry<K, V>(K key, V value) {}

    private volatile Entry<K, V> cached;
    private final Function<K, V> builder;
    private int builds;

    public VersionedCache(Function<K, V> builder) { this.builder = builder; }

    /** The value for this version of the source, building it only if the source object is not the one last built from. */
    public V get(K source) {
        Entry<K, V> c = cached;
        if (c != null && c.key == source) return c.value;
        V v = builder.apply(source);
        cached = new Entry<>(source, v);
        builds++;
        return v;
    }

    /** How many times the builder ran (for tests and the self-test). */
    public int builds() { return builds; }

    /** Forgets the value (the server stopped). */
    public void clear() { cached = null; }
}
