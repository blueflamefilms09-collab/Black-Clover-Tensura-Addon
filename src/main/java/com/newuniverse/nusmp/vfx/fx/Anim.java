package com.newuniverse.nusmp.vfx.fx;

/**
 * One stackable animation on a piece, active from tick 'start' to 'end' (effect ticks), shaped by 'ease'.
 * FADE_IN / FADE_OUT: alpha. GROW: scale a -> b. DRIFT: offset (a,b,c) blocks. SPIRAL_IN: radius a -> 0 over b turns
 * (moves the piece, never spins the mesh). SWAY: amplitude a at b Hz. FLASH: brightness up to a (<= 1.8) and back.
 * TINT: palette colour a -> b. FLICKER: brightness wobble of amplitude a (<= 0.35) at b Hz.
 */
public record Anim(Type type, int start, int end, Ease ease, double a, double b, double c) {
    public enum Type { FADE_IN, FADE_OUT, GROW, DRIFT, SPIRAL_IN, SWAY, FLASH, TINT, FLICKER }

    public static Anim fadeIn(int s, int e, Ease ease) { return new Anim(Type.FADE_IN, s, e, ease, 0, 0, 0); }
    public static Anim fadeOut(int s, int e, Ease ease) { return new Anim(Type.FADE_OUT, s, e, ease, 0, 0, 0); }
    public static Anim grow(int s, int e, Ease ease, double from, double to) { return new Anim(Type.GROW, s, e, ease, from, to, 0); }
    public static Anim drift(int s, int e, Ease ease, Vec by) { return new Anim(Type.DRIFT, s, e, ease, by.x(), by.y(), by.z()); }
    public static Anim spiralIn(int s, int e, Ease ease, double radius, double turns) { return new Anim(Type.SPIRAL_IN, s, e, ease, radius, turns, 0); }
    public static Anim sway(int s, int e, double amp, double hz) { return new Anim(Type.SWAY, s, e, Ease.IN_OUT_SINE, amp, hz, 0); }
    public static Anim flash(int s, int e, Ease ease, double peak) { return new Anim(Type.FLASH, s, e, ease, peak, 0, 0); }
    public static Anim tint(int s, int e, Ease ease, int from, int to) { return new Anim(Type.TINT, s, e, ease, from, to, 0); }
    public static Anim flicker(int s, int e, double amp, double hz) { return new Anim(Type.FLICKER, s, e, Ease.LINEAR, amp, hz, 0); }

    /** 0..1 progress at effect time t (clamped), eased. */
    public double progress(double t) { return ease.at((t - start) / Math.max(1e-6, end - start)); }
}
