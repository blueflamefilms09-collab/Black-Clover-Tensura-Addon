package com.newuniverse.nusmp.client.playermodel;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Why: hair is one grey strand texture tinted at draw time, and a tint that is only a multiply looks flat (a dark hair gets no shine, a pale hair no depth). A {@link Tint} is the three colours a
 * head of hair is drawn with: the base, a highlight for the tips of locks and the sheen band, and a shade for the roots and the underside. They are made from a single colour by lifting and
 * dropping its lightness a little and nudging the hue (warm lights, cool shades), so any 0xRRGGBB a creator or a character sheet names gets a convincing set. Pure arithmetic, no game class.
 */
public final class HairPalette {
    private HairPalette() {}

    /** The three colours of a head of hair, 0xRRGGBB. */
    public record Tint(int base, int highlight, int shade) {
        /** The colour of a box with this tone: 0 = base, 1 = highlight, -1 = shade, 2 = a lighter highlight (tips), -2 = a darker shade (roots). */
        public int tone(int tone) {
            return switch (tone) {
                case 1 -> highlight;
                case -1 -> shade;
                case 2 -> mix(highlight, 0xFFFFFF, 0.18f);
                case -2 -> mix(shade, 0x000000, 0.22f);
                default -> base;
            };
        }
    }

    /** The named colours (lower case): the colour words the character sheets use. */
    public static final Map<String, Integer> NAMED = new LinkedHashMap<>();

    static {
        NAMED.put("black", 0x1C1A22);
        NAMED.put("dark_brown", 0x3A281C);
        NAMED.put("brown", 0x5A3C28);
        NAMED.put("chestnut", 0x7A4A2C);
        NAMED.put("auburn", 0x8A3C20);
        NAMED.put("red", 0xB02A2A);
        NAMED.put("orange", 0xE0782C);
        NAMED.put("ginger", 0xC8601C);
        NAMED.put("gold", 0xEEC048);
        NAMED.put("blonde", 0xE0C070);
        NAMED.put("platinum", 0xE8E4D0);
        NAMED.put("silver", 0xC8CED8);
        NAMED.put("white", 0xF2F2F6);
        NAMED.put("grey", 0x7C7C86);
        NAMED.put("navy", 0x24305E);
        NAMED.put("blue", 0x3C6ED0);
        NAMED.put("teal", 0x2C9C94);
        NAMED.put("green", 0x3E9A52);
        NAMED.put("purple", 0x7A4AAE);
        NAMED.put("pink", 0xEE8AB0);
        NAMED.put("lavender", 0xB49AE0);
    }

    /**
     * The tint of a colour. A very light colour keeps its base and gains a cool shade; a very dark one gains a lifted highlight (so black hair still shines).
     */
    public static Tint of(int rgb) {
        int c = rgb & 0xFFFFFF;
        float[] hsl = toHsl(c);
        float l = hsl[2];
        float lift = 0.10f + 0.12f * (1f - l);                 // dark hair needs the bigger lift to read at all
        float drop = 0.10f + 0.08f * l;
        int hi = fromHsl(hsl[0] - 0.012f, Math.min(1f, hsl[1] * 1.02f), Math.min(0.97f, l + lift));
        int sh = fromHsl(hsl[0] + 0.018f, Math.min(1f, hsl[1] * 0.96f), Math.max(0.03f, l - drop));
        return new Tint(c, hi, sh);
    }

    /** A colour word ("auburn"), "#rrggbb", "0xrrggbb" or a plain number; {@code fallback} when it is none of them. */
    public static int parse(String text, int fallback) {
        if (text == null) return fallback;
        String s = text.trim().toLowerCase(Locale.ROOT);
        if (s.isEmpty()) return fallback;
        Integer named = NAMED.get(s);
        if (named != null) return named;
        try {
            if (s.startsWith("#")) return Integer.parseInt(s.substring(1), 16) & 0xFFFFFF;
            if (s.startsWith("0x")) return Integer.parseInt(s.substring(2), 16) & 0xFFFFFF;
            return Integer.parseInt(s) & 0xFFFFFF;
        } catch (NumberFormatException e) {
            return fallback;
        }
    }

    /** {@code a} to {@code b} by {@code k} (0..1), per channel. */
    public static int mix(int a, int b, float k) {
        float t = Math.max(0f, Math.min(1f, k));
        int r = Math.round(((a >> 16) & 255) * (1f - t) + ((b >> 16) & 255) * t);
        int g = Math.round(((a >> 8) & 255) * (1f - t) + ((b >> 8) & 255) * t);
        int bl = Math.round((a & 255) * (1f - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    // ---------------------------------------------------------------- HSL
    static float[] toHsl(int rgb) {
        float r = ((rgb >> 16) & 255) / 255f, g = ((rgb >> 8) & 255) / 255f, b = (rgb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float l = (max + min) / 2f, h = 0f, s = 0f;
        float d = max - min;
        if (d > 1e-6f) {
            s = l > 0.5f ? d / (2f - max - min) : d / (max + min);
            if (max == r) h = (g - b) / d + (g < b ? 6f : 0f);
            else if (max == g) h = (b - r) / d + 2f;
            else h = (r - g) / d + 4f;
            h /= 6f;
        }
        return new float[]{h, s, l};
    }

    static int fromHsl(float h, float s, float l) {
        h = h - (float) Math.floor(h);
        float r, g, b;
        if (s <= 1e-6f) {
            r = g = b = l;
        } else {
            float q = l < 0.5f ? l * (1f + s) : l + s - l * s, p = 2f * l - q;
            r = hue(p, q, h + 1f / 3f);
            g = hue(p, q, h);
            b = hue(p, q, h - 1f / 3f);
        }
        return (clamp8(r) << 16) | (clamp8(g) << 8) | clamp8(b);
    }

    private static float hue(float p, float q, float t) {
        if (t < 0f) t += 1f;
        if (t > 1f) t -= 1f;
        if (t < 1f / 6f) return p + (q - p) * 6f * t;
        if (t < 0.5f) return q;
        if (t < 2f / 3f) return p + (q - p) * (2f / 3f - t) * 6f;
        return p;
    }

    private static int clamp8(float v) { return Math.max(0, Math.min(255, Math.round(v * 255f))); }
}
