package com.newuniverse.nusmp.compass3d;

import java.util.Arrays;

/**
 * Why: the owner asked for ONE floating 3D compass beside the Compass Magic caster, like the anime's, and for every spell to come out of it as a 3D model. This is that model as plain
 * geometry with no Minecraft class (like DemonSlayerMesh), so the game's renderer ({@code client.compass.CompassRender}) and the preview tool ({@code tools/compass/preview_compass3d.py},
 * through {@code tools/compass/MeshDump.java}) draw exactly the same quads.
 * <p>
 * Model space: the compass has radius 1 (one block at scale 1), its front faces +z, the disc lies in the xy plane, north is +y. A thick olive-bronze rim (a lathe of
 * {@code compass3d_layout.json}'s rim rows: back dome, bevels, 16 engraved notches, a bright front lip, a gold glass seat), four lugs, a recessed green dial, a raised 8 point compass rose
 * (every point two facets, lit from the upper left), a glass dome, and a needle (diamond shaft, hub cap, a small ring at the tail) that {@link #needle} draws pointing +y at angle 0.
 * The same file also builds the 3D things the spells throw out of it: {@link #arrow} (an arrow-needle along +z with a tail ring and four fins), {@link #bar} and {@link #chevron}
 * (the segments and arrow heads of a navigation route).
 * <p>
 * Every quad is 23 floats: four corners {x, y, z, u, v}, then the unit normal. Layers say which texture the quad uses: {@link #RIM} {@link #FACE} {@link #STAR} {@link #NEEDLE}
 * {@link #GLASS}. The numbers marked "layout" are the ones of {@code tools/compass/compass3d_layout.json}, which {@code gen_compass3d_textures.py} also reads ({@code CompassMeshTest} checks
 * the two agree). Pure, deterministic, built once.
 */
public final class CompassMesh {
    private CompassMesh() {}

    public static final int RIM = 0, FACE = 1, STAR = 2, NEEDLE = 3, GLASS = 4, LAYERS = 5;
    public static final int STRIDE = 23;

    /** Where the quads go: layer, the data array and the offset of the quad in it. */
    @FunctionalInterface
    public interface Sink { void quad(int layer, float[] data, int offset); }

    // ---------------------------------------------------------------- layout (shared with compass3d_layout.json)
    public static final int SEGS = 40;
    public static final float FACE_R = 0.82f, FACE_Z = 0.0f;
    /** Rim profile rows: {r0, z0, r1, z1, v0, v1}; row 0..2 is the back dome, 3..8 the rim from the back outward and round to the glass seat. */
    public static final float[][] RIM_ROWS = {
            {0.00f, -0.26f, 0.45f, -0.245f, 0.00f, 0.06f},
            {0.45f, -0.245f, 0.78f, -0.21f, 0.06f, 0.12f},
            {0.78f, -0.21f, 0.96f, -0.18f, 0.12f, 0.18f},
            {0.96f, -0.18f, 1.00f, -0.14f, 0.18f, 0.24f},
            {1.00f, -0.14f, 1.00f, 0.08f, 0.24f, 0.55f},
            {1.00f, 0.08f, 0.96f, 0.125f, 0.55f, 0.64f},
            {0.96f, 0.125f, 0.90f, 0.125f, 0.64f, 0.74f},
            {0.90f, 0.125f, 0.84f, 0.09f, 0.74f, 0.84f},
            {0.84f, 0.09f, 0.82f, 0.00f, 0.84f, 1.00f}};
    public static final float STAR_VALLEY_R = 0.22f, STAR_HUB_Z = 0.085f, STAR_VALLEY_Z = 0.02f, STAR_TIP_Z = 0.03f, STAR_SHADOW_Z = 0.008f, STAR_SHADOW_GROW = 1.10f;
    public static final float[] STAR_TIP_R = {0.80f, 0.50f, 0.72f, 0.50f, 0.72f, 0.50f, 0.72f, 0.50f};
    public static final float[] TEX_U_LIGHT = {0.02f, 0.38f}, TEX_U_DARK = {0.42f, 0.78f}, TEX_U_OUTLINE = {0.82f, 0.98f};
    public static final float NEEDLE_TIP = 0.78f, NEEDLE_TAIL = 0.56f, NEEDLE_SHOULDER_HALF = 0.062f, NEEDLE_SHOULDER_Z = 0.115f, NEEDLE_RIDGE_Z = 0.165f, NEEDLE_TIP_Z = 0.125f,
            NEEDLE_RING_Y = -0.64f, NEEDLE_RING_OUTER = 0.085f, NEEDLE_RING_INNER = 0.05f, NEEDLE_RING_Z0 = 0.10f, NEEDLE_RING_Z1 = 0.14f, NEEDLE_HUB_R = 0.055f;
    public static final float GLASS_EDGE_Z = 0.10f, GLASS_DOME_H = 0.09f;
    // not shared
    private static final float[] LIGHT_DIR = {-0.5f, 0.8f};

