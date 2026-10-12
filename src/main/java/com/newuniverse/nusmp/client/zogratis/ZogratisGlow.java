package com.newuniverse.nusmp.client.zogratis;

/**
 * The numbers of how a Zogratis body shines and fades (pure, unit tested): the glow layer's brightness rising with the devil power it
 * has drawn, the flash that hides the model swap of the Gates' transformation, the fade of a dying body, the lift of a hovering one.
 */
public final class ZogratisGlow {
    private ZogratisGlow() {}

    /** What the body is doing, for the glow. */
    public static final int NORMAL = 0, CASTING = 1, ULTIMATE = 2, TRANSFORMING = 3, EXPOSED = 4;

    /**
     * Brightness 0..1 of the glow layer: a steady pulse that is brighter, wider and quicker the more power it has drawn (20 to 100%),
     * swelling over a cast (more for an ultimate), at its peak through the transformation, and starved while it is exposed.
     * 'progress' is the 0..1 of the wind-up or of the transformation.
     */
    public static float level(int power, float age, int mode, float progress) {
        float p = clamp01(power / 100f);
        float v = 0.50f + 0.30f * p + (0.05f + 0.13f * p) * (float) Math.sin(age * (0.10f + 0.10f * p));
        float k = clamp01(progress);
        switch (mode) {
            case CASTING -> v += 0.20f * k;
            case ULTIMATE -> v += 0.40f * k;
            case TRANSFORMING -> v += (1f - v) * Math.max(0.6f, flash(k));
            case EXPOSED -> v = 0.22f + 0.08f * (float) Math.sin(age * 0.5f);
            default -> { }
        }
        return clamp01(v);
    }

    /** 0..1 bell around the middle of the transformation (where the model swaps and the hitbox grows). */
    public static float flash(float progress) {
        float d = Math.abs(clamp01(progress) - 0.5f) / 0.14f;
        if (d >= 1f) return 0f;
        float s = 1f - d;
        return s * s * (3f - 2f * s);
    }

    /** 1 for the first 60% of the death, then fading to 0 at its end. */
    public static float deathFade(float deathTicks, float span) {
        float t = clamp01(deathTicks / Math.max(1f, span));
        if (t <= 0.6f) return 1f;
        float s = clamp01((t - 0.6f) / 0.4f);
        return 1f - s * s * (3f - 2f * s);
    }

    /**
     * How far of its hover a floating body has risen: 0 until it is in its new form, then it rises over the second third of the
     * transformation; 1 once the transformation is over.
     */
    public static float hoverRamp(boolean transforming, float transformProgress, boolean divine) {
        if (!divine) return 0f;
        if (!transforming) return 1f;
        float s = clamp01((transformProgress - 0.5f) / 0.3f);
        return s * s * (3f - 2f * s);
    }

    /** A grey 0xFFvvvvvv of a 0..1 brightness (the glow layer keeps its own colours; the grey only dims it). */
    public static int grey(float level) {
        int g = Math.round(255f * clamp01(level));
        return 0xFF000000 | (g << 16) | (g << 8) | g;
    }

    /** Colour 0xRRGGBB scaled by a 0..1 brightness, as an opaque ARGB. */
    public static int scaled(int rgb, float level) {
        float k = clamp01(level);
        int r = Math.round(((rgb >> 16) & 255) * k), g = Math.round(((rgb >> 8) & 255) * k), b = Math.round((rgb & 255) * k);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** Colour 0xRRGGBB with an alpha of 0..1. */
    public static int withAlpha(int rgb, float alpha) {
        return (Math.round(255f * clamp01(alpha)) << 24) | (rgb & 0xFFFFFF);
    }

    /** Blends two 0xRRGGBB colours (k = 0 the first, 1 the second). */
    public static int mix(int a, int b, float k) {
        float t = clamp01(k);
        int r = Math.round(((a >> 16) & 255) * (1f - t) + ((b >> 16) & 255) * t);
        int g = Math.round(((a >> 8) & 255) * (1f - t) + ((b >> 8) & 255) * t);
        int bl = Math.round((a & 255) * (1f - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static float clamp01(float v) { return Float.isNaN(v) ? 0f : v < 0f ? 0f : v > 1f ? 1f : v; }
}
