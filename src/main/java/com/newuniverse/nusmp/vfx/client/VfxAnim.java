package com.newuniverse.nusmp.vfx.client;

import net.minecraft.util.Mth;

/** Animation curves. All take t = 0..1 progress unless noted. */
public final class VfxAnim {
    private VfxAnim() {}

    public static float easeOutCubic(float t) { float u = 1 - t; return 1 - u * u * u; }
    public static float easeInCubic(float t) { return t * t * t; }
    public static float easeOutBack(float t) { float c = 1.70158f, u = t - 1; return 1 + (c + 1) * u * u * u + c * u * u; }
    public static float easeInOutSine(float t) { return -(Mth.cos(Mth.PI * t) - 1) / 2; }

    /** Quick fade in, hold, fade out. */
    public static float fadeInOut(float t, float in, float out) {
        if (t < in) return t / in;
        if (t > 1 - out) return Math.max(0, (1 - t) / out);
        return 1;
    }

    /** Grimoire circle: pops open with overshoot. */
    public static float circleOpen(float t) { return easeOutBack(Mth.clamp(t * 4f, 0, 1)); }

    /** Rotation in radians for a spinning circle; ageTicks is real time so it spins smoothly. */
    public static float magicSpin(float ageTicks, float speed) { return ageTicks * 0.05f * speed; }

    /** Angle of rune i of n orbiting at the given speed. */
    public static float runeOrbit(int i, int n, float ageTicks, float speed) { return Mth.TWO_PI * i / n + ageTicks * 0.04f * speed; }

    /** Flames swell then flicker. Returns a scale multiplier. */
    public static float flameGrow(float t, float ageTicks, long seed) {
        float base = easeOutCubic(Mth.clamp(t * 3f, 0, 1));
        float flick = 0.85f + 0.15f * Mth.sin(ageTicks * 0.9f + seed % 7);
        return base * flick;
    }

    /** Explosion: fast overshoot expansion. */
    public static float explosionBurst(float t) { return easeOutBack(Mth.clamp(t * 1.6f, 0, 1)) * (1 + 0.15f * t); }

    /** Slash sweep progress along its path (head) - the tail lags behind. */
    public static float slashHead(float t) { return easeOutCubic(Mth.clamp(t * 1.4f, 0, 1)); }
    public static float slashTail(float t) { return easeInCubic(Mth.clamp(t * 1.2f, 0, 1)); }

    /** 0..1 sine pulse, frequency in cycles per second. */
    public static float pulse(float ageTicks, float hz) { return 0.5f + 0.5f * Mth.sin(ageTicks / 20f * hz * Mth.TWO_PI); }
}
