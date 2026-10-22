package com.newuniverse.nusmp.scalecast;

import java.util.Arrays;

/**
 * Why: Scale Magic is carried out through ONE floating balance scale beside the caster (docs/magic/scale_casting.md). This is that model as plain geometry with no Minecraft class (like
 * CompassMesh), so the game's renderer ({@code client.scalecast.ScaleRender}) and the preview tool ({@code tools/scalecast/preview_scale.py}, through {@code tools/scalecast/ScaleMeshDump.java})
 * draw exactly the same quads.
 * <p>
 * Model space: the pivot is the origin, the front faces +z, the beam runs along x (half length 1), up is +y. Parts:
 * <ul>
 *   <li>{@link #beam}: the golden beam (a turned bar with swells at the pivot and knobs at the ends) and a hook ring at each end; it turns about z, the renderer rotates it;</li>
 *   <li>{@link #pointer}: the thin gauge needle that turns with it; {@link #column}: the fixed stand with a foot, the pivot drum, the finial ring and the gauge ticks; {@link #gem}: the jewel of the pivot;</li>
 *   <li>{@link #pan}: one pan with its lugs and top ring, origin at the middle of its rim, hanging level whatever the beam does; {@link #chains}: its three chains from the top ring to the lugs
 *       (crossed link plates, so a chain reads as links at a distance for two quads a link); {@link #orb}: the unit sphere of weight the renderer scales and tints;</li>
 *   <li>{@link #beamGlow}: the beam again, for the additive pass with the etched-rune texture.</li>
 * </ul>
 * Every quad is 23 floats: four corners {x, y, z, u, v}, then the unit normal. The layers say which texture a quad uses: {@link #GOLD} {@link #ENAMEL} {@link #CHAIN} {@link #GEM} {@link #RUNES}.
 * Levels of detail: 0 full, 1 about a third of the quads, 2 a handful (a bar, a stand, two discs, three lines). Pure, deterministic, baked once.
 */
public final class ScaleMesh {
    private ScaleMesh() {}

    public static final int GOLD = 0, ENAMEL = 1, CHAIN = 2, GEM = 3, RUNES = 4, LAYERS = 5;
    public static final int STRIDE = 23;
    public static final int LODS = 3;

    /** Where the quads go: layer, the data array and the offset of the quad in it. */
    @FunctionalInterface
    public interface Sink { void quad(int layer, float[] data, int offset); }

    // ---------------------------------------------------------------- layout (shared with tools/scalecast/gen_scale_textures.py through scale_layout.json)
    public static final float BEAM_HALF = 1.0f, HOOK_X = 0.93f, HOOK_Y = -0.10f;
    /** From the top ring of a pan down to the middle of its rim. */
    public static final float CHAIN_LEN = 0.60f;
    public static final float PAN_R = 0.43f, PAN_DEPTH = 0.16f;
    public static final float COLUMN_BOTTOM = -1.32f, GAUGE_R = 0.55f, POINTER_LEN = 0.50f;
    /** Beam rows: {axial position, radius}. */
    public static final float[][] BEAM_ROWS = {
            {-1.02f, 0.00f}, {-1.00f, 0.070f}, {-0.96f, 0.100f}, {-0.90f, 0.100f}, {-0.86f, 0.055f}, {-0.55f, 0.050f}, {-0.20f, 0.062f}, {-0.12f, 0.090f}, {-0.06f, 0.110f},
            {0.06f, 0.110f}, {0.12f, 0.090f}, {0.20f, 0.062f}, {0.55f, 0.050f}, {0.86f, 0.055f}, {0.90f, 0.100f}, {0.96f, 0.100f}, {1.00f, 0.070f}, {1.02f, 0.00f}};
    /** Column rows: {height, radius}. */
    public static final float[][] COLUMN_ROWS = {
            {-1.32f, 0.00f}, {-1.30f, 0.22f}, {-1.26f, 0.26f}, {-1.20f, 0.22f}, {-1.16f, 0.10f}, {-1.10f, 0.075f}, {-0.70f, 0.058f}, {-0.30f, 0.055f}, {-0.22f, 0.075f}, {-0.18f, 0.062f}, {-0.14f, 0.105f},
            {-0.10f, 0.130f}, {-0.04f, 0.130f}};
    /** Pan bowl outside: {radius, height}; lip and inside follow. */
    public static final float[][] PAN_OUT_ROWS = {{0.00f, -0.16f}, {0.16f, -0.15f}, {0.28f, -0.11f}, {0.38f, -0.05f}, {0.43f, 0.00f}, {0.45f, 0.015f}, {0.43f, 0.03f}};
    public static final float[][] PAN_IN_ROWS = {{0.41f, 0.03f}, {0.36f, -0.02f}, {0.26f, -0.08f}, {0.12f, -0.12f}, {0.00f, -0.125f}};
    public static final int LINKS = 8;

