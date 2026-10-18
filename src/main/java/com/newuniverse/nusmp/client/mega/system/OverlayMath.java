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

    /** A clamp to the configured range of the window's scale: 0.5 to 2.5 (the top was 2.0 before the mouse resize; {@link OverlayDrag#MAX_SCALE}). */
    public static float clampScale(double s) { return (float) Math.max(0.5, Math.min(2.5, Double.isNaN(s) ? 1.0 : s)); }

    // ================================================================ the movable window (1.58.0)
    /**
     * The largest offset (window pixels) the config keeps. Offsets are in the window's OWN pixels (the unit of its 164-pixel width, before the GUI scale of the
     * game and the window's scale option are applied), measured from the anchor's corner inwards: right-anchored panels count from the right edge, bottom-anchored
     * ones from the bottom edge, so "5" always means "5 pixels farther from the corner than the standard margin".
     */
    public static final int MAX_OFFSET = 4096;

    /** The corner a reset puts the window in: top-left, the 1.55.0 default. */
    public static final int DEFAULT_ANCHOR = 0;

    /** A position offset in window pixels, from the anchor's corner inwards. */
    public record Offset(int x, int y) {
        public static final Offset ZERO = new Offset(0, 0);
    }

    /** Is the corner on the right-hand side (1 top-right, 3 bottom-right)? */
    public static boolean rightAnchored(int anchor) { return anchor == 1 || anchor == 3; }

    /** Is the corner at the bottom (2 bottom-left, 3 bottom-right)? */
    public static boolean bottomAnchored(int anchor) { return anchor == 2 || anchor == 3; }

    /** A corner number forced into 0..3 (anything else reads as the default). */
    public static int safeAnchor(int anchor) { return anchor >= 0 && anchor <= 3 ? anchor : DEFAULT_ANCHOR; }

    /** The next corner when the player presses "cycle": clockwise, top-left, top-right, bottom-right, bottom-left, and round again. */
    public static int nextAnchor(int anchor) {
        return switch (safeAnchor(anchor)) {
            case 0 -> 1;
            case 1 -> 3;
            case 3 -> 2;
            default -> 0;
        };
    }

    /** {@code v} limited to [lo, hi]; when the range is empty (hi below lo: the panel is bigger than the screen) the low end wins. */
    public static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(Math.max(lo, hi), v)); }

    /** An offset limited to the range the config accepts. */
    public static int clampOffset(int v) { return Math.max(-MAX_OFFSET, Math.min(MAX_OFFSET, v)); }

    /** The size of one axis of the screen in the window's own pixels: the GUI-scaled size divided by the window's scale option, at least 1. */
    public static int windowSize(int guiSize, float scale) { return Math.max(1, Math.round(guiSize / Math.max(0.1f, scale))); }

    /** The panel's left edge: the anchor's corner plus the offset, then clamped so the panel stays on the screen. */
    public static int placeX(int anchor, int screenW, int panelW, int margin, int offset) {
        int raw = rightAnchored(anchor) ? screenW - panelW - margin - offset : margin + offset;
        return clamp(raw, 0, screenW - panelW);
    }

    /** The panel's top edge: the anchor's corner plus the offset, then clamped so the panel stays on the screen. */
    public static int placeY(int anchor, int screenH, int panelH, int margin, int offset) {
        int raw = bottomAnchored(anchor) ? screenH - panelH - margin - offset : margin + offset;
        return clamp(raw, 0, screenH - panelH);
    }

    /** The offset that puts the panel's left edge at {@code x} (the inverse of {@link #placeX} before clamping). */
    public static int offsetFromX(int anchor, int screenW, int panelW, int margin, int x) {
        return rightAnchored(anchor) ? screenW - panelW - margin - x : x - margin;
    }

    /** The offset that puts the panel's top edge at {@code y} (the inverse of {@link #placeY} before clamping). */
    public static int offsetFromY(int anchor, int screenH, int panelH, int margin, int y) {
        return bottomAnchored(anchor) ? screenH - panelH - margin - y : y - margin;
    }

    /** The offsets that put the panel's top-left corner as near to ({@code x}, {@code y}) as the screen allows: for a drag. */
    public static Offset offsetFor(int anchor, int screenW, int screenH, int panelW, int panelH, int margin, int x, int y) {
        int cx = clamp(x, 0, screenW - panelW), cy = clamp(y, 0, screenH - panelH);
        return new Offset(clampOffset(offsetFromX(anchor, screenW, panelW, margin, cx)), clampOffset(offsetFromY(anchor, screenH, panelH, margin, cy)));
    }

    /**
     * What a nudge of ({@code dx}, {@code dy}) screen pixels (right and down are positive) does: the panel moves from where it REALLY is now (after the clamp),
     * so an offset left over from a bigger screen never swallows the first key presses, and the result is clamped again. The anchor's side decides the sign of the
     * stored offset, the player only ever thinks in screen directions.
     */
    public static Offset nudge(int anchor, int screenW, int screenH, int panelW, int panelH, int margin, int offX, int offY, int dx, int dy) {
        int x = placeX(anchor, screenW, panelW, margin, offX) + dx;
        int y = placeY(anchor, screenH, panelH, margin, offY) + dy;
        return offsetFor(anchor, screenW, screenH, panelW, panelH, margin, x, y);
    }

    /** Is the point inside the rectangle (left and top edges in, right and bottom edges out)? */
    public static boolean contains(float x, float y, float w, float h, double px, double py) {
        return px >= x && px < x + w && py >= y && py < y + h;
    }

    /**
     * Moves the first toast clear of edit mode's hint box: when the box stands in the toasts' column and reaches into the room the toasts need, they start under the
     * box instead; otherwise {@code toastY} is returned unchanged. {@code stackH} is the height of the most toasts shown together. An empty box (width or height 0) never moves them.
     */
    public static int toastClear(int toastY, int boxX, int boxY, int boxW, int boxH, int columnW, int stackH, int margin, int gap) {
        if (boxW <= 0 || boxH <= 0) return toastY;
        boolean inColumn = boxX < margin + columnW && boxX + boxW > margin;
        boolean reaches = boxY < toastY + stackH && boxY + boxH > toastY;
        return inColumn && reaches ? boxY + boxH + gap : toastY;
    }

    /** The y of the first toast: under the panel when the panel sits in the toasts' column in the upper half of the screen (so they never cover it), else at the top margin. */
    public static int toastTop(int panelX, int panelY, int panelW, int panelH, int screenH, int columnW, int margin, int gap) {
        boolean inColumn = panelX < margin + columnW && panelX + panelW > margin;
        boolean upper = panelY < screenH / 2;
        return inColumn && upper ? panelY + panelH + gap : margin + 2;
    }
}
