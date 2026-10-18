package com.newuniverse.nusmp.client.skilltree.layout;

/**
 * Why: the owner's checkbox rule is exact: a checked box shows the tooltip "ON" and the passive acts; an empty box shows "OFF" and the passive is switched off at once. The state-to-text mapping and the
 * click rule are tiny but they are the part a player reads, so they are pure and tested instead of living inside a draw method: the tooltip key of a state, the state a click produces, and the
 * choice of which part of an optional texture to draw. The texture rule exists because Tensura's own checkbox texture cannot be inspected here: it is used only when it is a vertical strip of square
 * frames (off on top, on below, the layout the vanilla widget sprites also use), and for any other shape the checkbox falls back to the vanilla sprites, which are always correct, instead of drawing a
 * wrong part of an unknown image.
 */
public final class MenuCheckbox {
    private MenuCheckbox() {}

    /** The lang key of the tooltip for a state: checked is ON, empty is OFF. */
    public static String tooltipKey(boolean checked) { return checked ? "gui.nusmp.tmenu.on" : "gui.nusmp.tmenu.off"; }

    /** The state a click produces. */
    public static boolean next(boolean checked) { return !checked; }

    /**
     * The v offset of the frame to draw from a texture {@code texW} by {@code texH}, or -1 when the texture is not a usable strip (fewer than two square frames of the texture's width).
     */
    public static int stripV(int texW, int texH, boolean checked) {
        if (texW <= 0 || texH < 2 * texW) return -1;
        return checked ? texW : 0;
    }
}
