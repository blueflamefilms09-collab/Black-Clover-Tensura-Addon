package com.newuniverse.nusmp.client.skilltree.shop;

/**
 * Why: a route book is named in the colour of its route (the Fire Dragon Slayer is orange, the Sky Dragon Slayer pale blue), and a route's colour is chosen for its own icon and chat
 * text, not for dim glass: a deep purple or a pure blue is nearly invisible on the shop's dark panel. Lifting every channel to a minimum (the first idea) turns a deep purple into a
 * grey-blue and loses the hue, which is the very thing the colour is for, so the lift keeps the hue: it first scales the colour until its brightest channel is strong, and then, for
 * the colours that are still dim to the eye however strong their peak (a saturated blue), mixes in white until the perceived brightness reaches a floor. A colour that is already
 * bright is left exactly as it is. Pure arithmetic with no game type, so it is unit tested (and the preview tool mirrors it).
 */
public final class ShopColors {
    private ShopColors() {}

    /** The least value the strongest channel may have. */
    public static final int MIN_PEAK = 0xE0;
    /** The least perceived brightness (0 to 255, the usual weights of red, green and blue) of a lifted colour. */
    public static final float MIN_LUMINANCE = 150f;

    /** The colour (0xRRGGBB, any alpha is ignored) made readable on the dark glass, hue kept. */
    public static int lift(int rgb) {
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        int peak = Math.max(r, Math.max(g, b));
        if (peak < 8) return 0xB0B0B0;
        if (peak < MIN_PEAK) {
            float f = MIN_PEAK / (float) peak;
            r = Math.min(255, Math.round(r * f));
            g = Math.min(255, Math.round(g * f));
            b = Math.min(255, Math.round(b * f));
        }
        float lum = luminance(r, g, b);
        if (lum < MIN_LUMINANCE) {
            float t = Math.min(0.65f, (MIN_LUMINANCE - lum) / (255f - lum));
            r = Math.round(r + (255 - r) * t);
            g = Math.round(g + (255 - g) * t);
            b = Math.round(b + (255 - b) * t);
        }
        return (r << 16) | (g << 8) | b;
    }

    /** Perceived brightness of a colour, 0 to 255. */
    public static float luminance(int r, int g, int b) {
        return 0.299f * r + 0.587f * g + 0.114f * b;
    }

    public static float luminance(int rgb) {
        return luminance((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF);
    }
}
