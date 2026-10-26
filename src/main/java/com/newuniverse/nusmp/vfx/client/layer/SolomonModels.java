package com.newuniverse.nusmp.vfx.client.layer;

import java.util.Arrays;

/**
 * 3.2x.0: the REAL 3D MODELS of the Solomon ultimate (the pillars of the 72, the floating grimoire, the links of a spell chain and the sigil head at its end, the signet ring, the rune crystal and the broken-glass
 * shards of a magic circle), as PURE geometry: no Minecraft class is named here, so the meshes unit-test and preview headless ({@code SolomonModelsTest}, {@code python tools/vfx_preview/preview.py solomon_pillar_close ...}).
 * They are drawn by {@link SolomonMeshDraw} inside {@link SolomonLayer}; the material atlas is {@code textures/particle/solomon_model_atlas.png} ({@code tools/gen_solomon_model_textures.py}: violet marble with gold
 * trim and glowing script, a violet-leather grimoire with parchment pages, indigo steel links with a glowing rune inlay, amethyst facets, antique-gold signet band, pale rune crystal and lilac glass).
 *
 * <p>A mesh is built ONCE (static, at class load) from boxes, frusta and flat facets: every face is a quad (a triangle repeats its last corner), stored in the local space of one of its PARTS. Parts form a small skeleton
 * (the covers and turning pages of the grimoire, its ribbon): a part sits at a pivot in its parent and is turned by a bend (about its X axis) and a spread (about its Y axis). Nothing here runs per frame except
 * {@link Posed#prepare}, which writes into arrays that belong to the {@link Posed} and are allocated once.
 *
 * <p><b>Budget (rule 11).</b> A closed solid shows only the faces that look at the camera: {@link Posed#prepare} drops the others and the rest is drawn far to near (painter's order, the vertex buffer has no depth write).
 * The constants {@link #PILLAR_MAX} ... {@link #SHARD_MAX} are the most quads a view of each model can keep (checked from 3000 random views by {@code SolomonModelsTest}); a model that does not fit the remaining buffer
 * budget is skipped whole, never drawn in part.
 *
 * <p>The switch {@code visual.solomonModels} (CLIENT config, default ON) is mirrored here like {@code FlatProps}: off = the layer draws exactly what 3.2x.0 drew before (the flat sprites).
 */
public final class SolomonModels {
    private SolomonModels() {}

    // ------------------------------------------------------------------ the switch (a mirror of the CLIENT config, written by SolomonModelsConfig on the client only)
    private static volatile boolean enabled = true;

    /** True (the default) when the 3D models are drawn; false = the flat sprites of the older versions. */
    public static boolean enabled() { return enabled; }

    /** Written by the client config only. */
    public static void setEnabled(boolean on) { enabled = on; }

    // ------------------------------------------------------------------ the atlas: 8 x 8 cells of 128 px (solomon_model_atlas.png), the first CELLS are painted
    public static final int COLS = 8, ROWS = 8, CELLS = 39;
    public static final int STONE = 0, STONE_DARK = 1, FLUTE = 2, TRIM = 3, CAP_TOP = 4, BASE_RING = 5,
            SIGIL0 = 6,                                                                  // 6..13: the eight element sigils (flame, tide, gale, stone, storm, frost, light, dark) on a framed panel
            COVER = 14, COVER_EMBLEM = 15, COVER_BACK = 16, PAGE = 17, PAGE_L = 18, PAGE_GLOW = 19, PAGE_EDGE = 20, RIBBON = 21, SPINE = 22,
            LINK_TOP = 23, LINK_SIDE = 24, GEM_A = 25, GEM_B = 26, GEM_C = 27, HEAD_PLATE = 28,
            RING_BAND = 29, RING_BAND_B = 30, BEZEL = 31, HALO = 32, CRYSTAL_A = 33, CRYSTAL_B = 34, SHARD = 35, SHARD_B = 36, SHARD_EDGE = 37, PAGE_GLOW_L = 38;

    /** The most quads a view of each model keeps after the faces turned away are dropped (tested from random views). */
    public static final int PILLAR_MAX = 12, GRIMOIRE_MAX = 19, LINK_MAX = 10, HEAD_MAX = 10, RING_MAX = 32, CRYSTAL_MAX = 5, SHARD_MAX = 4;

    /** Part ids of the grimoire (the pose is written with {@link #grimoirePose}). */
    public static final int G_ROOT = 0, G_FRONT = 1, G_PAGE_A1 = 2, G_PAGE_A2 = 3, G_PAGE_B1 = 4, G_PAGE_B2 = 5, G_RIB1 = 6, G_RIB2 = 7, G_PARTS = 8;
    /** The parts that carry the turning pages and the ribbon (hidden for the light version of the Realm). */
    public static final int G_LIVELY = (1 << G_PAGE_A1) | (1 << G_PAGE_A2) | (1 << G_PAGE_B1) | (1 << G_PAGE_B2) | (1 << G_RIB1) | (1 << G_RIB2);

    // ------------------------------------------------------------------ the mesh
    /** An immutable mesh: faces of four corners (x, y, z) in the local space of their part, with an atlas cell, a UV rectangle inside the cell, an emissive amount and a two-sided flag. */
    public static final class Mesh {
        public final String name;
        public final int faces, parts, maxDrawn;
        /** Faces of a part given in an order that points inward (a mistake in the model code; the test wants 0). */
        public final int badWinding;
        final int[] parent;
        final float[] pivot;
        final float[] pos;
        final int[] cell, part;
        final float[] rect, glow;
        final boolean[] two, shift;

        Mesh(String name, int maxDrawn, int parts, int[] parent, float[] pivot, int faces, float[] pos, int[] cell, int[] part, float[] rect, float[] glow, boolean[] two, boolean[] shift, int bad) {
            this.name = name;
            this.maxDrawn = maxDrawn;
            this.parts = parts;
            this.parent = parent;
            this.pivot = pivot;
            this.faces = faces;
            this.pos = pos;
            this.cell = cell;
            this.part = part;
            this.rect = rect;
            this.glow = glow;
            this.two = two;
            this.shift = shift;
            this.badWinding = bad;
        }

