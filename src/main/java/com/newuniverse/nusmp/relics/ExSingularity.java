package com.newuniverse.nusmp.relics;

import java.util.Set;

/**
 * Why: the owner asked for a relic grade ABOVE Genesis, "EX-Singularity" ({@link Grade#AKASHIC}), and for it to look far cooler than Genesis. Genesis shows a pale, low-saturation
 * rainbow on its grade row; EX-Singularity is the full prismatic spectrum, fully saturated, with a white glint that runs along the text, a frame that cycles round the slot, and a sword
 * glow that walks through every hue. Everything colour-shaped lives here as pure arithmetic (no Minecraft type, no allocation: ints and floats in, an int out) so the tooltip, the
 * inventory frame, the sword's glow pass and the particle motes all read ONE palette and the tests can pin it down. The drawing is client-only ({@code client.ex.ExSingularityClient},
 * {@code client.ExSingularityFx}, {@code client.ui.tooltip.PrismText}); nothing here touches a client class (rule 9) and nothing here rolls loot: the grade is never rolled
 * ({@link Grade#rollable()}), only items that name it by hand carry it ({@link #isAkashicPath}).
 * <p>
 * Worst-case numbers this module's draw code is built on (docs/relics/ex_singularity.md): tooltip text at most {@link #MAX_GLYPHS} glyph draws per hovered tooltip; the slot frame
 * {@link #FRAME_FILLS} fills per decorated slot and at most {@link #FRAME_SLOTS_PER_FRAME} slots a frame; the sword at most {@link #MAX_SHARDS} shards (four quads each: two crossed planes, both faces) plus the blade veil (the
 * blade's own quads once more, about 450) on top of ONE glow pass, fewer quads than the two glow passes a Genesis weapon has; at most {@link #MOTES_PER_SECOND} particle motes a second per carrier.
 */
public final class ExSingularity {
    private ExSingularity() {}

    /** The grade name as it is shown. */
    public static final String NAME = "EX-Singularity";

    /** The star run of the tooltip: nine stars between two four-pointed ones, a row that is plainly longer than Genesis's seven. */
    public static final String STARS = "✦★★★★★★★★★✦";

    /** Items (by registry path) that carry the grade by hand: only Shishou. */
    private static final Set<String> AKASHIC_PATHS = Set.of("shishou");

    /** Does the item with this registry path show the EX-Singularity grade? */
    public static boolean isAkashicPath(String path) { return path != null && AKASHIC_PATHS.contains(path); }

    // ---------------------------------------------------------------- the budgets the draw code obeys
    /** The most glyphs a tooltip row draws one by one (a longer row falls back to one plain colour). */
    public static final int MAX_GLYPHS = 64;
    /** Fills one decorated inventory slot costs (batched into one managed draw). */
    public static final int FRAME_FILLS = 23;
    /** Decorated slots drawn in one frame (Shishou is one in the world; the hotbar, the inventory and the cursor can show it at most a few times). */
    public static final int FRAME_SLOTS_PER_FRAME = 4;
    /** Shards that orbit the sword (two crossed quads each). */
    public static final int MAX_SHARDS = 8;
    /** Particle motes a carrier or the entity sheds per second (the perf budget still applies on top). */
    public static final int MOTES_PER_SECOND = 5;

    // ---------------------------------------------------------------- hue arithmetic
    /** 0xRRGGBB of a hue in turns (any float; only its fractional part counts), saturation and value in 0..1. No allocation. */
    public static int hsv(float hue, float sat, float val) {
        float h = hue - (float) Math.floor(hue);
        float s = clamp01(sat), v = clamp01(val);
        float x = h * 6f;
        int i = (int) x;
        float f = x - i;
        float p = v * (1f - s), q = v * (1f - s * f), t = v * (1f - s * (1f - f));
        float r, g, b;
        switch (i) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return (Math.round(r * 255f) << 16) | (Math.round(g * 255f) << 8) | Math.round(b * 255f);
    }

