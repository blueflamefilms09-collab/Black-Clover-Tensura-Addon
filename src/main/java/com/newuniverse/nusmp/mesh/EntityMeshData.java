package com.newuniverse.nusmp.mesh;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Boss rework wave 2 (docs/bosses/rework_wave2_models.md): an entity body made in Blender ({@code tools/blender/bl_entity.py}), split into parts the client turns about
 * their pivots. The file ({@code assets/anime_multiverse/entity_meshes/<id>.txt}) uses the quad lines of the weapon meshes ({@code qn layer (x y z u v nx ny nz) x4},
 * u and v in 128 units) plus a few header lines:
 * <pre>
 * entity &lt;id&gt; &lt;scale&gt;                      model units to blocks (1/16: the model is in pixels)
 * glow &lt;RRGGBB&gt; &lt;speed&gt; &lt;min&gt; &lt;max&gt;        the glow pass's colour and pulse
 * pivot &lt;part&gt; &lt;parent or -&gt; &lt;x y z&gt;     one per part, a parent always before its children
 * part &lt;part&gt;                              the quads that follow belong to it (layer 0 = body, 2 = glow copy)
 * </pre>
 * Pure (no Minecraft class), so the asset test reads the shipped file with this very parser. Vertices are packed 8 floats each, 4 per quad, per part and layer.
 */
public final class EntityMeshData {
    public static final int BODY = 0, GLOW = 2;
    /** The most quad lines (body and glow together) one entity mesh may have: the per-entity draw budget (rule 11). */
    public static final int MAX_QUADS = 3000;
    public static final int STRIDE = 8;

    public final String id;
    public final float scale;
    public final int glowRgb;
    public final float glowSpeed, glowMin, glowMax;
    public final String[] parts;
    /** The parent part index of each part, -1 for a root. */
    public final int[] parent;
    /** x, y, z of each part's pivot (model units). */
    public final float[] pivot;
    /** [part] packed vertices of the body layer and of the glow layer. */
    public final float[][] body, glow;

    private EntityMeshData(String id, float scale, int glowRgb, float glowSpeed, float glowMin, float glowMax, List<String> parts, List<Integer> parent, List<float[]> pivot,
                           List<float[]> body, List<float[]> glow) {
        this.id = id;
        this.scale = scale;
        this.glowRgb = glowRgb;
        this.glowSpeed = glowSpeed;
        this.glowMin = glowMin;
        this.glowMax = glowMax;
        this.parts = parts.toArray(new String[0]);
        this.parent = new int[parts.size()];
        this.pivot = new float[parts.size() * 3];
        for (int i = 0; i < parts.size(); i++) {
            this.parent[i] = parent.get(i);
            System.arraycopy(pivot.get(i), 0, this.pivot, i * 3, 3);
        }
        this.body = body.toArray(new float[0][]);
        this.glow = glow.toArray(new float[0][]);
    }

    /** The index of a part by name, or -1. */
    public int part(String name) {
        for (int i = 0; i < parts.length; i++) if (parts[i].equals(name)) return i;
        return -1;
    }

    public int quads(int layer) {
        float[][] l = layer == GLOW ? glow : body;
        int n = 0;
        for (float[] p : l) n += p.length / (STRIDE * 4);
        return n;
    }

    public int quads() { return quads(BODY) + quads(GLOW); }

    /** The lowest and highest y of the body in blocks (the model's height). */
    public float[] heightBlocks() {
        float lo = Float.MAX_VALUE, hi = -Float.MAX_VALUE;
        for (float[] p : body) for (int i = 1; i < p.length; i += STRIDE) { lo = Math.min(lo, p[i]); hi = Math.max(hi, p[i]); }
        return new float[]{lo * scale, hi * scale};
    }

    /** Reads a file; throws IllegalArgumentException when it is broken, over {@link #MAX_QUADS}, or a part has no pivot. */
    public static EntityMeshData parse(BufferedReader in) throws IOException {
        String id = null;
        float scale = 1f / 16f, gs = 0.1f, gmin = 0.5f, gmax = 1f;
        int rgb = 0xFFFFFF;
        List<String> names = new ArrayList<>();
        List<Integer> parents = new ArrayList<>();
        List<float[]> pivots = new ArrayList<>();
        List<FloatList> body = new ArrayList<>(), glow = new ArrayList<>();
        int cur = -1, quads = 0;
        String line;
        while ((line = in.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] v = line.split("\\s+");
            switch (v[0]) {
                case "entity" -> {
                    need(v, 3, line);
                    id = v[1];
                    scale = Float.parseFloat(v[2]);
                }
                case "glow" -> {
                    need(v, 5, line);
                    rgb = Integer.parseInt(v[1], 16);
                    gs = Float.parseFloat(v[2]);
                    gmin = Float.parseFloat(v[3]);
                    gmax = Float.parseFloat(v[4]);
                }
                case "pivot" -> {
                    need(v, 6, line);
                    if (names.contains(v[1])) throw new IllegalArgumentException("pivot twice: " + v[1]);
                    int p = v[2].equals("-") ? -1 : names.indexOf(v[2]);
                    if (!v[2].equals("-") && p < 0) throw new IllegalArgumentException("the parent of " + v[1] + " must come before it: " + v[2]);
                    names.add(v[1]);
                    parents.add(p);
                    pivots.add(new float[]{Float.parseFloat(v[3]), Float.parseFloat(v[4]), Float.parseFloat(v[5])});
                    body.add(new FloatList());
                    glow.add(new FloatList());
                }
                case "part" -> {
                    need(v, 2, line);
                    cur = names.indexOf(v[1]);
                    if (cur < 0) throw new IllegalArgumentException("part without a pivot: " + v[1]);
                }
                case "qn" -> {
                    need(v, 2 + 4 * STRIDE, line);
                    if (cur < 0) throw new IllegalArgumentException("a quad before the first part line");
                    int layer = Integer.parseInt(v[1]);
                    if (layer != BODY && layer != GLOW) continue;
                    if (++quads > MAX_QUADS) throw new IllegalArgumentException("over " + MAX_QUADS + " quads");
                    FloatList dst = (layer == GLOW ? glow : body).get(cur);
                    for (int k = 2; k < 2 + 4 * STRIDE; k++) dst.add(Float.parseFloat(v[k]));
                }
                default -> { }
            }
        }
        if (id == null || names.isEmpty()) throw new IllegalArgumentException("no entity or pivot line");
        List<float[]> b = new ArrayList<>(), g = new ArrayList<>();
        for (int i = 0; i < names.size(); i++) {
            b.add(body.get(i).toArray());
            g.add(glow.get(i).toArray());
        }
        return new EntityMeshData(id, scale, rgb, gs, gmin, gmax, names, parents, pivots, b, g);
    }

    private static void need(String[] v, int n, String line) {
        if (v.length < n) throw new IllegalArgumentException("short line: " + line);
    }

    /** A growable float array (the loader runs once per resource reload). */
    private static final class FloatList {
        private float[] a = new float[256];
        private int n;

        void add(float f) {
            if (n == a.length) a = java.util.Arrays.copyOf(a, n * 2);
            a[n++] = f;
        }

        float[] toArray() { return java.util.Arrays.copyOf(a, n); }
    }
}
