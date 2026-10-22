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
 * Why: the contents list of the Engravement Codex is narrow and the full names of some franchises are long ("That Time I Got Reincarnated as a Slime"). The short labels live in
 * {@code data/nusmp/engraving_codex/short_names.json}; a franchise without one shows its full name (cut to fit by the screen). Presentation only, English. Pure (no Minecraft types), unit tested.
 */
public final class CodexNames {
    private CodexNames() {}

    private static final String RESOURCE = "/data/nusmp/engraving_codex/short_names.json";
    private static volatile Map<String, String> table;

    /** Franchise id to short label. An unreadable file gives an empty map (the full names are used; the unit test catches it). */
    public static Map<String, String> table() {
        Map<String, String> t = table;
        if (t == null) {
            synchronized (CodexNames.class) {
                t = table;
                if (t == null) {
                    try (InputStream in = CodexNames.class.getResourceAsStream(RESOURCE)) {
                        t = in == null ? Map.of() : parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                    } catch (Exception | LinkageError failed) {
                        t = Map.of();
                    }
                    table = t;
                }
            }
        }
        return t;
    }

    public static Map<String, String> parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        Map<String, String> out = new LinkedHashMap<>();
        JsonObject s = root.getAsJsonObject("short");
        if (s != null) for (Map.Entry<String, JsonElement> e : s.entrySet()) out.put(e.getKey(), e.getValue().getAsString());
        return Collections.unmodifiableMap(out);
    }
}
