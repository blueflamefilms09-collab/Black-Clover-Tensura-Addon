package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.mana.method.RuneGeneratorUtil.Frame;

/**
 * STAGED (preview only, not wired into the game): the glyph-ring look of the Mana Method rune, after the owner's reference frame (rings of readable angular rune glyphs with a bright
 * core and an orange-red outline, a tight ring round the hands, a bigger outer ring tilted like a bowl, loose glyphs floating above, yellow lightning arcs weaving through the rings, an
 * open glowing grimoire at the caster's side). Pure maths (no Minecraft class); it keeps the quaternion plane of {@link RuneGeneratorUtil#frameFacing}, so the whole figure faces the
 * caster. The output is QUADS, not particles: a glyph must be tangent-aligned and readable, which a billboard particle cannot be.
 *
 * <ul>
 *   <li>3 rings (hand 8 glyphs, middle 14, outer 20, tilted), 2 to 6 floating glyphs, up to 3 lightning arcs of 7 strip quads, one open book of 3 quads: at most
 *       {@value #MAX_QUADS} quads per rune ({@value #MAX_QUADS} x 4 vertices), and the levels of detail below that.</li>
 *   <li>The rings turn slowly against each other; the glyphs light up one after the other during the charge (hand ring first, then the middle, the outer ring, the floating glyphs),
 *       each with a short flash that settles; on the release the outer ring flares and widens.</li>
 *   <li>Everything is written into preallocated arrays of a {@link Layout} (one per rune): nothing is allocated per frame.</li>
 * </ul>
 * The glyph sprites are 32 glyphs of 32x32 in an 8x4 atlas ({@code tools/mana_method/gen_rune_glyphs.py}); {@code index} of a glyph is its cell, row major.
 */
public final class RuneGlyphLayout {
    private RuneGlyphLayout() {}

    public static final int ATLAS_COLS = 8, ATLAS_ROWS = 4, GLYPH_COUNT = ATLAS_COLS * ATLAS_ROWS;
    public static final int MAX_GLYPHS = 48, MAX_ARCS = 3, ARC_SEGMENTS = 7, MAX_ARC_QUADS = MAX_ARCS * ARC_SEGMENTS, BOOK_QUADS = 3, MAX_QUADS = MAX_GLYPHS + MAX_ARC_QUADS + BOOK_QUADS;
    /** The most spark particles a rune may add to its quads per client tick (they ask the client particle budget). */
    public static final int MAX_SPARKS_PER_TICK = 3;
    /** Ticks a glyph takes to light up, and the ticks the flash lasts. */
    public static final double LIGHT_TICKS = 3.0, FLASH_TICKS = 7.0;

    /** The rings: radius factor (of the rune radius), tilt (rad, about the plane's x axis), lift (of the radius, along up), glyph count, turn per tick (rad), glyph size (blocks). */
    private static final double[] RING_R = {0.28, 0.55, 0.80}, RING_TILT = {0.0, 0.0, 0.50}, RING_LIFT = {0.0, 0.0, 0.12}, RING_SPEED = {0.09, -0.05, 0.035}, RING_SIZE = {0.26, 0.34, 0.42};
    private static final int[] RING_N = {8, 14, 20};
    private static final double FLOAT_SIZE = 0.40, ARC_WIDTH = 0.06;

    // ---------------------------------------------------------------- levels of detail
    public static final int LEVEL_FULL = 0, LEVEL_MEDIUM = 1, LEVEL_LOW = 2, LEVEL_MIN = 3, LEVEL_NONE = -1;

    /** The level for a detail (0..1: effect quality x distance): full, no book and half the floating glyphs, half the ring glyphs, the outer ring only; nothing at 0. */
    public static int levelFor(float detail) {
        if (!(detail > 0f)) return LEVEL_NONE;
        if (detail >= 0.99f) return LEVEL_FULL;
        if (detail >= 0.7f) return LEVEL_MEDIUM;
        return detail >= 0.45f ? LEVEL_LOW : LEVEL_MIN;
    }

