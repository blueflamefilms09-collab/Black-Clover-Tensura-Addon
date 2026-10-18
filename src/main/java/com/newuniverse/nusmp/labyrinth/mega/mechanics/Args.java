package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Why: a primitive's, a trigger's and a counter's arguments are a small bag of named values written in JSON, and a numeric one may be SCALED by depth ({@code [a, b]} is
 * {@code a + b*s}, see {@link Scaling}). This immutable bag is the one reader of that format for the parser, the signature guard and the engine, so a number means the same
 * thing everywhere. A value is a Double, a double[2] (scaled), a String or a Boolean. Reading a missing or wrongly typed value gives the caller's default, never an exception:
 * a datapack typo must not crash a floor. Pure (Gson only).
 */
public final class Args {
    public static final Args EMPTY = new Args(Map.of());

    private final Map<String, Object> m;

    private Args(Map<String, Object> m) { this.m = Collections.unmodifiableMap(m); }

    /** Reads every key of the object except the {@code reserved} ones (the keys of the surrounding structure: op, delay ...). Anything not number / [a,b] / string / bool is skipped. */
    public static Args fromJson(JsonObject o, Set<String> reserved) {
        Map<String, Object> out = new LinkedHashMap<>();
        for (Map.Entry<String, JsonElement> e : o.entrySet()) {
            if (reserved.contains(e.getKey())) continue;
            Object v = value(e.getValue());
            if (v != null) out.put(e.getKey(), v);
        }
        return new Args(out);
    }

    public static Args of(Map<String, Object> raw) { return new Args(new LinkedHashMap<>(raw)); }

    private static Object value(JsonElement e) {
        if (e == null || e.isJsonNull()) return null;
        if (e.isJsonPrimitive()) {
            JsonPrimitive p = e.getAsJsonPrimitive();
            if (p.isBoolean()) return p.getAsBoolean();
            if (p.isNumber()) return p.getAsDouble();
            return p.getAsString();
        }
        if (e.isJsonArray()) {
            JsonArray a = e.getAsJsonArray();
            if (a.size() == 2 && a.get(0).isJsonPrimitive() && a.get(1).isJsonPrimitive() && a.get(0).getAsJsonPrimitive().isNumber() && a.get(1).getAsJsonPrimitive().isNumber())
                return new double[] {a.get(0).getAsDouble(), a.get(1).getAsDouble()};
        }
        return null;
    }

    public boolean has(String k) { return m.containsKey(k); }

    public Map<String, Object> raw() { return m; }

    /** The number at {@code floor}: a plain number as it is, a scaled pair as {@code a + b*s}, anything else the default. */
    public double num(String k, double def, int floor) {
        Object v = m.get(k);
        if (v instanceof Double d) return d;
        if (v instanceof double[] ab) return Scaling.value(ab[0], ab[1], floor);
        return def;
    }

    /** The number at the first floor (s = 0): the base value. */
    public double num(String k, double def) { return num(k, def, 1); }

    /** The (a, b) pair of a number: {@code {v, 0}} for a plain one, null when absent or not numeric. */
    public double[] pair(String k) {
        Object v = m.get(k);
        if (v instanceof Double d) return new double[] {d, 0.0};
        if (v instanceof double[] ab) return new double[] {ab[0], ab[1]};
        return null;
    }

    public String str(String k, String def) {
        Object v = m.get(k);
        return v instanceof String s ? s : def;
    }

    public boolean bool(String k, boolean def) {
        Object v = m.get(k);
        return v instanceof Boolean b ? b : def;
    }

    /** The JSON of this bag (the form the generator writes), used by the tests that round-trip a definition. */
    public void writeInto(JsonObject o) {
        for (Map.Entry<String, Object> e : m.entrySet()) {
            Object v = e.getValue();
            if (v instanceof Double d) o.addProperty(e.getKey(), d);
            else if (v instanceof double[] ab) { JsonArray a = new JsonArray(); a.add(ab[0]); a.add(ab[1]); o.add(e.getKey(), a); }
            else if (v instanceof String s) o.addProperty(e.getKey(), s);
            else if (v instanceof Boolean b) o.addProperty(e.getKey(), b);
        }
    }

    @Override public String toString() { return m.toString(); }
}
