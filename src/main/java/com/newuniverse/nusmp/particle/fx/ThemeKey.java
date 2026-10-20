package com.newuniverse.nusmp.particle.fx;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Why: the call sites name a theme by a string ({@code "relic.element.fire"}), but looking a string up in a table on every particle call would be a per-tick cost. A {@code ThemeKey} is the
 * interned handle of one theme id: {@link #of} hands out one object per id (the codemod's generated constants hold them), and the object caches the resolved {@link ParticleThemes.Theme}
 * plus an opaque slot for the facade's ready-made particle options, both invalidated by {@link ParticleThemes#epoch()} when the table is reloaded. Reading the cache is two volatile reads
 * and no allocation. Pure (no game classes): the facade stores its option arrays in {@link #cache}.
 */
public final class ThemeKey {
    private static final ConcurrentHashMap<String, ThemeKey> KEYS = new ConcurrentHashMap<>();

    private final String id;
    private volatile ParticleThemes.Theme theme;
    private volatile int themeEpoch = -1;
    /** The facade's own cache for this key (ready-made options), valid for {@link #cacheEpoch}. */
    public volatile Object cache;
    public volatile int cacheEpoch = -1;

    private ThemeKey(String id) {
        this.id = id;
    }

    public static ThemeKey of(String id) {
        String k = id == null || id.isBlank() ? "default" : id;
        ThemeKey t = KEYS.get(k);
        return t != null ? t : KEYS.computeIfAbsent(k, ThemeKey::new);
    }

    public String id() {
        return id;
    }

    /** The resolved theme of this key (exact, else the nearest parent, else the default). */
    public ParticleThemes.Theme theme() {
        int e = ParticleThemes.epoch();
        ParticleThemes.Theme t = theme;
        if (t == null || themeEpoch != e) {
            t = ParticleThemes.resolve(id);
            theme = t;
            themeEpoch = e;
        }
        return t;
    }

    /** A key for a nested id: {@code ThemeKey.of("magic").child("FLAME")} is {@code magic.FLAME}. */
    public ThemeKey child(String part) {
        return of(id + "." + part);
    }

    @Override
    public String toString() {
        return "ThemeKey[" + id + "]";
    }
}
