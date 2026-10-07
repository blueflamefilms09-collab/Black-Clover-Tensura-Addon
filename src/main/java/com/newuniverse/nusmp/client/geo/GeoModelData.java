package com.newuniverse.nusmp.client.geo;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 0.53: a parsed Bedrock-format model: the .geo.json files that Blockbench and GeckoLib use (format 1.12.0, and the older
 * "geometry.name" form): bones with pivots / rotations / parents, cubes with origin / size / uv (box or per face) / inflate /
 * mirror / pivot / rotation. This class only holds data (no game classes), so it can be unit-tested; {@link GeoDraw} draws it.
 *
 * <p>Coordinates are the file's: pixels (1/16 block), y up, the model's front on the -z side (the north face), +x the model's left,
 * origin at the feet. Face order of the per-face uv array is the vanilla polygon order below.</p>
 */
public final class GeoModelData {
    /** Vanilla ModelPart.Cube polygon order. Physical sides, in the model's own frame: WEST = -x (the model's right), EAST = +x, NORTH = -z (front), SOUTH = +z (back), TOP = +y, BOTTOM = -y. */
    public static final int WEST = 0, NORTH = 1, EAST = 2, SOUTH = 3, TOP = 4, BOTTOM = 5;

    public static final class Cube {
        public float ox, oy, oz, sx, sy, sz, inflate;
        public boolean mirror, hasRot;
        /** Absolute pivot and rotation (degrees) of the cube itself, only used when {@link #hasRot}. */
        public float px, py, pz, rx, ry, rz;
        /** Per face {u1, v1, u2, v2} in texture pixels (order WEST, NORTH, EAST, SOUTH, TOP, BOTTOM); null = the face is not drawn. */
        public final float[][] uv = new float[6][];
    }

    public static final class Bone {
        public String name, parent;
        public float px, py, pz, rx, ry, rz, inflate;
        public boolean mirror;
        public final List<Cube> cubes = new ArrayList<>();
        public final List<Bone> children = new ArrayList<>();
    }

    public final String id;
    public final int texW, texH;
    public final List<Bone> roots = new ArrayList<>();
    public final Map<String, Bone> bones = new LinkedHashMap<>();

    private GeoModelData(String id, int texW, int texH) {
        this.id = id;
        this.texW = Math.max(1, texW);
        this.texH = Math.max(1, texH);
    }

    public static GeoModelData empty(String id) { return new GeoModelData(id, 16, 16); }

    public int cubeCount() {
        int n = 0;
        for (Bone b : bones.values()) n += b.cubes.size();
        return n;
    }

    // ================================================================ parsing
    public static GeoModelData parse(String id, JsonObject root) {
        JsonObject geo = null;
        int tw = 64, th = 64;
        if (root.has("minecraft:geometry") && root.get("minecraft:geometry").isJsonArray()) {
            JsonArray a = root.getAsJsonArray("minecraft:geometry");
            if (a.size() > 0) geo = a.get(0).getAsJsonObject();
            if (geo != null && geo.has("description")) {
                JsonObject d = geo.getAsJsonObject("description");
                tw = intOf(d, "texture_width", tw);
                th = intOf(d, "texture_height", th);
            }
        } else {
            for (Map.Entry<String, JsonElement> e : root.entrySet()) {
                if (e.getKey().startsWith("geometry.") && e.getValue().isJsonObject()) {
                    geo = e.getValue().getAsJsonObject();
                    tw = intOf(geo, "texturewidth", tw);
                    th = intOf(geo, "textureheight", th);
                    break;
                }
            }
        }
        GeoModelData m = new GeoModelData(id, tw, th);
        if (geo == null || !geo.has("bones")) return m;
        for (JsonElement be : geo.getAsJsonArray("bones")) {
            JsonObject bo = be.getAsJsonObject();
            Bone b = new Bone();
            b.name = bo.has("name") ? bo.get("name").getAsString() : "bone" + m.bones.size();
            b.parent = bo.has("parent") ? bo.get("parent").getAsString() : null;
            float[] pv = vec(bo, "pivot", 0, 0, 0), rot = vec(bo, "rotation", 0, 0, 0);
            b.px = pv[0]; b.py = pv[1]; b.pz = pv[2];
            b.rx = rot[0]; b.ry = rot[1]; b.rz = rot[2];
            b.mirror = bo.has("mirror") && bo.get("mirror").getAsBoolean();
            b.inflate = bo.has("inflate") ? bo.get("inflate").getAsFloat() : 0f;
            if (bo.has("cubes")) for (JsonElement ce : bo.getAsJsonArray("cubes")) b.cubes.add(cube(ce.getAsJsonObject(), b, tw, th));
            m.bones.put(b.name, b);
        }
        for (Bone b : m.bones.values()) {
            Bone p = b.parent == null ? null : m.bones.get(b.parent);
            if (p == null || p == b) m.roots.add(b); else p.children.add(b);
        }
        return m;
    }