    // ---------------------------------------------------------------- baked parts
    private static final float[] BEAM_0, BEAM_1, BEAM_2, COLUMN_0, COLUMN_1, COLUMN_2, POINTER_Q, GEM_0, GEM_1, PAN_0, PAN_1, PAN_2, ENAMEL_0, ENAMEL_1, ENAMEL_2, CHAIN_0, CHAIN_1, CHAIN_2, ORB_0, ORB_1;

    static {
        Baker b = new Baker();
        buildBeam(b, 8, 1, true);
        BEAM_0 = b.done();
        b = new Baker();
        buildBeam(b, 6, 2, false);
        BEAM_1 = b.done();
        b = new Baker();
        buildBar(b, 0.06f, 1.0f, 0f);
        BEAM_2 = b.done();

        b = new Baker();
        buildColumn(b, 10, 1, true);
        COLUMN_0 = b.done();
        b = new Baker();
        buildColumn(b, 6, 2, false);
        COLUMN_1 = b.done();
        b = new Baker();
        buildBar(b, 0.05f, 0.0f, -0.66f);
        COLUMN_2 = b.done();

        b = new Baker();
        buildPointer(b);
        POINTER_Q = b.done();

        b = new Baker();
        buildGem(b, 8);
        GEM_0 = b.done();
        b = new Baker();
        buildGem(b, 5);
        GEM_1 = b.done();

        b = new Baker();
        buildPan(b, 16, 1, true);
        PAN_0 = b.done();
        b = new Baker();
        buildPan(b, 8, 2, false);
        PAN_1 = b.done();
        b = new Baker();
        buildDisc(b, PAN_R, 8, -0.02f, 0.2f);
        PAN_2 = b.done();

        b = new Baker();
        buildPanEnamel(b, 16, 1);
        ENAMEL_0 = b.done();
        b = new Baker();
        buildPanEnamel(b, 8, 2);
        ENAMEL_1 = b.done();
        b = new Baker();
        buildDisc(b, PAN_R * 0.9f, 8, 0.0f, 0.5f);
        ENAMEL_2 = b.done();

        b = new Baker();
        buildChains(b, LINKS);
        CHAIN_0 = b.done();
        b = new Baker();
        buildChains(b, 3);
        CHAIN_1 = b.done();
        b = new Baker();
        buildChains(b, 1);
        CHAIN_2 = b.done();

        b = new Baker();
        buildSphere(b, 1.0f, 8, 5);
        ORB_0 = b.done();
        b = new Baker();
        buildSphere(b, 1.0f, 5, 3);
        ORB_1 = b.done();
    }

    // ---------------------------------------------------------------- the public parts
    /** The beam and the hook rings at its ends (layer GOLD); the caller turns it about z. */
    public static void beam(Sink s, int lod) { emit(s, GOLD, lod <= 0 ? BEAM_0 : lod == 1 ? BEAM_1 : BEAM_2); }

