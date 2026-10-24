package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.mana.method.RuneGeneratorUtil.Frame;

/**
 * STAGED (preview only, not wired into the game): the rune-writing look of the Mana Method, after the owner's reference frame and clarification ("actual rune symbols written in the
 * air"). Pure maths (no Minecraft class); it keeps the quaternion plane of {@link RuneGeneratorUtil#frameFacing}, so everything is drawn in the plane that faces the caster.
 *
 * <p><b>Handwriting.</b> Every symbol is a glyph of {@link RuneAlphabet} (a few straight strokes). During the charge the rune WRITES itself: a pen walks the strokes of a glyph and leaves
 * a thick glowing line (a strip quad per stroke segment, lying in the caster-facing plane: yellow core, red-orange edge); the line is brightest right behind the pen, cools to its
 * resting glow over 3 ticks and then breathes. Glyph after glyph is written round three rings: the HAND ring (6 glyphs, tight), the MIDDLE ring (9, counter-rotating) and the OUTER ring
 * (13, tilted like a bowl), then a few loose glyphs float above. Rings are written at the same time, each glyph after the one before it. A glyph is written in its own frame (right
 * = the clockwise tangent, up = outward), so it reads correctly to the caster, never mirrored (tested). Each element has its own words ({@link RuneAlphabet#phrase}): the hand ring
 * writes the phrase, the other rings mix it with the rest of the alphabet.
 *
 * <p><b>Quads</b> (preallocated in a {@link Layout}, one per rune, nothing allocated per frame): at most {@value #MAX_STRIPS} stroke strips (32 glyphs x {@value RuneAlphabet#MAX_SEGMENTS}
 * segments), {@value #MAX_PENS} pen glows, {@value #MAX_ARC_QUADS} lightning strip quads and {@value #BOOK_QUADS} book quads: {@value #MAX_QUADS} per rune; the levels of detail below that.
 */
public final class RuneGlyphLayout {
    private RuneGlyphLayout() {}

    /** The stamped-sprite alternative (an 8x4 atlas of 32x32 glyph sprites, tools/mana_method/gen_rune_glyphs.py) keeps its numbers here: cell = glyph index. */
    public static final int ATLAS_COLS = 8, ATLAS_ROWS = 4, GLYPH_COUNT = RuneAlphabet.COUNT;

    public static final int RING_COUNT = 3, MAX_FLOATING = 4, MAX_GLYPH_SLOTS = 6 + 9 + 13 + MAX_FLOATING;
    public static final int MAX_STRIPS = MAX_GLYPH_SLOTS * RuneAlphabet.MAX_SEGMENTS, MAX_PENS = 4, MAX_ARCS = 3, ARC_SEGMENTS = 7, MAX_ARC_QUADS = MAX_ARCS * ARC_SEGMENTS, BOOK_QUADS = 3;
    public static final int MAX_QUADS = MAX_STRIPS + MAX_PENS + MAX_ARC_QUADS + BOOK_QUADS;
    /** The most spark particles a rune may add to its quads per client tick (they ask the client particle budget). */
    public static final int MAX_SPARKS_PER_TICK = 3;
    /** Ticks the line behind the pen takes to cool from its writing glow to its resting glow. */
    public static final double COOL_TICKS = 3.0;
    public static final double HEAD_LIGHT = 1.7, STROKE_HALF_WIDTH = 0.16, MARGIN = 2.6;

