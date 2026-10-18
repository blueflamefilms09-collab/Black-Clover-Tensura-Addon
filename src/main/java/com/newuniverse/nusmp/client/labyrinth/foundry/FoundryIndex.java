package com.newuniverse.nusmp.client.labyrinth.foundry;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Why: a CLIENT never sees data packs, so everything the renderer needs to draw one of the 400 foundry mobs is in an ASSET file, {@code assets/nusmp/foundry/index.json}, written by
 * tools/foundry/gen_foundry.py next to the models: for each mob id, which shared animation file its clips are in ({@code anim}: the art kit's animation class, see
 * tools/foundry/foundry_kit.py), its glow colour, its size (a multiplier on the model, 1 = as the kit built it), its tier (the glow burns brighter deeper down) and its model height.
 * This class is the pure parser and the lookup (Gson only, no Minecraft type), so the file's shape is tested without a client. A broken entry is skipped, never an exception: a mob
 * without an entry is simply not drawn (and the renderer says so once in the log).
 */
public final class FoundryIndex {
    private FoundryIndex() {}

    /** One mob of the index. */
    public record Entry(String anim, int glow, float size, int tier, float height) {}

    private static volatile Map<String, Entry> entries = Map.of();

    /** The entry of a mob id, or null. */
    public static Entry get(String id) { return id == null ? null : entries.get(id); }

    public static int size() { return entries.size(); }

    /** Replaces the loaded index (the reload listener, and the tests). */
    public static void install(Map<String, Entry> map) { entries = Collections.unmodifiableMap(new LinkedHashMap<>(map)); }

    /** Parses the file's text; a bad entry is left out. */
    public static Map<String, Entry> parse(String json) {
        Map<String, Entry> out = new LinkedHashMap<>();
        try {
            JsonElement root = JsonParser.parseString(json);
            if (!root.isJsonObject() || !root.getAsJsonObject().has("mobs") || !root.getAsJsonObject().get("mobs").isJsonObject()) return out;
            JsonObject mobs = root.getAsJsonObject().getAsJsonObject("mobs");
            for (Map.Entry<String, JsonElement> e : mobs.entrySet()) {
                if (!e.getValue().isJsonObject() || !e.getKey().matches("[a-z][a-z0-9_]{1,63}")) continue;
                JsonObject o = e.getValue().getAsJsonObject();
                String anim = o.has("anim") ? o.get("anim").getAsString() : "";
                if (!anim.matches("[a-z0-9_-]{3,120}")) continue;
                int glow = o.has("glow") ? o.get("glow").getAsInt() & 0xFFFFFF : 0xFFFFFF;
                float size = o.has("size") ? o.get("size").getAsFloat() : 1.0f;
                int tier = o.has("tier") ? o.get("tier").getAsInt() : 1;
                float height = o.has("height") ? o.get("height").getAsFloat() : 2.0f;
                out.put(e.getKey(), new Entry(anim, glow, clamp(size, 0.5f, 2.5f), Math.max(1, Math.min(5, tier)), clamp(height, 0.5f, 5f)));
            }
        } catch (RuntimeException ex) {
            return out;
        }
        return out;
    }

    private static float clamp(float v, float lo, float hi) { return Float.isNaN(v) ? lo : Math.max(lo, Math.min(hi, v)); }
}
