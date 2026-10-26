package com.newuniverse.nusmp.vfx.client.layer;

import java.util.Arrays;
import java.util.Random;

/**
 * 3.20.0: the REAL 3D MODELS of the Hercules ultimate (the Titan's hand, the clenched fist, the boulder and the rock chunk, the laurel crown and the laurel floor ring), as PURE geometry: no Minecraft class is named here, so the
 * meshes unit-test and preview headless ({@code HerculesModelsTest}, {@code python tools/vfx_preview/preview.py hercules_grip ...}). They are drawn by {@link HerculesMeshDraw} inside {@link HerculesLayer}; the material
 * atlas is {@code textures/particle/hercules_model_atlas.png} ({@code tools/gen_hercules_model_textures.py}: bronze plates with gold seams, ivory marble, ember-cracked basalt, laurel leaves).
 *
 * <p>A mesh is built ONCE (static, at class load) from tapered blocks, ridged prisms, strips and leaf blades: every face is a quad (a triangle repeats its last corner), stored in the local space of one of its PARTS. Parts
 * form a small skeleton (the finger joints of the hand): a part sits at a pivot in its parent and is turned by a bend (about its X axis, a finger curling toward the palm) and a spread (about its Y axis). Nothing here
 * runs per frame except {@link Posed#prepare}, which writes into arrays that belong to the {@link Posed} and are allocated once.
 *
 * <p><b>Budget (rule 11).</b> A closed solid shows only the faces that look at the camera: {@link Posed#prepare} drops the others and the rest is drawn far to near (painter's order, the vertex buffer has no depth write). The
 * constants {@link #HAND_MAX}, {@link #FIST_MAX}, {@link #BOULDER_MAX}, {@link #CHUNK_MAX}, {@link #CROWN_MAX} and {@link #RING_MAX} are the most quads a view of each model can keep (checked from 3000 random views by
 * {@code HerculesModelsTest}); a model that does not fit the remaining buffer budget is skipped whole, never drawn in part.
 *
 * <p>The switch {@code visual.herculesModels} (CLIENT config, default ON) is mirrored here like {@code FlatProps}: off = the layer draws exactly what 3.19.0 drew (the flat sprites).
 */
public final class HerculesModels {
    private HerculesModels() {}

    // ------------------------------------------------------------------ the switch (a mirror of the CLIENT config, written by HerculesModelsConfig on the client only)
    private static volatile boolean enabled = true;

    /** True (the default) when the 3D models are drawn; false = the flat sprites of 3.19.0. */
    public static boolean enabled() { return enabled; }

    /** Written by the client config only. */
    public static void setEnabled(boolean on) { enabled = on; }

    // ------------------------------------------------------------------ the atlas: 4 x 4 cells of 128 px (hercules_model_atlas.png)
    public static final int COLS = 4;
    public static final int PLATE = 0, PLATE_DARK = 1, SEAM = 2, IVORY = 3, EMBER_ROCK = 4, ROCK = 5, LEAF = 6, BAND = 7, KNUCKLE = 8, HOT = 9, PALM = 10, ROCK_CRACK = 11, LEAF_B = 12, CRIMSON = 13, PLATE_LIGHT = 14, ROCK_DARK = 15;

    /** The most quads a view of each model keeps after the faces turned away are dropped (tested from random views). */
    public static final int HAND_MAX = 40, FIST_MAX = 30, BOULDER_MAX = 16, CHUNK_MAX = 4, CROWN_MAX = 24, RING_MAX = 16;

    // ------------------------------------------------------------------ the mesh
    /** Part ids of the hand (the pose is written with {@link #handPose}). */
    public static final int H_PALM = 0, H_CUFF = 1, H_PLATE = 2, H_FINGER0 = 3, H_THUMB0 = 15, H_PARTS = 18;

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

