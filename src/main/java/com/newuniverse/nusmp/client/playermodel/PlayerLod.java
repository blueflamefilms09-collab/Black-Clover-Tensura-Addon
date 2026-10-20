package com.newuniverse.nusmp.client.playermodel;

/**
 * Why: a dungeon floor can show a squad of eight and a hall of sixty, and a skin's second layer, a hair style's hundred boxes and an extra's additive glow pass are all invisible at the
 * distance of a far wall. The kit draws less the farther a character is, in three steps, decided by one pure function of the squared distance (so it is a comparison, not a square root, and
 * unit tested): level 0 draws everything; level 1 (beyond the detail distance) drops the second skin layer (hat, jacket, sleeves, pants) and the hair; level 2 (beyond the far distance) also drops
 * every additive glow pass (the skin's, the extras'). The body, the armour, the held item and the extras' solid pass are always drawn, so a far character keeps its silhouette.
 */
public final class PlayerLod {
    private PlayerLod() {}

    public static final int FULL = 0, NEAR = 1, FAR = 2;
    /** Defaults, in blocks; the client options ({@link PlayerModelOptions}) change them. */
    public static final int DEFAULT_DETAIL = 24, DEFAULT_FAR = 64;

    /** The level of a character at this squared distance from the camera. A far distance below the detail distance is raised to it. */
    public static int level(double distSqr, double detailSqr, double farSqr) {
        if (distSqr <= detailSqr) return FULL;
        return distSqr <= Math.max(detailSqr, farSqr) ? NEAR : FAR;
    }

    /** Is the second skin layer drawn at this level? */
    public static boolean outerLayer(int level) { return level == FULL; }

    /** Is the hair drawn at this level? */
    public static boolean hair(int level) { return level == FULL; }

    /** Are the additive glow passes drawn at this level? */
    public static boolean glow(int level) { return level < FAR; }

    /** The squared form of a distance in blocks. */
    public static double sqr(double blocks) { return blocks * blocks; }
}
