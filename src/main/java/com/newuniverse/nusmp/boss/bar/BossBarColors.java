package com.newuniverse.nusmp.boss.bar;

/** 0xRRGGBB colour maths shared by the server themes and the client animation (pure: no Minecraft types). */
public final class BossBarColors {
    private BossBarColors() {}

    /** Linear blend of two colours, t clamped to 0..1. */
    public static int lerp(int a, int b, float t) {
        if (t <= 0f) return a & 0xFFFFFF;
        if (t >= 1f) return b & 0xFFFFFF;
        int r = mix((a >> 16) & 255, (b >> 16) & 255, t);
        int g = mix((a >> 8) & 255, (b >> 8) & 255, t);
        int bl = mix(a & 255, b & 255, t);
        return (r << 16) | (g << 8) | bl;
    }

    private static int mix(int x, int y, float t) {
        return Math.max(0, Math.min(255, Math.round(x + (y - x) * t)));
    }

    public static float red(int rgb) { return ((rgb >> 16) & 255) / 255f; }
    public static float green(int rgb) { return ((rgb >> 8) & 255) / 255f; }
    public static float blue(int rgb) { return (rgb & 255) / 255f; }
}
