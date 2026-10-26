package com.newuniverse.nusmp.vfx.client.layer;

import java.util.Arrays;

/**
 * 3.21.0: the REAL 3D MODELS of the Israfil ultimate (Lord of Trials): the Grand Trumpet (a winged brass angel's horn with a flared bell, a coiled tube and a glowing mouth), the Weight Plate (an engraved hexagonal
 * coordinate slab hung on chains under a glowing rune ring), the maze walls and pillars of the Dimensional Labyrinth, the faceted reality-shard and the dark faceted core of the Spatial Collapse and the Coordinate
 * Gate (an arch with a rune ring) of the Coordinate Shift, as PURE geometry: no Minecraft class is named here, so the meshes unit-test and preview headless ({@code IsrafilModelsTest},
 * {@code python tools/vfx_preview/preview.py israfil_trumpet_close ...}). They are drawn by {@link IsrafilMeshDraw} inside {@link TrialLayer}; the material atlas is
 * {@code textures/particle/israfil_model_atlas.png} ({@code tools/gen_israfil_model_textures.py}: engraved brass, indigo stone with cyan coordinate lines, maze relief, glass crystal facets, dark core facets).
 *
 * <p>A mesh is built ONCE (static, at class load) from tapered blocks, surfaces of revolution, swept tubes, triangles and feather blades: every face is a quad (a triangle repeats its last corner and uses the lower
 * right half of its atlas cell), stored in the local space of one of its PARTS. Parts form a tiny skeleton (the rune ring of the weight plate turns on its own): a part sits at a pivot in its parent and is turned by a
 * bend (about its X axis) and a spread (about its Y axis). Nothing here runs per frame except {@link Posed#prepare}, which writes into arrays that belong to the {@link Posed} and are allocated once.
 *
 * <p><b>Budget (rule 11).</b> A closed solid shows only the faces that look at the camera: {@link Posed#prepare} drops the others and the rest is drawn far to near (painter's order, the vertex buffer has no depth
 * write). The constants {@link #HORN_MAX}, {@link #PLATE_MAX}, {@link #WALL_MAX}, {@link #PILLAR_MAX}, {@link #SHARD_MAX}, {@link #CORE_MAX} and {@link #GATE_MAX} are the most quads a view of each model can keep
 * (checked from 3000 random views by {@code IsrafilModelsTest}); a model that does not fit the remaining buffer budget is skipped whole, never drawn in part.
 *
 * <p>The switch {@code visual.israfilModels} (CLIENT config, default ON) is mirrored here like {@code FlatProps}: off = the layer draws exactly what 3.20.0 drew (the flat sprites and planes).
 */
public final class IsrafilModels {
    private IsrafilModels() {}

    // ------------------------------------------------------------------ the switch (a mirror of the CLIENT config, written by IsrafilModelsConfig on the client only)
    private static volatile boolean enabled = true;

    /** True (the default) when the 3D models are drawn; false = the flat sprites of 3.20.0. */
    public static boolean enabled() { return enabled; }

    /** Written by the client config only. */
    public static void setEnabled(boolean on) { enabled = on; }

    // ------------------------------------------------------------------ the atlas: 4 x 4 cells of 128 px (israfil_model_atlas.png)
    public static final int COLS = 4;
    public static final int BRASS = 0, BRASS_DARK = 1, BRASS_BAND = 2, BELL = 3, MOUTH = 4, SLAB_TOP = 5, SLAB_SIDE = 6, MAZE = 7, WALL_TOP = 8, CHAIN = 9, RUNE = 10, GATE_CELL = 11, CRYSTAL_A = 12, CRYSTAL_B = 13, CORE_CELL = 14, FEATHER = 15;

    /** The most quads a view of each model keeps after the faces turned away are dropped (tested from random views). */
    public static final int HORN_MAX = 68, PLATE_MAX = 36, WALL_MAX = 8, PILLAR_MAX = 10, SHARD_MAX = 3, CORE_MAX = 12, GATE_MAX = 14;

