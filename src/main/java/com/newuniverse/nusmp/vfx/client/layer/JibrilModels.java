package com.newuniverse.nusmp.vfx.client.layer;

import java.util.Arrays;

/**
 * 3.21.0: the REAL 3D MODELS of Jibril, Lord of Rigor (the Holy Shackles cuff and chain link, the Spear of Heaven, the halo, the law plate, the open tome of law and the command crown), as PURE geometry: no Minecraft class is
 * named here, so the meshes unit-test and preview headless ({@code JibrilModelsTest}, {@code python tools/vfx_preview/preview.py jibril_shackles_models ...}). They are drawn by {@link JibrilMeshDraw} inside {@link JibrilLayer};
 * the material atlas is {@code textures/particle/jibril_model_atlas.png} ({@code tools/gen_jibril_model_textures.py}: polished gold, pearl enamel, azure lacquer, faceted light crystal, feathers, script bands, parchment).
 *
 * <p>A mesh is built ONCE (static, at class load) from tapered blocks, n-sided frusta, swept rings and leaf blades: every face is a quad (a triangle repeats its last corner), stored in the local space of one of its PARTS.
 * Parts form a small skeleton (the lid of a cuff, the two halves and the leaves of the tome): a part sits at a pivot in its parent and is turned by a bend (about its X axis) and a spread (about its Y axis). Nothing here runs
 * per frame except {@link Posed#prepare}, which writes into arrays that belong to the {@link Posed} and are allocated once.
 *
 * <p><b>Budget (rule 11).</b> A closed solid shows only the faces that look at the camera: {@link Posed#prepare} drops the others and the rest is drawn far to near (painter's order, the vertex buffer has no depth write). The
 * constants {@link #CUFF_MAX}, {@link #LINK_MAX}, {@link #SPEAR_MAX}, {@link #HALO_MAX}, {@link #PLATE_MAX}, {@link #TOME_MAX} and {@link #CROWN_MAX} are the most quads a view of each model can keep (checked from 3000
 * random views by {@code JibrilModelsTest}); a model that does not fit the remaining buffer budget is skipped whole, never drawn in part.
 *
 * <p>The switch {@code visual.jibrilModels} (CLIENT config, default ON) is mirrored here like {@code FlatProps}: off = the layer draws exactly what 3.20.0 drew (the flat sprites).
 */
public final class JibrilModels {
    private JibrilModels() {}

    // ------------------------------------------------------------------ the switch (a mirror of the CLIENT config, written by JibrilModelsConfig on the client only)
    private static volatile boolean enabled = true;

    /** True (the default) when the 3D models are drawn; false = the flat sprites of 3.20.0. */
    public static boolean enabled() { return enabled; }

    /** Written by the client config only. */
    public static void setEnabled(boolean on) { enabled = on; }

    // ------------------------------------------------------------------ the atlas: 4 x 4 cells of 128 px (jibril_model_atlas.png)
    public static final int COLS = 4;
    public static final int GOLD = 0, GOLD_DARK = 1, SEAM = 2, PEARL = 3, AZURE = 4, LAVENDER = 5, FEATHER = 6, FEATHER_B = 7, SCRIPT = 8, PAGE = 9, SEAL = 10, TABLET = 11, BLADE = 12, LINK_GOLD = 13, COVER = 14, ENAMEL = 15;

    /** The most quads a view of each model keeps after the faces turned away are dropped (tested from random views). */
    public static final int CUFF_MAX = 18, LINK_MAX = 6, SPEAR_MAX = 36, HALO_MAX = 22, PLATE_MAX = 3, TOME_MAX = 24, CROWN_MAX = 30;

    // ------------------------------------------------------------------ the mesh
    /** Part ids of the cuff (base half, lid) and the tome (spine with the right half, left half, four leaves). */
    public static final int C_BASE = 0, C_LID = 1;
    public static final int T_ROOT = 0, T_RIGHT = 1, T_LEFT = 2, T_LEAF0 = 3, T_LEAVES = 4, T_PARTS = 7;

    /** An immutable mesh: faces of four corners (x, y, z) in the local space of their part, with an atlas cell, a UV rectangle inside the cell, an emissive amount and a two-sided flag. */
    public static final class Mesh {
        public final String name;
        public final int faces, parts, maxDrawn;
        final int[] parent;
        final float[] pivot;
        final float[] pos;
        final int[] cell, part;
        final float[] rect, glow;
        final boolean[] two;

        Mesh(String name, int maxDrawn, int parts, int[] parent, float[] pivot, int faces, float[] pos, int[] cell, int[] part, float[] rect, float[] glow, boolean[] two) {
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
        }

        public int cellOf(int face) { return cell[face]; }
        public float[] corners(int face) { return Arrays.copyOfRange(pos, face * 12, face * 12 + 12); }
        public float[] rectOf(int face) { return Arrays.copyOfRange(rect, face * 4, face * 4 + 4); }
    }

