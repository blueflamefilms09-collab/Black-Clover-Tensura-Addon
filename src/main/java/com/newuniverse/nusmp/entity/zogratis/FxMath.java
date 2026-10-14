package com.newuniverse.nusmp.entity.zogratis;

/**
 * Pure numbers behind the Zogratis boss effects (1.35.0): how many effect packets a boss may send per tick, the payload flag bits and the
 * gold-to-purple tint of Lucius as his power rises.
 */
public final class FxMath {
    private FxMath() {}

    /** Seed flag bits read by the layers: bit 0 = the telegraph form, bit 1 = the variant (ground pane / purple form). */
    public static final long TELEGRAPH = 1L, VARIANT = 2L;
    /** Most effect packets one level may send in one tick (a boss with several marks, a volley, a crowd of disciples). */
    public static final int MAX_PER_TICK = 20;

    public static final int LUCIUS_GOLD = 0xE8B923, LUCIUS_LIGHT = 0xFFE680, LUCIUS_PURPLE = 0x9B5BFF, LUCIUS_DEEP = 0x3B0F6B;

    /** Seed with the flags set and nothing else in the two low bits. */
    public static long seed(long random, boolean telegraph, boolean variant) {
        return (random & ~3L) | (telegraph ? TELEGRAPH : 0L) | (variant ? VARIANT : 0L);
    }

    /** How far Lucius's light has drifted toward purple at 'power': nothing at 20% and 40%, then a third, two thirds, all of it with the Gates. */
    public static float purple(int power) {
        if (power >= 100) return 1f;
        if (power >= 80) return 0.66f;
        if (power >= 60) return 0.33f;
        return 0f;
    }

    /** Blend of two 0xRRGGBB colours (k clamped 0..1). */
    public static int mix(int a, int b, float k) {
        float t = Math.max(0f, Math.min(1f, k));
        int r = Math.round(((a >> 16) & 255) * (1 - t) + ((b >> 16) & 255) * t), g = Math.round(((a >> 8) & 255) * (1 - t) + ((b >> 8) & 255) * t);
        int bl = Math.round((a & 255) * (1 - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    /** Lucius's light colour (0xRRGGBB) at 'power': gold, drifting to violet. */
    public static int divine(int power) { return mix(LUCIUS_GOLD, LUCIUS_PURPLE, purple(power)); }

    /** The sender's per-tick packet counter: true while another packet may go out this tick. */
    public static final class Throttle {
        private long tick = Long.MIN_VALUE;
        private int sent;

        public boolean allow(long gameTime) {
            if (gameTime != tick) { tick = gameTime; sent = 0; }
            if (sent >= MAX_PER_TICK) return false;
            sent++;
            return true;
        }

        public int sentThisTick() { return sent; }
    }
}
