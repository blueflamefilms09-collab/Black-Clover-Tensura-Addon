package com.newuniverse.nusmp.avatar.appearance;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Why: every distinct (body, skin tone, outfit) is a GPU texture, and a server full of Avatars with slider-picked colours would make thousands; this keeps
 * the most recently drawn N (64 in the renderer) and tells its owner when one falls out, so the owner can release the texture (no leak). Generic and
 * pure so the eviction order and the callback are tested without the game.
 */
public final class TextureLru<K, V> {
    private final int capacity;
    private final BiConsumer<K, V> onEvict;
    private final LinkedHashMap<K, V> map = new LinkedHashMap<>(16, 0.75f, true);

    public TextureLru(int capacity, BiConsumer<K, V> onEvict) {
        this.capacity = Math.max(1, capacity);
        this.onEvict = onEvict;
    }

    /** The value (marking it most recently used), or null. */
    public V get(K key) { return map.get(key); }

    /** Adds a value; the least recently used ones beyond the capacity are removed and handed to the eviction callback. */
    public void put(K key, V value) {
        V old = map.put(key, value);
        if (old != null && old != value) onEvict.accept(key, old);
        Iterator<Map.Entry<K, V>> it = map.entrySet().iterator();
        while (map.size() > capacity && it.hasNext()) {
            Map.Entry<K, V> e = it.next();
            if (e.getKey().equals(key)) continue;
            it.remove();
            onEvict.accept(e.getKey(), e.getValue());
        }
    }

    public int size() { return map.size(); }

    /** Removes everything, handing every value to the callback (resource reload, logout). */
    public void clear() {
        Map<K, V> copy = new LinkedHashMap<>(map);
        map.clear();
        copy.forEach(onEvict);
    }
}