    /** The same quads for the additive pass with the etched-rune texture (layer RUNES); nothing at level 1 and 2. */
    public static void beamGlow(Sink s, int lod) { if (lod <= 0) emit(s, RUNES, BEAM_0); }

    /** The fixed stand: foot, shaft, pivot drum, finial ring, gauge ticks (layer GOLD). */
    public static void column(Sink s, int lod) { emit(s, GOLD, lod <= 0 ? COLUMN_0 : lod == 1 ? COLUMN_1 : COLUMN_2); }

    /** The gauge needle (layer GOLD), pointing -y, turned about z with the beam; nothing at level 2. */
    public static void pointer(Sink s, int lod) { if (lod <= 1) emit(s, GOLD, POINTER_Q); }

    /** The jewel of the pivot (layer GEM); nothing at level 2. */
    public static void gem(Sink s, int lod) { if (lod <= 0) emit(s, GEM, GEM_0); else if (lod == 1) emit(s, GEM, GEM_1); }

    /** One pan, origin at the middle of its rim: bowl, rim, lugs, top ring (GOLD) and the inside (ENAMEL). */
    public static void pan(Sink s, int lod) {
        emit(s, GOLD, lod <= 0 ? PAN_0 : lod == 1 ? PAN_1 : PAN_2);
        emit(s, ENAMEL, lod <= 0 ? ENAMEL_0 : lod == 1 ? ENAMEL_1 : ENAMEL_2);
    }

    /** Only the GOLD quads of a pan (the renderer draws one render type at a time). */
    public static void panGold(Sink s, int lod) { emit(s, GOLD, lod <= 0 ? PAN_0 : lod == 1 ? PAN_1 : PAN_2); }

    /** Only the ENAMEL quads of a pan. */
    public static void panEnamel(Sink s, int lod) { emit(s, ENAMEL, lod <= 0 ? ENAMEL_0 : lod == 1 ? ENAMEL_1 : ENAMEL_2); }

    /** The three chains of a pan, from the top ring (0, CHAIN_LEN, 0) down to the lugs (layer CHAIN). */
    public static void chains(Sink s, int lod) { emit(s, CHAIN, lod <= 0 ? CHAIN_0 : lod == 1 ? CHAIN_1 : CHAIN_2); }

    /** The unit sphere of weight (layer GEM; the renderer scales and tints it); nothing at level 2. */
    public static void orb(Sink s, int lod) { if (lod <= 0) emit(s, GEM, ORB_0); else if (lod == 1) emit(s, GEM, ORB_1); }

    /** The quads of the whole fixed-pose Scale at a level (beam, pointer, column, gem, two pans with chains, two orbs, and the glow pass): what the frame budget is charged. */
    public static int quads(int lod) {
        int l = Math.max(0, Math.min(2, lod));
        int[] beam = {BEAM_0.length, BEAM_1.length, BEAM_2.length}, col = {COLUMN_0.length, COLUMN_1.length, COLUMN_2.length}, pan = {PAN_0.length, PAN_1.length, PAN_2.length},
                en = {ENAMEL_0.length, ENAMEL_1.length, ENAMEL_2.length}, ch = {CHAIN_0.length, CHAIN_1.length, CHAIN_2.length};
        int n = beam[l] + col[l] + 2 * (pan[l] + en[l] + ch[l]);
        if (l <= 1) n += POINTER_Q.length + (l == 0 ? GEM_0.length : GEM_1.length) + 2 * (l == 0 ? ORB_0.length : ORB_1.length);
        if (l == 0) n += BEAM_0.length;                                                       // the rune pass
        return n / STRIDE;
    }

