package com.newuniverse.nusmp.client.ui.codex;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Why: turns the text of one {@code assets/<ns>/codex/<section>/<name>.json} into a {@link CodexEntryData}. It is lenient on purpose: a resource
 * pack written by hand must never break the book, so a bad file gives an error string the loader logs once, and every other file still loads.
 * Pure (Gson only, no Minecraft), so the format is pinned by tests. Format: {@code {"title":"..","text":"..","keys":"a|b","icon":"item:ns:id","order":0}},
 * {@code "remove": true} deletes an entry of a lower pack. A missing title falls back to the file name.
 */
public final class CodexEntryParser {
    private CodexEntryParser() {}

    /** The entry (null on failure) and why it failed (null on success). */
    public record Result(CodexEntryData entry, String error) {
        public boolean ok() { return entry != null; }
    }

    public static Result parse(String section, String id, String json) {
        if (section == null || section.isEmpty()) return new Result(null, "no section");
        if (id == null || id.isEmpty()) return new Result(null, "no id");
        JsonElement root;
        try {
            root = JsonParser.parseString(json == null ? "" : json);
        } catch (RuntimeException e) {
            return new Result(null, "not valid JSON: " + e.getMessage());
        }
        if (root == null || !root.isJsonObject()) return new Result(null, "the file must hold one JSON object");
        JsonObject o = root.getAsJsonObject();
        boolean remove = bool(o, "remove");
        String title = str(o, "title");
        if (title.isBlank() && !remove) title = CodexData.prettify(nameOf(id));
        return new Result(new CodexEntryData(section, id, title, str(o, "text"), str(o, "keys"), str(o, "icon"), intOf(o, "order"), remove), null);
    }

    /** The part of "namespace:name" after the colon, and after the last slash. */
    static String nameOf(String id) {
        String n = id.substring(id.indexOf(':') + 1);
        return n.substring(n.lastIndexOf('/') + 1);
    }

    private static String str(JsonObject o, String k) {
        JsonElement e = o.get(k);
        if (e == null || !e.isJsonPrimitive()) return "";
        return e.getAsString();
    }

    private static boolean bool(JsonObject o, String k) {
        JsonElement e = o.get(k);
        if (e == null || !e.isJsonPrimitive()) return false;
        try { return e.getAsBoolean(); } catch (RuntimeException ex) { return false; }
    }

    private static int intOf(JsonObject o, String k) {
        JsonElement e = o.get(k);
        if (e == null || !e.isJsonPrimitive()) return 0;
        try { return e.getAsInt(); } catch (RuntimeException ex) {
            try { return (int) Math.round(Double.parseDouble(e.getAsString().trim())); } catch (RuntimeException ex2) { return 0; }
        }
    }
}
