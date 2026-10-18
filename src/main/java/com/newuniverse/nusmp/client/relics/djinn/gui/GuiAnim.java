package com.newuniverse.nusmp.client.relics.djinn.gui;

/**
 * The animation curves of the panel. Pure functions of a clock {@code t} in game ticks (the game time plus the partial tick, so motion is smooth
 * at any frame rate) and of small integer seeds; no state, no allocation. Every "random" value is a hash of its seed, so the same seed gives the
 * same particle in the game, in the preview and in the tests.
 */
public final class GuiAnim {
    private GuiAnim() {}

    public static final float TAU = 6.2831855f;

    public static float sin(float x) { return (float) Math.sin(x); }
    public static float cos(float x) { return (float) Math.cos(x); }

    public static float clamp01(float v) { return v < 0f ? 0f : Math.min(1f, v); }
    public static float frac(float v) { return v - (float) Math.floor(v); }
    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    /** 0 below a, 1 above b, smooth between. */
    public static float ramp(float v, float a, float b) {
        if (b <= a) return v >= b ? 1f : 0f;
        float t = clamp01((v - a) / (b - a));
        return t * t * (3f - 2f * t);
    }

    /** Overshoots once (used for the panel unfolding). */
    public static float easeOutBack(float t) {
        t = clamp01(t) - 1f;
        return 1f + t * t * (2.2f * t + 1.2f);
    }

    /** A slow sine between lo and hi with the given period in ticks. */
    public static float breathe(float t, float period, float lo, float hi) {
        return lo + (hi - lo) * (0.5f + 0.5f * sin(t * TAU / period));
    }

    /** 0..1..0 once per unit of v. */
    public static float tri(float v) {
        float f = frac(v);
        return f < 0.5f ? f * 2f : 2f - f * 2f;
    }

    // ---------------------------------------------------------------- deterministic noise

    public static int hash(int n) {
        n ^= n >>> 16;
        n *= 0x7FEB352D;
        n ^= n >>> 15;
        n *= 0x846CA68B;
        n ^= n >>> 16;
        return n;
    }

    /** A repeatable number in [0,1) for a seed. */
    public static float rnd(int seed) { return (hash(seed) >>> 8) / 16777216f; }

    public static float rnd(int seed, int salt) { return rnd(seed * 31 + salt * 7919 + 17); }

    /** Smooth value noise in [0,1]: the same x and seed always give the same value, neighbours blend. */
    public static float noise(float x, int seed) {
        int i = (int) Math.floor(x);
        float f = x - i, s = f * f * (3f - 2f * f);
        return lerp(rnd(i, seed), rnd(i + 1, seed), s);
    }

    // ---------------------------------------------------------------- the wing beat

    /** A wing beat: rises slowly, falls fast; 0..1; {@code speed} beats per tick. */
    public static float flap(float t, float speed) {
        float u = frac(t * speed);
        return u < 0.7f ? ramp(u, 0f, 0.7f) : 1f - ramp(u, 0.7f, 1f);
    }

    public static int frame(float v01, int frames) { return Math.min(frames - 1, Math.max(0, (int) (v01 * frames))); }

    // ---------------------------------------------------------------- phase to look

    /**
     * How much of the Djinn the emblem shows at a phase: the sealed outline at 0, the head and horns at the Lesser Summon, the lit horns at the Half
     * Equip, the wings, tail and halo at the Full Equip and the Extreme Magic. Blends between the previous and the current phase over 14 ticks.
     */
    public static float reveal(int phase, int prev, float phaseAge) {
        float a = revealOf(prev), b = revealOf(phase);
        return lerp(a, b, ramp(phaseAge, 0f, 14f));
    }

    public static float revealOf(int phase) {
        return switch (phase) {
            case 0 -> 0.00f;
            case 1 -> 0.30f;
            case 2 -> 0.62f;
            case 3 -> 1.00f;
            default -> phase >= 4 ? 1.0f : 0.0f;
        };
    }

    /** How hard everything moves: slow while the Djinn is only asleep or summoned, fast in the Full Equip, frantic while an Extreme Magic charges. */
    public static float energy(int phase) {
        return switch (phase) {
            case 0 -> 0.30f;
            case 1 -> 0.55f;
            case 2 -> 0.75f;
            case 3 -> 1.00f;
            default -> phase >= 4 ? 1.40f : 0.30f;
        };
    }

    // ---------------------------------------------------------------- the summon intro

    /** Ticks the summon intro lasts. */
    public static final int INTRO_TICKS = 28;

    /** 0 at the first frame of the intro, 1 when it is over. */
    public static float intro(float introAge) { return clamp01(introAge / INTRO_TICKS); }

    // ---------------------------------------------------------------- perimeter walk

    /**
     * The point at distance {@code d} along the border of a w by h rectangle, clockwise from the top-left corner; written to out[0], out[1].
     */
    public static void perimeter(float d, int w, int h, int[] out) {
        float per = 2f * (w + h);
        d = ((d % per) + per) % per;
        if (d < w) { out[0] = (int) d; out[1] = 0; }
        else if (d < w + h) { out[0] = w - 1; out[1] = (int) (d - w); }
        else if (d < 2 * w + h) { out[0] = w - 1 - (int) (d - w - h); out[1] = h - 1; }
        else { out[0] = 0; out[1] = h - 1 - (int) (d - 2 * w - h); }
    }
}
