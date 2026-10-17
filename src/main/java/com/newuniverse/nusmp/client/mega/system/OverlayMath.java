package com.newuniverse.nusmp.client.mega.system;

import java.util.Locale;

/**
 * Why: the holographic window is drawn with a few dozen {@code fill} calls, and nearly everything that decides WHAT to draw is arithmetic: how many bar
 * segments are lit for 3 of 5 (or 7 of 40, which must still fit), how a countdown of 4210 ticks reads, when the header glitches, when the cursor blinks, where
 * the scanlines have crept to, where a panel sits for each of the four anchors at any scale, and how a colour's alpha is scaled without losing its hue. These
 * are the places where an off-by-one makes a draw look wrong and nobody can see it until the game runs, so they live here as pure functions (no Minecraft
 * type), unit tested, and the overlay class is left with nothing but drawing. Pure.
 */
public final class OverlayMath {
    private OverlayMath() {}

    /** The most segments the progress bar shows. */
    public static final int MAX_SEGMENTS = 12;

    /** How many segments the bar has for this target: one per unit up to {@link #MAX_SEGMENTS}, then that many. */
    public static int totalSegments(int target) { return Math.max(1, Math.min(MAX_SEGMENTS, target)); }

    /** How many segments are lit: exactly the progress when there is a segment per unit, else proportional (at least one once there is progress, all only when complete). */
    public static int litSegments(int progress, int target) {
        int total = totalSegments(target);
        if (target <= 0 || progress <= 0) return 0;
        if (progress >= target) return total;
        if (target <= MAX_SEGMENTS) return Math.min(total - 1, progress);
        int lit = Math.round((float) progress * total / target);
        return Math.max(1, Math.min(total - 1, lit));
    }

    /** A countdown as {@code m:ss}, rounded UP to a whole second (so it never shows 0:00 while time remains). */
    public static String countdown(int ticks) {
        int s = (int) Math.ceil(Math.max(0, ticks) / 20.0);
        return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }

    /** Is the header in a glitch frame? Two short flickers, 0.6 and 0.6 ticks long, 1.3 ticks apart, every 150 ticks (7.5 seconds). */
    public static boolean glitch(float time) {
        float p = time % 150f;
        if (p < 0f) p += 150f;
        return p < 0.6f || (p >= 1.3f && p < 1.9f);
    }

    /** Is the blinking cursor on? Half a second on, half a second off. */
    public static boolean blink(float time) { return ((int) Math.floor(time / 10f) & 1) == 0; }

    /** The vertical creep of the scanlines: one pixel every three ticks, wrapped to the line spacing. */
    public static int scanOffset(float time, int spacing) {
        int s = Math.max(1, spacing);
        int v = (int) Math.floor(time / 3f) % s;
        return v < 0 ? v + s : v;
    }

    /** Where the panel's left edge sits for an anchor (0 top-left, 1 top-right, 2 bottom-left, 3 bottom-right). All sizes are in screen pixels. */
    public static int originX(int anchor, int screenW, int panelW, int margin) {
        return (anchor == 1 || anchor == 3) ? Math.max(0, screenW - panelW - margin) : margin;
    }

    /** Where the panel's top edge sits for an anchor. */
    public static int originY(int anchor, int screenH, int panelH, int margin) {
        return (anchor == 2 || anchor == 3) ? Math.max(0, screenH - panelH - margin) : margin;
    }

    /** An ARGB colour with its alpha multiplied by {@code a} (0..1); the hue is untouched. */
    public static int scaleAlpha(int argb, float a) {
        float k = Math.max(0f, Math.min(1f, a));
        int alpha = Math.round((argb >>> 24) * k);
        return alpha << 24 | (argb & 0xFFFFFF);
    }

    /** A colour at a given opacity (0..1) as an ARGB int. */
    public static int withAlpha(int rgb, float a) {
        return Math.round(Math.max(0f, Math.min(1f, a)) * 255f) << 24 | (rgb & 0xFFFFFF);
    }

    /** Vanilla draws text whose alpha byte is under 4 as fully opaque, so such a colour must not be drawn at all. */
    public static boolean drawable(int argb) { return (argb >>> 24) >= 8; }

    /** A clamp to the configured range of the window's scale. */
    public static float clampScale(double s) { return (float) Math.max(0.5, Math.min(2.0, Double.isNaN(s) ? 1.0 : s)); }
}
