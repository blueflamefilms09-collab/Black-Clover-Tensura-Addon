package com.newuniverse.nusmp.client.ui.move;

/**
 * Why: every decision of the GUI Move system (where an element sits for a saved place, which anchor a dropped element gets, how far it may go, what it snaps to, how a
 * wheel notch or a key nudge changes it, and how the old System window offsets become a place of the new store) is plain arithmetic that nobody can see until the game runs.
 * So it lives here as pure functions (no Minecraft type) and is unit tested to the pixel; the glue ({@link GuiMove}) and the edit screen ({@link GuiMoveScreen}) only feed
 * it numbers.
 *
 * <p><b>The model.</b> A saved place is the model the System window already used since 1.58.0, so its saved offsets carry over unchanged: one of nine anchors (the four
 * corners by the bit pair {@code anchor}, plus a "measured from the middle" flag per axis) and an offset in the element's OWN pixels. "Own pixels" means the unit of the
 * element's unscaled design, so the offset scales with the element: an element dragged to 10 pixels from the right edge stays 10 own pixels from it whatever the GUI scale
 * or the window size, a centred one stays centred, and a window that gets smaller never leaves it out of sight (see {@link #clampVisible}).
 *
 * <p><b>Scale.</b> The size a player gives an element in edit mode ({@code moveScale}, 0.5 to 2.5) and the size the GUI Size sliders give it ({@code guiScale}, from
 * {@code GuiScale}) are two factors of ONE number, {@link #total}: they multiply, and an element is drawn at {@code total} exactly once. Pure.
 */
public final class GuiMoveMath {
    private GuiMoveMath() {}

    /** The smallest and largest size a player can give an element, the wheel step (Shift: the big one) and the limits of the product with the GUI Size. */
    public static final float MIN_SCALE = 0.5f, MAX_SCALE = 2.5f, STEP = 0.05f, BIG_STEP = 0.25f, MIN_TOTAL = 0.25f, MAX_TOTAL = 5f;
    /** At least this many GUI pixels of an element (or all of it, when it is smaller) always stay on the screen. */
    public static final int MIN_VISIBLE = 24;
    /** The largest offset (own pixels) a layout keeps. */
    public static final int MAX_OFFSET = 4096;
    /** A key nudge in GUI pixels (Shift: the big one) and the distance within which an edge snaps to another edge. */
    public static final int NUDGE = 1, BIG_NUDGE = 8;
    public static final double SNAP_DISTANCE = 6.0;

    /** A rectangle in GUI pixels. */
    public record Rect(double x, double y, double w, double h) {
        public double right() { return x + w; }
        public double bottom() { return y + h; }
        public double cx() { return x + w / 2.0; }
        public double cy() { return y + h / 2.0; }
        public boolean contains(double px, double py) { return px >= x && px < x + w && py >= y && py < y + h; }
    }

    /**
     * A saved place: the corner bits ({@code anchor}: 0 top-left, 1 top-right, 2 bottom-left, 3 bottom-right), whether each axis is measured from the middle of the screen
     * instead, and the offsets in own pixels, inwards from the edge (or from the middle). The same record as the System window's {@code OverlayDrag.Place}.
     */
    public record Place(int anchor, boolean centerX, boolean centerY, int offX, int offY) {
        public static final Place DEFAULT = new Place(0, false, false, 0, 0);

        public boolean right() { return (anchor & 1) != 0; }
        public boolean bottom() { return (anchor & 2) != 0; }
    }

    /** A float that is not a number or not finite reads as {@code fallback}. */
    public static float sane(double v, float fallback) { return Double.isNaN(v) || Double.isInfinite(v) ? fallback : (float) v; }

    public static int clampInt(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }

    public static int clampOffset(int v) { return clampInt(v, -MAX_OFFSET, MAX_OFFSET); }

    /** A corner number forced into 0..3. */
    public static int safeAnchor(int a) { return a >= 0 && a <= 3 ? a : 0; }

    /** A place with every field forced into its range. */
    public static Place sanitize(Place p) {
        if (p == null) return Place.DEFAULT;
        return new Place(safeAnchor(p.anchor()), p.centerX(), p.centerY(), clampOffset(p.offX()), clampOffset(p.offY()));
    }

    // ================================================================ scale
    /** The player's own size of an element: 0.5 to 2.5, a number that is not one reads as 1. */
    public static float clampScale(double s) { return Math.max(MIN_SCALE, Math.min(MAX_SCALE, sane(s, 1f))); }

    /** One wheel notch (the sign of {@code dir} is used) from a scale, 0.05 or 0.25 with {@code big}; rounded to the step and clamped. */
    public static float stepScale(float scale, double dir, boolean big) {
        if (dir == 0.0 || Double.isNaN(dir)) return clampScale(scale);
        double step = (big ? BIG_STEP : STEP) * Math.signum(dir);
        double grid = 1.0 / STEP;
        return clampScale(Math.round((scale + step) * grid) / grid);
    }