        public int cellOf(int face) { return cell[face]; }
        public boolean shiftOf(int face) { return shift[face]; }
        public float[] corners(int face) { return Arrays.copyOfRange(pos, face * 12, face * 12 + 12); }
        public float[] rectOf(int face) { return Arrays.copyOfRange(rect, face * 4, face * 4 + 4); }
    }

    /** Collects faces while a mesh is built (class-load time only). */
    private static final class Builder {
        float[] pos = new float[12 * 64];
        int[] cell = new int[64], part = new int[64];
        float[] rect = new float[4 * 64], glow = new float[64];
        boolean[] two = new boolean[64], shift = new boolean[64];
        int faces, cur, bad;
        final int[] parent = new int[16];
        final float[] pivot = new float[16 * 3];
        int parts;
        static final float[] FULL = {0f, 0f, 1f, 1f};

        int part(int parentId, float px, float py, float pz) {
            parent[parts] = parentId;
            pivot[parts * 3] = px;
            pivot[parts * 3 + 1] = py;
            pivot[parts * 3 + 2] = pz;
            cur = parts;
            return parts++;
        }

        void in(int partId) { cur = partId; }

        private void grow() {
            int n = cell.length * 2;
            pos = Arrays.copyOf(pos, n * 12);
            cell = Arrays.copyOf(cell, n);
            part = Arrays.copyOf(part, n);
            rect = Arrays.copyOf(rect, n * 4);
            glow = Arrays.copyOf(glow, n);
            two = Arrays.copyOf(two, n);
            shift = Arrays.copyOf(shift, n);
        }

        private static float[] v(float x, float y, float z) { return new float[]{x, y, z}; }

        private void store(int c, float gl, boolean twoSided, boolean sh, float[] r, float[] a, float[] b, float[] cc, float[] d) {
            if (faces >= cell.length) grow();
            int o = faces * 12;
            System.arraycopy(a, 0, pos, o, 3);
            System.arraycopy(b, 0, pos, o + 3, 3);
            System.arraycopy(cc, 0, pos, o + 6, 3);
            System.arraycopy(d, 0, pos, o + 9, 3);
            cell[faces] = c;
            part[faces] = cur;
            glow[faces] = gl;
            two[faces] = twoSided;
            shift[faces] = sh;
            System.arraycopy(r == null ? FULL : r, 0, rect, faces * 4, 4);
            faces++;
        }

        private static float side(float[] a, float[] b, float[] c, float[] d, float rx, float ry, float rz) {
            float nx = (c[1] - a[1]) * (d[2] - b[2]) - (c[2] - a[2]) * (d[1] - b[1]);
            float ny = (c[2] - a[2]) * (d[0] - b[0]) - (c[0] - a[0]) * (d[2] - b[2]);
            float nz = (c[0] - a[0]) * (d[1] - b[1]) - (c[1] - a[1]) * (d[0] - b[0]);
            float mx = (a[0] + b[0] + c[0] + d[0]) * 0.25f - rx, my = (a[1] + b[1] + c[1] + d[1]) * 0.25f - ry, mz = (a[2] + b[2] + c[2] + d[2]) * 0.25f - rz;
            return nx * mx + ny * my + nz * mz;
        }

        /**
         * An ORIENTED face: the corners are given bottom-left, bottom-right, top-right, top-left as seen from OUTSIDE (counter-clockwise), so the texture stands upright; {@code rot} turns the texture a quarter
         * per step. A face that points inward is flipped and counted in {@link #bad} (the test wants 0). {@code (rx, ry, rz)} is a point inside the solid.
         */
        void faceO(int c, float gl, boolean twoSided, boolean sh, float[] r, int rot, float[] bl, float[] br, float[] tr, float[] tl, float rx, float ry, float rz) {
            float[][] k = {bl, br, tr, tl};
            float[] a = k[rot & 3], b = k[(rot + 1) & 3], cc = k[(rot + 2) & 3], d = k[(rot + 3) & 3];
            if (!twoSided && side(a, b, cc, d, rx, ry, rz) < 0f) {
                bad++;
                store(c, gl, false, sh, r, a, d, cc, b);
            } else store(c, gl, twoSided, sh, r, a, b, cc, d);
        }

        /**
         * A face a-b-c-d (a triangle repeats its last corner) whose winding is chosen so it points away from the inside point: the texture keeps its up direction, its left-right may be mirrored. For facets
         * and bars where the picture has no handedness.
         */
        void faceA(int c, float gl, float[] r, float[] a, float[] b, float[] cc, float[] d, float rx, float ry, float rz) {
            if (side(a, b, cc, d, rx, ry, rz) < 0f) store(c, gl, false, false, r, b, a, d, cc);
            else store(c, gl, false, false, r, a, b, cc, d);
        }

        /** Side bits of {@link #box}: +X, -X, +Y, -Y, +Z, -Z. */
        static final int PX = 1, NX = 2, PY = 4, NY = 8, PZ = 16, NZ = 32, ALL = 63;

        /** An axis-aligned box; {@code cells[i]} / {@code rots[i]} are the cell and the quarter turns of the face i (+X, -X, +Y, -Y, +Z, -Z); the texture of every face stands upright (up = +Y; for +Y up = -Z, for -Y up = +Z). */
        void box(float x0, float x1, float y0, float y1, float z0, float z1, int mask, int[] cells, int[] rots, float gl, float[][] rects) {
            float rx = (x0 + x1) * 0.5f, ry = (y0 + y1) * 0.5f, rz = (z0 + z1) * 0.5f;
            if ((mask & PX) != 0) faceO(cells[0], gl, false, false, rects == null ? null : rects[0], rots[0], v(x1, y0, z1), v(x1, y0, z0), v(x1, y1, z0), v(x1, y1, z1), rx, ry, rz);
            if ((mask & NX) != 0) faceO(cells[1], gl, false, false, rects == null ? null : rects[1], rots[1], v(x0, y0, z0), v(x0, y0, z1), v(x0, y1, z1), v(x0, y1, z0), rx, ry, rz);
            if ((mask & PY) != 0) faceO(cells[2], gl, false, false, rects == null ? null : rects[2], rots[2], v(x0, y1, z1), v(x1, y1, z1), v(x1, y1, z0), v(x0, y1, z0), rx, ry, rz);
            if ((mask & NY) != 0) faceO(cells[3], gl, false, false, rects == null ? null : rects[3], rots[3], v(x0, y0, z0), v(x1, y0, z0), v(x1, y0, z1), v(x0, y0, z1), rx, ry, rz);
            if ((mask & PZ) != 0) faceO(cells[4], gl, false, false, rects == null ? null : rects[4], rots[4], v(x0, y0, z1), v(x1, y0, z1), v(x1, y1, z1), v(x0, y1, z1), rx, ry, rz);
            if ((mask & NZ) != 0) faceO(cells[5], gl, false, false, rects == null ? null : rects[5], rots[5], v(x1, y0, z0), v(x0, y0, z0), v(x0, y1, z0), v(x1, y1, z0), rx, ry, rz);
        }

