package com.newuniverse.nusmp.vfx.client.layer;

/**
 * Why: the pure half of the Black Sword Greed / devil-hit look ({@link GreedLayer}, {@link GreedDevilHits}): the texture list the generator ({@code tools/gen_greed_vfx_textures.py}) makes and the asset test
 * pins, and the two client switches {@code greed.classicVfx} / {@code devil.classicVfx} ({@code nusmp-greed-devil-vfx-client.toml}, mirrored here by {@code GreedVfxConfig}). No game type, so the asset test
 * and the headless preview read it without Minecraft.
 */
public final class GreedVfx {
    private GreedVfx() {}

    public static final String PREFIX = "greed_";
    /** Every texture, in the generator's order (the generator lists the same names with their sizes). */
    public static final String[] TEXTURE_NAMES = {"core", "glow", "void", "slash", "bolt", "streak", "ring", "scales", "scale", "teeth", "glint", "flame", "feather", "pentagram", "crack",
            "runes", "brand", "blood", "flash"};

    /** The eight devil worlds in {@code devil.DevilWorld} order: DEVIL_HIT's power is the ordinal + 1 (a payload power of 0 would read as 1). Pinned against the enum by GreedVfxAssetTest. */
    public static final String[] DEVIL_WORLDS = {"BLACK_CLOVER", "CHAINSAW_MAN", "BLACK_BUTLER", "BLUE_EXORCIST", "TENSURA", "DORORO", "BERSERK", "FAIRY_TAIL"};

    /** true: the Greed shapes draw nothing (the wielder's techniques show only the old particles). A client setting, looks only. */
    private static volatile boolean classicGreed = false;
    /** true: DEVIL_HIT draws nothing (the old mote spray only). A client setting, looks only. */
    private static volatile boolean classicDevil = false;

    public static boolean classicGreed() { return classicGreed; }

    public static boolean classicDevil() { return classicDevil; }

    public static void setClassic(boolean greed, boolean devil) {
        classicGreed = greed;
        classicDevil = devil;
    }

    /** The world index (0..7) a DEVIL_HIT power stands for; anything out of range reads as Black Clover. */
    public static int devilWorld(float power) {
        int w = Math.round(power) - 1;
        return w >= 0 && w < DEVIL_WORLDS.length ? w : 0;
    }

    public static String fileName(String name) { return PREFIX + name; }

    /** "textures/particle/greed_core.png", ... : the files the folder must hold. */
    public static String[] allTextures() {
        String[] out = new String[TEXTURE_NAMES.length];
        for (int i = 0; i < out.length; i++) out[i] = "textures/particle/" + fileName(TEXTURE_NAMES[i]) + ".png";
        return out;
    }
}
