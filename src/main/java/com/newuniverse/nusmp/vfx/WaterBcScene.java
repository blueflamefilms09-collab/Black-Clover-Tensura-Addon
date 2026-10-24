package com.newuniverse.nusmp.vfx;

/**
 * The water hybrid "BC" as geometry (docs/vfx_remake/water_bc.md, docs/vfx_remake/water_ingame.md), pure and allocation free: for one effect it hands the 3D cores (mesh pieces, placed by a position, a
 * direction, a hint and a scale) and the glass sprites (camera facing quads) to an {@link Out}. The painter ({@code vfx.client.layer.WaterBcLayer}) turns the sprites into vertex-buffer quads and the
 * meshes into toon-shaded {@link WaterBcMeshBuffer} quads; the unit tests and the CI self-test count what comes out without any client class; the headless preview runs the real painter.
 *
 * <p>Everything is in camera-relative space (the camera is the origin; {@link #view} holds the camera's right and up vectors), so a billboard, a strip facing the viewer or a lens toward the camera needs no
 * camera position. The drawing order of the glass is FIXED: the order of the calls below, which is authored back to front (shells behind the core first, then ribbons, mist, bubbles and drops, the lens
 * and the glints last); the vertex buffer keeps the first-use order of its (texture, blend) batches and draws the additive ones last, so no per-frame sort exists and none allocates.
 *
 * <p>Tiers (water_bc.md section 4): {@link WaterBc#LOD0} the full cores, {@link WaterBc#LOD1} the {@code _lo} cores with fewer segments, orbs and bubbles, {@link WaterBc#SPRITE} the flat anime sprites
 * (the "A" look: no mesh at all, 3 to 21 quads) used when the frame ceiling is reached. Seven looks: roar, lance, cradle, nest, burst, splash, ring (the Valkyrie wings are deferred).
 */
public final class WaterBcScene {
    /** Where the scene sends its quads. */
    public interface Out {
        /** One camera facing or ground sprite. {@code q} holds the corners top left, top right, bottom right, bottom left (12 floats, reused, valid during the call); the texture runs {@code u0 -> u1} across and 0 -> 1 down. */
        void sprite(int tex, boolean add, float u0, float u1, int argb, float[] q);

        /** One placed core piece (the object is reused, valid during the call). */
        void mesh(WaterBcToon.Inst m);
    }

    /** The camera (right / up); set by the painter before {@link #build}. */
    public final WaterBcToon.View view = new WaterBcToon.View();

    private Out out;
    private final float[] F = new float[3], T = new float[3];
    private float p, age;
    private int dur;
    private long seed;
    private boolean tinted;
    private int tintRgb;

    // scratch (the scene is used by one thread at a time)
    private final float[] q12 = new float[12];
    private final float[] A = new float[3], B = new float[3], C = new float[3], D3 = new float[3], E = new float[3], G = new float[3];
    private final float[] AX = new float[3], AY = new float[3], CEN = new float[3], SA = new float[3], SB = new float[3], SPH = new float[3];
    private final float[] HP = new float[3], TG = new float[3], TP = new float[3], MID = new float[3], DV = new float[3], ORG = new float[3], PREV = new float[3], CUR = new float[3];
    private final float[] FD = new float[3], FS = new float[3], FU = new float[3];            // the frame of the effect: direction, side, up
    private final float[] XX = new float[3], YY = new float[3], ZZ = new float[3];
    private final float[][] PTS = new float[9][3];
    private final WaterBcToon.Inst mi = new WaterBcToon.Inst();

    private static final float TAU = (float) (Math.PI * 2.0), PI = (float) Math.PI;

    /** Starts an effect: positions are relative to the camera. {@code tinted}: the effect draws in {@code tintRgb} (grey-twin textures and a tinted palette) instead of the water blues. */
    public WaterBcScene begin(Out out, float fx, float fy, float fz, float tx, float ty, float tz, float power, float age, int duration, long seed, boolean tinted, int tintRgb) {
        this.out = out;
        set(F, fx, fy, fz);
        set(T, tx, ty, tz);
        this.p = power;
        this.age = age;
        this.dur = Math.max(1, duration);
        this.seed = seed;
        this.tinted = tinted;
        this.tintRgb = tintRgb & 0xFFFFFF;
        return this;
    }

    /** Builds one look at one tier: LOD0 / LOD1 draw the hybrid, SPRITE the flat anime sprites, SKIP nothing. */
    public void build(int kind, int tier) {
        if (tier == WaterBc.SKIP || kind < 0) return;
        boolean lo = tier == WaterBc.LOD1;
        if (tier == WaterBc.SPRITE) {
            switch (kind) {
                case WaterBc.ROAR -> roarA();
                case WaterBc.LANCE -> lanceA();
                case WaterBc.CRADLE -> cradleA();
                case WaterBc.NEST -> nestA();
                case WaterBc.BURST -> burstA();
                case WaterBc.SPLASH -> splashA();
                case WaterBc.RING -> ringA();
                default -> { }
            }
            return;
        }
        switch (kind) {
            case WaterBc.ROAR -> roar(lo);
            case WaterBc.LANCE -> lance(lo);
            case WaterBc.CRADLE -> cradle(lo);
            case WaterBc.NEST -> nest(lo);
            case WaterBc.BURST -> burst(lo);
            case WaterBc.SPLASH -> splash(lo);
            case WaterBc.RING -> ring(lo);
            default -> { }
        }
    }

    // ================================================================================================ math (all in place)
    private static void set(float[] o, float x, float y, float z) { o[0] = x; o[1] = y; o[2] = z; }

    private static void cp(float[] o, float[] a) { o[0] = a[0]; o[1] = a[1]; o[2] = a[2]; }

    /** o = a + b * k */
    private static void addS(float[] o, float[] a, float[] b, float k) { o[0] = a[0] + b[0] * k; o[1] = a[1] + b[1] * k; o[2] = a[2] + b[2] * k; }

    private static void add(float[] o, float[] a, float[] b) { o[0] = a[0] + b[0]; o[1] = a[1] + b[1]; o[2] = a[2] + b[2]; }

    private static void sub(float[] o, float[] a, float[] b) { o[0] = a[0] - b[0]; o[1] = a[1] - b[1]; o[2] = a[2] - b[2]; }

    private static float dot(float[] a, float[] b) { return a[0] * b[0] + a[1] * b[1] + a[2] * b[2]; }

    private static float len(float[] a) { return (float) Math.sqrt(a[0] * a[0] + a[1] * a[1] + a[2] * a[2]); }

    private static void cross(float[] o, float[] a, float[] b) {
        float x = a[1] * b[2] - a[2] * b[1], y = a[2] * b[0] - a[0] * b[2], z = a[0] * b[1] - a[1] * b[0];
        o[0] = x; o[1] = y; o[2] = z;
    }

    /** normalises in place; a (nearly) zero vector stays as it is */
    private static void nrm(float[] a) {
        float l = len(a);
        if (l > 1e-9f) { a[0] /= l; a[1] /= l; a[2] /= l; }
    }

    private static float clamp(float x) { return x < 0f ? 0f : x > 1f ? 1f : x; }

    private static float easeOut(float t) { float u = 1f - clamp(t); return 1f - u * u * u; }

    private static float easeIn(float t) { float u = clamp(t); return u * u * u; }

    private static float easeBack(float t) {
        float u = clamp(t) - 1f, c1 = 1.70158f;
        return 1f + (c1 + 1f) * u * u * u + c1 * u * u;
    }

    private static float frac(float x) { return x - (float) Math.floor(x); }

    private static float sin(float x) { return (float) Math.sin(x); }

    private static float cos(float x) { return (float) Math.cos(x); }

    /** the cheap hash of the preview and of the old layers' patterns: the same effect looks the same on every client */
    private float hs(int i, int salt) { return hs(seed, i, salt); }

