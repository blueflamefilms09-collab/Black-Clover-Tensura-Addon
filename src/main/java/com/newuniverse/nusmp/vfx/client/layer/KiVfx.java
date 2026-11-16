package com.newuniverse.nusmp.vfx.client.layer;

/**
 * Why: the pure half of the Dragon Ball Ki look ({@link KiLayer}): the texture list the generator ({@code tools/gen_ki_vfx_textures.py}) makes and the asset test pins, and the client switch
 * {@code saiyan.classicVfx} ({@code nusmp-saiyan-look-client.toml}, mirrored here by {@code SaiyanLookClientConfig}). No game type, so the asset test and the headless preview read it without Minecraft.
 */
public final class KiVfx {
    private KiVfx() {}

    public static final String PREFIX = "ki_";
    /** Every Ki texture, in the generator's order (the generator lists the same names with their sizes). */
    public static final String[] TEXTURE_NAMES = {"core", "halo", "beam", "beam_edge", "ring", "dust", "flash", "spark", "streak", "slash", "swirl", "hex", "dragon_head", "dragon_body", "afterimage"};

    /** The most quads (4 vertices each) one Ki effect writes at full detail; the engine's own cap is 100 (400 vertices). Pinned by KiVfxAssetTest against the layer's budget. */
    public static final int MAX_QUADS = 100;

    /** true: the Saiyan techniques show only the old particles of 3.66-3.89 (the server still sends them); the Ki layer draws nothing. A client setting, looks only. */
    private static volatile boolean classic = false;

    public static boolean classic() { return classic; }

    public static void setClassic(boolean on) { classic = on; }

    public static String fileName(String name) { return PREFIX + name; }

    /** "ki_core.png", ... : the files the folder must hold. */
    public static String[] allTextures() {
        String[] out = new String[TEXTURE_NAMES.length];
        for (int i = 0; i < out.length; i++) out[i] = "textures/particle/" + fileName(TEXTURE_NAMES[i]) + ".png";
        return out;
    }
}
