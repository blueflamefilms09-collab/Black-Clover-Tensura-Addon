package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.mana.method.RuneGeneratorUtil.Frame;

/**
 * The look of the Mana Method rune: ACTUAL RUNE SYMBOLS WRITTEN IN THE AIR, after the owner's reference frame (several rings of readable angular runes with a bright yellow core and an orange-red
 * edge, a tight ring round the hands, a bigger tilted outer ring, thin glowing circle lines, loose glyphs above, yellow lightning, an open glowing grimoire at the caster's side). Pure maths (no
 * Minecraft class: the client layer {@code vfx.client.layer.RuneWritingLayer} draws what is computed here, the tests and the previews read the same arrays); it keeps the quaternion plane of
 * {@link RuneGeneratorUtil#frameFacing}, so everything is drawn in the plane that faces the caster.
 *
 * <p><b>Handwriting.</b> Every symbol is a glyph of {@link RuneAlphabet} (a few straight strokes). During the charge the rune WRITES itself: a pen walks the strokes of a glyph and leaves
 * a thick glowing line (a strip quad per stroke segment, lying in the plane: yellow core, red-orange edge); the line is brightest right behind the pen, cools to its resting glow over 3
 * ticks and then breathes. Glyph after glyph is written round three rings that fill their circumference evenly: the HAND ring (10 glyphs, tight), the MIDDLE ring (16, counter-rotating) and
 * the OUTER ring (24, tilted like a bowl), a thin continuous glowing line under each ring and a second thin boundary line outside the outer ring, then a few loose glyphs float above.
 * Rings are written at the same time, several glyphs of a ring being under the pen at once. A glyph is written in its own frame (right = the clockwise tangent, up = outward), so it reads
 * correctly to the caster, never mirrored (tested). Each element has its own words ({@link RuneAlphabet#phrase}).
 *
 * <p><b>Quads</b> (preallocated in a {@link Layout}, nothing allocated per frame): at most {@value #MAX_STRIPS} stroke strips (54 glyphs x {@value RuneAlphabet#MAX_SEGMENTS} segments),
 * {@value #MAX_PENS} pen glows, {@value #MAX_LINES} circle lines, {@value #MAX_ARC_QUADS} lightning strips and 1 glow behind the book: {@value #MAX_QUADS} per rune, 1,200 vertices; fewer at the
 * lower levels of detail. The open grimoire itself is the mod's real book model, drawn by the client through the item renderer at {@link #bookPos}.
 */
public final class RuneGlyphLayout {
    private RuneGlyphLayout() {}

    public static final int RING_COUNT = 3, MAX_FLOATING = 4, MAX_GLYPH_SLOTS = 10 + 16 + 24 + MAX_FLOATING;
    public static final int MAX_STRIPS = MAX_GLYPH_SLOTS * RuneAlphabet.MAX_SEGMENTS, MAX_PENS = 4, MAX_LINES = 4, MAX_ARCS = 3, ARC_SEGMENTS = 7, MAX_ARC_QUADS = MAX_ARCS * ARC_SEGMENTS, GLOW_QUADS = 1;
    public static final int MAX_QUADS = MAX_STRIPS + MAX_PENS + MAX_LINES + MAX_ARC_QUADS + GLOW_QUADS;
    /** The most spark particles a rune may add to its quads per client tick (they ask the client particle budget). */
    public static final int MAX_SPARKS_PER_TICK = 3;
    /** Ticks the line behind the pen takes to cool from its writing glow to its resting glow. */
    public static final double COOL_TICKS = 3.0;
    public static final double HEAD_LIGHT = 1.7, STROKE_HALF_WIDTH = 0.16, MARGIN = 2.6, LINE_LIGHT = 0.6;
    /** How much of the ring's circumference a glyph's width takes: neighbours nearly touch. */
    public static final double FILL = 0.92;

    /** The rings: radius factor (of the rune radius), tilt (rad, about the plane's x axis), lift (of the radius, along up), turn per tick (rad), and the written window (fractions of the charge). */
    private static final double[] RING_R = {0.30, 0.58, 0.86}, RING_TILT = {0.0, 0.0, 0.50}, RING_LIFT = {0.0, 0.0, 0.12}, RING_SPEED = {0.09, -0.05, 0.035};
    private static final double[] WIN_START = {0.03, 0.12, 0.22, 0.60}, WIN_END = {0.48, 0.66, 0.84, 0.92};
    public static final int[] RING_N = {10, 16, 24};
    /** Several glyphs of a ring are under the pen at once: a glyph takes this many times its share of the ring's window. */
    private static final double OVERLAP = 4.0;
    private static final double FLOAT_SIZE = 0.50, ARC_WIDTH = 0.06;

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

