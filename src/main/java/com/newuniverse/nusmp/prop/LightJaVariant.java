package com.newuniverse.nusmp.prop;

/**
 * Why: the variants of the Light Magic props (PropKind.LIGHT_JA) are named by the server code (prop.LightJaProps, book.LightJaArts) and by the painter (client.prop.LightJaPropPainter); the names
 * live here, in a class with no game classes, so both sides and the preview tool read one table. The variant is the low byte of the param; flags are above bit 15, data (a speed, a length,
 * the lash) in bits 8-15.
 */
public final class LightJaVariant {
    private LightJaVariant() {}

    public static final int SWORD = 0, CROSS = 1, WHIP = 2, BEAM = 3, COLUMN = 4, ARROW = 5, ORB = 6, HEAL = 7, MONARCH = 8, GLORIA = 9;
    /** ORB: flying; ORB: the starburst where it ended; MONARCH: striking. */
    public static final int FLIGHT = 0x10000, BURST = 0x20000, STRIKE = 0x10000;
}
