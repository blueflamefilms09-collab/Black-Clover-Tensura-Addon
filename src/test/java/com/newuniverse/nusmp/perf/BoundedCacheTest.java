package com.newuniverse.nusmp.perf;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: every cache of a per-frame lookup in this pass is a {@link BoundedCache}. This pins the three promises that make it safe to put in front of an expression: the same key gives back the
 * SAME object (so identity-compared and hashed users see no change), the builder runs once per key, and a key set that is not really finite cannot grow it past its bound.
 */
class BoundedCacheTest {
    @Test
    void theSameKeyGivesTheSameObjectAndTheBuilderRunsOnce() {
        BoundedCache<String, String> c = new BoundedCache<>(100);
        AtomicInteger built = new AtomicInteger();
        String first = c.get("a", k -> {
            built.incrementAndGet();
            return new String("value-" + k);
        });
        for (int i = 0; i < 50; i++) assertSame(first, c.get("a", k -> {
            built.incrementAndGet();
            return "other";
        }));
        assertEquals(1, built.get());
        assertEquals(50L, c.hits());
        assertEquals(1L, c.misses());
    }

    @Test
    void differentKeysGiveDifferentValues() {
        BoundedCache<Integer, String> c = new BoundedCache<>(100);
        assertEquals("v1", c.get(1, k -> "v" + k));
        assertEquals("v2", c.get(2, k -> "v" + k));
        assertEquals(2, c.size());
    }

    @Test
    void aKeySetThatIsNotFiniteCannotGrowItWithoutEnd() {
        BoundedCache<Integer, Integer> c = new BoundedCache<>(10);
        for (int i = 0; i < 1000; i++) assertEquals(Integer.valueOf(i), c.get(i, k -> k));
        assertTrue(c.size() <= 10, "size " + c.size());
    }

    @Test
    void clearForgetsEverything() {
        BoundedCache<String, Object> c = new BoundedCache<>(10);
        Object a = c.get("k", k -> new Object());
        c.clear();
        assertEquals(0, c.size());
        Object b = c.get("k", k -> new Object());
        assertTrue(a != b);
    }
}