    private static float hs(long seed, int i, int salt) {
        double x = Math.sin(i * 12.9898 + salt * 78.233 + seed * 0.001) * 43758.5453;
        return (float) (x - Math.floor(x));
    }

    // ================================================================================================ colours
    /** white (or the tint colour) with this alpha */
    private int w(float a) { return WaterBc.argb(a, tinted ? tintRgb : 0xFFFFFF); }

    /** a fixed colour with this alpha; a tinted effect uses its own colour instead */
    private int col(int rgb, float a) { return WaterBc.argb(a, tinted ? tintRgb : rgb); }

    // ================================================================================================ sprite primitives
    private void plane(float[] c, float[] ax, float[] ay, float hw, float hh, int tex, int argb, boolean flip, float u0, float u1, boolean add) {
        if ((argb >>> 24) == 0) return;
        float[] q = q12;
        q[0] = c[0] - ax[0] * hw + ay[0] * hh; q[1] = c[1] - ax[1] * hw + ay[1] * hh; q[2] = c[2] - ax[2] * hw + ay[2] * hh;
        q[3] = c[0] + ax[0] * hw + ay[0] * hh; q[4] = c[1] + ax[1] * hw + ay[1] * hh; q[5] = c[2] + ax[2] * hw + ay[2] * hh;
        q[6] = c[0] + ax[0] * hw - ay[0] * hh; q[7] = c[1] + ax[1] * hw - ay[1] * hh; q[8] = c[2] + ax[2] * hw - ay[2] * hh;
        q[9] = c[0] - ax[0] * hw - ay[0] * hh; q[10] = c[1] - ax[1] * hw - ay[1] * hh; q[11] = c[2] - ax[2] * hw - ay[2] * hh;
        if (flip) out.sprite(tex, add, u1, u0, argb, q);
        else out.sprite(tex, add, u0, u1, argb, q);
    }

    /** a camera facing sprite, rolled */
    private void bb(float[] c, float w, float h, int tex, float roll, int argb, boolean add) {
        float ca = cos(roll), sa = sin(roll);
        AX[0] = view.rx * ca + view.ux * sa; AX[1] = view.ry * ca + view.uy * sa; AX[2] = view.rz * ca + view.uz * sa;
        AY[0] = -view.rx * sa + view.ux * ca; AY[1] = -view.ry * sa + view.uy * ca; AY[2] = -view.rz * sa + view.uz * ca;
        plane(c, AX, AY, w * 0.5f, h * 0.5f, tex, argb, false, 0f, 1f, add);
    }

    /** a strip from a to b facing the viewer (the texture's +x runs a to b, its top kept up on the screen) */
    private void strip2(float[] a, float[] b, float width, int tex, int argb, float tile, float uoff, boolean flip) {
        float dx = b[0] - a[0], dy = b[1] - a[1], dz = b[2] - a[2];
        float ln = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (ln < 1e-6f) return;
        AX[0] = dx / ln; AX[1] = dy / ln; AX[2] = dz / ln;
        CEN[0] = (a[0] + b[0]) * 0.5f; CEN[1] = (a[1] + b[1]) * 0.5f; CEN[2] = (a[2] + b[2]) * 0.5f;
        cross(AY, AX, CEN);                                                   // the camera is the origin: the view ray is the centre itself
        float yl = len(AY);
        if (yl < 1e-9f) return;
        AY[0] /= yl; AY[1] /= yl; AY[2] /= yl;
        if (AY[1] < 0f) { AY[0] = -AY[0]; AY[1] = -AY[1]; AY[2] = -AY[2]; }
        plane(CEN, AX, AY, ln * 0.5f, width * 0.5f, tex, argb, flip, uoff, uoff + tile, false);
    }

    private void strip2(float[] a, float[] b, float width, int tex, int argb) { strip2(a, b, width, tex, argb, 1f, 0f, false); }

    /** a strip pushed {@code off} blocks toward (+) or away from (-) the camera: the shells of glass in front of and behind a core */
    private void sheet(float[] a, float[] b, float width, int tex, float alpha, float off, float tile, float uoff) {
        MID[0] = (a[0] + b[0]) * 0.5f; MID[1] = (a[1] + b[1]) * 0.5f; MID[2] = (a[2] + b[2]) * 0.5f;
        float l = len(MID), k = l > 1e-6f ? -off / l : 0f;
        SA[0] = a[0] + MID[0] * k; SA[1] = a[1] + MID[1] * k; SA[2] = a[2] + MID[2] * k;
        SB[0] = b[0] + MID[0] * k; SB[1] = b[1] + MID[1] * k; SB[2] = b[2] + MID[2] * k;
        strip2(SA, SB, width, tex, w(alpha), tile, uoff, false);
    }

    private void ground(float[] c, float r, int tex, int argb, float rot, boolean add) {
        set(AX, cos(rot), 0f, sin(rot));
        set(AY, -sin(rot), 0f, cos(rot));
        plane(c, AX, AY, r, r, tex, argb, false, 0f, 1f, add);
    }

    /** base centre c, facing the camera round the vertical axis */
    private void upright(float[] c, float w, float h, int tex, int argb) {
        float rx = view.rx, rz = view.rz, l = (float) Math.sqrt(rx * rx + rz * rz);
        if (l < 1e-6f) { rx = 1f; rz = 0f; l = 1f; }
        set(AX, rx / l, 0f, rz / l);
        set(AY, 0f, 1f, 0f);
        set(CEN, c[0], c[1] + h * 0.5f, c[2]);
        plane(CEN, AX, AY, w * 0.5f, h * 0.5f, tex, argb, false, 0f, 1f, false);
    }

    /** a vertical plane with its base centre at c, turned by yaw round the vertical axis */
    private void vplane(float[] c, float yaw, float w, float h, int tex, int argb) {
        set(AX, cos(yaw), 0f, sin(yaw));
        set(AY, 0f, 1f, 0f);
        set(CEN, c[0], c[1] + h * 0.5f, c[2]);
        plane(CEN, AX, AY, w * 0.5f, h * 0.5f, tex, argb, false, 0f, 1f, false);
    }

    private void mist(float[] pos, float size, float alpha, float roll) { bb(pos, size, size, WaterBc.T_MIST, roll, w(alpha), false); }

    private void bubble(float[] pos, float size, float alpha) { bb(pos, size, size, WaterBc.T_B_BUBBLE, 0f, w(alpha), false); }

    // ================================================================================================ mesh primitive
    private void mesh(int piece, boolean lo, float[] pos, float[] y, float[] hint, boolean xHint, float sx, float sy, float sz, float alpha, float foam, int pal, boolean fres) {
        out.mesh(mi.set(piece, lo, pos[0], pos[1], pos[2], y[0], y[1], y[2], hint[0], hint[1], hint[2], xHint, sx, sy, sz, alpha, foam, pal, fres));
    }

    private static final float[] UP = {0f, 1f, 0f}, Z1 = {0f, 0f, 1f};

    // ================================================================================================ shared frames and states
    /** the direction, side and up of an effect that runs from F to T (a straight up/down effect uses the x axis as its side) */
    private float frameOf() {
        sub(FD, T, F);
        float l = len(FD);
        if (l < 0.05f) { set(FD, 0f, 0f, 1f); l = 0.05f; }
        else { FD[0] /= l; FD[1] /= l; FD[2] /= l; }
        if (Math.abs(FD[1]) < 0.95f) { cross(FS, FD, UP); nrm(FS); }
        else set(FS, 1f, 0f, 0f);
        cross(FU, FS, FD);
        nrm(FU);
        return l;
    }

    private float rL, rHead, rTail, rFade, rOpen, rRun;

    private void roarState() {
        rL = frameOf();
        float charge = clamp(age / (0.25f * dur));
        rRun = clamp((age - 0.12f * dur) / (0.7f * dur));
        rHead = easeOut(rRun);
        rTail = Math.max(0f, rHead - 0.4f);
        rFade = clamp((dur - age) / 6f);
        rOpen = easeBack(charge) * (1f - 0.4f * clamp((age - 0.4f * dur) / (0.3f * dur)));
    }

