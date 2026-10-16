package com.newuniverse.nusmp.avatar.appearance;

/**
 * Why: the Avatar's body skin is made once per (body group, skin tone, outfit) by laying an outfit over a grey-shaded body template, tinted by the
 * creator's skin and eye colours. Doing the arithmetic on plain ARGB int arrays (instead of on the game's image class) keeps it testable without the
 * game and keeps the client class that owns the GPU texture tiny. Mask channels: red = how much of the pixel is flesh (tinted by the skin tone),
 * green = iris (tinted by the eye colour); the template's grey value is the shading that is kept.
 */
public final class SkinCompose {
    private SkinCompose() {}

    /** Quantises a colour to 4 bits per channel, so a continuous slider makes at most 4096 different textures, not 16 million. */
    public static int quantize(int rgb) {
        int r = ((rgb >> 16) & 0xF0) | ((rgb >> 20) & 0x0F), g = ((rgb >> 8) & 0xF0) | ((rgb >> 12) & 0x0F), b = (rgb & 0xF0) | ((rgb >> 4) & 0x0F);
        return (r << 16) | (g << 8) | b;
    }

    /**
     * @param base    the body template, ARGB, grey flesh
     * @param mask    the template's mask (same size): R flesh, G iris, alpha unused
     * @param outfit  the outfit overlay, ARGB, transparent where there is none (may be null)
     * @return a new ARGB array: base tinted, then the outfit over it
     */
    public static int[] compose(int[] base, int[] mask, int[] outfit, int skinRgb, int eyeRgb) {
        int n = base.length;
        int[] out = new int[n];
        for (int i = 0; i < n; i++) {
            int b = base[i], a = b >>> 24;
            int r = (b >> 16) & 255, g = (b >> 8) & 255, bl = b & 255;
            int m = mask == null ? 0 : mask[i];
            int flesh = (m >> 16) & 255, iris = (m >> 8) & 255;
            if (a != 0 && flesh > 0) {
                int[] t = tint(r, skinRgb, 236f);
                r = mix(r, t[0], flesh); g = mix(g, tint(g, skinRgb, 236f)[1], flesh); bl = mix(bl, tint(bl, skinRgb, 236f)[2], flesh);
            }
            if (a != 0 && iris > 0) {
                int lum = (r + g + bl) / 3;
                float k = Math.min(1.4f, lum / 150f);
                r = mix(r, clamp(((eyeRgb >> 16) & 255) * k), iris); g = mix(g, clamp(((eyeRgb >> 8) & 255) * k), iris); bl = mix(bl, clamp((eyeRgb & 255) * k), iris);
            }
            int px = (a << 24) | (r << 16) | (g << 8) | bl;
            if (outfit != null) {
                int o = outfit[i], oa = o >>> 24;
                if (oa == 255) px = o;
                else if (oa > 0) {
                    int or = (o >> 16) & 255, og = (o >> 8) & 255, ob = o & 255;
                    px = (Math.max(a, oa) << 24) | (mix(r, or, oa) << 16) | (mix(g, og, oa) << 8) | mix(bl, ob, oa);
                }
            }
            out[i] = px;
        }
        return out;
    }

    /** Multiplies a grey value (0..255) by the colour's three channels, normalised to the template's own flesh grey. Index 0 = red, 1 = green, 2 = blue of the result. */
    private static int[] tint(int grey, int rgb, float whiteGrey) {
        float k = Math.min(1.25f, grey / whiteGrey);
        return new int[]{clamp(((rgb >> 16) & 255) * k), clamp(((rgb >> 8) & 255) * k), clamp((rgb & 255) * k)};
    }

    private static int mix(int a, int b, int w) { return (a * (255 - w) + b * w) / 255; }

    private static int clamp(float v) { return v < 0 ? 0 : v > 255 ? 255 : (int) v; }
}