    /** Glyphs of ring {@code ring} at a level (0 = hand, 1 = middle, 2 = outer). */
    public static int ringGlyphs(int ring, int level) {
        int n = RING_N[ring];
        return switch (level) {
            case LEVEL_FULL, LEVEL_MEDIUM -> n;
            case LEVEL_LOW -> n / 2;
            case LEVEL_MIN -> ring == 2 ? n / 2 : 0;
            default -> 0;
        };
    }

    public static int floatingGlyphs(RuneGeneratorUtil.Params p, int level) {
        int n = Math.min(6, p.complexity() + 1);
        return level == LEVEL_FULL ? n : level == LEVEL_MEDIUM ? (n + 1) / 2 : 0;
    }

    public static int arcs(int level) { return level == LEVEL_FULL ? MAX_ARCS : level == LEVEL_MEDIUM ? 2 : level == LEVEL_LOW ? 1 : 0; }

    public static boolean book(int level) { return level == LEVEL_FULL; }

    /** The quads of one rune at a level: glyphs + arc strips + book. */
    public static int quadsAt(RuneGeneratorUtil.Params p, int level) {
        if (level == LEVEL_NONE) return 0;
        int g = floatingGlyphs(p, level);
        for (int r = 0; r < 3; r++) g += ringGlyphs(r, level);
        return g + arcs(level) * ARC_SEGMENTS + (book(level) ? BOOK_QUADS : 0);
    }

    // ---------------------------------------------------------------- the output
    /** One rune's quads, written in place. A glyph: centre, half right axis, half up axis (world vectors, so the quad is c +- right +- up), the atlas cell, its light (0 dark, 1 lit, up to 1.6 in the flash). */
    public static final class Layout {
        public int glyphs, arcQuads, bookQuads;
        public final float[] gc = new float[MAX_GLYPHS * 3], gr = new float[MAX_GLYPHS * 3], gu = new float[MAX_GLYPHS * 3], light = new float[MAX_GLYPHS];
        public final int[] cell = new int[MAX_GLYPHS], ring = new int[MAX_GLYPHS];
        /** Strip quads of the arcs: 4 corners x 3 floats each; {@code arcLight} per quad. */
        public final float[] aq = new float[MAX_ARC_QUADS * 12], arcLight = new float[MAX_ARC_QUADS];
        /** The book: 3 quads (left page, right page, glow), 4 corners x 3 floats each, and the light of the glow. */
        public final float[] bq = new float[BOOK_QUADS * 12];
        public float bookLight;

        public int quads() { return glyphs + arcQuads + bookQuads; }
    }

    // ---------------------------------------------------------------- the hash (no Random, no allocation)
    static int hash(int a, int b) {
        int h = a * 0x9E3779B1 + b * 0x85EBCA6B + 0x27D4EB2F;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        h *= 0x297A2D39;
        h ^= h >>> 15;
        return h;
    }

    /** A float in [0, 1) from two ints. */
    static double unit(int a, int b) { return (hash(a, b) >>> 8) / (double) (1 << 24); }

    /** The light of a glyph that lights at charge fraction {@code at}, when the rune is {@code progress} charged over {@code charge} ticks: dark, then a flash that settles. */
    public static double lightAt(double at, double progress, int charge) {
        double since = (progress - at) * charge;
        if (since <= 0.0) return 0.0;
        if (since < LIGHT_TICKS) return 1.6 * since / LIGHT_TICKS;
        if (since < FLASH_TICKS) return 1.6 - 0.6 * (since - LIGHT_TICKS) / (FLASH_TICKS - LIGHT_TICKS);
        return 1.0;
    }

