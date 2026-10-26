package com.newuniverse.nusmp.vfx.client.layer;

import java.util.Arrays;

/**
 * 3.21.0: the REAL 3D MODELS of Sandalphon, Lord of Judgment (the guillotine blade of the Cleave, the scales of judgment of the Field, the law tablet and its shards of the Edict, the lance head of the Bullet and the sword of
 * Final Judgment), as PURE geometry: no Minecraft class is named here, so the meshes unit-test and preview headless ({@code JudgmentModelsTest}, {@code python tools/vfx_preview/preview.py judgment_cleave_close ...}).
 * They are drawn by {@link JudgmentMeshDraw} inside {@link JudgmentLayer}; the material atlas is {@code textures/particle/judgment_model_atlas.png} ({@code tools/gen_judgment_model_textures.py}: pale gold plate, pale steel, white
 * marble with hairline black cracks, glowing glyph panels, feathers, chain links).
 *
 * <p>A mesh is built ONCE (static, at class load) from tapered blocks, n-sided frusta and pyramids, strips and leaf blades: every face is a quad (a triangle repeats its last corner), stored in the local space of one of its
 * PARTS. Parts form a small skeleton (the beam and the two pans of the scales): a part sits at a pivot in its parent and is turned by a bend (about its X axis), a spread (about its Y axis) and a roll (about its Z axis).
 * Nothing here runs per frame except {@link Posed#prepare}, which writes into arrays that belong to the {@link Posed} and are allocated once.
 *
 * <p><b>Budget (rule 11).</b> A closed solid shows only the faces that look at the camera: {@link Posed#prepare} drops the others and the rest is drawn far to near (painter's order, the vertex buffer has no depth write).
 * The constants {@link #GUILLOTINE_MAX} ... are the most quads a view of each model can keep (checked from 3000 random views by {@code JudgmentModelsTest}); a model that does not fit the remaining buffer budget is skipped
 * whole, never drawn in part.
 *
 * <p>The switch {@code visual.judgmentModels} (CLIENT config, default ON) is mirrored here like {@code HerculesModels}: off = the layer draws exactly what 3.20.0 drew (the flat sprites).
 */
public final class JudgmentModels {
    private JudgmentModels() {}

    // ------------------------------------------------------------------ the switch (a mirror of the CLIENT config, written by JudgmentModelsConfig on the client only)
    private static volatile boolean enabled = true;

    /** True (the default) when the 3D models are drawn; false = the flat sprites of 3.20.0. */
    public static boolean enabled() { return enabled; }

    /** Written by the client config only. */
    public static void setEnabled(boolean on) { enabled = on; }

    // ------------------------------------------------------------------ the atlas: 4 x 4 cells of 128 px (judgment_model_atlas.png)
    public static final int COLS = 4;
    public static final int GOLD = 0, STEEL = 1, EDGE = 2, STONE = 3, STONE_DARK = 4, GLYPH_A = 5, GLYPH_B = 6, CREST = 7, CHAIN = 8, PAN = 9, HOT = 10, SHARD_CELL = 11, BLADE_FACE = 12, GOLD_DARK = 13, FEATHER = 14, FULLER = 15;

    /** The most quads a view of each model keeps after the faces turned away are dropped (tested from random views). */
    public static final int GUILLOTINE_MAX = 18, SCALES_MAX = 56, TABLET_MAX = 14, SHARD_MAX = 4, LANCE_MAX = 22, SWORD_MAX = 36;

    // ------------------------------------------------------------------ the mesh
    /** Part ids of the scales: the pillar and ring, the beam, the left pan (with its chains) and the right pan. */
    public static final int S_BASE = 0, S_BEAM = 1, S_PAN_L = 2, S_PAN_R = 3;
    /** Where the beam turns (local model space of the scales) and how far from it the pans hang. */
    public static final float SCALES_PIVOT_Y = 2.05f, SCALES_ARM = 1.47f;

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
        public boolean twoSided(int face) { return two[face]; }
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
         * A face a-b-c-d (a = bottom left of the texture, b = bottom right, c = top right, d = top left); (rx, ry, rz) is a point inside the solid, so the winding is made to point outward (a two-sided face keeps the order
         * given). When the winding has to be turned the face is MIRRORED left to right (b, a, d, c), never turned on its side, so a texture keeps its up. A triangle repeats its last corner.
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
                put(o, bx, by, bz); put(o + 3, ax, ay, az); put(o + 6, dx, dy, dz); put(o + 9, cx, cy, cz);
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