    /** Collects faces while a mesh is built (class-load time only). */
    private static final class Builder {
        float[] pos = new float[12 * 64];
        int[] cell = new int[64], part = new int[64];
        float[] rect = new float[4 * 64], glow = new float[64];
        boolean[] two = new boolean[64];
        int faces, cur;
        final int[] parent = new int[32];
        final float[] pivot = new float[32 * 3];
        int parts;
        private static final float[] FULL = {0f, 0f, 1f, 1f};

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
        }

        /**
         * A face a-b-c-d (a = bottom left of the texture, b = bottom right, c = top right, d = top left, seen from the outside); {@code (rx, ry, rz)} is a point inside the solid, so the winding is made to point outward (a
         * two-sided face keeps the order given). A triangle repeats its last corner.
         */
        void face(int c, float gl, boolean twoSided, float[] r, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz, float rx, float ry, float rz) {
            if (faces >= cell.length) grow();
            float nx = (cy - ay) * (dz - bz) - (cz - az) * (dy - by);
            float ny = (cz - az) * (dx - bx) - (cx - ax) * (dz - bz);
            float nz = (cx - ax) * (dy - by) - (cy - ay) * (dx - bx);
            float mx = (ax + bx + cx + dx) * 0.25f - rx, my = (ay + by + cy + dy) * 0.25f - ry, mz = (az + bz + cz + dz) * 0.25f - rz;
            boolean flip = !twoSided && nx * mx + ny * my + nz * mz < 0f;
            int o = faces * 12;
            if (!flip) {
                put(o, ax, ay, az); put(o + 3, bx, by, bz); put(o + 6, cx, cy, cz); put(o + 9, dx, dy, dz);
            } else {
                put(o, ax, ay, az); put(o + 3, dx, dy, dz); put(o + 6, cx, cy, cz); put(o + 9, bx, by, bz);
            }
            cell[faces] = c;
            part[faces] = cur;
            glow[faces] = gl;
            two[faces] = twoSided;
            float[] rr = r == null ? FULL : r;
            System.arraycopy(rr, 0, rect, faces * 4, 4);
            faces++;
        }

        private void put(int o, float x, float y, float z) { pos[o] = x; pos[o + 1] = y; pos[o + 2] = z; }

        /** Side bits of {@link #block}: 1 bottom, 2 right, 4 top, 8 left, 16 start cap, 32 end cap. */
        static final int BOT = 1, RIGHT = 2, TOP = 4, LEFT = 8, START = 16, END = 32, ALL = 63;

        /**
         * A tapered rectangular block from the centre a to the centre b: half width / half height w0, h0 at a and w1, h1 at b; {@code up} says which way the top face points. {@code cells} = bottom, right, top, left,
         * start, end.
         */
        void block(float ax, float ay, float az, float bx, float by, float bz, float w0, float h0, float w1, float h1, float upx, float upy, float upz, int mask, int[] cells, float gl) {
            float dx = bx - ax, dy = by - ay, dz = bz - az;
            float dl = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            dx /= dl; dy /= dl; dz /= dl;
            float sx = upy * dz - upz * dy, sy = upz * dx - upx * dz, sz = upx * dy - upy * dx;
            float sl = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
            if (sl < 1e-4f) { sx = 1f; sy = 0f; sz = 0f; sl = 1f; }
            sx /= sl; sy /= sl; sz /= sl;
            float ux = dy * sz - dz * sy, uy = dz * sx - dx * sz, uz = dx * sy - dy * sx;
            float[][] sgn = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
            float[] A = new float[12], B = new float[12];
            for (int i = 0; i < 4; i++) {
                A[i * 3] = ax + sx * sgn[i][0] * w0 + ux * sgn[i][1] * h0;
                A[i * 3 + 1] = ay + sy * sgn[i][0] * w0 + uy * sgn[i][1] * h0;
                A[i * 3 + 2] = az + sz * sgn[i][0] * w0 + uz * sgn[i][1] * h0;
                B[i * 3] = bx + sx * sgn[i][0] * w1 + ux * sgn[i][1] * h1;
                B[i * 3 + 1] = by + sy * sgn[i][0] * w1 + uy * sgn[i][1] * h1;
                B[i * 3 + 2] = bz + sz * sgn[i][0] * w1 + uz * sgn[i][1] * h1;
            }
            float rx = (ax + bx) * 0.5f, ry = (ay + by) * 0.5f, rz = (az + bz) * 0.5f;
            for (int k = 0; k < 4; k++) {                                              // k = 0 bottom (c0 c1), 1 right (c1 c2), 2 top (c2 c3), 3 left (c3 c0)
                if ((mask & (1 << k)) == 0) continue;
                int i = k, j = (k + 1) % 4;
                face(cells[k], gl, false, null, A[i * 3], A[i * 3 + 1], A[i * 3 + 2], A[j * 3], A[j * 3 + 1], A[j * 3 + 2], B[j * 3], B[j * 3 + 1], B[j * 3 + 2], B[i * 3], B[i * 3 + 1], B[i * 3 + 2], rx, ry, rz);
            }
            if ((mask & START) != 0) face(cells[4], gl, false, null, A[0], A[1], A[2], A[3], A[4], A[5], A[6], A[7], A[8], A[9], A[10], A[11], rx, ry, rz);
            if ((mask & END) != 0) face(cells[5], gl, false, null, B[0], B[1], B[2], B[3], B[4], B[5], B[6], B[7], B[8], B[9], B[10], B[11], rx, ry, rz);
        }