    /** The circle lines at a level (line 0 = the hand ring's, 1 = the middle's, 2 = the outer ring's, 3 = the boundary): all four, then the outer ones only. */
    public static int lines(int level) { return level == LEVEL_FULL || level == LEVEL_MEDIUM ? MAX_LINES : level == LEVEL_LOW ? 2 : level == LEVEL_MIN ? 1 : 0; }

    /** The real grimoire and its glow: only at full detail. */
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
        return g * RuneAlphabet.MAX_SEGMENTS + Math.min(MAX_PENS, 3 + (floatingGlyphs(p, level) > 0 ? 1 : 0)) + lines(level) + arcs(level) * ARC_SEGMENTS + (book(level) ? GLOW_QUADS : 0);
    }

    // ---------------------------------------------------------------- the output
    /** One rune's quads, written in place. Every quad is 4 corners x 3 floats. */
    public static final class Layout {
        public int strips, pens, lines, arcQuads, glyphs;
        /** The stroke strips (corner order: start-left, start-right, end-right, end-left; across the stroke first, then along it) and the light of each. */
        public final float[] sq = new float[MAX_STRIPS * 12], stripLight = new float[MAX_STRIPS];
        /** Which glyph (the slot) each strip belongs to, and the ring of that slot (0..2, 3 = floating). */
        public final int[] stripGlyph = new int[MAX_STRIPS], stripRing = new int[MAX_STRIPS];
        /** The pen glows (a square in the plane round the pen's point, its centre in {@code penPos}) and their light. */
        public final float[] pq = new float[MAX_PENS * 12], penLight = new float[MAX_PENS], penPos = new float[MAX_PENS * 3];
        /** The circle lines: a square in the ring's plane whose texture is a thin circle at {@link #LINE_RADIUS_IN_TEXTURE} of the half width. */
        public final float[] lq = new float[MAX_LINES * 12], lineLight = new float[MAX_LINES];
        public final float[] aq = new float[MAX_ARC_QUADS * 12], arcLight = new float[MAX_ARC_QUADS];
        /** The open grimoire: where it hovers (world), and how lit it is (0 = not there). */
        public double bookX, bookY, bookZ;
        public float bookLight;

        public int quads() { return strips + pens + lines + arcQuads + (bookLight > 0f ? GLOW_QUADS : 0); }
    }

    /** The circle of the line texture sits at this share of the half width of its quad. */
    public static final double LINE_RADIUS_IN_TEXTURE = 0.92;

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
     * ticks since the whole glyph was finished, {@code dur} its writing time in ticks, {@code mul} the brightness. Returns the number of strips written.
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
            corner(o.penPos, o.pens * 3, cx, cy, cz, rx, ry, rz, ux, uy, uz, x, y);
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

    private static void reset(Layout o) {
        o.strips = 0;
        o.pens = 0;
        o.lines = 0;
        o.arcQuads = 0;
        o.glyphs = 0;
        o.bookLight = 0f;
    }

    /** One glyph on its own, upright in the plane {@code f}, centred at the plane's centre with half size {@code half}: for the previews and the tests of the writing. */
    public static void writeSingle(Layout o, int g, double w, Frame f, double half, double age) {
        reset(o);
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

    // ---------------------------------------------------------------- the geometry of the rings
    /** The radius of ring {@code r} of a rune of radius {@code R}. */
    public static double ringRadius(int r, double R) { return R * RING_R[r]; }

    /** The size of a glyph of ring {@code r}: its width takes {@link #FILL} of the circumference share it has, so neighbours nearly touch. */
    public static double glyphSize(int r, double R) { return FILL * 2 * Math.PI * ringRadius(r, R) / RING_N[r]; }

    // ---------------------------------------------------------------- the whole rune
    /**
     * Fills {@code o} for a rune with the plane {@code f}, {@code age} ticks old (the charge is {@code charge} ticks; after it the rings keep turning, the glyphs breathe and the outer
     * ring flares for {@link RuneRules#FLARE_TICKS}), at a level of detail. {@code (camX, camY, camZ)} is the camera, used only to turn the lightning strips to face it.
     */
    public static void fill(Layout o, RuneSpec spec, RuneGeneratorUtil.Params p, Frame f, double age, int charge, int level, double camX, double camY, double camZ) {
        reset(o);
        if (level == LEVEL_NONE) return;
        int ch = Math.max(1, charge);
        double progress = Math.max(0.0, Math.min(1.0, age / ch));
        double flare = age <= ch ? 0.0 : Math.min(1.0, (age - ch) / RuneRules.FLARE_TICKS);
        for (int k = 0; k < RING_GROUPS; k++) { LEGACY_PROG[k] = progress; LEGACY_SCALE[k] = ch; }
        fillCore(o, spec, p, f, age, LEGACY_PROG, LEGACY_SCALE, flare, level, camX, camY, camZ, false);
    }

    /** The four groups that are written separately: the hand ring, the middle ring, the outer ring, the loose glyphs. */
    public static final int RING_GROUPS = 4;
    private static final double[] LEGACY_PROG = new double[RING_GROUPS], LEGACY_SCALE = new double[RING_GROUPS];

    /** The layout progress at which group {@code group} (0 hand ring, 1 middle ring, 2 outer ring, 3 loose glyphs) has done {@code written} (0..1) of its work: a point in its window of the timeline. */
    public static double progressOf(int group, double written) {
        double w = Math.max(0.0, Math.min(1.0, written));
        return WIN_START[group] + w * (WIN_END[group] - WIN_START[group]);
    }

    /** The inverse of {@link #progressOf}: how much (0..1) of group {@code group}'s work the layout progress {@code progress} stands for. */
    public static double writtenOf(int group, double progress) {
        double span = WIN_END[group] - WIN_START[group];
        double w = (progress - WIN_START[group]) / span;
        return Double.isNaN(w) ? 0.0 : Math.max(0.0, Math.min(1.0, w));
    }

    /**
     * The same picture driven by a HOLD (the charge of a held page): every group has its own progress {@code prog[g]} (see {@link #progressOf}) and its own ticks per unit of progress
     * {@code scale[g]} (how long its pens take), so the rings can be written one after the other as the player holds, stop where the player released, and only the tiers that were reached
     * are completed. {@code age} drives the turning and the breathing, {@code flare} (0..1) the release flare of the outer ring. Allocation free like {@link #fill}.
     */
    public static void fillStaged(Layout o, RuneSpec spec, RuneGeneratorUtil.Params p, Frame f, double age, double[] prog, double[] scale, double flare, int level, double camX, double camY, double camZ) {
        reset(o);
        if (level == LEVEL_NONE) return;
        fillCore(o, spec, p, f, age, prog, scale, Math.max(0.0, Math.min(1.0, flare)), level, camX, camY, camZ, true);
    }

    /**
     * {@code staged}: the picture of a HELD rune (3.6.0). Only there does the LIGHTNING element's rune grow its arcs with the rings that are written ({@link LightningCharge}); the fixed rune
     * of the 3.4.0 cast (and so hold-to-charge off) draws the arcs exactly as before.
     */
    private static void fillCore(Layout o, RuneSpec spec, RuneGeneratorUtil.Params p, Frame f, double age, double[] prog, double[] scale, double flare, int level, double camX, double camY, double camZ, boolean staged) {
        double R = p.radius();
        int floating = floatingGlyphs(p, level);
        int slot = 0, outerN = ringGlyphs(2, level);
        for (int r = 0; r < RING_COUNT; r++) {
            int n = ringGlyphs(r, level);
            double rr = R * RING_R[r] * (r == 2 ? 1.0 + 0.25 * flare : 1.0), tilt = RING_TILT[r], lift = RING_LIFT[r] * R;
            double ct = Math.cos(tilt), st = Math.sin(tilt), turn = RING_SPEED[r] * age;
            double half = glyphSize(r, R) * 0.5 * (r == 2 ? 1.0 + 0.25 * flare : 1.0);
            int full = RING_N[r], stride = n == 0 ? 1 : Math.max(1, full / n);
            if (stride > 1) half *= Math.min(1.8, 0.5 + 0.5 * stride);                         // a thinned ring (a lower level of detail) writes bigger glyphs to keep the circle filled
            double dur = (WIN_END[r] - WIN_START[r]) / Math.max(1, full) * OVERLAP;
            for (int i = 0; i < n; i++, slot++) {
                int pos = i * stride;
                double a = 2 * Math.PI * pos / full + turn;
                double ca = Math.cos(a), sa = Math.sin(a);
                double start = WIN_START[r] + (WIN_END[r] - WIN_START[r] - dur) * pos / Math.max(1, full - 1);
                double w = written(prog[r], start, dur);
                if (w <= 0.0) continue;
                // the glyph's frame on the ring: right = the clockwise tangent, up = outward; the tilt turns y into (y cos t, y sin t) about the plane's x axis
                double lx = rr * ca, ly = rr * sa * ct + lift, lz = rr * sa * st;
                double rLx = sa * half, rLy = -ca * ct * half, rLz = -ca * st * half, uLx = ca * half, uLy = sa * ct * half, uLz = sa * st * half;
                double mul = 1.0 + (r == 2 && flare > 0 ? 0.5 * (1.0 - flare) : 0.0);
                double sinceDone = prog[r] >= start + dur ? (prog[r] - start - dur) * scale[r] : 0.0;
                emit(o, f, spec, r, pos, w, lx, ly, lz, rLx, rLy, rLz, uLx, uLy, uLz, dur * scale[r], sinceDone, mul, age, slot);
            }
            // the thin circle line under the ring (at the inner foot of its glyphs), drawn itself with the ring's window
            if (n > 0 && r >= MAX_LINES - lines(level)) circle(o, f, rr - half, tilt, lift, Math.min(1.0, Math.max(0.0, (prog[r] - WIN_START[r]) / 0.12)) * LINE_LIGHT * (1.0 + 0.15 * Math.sin(age * 0.17 + r)));
        }
        // the second thin boundary line outside the outer ring
        if (outerN > 0 && lines(level) > 0) {
            double rr = R * RING_R[2] * (1.0 + 0.25 * flare), half = glyphSize(2, R) * 0.5 * (1.0 + 0.25 * flare);
            circle(o, f, rr + half * 1.2, RING_TILT[2], RING_LIFT[2] * R, Math.min(1.0, Math.max(0.0, (prog[2] - WIN_START[2]) / 0.12)) * LINE_LIGHT * 0.8);
        }
        // the loose glyphs above the rune, written last, bobbing and drifting a little; spread evenly across the width, each at its own height
        int seed = spec.pack() * 31 + 17;
        double fdur = (WIN_END[3] - WIN_START[3]) / Math.max(1, floating) * 1.6;
        for (int i = 0; i < floating; i++, slot++) {
            double start = WIN_START[3] + (WIN_END[3] - WIN_START[3] - fdur) * i / Math.max(1, floating - 1);
            double w = written(prog[3], start, fdur);
            if (w <= 0.0) continue;
            double across = floating == 1 ? 0.0 : (i / (double) (floating - 1) - 0.5) * 2.0;
            double ox = across * 1.05 * R + (unit(seed, 100 + i) - 0.5) * 0.25 * R, oy = (1.12 + 0.18 * unit(seed, 200 + i) + 0.10 * (i % 2)) * R + Math.sin(age * 0.11 + i * 1.7) * 0.06, oz = 0.12 * R;
            double half = FLOAT_SIZE * 0.5 * Math.max(0.7, R / 1.5), wob = 0.30 * Math.sin(age * 0.07 + i), cw = Math.cos(wob) * half, sw = Math.sin(wob) * half;
            double sinceDone = prog[3] >= start + fdur ? (prog[3] - start - fdur) * scale[3] : 0.0;
            emit(o, f, spec, 3, i, w, ox, oy, oz, cw, sw, 0.0, -sw, cw, 0.0, fdur * scale[3], sinceDone, 1.0, age, slot);
        }
        o.glyphs = slot;
        // lightning arcs from the outer ring to the hand ring: new shape every 3 ticks, present two slices out of three, only once the rings are mostly written
        int nArcs = arcs(level);
        if (staged && spec.element() == RuneElement.LIGHTNING) {
            // the lightning rune of a hold: one arc per ring written (hand, middle, outer), starting on the outermost written ring, thicker, wilder and brighter with each ring
            int reached = LightningCharge.arcsReached(writtenOf(0, prog[0]), writtenOf(1, prog[1]), writtenOf(2, prog[2]));          // the progress is a point of the timeline: the arcs ask how much of each ring is WRITTEN (0..1)
            int n = Math.min(nArcs, reached);
            if (n > 0) {
                int slice = (int) Math.floor(age / 3.0), from = LightningCharge.startRing(reached);
                for (int k = 0; k < n; k++) {
                    if (unit(seed + slice, 700 + k) < 0.33) continue;
                    int a = (int) (unit(seed + slice, 710 + k) * RING_N[from]), b = (int) (unit(seed + slice, 720 + k) * RING_N[0]);
                    if (from == 0 && a == b) b = (b + 4) % RING_N[0];
                    arc(o, f, R, from, a, b, seed + slice * 7 + k, camX, camY, camZ, age, LightningCharge.arcWidth(reached), LightningCharge.arcJag(reached), LightningCharge.arcLight(reached));
                }
            }
        } else if (prog[2] > 0.55 && nArcs > 0 && outerN > 0) {
            int slice = (int) Math.floor(age / 3.0);
            for (int k = 0; k < nArcs; k++) {
                if (unit(seed + slice, 700 + k) < 0.33) continue;
                int a = (int) (unit(seed + slice, 710 + k) * RING_N[2]), b = (int) (unit(seed + slice, 720 + k) * RING_N[0]);
                arc(o, f, R, 2, a, b, seed + slice * 7 + k, camX, camY, camZ, age, 1.0, 1.0, 1.0);
            }
        }
        if (book(level) && prog[0] > 0.08) {
            double[] at = BOOK;
            bookPos(f, R, age, at);
            o.bookX = at[0];
            o.bookY = at[1];
            o.bookZ = at[2];
            o.bookLight = (float) (Math.min(1.0, prog[0] * 4.0) * (0.85 + 0.15 * Math.sin(age * 0.3)));
        }
    }

    private static final double[] BOOK = new double[3];

    /** Where the open grimoire hovers: at the caster's side (lower right of the plane as the caster sees it), bobbing; (x, y, z) in the world. */
    public static void bookPos(Frame f, double R, double age, double[] out) {
        double lx = 0.95 * R, ly = -0.62 * R + Math.sin(age * 0.13) * 0.04, lz = 0.28;
        out[0] = f.cx() + f.ux() * lx + f.vx() * ly + f.nx() * lz;
        out[1] = f.cy() + f.uy() * lx + f.vy() * ly + f.ny() * lz;
        out[2] = f.cz() + f.uz() * lx + f.vz() * ly + f.nz() * lz;
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

    /** A circle line: a square in the ring's plane (tilted about the plane's x axis, lifted along up) whose texture holds the thin circle at {@link #LINE_RADIUS_IN_TEXTURE} of the half width. */
    private static void circle(Layout o, Frame f, double radius, double tilt, double lift, double light) {
        if (o.lines >= MAX_LINES || radius <= 0.02 || light <= 0.01) return;
        double e = radius / LINE_RADIUS_IN_TEXTURE, ct = Math.cos(tilt), st = Math.sin(tilt);
        int q = o.lines * 12;
        // corners: bottom-left, bottom-right, top-right, top-left of the square (local x right, local y up before the tilt)
        double[][] c = {{-e, -e}, {e, -e}, {e, e}, {-e, e}};
        for (int k = 0; k < 4; k++) {
            double lx = c[k][0], ly = c[k][1] * ct + lift, lz = c[k][1] * st;
            o.lq[q + k * 3] = (float) (f.cx() + f.ux() * lx + f.vx() * ly + f.nx() * lz);
            o.lq[q + k * 3 + 1] = (float) (f.cy() + f.uy() * lx + f.vy() * ly + f.ny() * lz);
            o.lq[q + k * 3 + 2] = (float) (f.cz() + f.uz() * lx + f.vz() * ly + f.nz() * lz);
        }
        o.lineLight[o.lines] = (float) light;
        o.lines++;
    }

    /** A jagged polyline of {@link #ARC_SEGMENTS} strip quads from position {@code a} of the outer ring to position {@code b} of the hand ring, each strip turned to face the camera. */
    private static void arc(Layout o, Frame f, double R, int fromRing, int a, int b, int seed, double camX, double camY, double camZ, double age, double widthMult, double jagMult, double lightMult) {
        ringPoint(f, R, fromRing, a, age, END_A);
        ringPoint(f, R, 0, b, age, END_B);
        double ax = END_A[0], ay = END_A[1], az = END_A[2], bx = END_B[0], by = END_B[1], bz = END_B[2];
        double dx = bx - ax, dy = by - ay, dz = bz - az, len = Math.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 1e-6) return;
        double jag = Math.min(0.28 * jagMult, (0.12 * len + 0.04) * jagMult);
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
            double k = ARC_WIDTH * widthMult * 0.5 / wl;
            wx *= k;
            wy *= k;
            wz *= k;
            int q = o.arcQuads * 12;
            o.aq[q] = (float) (px - wx); o.aq[q + 1] = (float) (py - wy); o.aq[q + 2] = (float) (pz - wz);
            o.aq[q + 3] = (float) (px + wx); o.aq[q + 4] = (float) (py + wy); o.aq[q + 5] = (float) (pz + wz);
            o.aq[q + 6] = (float) (qx + wx); o.aq[q + 7] = (float) (qy + wy); o.aq[q + 8] = (float) (qz + wz);
            o.aq[q + 9] = (float) (qx - wx); o.aq[q + 10] = (float) (qy - wy); o.aq[q + 11] = (float) (qz - wz);
            o.arcLight[o.arcQuads] = (float) ((0.75 + 0.25 * unit(seed, 90 + s)) * lightMult);
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
}
