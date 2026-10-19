package com.newuniverse.nusmp.labyrinth.mega.guard;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Why: the one table of the abyssal boss looks (docs/labyrinth/abyssal_bosses.md), pure Java with no game class so the pick of a look, the numbers a renderer needs and the check that every look has
 * its files can be unit tested. The table is {@code assets/nusmp/abyssal/looks.json}, written by tools/abyssal/gen_abyssal.py next to the models it describes: look id to display name, kind
 * ({@code humanoid}: the vanilla player body with a 64x64 skin and separate extras; {@code geo}: a full model), the height the model was built to, the ground one walk and run cycle cover, the
 * shadow, the culling box, the size boost, the glow colour, and for each of the three phases the model, the caption said when the boss reaches it and, for a humanoid, its skin and its extras (id,
 * the part of the body they hang on, their bones) so the player-model renderer can take the same skin and the same extras. A boss profile names its look in {@code look}; a profile without one keeps
 * the look the Guardian always had, except an evil boss of a demon sector (abyss, blood, flame, infernal foundry), which wears the demon of its tier ({@link #resolve}).
 */
public final class AbyssalLooks {
    private AbyssalLooks() {}

    /** The resource the table is read from (it is on the class path of the game and of the tests). */
    public static final String TABLE = "/assets/nusmp/abyssal/looks.json";

    /** One extra of a humanoid: a crown, a phylactery, wings ... hung on the player part {@code anchor} (head, body, arm_r, arm_l, leg_r, leg_l or root); {@code bones} are its bones in the extras model ([] = cubes on the anchor bone itself). */
    public record Extra(String id, String anchor, List<String> bones) {}

    /**
     * One phase of a look: the model drawn now, what is said when it is reached, and for a humanoid its skin (under {@code textures/entity/}, without {@code .png}: {@code playermodel/abyssal_lich_p1}),
     * its extras model ({@code pmx_abyssal_lich_p1}: a GeoLite file whose roots are {@code x_head}, {@code x_body}, {@code x_arm_r}, {@code x_arm_l}, for the player-model kit) and the extras.
     */
    public record Phase(String model, String caption, String skin, String extrasModel, List<Extra> extras) {}

    /** A look. {@code boost} multiplies the size the model is drawn at (the hitbox height over {@code height}); the {@code glow} colour is 0xRRGGBB. */
    public record Look(String id, String name, boolean humanoid, float height, float walkCycle, float runCycle, float shadow, float growXz, float growUp, float boost, int glow, boolean dust,
                       float lift, String animation, List<Phase> phases, float[] glowLevel, boolean slim, Map<String, float[]> partScale) {
        public Phase phase(int p) { return phases.get(Math.max(1, Math.min(phases.size(), p)) - 1); }

        public String model(int p) { return phase(p).model(); }

        public float glowLevel(int p) { return glowLevel[Math.max(1, Math.min(glowLevel.length, p)) - 1]; }
    }

    /** The id of one phase of a humanoid look in the player-model kit's table {@code assets/nusmp/playermodel/looks_abyssal.json}. */
    public static String kitId(String lookId, int phase) { return "abyssal_" + lookId + "_p" + Math.max(1, Math.min(3, phase)); }

    private static volatile Map<String, Look> looks;
    private static volatile Map<String, Map<String, List<String>>> themeDefaults = Map.of();

    // ================================================================ lookups
    public static Map<String, Look> all() {
        Map<String, Look> m = looks;
        if (m == null) {
            synchronized (AbyssalLooks.class) {
                m = looks;
                if (m == null) {
                    m = load();
                    looks = m;
                }
            }
        }
        return m;
    }

    /** The look of an id, or null (a profile that names a look this build does not have keeps the old look). */
    public static Look get(String id) { return id == null || id.isEmpty() ? null : all().get(id); }

    /** The id of the look a boss wears: its profile's {@code look}; else, for an evil boss of a demon sector, the demon of its tier; else "" (the look the Guardian always had). */
    public static String resolve(String profileLook, String themeId, String side, int tier) {
        if (profileLook != null && !profileLook.isEmpty()) return get(profileLook) != null ? profileLook : "";
        all();
        Map<String, List<String>> bySide = themeDefaults.get(themeId == null ? "" : themeId);
        if (bySide == null) return "";
        List<String> tiers = bySide.get(side == null ? "" : side);
        if (tiers == null || tiers.isEmpty()) return "";
        String id = tiers.get(Math.max(1, Math.min(tiers.size(), tier)) - 1);
        return get(id) != null ? id : "";
    }

    // ================================================================ files
    /** The path (inside the mod's resources) of everything a look needs, for the asset test and the self-test: models, animations, textures with their glow layers, and for a humanoid the skins and extras. */
    public static List<String> assetPaths(Look look) {
        List<String> out = new ArrayList<>();
        String a = "assets/nusmp/";
        out.add(a + "animations/entity/abyssal_" + look.animation() + ".animation.json");
        for (Phase p : look.phases()) {
            out.add(a + "geo/entity/abyssal_" + p.model() + ".geo.json");
            out.add(a + "textures/entity/abyssal_" + p.model() + ".png");
            out.add(a + "textures/entity/abyssal_" + p.model() + "_glow.png");
            if (look.humanoid()) {
                out.add(a + "textures/entity/" + p.skin() + ".png");
                out.add(a + "textures/entity/" + p.skin() + "_glow.png");
                out.add(a + "geo/entity/" + p.extrasModel() + ".geo.json");
                out.add(a + "animations/entity/" + p.extrasModel() + ".animation.json");
                out.add(a + "textures/entity/" + p.extrasModel() + ".png");
                out.add(a + "textures/entity/" + p.extrasModel() + "_glow.png");
            }
        }
        return out;
    }

    /** What is wrong with the table: every look's files that {@code exists} refuses, and every structural slip. Empty = fine. */
    public static List<String> problems(Predicate<String> exists) {
        List<String> out = new ArrayList<>();
        if (all().isEmpty()) out.add("the table " + TABLE + " is missing or empty");
        for (Look l : all().values()) {
            if (l.phases().size() != 3) out.add(l.id() + ": needs three phases, has " + l.phases().size());
            if (l.height() <= 0f) out.add(l.id() + ": no height");
            for (String path : assetPaths(l)) if (!exists.test(path)) out.add(l.id() + ": missing " + path);
            if (l.humanoid()) for (Phase p : l.phases()) if (p.skin() == null || p.extras() == null) out.add(l.id() + ": a humanoid phase without a skin or extras");
        }
        return out;
    }

    // ================================================================ reading
    private static Map<String, Look> load() {
        try (InputStream in = AbyssalLooks.class.getResourceAsStream(TABLE)) {
            if (in == null) return Map.of();
            try (Reader r = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                return parse(JsonParser.parseReader(r).getAsJsonObject());
            }
        } catch (Exception e) {
            return Map.of();
        }
    }

    /** Reads the table (kept separate for the tests). Also fills the theme defaults. */
    static Map<String, Look> parse(JsonObject root) {
        Map<String, Look> out = new LinkedHashMap<>();
        JsonObject ls = root.getAsJsonObject("looks");
        for (Map.Entry<String, JsonElement> e : ls.entrySet()) {
            JsonObject o = e.getValue().getAsJsonObject();
            boolean humanoid = "humanoid".equals(str(o, "kind", "geo"));
            List<Phase> phases = new ArrayList<>();
            for (JsonElement pe : o.getAsJsonArray("phases")) {
                JsonObject p = pe.getAsJsonObject();
                List<Extra> extras = new ArrayList<>();
                if (p.has("extras")) for (JsonElement xe : p.getAsJsonArray("extras")) {
                    JsonObject x = xe.getAsJsonObject();
                    List<String> bones = new ArrayList<>();
                    if (x.has("bones")) for (JsonElement b : x.getAsJsonArray("bones")) bones.add(b.getAsString());
                    extras.add(new Extra(str(x, "id", ""), str(x, "anchor", "root"), List.copyOf(bones)));
                }
                phases.add(new Phase(str(p, "model", ""), str(p, "caption", ""), p.has("skin") ? str(p, "skin", null) : null, p.has("extras_model") ? str(p, "extras_model", null) : null, List.copyOf(extras)));
            }
            float[] gl = {0.75f, 0.9f, 1f};
            if (o.has("glow_level")) {
                JsonArray a = o.getAsJsonArray("glow_level");
                gl = new float[a.size()];
                for (int i = 0; i < gl.length; i++) gl[i] = a.get(i).getAsFloat();
            }
            Map<String, float[]> scale = new LinkedHashMap<>();
            boolean slim = false;
            if (o.has("humanoid")) {
                JsonObject h = o.getAsJsonObject("humanoid");
                slim = h.has("slim") && h.get("slim").getAsBoolean();
                if (h.has("part_scale")) for (Map.Entry<String, JsonElement> ps : h.getAsJsonObject("part_scale").entrySet()) {
                    JsonArray a = ps.getValue().getAsJsonArray();
                    scale.put(ps.getKey(), new float[]{a.get(0).getAsFloat(), a.get(1).getAsFloat(), a.get(2).getAsFloat()});
                }
            }
            out.put(e.getKey(), new Look(e.getKey(), str(o, "name", e.getKey()), humanoid, num(o, "height", 2f), num(o, "walk_cycle", 1.5f), num(o, "run_cycle", 2.5f), num(o, "shadow", 0.5f),
                    num(o, "grow_xz", 1.5f), num(o, "grow_up", 1f), num(o, "boost", 1f), rgb(str(o, "glow", "#ffffff")), o.has("dust") && o.get("dust").getAsBoolean(), num(o, "lift", 0f),
                    str(o, "animation", e.getKey()), List.copyOf(phases), gl, slim, Map.copyOf(scale)));
        }
        Map<String, Map<String, List<String>>> td = new LinkedHashMap<>();
        if (root.has("theme_defaults")) for (Map.Entry<String, JsonElement> t : root.getAsJsonObject("theme_defaults").entrySet()) {
            Map<String, List<String>> bySide = new LinkedHashMap<>();
            for (Map.Entry<String, JsonElement> s : t.getValue().getAsJsonObject().entrySet()) {
                List<String> ids = new ArrayList<>();
                for (JsonElement id : s.getValue().getAsJsonArray()) ids.add(id.getAsString());
                bySide.put(s.getKey(), List.copyOf(ids));
            }
            td.put(t.getKey(), Map.copyOf(bySide));
        }
        themeDefaults = Map.copyOf(td);
        return Map.copyOf(out);
    }

    private static String str(JsonObject o, String k, String d) { return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : d; }

    private static float num(JsonObject o, String k, float d) { return o.has(k) ? o.get(k).getAsFloat() : d; }

    private static int rgb(String s) {
        String h = s.startsWith("#") ? s.substring(1) : s;
        try {
            return Integer.parseInt(h, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }
}
