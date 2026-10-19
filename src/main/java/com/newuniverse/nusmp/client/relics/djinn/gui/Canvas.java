package com.newuniverse.nusmp.client.relics.djinn.gui;

/**
 * The three drawing calls the active-Djinn panel needs, so the whole panel is drawn by pure code: the game feeds them to a {@code GuiGraphics}
 * ({@code GfxCanvas}), the preview tool and the tests feed them to an image or a counter. Colours are 0xAARRGGBB, rectangles are [x1,x2) by [y1,y2).
 */
public interface Canvas {
    void fill(int x1, int y1, int x2, int y2, int argb);

    /** Shadowed text, 8 pixels high, its top-left corner at x,y. */
    void text(String s, int x, int y, int argb);

    int textWidth(String s);
}
