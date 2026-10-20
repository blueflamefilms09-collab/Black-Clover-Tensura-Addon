package com.newuniverse.nusmp.client.relics.vu;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Why: the ambient effect layers of an upgraded relic are data, not Java per item: assets/nusmp/visual_upgrade/fx/&lt;relic id&gt;.json lists them (name, kind, colour, rate), written by
 * tools/visual_upgrade/charms_genesis_worn. A {@code geometry} layer is part of the model (a spinning halo ring, orbiting shards, a light shaft, a portal ripple: the named animation
 * group of the model file draws it, so it costs nothing outside the draw passes); a {@code particle} layer is spawned by {@link VuFxClient} while the relic is held or worn, within the
 * per-tick particle budget and thinned by the quality options. Pure (Gson only), so the files are checked in tests.
 * <pre>
 * { "id": "...", "ambient_sound": "nusmp:relic.ambient.x", "draw_ticks": 12,
 *   "layers": [ { "name": "halo ring", "kind": "geometry", "type": "halo_ring", "group": "halo", "views": ["hand","worn"] },
 *               { "name": "idle motes", "kind": "particle", "type": "motes", "particle": "end_rod", "color": "FFD93A", "count": 1, "every": 6,
 *                 "radius": 0.35, "rise": 0.012, "views": ["hand","worn"] } ] }
 * </pre>
 */
public final class VuFx {
    /** Geometry layer types (a group of the model) and particle layer types (spawned in the world). */
    public static final Set<String> GEOMETRY = Set.of("halo_ring", "rune_ring", "orbit_shards", "light_shafts", "lens_flare", "ground_cast", "distortion", "portal_ripples",
            "starfield", "nebula_core", "energy_arcs", "chains", "ribbons", "petals", "aura_shell");
    public static final Set<String> PARTICLE = Set.of("motes", "sparks", "embers", "arcs", "drips", "glints", "wisps", "stars");

    /** One layer. {@code particle} is a vanilla particle id ("end_rod", "dust" ...), {@code color} 0xRRGGBB (-1: none). */
    public static final class Layer {
        public final String name, kind, type, group, particle;
        public final List<String> views;
        public final int color, count, every;
        public final float radius, rise, speed, dy;

        Layer(JsonObject o) {
            this.name = str(o, "name", "");
            this.kind = str(o, "kind", "geometry");
            this.type = str(o, "type", "");
            this.group = str(o, "group", "");
            this.particle = str(o, "particle", "end_rod");
            List<String> v = new ArrayList<>();
            if (o.has("views") && o.get("views").isJsonArray()) for (JsonElement e : o.getAsJsonArray("views")) v.add(e.getAsString());
            if (v.isEmpty()) v = List.of("hand", "worn");
            this.views = List.copyOf(v);
            this.color = o.has("color") ? parseColor(o.get("color").getAsString()) : -1;
            this.count = Math.max(1, Math.min(6, num(o, "count", 1)));
            this.every = Math.max(1, num(o, "every", 6));
            this.radius = (float) dbl(o, "radius", 0.35);
            this.rise = (float) dbl(o, "rise", 0.01);
            this.speed = (float) dbl(o, "speed", 0.0);
            this.dy = (float) dbl(o, "dy", 0.0);
        }

        public boolean isParticle() { return "particle".equals(kind); }

        public boolean isGeometry() { return "geometry".equals(kind); }

        public boolean in(String view) { return views.contains(view); }
    }

    public final String id, ambientSound;
    public final int drawTicks;
    public final List<Layer> layers;

    private VuFx(String id, String sound, int drawTicks, List<Layer> layers) {
        this.id = id;
        this.ambientSound = sound;
        this.drawTicks = drawTicks;
        this.layers = layers;
    }

    public static VuFx parse(String json) {
        JsonObject o = JsonParser.parseString(json).getAsJsonObject();
        List<Layer> ls = new ArrayList<>();
        if (o.has("layers") && o.get("layers").isJsonArray()) {
            JsonArray a = o.getAsJsonArray("layers");
            for (JsonElement e : a) if (e.isJsonObject()) ls.add(new Layer(e.getAsJsonObject()));
        }
        return new VuFx(str(o, "id", ""), str(o, "ambient_sound", ""), Math.max(1, Math.min(40, num(o, "draw_ticks", 12))), List.copyOf(ls));
    }

    /** How many layers of the given kind ("geometry" or "particle") show in a view ("hand" or "worn"). */
    public int count(String kind, String view) {
        int n = 0;
        for (Layer l : layers) if (kind.equals(l.kind) && l.in(view)) n++;
        return n;
    }

    /** Every problem of the file (an unknown type, a particle layer without a rate ...); empty for a good file. */
    public List<String> problems() {
        List<String> out = new ArrayList<>();
        if (id.isEmpty()) out.add("no id");
        for (Layer l : layers) {
            if (l.isGeometry() && !GEOMETRY.contains(l.type)) out.add("layer " + l.name + ": unknown geometry type " + l.type);
            else if (l.isParticle() && !PARTICLE.contains(l.type)) out.add("layer " + l.name + ": unknown particle type " + l.type);
            else if (!l.isGeometry() && !l.isParticle()) out.add("layer " + l.name + ": kind must be geometry or particle");
            if (l.isGeometry() && l.group.isEmpty()) out.add("layer " + l.name + ": a geometry layer names the model group that draws it");
            if (l.isParticle() && l.color < 0 && l.particle.equals("dust")) out.add("layer " + l.name + ": a dust layer needs a colour");
        }
        return out;
    }

    static int parseColor(String s) {
        try {
            return Integer.parseInt(s.startsWith("#") ? s.substring(1) : s, 16) & 0xFFFFFF;
        } catch (NumberFormatException bad) {
            return -1;
        }
    }

    private static String str(JsonObject o, String k, String d) { return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsString() : d; }

    private static int num(JsonObject o, String k, int d) { return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsInt() : d; }

    private static double dbl(JsonObject o, String k, double d) { return o.has(k) && o.get(k).isJsonPrimitive() ? o.get(k).getAsDouble() : d; }
}
