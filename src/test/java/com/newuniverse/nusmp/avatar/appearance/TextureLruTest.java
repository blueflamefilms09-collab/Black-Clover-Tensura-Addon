package com.newuniverse.nusmp.avatar.appearance;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The texture cache's eviction: least recently used goes first, every evicted value is handed back exactly once (so its texture is released). */
class TextureLruTest {
    @Test
    void theLeastRecentlyUsedIsEvictedAndReported() {
        List<String> gone = new ArrayList<>();
        TextureLru<Integer, String> c = new TextureLru<>(3, (k, v) -> gone.add(v));
        c.put(1, "a"); c.put(2, "b"); c.put(3, "c");
        assertEquals("a", c.get(1));                 // 1 is now the freshest
        c.put(4, "d");
        assertEquals(List.of("b"), gone);
        assertNull(c.get(2));
        assertEquals(3, c.size());
    }

    @Test
    void replacingAKeyReleasesTheOldValue() {
        List<String> gone = new ArrayList<>();
        TextureLru<Integer, String> c = new TextureLru<>(2, (k, v) -> gone.add(v));
        c.put(1, "a"); c.put(1, "a2");
        assertEquals(List.of("a"), gone);
        assertEquals("a2", c.get(1));
    }

    @Test
    void clearReleasesEverything() {
        List<String> gone = new ArrayList<>();
        TextureLru<Integer, String> c = new TextureLru<>(5, (k, v) -> gone.add(v));
        for (int i = 0; i < 4; i++) c.put(i, "v" + i);
        c.clear();
        assertEquals(4, gone.size());
        assertEquals(0, c.size());
    }

    @Test
    void neverHoldsMoreThanTheCapacity() {
        int[] evicted = {0};
        TextureLru<Integer, Integer> c = new TextureLru<>(64, (k, v) -> evicted[0]++);
        for (int i = 0; i < 1000; i++) { c.put(i, i); assertTrue(c.size() <= 64); }
        assertEquals(1000 - 64, evicted[0]);
    }
}
