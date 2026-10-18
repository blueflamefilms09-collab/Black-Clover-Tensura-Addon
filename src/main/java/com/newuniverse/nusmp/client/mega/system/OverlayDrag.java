package com.newuniverse.nusmp.client.mega.system;

/**
 * Why: the owner wants to move and resize the System window with the MOUSE, and wants the place to survive a change of resolution or GUI scale. Both are
 * arithmetic nobody can see until the game runs, so they live here as pure functions (no Minecraft type) and are unit tested to the pixel; the screen that
 * listens to the mouse ({@code SystemEditScreen}) only feeds them numbers.
 *
 * <p><b>Anchors.</b> The saved place is an anchor plus an offset from it ({@link OverlayMath}: the four corners, offsets in the window's own pixels, measured
 * inwards). A window dragged to the middle of the screen would, with a corner anchor, drift off-centre when the screen gets wider or narrower, so this adds two
 * flags, {@code centerX} and {@code centerY}: when one is set, that axis is measured from the screen's MIDDLE instead of from the corner's edge. Together with the
 * corner bits this is a 3 x 3 grid of anchors (left / middle / right by top / middle / bottom), and {@link #snap} picks the nearest one for wherever the window is
 * dropped, so a window left at the right edge stays at the right edge, one left in the middle stays in the middle, on any screen. The old config keys
 * ({@code anchor}, {@code offsetX}, {@code offsetY}) keep their meaning; the flags default to off, so every saved place of the earlier versions reads as before.
 *
 * <p><b>Size.</b> The window is drawn in its own pixels and scaled as a whole (config {@code scale}, 0.5 to 2.5). A resize keeps one point of the window fixed on
 * the screen (the corner opposite the handle that is dragged, or the point under the mouse for the wheel) and returns the new scale and the new top-left in GUI
 * pixels; {@link #snapGui} turns that into a saved place under the new scale. Pure.
 */
public final class OverlayDrag {
    private OverlayDrag() {}

    /** The smallest and the largest window scale the config accepts, and the step of one wheel notch (a notch with Shift is {@link #BIG_STEP}). */
    public static final float MIN_SCALE = 0.5f, MAX_SCALE = 2.5f, WHEEL_STEP = 0.05f, BIG_STEP = 0.25f;

    /** A saved place: the corner, whether each axis is measured from the screen's middle, and the offsets in the window's own pixels. */
    public record Place(int anchor, boolean centerX, boolean centerY, int offX, int offY) {
        /** The default place: the top-left corner, no offset. */
        public static final Place DEFAULT = new Place(OverlayMath.DEFAULT_ANCHOR, false, false, 0, 0);
    }

    /** A new size and the window's new top-left corner in GUI pixels. */
    public record Resized(float scale, double left, double top) {}

    // ================================================================ where the window sits
    /** The panel's left edge: the corner's edge plus the offset, or the middle of the screen plus the offset; clamped so the panel stays on the screen. */
    public static int placeX(int anchor, boolean centerX, int screenW, int panelW, int margin, int offset) {
        if (!centerX) return OverlayMath.placeX(anchor, screenW, panelW, margin, offset);
        return OverlayMath.clamp((screenW - panelW) / 2 + offset, 0, screenW - panelW);
    }

    /** The panel's top edge (see {@link #placeX}). */
    public static int placeY(int anchor, boolean centerY, int screenH, int panelH, int margin, int offset) {
        if (!centerY) return OverlayMath.placeY(anchor, screenH, panelH, margin, offset);
        return OverlayMath.clamp((screenH - panelH) / 2 + offset, 0, screenH - panelH);
    }

    /** The offset that puts the panel's left edge at {@code x} (the inverse of {@link #placeX} before clamping). */
    public static int offsetFromX(int anchor, boolean centerX, int screenW, int panelW, int margin, int x) {
        return centerX ? x - (screenW - panelW) / 2 : OverlayMath.offsetFromX(anchor, screenW, panelW, margin, x);
    }

    /** The offset that puts the panel's top edge at {@code y}. */
    public static int offsetFromY(int anchor, boolean centerY, int screenH, int panelH, int margin, int y) {
        return centerY ? y - (screenH - panelH) / 2 : OverlayMath.offsetFromY(anchor, screenH, panelH, margin, y);
    }

    /** The panel's left edge for a saved place, in window pixels. */
    public static int placeX(Place p, int screenW, int panelW, int margin) { return placeX(p.anchor(), p.centerX(), screenW, panelW, margin, p.offX()); }

    /** The panel's top edge for a saved place, in window pixels. */
    public static int placeY(Place p, int screenH, int panelH, int margin) { return placeY(p.anchor(), p.centerY(), screenH, panelH, margin, p.offY()); }

    /**
     * The place that puts the panel's top-left corner at ({@code x}, {@code y}) (window pixels; clamped to the screen) and anchors it to the NEAREST of the nine
     * anchor points: an axis is "middle" when the panel's centre is in the middle third of the screen, else it is the near side. The offsets are computed for that
     * anchor, so {@link #placeX} / {@link #placeY} of the result give back the clamped corner exactly.
     */
    public static Place snap(int screenW, int screenH, int panelW, int panelH, int margin, int x, int y) {
        int cx0 = OverlayMath.clamp(x, 0, screenW - panelW), cy0 = OverlayMath.clamp(y, 0, screenH - panelH);
        double cx = cx0 + panelW / 2.0, cy = cy0 + panelH / 2.0;
        boolean right = cx >= screenW / 2.0, bottom = cy >= screenH / 2.0;
        boolean centerX = Math.abs(cx - screenW / 2.0) < screenW / 6.0, centerY = Math.abs(cy - screenH / 2.0) < screenH / 6.0;
        int anchor = (bottom ? 2 : 0) | (right ? 1 : 0);
        int ox = OverlayMath.clampOffset(offsetFromX(anchor, centerX, screenW, panelW, margin, cx0));
        int oy = OverlayMath.clampOffset(offsetFromY(anchor, centerY, screenH, panelH, margin, cy0));
        return new Place(anchor, centerX, centerY, ox, oy);
    }

