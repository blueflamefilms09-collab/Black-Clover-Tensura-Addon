package com.newuniverse.nusmp.particle.fx;

/**
 * Why: a relic's effect code names its colour (0xRRGGBB) and nothing else, but the owner wants every relic to have its own particles: a red relic should throw embers, a blue one bubbles, a
 * green one leaves, a gold one glints. This classifies a colour into one of the relic element themes ({@code relic.element.<name>} in the theme table) by its brightness, saturation and
 * hue, so the same call site gets different particles for different relics without knowing anything about them. Pure (no game classes), unit-tested on the colour wheel.
 */
public final class FxHue {
    private FxHue() {}

    /** The element name of a colour: one of fire, blood, light, nature, ice, water, arcane, dream, dark, metal, holy. */
    public static String element(int rgb) {
        int r = (rgb >> 16) & 255, g = (rgb >> 8) & 255, b = rgb & 255;
        int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        float v = max / 255f;
        float s = max == 0 ? 0f : (max - min) / (float) max;
        if (v < 0.28f) return "dark";
        if (s < 0.18f) return v > 0.85f ? "holy" : (v > 0.55f ? "metal" : "dark");
        float h;
        float d = max - min;
        if (max == r) h = 60f * (((g - b) / d) % 6f);
        else if (max == g) h = 60f * (((b - r) / d) + 2f);
        else h = 60f * (((r - g) / d) + 4f);
        if (h < 0f) h += 360f;
        if (h < 12f || h >= 345f) return v < 0.72f && s > 0.6f ? "blood" : "fire";
        if (h < 45f) return "fire";
        if (h < 70f) return "light";
        if (h < 165f) return "nature";
        if (h < 200f) return "ice";
        if (h < 255f) return "water";
        if (h < 285f) return "arcane";
        return "dream";
    }

    /** Dark colours (perceived luminance under 0.35) take the dark flavour, everything else the light one; the old Rukh rule. */
    public static FxTone tone(int rgb) {
        double lum = (((rgb >> 16) & 255) * 0.2126 + ((rgb >> 8) & 255) * 0.7152 + (rgb & 255) * 0.0722) / 255.0;
        return FxTone.of(lum < 0.35);
    }

    /** The id of the theme of a colour: {@code relic.element.<element>}. */
    public static String themeId(int rgb) {
        return "relic.element." + element(rgb);
    }
}
