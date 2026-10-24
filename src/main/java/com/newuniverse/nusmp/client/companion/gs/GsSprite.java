package com.newuniverse.nusmp.client.companion.gs;

/**
 * The picture sheets the painter can ask a {@link GsCanvas} to blit: the Great Spirit sheets of the skin being drawn (the filigree corner, the clover frames and their glow, the drips, the ripple ring) and
 * FACE, the companion's own player-model skin (64 by 64 texels, or 64 by 32 for an old skin). All GS sheets are 96 by 96 texels except the ripple (64 by 64).
 */
public enum GsSprite {
    CORNER, CLOVER3, CLOVER4, CLOVER5, GLOW4, GLOW5, DRIPS, RIPPLE, FACE;

    /** The width (and height) in texels of the GS sheets. */
    public static final int SHEET = 96, RIPPLE_SHEET = 64;
}
