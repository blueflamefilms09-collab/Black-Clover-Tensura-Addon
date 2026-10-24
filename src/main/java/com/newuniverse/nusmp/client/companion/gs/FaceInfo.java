package com.newuniverse.nusmp.client.companion.gs;

/**
 * What the portrait needs to know about a companion's player-model skin: whether there is one, whether its arms are slim (3 texels) or wide (4), whether it is an old 64 by 32 skin (no second layer, the
 * left arm is the right arm turned round), and her skin tone (the colour the expression rectangles use to "erase" an eye or a mouth). It is read from the pixels once, when the screen opens (the game
 * converts the skin image to ARGB numbers; the preview tool reads a PNG the same way). Pure, unit tested.
 */
public record FaceInfo(boolean has, boolean slim, boolean legacy, int tone, int texH) {
    /** A generic skin tone, used when the skin has no opaque pixel where it is sampled. */
    public static final int DEFAULT_TONE = 0xFFE8BE9E;

    /** No skin: the portrait falls back to a crest. */
    public static final FaceInfo NONE = new FaceInfo(false, false, false, DEFAULT_TONE, 64);

    /** The width in texels of an arm. */
    public int armW() { return slim ? 3 : 4; }

    /**
     * Reads a 64 wide skin of {@code argb.length / 64} rows. Anything else (too small, too big, not 64 wide) is {@link #NONE}: the screen never draws a texture it did not check. The slim test is the
     * transparent column at the 4th texel of the right arm; the tone is the mean of two cheek-and-nose texels of the face.
     */
    public static FaceInfo from(int[] argb, int width, int height) {
        if (argb == null || width != 64 || (height != 64 && height != 32) || argb.length < width * height) return NONE;
        boolean legacy = height == 32;
        boolean slim = !legacy && alpha(argb, width, 47, 24) == 0;
        int a = argb[(8 + 6) * width + (8 + 3)], b = argb[(8 + 6) * width + (8 + 4)];
        int tone = (a >>> 24) == 0 || (b >>> 24) == 0 ? DEFAULT_TONE : mean(a, b);
        return new FaceInfo(true, slim, legacy, tone, height);
    }

    private static int alpha(int[] argb, int w, int x, int y) { return argb[y * w + x] >>> 24; }

    private static int mean(int a, int b) {
        int r = (((a >> 16) & 255) + ((b >> 16) & 255)) / 2, g = (((a >> 8) & 255) + ((b >> 8) & 255)) / 2, bl = ((a & 255) + (b & 255)) / 2;
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }
}