    /** the body's path: o = the point at parameter sg (0 at the caster, 1 at the target), waving along the side and up */
    private void roarPt(float sg, float[] o) {
        float env = clamp((rHead - sg) / 0.22f), amp = 0.42f * p * env, wave = amp * sin(sg * rL * 0.85f - age * 0.45f), lift = 0.14f * p * env * sin(sg * rL * 0.55f - age * 0.3f);
        float d = rL * sg;
        o[0] = F[0] + FD[0] * d + FS[0] * wave + FU[0] * lift;
        o[1] = F[1] + FD[1] * d + FS[1] * wave + FU[1] * lift;
        o[2] = F[2] + FD[2] * d + FS[2] * wave + FU[2] * lift;
    }

    private void roarTang(float sg, float[] o) {
        roarPt(Math.max(0f, sg - 0.03f), E);
        roarPt(sg + 0.001f, o);
        sub(o, o, E);
        nrm(o);
    }

    private float lFade, lHead, lRun, lL;
    private final float[] LHP = new float[3];

    private void lanceState() {
        lL = frameOf();
        lRun = clamp((age - 0.1f * dur) / (0.7f * dur));
        lHead = easeOut(lRun);
        lFade = clamp((dur - age) / 4f);
        addS(LHP, F, FD, lL * lHead);
    }

    private float zFade, zOp;

    private void zoneState() {
        zFade = clamp(Math.min(age / 8f, (dur - age) / 12f));
        zOp = easeBack(clamp(age / 10f));
    }

    /** drops and bubbles thrown up and falling: positions on an arc (burst, splash) */
    private void bubblesArc(float t, int n, int tex, float size, long sd, int argb, float vy0, float vy1, float vh0, float vh1) {
        for (int i = 0; i < n; i++) {
            float a = hs(sd, i, 1) * TAU, v = (vh0 + (vh1 - vh0) * hs(sd, i, 2)) * p, vyy = (vy0 + (vy1 - vy0) * hs(sd, i, 3)) * p, tt = t * 1.2f;
            set(G, F[0] + cos(a) * v * tt * 0.7f, F[1] + 0.1f + vyy * tt - 2.8f * tt * tt * p, F[2] + sin(a) * v * tt * 0.7f);
            float s = size * p * (1f - 0.4f * t);
            bb(G, s, s, tex, 0f, argb, false);
        }
    }

    // ================================================================================================ ROAR (WATERFX_CAST, the dragon)
    private void roar(boolean lo) {
        roarState();
        if (rHead <= 0.001f) {
            set(A, F[0], F[1] + 0.05f, F[2]);
            ground(A, 0.7f * p * rOpen, WaterBc.T_B_RIPPLE, w(0.8f * rFade), -age * 0.07f, false);
            set(A, F[0], F[1] + 0.06f, F[2]);
            ground(A, 0.7f * p * rOpen, WaterBc.T_GLOW, col(0x78DCFF, 0.7f * rFade), 0f, true);
            return;
        }
        roarPt(rHead, HP);
        roarTang(rHead, TG);
        float kh = 0.95f * p;
        cross(ZZ, FS, TG);                                                    // up of the head: side x tangent, kept on the upper side
        nrm(ZZ);
        if (ZZ[1] < 0f) { ZZ[0] = -ZZ[0]; ZZ[1] = -ZZ[1]; ZZ[2] = -ZZ[2]; }
        // basis of the head: Y = tangent, Z = up hint made perpendicular, X = Y x Z
        cp(YY, TG);
        nrm(YY);
        float d = dot(ZZ, YY);
        set(E, ZZ[0] - YY[0] * d, ZZ[1] - YY[1] * d, ZZ[2] - YY[2] * d);
        if (len(E) < 1e-4f) set(E, 1f - YY[0] * YY[0], -YY[0] * YY[1], -YY[0] * YY[2]);
        if (len(E) < 1e-4f) set(E, 0f, 0f, 1f);
        nrm(E);
        cp(ZZ, E);
        cross(XX, YY, ZZ);
        addS(ORG, HP, YY, 8f * kh / 16f);
        float jaw = (0.35f + 0.30f * rOpen) * (0.9f + 0.1f * sin(age * 0.5f));
        mesh(WaterBc.P_HEAD_UPPER, lo, ORG, YY, ZZ, false, kh, kh, kh, 1f, 1f, WaterBcToon.PAL_CORE, false);
        mesh(WaterBc.P_HEAD_HULL, lo, ORG, YY, ZZ, false, kh, kh, kh, 0.32f, 1f, WaterBcToon.PAL_GLASS, true);
        float cj = cos(jaw), sj = sin(jaw);
        set(A, YY[0] * cj - ZZ[0] * sj, YY[1] * cj - ZZ[1] * sj, YY[2] * cj - ZZ[2] * sj);               // jaw y
        set(B, ZZ[0] * cj + YY[0] * sj, ZZ[1] * cj + YY[1] * sj, ZZ[2] * cj + YY[2] * sj);               // jaw z
        set(C, ORG[0] + (YY[0] * 2f - ZZ[0] * 9f) * kh / 16f, ORG[1] + (YY[1] * 2f - ZZ[1] * 9f) * kh / 16f, ORG[2] + (YY[2] * 2f - ZZ[2] * 9f) * kh / 16f);
        mesh(WaterBc.P_HEAD_LOWER, lo, C, A, B, false, kh, kh, kh, 1f, 1f, WaterBcToon.PAL_CORE, false);
        float land = clamp((rHead - 0.88f) / 0.12f);
        int nseg = Math.max(1, Math.round((lo ? 3 : 4) * (1f - 0.8f * land)));                           // 4 pieces of body (3 at LOD1), fewer while it lands
        float g0 = rHead - 0.05f * p;
        for (int i = 0; i < nseg; i++) {
            roarPt(g0 - (rHead - rTail) * (i / (float) nseg), PTS[0]);
            roarPt(g0 - (rHead - rTail) * ((i + 1) / (float) nseg), PTS[1]);
            add(MID, PTS[0], PTS[1]);
            MID[0] *= 0.5f; MID[1] *= 0.5f; MID[2] *= 0.5f;
            sub(DV, PTS[0], PTS[1]);
            float ln = len(DV);
            if (ln < 1e-4f) continue;
            float g = (i + 0.5f) / nseg, ks = kh * (0.97f - 0.52f * g);
            mesh(i % 2 == 0 ? WaterBc.P_SEG_B : WaterBc.P_SEG, lo, MID, DV, UP, false, ks, ln * 1.08f, ks, 1f, 1f, WaterBcToon.PAL_CORE, false);
        }
        roarPt(rTail, TP);
        roarPt(rTail + 0.06f, E);
        sub(E, TP, E);
        nrm(E);
        mesh(WaterBc.P_TAIL, lo, TP, E, UP, false, kh * 0.42f, kh * 0.42f, kh * 0.42f, 1f, 1f, WaterBcToon.PAL_CORE, false);

        // ---- glass wrap: sheen shells on the body, flow ribbons, mist over the neck join, bubbles, drops, the lens, glints
        int nst = lo ? 4 : 6;
        float fade = rFade;
        for (int i = 0; i <= nst; i++) roarPt(rTail + (rHead - rTail) * i / nst, PTS[Math.min(i, 8)]);
        for (int i = 0; i < nst; i++) {
            float r0 = 0.47f * kh * (0.97f - 0.52f * (1f - i / (float) nst)), r1 = 0.47f * kh * (0.97f - 0.52f * (1f - (i + 1) / (float) nst)), rm = (r0 + r1) * 0.5f;
            sub(DV, PTS[i + 1], PTS[i]);
            addS(A, PTS[i], DV, -0.3f);                                         // the strips overlap, so their rectangular ends do not show as seams
            addS(B, PTS[i + 1], DV, 0.3f);
            sheet(A, B, 2f * rm * 2.0f, WaterBc.T_B_BODY, 0.26f * fade, -0.3f * rm, 0.6f, age * 0.020f + i * 0.45f);
            sheet(A, B, 2f * rm * 1.55f, WaterBc.T_B_BODY, 0.36f * fade, 1.05f * rm, 0.6f, -age * 0.03f + i * 0.7f + 0.3f);
        }
        for (int k = 0; k < (lo ? 1 : 2); k++) {                                // flow ribbons winding round the body
            boolean first = true;
            int nrb = 8;
            for (int j = 0; j <= nrb; j++) {
                float sg = rTail + (rHead - 0.02f - rTail) * j / nrb, gg = j / (float) nrb;
                float ang = sg * rL * 1.25f - age * 0.42f + k * PI, rr = 0.47f * kh * (0.97f - 0.52f * (1f - gg)) * 1.28f;
                roarPt(sg, CUR);
                CUR[0] += FS[0] * cos(ang) * rr + FU[0] * sin(ang) * rr * 0.8f;
                CUR[1] += FS[1] * cos(ang) * rr + FU[1] * sin(ang) * rr * 0.8f;
                CUR[2] += FS[2] * cos(ang) * rr + FU[2] * sin(ang) * rr * 0.8f;
                if (!first) strip2(PREV, CUR, 0.15f * p, WaterBc.T_RIBBON, w(0.85f * fade));
                cp(PREV, CUR);
                first = false;
            }
        }
        for (int i = 0; i < (lo ? 2 : 5); i++) {                                // mist at the neck join and along the wake
            float sg = rHead - (0.03f + 0.07f * i) * (i < 2 ? 1f : 3f);
            sg = Math.max(rTail, Math.min(rHead, sg));
            roarPt(sg, G);
            G[0] += FU[0] * 0.25f * p + FS[0] * (hs(i, 9) - 0.5f) * 0.5f * p;
            G[1] += FU[1] * 0.25f * p + FS[1] * (hs(i, 9) - 0.5f) * 0.5f * p;
            G[2] += FU[2] * 0.25f * p + FS[2] * (hs(i, 9) - 0.5f) * 0.5f * p;
            mist(G, (0.9f + 0.3f * hs(i, 8)) * p, 0.34f * fade, i * 1.3f + age * 0.02f);
        }
        for (int i = 0; i < (lo ? 5 : 10); i++) {
            float sg = rTail + (rHead - rTail) * hs(i, 4) * 0.9f, life = frac(age * 0.07f + hs(i, 5));
            roarPt(sg, G);
            float sd = (hs(i, 6) - 0.5f) * 0.8f * p, up = 0.4f * life * p + 0.15f * p;
            G[0] += FS[0] * sd + FU[0] * up; G[1] += FS[1] * sd + FU[1] * up; G[2] += FS[2] * sd + FU[2] * up;
            bubble(G, (0.2f + 0.15f * hs(i, 7)) * p, sin(life * PI) * 0.9f * fade);
        }
        for (int i = 0; i < (lo ? 2 : 4); i++) {
            float sg = rTail + (rHead - rTail) * hs(i, 4), life = frac(age * 0.06f + hs(i, 5));
            roarPt(sg, G);
            float sd = (hs(i, 6) - 0.5f) * 0.9f * p, up = 0.5f * sin(life * PI) * p - 0.35f * life * life * p + 0.35f * p;
            G[0] += FS[0] * sd + FU[0] * up; G[1] += FS[1] * sd + FU[1] * up; G[2] += FS[2] * sd + FU[2] * up;
            bb(G, 0.22f * p, 0.28f * p, WaterBc.T_B_DROP, 0f, w(sin(life * PI) * fade), false);
        }
        addS(A, HP, TG, -0.1f * p);
        addS(B, HP, TG, 2.6f * p);
        sheet(A, B, 1.30f * p, WaterBc.T_SHELL, 0.45f * fade, 0.7f * kh, 1f, 0f);       // a soft glass lens over the skull
        float sx = (XX[0] * -ORG[0] + XX[1] * -ORG[1] + XX[2] * -ORG[2]) > 0f ? 1f : -1f;
        bb(ORG, 3.4f * p, 3.4f * p, WaterBc.T_GLOW, 0f, col(0x82D7FF, 0.28f * fade), true);
        set(G, ORG[0] + (XX[0] * sx * 9f + YY[0] * 11f + ZZ[0] * 3.5f) * kh / 16f, ORG[1] + (XX[1] * sx * 9f + YY[1] * 11f + ZZ[1] * 3.5f) * kh / 16f, ORG[2] + (XX[2] * sx * 9f + YY[2] * 11f + ZZ[2] * 3.5f) * kh / 16f);
        bb(G, 0.45f * p, 0.45f * p, WaterBc.T_B_GLINT, age * 0.1f, w(0.85f * fade), true);
        addS(G, HP, TG, 1.3f * p);
        bb(G, 0.9f * p, 0.9f * p, WaterBc.T_GLOW, 0f, col(0x78DCFF, 0.45f * fade), true);
        set(A, F[0], F[1] + 0.05f, F[2]);
        ground(A, 0.7f * p * rOpen, WaterBc.T_B_RIPPLE, w(0.8f * fade * (1f - rRun)), -age * 0.07f, false);
        if (rHead > 0.88f) landingCrown((rHead - 0.88f) / 0.12f, lo, fade, 0.35f, 0.8f, 0.7f, 4, 2.6f, 1.3f, 0.3f, 0.55f);
    }

