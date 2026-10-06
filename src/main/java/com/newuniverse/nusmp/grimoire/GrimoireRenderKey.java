package com.newuniverse.nusmp.grimoire;

/**
 * Everything that changes a grimoire's GEOMETRY or which textures it samples. Colours are deliberately absent: they are applied
 * per quad by tintindex, so two grimoires that differ only in colour share one cached quad list.
 * <p>
 * Distinct keys = 16 crests x 5 covers x 3 trim styles x 4 thicknesses x 4 clasps x 3 aura tiers = 11,520 (most never appear
 * together, and the client keeps only the most recently used few hundred).
 */
public record GrimoireRenderKey(InsigniaType insignia, CoverMaterial cover, TrimMetal.Style trimStyle,
                                Thickness thickness, ClaspType clasp, int auraTier, boolean held) {

    public static GrimoireRenderKey of(GrimoireAppearance a) {
        return new GrimoireRenderKey(a.insignia(), a.cover(), a.trimMetal().style, a.thickness(), a.clasp(), auraTier(a.insignia()), false);
    }

    /** The same look as it is drawn in a hand: a grimoire only glows (slightly) while it is out and in use. */
    public GrimoireRenderKey withHeld(boolean held) {
        return held == this.held ? this : new GrimoireRenderKey(insignia, cover, trimStyle, thickness, clasp, auraTier, held);
    }

    /**
     * Lore: a grimoire glows slightly while floating in front of its mage, and not when it is put away. So the aura only exists
     * on the held variant of a key. 0 = a faint rim (every grimoire); 3 = rim + magic circle + drifting sparks, which only the
     * special outliers (Boundless, Forbidden Runes) are that showy about.
     */
    public static int auraTier(InsigniaType insignia) {
        return insignia.isSpecial() ? 3 : 0;
    }
}
