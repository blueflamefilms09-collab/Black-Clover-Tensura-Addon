package com.newuniverse.nusmp.client.companion.gs;

import com.newuniverse.nusmp.client.companion.TalkCanvas;
import com.newuniverse.nusmp.client.ui.gs.GsSkin;

/**
 * The drawing surface of the Great Spirit talk screen: the three calls of the classic {@link TalkCanvas} (fill, text, width) plus what the new look needs: a vertical gradient, a clipped area, sprite
 * blits (tinted, optionally additive, optionally flipped), an item icon by reference, and a block of text with the ink-to-matrix compile animation. The game's implementation draws on a
 * {@code GuiGraphics}; the preview tool and the tests draw on an image or a counter. Colours are 0xAARRGGBB. Pure interface.
 */
public interface GsCanvas extends TalkCanvas {
    /** A vertical gradient from {@code top} to {@code bottom} over [x1,x2) by [y1,y2). */
    void gradient(int x1, int y1, int x2, int y2, int top, int bottom);

    /** Restricts drawing to the rectangle until {@link #unclip()} (not nested). */
    void clip(int x1, int y1, int x2, int y2);

    void unclip();

    /**
     * Blits texels (u, v, uw, vh) of a sheet to the rectangle (x, y, w, h). A negative uw or vh flips (u, v is then the far edge). {@code tint} is ARGB (alpha scales the sprite, rgb multiplies it);
     * {@code additive} adds the light instead of blending. For {@link GsSprite#FACE} the skin is the screen's companion's skin and {@code skin} is ignored.
     */
    void sprite(GsSprite s, GsSkin skin, int x, int y, int w, int h, int u, int v, int uw, int vh, int tint, boolean additive);

    /** An item icon (16 by 16) at x, y. {@code ref} is the screen's own reference: {@link GsItemRef}. */
    void item(int ref, int x, int y);

    /**
     * The first {@code count} of {@code lines} as a block with the compile animation 'age' ticks into it (a value of {@link com.newuniverse.nusmp.client.ui.gs.GsCompile#DURATION} or more draws them
     * settled). {@code lines} is the same array every frame (the implementation may cache by it).
     */
    void compiled(String[] lines, int count, int x, int y, int lineH, float age, int ink, int settled, int data);
}