    /**
     * Fills {@code out} for a rune with the plane {@code f}, {@code age} ticks old (the charge is {@code charge} ticks; after it the rings keep turning and the outer ring flares for
     * {@link RuneRules#FLARE_TICKS}), at a level of detail. {@code (cx, cy, cz)} is the camera, used only to turn the lightning strips to face it.
     */
    public static void fill(Layout out, RuneSpec spec, RuneGeneratorUtil.Params p, Frame f, double age, int charge, int level, double camX, double camY, double camZ) {
        out.glyphs = 0;
        out.arcQuads = 0;
        out.bookQuads = 0;
        if (level == LEVEL_NONE) return;
        double progress = Math.max(0.0, Math.min(1.0, age / Math.max(1, charge)));
        double flare = age <= charge ? 0.0 : Math.min(1.0, (age - charge) / RuneRules.FLARE_TICKS);
        double R = p.radius();
        int seed = spec.pack() * 31 + 17;
        int floating = floatingGlyphs(p, level);
        int total = floating;
        for (int r = 0; r < 3; r++) total += ringGlyphs(r, level);
        int rank = 0, ringStart2 = 0, ringStart0 = 0;
        // the hand ring lights first, then the middle, then the outer ring, then the floating glyphs
        for (int r = 0; r < 3; r++) {
            int n = ringGlyphs(r, level);
            if (r == 0) ringStart0 = out.glyphs;
            if (r == 2) ringStart2 = out.glyphs;
            double rr = R * RING_R[r] * (r == 2 ? 1.0 + 0.25 * flare : 1.0), tilt = RING_TILT[r], lift = RING_LIFT[r] * R;
            double ct = Math.cos(tilt), st = Math.sin(tilt), turn = RING_SPEED[r] * age;
            double half = RING_SIZE[r] * 0.5 * (r == 2 ? 1.0 + 0.15 * flare : 1.0);
            int full = RING_N[r], stride = n == 0 ? 1 : Math.max(1, full / n);
            for (int i = 0; i < n; i++, rank++) {
                int slot = i * stride;
                double a = 2 * Math.PI * slot / full + turn;
                double ca = Math.cos(a), sa = Math.sin(a);
                // local: position (rr ca, rr sa), outward (ca, sa), clockwise tangent (sa, -ca); the tilt turns y into (y cos t, y sin t) about the plane's x axis
                put(out, f, out.glyphs, rr * ca, rr * sa * ct + lift, rr * sa * st, sa * half, -ca * ct * half, -ca * st * half, ca * half, sa * ct * half, sa * st * half);
                double at = 0.04 + 0.78 * rank / Math.max(1, total);
                double l = lightAt(at, progress, charge);
                double since = (progress - at) * charge;
                double grow = since <= 0 ? 0.0 : Math.min(1.0, 0.6 + 0.4 * since / LIGHT_TICKS);
                scale(out, out.glyphs, grow);
                out.light[out.glyphs] = (float) (l * (r == 2 ? 1.0 + 0.5 * (1.0 - flare) * (flare > 0 ? 1 : 0) : 1.0));
                out.cell[out.glyphs] = (hash(seed, r * 64 + slot) >>> 1) % GLYPH_COUNT;
                out.ring[out.glyphs] = r;
                out.glyphs++;
            }
        }
        // the loose glyphs above the rune, bobbing and drifting a little
        for (int i = 0; i < floating; i++, rank++) {
            double ox = (unit(seed, 100 + i) - 0.5) * 1.7 * R, oy = (0.95 + 0.30 * unit(seed, 200 + i)) * R, oz = 0.12 * R;
            double bob = Math.sin(age * 0.11 + i * 1.7) * 0.06;
            double half = FLOAT_SIZE * 0.5, wob = 0.35 * Math.sin(age * 0.07 + i);
            double cw = Math.cos(wob), sw = Math.sin(wob);
            put(out, f, out.glyphs, ox, oy + bob, oz, cw * half, sw * half, 0.0, -sw * half, cw * half, 0.0);
            double at = 0.04 + 0.78 * rank / Math.max(1, total);
            double since = (progress - at) * charge;
            scale(out, out.glyphs, since <= 0 ? 0.0 : Math.min(1.0, 0.6 + 0.4 * since / LIGHT_TICKS));
            out.light[out.glyphs] = (float) lightAt(at, progress, charge);
            out.cell[out.glyphs] = (hash(seed, 500 + i) >>> 1) % GLYPH_COUNT;
            out.ring[out.glyphs] = 3;
            out.glyphs++;
        }
        // lightning arcs from the outer ring to the hand ring (or a loose glyph), new shape every 3 ticks, present two slices out of three, only once the rings are mostly lit
        int nArcs = arcs(level);
        int outerN = ringGlyphs(2, level), handN = ringGlyphs(0, level);
        if (progress > 0.35 && nArcs > 0 && outerN > 0) {
            int slice = (int) Math.floor(age / 3.0);
            for (int k = 0; k < nArcs; k++) {
                if (unit(seed + slice, 700 + k) < 0.33) continue;
                int a = ringStart2 + (int) (unit(seed + slice, 710 + k) * outerN);
                int b = handN > 0 && unit(seed + slice, 720 + k) < 0.7 ? ringStart0 + (int) (unit(seed + slice, 730 + k) * handN)
                        : (floating > 0 ? out.glyphs - floating + (int) (unit(seed + slice, 740 + k) * floating) : ringStart0);
                arc(out, a, b, seed + slice * 7 + k, camX, camY, camZ);
            }
        }
        if (book(level) && progress > 0.08) book(out, f, R, age, Math.min(1.0, progress * 4.0));
    }