    /** A copy of one part's quads by name (tests and the preview dump): beam, beam1, beam2, column, column1, column2, pointer, gem, gem1, pan, pan1, pan2, enamel, enamel1, enamel2, chain, chain1, chain2, orb, orb1. */
    public static float[] raw(String part) {
        return switch (part) {
            case "beam" -> BEAM_0.clone();
            case "beam1" -> BEAM_1.clone();
            case "beam2" -> BEAM_2.clone();
            case "column" -> COLUMN_0.clone();
            case "column1" -> COLUMN_1.clone();
            case "column2" -> COLUMN_2.clone();
            case "pointer" -> POINTER_Q.clone();
            case "gem" -> GEM_0.clone();
            case "gem1" -> GEM_1.clone();
            case "pan" -> PAN_0.clone();
            case "pan1" -> PAN_1.clone();
            case "pan2" -> PAN_2.clone();
            case "enamel" -> ENAMEL_0.clone();
            case "enamel1" -> ENAMEL_1.clone();
            case "enamel2" -> ENAMEL_2.clone();
            case "chain" -> CHAIN_0.clone();
            case "chain1" -> CHAIN_1.clone();
            case "chain2" -> CHAIN_2.clone();
            case "orb" -> ORB_0.clone();
            case "orb1" -> ORB_1.clone();
            default -> throw new IllegalArgumentException(part);
        };
    }

    /** Names of all parts, in the order the dump writes them. */
    public static final String[] PART_NAMES = {"beam", "beam1", "beam2", "column", "column1", "column2", "pointer", "gem", "gem1", "pan", "pan1", "pan2", "enamel", "enamel1", "enamel2", "chain", "chain1", "chain2", "orb", "orb1"};

    /** The layer a part is drawn with. */
    public static int layerOf(String part) {
        if (part.startsWith("enamel")) return ENAMEL;
        if (part.startsWith("chain")) return CHAIN;
        if (part.startsWith("gem") || part.startsWith("orb")) return GEM;
        return GOLD;
    }

    private static void emit(Sink s, int layer, float[] q) {
        for (int o = 0; o + STRIDE <= q.length; o += STRIDE) s.quad(layer, q, o);
    }

    // ================================================================ builders
    private static void buildBeam(Baker b, int segs, int step, boolean hooks) {
        float[] ax = {1, 0, 0}, u = {0, 1, 0}, v = {0, 0, 1};
        int rows = BEAM_ROWS.length - 1;
        for (int i = 0; i < rows; i += step) {
            int j = Math.min(rows, i + step);
            float[] r0 = BEAM_ROWS[i], r1 = BEAM_ROWS[j];
            float v0 = (r0[0] + 1.02f) / 2.04f, v1 = (r1[0] + 1.02f) / 2.04f;
            // lathe about the x axis: the profile is (radius, axial position)
            lathe(b, 0, 0, 0, u, v, ax, r0[1], r0[0], r1[1], r1[0], v0, v1, segs);
        }
        if (hooks) {
            for (int k = -1; k <= 1; k += 2) torus(b, k * HOOK_X, HOOK_Y + 0.0f, 0f, new float[]{1, 0, 0}, new float[]{0, 1, 0}, new float[]{0, 0, 1}, 0.040f, 0.012f, 8, 4, 0.45f, 0.5f);
        } else {
            for (int k = -1; k <= 1; k += 2) box(b, new float[]{k * HOOK_X, HOOK_Y, 0f}, new float[]{1, 0, 0}, new float[]{0, 1, 0}, new float[]{0, 0, 1}, 0.014f, 0.045f, 0.014f, 0.5f, 0.5f);
        }
    }

