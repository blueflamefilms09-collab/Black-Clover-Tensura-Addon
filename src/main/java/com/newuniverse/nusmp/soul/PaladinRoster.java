package com.newuniverse.nusmp.soul;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: a caster may hold only so many Paladins ({@code 3 + 1 per 25% mastery}, never more than six) and the oldest one lets go when a
 * new one is made. That bookkeeping, who serves whom, since when, the cap, the eviction of the oldest, restoring after a world load in
 * any order, and the release of everything when the owner dies, is separate from the entity work, so it is a small generic class with
 * no Minecraft types, tested on its own. The glue keeps the same facts in the creature's own saved data, so a reload cannot lose them.
 *
 * @param <K> the id type (a UUID in the game)
 */
public final class PaladinRoster<K> {
    /** The hard ceiling and the default base and step of the limit. */
    public static final int HARD_MAX = 6, BASE = 3, STEP_PERCENT = 25;

    private record Entry<K>(K id, K owner, long time, long order) {}

    private final Map<K, Entry<K>> entries = new LinkedHashMap<>();
    private long counter;

    /** The limit at this mastery: base plus one per step percent, clamped to 1 .. ceiling. */
    public static int limit(double masteryPercent, int base, int stepPercent, int ceiling) {
        int step = Math.max(1, stepPercent);
        double mp = Double.isNaN(masteryPercent) ? 0 : Math.max(0, masteryPercent);
        int n = base + (int) Math.floor(mp / step);
        return Math.max(1, Math.min(Math.max(1, Math.min(HARD_MAX, ceiling)), n));
    }

    public static int limit(double masteryPercent) { return limit(masteryPercent, BASE, STEP_PERCENT, HARD_MAX); }

    public int size() { return entries.size(); }

    public boolean contains(K id) { return entries.containsKey(id); }

    public K ownerOf(K id) {
        Entry<K> e = entries.get(id);
        return e == null ? null : e.owner();
    }

    public int count(K owner) {
        int n = 0;
        for (Entry<K> e : entries.values()) if (e.owner().equals(owner)) n++;
        return n;
    }

    /** The ids of one owner's Paladins, oldest first. */
    public List<K> of(K owner) {
        List<Entry<K>> mine = new ArrayList<>();
        for (Entry<K> e : entries.values()) if (e.owner().equals(owner)) mine.add(e);
        mine.sort((a, b) -> a.time() != b.time() ? Long.compare(a.time(), b.time()) : Long.compare(a.order(), b.order()));
        List<K> out = new ArrayList<>();
        for (Entry<K> e : mine) out.add(e.id());
        return out;
    }

    /**
     * Adds a Paladin (or restores one after a load with its saved time). If the owner is then over 'max', the OLDEST of theirs (the new
     * one is never the one that goes unless the limit is zero) are removed and returned so the glue can release those creatures.
     * An id already known to another owner moves to the new one.
     */
    public List<K> add(K owner, K id, long time, int max) {
        if (owner == null || id == null) return List.of();
        entries.put(id, new Entry<>(id, owner, time, counter++));
        return trim(owner, max);
    }

    /** Evicts the oldest of one owner's Paladins until they hold at most 'max'; returns the evicted ids. */
    public List<K> trim(K owner, int max) {
        List<K> out = new ArrayList<>();
        List<K> mine = of(owner);
        for (int i = 0; mine.size() - i > Math.max(0, max); i++) {
            K gone = mine.get(i);
            entries.remove(gone);
            out.add(gone);
        }
        return out;
    }

    /** Takes one out (it died, was dismissed or was discarded). Returns its owner, or null if it was not held. */
    public K remove(K id) {
        Entry<K> e = entries.remove(id);
        return e == null ? null : e.owner();
    }

    /** The owner died or let everyone go: removes and returns every one of their Paladins (oldest first). */
    public List<K> releaseOwner(K owner) {
        List<K> mine = of(owner);
        for (K id : mine) entries.remove(id);
        return mine;
    }

    /** Every owner that holds at least one Paladin, with their count (for the periodic sweep). */
    public Map<K, Integer> owners() {
        Map<K, Integer> out = new HashMap<>();
        for (Entry<K> e : entries.values()) out.merge(e.owner(), 1, Integer::sum);
        return out;
    }

    public void clear() { entries.clear(); }
}
