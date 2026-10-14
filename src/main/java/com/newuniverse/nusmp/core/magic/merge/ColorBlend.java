package com.newuniverse.nusmp.core.magic.merge;

/**
 * Pure colour mathematics of merged magics (ARGB ints, alpha is always forced opaque where a colour is created from several). The
 * shared palette of a merged magic is the parents' colours; every effect, armour and model takes its colours from it so that nothing
 * of a merged magic looks like one parent alone.
 */
public final class ColorBlend {
    private ColorBlend() {}

    /** Parent weights by count: the first (dominant) parent weighs most. Sums to 1. */
    public static float[] weights(int n) {
        return switch (n) {
            case 1 -> new float[]{1f};
            case 2 -> new float[]{0.55f, 0.45f};
            case 3 -> new float[]{0.40f, 0.33f, 0.27f};
            default -> {
                float[] w = {0.34f, 0.26f, 0.22f, 0.18f};
                if (n == 4) yield w;
                float[] out = new float[Math.max(1, n)];
                java.util.Arrays.fill(out, 1f / out.length);
                yield out;
            }
        };
    }

    private static int ch(int c, int shift) { return (c >>> shift) & 0xFF; }

    private static int clamp(int v) { return v < 0 ? 0 : Math.min(v, 255); }

    private static int pack(int a, int r, int g, int b) { return (clamp(a) << 24) | (clamp(r) << 16) | (clamp(g) << 8) | clamp(b); }

    /** Linear blend of two ARGB colours, t = 0 gives a, t = 1 gives b (t is clamped). */
    public static int lerp(int a, int b, float t) {
        float k = t < 0f ? 0f : Math.min(t, 1f);
        return pack(Math.round(ch(a, 24) + (ch(b, 24) - ch(a, 24)) * k), Math.round(ch(a, 16) + (ch(b, 16) - ch(a, 16)) * k),
                Math.round(ch(a, 8) + (ch(b, 8) - ch(a, 8)) * k), Math.round(ch(a, 0) + (ch(b, 0) - ch(a, 0)) * k));
    }

    /**
     * The weighted mean of the colours (opaque), lifted a little in saturation so that blending a red and a blue gives a vivid violet and
     * not a dull grey. One colour returns itself; null or empty returns white.
     */
    public static int mix(int[] colors, float[] weights) {
        if (colors == null || colors.length == 0) return 0xFFFFFFFF;
        float r = 0, g = 0, b = 0, sum = 0;
        for (int i = 0; i < colors.length; i++) {
            float w = weights != null && i < weights.length ? Math.max(0f, weights[i]) : 1f;
            r += ch(colors[i], 16) * w; g += ch(colors[i], 8) * w; b += ch(colors[i], 0) * w; sum += w;
        }
        if (sum <= 0f) { r = g = b = 0; for (int c : colors) { r += ch(c, 16); g += ch(c, 8); b += ch(c, 0); } sum = colors.length; }
        r /= sum; g /= sum; b /= sum;
        if (colors.length > 1) {
            float luma = 0.299f * r + 0.587f * g + 0.114f * b, lift = 1.18f;
            r = luma + (r - luma) * lift; g = luma + (g - luma) * lift; b = luma + (b - luma) * lift;
        }
        return pack(255, Math.round(r), Math.round(g), Math.round(b));
    }

    /**
     * A smooth walk round the palette: phase 0 is the first colour, 1/n the second ... 1 is the first again. Phase wraps. This is the
     * "alternating" colour of rings and motes.
     */
    public static int cycle(int[] colors, float phase) {
        if (colors == null || colors.length == 0) return 0xFFFFFFFF;
        if (colors.length == 1) return colors[0] | 0xFF000000;
        float p = phase - (float) Math.floor(phase);
        float x = p * colors.length;
        int i = (int) x;
        if (i >= colors.length) i = colors.length - 1;
        return lerp(colors[i], colors[(i + 1) % colors.length], x - i) | 0xFF000000;
    }

    /** The colour of the k-th element of a ring of motes / spikes: parents take turns, no blending (crisp alternation). */
    public static int alternate(int[] colors, int k) {
        if (colors == null || colors.length == 0) return 0xFFFFFFFF;
        return colors[Math.floorMod(k, colors.length)] | 0xFF000000;
    }

    /** The colour a parent's own effect is drawn in during a merged cast: its own colour pulled toward the shared mix by {@code pull} (0..1). */
    public static int tint(int own, int shared, float pull) { return lerp(own, shared, pull) | 0xFF000000; }

    /** A lighter version of the colour (toward white), for cores and highlights. */
    public static int whiten(int argb, float t) { return lerp(argb, 0xFFFFFFFF, t) | (argb & 0xFF000000); }

    /** Size factor of a parent's part in a composite: heavier parents draw bigger (0.75 .. 1.25 around 1 for the weights above). */
    public static float sizeFor(float weight, int n) { return 0.75f + 0.5f * Math.min(1f, weight * Math.max(1, n) / 2f); }
}
