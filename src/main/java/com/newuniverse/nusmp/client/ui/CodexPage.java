package com.newuniverse.nusmp.client.ui;

import net.minecraft.client.gui.GuiGraphics;

/**
 * 0.100: one spread of the Codex. A page draws the whole spread (both halves) inside the rectangle it is given; the screen clips and
 * turns it. Pages that are taller than the spread report {@link #contentHeight} and honour the scroll offset. Register new pages in
 * {@link CodexPages}; nothing else needs to change.
 */
public interface CodexPage {
    /** Tab label. */
    String title();

    /** A short glyph for the tab (one or two characters). */
    String glyph();

    /** The y extent the content wants (pixels) for a spread of this width; the screen scrolls when it exceeds the spread's height. */
    int contentHeight(CodexContext ctx, int width);

    /** Draw the spread. (x, y, w, h) is the inner area; scroll is the pixel offset to apply to scrollable content. */
    void render(CodexContext ctx, GuiGraphics g, int x, int y, int w, int h, int scroll, int mouseX, int mouseY, float time);

    /** 1.37.0: the Codex was just opened; a page that caches (registry catalogues, learned spells) forgets what it built. Default: nothing to do. */
    default void opened() {}

    /** A click inside the spread (returns true if it was used). */
    default boolean click(CodexContext ctx, double mouseX, double mouseY, int button, int x, int y, int w, int h) { return false; }
}