        Mesh build(String name, int maxDrawn) {
            return new Mesh(name, maxDrawn, Math.max(1, parts), Arrays.copyOf(parent, Math.max(1, parts)), Arrays.copyOf(pivot, Math.max(1, parts) * 3), faces, Arrays.copyOf(pos, faces * 12), Arrays.copyOf(cell, faces),
                    Arrays.copyOf(part, faces), Arrays.copyOf(rect, faces * 4), Arrays.copyOf(glow, faces), Arrays.copyOf(two, faces));
        }
    }

    private static float[] r(float u0, float v0, float u1, float v1) { return new float[]{u0, v0, u1, v1}; }

    private static float hash(int i, int k) {
        long z = i * 0x9E3779B97F4A7C15L + k * 0xBF58476D1CE4E5B9L + 0x2545F4914F6CDD1DL;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= z >>> 31;
        return (z >>> 40) / (float) (1 << 24);
    }

    // ================================================================== the Titan's hand (open, then a fist): about 3 model units long, +Z along the fingers, +Y the back of the hand, +X the thumb side
    public static final Mesh HAND = buildHand();
    /** Closed-fist centre (local model space) the layer puts on the target. */
    public static final float HAND_FIST_Y = -0.12f, HAND_FIST_Z = 0.42f;

    private static Mesh buildHand() {
        Builder b = new Builder();
        int palm = b.part(-1, 0f, 0f, 0f);
        // the palm: a tapered block, wider at the knuckles; the front is left open (the fingers stand in it), the back is under the cuff
        b.block(0f, 0f, -0.30f, 0f, 0f, 0.80f, 0.46f, 0.20f, 0.70f, 0.17f, 0f, 1f, 0f, Builder.BOT | Builder.RIGHT | Builder.TOP | Builder.LEFT,
                new int[]{PALM, PLATE, PLATE_DARK, PLATE, 0, 0}, 0f);
        int cuff = b.part(palm, 0f, 0f, 0f);
        b.block(0f, 0f, -0.80f, 0f, 0f, -0.26f, 0.56f, 0.27f, 0.52f, 0.25f, 0f, 1f, 0f, Builder.ALL & ~Builder.END, new int[]{BAND, BAND, BAND, BAND, PLATE, 0}, 0f);
        int plate = b.part(palm, 0f, 0f, 0f);
        // armour on the back of the hand: a raised plate with a gold inlay and a knuckle bar at its front edge
        b.block(0f, 0.255f, 0.02f, 0f, 0.235f, 0.70f, 0.42f, 0.055f, 0.55f, 0.055f, 0f, 1f, 0f, Builder.RIGHT | Builder.TOP | Builder.LEFT | Builder.END, new int[]{0, PLATE, PLATE_LIGHT, PLATE, 0, KNUCKLE}, 0.12f);
        // four fingers of three plated phalanges: index nearest the thumb (+X) to little finger (-X)
        float[] fx = {0.50f, 0.165f, -0.165f, -0.50f}, fl = {0.94f, 1.04f, 0.98f, 0.80f};
        for (int f = 0; f < 4; f++) {
            float L = fl[f];
            int p1 = b.part(palm, fx[f], 0f, 0.80f);
            b.ridge(0f, 0f, 0f, 0f, 0f, 0.60f * L, 0.165f, 0.11f, 0.17f, 0.155f, 0.105f, 0.16f, 7, PLATE_DARK, PLATE, 0, 0.1f);
            int p2 = b.part(p1, 0f, 0f, 0.60f * L);
            b.ridge(0f, 0f, 0f, 0f, 0f, 0.40f * L, 0.155f, 0.105f, 0.16f, 0.145f, 0.10f, 0.15f, 7, PLATE_DARK, PLATE, 0, 0.1f);
            int p3 = b.part(p2, 0f, 0f, 0.40f * L);
            b.ridge(0f, 0f, 0f, 0f, 0f, 0.32f * L, 0.145f, 0.10f, 0.15f, 0.085f, 0.05f, 0.08f, 7, PLATE_DARK, PLATE, KNUCKLE, 0.18f);
        }
        // the thumb: a heavy metacarpal on the +X side, then two phalanges
        int t0 = b.part(palm, 0.66f, -0.04f, 0.05f);
        b.ridge(0f, 0f, 0f, 0f, 0f, 0.42f, 0.22f, 0.14f, 0.19f, 0.205f, 0.135f, 0.18f, 7, PLATE_DARK, PLATE, 0, 0.1f);
        int t1 = b.part(t0, 0f, 0f, 0.42f);
        b.ridge(0f, 0f, 0f, 0f, 0f, 0.36f, 0.205f, 0.135f, 0.18f, 0.19f, 0.125f, 0.17f, 7, PLATE_DARK, PLATE, 0, 0.1f);
        int t2 = b.part(t1, 0f, 0f, 0.36f);
        b.ridge(0f, 0f, 0f, 0f, 0f, 0.30f, 0.19f, 0.125f, 0.17f, 0.10f, 0.06f, 0.09f, 7, PLATE_DARK, PLATE, KNUCKLE, 0.18f);
        return b.build("hand", HAND_MAX);
    }

    /**
     * Writes the finger joints of the hand: {@code close} 0 = open and fanned, 1 = a fist; {@code splay} widens the fan. {@code bend} and {@code spread} are the arrays of a {@link Posed} (one entry per part).
     */
    public static void handPose(float close, float splay, float[] bend, float[] spread) {
        float c = Math.max(0f, Math.min(1f, close));
        float[] fan = {0.30f, 0.10f, -0.11f, -0.32f};
        float[] extra = {0.0f, 0.05f, 0.1f, 0.18f};
        for (int f = 0; f < 4; f++) {
            int p = H_FINGER0 + f * 3;
            bend[p] = lerp(0.06f - 0.06f * f * 0.3f, 1.42f + extra[f], c);
            bend[p + 1] = lerp(0.03f, 1.62f + extra[f], c);
            bend[p + 2] = lerp(0.0f, 1.18f, c);
            spread[p] = fan[f] * splay * (1f - c);
        }
        int t = H_THUMB0;
        spread[t] = lerp(0.95f, -0.18f, c);
        bend[t] = lerp(0.05f, 0.40f, c);
        bend[t + 1] = lerp(0.10f, 0.80f, c);
        bend[t + 2] = lerp(0.05f, 0.70f, c);
    }

    // ================================================================== the clenched fist: about 2.4 long, +Z the punch, +Y the back of the hand
    public static final Mesh FIST = buildFist();

    private static Mesh buildFist() {
        Builder b = new Builder();
        b.part(-1, 0f, 0f, 0f);
        // the back of the hand (metacarpals) with an armoured plate; a short flared wrist guard and a laurel band
        b.block(0f, 0.04f, -0.50f, 0f, 0.04f, 0.28f, 0.60f, 0.28f, 0.68f, 0.30f, 0f, 1f, 0f, Builder.ALL & ~Builder.END, new int[]{PLATE_DARK, PLATE, PLATE_LIGHT, PLATE, PLATE, 0}, 0.1f);
        b.block(0f, 0.01f, -1.02f, 0f, 0.02f, -0.50f, 0.56f, 0.30f, 0.56f, 0.28f, 0f, 1f, 0f, Builder.ALL & ~Builder.END, new int[]{PLATE_DARK, PLATE, PLATE, PLATE, PLATE, 0}, 0.05f);
        b.block(0f, 0.015f, -0.80f, 0f, 0.015f, -0.62f, 0.62f, 0.33f, 0.62f, 0.33f, 0f, 1f, 0f, Builder.BOT | Builder.RIGHT | Builder.TOP | Builder.LEFT, new int[]{BAND, BAND, BAND, BAND, 0, 0}, 0.22f);
        b.leaf(LEAF_B, 0.3f, 0.52f, 0.30f, -0.78f, 0.55f, 0.35f, -0.62f, 0f, 1f, 0f, 0.62f, 0.12f, 0.06f);
        b.leaf(LEAF_B, 0.3f, -0.52f, 0.30f, -0.78f, -0.55f, 0.35f, -0.62f, 0f, 1f, 0f, 0.62f, 0.12f, 0.06f);
        // the four curled fingers: a strip over the knuckle (top plate, bevel, front, tuck) per finger, in profile (y, z); the row of knuckles is domed (the outer fingers sit lower and tuck in sooner)
        float[] py = {0.30f, 0.38f, 0.14f, -0.28f, -0.50f}, pz = {0.14f, 0.54f, 0.90f, 0.86f, 0.46f};
        float[] fx = {-0.525f, -0.175f, 0.175f, 0.525f}, off = {-0.03f, 0.05f, 0.06f, -0.02f};
        float hw = 0.172f;
        for (int f = 0; f < 4; f++) {
            float xl = fx[f] - hw, xr = fx[f] + hw, zo = off[f];
            float dl = 0.2f * xl * xl, dr = 0.2f * xr * xr;                            // the dome: a little lower toward the outside
            int[] cells = {KNUCKLE, PLATE, PLATE_DARK, PLATE_DARK};
            float[][] rects = {null, r(0.12f, 0.12f, 0.88f, 0.88f), r(0.12f, 0.12f, 0.88f, 0.88f), r(0.12f, 0.12f, 0.88f, 0.88f)};
            for (int s = 0; s < 4; s++) {
                float y0 = py[s], z0 = pz[s] + zo, y1 = py[s + 1], z1 = pz[s + 1] + zo;
                b.face(cells[s], s == 0 ? 0.25f : 0.1f, false, rects[s], xl, y0 - dl, z0, xr, y0 - dr, z0, xr, y1 - dr, z1, xl, y1 - dl, z1, fx[f], 0.05f, 0.4f);
            }
        }
        // the sides of the fist (the outer two fingers): two quads each
        for (int side = -1; side <= 1; side += 2) {
            float x = side * (0.525f + hw), dd = 0.2f * x * x;
            b.face(PLATE, 0.1f, false, r(0.12f, 0.12f, 0.88f, 0.88f), x, 0.30f - dd, 0.11f, x, 0.38f - dd, 0.52f, x, 0.14f - dd, 0.90f, x, -0.05f - dd, 0.28f, 0f, 0.05f, 0.4f);
            b.face(PLATE_DARK, 0.1f, false, r(0.12f, 0.12f, 0.88f, 0.88f), x, 0.14f - dd, 0.90f, x, -0.28f - dd, 0.86f, x, -0.50f - dd, 0.46f, x, -0.05f - dd, 0.28f, 0f, 0.05f, 0.4f);
        }
        // the thumb across the front of the fingers
        b.block(0.74f, -0.06f, 0.40f, 0.20f, -0.20f, 1.0f, 0.19f, 0.16f, 0.17f, 0.15f, 0f, 1f, 0f, Builder.BOT | Builder.RIGHT | Builder.TOP | Builder.LEFT | Builder.END, new int[]{PLATE_DARK, PLATE, PLATE_LIGHT, PLATE, 0, PLATE}, 0.12f);
        return b.build("fist", FIST_MAX);
    }

    // ================================================================== rock: a hurled boulder and a chunk of ground
    public static final Mesh BOULDER = buildBoulder();
    public static final Mesh CHUNK = buildChunk();

    private static Mesh buildBoulder() {
        Builder b = new Builder();
        b.part(-1, 0f, 0f, 0f);
        int lon = 7;
        float[] lat = {52f, 10f, -42f};
        Random rnd = new Random(2024);
        float[][] top = new float[lon][3], mid = new float[lon][3], low = new float[lon][3], pole = {{0f, 1.02f, 0f}, {0f, -0.92f, 0f}};
        float[][][] rings = {top, mid, low};
        for (int k = 0; k < 3; k++) {
            for (int i = 0; i < lon; i++) {
                double a = Math.PI * 2 * (i + (k == 1 ? 0.5 : 0.0) + (rnd.nextFloat() - 0.5) * 0.3) / lon;
                double la = Math.toRadians(lat[k]) + (rnd.nextFloat() - 0.5) * 0.18;
                float rr = 0.80f + rnd.nextFloat() * 0.34f;
                rings[k][i][0] = (float) (Math.cos(la) * Math.cos(a)) * rr * 1.04f;
                rings[k][i][1] = (float) Math.sin(la) * rr * 0.92f;
                rings[k][i][2] = (float) (Math.cos(la) * Math.sin(a)) * rr;
            }
        }
        int n = 0;
        for (int i = 0; i < lon; i++) {
            int j = (i + 1) % lon;
            n = rockFace(b, n, pole[0], top[i], top[j], top[j]);
            n = rockFace(b, n, top[i], mid[i], mid[j], top[j]);
            n = rockFace(b, n, mid[i], low[i], low[j], mid[j]);
            n = rockFace(b, n, low[i], pole[1], pole[1], low[j]);
        }
        return b.build("boulder", BOULDER_MAX);
    }

    private static int rockFace(Builder b, int n, float[] p0, float[] p1, float[] p2, float[] p3) {
        float h = hash(n, 7);
        int c = h < 0.46f ? ROCK : h < 0.78f ? ROCK_CRACK : h < 0.9f ? EMBER_ROCK : ROCK_DARK;
        float g = c == ROCK_CRACK ? 0.55f : c == EMBER_ROCK ? 0.85f : 0f;
        float ou = hash(n, 8) * 0.45f, ov = hash(n, 9) * 0.45f;
        b.face(c, g, false, r(ou, ov, ou + 0.55f, ov + 0.55f), p0[0], p0[1], p0[2], p1[0], p1[1], p1[2], p2[0], p2[1], p2[2], p3[0], p3[1], p3[2], 0f, 0f, 0f);
        return n + 1;
    }

    private static Mesh buildChunk() {
        Builder b = new Builder();
        b.part(-1, 0f, 0f, 0f);
        Random rnd = new Random(77);
        float[][] v = new float[8][3];
        float[] ex = {0.50f, 0.26f, 0.40f};
        for (int i = 0; i < 8; i++) {
            float sx = (i & 1) == 0 ? -1 : 1, sy = (i & 2) == 0 ? -1 : 1, sz = (i & 4) == 0 ? -1 : 1;
            v[i][0] = sx * ex[0] * (0.85f + rnd.nextFloat() * 0.3f);
            v[i][1] = sy * ex[1] * (0.8f + rnd.nextFloat() * 0.4f) + (sy > 0 ? (rnd.nextFloat() - 0.5f) * 0.12f : 0f);
            v[i][2] = sz * ex[2] * (0.85f + rnd.nextFloat() * 0.3f);
        }
        int[][] quads = {{2, 3, 7, 6}, {0, 1, 3, 2}, {4, 5, 7, 6}, {0, 2, 6, 4}, {1, 3, 7, 5}, {0, 1, 5, 4}};
        int n = 100;
        for (int[] q : quads) n = rockFace(b, n, v[q[0]], v[q[1]], v[q[2]], v[q[3]]);
        return b.build("chunk", CHUNK_MAX);
    }

    // ================================================================== laurel: a crown over the head (a band and two branches that meet at the front) and a ring of leaves for the floor
    public static final Mesh CROWN = buildLaurel("crown", 5, true, 0.34f, CROWN_MAX);
    public static final Mesh RING = buildLaurel("ring", 7, false, 0.06f, RING_MAX);

    /** A wreath of radius 1 in the XZ plane (front = +Z): two branches of {@code perBranch} leaves growing from the back to the front, an optional bronze band, a crimson tie at the back. */
    private static Mesh buildLaurel(String name, int perBranch, boolean band, float pitch, int max) {
        Builder b = new Builder();
        b.part(-1, 0f, 0f, 0f);
        if (band) {
            int segs = 8;
            for (int i = 0; i < segs; i++) {
                double a0 = Math.PI * 2 * i / segs, a1 = Math.PI * 2 * (i + 1) / segs;
                float x0 = (float) Math.sin(a0), z0 = (float) Math.cos(a0), x1 = (float) Math.sin(a1), z1 = (float) Math.cos(a1);
                float ro = 1.03f, ri = 0.93f, yt = 0.05f, yb = -0.05f;
                b.face(BAND, 0.25f, false, null, x0 * ro, yb, z0 * ro, x1 * ro, yb, z1 * ro, x1 * ro, yt, z1 * ro, x0 * ro, yt, z0 * ro, 0f, 0f, 0f);
                b.face(BAND, 0.2f, false, null, x0 * ri, yb, z0 * ri, x1 * ri, yb, z1 * ri, x1 * ri, yt, z1 * ri, x0 * ri, yt, z0 * ri, 0f, 0f, 0f);
            }
            b.face(CRIMSON, 0.1f, true, null, -0.06f, -0.02f, -1.0f, 0.06f, -0.02f, -1.0f, 0.16f, -0.30f, -1.12f, -0.16f, -0.30f, -1.12f, 0f, 0f, 0f);
        }
        float len = band ? 0.52f : 0.50f;
        for (int side = -1; side <= 1; side += 2) {
            for (int k = 0; k < perBranch; k++) {
                float t = (k + 0.5f) / perBranch;                                          // 0 at the back, 1 at the front
                double th = Math.PI - 0.34 - t * (Math.PI - 0.62);                         // angle from the front (+Z) round to the back
                float s = (float) Math.sin(th) * side, c = (float) Math.cos(th);
                float rb = 1.0f;
                float bx = s * rb, bz = c * rb;
                // the tangent toward the front is the direction of decreasing th along (side * sin th, cos th)
                float tx = -(float) Math.cos(th) * side, tz = (float) Math.sin(th);
                float ox = s, oz = c;                                                      // outward
                float dirx = tx + ox * 0.35f, diry = pitch, dirz = tz + oz * 0.35f;
                float nx = -ox * 0.3f, ny = 1f, nz = -oz * 0.3f;
                b.leaf((k + (side > 0 ? 0 : 1)) % 2 == 0 ? LEAF : LEAF_B, 0.32f, bx, 0f, bz, dirx, diry, dirz, nx, ny, nz, len * (0.78f + 0.3f * t), 0.15f, 0.07f);
            }
        }
        return b.build(name, max);
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
                float rr = Math.min(255f, tr * shade + 255f * hot * 0.5f), gg = Math.min(255f, tg * shade + 220f * hot * 0.5f), bb = Math.min(255f, tb * shade + 150f * hot * 0.5f);
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