    // ---------------------------------------------------------------- baked parts
    private static final float[] BODY_RIM, BODY_FACE, BODY_STAR, NEEDLE_Q, GLASS_Q, ARROW_Q, BAR_Q, CHEVRON_Q;
    /**
     * 1.112.0 (lag audit, docs/perf/lag_audit_1.md): the same parts at fewer segments for the compasses that are far from the camera. Level 0 is the model above (the arrays before this change,
     * unchanged); level 1 keeps every part with a third of the segments; level 2 is only the dial disc (the caller adds a ring card). The parts are baked once, like the full ones.
     */
    private static final float[] RIM_1, FACE_1, STAR_1, NEEDLE_1, FACE_2, ARROW_1;
    /** The number of levels of detail ({@link #body(Sink, int)} and friends take 0 .. LODS - 1). */
    public static final int LODS = 3;

    static {
        Baker rim = new Baker(), face = new Baker(), star = new Baker();
        buildRim(rim, SEGS, true);
        buildFace(face, 4, SEGS);
        buildStar(star);
        BODY_RIM = rim.done();
        BODY_FACE = face.done();
        BODY_STAR = star.done();
        Baker n = new Baker();
        buildNeedle(n, 14, 16);
        NEEDLE_Q = n.done();
        Baker g = new Baker();
        buildGlass(g, 4, 32);
        GLASS_Q = g.done();
        Baker a = new Baker();
        buildArrow(a, true);
        ARROW_Q = a.done();
        Baker r1 = new Baker(), f1 = new Baker(), s1 = new Baker(), n1 = new Baker(), f2 = new Baker(), a1 = new Baker();
        buildRim(r1, 16, true);
        buildFace(f1, 2, 16);
        buildStar(s1);
        buildNeedle(n1, 8, 8);
        buildFace(f2, 1, 16);
        buildArrow(a1, false);
        RIM_1 = r1.done();
        FACE_1 = f1.done();
        STAR_1 = s1.done();
        NEEDLE_1 = n1.done();
        FACE_2 = f2.done();
        ARROW_1 = a1.done();
        Baker b = new Baker();
        buildBar(b);
        BAR_Q = b.done();
        Baker c = new Baker();
        buildChevron(c);
        CHEVRON_Q = c.done();
    }

    // ---------------------------------------------------------------- the public parts
    /** The rim, back dome and lugs (layer RIM), the dial (FACE) and the compass rose (STAR). */
    public static void body(Sink s) { body(s, 0); }

    /** The same at a level of detail (0 full, 1 a third of the segments, 2 only the dial disc: no rim, no rose). */
    public static void body(Sink s, int lod) {
        if (lod <= 0) {
            emit(s, RIM, BODY_RIM);
            emit(s, FACE, BODY_FACE);
            emit(s, STAR, BODY_STAR);
        } else if (lod == 1) {
            emit(s, RIM, RIM_1);
            emit(s, FACE, FACE_1);
            emit(s, STAR, STAR_1);
        } else {
            emit(s, FACE, FACE_2);
        }
    }

    /** Only the quads of one layer (RIM, FACE or STAR) of the body at a level of detail: the renderer draws the layers one render type at a time and used to walk all three arrays for each. */
    public static void bodyLayer(Sink s, int lod, int layer) {
        float[] q;
        if (lod <= 0) q = layer == RIM ? BODY_RIM : layer == FACE ? BODY_FACE : layer == STAR ? BODY_STAR : null;
        else if (lod == 1) q = layer == RIM ? RIM_1 : layer == FACE ? FACE_1 : layer == STAR ? STAR_1 : null;
        else q = layer == FACE ? FACE_2 : null;
        if (q != null) emit(s, layer, q);
    }

    /** The needle (layer NEEDLE), pointing +y; the caller rotates it about z. */
    public static void needle(Sink s) { needle(s, 0); }

    /** The needle at a level of detail (nothing at level 2). */
    public static void needle(Sink s, int lod) {
        if (lod <= 0) emit(s, NEEDLE, NEEDLE_Q);
        else if (lod == 1) emit(s, NEEDLE, NEEDLE_1);
    }