    private static void scale(Layout o, int i, double s) {
        for (int k = 0; k < 3; k++) {
            o.gr[i * 3 + k] *= (float) s;
            o.gu[i * 3 + k] *= (float) s;
        }
    }

    /** Writes glyph {@code i}: local centre (x, y, z) and the local right and up half axes, turned into the world by the plane's axes. */
    private static void put(Layout o, Frame f, int i, double x, double y, double z, double rx, double ry, double rz, double ux, double uy, double uz) {
        int b = i * 3;
        o.gc[b] = (float) (f.cx() + f.ux() * x + f.vx() * y + f.nx() * z);
        o.gc[b + 1] = (float) (f.cy() + f.uy() * x + f.vy() * y + f.ny() * z);
        o.gc[b + 2] = (float) (f.cz() + f.uz() * x + f.vz() * y + f.nz() * z);
        o.gr[b] = (float) (f.ux() * rx + f.vx() * ry + f.nx() * rz);
        o.gr[b + 1] = (float) (f.uy() * rx + f.vy() * ry + f.ny() * rz);
        o.gr[b + 2] = (float) (f.uz() * rx + f.vz() * ry + f.nz() * rz);
        o.gu[b] = (float) (f.ux() * ux + f.vx() * uy + f.nx() * uz);
        o.gu[b + 1] = (float) (f.uy() * ux + f.vy() * uy + f.ny() * uz);
        o.gu[b + 2] = (float) (f.uz() * ux + f.vz() * uy + f.nz() * uz);
    }

