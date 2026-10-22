package com.newuniverse.nusmp.prop;

/**
 * Why: the variants of the Bone Magic props (PropKind.BONE_JA), shared by the server code (prop.BoneJaProps, book.BoneJaArts) and the painter (client.prop.BoneJaPropPainter) in a class with no game
 * classes (so the preview tool reads it too). The variant is the low byte of the param; SHORT is a flag above bit 15, data (a speed, a length) is in bits 8-15.
 */
public final class BoneJaVariant {
    private BoneJaVariant() {}

    public static final int FANG = 0, BULLET = 1, DAINSLEIF = 2, SLASH = 3;
    /** SLASH: only the short crescent of a Dainsleif swing (no hitbox). */
    public static final int SHORT = 0x10000;
}
