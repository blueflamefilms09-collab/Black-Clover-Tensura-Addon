package com.newuniverse.nusmp.skilltree.loot;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: the owner's rule is "without building a list per drop (cache by registry version)". {@link VersionedCache} is that cache, keyed by the IDENTITY of the registry's immutable snapshot, so
 * the promise to pin is: the builder runs once per version however many drops read it, a new snapshot object rebuilds on the very next read, going back to an older object also rebuilds (the
 * cache holds only the newest, it never serves a stale value), two equal-but-distinct snapshots are two versions (identity, not equals), and clearing forgets.
 */
class VersionedCacheTest {
    @Test
    void theBuilderRunsOncePerVersion() {
        VersionedCache<List<Integer>, Integer> cache = new VersionedCache<>(List::size);
        List<Integer> v1 = new ArrayList<>(List.of(1, 2, 3));
        for (int i = 0; i < 10_000; i++) assertEquals(3, (int) cache.get(v1));
        assertEquals(1, cache.builds(), "10,000 drops on one registry version must build the table once");
    }

    @Test
    void aNewSnapshotRebuildsAtOnce() {
        VersionedCache<List<Integer>, Integer> cache = new VersionedCache<>(List::size);
        List<Integer> v1 = List.of(1, 2, 3), v2 = List.of(1, 2, 3, 4);
        assertEquals(3, (int) cache.get(v1));
        assertEquals(4, (int) cache.get(v2));
        assertEquals(4, (int) cache.get(v2));
        assertEquals(2, cache.builds());
        assertEquals(3, (int) cache.get(v1), "back to the older snapshot: never a stale value");
        assertEquals(3, cache.builds());
    }

    @Test
    void identityNotEquality() {
        VersionedCache<List<Integer>, Integer> cache = new VersionedCache<>(List::size);
        List<Integer> a = new ArrayList<>(List.of(1)), b = new ArrayList<>(List.of(1));
        assertEquals(a, b);
        cache.get(a);
        cache.get(b);
        assertEquals(2, cache.builds(), "two snapshots are two versions even if they are equal: a reload that changes nothing costs one rebuild, a change can never be missed");
    }

    @Test
    void clearForgetsAndANullSourceIsAVersionToo() {
        VersionedCache<String, Integer> cache = new VersionedCache<>(s -> s == null ? -1 : s.length());
        assertEquals(-1, (int) cache.get(null));
        assertEquals(-1, (int) cache.get(null));
        assertEquals(1, cache.builds());
        String s = "abc";
        cache.get(s);
        cache.clear();
        cache.get(s);
        assertEquals(3, cache.builds());
    }
}