        /**
         * An n-sided frustum standing on Y: radius r0 at y0 and r1 at y1 (circumradius), the first corner at angle {@code rot0}; the sides carry {@code cell} (+ the pillar's element when {@code sh}); a top cap is made
         * for a square (n = 4) only. The texture of a side stands upright.
         */
        void frustum(int n, float rot0, float cx, float cz, float r0, float y0, float r1, float y1, int cell, boolean sh, float gl, int topCell, float glTop, float[] rectSide) {
            float ry = (y0 + y1) * 0.5f;
            for (int i = 0; i < n; i++) {
                double a0 = rot0 + Math.PI * 2 * i / n, a1 = rot0 + Math.PI * 2 * (i + 1) / n;
                faceO(cell, gl, false, sh, rectSide, 0,
                        v(cx + (float) Math.cos(a1) * r0, y0, cz + (float) Math.sin(a1) * r0), v(cx + (float) Math.cos(a0) * r0, y0, cz + (float) Math.sin(a0) * r0),
                        v(cx + (float) Math.cos(a0) * r1, y1, cz + (float) Math.sin(a0) * r1), v(cx + (float) Math.cos(a1) * r1, y1, cz + (float) Math.sin(a1) * r1), cx, ry, cz);
            }
            if (topCell >= 0 && n == 4) {                                                  // seen from above (+X right, +Z down) the corners go clockwise with the angle: BL = v1, BR = v0, TR = v3, TL = v2
                float[][] p = new float[4][];
                for (int i = 0; i < 4; i++) p[i] = v(cx + (float) Math.cos(rot0 + Math.PI * i / 2) * r1, y1, cz + (float) Math.sin(rot0 + Math.PI * i / 2) * r1);
                faceO(topCell, glTop, false, false, null, 0, p[1], p[0], p[3], p[2], cx, ry, cz);
            }
        }

        Mesh build(String name, int maxDrawn) {
            int np = Math.max(1, parts);
            return new Mesh(name, maxDrawn, np, Arrays.copyOf(parent, np), Arrays.copyOf(pivot, np * 3), faces, Arrays.copyOf(pos, faces * 12), Arrays.copyOf(cell, faces),
                    Arrays.copyOf(part, faces), Arrays.copyOf(rect, faces * 4), Arrays.copyOf(glow, faces), Arrays.copyOf(two, faces), Arrays.copyOf(shift, faces), bad);
        }
    }

    private static float[] r(float u0, float v0, float u1, float v1) { return new float[]{u0, v0, u1, v1}; }

    // ================================================================== the pillar of the 72: model units are blocks, +Y up, standing on y = 0, about 2.9 tall and 1.5 wide at the plinth
    public static final Mesh PILLAR = buildPillar();
    /** Where the capital top and the base ring are (the layer puts glows there). */
    public static final float PILLAR_TOP = 2.72f, PILLAR_BASE_RING = 0.78f;

    private static Mesh buildPillar() {
        Builder b = new Builder();
        b.part(-1, 0f, 0f, 0f);
        // the base ring: a flat glowing seal under the plinth (a ring of runes, alpha, one quad)
        float hr = PILLAR_BASE_RING, yr = 0.014f;
        b.faceO(BASE_RING, 0.7f, false, false, null, 0, Builder.v(-hr, yr, hr), Builder.v(hr, yr, hr), Builder.v(hr, yr, -hr), Builder.v(-hr, yr, -hr), 0f, -1f, 0f);
        // the plinth: a square frustum (four sides, one top), dark marble with a gold line
        b.frustum(4, (float) Math.PI / 4, 0f, 0f, 0.64f, 0.0f, 0.54f, 0.34f, STONE_DARK, false, 0.12f, STONE, 0.1f, null);
        // the shaft: an octagon that narrows a little toward the top, fluted marble with a glowing inscription
        b.frustum(8, (float) Math.PI / 8, 0f, 0f, 0.37f, 0.34f, 0.30f, 2.30f, FLUTE, false, 0.22f, -1, 0f, null);
        // the capital: a square that flares out, the sigil band of the pillar's element on every side, and a glowing inscribed top
        b.frustum(4, (float) Math.PI / 4, 0f, 0f, 0.46f, 2.30f, 0.74f, PILLAR_TOP, SIGIL0, true, 0.62f, CAP_TOP, 0.55f, null);
        return b.build("pillar", PILLAR_MAX);
    }

    // ================================================================== the grimoire: open, the spine on the Y axis at x = 0, the pages face +Z, about 0.84 wide open and 0.92 tall
    public static final Mesh GRIMOIRE = buildGrimoire();
    /** Half the width of the open book and half its height (model units): the layer centres the closing book with these. */
    public static final float BOOK_HALF_W = 0.42f, BOOK_HALF_H = 0.46f;

