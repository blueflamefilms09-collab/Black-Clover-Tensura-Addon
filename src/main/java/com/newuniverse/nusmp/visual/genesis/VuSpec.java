package com.newuniverse.nusmp.visual.genesis;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The effect data of one upgraded weapon (assets/nusmp/visual_upgrade/&lt;id&gt;.json, written by tools/visual_upgrade/genesis_weapons): the animated groups of the mesh
 * (what spins, bobs, breathes, flickers), the ambient effect layers, the idle animations, the draw animation, the swing trail, the signature burst of every act and the
 * looped sound id. Pure data (no Minecraft classes): the client reads it once per resource reload, the tests read it from the file.
 */
public final class VuSpec {
    public final String id;
    public final String mesh;
    public final String lodMesh;
    public final float lodNear, lodFar;
    public final Map<String, Group> groups;
    public final List<Layer> layers;
    public final List<Idle> idle;
    public final Draw draw;
    public final Swing swing;
    public final Map<String, Act> acts;
    public final String soundLoop;

    /** One animation of a group: kind is spin, bob, sway, pulse, breathe or flicker; unused numbers stay 0. */
    public static final class Anim {
        public final String kind;
        public final float ax, ay, az;               // spin / sway / bob axis (normalised)
        public final float dps, amp, period, phase, deg, min, max, rate;

        Anim(String kind, float ax, float ay, float az, float dps, float amp, float period, float phase, float deg, float min, float max, float rate) {
            this.kind = kind;
            float n = (float) Math.sqrt(ax * ax + ay * ay + az * az);
            if (n < 1e-6f) { ax = 0f; ay = 1f; az = 0f; n = 1f; }
            this.ax = ax / n;
            this.ay = ay / n;
            this.az = az / n;
            this.dps = dps;
            this.amp = amp;
            this.period = period <= 0f ? 80f : period;
            this.phase = phase;
            this.deg = deg;
            this.min = min;
            this.max = max;
            this.rate = rate;
        }
    }

    /** The draw (equip) animation of one group: it grows from {@code fromScale} after {@code delay} ticks over {@code ticks} ticks. */
    public record GroupDraw(float fromScale, int delay, int ticks) {}

    public static final class Group {
        public final String name;
        public final float px, py, pz;
        public final List<Anim> anim;
        public final boolean lodSkip;
        public final int tintRgb;
        public final float alpha;
        public final GroupDraw draw;

        Group(String name, float px, float py, float pz, List<Anim> anim, boolean lodSkip, int tintRgb, float alpha, GroupDraw draw) {
            this.name = name;
            this.px = px;
            this.py = py;
            this.pz = pz;
            this.anim = anim;
            this.lodSkip = lodSkip;
            this.tintRgb = tintRgb;
            this.alpha = alpha;
            this.draw = draw;
        }
    }

    /** An ambient effect layer: {@code kind} says how it is drawn (a mesh group, the void pass, flares, or a particle emitter). */
    public record Layer(String id, String kind, String group, String particle, int rgb, float rate, String where, float radius) {}

    public record Idle(String id, String kind, String group) {}

    public record Draw(int ticks, int flashRgb) {}

    public record Swing(String trail, int rgb, int rgb2, float length, float width, int life) {}

    public record Act(String burst, String pattern, int rgb, float ring, int particles, String sound) {}

    private VuSpec(String id, String mesh, String lodMesh, float lodNear, float lodFar, Map<String, Group> groups, List<Layer> layers, List<Idle> idle, Draw draw, Swing swing,
                   Map<String, Act> acts, String soundLoop) {
        this.id = id;
        this.mesh = mesh;
        this.lodMesh = lodMesh;
        this.lodNear = lodNear;
        this.lodFar = lodFar;
        this.groups = Collections.unmodifiableMap(groups);
        this.layers = Collections.unmodifiableList(layers);
        this.idle = Collections.unmodifiableList(idle);
        this.draw = draw;
        this.swing = swing;
        this.acts = Collections.unmodifiableMap(acts);
        this.soundLoop = soundLoop;
    }