    /** the roar's landing at T: horns, a glass crown, a ripple ring; {@code k} 0..1 */
    private void landingCrown(float k, boolean lo, float fade, float reach, float horn, float hornHeight, int nHorns, float crownW, float crownH, float base, float crownA) {
        int n = lo ? nHorns - 1 : nHorns;
        for (int i = 0; i < n; i++) {
            float a = i * TAU / n + base;
            set(E, cos(a), 0f, sin(a));
            set(G, T[0] + E[0] * reach * p * k, T[1] - 0.05f, T[2] + E[2] * reach * p * k);
            mesh(WaterBc.P_HORN, lo, G, UP, E, true, horn * p, hornHeight * p * k, horn * p, 0.92f, 0.92f, WaterBcToon.PAL_WATER, false);
        }
        set(A, T[0], T[1] - 0.1f, T[2]);
        upright(A, crownW * p * k, crownH * p * k, WaterBc.T_B_CROWN, w(crownA * (1f - 0.5f * k) * fade));
        set(A, T[0], T[1] + 0.05f, T[2]);
        float rr = (0.5f + 1.1f * k) * p;
        mesh(WaterBc.P_RIPPLE, lo, A, UP, Z1, false, rr, 0.8f * p * (1f - k), rr, 0.6f, 0.7f, WaterBcToon.PAL_GLASS, true);
        set(A, T[0], T[1] + 0.08f, T[2]);
        ground(A, rr, WaterBc.T_B_RIPPLE, w((1f - k) * 0.7f * fade), age * 0.05f, false);
    }

