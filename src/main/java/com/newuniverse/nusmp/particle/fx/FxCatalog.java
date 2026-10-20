package com.newuniverse.nusmp.particle.fx;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Why: the list of the mod's custom particles is data, not code: {@code data/nusmp/particle_catalog.json} is written by {@code tools/gen_custom_particles.py} together with the textures, so the
 * ids, the number of animation frames and the physics of every particle can never drift away from its art. The registry ({@link FxTypes}), the client providers, the theme table, the command
 * and the tests all read this one list. Pure (Gson only, no game classes): the unit tests parse the real file. The list is read once, on first use, and is immutable afterwards.
 */
public final class FxCatalog {
    private FxCatalog() {}

    public static final String RESOURCE = "/data/nusmp/particle_catalog.json";

    /** One particle of the library. {@code tint}: its texture is neutral light so the theme colour may dye it; {@code px}: the texture size (16 or 32). */
    public record Def(String id, String family, String group, int frames, int px, boolean tint, String behaviour, FxProfile profile) {
        /** The sprite ids of the frames, as the particle json lists them: {@code nusmp:<id>_<n>}. */
        public List<String> sprites() {
            List<String> out = new ArrayList<>(frames);
            for (int i = 0; i < frames; i++) out.add("nusmp:" + id + "_" + i);
            return out;
        }

        /** The texture file name (no folder) of a frame. */
        public String textureFile(int frame) {
            return id + "_" + frame + ".png";
        }
    }

    private static final class Holder {
        static final List<Def> ALL = load();
        static final Map<String, Def> BY_ID = index(ALL);
    }

    /** Every particle, in catalog order. Never null; empty only if the resource is missing (the tests fail then). */
    public static List<Def> all() {
        return Holder.ALL;
    }

    public static Def byId(String id) {
        return id == null ? null : Holder.BY_ID.get(id);
    }

    public static boolean has(String id) {
        return byId(id) != null;
    }

    public static List<String> ids() {
        List<String> out = new ArrayList<>(Holder.ALL.size());
        for (Def d : Holder.ALL) out.add(d.id());
        return out;
    }

    private static Map<String, Def> index(List<Def> list) {
        Map<String, Def> m = new LinkedHashMap<>();
        for (Def d : list) m.put(d.id(), d);
        return Collections.unmodifiableMap(m);
    }

    private static List<Def> load() {
        try (InputStream in = FxCatalog.class.getResourceAsStream(RESOURCE)) {
            if (in == null) return List.of();
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return parse(JsonParser.parseReader(r));
            }
        } catch (IOException | RuntimeException e) {
            return List.of();
        }
    }

    /** Parses the catalog document; entries without an id or with no frames are skipped, a repeated id keeps its first entry. Public for the tests. */
    public static List<Def> parse(JsonElement root) {
        List<Def> out = new ArrayList<>();
        if (root == null || !root.isJsonObject()) return Collections.unmodifiableList(out);
        JsonElement arr = root.getAsJsonObject().get("particles");
        if (arr == null || !arr.isJsonArray()) return Collections.unmodifiableList(out);
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (JsonElement e : (JsonArray) arr) {
            if (!e.isJsonObject()) continue;
            JsonObject o = e.getAsJsonObject();
            String id = str(o, "id", "");
            int frames = o.has("frames") && o.get("frames").isJsonPrimitive() ? o.get("frames").getAsInt() : 0;
            if (id.isEmpty() || frames <= 0 || !seen.add(id)) continue;
            out.add(new Def(id, str(o, "family", "glow"), str(o, "group", str(o, "family", "glow")), frames, o.has("px") ? o.get("px").getAsInt() : 16,
                    o.has("tint") && o.get("tint").getAsBoolean(), str(o, "behaviour", ""), FxProfile.fromJson(o.has("profile") && o.get("profile").isJsonObject() ? o.getAsJsonObject("profile") : null)));
        }
        return Collections.unmodifiableList(out);
    }

    private static String str(JsonObject o, String key, String fallback) {
        JsonElement e = o.get(key);
        return e != null && e.isJsonPrimitive() ? e.getAsString() : fallback;
    }
}
