package com.newuniverse.nusmp.client.ui.theme;

/**
 * The pure maths of the UI theme (no Minecraft types, unit tested): easing, pulses, rotation angles, the typewriter count and colour mixing.
 * Time is always in game ticks as a float (a whole tick plus the partial tick), so every animation honours the partial tick and runs the
 * same at any frame rate.
 */
public final class UiMath {
    private UiMath() {}

    public static float clamp01(float v) { return v < 0f ? 0f : Math.min(v, 1f); }

    public static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    /** Smoothstep 0..1. */
    public static float smooth(float t) {
        t = clamp01(t);
        return t * t * (3f - 2f * t);
    }

    public static float easeOutCubic(float t) {
        t = clamp01(t);
        float u = 1f - t;
        return 1f - u * u * u;
    }

    /** Ease out with a small overshoot (settles at 1). */
    public static float easeOutBack(float t) {
        t = clamp01(t);
        float c1 = 1.70158f, c3 = c1 + 1f, u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    }

    /** A slow breathing value between 0 and 1: 0.5 + 0.5 sin, one full breath every 'period' ticks. */
    public static float pulse(float time, float period, float phase) {
        return 0.5f + 0.5f * (float) Math.sin((time / period + phase) * (Math.PI * 2.0));
    }

    /** Degrees, kept in [0, 360), of a layer turning at 'degPerTick' (negative = the other way) from a start angle. */
    public static float angle(float time, float degPerTick, float startDeg) {
        float a = (time * degPerTick + startDeg) % 360f;
        return a < 0f ? a + 360f : a;
    }

    /** How much of the exponential approach to a target one frame covers: 1 - (1 - k)^dt, for dt in ticks. */
    public static float approachFactor(float k, float dtTicks) {
        if (dtTicks <= 0f) return 0f;
        return 1f - (float) Math.pow(1.0 - clamp01(k), dtTicks);
    }

    /** Moves 'cur' toward 'target' by the approach factor; snaps when closer than 'eps'. */
    public static float approach(float cur, float target, float k, float dtTicks, float eps) {
        float n = cur + (target - cur) * approachFactor(k, dtTicks);
        return Math.abs(target - n) < eps ? target : n;
    }

    /** Characters revealed by the typewriter after 'ageTicks' at 'charsPerTick' (never above 'length', never below 0). */
    public static int typed(float ageTicks, float charsPerTick, int length) {
        if (ageTicks <= 0f || length <= 0) return 0;
        long n = (long) Math.floor(ageTicks * charsPerTick);
        return (int) Math.min(length, n);
    }

    /** Alpha (0..255) of an ARGB colour scaled by 'a' (0..1). */
    public static int scaleAlpha(int argb, float a) {
        int al = Math.round(((argb >>> 24) & 0xFF) * clamp01(a));
        return (argb & 0x00FFFFFF) | (al << 24);
    }

    /** The colour with its alpha replaced by 'a' (0..1). */
    public static int withAlpha(int rgbOrArgb, float a) {
        return (rgbOrArgb & 0x00FFFFFF) | (Math.round(clamp01(a) * 255f) << 24);
    }

    /** Linear mix of two colours (all four channels). */
    public static int mix(int a, int b, float t) {
        t = clamp01(t);
        int out = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int ca = (a >>> shift) & 0xFF, cb = (b >>> shift) & 0xFF;
            out |= (Math.round(ca + (cb - ca) * t) & 0xFF) << shift;
        }
        return out;
    }

    /** A lang-key slug: lower case, runs of anything that is not a letter or digit become one underscore, none at the ends ("Spirit Lords" -> "spirit_lords"). */
    public static String slug(String s) {
        StringBuilder b = new StringBuilder();
        boolean gap = false;
        for (int i = 0; i < (s == null ? 0 : s.length()); i++) {
            char c = Character.toLowerCase(s.charAt(i));
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                if (gap && b.length() > 0) b.append('_');
                gap = false;
                b.append(c);
            } else {
                gap = true;
            }
        }
        return b.toString();
    }
}
