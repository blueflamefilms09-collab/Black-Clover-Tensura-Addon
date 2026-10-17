package com.newuniverse.nusmp.labyrinth.mega.theme;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the parsers of the theme files and of the spirit roster are pure classes that read a plain tree of maps, lists, strings, numbers and booleans
 * (so a JUnit test can run them on the shipped files without a JSON library). The game reads the same files with Gson, so this is the one place that
 * converts Gson's {@link JsonElement} into that tree: object to {@code LinkedHashMap} (keeping the file's order), array to {@code ArrayList}, number to
 * {@link Number}, string to {@code String}, boolean to {@code Boolean}, null to null. Nothing else lives here.
 */
public final class JsonTrees {
    private JsonTrees() {}

    /** Reads a JSON document from the reader into the tree; throws on malformed JSON (the caller logs the file and skips it). */
    public static Object read(Reader reader) { return toTree(JsonParser.parseReader(reader)); }

    /** Converts one element. */
    public static Object toTree(JsonElement e) {
        if (e == null || e.isJsonNull()) return null;
        if (e.isJsonObject()) {
            Map<String, Object> m = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> x : e.getAsJsonObject().entrySet()) m.put(x.getKey(), toTree(x.getValue()));
            return m;
        }
        if (e.isJsonArray()) {
            JsonArray a = e.getAsJsonArray();
            List<Object> l = new ArrayList<>(a.size());
            for (JsonElement x : a) l.add(toTree(x));
            return l;
        }
        JsonPrimitive p = e.getAsJsonPrimitive();
        if (p.isBoolean()) return p.getAsBoolean();
        if (p.isNumber()) return p.getAsNumber();
        return p.getAsString();
    }
}
