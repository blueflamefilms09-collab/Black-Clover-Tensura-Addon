package com.newuniverse.nusmp.visual.genesis;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A baked upgraded weapon mesh (assets/nusmp/weapon_meshes_vu/&lt;id&gt;.txt, from tools/visual_upgrade/genesis_weapons): the dialect of the game's weapon meshes
 * (docs/relic_format.md) with five layers and animation groups. Lines:
 * <pre>
 *   vu 1                               format version
 *   place ox oy angle scale ymin       the placement on the old sprite's diagonal (the old mesh's own line, so every display transform still fits)
 *   glow RRGGBB speed min max          the tint and pulse of the core glow
 *   skin &lt;name&gt;                       textures/entity/weapon_vu/&lt;name&gt;[_glow|_veins].png
 *   lod &lt;mesh id&gt;                     the level-of-detail mesh
 *   fl x y z size RRGGBB cell phase pulse    a camera-facing flare (a cell of halo.png)
 *   g &lt;group&gt;                          the quads below belong to this animation group ("-" = the static body)
 *   qn layer (x y z u v nx ny nz) * 4  a quad: layer 0 body, 1 void, 2 core glow, 3 veins, 4 halo cards
 * </pre>
 * UVs are in a 128 x 128 space whatever the size of the PNG. No Minecraft classes: the client draws from these arrays and the tests read the same file.
 */
public final class VuMesh {
    public static final int BODY = 0, VOID = 1, GLOW = 2, VEINS = 3, HALO = 4, LAYERS = 5;
    public static final int FLOATS = 8;                                                   // x y z u v nx ny nz per vertex

    public float ox, oy, angle, scale = 1f, ymin;
    public int glowRgb = 0xFFFFFF;
    public float glowSpeed = 0.14f, glowMin = 0.45f, glowMax = 1f;
    public String skin = "", lod = "";
    public final List<Flare> flares = new ArrayList<>();
    public final List<Group> groups = new ArrayList<>();
    /** y range of the body quads (model units) and the length that follows (blocks, with `scale`). */
    public float bodyMinY = Float.MAX_VALUE, bodyMaxY = -Float.MAX_VALUE;

    public record Flare(float x, float y, float z, float size, int rgb, int cell, float phase, float pulse) {}

    /** One animation group: its vertices per layer as flat arrays of {@link #FLOATS} floats per vertex, four vertices per quad. */
    public static final class Group {
        public final String name;
        public final float[][] verts = new float[LAYERS][];
        public final int[] count = new int[LAYERS];                                      // floats used per layer

        Group(String name) {
            this.name = name;
            for (int i = 0; i < LAYERS; i++) verts[i] = new float[0];
        }

        public int quads(int layer) { return count[layer] / (FLOATS * 4); }

        public boolean empty() {
            for (int i = 0; i < LAYERS; i++) if (count[i] > 0) return false;
            return true;
        }

        void add(int layer, float[] q) {
            if (count[layer] + q.length > verts[layer].length) verts[layer] = Arrays.copyOf(verts[layer], Math.max(256, (count[layer] + q.length) * 2));
            System.arraycopy(q, 0, verts[layer], count[layer], q.length);
            count[layer] += q.length;
        }
    }

    // ---------------------------------------------------------------- parsing
    public static VuMesh parse(BufferedReader in) throws IOException {
        VuMesh m = new VuMesh();
        Group cur = null;
        String line;
        float[] q = new float[FLOATS * 4];
        while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.charAt(0) == '#') continue;
            String[] v = line.split("\\s+");
            switch (v[0]) {
                case "place" -> {
                    m.ox = Float.parseFloat(v[1]);
                    m.oy = Float.parseFloat(v[2]);
                    m.angle = Float.parseFloat(v[3]);
                    m.scale = Float.parseFloat(v[4]);
                    m.ymin = Float.parseFloat(v[5]);
                }
                case "glow" -> {
                    m.glowRgb = Integer.parseInt(v[1], 16);
                    m.glowSpeed = Float.parseFloat(v[2]);
                    m.glowMin = Float.parseFloat(v[3]);
                    m.glowMax = Float.parseFloat(v[4]);
                }
                case "skin" -> m.skin = v[1];
                case "lod" -> m.lod = v[1];
                case "fl" -> m.flares.add(new Flare(Float.parseFloat(v[1]), Float.parseFloat(v[2]), Float.parseFloat(v[3]), Float.parseFloat(v[4]), Integer.parseInt(v[5], 16),
                        Integer.parseInt(v[6]), Float.parseFloat(v[7]), v.length > 8 ? Float.parseFloat(v[8]) : 0.3f));
                case "g" -> {
                    String name = v.length > 1 ? v[1] : "-";
                    cur = m.group(name);
                }
                case "qn" -> {
                    if (cur == null) cur = m.group("-");
                    int layer = Integer.parseInt(v[1]);
                    for (int k = 0; k < 4; k++) {
                        for (int j = 0; j < FLOATS; j++) q[k * FLOATS + j] = Float.parseFloat(v[2 + k * FLOATS + j]);
                        if (layer == BODY) {
                            float y = q[k * FLOATS + 1];
                            m.bodyMinY = Math.min(m.bodyMinY, y);
                            m.bodyMaxY = Math.max(m.bodyMaxY, y);
                        }
                    }
                    cur.add(layer, q);
                }
                default -> { }
            }
        }
        return m;
    }

    private Group group(String name) {
        for (Group g : groups) if (g.name.equals(name)) return g;
        Group g = new Group(name);
        groups.add(g);
        return g;
    }

    // ---------------------------------------------------------------- measures
    public int quads(int layer) {
        int n = 0;
        for (Group g : groups) n += g.quads(layer);
        return n;
    }

    /** Welded vertices of the given layers (positions rounded to 1/1000), what an index buffer would hold: the number the vertex caps are about. */
    public int weldedVertices(int... layers) {
        Set<Long> seen = new HashSet<>();
        for (Group g : groups) {
            for (int layer : layers) {
                float[] a = g.verts[layer];
                for (int i = 0; i < g.count[layer]; i += FLOATS) {
                    long kx = Math.round(a[i] * 1000f), ky = Math.round(a[i + 1] * 1000f), kz = Math.round(a[i + 2] * 1000f);
                    seen.add((kx & 0x1FFFFFL) | (ky & 0x1FFFFFL) << 21 | (kz & 0x1FFFFFL) << 42);
                }
            }
        }
        return seen.size();
    }

    /** Vertices the renderer submits for one frame at full detail: four per quad over all layers (the void, the glow and the veins are drawn again over the body). */
    public int submittedVertices() {
        int n = 0;
        for (int l = 0; l < LAYERS; l++) n += quads(l) * 4;
        return n;
    }

    /** Length of the weapon along its axis in item units (the body's y range times the place scale). */
    public float length() { return bodyMaxY > bodyMinY ? (bodyMaxY - Math.min(bodyMinY, ymin)) * scale : 0f; }

    public int animatedGroups() {
        int n = 0;
        for (Group g : groups) if (!g.name.equals("-")) n++;
        return n;
    }
}
