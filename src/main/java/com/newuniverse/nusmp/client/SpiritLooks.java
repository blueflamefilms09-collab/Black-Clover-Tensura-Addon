package com.newuniverse.nusmp.client;

/**
 * 1.99.0 Great Spirits: the pure numbers behind the looks of the four Great Spirits (no game classes, unit tested by SpiritLooksTest). The drawing lives in {@link GreatSpiritModels} and
 * {@link ElementalSpiritRenderer}; what is decided here is "which clip now", "how big" and "what colour the iridescence has this tick".
 *
 * <p>Canon (the owner's text and pictures): Sylph is a TINY winged fairy (about 0.4 of a block), Undine a GIANT elegant adult mermaid (3 to 4 blocks from the crown to the tail fin),
 * Salamander a MASSIVE dragon, the earth spirit an unnamed stone giant (its name, look and host were never shown, so it stays generic).</p>
 */
public final class SpiritLooks {
    private SpiritLooks() {}

    /**
     * True: the renderer draws the canon models (tiny Sylph with honeycomb wings, the giant mermaid Undine, the blaze layer of Salamander). False: the models of 0.102 (a lime-green fairy
     * and a woman with a wave skirt), which stay on disk and keep working. A switch, so the older look can be brought back by changing one value.
     */
    public static final boolean CANON = true;

    /** The Sylph models are authored at four times their drawn size (27 px) so the face and the wing cells have texels; this is the factor they are drawn at (about 0.42 of a block). */
    public static final float SYLPH_SCALE = 0.22f;
    /** The Undine mermaid is authored at her drawn size (about 3.5 blocks head to tail fin). */
    public static final float UNDINE_SCALE = 1.0f;
    /** Salamander's body is drawn this much bigger: a MASSIVE dragon. (The hit box grows with it, see ElementalSpiritLordEntity.Kind.) */
    public static final float SALAMANDER_SCALE = 1.3f;

    /** Seconds between two annoyed hops of an idle Sylph, and how long the hop lasts (the clip "annoyed" of her model). */
    public static final float ANNOYED_EVERY = 9.0f, ANNOYED_LENGTH = 1.0f;

    /** Seconds into the annoyed hop at this clock (seconds), or -1 when she is not hopping. {@code phase} (0..1) shifts the cycle so two spirits do not hop together. */
    public static float annoyedAt(float seconds, float phase) {
        float t = (seconds + phase * ANNOYED_EVERY) % ANNOYED_EVERY;
        if (t < 0f) t += ANNOYED_EVERY;
        return t < ANNOYED_LENGTH ? t : -1f;
    }

    /**
     * The tint of the iridescent glow of Sylph's wings at this clock (client ticks with the partial tick): the thin-film hue wanders green - blue - violet - pink and back, slowly, as
     * light does over a soap bubble. Returns 0xAARRGGBB with the given alpha (0..255).
     */
    public static int filmArgb(float age, int alpha) {
        float k = 0.5f + 0.5f * (float) Math.sin(age * 0.045f);
        float hue = 0.30f + 0.62f * k;                       // 0.30 green .. 0.92 pink
        return (Math.max(0, Math.min(255, alpha)) << 24) | hsv(hue, 0.42f, 1.0f);
    }

    /** The hue (0..1) used by {@link #filmArgb} at a clock: exposed for the test. */
    public static float filmHue(float age) { return 0.30f + 0.62f * (0.5f + 0.5f * (float) Math.sin(age * 0.045f)); }

    /** HSV (each 0..1) to 0xRRGGBB. */
    public static int hsv(float h, float s, float v) {
        h = h - (float) Math.floor(h);
        int i = (int) (h * 6f);
        float f = h * 6f - i, p = v * (1f - s), q = v * (1f - s * f), t = v * (1f - s * (1f - f));
        float r, g, b;
        switch (i % 6) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return (Math.round(r * 255f) << 16) | (Math.round(g * 255f) << 8) | Math.round(b * 255f);
    }

    /** A grey glow tint of the given level (0..1) as 0xFFRRGGBB: the usual emissive pulse of a spirit body. */
    public static int greyArgb(float level) {
        int g = Math.max(0, Math.min(255, Math.round(255f * level)));
        return 0xFF000000 | (g << 16) | (g << 8) | g;
    }

    /** How big the model of a kind is drawn: Sylph tiny, Undine as authored, Salamander 1.3 times, the earth giant as it is. */
    public static float scale(String kind) {
        if (!CANON) return 1f;
        return switch (kind) {
            case "SYLPH" -> SYLPH_SCALE;
            case "UNDINE" -> UNDINE_SCALE;
            case "SALAMANDER" -> SALAMANDER_SCALE;
            default -> 1f;
        };
    }
}
