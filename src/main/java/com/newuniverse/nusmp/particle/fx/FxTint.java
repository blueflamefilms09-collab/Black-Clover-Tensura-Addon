package com.newuniverse.nusmp.particle.fx;

import java.nio.ByteBuffer;
import java.util.Locale;

/**
 * Why: the colour and size multiplier a tinted custom particle carries from the server to the client, as plain data, so the codec's behaviour (what is kept, what is clamped, what a
 * colour string means) is unit-tested without the game. {@code rgb} is 0xRRGGBB, or {@code -1} for "no tint: the texture's own colours"; {@code scale} multiplies the size of the particle,
 * kept between 0.1 and 8 and quantised to 1/64 so that a value that went through the wire compares equal to the one that was sent. The wire form is an int and a float (8 bytes).
 */
public record FxTint(int rgb, float scale) {
    public static final FxTint NONE = new FxTint(-1, 1.0f);
    public static final float MIN_SCALE = 0.1f;
    public static final float MAX_SCALE = 8.0f;

    public FxTint {
        rgb = rgb < 0 ? -1 : (rgb & 0xFFFFFF);
        scale = quantise(scale);
    }

    /** Clamps to 0.1..8 and rounds to 1/64; NaN becomes 1. */
    public static float quantise(float s) {
        if (Float.isNaN(s)) return 1.0f;
        float c = Math.max(MIN_SCALE, Math.min(MAX_SCALE, s));
        return Math.round(c * 64f) / 64f;
    }

    public boolean tinted() {
        return rgb >= 0;
    }

    public float red() {
        return ((rgb >> 16) & 255) / 255f;
    }

    public float green() {
        return ((rgb >> 8) & 255) / 255f;
    }

    public float blue() {
        return (rgb & 255) / 255f;
    }

    /** The wire form: the colour int, then the scale float. */
    public void write(ByteBuffer out) {
        out.putInt(rgb);
        out.putFloat(scale);
    }

    public static FxTint read(ByteBuffer in) {
        int c = in.getInt();
        float s = in.getFloat();
        return new FxTint(c, s);
    }

    /**
     * Parses {@code #RRGGBB}, {@code 0xRRGGBB}, {@code RRGGBB} (six hex digits) or {@code none}; anything else is {@code -1} (no tint). Used by the command.
     */
    public static int parseColor(String s) {
        if (s == null) return -1;
        String t = s.trim().toLowerCase(Locale.ROOT);
        if (t.startsWith("#")) t = t.substring(1);
        else if (t.startsWith("0x")) t = t.substring(2);
        if (t.length() != 6) return -1;
        try {
            return Integer.parseInt(t, 16) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** {@code #RRGGBB}, or {@code none}. */
    public static String format(int rgb) {
        return rgb < 0 ? "none" : String.format(Locale.ROOT, "#%06X", rgb & 0xFFFFFF);
    }

    /** Multiplies the brightness of an 0xRRGGBB colour (used for the dark flavour of a theme colour). */
    public static int scaleRgb(int rgb, float f) {
        if (rgb < 0) return rgb;
        int r = Math.min(255, Math.max(0, Math.round(((rgb >> 16) & 255) * f)));
        int g = Math.min(255, Math.max(0, Math.round(((rgb >> 8) & 255) * f)));
        int b = Math.min(255, Math.max(0, Math.round((rgb & 255) * f)));
        return (r << 16) | (g << 8) | b;
    }
}