        /** A triangle (a, b, c); a is the bottom left and b the bottom right of the texture, c the apex. */
        void tri(int c, float gl, boolean twoSided, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float rx, float ry, float rz) {
            face(c, gl, twoSided, null, ax, ay, az, bx, by, bz, cx, cy, cz, cx, cy, cz, rx, ry, rz);
        }

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
            for (int k = 0; k < 4; k++) {
                if ((mask & (1 << k)) == 0) continue;
                int i = k, j = (k + 1) % 4;
                face(cells[k], gl, false, null, A[i * 3], A[i * 3 + 1], A[i * 3 + 2], A[j * 3], A[j * 3 + 1], A[j * 3 + 2], B[j * 3], B[j * 3 + 1], B[j * 3 + 2], B[i * 3], B[i * 3 + 1], B[i * 3 + 2], rx, ry, rz);
            }
            if ((mask & START) != 0) face(cells[4], gl, false, null, A[0], A[1], A[2], A[3], A[4], A[5], A[6], A[7], A[8], A[9], A[10], A[11], rx, ry, rz);
            if ((mask & END) != 0) face(cells[5], gl, false, null, B[0], B[1], B[2], B[3], B[4], B[5], B[6], B[7], B[8], B[9], B[10], B[11], rx, ry, rz);
        }

        /** An axis-aligned box; the cells are -X, +X, -Y, +Y, -Z, +Z and the mask bit i keeps face i. */
        void box(float cx, float cy, float cz, float hx, float hy, float hz, int[] cells, float gl, int mask) {
            float x0 = cx - hx, x1 = cx + hx, y0 = cy - hy, y1 = cy + hy, z0 = cz - hz, z1 = cz + hz;
            if ((mask & 1) != 0) face(cells[0], gl, false, null, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, cx, cy, cz);
            if ((mask & 2) != 0) face(cells[1], gl, false, null, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, cx, cy, cz);
            if ((mask & 4) != 0) face(cells[2], gl, false, null, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, cx, cy, cz);
            if ((mask & 8) != 0) face(cells[3], gl, false, null, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, cx, cy, cz);
            if ((mask & 16) != 0) face(cells[4], gl, false, null, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, cx, cy, cz);
            if ((mask & 32) != 0) face(cells[5], gl, false, null, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, cx, cy, cz);
        }

        /**
         * An n-sided frustum from the centre a (radius r0) to the centre b (radius r1), {@code rot} turns the polygon; the sides are textured bottom (at a) to top (at b). Caps: 1 = at a, 2 = at b (a cap is one quad, two for
         * a polygon of more than four sides).
         */
        void frustum(int n, float ax, float ay, float az, float bx, float by, float bz, float r0, float r1, float rot, int cellSide, int cellCap, float gl, boolean twoSided, int caps) {
            float dx = bx - ax, dy = by - ay, dz = bz - az;
            float dl = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            dx /= dl; dy /= dl; dz /= dl;
            float hx = Math.abs(dy) < 0.9f ? 0f : 1f, hy = Math.abs(dy) < 0.9f ? 1f : 0f;
            float[] p = cross(hx, hy, 0f, dx, dy, dz);
            float pl = (float) Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]);
            float px = p[0] / pl, py = p[1] / pl, pz = p[2] / pl;
            float[] q = cross(dx, dy, dz, px, py, pz);
            float rx = (ax + bx) * 0.5f, ry = (ay + by) * 0.5f, rz = (az + bz) * 0.5f;
            float[] A = new float[n * 3], B = new float[n * 3];
            for (int i = 0; i < n; i++) {
                double th = rot + Math.PI * 2 * i / n;
                float c = (float) Math.cos(th), s = (float) Math.sin(th);
                A[i * 3] = ax + (px * c + q[0] * s) * r0; A[i * 3 + 1] = ay + (py * c + q[1] * s) * r0; A[i * 3 + 2] = az + (pz * c + q[2] * s) * r0;
                B[i * 3] = bx + (px * c + q[0] * s) * r1; B[i * 3 + 1] = by + (py * c + q[1] * s) * r1; B[i * 3 + 2] = bz + (pz * c + q[2] * s) * r1;
            }
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                face(cellSide, gl, twoSided, null, A[i * 3], A[i * 3 + 1], A[i * 3 + 2], A[j * 3], A[j * 3 + 1], A[j * 3 + 2], B[j * 3], B[j * 3 + 1], B[j * 3 + 2], B[i * 3], B[i * 3 + 1], B[i * 3 + 2], rx, ry, rz);
            }
            for (int end = 0; end < 2; end++) {
                if ((caps & (1 << end)) == 0) continue;
                float[] R = end == 0 ? A : B;
                if (n == 4) {
                    capQuad(cellCap, gl, twoSided, R, 0, 1, 2, 3, rx, ry, rz);
                } else {
                    int h = n / 2;
                    capQuad(cellCap, gl, twoSided, R, 0, 1, 2, h, rx, ry, rz);
                    capQuad(cellCap, gl, twoSided, R, h, h + 1, n - 1, 0, rx, ry, rz);
                }
            }
        }

        private void capQuad(int cell, float gl, boolean twoSided, float[] R, int i0, int i1, int i2, int i3, float rx, float ry, float rz) {
            face(cell, gl, twoSided, null, R[i0 * 3], R[i0 * 3 + 1], R[i0 * 3 + 2], R[i1 * 3], R[i1 * 3 + 1], R[i1 * 3 + 2], R[i2 * 3], R[i2 * 3 + 1], R[i2 * 3 + 2], R[i3 * 3], R[i3 * 3 + 1], R[i3 * 3 + 2], rx, ry, rz);
        }

        /** An n-sided pyramid: the base polygon (radius r) round the centre b, the apex at (ex, ey, ez); optional base cap. */
        void pyramid(int n, float bx, float by, float bz, float ex, float ey, float ez, float r, float rot, int cell, float gl, boolean twoSided, boolean cap, int capCell) {
            float dx = ex - bx, dy = ey - by, dz = ez - bz;
            float dl = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            dx /= dl; dy /= dl; dz /= dl;
            float hx = Math.abs(dy) < 0.9f ? 0f : 1f, hy = Math.abs(dy) < 0.9f ? 1f : 0f;
            float[] p = cross(hx, hy, 0f, dx, dy, dz);
            float pl = (float) Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]);
            p[0] /= pl; p[1] /= pl; p[2] /= pl;
            float[] q = cross(dx, dy, dz, p[0], p[1], p[2]);
            float rx = (bx + ex) * 0.5f, ry = (by + ey) * 0.5f, rz = (bz + ez) * 0.5f;
            float[] A = new float[n * 3];
            for (int i = 0; i < n; i++) {
                double th = rot + Math.PI * 2 * i / n;
                float c = (float) Math.cos(th), s = (float) Math.sin(th);
                A[i * 3] = bx + (p[0] * c + q[0] * s) * r; A[i * 3 + 1] = by + (p[1] * c + q[1] * s) * r; A[i * 3 + 2] = bz + (p[2] * c + q[2] * s) * r;
            }
            for (int i = 0; i < n; i++) {
                int j = (i + 1) % n;
                tri(cell, gl, twoSided, A[i * 3], A[i * 3 + 1], A[i * 3 + 2], A[j * 3], A[j * 3 + 1], A[j * 3 + 2], ex, ey, ez, rx, ry, rz);
            }
            if (cap && n == 4) face(capCell, gl, twoSided, null, A[0], A[1], A[2], A[3], A[4], A[5], A[6], A[7], A[8], A[9], A[10], A[11], rx, ry, rz);
        }

        /** A leaf blade (one two-sided quad): base centre, the way it points, the way its face looks, length, base half width and tip half width. */
        void leaf(int c, float gl, float[] rc, float bx, float by, float bz, float dx, float dy, float dz, float nx, float ny, float nz, float len, float w0, float w1) {
            float dl = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            dx /= dl; dy /= dl; dz /= dl;
            float sx = ny * dz - nz * dy, sy = nz * dx - nx * dz, sz = nx * dy - ny * dx;
            float sl = (float) Math.sqrt(sx * sx + sy * sy + sz * sz);
            sx /= sl; sy /= sl; sz /= sl;
            float tx = bx + dx * len, ty = by + dy * len, tz = bz + dz * len;
            face(c, gl, true, rc, bx - sx * w0, by - sy * w0, bz - sz * w0, bx + sx * w0, by + sy * w0, bz + sz * w0, tx + sx * w1, ty + sy * w1, tz + sz * w1, tx - sx * w1, ty - sy * w1, tz - sz * w1, bx, by, bz);
        }

        /** A flat annulus in the plane z = cz (two-sided quads): n segments between radius ri and ro round (cx, cy). */
        void annulus(int n, float cx, float cy, float cz, float ri, float ro, int cell, float gl) {
            for (int i = 0; i < n; i++) {
                double a0 = Math.PI * 2 * i / n, a1 = Math.PI * 2 * (i + 1) / n;
                float c0 = (float) Math.cos(a0), s0 = (float) Math.sin(a0), c1 = (float) Math.cos(a1), s1 = (float) Math.sin(a1);
                face(cell, gl, true, null, cx + c0 * ri, cy + s0 * ri, cz, cx + c1 * ri, cy + s1 * ri, cz, cx + c1 * ro, cy + s1 * ro, cz, cx + c0 * ro, cy + s0 * ro, cz, cx, cy, cz);
            }
        }

        private static float[] cross(float ax, float ay, float az, float bx, float by, float bz) {
            return new float[]{ay * bz - az * by, az * bx - ax * bz, ax * by - ay * bx};
        }

        Mesh build(String name, int maxDrawn) {
            return new Mesh(name, maxDrawn, Math.max(1, parts), Arrays.copyOf(parent, Math.max(1, parts)), Arrays.copyOf(pivot, Math.max(1, parts) * 3), faces, Arrays.copyOf(pos, faces * 12), Arrays.copyOf(cell, faces),
                    Arrays.copyOf(part, faces), Arrays.copyOf(rect, faces * 4), Arrays.copyOf(glow, faces), Arrays.copyOf(two, faces));
        }
    }

    private static float[] r(float u0, float v0, float u1, float v1) { return new float[]{u0, v0, u1, v1}; }

    // ================================================================== 1. the guillotine blade: x across (the width), y up (away from the cutting edge), the bevel and the glowing edge on the -Z side
    public static final Mesh GUILLOTINE = buildGuillotine();
    /** Half width of the blade, the height of the top and the lowest point of the slanted edge (model units). */
    public static final float BLADE_HALF_W = 0.95f, BLADE_TOP = 0.8f, BLADE_EDGE_LOW = -0.75f;

    private static float edgeY(float x) { return -0.75f + (x + 0.95f) * (1.05f / 1.9f); }

    private static Mesh buildGuillotine() {
        Builder b = new Builder();
        b.part(-1, 0f, 0f, 0f);
        float t = 0.07f, top = BLADE_TOP, off = 0.26f, ex = BLADE_HALF_W;
        // the front (the side that looks at the caster): five vertical strips from the bevel up to the top; the two gold straps stand a little proud and carry the bolts
        float[] xs = {-ex, -0.60f, -0.36f, 0.36f, 0.60f, ex};
        int[] cells = {STEEL, GOLD, BLADE_FACE, GOLD, STEEL};
        for (int i = 0; i < 5; i++) {
            float xl = xs[i], xr = xs[i + 1];
            boolean strap = cells[i] == GOLD;
            float zf = strap ? -t - 0.035f : -t;
            b.face(cells[i], strap ? 0.12f : 0.04f, false, null, xl, edgeY(xl) + off, zf, xr, edgeY(xr) + off, zf, xr, top, zf, xl, top, zf, 0f, 0f, 0f);
            if (strap) {
                b.face(GOLD_DARK, 0.05f, false, null, xl, edgeY(xl) + off, -t, xl, top, -t, xl, top, zf, xl, edgeY(xl) + off, zf, (xl + xr) * 0.5f, 0.2f, -t - 0.017f);
                b.face(GOLD_DARK, 0.05f, false, null, xr, edgeY(xr) + off, zf, xr, top, zf, xr, top, -t, xr, edgeY(xr) + off, -t, (xl + xr) * 0.5f, 0.2f, -t - 0.017f);
            }
        }
        // the bevel (one-sided, like a real blade) and the glowing edge along its lower fifth
        float f = 0.80f;
        for (int pass = 0; pass < 2; pass++) {
            float lo = pass == 0 ? 0f : f, hi = pass == 0 ? f : 1f;
            float yl0 = edgeY(-ex) + off * lo, yr0 = edgeY(ex) + off * lo, yl1 = edgeY(-ex) + off * hi, yr1 = edgeY(ex) + off * hi;
            float z0 = t - 2f * t * lo, z1 = t - 2f * t * hi;
            if (pass == 0) b.face(EDGE, 1f, false, null, -ex, yl0, z0, ex, yr0, z0, ex, yr1, z1, -ex, yl1, z1, 0f, 0f, 0f);
            else b.face(STEEL, 0.1f, false, r(0.05f, 0.05f, 0.95f, 0.95f), -ex, yl0, z0, ex, yr0, z0, ex, yr1, z1, -ex, yl1, z1, 0f, 0f, 0f);
        }
        // the back: three strips from the edge to the top
        float[] bx = {-ex, -0.30f, 0.30f, ex};
        int[] bc = {STEEL, GOLD, STEEL};
        for (int i = 0; i < 3; i++) {
            float xl = bx[i], xr = bx[i + 1];
            b.face(bc[i], 0.04f, false, null, xr, edgeY(xr), t, xl, edgeY(xl), t, xl, top, t, xr, top, t, 0f, 0f, 0f);
        }
        // the two ends
        b.face(STEEL, 0.02f, false, null, -ex, top, -t, -ex, edgeY(-ex) + off, -t, -ex, edgeY(-ex), t, -ex, top, t, 0f, 0f, 0f);
        b.face(STEEL, 0.02f, false, null, ex, edgeY(ex) + off, -t, ex, top, -t, ex, top, t, ex, edgeY(ex), t, 0f, 0f, 0f);
        // the gold weight on top (the head of the blade) and the tang that points back at the pivot
        b.box(0f, top + 0.11f, 0f, ex + 0.04f, 0.11f, t + 0.07f, new int[]{GOLD_DARK, GOLD_DARK, GOLD, GOLD, GOLD, GOLD}, 0.1f, 2 | 1 | 8 | 16 | 32);
        b.box(0f, top + 0.22f + 0.5f, 0f, 0.075f, 0.5f, 0.075f, new int[]{GOLD_DARK, GOLD_DARK, GOLD, GOLD, GOLD_DARK, GOLD_DARK}, 0.08f, 1 | 2 | 8 | 16 | 32);
        return b.build("guillotine", GUILLOTINE_MAX);
    }

    // ================================================================== 2. the scales of judgment: the beam turns about its pivot (roll), the pans hang from its ends (rolled back, so they stay level)
    public static final Mesh SCALES = buildScales();

    private static Mesh buildScales() {
        Builder b = new Builder();
        b.part(-1, 0f, 0f, 0f);
        // the pillar: a spire under it, a tapering column, a collar, the ring, a finial
        b.pyramid(4, 0f, 0.9f, 0f, 0f, 0.12f, 0f, 0.2f, (float) Math.PI / 4f, GOLD_DARK, 0.06f, false, false, 0);
        b.frustum(6, 0f, 0.9f, 0f, 0f, 1.85f, 0f, 0.2f, 0.12f, 0f, GOLD, GOLD, 0.06f, false, 0);
        b.frustum(6, 0f, 1.85f, 0f, 0f, 2.0f, 0f, 0.22f, 0.22f, 0f, GOLD_DARK, GOLD, 0.1f, false, 2);
        b.annulus(8, 0f, SCALES_PIVOT_Y, 0f, 0.44f, 0.52f, GOLD, 0.2f);
        b.pyramid(4, 0f, 2.57f, 0f, 0f, 2.86f, 0f, 0.07f, (float) Math.PI / 4f, GOLD, 0.25f, false, false, 0);
        // the beam: a slim bar, a boss over the pivot, a point at either end
        b.part(S_BASE, 0f, SCALES_PIVOT_Y, 0f);
        b.block(-SCALES_ARM, 0f, 0f, SCALES_ARM, 0f, 0f, 0.055f, 0.07f, 0.055f, 0.07f, 0f, 1f, 0f, Builder.BOT | Builder.RIGHT | Builder.TOP | Builder.LEFT, new int[]{GOLD_DARK, GOLD, GOLD, GOLD, 0, 0}, 0.1f);
        b.box(0f, 0f, 0f, 0.14f, 0.12f, 0.1f, new int[]{GOLD_DARK, GOLD_DARK, GOLD_DARK, GOLD, GOLD, GOLD}, 0.15f, 63);
        b.pyramid(4, SCALES_ARM, 0f, 0f, SCALES_ARM + 0.2f, 0f, 0f, 0.09f, (float) Math.PI / 4f, GOLD, 0.15f, false, false, 0);
        b.pyramid(4, -SCALES_ARM, 0f, 0f, -SCALES_ARM - 0.2f, 0f, 0f, 0.09f, (float) Math.PI / 4f, GOLD, 0.15f, false, false, 0);
        // the pans: three chains and a six-sided dish each; the heavier pan (right) holds a burning heart, the lighter one a feather
        for (int side = 0; side < 2; side++) {
            b.part(S_BEAM, side == 0 ? -SCALES_ARM : SCALES_ARM, -0.05f, 0f);
            float rim = 0.5f;
            for (int k = 0; k < 3; k++) {
                double th = Math.PI * 0.5 + k * Math.PI * 2 / 3;
                float tx = (float) Math.cos(th) * rim, tz = (float) Math.sin(th) * rim, ty = -1.04f;
                float len = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
                float nz = 1f;
                // the face of a chain looks towards +Z (the viewer), made perpendicular to the chain
                float dd = (tx * 0f + ty * 0f + tz * 1f) / len / len;
                float nx2 = -tx * dd, ny2 = -ty * dd, nz2 = nz - tz * dd;
                b.leaf(CHAIN, 0.1f, r(0.4375f, 0f, 0.5625f, 1f), 0f, 0f, 0f, tx, ty, tz, nx2, ny2, nz2, len, 0.055f, 0.055f);
            }
            b.frustum(6, 0f, -1.2f, 0f, 0f, -1.05f, 0f, 0.28f, 0.56f, 0f, PAN, PAN, 0.12f, true, 0);
            b.face(GOLD_DARK, 0.05f, true, null, 0.2f, -1.2f, 0.2f, 0.2f, -1.2f, -0.2f, -0.2f, -1.2f, -0.2f, -0.2f, -1.2f, 0.2f, 0f, -1.2f, 0f);
            if (side == 1) {
                b.pyramid(4, 0f, -0.95f, 0f, 0f, -0.78f, 0f, 0.13f, 0f, HOT, 1f, false, false, 0);
                b.pyramid(4, 0f, -0.95f, 0f, 0f, -1.12f, 0f, 0.13f, 0f, HOT, 1f, false, false, 0);
            } else {
                b.leaf(FEATHER, 0.35f, null, -0.22f, -1.15f, 0f, 0.55f, 0.4f, 0.12f, 0f, 0f, 1f, 0.8f, 0.1f, 0.04f);
            }
        }
        return b.build("scales", SCALES_MAX);
    }

    // ================================================================== 3. the law tablet (front = +Z, bottom at y = 0) and a shard of it
    public static final Mesh TABLET = buildTablet();
    public static final Mesh SHARD = buildShard();
    /** The tablet is this tall and this wide (model units). */
    public static final float TABLET_H = 3.25f, TABLET_HALF_W = 0.5f;

    private static Mesh buildTablet() {
        Builder b = new Builder();
        b.part(-1, 0f, 0f, 0f);
        float w = TABLET_HALF_W, z = 0.15f, y0 = 0.2f;
        float[] ys = {y0, 1.05f, 1.9f, 2.75f};
        int[] cells = {GLYPH_A, GLYPH_B, CREST};
        for (int i = 0; i < 3; i++) {
            b.face(cells[i], 0.85f, false, null, -w, ys[i], z, w, ys[i], z, w, ys[i + 1], z, -w, ys[i + 1], z, 0f, 1.5f, 0f);
        }
        float sh = 0.34f, ys2 = 3.1f, pk = 3.25f;
        // the arched head: a shoulder piece and the peak (front)
        b.face(STONE, 0.1f, false, null, -w, 2.75f, z, w, 2.75f, z, sh, ys2, z, -sh, ys2, z, 0f, 1.5f, 0f);
        b.tri(STONE, 0.1f, false, -sh, ys2, z, sh, ys2, z, 0f, pk, z, 0f, 1.5f, 0f);
        // the back: a plain slab with the same outline
        b.face(STONE_DARK, 0.02f, false, null, w, y0, -z, -w, y0, -z, -w, 2.75f, -z, w, 2.75f, -z, 0f, 1.5f, 0f);
        b.face(STONE_DARK, 0.02f, false, null, w, 2.75f, -z, -w, 2.75f, -z, -sh, ys2, -z, sh, ys2, -z, 0f, 1.5f, 0f);
        b.tri(STONE_DARK, 0.02f, false, sh, ys2, -z, -sh, ys2, -z, 0f, pk, -z, 0f, 1.5f, 0f);
        // the sides and the roof
        b.face(STONE_DARK, 0.02f, false, null, -w, y0, z, -w, y0, -z, -w, 2.75f, -z, -w, 2.75f, z, 0f, 1.5f, 0f);
        b.face(STONE_DARK, 0.02f, false, null, w, y0, -z, w, y0, z, w, 2.75f, z, w, 2.75f, -z, 0f, 1.5f, 0f);
        b.face(STONE, 0.04f, false, null, -w, 2.75f, z, -w, 2.75f, -z, -sh, ys2, -z, -sh, ys2, z, 0f, 1.5f, 0f);
        b.face(STONE, 0.04f, false, null, w, 2.75f, -z, w, 2.75f, z, sh, ys2, z, sh, ys2, -z, 0f, 1.5f, 0f);
        b.face(STONE, 0.05f, false, null, -sh, ys2, z, -sh, ys2, -z, 0f, pk, -z, 0f, pk, z, 0f, 1.5f, 0f);
        b.face(STONE, 0.05f, false, null, sh, ys2, -z, sh, ys2, z, 0f, pk, z, 0f, pk, -z, 0f, 1.5f, 0f);
        // the plinth
        b.box(0f, 0.1f, 0f, w + 0.14f, 0.1f, z + 0.1f, new int[]{STONE_DARK, STONE_DARK, STONE_DARK, STONE, STONE_DARK, STONE}, 0.04f, 1 | 2 | 8 | 16 | 32);
        return b.build("tablet", TABLET_MAX);
    }

    /** A broken piece of the tablet: five faces (a triangular prism, skewed), the fresh break glows. */
    private static Mesh buildShard() {
        Builder b = new Builder();
        b.part(-1, 0f, 0f, 0f);
        float[] a = {-0.30f, -0.26f, -0.16f}, bb = {0.34f, -0.20f, -0.12f}, c = {0.02f, 0.42f, -0.20f};
        float[] a2 = {-0.22f, -0.22f, 0.20f}, b2 = {0.30f, -0.30f, 0.16f}, c2 = {-0.04f, 0.30f, 0.24f};
        b.tri(SHARD_CELL, 0.55f, false, a[0], a[1], a[2], c[0], c[1], c[2], bb[0], bb[1], bb[2], 0f, 0f, 0f);
        b.tri(SHARD_CELL, 0.55f, false, a2[0], a2[1], a2[2], b2[0], b2[1], b2[2], c2[0], c2[1], c2[2], 0f, 0f, 0f);
        b.face(STONE, 0.05f, false, null, a[0], a[1], a[2], bb[0], bb[1], bb[2], b2[0], b2[1], b2[2], a2[0], a2[1], a2[2], 0f, 0f, 0f);
        b.face(GLYPH_A, 0.5f, false, r(0.1f, 0.1f, 0.9f, 0.9f), bb[0], bb[1], bb[2], c[0], c[1], c[2], c2[0], c2[1], c2[2], b2[0], b2[1], b2[2], 0f, 0f, 0f);
        b.face(STONE_DARK, 0.05f, false, null, c[0], c[1], c[2], a[0], a[1], a[2], a2[0], a2[1], a2[2], c2[0], c2[1], c2[2], 0f, 0f, 0f);
        return b.build("shard", SHARD_MAX);
    }

    // ================================================================== 4. the lance head: +Z the way it flies, the tip at z = LANCE_TIP, the socket behind
    public static final Mesh LANCE = buildLance();
    public static final float LANCE_TIP = 1.7f, LANCE_BACK = -0.7f;

    private static Mesh buildLance() {
        Builder b = new Builder();
        b.part(-1, 0f, 0f, 0f);
        // a six-sided (flattened) section: points at 0, 60 ... degrees; the blade is a hexagonal pyramid from the shoulder to the tip and a short taper behind it
        float zs = 0.5f, zb = 0.02f;
        float w = 0.17f, h = 0.115f, wb = 0.075f, hb = 0.05f;
        float[] sx = new float[6], sy = new float[6], bx = new float[6], by = new float[6];
        for (int k = 0; k < 6; k++) {
            double a = Math.PI / 3 * k;
            sx[k] = (float) Math.cos(a) * w; sy[k] = (float) Math.sin(a) * h;
            bx[k] = (float) Math.cos(a) * wb; by[k] = (float) Math.sin(a) * hb;
        }
        for (int k = 0; k < 6; k++) {
            int n = (k + 1) % 6;
            int cell = (k & 1) == 0 ? STEEL : BLADE_FACE;
            b.tri(cell, 0.22f, false, sx[k], sy[k], zs, sx[n], sy[n], zs, 0f, 0f, LANCE_TIP, 0f, 0f, 0.5f);
            b.face(cell, 0.1f, false, null, bx[k], by[k], zb, bx[n], by[n], zb, sx[n], sy[n], zs, sx[k], sy[k], zs, 0f, 0f, 0.3f);
        }
        // the barbs: four thin swept-back plates at the shoulder
        for (int s = -1; s <= 1; s += 2) {
            b.tri(GOLD, 0.12f, true, s * 0.15f, 0f, zs - 0.02f, s * 0.07f, 0f, 0.12f, s * 0.44f, 0f, -0.02f, 0f, 0f, 0.3f);
            b.tri(GOLD, 0.12f, true, 0f, s * 0.11f, zs - 0.02f, 0f, s * 0.05f, 0.12f, 0f, s * 0.33f, -0.02f, 0f, 0f, 0.3f);
        }
        // the gold socket: a collar at the root of the blade, a tube and a band
        b.frustum(6, 0f, 0f, -0.02f, 0f, 0f, 0.16f, 0.12f, 0.12f, 0f, GOLD, GOLD, 0.2f, false, 2);
        b.frustum(6, 0f, 0f, -0.7f, 0f, 0f, -0.02f, 0.075f, 0.1f, 0f, GOLD_DARK, GOLD_DARK, 0.1f, false, 1);
        b.frustum(6, 0f, 0f, -0.42f, 0f, 0f, -0.31f, 0.112f, 0.112f, 0f, GOLD, GOLD, 0.2f, false, 0);
        return b.build("lance", LANCE_MAX);
    }

    // ================================================================== 5. the sword of judgment: the tip at z = 0, the hilt up +Z, the broad faces look along +-Y, the guard spreads along X
    public static final Mesh SWORD = buildSword();
    /** The z of the guard centre, the top of the hilt and the half span of the guard (model units). */
    public static final float SWORD_GUARD_Z = 4.94f, SWORD_TOP_Z = 6.08f, SWORD_BLADE_HALF_W = 0.30f;

    private static Mesh buildSword() {
        Builder b = new Builder();
        b.part(-1, 0f, 0f, 0f);
        float zt = 0.9f, zg = 4.8f;
        float[] wx = {-0.30f, -0.20f, -0.07f, 0.07f, 0.20f, 0.30f};
        float[] wy = {0f, 0.05f, 0.036f, 0.036f, 0.05f, 0f};
        int[] bc = {STEEL, BLADE_FACE, FULLER, BLADE_FACE, STEEL};
        float[] bg = {0.12f, 0.04f, 0.9f, 0.04f, 0.12f};
        float k0 = 0.84f;                                                      // the blade is a little narrower at the tip than at the guard
        for (int side = -1; side <= 1; side += 2) {
            for (int i = 0; i < 5; i++) {
                float xl = wx[i], xr = wx[i + 1], yl = wy[i] * side, yr = wy[i + 1] * side;
                if (side > 0) {
                    b.face(bc[i], bg[i], false, null, xl * k0, yl, zt, xr * k0, yr, zt, xr, yr, zg, xl, yl, zg, 0f, 0f, 2f);
                    b.tri(bc[i], bg[i], false, xl * k0, yl, zt, xr * k0, yr, zt, 0f, 0f, 0f, 0f, 0f, 2f);
                } else {
                    b.face(bc[i], bg[i], false, null, xr * k0, yr, zt, xl * k0, yl, zt, xl, yl, zg, xr, yr, zg, 0f, 0f, 2f);
                    b.tri(bc[i], bg[i], false, xr * k0, yr, zt, xl * k0, yl, zt, 0f, 0f, 0f, 0f, 0f, 2f);
                }
            }
        }
        // the guard: a central block, a lower bar with pointed ends and a glowing jewel
        b.box(0f, 0f, zg + 0.14f, 0.40f, 0.14f, 0.14f, new int[]{GOLD_DARK, GOLD_DARK, GOLD_DARK, GOLD, GOLD_DARK, GOLD}, 0.12f, 63);
        b.box(0f, 0f, zg - 0.01f, 0.96f, 0.065f, 0.065f, new int[]{GOLD_DARK, GOLD_DARK, GOLD_DARK, GOLD, GOLD_DARK, GOLD}, 0.12f, 63);
        b.face(HOT, 1f, true, null, 0f, 0.145f, zg + 0.0f, 0.1f, 0.145f, SWORD_GUARD_Z + 0.02f, 0f, 0.145f, zg + 0.28f, -0.1f, 0.145f, SWORD_GUARD_Z + 0.02f, 0f, 0f, 0f);
        b.face(HOT, 1f, true, null, 0f, -0.145f, zg + 0.0f, 0.1f, -0.145f, SWORD_GUARD_Z + 0.02f, 0f, -0.145f, zg + 0.28f, -0.1f, -0.145f, SWORD_GUARD_Z + 0.02f, 0f, 0f, 0f);
        // the wings: four feathers either side, fanned up and out, the shortest on top
        float[] ang = {8f, 24f, 40f, 56f}, len = {1.3f, 1.12f, 0.95f, 0.8f};
        for (int s = -1; s <= 1; s += 2) {
            for (int k = 0; k < 4; k++) {
                double a = Math.toRadians(ang[k]);
                float yoff = 0.012f * (k + 1) * s;
                b.leaf(FEATHER, 0.3f, null, s * 0.42f, yoff, zg + 0.04f + 0.075f * k, s * (float) Math.cos(a), 0f, (float) Math.sin(a), 0f, 1f, 0f, len[k], 0.14f - 0.012f * k, 0.075f);
            }
        }
        // the hilt: a wrapped grip and a pommel
        b.block(0f, 0f, zg + 0.28f, 0f, 0f, 5.78f, 0.085f, 0.085f, 0.07f, 0.07f, 0f, 1f, 0f, Builder.BOT | Builder.RIGHT | Builder.TOP | Builder.LEFT, new int[]{GOLD_DARK, GOLD_DARK, GOLD_DARK, GOLD_DARK, 0, 0}, 0.05f);
        b.pyramid(4, 0f, 0f, 5.92f, 0f, 0f, 5.76f, 0.17f, (float) Math.PI / 4f, GOLD, 0.2f, false, false, 0);
        b.pyramid(4, 0f, 0f, 5.92f, 0f, 0f, SWORD_TOP_Z, 0.17f, (float) Math.PI / 4f, GOLD, 0.25f, false, false, 0);
        return b.build("sword", SWORD_MAX);
    }

    // ================================================================== posing, culling and sorting: pure, allocation-free after construction
    /** One model being posed and prepared for drawing: arrays allocated once, reused for every frame and every model (render thread only). */
    public static final class Posed {
        public static final int MAX_FACES = 100, MAX_PARTS = 24;
        private static final float INSET = 2f / 512f;
        private static final float SUN_X = 0.3f, SUN_Y = 0.85f, SUN_Z = 0.42f;

        private final float[] bend = new float[MAX_PARTS], spread = new float[MAX_PARTS], roll = new float[MAX_PARTS];
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

        /** The world vector of the local vector (x, y, z) under the current axes and scale (to put a model's own point on a spot). */
        public float wx(float x, float y, float z) { return ex * sx * x + fx * sy * y + gx * sz * z; }
        public float wy(float x, float y, float z) { return ey * sx * x + fy * sy * y + gy * sz * z; }
        public float wz(float x, float y, float z) { return ez * sx * x + fz * sy * y + gz * sz * z; }

        public float[] bend() { return bend; }
        public float[] spread() { return spread; }
        public float[] roll() { return roll; }
        public int count() { return count; }

        public void clearPose() { Arrays.fill(bend, 0f); Arrays.fill(spread, 0f); Arrays.fill(roll, 0f); }

        /** The model axes in the world: local X, Y, Z mapped to these vectors (unit length, right-handed), then scaled per axis. */
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

        /** Turns the model about the world axis (ax, ay, az) by {@code angle} (a tumbling shard), scaled per axis. */
        public Posed tumble(float ax, float ay, float az, float angle, float sx, float sy, float sz) {
            float al = (float) Math.sqrt(ax * ax + ay * ay + az * az);
            if (al < 1e-5f) { ax = 0f; ay = 1f; az = 0f; al = 1f; }
            ax /= al; ay /= al; az /= al;
            float c = (float) Math.cos(angle), s = (float) Math.sin(angle), t = 1f - c;
            return axes(t * ax * ax + c, t * ax * ay + s * az, t * ax * az - s * ay,
                    t * ax * ay - s * az, t * ay * ay + c, t * ay * az + s * ax,
                    t * ax * az + s * ay, t * ay * az - s * ax, t * az * az + c, sx, sy, sz);
        }

        /** Y up, turned {@code yaw} about Y (local Z then looks along (sin yaw, 0, cos yaw)), tilted by {@code roll} about that Z, then {@code scale}d (the scales and the tablet face the viewer). */
        public Posed upright(float yaw, float tilt, float scale) {
            float cy = (float) Math.cos(yaw), sy2 = (float) Math.sin(yaw), ct = (float) Math.cos(tilt), st = (float) Math.sin(tilt);
            float x0 = cy, z0 = -sy2, x2 = sy2, z2 = cy;
            // X = (x0, 0, z0) turned towards Y by tilt; Y = (0, 1, 0) turned towards -X by tilt
            return axes(x0 * ct, st, z0 * ct, -x0 * st, ct, -z0 * st, x2, 0f, z2, scale, scale, scale);
        }

        /**
         * Poses {@code mesh} at the camera-relative point (ox, oy, oz), keeps the faces that look at the camera (at the origin), shades them from the sun, and sorts them far to near. Returns the number of faces
         * kept; they are in {@link #pts} / {@link #uv} / {@link #colour} in drawing order (index 0 first).
         *
         * @param tint  the colour that multiplies the shaded texture (alpha = the model's opacity)
         * @param heat  0..1 lifts the shade toward a white-hot glow on the emissive faces
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
                float cr = (float) Math.cos(roll[p]), sr = (float) Math.sin(roll[p]);
                // local = Rz(roll) * Ry(spread) * Rx(bend)
                float k00 = ca, k01 = sa * sb, k02 = sa * cb, k10 = 0f, k11 = cb, k12 = -sb, k20 = -sa, k21 = ca * sb, k22 = ca * cb;
                float l00 = cr * k00 - sr * k10, l01 = cr * k01 - sr * k11, l02 = cr * k02 - sr * k12;
                float l10 = sr * k00 + cr * k10, l11 = sr * k01 + cr * k11, l12 = sr * k02 + cr * k12;
                float l20 = k20, l21 = k21, l22 = k22;
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
                float shade = 0.42f + 0.58f * lit;
                float gl = mesh.glow[f];
                shade += (1f - shade) * gl;
                float hot = heat * (0.25f + 0.75f * gl);
                int i = count++;
                System.arraycopy(tmp, 0, pts, i * 12, 12);
                key[i] = cx * cx + cy * cy + cz * cz;
                float rr = Math.min(255f, tr * shade + 255f * hot * 0.5f), gg = Math.min(255f, tg * shade + 235f * hot * 0.5f), bb = Math.min(255f, tb * shade + 190f * hot * 0.5f);
                colour[i] = (ta << 24) | ((int) rr << 16) | ((int) gg << 8) | (int) bb;
                int c = mesh.cell[f];
                float cu = (c % COLS) * 0.25f, cv = (c / COLS) * 0.25f;
                float u0 = cu + INSET + mesh.rect[f * 4] * (0.25f - 2 * INSET), v0 = cv + INSET + mesh.rect[f * 4 + 1] * (0.25f - 2 * INSET);
                float u1 = cu + INSET + mesh.rect[f * 4 + 2] * (0.25f - 2 * INSET), v1 = cv + INSET + mesh.rect[f * 4 + 3] * (0.25f - 2 * INSET);
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
