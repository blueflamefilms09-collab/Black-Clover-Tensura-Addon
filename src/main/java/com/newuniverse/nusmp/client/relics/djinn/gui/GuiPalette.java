package com.newuniverse.nusmp.client.relics.djinn.gui;

/**
 * Every colour of one Djinn's panel, derived from three numbers of its row (body colour, glow colour, the tint of its magic) and whether it is a
 * Dark Djinn. Nothing here knows a Djinn by name, so a Djinn added later gets a full palette from its row alone. All values are 0xRRGGBB.
 *
 * <p>Readability is part of the contract (tested): {@code text} is always bright against {@code deep}, {@code primary} and {@code glow} are lifted
 * so a dark body colour (a deep blue, a crimson) still shows on the dark panel.
 */
public record GuiPalette(int primary, int glow, int accent, int deep, int mid, int rim, int text, int textDim, int barA, int barB, int track) {

    public static GuiPalette of(int color, int glow, int tint, boolean dark) {
        color &= 0xFFFFFF;
        glow &= 0xFFFFFF;
        tint &= 0xFFFFFF;
        int deep = dark ? GuiColor.mix(0x06020A, color, 0.07f) : GuiColor.mix(0x04060C, color, 0.12f);
        int mid = dark ? GuiColor.mix(deep, 0x3A1048, 0.55f) : GuiColor.mix(deep, color, 0.34f);
        int primary = GuiColor.lift(dark ? GuiColor.mix(color, glow, 0.3f) : color, 0.30f);
        int glw = GuiColor.lift(glow, 0.58f);
        int accent = GuiColor.lift(GuiColor.mix(tint, primary, 0.35f), 0.48f);
        int rim = GuiColor.lift(dark ? GuiColor.mix(GuiColor.GOLD, primary, 0.25f) : GuiColor.mix(primary, glw, 0.5f), 0.52f);
        int text = GuiColor.lift(GuiColor.mix(glw, GuiColor.WHITE, 0.55f), 0.80f);
        int textDim = GuiColor.lift(GuiColor.mix(text, deep, 0.45f), 0.58f);
        int barA = GuiColor.lift(GuiColor.mix(primary, 0x3A7BFF, 0.25f), 0.34f);
        int barB = GuiColor.lift(GuiColor.mix(primary, glw, 0.65f), 0.55f);
        int track = GuiColor.mix(deep, 0x000000, 0.35f);
        return new GuiPalette(primary, glw, accent, deep, mid, rim, text, textDim, barA, barB, track);
    }
}