    /** A jagged polyline of {@link #ARC_SEGMENTS} strip quads from glyph {@code a} to glyph {@code b}, each strip turned to face the camera. */
    private static void arc(Layout o, int a, int b, int seed, double camX, double camY, double camZ) {
        double ax = o.gc[a * 3], ay = o.gc[a * 3 + 1], az = o.gc[a * 3 + 2], bx = o.gc[b * 3], by = o.gc[b * 3 + 1], bz = o.gc[b * 3 + 2];
        double dx = bx - ax, dy = by - ay, dz = bz - az, len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6) return;
        double jag = Math.min(0.28, 0.12 * len + 0.04);
        double px = ax, py = ay, pz = az;
        for (int s = 1; s <= ARC_SEGMENTS && o.arcQuads < MAX_ARC_QUADS; s++) {
            double t = s / (double) ARC_SEGMENTS;
            double qx = ax + dx * t, qy = ay + dy * t, qz = az + dz * t;
            if (s < ARC_SEGMENTS) {
                double fade = Math.sin(Math.PI * t);                                   // no jitter at the two ends
                qx += (unit(seed, s * 3) - 0.5) * 2 * jag * fade;
                qy += (unit(seed, s * 3 + 1) - 0.5) * 2 * jag * fade;
                qz += (unit(seed, s * 3 + 2) - 0.5) * 2 * jag * fade;
            }
            // the strip's width direction: perpendicular to the segment and to the view
            double sx = qx - px, sy = qy - py, sz = qz - pz;
            double vx = camX - (px + qx) * 0.5, vy = camY - (py + qy) * 0.5, vz = camZ - (pz + qz) * 0.5;
            double wx = sy * vz - sz * vy, wy = sz * vx - sx * vz, wz = sx * vy - sy * vx, wl = Math.sqrt(wx * wx + wy * wy + wz * wz);
            if (wl < 1e-9) { px = qx; py = qy; pz = qz; continue; }
            double k = ARC_WIDTH * 0.5 / wl;
            wx *= k;
            wy *= k;
            wz *= k;
            int q = o.arcQuads * 12;
            o.aq[q] = (float) (px - wx); o.aq[q + 1] = (float) (py - wy); o.aq[q + 2] = (float) (pz - wz);
            o.aq[q + 3] = (float) (px + wx); o.aq[q + 4] = (float) (py + wy); o.aq[q + 5] = (float) (pz + wz);
            o.aq[q + 6] = (float) (qx + wx); o.aq[q + 7] = (float) (qy + wy); o.aq[q + 8] = (float) (qz + wz);
            o.aq[q + 9] = (float) (qx - wx); o.aq[q + 10] = (float) (qy - wy); o.aq[q + 11] = (float) (qz - wz);
            o.arcLight[o.arcQuads] = (float) (0.75 + 0.25 * unit(seed, 90 + s));
            o.arcQuads++;
            px = qx;
            py = qy;
            pz = qz;
        }
    }

    /** The open grimoire at the caster's side (lower right of the plane as the caster sees it): two tilted pages meeting at the spine, and a glow quad behind them. */
    private static void book(Layout o, Frame f, double R, double age, double fade) {
        double bob = Math.sin(age * 0.13) * 0.04;
        double sx = 0.95 * R, sy = -0.62 * R + bob, sz = 0.28;                      // the spine's centre, local
        double pw = 0.30, ph = 0.40, lift = 0.10, tilt = 0.25;
        double ct = Math.cos(tilt), st = Math.sin(tilt);
        // the spine runs along a slightly slanted "up"; the pages open to both sides and rise toward the viewer
        double ax = -st * ph, ay = ct * ph;                                         // half spine axis (local)
        double[][] page = new double[2][];
        for (int side = 0; side < 2; side++) {
            double dir = side == 0 ? -1.0 : 1.0;                                    // left page, right page
            double ex = dir * pw * ct, ey = dir * pw * st;                           // the page's outward axis (local, in the plane)
            int q = side * 12;
            double[][] c = {{sx - ax, sy - ay, sz}, {sx + ax, sy + ay, sz}, {sx + ax + ex, sy + ay + ey, sz + lift}, {sx - ax + ex, sy - ay + ey, sz + lift}};
            for (int k = 0; k < 4; k++) {
                o.bq[q + k * 3] = (float) (f.cx() + f.ux() * c[k][0] + f.vx() * c[k][1] + f.nx() * c[k][2]);
                o.bq[q + k * 3 + 1] = (float) (f.cy() + f.uy() * c[k][0] + f.vy() * c[k][1] + f.ny() * c[k][2]);
                o.bq[q + k * 3 + 2] = (float) (f.cz() + f.uz() * c[k][0] + f.vz() * c[k][1] + f.nz() * c[k][2]);
            }
        }
        double g = 0.62;                                                             // the glow: a square plane billboard behind the book
        double[][] c = {{sx - g, sy - g, sz - 0.05}, {sx + g, sy - g, sz - 0.05}, {sx + g, sy + g, sz - 0.05}, {sx - g, sy + g, sz - 0.05}};
        for (int k = 0; k < 4; k++) {
            o.bq[24 + k * 3] = (float) (f.cx() + f.ux() * c[k][0] + f.vx() * c[k][1] + f.nx() * c[k][2]);
            o.bq[24 + k * 3 + 1] = (float) (f.cy() + f.uy() * c[k][0] + f.vy() * c[k][1] + f.ny() * c[k][2]);
            o.bq[24 + k * 3 + 2] = (float) (f.cz() + f.uz() * c[k][0] + f.vz() * c[k][1] + f.nz() * c[k][2]);
        }
        o.bookLight = (float) (fade * (0.85 + 0.15 * Math.sin(age * 0.3)));
        o.bookQuads = BOOK_QUADS;
    }
}
