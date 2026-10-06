package com.newuniverse.nusmp.grimoire;

/** Render layer 0: the base cover texture family. Colour comes from the primary tint. APPEND ONLY. */
public enum CoverMaterial {
    PLAIN("Plain", GrimoireSprite.COVER_PLAIN, 1.00f),
    LEATHER_STITCHED("Stitched Leather", GrimoireSprite.COVER_STITCHED, 1.00f),
    DRAGON_HIDE("Dragon Hide", GrimoireSprite.COVER_DRAGONHIDE, 0.86f),
    TATTERED("Tattered", GrimoireSprite.COVER_TATTERED, 0.92f),
    METALLIC_TRIM("Ornate Gilded", GrimoireSprite.COVER_METALLIC, 1.08f);

    public final String displayName;
    public final GrimoireSprite sprite;
    /** Brightness multiplier the generator applies to the primary colour so each material keeps its own feel. */
    public final float brightness;

    CoverMaterial(String displayName, GrimoireSprite sprite, float brightness) {
        this.displayName = displayName; this.sprite = sprite; this.brightness = brightness;
    }

    private static final CoverMaterial[] VALUES = values();
    public static CoverMaterial byOrdinal(int i) { return VALUES[Math.floorMod(i, VALUES.length)]; }
}