    private void roarA() {
        roarState();
        if (rHead <= 0.001f) {
            set(A, F[0], F[1] + 0.05f, F[2]);
            ground(A, 0.6f * p * rOpen, WaterBc.T_A_RING, w(0.9f * rFade), -age * 0.07f, false);
            return;
        }
        roarPt(rHead, HP);
        roarTang(rHead, TG);
        int n = 8;
        for (int i = 0; i <= n; i++) roarPt(rTail + (rHead - rTail) * i / n, PTS[i]);
        for (int i = 0; i < n; i++) {
            float g = (i + 0.5f) / n, wd = (0.35f + 1.0f * g) * p;
            strip2(PTS[i], PTS[i + 1], wd, WaterBc.T_A_BODY, w(0.95f * rFade), 0.45f, -age * 0.02f + i * 0.45f, false);
        }
        addS(A, HP, TG, -0.1f * p);
        addS(B, HP, TG, 3.0f * p);
        strip2(A, B, 2.3f * p, WaterBc.T_A_HEAD, w(rFade));
        for (int i = 0; i < 8; i++) {                                           // drops shed from the body
            float sg = rTail + (rHead - rTail) * hs(i, 4), life = frac(age * 0.06f + hs(i, 5));
            roarPt(sg, G);
            float sd = (hs(i, 6) - 0.5f) * 0.9f * p, up = 0.5f * sin(life * PI) * p - 0.35f * life * life * p + 0.2f * p;
            G[0] += FS[0] * sd + FU[0] * up; G[1] += FS[1] * sd + FU[1] * up; G[2] += FS[2] * sd + FU[2] * up;
            bb(G, 0.24f * p, 0.28f * p, WaterBc.T_A_DROP, 0f, w(sin(life * PI) * rFade), false);
        }
        set(A, F[0], F[1] + 0.05f, F[2]);
        ground(A, 0.6f * p * rOpen, WaterBc.T_A_RING, w(0.9f * rFade * (1f - rRun)), -age * 0.07f, false);
        if (rHead > 0.88f) {
            float k = (rHead - 0.88f) / 0.12f;
            set(A, T[0], T[1] - 0.1f, T[2]);
            upright(A, 2.4f * p * k, 1.3f * p * k, WaterBc.T_A_CROWN, w((1f - 0.5f * k) * rFade));
            set(A, T[0], T[1] + 0.08f, T[2]);
            ground(A, (0.5f + 1.1f * k) * p, WaterBc.T_A_RING, w((1f - k) * 0.9f * rFade), age * 0.05f, false);
        }
    }

    // ================================================================================================ LANCE (WATERFX_CAST flying fast: Aqua Javelin, Tide Lance)
    private void lance(boolean lo) {
        lanceState();
        set(A, F[0], F[1] + 0.05f, F[2]);
        ground(A, 0.5f * p * clamp(age / 4f), WaterBc.T_B_RIPPLE, w(0.8f * lFade * (1f - lRun)), -age * 0.1f, false);
        if (lHead <= 0.001f) return;
        float[] hp = LHP;
        float k = 1.15f * p * (0.85f + 0.15f * lFade);
        addS(A, hp, FD, -2.6f * p);
        mesh(WaterBc.P_LANCE, lo, A, FD, UP, false, k, k, k, 1f, 1f, WaterBcToon.PAL_CORE, false);
        addS(A, hp, FD, -5.5f * p);
        addS(B, hp, FD, 0.8f * p);
        strip2(A, B, 1.75f * p, WaterBc.T_B_LANCE, w(0.40f * lFade));            // the wide glass shell behind the spear
        addS(A, hp, FD, -4.6f * p);
        addS(B, hp, FD, -0.8f * p);
        sheet(A, B, 0.62f * p, WaterBc.T_SHEEN, 0.30f * lFade, 0.4f * p, 0.45f, -age * 0.05f);
        for (int kk = 0; kk < (lo ? 1 : 2); kk++) {                             // two helical flow ribbons
            boolean first = true;
            int nrb = lo ? 7 : 10;
            for (int i = 0; i <= nrb; i++) {
                float g = i / (float) nrb, ang = g * 9f - age * 0.45f + kk * PI, rr = 0.5f * p * (1f - 0.35f * g), back = 4.9f * p * (1f - g) - 0.2f * p;
                CUR[0] = hp[0] - FD[0] * back + FS[0] * cos(ang) * rr + FU[0] * sin(ang) * rr;
                CUR[1] = hp[1] - FD[1] * back + FS[1] * cos(ang) * rr + FU[1] * sin(ang) * rr;
                CUR[2] = hp[2] - FD[2] * back + FS[2] * cos(ang) * rr + FU[2] * sin(ang) * rr;
                if (!first) strip2(PREV, CUR, 0.17f * p, WaterBc.T_RIBBON, w(0.85f * lFade));
                cp(PREV, CUR);
                first = false;
            }
        }
        for (int i = 0; i < (lo ? 2 : 3); i++) {
            addS(G, hp, FD, -(3.6f + 1.3f * i) * p);
            addS(G, G, FU, 0.12f * p);
            mist(G, 0.85f * p, 0.32f * lFade, i);
        }
        for (int i = 0; i < (lo ? 4 : 7); i++) {
            float life = frac(age * 0.09f + hs(i, 5)), sd = (hs(i, 6) - 0.5f) * 0.8f * p, up = 0.25f * life * p;
            addS(G, hp, FD, -hs(i, 4) * 4.8f * p);
            G[0] += FS[0] * sd + FU[0] * up; G[1] += FS[1] * sd + FU[1] * up; G[2] += FS[2] * sd + FU[2] * up;
            bubble(G, (0.16f + 0.12f * hs(i, 7)) * p, sin(life * PI) * 0.9f * lFade);
        }
        for (int i = 0; i < (lo ? 1 : 3); i++) {
            float life = frac(age * 0.08f + hs(i, 5)), sd = (hs(i, 6) - 0.5f) * 0.9f * p, up = 0.3f * p - 0.3f * life * p;
            addS(G, hp, FD, -hs(i, 4) * 4.5f * p);
            G[0] += FS[0] * sd + FU[0] * up; G[1] += FS[1] * sd + FU[1] * up; G[2] += FS[2] * sd + FU[2] * up;
            bb(G, 0.18f * p, 0.22f * p, WaterBc.T_B_DROP, 0f, w(sin(life * PI) * lFade), false);
        }
        addS(G, hp, FD, 0.62f * p);
        bb(G, 0.7f * p, 0.7f * p, WaterBc.T_B_GLINT, age * 0.2f, w(0.9f * lFade), true);
        addS(G, hp, FD, 2.2f * p);
        bb(G, 0.8f * p, 0.8f * p, WaterBc.T_GLOW, 0f, col(0x8CE6FF, 0.4f * lFade), true);
        if (lHead > 0.9f) {
            float kk = (lHead - 0.9f) / 0.1f;
            for (int i = 0; i < 3; i++) {
                float a = i * TAU / 3f;
                set(E, cos(a), 0f, sin(a));
                set(G, T[0] + E[0] * 0.25f * p * kk, T[1] - 0.05f, T[2] + E[2] * 0.25f * p * kk);
                mesh(WaterBc.P_HORN, lo, G, UP, E, true, 0.6f * p, 0.5f * p * kk, 0.6f * p, 0.92f, 0.92f, WaterBcToon.PAL_WATER, false);
            }
            set(A, T[0], T[1] - 0.1f, T[2]);
            upright(A, 1.9f * p * kk, 1.0f * p * kk, WaterBc.T_B_CROWN, w(0.55f * (1f - 0.5f * kk)));
        }
    }

    private void lanceA() {
        lanceState();
        float[] hp = LHP;
        set(A, F[0], F[1] + 0.05f, F[2]);
        ground(A, 0.5f * p * clamp(age / 4f), WaterBc.T_A_RING, w(0.9f * lFade * (1f - lRun)), -age * 0.1f, false);
        if (lHead <= 0.001f) return;
        addS(A, hp, FD, -5.2f * p);
        addS(B, hp, FD, 0.6f * p);
        strip2(A, B, 1.35f * p, WaterBc.T_A_LANCE, w(lFade));
        for (int k = 0; k < 2; k++) {                                           // two wisps winding round the shaft
            boolean first = true;
            for (int i = 0; i < 7; i++) {
                float g = i / 6f, ang = g * 9f - age * 0.45f + k * PI, rr = 0.5f * p * (1f - 0.4f * g), back = 4.6f * p * (1f - g);
                CUR[0] = hp[0] - FD[0] * back + FS[0] * cos(ang) * rr + FU[0] * sin(ang) * rr;
                CUR[1] = hp[1] - FD[1] * back + FS[1] * cos(ang) * rr + FU[1] * sin(ang) * rr;
                CUR[2] = hp[2] - FD[2] * back + FS[2] * cos(ang) * rr + FU[2] * sin(ang) * rr;
                if (!first) strip2(PREV, CUR, 0.2f * p, WaterBc.T_A_BODY, w(0.9f * lFade));
                cp(PREV, CUR);
                first = false;
            }
        }
        for (int i = 0; i < 6; i++) {
            float life = frac(age * 0.08f + hs(i, 5)), sd = (hs(i, 6) - 0.5f) * 0.9f * p, up = 0.3f * p - 0.3f * life * p;
            addS(G, hp, FD, -hs(i, 4) * 4.5f * p);
            G[0] += FS[0] * sd + FU[0] * up; G[1] += FS[1] * sd + FU[1] * up; G[2] += FS[2] * sd + FU[2] * up;
            bb(G, 0.2f * p, 0.24f * p, WaterBc.T_A_DROP, 0f, w(sin(life * PI) * lFade), false);
        }
        if (lHead > 0.9f) {
            float k = (lHead - 0.9f) / 0.1f;
            set(A, T[0], T[1] - 0.1f, T[2]);
            upright(A, 1.8f * p * k, 1.0f * p * k, WaterBc.T_A_CROWN, w((1f - 0.5f * k) * lFade));
        }
    }

