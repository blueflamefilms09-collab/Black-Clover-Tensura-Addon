package com.newuniverse.nusmp.elite.records.rule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: everything the records system remembers about ONE player: the records unlocked (id to the unix second they unlocked), the titles held (same), the active title, the running counters (kills,
 * war wins, calamity damage ...), the best values (highest EP seen, hardest single hit ...), keyed counters (kills per entity id) and found-sets (biomes, structures, skills). It is a pure class so
 * the unlock rules and the saving are unit-tested without the game; the player attachment holds one and {@code RecordDataNbt} converts it to and from NBT through {@link #toRaw} /
 * {@link #fromRaw}. Reading is TOLERANT on purpose: a save edited by hand, written by an older or newer build or damaged must load as much as it can (a wrong type or a negative number is skipped, a
 * list that is absurdly long is cut at {@link #MAX_ENTRIES}) and never throw, because a player who cannot load their data cannot log in.
 */
public final class RecordData {
    public static final int VERSION = 1;
    /** The most entries of any one map or set read back from a save (a damaged file must not allocate without bound). */
    public static final int MAX_ENTRIES = 8192;

    private final Map<String, Long> unlocked = new LinkedHashMap<>();
    private final Map<String, Long> titles = new LinkedHashMap<>();
    private String active = "";
    private final Map<String, Double> counters = new HashMap<>();
    private final Map<String, Double> bests = new HashMap<>();
    private final Map<String, Map<String, Integer>> maps = new HashMap<>();
    private final Map<String, Set<String>> sets = new HashMap<>();

    // ------------------------------------------------------------------ records
    public boolean isUnlocked(String id) { return unlocked.containsKey(id); }

    /** The unix second the record unlocked, 0 when it is locked. */
    public long unlockedAt(String id) { return unlocked.getOrDefault(id, 0L); }

    /** Unlocks the record; false when it already was. */
    public boolean unlock(String id, long nowSec) {
        if (id == null || id.isEmpty() || unlocked.containsKey(id)) return false;
        unlocked.put(id, Math.max(1L, nowSec));
        return true;
    }

    public boolean lock(String id) { return unlocked.remove(id) != null; }

    public Set<String> unlockedIds() { return Collections.unmodifiableSet(unlocked.keySet()); }

    public int unlockedCount() { return unlocked.size(); }

    // ------------------------------------------------------------------ titles
    public boolean holds(String title) { return titles.containsKey(title); }

    public long heldAt(String title) { return titles.getOrDefault(title, 0L); }

    public boolean grantTitle(String id, long nowSec) {
        if (id == null || id.isEmpty() || titles.containsKey(id)) return false;
        titles.put(id, Math.max(1L, nowSec));
        return true;
    }

    /** Takes a title away; the active title is cleared when it was that one. */
    public boolean revokeTitle(String id) {
        boolean had = titles.remove(id) != null;
        if (had && id.equals(active)) active = "";
        return had;
    }

    public Set<String> heldTitles() { return Collections.unmodifiableSet(titles.keySet()); }

    public int heldCount() { return titles.size(); }

    public String active() { return active; }

    /** Makes a held title the active one ("" clears it). False (and no change) when the title is not held. */
    public boolean setActive(String id) {
        if (id == null || id.isEmpty()) {
            active = "";
            return true;
        }
        if (!titles.containsKey(id)) return false;
        active = id;
        return true;
    }

    // ------------------------------------------------------------------ counters and bests
    public double counter(String key) { return counters.getOrDefault(key, 0.0); }

    public void addCounter(String key, double delta) {
        if (Double.isNaN(delta) || Double.isInfinite(delta) || delta == 0) return;
        counters.put(key, Math.max(0.0, counter(key) + delta));
    }

    public double best(String key) { return bests.getOrDefault(key, 0.0); }

    /** Raises the best value of {@code key} to {@code v}; true when it rose. */
    public boolean bestMax(String key, double v) {
        if (Double.isNaN(v) || Double.isInfinite(v) || v <= best(key)) return false;
        bests.put(key, v);
        return true;
    }

    // ------------------------------------------------------------------ keyed counters and sets
    public int mapGet(String map, String key) {
        Map<String, Integer> m = maps.get(map);
        return m == null ? 0 : m.getOrDefault(key, 0);
    }

    public void mapInc(String map, String key, int delta) {
        if (key == null || key.isEmpty() || delta == 0) return;
        Map<String, Integer> m = maps.computeIfAbsent(map, k -> new HashMap<>());
        if (!m.containsKey(key) && m.size() >= MAX_ENTRIES) return;
        long v = (long) m.getOrDefault(key, 0) + delta;
        m.put(key, (int) Math.max(0, Math.min(Integer.MAX_VALUE, v)));
    }

    public Map<String, Integer> map(String name) {
        Map<String, Integer> m = maps.get(name);
        return m == null ? Map.of() : Collections.unmodifiableMap(m);
    }

    /** Adds a value to a found-set; true when it was new. */
    public boolean setAdd(String name, String value) {
        if (value == null || value.isEmpty()) return false;
        Set<String> s = sets.computeIfAbsent(name, k -> new LinkedHashSet<>());
        if (s.size() >= MAX_ENTRIES && !s.contains(value)) return false;
        return s.add(value);
    }

    public Set<String> set(String name) {
        Set<String> s = sets.get(name);
        return s == null ? Set.of() : Collections.unmodifiableSet(s);
    }

    // ------------------------------------------------------------------ saving
    /** The data as plain Maps / Lists / Doubles / Longs / Strings, ready for any serialiser (NBT, JSON). */
    public Map<String, Object> toRaw() {
        Map<String, Object> o = new LinkedHashMap<>();
        o.put("v", (long) VERSION);
        o.put("rec", new LinkedHashMap<String, Object>(unlocked));
        o.put("tit", new LinkedHashMap<String, Object>(titles));
        o.put("act", active);
        o.put("cnt", new LinkedHashMap<String, Object>(counters));
        o.put("best", new LinkedHashMap<String, Object>(bests));
        Map<String, Object> mm = new LinkedHashMap<>();
        for (var e : maps.entrySet()) mm.put(e.getKey(), new LinkedHashMap<String, Object>(e.getValue()));
        o.put("maps", mm);
        Map<String, Object> ss = new LinkedHashMap<>();
        for (var e : sets.entrySet()) ss.put(e.getKey(), new ArrayList<Object>(e.getValue()));
        o.put("sets", ss);
        return o;
    }

    /** Reads {@link #toRaw}'s form back, skipping everything it cannot understand. Never throws; null or garbage gives an empty data. */
    public static RecordData fromRaw(Object raw) {
        RecordData d = new RecordData();
        if (!(raw instanceof Map<?, ?> m)) return d;
        readTimes(m.get("rec"), d.unlocked);
        readTimes(m.get("tit"), d.titles);
        Object act = m.get("act");
        if (act instanceof String s && d.titles.containsKey(s)) d.active = s;
        readDoubles(m.get("cnt"), d.counters);
        readDoubles(m.get("best"), d.bests);
        if (m.get("maps") instanceof Map<?, ?> mm) {
            for (var e : mm.entrySet()) {
                if (!(e.getKey() instanceof String name) || !(e.getValue() instanceof Map<?, ?> inner)) continue;
                Map<String, Integer> out = new HashMap<>();
                for (var x : inner.entrySet()) {
                    if (out.size() >= MAX_ENTRIES) break;
                    if (x.getKey() instanceof String k && !k.isEmpty() && x.getValue() instanceof Number n && n.doubleValue() >= 0 && n.doubleValue() < Integer.MAX_VALUE) out.put(k, n.intValue());
                }
                if (!out.isEmpty()) d.maps.put(name, out);
            }
        }
        if (m.get("sets") instanceof Map<?, ?> ss) {
            for (var e : ss.entrySet()) {
                if (!(e.getKey() instanceof String name) || !(e.getValue() instanceof Collection<?> c)) continue;
                Set<String> out = new LinkedHashSet<>();
                for (Object x : c) {
                    if (out.size() >= MAX_ENTRIES) break;
                    if (x instanceof String s && !s.isEmpty()) out.add(s);
                }
                if (!out.isEmpty()) d.sets.put(name, out);
            }
        }
        return d;
    }

    private static void readTimes(Object src, Map<String, Long> into) {
        if (!(src instanceof Map<?, ?> m)) return;
        for (var e : m.entrySet()) {
            if (into.size() >= MAX_ENTRIES) break;
            if (e.getKey() instanceof String k && !k.isEmpty() && e.getValue() instanceof Number n && n.longValue() > 0) into.put(k, n.longValue());
        }
    }

    private static void readDoubles(Object src, Map<String, Double> into) {
        if (!(src instanceof Map<?, ?> m)) return;
        for (var e : m.entrySet()) {
            if (into.size() >= MAX_ENTRIES) break;
            if (!(e.getKey() instanceof String k) || k.isEmpty() || !(e.getValue() instanceof Number n)) continue;
            double v = n.doubleValue();
            if (!Double.isNaN(v) && !Double.isInfinite(v) && v >= 0) into.put(k, v);
        }
    }

    /** Copy for the new body after death / dimension return (the attachment copies on death). */
    public RecordData copy() { return fromRaw(toRaw()); }

    public boolean isEmpty() { return unlocked.isEmpty() && titles.isEmpty() && counters.isEmpty() && bests.isEmpty() && maps.isEmpty() && sets.isEmpty(); }

    /** Wipes everything (the reset command). */
    public void clear() {
        unlocked.clear();
        titles.clear();
        active = "";
        counters.clear();
        bests.clear();
        maps.clear();
        sets.clear();
    }

    /** A flat list of unlocked ids in unlock order, newest last. */
    public List<String> unlockedInOrder() { return new ArrayList<>(unlocked.keySet()); }
}