    /** The glass dome (layer GLASS). */
    public static void glass(Sink s) { glass(s, 0); }

    /** The glass dome at a level of detail (only at level 0: from level 1 on it is a faint sheet on a thing too far to see through, and a translucent draw call of its own). */
    public static void glass(Sink s, int lod) {
        if (lod <= 0) emit(s, GLASS, GLASS_Q);
    }

    /** A flying arrow-needle along +z, tip at z = 1.0, tail ring near z = -0.72 (layer NEEDLE). */
    public static void arrow(Sink s) { arrow(s, 0); }

    /** The arrow-needle at a level of detail: 0 the whole model, 1 head, shaft and fins without the tail ring and neck, nothing from 2 (the caller draws only its light streak). */
    public static void arrow(Sink s, int lod) {
        if (lod <= 0) emit(s, NEEDLE, ARROW_Q);
        else if (lod == 1) emit(s, NEEDLE, ARROW_1);
    }

    /** The quads the solid passes of body + needle + glass cost at this level of detail (what the frame budget is charged). */
    public static int compassQuads(int lod, boolean needle, boolean glass) {
        if (lod <= 0) return bodyQuads() + (needle ? quads(NEEDLE_Q) : 0) + (glass ? quads(GLASS_Q) : 0);
        if (lod == 1) return quads(RIM_1) + quads(FACE_1) + quads(STAR_1) + (needle ? quads(NEEDLE_1) : 0);
        return quads(FACE_2);
    }

    /** The quads of the arrow-needle model at this level of detail. */
    public static int arrowQuads(int lod) { return lod <= 0 ? quads(ARROW_Q) : lod == 1 ? quads(ARROW_1) : 0; }

    /** The quads of the additive glow passes (dial lines, rose, needle) at this level of detail: level 1 keeps only the dial lines, level 2 none. */
    public static int glowQuads(int lod, boolean needle) {
        if (lod <= 0) return quads(BODY_FACE) + quads(BODY_STAR) + (needle ? quads(NEEDLE_Q) : 0);
        if (lod == 1) return quads(FACE_1);
        return 0;
    }

    /** A unit segment of a route, along +z from 0 to 1, half width 0.5 (layer NEEDLE). */
    public static void bar(Sink s) { emit(s, NEEDLE, BAR_Q); }

    /** A unit arrow head of a route: a four sided pyramid, base at z = 0 (half width 0.5), tip at z = 1 (layer NEEDLE). */
    public static void chevron(Sink s) { emit(s, NEEDLE, CHEVRON_Q); }

    public static int quads(float[] part) { return part.length / STRIDE; }
    public static int bodyQuads() { return quads(BODY_RIM) + quads(BODY_FACE) + quads(BODY_STAR); }
    public static int needleQuads() { return quads(NEEDLE_Q); }
    public static int glassQuads() { return quads(GLASS_Q); }
    public static int arrowQuads() { return quads(ARROW_Q); }
    /** Every part's raw data (a copy), for tests and the preview dump: "rim", "face", "star", "needle", "glass", "arrow", "bar", "chevron". */
    public static float[] raw(String part) {
        return switch (part) {
            case "rim" -> BODY_RIM.clone();
            case "face" -> BODY_FACE.clone();
            case "star" -> BODY_STAR.clone();
            case "needle" -> NEEDLE_Q.clone();
            case "glass" -> GLASS_Q.clone();
            case "arrow" -> ARROW_Q.clone();
            case "bar" -> BAR_Q.clone();
            case "chevron" -> CHEVRON_Q.clone();
            default -> throw new IllegalArgumentException(part);
        };
    }

    private static void emit(Sink s, int layer, float[] q) {
        for (int o = 0; o + STRIDE <= q.length; o += STRIDE) s.quad(layer, q, o);
    }

    // ================================================================ builders
    private static void buildRim(Baker b, int segs, boolean lugs) {
        float[] u = {1, 0, 0}, v = {0, 1, 0}, n = {0, 0, 1};
        for (float[] r : RIM_ROWS) lathe(b, 0, 0, 0, u, v, n, r[0], r[1], r[2], r[3], r[4], r[5], segs, 0f, 1f);
        if (!lugs) return;
        // four lugs on the outer wall (north is the larger crown lug)
        for (int k = 0; k < 4; k++) {
            double a = Math.PI / 2 - k * Math.PI / 2;
            float c = (float) Math.cos(a), s = (float) Math.sin(a);
            float[] rad = {c, s, 0}, tan = {-s, c, 0}, z = {0, 0, 1};
            float grow = k == 0 ? 1.35f : 1f;
            box(b, new float[]{c * 1.045f, s * 1.045f, -0.03f}, rad, tan, z, 0.05f * grow, 0.075f * grow, 0.085f, 0.30f, 0.52f);
        }
    }