    // ================================================================================================ CRADLE (WATERFX_ZONE following its caster: the sphere)
    private void cradle(boolean lo) {
        zoneState();
        float R = p, fade = zFade, op = zOp;
        set(SPH, F[0], F[1] + R * 0.95f, F[2]);
        mesh(WaterBc.P_SPHERE, lo, SPH, UP, Z1, false, 16f * R * 0.97f * op, 16f * R * 0.97f * op, 16f * R * 0.97f * op, 0.40f, 0.9f, WaterBcToon.PAL_GLASS, true);
        for (int i = 0; i < (lo ? 2 : 3); i++) {                                // whirl bands: rings turning on their own axes
            float ang = age * (0.05f + 0.02f * i) + i * 2.1f;
            set(E, cos(ang) * 0.5f + 0.3f * i, 1.0f, sin(ang) * 0.5f - 0.3f * i);
            nrm(E);
            mesh(WaterBc.P_RIPPLE, true, SPH, E, Z1, false, R * op, R * 1.3f * op, R * op, 0.7f, 0.8f, WaterBcToon.PAL_GLASS, true);
        }
        set(A, F[0], F[1] + 0.05f, F[2]);
        mesh(WaterBc.P_RIPPLE, true, A, UP, Z1, false, R * op, 0.9f * R, R * op, 0.6f, 0.7f, WaterBcToon.PAL_GLASS, true);
        // glass fill: the rim shell, a faint caustic sphere, two crossed caustic sheets, bubbles inside, the chain beside it
        ground(A, R * 1.1f * op, WaterBc.T_B_RIPPLE, w(0.7f * fade), age * 0.02f, false);
        bb(SPH, 2f * R * 0.99f * op, 2f * R * 0.99f * op, WaterBc.T_SHELL, 0f, w(0.95f * fade), false);
        bb(SPH, 2f * R * 0.93f * op, 2f * R * 0.93f * op, WaterBc.T_B_SPHERE, 0f, w(0.20f * fade), false);
        if (!lo) {
            for (int i = 0; i < 2; i++) {
                float yaw = age * 0.04f * (i == 0 ? 1f : -1.4f) + i * PI / 2f;
                set(B, F[0], F[1] + R * 0.05f, F[2]);
                vplane(B, yaw, 2f * R * 0.9f * op, 1.9f * R * op, WaterBc.T_B_SPHERE, w(0.16f * fade));
            }
        }
        for (int i = 0; i < (lo ? 5 : 9); i++) {
            float life = frac(age * 0.03f + hs(i, 5)), ang = hs(i, 4) * TAU, rr = R * 0.8f * hs(i, 3);
            set(G, F[0] + cos(ang) * rr, F[1] + 0.2f + life * 1.8f * R, F[2] + sin(ang) * rr);
            bubble(G, (0.18f + 0.2f * hs(i, 7)) * R * 0.6f, sin(life * PI) * 0.95f * fade);
        }
        for (int i = 0; i < (lo ? 3 : 5); i++) {                                // the bubble chain beside it
            float rr = R * (1.15f + 0.3f * i), uu = -0.22f * R * i + 0.15f * R;
            set(G, SPH[0] + view.rx * rr + view.ux * uu, SPH[1] + view.ry * rr + view.uy * uu, SPH[2] + view.rz * rr + view.uz * uu);
            bubble(G, R * (0.36f - 0.05f * i), 0.95f * fade);
        }
        for (int i = 0; i < (lo ? 2 : 3); i++) {
            set(G, F[0] + cos(i * 2.1f + age * 0.02f) * R * 0.8f, F[1] + 0.12f, F[2] + sin(i * 2.1f + age * 0.02f) * R * 0.8f);
            mist(G, 1.3f * R, 0.30f * fade, i);
        }
        set(G, SPH[0] + view.rx * (-R * 0.42f) + view.ux * (R * 0.55f), SPH[1] + view.ry * (-R * 0.42f) + view.uy * (R * 0.55f), SPH[2] + view.rz * (-R * 0.42f) + view.uz * (R * 0.55f));
        bb(G, 0.35f * R, 0.35f * R, WaterBc.T_B_GLINT, 0.3f, w(0.8f * fade), true);
    }

    private void cradleA() {
        zoneState();
        float R = p, fade = zFade, op = zOp;
        set(SPH, F[0], F[1] + R * 0.95f, F[2]);
        set(A, F[0], F[1] + 0.05f, F[2]);
        ground(A, R * 1.1f * op, WaterBc.T_A_RING, w(0.8f * fade), age * 0.02f, false);
        set(A, F[0], F[1] + 0.07f, F[2]);
        ground(A, R * 0.8f * op, WaterBc.T_A_RING, w(0.7f * fade), -age * 0.03f, false);
        bb(SPH, 2f * R * 0.97f * op, 2f * R * 0.97f * op, WaterBc.T_A_SPHERE, 0f, w(0.78f * fade), false);
        for (int i = 0; i < 5; i++) {                                           // the chain of bubbles beside it
            float rr = R * (1.05f + 0.34f * i), uu = -0.25f * R * i + 0.2f * R, sz = R * (0.38f - 0.05f * i);
            set(G, SPH[0] + view.rx * rr + view.ux * uu, SPH[1] + view.ry * rr + view.uy * uu, SPH[2] + view.rz * rr + view.uz * uu);
            bb(G, sz, sz, WaterBc.T_A_BLOB, 0f, w(0.9f * fade), false);
        }
        for (int i = 0; i < 4; i++) {
            float ang = age * 0.06f + i * TAU / 4f;
            set(G, SPH[0] + cos(ang) * R * 1.15f, SPH[1] + 0.3f * R * sin(ang * 2f), SPH[2] + sin(ang) * R * 1.15f);
            bb(G, 0.3f * R, 0.3f * R, WaterBc.T_A_BLOB, 0f, w(0.9f * fade), false);
        }
    }

