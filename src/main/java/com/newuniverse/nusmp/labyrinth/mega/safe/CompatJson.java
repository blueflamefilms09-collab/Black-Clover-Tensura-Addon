package com.newuniverse.nusmp.labyrinth.mega.safe;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Why: the compat tables are data files ({@code data/nusmp/mega/compat/<mod>.json}) so the owner can correct a block id of another mod without
 * a rebuild, and reading a data file must be forgiving (one wrong entry must never take the whole file, or the whole data reload, down) and must
 * be testable. This class is only the translation from parsed JSON (Gson, which the game ships) to the pure {@link CompatTable.Source}: it has no
 * Minecraft types, so the same code is unit-tested against the files that really ship. The schema is
 * {@code {"mod": "farmersdelight", "swaps": {"<hint>": ["<block id>", ...]}, "flip_facing": ["<hint>", ...], "facing_property": "facing"}}:
 * {@code mod} and {@code swaps} are required, the other two optional, a hint's value may be one string instead of a list, entries that are not
 * strings are skipped, and keys starting with an underscore (comments) are ignored.
 */
public final class CompatJson {
    private CompatJson() {}

    /** Parses the text of one compat file; null when it is not valid JSON or lacks {@code mod} or {@code swaps}. */
    public static CompatTable.Source parse(String text) {
        try {
            return parse(JsonParser.parseString(text));
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Maps parsed JSON to a source; null when the required fields are missing. Never throws. */
    public static CompatTable.Source parse(JsonElement root) {
        try {
            if (root == null || !root.isJsonObject()) return null;
            JsonObject o = root.getAsJsonObject();
            String mod = string(o, "mod");
            if (mod == null || !o.has("swaps") || !o.get("swaps").isJsonObject()) return null;
            Map<String, List<String>> swaps = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("swaps").entrySet()) {
                if (e.getKey().startsWith("_")) continue;
                swaps.put(e.getKey(), strings(e.getValue()));
            }
            Set<String> flip = new LinkedHashSet<>(o.has("flip_facing") ? strings(o.get("flip_facing")) : List.of());
            return new CompatTable.Source(mod, swaps, flip, string(o, "facing_property"));
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String string(JsonObject o, String key) {
        JsonElement v = o.get(key);
        return v != null && v.isJsonPrimitive() && v.getAsJsonPrimitive().isString() && !v.getAsString().isBlank() ? v.getAsString().trim() : null;
    }

    /** A string, or a list of strings (anything else in it is skipped). */
    private static List<String> strings(JsonElement v) {
        List<String> out = new ArrayList<>();
        if (v == null) return out;
        if (v.isJsonPrimitive() && v.getAsJsonPrimitive().isString()) {
            out.add(v.getAsString());
        } else if (v.isJsonArray()) {
            for (JsonElement x : v.getAsJsonArray()) if (x.isJsonPrimitive() && x.getAsJsonPrimitive().isString()) out.add(x.getAsString());
        }
        return out;
    }
}