    private static void buildColumn(Baker b, int segs, int step, boolean full) {
        float[] ax = {0, 1, 0}, u = {1, 0, 0}, v = {0, 0, 1};
        int rows = COLUMN_ROWS.length - 1;
        for (int i = 0; i < rows; i += step) {
            int j = Math.min(rows, i + step);
            float[] r0 = COLUMN_ROWS[i], r1 = COLUMN_ROWS[j];
            lathe(b, 0, 0, 0, u, v, ax, r0[1], r0[0], r1[1], r1[0], (r0[0] + 1.32f) / 1.32f, (r1[0] + 1.32f) / 1.32f, segs);
        }
        // the pivot drum: axis z, through the beam
        float[] az = {0, 0, 1}, ux = {1, 0, 0}, vy = {0, 1, 0};
        lathe(b, 0, 0, 0, ux, vy, az, 0.0f, -0.09f, 0.15f, -0.09f, 0.0f, 0.1f, segs);
        lathe(b, 0, 0, 0, ux, vy, az, 0.15f, -0.09f, 0.15f, 0.07f, 0.1f, 0.5f, segs);
        lathe(b, 0, 0, 0, ux, vy, az, 0.15f, 0.07f, 0.11f, 0.10f, 0.5f, 0.6f, segs);
        lathe(b, 0, 0, 0, ux, vy, az, 0.11f, 0.10f, 0.0f, 0.10f, 0.6f, 0.7f, segs);
        if (full) {
            // the finial ring above the pivot
            torus(b, 0f, 0.235f, 0f, new float[]{0, 0, 1}, new float[]{1, 0, 0}, new float[]{0, 1, 0}, 0.060f, 0.015f, 10, 4, 0.45f, 0.5f);
            // the gauge: nine ticks on an arc below the pivot, the middle one longer
            for (int k = -4; k <= 4; k++) {
                double a = Math.toRadians(k * 6.0);
                float sx = (float) Math.sin(a), cy = (float) -Math.cos(a);
                float[] rad = {sx, cy, 0}, tan = {-cy, sx, 0}, z = {0, 0, 1};
                float len = k == 0 ? 0.055f : 0.035f;
                box(b, new float[]{sx * GAUGE_R, cy * GAUGE_R, 0.16f}, rad, tan, z, len, 0.007f, 0.012f, 0.5f, 0.5f);
            }
        }
    }

    private static void buildPointer(Baker b) {
        // a thin rhombus plate in front of the column, from the pivot down; seen from both sides
        float z = 0.18f;
        float[] top = {0, -0.05f, z}, l = {-0.024f, -0.13f, z}, r = {0.024f, -0.13f, z}, tip = {0, -POINTER_LEN, z};
        quad(b, top, l, tip, r, 0.5f, 0.0f, 0.2f, 0.2f, 0.5f, 1.0f, 0.8f, 0.2f, 0, 0, 1);
        quad(b, top, r, tip, l, 0.5f, 0.0f, 0.8f, 0.2f, 0.5f, 1.0f, 0.2f, 0.2f, 0, 0, -1);
    }

    private static void buildGem(Baker b, int segs) {
        float[] ux = {1, 0, 0}, vy = {0, 1, 0}, az = {0, 0, 1};
        // a faceted jewel: pointed front, waist, flat back
        lathe(b, 0, 0, 0, ux, vy, az, 0.0f, 0.10f, 0.095f, 0.10f, 0.0f, 0.2f, segs);
        lathe(b, 0, 0, 0, ux, vy, az, 0.095f, 0.10f, 0.115f, 0.15f, 0.2f, 0.5f, segs);
        lathe(b, 0, 0, 0, ux, vy, az, 0.115f, 0.15f, 0.075f, 0.205f, 0.5f, 0.8f, segs);
        lathe(b, 0, 0, 0, ux, vy, az, 0.075f, 0.205f, 0.0f, 0.235f, 0.8f, 1.0f, segs);
    }

    private static void buildPan(Baker b, int segs, int step, boolean full) {
        float[] ax = {0, 1, 0}, u = {1, 0, 0}, v = {0, 0, 1};
        // the outside of the bowl and the lip: profile (radius, height) turned about y
        for (int i = 0; i + 1 < PAN_OUT_ROWS.length; i += step) {
            int j = Math.min(PAN_OUT_ROWS.length - 1, i + step);
            float[] p0 = PAN_OUT_ROWS[i], p1 = PAN_OUT_ROWS[j];
            lathe(b, 0, 0, 0, u, v, ax, p0[0], p0[1], p1[0], p1[1], i / (float) PAN_OUT_ROWS.length, j / (float) PAN_OUT_ROWS.length, segs);
        }
        if (full) {
            // three lugs on the rim, where the chains end
            for (int k = 0; k < 3; k++) lug(b, k);
            torus(b, 0f, CHAIN_LEN, 0f, new float[]{1, 0, 0}, new float[]{0, 0, 1}, new float[]{0, 1, 0}, 0.036f, 0.010f, 8, 4, 0.45f, 0.5f);
        }
    }