    // ================================================================================================ NEST (WATERFX_ZONE placed on the ground: the dome)
    private void nest(boolean lo) {
        zoneState();
        float R = p, fade = zFade, op = zOp, spin = age * 0.05f;
        set(E, cos(spin), 0f, sin(spin));
        mesh(WaterBc.P_DOME, lo, F, UP, E, false, R * op, R * op, R * op, 0.50f * Math.min(1f, fade * 1.4f), 0.8f, WaterBcToon.PAL_CORE, true);
        set(A, F[0], F[1] + 0.05f, F[2]);
        mesh(WaterBc.P_RIPPLE, true, A, UP, Z1, false, R * 1.12f * op, 0.9f * R, R * 1.12f * op, 0.6f, 0.7f, WaterBcToon.PAL_GLASS, true);
        ground(A, R * 1.2f * op, WaterBc.T_B_RIPPLE, w(0.65f * fade), age * 0.03f, false);
        float base = (float) Math.atan2(view.rz, view.rx);
        for (int i = 0; i < (lo ? 2 : 3); i++) {
            float wy = i == 0 ? 0f : i == 1 ? 0.8f : -0.8f, sp = i == 0 ? 0f : i == 1 ? 1f : -1.4f, ww = i == 0 ? 2.0f : i == 1 ? 1.7f : 1.5f, al = i == 0 ? 0.34f : 0.28f;
            vplane(F, base + wy + age * 0.02f * sp, ww * R * op, R * (1.02f - 0.05f * i) * op, WaterBc.T_B_DOME, w(al * fade));
        }
        for (int i = 0; i < (lo ? 5 : 9); i++) {
            float life = frac(age * 0.03f + hs(i, 5)), ang = hs(i, 4) * TAU, rr = R * 0.85f * hs(i, 3);
            set(G, F[0] + cos(ang) * rr, F[1] + 0.1f + life * 1.1f * R, F[2] + sin(ang) * rr);
            bubble(G, 0.22f * R * 0.8f, sin(life * PI) * fade);
        }
        for (int i = 0; i < (lo ? 2 : 4); i++) {
            float a = age * 0.03f + i * TAU / 4f;
            set(G, F[0] + cos(a) * R * 1.05f, F[1] + 0.15f, F[2] + sin(a) * R * 1.05f);
            mist(G, 0.9f * R, 0.30f * fade, i);
        }
        int beads = lo ? 3 : 5;
        for (int i = 0; i < beads; i++) {                                       // beads of foam round the rim (glass bubbles)
            float ang = age * 0.05f + i * TAU / beads;
            set(G, F[0] + cos(ang) * R * 1.1f, F[1] + 0.2f * R + 0.1f * R * sin(age * 0.2f + i), F[2] + sin(ang) * R * 1.1f);
            bubble(G, 0.26f * R, 0.95f * fade);
        }
        set(G, F[0] + view.rx * (-R * 0.4f) + view.ux * (R * 0.7f), F[1] + view.ry * (-R * 0.4f) + view.uy * (R * 0.7f) + 0.1f, F[2] + view.rz * (-R * 0.4f) + view.uz * (R * 0.7f));
        bb(G, 0.35f * R, 0.35f * R, WaterBc.T_B_GLINT, 0.2f, w(0.7f * fade), true);
        if (!lo) {                                                              // the three spiral eddies of the still, as swirl sprites lying on the camera side of the dome
            float phi = (float) Math.atan2(-F[2], -F[0]);
            for (int i = 0; i < 3; i++) {
                float lon = i == 0 ? -0.62f : i == 1 ? 0f : 0.62f, lat = i == 1 ? 50f : 28f, sz = i == 1 ? 0.42f : 0.55f, la = (float) Math.toRadians(lat), rad = R * op * 1.03f;
                set(G, F[0] + cos(la) * cos(phi + lon) * rad, F[1] + sin(la) * rad, F[2] + cos(la) * sin(phi + lon) * rad);
                bb(G, sz * R * op, sz * R * op, WaterBc.T_B_SWIRL, age * 0.06f * (lon >= 0f ? 1f : -1f), w(0.62f * fade), false);
            }
        }
    }

    private void nestA() {
        zoneState();
        float R = p, fade = zFade, op = zOp;
        set(A, F[0], F[1] + 0.05f, F[2]);
        ground(A, R * 1.15f * op, WaterBc.T_A_RING, w(0.9f * fade), age * 0.03f, false);
        float base = (float) Math.atan2(view.rz, view.rx);
        upright(F, 2f * R * op, R * 1.02f * op, WaterBc.T_A_DOME, w(0.9f * fade));
        vplane(F, base + 0.9f, 2f * R * 0.78f * op, R * 0.98f * op, WaterBc.T_A_DOME, w(0.45f * fade));
        vplane(F, base - 0.9f, 2f * R * 0.78f * op, R * 0.98f * op, WaterBc.T_A_DOME, w(0.45f * fade));
        for (int i = 0; i < 6; i++) {
            float ang = age * 0.05f + i * TAU / 6f;
            set(G, F[0] + cos(ang) * R * 1.1f, F[1] + 0.15f * R + 0.12f * R * sin(age * 0.2f + i), F[2] + sin(ang) * R * 1.1f);
            bb(G, 0.26f * R, 0.26f * R, WaterBc.T_A_BLOB, 0f, w(0.95f * fade), false);
        }
    }

    // ================================================================================================ BURST (WATERFX_BURST: crown, ripples)
    /** the crown of horns of the burst and the splash: tongues of water with a droplet at every tip */
    private void crownHorns(boolean lo, int n, float reach, float hgt, float wid, float base, float foam) {
        float sy = Math.max(0.02f, hgt * 0.6f);
        for (int i = 0; i < n; i++) {
            float a = i * TAU / n + base;
            set(E, cos(a), 0f, sin(a));
            set(G, F[0] + E[0] * reach, F[1] + 0.02f, F[2] + E[2] * reach);
            mesh(WaterBc.P_HORN, lo, G, UP, E, true, wid * p, sy, wid * p, 0.78f, foam, WaterBcToon.PAL_TONGUE, true);
            if (hgt > 0.35f * p) {
                set(G, F[0] + E[0] * (reach + 23.2f * wid * p / 16f), F[1] + 0.02f + 27.0f * sy / 16f + 0.05f * p, F[2] + E[2] * (reach + 23.2f * wid * p / 16f));
                bubble(G, 0.2f * p, 0.95f);
            }
        }
    }

    private void burst(boolean lo) {
        float t = age / dur, fade = 1f - easeIn(t), flash = clamp(1f - age / 6f);
        if (flash > 0f) {
            set(A, F[0], F[1] + 0.6f * p, F[2]);
            float s = 2.6f * p * (0.6f + 0.4f * (1f - flash));
            bb(A, s, s, WaterBc.T_GLOW, 0f, w(flash * 0.8f), true);
        }
        float k = clamp(t * 1.4f), reach = (0.5f + 1.1f * easeOut(clamp(t * 1.4f))) * p, hgt = (0.35f + 1.25f * sin(k * PI)) * p;
        crownHorns(lo, lo ? 4 : 6, reach, hgt, 0.95f, 0.35f, 0.92f);
        float rise = easeOut(clamp(t * 1.6f)), h = (0.5f + 1.1f * sin(k * PI)) * p, base = (float) Math.atan2(view.rz, view.rx);
        vplane(F, base + 0.55f, (2.0f + 2.4f * rise) * p, h * 1.6f, WaterBc.T_B_CROWN, w(0.62f * fade));       // the glass crown that fills the gaps between the horns
        if (!lo) vplane(F, base - 0.55f, (2.0f + 2.4f * rise) * p, h * 1.6f, WaterBc.T_B_CROWN, w(0.62f * fade));
        for (int i = 0; i < 2; i++) {
            float lt = clamp(t * 1.5f - i * 0.2f);
            if (lt > 0f) {
                float rr = (0.4f + 2.2f * easeOut(lt)) * p;
                set(A, F[0], F[1] + 0.04f, F[2]);
                mesh(WaterBc.P_RIPPLE, lo, A, UP, Z1, false, rr, 1.0f * p * (1f - lt), rr, 0.6f, 0.7f, WaterBcToon.PAL_GLASS, true);
                set(A, F[0], F[1] + 0.07f + 0.01f * i, F[2]);
                ground(A, (0.4f + 2.6f * easeOut(lt)) * p, WaterBc.T_B_RIPPLE, w((1f - lt) * 0.7f), age * 0.05f + i, false);
            }
        }
        for (int i = 0; i < (lo ? 3 : 6); i++) {
            float a = hs(i, 1) * TAU, v = (1.0f + 2.2f * hs(i, 2)) * p, vy = (1.6f + 2.0f * hs(i, 3)) * p, tt = t * 1.2f;
            set(G, F[0] + cos(a) * v * tt * 0.7f, F[1] + 0.1f + vy * tt - 2.8f * tt * tt * p, F[2] + sin(a) * v * tt * 0.7f);
            bb(G, 0.2f * p * (1f - 0.4f * t), 0.26f * p * (1f - 0.4f * t), WaterBc.T_B_DROP, 0f, w(0.4f + 0.6f * fade), false);
        }
        bubblesArc(t, lo ? 4 : 8, WaterBc.T_B_BUBBLE, 0.28f, seed, w(fade), 1.6f, 3.4f, 1.0f, 3.2f);
        for (int i = 0; i < (lo ? 2 : 4); i++) {
            float a = i * 1.7f + 0.4f;
            set(G, F[0] + cos(a) * reach * 0.8f, F[1] + 0.25f * p + 0.5f * p * t, F[2] + sin(a) * reach * 0.8f);
            mist(G, 1.1f * p, 0.32f * fade, i);
        }
        set(A, F[0], F[1] + 0.8f * p, F[2]);
        bb(A, 0.6f * p, 0.6f * p, WaterBc.T_B_GLINT, age * 0.1f, w(flash), true);
    }