    /** The colour {@code amount} (0..1) of the way from {@code a} to white. */
    public static int towardWhite(int rgb, float amount) {
        float k = clamp01(amount);
        int r = (rgb >> 16 & 255), g = (rgb >> 8 & 255), b = (rgb & 255);
        r += Math.round((255 - r) * k);
        g += Math.round((255 - g) * k);
        b += Math.round((255 - b) * k);
        return (r << 16) | (g << 8) | b;
    }

    private static float clamp01(float v) { return v < 0f ? 0f : Math.min(v, 1f); }

    private static float frac(double v) { return (float) (v - Math.floor(v)); }

    // ---------------------------------------------------------------- the tooltip text
    /** Milliseconds one full turn of the spectrum takes along the text. */
    public static final long TEXT_PERIOD_MS = 5200L;
    /** The hue step between neighbouring glyphs (a turn spreads over about 18 glyphs, so a row shows most of the spectrum at once). */
    public static final float TEXT_HUE_STEP = 0.055f;

    /**
     * The colour of glyph {@code index} of {@code count} at {@code millis}: the whole spectrum at full saturation, shifting with time, plus a white glint (a soft band) that runs from the left
     * to the right of the row every 1.6 seconds. Genesis's row is one pale hue; this is why EX-Singularity reads as the higher tier. Cheap: one hsv and one blend, no allocation.
     */
    public static int textRgb(int index, int count, long millis) {
        float hue = (millis % TEXT_PERIOD_MS) / (float) TEXT_PERIOD_MS + index * TEXT_HUE_STEP;
        int rgb = hsv(hue, 0.78f, 1f);
        float n = Math.max(1, count);
        float pos = (millis % 1600L) / 1600f * 1.5f - 0.25f;
        float d = Math.abs(pos - index / n);
        return d < 0.12f ? towardWhite(rgb, 0.85f * (1f - d / 0.12f)) : rgb;
    }

    /** The colour for a whole row drawn in ONE colour (the fallback when a row is too long to draw glyph by glyph, and the border's gradient ends). */
    public static int rowRgb(long millis, float offset) {
        return hsv((millis % TEXT_PERIOD_MS) / (float) TEXT_PERIOD_MS + offset, 0.72f, 1f);
    }

    /** The tooltip's border: start and end of the gradient are two hues a third of a turn apart that circle together. */
    public static int borderRgb(long millis, boolean end) {
        return hsv((millis % 7000L) / 7000f + (end ? 0.33f : 0f), 0.80f, 1f);
    }

    // ---------------------------------------------------------------- the sword's glow
    /** Ticks one full turn of the spectrum takes on the sword (ten seconds: slow, so the sword is never flashing). */
    public static final float GLOW_PERIOD_TICKS = 200f;

    /** The sword's main glow colour (0xRRGGBB) at a time in ticks: a slow walk through every hue. */
    public static int glowRgb(float ticks) { return hsv(ticks / GLOW_PERIOD_TICKS, 0.90f, 0.78f); }

    /** Where the veil starts on the sword, as a share of its length from the pommel end: the blade only (the hilt and the guard keep their own metal). */
    public static final float VEIL_FROM = 0.38f;
    /** Ticks the bright band of the veil takes to run up the blade once (three seconds). */
    public static final float VEIL_BAND_TICKS = 60f;

    /**
     * The colour of the iridescent VEIL at a point of the blade ({@code yFrac} 0 = pommel end, 1 = tip) at a time in ticks: the spectrum laid along the blade (0.8 of a turn from the
     * guard to the tip) and turning with the glow, dim all over (value 0.16) with a bright band (up to 0.66) that runs up the blade every three seconds. The sword's second pass draws the
     * blade's own skin additively with this colour per quad, so the silver shimmers like oil on water. Pure; no allocation.
     */
    public static int veilRgb(float yFrac, float ticks) {
        float hue = ticks / GLOW_PERIOD_TICKS + yFrac * 0.8f;
        float pos = frac(ticks / VEIL_BAND_TICKS) * 1.5f - 0.25f;
        float d = Math.abs(yFrac - pos);
        float band = d < 0.25f ? 1f - d / 0.25f : 0f;
        return hsv(hue, 0.85f, 0.16f + 0.50f * band * band);
    }

