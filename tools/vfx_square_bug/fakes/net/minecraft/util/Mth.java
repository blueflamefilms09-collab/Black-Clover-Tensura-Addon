package net.minecraft.util;

/** Harness fake: the same arithmetic for the old and the new buffer, so only the buffer's own logic is compared. */
public final class Mth {
    public static final float PI = (float) Math.PI, TWO_PI = (float) (Math.PI * 2);
    public static float cos(float v) { return (float) Math.cos(v); }
    public static float sin(float v) { return (float) Math.sin(v); }
    public static float lerp(float t, float a, float b) { return a + t * (b - a); }
    public static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
