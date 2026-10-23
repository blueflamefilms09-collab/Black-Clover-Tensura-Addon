package com.newuniverse.nusmp.book;

import com.newuniverse.nusmp.book.ModeArmorWindV2Rules.Piece;

/**
 * Mode Armour, WIND SPIRIT DIVE v2: the static meshes of Yuno's overlay, built ONCE at class load by pure code (no game class: the unit tests and the preview read the very same arrays the painter draws).
 * Every mesh is a list of quads (4 vertices each) in the model-pixel space of the body part it belongs to (y points down, -z is the front, -x is the wearer's right); the mirrored pieces (arms, legs, wings,
 * ribbons) are built for the left side (+x) and the painter mirrors x for the right. A vertex has a position, a texture coordinate, a smooth normal, a sway weight (0 = rigid, 1 = free end: the painter
 * moves it with a sine wave) and a sway phase.
 *
 * <p>The surfaces are grids ({@link #grid}): lofted rings for the body, the sleeves, the legs and the collar, lofted sheets for the coat tails and the leaf wings, small prisms and pyramids for the crown blades.
 * Normals are the smoothed grid normals, so a ring of twelve sides shades like a round cloth tube, not like a box.</p>
 */
public final class ModeArmorWindV2Mesh {
    /** One mesh: {@code verts} vertices, 4 per quad. */
    public final float[] pos, uv, nrm, sw, ph;
    public final int verts;

    private ModeArmorWindV2Mesh(B b) {
        this.verts = b.n;
        this.pos = java.util.Arrays.copyOf(b.pos, b.n * 3);
        this.uv = java.util.Arrays.copyOf(b.uv, b.n * 2);
        this.nrm = java.util.Arrays.copyOf(b.nrm, b.n * 3);
        this.sw = java.util.Arrays.copyOf(b.sw, b.n);
        this.ph = java.util.Arrays.copyOf(b.ph, b.n);
        for (int q = 0; q + 3 < verts; q += 4) {                                                               // every quad's front face looks where its normal looks
            int a = q * 3;
            float e1x = pos[a + 3] - pos[a], e1y = pos[a + 4] - pos[a + 1], e1z = pos[a + 5] - pos[a + 2];
            float e2x = pos[a + 6] - pos[a], e2y = pos[a + 7] - pos[a + 1], e2z = pos[a + 8] - pos[a + 2];
            float gx = e1y * e2z - e1z * e2y, gy = e1z * e2x - e1x * e2z, gz = e1x * e2y - e1y * e2x;
            if (gx * (nrm[a] + nrm[a + 6]) + gy * (nrm[a + 1] + nrm[a + 7]) + gz * (nrm[a + 2] + nrm[a + 8]) < 0) swap(q + 1, q + 3);
        }
    }

    private void swap(int i, int j) {
        for (int k = 0; k < 3; k++) { float t = pos[i * 3 + k]; pos[i * 3 + k] = pos[j * 3 + k]; pos[j * 3 + k] = t; t = nrm[i * 3 + k]; nrm[i * 3 + k] = nrm[j * 3 + k]; nrm[j * 3 + k] = t; }
        for (int k = 0; k < 2; k++) { float t = uv[i * 2 + k]; uv[i * 2 + k] = uv[j * 2 + k]; uv[j * 2 + k] = t; }
        float t = sw[i]; sw[i] = sw[j]; sw[j] = t;
        t = ph[i]; ph[i] = ph[j]; ph[j] = t;
    }

    public int quads() { return verts / 4; }

    // ------------------------------------------------------------------ the registry
    private static final ModeArmorWindV2Mesh[][] MESHES = new ModeArmorWindV2Mesh[Piece.values().length][3];

    /** The mesh of a piece at a detail index (0 full, 1 mid, 2 far); throws when the piece has no such detail. */
    public static ModeArmorWindV2Mesh get(Piece p, int detail) {
        ModeArmorWindV2Mesh m = MESHES[p.ordinal()][detail];
        if (m == null) throw new IllegalArgumentException(p + " has no detail " + detail);
        return m;
    }

    public static boolean has(Piece p, int detail) { return MESHES[p.ordinal()][detail] != null; }

    // ------------------------------------------------------------------ the build helpers
    private static final class B {
        float[] pos = new float[3 * 512], uv = new float[2 * 512], nrm = new float[3 * 512], sw = new float[512], ph = new float[512];
        int n;

        void v(float x, float y, float z, float u, float v, float nx, float ny, float nz, float w, float p) {
            if (n == sw.length) {
                int c = n * 2;
                pos = java.util.Arrays.copyOf(pos, c * 3); uv = java.util.Arrays.copyOf(uv, c * 2); nrm = java.util.Arrays.copyOf(nrm, c * 3);
                sw = java.util.Arrays.copyOf(sw, c); ph = java.util.Arrays.copyOf(ph, c);
            }
            pos[n * 3] = x; pos[n * 3 + 1] = y; pos[n * 3 + 2] = z;
            uv[n * 2] = u; uv[n * 2 + 1] = v;
            nrm[n * 3] = nx; nrm[n * 3 + 1] = ny; nrm[n * 3 + 2] = nz;
            sw[n] = w; ph[n] = p;
            n++;
        }
    }

    private interface Surf { void at(float a, float b, float[] o); }

    private interface Fn { float f(float a, float b); }

    private static final Fn ZERO = (a, b) -> 0f;

    private static float[] reg(int[] r, int w, int h) { return new float[]{r[0] / (float) w, r[1] / (float) h, r[2] / (float) w, r[3] / (float) h}; }

    private static float[] coat(int[] r) { return reg(r, ModeArmorWindV2Rules.COAT_W, ModeArmorWindV2Rules.COAT_H); }

    private static float[] fur(int[] r) { return reg(r, ModeArmorWindV2Rules.FUR_W, ModeArmorWindV2Rules.FUR_H); }

    private static float[] fx(int[] r) { return reg(r, ModeArmorWindV2Rules.FX_W, ModeArmorWindV2Rules.FX_H); }