    /** The slow pulse (about 0.62..1.0) both passes breathe with; period about nine seconds, so it never reads as a flicker. */
    public static float pulse(float ticks) { return 0.81f + 0.19f * (float) Math.sin(ticks * 0.07f); }

    
    public static int haloAlpha(float ticks) { return 110 + Math.round(80f * (0.5f + 0.5f * (float) Math.sin(ticks * 0.07f + 2.2f))); }

    /** The colour of orbiting shard {@code i} (they sit at evenly spaced hues, all turning with the glow). */
    public static int shardRgb(int i, int count, float ticks) { return hsv(ticks / GLOW_PERIOD_TICKS + i / (float) Math.max(1, count), 0.70f, 1f); }

    /**
     * Where shard {@code i} of {@code count} is at {@code ticks}, in units of the blade length: {@code out[0]} and {@code out[2]} are the offsets across the blade (the circle's radius is
     * {@code radius} blade lengths), {@code out[1]} is the height along the blade 0..1 (0 = the pommel end, 1 = the tip), {@code out[3]} the shard's twinkle 0..1.
     * Pure; no allocation (the caller passes the array).
     */
    public static void shard(int i, int count, float ticks, float radius, float[] out) {
        float a = i * (6.2831855f / Math.max(1, count)) + ticks * (0.045f + 0.006f * (i % 3));
        float r = radius * (0.85f + 0.3f * ((i * 7) % 5) / 4f);
        out[0] = (float) Math.cos(a) * r;
        out[2] = (float) Math.sin(a) * r;
        out[1] = 0.12f + 0.76f * frac(i * 0.381966 + ticks * 0.0035 * (1 + (i & 1)));
        out[3] = 0.5f + 0.5f * (float) Math.sin(ticks * 0.21f + i * 1.7f);
    }

    // ---------------------------------------------------------------- the slot frame
    /** Milliseconds one full turn of the frame takes. */
    public static final long FRAME_PERIOD_MS = 6000L;

    /** The frame colour of ring segment {@code k} of {@code n} at {@code millis}: hues run round the ring, the ring turns once per period. {@code counter} turns the inner ring the other way. */
    public static int frameRgb(int k, int n, long millis, boolean counter) {
        float t = (millis % FRAME_PERIOD_MS) / (float) FRAME_PERIOD_MS;
        float seg = k / (float) Math.max(1, n);
        return hsv(counter ? seg - t : seg + t, 0.85f, 1f);
    }

    /** The alpha (0..255) of a frame segment: the outer ring is bright, the inner one softer; both breathe a little. */
    public static int frameAlpha(boolean inner, long millis) {
        float b = 0.5f + 0.5f * (float) Math.sin((millis % FRAME_PERIOD_MS) / (float) FRAME_PERIOD_MS * 6.2831855f * 2f);
        return inner ? 100 + Math.round(50f * b) : 200 + Math.round(55f * b);
    }

    /** How strong corner twinkle {@code c} (0..3) is at {@code millis}, 0..1 (the four corners take turns). */
    public static float twinkle(int c, long millis) {
        float p = frac((millis % 2400L) / 2400.0 - c * 0.25);
        return p < 0.25f ? (float) Math.sin(p / 0.25f * Math.PI) : 0f;
    }

    // ---------------------------------------------------------------- particle motes
    /** The tint of a mote at a game time: the glow's hue plus a small random offset the caller adds. */
    public static int moteRgb(float ticks, float jitter) { return hsv(ticks / GLOW_PERIOD_TICKS + jitter, 0.65f, 1f); }
}
