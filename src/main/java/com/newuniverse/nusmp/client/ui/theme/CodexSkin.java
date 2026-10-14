package com.newuniverse.nusmp.client.ui.theme;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The anime drawing of the Codex's book furniture (frame, page bodies, spine, tabs, scrollbar, list rows): the screen keeps its geometry and
 * its clicks, and in the anime look calls these instead of its parchment drawing. Static, allocation-free.
 */
public final class CodexSkin {
    private CodexSkin() {}

    /** The frame of the whole book: a soft shadow, a dark glass backdrop, the glowing edge and brackets, the gutter and the spine line. */
    public static void frame(GuiGraphics g, int bx, int by, int bw, int bh, float now) {
        g.fill(bx + 3, by + 4, bx + bw + 3, by + bh + 4, 0x55000000);
        g.fill(bx, by, bx + bw, by + bh, UiMath.withAlpha(0x04060D, 0.86f));
        g.renderOutline(bx - 2, by - 2, bw + 4, bh + 4, UiMath.withAlpha(UiTheme.CYAN, 0.05f));
        g.renderOutline(bx - 1, by - 1, bw + 2, bh + 2, UiMath.withAlpha(UiTheme.CYAN, 0.12f));
        g.renderOutline(bx, by, bw, bh, UiMath.withAlpha(UiTheme.CYAN, 0.7f));
        g.renderOutline(bx + 3, by + 3, bw - 6, bh - 6, UiMath.withAlpha(UiTheme.GOLD, 0.55f));
        UiPanel.brackets(g, bx, by, bw, bh, 10, UiMath.withAlpha(UiTheme.GOLD, 0.95f));
        int spine = bx + bw / 2;
        g.fillGradient(spine - 12, by + 6, spine, by + bh - 6, 0x00000000, UiMath.withAlpha(UiTheme.CYAN, 0.10f));
        g.fillGradient(spine, by + 6, spine + 12, by + bh - 6, UiMath.withAlpha(UiTheme.CYAN, 0.10f), 0x00000000);
        g.fillGradient(spine, by + 6, spine + 1, by + bh - 6, UiMath.withAlpha(UiTheme.CYAN, 0.9f), UiMath.withAlpha(UiTheme.CYAN, 0.15f));
    }

    /**
     * One page body inside (x0, y0)-(x1, y1): translucent glass with a sheen, and a very faint magic circle (centre cx, cy, radius r: the same
     * for a whole spread and for each half of it, so a turning page does not make it jump) turning behind the text. 'leaf' is the page-turn
     * shade 0..1 (the moving half is a little darker).
     */
    public static void body(GuiGraphics g, int x0, int y0, int x1, int y1, float cx, float cy, float r, float now, float leaf) {
        g.fillGradient(x0, y0, x1, y1, UiMath.withAlpha(0x131E38, 0.78f), UiMath.withAlpha(0x090E1C, 0.82f));
        g.fillGradient(x0, y0, x1, y0 + Math.min(34, (y1 - y0) / 3), UiMath.withAlpha(0xFFFFFF, 0.05f), 0x00FFFFFF);
        MagicCircle.draw(g, cx, cy, r, now, UiTheme.CYAN, 0.13f, MagicCircle.CALM, 1f);
        if (leaf > 0f) g.fill(x0, y0, x1, y1, UiMath.withAlpha(0x000000, 0.25f * leaf));
    }

    /** The book's title line, page number and the page-corner arrows. */
    public static void furniture(GuiGraphics g, Font font, int bx, int by, int bw, int bh, String title, int index, int count) {
        int spine = bx + bw / 2;
        g.drawCenteredString(font, title, spine, by + 10, UiTheme.GOLD);
        g.fill(spine - 26, by + 20, spine + 26, by + 21, UiMath.withAlpha(UiTheme.GOLD, 0.35f));
        String num = (index + 1) + " / " + count;
        g.drawString(font, num, bx + bw - 14 - font.width(num), by + bh - 14, UiTheme.TEXT_DIM, false);
        if (index > 0) g.drawString(font, "<", bx + 10, by + bh - 14, UiTheme.CYAN, false);
        if (index < count - 1) g.drawString(font, ">", bx + bw - 14, by + bh - 14, UiTheme.CYAN, false);
    }

    /** A side tab: a chip with its glyph; the active tab is long and bright. */
    public static void tab(GuiGraphics g, Font font, int tx, int ty, int ext, boolean on, boolean hover, String glyph) {
        UiPanel.chip(g, tx - 2, ty, ext + 2, 24, on, hover, on ? UiTheme.GOLD : UiTheme.CYAN);
        g.drawCenteredString(font, glyph, tx + (ext - 2) / 2 + 1, ty + 8, on ? UiTheme.WHITE : UiTheme.CYAN);
    }

    public static void scrollbar(GuiGraphics g, int x, int y, int h, int pos, int bar) {
        g.fill(x, y, x + 3, y + h, UiMath.withAlpha(UiTheme.CYAN, 0.18f));
        g.fill(x, y + pos, x + 3, y + pos + bar, UiMath.withAlpha(UiTheme.CYAN, 0.85f));
        g.fill(x + 1, y + pos, x + 2, y + pos + bar, UiMath.withAlpha(UiTheme.WHITE, 0.6f));
    }

    /** A list row (the overview's magic list): a dim well, a bright marker on the left and a hover highlight when the mouse is over it. */
    public static void row(GuiGraphics g, int x, int y, int w, int h, int mx, int my, boolean mouseLive) {
        g.fill(x, y, x + w, y + h, UiMath.withAlpha(0x0A1226, 0.55f));
        g.fill(x, y + 1, x + 2, y + h - 1, UiMath.withAlpha(UiTheme.CYAN, 0.95f));
        if (mouseLive && mx >= x && mx < x + w && my >= y && my < y + h) UiPanel.hover(g, x, y, w, h, 1f, UiTheme.CYAN);
    }

    /** The summoning circle under the player figure: a flat (squashed) magic circle on the floor. */
    public static void floorCircle(GuiGraphics g, float cx, float cy, float radius, float now) {
        g.pose().pushPose();
        g.pose().translate(cx, cy, 0f);
        g.pose().scale(1f, 0.28f, 1f);
        MagicCircle.draw(g, 0f, 0f, radius, now, UiTheme.CYAN, 0.8f, MagicCircle.STANDARD, 1f);
        g.pose().popPose();
    }
}
