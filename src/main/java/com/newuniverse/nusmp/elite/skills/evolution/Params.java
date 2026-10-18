package com.newuniverse.nusmp.elite.skills.evolution;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Why: a trigger in the data file carries a few loose parameters ({"count": 50}, {"item": "minecraft:netherite_ingot", "count": 4}, {"titles": ["a", "b"]}). Triggers must be pure
 * classes that tests can build without Gson or Minecraft, so the parameters are handed over as this small read-only view over a plain map; every getter has a default and never
 * throws, because a hand-edited data file is the likeliest thing to be wrong in this whole feature.
 */
public final class Params {
    public static final Params EMPTY = new Params(Map.of());

    private final Map<String, Object> map;

    public Params(Map<String, Object> map) { this.map = map == null ? Map.of() : map; }

    public static Params of(Object... kv) {
        java.util.LinkedHashMap<String, Object> m = new java.util.LinkedHashMap<>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put(String.valueOf(kv[i]), kv[i + 1]);
        return new Params(m);
    }

    public boolean has(String key) { return map.containsKey(key); }

    public String str(String key, String def) {
        Object v = map.get(key);
        return v == null ? def : String.valueOf(v);
    }

    public int integer(String key, int def) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.intValue();
        if (v instanceof String s) {
            try { return Integer.parseInt(s.trim()); } catch (NumberFormatException ignored) { return def; }
        }
        return def;
    }

    public double dbl(String key, double def) {
        Object v = map.get(key);
        if (v instanceof Number n) return n.doubleValue();
        if (v instanceof String s) {
            try { return Double.parseDouble(s.trim()); } catch (NumberFormatException ignored) { return def; }
        }
        return def;
    }

    public boolean bool(String key, boolean def) {
        Object v = map.get(key);
        if (v instanceof Boolean b) return b;
        if (v instanceof String s) return Boolean.parseBoolean(s.trim());
        return def;
    }

    /** A list of strings: a JSON array, or a single string as a one-element list. */
    public List<String> strings(String key) {
        Object v = map.get(key);
        List<String> out = new ArrayList<>();
        if (v instanceof List<?> l) for (Object o : l) { if (o != null) out.add(String.valueOf(o)); }
        else if (v != null) out.add(String.valueOf(v));
        return out;
    }

    public Map<String, Object> asMap() { return map; }

    @Override public String toString() { return map.toString(); }
}