    // ---------------------------------------------------------------- parsing
    public static VuSpec parse(String json) {
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        String id = str(o, "id", "");
        JsonObject lod = o.has("lod") && o.get("lod").isJsonObject() ? o.getAsJsonObject("lod") : new JsonObject();
        Map<String, Group> groups = new LinkedHashMap<>();
        if (o.has("groups") && o.get("groups").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("groups").entrySet()) groups.put(e.getKey(), group(e.getKey(), e.getValue().getAsJsonObject()));
        }
        List<Layer> layers = new ArrayList<>();
        if (o.has("layers")) {
            for (JsonElement e : o.getAsJsonArray("layers")) {
                JsonObject l = e.getAsJsonObject();
                layers.add(new Layer(str(l, "id", ""), str(l, "kind", ""), str(l, "group", ""), str(l, "particle", ""), rgb(str(l, "rgb", "FFFFFF")), num(l, "rate", 0f), str(l, "where", ""), num(l, "radius", 0f)));
            }
        }
        List<Idle> idle = new ArrayList<>();
        if (o.has("idle")) {
            for (JsonElement e : o.getAsJsonArray("idle")) {
                JsonObject l = e.getAsJsonObject();
                idle.add(new Idle(str(l, "id", ""), str(l, "kind", ""), str(l, "group", "")));
            }
        }
        Draw draw = null;
        if (o.has("draw") && o.get("draw").isJsonObject()) {
            JsonObject d = o.getAsJsonObject("draw");
            draw = new Draw((int) num(d, "ticks", 14f), rgb(str(d, "flash", "FFFFFF")));
        }
        Swing swing = null;
        if (o.has("swing") && o.get("swing").isJsonObject()) {
            JsonObject s = o.getAsJsonObject("swing");
            swing = new Swing(str(s, "trail", ""), rgb(str(s, "rgb", "FFFFFF")), rgb(str(s, "rgb2", "000000")), num(s, "length", 1f), num(s, "width", 0.3f), (int) num(s, "life", 8f));
        }
        Map<String, Act> acts = new LinkedHashMap<>();
        if (o.has("acts") && o.get("acts").isJsonObject()) {
            for (Map.Entry<String, JsonElement> e : o.getAsJsonObject("acts").entrySet()) {
                JsonObject a = e.getValue().getAsJsonObject();
                acts.put(e.getKey(), new Act(str(a, "burst", ""), str(a, "pattern", "ring"), rgb(str(a, "rgb", "FFFFFF")), num(a, "ring", 3f), (int) num(a, "particles", 20f), str(a, "sound", "")));
            }
        }
        return new VuSpec(id, str(o, "mesh", id), str(lod, "mesh", id + "_lod"), num(lod, "near", 12f), num(lod, "far", 24f), groups, layers, idle, draw, swing, acts,
                o.has("sound_loop") && !o.get("sound_loop").isJsonNull() ? o.get("sound_loop").getAsString() : null);
    }

    private static Group group(String name, JsonObject g) {
        float px = 0f, py = 0f, pz = 0f;
        if (g.has("pivot")) {
            JsonArray p = g.getAsJsonArray("pivot");
            px = p.get(0).getAsFloat();
            py = p.get(1).getAsFloat();
            pz = p.get(2).getAsFloat();
        }
        List<Anim> anims = new ArrayList<>();
        if (g.has("anim")) {
            for (JsonElement e : g.getAsJsonArray("anim")) {
                JsonObject a = e.getAsJsonObject();
                float ax = 0f, ay = 1f, az = 0f;
                if (a.has("axis")) {
                    JsonArray ar = a.getAsJsonArray("axis");
                    ax = ar.get(0).getAsFloat();
                    ay = ar.get(1).getAsFloat();
                    az = ar.get(2).getAsFloat();
                }
                anims.add(new Anim(str(a, "kind", "spin"), ax, ay, az, num(a, "dps", 0f), num(a, "amp", 0f), num(a, "period", 80f), num(a, "phase", 0f), num(a, "deg", 0f),
                        num(a, "min", 0f), num(a, "max", 1f), num(a, "rate", 4f)));
            }
        }
        GroupDraw draw = null;
        if (g.has("draw") && g.get("draw").isJsonObject()) {
            JsonObject d = g.getAsJsonObject("draw");
            draw = new GroupDraw(num(d, "from_scale", 0f), (int) num(d, "delay", 0f), Math.max(1, (int) num(d, "ticks", 12f)));
        }
        return new Group(name, px, py, pz, Collections.unmodifiableList(anims), !g.has("lodSkip") || g.get("lodSkip").getAsBoolean(), g.has("tint") && g.get("tint").isJsonPrimitive() ? rgb(g.get("tint").getAsString()) : 0xFFFFFF,
                num(g, "alpha", 1f), draw);
    }

    private static String str(JsonObject o, String k, String d) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : d;
    }

    private static float num(JsonObject o, String k, float d) {
        return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsFloat() : d;
    }

    static int rgb(String hex) {
        try {
            return Integer.parseInt(hex.startsWith("#") ? hex.substring(1) : hex, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return 0xFFFFFF;
        }
    }

    // ---------------------------------------------------------------- derived
    /** The ambient layers that are drawn as particles (a client tick emitter) rather than as mesh. */
    public List<Layer> particleLayers() {
        List<Layer> out = new ArrayList<>();
        for (Layer l : layers) if (!l.particle().isEmpty()) out.add(l);
        return out;
    }

    /** The key a signature burst is looked up by: the act's key as the server sends it ({@code RelicCooldownPayload#actKey}). */
    public Act act(String actKey) { return acts.get(actKey); }
}
