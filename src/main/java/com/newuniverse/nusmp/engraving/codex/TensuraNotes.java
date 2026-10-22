package com.newuniverse.nusmp.engraving.codex;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why: Tensura Reincarnated's own code is not part of this mod, so the Engravement Codex needs one honest line for each Tensura engraving the mod puts on its items. The lines live in
 * {@code data/nusmp/engraving_codex/tensura_engravings.json} (words only, never a number: the numbers are Tensura's). The Codex prefers Tensura's own description text from the language
 * file when the game has it; this table is only the fallback. Pure (no Minecraft types), unit tested.
 */
public final class TensuraNotes {
    private TensuraNotes() {}

    /** The parsed file: the credit line, the line for an unknown id and the line of each known id. */
    public record Table(String credit, String generic, Map<String, String> lines) {
        /** The line of an engraving id ({@code swift}), or the generic line. */
        public String lineOf(String id) {
            String l = id == null ? null : lines.get(id);
            return l == null ? generic : l;
        }

        public boolean has(String id) { return id != null && lines.containsKey(id); }
    }

    private static final String RESOURCE = "/data/nusmp/engraving_codex/tensura_engravings.json";
    private static volatile Table table;

    /** The table, loaded once; an unreadable file gives an empty table with a generic line (the unit test catches it). */
    public static Table table() {
        Table t = table;
        if (t == null) {
            synchronized (TensuraNotes.class) {
                t = table;
                if (t == null) {
                    try (InputStream in = TensuraNotes.class.getResourceAsStream(RESOURCE)) {
                        t = in == null ? empty() : parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                    } catch (Exception | LinkageError failed) {
                        t = empty();
                    }
                    table = t;
                }
            }
        }
        return t;
    }

    private static Table empty() {
        return new Table("From Tensura Reincarnated; the exact numbers are Tensura's.", "An engraving of Tensura Reincarnated.", Map.of());
    }

    public static Table parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        Map<String, String> lines = new LinkedHashMap<>();
        JsonObject eng = root.getAsJsonObject("engravings");
        if (eng != null) for (Map.Entry<String, JsonElement> e : eng.entrySet()) lines.put(e.getKey(), e.getValue().getAsString());
        return new Table(root.get("credit").getAsString(), root.get("generic").getAsString(), Collections.unmodifiableMap(lines));
    }
}
