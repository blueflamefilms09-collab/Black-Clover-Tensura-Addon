package com.newuniverse.nusmp.client.upgrade;

/**
 * Why: the arithmetic of the purple that Hellblaze's glow takes on every hit (docs/hellblaze_katana.md). Pure (no Minecraft type) so the numbers are unit tested: {@link #tone} is 1 on the tick of a hit and
 * falls to 0 over {@code ticks} (about half a second at 10) along a smoothstep; {@link #mix} blends the dark-red tint of the glow toward the purple of the hit by that amount.
 */
public final class HellblazeTone {
    private HellblazeTone() {}

    /** The hit tone for a hit that landed {@code age} ticks ago (partial ticks included): 1 right after it, 0 once {@code ticks} have passed or before it happened. */
    public static float tone(float age, int ticks) {
        if (ticks <= 0 || age < 0f || age >= ticks) return 0f;
        float u = 1f - age / ticks;
        return u * u * (3f - 2f * u);
    }

    /** The colour {@code a} blended toward {@code b} by {@code t} (0..1), per channel, as 0xRRGGBB. */
    public static int mix(int a, int b, float t) {
        if (t <= 0f) return a & 0xFFFFFF;
        if (t >= 1f) return b & 0xFFFFFF;
        int r = Math.round((a >> 16 & 255) + ((b >> 16 & 255) - (a >> 16 & 255)) * t);
        int g = Math.round((a >> 8 & 255) + ((b >> 8 & 255) - (a >> 8 & 255)) * t);
        int bl = Math.round((a & 255) + ((b & 255) - (a & 255)) * t);
        return r << 16 | g << 8 | bl;
    }
}
