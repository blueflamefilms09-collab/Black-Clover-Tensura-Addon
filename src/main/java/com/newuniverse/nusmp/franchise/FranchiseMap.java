package com.newuniverse.nusmp.franchise;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Which franchise an item of the mod comes from: {@code data/nusmp/relics/franchise_map.json}, written by {@code tools/relics/franchise_map.py} from the relic waves, the weapon
 * rosters and the manual overrides. An id that is not there has no franchise. Pure (unit-tested).
 * <p>
 * Lookup: the exact item path, then the left half of a pair ({@code x_left} follows {@code x}), then the longest id prefix of the file's {@code prefixes}.
 */
public final class FranchiseMap {
    private FranchiseMap() {}

    /** The parsed file. */
    public record Map2(Map<String, String> items, Map<String, String> prefixes) {
        /** The franchise id of an item path, or null. */
        public String franchiseOf(String path) {
            if (path == null || path.isEmpty()) return null;
            String exact = items.get(path);
            if (exact != null) return exact;
            if (path.endsWith("_left")) {
                String base = items.get(path.substring(0, path.length() - 5));
                if (base != null) return base;
            }
            String best = null;
            int len = -1;
            for (Map.Entry<String, String> e : prefixes.entrySet()) {
                if (e.getKey().length() > len && path.startsWith(e.getKey())) {
                    best = e.getValue();
                    len = e.getKey().length();
                }
            }
            return best;
        }
    }

    private static final String RESOURCE = "/data/nusmp/relics/franchise_map.json";
    private static volatile Map2 map;

    public static Map2 map() {
        Map2 m = map;
        if (m == null) {
            synchronized (FranchiseMap.class) {
                m = map;
                if (m == null) {
                    try (InputStream in = FranchiseMap.class.getResourceAsStream(RESOURCE)) {
                        m = in == null ? new Map2(Map.of(), Map.of()) : parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
                    } catch (Exception | LinkageError failed) {
                        m = new Map2(Map.of(), Map.of());
                    }
                    map = m;
                }
            }
        }
        return m;
    }

    /** The franchise id of an item path ({@code grimoire}), or null. */
    public static String franchiseOf(String path) { return map().franchiseOf(path); }

    public static Map2 parse(String json) {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        return new Map2(strings(root.getAsJsonObject("items")), strings(root.getAsJsonObject("prefixes")));
    }

    private static Map<String, String> strings(JsonObject o) {
        Map<String, String> out = new LinkedHashMap<>();
        if (o != null) for (Map.Entry<String, JsonElement> e : o.entrySet()) out.put(e.getKey(), e.getValue().getAsString());
        return Collections.unmodifiableMap(out);
    }
}