    private static Mesh buildGrimoire() {
        Builder b = new Builder();
        float x1 = BOOK_HALF_W, y1 = BOOK_HALF_H, zb = -0.13f, zf = 0.13f;
        // part 0: the back cover and its pages (the right-hand half of the open book), the spine bar behind it
        b.part(-1, 0f, 0f, 0f);
        b.box(0f, x1, -y1, y1, zb, 0f, Builder.PX | Builder.NX | Builder.PY | Builder.NY | Builder.PZ | Builder.NZ,
                new int[]{PAGE_EDGE, SPINE, PAGE_EDGE, PAGE_EDGE, PAGE, COVER_BACK}, new int[]{3, 0, 0, 2, 0, 0}, 0.34f, null);
        b.box(-0.035f, 0.035f, -y1, y1, zb - 0.02f, zb, Builder.NZ | Builder.PY | Builder.NY, new int[]{0, 0, SPINE, SPINE, 0, SPINE}, new int[]{0, 0, 0, 0, 0, 0}, 0.1f, null);
        // part 1: the front cover and its pages, hinged at the spine; closed it lies over the back half, open it is turned by -PI about Y onto the left side
        b.part(0, 0f, 0f, 0f);
        b.box(0f, x1, -y1, y1, 0f, zf, Builder.PX | Builder.NX | Builder.PY | Builder.NY | Builder.PZ | Builder.NZ,
                new int[]{PAGE_EDGE, SPINE, PAGE_EDGE, PAGE_EDGE, COVER_EMBLEM, PAGE_L}, new int[]{1, 0, 2, 0, 0, 0}, 0.34f, null);
        // parts 2 to 5: two turning pages, each of two segments (the second curls)
        float ph = 0.42f, pz = 0.006f, seg = 0.205f;
        for (int pg = 0; pg < 2; pg++) {
            int s1 = b.part(0, 0.004f, 0f, pz * (pg + 1));
            float[] ra = pg == 0 ? r(0f, 0f, 0.5f, 1f) : r(0f, 0f, 0.5f, 1f);
            b.faceO(pg == 0 ? PAGE_GLOW : PAGE_GLOW_L, 0.45f, true, false, ra, 0, Builder.v(0f, -ph, 0f), Builder.v(seg, -ph, 0f), Builder.v(seg, ph, 0f), Builder.v(0f, ph, 0f), 0f, 0f, 1f);
            int s2 = b.part(s1, seg, 0f, 0f);
            b.faceO(pg == 0 ? PAGE_GLOW : PAGE_GLOW_L, 0.55f, true, false, r(0.5f, 0f, 1f, 1f), 0, Builder.v(0f, -ph, 0f), Builder.v(seg - 0.015f, -ph + 0.02f, 0f), Builder.v(seg - 0.015f, ph - 0.02f, 0f), Builder.v(0f, ph, 0f), 0f, 0f, 1f);
        }
        // parts 6 and 7: the ribbon that hangs from the spine and sways (two-sided, the end a point)
        int rb1 = b.part(0, 0.045f, -y1 + 0.02f, -0.045f);
        b.faceO(RIBBON, 0.2f, true, false, null, 0, Builder.v(-0.034f, -0.30f, 0f), Builder.v(0.034f, -0.30f, 0f), Builder.v(0.034f, 0f, 0f), Builder.v(-0.034f, 0f, 0f), 0f, 0f, 1f);
        b.part(rb1, 0f, -0.30f, 0f);
        b.faceO(RIBBON, 0.2f, true, false, r(0f, 0.55f, 1f, 1f), 0, Builder.v(0f, -0.12f, 0f), Builder.v(0.034f, 0f, 0f), Builder.v(0.034f, 0f, 0f), Builder.v(-0.034f, 0f, 0f), 0f, 0f, 1f);
        return b.build("grimoire", GRIMOIRE_MAX);
    }

    /**
     * Writes the joints of the grimoire: {@code open} 0 = closed, 1 = open flat; {@code turnA} / {@code turnB} 0..1 are the two turning pages (0 lying on the right, 1 lying on the left); {@code sway} swings the ribbon.
     */
    public static void grimoirePose(float open, float turnA, float turnB, float sway, float[] bend, float[] spread) {
        float o = Math.max(0f, Math.min(1f, open));
        spread[G_FRONT] = -(float) Math.PI * o;
        float[] t = {turnA, turnB};
        for (int pg = 0; pg < 2; pg++) {
            float k = Math.max(0f, Math.min(1f, t[pg]));
            int p = G_PAGE_A1 + pg * 2;
            float lift = 0.9f * (float) Math.sin(Math.PI * k);
            spread[p] = -(float) Math.PI * k * o + 0.0f;
            spread[p + 1] = -0.55f * lift * o;                                           // the far half of the page lags behind: the page bows while it turns
            bend[p] = 0.05f * lift * o;
        }
        bend[G_RIB1] = 0.10f + sway;
        bend[G_RIB2] = sway * 0.8f;
    }

    // ================================================================== a link of the spell chain: a flat diamond-section loop along Z (the chain runs on Z), about 1.24 long and 0.54 wide
    public static final Mesh LINK = buildLink();
    public static final float LINK_LEN = 1.28f;

    private static Mesh buildLink() {
        Builder b = new Builder();
        b.part(-1, 0f, 0f, 0f);
        float t = 0.11f;
        float[] ext = {0.38f, 0.29f, 0.15f, 0.29f}, len = {0.64f, 0.625f, 0.50f, 0.625f}, ys = {0f, t, 0f, -t};      // the loops outer (y 0), ridge up (+t), inner (y 0), ridge down (-t): half width, half length, height
        int[] cells = {LINK_TOP, LINK_SIDE, LINK_SIDE, LINK_TOP};
        float[] gls = {0.30f, 0.12f, 0.12f, 0.30f};
        for (int s = 0; s < 4; s++) {
            boolean xs = s < 2;                                                         // bars along Z at +-X, or bars along X at +-Z
            float sg = (s & 1) == 0 ? 1f : -1f;
            float rx = xs ? sg * 0.27f : 0f, rz = xs ? 0f : sg * 0.58f;                 // the middle of the bar (inside the solid)
            for (int k = 0; k < 4; k++) {
                int n = (k + 1) & 3;
                float ok = xs ? ext[k] : len[k], ak = xs ? len[k] : ext[k], on = xs ? ext[n] : len[n], an = xs ? len[n] : ext[n];
                b.faceA(cells[k], gls[k], null, bar(xs, sg, ok, ys[k], -ak), bar(xs, sg, ok, ys[k], ak), bar(xs, sg, on, ys[n], an), bar(xs, sg, on, ys[n], -an), rx, 0f, rz);
            }
        }
        return b.build("link", LINK_MAX);
    }

    /** A point of a bar loop: for the bars along Z at +-X ({@code xs}) (out = x, along = z), for the bars along X at +-Z (out = z, along = x). */
    private static float[] bar(boolean xs, float sg, float out, float y, float along) {
        return xs ? new float[]{sg * out, y, along} : new float[]{along, y, sg * out};
    }