    private static Cube cube(JsonObject co, Bone bone, int tw, int th) {
        Cube c = new Cube();
        float[] o = vec(co, "origin", 0, 0, 0), s = vec(co, "size", 0, 0, 0);
        c.ox = o[0]; c.oy = o[1]; c.oz = o[2];
        c.sx = s[0]; c.sy = s[1]; c.sz = s[2];
        c.inflate = co.has("inflate") ? co.get("inflate").getAsFloat() : bone.inflate;
        c.mirror = co.has("mirror") ? co.get("mirror").getAsBoolean() : bone.mirror;
        if (co.has("rotation")) {
            float[] r = vec(co, "rotation", 0, 0, 0), p = vec(co, "pivot", bone.px, bone.py, bone.pz);
            c.hasRot = true;
            c.rx = r[0]; c.ry = r[1]; c.rz = r[2];
            c.px = p[0]; c.py = p[1]; c.pz = p[2];
        }
        JsonElement uv = co.get("uv");
        if (uv != null && uv.isJsonObject()) {                                  // per face
            JsonObject f = uv.getAsJsonObject();
            // Bedrock names its sides from the other end of the x axis: "east" is the model's -x side (vanilla WEST), "west" is +x.
            face(c, WEST, f.get("east"));
            face(c, NORTH, f.get("north"));
            face(c, EAST, f.get("west"));
            face(c, SOUTH, f.get("south"));
            face(c, TOP, f.get("up"));
            face(c, BOTTOM, f.get("down"));
            if (c.uv[BOTTOM] != null) { float t = c.uv[BOTTOM][1]; c.uv[BOTTOM][1] = c.uv[BOTTOM][3]; c.uv[BOTTOM][3] = t; }   // the bottom is stored flipped, like the box layout
        } else {
            float u = 0, v = 0;
            if (uv != null && uv.isJsonArray() && uv.getAsJsonArray().size() >= 2) { u = uv.getAsJsonArray().get(0).getAsFloat(); v = uv.getAsJsonArray().get(1).getAsFloat(); }
            box(c, u, v);
        }
        return c;
    }

    /** The vanilla box layout (ModelPart.Cube): u,v is the top-left of the unfolded cube. */
    static void box(Cube c, float u, float v) {
        float w = c.sx, h = c.sy, d = c.sz;
        float f4 = u, f5 = u + d, f6 = u + d + w, f7 = u + d + w + w, f8 = u + d + w + d, f9 = u + d + w + d + w;
        float f10 = v, f11 = v + d, f12 = v + d + h;
        c.uv[WEST] = new float[]{f4, f11, f5, f12};
        c.uv[NORTH] = new float[]{f5, f11, f6, f12};
        c.uv[EAST] = new float[]{f6, f11, f8, f12};
        c.uv[SOUTH] = new float[]{f8, f11, f9, f12};
        c.uv[TOP] = new float[]{f5, f10, f6, f11};
        c.uv[BOTTOM] = new float[]{f6, f11, f7, f10};
    }

    private static void face(Cube c, int index, JsonElement fe) {
        if (fe == null || !fe.isJsonObject()) return;
        JsonObject f = fe.getAsJsonObject();
        float[] uv = vec2(f, "uv"), size = vec2(f, "uv_size");
        c.uv[index] = new float[]{uv[0], uv[1], uv[0] + size[0], uv[1] + size[1]};
    }

    private static float[] vec2(JsonObject o, String key) {
        float[] r = {0, 0};
        if (o.has(key) && o.get(key).isJsonArray()) {
            JsonArray a = o.getAsJsonArray(key);
            for (int i = 0; i < 2 && i < a.size(); i++) r[i] = a.get(i).getAsFloat();
        }
        return r;
    }

    private static float[] vec(JsonObject o, String key, float dx, float dy, float dz) {
        float[] r = {dx, dy, dz};
        if (o.has(key) && o.get(key).isJsonArray()) {
            JsonArray a = o.getAsJsonArray(key);
            for (int i = 0; i < 3 && i < a.size(); i++) r[i] = a.get(i).getAsFloat();
        }
        return r;
    }

    private static int intOf(JsonObject o, String key, int def) { return o.has(key) ? o.get(key).getAsInt() : def; }
}
