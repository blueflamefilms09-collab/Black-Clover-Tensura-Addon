package com.newuniverse.nusmp.client.remodel;

/**
 * Why: the few numbers the remodel's renderers compute that have nothing to do with the game's classes (a tint multiplied into a body, the curves of Rouge unravelling and re-knitting and of a
 * painted construct rising off the canvas), kept apart so they can be unit tested without Minecraft. They are the same curves the old renderers had, moved here unchanged so the look of the
 * motion is what the owner already knew.
 */
public final class KitMath {
    private KitMath() {}

    /** Two 0xRRGGBB colours multiplied channel by channel (white is no change). */
    public static int multiply(int a, int b) {
        int r = ((a >> 16) & 255) * ((b >> 16) & 255) / 255, g = ((a >> 8) & 255) * ((b >> 8) & 255) / 255, bl = (a & 255) * (b & 255) / 255;
        return (r << 16) | (g << 8) | bl;
    }

    /** The colour k of the way from a to b (0xRRGGBB, k in 0..1). */
    public static int mix(int a, int b, float k) {
        float c = Math.max(0f, Math.min(1f, k));
        int r = (int) (((a >> 16) & 255) * (1 - c) + ((b >> 16) & 255) * c), g = (int) (((a >> 8) & 255) * (1 - c) + ((b >> 8) & 255) * c), bl = (int) ((a & 255) * (1 - c) + (b & 255) * c);
        return (r << 16) | (g << 8) | bl;
    }

    /** Rouge unravelling, u from 0 to 1: shrinks to nothing, slowly at first (1 - u squared), never quite zero so the pose stays valid. */
    public static float unravelScale(float u) {
        float c = Math.max(0f, Math.min(1f, u));
        return Math.max(0.02f, 1f - c * c);
    }

    /** Rouge re-knitting, u from 0 to 1: grows back, through full size and up to the cap of 1.1 (the old renderer's curve, kept); the state after it draws her at 1 again. */
    public static float reknitScale(float u) {
        float c = Math.max(0f, Math.min(1f, u));
        float v = 1.15f - 0.15f * (float) Math.cos(c * Math.PI) - (1f - c) * (1f - c) * 1.15f;
        return Math.max(0.02f, Math.min(1.1f, v));
    }

    /** The ease-out-back curve of a body springing up: 0 at t = 0, 1 at t = 1, overshooting about 10 percent between. */
    public static float easeOutBack(float t) {
        float c = 1.70158f, u = Math.max(0f, Math.min(1f, t)) - 1f;
        return 1f + (c + 1f) * u * u * u + c * u * u;
    }

    /**
     * A painted construct rising off the canvas over its first 16 ticks: {sx, sy, sz} to scale the body by, flat and wide at first (a drawing on the ground) and springing up to full height, or
     * null once it stands (t >= 1, t = age in ticks / 16).
     */
    public static float[] paintedRise(float t) {
        if (t >= 1f) return null;
        float c = Math.max(0f, t);
        float wide = 1f + 0.3f * (1f - c);
        return new float[]{wide, Math.max(0.04f, easeOutBack(c)), wide};
    }
}