    private static void buildFace(Baker b, int rings, int segs) {
        disc(b, FACE_R, rings, segs, (r, th) -> FACE_Z);
    }

    private static void buildGlass(Baker b, int rings, int segs) {
        disc(b, FACE_R, rings, segs, (r, th) -> {
            float q = r / FACE_R;
            return GLASS_EDGE_Z + GLASS_DOME_H * (1 - q * q);
        });
    }

    private static void buildStar(Baker b) {
        // an outline underlay a little larger than the star, then 8 points of 2 facets each
        float[][] tip = new float[8][], val = new float[8][];
        for (int i = 0; i < 8; i++) {
            double a = Math.PI / 2 - i * Math.PI / 4;                       // clockwise from north
            tip[i] = new float[]{(float) Math.cos(a), (float) Math.sin(a)};
            double av = a + Math.PI / 8;                       // the valley between tip i-1 and tip i (counter-clockwise of tip i)
            val[i] = new float[]{(float) Math.cos(av), (float) Math.sin(av)};
        }
        float uo = (TEX_U_OUTLINE[0] + TEX_U_OUTLINE[1]) / 2;
        for (int i = 0; i < 8; i++) {
            float tr = STAR_TIP_R[i] * STAR_SHADOW_GROW, vr = STAR_VALLEY_R * STAR_SHADOW_GROW;
            float[] t = {tip[i][0] * tr, tip[i][1] * tr, STAR_SHADOW_Z}, vl = {val[i][0] * vr, val[i][1] * vr, STAR_SHADOW_Z};
            int j = (i + 1) % 8;
            float[] t2 = {tip[j][0] * STAR_TIP_R[j] * STAR_SHADOW_GROW, tip[j][1] * STAR_TIP_R[j] * STAR_SHADOW_GROW, STAR_SHADOW_Z};
            float[] c = {0, 0, STAR_SHADOW_Z};
            // fan: centre, the valley before the tip, the tip ; centre, the tip, the valley after it (the valley between tip i and tip i+1 is val[(i+1)%8])
            tri(b, c, vl, t, uo, 0.1f, uo, 0.5f, uo, 0.9f, 0, 0, 1);
            float[] vl2 = {val[j][0] * vr, val[j][1] * vr, STAR_SHADOW_Z};
            tri(b, c, t, vl2, uo, 0.1f, uo, 0.9f, uo, 0.5f, 0, 0, 1);
        }
        for (int i = 0; i < 8; i++) {
            float tr = STAR_TIP_R[i];
            float[] t = {tip[i][0] * tr, tip[i][1] * tr, STAR_TIP_Z};
            float[] bl = {val[i][0] * STAR_VALLEY_R, val[i][1] * STAR_VALLEY_R, STAR_VALLEY_Z};
            int j = (i + 1) % 8;
            float[] br = {val[j][0] * STAR_VALLEY_R, val[j][1] * STAR_VALLEY_R, STAR_VALLEY_Z};
            float[] h = {0, 0, STAR_HUB_Z};
            facet(b, h, t, bl);
            facet(b, h, br, t);
        }
    }

    /** One star facet (triangle hub-tip-corner), light or dark by which way it leans (lit from the upper left). */
    private static void facet(Baker b, float[] h, float[] t, float[] corner) {
        float[] nrm = normal(h, t, corner);
        if (nrm[2] < 0) { nrm[0] = -nrm[0]; nrm[1] = -nrm[1]; nrm[2] = -nrm[2]; }
        boolean light = nrm[0] * LIGHT_DIR[0] + nrm[1] * LIGHT_DIR[1] > 0;
        float[] r = light ? TEX_U_LIGHT : TEX_U_DARK;
        float um = (r[0] + r[1]) / 2;
        tri(b, h, t, corner, um, 0.0f, um, 1.0f, light ? r[0] : r[1], 0.30f, nrm[0], nrm[1], nrm[2]);
    }