    // ================================================================== the sigil head at the far end of the chain: a faceted crystal pointing along +Z through a seal disc
    public static final Mesh HEAD = buildHead();

    private static Mesh buildHead() {
        Builder b = new Builder();
        b.part(-1, 0f, 0f, 0f);
        float gr = 0.27f, zf = 0.95f, zb = -0.5f, zg = 0.0f;
        float[][] g = new float[6][];
        for (int i = 0; i < 6; i++) {
            double a = Math.PI / 6 + Math.PI * i / 3;
            g[i] = Builder.v((float) Math.cos(a) * gr, (float) Math.sin(a) * gr, zg);
        }
        float[] fa = Builder.v(0f, 0f, zf), ba = Builder.v(0f, 0f, zb);
        for (int i = 0; i < 6; i++) {
            int j = (i + 1) % 6;
            int cf = i % 3 == 0 ? GEM_C : (i & 1) == 0 ? GEM_A : GEM_B, cb = (i & 1) == 0 ? GEM_B : GEM_A;
            b.faceA(cf, cf == GEM_C ? 0.8f : 0.4f, null, g[i], g[j], fa, fa, 0f, 0f, 0.1f);
            b.faceA(cb, 0.3f, null, g[j], g[i], ba, ba, 0f, 0f, 0.1f);
        }
        // the seal: a diamond-shaped disc across the crystal behind its girdle (two-sided, the seal is cut out of it by alpha)
        float s = 0.78f, zs = -0.2f;
        b.faceO(HEAD_PLATE, 0.6f, true, false, null, 0, Builder.v(-s, -s, zs), Builder.v(s, -s, zs), Builder.v(s, s, zs), Builder.v(-s, s, zs), 0f, 0f, 1f);
        return b.build("head", HEAD_MAX);
    }

    // ================================================================== the signet ring: the band lies in the XY plane (its axis is Z), the gem on top (+Y), the halo behind
    public static final Mesh RING = buildRing();
    public static final float RING_R = 0.5f;

    private static Mesh buildRing() {
        Builder b = new Builder();
        b.part(-1, 0f, 0f, 0f);
        int n = 8;
        float R = RING_R, ro = R + 0.085f, ri = R - 0.085f, hz = 0.17f;
        for (int i = 0; i < n; i++) {
            double a0 = Math.PI / 2 - Math.PI / n + Math.PI * 2 * i / n, a1 = a0 + Math.PI * 2 / n;
            float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
            double am = (a0 + a1) / 2;
            float rx = (float) Math.cos(am) * R, ry = (float) Math.sin(am) * R;
            float[] oz0 = Builder.v(c0 * ro, s0 * ro, hz), oz1 = Builder.v(c1 * ro, s1 * ro, hz), oy0 = Builder.v(c0 * ro, s0 * ro, -hz), oy1 = Builder.v(c1 * ro, s1 * ro, -hz);
            float[] iz0 = Builder.v(c0 * ri, s0 * ri, hz), iz1 = Builder.v(c1 * ri, s1 * ri, hz), iy0 = Builder.v(c0 * ri, s0 * ri, -hz), iy1 = Builder.v(c1 * ri, s1 * ri, -hz);
            boolean engraved = i != 0;
            b.faceA(engraved ? RING_BAND : RING_BAND_B, 0.28f, null, oy0, oy1, oz1, oz0, rx, ry, 0f);                 // the outside of the band (runes run along it)
            b.faceA(RING_BAND_B, 0.12f, null, iz0, iz1, iy1, iy0, rx, ry, 0f);                                          // the inside
            b.faceA(RING_BAND, 0.22f, null, oz0, oz1, iz1, iz0, rx, ry, 0f);                                            // the front side (+Z), a glowing rune line
            b.faceA(RING_BAND_B, 0.12f, null, oy1, oy0, iy0, iy1, rx, ry, 0f);                                          // the back side (-Z)
        }
        // the bezel on top: a square frustum with a rim, then the gem (eight facets and a table)
        float yb = ro - 0.06f, yb1 = ro + 0.14f, bx0 = 0.38f, bz0 = 0.28f, bx1 = 0.32f, bz1 = 0.22f;
        float[][] lo = {Builder.v(-bx0, yb, -bz0), Builder.v(bx0, yb, -bz0), Builder.v(bx0, yb, bz0), Builder.v(-bx0, yb, bz0)};
        float[][] hi = {Builder.v(-bx1, yb1, -bz1), Builder.v(bx1, yb1, -bz1), Builder.v(bx1, yb1, bz1), Builder.v(-bx1, yb1, bz1)};
        for (int i = 0; i < 4; i++) b.faceA(BEZEL, 0.25f, null, lo[i], lo[(i + 1) & 3], hi[(i + 1) & 3], hi[i], 0f, yb, 0f);
        b.faceA(BEZEL, 0.35f, null, hi[0], hi[1], hi[2], hi[3], 0f, yb, 0f);
        float gy0 = yb1 + 0.005f, gy1 = gy0 + 0.38f, gwx = 0.30f, gwz = 0.21f, twx = 0.19f, twz = 0.11f;
        float[][] gb = {Builder.v(-gwx, gy0, -gwz), Builder.v(gwx, gy0, -gwz), Builder.v(gwx, gy0, gwz), Builder.v(-gwx, gy0, gwz)};
        float[][] gt = {Builder.v(0f, gy1, -twz), Builder.v(twx, gy1, 0f), Builder.v(0f, gy1, twz), Builder.v(-twx, gy1, 0f)};
        for (int i = 0; i < 4; i++) {
            b.faceA(i % 2 == 0 ? GEM_A : GEM_B, 0.55f, null, gb[i], gb[(i + 1) & 3], gt[i], gt[i], 0f, gy0 - 0.1f, 0f);                 // the facet on a side of the gem reaches the table corner above its middle
            b.faceA(GEM_C, 0.75f, null, gb[i], gt[(i + 3) & 3], gt[i], gt[i], 0f, gy0 - 0.1f, 0f);                                      // the facet on a corner of the gem
        }
        b.faceA(GEM_C, 0.95f, null, gt[0], gt[1], gt[2], gt[3], 0f, gy0 - 0.1f, 0f);
        // the rune halo: a flat disc of runes behind the band (one two-sided quad, the runes cut out by alpha)
        float h = 1.0f;
        b.faceO(HALO, 0.65f, true, false, null, 0, Builder.v(-h, -h, -hz - 0.03f), Builder.v(h, -h, -hz - 0.03f), Builder.v(h, h, -hz - 0.03f), Builder.v(-h, h, -hz - 0.03f), 0f, 0f, 1f);
        return b.build("ring", RING_MAX);
    }

