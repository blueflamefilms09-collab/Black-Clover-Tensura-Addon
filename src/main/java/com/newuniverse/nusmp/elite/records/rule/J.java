package com.newuniverse.nusmp.elite.records.rule;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: every data file of the records system is hand-edited JSON that a datapack author may get wrong; a wrong type must be a skipped field with a problem line, never an exception that drops the whole
 * file. These are the tolerant typed getters all the parsers use. Pure (Gson only).
 */
public final class J {
    private J() {}

    public static String str(JsonObject o, String key, String def) {
        JsonElement e = o == null ? null : o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : def;
    }

    public static double num(JsonObject o, String key, double def) {
        JsonElement e = o == null ? null : o.get(key);
        if (e != null && e.isJsonPrimitive()) {
            JsonPrimitive p = e.getAsJsonPrimitive();
            if (p.isNumber()) return finite(p.getAsDouble(), def);
            if (p.isString()) {
                try { return finite(Double.parseDouble(p.getAsString().trim()), def); } catch (NumberFormatException ignored) { return def; }
            }
        }
        return def;
    }

    private static double finite(double v, double def) { return Double.isNaN(v) || Double.isInfinite(v) ? def : v; }

    public static boolean bool(JsonObject o, String key, boolean def) {
        JsonElement e = o == null ? null : o.get(key);
        if (e != null && e.isJsonPrimitive()) {
            JsonPrimitive p = e.getAsJsonPrimitive();
            if (p.isBoolean()) return p.getAsBoolean();
            if (p.isString()) return "true".equalsIgnoreCase(p.getAsString());
        }
        return def;
    }

    public static JsonObject obj(JsonObject o, String key) {
        JsonElement e = o == null ? null : o.get(key);
        return e != null && e.isJsonObject() ? e.getAsJsonObject() : null;
    }

    public static JsonArray arr(JsonObject o, String key) {
        JsonElement e = o == null ? null : o.get(key);
        return e != null && e.isJsonArray() ? e.getAsJsonArray() : null;
    }

    /** A string list from an array (non-strings skipped) or from a single string. */
    public static List<String> strings(JsonObject o, String key) {
        List<String> out = new ArrayList<>();
        JsonElement e = o == null ? null : o.get(key);
        if (e == null) return out;
        if (e.isJsonPrimitive()) out.add(e.getAsString());
        else if (e.isJsonArray()) for (JsonElement x : e.getAsJsonArray()) if (x.isJsonPrimitive()) out.add(x.getAsString());
        return out;
    }
}
