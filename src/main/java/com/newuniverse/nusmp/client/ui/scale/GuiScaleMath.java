package com.newuniverse.nusmp.client.ui.scale;

/**
 * Why: every size decision of the GUI Size system lives here as plain numbers (no Minecraft classes), so the rules can be unit tested:
 * clamping, which of the global / per-GUI values wins, the fit-to-window limit, rounding for readable text, and the mouse transform that makes
 * clicks line up with a scaled screen.
 */
public final class GuiScaleMath {
    public static final double MIN = 0.5, MAX = 2.0, DEFAULT = 1.0;
    /** Scale values are rounded to this step so the bitmap font is not drawn at awkward fractions. */
    public static final double STEP = 0.05;

    private GuiScaleMath() {}

    /** The nine places a HUD element can be anchored; the scale is applied around that point of the screen. */
    public enum Anchor {
        TOP_LEFT(0f, 0f), TOP_CENTER(0.5f, 0f), TOP_RIGHT(1f, 0f),
        CENTER_LEFT(0f, 0.5f), CENTER(0.5f, 0.5f), CENTER_RIGHT(1f, 0.5f),
        BOTTOM_LEFT(0f, 1f), BOTTOM_CENTER(0.5f, 1f), BOTTOM_RIGHT(1f, 1f);

        private final float fx, fy;
        Anchor(float fx, float fy) { this.fx = fx; this.fy = fy; }
        public float pivotX(float width) { return fx * width; }
        public float pivotY(float height) { return fy * height; }
    }

    /** A scale kept inside [MIN, MAX]; anything that is not a number becomes 1. */
    public static double clamp(double s) {
        if (Double.isNaN(s) || Double.isInfinite(s)) return DEFAULT;
        return Math.max(MIN, Math.min(MAX, s));
    }

    /** Rounded to the nearest {@link #STEP}, then clamped. */
    public static double snap(double s) {
        return clamp(Math.round(clamp(s) / STEP) * STEP);
    }

    /** A per-GUI override: 0 (or below, or not a number) means "follow the global value". */
    public static boolean hasOverride(double override) {
        return !Double.isNaN(override) && override > 0.0;
    }

    /** The scale a GUI asks for: its own override when it has one, else the global value. */
    public static double resolve(double global, double override) {
        return snap(hasOverride(override) ? override : global);
    }

    /**
     * The largest scale at which a design of designW x designH still fits a window of guiW x guiH. A design with no size (0) counts as the whole window,
     * so only shrinking is possible. Never below 1, so a window that is already too small is not shrunk against the player's will.
     */
    public static double fitLimit(double designW, double designH, double guiW, double guiH) {
        double dw = designW > 0 ? designW : guiW, dh = designH > 0 ? designH : guiH;
        if (dw <= 0 || dh <= 0 || guiW <= 0 || guiH <= 0) return 1.0;
        return Math.max(1.0, Math.min(guiW / dw, guiH / dh));
    }

    /** The scale actually drawn: the requested one, held to {@link #fitLimit} when fit-to-window is on. Rounded down to the step so it never overshoots. */
    public static double effective(double requested, boolean fit, double designW, double designH, double guiW, double guiH) {
        double s = clamp(requested);
        if (!fit) return s;
        double limit = fitLimit(designW, designH, guiW, guiH);
        if (s <= limit) return s;
        return Math.max(MIN, Math.floor(limit / STEP + 1e-9) * STEP);
    }

    /** Screen-space mouse coordinate to the coordinate the (scaled) screen believes: inverse of scaling by s around pivot. */
    public static double toLocal(double mouse, double pivot, double s) {
        return (mouse - pivot) / s + pivot;
    }

    /** The other way: where a point of the unscaled layout lands on the screen. */
    public static double toScreen(double local, double pivot, double s) {
        return (local - pivot) * s + pivot;
    }

    /** A drag distance as the scaled screen sees it. */
    public static double deltaToLocal(double delta, double s) {
        return delta / s;
    }

    /** True when the scale is close enough to 1 that nothing needs to be hooked. */
    public static boolean isIdentity(double s) {
        return Math.abs(s - 1.0) < 1e-6;
    }

    /** A number from a config or a text box: "1.25", "1,25", "125%" is read as 125 (not a fraction), a Number as is. Not a number gives NaN. */
    public static double parse(Object raw) {
        if (raw instanceof Number n) return n.doubleValue();
        if (raw == null) return Double.NaN;
        try {
            return Double.parseDouble(raw.toString().trim().replace(',', '.').replace("%", ""));
        } catch (NumberFormatException e) {
            return Double.NaN;
        }
    }

    /** A percentage text such as "125%" (the slider label). */
    public static String percent(double s) {
        return Math.round(s * 100.0) + "%";
    }
}