    /**
     * THE order of the two sizes: the player's size in edit mode times the size of the GUI Size sliders, one product, applied once. Held to [0.25, 5] so two large
     * values cannot draw an element bigger than any screen.
     */
    public static float total(float moveScale, float guiScale) {
        float m = sane(moveScale, 1f), g = sane(guiScale, 1f);
        return Math.max(MIN_TOTAL, Math.min(MAX_TOTAL, m * g));
    }

    // ================================================================ where an element sits
    /** An extent of the screen in own pixels: the GUI-scaled size divided by the total scale, at least 1. */
    public static int own(int guiExtent, float total) { return Math.max(1, Math.round(guiExtent / Math.max(0.1f, total))); }

    /**
     * Keeps at least {@link #MIN_VISIBLE} GUI pixels of an element on the screen along one axis (all of it when it is smaller than that): the position is limited to
     * [visible - size, screen - visible]. An element that fits is NOT pushed fully inside, so a big panel can hang over an edge, but never out of reach.
     */
    public static double clampVisible(double pos, double size, double screen) {
        double vis = Math.min(MIN_VISIBLE, Math.max(1.0, size));
        double lo = vis - size, hi = screen - vis;
        if (Double.isNaN(pos)) return Math.max(lo, Math.min(hi, 0.0));
        return Math.max(lo, Math.min(Math.max(lo, hi), pos));
    }

    /** One axis of a place in GUI pixels: from the near edge, the far edge or the middle, then held on the screen. */
    public static double axisGui(boolean far, boolean center, int screenGui, float total, int sizeOwn, int margin, int offset) {
        int screenOwn = own(screenGui, total);
        int raw = center ? (screenOwn - sizeOwn) / 2 + offset : far ? screenOwn - sizeOwn - margin - offset : margin + offset;
        return clampVisible(raw * (double) total, sizeOwn * (double) total, screenGui);
    }

    /** The left edge of an element at a saved place, in GUI pixels. */
    public static double leftGui(Place p, int guiW, float total, int wOwn, int margin) {
        return axisGui(p.right(), p.centerX(), guiW, total, wOwn, margin, p.offX());
    }

    /** The top edge of an element at a saved place, in GUI pixels. */
    public static double topGui(Place p, int guiH, float total, int hOwn, int margin) {
        return axisGui(p.bottom(), p.centerY(), guiH, total, hOwn, margin, p.offY());
    }

    /** The rectangle (GUI pixels) an element of {@code wOwn x hOwn} own pixels covers at a saved place. */
    public static Rect rect(Place p, int guiW, int guiH, float total, int wOwn, int hOwn, int margin) {
        return new Rect(leftGui(p, guiW, total, wOwn, margin), topGui(p, guiH, total, hOwn, margin), wOwn * (double) total, hOwn * (double) total);
    }

    /**
     * The place that puts an element's top-left corner at ({@code leftGui}, {@code topGui}) (GUI pixels, held on the screen first) and anchors it to the NEAREST of the
     * nine anchor points: an axis is "middle" when the element's centre is in the middle third of the screen, else it is the near side. The offsets are computed for that
     * anchor, so {@link #leftGui} / {@link #topGui} of the result give the held corner back (to the rounding of one own pixel).
     */
    public static Place placeFromGui(int guiW, int guiH, float total, int wOwn, int hOwn, int margin, double leftGui, double topGui) {
        double l = clampVisible(leftGui, wOwn * (double) total, guiW), t = clampVisible(topGui, hOwn * (double) total, guiH);
        double cx = l + wOwn * (double) total / 2.0, cy = t + hOwn * (double) total / 2.0;
        boolean right = cx >= guiW / 2.0, bottom = cy >= guiH / 2.0;
        boolean centerX = Math.abs(cx - guiW / 2.0) < guiW / 6.0, centerY = Math.abs(cy - guiH / 2.0) < guiH / 6.0;
        int sx = own(guiW, total), sy = own(guiH, total);
        int xo = (int) Math.round(l / total), yo = (int) Math.round(t / total);
        int ox = centerX ? xo - (sx - wOwn) / 2 : right ? sx - wOwn - margin - xo : xo - margin;
        int oy = centerY ? yo - (sy - hOwn) / 2 : bottom ? sy - hOwn - margin - yo : yo - margin;
        return new Place((bottom ? 2 : 0) | (right ? 1 : 0), centerX, centerY, clampOffset(ox), clampOffset(oy));
    }

    // ================================================================ resizing and nudging
    /** The new size and the top-left corner (GUI pixels) a resize leaves. */
    public record Resized(float moveScale, double left, double top) {}

