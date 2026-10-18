package com.newuniverse.nusmp.elite.records.rule;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Why: the evaluators must be pure functions, so they read a snapshot of everything they may ask about instead of the player: named numbers (deaths, EP, kills_all, distance_walk ...), named
 * counters keyed by a string (kills per entity id, forge items per quality), named sets (biomes found, structures found, skills learned) and the list of outside SOURCES that could not answer
 * ("ftbchunks" when FTB Chunks is absent). The server builds one of these from the player's saved record data and the live game ({@link FactsBuilder}), the tests build one by hand. Absent keys read
 * as 0 / empty. Mutable only while being filled.
 */
public final class Facts {
    private final Map<String, Double> numbers = new HashMap<>();
    private final Map<String, Map<String, Integer>> maps = new HashMap<>();
    private final Map<String, Set<String>> sets = new HashMap<>();
    private final Set<String> unavailable = new HashSet<>();

    public double num(String key) { return numbers.getOrDefault(key, 0.0); }

    public Facts num(String key, double v) {
        numbers.put(key, v);
        return this;
    }

    /** One entry of a keyed counter ("kills" / "minecraft:zombie"), 0 when absent. */
    public int count(String map, String key) {
        Map<String, Integer> m = maps.get(map);
        return m == null ? 0 : m.getOrDefault(key, 0);
    }

    public Facts count(String map, String key, int v) {
        maps.computeIfAbsent(map, k -> new HashMap<>()).put(key, v);
        return this;
    }

    /** The whole keyed counter (read only). */
    public Map<String, Integer> counts(String map) {
        Map<String, Integer> m = maps.get(map);
        return m == null ? Map.of() : Collections.unmodifiableMap(m);
    }

    public Set<String> set(String name) {
        Set<String> s = sets.get(name);
        return s == null ? Set.of() : Collections.unmodifiableSet(s);
    }

    public Facts set(String name, Collection<String> values) {
        sets.put(name, new HashSet<>(values));
        return this;
    }

    public boolean unavailable(String source) { return unavailable.contains(source); }

    public Facts markUnavailable(String source) {
        unavailable.add(source);
        return this;
    }
}