    /** The rings: radius factor (of the rune radius), tilt (rad, about the plane's x axis), lift (of the radius, along up), turn per tick (rad), glyph size (blocks), and the written window (fractions of the charge). */
    private static final double[] RING_R = {0.34, 0.60, 0.84}, RING_TILT = {0.0, 0.0, 0.50}, RING_LIFT = {0.0, 0.0, 0.12}, RING_SPEED = {0.09, -0.05, 0.035}, RING_SIZE = {0.32, 0.42, 0.52};
    private static final double[] WIN_START = {0.03, 0.12, 0.22, 0.60}, WIN_END = {0.48, 0.66, 0.84, 0.92};
    private static final int[] RING_N = {6, 9, 13};
    private static final double FLOAT_SIZE = 0.54, ARC_WIDTH = 0.06;

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
            case LEVEL_LOW -> (n + 1) / 2;
            case LEVEL_MIN -> ring == 2 ? (n + 1) / 2 : 0;
            default -> 0;
        };
    }

    public static int floatingGlyphs(RuneGeneratorUtil.Params p, int level) {
        int n = Math.min(MAX_FLOATING, p.complexity() - 1);
        return level == LEVEL_FULL ? n : level == LEVEL_MEDIUM ? (n + 1) / 2 : 0;
    }

    public static int arcs(int level) { return level == LEVEL_FULL ? MAX_ARCS : level == LEVEL_MEDIUM ? 2 : level == LEVEL_LOW ? 1 : 0; }

    public static boolean book(int level) { return level == LEVEL_FULL; }

    /** The glyphs of one rune at a level. */
    public static int glyphsAt(RuneGeneratorUtil.Params p, int level) {
        if (level == LEVEL_NONE) return 0;
        int g = floatingGlyphs(p, level);
        for (int r = 0; r < RING_COUNT; r++) g += ringGlyphs(r, level);
        return g;
    }

    /** The most quads one rune can fill at a level (every glyph the longest it may be). */
    public static int quadsAt(RuneGeneratorUtil.Params p, int level) {
        if (level == LEVEL_NONE) return 0;
        int g = glyphsAt(p, level);
        return g * RuneAlphabet.MAX_SEGMENTS + Math.min(MAX_PENS, 3 + (floatingGlyphs(p, level) > 0 ? 1 : 0)) + arcs(level) * ARC_SEGMENTS + (book(level) ? BOOK_QUADS : 0);
    }

    // ---------------------------------------------------------------- the output
    /** One rune's quads, written in place. Every quad is 4 corners x 3 floats. */
    public static final class Layout {
        public int strips, pens, arcQuads, bookQuads, glyphs;
        /** The stroke strips (corner order: start-left, start-right, end-right, end-left; the texture runs along the stroke, across it from the left edge to the right edge) and the light of each. */
        public final float[] sq = new float[MAX_STRIPS * 12], stripLight = new float[MAX_STRIPS];
        /** Which glyph (the slot) each strip belongs to, and the ring of that slot (0..2, 3 = floating). */
        public final int[] stripGlyph = new int[MAX_STRIPS], stripRing = new int[MAX_STRIPS];
        /** The pen glows (a square in the plane round the pen's point) and their light. */
        public final float[] pq = new float[MAX_PENS * 12], penLight = new float[MAX_PENS];
        public final float[] aq = new float[MAX_ARC_QUADS * 12], arcLight = new float[MAX_ARC_QUADS];
        /** The book: 3 quads (left page, right page, glow). */
        public final float[] bq = new float[BOOK_QUADS * 12];
        public float bookLight;

        public int quads() { return strips + pens + arcQuads + bookQuads; }
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

    /** The glyph written in a slot: the hand ring writes the element's phrase; the other rings and the floating glyphs mix the phrase with the rest of the alphabet (deterministic). */
    public static int glyphFor(RuneSpec spec, int ring, int slot) {
        if (ring == 0) return RuneAlphabet.phrase(spec.element(), spec.trueMagic(), slot);
        int h = hash(spec.pack() * 31 + 17, ring * 64 + slot);
        if ((h & 1) == 0) return RuneAlphabet.phrase(spec.element(), spec.trueMagic(), slot + ring * 2);
        return (h >>> 1) % RuneAlphabet.COUNT;
    }

    /** How far the pen has got through a glyph (0..1) when the rune is {@code progress} charged: the glyph's writing starts at {@code start} and lasts {@code dur} (both fractions of the charge). */
    public static double written(double progress, double start, double dur) {
        if (dur <= 0) return progress >= start ? 1.0 : 0.0;
        double w = (progress - start) / dur;
        return w <= 0.0 ? 0.0 : Math.min(1.0, w);
    }

    /** The light of a stroke segment whose pen passed its end {@code since} ticks ago (negative: the pen is on it now): the writing glow, cooling to 1 over {@link #COOL_TICKS}. */
    public static double strokeLight(double since) {
        if (since <= 0.0) return HEAD_LIGHT;
        if (since >= COOL_TICKS) return 1.0;
        return 1.0 + (HEAD_LIGHT - 1.0) * (1.0 - since / COOL_TICKS);
    }

    /** The breathing of a finished glyph: 1 +- 12 %, each glyph a little out of step. */
    public static double breath(double age, int slot) { return 1.0 + 0.12 * Math.sin(age * 0.21 + slot * 0.9); }

    // ---------------------------------------------------------------- the writing of one glyph
    /**
     * Writes the strips of glyph {@code g} up to the pen position {@code w} (0..1 of its path) into {@code o}. The glyph's own frame in the world: centre (cx, cy, cz), half right axis
     * (rx, ry, rz), half up axis (ux, uy, uz) (so the lattice point (x, y) is at centre + right * (x - 2) / {@value #MARGIN} + up * (y - 2) / {@value #MARGIN}). {@code sinceDone} is the
     * ticks since the whole glyph was finished (negative while it is being written, measured from the writing's end), {@code dur} its writing time in ticks, {@code mul} the brightness.
     * Returns the number of strips written.
     */
    public static int write(Layout o, int g, double w, double cx, double cy, double cz, double rx, double ry, double rz, double ux, double uy, double uz, double dur, double sinceDone, double mul,
                            int slot, int ring) {
        if (w <= 0.0) return 0;
        double[] cum = RuneAlphabet.cumulative(g);
        int n = cum.length;
        double total = cum[n - 1], pen = Math.min(1.0, w) * total;
        int made = 0;
        for (int s = 0; s < n && o.strips < MAX_STRIPS; s++) {
            double from = s == 0 ? 0.0 : cum[s - 1];
            if (pen <= from) break;
            double to = cum[s];
            double t = Math.min(1.0, (pen - from) / (to - from));
            double x0 = (RuneAlphabet.coord(g, s, 0) - 2) / MARGIN, y0 = (RuneAlphabet.coord(g, s, 1) - 2) / MARGIN;
            double x1 = (RuneAlphabet.coord(g, s, 2) - 2) / MARGIN, y1 = (RuneAlphabet.coord(g, s, 3) - 2) / MARGIN;
            x1 = x0 + (x1 - x0) * t;
            y1 = y0 + (y1 - y0) * t;
            double dx = x1 - x0, dy = y1 - y0, len = Math.sqrt(dx * dx + dy * dy);
            if (len < 1e-6) continue;
            dx /= len;
            dy /= len;
            double px = -dy * STROKE_HALF_WIDTH, py = dx * STROKE_HALF_WIDTH;                 // the half width across the stroke, in the glyph's plane
            double ax = x0 - dx * STROKE_HALF_WIDTH, ay = y0 - dy * STROKE_HALF_WIDTH, bx = x1 + dx * STROKE_HALF_WIDTH, by = y1 + dy * STROKE_HALF_WIDTH;     // square caps: the joints close
            int q = o.strips * 12;
            corner(o.sq, q, cx, cy, cz, rx, ry, rz, ux, uy, uz, ax + px, ay + py);
            corner(o.sq, q + 3, cx, cy, cz, rx, ry, rz, ux, uy, uz, ax - px, ay - py);
            corner(o.sq, q + 6, cx, cy, cz, rx, ry, rz, ux, uy, uz, bx - px, by - py);
            corner(o.sq, q + 9, cx, cy, cz, rx, ry, rz, ux, uy, uz, bx + px, by + py);
            // the pen is on this segment while it has not reached its end; the finished ones cool from the moment the pen left them
            double sinceSeg = pen < to ? -1.0 : (w >= 1.0 ? sinceDone + (1.0 - to / total) * dur : (w - to / total) * dur);
            o.stripLight[o.strips] = (float) (strokeLight(sinceSeg) * mul);
            o.stripGlyph[o.strips] = slot;
            o.stripRing[o.strips] = ring;
            o.strips++;
            made++;
        }
        if (w < 1.0 && o.pens < MAX_PENS) {                                                  // the pen's glow, in the plane, on the tip of the line
            int s = 0;
            while (s < n - 1 && cum[s] < pen) s++;
            double from = s == 0 ? 0.0 : cum[s - 1], t = Math.min(1.0, (pen - from) / (cum[s] - from));
            double x = (RuneAlphabet.coord(g, s, 0) + (RuneAlphabet.coord(g, s, 2) - RuneAlphabet.coord(g, s, 0)) * t - 2) / MARGIN, y = (RuneAlphabet.coord(g, s, 1) + (RuneAlphabet.coord(g, s, 3) - RuneAlphabet.coord(g, s, 1)) * t - 2) / MARGIN;
            double h = 0.34;
            int q = o.pens * 12;
            corner(o.pq, q, cx, cy, cz, rx, ry, rz, ux, uy, uz, x - h, y + h);
            corner(o.pq, q + 3, cx, cy, cz, rx, ry, rz, ux, uy, uz, x + h, y + h);
            corner(o.pq, q + 6, cx, cy, cz, rx, ry, rz, ux, uy, uz, x + h, y - h);
            corner(o.pq, q + 9, cx, cy, cz, rx, ry, rz, ux, uy, uz, x - h, y - h);
            o.penLight[o.pens] = (float) mul;
            o.pens++;
        }
        return made;
    }

    private static void corner(float[] a, int i, double cx, double cy, double cz, double rx, double ry, double rz, double ux, double uy, double uz, double x, double y) {
        a[i] = (float) (cx + rx * x + ux * y);
        a[i + 1] = (float) (cy + ry * x + uy * y);
        a[i + 2] = (float) (cz + rz * x + uz * y);
    }

    /** One glyph on its own, upright in the plane {@code f}, centred at the plane's centre with half size {@code half}: for the previews and the tests of the writing. */
    public static void writeSingle(Layout o, int g, double w, Frame f, double half, double age) {
        o.strips = 0;
        o.pens = 0;
        o.arcQuads = 0;
        o.bookQuads = 0;
        o.glyphs = 1;
        write(o, g, w, f.cx(), f.cy(), f.cz(), f.ux() * half, f.uy() * half, f.uz() * half, f.vx() * half, f.vy() * half, f.vz() * half, 4.0, 0.0, 1.0, 0, 0);
    }

    /** The same pen in the unit glyph frame: the number of segments the pen has completed at {@code w} (for the tests). */
    public static int segmentsDone(int g, double w) {
        double[] cum = RuneAlphabet.cumulative(g);
        double pen = Math.min(1.0, Math.max(0.0, w)) * cum[cum.length - 1];
        int n = 0;
        for (double c : cum) if (pen >= c - 1e-9) n++;
        return n;
    }

    // ---------------------------------------------------------------- the whole rune
    /**
     * Fills {@code o} for a rune with the plane {@code f}, {@code age} ticks old (the charge is {@code charge} ticks; after it the rings keep turning, the glyphs breathe and the outer
     * ring flares for {@link RuneRules#FLARE_TICKS}), at a level of detail. {@code (camX, camY, camZ)} is the camera, used only to turn the lightning strips to face it.
     */
    public static void fill(Layout o, RuneSpec spec, RuneGeneratorUtil.Params p, Frame f, double age, int charge, int level, double camX, double camY, double camZ) {
        o.strips = 0;
        o.pens = 0;
        o.arcQuads = 0;
        o.bookQuads = 0;
        o.glyphs = 0;
        if (level == LEVEL_NONE) return;
        int ch = Math.max(1, charge);
        double progress = Math.max(0.0, Math.min(1.0, age / ch));
        double flare = age <= ch ? 0.0 : Math.min(1.0, (age - ch) / RuneRules.FLARE_TICKS);
        double R = p.radius();
        int floating = floatingGlyphs(p, level);
        int slot = 0, outerN = ringGlyphs(2, level);
        for (int r = 0; r < RING_COUNT; r++) {
            int n = ringGlyphs(r, level);
            double rr = R * RING_R[r] * (r == 2 ? 1.0 + 0.25 * flare : 1.0), tilt = RING_TILT[r], lift = RING_LIFT[r] * R;
            double ct = Math.cos(tilt), st = Math.sin(tilt), turn = RING_SPEED[r] * age;
            double half = RING_SIZE[r] * 0.5 * (r == 2 ? 1.0 + 0.15 * flare : 1.0);
            int full = RING_N[r], stride = n == 0 ? 1 : Math.max(1, full / n);
            double dur = (WIN_END[r] - WIN_START[r]) / Math.max(1, full) * 2.0;
            for (int i = 0; i < n; i++, slot++) {
                int pos = i * stride;
                double a = 2 * Math.PI * pos / full + turn;
                double ca = Math.cos(a), sa = Math.sin(a);
                double start = WIN_START[r] + (WIN_END[r] - WIN_START[r] - dur) * pos / Math.max(1, full - 1);
                double w = written(progress, start, dur);
                if (w <= 0.0) continue;
                // the glyph's frame on the ring: right = the clockwise tangent, up = outward; the tilt turns y into (y cos t, y sin t) about the plane's x axis
                double lx = rr * ca, ly = rr * sa * ct + lift, lz = rr * sa * st;
                double rLx = sa * half, rLy = -ca * ct * half, rLz = -ca * st * half, uLx = ca * half, uLy = sa * ct * half, uLz = sa * st * half;
                double mul = 1.0 + (r == 2 && flare > 0 ? 0.5 * (1.0 - flare) : 0.0);
                double sinceDone = progress >= start + dur ? (progress - start - dur) * ch : 0.0;
                emit(o, f, spec, r, pos, w, lx, ly, lz, rLx, rLy, rLz, uLx, uLy, uLz, dur * ch, sinceDone, mul, age, slot);
            }
        }
        // the loose glyphs above the rune, written last, bobbing and drifting a little
        int seed = spec.pack() * 31 + 17;
        double fdur = (WIN_END[3] - WIN_START[3]) / Math.max(1, floating) * 1.6;
        for (int i = 0; i < floating; i++, slot++) {
            double start = WIN_START[3] + (WIN_END[3] - WIN_START[3] - fdur) * i / Math.max(1, floating - 1);
            double w = written(progress, start, fdur);
            if (w <= 0.0) continue;
            double ox = (unit(seed, 100 + i) - 0.5) * 1.7 * R, oy = (0.95 + 0.30 * unit(seed, 200 + i)) * R + Math.sin(age * 0.11 + i * 1.7) * 0.06, oz = 0.12 * R;
            double half = FLOAT_SIZE * 0.5, wob = 0.30 * Math.sin(age * 0.07 + i), cw = Math.cos(wob) * half, sw = Math.sin(wob) * half;
            double sinceDone = progress >= start + fdur ? (progress - start - fdur) * ch : 0.0;
            emit(o, f, spec, 3, i, w, ox, oy, oz, cw, sw, 0.0, -sw, cw, 0.0, fdur * ch, sinceDone, 1.0, age, slot);
        }
        o.glyphs = slot;
        // lightning arcs from the outer ring to the hand ring (or a loose glyph): new shape every 3 ticks, present two slices out of three, only once the rings are mostly written
        int nArcs = arcs(level);
        if (progress > 0.55 && nArcs > 0 && outerN > 0) {
            int slice = (int) Math.floor(age / 3.0);
            for (int k = 0; k < nArcs; k++) {
                if (unit(seed + slice, 700 + k) < 0.33) continue;
                int a = (int) (unit(seed + slice, 710 + k) * RING_N[2]), b = (int) (unit(seed + slice, 720 + k) * RING_N[0]);
                arc(o, f, R, a, b, seed + slice * 7 + k, camX, camY, camZ, age);
            }
        }
        if (book(level) && progress > 0.08) book(o, f, R, age, Math.min(1.0, progress * 4.0));
    }

    /** Writes one ring or floating glyph (slot {@code pos} of its ring) into the layout. */
    private static void emit(Layout o, Frame f, RuneSpec spec, int ring, int pos, double w, double lx, double ly, double lz, double rLx, double rLy, double rLz, double uLx, double uLy, double uLz,
                             double durTicks, double sinceDone, double mul, double age, int slot) {
        int g = glyphFor(spec, ring, pos);
        double cx = f.cx() + f.ux() * lx + f.vx() * ly + f.nx() * lz, cy = f.cy() + f.uy() * lx + f.vy() * ly + f.ny() * lz, cz = f.cz() + f.uz() * lx + f.vz() * ly + f.nz() * lz;
        double rx = f.ux() * rLx + f.vx() * rLy + f.nx() * rLz, ry = f.uy() * rLx + f.vy() * rLy + f.ny() * rLz, rz = f.uz() * rLx + f.vz() * rLy + f.nz() * rLz;
        double ux = f.ux() * uLx + f.vx() * uLy + f.nx() * uLz, uy = f.uy() * uLx + f.vy() * uLy + f.ny() * uLz, uz = f.uz() * uLx + f.vz() * uLy + f.nz() * uLz;
        double settle = w >= 1.0 ? breath(age, slot) : 1.0;
        write(o, g, w, cx, cy, cz, rx, ry, rz, ux, uy, uz, durTicks, sinceDone, mul * settle, slot, ring);
    }

    /**
     * A jagged polyline of {@link #ARC_SEGMENTS} strip quads from position {@code a} of the outer ring to position {@code b} of the hand ring, each strip turned to face the camera.
     */
    private static void arc(Layout o, Frame f, double R, int a, int b, int seed, double camX, double camY, double camZ, double age) {
        // the two ends: a point on the outer ring and a point on the hand ring (slot index -> angle), in the plane
        ringPoint(f, R, 2, a, age, END_A);
        ringPoint(f, R, 0, b, age, END_B);
        double ax = END_A[0], ay = END_A[1], az = END_A[2], bx = END_B[0], by = END_B[1], bz = END_B[2];
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

    /** The ends of the arc being built (the render thread only: one rune is filled at a time). */
    private static final double[] END_A = new double[3], END_B = new double[3];

    /** The world point of position {@code slotIndex} on ring {@code ring} (the angle follows the ring's turn): the end of a lightning arc. */
    private static void ringPoint(Frame f, double R, int ring, int slotIndex, double age, double[] out) {
        int n = RING_N[ring];
        double a = 2 * Math.PI * (slotIndex % n) / n + RING_SPEED[ring] * age, tilt = RING_TILT[ring], rr = R * RING_R[ring], lift = RING_LIFT[ring] * R;
        double lx = rr * Math.cos(a), ly = rr * Math.sin(a) * Math.cos(tilt) + lift, lz = rr * Math.sin(a) * Math.sin(tilt);
        out[0] = f.cx() + f.ux() * lx + f.vx() * ly + f.nx() * lz;
        out[1] = f.cy() + f.uy() * lx + f.vy() * ly + f.ny() * lz;
        out[2] = f.cz() + f.uz() * lx + f.vz() * ly + f.nz() * lz;
    }

    /** The open grimoire at the caster's side (lower right of the plane as the caster sees it): two tilted pages meeting at the spine, and a glow quad behind them. */
    private static void book(Layout o, Frame f, double R, double age, double fade) {
        double bob = Math.sin(age * 0.13) * 0.04;
        double sx = 0.95 * R, sy = -0.62 * R + bob, sz = 0.28;                      // the spine's centre, local
        double pw = 0.30, ph = 0.40, lift = 0.10, tilt = 0.25;
        double ct = Math.cos(tilt), st = Math.sin(tilt);
        double ax = -st * ph, ay = ct * ph;                                         // half spine axis (local)
        for (int side = 0; side < 2; side++) {
            double dir = side == 0 ? -1.0 : 1.0;                                    // left page, right page
            double ex = dir * pw * ct, ey = dir * pw * st;                           // the page's outward axis (local, in the plane)
            int q = side * 12;
            double[][] c = {{sx - ax, sy - ay, sz}, {sx + ax, sy + ay, sz}, {sx + ax + ex, sy + ay + ey, sz + lift}, {sx - ax + ex, sy - ay + ey, sz + lift}};
            for (int k = 0; k < 4; k++) world(o.bq, q + k * 3, f, c[k][0], c[k][1], c[k][2]);
        }
        double g = 0.62;                                                             // the glow: a square plane billboard behind the book
        double[][] c = {{sx - g, sy - g, sz - 0.05}, {sx + g, sy - g, sz - 0.05}, {sx + g, sy + g, sz - 0.05}, {sx - g, sy + g, sz - 0.05}};
        for (int k = 0; k < 4; k++) world(o.bq, 24 + k * 3, f, c[k][0], c[k][1], c[k][2]);
        o.bookLight = (float) (fade * (0.85 + 0.15 * Math.sin(age * 0.3)));
        o.bookQuads = BOOK_QUADS;
    }

    private static void world(float[] a, int i, Frame f, double x, double y, double z) {
        a[i] = (float) (f.cx() + f.ux() * x + f.vx() * y + f.nx() * z);
        a[i + 1] = (float) (f.cy() + f.uy() * x + f.vy() * y + f.ny() * z);
        a[i + 2] = (float) (f.cz() + f.uz() * x + f.vz() * y + f.nz() * z);
    }
}
