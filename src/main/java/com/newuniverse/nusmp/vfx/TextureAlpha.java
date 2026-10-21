package com.newuniverse.nusmp.vfx;

/**
 * Why (docs/vfx_square_bug.md): vanilla's {@code RenderType.eyes} adds the texture's RGB to the screen and does not weigh it by the texture's alpha. A sprite whose transparent texels still carry
 * a colour (the usual way to paint a soft ring or glow: colour everywhere, alpha shaping it, so the edges never get a dark fringe) therefore shows up as a solid square of that colour. This pure
 * class reads the pixels of a texture once and says which of those cases it is, so the renderer can pick the blend that honours alpha only where it is needed. No game classes, unit-tested.
 */
public final class TextureAlpha {
    private TextureAlpha() {}

    public enum Kind {
        /** Every texel is transparent. */
        EMPTY,
        /** No transparent texel and dark corners: a skin or a pattern on black (meant to be added as it is). */
        OPAQUE,
        /** No transparent texel and bright corners: drawn on a flat quad it is a solid square (a ring or glow that lost its alpha). */
        OPAQUE_BRIGHT_EDGE,
        /** Transparent texels are black (colour premultiplied by alpha): adding the RGB is right with any blend. */
        BLACK_UNDER_ALPHA,
        /** Transparent texels carry a colour: with an additive blend that ignores alpha every one of them is lit, the square. */
        COLOUR_UNDER_ALPHA
    }

    /** A texel with at most this alpha counts as transparent. */
    public static final int TRANSPARENT_MAX_ALPHA = 8;
    /** A transparent texel whose brightest channel is above this carries a colour. */
    public static final int DARK_MAX = 24;
    /** The share of transparent texels that carry a colour from which the texture is {@link Kind#COLOUR_UNDER_ALPHA}. */
    public static final float COLOUR_SHARE = 0.05f;
    /** A corner whose brightest channel is above this is bright. */
    public static final int BRIGHT_CORNER = 96;

    /** Classifies a texture given as {@code w * h} ARGB ints (row by row). */
    public static Kind classify(int w, int h, int[] argb) {
        if (w <= 0 || h <= 0 || argb == null || argb.length < w * h) return Kind.EMPTY;
        long transparent = 0, coloured = 0;
        for (int i = 0, n = w * h; i < n; i++) {
            int p = argb[i];
            if ((p >>> 24) > TRANSPARENT_MAX_ALPHA) continue;
            transparent++;
            if (brightest(p) > DARK_MAX) coloured++;
        }
        if (transparent == (long) w * h) return Kind.EMPTY;
        if (transparent == 0) return cornersBright(w, h, argb) ? Kind.OPAQUE_BRIGHT_EDGE : Kind.OPAQUE;
        if (coloured > transparent * (double) COLOUR_SHARE) return Kind.COLOUR_UNDER_ALPHA;
        return straightAlpha(w, h, argb) ? Kind.COLOUR_UNDER_ALPHA : Kind.BLACK_UNDER_ALPHA;
    }

    /** 1.110.0: a texel is "straight" when its colour is brighter than its own alpha (a premultiplied texel never is). */
    public static final int STRAIGHT_MARGIN = 48;
    /** The share of the semi-transparent texels that are straight from which the texture is not premultiplied (a white card with a soft alpha: ONE, ONE would light it fully). */
    public static final float STRAIGHT_SHARE = 0.25f;

    /** True when most of the semi-transparent texels (alpha above {@link #TRANSPARENT_MAX_ALPHA}, below 255) carry a colour much brighter than their alpha. */
    public static boolean straightAlpha(int w, int h, int[] argb) {
        long semi = 0, straight = 0;
        for (int i = 0, n = w * h; i < n; i++) {
            int p = argb[i], a = p >>> 24;
            if (a <= TRANSPARENT_MAX_ALPHA || a >= 255) continue;
            semi++;
            if (brightest(p) > a + STRAIGHT_MARGIN) straight++;
        }
        return semi > 0 && straight > semi * (double) STRAIGHT_SHARE;
    }

    /** True when the four corner patches (a sixteenth of the side, at least one texel) are all transparent (alpha at most {@code maxAlpha}). */
    public static boolean cornersTransparent(int w, int h, int[] argb, int maxAlpha) {
        int k = patch(w, h);
        for (int c = 0; c < 4; c++) {
            int x0 = (c & 1) == 0 ? 0 : w - k, y0 = (c & 2) == 0 ? 0 : h - k;
            for (int y = y0; y < y0 + k; y++) for (int x = x0; x < x0 + k; x++) if ((argb[y * w + x] >>> 24) > maxAlpha) return false;
        }
        return true;
    }

    private static boolean cornersBright(int w, int h, int[] argb) {
        int k = patch(w, h);
        for (int c = 0; c < 4; c++) {
            int x0 = (c & 1) == 0 ? 0 : w - k, y0 = (c & 2) == 0 ? 0 : h - k;
            int best = 0;
            for (int y = y0; y < y0 + k; y++) for (int x = x0; x < x0 + k; x++) best = Math.max(best, brightest(argb[y * w + x]));
            if (best <= BRIGHT_CORNER) return false;
        }
        return true;
    }

    private static int patch(int w, int h) { return Math.max(1, Math.min(w, h) / 16); }

    private static int brightest(int argb) { return Math.max((argb >> 16) & 255, Math.max((argb >> 8) & 255, argb & 255)); }
}
