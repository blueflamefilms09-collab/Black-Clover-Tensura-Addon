package com.newuniverse.nusmp.client.companion.gs;

import com.newuniverse.nusmp.client.playermodel.SkinPath;

/**
 * What the portrait needs to know about a companion's player-model skin: whether there is one, whether its arms are slim (3 texels) or wide (4), whether it is an old 64 by 32 skin (no second layer, the
 * left arm is the right arm turned round), and her skin tone (the colour the expression rectangles use to "erase" an eye or a mouth). It is read from the pixels once, when the screen opens (the game
 * converts the skin image to ARGB numbers; the preview tool reads a PNG the same way). Pure, unit tested.
 *
 * 3.38 (HD arc characters): a skin may be 64, 128 or 256 wide with the SAME layout (every model pixel a k by k block of texels, {@link SkinPath#validSkinSize}); the pixels are read at the centre of
 * the block of each model pixel, and {@code texH} stays the height in the 64 grid (64, or 32 for an old skin) because the portrait blits with fractions of that grid, which the GPU samples on the
 * finer texture by itself. Before this, an HD skin was "not a standard skin" and the portrait fell back to the clover crest.
 */
public record FaceInfo(boolean has, boolean slim, boolean legacy, int tone, int texH) {
    /** A generic skin tone, used when the skin has no opaque pixel where it is sampled. */
    public static final int DEFAULT_TONE = 0xFFE8BE9E;

    /** No skin: the portrait falls back to a crest. */
    public static final FaceInfo NONE = new FaceInfo(false, false, false, DEFAULT_TONE, 64);

    /** The width in texels of an arm. */
    public int armW() { return slim ? 3 : 4; }

    /**
     * Reads a skin of the vanilla layout at 64, 128, 256 ... texels wide ({@code argb.length / width} rows: a square, or half as many for an old skin). Anything else (too small, too big, not a
     * power-of-two multiple of 64, the wrong shape) is {@link #NONE}: the screen never draws a texture it did not check. The slim test is the transparent column at the 4th texel of the right arm; the
     * tone is the mean of two cheek-and-nose texels of the face.
     */
    public static FaceInfo from(int[] argb, int width, int height) {
        if (argb == null || !SkinPath.validSkinSize(width, width) || (height != width && height * 2 != width) || argb.length < width * height) return NONE;
        int k = width / SkinPath.BASE_SIZE;
        boolean legacy = height * 2 == width;
        boolean slim = !legacy && alpha(argb, width, k, 47, 24) == 0;
        int a = at(argb, width, k, 8 + 3, 8 + 6), b = at(argb, width, k, 8 + 4, 8 + 6);
        int tone = (a >>> 24) == 0 || (b >>> 24) == 0 ? DEFAULT_TONE : mean(a, b);
        return new FaceInfo(true, slim, legacy, tone, height / k);
    }

    /** The texel at the centre of model pixel (x, y) of the 64 grid, for a skin that is {@code k} times finer. */
    private static int at(int[] argb, int width, int k, int x, int y) { return argb[(y * k + k / 2) * width + x * k + k / 2]; }

    private static int alpha(int[] argb, int w, int k, int x, int y) { return at(argb, w, k, x, y) >>> 24; }

    private static int mean(int a, int b) {
        int r = (((a >> 16) & 255) + ((b >> 16) & 255)) / 2, g = (((a >> 8) & 255) + ((b >> 8) & 255)) / 2, bl = ((a & 255) + (b & 255)) / 2;
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }
}
