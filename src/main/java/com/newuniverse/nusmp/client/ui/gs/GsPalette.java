package com.newuniverse.nusmp.client.ui.gs;

/**
 * The colours of the two Great Spirit skins as plain numbers (no Minecraft type), so the drawing code of the game ({@link GsDraw}) and the pure painters that the preview tool and the tests share draw with
 * ONE palette. RGB per skin, indexed by {@link GsSkin#ordinal()}: B = Parchment and Ink, C = Slime Crest.
 */
public final class GsPalette {
    private GsPalette() {}

    public static final int[] NEON = {0xDEB252, 0xA65AFF};
    public static final int[] NEON2 = {0xFFDC8C, 0xFF50D6};
    public static final int[] TEXT = {0xF6E8C8, 0xECE2F8};
    public static final int[] DIM = {0xC4A878, 0x9E82B8};
    /** Ink on a skin-B page, and the colour a compiled line settles into; for skin C (dark glass pages) light violet tones. */
    public static final int[] INK = {0x3A2616, 0xCCB0EC};
    public static final int[] SETTLED = {0x1F4E7A, 0xEBDDFF};
    public static final int[] DATA = {0x28AAFF, 0xFF6EE6};
    public static final int[] TITLE = {0x6A3A10, 0xFF8EEA};
    public static final int[] PANEL_TOP = {0x34210F, 0x16092A}, PANEL_BOTTOM = {0x22150A, 0x09040F};
    public static final int[] OK = {0x6FE08A, 0x9BFF72}, WARN = {0xFFB04A, 0xFFA050}, BAD = {0xF0605A, 0xFF5078};
}
