package com.newuniverse.nusmp.client.relics.djinn.gui;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/** The game's {@link Canvas}: the panel's fills and text go straight to a GuiGraphics. One instance is reused and re-pointed every frame. */
final class GfxCanvas implements Canvas {
    private GuiGraphics g;
    private Font font;

    void set(GuiGraphics graphics, Font f) {
        g = graphics;
        font = f;
    }

    @Override
    public void fill(int x1, int y1, int x2, int y2, int argb) { g.fill(x1, y1, x2, y2, argb); }

    @Override
    public void text(String s, int x, int y, int argb) { g.drawString(font, s, x, y, argb, true); }

    @Override
    public int textWidth(String s) { return font.width(s); }
}