    private static void buildNeedle(Baker b, int hubSegs, int ringSegs) {
        float sh = NEEDLE_SHOULDER_HALF;
        float[] tip = {0, NEEDLE_TIP, NEEDLE_TIP_Z}, tail = {0, -NEEDLE_TAIL, NEEDLE_TIP_Z * 0.95f}, c = {0, 0, NEEDLE_RIDGE_Z};
        float[] sl = {-sh, 0, NEEDLE_SHOULDER_Z}, sr = {sh, 0, NEEDLE_SHOULDER_Z};
        needleFacet(b, tip, c, sl);
        needleFacet(b, tip, sr, c);
        needleFacet(b, tail, sl, c);
        needleFacet(b, tail, c, sr);
        // the hub cap and the small ring at the tail
        float[] u = {1, 0, 0}, v = {0, 1, 0}, n = {0, 0, 1};
        float[][] hub = {{NEEDLE_HUB_R, 0.10f, NEEDLE_HUB_R, 0.145f, 0.2f, 0.45f}, {NEEDLE_HUB_R, 0.145f, 0.035f, 0.172f, 0.45f, 0.7f}, {0.035f, 0.172f, 0f, 0.18f, 0.7f, 0.9f}};
        for (float[] r : hub) lathe(b, 0, 0, 0, u, v, n, r[0], r[1], r[2], r[3], r[4], r[5], hubSegs, TEX_U_LIGHT[0], TEX_U_LIGHT[1]);
        float ro = NEEDLE_RING_OUTER, ri = NEEDLE_RING_INNER;
        float[][] ring = {{ro, NEEDLE_RING_Z0, ro, NEEDLE_RING_Z1, 0.2f, 0.4f}, {ro, NEEDLE_RING_Z1, ri, NEEDLE_RING_Z1, 0.4f, 0.6f}, {ri, NEEDLE_RING_Z1, ri, NEEDLE_RING_Z0, 0.6f, 0.8f}};
        for (float[] r : ring) lathe(b, 0, NEEDLE_RING_Y, 0, u, v, n, r[0], r[1], r[2], r[3], r[4], r[5], ringSegs, TEX_U_LIGHT[0], TEX_U_LIGHT[1]);
    }

    private static void needleFacet(Baker b, float[] a, float[] bb, float[] c) {
        float[] nrm = normal(a, bb, c);
        if (nrm[2] < 0) { nrm[0] = -nrm[0]; nrm[1] = -nrm[1]; nrm[2] = -nrm[2]; }
        boolean light = nrm[0] * LIGHT_DIR[0] + nrm[1] * LIGHT_DIR[1] > 0;
        float[] r = light ? TEX_U_LIGHT : TEX_U_DARK;
        float um = (r[0] + r[1]) / 2;
        tri(b, a, bb, c, um, 0.0f, um, 0.5f, um, 1.0f, nrm[0], nrm[1], nrm[2]);
    }