    /**
     * {@link #snap} for a top-left corner given in GUI pixels under a window scale: converts to the window's own pixels (the unit the offsets are kept in) first.
     */
    public static Place snapGui(int guiW, int guiH, float scale, int panelW, int panelH, int margin, double leftGui, double topGui) {
        float s = OverlayMath.clampScale(scale);
        return snap(OverlayMath.windowSize(guiW, s), OverlayMath.windowSize(guiH, s), panelW, panelH, margin, (int) Math.round(leftGui / s), (int) Math.round(topGui / s));
    }

    /** Where a saved place puts the panel's left edge on the real screen, in GUI pixels. */
    public static double leftGui(Place p, int guiW, float scale, int panelW, int margin) {
        float s = OverlayMath.clampScale(scale);
        return placeX(p, OverlayMath.windowSize(guiW, s), panelW, margin) * (double) s;
    }

    /** Where a saved place puts the panel's top edge on the real screen, in GUI pixels. */
    public static double topGui(Place p, int guiH, float scale, int panelH, int margin) {
        float s = OverlayMath.clampScale(scale);
        return placeY(p, OverlayMath.windowSize(guiH, s), panelH, margin) * (double) s;
    }

    // ================================================================ dragging
    /**
     * A drag: the mouse is at ({@code mx}, {@code my}) in GUI pixels and took hold of the window at ({@code grabX}, {@code grabY}) inside it (GUI pixels from its
     * top-left); the window's top-left follows the mouse and is snapped to the nearest anchor.
     */
    public static Place drag(int guiW, int guiH, float scale, int panelW, int panelH, int margin, double mx, double my, double grabX, double grabY) {
        return snapGui(guiW, guiH, scale, panelW, panelH, margin, mx - grabX, my - grabY);
    }

    // ================================================================ resizing
    /** One wheel notch (+1 up = bigger, -1 down = smaller; the sign of {@code dir} is used) from a scale: 0.05, or 0.25 with {@code big}; rounded to the step and clamped. */
    public static float stepScale(float scale, double dir, boolean big) {
        if (dir == 0.0 || Double.isNaN(dir)) return OverlayMath.clampScale(scale);
        double step = (big ? BIG_STEP : WHEEL_STEP) * Math.signum(dir);
        double grid = 1.0 / WHEEL_STEP;
        return OverlayMath.clampScale(Math.round((scale + step) * grid) / grid);
    }

    /**
     * Resizes round a pivot: the point of the window at the fraction ({@code fx}, {@code fy}) of its size stays where it is on the screen. {@code left} and
     * {@code top} are the window's top-left in GUI pixels, {@code w} and {@code h} its size in window pixels, {@code oldScale} and {@code newScale} the scales.
     */
    public static Resized around(double left, double top, int w, int h, float oldScale, float newScale, double fx, double fy) {
        float ns = OverlayMath.clampScale(newScale);
        double px = left + fx * w * oldScale, py = top + fy * h * oldScale;
        return new Resized(ns, px - fx * w * ns, py - fy * h * ns);
    }

    /** A wheel notch over the window: the point under the mouse (limited to the window) stays put. */
    public static Resized wheel(double left, double top, int w, int h, float scale, double dir, boolean big, double mx, double my) {
        double fx = w <= 0 ? 0.0 : Math.max(0.0, Math.min(1.0, (mx - left) / (w * (double) scale)));
        double fy = h <= 0 ? 0.0 : Math.max(0.0, Math.min(1.0, (my - top) / (h * (double) scale)));
        return around(left, top, w, h, scale, stepScale(scale, dir, big), fx, fy);
    }

    /**
     * Dragging a corner handle (0 top-left, 1 top-right, 2 bottom-left, 3 bottom-right): the OPPOSITE corner stays where it is, and the dragged corner follows the
     * mouse along whichever axis the mouse has gone further in (the window keeps its shape). The scale is rounded to 0.01 and clamped to the allowed range.
     */
    public static Resized corner(int corner, double mx, double my, double left, double top, int w, int h, float scale) {
        boolean dragRight = corner == 1 || corner == 3, dragBottom = corner == 2 || corner == 3;
        double ox = dragRight ? left : left + w * (double) scale;               // the fixed (opposite) corner, GUI pixels
        double oy = dragBottom ? top : top + h * (double) scale;
        double rw = w <= 0 ? 1.0 : Math.abs(mx - ox) / w, rh = h <= 0 ? 1.0 : Math.abs(my - oy) / h;
        float ns = OverlayMath.clampScale(Math.round(Math.max(rw, rh) * 100.0) / 100.0);
        return new Resized(ns, dragRight ? ox : ox - w * (double) ns, dragBottom ? oy : oy - h * (double) ns);
    }

    /** The corner handle under the mouse, 0..3 (see {@link #corner}), or -1: a handle is a square of {@code reach} GUI pixels each way round the corner. */
    public static int cornerAt(double mx, double my, double left, double top, double w, double h, double reach) {
        double[][] pts = {{left, top}, {left + w, top}, {left, top + h}, {left + w, top + h}};
        int best = -1;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < 4; i++) {
            double dx = Math.abs(mx - pts[i][0]), dy = Math.abs(my - pts[i][1]);
            if (dx <= reach && dy <= reach && dx + dy < bestD) {
                bestD = dx + dy;
                best = i;
            }
        }
        return best;
    }
}
