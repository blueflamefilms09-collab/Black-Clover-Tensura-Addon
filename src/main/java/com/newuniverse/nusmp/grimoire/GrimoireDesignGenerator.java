package com.newuniverse.nusmp.grimoire;

import java.util.Random;

/**
 * Procedural grimoire design generator: randomizes cover patterns, color overlays, page motifs,
 * and rune styling to give each grimoire unique visual variation instead of hard-fixed designs.
 * 
 * v0.92.0: Added for future integration into BookLook design pipeline. Currently provides:
 * - Cover pattern variants (geometric overlay styles)
 * - Page motif variations (decorative borders and corners)
 * - Rune styling options (font/glyph styles for signature runes)
 * - Accent color tints (procedurally picked hues)
 * - Metallic finish variations (spine and trim aesthetics)
 * - Edge decoration complexity levels
 * 
 * Usage: Call GrimoireDesignGenerator.forMagic(magicName, coverName) to get a seeded,
 * deterministic generator for a given magic type + cover combination. Each combination
 * produces a stable, repeatable set of design variations.
 *
 * Used by {@link BookLook} to generate per-magic procedurally varied cover ornament, page motif,
 * and rune frames.
 */
public final class GrimoireDesignGenerator {
    private final Random rng;
    private final long seed;

    public GrimoireDesignGenerator(long seed) {
        this.seed = seed;
        this.rng = new Random(seed);
    }

    /**
     * Cover pattern variation: selects from a palette of geometric overlays to apply over the
     * base art cover (filigree density, frame complexity, ornament symmetry).
     * Returns an index 0-15 describing the cover's procedural pattern style.
     */
    public int coverPatternVariant() {
        return rng.nextInt(16);
    }

    /**
     * Page motif variation: selects the decorative motif applied to page edges and corners
     * (corner flourishes, page-rule styles, rune borders). Returns 0-31 for motif selection.
     */
    public int pageMotifVariant() {
        return rng.nextInt(32);
    }

    /**
     * Rune styling variation: pick the font/glyph style for the signature runes on the cover
     * (angular, flowing, arcane, rigid). Returns 0-7.
     */
    public int runeStyleVariant() {
        return rng.nextInt(8);
    }

    /**
     * Accent color overlay tint: a procedurally picked accent hue to overlay on the cover design
     * while keeping the base art intact. Returns a packed ARGB where full alpha = apply tint,
     * lower alpha = blend lightly (0xFF = opaque accent, 0x00 = no tint, use base only).
     * Excludes the alpha channel itself; call withAlpha() to set it.
     */
    public int accentTint() {
        // Procedurally pick a hue from a palette biased toward magic-book colors
        int hue = rng.nextInt(360);
        float saturation = 0.4f + rng.nextFloat() * 0.3f;  // moderate saturation for overlay
        float lightness = 0.5f + rng.nextFloat() * 0.2f;
        return hslToRgb(hue, saturation, lightness);
    }

    /**
     * Spine and trim metallic finish: returns a variation (0-3) for spine rendering.
     * 0 = bronze, 1 = silver, 2 = gold, 3 = copper.
     */
    public int metalFinish() {
        return rng.nextInt(4);
    }

    /**
     * Edge decoration complexity: returns 0-3 describing how ornate the frame bars are.
     */
    public int edgeComplexity() {
        return rng.nextInt(4);
    }

    // ---------------------------------------------------------------- helpers
    private static int hslToRgb(int hue, float sat, float light) {
        float h = hue / 360f;
        float c = (1 - Math.abs(2 * light - 1)) * sat;
        float x = c * (1 - Math.abs(h * 6 % 2 - 1));
        float m = light - c / 2;
        float r, g, b;

        if (h < 1f / 6) { r = c; g = x; b = 0; }
        else if (h < 2f / 6) { r = x; g = c; b = 0; }
        else if (h < 3f / 6) { r = 0; g = c; b = x; }
        else if (h < 4f / 6) { r = 0; g = x; b = c; }
        else if (h < 5f / 6) { r = x; g = 0; b = c; }
        else { r = c; g = 0; b = x; }

        int R = Math.round((r + m) * 255);
        int G = Math.round((g + m) * 255);
        int B = Math.round((b + m) * 255);
        return (R << 16) | (G << 8) | B;
    }

    /**
     * Seed-based determinism: each magic type + cover type produces a stable, repeatable design.
     * Call this with the book's magic type and cover enum to generate a stable generator.
     */
    public static GrimoireDesignGenerator forMagic(String magicName, String coverName) {
        long seed = (long) magicName.hashCode() * 31 + (long) coverName.hashCode();
        return new GrimoireDesignGenerator(seed);
    }
}
