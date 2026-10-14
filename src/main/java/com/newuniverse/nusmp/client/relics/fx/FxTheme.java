package com.newuniverse.nusmp.client.relics.fx;

import java.util.Locale;

/**
 * The ambient particle theme of a worn relic: what drifts off it. A signature relic names its own theme in the catalog (assets/nusmp/relic_worn/catalog.txt, "fx" lines);
 * every other relic gets the one that fits the hue of its glow colour ({@link #fromGlow}). Pure.
 */
public enum FxTheme {
    EMBER, SNOW, SPARK, LEAF, WISP, STAR, BEE, PETAL, NONE;

    public static FxTheme byName(String name) {
        if (name == null) return null;
        String s = name.trim().toUpperCase(Locale.ROOT);
        for (FxTheme t : values()) if (t.name().equals(s)) return t;
        return null;
    }

    /** Red and orange burn, yellow and white glitter, green drops leaves, cyan snows, blue crackles, violet leaves wisps, pink sheds petals, dark colours smoke. */
    public static FxTheme fromGlow(int rgb) {
        float r = (rgb >> 16 & 255) / 255f, g = (rgb >> 8 & 255) / 255f, b = (rgb & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        float sat = max <= 0f ? 0f : d / max;
        if (max < 0.25f) return WISP;
        if (sat < 0.2f) return STAR;
        float h;
        if (d <= 0f) h = 0f;
        else if (max == r) h = 60f * (((g - b) / d) % 6f);
        else if (max == g) h = 60f * ((b - r) / d + 2f);
        else h = 60f * ((r - g) / d + 4f);
        if (h < 0f) h += 360f;
        if (h < 45f || h >= 345f) return EMBER;
        if (h < 75f) return STAR;
        if (h < 170f) return LEAF;
        if (h < 200f) return SNOW;
        if (h < 255f) return SPARK;
        if (h < 300f) return WISP;
        return PETAL;
    }
}