    // ================================================================== a rune crystal: an elongated square bipyramid standing on Y (about 1.1 tall)
    public static final Mesh CRYSTAL = buildCrystal();

    private static Mesh buildCrystal() {
        Builder b = new Builder();
        b.part(-1, 0f, 0f, 0f);
        float w = 0.2f, yt = 0.62f, yb = -0.5f;
        float[][] g = {Builder.v(w, 0f, 0f), Builder.v(0f, 0f, w), Builder.v(-w, 0f, 0f), Builder.v(0f, 0f, -w)};
        float[] top = Builder.v(0f, yt, 0f), bot = Builder.v(0f, yb, 0f);
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) & 3;
            b.faceA(i % 2 == 0 ? CRYSTAL_A : CRYSTAL_B, 0.5f, null, g[i], g[j], top, top, 0f, 0f, 0f);
            b.faceA(i % 2 == 0 ? CRYSTAL_B : CRYSTAL_A, 0.35f, null, g[j], g[i], bot, bot, 0f, 0f, 0f);
        }
        return b.build("crystal", CRYSTAL_MAX);
    }

    // ================================================================== shards of a broken magic circle: thin pieces of glass in the XY plane (a front, a back and the chipped rims)
    public static final Mesh SHARD_TRI = buildShard("shard_tri", new float[]{0.02f, 0.58f, -0.44f, -0.34f, 0.50f, -0.30f}, r(0.05f, 0.1f, 0.6f, 0.95f));
    public static final Mesh SHARD_QUAD = buildShard("shard_quad", new float[]{-0.40f, 0.34f, 0.26f, 0.50f, 0.46f, -0.20f, -0.14f, -0.46f}, r(0.3f, 0.05f, 0.95f, 0.8f));
    public static final Mesh SHARD_LONG = buildShard("shard_long", new float[]{0.0f, 0.86f, -0.13f, -0.50f, 0.17f, -0.46f}, r(0.15f, 0.0f, 0.5f, 1f));

    /** A glass shard from its outline (x, y pairs: 3 or 4 corners), 0.045 thick. */
    private static Mesh buildShard(String name, float[] xy, float[] rect) {
        Builder b = new Builder();
        b.part(-1, 0f, 0f, 0f);
        int n = xy.length / 2;
        float t = 0.022f;
        float[][] f = new float[4][], k = new float[4][];
        for (int i = 0; i < 4; i++) {
            int j = Math.min(i, n - 1);
            f[i] = Builder.v(xy[j * 2], xy[j * 2 + 1], t);
            k[i] = Builder.v(xy[j * 2], xy[j * 2 + 1], -t);
        }
        // front (+Z) counter-clockwise, back (-Z) the other way round; the corner list of a triangle repeats its last corner
        b.faceA(SHARD, 0.35f, rect, f[0], f[1], f[2], f[3], 0f, 0f, 0f);
        b.faceA(SHARD_B, 0.25f, rect, k[1], k[0], k[3], k[2], 0f, 0f, 0f);
        for (int i = 0; i < n; i++) {
            int j = (i + 1) % n;
            b.faceA(SHARD_EDGE, 0.9f, null, f[i], f[j], k[j], k[i], 0f, 0f, 0f);
        }
        return b.build(name, SHARD_MAX);
    }

    // ================================================================== posing, culling and sorting: pure, allocation-free after construction
    /** One model being posed and prepared for drawing: arrays allocated once, reused for every frame and every model (render thread only). */
    public static final class Posed {
        public static final int MAX_FACES = 64, MAX_PARTS = 12;
        private static final float INSET = 2f / 1024f, CELL = 1f / COLS;
        private static final float SUN_X = 0.3f, SUN_Y = 0.85f, SUN_Z = 0.42f;

        private final float[] bend = new float[MAX_PARTS], spread = new float[MAX_PARTS];
        private final float[] m = new float[MAX_PARTS * 12];
        private final float[] rb = new float[9];
        // kept faces, camera-relative
        public final float[] pts = new float[MAX_FACES * 12];
        public final float[] uv = new float[MAX_FACES * 4];
        public final int[] colour = new int[MAX_FACES];
        private final float[] key = new float[MAX_FACES];
        private final int[] order = new int[MAX_FACES];
        private final float[] tmp = new float[12];
        private int count;
        private float ex = 1, ey = 0, ez = 0, fx = 0, fy = 1, fz = 0, gx = 0, gy = 0, gz = 1, sx = 1, sy = 1, sz = 1;
        /** Faces of the parts whose bit is set are not drawn; the cell shift moves the sigil of the faces marked as shifting; the accent is the colour a hot face lifts toward. Reset by {@link SolomonMeshDraw#draw}. */
        public int hide, cellShift;
        private int accR = 255, accG = 220, accB = 150;

        /** The world vector of the local vector (x, y, z) under the current axes and scale (to put a model's own centre on a point). */
        public float wx(float x, float y, float z) { return ex * sx * x + fx * sy * y + gx * sz * z; }
        public float wy(float x, float y, float z) { return ey * sx * x + fy * sy * y + gy * sz * z; }
        public float wz(float x, float y, float z) { return ez * sx * x + fz * sy * y + gz * sz * z; }

        public float[] bend() { return bend; }
        public float[] spread() { return spread; }
        public int count() { return count; }

        public void clearPose() { Arrays.fill(bend, 0f); Arrays.fill(spread, 0f); }

        /** The colour (0xRRGGBB) a hot face lifts toward (the element of a pillar); the default is warm gold-white. */
        public Posed accent(int rgb) { accR = (rgb >> 16) & 255; accG = (rgb >> 8) & 255; accB = rgb & 255; return this; }

        public Posed reset() { hide = 0; cellShift = 0; accR = 255; accG = 220; accB = 150; return this; }

        /** The model axes in the world: local X, Y, Z mapped to these vectors (unit length), then scaled per axis. */
        public Posed axes(float ex, float ey, float ez, float fx, float fy, float fz, float gx, float gy, float gz, float sx, float sy, float sz) {
            this.ex = ex; this.ey = ey; this.ez = ez; this.fx = fx; this.fy = fy; this.fz = fz; this.gx = gx; this.gy = gy; this.gz = gz;
            this.sx = sx; this.sy = sy; this.sz = sz;
            return this;
        }

        /** Y up, turned {@code yaw} about Y, scaled per axis (a pillar growing out of the floor). */
        public Posed stand(float yaw, float sx, float sy, float sz) {
            float c = (float) Math.cos(yaw), s = (float) Math.sin(yaw);
            return axes(c, 0f, -s, 0f, 1f, 0f, s, 0f, c, sx, sy, sz);
        }

        /** Z along (dx, dy, dz), Y as close to the hint (hx, hy, hz) as the direction allows, uniform scale, a roll (radians) about Z. */
        public Posed look(float dx, float dy, float dz, float hx, float hy, float hz, float roll, float scale) {
            float dl = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dl < 1e-5f) { dx = 0f; dy = 0f; dz = 1f; dl = 1f; }
            dx /= dl; dy /= dl; dz /= dl;
            float k = hx * dx + hy * dy + hz * dz;
            float ux = hx - dx * k, uy = hy - dy * k, uz = hz - dz * k;
            float ul = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);
            if (ul < 1e-4f) { ux = Math.abs(dy) < 0.9f ? 0f : 1f; uy = Math.abs(dy) < 0.9f ? 1f : 0f; uz = 0f; k = ux * dx + uy * dy + uz * dz; ux -= dx * k; uy -= dy * k; uz -= dz * k; ul = (float) Math.sqrt(ux * ux + uy * uy + uz * uz); }
            ux /= ul; uy /= ul; uz /= ul;
            float rx = uy * dz - uz * dy, ry = uz * dx - ux * dz, rz = ux * dy - uy * dx;     // X = Y x Z
            float c = (float) Math.cos(roll), s = (float) Math.sin(roll);
            return axes(rx * c + ux * s, ry * c + uy * s, rz * c + uz * s, ux * c - rx * s, uy * c - ry * s, uz * c - rz * s, dx, dy, dz, scale, scale, scale);
        }

        /** Turns the model about the world axis (ax, ay, az) by {@code angle} (a tumbling shard or crystal), scaled per axis. */
        public Posed tumble(float ax, float ay, float az, float angle, float sx, float sy, float sz) {
            float al = (float) Math.sqrt(ax * ax + ay * ay + az * az);
            if (al < 1e-5f) { ax = 0f; ay = 1f; az = 0f; al = 1f; }
            ax /= al; ay /= al; az /= al;
            float c = (float) Math.cos(angle), s = (float) Math.sin(angle), t = 1f - c;
            return axes(t * ax * ax + c, t * ax * ay + s * az, t * ax * az - s * ay,
                    t * ax * ay - s * az, t * ay * ay + c, t * ay * az + s * ax,
                    t * ax * az + s * ay, t * ay * az - s * ax, t * az * az + c, sx, sy, sz);
        }

        /**
         * Poses {@code mesh} at the camera-relative point (ox, oy, oz), keeps the faces that look at the camera (at the origin), shades them from the sun, and sorts them far to near. Returns the number of faces
         * kept; they are in {@link #pts} / {@link #uv} / {@link #colour} in drawing order (index 0 first).
         *
         * @param tint  the colour that multiplies the shaded texture (alpha = the model's opacity)
         * @param heat  0..1 lifts the shade toward the accent colour on the emissive faces
         */
        public int prepare(Mesh mesh, float ox, float oy, float oz, int tint, float heat) {
            count = 0;
            int np = Math.min(mesh.parts, MAX_PARTS);
            // part matrices: row-major 3x3 then the translation
            rb[0] = ex * sx; rb[1] = fx * sy; rb[2] = gx * sz;
            rb[3] = ey * sx; rb[4] = fy * sy; rb[5] = gy * sz;
            rb[6] = ez * sx; rb[7] = fz * sy; rb[8] = gz * sz;
            for (int p = 0; p < np; p++) {
                int o = p * 12, par = mesh.parent[p];
                float px = mesh.pivot[p * 3], py = mesh.pivot[p * 3 + 1], pz = mesh.pivot[p * 3 + 2];
                float cb = (float) Math.cos(bend[p]), sb = (float) Math.sin(bend[p]), ca = (float) Math.cos(spread[p]), sa = (float) Math.sin(spread[p]);
                // local = Ry(spread) * Rx(bend)
                float l00 = ca, l01 = sa * sb, l02 = sa * cb, l10 = 0f, l11 = cb, l12 = -sb, l20 = -sa, l21 = ca * sb, l22 = ca * cb;
                if (par < 0) {
                    mul(rb, 0, l00, l01, l02, l10, l11, l12, l20, l21, l22, o);
                    m[o + 9] = ox + rb[0] * px + rb[1] * py + rb[2] * pz;
                    m[o + 10] = oy + rb[3] * px + rb[4] * py + rb[5] * pz;
                    m[o + 11] = oz + rb[6] * px + rb[7] * py + rb[8] * pz;
                } else {
                    int q = par * 12;
                    mulFrom(q, l00, l01, l02, l10, l11, l12, l20, l21, l22, o);
                    m[o + 9] = m[q + 9] + m[q] * px + m[q + 1] * py + m[q + 2] * pz;
                    m[o + 10] = m[q + 10] + m[q + 3] * px + m[q + 4] * py + m[q + 5] * pz;
                    m[o + 11] = m[q + 11] + m[q + 6] * px + m[q + 7] * py + m[q + 8] * pz;
                }
            }
            int ta = (tint >>> 24), tr = (tint >> 16) & 255, tg = (tint >> 8) & 255, tb = tint & 255;
            for (int f = 0; f < mesh.faces && count < MAX_FACES; f++) {
                int pi = Math.min(mesh.part[f], np - 1);
                if (((hide >>> pi) & 1) != 0) continue;
                int o = pi * 12;
                int fo = f * 12;
                for (int v = 0; v < 4; v++) {
                    float x = mesh.pos[fo + v * 3], y = mesh.pos[fo + v * 3 + 1], z = mesh.pos[fo + v * 3 + 2];
                    tmp[v * 3] = m[o] * x + m[o + 1] * y + m[o + 2] * z + m[o + 9];
                    tmp[v * 3 + 1] = m[o + 3] * x + m[o + 4] * y + m[o + 5] * z + m[o + 10];
                    tmp[v * 3 + 2] = m[o + 6] * x + m[o + 7] * y + m[o + 8] * z + m[o + 11];
                }
                // normal = (p2 - p0) x (p3 - p1), outward by the winding of the mesh
                float d1x = tmp[6] - tmp[0], d1y = tmp[7] - tmp[1], d1z = tmp[8] - tmp[2], d2x = tmp[9] - tmp[3], d2y = tmp[10] - tmp[4], d2z = tmp[11] - tmp[5];
                float nx = d1y * d2z - d1z * d2y, ny = d1z * d2x - d1x * d2z, nz = d1x * d2y - d1y * d2x;
                float nl = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                if (nl < 1e-9f) continue;
                nx /= nl; ny /= nl; nz /= nl;
                float cx = (tmp[0] + tmp[3] + tmp[6] + tmp[9]) * 0.25f, cy = (tmp[1] + tmp[4] + tmp[7] + tmp[10]) * 0.25f, cz = (tmp[2] + tmp[5] + tmp[8] + tmp[11]) * 0.25f;
                float facing = nx * cx + ny * cy + nz * cz;                               // < 0: the normal points to the camera
                boolean flipped = false;
                if (facing >= 0f) {
                    if (!mesh.two[f]) continue;
                    nx = -nx; ny = -ny; nz = -nz;
                    flipped = true;
                }
                float lit = Math.max(0f, nx * SUN_X + ny * SUN_Y + nz * SUN_Z);
                float shade = 0.38f + 0.62f * lit;
                float gl = mesh.glow[f];
                shade += (1f - shade) * gl;
                float hot = heat * (0.25f + 0.75f * gl);
                int i = count++;
                System.arraycopy(tmp, 0, pts, i * 12, 12);
                key[i] = cx * cx + cy * cy + cz * cz;
                float rr = Math.min(255f, tr * shade + accR * hot * 0.5f), gg = Math.min(255f, tg * shade + accG * hot * 0.5f), bb = Math.min(255f, tb * shade + accB * hot * 0.5f);
                colour[i] = (ta << 24) | ((int) rr << 16) | ((int) gg << 8) | (int) bb;
                int c = mesh.cell[f] + (mesh.shift[f] ? cellShift : 0);
                float cu = (c % COLS) * CELL, cv = (c / COLS) * CELL;
                float span = CELL - 2 * INSET;
                float u0 = cu + INSET + mesh.rect[f * 4] * span, v0 = cv + INSET + mesh.rect[f * 4 + 1] * span;
                float u1 = cu + INSET + mesh.rect[f * 4 + 2] * span, v1 = cv + INSET + mesh.rect[f * 4 + 3] * span;
                uv[i * 4] = u0; uv[i * 4 + 1] = v0; uv[i * 4 + 2] = u1; uv[i * 4 + 3] = v1;
                if (flipped) { uv[i * 4] = u1; uv[i * 4 + 2] = u0; }                      // the back of a two-sided face is the mirror image
                order[i] = i;
            }
            // far to near: insertion sort of the index list (few faces)
            for (int i = 1; i < count; i++) {
                int v = order[i];
                float kv = key[v];
                int j = i;
                while (j > 0 && key[order[j - 1]] < kv) { order[j] = order[j - 1]; j--; }
                order[j] = v;
            }
            return count;
        }

        /** The index (into pts / uv / colour) of the {@code n}-th face in drawing order. */
        public int at(int n) { return order[n]; }

        private void mul(float[] a, int ao, float l00, float l01, float l02, float l10, float l11, float l12, float l20, float l21, float l22, int o) {
            m[o] = a[ao] * l00 + a[ao + 1] * l10 + a[ao + 2] * l20; m[o + 1] = a[ao] * l01 + a[ao + 1] * l11 + a[ao + 2] * l21; m[o + 2] = a[ao] * l02 + a[ao + 1] * l12 + a[ao + 2] * l22;
            m[o + 3] = a[ao + 3] * l00 + a[ao + 4] * l10 + a[ao + 5] * l20; m[o + 4] = a[ao + 3] * l01 + a[ao + 4] * l11 + a[ao + 5] * l21; m[o + 5] = a[ao + 3] * l02 + a[ao + 4] * l12 + a[ao + 5] * l22;
            m[o + 6] = a[ao + 6] * l00 + a[ao + 7] * l10 + a[ao + 8] * l20; m[o + 7] = a[ao + 6] * l01 + a[ao + 7] * l11 + a[ao + 8] * l21; m[o + 8] = a[ao + 6] * l02 + a[ao + 7] * l12 + a[ao + 8] * l22;
        }

        private void mulFrom(int q, float l00, float l01, float l02, float l10, float l11, float l12, float l20, float l21, float l22, int o) {
            float a0 = m[q], a1 = m[q + 1], a2 = m[q + 2], a3 = m[q + 3], a4 = m[q + 4], a5 = m[q + 5], a6 = m[q + 6], a7 = m[q + 7], a8 = m[q + 8];
            m[o] = a0 * l00 + a1 * l10 + a2 * l20; m[o + 1] = a0 * l01 + a1 * l11 + a2 * l21; m[o + 2] = a0 * l02 + a1 * l12 + a2 * l22;
            m[o + 3] = a3 * l00 + a4 * l10 + a5 * l20; m[o + 4] = a3 * l01 + a4 * l11 + a5 * l21; m[o + 5] = a3 * l02 + a4 * l12 + a5 * l22;
            m[o + 6] = a6 * l00 + a7 * l10 + a8 * l20; m[o + 7] = a6 * l01 + a7 * l11 + a8 * l21; m[o + 8] = a6 * l02 + a7 * l12 + a8 * l22;
        }
    }
}