    private static float[] wingReg() { return reg(ModeArmorWindV2Rules.R_WING, ModeArmorWindV2Rules.WING_W, ModeArmorWindV2Rules.WING_H); }

    private static float sp(float v, float n) { return Math.signum(v) * (float) Math.pow(Math.abs(v), 2f / n); }

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    private static float[] norm(float x, float y, float z) {
        float l = (float) Math.sqrt(x * x + y * y + z * z);
        return l < 1e-9f ? new float[]{0, -1, 0} : new float[]{x / l, y / l, z / l};
    }

    /**
     * A grid of {@code cols} x {@code rows} quads on the surface {@code s} (a = column 0..1, b = row 0..1). {@code wrap}: the last column meets the first (a ring). The uv comes from {@code reg}
     * (u0, v0, u1, v1; {@code uSpan} of its width is used, {@code flipU} / {@code flipV} mirror it; {@code rowV} gives the v of each row when the rows are not evenly spaced in the texture).
     * The normal is the smoothed grid normal, turned to face {@code ref} (a fixed direction) or, with {@code radial} = +1 / -1, away from / toward the vertical axis through (axX, axZ).
     */
    private static void grid(B bd, int cols, int rows, boolean wrap, Surf s, float[] reg, boolean flipU, boolean flipV, float uSpan, float[] rowV,
                             float[] ref, float axX, float axZ, int radial, Fn w, Fn p) {
        int nc = wrap ? cols : cols + 1;
        float[][][] pt = new float[rows + 1][nc][3];
        for (int r = 0; r <= rows; r++) for (int c = 0; c < nc; c++) s.at(c / (float) cols, r / (float) rows, pt[r][c]);
        float[][][] nr = new float[rows + 1][nc][3];
        for (int r = 0; r <= rows; r++) {
            for (int c = 0; c < nc; c++) {
                int ca = wrap ? (c + 1) % cols : Math.min(c + 1, cols), cb = wrap ? (c - 1 + cols) % cols : Math.max(c - 1, 0);
                int ra = Math.min(r + 1, rows), rb = Math.max(r - 1, 0);
                float[] ta = {pt[r][ca][0] - pt[r][cb][0], pt[r][ca][1] - pt[r][cb][1], pt[r][ca][2] - pt[r][cb][2]};
                float[] tb = {pt[ra][c][0] - pt[rb][c][0], pt[ra][c][1] - pt[rb][c][1], pt[ra][c][2] - pt[rb][c][2]};
                float[] n = norm(ta[1] * tb[2] - ta[2] * tb[1], ta[2] * tb[0] - ta[0] * tb[2], ta[0] * tb[1] - ta[1] * tb[0]);
                float ox, oy, oz;
                if (radial != 0) { ox = (pt[r][c][0] - axX) * radial; oy = 0; oz = (pt[r][c][2] - axZ) * radial; } else { ox = ref[0]; oy = ref[1]; oz = ref[2]; }
                if (n[0] * ox + n[1] * oy + n[2] * oz < 0) { n[0] = -n[0]; n[1] = -n[1]; n[2] = -n[2]; }
                nr[r][c] = n;
            }
        }
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int[] rr = {r, r, r + 1, r + 1}, cc = {c, c + 1, c + 1, c};
                int[] ci = new int[4];
                for (int k = 0; k < 4; k++) ci[k] = wrap ? cc[k] % cols : cc[k];
                float[] p0 = pt[rr[0]][ci[0]], p1 = pt[rr[1]][ci[1]], p2 = pt[rr[2]][ci[2]];
                float[] e1 = {p1[0] - p0[0], p1[1] - p0[1], p1[2] - p0[2]}, e2 = {p2[0] - p0[0], p2[1] - p0[1], p2[2] - p0[2]};
                float[] g = {e1[1] * e2[2] - e1[2] * e2[1], e1[2] * e2[0] - e1[0] * e2[2], e1[0] * e2[1] - e1[1] * e2[0]};
                float[] n0 = nr[rr[0]][ci[0]], n2 = nr[rr[2]][ci[2]];
                boolean flip = g[0] * (n0[0] + n2[0]) + g[1] * (n0[1] + n2[1]) + g[2] * (n0[2] + n2[2]) < 0;      // the front face looks where the normal looks
                for (int kk = 0; kk < 4; kk++) {
                    int k = flip ? (4 - kk) % 4 : kk;
                    float a = cc[k] / (float) cols, b = rr[k] / (float) rows;
                    float aw = (cc[k] % (wrap ? cols : cols + 1)) / (float) cols;
                    float rv = rowV != null ? rowV[rr[k]] : b;
                    float u = reg[0] + (reg[2] - reg[0]) * uSpan * (flipU ? 1f - a : a);
                    float v = reg[1] + (reg[3] - reg[1]) * (flipV ? 1f - rv : rv);
                    float[] q = pt[rr[k]][ci[k]], n = nr[rr[k]][ci[k]];
                    bd.v(q[0], q[1], q[2], u, v, n[0], n[1], n[2], w.f(aw, b), p.f(aw, b));
                }
            }
        }
    }

    /** A surface of rings: each ring is {y, rx, rz, cz}; a = the angle round the axis (0 = the back, +z), b = which ring (evenly spaced). */
    private static Surf loft(float[][] rings, float n, float cx) {
        return (a, b, o) -> {
            float t = b * (rings.length - 1);
            int i = Math.min((int) t, rings.length - 2);
            float f = t - i;
            float[] r0 = rings[i], r1 = rings[i + 1];
            float rx = lerp(r0[1], r1[1], f), rz = lerp(r0[2], r1[2], f), cz = lerp(r0[3], r1[3], f);
            double phi = Math.PI * 2 * a;
            o[0] = cx + rx * sp((float) Math.sin(phi), n);
            o[1] = lerp(r0[0], r1[0], f);
            o[2] = cz + rz * sp((float) Math.cos(phi), n);
        };
    }

    private static float[] rowVofY(float[][] rings) {
        float[] v = new float[rings.length];
        float y0 = rings[0][0], y1 = rings[rings.length - 1][0];
        for (int i = 0; i < rings.length; i++) v[i] = (rings[i][0] - y0) / (y1 - y0);
        return v;
    }

    private static float[][] pick(float[][] rings, int... idx) {
        float[][] o = new float[idx.length][];
        for (int i = 0; i < idx.length; i++) o[i] = rings[idx[i]];
        return o;
    }

    private static final float[] UP = {0, -1, 0}, BACK = {0, 0, 1}, FRONT = {0, 0, -1};

    // ------------------------------------------------------------------ BODY: the coat
    private static final float[][] TORSO_RINGS = {{-0.5f, 4.45f, 2.65f, 0f}, {2.5f, 4.85f, 2.9f, 0f}, {6.0f, 4.7f, 2.8f, 0f}, {9.0f, 4.55f, 2.72f, 0f}, {12.4f, 5.05f, 3.1f, 0f}};

    private static ModeArmorWindV2Mesh torso(int d) {
        B b = new B();
        float[][] rings = d == 0 ? TORSO_RINGS : pick(TORSO_RINGS, 0, 2, 4);
        grid(b, d == 0 ? 12 : 8, rings.length - 1, true, loft(rings, 3.2f, 0f), coat(ModeArmorWindV2Rules.R_TORSO), false, false, 1f, rowVofY(rings), null, 0, 0, 1, ZERO, ZERO);
        return new ModeArmorWindV2Mesh(b);
    }

    private static ModeArmorWindV2Mesh sash(int d) {
        B b = new B();
        float[][] rings = {{8.4f, 4.78f, 2.92f, 0f}, {9.9f, 4.84f, 2.97f, 0f}};
        grid(b, d == 0 ? 12 : 8, 1, true, loft(rings, 3.2f, 0f), coat(ModeArmorWindV2Rules.R_SASH), false, false, 1f, null, null, 0, 0, 1, ZERO, ZERO);
        return new ModeArmorWindV2Mesh(b);
    }

    /** The gold buckle on the front of the sash: a small box (front, two sides, top). */
    private static ModeArmorWindV2Mesh buckle() {
        B b = new B();
        float[] r = coat(ModeArmorWindV2Rules.R_BUCKLE);
        float x0 = -1.0f, x1 = 1.0f, y0 = 8.3f, y1 = 10.0f, zf = -3.32f, zb = -2.85f;
        box(b, r, x0, y0, zf, x1, y1, zb);
        return new ModeArmorWindV2Mesh(b);
    }

    /** A box with the whole region on every visible face (no back face: the buckle sits on the sash). */
    private static void box(B b, float[] r, float x0, float y0, float z0, float x1, float y1, float z1) {
        float[][] q = {
                {x0, y1, z0, x1, y1, z0, x1, y0, z0, x0, y0, z0, 0, 0, -1},        // front
                {x0, y1, z1, x0, y1, z0, x0, y0, z0, x0, y0, z1, -1, 0, 0},        // left
                {x1, y1, z0, x1, y1, z1, x1, y0, z1, x1, y0, z0, 1, 0, 0},         // right
                {x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0, -1, 0}};       // top
        float[] us = {r[0], r[2], r[2], r[0]}, vs = {r[3], r[3], r[1], r[1]};
        for (float[] f : q) for (int k = 0; k < 4; k++) b.v(f[k * 3], f[k * 3 + 1], f[k * 3 + 2], us[k], vs[k], f[12], f[13], f[14], 0f, 0f);
    }

    /** The two cords of the sash knotted at the left hip, hanging and swaying. */
    private static ModeArmorWindV2Mesh cords(int d) {
        B b = new B();
        float[] r = coat(ModeArmorWindV2Rules.R_CORD);
        Fn w = (a, bb) -> bb * bb, p = (a, bb) -> bb * 1.2f;
        float[][] ends = {{3.9f, 15.6f, -3.55f}, {4.55f, 13.4f, -3.25f}};
        for (int i = 0; i < 2; i++) {
            final float[] e = ends[i];
            final float x0 = 3.3f + i * 0.5f;
            Surf s = (a, bb, o) -> {
                float t = bb;
                o[0] = lerp(x0, e[0], t) + (a - 0.5f) * 0.9f * (1f - 0.3f * t);
                o[1] = lerp(9.1f, e[1], t);
                o[2] = lerp(-2.85f, e[2], t) - 0.15f * (float) Math.sin(Math.PI * t);
            };
            grid(b, 1, d == 0 ? 3 : 2, false, s, r, i == 1, false, 1f, null, FRONT, 0, 0, 0, w, p);
        }
        return new ModeArmorWindV2Mesh(b);
    }

    /** The tassel at the end of the longer cord: a four sided pyramid of gold. */
    private static ModeArmorWindV2Mesh tassel() {
        B b = new B();
        float[] r = coat(ModeArmorWindV2Rules.R_TASSEL);
        pyramid(b, 3.9f, 15.4f, -3.55f, 0.75f, 1f, 3.2f, r, 1f, 2.2f);
        return new ModeArmorWindV2Mesh(b);
    }

    /** A four sided pyramid: a square base of half size {@code r} at (x, y, z), the apex {@code len} below (positive y) it. dir -1 = apex up (negative y). */
    private static void pyramid(B b, float x, float y, float z, float r, float dir, float len, float[] reg, float w, float p) {
        float ay = y + dir * len;
        float[][] base = {{x - r, z - r}, {x + r, z - r}, {x + r, z + r}, {x - r, z + r}};
        for (int i = 0; i < 4; i++) {
            float[] q0 = base[i], q1 = base[(i + 1) % 4];
            float[] n = norm((q0[0] + q1[0]) * 0.5f - x, dir * 0.6f, (q0[1] + q1[1]) * 0.5f - z);
            b.v(q0[0], y, q0[1], reg[0], reg[3], n[0], n[1], n[2], w, p);
            b.v(q1[0], y, q1[1], reg[2], reg[3], n[0], n[1], n[2], w, p);
            b.v(x, ay, z, (reg[0] + reg[2]) * 0.5f, reg[1], n[0], n[1], n[2], w, p);
            b.v(x, ay, z, (reg[0] + reg[2]) * 0.5f, reg[1], n[0], n[1], n[2], w, p);
        }
    }

    private static ModeArmorWindV2Mesh hemFur() {
        B b = new B();
        float[][] rings = {{11.45f, 5.0f, 3.08f, 0f}, {12.95f, 5.28f, 3.26f, 0f}};
        grid(b, 12, 1, true, loft(rings, 3.2f, 0f), fur(ModeArmorWindV2Rules.R_FUR_OUTER), false, true, 1f, null, null, 0, 0, 1, ZERO, ZERO);
        return new ModeArmorWindV2Mesh(b);
    }

    /** The coat tails: two long ones behind (split in the middle), two short panels in front, flared at the hem; they sway and flare when the wearer runs. */
    private static ModeArmorWindV2Mesh tailsBack(int d) {
        B b = new B();
        float[] r = coat(ModeArmorWindV2Rules.R_TAIL_BACK);
        for (int side = 0; side < 2; side++) {                                                               // side 0 = the left (+x) tail, 1 = the right (-x)
            final float sg = side == 0 ? 1f : -1f;
            Surf s = (a, bb, o) -> {
                float xt = lerp(0.45f, 5.0f, a), zt = 3.1f;
                float xb = lerp(0.8f, 5.55f, a), zb = 3.75f + 0.25f * a, yb = lerp(23.5f, 21.7f, a);
                o[0] = sg * lerp(xt, xb, bb);
                o[1] = lerp(12.0f, yb, bb);
                o[2] = lerp(zt, zb, bb) + 0.45f * (float) Math.sin(a * Math.PI * 3f) * bb;
            };
            Fn w = (a, bb) -> (float) Math.pow(bb, 1.4), p = (a, bb) -> a * 0.5f + bb * 1.3f + (sg < 0 ? 1.7f : 0f);
            grid(b, d == 0 ? 3 : 2, d == 0 ? 5 : 3, false, s, r, sg < 0, false, 1f, null, BACK, 0, 0, 0, w, p);
        }
        return new ModeArmorWindV2Mesh(b);
    }

    private static ModeArmorWindV2Mesh tailsFront() {
        B b = new B();
        float[] r = coat(ModeArmorWindV2Rules.R_TAIL_FRONT);
        for (int side = 0; side < 2; side++) {
            final float sg = side == 0 ? 1f : -1f;
            Surf s = (a, bb, o) -> {
                float xt = lerp(0.7f, 4.4f, a), zt = -3.05f;
                float xb = lerp(1.0f, 4.9f, a), zb = -3.65f - 0.15f * a, yb = lerp(20.6f, 19.4f, a);
                o[0] = sg * lerp(xt, xb, bb);
                o[1] = lerp(12.0f, yb, bb);
                o[2] = lerp(zt, zb, bb) - 0.3f * (float) Math.sin(a * Math.PI * 2f) * bb;
            };
            Fn w = (a, bb) -> (float) Math.pow(bb, 1.4), p = (a, bb) -> a * 0.5f + bb * 1.3f + (sg < 0 ? 2.4f : 0.9f);
            grid(b, 2, 4, false, s, r, sg < 0, false, 1f, null, FRONT, 0, 0, 0, w, p);
        }
        return new ModeArmorWindV2Mesh(b);
    }

    /**
     * The standing fur collar: an outer wall that flares outward at the top, a flat lip on top, an inner wall. The rim is high at the back (it stands behind the head), at the shoulder at the sides and
     * low at the chin in front; the top texels of the outer wall are tufts (alpha cut).
     */
    private static ModeArmorWindV2Mesh collar(int d) {
        B b = new B();
        int n = d == 0 ? 12 : 8;
        float[] outer = fur(ModeArmorWindV2Rules.R_FUR_OUTER), lip = fur(ModeArmorWindV2Rules.R_FUR_LIP), inner = fur(ModeArmorWindV2Rules.R_FUR_INNER);
        Surf wall = (a, bb, o) -> collarPoint(a, bb, 0f, o);
        grid(b, n, d == 0 ? 2 : 1, true, wall, outer, false, false, 1f, null, null, 0, 0, 1, ZERO, ZERO);
        Surf top = (a, bb, o) -> collarPoint(a, 0f, bb, o);                                                  // bb: 0 outer rim -> 1 inner rim
        grid(b, n, 1, true, top, lip, false, false, 1f, null, UP, 0, 0, 0, ZERO, ZERO);
        Surf in = (a, bb, o) -> collarPoint(a, bb, 1f, o);
        grid(b, n, 1, true, in, inner, false, false, 1f, null, null, 0, 0, -1, ZERO, ZERO);
        return new ModeArmorWindV2Mesh(b);
    }

    /** A point of the collar: {@code a} the angle (0 = back), {@code down} 0..1 from the rim to the bottom, {@code in} 0 on the outer surface, 1 on the inner one (the top lip runs between them at down = 0). */
    private static void collarPoint(float a, float down, float in, float[] o) {
        double phi = Math.PI * 2 * a;
        float psi = (float) Math.cos(phi);
        float yTop = -2.6f - 2.8f * psi, yBot = 2.9f;
        float f = (float) Math.pow(1f - down, 1.5f);
        float rx = 5.0f + 0.55f * f, rz = 3.55f + 0.6f * f, cz = 0.9f * f * Math.max(0f, psi);
        float thick = 1.45f * in;
        float y = yTop + (yBot - yTop) * down + 0.3f * in * (1f - down);
        o[0] = (rx - thick) * sp((float) Math.sin(phi), 2.6f);
        o[1] = y;
        o[2] = cz + (rz - thick) * sp((float) Math.cos(phi), 2.6f);
    }

    // ------------------------------------------------------------------ ARMS (the left arm, centre x = +1; the right one is the mirror)
    private static final float[][] SLEEVE_RINGS = {{-1.6f, 2.6f, 2.6f, 0f}, {3.0f, 2.52f, 2.5f, 0f}, {7.0f, 2.38f, 2.36f, 0f}};

    private static ModeArmorWindV2Mesh sleeve(int d) {
        B b = new B();
        float[][] rings = d == 0 ? SLEEVE_RINGS : pick(SLEEVE_RINGS, 0, 2);
        grid(b, 8, rings.length - 1, true, loft(rings, 2.4f, 1f), coat(ModeArmorWindV2Rules.R_SLEEVE), false, false, 1f, rowVofY(rings), null, 1f, 0, 1, ZERO, ZERO);
        return new ModeArmorWindV2Mesh(b);
    }

    /** The fur cuff: an outer wall flaring a little toward the hand, and the upper lip. */
    private static ModeArmorWindV2Mesh cuff(int d) {
        B b = new B();
        float[][] rings = {{6.5f, 2.62f, 2.6f, 0f}, {9.0f, 2.95f, 2.92f, 0f}};
        int n = d == 0 ? 8 : 6;
        grid(b, n, 1, true, loft(rings, 2.4f, 1f), fur(ModeArmorWindV2Rules.R_FUR_OUTER), false, false, 0.5f, null, null, 1f, 0, 1, ZERO, ZERO);
        if (d == 0) {
            Surf top = (a, bb, o) -> {
                double phi = Math.PI * 2 * a;
                float r = lerp(2.62f, 2.0f, bb);
                o[0] = 1f + r * sp((float) Math.sin(phi), 2.4f);
                o[1] = 6.5f + 0.1f * bb;
                o[2] = r * sp((float) Math.cos(phi), 2.4f);
            };
            grid(b, n, 1, true, top, fur(ModeArmorWindV2Rules.R_FUR_LIP), false, false, 0.5f, null, UP, 0, 0, 0, ZERO, ZERO);
        }
        return new ModeArmorWindV2Mesh(b);
    }

    /** The shoulder mantle: a dome over the shoulder (the pauldron of the coat). */
    private static ModeArmorWindV2Mesh mantle(int d) {
        B b = new B();
        float[][] rings = {{0.9f, 3.6f, 3.3f, 0f}, {-1.2f, 3.4f, 3.1f, 0f}, {-2.8f, 1.9f, 1.8f, 0f}};
        float[][] use = d == 0 ? rings : pick(rings, 0, 2);
        grid(b, d == 0 ? 8 : 6, use.length - 1, true, loft(use, 2.4f, 1.15f), coat(ModeArmorWindV2Rules.R_MANTLE), false, false, 1f, null, null, 1.15f, 0, 1, ZERO, ZERO);
        return new ModeArmorWindV2Mesh(b);
    }

    private static ModeArmorWindV2Mesh mantleTrim() {
        B b = new B();
        float[][] rings = {{0.15f, 3.7f, 3.4f, 0f}, {2.0f, 3.95f, 3.65f, 0f}};
        grid(b, 8, 1, true, loft(rings, 2.4f, 1.15f), fur(ModeArmorWindV2Rules.R_FUR_OUTER), false, false, 0.6f, null, null, 1.15f, 0, 1, ZERO, ZERO);
        return new ModeArmorWindV2Mesh(b);
    }

    // ------------------------------------------------------------------ LEGS (centred on x = 0)
    private static ModeArmorWindV2Mesh trouser(int d) {
        B b = new B();
        float[][] rings = {{0.2f, 2.5f, 2.46f, 0f}, {7.0f, 2.32f, 2.32f, 0f}};
        grid(b, d == 0 ? 8 : 6, 1, true, loft(rings, 2.4f, 0f), coat(ModeArmorWindV2Rules.R_TROUSER), false, false, 1f, null, null, 0, 0, 1, ZERO, ZERO);
        return new ModeArmorWindV2Mesh(b);
    }

    private static ModeArmorWindV2Mesh boot(int d) {
        B b = new B();
        float[][] rings = {{6.9f, 2.4f, 2.4f, 0f}, {12.05f, 2.5f, 2.62f, -0.12f}};
        grid(b, d == 0 ? 8 : 6, 1, true, loft(rings, 2.4f, 0f), coat(ModeArmorWindV2Rules.R_BOOT), false, false, 1f, null, null, 0, 0, 1, ZERO, ZERO);
        return new ModeArmorWindV2Mesh(b);
    }

    private static ModeArmorWindV2Mesh bootFur() {
        B b = new B();
        float[][] rings = {{5.9f, 2.66f, 2.66f, 0f}, {7.7f, 2.95f, 2.95f, 0f}};
        grid(b, 8, 1, true, loft(rings, 2.4f, 0f), fur(ModeArmorWindV2Rules.R_FUR_OUTER), false, false, 0.5f, null, null, 0, 0, 1, ZERO, ZERO);
        Surf top = (a, bb, o) -> {
            double phi = Math.PI * 2 * a;
            float r = lerp(2.66f, 2.0f, bb);
            o[0] = r * sp((float) Math.sin(phi), 2.4f);
            o[1] = 5.9f + 0.1f * bb;
            o[2] = r * sp((float) Math.cos(phi), 2.4f);
        };
        grid(b, 8, 1, true, top, fur(ModeArmorWindV2Rules.R_FUR_LIP), false, false, 0.5f, null, UP, 0, 0, 0, ZERO, ZERO);
        return new ModeArmorWindV2Mesh(b);
    }

    // ------------------------------------------------------------------ HEAD: glowing eyes and a hair tint
    private static ModeArmorWindV2Mesh eyes() {
        B b = new B();
        float[] r = fx(ModeArmorWindV2Rules.R_EYE);
        for (float x0 : new float[]{-2.9f, 1.3f}) quadXY(b, r, x0, -4.3f, x0 + 1.6f, -3.3f, -4.06f, 0, 0, -1);
        return new ModeArmorWindV2Mesh(b);
    }

    private static void quadXY(B b, float[] r, float x0, float y0, float x1, float y1, float z, float nx, float ny, float nz) {
        b.v(x0, y1, z, r[0], r[3], nx, ny, nz, 0, 0); b.v(x1, y1, z, r[2], r[3], nx, ny, nz, 0, 0);
        b.v(x1, y0, z, r[2], r[1], nx, ny, nz, 0, 0); b.v(x0, y0, z, r[0], r[1], nx, ny, nz, 0, 0);
    }

    private static ModeArmorWindV2Mesh hair() {
        B b = new B();
        float[] r = fx(ModeArmorWindV2Rules.R_HAIR);
        float h = 4.14f, top = -8.14f;
        quadXY(b, r, -h, top, h, -6.0f, -h, 0, 0, -1);                                                       // the fringe
        quadXY(b, r, -h, top, h, -1.6f, h, 0, 0, 1);                                                         // the back of the head
        // the top
        b.v(-h, top, -h, r[0], r[3], 0, -1, 0, 0, 0); b.v(h, top, -h, r[2], r[3], 0, -1, 0, 0, 0); b.v(h, top, h, r[2], r[1], 0, -1, 0, 0, 0); b.v(-h, top, h, r[0], r[1], 0, -1, 0, 0, 0);
        // the sides
        for (float x : new float[]{-h, h}) {
            float s = x < 0 ? -1 : 1;
            b.v(x, top, -h, r[0], r[1], s, 0, 0, 0, 0); b.v(x, top, h, r[2], r[1], s, 0, 0, 0, 0); b.v(x, -5.6f, h, r[2], r[3], s, 0, 0, 0, 0); b.v(x, -5.6f, -h, r[0], r[3], s, 0, 0, 0, 0);
        }
        return new ModeArmorWindV2Mesh(b);
    }

    // ------------------------------------------------------------------ THE CROWN (the frame of the ring: origin at the ring's centre, y down)
    private static final float CROWN_R = 2.9f;
    private static final int CROWN_BLADES = 10;
    private static final float TALL_SHAFT = 3.4f, SHORT_SHAFT = 2.4f, HEAD_LEN = 1.5f;

    /** Height of blade {@code i} from the ring to its tip (shaft plus the arrow head). */
    public static float bladeHeight(int i) { return (i % 2 == 0 ? TALL_SHAFT : SHORT_SHAFT) + HEAD_LEN; }

    public static int crownBlades() { return CROWN_BLADES; }

    public static float crownRadius() { return CROWN_R; }

    private static ModeArmorWindV2Mesh crown(int d) {
        B b = new B();
        int n = d == 0 ? 16 : 8;
        float[] ring = fx(ModeArmorWindV2Rules.R_RING), top = fx(ModeArmorWindV2Rules.R_RING_TOP);
        float[][] rings = {{0.4f, CROWN_R + 0.1f, CROWN_R + 0.1f, 0f}, {-0.4f, CROWN_R + 0.1f, CROWN_R + 0.1f, 0f}};
        grid(b, n, 1, true, loft(rings, 2f, 0f), ring, false, false, 1f, null, null, 0, 0, 1, ZERO, ZERO);
        if (d == 0) {
            Surf lip = (a, bb, o) -> {
                double phi = Math.PI * 2 * a;
                float r = lerp(CROWN_R + 0.12f, CROWN_R - 0.28f, bb);
                o[0] = r * (float) Math.sin(phi); o[1] = -0.5f; o[2] = r * (float) Math.cos(phi);
            };
            grid(b, n, 1, true, lip, top, false, false, 1f, null, UP, 0, 0, 0, ZERO, ZERO);
        }
        if (d == 2) return new ModeArmorWindV2Mesh(b);
        float[] blade = fx(ModeArmorWindV2Rules.R_BLADE), arrow = fx(ModeArmorWindV2Rules.R_ARROW), flat = fx(ModeArmorWindV2Rules.R_BLADE_FLAT);
        int count = d == 0 ? CROWN_BLADES : 6;
        for (int i = 0; i < count; i++) {
            double th = Math.PI * 2 * i / count;
            float cx = CROWN_R * (float) Math.sin(th), cz = CROWN_R * (float) Math.cos(th);
            float shaft = (i % 2 == 0 ? TALL_SHAFT : SHORT_SHAFT);
            if (d == 0) {
                prism(b, cx, -0.4f, cz, 0.22f, shaft, blade, (float) th);
                pyramid(b, cx, -0.4f - shaft, cz, 0.5f, -1f, HEAD_LEN, arrow, 0f, 0f);
            } else {                                                                                         // mid: two crossed flat cards with the whole arrow painted on
                float h = shaft + HEAD_LEN, hw = 0.85f;
                float sx = (float) Math.cos(th), sz = -(float) Math.sin(th);
                for (int k = 0; k < 2; k++) {
                    float dx = k == 0 ? sx : (float) Math.sin(th), dz = k == 0 ? sz : (float) Math.cos(th);
                    b.v(cx - dx * hw, -0.5f, cz - dz * hw, flat[0], flat[3], dz, 0, -dx, 0, 0);
                    b.v(cx + dx * hw, -0.5f, cz + dz * hw, flat[2], flat[3], dz, 0, -dx, 0, 0);
                    b.v(cx + dx * hw, -0.5f - h, cz + dz * hw, flat[2], flat[1], dz, 0, -dx, 0, 0);
                    b.v(cx - dx * hw, -0.5f - h, cz - dz * hw, flat[0], flat[1], dz, 0, -dx, 0, 0);
                }
            }
        }
        return new ModeArmorWindV2Mesh(b);
    }

    /** A square prism standing up (negative y) from (x, y, z), turned about the vertical by {@code yaw}. */
    private static void prism(B b, float x, float y, float z, float hw, float h, float[] r, float yaw) {
        float c = (float) Math.cos(yaw), s = (float) Math.sin(yaw);
        float[][] cs = {{-hw, -hw}, {hw, -hw}, {hw, hw}, {-hw, hw}};
        for (int i = 0; i < 4; i++) {
            float[] a = cs[i], e = cs[(i + 1) % 4];
            float ax = x + a[0] * c + a[1] * s, az = z - a[0] * s + a[1] * c, ex = x + e[0] * c + e[1] * s, ez = z - e[0] * s + e[1] * c;
            float nx = (ax + ex) * 0.5f - x, nz = (az + ez) * 0.5f - z;
            float[] n = norm(nx, 0, nz);
            b.v(ax, y, az, r[0], r[3], n[0], 0, n[2], 0, 0); b.v(ex, y, ez, r[2], r[3], n[0], 0, n[2], 0, 0);
            b.v(ex, y - h, ez, r[2], r[1], n[0], 0, n[2], 0, 0); b.v(ax, y - h, az, r[0], r[1], n[0], 0, n[2], 0, 0);
        }
    }

    private static ModeArmorWindV2Mesh halo(int d) {
        B b = new B();
        float[] r = fx(ModeArmorWindV2Rules.R_HALO);
        float h = d == 0 ? 6.4f : 5.6f, y = 0.2f;
        b.v(-h, y, -h, r[0], r[3], 0, -1, 0, 0, 0); b.v(h, y, -h, r[2], r[3], 0, -1, 0, 0, 0); b.v(h, y, h, r[2], r[1], 0, -1, 0, 0, 0); b.v(-h, y, h, r[0], r[1], 0, -1, 0, 0, 0);
        return new ModeArmorWindV2Mesh(b);
    }

    // ------------------------------------------------------------------ THE WINGS (the frame of the root of the left wing: +x out, up is -y, +z back)
    private static final float[] N0 = norm(0.20f, 0.12f, 0.97f);

    /** The shape of one leaf wing: the spine from the root, bending from the direction d0 to d1, with a leading edge (the upper, convex one) and a trailing edge. */
    private record Leaf(float len, float[] d0, float[] d1, float pw, float[] root, float lead, float trail) {}

    private static final Leaf BIG = new Leaf(31f, norm(0.80f, -0.58f, 0.20f), norm(0.95f, -0.16f, 0.28f), 1.3f, new float[]{0.6f, 0, 0}, 5.2f, 7.8f);
    private static final Leaf SMALL = new Leaf(19f, norm(0.88f, -0.14f, 0.45f), norm(0.98f, 0.14f, 0.20f), 1.2f, new float[]{0.4f, 3.6f, 0.8f}, 3.8f, 3.6f);
    private static final int SPINE_STEPS = 96;

    /** The point of a leaf at s (0 root .. 1 tip) and a (0 leading edge .. 1 trailing edge; outside 0..1 is beyond the edge); fills {@code o} with x, y, z and {@code axes} with the spine direction and the chord. */
    private static void leafPoint(Leaf lf, float s, float a, float[] o, float[] axes) {
        float px = lf.root[0], py = lf.root[1], pz = lf.root[2];
        float target = s * SPINE_STEPS;
        float[] dd = lf.d0.clone();
        for (int i = 0; i < SPINE_STEPS; i++) {
            float st = (i + 0.5f) / SPINE_STEPS;
            float t = (float) Math.pow(st, lf.pw);
            dd = norm(lerp(lf.d0[0], lf.d1[0], t), lerp(lf.d0[1], lf.d1[1], t), lerp(lf.d0[2], lf.d1[2], t));
            float step = Math.min(1f, target - i);
            if (step <= 0) break;
            px += dd[0] * lf.len / SPINE_STEPS * step; py += dd[1] * lf.len / SPINE_STEPS * step; pz += dd[2] * lf.len / SPINE_STEPS * step;
        }
        float dn = dd[0] * N0[0] + dd[1] * N0[1] + dd[2] * N0[2];
        float[] ds = norm(dd[0] - N0[0] * dn, dd[1] - N0[1] * dn, dd[2] - N0[2] * dn);
        float[] c = norm(N0[1] * ds[2] - N0[2] * ds[1], N0[2] * ds[0] - N0[0] * ds[2], N0[0] * ds[1] - N0[1] * ds[0]);
        float lead = lf.lead * prof(s, 0.72f), trail = lf.trail * prof(s, 0.95f);
        float off = a < 0 ? -lead + a * lead : a > 1 ? trail * a : lerp(-lead, trail, a);
        float ac = Math.max(0f, Math.min(1f, a));
        float bend = 2.6f * s * s - 1.1f * (float) Math.sin(Math.PI * s) * (1f - (2f * ac - 1f) * (2f * ac - 1f));
        o[0] = px + c[0] * off + N0[0] * bend;
        o[1] = py + c[1] * off + N0[1] * bend;
        o[2] = pz + c[2] * off + N0[2] * bend;
        if (axes != null) { axes[0] = ds[0]; axes[1] = ds[1]; axes[2] = ds[2]; axes[3] = c[0]; axes[4] = c[1]; axes[5] = c[2]; }
    }

    private static float prof(float s, float p) {
        return (float) Math.pow(Math.sin(Math.PI * Math.pow(s, p)), 0.85) + 0.07f * (float) Math.pow(1f - s, 3);
    }

    private static ModeArmorWindV2Mesh wing(Leaf lf, int cols, int rows) {
        B b = new B();
        Surf s = (a, bb, o) -> leafPoint(lf, bb, a, o, null);
        Fn w = (a, bb) -> (float) Math.pow(bb, 1.3), p = (a, bb) -> bb * 2f;
        grid(b, cols, rows, false, s, wingReg(), false, false, 1f, null, N0, 0, 0, 0, w, p);
        return new ModeArmorWindV2Mesh(b);
    }

    /** The cross ornaments: a big one just beyond the leading edge near the tip of the big leaf, a small one at the root on the trailing side. */
    private static ModeArmorWindV2Mesh wingOrn() {
        B b = new B();
        float[] r = fx(ModeArmorWindV2Rules.R_CROSS);
        float[][] defs = {{0.80f, -0.10f, 3.9f, 3.4f}, {0.12f, 1.25f, 2.4f, 2.1f}};
        for (float[] df : defs) {
            float[] o = new float[3], ax = new float[6];
            leafPoint(BIG, df[0], df[1], o, ax);
            float hl = df[2], hw = df[3];
            float wgt = (float) Math.pow(df[0], 1.3), phs = df[0] * 2f;
            float[][] c = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};                                              // along the spine (down toward the root = -1), across
            for (int k = 0; k < 4; k++) {
                float al = c[k][0] * hl, ac = c[k][1] * hw;
                b.v(o[0] + ax[0] * al + ax[3] * ac, o[1] + ax[1] * al + ax[4] * ac, o[2] + ax[2] * al + ax[5] * ac, c[k][1] < 0 ? r[0] : r[2], c[k][0] < 0 ? r[3] : r[1], N0[0], N0[1], N0[2], wgt, phs);
            }
        }
        return new ModeArmorWindV2Mesh(b);
    }

    // ------------------------------------------------------------------ THE RIBBONS of wind
    private static ModeArmorWindV2Mesh ribbonCuff() {
        B b = new B();
        float[] r = fx(ModeArmorWindV2Rules.R_RIBBON);
        Surf s = (a, bb, o) -> {
            float cx = 1f + 0.4f * bb, wd = 0.55f * (1f - 0.4f * bb);
            o[0] = cx + (a - 0.5f) * 2f * wd * 0.8f;
            o[1] = 9.2f + 6.8f * bb;
            o[2] = 0.7f + 4.6f * (float) Math.pow(bb, 1.5) + (a - 0.5f) * 2f * wd * 0.6f;
        };
        Fn w = (a, bb) -> bb, p = (a, bb) -> bb * 1.6f;
        grid(b, 1, 5, false, s, r, false, false, 1f, null, FRONT, 0, 0, 0, w, p);
        return new ModeArmorWindV2Mesh(b);
    }

    private static ModeArmorWindV2Mesh ribbonTail() {
        B b = new B();
        float[] r = fx(ModeArmorWindV2Rules.R_RIBBON);
        Surf s = (a, bb, o) -> {
            float wd = 0.6f * (1f - 0.4f * bb);
            o[0] = 2.8f + 0.9f * bb + (a - 0.5f) * 2f * wd;
            o[1] = 22.9f + 6.4f * bb;
            o[2] = 3.7f + 3.4f * (float) Math.pow(bb, 1.4);
        };
        Fn w = (a, bb) -> bb, p = (a, bb) -> bb * 1.6f + 0.8f;
        grid(b, 1, 5, false, s, r, false, false, 1f, null, BACK, 0, 0, 0, w, p);
        return new ModeArmorWindV2Mesh(b);
    }

    // ------------------------------------------------------------------ the registry, filled once
    static {
        MESHES[Piece.TORSO.ordinal()][0] = torso(0);
        MESHES[Piece.TORSO.ordinal()][1] = torso(1);
        MESHES[Piece.SASH.ordinal()][0] = sash(0);
        MESHES[Piece.SASH.ordinal()][1] = sash(1);
        MESHES[Piece.BUCKLE.ordinal()][0] = buckle();
        MESHES[Piece.CORDS.ordinal()][0] = cords(0);
        MESHES[Piece.TASSEL.ordinal()][0] = tassel();
        MESHES[Piece.HEM_FUR.ordinal()][0] = hemFur();
        MESHES[Piece.TAILS_BACK.ordinal()][0] = tailsBack(0);
        MESHES[Piece.TAILS_BACK.ordinal()][1] = tailsBack(1);
        MESHES[Piece.TAILS_FRONT.ordinal()][0] = tailsFront();
        MESHES[Piece.COLLAR.ordinal()][0] = collar(0);
        MESHES[Piece.COLLAR.ordinal()][1] = collar(1);
        MESHES[Piece.SLEEVE.ordinal()][0] = sleeve(0);
        MESHES[Piece.SLEEVE.ordinal()][1] = sleeve(1);
        MESHES[Piece.CUFF.ordinal()][0] = cuff(0);
        MESHES[Piece.CUFF.ordinal()][1] = cuff(1);
        MESHES[Piece.MANTLE.ordinal()][0] = mantle(0);
        MESHES[Piece.MANTLE.ordinal()][1] = mantle(1);
        MESHES[Piece.MANTLE_TRIM.ordinal()][0] = mantleTrim();
        MESHES[Piece.TROUSER.ordinal()][0] = trouser(0);
        MESHES[Piece.TROUSER.ordinal()][1] = trouser(1);
        MESHES[Piece.BOOT.ordinal()][0] = boot(0);
        MESHES[Piece.BOOT.ordinal()][1] = boot(1);
        MESHES[Piece.BOOT_FUR.ordinal()][0] = bootFur();
        MESHES[Piece.EYES.ordinal()][0] = eyes();
        MESHES[Piece.HAIR.ordinal()][0] = hair();
        MESHES[Piece.CROWN.ordinal()][0] = crown(0);
        MESHES[Piece.CROWN.ordinal()][1] = crown(1);
        MESHES[Piece.CROWN.ordinal()][2] = crown(2);
        MESHES[Piece.HALO.ordinal()][0] = halo(0);
        MESHES[Piece.HALO.ordinal()][1] = halo(1);
        MESHES[Piece.HALO.ordinal()][2] = halo(2);
        MESHES[Piece.WING_BIG.ordinal()][0] = wing(BIG, 4, 8);
        MESHES[Piece.WING_BIG.ordinal()][1] = wing(BIG, 2, 5);
        MESHES[Piece.WING_BIG.ordinal()][2] = wing(BIG, 1, 3);
        MESHES[Piece.WING_SMALL.ordinal()][0] = wing(SMALL, 3, 4);
        MESHES[Piece.WING_SMALL.ordinal()][1] = wing(SMALL, 2, 3);
        MESHES[Piece.WING_ORN.ordinal()][0] = wingOrn();
        MESHES[Piece.RIBBON_CUFF.ordinal()][0] = ribbonCuff();
        MESHES[Piece.RIBBON_TAIL.ordinal()][0] = ribbonTail();
    }
}
