package com.newuniverse.nusmp.client.companion.gs;

import com.newuniverse.nusmp.client.ui.gs.GsPalette;
import com.newuniverse.nusmp.client.ui.gs.GsSkin;

/**
 * The Great Spirit look of a speech bubble over a companion's head: the numbers only (the draw stays in {@code BubbleClient}). The bubble pops in with a small overshoot, fades out in its last half
 * second, shows fewer lines when it is far away (two instead of four beyond 12 blocks, so a crowd of bubbles stays cheap and readable), wears its skin's colours (parchment and gold for skin B, violet and
 * magenta for skin C) and carries the speaker's name in italics above the words. It never adds a bubble or a line beyond the ones the base code already budgets (24 bubbles, 4 lines). Pure, unit tested.
 */
public final class BubbleStyle {
    private BubbleStyle() {}

    /** Ticks the pop-in takes and ticks the fade-out takes. */
    public static final float POP = 6f, FADE = 10f;
    /** Beyond this squared distance (12 blocks) a bubble shows {@link #FAR_LINES} lines; closer ones show {@link #NEAR_LINES}. */
    public static final double FAR_SQR = 12.0 * 12.0;
    public static final int NEAR_LINES = 4, FAR_LINES = 2;

    /** The scale of a bubble 'age' ticks after it appeared: 0.55 up to 1 with a little overshoot. */
    public static float pop(float age) {
        if (age >= POP) return 1f;
        if (age <= 0f) return 0.55f;
        float t = age / POP, c1 = 1.70158f, c3 = c1 + 1f, u = t - 1f;
        float back = 1f + c3 * u * u * u + c1 * u * u;
        return 0.55f + 0.45f * back;
    }

    /** Opacity 0..1 of a bubble that has 'remaining' ticks left (full until the last {@link #FADE} ticks). */
    public static float fade(float remaining) {
        if (remaining >= FADE) return 1f;
        if (remaining <= 0f) return 0f;
        return remaining / FADE;
    }

    /** The most lines to draw at a squared distance, never more than 'have'. */
    public static int lines(double distSqr, int have) { return Math.min(have, distSqr > FAR_SQR ? FAR_LINES : NEAR_LINES); }

    /** True when the bubble is faint enough that drawing it would only waste work (and the font's alpha under 4 would draw opaque). */
    public static boolean invisible(float opacity) { return opacity < 0.04f; }

    /** ARGB with the opacity applied to a 0xRRGGBB colour and a base alpha 0..255. */
    public static int argb(int rgb, int baseAlpha, float opacity) {
        int a = Math.max(0, Math.min(255, Math.round(baseAlpha * Math.max(0f, Math.min(1f, opacity)))));
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    public static int textColor(GsSkin s, float opacity) { return argb(GsPalette.TEXT[s.ordinal()], 255, opacity); }

    public static int nameColor(GsSkin s, float opacity) { return argb(GsPalette.NEON2[s.ordinal()], 255, opacity); }

    public static int tailColor(GsSkin s, float opacity) { return argb(GsPalette.NEON[s.ordinal()], 255, opacity); }

    /** The text background (the card behind a line). */
    public static int cardColor(GsSkin s, float opacity) { return argb(GsPalette.PANEL_TOP[s.ordinal()], 0xD8, opacity); }

    /** The darker background behind the name line. */
    public static int nameCardColor(GsSkin s, float opacity) { return argb(GsPalette.PANEL_BOTTOM[s.ordinal()], 0xE8, opacity); }
}