    /** The flying arrow-needle (along +z): a four sided head, a tapering shaft, a ring at the tail, four fins. */
    private static void buildArrow(Baker b, boolean tail) {
        float hh = 0.065f, hs = 0.045f, hs2 = 0.028f;
        float zb = 0.25f, zt = 1.0f, ze = -0.62f;
        float[][] base = {{-hh, -hh, zb}, {hh, -hh, zb}, {hh, hh, zb}, {-hh, hh, zb}};
        float[] tip = {0, 0, zt};
        for (int i = 0; i < 4; i++) {
            float[] a = base[i], c = base[(i + 1) % 4];
            float[] nrm = normal(a, c, tip);
            float cx = (a[0] + c[0] + tip[0]) / 3, cy = (a[1] + c[1] + tip[1]) / 3;
            if (nrm[0] * cx + nrm[1] * cy < 0) { nrm[0] = -nrm[0]; nrm[1] = -nrm[1]; nrm[2] = -nrm[2]; }
            boolean light = nrm[0] * LIGHT_DIR[0] + nrm[1] * LIGHT_DIR[1] > 0;
            float[] r = light ? TEX_U_LIGHT : TEX_U_DARK;
            float um = (r[0] + r[1]) / 2;
            tri(b, a, c, tip, um, 0.0f, um, 0.0f, um, 1.0f, nrm[0], nrm[1], nrm[2]);
        }
        // the shaft: a square frustum from the head's base to the tail
        float[][] top = {{-hs, -hs, zb}, {hs, -hs, zb}, {hs, hs, zb}, {-hs, hs, zb}}, bot = {{-hs2, -hs2, ze}, {hs2, -hs2, ze}, {hs2, hs2, ze}, {-hs2, hs2, ze}};
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            float[] nrm = normal(top[i], top[j], bot[j]);
            float cx = top[i][0] + top[j][0], cy = top[i][1] + top[j][1];
            if (nrm[0] * cx + nrm[1] * cy < 0) { nrm[0] = -nrm[0]; nrm[1] = -nrm[1]; nrm[2] = -nrm[2]; }
            boolean light = nrm[0] * LIGHT_DIR[0] + nrm[1] * LIGHT_DIR[1] > 0;
            float[] r = light ? TEX_U_LIGHT : TEX_U_DARK;
            float um = (r[0] + r[1]) / 2;
            quad(b, top[i], top[j], bot[j], bot[i], um, 0.0f, um, 0.0f, um, 1.0f, um, 1.0f, nrm[0], nrm[1], nrm[2]);
        }
        // four fins: flat triangles in the planes through the axis, both faces
        float fz0 = -0.18f, fz1 = -0.56f, fr = 0.20f;
        for (int k = 0; k < 4; k++) {
            double a = k * Math.PI / 2 + Math.PI / 4;
            float c = (float) Math.cos(a), s = (float) Math.sin(a);
            float[] p0 = {c * 0.03f, s * 0.03f, fz0}, p1 = {c * 0.03f, s * 0.03f, fz1}, p2 = {c * fr, s * fr, fz1};
            float[] nrm = {-s, c, 0};
            float um = (TEX_U_LIGHT[0] + TEX_U_LIGHT[1]) / 2;
            tri(b, p0, p1, p2, um, 0.0f, um, 0.6f, um, 1.0f, nrm[0], nrm[1], nrm[2]);
            tri(b, p0, p2, p1, um, 0.0f, um, 1.0f, um, 0.6f, -nrm[0], -nrm[1], -nrm[2]);
        }
        if (!tail) return;                                              // the level of detail without the tail ring and its neck
        // the small ring at the tail, in the plane through the axis (axis of the ring along x)
        float[] ru = {0, 1, 0}, rv = {0, 0, 1}, rn = {1, 0, 0};
        float ro = 0.115f, ri = 0.065f, rz = 0.022f;
        float[][] ring = {{ro, -rz, ro, rz, 0.2f, 0.4f}, {ro, rz, ri, rz, 0.4f, 0.6f}, {ri, rz, ri, -rz, 0.6f, 0.8f}};
        for (float[] r : ring) lathe(b, 0, 0, ze - 0.10f, ru, rv, rn, r[0], r[1], r[2], r[3], r[4], r[5], 14, TEX_U_LIGHT[0], TEX_U_LIGHT[1]);
        // the neck from the shaft's end to the ring
        float um = (TEX_U_DARK[0] + TEX_U_DARK[1]) / 2;
        box(b, new float[]{0, 0, ze - 0.02f}, new float[]{1, 0, 0}, new float[]{0, 1, 0}, new float[]{0, 0, 1}, 0.014f, 0.014f, 0.03f, um, 0.4f);
    }

    private static void buildBar(Baker b) { frustum(b, 0.5f, 0.5f, 0f, 1f); }

    private static void buildChevron(Baker b) {
        float[][] base = {{-0.5f, -0.5f, 0}, {0.5f, -0.5f, 0}, {0.5f, 0.5f, 0}, {-0.5f, 0.5f, 0}};
        float[] tip = {0, 0, 1};
        for (int i = 0; i < 4; i++) {
            float[] a = base[i], c = base[(i + 1) % 4];
            float[] nrm = normal(a, c, tip);
            if (nrm[0] * (a[0] + c[0]) + nrm[1] * (a[1] + c[1]) < 0) { nrm[0] = -nrm[0]; nrm[1] = -nrm[1]; nrm[2] = -nrm[2]; }
            boolean light = nrm[0] * LIGHT_DIR[0] + nrm[1] * LIGHT_DIR[1] > 0;
            float[] r = light ? TEX_U_LIGHT : TEX_U_DARK;
            float um = (r[0] + r[1]) / 2;
            tri(b, a, c, tip, um, 0.0f, um, 0.0f, um, 1.0f, nrm[0], nrm[1], nrm[2]);
        }
        quad(b, base[3], base[2], base[1], base[0], 0.2f, 0.5f, 0.2f, 0.5f, 0.2f, 0.5f, 0.2f, 0.5f, 0, 0, -1);
    }

    private static void frustum(Baker b, float h0, float h1, float z0, float z1) {
        float[][] a = {{-h0, -h0, z0}, {h0, -h0, z0}, {h0, h0, z0}, {-h0, h0, z0}}, c = {{-h1, -h1, z1}, {h1, -h1, z1}, {h1, h1, z1}, {-h1, h1, z1}};
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) % 4;
            float[] nrm = normal(a[i], a[j], c[j]);
            if (nrm[0] * (a[i][0] + a[j][0]) + nrm[1] * (a[i][1] + a[j][1]) < 0) { nrm[0] = -nrm[0]; nrm[1] = -nrm[1]; nrm[2] = -nrm[2]; }
            boolean light = nrm[0] * LIGHT_DIR[0] + nrm[1] * LIGHT_DIR[1] > 0;
            float[] r = light ? TEX_U_LIGHT : TEX_U_DARK;
            float um = (r[0] + r[1]) / 2;
            quad(b, a[i], a[j], c[j], c[i], um, 0.2f, um, 0.2f, um, 0.8f, um, 0.8f, nrm[0], nrm[1], nrm[2]);
        }
        quad(b, c[0], c[1], c[2], c[3], 0.2f, 0.5f, 0.2f, 0.5f, 0.2f, 0.5f, 0.2f, 0.5f, 0, 0, 1);
    }

    // ================================================================ primitives
    @FunctionalInterface private interface Height { float at(float r, float theta); }

    /** A planar-mapped disc (u = 0.5 + x / 2R, v = 0.5 - y / 2R), 'rings' rings of 'segs' segments, height by the function. */
    private static void disc(Baker b, float radius, int rings, int segs, Height h) {
        for (int i = 0; i < rings; i++) {
            float r0 = radius * i / rings, r1 = radius * (i + 1) / rings;
            for (int j = 0; j < segs; j++) {
                float t0 = (float) (2 * Math.PI * j / segs), t1 = (float) (2 * Math.PI * (j + 1) / segs);
                float[] p0 = pt(r0, t0, h, radius), p1 = pt(r1, t0, h, radius), p2 = pt(r1, t1, h, radius), p3 = pt(r0, t1, h, radius);
                // normal from the slope: use the quad itself, oriented to +z
                float[] n = normal(p0, p1, p2);
                if (n[2] < 0) { n[0] = -n[0]; n[1] = -n[1]; n[2] = -n[2]; }
                float[] n3 = normal(p0, p2, p3);
                if (n3[2] < 0) { n3[0] = -n3[0]; n3[1] = -n3[1]; n3[2] = -n3[2]; }
                float nx = (n[0] + n3[0]) / 2, ny = (n[1] + n3[1]) / 2, nz = (n[2] + n3[2]) / 2, l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
                // a ring at radius 0 collapses to a point: the quad is a triangle, fine
                quad(b, p0, p1, p2, p3, p0[3], p0[4], p1[3], p1[4], p2[3], p2[4], p3[3], p3[4], nx / l, ny / l, nz / l);
            }
        }
    }

    private static float[] pt(float r, float theta, Height h, float radius) {
        float x = (float) (r * Math.sin(theta)), y = (float) (r * Math.cos(theta));
        return new float[]{x, y, h.at(r, theta), 0.5f + x / (2 * radius), 0.5f - y / (2 * radius)};
    }

    /**
     * One row of a lathe: the profile segment (r0,z0)-(r1,z1) turned about the axis N through the centre (cx,cy,cz) with in-plane axes U and V. The outward normal of the row is
     * (dz, -dr) of the segment; u runs round from uMin to uMax, v from v0 to v1.
     */
    private static void lathe(Baker b, float cx, float cy, float cz, float[] u, float[] v, float[] n, float r0, float z0, float r1, float z1, float v0, float v1, int segs, float uMin, float uMax) {
        float dr = r1 - r0, dz = z1 - z0, l = (float) Math.hypot(dr, dz);
        if (l < 1e-6f) return;
        float nr = dz / l, nz = -dr / l;
        for (int j = 0; j < segs; j++) {
            double t0 = 2 * Math.PI * j / segs, t1 = 2 * Math.PI * (j + 1) / segs, tm = (t0 + t1) / 2;
            float[] a = lp(cx, cy, cz, u, v, n, r0, z0, t0), bb = lp(cx, cy, cz, u, v, n, r1, z0 + dz, t0), c = lp(cx, cy, cz, u, v, n, r1, z0 + dz, t1), d = lp(cx, cy, cz, u, v, n, r0, z0, t1);
            float cm = (float) Math.cos(tm), sm = (float) Math.sin(tm);
            float hx = (u[0] * cm + v[0] * sm) * nr + n[0] * nz, hy = (u[1] * cm + v[1] * sm) * nr + n[1] * nz, hz = (u[2] * cm + v[2] * sm) * nr + n[2] * nz;
            float uA = uMin + (uMax - uMin) * j / segs, uB = uMin + (uMax - uMin) * (j + 1) / segs;
            quad(b, a, bb, c, d, uA, v0, uA, v1, uB, v1, uB, v0, hx, hy, hz);
        }
    }

    private static float[] lp(float cx, float cy, float cz, float[] u, float[] v, float[] n, float r, float z, double t) {
        float c = (float) Math.cos(t), s = (float) Math.sin(t);
        return new float[]{cx + (u[0] * c + v[0] * s) * r + n[0] * z, cy + (u[1] * c + v[1] * s) * r + n[1] * z, cz + (u[2] * c + v[2] * s) * r + n[2] * z};
    }

    /** An oriented box centred at c with the half sizes (hr, ht, hz) along the basis (rad, tan, z); every face shows the texture patch u in [u, u + 0.06], v in [v, v + 0.2]. */
    private static void box(Baker b, float[] c, float[] rad, float[] tan, float[] z, float hr, float ht, float hz, float u, float v) {
        float[][] axes = {rad, tan, z};
        float[] half = {hr, ht, hz};
        for (int ax = 0; ax < 3; ax++) {
            for (int sign = -1; sign <= 1; sign += 2) {
                int a1 = (ax + 1) % 3, a2 = (ax + 2) % 3;
                float[] base = {c[0] + axes[ax][0] * half[ax] * sign, c[1] + axes[ax][1] * half[ax] * sign, c[2] + axes[ax][2] * half[ax] * sign};
                float[][] p = new float[4][];
                int[][] corner = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
                for (int k = 0; k < 4; k++)
                    p[k] = new float[]{base[0] + axes[a1][0] * half[a1] * corner[k][0] + axes[a2][0] * half[a2] * corner[k][1],
                            base[1] + axes[a1][1] * half[a1] * corner[k][0] + axes[a2][1] * half[a2] * corner[k][1],
                            base[2] + axes[a1][2] * half[a1] * corner[k][0] + axes[a2][2] * half[a2] * corner[k][1]};
                float[] nrm = {axes[ax][0] * sign, axes[ax][1] * sign, axes[ax][2] * sign};
                quad(b, p[0], p[1], p[2], p[3], u, v, u + 0.06f, v, u + 0.06f, v + 0.2f, u, v + 0.2f, nrm[0], nrm[1], nrm[2]);
            }
        }
    }

    private static float[] normal(float[] a, float[] b, float[] c) {
        float ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2], vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
        float nx = uy * vz - uz * vy, ny = uz * vx - ux * vz, nz = ux * vy - uy * vx, l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        return l < 1e-9f ? new float[]{0, 0, 1} : new float[]{nx / l, ny / l, nz / l};
    }

    private static void tri(Baker b, float[] a, float[] bb, float[] c, float ua, float va, float ub, float vb, float uc, float vc, float nx, float ny, float nz) {
        quad(b, a, bb, c, c, ua, va, ub, vb, uc, vc, uc, vc, nx, ny, nz);
    }

    private static void quad(Baker b, float[] a, float[] bb, float[] c, float[] d, float ua, float va, float ub, float vb, float uc, float vc, float ud, float vd, float nx, float ny, float nz) {
        b.add(a, ua, va, bb, ub, vb, c, uc, vc, d, ud, vd, nx, ny, nz);
    }

    /** Growable float store of baked quads. */
    private static final class Baker {
        float[] data = new float[STRIDE * 64];
        int len;

        void add(float[] a, float ua, float va, float[] b, float ub, float vb, float[] c, float uc, float vc, float[] d, float ud, float vd, float nx, float ny, float nz) {
            if (len + STRIDE > data.length) data = Arrays.copyOf(data, data.length * 2);
            float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (l < 1e-9f) { nx = 0; ny = 0; nz = 1; l = 1; }
            float[][] p = {a, b, c, d};
            float[] uv = {ua, va, ub, vb, uc, vc, ud, vd};
            int o = len;
            for (int k = 0; k < 4; k++) {
                data[o++] = p[k][0]; data[o++] = p[k][1]; data[o++] = p[k][2];
                data[o++] = uv[2 * k]; data[o++] = uv[2 * k + 1];
            }
            data[o++] = nx / l; data[o++] = ny / l; data[o++] = nz / l;
            len = o;
        }

        float[] done() { return Arrays.copyOf(data, len); }
    }
}