    private static void lug(Baker b, int k) {
        double a = Math.PI / 2 + k * 2 * Math.PI / 3;
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        float[] rad = {c, 0, s}, tan = {-s, 0, c}, up = {0, 1, 0};
        box(b, new float[]{c * PAN_R, 0.035f, s * PAN_R}, rad, tan, up, 0.014f, 0.016f, 0.028f, 0.5f, 0.5f);
    }

    private static void buildPanEnamel(Baker b, int segs, int step) {
        float[] ax = {0, 1, 0}, u = {1, 0, 0}, v = {0, 0, 1};
        for (int i = 0; i + 1 < PAN_IN_ROWS.length; i += step) {
            int j = Math.min(PAN_IN_ROWS.length - 1, i + step);
            float[] p0 = PAN_IN_ROWS[i], p1 = PAN_IN_ROWS[j];
            // the inside faces up and inward: the profile runs from the rim down to the middle, so the lathe's normal (dz, -dr) points up and inward
            lathe(b, 0, 0, 0, u, v, ax, p0[0], p0[1], p1[0], p1[1], i / (float) PAN_IN_ROWS.length, j / (float) PAN_IN_ROWS.length, segs);
        }
    }

    private static void buildDisc(Baker b, float radius, int segs, float y, float uv) {
        for (int j = 0; j < segs; j++) {
            double t0 = 2 * Math.PI * j / segs, t1 = 2 * Math.PI * (j + 1) / segs;
            float[] c = {0, y, 0}, p = {(float) Math.cos(t0) * radius, y, (float) Math.sin(t0) * radius}, q = {(float) Math.cos(t1) * radius, y, (float) Math.sin(t1) * radius};
            quad(b, c, q, p, p, uv, uv, uv, uv, uv, uv, uv, uv, 0, 1, 0);
        }
    }

    /** A thin bar along x (the beam at level 2: a box of the given half thickness, half length scale) or along y (the stand: yMid is the middle height when xScale is 0). */
    private static void buildBar(Baker b, float half, float xScale, float yMid) {
        if (xScale > 0f) {
            box(b, new float[]{0, 0, 0}, new float[]{1, 0, 0}, new float[]{0, 1, 0}, new float[]{0, 0, 1}, BEAM_HALF * xScale, half, half, 0.5f, 0.5f);
        } else {
            box(b, new float[]{0, yMid, 0}, new float[]{0, 1, 0}, new float[]{1, 0, 0}, new float[]{0, 0, 1}, 0.66f, half, half, 0.5f, 0.5f);
            box(b, new float[]{0, COLUMN_BOTTOM + 0.04f, 0}, new float[]{1, 0, 0}, new float[]{0, 0, 1}, new float[]{0, 1, 0}, 0.22f, 0.22f, 0.04f, 0.5f, 0.5f);
        }
    }

