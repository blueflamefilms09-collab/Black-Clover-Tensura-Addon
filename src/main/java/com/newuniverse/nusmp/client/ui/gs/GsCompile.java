package com.newuniverse.nusmp.client.ui.gs;

/**
 * The ink-to-matrix compile animation of a grimoire page: when a page is shown, its text starts as ink and a bright band sweeps down the page;
 * each line the band has passed turns into its "compiled" matrix data. Pure timeline maths (ticks in, 0..1 out), so the screen stays a thin
 * drawing layer and the tests can pin the shape. A line is drawn with at most two layers (ink and data) and only while it is inside the band;
 * outside the band it is one layer. {@link #glyphBudget} keeps the quads of a frame under the rule-11 budget.
 */
public final class GsCompile {
    private GsCompile() {}

    /** Ticks the sweep takes (2 seconds). */
    public static final float DURATION = 40f;
    /** The height of the band in GUI pixels: lines inside it show both layers and scrambled characters. */
    public static final float BAND = 22f;
    /** At most this many glyph quads per frame for the whole animation (ink layer + data layer + scramble). */
    public static final int GLYPH_BUDGET = 900;
    /** Per-page glyph budget: three pages can be on screen at once during a page turn, so a third of the frame budget each. */
    public static final int PAGE_GLYPHS = GLYPH_BUDGET / 3;

    /** 0..1 progress of the sweep after 'age' ticks. */
    public static float progress(float age) { return age <= 0f ? 0f : Math.min(1f, age / DURATION); }

    public static boolean done(float age) { return age >= DURATION; }

    /** The y of the band's lower edge, relative to the top of the text block of height 'h': starts above it and ends below it. */
    public static float bandY(float progress, float h) {
        float p = Math.max(0f, Math.min(1f, progress));
        return -BAND + p * (h + 2f * BAND);
    }

    /**
     * How compiled a line at y (relative to the top of the text block) is: 0 = pure ink (the band has not reached it), 1 = pure data (the band
     * has passed it), in between inside the band.
     */
    public static float lineMix(float lineY, float progress, float h) {
        float t = (bandY(progress, h) - lineY) / BAND;
        if (t <= 0f) return 0f;
        if (t >= 1f) return 1f;
        return t * t * (3f - 2f * t);
    }

    /** 0 = only the ink layer, 1 = only the data layer, 2 = both (the line is inside the band). */
    public static int layers(float mix) { return mix <= 0f ? 0 : mix >= 1f ? 1 : 2; }

    /** Fraction of a line's characters shown as scrambled "matrix" glyphs: none at the ends, most in the middle of the band. */
    public static float scrambleFrac(float mix) {
        if (mix <= 0f || mix >= 1f) return 0f;
        return 1f - Math.abs(mix * 2f - 1f);
    }

    /** Whether character i of a line is scrambled at a given frame step: a cheap deterministic hash (no random object, no allocation). */
    public static boolean scrambled(int i, int step, float frac) {
        if (frac <= 0f) return false;
        int h = i * 0x9E3779B1 + step * 0x85EBCA6B;
        h ^= h >>> 15;
        h *= 0x2C1B3C6D;
        h ^= h >>> 12;
        return ((h & 0xFFFF) / 65536f) < frac;
    }

    /** The glyph for a scrambled character (hex digits and a few matrix marks); deterministic. */
    public static char scrambleGlyph(int i, int step) {
        final String set = "0123456789ABCDEF<>/#+=";
        int h = i * 0x27D4EB2F + step * 0x165667B1;
        h ^= h >>> 13;
        return set.charAt((h & 0x7FFFFFFF) % set.length());
    }

    /**
     * The most glyph quads one block of 'lines' lines of 'perLine' characters can draw in a frame: every line once, plus a second layer for the
     * lines that can be inside the band at the same time (the band is {@link #BAND} pixels high, so about BAND / lineH + 1 lines).
     */
    public static int worstGlyphs(int lines, int perLine, int lineH) {
        int inBand = Math.min(lines, (int) Math.ceil(BAND / Math.max(1, lineH)) + 1);
        return (lines + inBand) * perLine;
    }

    /** The longest line (in characters) a block may have so that {@link #worstGlyphs} stays within 'budget'. Never negative, at most 'maxPerLine'. */
    public static int glyphCap(int lines, int maxPerLine, int lineH, int budget) {
        if (lines <= 0 || maxPerLine <= 0) return 0;
        int cap = maxPerLine;
        while (cap > 0 && worstGlyphs(lines, cap, lineH) > budget) cap--;
        return cap;
    }

    /** The line cut to 'cap' characters, ending in "..." when it had to be cut. */
    public static String fit(String s, int cap) {
        if (s.length() <= cap) return s;
        return s.substring(0, Math.max(0, cap - 3)) + "...";
    }
}
