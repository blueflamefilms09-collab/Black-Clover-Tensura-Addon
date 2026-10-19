package com.newuniverse.nusmp.client.relics.djinn.gui;

/** Colour maths of the panel (0xRRGGBB unless a name says argb). Pure, allocation free. */
public final class GuiColor {
    private GuiColor() {}

    public static final int GOLD = 0xD8B048, WHITE = 0xFFFFFF;

    public static int mix(int a, int b, float t) {
        float k = t < 0f ? 0f : Math.min(1f, t);
        int r = Math.round(((a >> 16) & 255) * (1 - k) + ((b >> 16) & 255) * k);
        int g = Math.round(((a >> 8) & 255) * (1 - k) + ((b >> 8) & 255) * k);
        int bl = Math.round((a & 255) * (1 - k) + (b & 255) * k);
        return (r << 16) | (g << 8) | bl;
    }

    public static int scale(int rgb, float k) {
        int r = clamp255((int) (((rgb >> 16) & 255) * k)), g = clamp255((int) (((rgb >> 8) & 255) * k)), b = clamp255((int) ((rgb & 255) * k));
        return (r << 16) | (g << 8) | b;
    }

    /** a + b per channel, clamped. */
    public static int add(int a, int b) {
        int r = clamp255(((a >> 16) & 255) + ((b >> 16) & 255)), g = clamp255(((a >> 8) & 255) + ((b >> 8) & 255)), bl = clamp255((a & 255) + (b & 255));
        return (r << 16) | (g << 8) | bl;
    }

    /** An opaque-or-not argb from a colour and an alpha of 0..1. */
    public static int argb(int rgb, float alpha) {
        int a = clamp255(Math.round(alpha * 255f));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    /** The argb with its alpha multiplied by k (0..1). */
    public static int fade(int argb, float k) {
        int a = clamp255(Math.round(((argb >>> 24) & 255) * (k < 0f ? 0f : Math.min(1f, k))));
        return (a << 24) | (argb & 0xFFFFFF);
    }

    /** Perceived brightness 0..1 (Rec. 709 weights on the gamma-coded channels). */
    public static float luma(int rgb) {
        return (0.2126f * ((rgb >> 16) & 255) + 0.7152f * ((rgb >> 8) & 255) + 0.0722f * (rgb & 255)) / 255f;
    }

    /** Blends towards white until the colour is at least {@code minLuma} bright (a dark Djinn colour stays readable on a dark panel). */
    public static int lift(int rgb, float minLuma) {
        rgb &= 0xFFFFFF;
        float l = luma(rgb);
        if (l >= minLuma) return rgb;
        float t = Math.min(1f, (minLuma - l) / Math.max(0.001f, 1f - l));
        return mix(rgb, WHITE, t);
    }

    /** Scales the colour down until it is at most {@code maxLuma} bright. */
    public static int sink(int rgb, float maxLuma) {
        rgb &= 0xFFFFFF;
        float l = luma(rgb);
        if (l <= maxLuma || l <= 0f) return rgb;
        return scale(rgb, maxLuma / l);
    }

    private static int clamp255(int v) { return v < 0 ? 0 : Math.min(255, v); }
}