    private static void buildChains(Baker b, int links) {
        for (int k = 0; k < 3; k++) {
            double a = Math.PI / 2 + k * 2 * Math.PI / 3;
            float lx = (float) Math.cos(a) * PAN_R, lz = (float) Math.sin(a) * PAN_R;
            float[] h = {0, CHAIN_LEN, 0}, e = {lx, 0.035f, lz};
            float dx = e[0] - h[0], dy = e[1] - h[1], dz = e[2] - h[2], len = (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
            float[] d = {dx / len, dy / len, dz / len};
            // two perpendiculars: p1 horizontal across the chain, p2 completes the frame
            float[] p1 = {-d[2], 0, d[0]};
            float pl = (float) Math.sqrt(p1[0] * p1[0] + p1[2] * p1[2]);
            p1 = new float[]{p1[0] / pl, 0, p1[2] / pl};
            float[] p2 = {d[1] * p1[2] - d[2] * p1[1], d[2] * p1[0] - d[0] * p1[2], d[0] * p1[1] - d[1] * p1[0]};
            float step = len / links, half = step * 0.62f, w = links == 1 ? 0.012f : 0.017f;
            for (int i = 0; i < links; i++) {
                float t = (i + 0.5f) * step;
                float[] c = {h[0] + d[0] * t, h[1] + d[1] * t, h[2] + d[2] * t};
                float[] pa = (i % 2 == 0) ? p1 : p2, pb = (i % 2 == 0) ? p2 : p1;
                // a link: a plate along d spanning pa, and a thin plate crossing it along pb (the eye of the next link passes through)
                plate(b, c, d, pa, pb, half, w, 0.20f, 0.45f);
                plate(b, c, d, pb, pa, half * 0.8f, w * 0.7f, 0.55f, 0.8f);
            }
        }
    }

    /** A flat rectangle centred at c, half length along d, half width along w, seen from both sides (two quads); n is its normal. */
    private static void plate(Baker b, float[] c, float[] d, float[] w, float[] n, float halfLen, float halfW, float u0, float u1) {
        float[] a = pt(c, d, -halfLen, w, -halfW), bb = pt(c, d, halfLen, w, -halfW), cc = pt(c, d, halfLen, w, halfW), dd = pt(c, d, -halfLen, w, halfW);
        quad(b, a, bb, cc, dd, u0, 0.0f, u0, 1.0f, u1, 1.0f, u1, 0.0f, n[0], n[1], n[2]);
        quad(b, dd, cc, bb, a, u1, 0.0f, u1, 1.0f, u0, 1.0f, u0, 0.0f, -n[0], -n[1], -n[2]);
    }

    private static float[] pt(float[] c, float[] d, float td, float[] w, float tw) {
        return new float[]{c[0] + d[0] * td + w[0] * tw, c[1] + d[1] * td + w[1] * tw, c[2] + d[2] * td + w[2] * tw};
    }

    private static void buildSphere(Baker b, float radius, int segs, int rings) {
        float[] ux = {1, 0, 0}, vz = {0, 0, 1}, ay = {0, 1, 0};
        for (int i = 0; i < rings; i++) {
            double a0 = Math.PI * i / rings - Math.PI / 2, a1 = Math.PI * (i + 1) / rings - Math.PI / 2;
            lathe(b, 0, 0, 0, ux, vz, ay, (float) Math.cos(a0) * radius, (float) Math.sin(a0) * radius, (float) Math.cos(a1) * radius, (float) Math.sin(a1) * radius,
                    i / (float) rings, (i + 1) / (float) rings, segs);
        }
    }

    // ================================================================ primitives
    /**
     * One row of a lathe: the profile segment (r0,z0)-(r1,z1) turned about the axis N through the centre with in-plane axes U and V. The outward normal of the row is (dz, -dr) of the
     * segment (so a profile runs from the -N end to the +N end along the outside); u runs round 0..1, v from v0 to v1.
     */
    private static void lathe(Baker b, float cx, float cy, float cz, float[] u, float[] v, float[] n, float r0, float z0, float r1, float z1, float v0, float v1, int segs) {
        float dr = r1 - r0, dz = z1 - z0, l = (float) Math.hypot(dr, dz);
        if (l < 1e-6f) return;
        float nr = dz / l, nz = -dr / l;
        for (int j = 0; j < segs; j++) {
            double t0 = 2 * Math.PI * j / segs, t1 = 2 * Math.PI * (j + 1) / segs, tm = (t0 + t1) / 2;
            float[] a = lp(cx, cy, cz, u, v, n, r0, z0, t0), bb = lp(cx, cy, cz, u, v, n, r1, z1, t0), c = lp(cx, cy, cz, u, v, n, r1, z1, t1), d = lp(cx, cy, cz, u, v, n, r0, z0, t1);
            float cm = (float) Math.cos(tm), sm = (float) Math.sin(tm);
            float hx = (u[0] * cm + v[0] * sm) * nr + n[0] * nz, hy = (u[1] * cm + v[1] * sm) * nr + n[1] * nz, hz = (u[2] * cm + v[2] * sm) * nr + n[2] * nz;
            float uA = j / (float) segs, uB = (j + 1) / (float) segs;
            quad(b, a, bb, c, d, uA, v0, uA, v1, uB, v1, uB, v0, hx, hy, hz);
        }
    }

    private static float[] lp(float cx, float cy, float cz, float[] u, float[] v, float[] n, float r, float z, double t) {
        float c = (float) Math.cos(t), s = (float) Math.sin(t);
        return new float[]{cx + (u[0] * c + v[0] * s) * r + n[0] * z, cy + (u[1] * c + v[1] * s) * r + n[1] * z, cz + (u[2] * c + v[2] * s) * r + n[2] * z};
    }

    /** A torus centred at (cx,cy,cz): major radius R in the plane of u and v, minor radius r, with M ring segments and m tube segments. */
    private static void torus(Baker b, float cx, float cy, float cz, float[] n, float[] u, float[] v, float major, float minor, int segs, int tube, float uMin, float uMax) {
        for (int j = 0; j < segs; j++) {
            double t0 = 2 * Math.PI * j / segs, t1 = 2 * Math.PI * (j + 1) / segs;
            for (int k = 0; k < tube; k++) {
                double f0 = 2 * Math.PI * k / tube, f1 = 2 * Math.PI * (k + 1) / tube, fm = (f0 + f1) / 2, tm = (t0 + t1) / 2;
                float[] p0 = tp(cx, cy, cz, n, u, v, major, minor, t0, f0), p1 = tp(cx, cy, cz, n, u, v, major, minor, t1, f0),
                        p2 = tp(cx, cy, cz, n, u, v, major, minor, t1, f1), p3 = tp(cx, cy, cz, n, u, v, major, minor, t0, f1);
                float ct = (float) Math.cos(tm), st = (float) Math.sin(tm), cf = (float) Math.cos(fm), sf = (float) Math.sin(fm);
                float nx = (u[0] * ct + v[0] * st) * cf + n[0] * sf, ny = (u[1] * ct + v[1] * st) * cf + n[1] * sf, nz = (u[2] * ct + v[2] * st) * cf + n[2] * sf;
                float uu = uMin + (uMax - uMin) * j / segs, vv = k / (float) tube;
                quad(b, p0, p1, p2, p3, uu, vv, uu + 0.02f, vv, uu + 0.02f, vv + 0.2f, uu, vv + 0.2f, nx, ny, nz);
            }
        }
    }

    private static float[] tp(float cx, float cy, float cz, float[] n, float[] u, float[] v, float major, float minor, double t, double f) {
        float ct = (float) Math.cos(t), st = (float) Math.sin(t), cf = (float) Math.cos(f), sf = (float) Math.sin(f);
        float rr = major + minor * cf;
        return new float[]{cx + (u[0] * ct + v[0] * st) * rr + n[0] * minor * sf, cy + (u[1] * ct + v[1] * st) * rr + n[1] * minor * sf, cz + (u[2] * ct + v[2] * st) * rr + n[2] * minor * sf};
    }

    /** An oriented box centred at c with the half sizes (hr, ht, hz) along the basis (rad, tan, z); every face shows the texture patch around (u, v). */
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
                data[o++] = Math.max(0f, Math.min(1f, uv[2 * k])); data[o++] = Math.max(0f, Math.min(1f, uv[2 * k + 1]));
            }
            data[o++] = nx / l; data[o++] = ny / l; data[o++] = nz / l;
            len = o;
        }

        float[] done() { return Arrays.copyOf(data, len); }
    }
}