        /**
         * An n-sided frustum (r1 = 0 makes a pyramid) from the centre a to the centre b, radius r0 at a and r1 at b, turned by {@code rot} about its axis. {@code cells} are used round the sides; the texture runs
         * across a side and "up" toward b. {@code endCap} >= 0 closes the b end with one fan of quads (n even only: n / 2 quads).
         */
        void frustum(int n, float ax, float ay, float az, float bx, float by, float bz, float r0, float r1, float rot, int[] cells, float gl) {
            float dx = bx - ax, dy = by - ay, dz = bz - az;
            float dl = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            dx /= dl; dy /= dl; dz /= dl;
            float hx = Math.abs(dy) < 0.9f ? 0f : 1f, hy = Math.abs(dy) < 0.9f ? 1f : 0f;
            float e1x = hy * dz, e1y = -hx * dz, e1z = hx * dy - hy * dx;                  // d x h
            float el = (float) Math.sqrt(e1x * e1x + e1y * e1y + e1z * e1z);
            e1x /= el; e1y /= el; e1z /= el;
            float e2x = dy * e1z - dz * e1y, e2y = dz * e1x - dx * e1z, e2z = dx * e1y - dy * e1x;   // d x e1
            float rx = (ax + bx) * 0.5f, ry = (ay + by) * 0.5f, rz = (az + bz) * 0.5f;
            for (int i = 0; i < n; i++) {
                double t0 = rot + Math.PI * 2 * i / n, t1 = rot + Math.PI * 2 * (i + 1) / n;
                float c0 = (float) Math.cos(t0), s0 = (float) Math.sin(t0), c1 = (float) Math.cos(t1), s1 = (float) Math.sin(t1);
                float[] a0 = {ax + (e1x * c0 + e2x * s0) * r0, ay + (e1y * c0 + e2y * s0) * r0, az + (e1z * c0 + e2z * s0) * r0};
                float[] a1 = {ax + (e1x * c1 + e2x * s1) * r0, ay + (e1y * c1 + e2y * s1) * r0, az + (e1z * c1 + e2z * s1) * r0};
                float[] b0 = {bx + (e1x * c0 + e2x * s0) * r1, by + (e1y * c0 + e2y * s0) * r1, bz + (e1z * c0 + e2z * s0) * r1};
                float[] b1 = {bx + (e1x * c1 + e2x * s1) * r1, by + (e1y * c1 + e2y * s1) * r1, bz + (e1z * c1 + e2z * s1) * r1};
                face(cells[i % cells.length], gl, false, null, a0[0], a0[1], a0[2], a1[0], a1[1], a1[2], b1[0], b1[1], b1[2], b0[0], b0[1], b0[2], rx, ry, rz);
            }
        }

        /**
         * A ring swept about the Y axis: {@code n} segments of the circle, of which {@code segFrom} .. {@code segTo - 1} are built, a closed profile of (dr, dy) points in counter-clockwise order (dr outward, dy up) of
         * which edge e (profile[e] -> profile[e + 1]) gets {@code cells[e]} and {@code glows[e]} when bit e of {@code mask} is set. The texture runs along the ring (readable from outside) and "up" along the profile edge.
         */
        void sweep(int n, int segFrom, int segTo, float radius, float y0, float rotOff, float[] prof, int[] cells, float[] glows, int mask, float[][] rects) {
            int m = prof.length / 2;
            for (int s = segFrom; s < segTo; s++) {
                double lo = rotOff + Math.PI * 2 * s / n, hi = rotOff + Math.PI * 2 * (s + 1) / n, mid = (lo + hi) * 0.5;
                float cl = (float) Math.cos(lo), sl = (float) Math.sin(lo), ch = (float) Math.cos(hi), sh = (float) Math.sin(hi);
                float rx = radius * (float) Math.cos(mid), ry = y0, rz = radius * (float) Math.sin(mid);
                for (int e = 0; e < m; e++) {
                    if ((mask & (1 << e)) == 0) continue;
                    int f = (e + 1) % m;
                    float r0 = radius + prof[e * 2], y00 = y0 + prof[e * 2 + 1], r1 = radius + prof[f * 2], y11 = y0 + prof[f * 2 + 1];
                    face(cells[e], glows[e], false, rects == null ? null : rects[e],
                            r0 * ch, y00, r0 * sh, r0 * cl, y00, r0 * sl, r1 * cl, y11, r1 * sl, r1 * ch, y11, r1 * sh, rx, ry, rz);
                }
            }
        }

