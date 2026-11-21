package com.newuniverse.nusmp.grimoire;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The revamped grimoire (docs/grimoire_revamp.md, "visual.classicGrimoires" = false): the same book frame as {@link GrimoireBookPlan}, but the body is the mesh Blender made
 * (tools/blender/grimoire_revamp.py, assets/anime_multiverse/grimoire_revamp/revamp_mesh.txt): thick bevelled boards with a stepped cover (board, metal frame ring, leather plateau),
 * metal corner caps, a round banded spine, a page block of staggered slabs, a ribbon bookmark and an octagonal emblem plate. Its texture is one baked atlas, grey on purpose: the item
 * colour handler tints each quad by its class (leather = cover colour, metal and frame ring = trim colour, ribbon = emblem colour, pages untinted and glowing while held).
 *
 * <p>What the art pack and the magic add is still laid on top by {@link GrimoireBookPlan#design} and the emblem quad (cover design, magic sigil, studs, fusion filigree), over the plateau and the
 * plate dish whose numbers the mesh file carries on its {@code meta} lines. If the mesh file cannot be read, {@link #build} returns the classic plan, so a broken pack never breaks the book.
 * Pure: no game classes; the client turns each quad into a BakedQuad.
 */
public final class GrimoireRevampPlan {
    public static final String ATLAS = "revamp_atlas", ATLAS_TATTERED = "revamp_atlas_tattered";
    static final String RESOURCE = "/assets/anime_multiverse/grimoire_revamp/revamp_mesh.txt";

    /** Quad budgets of the revamped book (the classic one was 140 closed and 420 open, test-checked). The open book is double-sided: every quad is drawn twice. */
    public static final int MAX_CLOSED_QUADS = 420, MAX_OPEN_QUADS = 760;

    /** Overlay numbers of one cover: its top plane z, the flat rectangle the cover design is laid on, the plate centre and the plate dish height. */
    public record Cover(float z, float x0, float y0, float x1, float y1, float cx, float cy, float dish) {}

    /** One mesh quad: class letter, 4 corners (xyz), uv (0..1). */
    record Raw(char cls, float[] pos, float[] uv) {}

    /** The parsed mesh file. */
    public record Mesh(Map<String, List<Raw>> poses, Map<String, Cover> covers, Map<String, float[]> pages) {}

    private static volatile Mesh cached;
    private static volatile boolean failed;

    private GrimoireRevampPlan() {}

    /** The mesh, read once; null if the file is missing or broken. */
    public static Mesh mesh() {
        Mesh m = cached;
        if (m != null || failed) return m;
        synchronized (GrimoireRevampPlan.class) {
            if (cached == null && !failed) {
                try (InputStream in = GrimoireRevampPlan.class.getResourceAsStream(RESOURCE)) {
                    if (in == null) failed = true; else cached = parse(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)));
                } catch (IOException | RuntimeException e) {
                    failed = true;
                }
            }
            return cached;
        }
    }

    /** Parses the mesh text (see tools/blender/grimoire_revamp.py for the format). */
    public static Mesh parse(BufferedReader r) throws IOException {
        Map<String, List<Raw>> poses = new HashMap<>();
        Map<String, Cover> covers = new HashMap<>();
        Map<String, float[]> pages = new HashMap<>();
        List<Raw> cur = null;
        for (String line; (line = r.readLine()) != null; ) {
            line = line.strip();
            if (line.isEmpty() || line.startsWith("#")) continue;
            String[] t = line.split("\\s+");
            switch (t[0]) {
                case "meta" -> {
                    float[] v = new float[t.length - 2];
                    for (int i = 0; i < v.length; i++) v[i] = Float.parseFloat(t[i + 2]);
                    if (t[1].startsWith("open.pages")) pages.put(t[1], v);
                    else covers.put(t[1], new Cover(v[0], v[1], v[2], v[3], v[4], v[5], v[6], v[7]));
                }
                case "mesh" -> poses.put(t[1], cur = new ArrayList<>());
                case "q" -> {
                    if (cur == null || t.length != 2 + 20) throw new IllegalArgumentException("bad quad line: " + line);
                    float[] pos = new float[12], uv = new float[8];
                    for (int i = 0; i < 4; i++) {
                        for (int c = 0; c < 3; c++) pos[i * 3 + c] = Float.parseFloat(t[2 + i * 5 + c]);
                        uv[i * 2] = Float.parseFloat(t[2 + i * 5 + 3]);
                        uv[i * 2 + 1] = Float.parseFloat(t[2 + i * 5 + 4]);
                    }
                    cur.add(new Raw(t[1].charAt(0), pos, uv));
                }
                default -> throw new IllegalArgumentException("unknown line: " + line);
            }
        }
        for (String need : new String[]{"closed", "open1", "open2", "open3"}) if (!poses.containsKey(need)) throw new IllegalArgumentException("missing mesh " + need);
        for (String need : new String[]{"closed.front", "closed.back", "open.right", "open.left"}) if (!covers.containsKey(need)) throw new IllegalArgumentException("missing meta " + need);
        return new Mesh(poses, covers, pages);
    }

    public static List<GrimoireBookPlan.Quad> build(BookLook.Key key) {
        Mesh m = mesh();
        if (m == null) return GrimoireBookPlan.build(key);
        return build(m, key);
    }

    public static List<GrimoireBookPlan.Quad> build(Mesh m, BookLook.Key key) {
        return key.open() > 0 ? buildOpen(m, key) : buildClosed(m, key);
    }

    private static String atlas(BookLook.Key key) { return key.tattered() ? ATLAS_TATTERED : ATLAS; }

    private static List<GrimoireBookPlan.Quad> buildClosed(Mesh m, BookLook.Key key) {
        List<GrimoireBookPlan.Quad> out = new ArrayList<>(m.poses.get("closed").size() + 40);
        body(out, m.poses.get("closed"), key);
        Cover f = m.covers.get("closed.front"), b = m.covers.get("closed.back");
        List<GrimoireBookPlan.Quad> front = new ArrayList<>();
        overlay(front, f, +1, key, true);
        List<GrimoireBookPlan.Quad> rear = new ArrayList<>();
        overlay(rear, b, -1, key, false);
        out.addAll(front);
        out.addAll(rear);
        return Collections.unmodifiableList(out);
    }

    private static List<GrimoireBookPlan.Quad> buildOpen(Mesh m, BookLook.Key key) {
        int step = Math.max(1, Math.min(GrimoireBookPlan.OPEN_STEPS, key.open()));
        List<GrimoireBookPlan.Quad> out = new ArrayList<>(2 * m.poses.get("open" + step).size() + 120);
        List<GrimoireBookPlan.Quad> body = new ArrayList<>();
        body(body, m.poses.get("open" + step), key);
        float th = (float) Math.toRadians(GrimoireBookPlan.OPEN_DEGREES[step]);
        float cx = GrimoireBookPlan.OPEN_CX, cz = GrimoireBookPlan.OPEN_CZ;
        for (int side : new int[]{1, -1}) {
            List<GrimoireBookPlan.Quad> half = new ArrayList<>();
            Cover c = m.covers.get(side > 0 ? "open.right" : "open.left");
            overlay(half, c, +1, key, side > 0);
            float[] pg = m.pages.get(side > 0 ? "open.pagesR" : "open.pagesL");
            if (key.held() && pg != null)                         // glowing spell runes on the open pages while the book is held (the cast state)
                half.add(GrimoireBookPlan.quadOf(GrimoireBookPlan.Face.NORTH, pg[0] + 0.5f, GrimoireBookPlan.PY0 + 0.6f, pg[1] - 0.5f, GrimoireBookPlan.PY1 - 0.6f,
                        pg[2] - 0.03f, "page_runes", BookLook.TINT_TRIM, true));
            for (GrimoireBookPlan.Quad q : half) body.add(GrimoireBookPlan.turn(q, cx, cz, side * th));
        }
        for (GrimoireBookPlan.Quad q : body) {                    // double-sided: no viewing angle can cull a half away
            out.add(q);
            out.add(GrimoireBookPlan.backFace(q));
        }
        return Collections.unmodifiableList(out);
    }

    /** The cover design, the emblem and (front only) nothing else: laid over the plateau (z plane) and the plate dish. */
    private static void overlay(List<GrimoireBookPlan.Quad> out, Cover c, int dir, BookLook.Key key, boolean emblem) {
        GrimoireBookPlan.design(out, dir > 0 ? GrimoireBookPlan.Face.SOUTH : GrimoireBookPlan.Face.NORTH, c.x0, c.y0, c.x1, c.y1, c.z, dir, key);
        if (!emblem) return;
        float e = GrimoireBookPlan.EMB * (c.x1 - c.x0 < 6f ? 0.8f : 1f);
        out.add(GrimoireBookPlan.quadOf(GrimoireBookPlan.Face.SOUTH, c.cx - e, c.cy - e, c.cx + e, c.cy + e, c.dish + 0.02f, "emblem_" + key.emblem().toLowerCase(),
                BookLook.TINT_EMBLEM, key.held()));
    }

    /** The Blender body quads of a pose, tinted by class; the frame ring is dropped on a tattered book (as the classic frame was). */
    private static void body(List<GrimoireBookPlan.Quad> out, List<Raw> raws, BookLook.Key key) {
        String tex = atlas(key);
        for (Raw r : raws) {
            if (r.cls == 'F' && key.tattered()) continue;
            int tint = switch (r.cls) { case 'L' -> BookLook.TINT_COVER; case 'M', 'F' -> BookLook.TINT_TRIM; case 'R' -> BookLook.TINT_EMBLEM; default -> -1; };
            float[] n = normal(r.pos);
            float[] uv = new float[8];
            for (int i = 0; i < 8; i++) uv[i] = r.uv[i] * 16f;
            out.add(new GrimoireBookPlan.Quad(r.pos, uv, tex, tint, r.cls == 'P' && key.held(), GrimoireBookPlan.nearest(n[0], n[1], n[2]), n));
        }
    }

    /** The outward unit normal of a (possibly triangular) quad: Newell's sum, so a repeated corner is no problem. */
    static float[] normal(float[] p) {
        float nx = 0, ny = 0, nz = 0;
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            float ax = p[i * 3], ay = p[i * 3 + 1], az = p[i * 3 + 2], bx = p[j * 3], by = p[j * 3 + 1], bz = p[j * 3 + 2];
            nx += (ay - by) * (az + bz);
            ny += (az - bz) * (ax + bx);
            nz += (ax - bx) * (ay + by);
        }
        float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        return l < 1e-9f ? new float[]{0, 0, 1} : new float[]{nx / l, ny / l, nz / l};
    }

    /** Every texture the revamped plan can use beyond the classic ones (for the model loader to resolve). */
    public static List<String> textures() { return List.of(ATLAS, ATLAS_TATTERED); }
}