    /**
     * A wheel notch over an element: the point under the mouse (limited to the element) stays where it is on the screen, as {@code OverlayDrag.wheel} does for the
     * System window. {@code left}/{@code top} are the element's corner now, {@code wOwn}/{@code hOwn} its own size.
     */
    public static Resized wheel(double left, double top, int wOwn, int hOwn, float moveScale, float guiScale, double dir, boolean big, double mx, double my) {
        float newScale = stepScale(moveScale, dir, big);
        float t0 = total(moveScale, guiScale), t1 = total(newScale, guiScale);
        double fx = wOwn <= 0 ? 0.0 : Math.max(0.0, Math.min(1.0, (mx - left) / (wOwn * (double) t0)));
        double fy = hOwn <= 0 ? 0.0 : Math.max(0.0, Math.min(1.0, (my - top) / (hOwn * (double) t0)));
        double px = left + fx * wOwn * t0, py = top + fy * hOwn * t0;
        return new Resized(newScale, px - fx * wOwn * t1, py - fy * hOwn * t1);
    }

    /** The place after a key nudge of ({@code dx}, {@code dy}) GUI pixels (right and down are positive) from where the element really is. */
    public static Place nudge(Place p, int guiW, int guiH, float total, int wOwn, int hOwn, int margin, int dx, int dy) {
        return placeFromGui(guiW, guiH, total, wOwn, hOwn, margin, leftGui(p, guiW, total, wOwn, margin) + dx, topGui(p, guiH, total, hOwn, margin) + dy);
    }

    // ================================================================ snapping
    /** The result of snapping a rectangle: where it goes, and the lines to draw ({@code NaN} for none). */
    public record Snapped(double x, double y, double guideX, double guideY) {
        public boolean snappedX() { return !Double.isNaN(guideX); }
        public boolean snappedY() { return !Double.isNaN(guideY); }
    }

    /**
     * Snaps a rectangle that is being dragged: its left, middle and right edge snap to the screen's left edge, middle and right edge and to the same three lines of every
     * other rectangle; the same vertically. The nearest line within {@code dist} wins on each axis; the rest is untouched. {@code others} may hold nulls (skipped). Used
     * only while the edit screen is open (it allocates a little).
     */
    public static Snapped snap(Rect moving, Rect[] others, int guiW, int guiH, double dist) {
        double bestDx = Double.NaN, bestGx = Double.NaN, bestDy = Double.NaN, bestGy = Double.NaN;
        double[] mine = {moving.x(), moving.cx(), moving.right()};
        double[] mineY = {moving.y(), moving.cy(), moving.bottom()};
        int n = others == null ? 0 : others.length;
        for (int k = -1; k < n; k++) {
            double[] tx, ty;
            if (k < 0) {
                tx = new double[]{0, guiW / 2.0, guiW};
                ty = new double[]{0, guiH / 2.0, guiH};
            } else {
                Rect o = others[k];
                if (o == null) continue;
                tx = new double[]{o.x(), o.cx(), o.right()};
                ty = new double[]{o.y(), o.cy(), o.bottom()};
            }
            for (double m : mine) for (double t : tx) {
                double d = t - m;
                if (Math.abs(d) <= dist && (Double.isNaN(bestDx) || Math.abs(d) < Math.abs(bestDx))) { bestDx = d; bestGx = t; }
            }
            for (double m : mineY) for (double t : ty) {
                double d = t - m;
                if (Math.abs(d) <= dist && (Double.isNaN(bestDy) || Math.abs(d) < Math.abs(bestDy))) { bestDy = d; bestGy = t; }
            }
        }
        return new Snapped(moving.x() + (Double.isNaN(bestDx) ? 0 : bestDx), moving.y() + (Double.isNaN(bestDy) ? 0 : bestDy), bestGx, bestGy);
    }

    // ================================================================ the old System window
    /**
     * The 1.58.0 System window place (corner bits, middle flags, offsets in the window's own pixels) as a place of the new store. The model is the same, so nothing is
     * converted: the numbers are only forced into range. A saved offset of the old config therefore lands at exactly the same spot.
     */
    public static Place fromLegacy(int anchor, boolean centerX, boolean centerY, int offX, int offY) {
        return new Place(safeAnchor(anchor), centerX, centerY, clampOffset(offX), clampOffset(offY));
    }

    // ================================================================ a point in an element's own space
    /** A mouse coordinate of the screen in the space an element draws in, when its default corner {@code defPos} now sits at {@code newPos} at {@code total}. */
    public static double toLocal(double mouse, double newPos, double defPos, float total) { return (mouse - newPos) / total + defPos; }

    /** The other way: where a point of the element's own space lands on the screen. */
    public static double toScreen(double local, double newPos, double defPos, float total) { return (local - defPos) * total + newPos; }
}