    private void burstA() {
        float t = age / dur, fade = 1f - easeIn(t), flash = clamp(1f - age / 6f);
        if (flash > 0f) {
            set(A, F[0], F[1] + 0.9f * p, F[2]);
            float s = 4.0f * p * (0.5f + 0.5f * (1f - flash));
            bb(A, s, s, WaterBc.T_A_FLASH, age * 0.6f, w(flash * 0.9f), true);
        }
        float k = clamp(t * 1.4f), h = (0.5f + 1.1f * sin(k * PI)) * p, rise = easeOut(clamp(t * 1.6f)), base = (float) Math.atan2(view.rz, view.rx);
        vplane(F, base + 0.55f, (2.0f + 2.4f * rise) * p, h * 1.6f, WaterBc.T_A_CROWN, w(0.95f * fade));
        vplane(F, base - 0.55f, (2.0f + 2.4f * rise) * p, h * 1.6f, WaterBc.T_A_CROWN, w(0.95f * fade));
        for (int i = 0; i < 2; i++) {
            float lt = clamp(t * 1.5f - i * 0.18f);
            if (lt > 0f) {
                set(A, F[0], F[1] + 0.07f + 0.01f * i, F[2]);
                ground(A, (0.4f + 2.6f * easeOut(lt)) * p, WaterBc.T_A_RING, w((1f - lt) * 0.95f), age * 0.05f + i, false);
            }
        }
        bubblesArc(t, 8, WaterBc.T_A_BLOB, 0.3f, seed, w(fade), 1.6f, 3.4f, 1.0f, 3.2f);
    }

    // ================================================================================================ SPLASH (WATER_SPLASH)
    private void splash(boolean lo) {
        float t = age / dur, fade = 1f - easeIn(t), k = clamp(t * 1.5f);
        float reach = (0.25f + 0.7f * easeOut(clamp(t * 1.6f))) * p, hgt = (0.25f + 0.95f * sin(k * PI)) * p;
        crownHorns(lo, lo ? 3 : 5, reach, hgt, 0.75f, 0.5f, 0.92f);
        float rise = easeOut(clamp(t * 2.0f));
        upright(F, (1.8f + 1.2f * rise) * p, (0.4f + 1.1f * sin(clamp(t * 1.5f) * PI)) * p, WaterBc.T_B_CROWN, w(0.6f * fade));
        float rr = (0.3f + 1.5f * easeOut(t)) * p;
        set(A, F[0], F[1] + 0.04f, F[2]);
        mesh(WaterBc.P_RIPPLE, lo, A, UP, Z1, false, rr, 0.8f * p * (1f - t), rr, 0.6f, 0.7f, WaterBcToon.PAL_GLASS, true);
        set(A, F[0], F[1] + 0.05f, F[2]);
        ground(A, (0.3f + 1.6f * easeOut(t)) * p, WaterBc.T_B_RIPPLE, w((1f - t) * 0.7f), age * 0.05f, false);
        for (int i = 0; i < (lo ? 2 : 4); i++) {
            float a = hs(seed + 5, i, 1) * TAU, v = (0.6f + 1.0f * hs(seed + 5, i, 2)) * p, vy = (1.2f + 1.4f * hs(seed + 5, i, 3)) * p, tt = t * 1.2f;
            set(G, F[0] + cos(a) * v * tt * 0.7f, F[1] + 0.1f + vy * tt - 2.8f * tt * tt * p, F[2] + sin(a) * v * tt * 0.7f);
            bb(G, 0.17f * p, 0.22f * p, WaterBc.T_B_DROP, 0f, w(0.4f + 0.6f * fade), false);
        }
        bubblesArc(t, lo ? 3 : 5, WaterBc.T_B_BUBBLE, 0.22f, seed + 5, w(fade), 1.2f, 2.6f, 0.6f, 1.6f);
        set(A, F[0], F[1] + 0.25f * p, F[2]);
        mist(A, 1.6f * p, 0.3f * fade, 0f);
    }

    private void splashA() {
        float t = age / dur, fade = 1f - easeIn(t), rise = easeOut(clamp(t * 2.0f));
        upright(F, (1.8f + 1.2f * rise) * p, (0.4f + 1.1f * sin(clamp(t * 1.5f) * PI)) * p, WaterBc.T_A_CROWN, w(fade));
        set(A, F[0], F[1] + 0.05f, F[2]);
        ground(A, (0.3f + 1.6f * easeOut(t)) * p, WaterBc.T_A_RING, w((1f - t) * 0.9f), age * 0.05f, false);
        bubblesArc(t, 6, WaterBc.T_A_BLOB, 0.22f, seed + 5, w(fade), 1.2f, 2.6f, 0.6f, 1.6f);
    }

    // ================================================================================================ RING (WATER_RING)
    private void ring(boolean lo) {
        int n = lo ? 2 : 3;
        for (int kk = 0; kk < n; kk++) {
            float lt = clamp(age / dur * 1.3f - kk * 0.15f), r = (0.5f + 3.0f * easeOut(lt)) * p;
            if (lt > 0f && lt < 1f) {
                set(A, F[0], F[1] + 0.02f + 0.01f * kk, F[2]);
                set(E, cos(age * 0.05f + kk), 0f, sin(age * 0.05f + kk));
                float ht = 0.9f * p * (float) Math.pow(1f - lt, 0.7);
                mesh(WaterBc.P_RIPPLE, lo, A, UP, E, false, r, ht, r, 0.55f, 0.7f, WaterBcToon.PAL_GLASS, true);
                set(A, F[0], F[1] + 0.05f + 0.01f * kk, F[2]);
                ground(A, r, WaterBc.T_B_RIPPLE, w((1f - lt) * 0.78f), age * (kk % 2 == 1 ? 0.05f : -0.05f) + kk, false);
            }
        }
        for (int i = 0; i < (lo ? 2 : 4); i++) {
            float life = frac(age * 0.05f + hs(i, 5)), a = hs(i, 4) * TAU;
            set(G, F[0] + cos(a) * 0.8f * p, F[1] + 0.1f + life * 0.8f * p, F[2] + sin(a) * 0.8f * p);
            bubble(G, 0.2f * p, sin(life * PI));
        }
    }

    private void ringA() {
        for (int kk = 0; kk < 3; kk++) {
            float lt = clamp(age / dur * 1.3f - kk * 0.15f), r = (0.5f + 3.0f * easeOut(lt)) * p;
            if (lt > 0f && lt < 1f) {
                set(A, F[0], F[1] + 0.05f + 0.01f * kk, F[2]);
                ground(A, r, WaterBc.T_A_RING, w((1f - lt) * 0.95f), age * (kk % 2 == 1 ? 0.05f : -0.05f) + kk, false);
            }
        }
    }
}
