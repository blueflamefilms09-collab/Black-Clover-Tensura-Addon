package com.newuniverse.nusmp.client.skilltree;

import com.newuniverse.nusmp.skilltree.item.UnlockTiming;

/**
 * Why: the "ROUTE UNLOCKED" banner has to feel like the title card of an anime power-up (a white-hot flash, the big words slamming in a little too large and settling,
 * a line drawing itself under them, then a slow fade over three seconds), and all of that is a function of one number: the banner's age in ticks, counted from the
 * burst of the circle on the server. Keeping it as plain arithmetic with no Minecraft type means the timing is unit tested on the edges that matter (nothing shows
 * before the burst, the words are fully opaque before they start to fade, the flash is gone before the words are, every value stays in range, and the banner is over
 * exactly when its life ends), and the screen code only reads these numbers and draws. Ages are game ticks plus the partial tick so the animation is smooth at any
 * frame rate. The life is {@link UnlockTiming#BANNER_TICKS}, the same constant the server-side staging is written against. Pure.
 */
public final class BannerTimeline {
    private BannerTimeline() {}

    /** Ticks the banner lives (three seconds). */
    public static final float LIFE = UnlockTiming.BANNER_TICKS;
    /** Ticks the words take to fade in. */
    public static final float FADE_IN = 4f;
    /** Ticks the fade at the end takes. */
    public static final float FADE_OUT = 24f;
    /** Ticks the words take to settle from their slam size to normal. */
    public static final float POP = 9f;
    /** The size factor the words start at. */
    public static final float POP_FROM = 1.7f;
    /** Ticks the screen flash lasts. */
    public static final float FLASH = 16f;
    /** Ticks the line under the words takes to draw itself. */
    public static final float RULE = 12f;

    /** Is the banner over? */
    public static boolean done(float age) { return age >= LIFE; }

    /** Opacity of the words, 0 before the burst and after the end, a quick fade in, full, then the long fade out. */
    public static float alpha(float age) {
        if (age <= 0f || age >= LIFE) return 0f;
        float in = clamp01(age / FADE_IN);
        float out = clamp01((LIFE - age) / FADE_OUT);
        return Math.min(in, out);
    }

    /** Size factor of the words: {@link #POP_FROM} at the burst, settling to 1 with a small overshoot (the slam), then 1. */
    public static float scale(float age) {
        float t = clamp01(age / POP);
        return POP_FROM + (1f - POP_FROM) * easeOutBack(t);
    }

    /** Strength of the screen flash: 1 at the burst, falling away quadratically to 0 at {@link #FLASH}; 0 before the burst. */
    public static float flash(float age) {
        if (age < 0f || age >= FLASH) return 0f;
        float u = 1f - age / FLASH;
        return u * u;
    }

    /** How much of the line under the words is drawn, 0 to 1 (fast, then slowing). */
    public static float rule(float age) {
        float t = clamp01(age / RULE);
        float u = 1f - t;
        return 1f - u * u * u;
    }

    /** Packs an RGB colour and an opacity 0..1 into 0xAARRGGBB. */
    public static int argb(int rgb, float alpha) {
        int a = Math.round(clamp01(alpha) * 255f);
        return (a << 24) | (rgb & 0xFFFFFF);
    }

    /** Mixes an RGB colour toward white by {@code amount} 0..1. */
    public static int lighten(int rgb, float amount) {
        float t = clamp01(amount);
        int r = (rgb >> 16) & 0xFF, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
        r += Math.round((255 - r) * t);
        g += Math.round((255 - g) * t);
        b += Math.round((255 - b) * t);
        return (r << 16) | (g << 8) | b;
    }

    /**
     * A readable name for a route the client has no copy of yet (the packet arrived before the registry sync, which should not happen but must not show a blank banner):
     * the path of the id with underscores turned into spaces and each word capitalised, so {@code nusmp:lady_luck} reads "Fire Dragon Slayer".
     */
    public static String fallbackName(String routeId) {
        if (routeId == null || routeId.isEmpty()) return "";
        String path = routeId.substring(routeId.indexOf(':') + 1);
        StringBuilder out = new StringBuilder();
        for (String word : path.split("[_/\\s]+")) {
            if (word.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return out.toString();
    }

    private static float easeOutBack(float t) {
        float c1 = 1.70158f, c3 = c1 + 1f, u = t - 1f;
        return 1f + c3 * u * u * u + c1 * u * u;
    }

    private static float clamp01(float v) { return v < 0f ? 0f : Math.min(1f, v); }
}
