package com.newuniverse.nusmp.vfx;

/**
 * The toon shading of the water hybrid's 3D cores, on the CPU, once per quad per frame, with no shader and no allocation: a mesh piece is placed in camera-relative space (a basis from a direction and a hint,
 * a scale per axis, a position), every quad gets ONE colour from its geometric normal and goes into the {@link WaterBcMeshBuffer} pass it belongs to. The look is the one of the preview
 * (water_bc.md section 2): three tones by the light (from the upper left, on the viewer's side), one hard white specular, a pale rim where the quad is edge on, emissive quads additive, glass hulls with a
 * fresnel alpha (clear in the middle, denser at the grazing edge). Lighting is two sided (the normal is flipped to face the camera), so back faces and front faces of a blade shade alike and nothing
 * depends on the winding of the file or on whether culling is on.
 */
public final class WaterBcToon {
    private WaterBcToon() {}

    public static final int PAL_WATER = 0, PAL_FOAM = 1, PAL_DEEP = 2, PAL_GLASS = 3, PAL_TONGUE = 4, PAL_CORE = 5;

    /** [palette][tone 0 dark, 1 mid, 2 light][r g b] */
    private static final int[][][] PAL = {
            {{30, 84, 164}, {58, 150, 236}, {140, 220, 255}},     // water
            {{150, 190, 225}, {214, 236, 252}, {255, 255, 255}},  // foam
            {{18, 58, 128}, {30, 92, 178}, {84, 160, 232}},       // deep
            {{118, 188, 238}, {168, 222, 252}, {236, 249, 255}},  // glass
            {{64, 134, 212}, {118, 194, 246}, {206, 237, 255}},   // tongue
            {{14, 46, 104}, {26, 84, 166}, {84, 162, 232}}};      // core (deeper than the glass on top of it)
    private static final int[] RIM = {210, 244, 255}, GLOW = {150, 235, 255};
    /** luma of the lightest water tone: the tinted palettes scale the tint colour by luma / this */
    private static final float LIGHT_LUMA = 199.9f;

    /** The camera as the shading needs it: right and up (as the layer's context has them), the forward vector and the light derived from them. Set once per effect. */
    public static final class View {
        public float rx, ry, rz, ux, uy, uz, fx, fy, fz, sx, sy, sz;

        /** Sets the camera right and up vectors (unit length); forward is up x right (the camera looks along -(right x up)); the light comes from the upper left and a little from the viewer. */
        public void set(float rx, float ry, float rz, float ux, float uy, float uz) {
            this.rx = rx; this.ry = ry; this.rz = rz; this.ux = ux; this.uy = uy; this.uz = uz;
            fx = uy * rz - uz * ry;
            fy = uz * rx - ux * rz;
            fz = ux * ry - uy * rx;
            float lx = -0.55f * rx - 0.3f * fx, ly = -0.55f * ry + 0.8f - 0.3f * fy, lz = -0.55f * rz - 0.3f * fz;
            float l = (float) Math.sqrt(lx * lx + ly * ly + lz * lz);
            if (l < 1e-6f) { lx = 0f; ly = 1f; lz = 0f; l = 1f; }
            sx = lx / l; sy = ly / l; sz = lz / l;
        }
    }

    /** One placed piece (the painter reuses one object, nothing is allocated per piece). */
    public static final class Inst {
        public int piece, pal;
        public boolean lo, xHint, fres;
        public float px, py, pz, yx, yy, yz, hx, hy, hz, sx, sy, sz, alpha, foamAlpha;

        public Inst set(int piece, boolean lo, float px, float py, float pz, float yx, float yy, float yz, float hx, float hy, float hz, boolean xHint, float sx, float sy, float sz,
                        float alpha, float foamAlpha, int pal, boolean fres) {
            this.piece = piece; this.lo = lo; this.px = px; this.py = py; this.pz = pz; this.yx = yx; this.yy = yy; this.yz = yz; this.hx = hx; this.hy = hy; this.hz = hz; this.xHint = xHint;
            this.sx = sx; this.sy = sy; this.sz = sz; this.alpha = alpha; this.foamAlpha = foamAlpha; this.pal = pal; this.fres = fres;
            return this;
        }
    }

