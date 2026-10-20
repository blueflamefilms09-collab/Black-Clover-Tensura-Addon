package com.newuniverse.nusmp.client.vupgrade.godb;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.List;

/**
 * The data table of one upgraded weapon (release 1.102.0, slice god_melee_b): assets/nusmp/visual_upgrade/&lt;item id&gt;.json, written by tools/visual_upgrade/god_melee_b/build.py.
 * Pure data and parsing (no Minecraft classes), so it is tested without the game. It says which meshes the weapon uses (the full-detail one, its level-of-detail twin, the one
 * orbiting piece), the colours of its passes, how its energy veins flow, how its halo shell breathes, which pieces orbit it and how, how its swing smears, and which ambient
 * particle layers run while it is held. The renderer ({@code client/GodMeleeBRender}) and the particle tick ({@link UpgradeFx}) only read this record.
 */
public record UpgradeSpec(String item, String mesh, String lodMesh, String orbitMesh, float axisY0, float axisY1, float lengthBlocks,
                          List<String> passes, List<String> ambientLayers, List<String> animations,
                          int coreRgb, int accentRgb, int veinRgb, int haloRgb,
                          float veinSpeed, float veinWavelength, float veinBase, float veinPeak,
                          float haloScale, int haloAlpha, float haloBreath, float haloSpeed,
                          float coreFlicker, float coreFlickerSpeed,
                          List<Orbiter> orbiters, Trail trail, List<Layer> layers, float groundRadius, float shaftHeight,
                          float lodNearBlocks, float additiveBlocks, float particleBlocks) {

    /** One set of identical orbiting pieces: {@code count} copies of the orbit mesh on a circle round the weapon axis. Units are model units (centimetres) along / about the weapon axis. */
    public record Orbiter(int count, float radius, float yFrac, float speed, float spin, float bob, float bobSpeed, float tilt, float phase, float scale) {}

    /** The ghost copies drawn behind the weapon while it is swung. */
    public record Trail(int rgb, int ghosts, float lagDeg, int alpha) {}

    /** One ambient particle layer: its kind (motes, halo_ring, edge_line, ground_cast, wrist_flare, light_shaft, accent_flare), the particle ("dust" = a coloured dust mote), colour, rate per tick at full quality. */
    public record Layer(String kind, String particle, int rgb, float rate, float speed, float size) {}

    /** The seven kinds of particle layers, in the order the particle tick runs them. */
    public static final List<String> LAYER_KINDS = List.of("motes", "halo_ring", "edge_line", "ground_cast", "wrist_flare", "light_shaft", "accent_flare");

    /** Parses one table; throws IllegalArgumentException (with the reason) when a required field is missing or out of range. */
    public static UpgradeSpec parse(JsonObject o) {
        String item = str(o, "item");
        JsonObject axis = obj(o, "axis");
        JsonObject pal = obj(o, "palette");
        JsonObject veins = obj(o, "veins");
        JsonObject halo = obj(o, "halo");
        JsonObject core = o.has("core") ? obj(o, "core") : new JsonObject();
        JsonObject lod = o.has("lod") ? obj(o, "lod") : new JsonObject();
        List<Orbiter> orbiters = new ArrayList<>();
        for (JsonElement e : arr(o, "orbiters")) {
            JsonObject j = e.getAsJsonObject();
            orbiters.add(new Orbiter(Math.max(0, (int) num(j, "count")), num(j, "radius"), num(j, "y_frac"), num(j, "speed"), num(j, "spin"), num(j, "bob"), num(j, "bob_speed"),
                    num(j, "tilt"), num(j, "phase"), j.has("scale") ? num(j, "scale") : 1f));
        }
        JsonObject tj = obj(o, "swing_trail");
        Trail trail = new Trail(rgb(tj, "rgb"), Math.max(0, (int) num(tj, "ghosts")), num(tj, "lag_deg"), (int) num(tj, "alpha"));
        JsonObject pj = obj(o, "particles");
        List<Layer> layers = new ArrayList<>();
        for (JsonElement e : arr(pj, "layers")) {
            JsonObject j = e.getAsJsonObject();
            layers.add(new Layer(str(j, "kind"), str(j, "particle"), rgb(j, "rgb"), num(j, "rate"), num(j, "speed"), num(j, "size")));
        }
        UpgradeSpec s = new UpgradeSpec(item, str(o, "mesh"), str(o, "lod_mesh"), str(o, "orbit_mesh"), num(axis, "y0"), num(axis, "y1"), num(axis, "length_blocks"),
                strs(o, "passes"), strs(o, "ambient_layers"), strs(o, "animations"),
                rgb(pal, "core"), rgb(pal, "accent"), rgb(pal, "vein"), rgb(pal, "halo"),
                num(veins, "speed"), num(veins, "wavelength"), num(veins, "base"), num(veins, "peak"),
                num(halo, "scale"), (int) num(halo, "alpha"), num(halo, "breath"), num(halo, "speed"),
                core.has("flicker") ? num(core, "flicker") : 0f, core.has("flicker_speed") ? num(core, "flicker_speed") : 0.37f,
                List.copyOf(orbiters), trail, List.copyOf(layers), num(pj, "ground_radius"), num(pj, "shaft_height"),
                lod.has("near_blocks") ? num(lod, "near_blocks") : 12f, lod.has("additive_blocks") ? num(lod, "additive_blocks") : 24f, lod.has("particle_blocks") ? num(lod, "particle_blocks") : 32f);
        s.validate();
        return s;
    }

    /** The rules every table must meet (the same ones the JUnit data test applies to the shipped tables). */
    public void validate() {
        if (item.isEmpty() || !mesh.equals("up/" + item) || !lodMesh.equals(mesh + "_lod") || !orbitMesh.equals(mesh + "_orbit")) throw new IllegalArgumentException(item + ": the mesh ids must be up/<item>, _lod and _orbit");
        if (!(axisY1 > axisY0) || lengthBlocks <= 0f) throw new IllegalArgumentException(item + ": the axis range is empty");
        if (!(veinWavelength > 0f) || veinBase < 0f || veinPeak > 1.5f || veinPeak < veinBase) throw new IllegalArgumentException(item + ": the vein wave is out of range");
        if (haloScale < 1f || haloScale > 1.3f || haloAlpha < 0 || haloAlpha > 255) throw new IllegalArgumentException(item + ": the halo is out of range");
        if (!(lodNearBlocks > 0f) || additiveBlocks < lodNearBlocks || particleBlocks < lodNearBlocks) throw new IllegalArgumentException(item + ": the level-of-detail distances are out of order");
        int pieces = 0;
        for (Orbiter ob : orbiters) {
            pieces += ob.count();
            if (ob.yFrac() < 0f || ob.yFrac() > 1.1f || ob.radius() <= 0f || ob.count() > 12) throw new IllegalArgumentException(item + ": an orbiter is out of range");
        }
        if (pieces > 12) throw new IllegalArgumentException(item + ": more than 12 orbiting pieces");
        if (trail.ghosts() > 5 || trail.alpha() > 255) throw new IllegalArgumentException(item + ": the swing trail is out of range");
        for (Layer l : layers) {
            if (!LAYER_KINDS.contains(l.kind()) || l.rate() < 0f || l.rate() > 4f) throw new IllegalArgumentException(item + ": particle layer " + l.kind() + " is out of range");
        }
        if (layers.size() != LAYER_KINDS.size()) throw new IllegalArgumentException(item + ": every one of the seven particle layers must be set");
    }

    public Layer layer(String kind) {
        for (Layer l : layers) if (l.kind().equals(kind)) return l;
        return null;
    }

    public int orbitPieces() {
        int n = 0;
        for (Orbiter o : orbiters) n += o.count();
        return n;
    }

    // ---------------------------------------------------------------- json helpers
    static String str(JsonObject o, String k) {
        if (!o.has(k)) throw new IllegalArgumentException("missing field " + k);
        return o.get(k).getAsString();
    }

    static float num(JsonObject o, String k) {
        if (!o.has(k)) throw new IllegalArgumentException("missing field " + k);
        return o.get(k).getAsFloat();
    }

    static JsonObject obj(JsonObject o, String k) {
        if (!o.has(k)) throw new IllegalArgumentException("missing object " + k);
        return o.getAsJsonObject(k);
    }

    static JsonArray arr(JsonObject o, String k) {
        if (!o.has(k)) throw new IllegalArgumentException("missing array " + k);
        return o.getAsJsonArray(k);
    }

    static int rgb(JsonObject o, String k) {
        return Integer.parseInt(str(o, k), 16) & 0xFFFFFF;
    }

    static List<String> strs(JsonObject o, String k) {
        List<String> out = new ArrayList<>();
        for (JsonElement e : arr(o, k)) out.add(e.getAsString());
        return List.copyOf(out);
    }
}
