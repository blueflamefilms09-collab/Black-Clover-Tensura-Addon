package com.newuniverse.nusmp.client.companion;

/**
 * Why: the three drawing calls the talk screen needs, so the whole screen is drawn by pure code ({@link TalkPainter}): the game feeds them to a {@code GuiGraphics} (inside {@link TalkScreen}), the preview tool and the
 * tests feed them to an image or a counter. Colours are 0xAARRGGBB, rectangles are [x1,x2) by [y1,y2).
 */
public interface TalkCanvas {
    void fill(int x1, int y1, int x2, int y2, int argb);

    /** Shadowed text, 8 pixels high, its top-left corner at x,y. */
    void text(String s, int x, int y, int argb);

    int textWidth(String s);
}