    private static int clamp255(float v) { return v < 0f ? 0 : v > 255f ? 255 : (int) (v + 0.5f); }

    /**
     * Places the piece, shades its quads and appends them to {@code buf} (solid pass for opaque quads, translucent for glass, glow for the emissive layer). {@code tinted} turns every palette to the colour
     * {@code tint} (0xRRGGBB): the blood and paint pages that reuse the splash and the ring. Returns the number of quads accepted by the buffer. Degenerate quads (a scale of zero) and quads wholly behind the
     * camera are skipped.
     */
    public static int emit(Inst m, View v, WaterBcMeshBuffer buf, boolean tinted, int tint) {
        WaterBcMeshes.Piece pc = WaterBcMeshes.get(m.piece, m.lo);
        if (pc.quads == 0) return 0;
        // ---- the basis: y along the piece; z (or x) from the hint
        float yl = (float) Math.sqrt(m.yx * m.yx + m.yy * m.yy + m.yz * m.yz);
        float Yx, Yy, Yz;
        if (yl < 1e-6f) { Yx = 0f; Yy = 1f; Yz = 0f; } else { Yx = m.yx / yl; Yy = m.yy / yl; Yz = m.yz / yl; }
        float hd = m.hx * Yx + m.hy * Yy + m.hz * Yz;
        float Hx = m.hx - Yx * hd, Hy = m.hy - Yy * hd, Hz = m.hz - Yz * hd;
        float hl = (float) Math.sqrt(Hx * Hx + Hy * Hy + Hz * Hz);
        if (hl < 1e-4f) {
            Hx = 1f - Yx * Yx; Hy = -Yx * Yy; Hz = -Yx * Yz;
            hl = (float) Math.sqrt(Hx * Hx + Hy * Hy + Hz * Hz);
            if (hl < 1e-4f) { Hx = 0f; Hy = 0f; Hz = 1f; hl = 1f; }
        }
        Hx /= hl; Hy /= hl; Hz /= hl;
        float Xx, Xy, Xz, Zx, Zy, Zz;
        if (m.xHint) {                                    // the hint is the x axis: z = x cross y
            Xx = Hx; Xy = Hy; Xz = Hz;
            Zx = Xy * Yz - Xz * Yy; Zy = Xz * Yx - Xx * Yz; Zz = Xx * Yy - Xy * Yx;
        } else {                                          // the hint is the z axis: x = y cross z
            Zx = Hx; Zy = Hy; Zz = Hz;
            Xx = Yy * Zz - Yz * Zy; Xy = Yz * Zx - Yx * Zz; Xz = Yx * Zy - Yy * Zx;
        }
        float k = 1f / 16f;
        float axx = Xx * m.sx * k, axy = Xy * m.sx * k, axz = Xz * m.sx * k;
        float ayx = Yx * m.sy * k, ayy = Yy * m.sy * k, ayz = Yz * m.sy * k;
        float azx = Zx * m.sz * k, azy = Zy * m.sz * k, azz = Zz * m.sz * k;

        // ---- palettes (tinted: the tint colour scaled by the tone's luma; foam and the rim stay pale)
        int[][] water = PAL[m.pal], foam = PAL[PAL_FOAM];
        int w0r, w0g, w0b, w1r, w1g, w1b, w2r, w2g, w2b, f0r, f0g, f0b, f1r, f1g, f1b, f2r, f2g, f2b, rimR, rimG, rimB, glR, glG, glB;
        if (!tinted) {
            w0r = water[0][0]; w0g = water[0][1]; w0b = water[0][2]; w1r = water[1][0]; w1g = water[1][1]; w1b = water[1][2]; w2r = water[2][0]; w2g = water[2][1]; w2b = water[2][2];
            f0r = foam[0][0]; f0g = foam[0][1]; f0b = foam[0][2]; f1r = foam[1][0]; f1g = foam[1][1]; f1b = foam[1][2]; f2r = foam[2][0]; f2g = foam[2][1]; f2b = foam[2][2];
            rimR = RIM[0]; rimG = RIM[1]; rimB = RIM[2]; glR = GLOW[0]; glG = GLOW[1]; glB = GLOW[2];
        } else {
            int tr = (tint >> 16) & 255, tg = (tint >> 8) & 255, tb = tint & 255;
            float s0 = Math.min(1.3f, lum(water[0]) / LIGHT_LUMA), s1 = Math.min(1.3f, lum(water[1]) / LIGHT_LUMA), s2 = Math.min(1.3f, lum(water[2]) / LIGHT_LUMA);
            w0r = clamp255(tr * s0); w0g = clamp255(tg * s0); w0b = clamp255(tb * s0);
            w1r = clamp255(tr * s1); w1g = clamp255(tg * s1); w1b = clamp255(tb * s1);
            w2r = clamp255(tr * s2); w2g = clamp255(tg * s2); w2b = clamp255(tb * s2);
            f0r = clamp255(tr * 0.55f + 255f * 0.45f * 0.7f); f0g = clamp255(tg * 0.55f + 255f * 0.45f * 0.7f); f0b = clamp255(tb * 0.55f + 255f * 0.45f * 0.7f);
            f1r = clamp255(tr * 0.35f + 255f * 0.65f); f1g = clamp255(tg * 0.35f + 255f * 0.65f); f1b = clamp255(tb * 0.35f + 255f * 0.65f);
            f2r = f2g = f2b = 255;
            rimR = clamp255(tr * 0.3f + 255f * 0.7f); rimG = clamp255(tg * 0.3f + 255f * 0.7f); rimB = clamp255(tb * 0.3f + 255f * 0.7f);
            glR = clamp255(tr * 0.65f + 255f * 0.35f); glG = clamp255(tg * 0.65f + 255f * 0.35f); glB = clamp255(tb * 0.65f + 255f * 0.35f);
        }

        int accepted = 0;
        float[] pos = pc.pos, tin = pc.tint;
        for (int q = 0; q < pc.quads; q++) {
            int b = q * 12;
            float x0 = m.px + axx * pos[b] + ayx * pos[b + 1] + azx * pos[b + 2];
            float y0 = m.py + axy * pos[b] + ayy * pos[b + 1] + azy * pos[b + 2];
            float z0 = m.pz + axz * pos[b] + ayz * pos[b + 1] + azz * pos[b + 2];
            float x1 = m.px + axx * pos[b + 3] + ayx * pos[b + 4] + azx * pos[b + 5];
            float y1 = m.py + axy * pos[b + 3] + ayy * pos[b + 4] + azy * pos[b + 5];
            float z1 = m.pz + axz * pos[b + 3] + ayz * pos[b + 4] + azz * pos[b + 5];
            float x2 = m.px + axx * pos[b + 6] + ayx * pos[b + 7] + azx * pos[b + 8];
            float y2 = m.py + axy * pos[b + 6] + ayy * pos[b + 7] + azy * pos[b + 8];
            float z2 = m.pz + axz * pos[b + 6] + ayz * pos[b + 7] + azz * pos[b + 8];
            float x3 = m.px + axx * pos[b + 9] + ayx * pos[b + 10] + azx * pos[b + 11];
            float y3 = m.py + axy * pos[b + 9] + ayy * pos[b + 10] + azy * pos[b + 11];
            float z3 = m.pz + axz * pos[b + 9] + ayz * pos[b + 10] + azz * pos[b + 11];
            float cx = (x0 + x1 + x2 + x3) * 0.25f, cy = (y0 + y1 + y2 + y3) * 0.25f, cz = (z0 + z1 + z2 + z3) * 0.25f;
            float d0 = x0 * v.fx + y0 * v.fy + z0 * v.fz, d1 = x1 * v.fx + y1 * v.fy + z1 * v.fz, d2 = x2 * v.fx + y2 * v.fy + z2 * v.fz, d3 = x3 * v.fx + y3 * v.fy + z3 * v.fz;
            if (Math.max(Math.max(d0, d1), Math.max(d2, d3)) < 0.05f) continue;                 // wholly behind the camera
            // geometric normal = diagonal cross product
            float nx = (y2 - y0) * (z3 - z1) - (z2 - z0) * (y3 - y1);
            float ny = (z2 - z0) * (x3 - x1) - (x2 - x0) * (z3 - z1);
            float nz = (x2 - x0) * (y3 - y1) - (y2 - y0) * (x3 - x1);
            float nl = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (nl < 1e-9f) continue;                                                           // a zero-scale quad
            nx /= nl; ny /= nl; nz /= nl;
            float vl = (float) Math.sqrt(cx * cx + cy * cy + cz * cz);
            float vx = vl < 1e-6f ? 0f : -cx / vl, vy = vl < 1e-6f ? 0f : -cy / vl, vz = vl < 1e-6f ? 1f : -cz / vl;
            float ndv = nx * vx + ny * vy + nz * vz;
            if (ndv < 0f) { nx = -nx; ny = -ny; nz = -nz; ndv = -ndv; }                          // two sided lighting
            int layer = pc.layer[q];
            int pass, argb;
            if (layer == 1) {
                pass = WaterBcMeshBuffer.GLOW;
                argb = (205 << 24) | (glR << 16) | (glG << 8) | glB;
            } else {
                float light = 0.5f + 0.5f * (nx * v.sx + ny * v.sy + nz * v.sz);
                int tone = light > 0.86f ? 2 : light > 0.58f ? 1 : 0;
                int br, bg, bb;
                if (layer == 2) {
                    br = tone == 0 ? f0r : tone == 1 ? f1r : f2r; bg = tone == 0 ? f0g : tone == 1 ? f1g : f2g; bb = tone == 0 ? f0b : tone == 1 ? f1b : f2b;
                } else {
                    br = tone == 0 ? w0r : tone == 1 ? w1r : w2r; bg = tone == 0 ? w0g : tone == 1 ? w1g : w2g; bb = tone == 0 ? w0b : tone == 1 ? w1b : w2b;
                }
                float r = br * tin[q * 3], g = bg * tin[q * 3 + 1], bl = bb * tin[q * 3 + 2];
                float hx = v.sx + vx, hy = v.sy + vy, hz = v.sz + vz;
                float hn = (float) Math.sqrt(hx * hx + hy * hy + hz * hz);
                boolean spec = hn > 1e-6f && (nx * hx + ny * hy + nz * hz) / hn > 0.9985f;
                if (spec) { r = r * 0.25f + 255f * 0.75f; g = g * 0.25f + 255f * 0.75f; bl = bl * 0.25f + 255f * 0.75f; }
                else if (ndv < 0.22f) { r = r * 0.6f + rimR * 0.4f; g = g * 0.6f + rimG * 0.4f; bl = bl * 0.6f + rimB * 0.4f; }
                float al = layer == 2 && m.alpha < 0.95f ? m.foamAlpha : m.alpha;
                if (m.fres && layer != 2) {
                    float e = 1f - ndv;
                    al = Math.min(1f, al * (0.30f + 0.70f * e * (float) Math.sqrt(e)) * 1.55f);
                }
                pass = al >= 0.9f ? WaterBcMeshBuffer.SOLID : WaterBcMeshBuffer.TRANSLUCENT;
                int a = pass == WaterBcMeshBuffer.SOLID ? 255 : clamp255(al * 255f);
                argb = (a << 24) | (clamp255(r) << 16) | (clamp255(g) << 8) | clamp255(bl);
            }
            if (buf.add(pass, x0, y0, z0, x1, y1, z1, x2, y2, z2, x3, y3, z3, argb)) accepted++;
        }
        return accepted;
    }

    private static float lum(int[] c) { return c[0] * 0.30f + c[1] * 0.59f + c[2] * 0.11f; }
}