    // ------------------------------------------------------------------ the mesh
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
         * A face a-b-c-d (p0 = bottom left of the texture, p1 = bottom right, p2 = top right, p3 = top left); {@code (rx, ry, rz)} is a point inside the solid, so the winding is made to point outward (a
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

        /** Side bits of {@link #prism}: 1 bottom, 2 right, 4 top, 8 left, 16 start cap, 32 end cap. */
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
         * A three-sided ridged prism (a finger of plate armour: a flat underside and two slopes meeting in a ridge) from a to b: half width w and bottom depth hb, ridge height ht (at a and at b). Bits: 1 underside, 2 right
         * slope, 4 left slope, 32 end cap (a triangle). A viewer always sees at most two of the three long faces.
         */
        void ridge(float ax, float ay, float az, float bx, float by, float bz, float w0, float hb0, float ht0, float w1, float hb1, float ht1, int mask, int cBottom, int cSlope, int cEnd, float gl) {
            float dx = bx - ax, dy = by - ay, dz = bz - az;
            float dl = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            dx /= dl; dy /= dl; dz /= dl;
            float sx = dz, sy = 0f, sz = -dx;                                            // cross((0,1,0), d)
            float sl = (float) Math.sqrt(sx * sx + sz * sz);
            if (sl < 1e-4f) { sx = 1f; sz = 0f; sl = 1f; }
            sx /= sl; sz /= sl;
            float ux = dy * sz - dz * sy, uy = dz * sx - dx * sz, uz = dx * sy - dy * sx;
            float[] a0 = corner(ax, ay, az, sx, sz, ux, uy, uz, -w0, -hb0), a1 = corner(ax, ay, az, sx, sz, ux, uy, uz, w0, -hb0), a2 = corner(ax, ay, az, sx, sz, ux, uy, uz, 0f, ht0);
            float[] b0 = corner(bx, by, bz, sx, sz, ux, uy, uz, -w1, -hb1), b1 = corner(bx, by, bz, sx, sz, ux, uy, uz, w1, -hb1), b2 = corner(bx, by, bz, sx, sz, ux, uy, uz, 0f, ht1);
            float rx = (ax + bx) * 0.5f, ry = (ay + by) * 0.5f, rz = (az + bz) * 0.5f;
            if ((mask & 1) != 0) face(cBottom, gl, false, null, a0[0], a0[1], a0[2], a1[0], a1[1], a1[2], b1[0], b1[1], b1[2], b0[0], b0[1], b0[2], rx, ry, rz);
            if ((mask & 2) != 0) face(cSlope, gl, false, null, a1[0], a1[1], a1[2], a2[0], a2[1], a2[2], b2[0], b2[1], b2[2], b1[0], b1[1], b1[2], rx, ry, rz);
            if ((mask & 4) != 0) face(cSlope, gl, false, null, a2[0], a2[1], a2[2], a0[0], a0[1], a0[2], b0[0], b0[1], b0[2], b2[0], b2[1], b2[2], rx, ry, rz);
            if ((mask & END) != 0) face(cEnd, gl, false, null, b0[0], b0[1], b0[2], b1[0], b1[1], b1[2], b2[0], b2[1], b2[2], b2[0], b2[1], b2[2], rx, ry, rz);
        }

        private static float[] corner(float ox, float oy, float oz, float sx, float sz, float ux, float uy, float uz, float s, float u) {
            return new float[]{ox + sx * s + ux * u, oy + uy * u, oz + sz * s + uz * u};
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

        /**
         * A triangle (a, b, c) wound to point away from the inside point (rx, ry, rz); it always lands on the lower right half of its cell (p0 = bottom left, p1 = bottom right, p2 = top right), so the triangle cells of
         * the atlas ({@code CRYSTAL_A}, {@code CRYSTAL_B}, {@code CORE_CELL}) draw their art there.
         */
        void tri(int c, float gl, float[] r, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float rx, float ry, float rz) {
            float nx = (by - ay) * (cz - az) - (bz - az) * (cy - ay);
            float ny = (bz - az) * (cx - ax) - (bx - ax) * (cz - az);
            float nz = (bx - ax) * (cy - ay) - (by - ay) * (cx - ax);
            float mx = (ax + bx + cx) / 3f - rx, my = (ay + by + cy) / 3f - ry, mz = (az + bz + cz) / 3f - rz;
            if (nx * mx + ny * my + nz * mz >= 0f) face(c, gl, false, r, ax, ay, az, bx, by, bz, cx, cy, cz, cx, cy, cz, rx, ry, rz);
            else face(c, gl, false, r, ax, ay, az, cx, cy, cz, bx, by, bz, bx, by, bz, rx, ry, rz);
        }

        /**
         * A surface of revolution about the line (ox, oy, z): ring i has the radius {@code rad[i]} at {@code z[i]}, {@code n} sides starting at the angle {@code rot}; band i (between ring i and i + 1) uses {@code cells[i]}
         * and glows {@code gl[i]}. {@code ex} / {@code ey} stretch the cross section.
         */
        void lathe(float ox, float oy, float[] z, float[] rad, int n, float rot, float ex, float ey, int[] cells, float[] gl) {
            for (int i = 0; i + 1 < z.length; i++) {
                float zm = (z[i] + z[i + 1]) * 0.5f;
                for (int j = 0; j < n; j++) {
                    double a0 = rot + Math.PI * 2 * j / n, a1 = rot + Math.PI * 2 * (j + 1) / n;
                    float c0 = (float) Math.cos(a0) * ex, s0 = (float) Math.sin(a0) * ey, c1 = (float) Math.cos(a1) * ex, s1 = (float) Math.sin(a1) * ey;
                    float r0 = rad[i], r1 = rad[i + 1];
                    face(cells[i], gl[i], false, null, ox + c0 * r0, oy + s0 * r0, z[i], ox + c1 * r0, oy + s1 * r0, z[i], ox + c1 * r1, oy + s1 * r1, z[i + 1], ox + c0 * r1, oy + s0 * r1, z[i + 1], ox, oy, zm);
                }
            }
        }

        /**
         * A tube swept along the points (px, py, pz) with the radius {@code rad[i]} at each, {@code k} sides (a diamond for k = 4) whose first corner looks along the world Y axis; segment i uses {@code cells[i]}.
         */
        void tube(float[] px, float[] py, float[] pz, float[] rad, int k, int[] cells, float gl) {
            int m = px.length;
            float[][] ring = new float[m][k * 3];
            for (int i = 0; i < m; i++) {
                int a = Math.max(0, i - 1), b = Math.min(m - 1, i + 1);
                float tx = px[b] - px[a], ty = py[b] - py[a], tz = pz[b] - pz[a];
                float tl = (float) Math.sqrt(tx * tx + ty * ty + tz * tz);
                tx /= tl; ty /= tl; tz /= tl;
                float d = ty;                                                          // u = world Y made square to the tangent
                float ux = -tx * d, uy = 1f - ty * d, uz = -tz * d;
                float ul = (float) Math.sqrt(ux * ux + uy * uy + uz * uz);
                ux /= ul; uy /= ul; uz /= ul;
                float vx = ty * uz - tz * uy, vy = tz * ux - tx * uz, vz = tx * uy - ty * ux;
                for (int j = 0; j < k; j++) {
                    double an = Math.PI * 2 * j / k;
                    float c = (float) Math.cos(an) * rad[i], s = (float) Math.sin(an) * rad[i];
                    ring[i][j * 3] = px[i] + ux * c + vx * s;
                    ring[i][j * 3 + 1] = py[i] + uy * c + vy * s;
                    ring[i][j * 3 + 2] = pz[i] + uz * c + vz * s;
                }
            }
            for (int i = 0; i + 1 < m; i++) {
                float mx = (px[i] + px[i + 1]) * 0.5f, my = (py[i] + py[i + 1]) * 0.5f, mz = (pz[i] + pz[i + 1]) * 0.5f;
                for (int j = 0; j < k; j++) {
                    int j2 = (j + 1) % k;
                    float[] a = ring[i], b = ring[i + 1];
                    face(cells[i], gl, false, null, a[j * 3], a[j * 3 + 1], a[j * 3 + 2], a[j2 * 3], a[j2 * 3 + 1], a[j2 * 3 + 2], b[j2 * 3], b[j2 * 3 + 1], b[j2 * 3 + 2], b[j * 3], b[j * 3 + 1], b[j * 3 + 2], mx, my, mz);
                }
            }
        }

        /** Gives the face that was added last another UV rectangle inside its cell (a cropped chain strip). */
        void lastRect(float[] rc) { System.arraycopy(rc, 0, rect, (faces - 1) * 4, 4); }

        Mesh build(String name, int maxDrawn) {
            return new Mesh(name, maxDrawn, Math.max(1, parts), Arrays.copyOf(parent, Math.max(1, parts)), Arrays.copyOf(pivot, Math.max(1, parts) * 3), faces, Arrays.copyOf(pos, faces * 12), Arrays.copyOf(cell, faces),
                    Arrays.copyOf(part, faces), Arrays.copyOf(rect, faces * 4), Arrays.copyOf(glow, faces), Arrays.copyOf(two, faces));
        }
    }

    private static float[] r(float u0, float v0, float u1, float v1) { return new float[]{u0, v0, u1, v1}; }

    // ================================================================== the Grand Trumpet: about 5 model units long, +Z the bell (it points up in the game), +Y toward the viewer, the coil lies in the X-Z plane
    public static final Mesh HORN = buildHorn();
    /** The rim of the bell lies at this local Z (the layer puts the blast of light there). */
    public static final float HORN_MOUTH_Z = 2.46f;

    private static Mesh buildHorn() {
        Builder b = new Builder();
        b.part(-1, 0f, 0f, 0f);
        // the mouthpiece: a flared brass cup with a gold rim
        b.lathe(-0.62f, 0f, new float[]{-2.58f, -2.38f, -2.0f}, new float[]{0.27f, 0.20f, 0.10f}, 6, 0f, 1f, 1f, new int[]{BRASS_BAND, BRASS_DARK}, new float[]{0.3f, 0.05f});
        // the tube: a long lead pipe, one turn of a tightening coil (the loop of an angel's horn), then the pipe that runs into the bell
        int loop = 6;
        int n = 1 + loop + 1 + 2;
        float[] px = new float[n], py = new float[n], pz = new float[n], rad = new float[n];
        px[0] = -0.62f; pz[0] = -2.0f; rad[0] = 0.095f;
        for (int k = 0; k <= loop; k++) {
            double th = Math.PI - k * Math.PI * 2 / loop;
            float rr = 0.62f - 0.22f * k / loop;
            px[1 + k] = (float) Math.cos(th) * rr;
            pz[1 + k] = -0.72f + (float) Math.sin(th) * rr;
            py[1 + k] = 0.05f * (float) Math.sin(k * 0.9);
            rad[1 + k] = 0.105f + 0.02f * k / loop;
        }
        px[loop + 2] = -0.20f; pz[loop + 2] = -0.20f; rad[loop + 2] = 0.14f;
        px[loop + 3] = 0.0f; pz[loop + 3] = 0.30f; rad[loop + 3] = 0.15f;
        int[] cells = new int[n - 1];
        for (int i = 0; i < n - 1; i++) cells[i] = i % 3 == 1 ? BRASS_DARK : BRASS;
        b.tube(px, py, pz, rad, 4, cells, 0.04f);
        // gold ferrules where the lead pipe enters the coil and where the pipe enters the bell
        b.lathe(-0.62f, 0f, new float[]{-1.56f, -1.38f}, new float[]{0.17f, 0.17f}, 6, 0f, 1f, 1f, new int[]{BRASS_BAND}, new float[]{0.3f});
        b.lathe(0f, 0f, new float[]{0.14f, 0.36f}, new float[]{0.21f, 0.21f}, 6, 0f, 1f, 1f, new int[]{BRASS_BAND}, new float[]{0.3f});
        // the bell: a flaring horn with a gold lip
        b.lathe(0f, 0f, new float[]{0.28f, 1.05f, 1.80f, 2.32f, HORN_MOUTH_Z}, new float[]{0.17f, 0.31f, 0.64f, 1.08f, 1.14f}, 8, (float) Math.PI / 8f, 1f, 1f,
                new int[]{BELL, BELL, BELL, BRASS_BAND}, new float[]{0f, 0.05f, 0.12f, 0.4f});
        // the glowing mouth (one cut-out disc, it faces along the bell)
        b.face(MOUTH, 1f, false, null, -1.04f, -1.04f, HORN_MOUTH_Z - 0.01f, 1.04f, -1.04f, HORN_MOUTH_Z - 0.01f, 1.04f, 1.04f, HORN_MOUTH_Z - 0.01f, -1.04f, 1.04f, HORN_MOUTH_Z - 0.01f, 0f, 0f, 0f);
        // the wings: three feathers each side, swept back from the coil
        float[][] fd = {{1.0f, 0.04f, -0.05f}, {0.9f, 0.05f, -0.52f}, {0.62f, 0.06f, -0.95f}};
        float[] fl = {1.55f, 1.35f, 1.15f};
        for (int side = -1; side <= 1; side += 2) {
            for (int f = 0; f < 3; f++) b.leaf(FEATHER, 0.28f, side * 0.66f, 0.03f, -0.95f, side * fd[f][0], fd[f][1], fd[f][2], 0f, 1f, 0f, fl[f], 0.17f, 0.07f);
        }
        return b.build("horn", HORN_MAX);
    }

    // ================================================================== the Weight Plate: a hexagonal engraved slab (radius 1, thickness 0.2) on three chains under a rune ring, +Y up
    public static final Mesh PLATE = buildPlate();
    /** Height of the rune ring above the slab (in the radius of the slab). */
    public static final float PLATE_APEX = 1.45f;
    /** Part ids of the plate: 0 the slab with its chains, 1 the rune ring (turns by its own spread). */
    public static final int P_SLAB = 0, P_RING = 1;

    private static Mesh buildPlate() {
        Builder b = new Builder();
        b.part(-1, 0f, 0f, 0f);
        float T = 0.2f;
        float[] oc = new float[7], os = new float[7], ic = new float[7], is = new float[7];
        for (int k = 0; k <= 6; k++) {
            double a = Math.PI / 6 + k * Math.PI / 3;
            oc[k] = (float) Math.cos(a);
            os[k] = (float) Math.sin(a);
        }
        for (int k = 0; k < 6; k++) {
            int j = k + 1;
            float am = (float) (Math.PI / 6 + (k + 0.5) * Math.PI / 3), mc = (float) Math.cos(am), ms = (float) Math.sin(am);
            // the top ring (the engraved coordinate face), the underside, the slanted outer wall and the wall of the hole
            b.face(SLAB_TOP, 0.14f, false, null, oc[k] * 0.93f, T, os[k] * 0.93f, oc[j] * 0.93f, T, os[j] * 0.93f, oc[j] * 0.44f, T, os[j] * 0.44f, oc[k] * 0.44f, T, os[k] * 0.44f, 0f, T * 0.5f, 0f);
            b.face(SLAB_SIDE, 0f, false, null, oc[k], 0f, os[k], oc[j], 0f, os[j], oc[j] * 0.40f, 0f, os[j] * 0.40f, oc[k] * 0.40f, 0f, os[k] * 0.40f, 0f, T * 0.5f, 0f);
            b.face(SLAB_SIDE, 0f, false, null, oc[k], 0f, os[k], oc[j], 0f, os[j], oc[j] * 0.93f, T, os[j] * 0.93f, oc[k] * 0.93f, T, os[k] * 0.93f, 0f, T * 0.5f, 0f);
            b.face(SLAB_SIDE, 0.22f, false, null, oc[k] * 0.40f, 0f, os[k] * 0.40f, oc[j] * 0.40f, 0f, os[j] * 0.40f, oc[j] * 0.44f, T, os[j] * 0.44f, oc[k] * 0.44f, T, os[k] * 0.44f, mc * 0.7f, T * 0.5f, ms * 0.7f);
        }
        // three chains from the corners of the slab up to the rune ring (two crossed strips each, so a chain never vanishes edge on)
        float H = PLATE_APEX;
        for (int k = 0; k < 6; k += 2) {
            float bx = oc[k] * 0.93f, bz = os[k] * 0.93f;
            float dx = -bx, dy = H - T, dz = -bz;
            float len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            b.leaf(CHAIN, 0.2f, bx, T, bz, dx, dy, dz, oc[k], 0f, os[k], len, 0.085f, 0.085f);
            b.lastRect(r(0.31f, 0f, 0.69f, 1f));
            b.leaf(CHAIN, 0.2f, bx, T, bz, dx, dy, dz, -os[k], 0f, oc[k], len, 0.085f, 0.085f);
            b.lastRect(r(0.31f, 0f, 0.69f, 1f));
        }
        // the rune ring: a brass hoop with six glyph tiles standing on it, glowing
        b.part(P_SLAB, 0f, H, 0f);
        for (int k = 0; k < 6; k++) {
            int j = k + 1;
            float rr = 0.34f;
            b.face(BRASS_BAND, 0.35f, false, null, oc[k] * rr, -0.05f, os[k] * rr, oc[j] * rr, -0.05f, os[j] * rr, oc[j] * rr, 0.05f, os[j] * rr, oc[k] * rr, 0.05f, os[k] * rr, 0f, 0f, 0f);
            float am = (float) (Math.PI / 6 + (k + 0.5) * Math.PI / 3), mc = (float) Math.cos(am), ms = (float) Math.sin(am);
            b.leaf(RUNE, 0.9f, mc * 0.30f, 0.05f, ms * 0.30f, 0f, 1f, 0f, mc, 0f, ms, 0.27f, 0.135f, 0.135f);
        }
        return b.build("plate", PLATE_MAX);
    }

    // ================================================================== the maze walls and pillars (1 wide, 1 high, standing on the floor at the origin, +Z the maze face turned toward the cage)
    public static final Mesh WALL_A = buildWall(false);
    public static final Mesh WALL_B = buildWall(true);
    public static final Mesh PILLAR = buildPillar();

    private static Mesh buildWall(boolean mirror) {
        Builder b = new Builder();
        b.part(-1, 0f, 0f, 0f);
        float[] rect = mirror ? r(1f, 0f, 0f, 1f) : null;
        // the body (its two ends; the two faces are the maze reliefs), a wide cap with a gold lip
        b.block(0f, 0f, 0f, 0f, 0.93f, 0f, 0.5f, 0.14f, 0.5f, 0.14f, 0f, 0f, 1f, Builder.RIGHT | Builder.LEFT, new int[]{0, SLAB_SIDE, 0, SLAB_SIDE, 0, 0}, 0.04f);
        b.face(MAZE, 0.25f, false, rect, -0.5f, 0f, 0.141f, 0.5f, 0f, 0.141f, 0.5f, 0.93f, 0.141f, -0.5f, 0.93f, 0.141f, 0f, 0.4f, 0f);
        b.face(MAZE, 0.25f, false, rect, 0.5f, 0f, -0.141f, -0.5f, 0f, -0.141f, -0.5f, 0.93f, -0.141f, 0.5f, 0.93f, -0.141f, 0f, 0.4f, 0f);
        b.block(0f, 0.93f, 0f, 0f, 1.0f, 0f, 0.58f, 0.2f, 0.58f, 0.2f, 0f, 0f, 1f, Builder.BOT | Builder.RIGHT | Builder.TOP | Builder.LEFT | Builder.END, new int[]{WALL_TOP, SLAB_SIDE, WALL_TOP, SLAB_SIDE, 0, WALL_TOP}, 0.1f);
        return b.build(mirror ? "wall_b" : "wall_a", WALL_MAX);
    }

    private static Mesh buildPillar() {
        Builder b = new Builder();
        b.part(-1, 0f, 0f, 0f);
        b.block(0f, 0f, 0f, 0f, 0.08f, 0f, 0.58f, 0.58f, 0.58f, 0.58f, 0f, 0f, 1f, 15, new int[]{SLAB_SIDE, SLAB_SIDE, SLAB_SIDE, SLAB_SIDE, 0, WALL_TOP}, 0.02f);
        b.block(0f, 0.08f, 0f, 0f, 0.9f, 0f, 0.5f, 0.5f, 0.42f, 0.42f, 0f, 0f, 1f, 15, new int[]{MAZE, MAZE, MAZE, MAZE, 0, 0}, 0.25f);
        b.block(0f, 0.9f, 0f, 0f, 1.0f, 0f, 0.5f, 0.5f, 0.62f, 0.62f, 0f, 0f, 1f, 15 | Builder.END, new int[]{SLAB_SIDE, WALL_TOP, SLAB_SIDE, WALL_TOP, 0, WALL_TOP}, 0.1f);
        return b.build("pillar", PILLAR_MAX);
    }

    // ================================================================== a reality shard: an elongated crystal spike (a tetrahedron), the tip is +Z, about 1.25 long
    public static final Mesh SHARD = buildShard();

    private static Mesh buildShard() {
        Builder b = new Builder();
        b.part(-1, 0f, 0f, 0f);
        float[] t = {0f, 0f, 1.0f}, a = {-0.30f, -0.18f, -0.22f}, c = {0.30f, -0.14f, -0.25f}, d = {0.02f, 0.30f, -0.15f};
        float rx = (t[0] + a[0] + c[0] + d[0]) / 4f, ry = (t[1] + a[1] + c[1] + d[1]) / 4f, rz = (t[2] + a[2] + c[2] + d[2]) / 4f;
        b.tri(CRYSTAL_A, 0.4f, null, t[0], t[1], t[2], a[0], a[1], a[2], c[0], c[1], c[2], rx, ry, rz);
        b.tri(CRYSTAL_B, 0.3f, null, t[0], t[1], t[2], c[0], c[1], c[2], d[0], d[1], d[2], rx, ry, rz);
        b.tri(CRYSTAL_A, 0.4f, null, t[0], t[1], t[2], d[0], d[1], d[2], a[0], a[1], a[2], rx, ry, rz);
        b.tri(CRYSTAL_B, 0.2f, null, a[0], a[1], a[2], d[0], d[1], d[2], c[0], c[1], c[2], rx, ry, rz);
        return b.build("shard", SHARD_MAX);
    }

    // ================================================================== the dark core: a faceted sphere (an icosahedron of radius 1) with bright facet edges
    public static final Mesh CORE = buildCore();

    private static Mesh buildCore() {
        Builder b = new Builder();
        b.part(-1, 0f, 0f, 0f);
        float t = (1f + (float) Math.sqrt(5.0)) / 2f;
        float[][] v = {{-1, t, 0}, {1, t, 0}, {-1, -t, 0}, {1, -t, 0}, {0, -1, t}, {0, 1, t}, {0, -1, -t}, {0, 1, -t}, {t, 0, -1}, {t, 0, 1}, {-t, 0, -1}, {-t, 0, 1}};
        for (float[] p : v) {
            float l = (float) Math.sqrt(p[0] * p[0] + p[1] * p[1] + p[2] * p[2]);
            p[0] /= l; p[1] /= l; p[2] /= l;
        }
        int[][] f = {{0, 11, 5}, {0, 5, 1}, {0, 1, 7}, {0, 7, 10}, {0, 10, 11}, {1, 5, 9}, {5, 11, 4}, {11, 10, 2}, {10, 7, 6}, {7, 1, 8}, {3, 9, 4}, {3, 4, 2}, {3, 2, 6}, {3, 6, 8}, {3, 8, 9}, {4, 9, 5}, {2, 4, 11}, {6, 2, 10}, {8, 6, 7}, {9, 8, 1}};
        for (int[] q : f) b.tri(CORE_CELL, 0.12f, null, v[q[0]][0], v[q[0]][1], v[q[0]][2], v[q[1]][0], v[q[1]][1], v[q[1]][2], v[q[2]][0], v[q[2]][1], v[q[2]][2], 0f, 0f, 0f);
        return b.build("core", CORE_MAX);
    }

    // ================================================================== the Coordinate Gate: an arch standing on the ground (the opening is 1.76 wide, 2.1 high), +Z the face
    public static final Mesh GATE = buildGate();

    private static Mesh buildGate() {
        Builder b = new Builder();
        b.part(-1, 0f, 0f, 0f);
        float[][] o = {{-1.12f, 0f}, {-1.12f, 1.7f}, {-0.72f, 2.28f}, {0f, 2.55f}, {0.72f, 2.28f}, {1.12f, 1.7f}, {1.12f, 0f}};
        float[][] in = {{-0.88f, 0f}, {-0.88f, 1.66f}, {-0.58f, 2.08f}, {0f, 2.28f}, {0.58f, 2.08f}, {0.88f, 1.66f}, {0.88f, 0f}};
        float d = 0.10f;
        for (int k = 0; k < 6; k++) {
            int j = k + 1;
            float rx = (o[k][0] + o[j][0] + in[k][0] + in[j][0]) / 4f, ry = (o[k][1] + o[j][1] + in[k][1] + in[j][1]) / 4f;
            float gl = k == 2 || k == 3 ? 0.45f : 0.25f;
            b.face(GATE_CELL, gl, false, null, o[k][0], o[k][1], d, o[j][0], o[j][1], d, in[j][0], in[j][1], d, in[k][0], in[k][1], d, rx, ry, 0f);
            b.face(GATE_CELL, gl, false, null, o[k][0], o[k][1], -d, o[j][0], o[j][1], -d, in[j][0], in[j][1], -d, in[k][0], in[k][1], -d, rx, ry, 0f);
            b.face(SLAB_SIDE, 0f, false, null, o[k][0], o[k][1], -d, o[j][0], o[j][1], -d, o[j][0], o[j][1], d, o[k][0], o[k][1], d, rx, ry, 0f);
            b.face(SLAB_SIDE, 0.2f, false, null, in[k][0], in[k][1], -d, in[j][0], in[j][1], -d, in[j][0], in[j][1], d, in[k][0], in[k][1], d, rx, ry, 0f);
        }
        return b.build("gate", GATE_MAX);
    }

    // ================================================================== posing, culling and sorting: pure, allocation-free after construction
    /** One model being posed and prepared for drawing: arrays allocated once, reused for every frame and every model (render thread only). */
    public static final class Posed {
        public static final int MAX_FACES = 160, MAX_PARTS = 24;
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

        /** Turns the model about the world axis (ax, ay, az) by {@code angle} (a tumbling rock), scaled per axis. */
        public Posed tumble(float ax, float ay, float az, float angle, float sx, float sy, float sz) {
            float al = (float) Math.sqrt(ax * ax + ay * ay + az * az);
            if (al < 1e-5f) { ax = 0f; ay = 1f; az = 0f; al = 1f; }
            ax /= al; ay /= al; az /= al;
            float c = (float) Math.cos(angle), s = (float) Math.sin(angle), t = 1f - c;
            return axes(t * ax * ax + c, t * ax * ay + s * az, t * ax * az - s * ay,
                    t * ax * ay - s * az, t * ay * ay + c, t * ay * az + s * ax,
                    t * ax * az + s * ay, t * ay * az - s * ax, t * az * az + c, sx, sy, sz);
        }

        /** Y up, turned {@code yaw} about Y, then tipped {@code pitch} about the world X, then {@code scale}d (a wreath over a head). */
        public Posed upright(float yaw, float pitch, float tiltX, float tiltZ, float scale) {
            float cy = (float) Math.cos(yaw), sy2 = (float) Math.sin(yaw);
            // local axes after the yaw about Y
            float x0 = cy, y0 = 0f, z0 = -sy2, x1 = 0f, y1 = 1f, z1 = 0f, x2 = sy2, y2 = 0f, z2 = cy;
            // then a tilt that leans the wreath's up axis towards (tiltX, 1, tiltZ): rotate about the horizontal axis perpendicular to the lean
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

        /** Y up, local Z toward the horizontal direction (zx, zz), scaled per axis (a wall, a pillar or a gate standing on the floor). */
        public Posed standing(float zx, float zz, float sx, float sy, float sz) {
            float l = (float) Math.sqrt(zx * zx + zz * zz);
            if (l < 1e-5f) { zx = 0f; zz = 1f; l = 1f; }
            zx /= l; zz /= l;
            return axes(zz, 0f, -zx, 0f, 1f, 0f, zx, 0f, zz, sx, sy, sz);
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
                float shade = 0.38f + 0.62f * lit;
                float gl = mesh.glow[f];
                shade += (1f - shade) * gl;
                float hot = heat * (0.25f + 0.75f * gl);
                int i = count++;
                System.arraycopy(tmp, 0, pts, i * 12, 12);
                key[i] = cx * cx + cy * cy + cz * cz;
                float rr = Math.min(255f, tr * shade + 215f * hot * 0.5f), gg = Math.min(255f, tg * shade + 250f * hot * 0.5f), bb = Math.min(255f, tb * shade + 255f * hot * 0.5f);   // heat is cyan-white, the palette of the layer
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