        /** A leaf blade (one two-sided quad): base centre, the way it points, the way its face looks, length, base width and tip width. */
        void leaf(int c, float gl, float bx, float by, float bz, float dx, float dy, float dz, float nx, float ny, float nz, float len, float w0, float w1) {
            float dl = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            dx /= dl; dy /= dl; dz /= dl;
            float sx = ny * dz - nz * dy, sy = nz * dx - nx * dz, sz = nx * dy - ny * dx;
            float sl = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
            sx /= sl; sy /= sl; sz /= sl;
            float tx = bx + dx * len, ty = by + dy * len, tz = bz + dz * len;
            face(c, gl, true, null, bx - sx * w0, by - sy * w0, bz - sz * w0, bx + sx * w0, by + sy * w0, bz + sz * w0, tx + sx * w1, ty + sy * w1, tz + sz * w1, tx - sx * w1, ty - sy * w1, tz - sz * w1, bx, by, bz);
        }

        Mesh build(String name, int maxDrawn) {
            return new Mesh(name, maxDrawn, Math.max(1, parts), Arrays.copyOf(parent, Math.max(1, parts)), Arrays.copyOf(pivot, Math.max(1, parts) * 3), faces, Arrays.copyOf(pos, faces * 12), Arrays.copyOf(cell, faces),
                    Arrays.copyOf(part, faces), Arrays.copyOf(rect, faces * 4), Arrays.copyOf(glow, faces), Arrays.copyOf(two, faces));
        }
    }

    private static float[] r(float u0, float v0, float u1, float v1) { return new float[]{u0, v0, u1, v1}; }

    /** The profile of a hexagonal bar of gold: dr outward, dy up, counter-clockwise from the outer tip (outer-top bevel, top flat, inner-top bevel, inner-bottom bevel, bottom flat, outer-bottom bevel). */
    private static final float[] KITE = {0.12f, 0f, 0f, 0.09f, -0.12f, 0f, 0f, -0.09f};

    // ================================================================== the holy shackle cuff: a hinged manacle round a wrist, radius 1 about the Y axis, hinge at +X, latch with the glowing seal at -X
    private static final float[] CUFF_PROFILE = {0.10f, -0.28f, 0.10f, 0.28f, -0.09f, 0.35f, -0.09f, -0.35f};
    public static final Mesh CUFF = buildCuff();
    /** Where the hinge pin of the lid sits (local model space). */
    public static final float CUFF_HINGE_X = 1f;

    private static Mesh buildCuff() {
        Builder b = new Builder();
        b.part(-1, 0f, 0f, 0f);
        float[] prof = CUFF_PROFILE;
        int[] cells = {ENAMEL, GOLD, GOLD_DARK, GOLD};
        float[] gl = {0.30f, 0.5f, 0.2f, 0.4f};
        // the base half (the back): segments 3, 4, 5 of 6 (180 to 360 degrees)
        b.sweep(6, 3, 6, 1f, 0f, 0f, prof, cells, gl, 15, null);
        int lid = b.part(0, 1f, 0f, 0f);
        // the lid (the front, hinged at the +X pin): segments 0, 1, 2; its sweep is built in the lid's own space (the pivot is at (1, 0, 0), so the ring is shifted by -1 in X)
        buildCuffLid(b, prof, cells, gl);
        return b.build("cuff", CUFF_MAX);
    }

    private static void buildCuffLid(Builder b, float[] prof, int[] cells, float[] gl) {
        int before = b.faces;
        b.sweep(6, 0, 3, 1f, 0f, 0f, prof, cells, gl, 15, null);
        for (int f = before; f < b.faces; f++) for (int v = 0; v < 4; v++) b.pos[f * 12 + v * 3] -= 1f;            // into the space of the pivot
        // the glowing seal plate on the latch side of the lid (outer wall at 150 degrees), standing a little out of the wall
        double th = Math.toRadians(150);
        float cx = (float) Math.cos(th) * 0.972f - 1f, cz = (float) Math.sin(th) * 0.972f, tx = (float) Math.sin(th), tz = (float) -Math.cos(th);
        b.face(SEAL, 1f, false, null, cx - tx * 0.17f, -0.20f, cz - tz * 0.17f, cx + tx * 0.17f, -0.20f, cz + tz * 0.17f, cx + tx * 0.17f, 0.20f, cz + tz * 0.17f, cx - tx * 0.17f, 0.20f, cz - tz * 0.17f, -1f, 0f, 0f);
    }

    /** Lid angle (spread, radians) that opens the cuff fully / shuts it. */
    public static final float CUFF_OPEN = 1.75f;

    // ================================================================== a chain link: a flat frame of gold bars 0.40 long (Z) and 0.22 wide (X)
    public static final Mesh LINK = buildLink();

    private static Mesh buildLink() {
        Builder b = new Builder();
        b.part(-1, 0f, 0f, 0f);
        float hx = 0.095f, hz = 0.175f, t = 0.036f, y = 0.026f;
        // the four bars of the frame: each shows its top (two-sided, so the frame never has a hole), and the two long bars get an outer wall so the link has a thickness
        for (int s = -1; s <= 1; s += 2) {
            b.face(LINK_GOLD, 0.2f, true, null, s * (hx - t), y, -hz - t, s * (hx + t), y, -hz - t, s * (hx + t), y, hz + t, s * (hx - t), y, hz + t, 0f, 0f, 0f);
            b.face(LINK_GOLD, 0.2f, true, null, -hx + t, y, s * (hz - t), hx - t, y, s * (hz - t), hx - t, y, s * (hz + t), -hx + t, y, s * (hz + t), 0f, 0f, 0f);
            b.face(LINK_GOLD, 0.1f, false, null, s * (hx + t), -y, -hz - t, s * (hx + t), -y, hz + t, s * (hx + t), y, hz + t, s * (hx + t), y, -hz - t, 0f, 0f, 0f);
        }
        return b.build("link", LINK_MAX);
    }

    // ================================================================== the Spear of Heaven: tip at the origin, the shaft runs back along -Z, about 9.7 long; the wings spread in X and (smaller) in Y
    public static final Mesh SPEAR = buildSpear();
    public static final float SPEAR_LENGTH = 9.7f;

    private static Mesh buildSpear() {
        Builder b = new Builder();
        b.part(-1, 0f, 0f, 0f);
        // the head: a long six-sided point, the widest 1.9 behind the tip, then a taper to the neck; the faces alternate between two crystals so every facet catches the light differently
        b.frustum(6, 0f, 0f, -1.9f, 0f, 0f, 0f, 0.44f, 0f, 0f, new int[]{BLADE, PEARL}, 0.65f);
        b.frustum(6, 0f, 0f, -3.35f, 0f, 0f, -1.9f, 0.17f, 0.44f, 0f, new int[]{BLADE, PEARL}, 0.5f);
        // four barbs of light on the head: two in the X plane (large), two in the Y plane (small)
        for (int k = 0; k < 4; k++) {
            float ax = k < 2 ? (k == 0 ? 1f : -1f) : 0f, ay = k < 2 ? 0f : (k == 2 ? 1f : -1f);
            float sc = k < 2 ? 1f : 0.62f;
            float x0 = ax * 0.30f, y0 = ay * 0.30f, x1 = ax * 0.86f * sc, y1 = ay * 0.86f * sc, x2 = ax * 0.19f, y2 = ay * 0.19f, x3 = ax * 0.20f, y3 = ay * 0.20f;
            b.face(BLADE, 0.6f, true, null, x0, y0, -1.55f, x1, y1, -3.0f * (0.6f + 0.4f * sc), x2, y2, -3.5f, x3, y3, -2.35f, 0f, 0f, -2.4f);
        }
        // the gold collar and the pearl socket that holds the wings
        b.frustum(6, 0f, 0f, -3.65f, 0f, 0f, -3.35f, 0.31f, 0.20f, 0.5f, new int[]{GOLD}, 0.35f);
        b.frustum(6, 0f, 0f, -4.6f, 0f, 0f, -3.65f, 0.22f, 0.31f, 0.5f, new int[]{PEARL, GOLD_DARK}, 0.3f);
        // the wings: feathers fanned from the socket, three on each side in X (large), two on each side in Y (small)
        float[] ang = {16f, 46f, 76f};
        float[] len = {1.55f, 2.0f, 1.45f};
        for (int side = -1; side <= 1; side += 2) {
            for (int k = 0; k < 3; k++) {
                double a = Math.toRadians(ang[k]);
                b.leaf(k % 2 == 0 ? FEATHER : FEATHER_B, 0.28f, side * 0.20f, 0f, -3.95f, side * (float) Math.cos(a), 0f, -(float) Math.sin(a), 0f, 1f, 0f, len[k], 0.30f, 0.19f);
            }
            for (int k = 0; k < 1; k++) {
                double a = Math.toRadians(ang[k] + 30);
                b.leaf(k % 2 == 0 ? FEATHER_B : FEATHER, 0.28f, 0f, side * 0.20f, -3.95f, 0f, side * (float) Math.cos(a), -(float) Math.sin(a), 1f, 0f, 0f, len[k] * 0.62f, 0.2f, 0.12f);
            }
        }
        // the shaft with one gold band, and a crystal pommel
        b.frustum(6, 0f, 0f, -8.75f, 0f, 0f, -4.6f, 0.13f, 0.13f, 0f, new int[]{ENAMEL}, 0.2f);
        b.frustum(6, 0f, 0f, -6.45f, 0f, 0f, -6.1f, 0.19f, 0.19f, 0.3f, new int[]{GOLD}, 0.4f);
        b.frustum(6, 0f, 0f, -9.7f, 0f, 0f, -8.75f, 0f, 0.2f, 0f, new int[]{BLADE, PEARL}, 0.7f);
        return b.build("spear", SPEAR_MAX);
    }

    // ================================================================== the halo: radius 1 in the XZ plane, a hexagonal bar of gold, ten facets round, the script band on the flats
    public static final Mesh HALO = buildHalo();

    private static Mesh buildHalo() {
        Builder b = new Builder();
        b.part(-1, 0f, 0f, 0f);
        // outer-top slope, inner-top slope, inner-bottom slope, outer-bottom slope: the script band is cut into the two outer slopes
        int[] cells = {SCRIPT, GOLD_DARK, GOLD_DARK, SCRIPT};
        float[] gl = {0.6f, 0.3f, 0.3f, 0.6f};
        float[][] rects = {r(0f, 0.2f, 1f, 0.8f), null, null, r(0f, 0.2f, 1f, 0.8f)};
        b.sweep(10, 0, 10, 1f, 0f, 0f, KITE, cells, gl, 15, rects);
        return b.build("halo", HALO_MAX);
    }

    // ================================================================== a law plate: a small tablet, 0.29 wide, 0.42 tall, with a gold edge, the law on its front (+Z) and an azure back
    public static final Mesh PLATE = buildPlate();

    private static Mesh buildPlate() {
        Builder b = new Builder();
        b.part(-1, 0f, 0f, 0f);
        b.block(0f, 0f, -0.022f, 0f, 0f, 0.022f, 0.145f, 0.21f, 0.122f, 0.188f, 0f, 1f, 0f, Builder.BOT | Builder.TOP | Builder.START | Builder.END, new int[]{GOLD, GOLD, GOLD, GOLD, AZURE, TABLET}, 0.35f);
        return b.build("plate", PLATE_MAX);
    }

    // ================================================================== the open tome of law: spine along Y, the pages face +Z, the right half is +X; open about 0.35 rad each way, a few leaves fanned between the halves
    public static final Mesh TOME = buildTome();

    private static Mesh buildTome() {
        Builder b = new Builder();
        b.part(-1, 0f, 0f, 0f);
        // the spine
        b.block(0f, -0.45f, 0f, 0f, 0.45f, 0f, 0.05f, 0.07f, 0.05f, 0.07f, 0f, 0f, 1f, Builder.BOT | Builder.RIGHT | Builder.TOP | Builder.LEFT, new int[]{COVER, COVER, GOLD_DARK, COVER, 0, 0}, 0.15f);
        b.part(0, 0f, 0f, 0f);
        tomeHalf(b, 1f);
        b.part(0, 0f, 0f, 0f);
        tomeHalf(b, -1f);
        // four leaves hinged on the spine; each is one two-sided sheet of script
        for (int i = 0; i < 4; i++) {
            b.part(0, 0f, 0f, -0.012f);
            float w = 0.74f - 0.02f * i;
            b.face(PAGE, 0.5f, true, null, 0.06f, -0.405f, 0f, w, -0.395f, 0f, w, 0.395f, 0f, 0.06f, 0.405f, 0f, 0f, 0f, 0f);
        }
        return b.build("tome", TOME_MAX);
    }

    /** One half of the tome: the cover, the stack of pages with its script face and the gold corners (side +1 = right, -1 = left). */
    private static void tomeHalf(Builder b, float side) {
        // the cover board behind the pages (the outer face is the back of the book, the inner face is a margin round the pages)
        b.block(side * 0.05f, 0f, -0.075f, side * 0.80f, 0f, -0.075f, 0.46f, 0.018f, 0.46f, 0.018f, 0f, 0f, 1f, Builder.ALL & ~Builder.START, new int[]{COVER, GOLD, AZURE, GOLD, 0, GOLD}, 0.12f);
        // the page stack: its top, bottom and fore edges, then the face with the script
        b.block(side * 0.06f, 0f, -0.0385f, side * 0.76f, 0f, -0.0385f, 0.43f, 0.0185f, 0.43f, 0.0185f, 0f, 0f, 1f, Builder.RIGHT | Builder.LEFT | Builder.END, new int[]{0, PEARL, 0, PEARL, 0, PEARL}, 0.2f);
        float x0 = side * 0.06f, x1 = side * 0.76f;
        if (side > 0) b.face(PAGE, 0.5f, false, null, x0, -0.43f, -0.019f, x1, -0.43f, -0.019f, x1, 0.43f, -0.019f, x0, 0.43f, -0.019f, side * 0.4f, 0f, -0.05f);
        else b.face(PAGE, 0.5f, false, null, x1, -0.43f, -0.019f, x0, -0.43f, -0.019f, x0, 0.43f, -0.019f, x1, 0.43f, -0.019f, side * 0.4f, 0f, -0.05f);
        // gold corners on the outer corners of the cover
        for (int s = -1; s <= 1; s += 2) {
            float cx = side * 0.80f, cy = s * 0.44f;
            b.face(GOLD, 0.5f, true, null, cx - side * 0.11f, cy, -0.05f, cx, cy - s * 0.11f, -0.05f, cx, cy, -0.05f, cx - side * 0.11f, cy, -0.05f, 0f, 0f, 0f);
        }
    }

    /**
     * Writes the pose of the tome: {@code open} 0 = shut (the left half lies over the right), 1 = open; {@code flip} 0..1 turns the leaves over the spine one after the other (their angles fan between the halves).
     * {@code bend} and {@code spread} are the arrays of a {@link Posed}.
     */
    public static void tomePose(float open, float flip, float[] bend, float[] spread) {
        float o = Math.max(0f, Math.min(1f, open));
        float oe = o * o * (3f - 2f * o);
        float spreadL = lerp((float) Math.PI - 0.08f, 0.36f, oe);
        float spreadR = lerp(0f, -0.36f, oe);
        spread[T_RIGHT] = spreadR;
        spread[T_LEFT] = spreadL;
        for (int i = 0; i < 4; i++) {
            float f = Math.max(0f, Math.min(1f, flip * 5f - i));                          // each leaf turns in its own fifth of the flip
            float fe = f * f * (3f - 2f * f);
            float rest = spreadR - 0.06f * i * oe;                                         // lying on the right half, a little fanned
            float far = -((float) Math.PI - spreadL) + 0.06f * (3 - i) * oe;                // lying on the left half
            spread[T_LEAF0 + i] = lerp(rest, far, fe);
            bend[T_LEAF0 + i] = 0.08f * (float) Math.sin(f * Math.PI) * oe;
        }
    }

    // ================================================================== the command crown: radius 1 in the XZ plane, a band with a script wall, six spikes of light crystal alternating tall and short
    public static final Mesh CROWN = buildCrown();

    private static Mesh buildCrown() {
        Builder b = new Builder();
        b.part(-1, 0f, 0f, 0f);
        float[] prof = {0.07f, -0.11f, 0.07f, 0.11f, -0.07f, 0.11f, -0.07f, -0.11f};
        b.sweep(6, 0, 6, 1f, 0f, 0f, prof, new int[]{SCRIPT, GOLD, GOLD_DARK, GOLD}, new float[]{0.55f, 0.6f, 0.3f, 0.3f}, 5, new float[][]{r(0f, 0.2f, 1f, 0.8f), null, null, null});
        for (int k = 0; k < 6; k++) {
            double a = Math.PI * 2 * (k + 0.5) / 6;
            float h = k % 2 == 0 ? 0.62f : 0.38f;
            float ca = (float) Math.cos(a) * 0.87f, sa = (float) Math.sin(a) * 0.87f;
            b.frustum(3, ca, 0.1f, sa, ca, 0.1f + h, sa, 0.11f, 0f, (float) a, new int[]{BLADE, PEARL}, 0.65f);
        }
        return b.build("crown", CROWN_MAX);
    }

    // ================================================================== posing, culling and sorting: pure, allocation-free after construction
    /** One model being posed and prepared for drawing: arrays allocated once, reused for every frame and every model (render thread only). */
    public static final class Posed {
        public static final int MAX_FACES = 100, MAX_PARTS = 24;
        private static final float INSET = 2f / 512f;
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

        /** The world vector of the local vector (x, y, z) under the current axes and scale (to put a model's own centre on a point). */
        public float wx(float x, float y, float z) { return ex * sx * x + fx * sy * y + gx * sz * z; }
        public float wy(float x, float y, float z) { return ey * sx * x + fy * sy * y + gy * sz * z; }
        public float wz(float x, float y, float z) { return ez * sx * x + fz * sy * y + gz * sz * z; }

        public float[] bend() { return bend; }
        public float[] spread() { return spread; }
        public int count() { return count; }

        public void clearPose() { Arrays.fill(bend, 0f); Arrays.fill(spread, 0f); }

        /** The model axes in the world: local X, Y, Z mapped to these vectors (unit length), then scaled per axis. */
        public Posed axes(float ex, float ey, float ez, float fx, float fy, float fz, float gx, float gy, float gz, float sx, float sy, float sz) {
            this.ex = ex; this.ey = ey; this.ez = ez; this.fx = fx; this.fy = fy; this.fz = fz; this.gx = gx; this.gy = gy; this.gz = gz;
            this.sx = sx; this.sy = sy; this.sz = sz;
            return this;
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

        /** Turns the model about the world axis (ax, ay, az) by {@code angle} (a tumbling spear), scaled per axis. */
        public Posed tumble(float ax, float ay, float az, float angle, float sx, float sy, float sz) {
            float al = (float) Math.sqrt(ax * ax + ay * ay + az * az);
            if (al < 1e-5f) { ax = 0f; ay = 1f; az = 0f; al = 1f; }
            ax /= al; ay /= al; az /= al;
            float c = (float) Math.cos(angle), s = (float) Math.sin(angle), t = 1f - c;
            return axes(t * ax * ax + c, t * ax * ay + s * az, t * ax * az - s * ay,
                    t * ax * ay - s * az, t * ay * ay + c, t * ay * az + s * ax,
                    t * ax * az + s * ay, t * ay * az - s * ax, t * az * az + c, sx, sy, sz);
        }

        /** Y up, turned {@code yaw} about Y, then leaned toward (tiltX, 1, tiltZ) by {@code pitch}, then {@code scale}d (a ring over a head). */
        public Posed upright(float yaw, float pitch, float tiltX, float tiltZ, float scale) {
            float cy = (float) Math.cos(yaw), sy2 = (float) Math.sin(yaw);
            // local axes after the yaw about Y
            float x0 = cy, y0 = 0f, z0 = -sy2, x1 = 0f, y1 = 1f, z1 = 0f, x2 = sy2, y2 = 0f, z2 = cy;
            // then a tilt that leans the ring's up axis towards (tiltX, 1, tiltZ): rotate about the horizontal axis perpendicular to the lean
            float ux = tiltX * pitch, uz = tiltZ * pitch, ul = (float) Math.sqrt(ux * ux + 1f + uz * uz);
            ux /= ul; uz /= ul;
            float uy = 1f / ul;
            float ax = uz, az = -ux;                                                       // axis = Y x lean
            float al = (float) Math.sqrt(ax * ax + az * az);
            if (al < 1e-5f) return axes(x0, y0, z0, x1, y1, z1, x2, y2, z2, scale, scale, scale);
            float ang = (float) Math.acos(Math.max(-1f, Math.min(1f, uy)));
            ax /= al; az /= al;
            float c = (float) Math.cos(ang), s = (float) Math.sin(ang), t = 1f - c;
            float r00 = t * ax * ax + c, r01 = -s * az, r02 = t * ax * az, r10 = s * az, r11 = c, r12 = -s * ax, r20 = t * ax * az, r21 = s * ax, r22 = t * az * az + c;
            return axes(r00 * x0 + r01 * y0 + r02 * z0, r10 * x0 + r11 * y0 + r12 * z0, r20 * x0 + r21 * y0 + r22 * z0,
                    r00 * x1 + r01 * y1 + r02 * z1, r10 * x1 + r11 * y1 + r12 * z1, r20 * x1 + r21 * y1 + r22 * z1,
                    r00 * x2 + r01 * y2 + r02 * z2, r10 * x2 + r11 * y2 + r12 * z2, r20 * x2 + r21 * y2 + r22 * z2, scale, scale, scale);
        }

        /**
         * Poses {@code mesh} at the camera-relative point (ox, oy, oz), keeps the faces that look at the camera (at the origin), shades them from the sun, and sorts them far to near. Returns the number of faces
         * kept; they are in {@link #pts} / {@link #uv} / {@link #colour} in drawing order (index 0 first).
         *
         * @param tint  the colour that multiplies the shaded texture (alpha = the model's opacity)
         * @param heat  0..1 lifts the shade toward a white-gold glow on the emissive faces
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
                int o = Math.min(mesh.part[f], np - 1) * 12;
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
                float shade = 0.56f + 0.44f * lit;                                          // a light, bright material: Jibril's models glow rather than shade
                float gl = mesh.glow[f];
                shade += (1f - shade) * gl;
                float hot = heat * (0.25f + 0.75f * gl);
                int i = count++;
                System.arraycopy(tmp, 0, pts, i * 12, 12);
                key[i] = cx * cx + cy * cy + cz * cz;
                float rr = Math.min(255f, tr * shade + 255f * hot * 0.5f), gg = Math.min(255f, tg * shade + 240f * hot * 0.5f), bb = Math.min(255f, tb * shade + 200f * hot * 0.5f);
                colour[i] = (ta << 24) | ((int) rr << 16) | ((int) gg << 8) | (int) bb;
                int c = mesh.cell[f];
                float cu = (c % COLS) * 0.25f, cv = (c / COLS) * 0.25f;
                float u0 = cu + INSET + mesh.rect[f * 4] * (0.25f - 2 * INSET), v0 = cv + INSET + mesh.rect[f * 4 + 1] * (0.25f - 2 * INSET);
                float u1 = cu + INSET + mesh.rect[f * 4 + 2] * (0.25f - 2 * INSET), v1 = cv + INSET + mesh.rect[f * 4 + 3] * (0.25f - 2 * INSET);
                uv[i * 4] = u0; uv[i * 4 + 1] = v0; uv[i * 4 + 2] = u1; uv[i * 4 + 3] = v1;
                if (flipped) { uv[i * 4] = u1; uv[i * 4 + 2] = u0; }                      // the back of a two-sided leaf is the mirror image
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

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }
}
