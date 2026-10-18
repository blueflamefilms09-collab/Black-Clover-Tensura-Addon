package com.newuniverse.nusmp.client.skilltree;

import com.newuniverse.nusmp.client.skilltree.layout.Rect;
import com.newuniverse.nusmp.client.ui.theme.CodexSkin;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.client.ui.theme.UiPanel;
import com.newuniverse.nusmp.client.ui.theme.UiTheme;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * Why: the mod's menus have two looks (the anime glass look by default and the classic flat look, config {@code ui.theme}), and every screen is expected to honour the switch at
 * once, so the look cannot be scattered through the screen's drawing code as a dozen {@code if} statements. This class is the one place that knows both: each method draws the same
 * element (a panel, a well, a chip, a node frame, a padlock) in whichever look is on, built from the same toolkit the other themed screens use ({@code UiPanel}, {@code CodexSkin},
 * {@code UiTheme}), so the tree looks like part of the mod and not like a visitor. Everything is plain {@code GuiGraphics} fills, the same as the toolkit (no textures), which keeps
 * the draw calls cheap enough to run for forty nodes every frame and lets the preview tool re-implement them exactly. The classic palette is the one of the old Multiverse Status
 * screen. All static and allocation-free; the colours the callers pass are RGB ints and the methods add the alpha.
 */
final class TreeSkin {
    private TreeSkin() {}

    // the classic look (the old status screen's flat panels)
    private static final int CLASSIC_BG = 0xE0101016, CLASSIC_BORDER = 0xFF3B3B4C, CLASSIC_WELL = 0xF00A0A10, CLASSIC_TEXT = 0xFFE6E6EE, CLASSIC_DIM = 0xFF9A9AAE, CLASSIC_FAINT = 0xFF6A6A7C;

    static boolean anime() { return UiTheme.anime(); }

    static int text() { return anime() ? UiTheme.TEXT : CLASSIC_TEXT; }

    static int dim() { return anime() ? UiTheme.TEXT_DIM : CLASSIC_DIM; }

    static int faint() { return anime() ? UiTheme.TEXT_FAINT : CLASSIC_FAINT; }

    static int gold() { return UiTheme.GOLD; }

    static int good() { return UiTheme.GREEN; }

    static int bad() { return UiTheme.RED; }

    /** The opaque colour of an RGB int. */
    static int opaque(int rgb) { return 0xFF000000 | (rgb & 0xFFFFFF); }

    /** The grey a sealed route is drawn in. */
    static final int SEALED = 0xFF7C8496;

    // ------------------------------------------------------------------ containers
    /** The big outer panel: glowing glass in the anime look, a flat bordered box in the classic look. */
    static void panel(GuiGraphics g, Rect r, int rgb, float glow) {
        if (anime()) {
            UiPanel.panel(g, r.x(), r.y(), r.w(), r.h(), rgb, glow, 1f);
            return;
        }
        g.fill(r.x(), r.y(), r.right(), r.bottom(), CLASSIC_BG);
        g.renderOutline(r.x(), r.y(), r.w(), r.h(), CLASSIC_BORDER);
    }

    /** An inset area (the route list, the canvas, the detail card): a deeper well with a thin edge in the accent colour. */
    static void well(GuiGraphics g, Rect r, int rgb) {
        if (anime()) {
            UiPanel.well(g, r.x(), r.y(), r.w(), r.h(), rgb, 1f);
            return;
        }
        g.fill(r.x(), r.y(), r.right(), r.bottom(), CLASSIC_WELL);
        g.renderOutline(r.x(), r.y(), r.w(), r.h(), CLASSIC_BORDER);
    }

    /** A section heading with its rule; returns the y below it. */
    static int heading(GuiGraphics g, Font font, String text, int x, int y, int w, int rgb) {
        if (anime()) return UiPanel.heading(g, font, text, x, y, w, rgb);
        g.drawString(font, text, x, y, opaque(rgb), false);
        g.hLine(x, x + w - 1, y + font.lineHeight + 1, CLASSIC_BORDER);
        return y + font.lineHeight + 5;
    }

    /** A selectable row or a small button drawn by hand: a tinted chip, brighter when active or hovered. */
    static void chip(GuiGraphics g, Rect r, boolean active, boolean hovered, int rgb) {
        if (anime()) {
            UiPanel.chip(g, r.x(), r.y(), r.w(), r.h(), active, hovered, rgb);
            return;
        }
        g.fill(r.x(), r.y(), r.right(), r.bottom(), active ? 0xFF2A2A3C : hovered ? 0xFF1E1E2C : 0xFF14141C);
        g.renderOutline(r.x(), r.y(), r.w(), r.h(), active ? opaque(rgb) : CLASSIC_BORDER);
    }

    /** {@link #chip(GuiGraphics, Rect, boolean, boolean, int)} without a {@code Rect}, for a list that draws a dozen rows every frame and must not allocate one for each. */
    static void chip(GuiGraphics g, int x, int y, int w, int h, boolean active, boolean hovered, int rgb) {
        if (anime()) {
            UiPanel.chip(g, x, y, w, h, active, hovered, rgb);
            return;
        }
        g.fill(x, y, x + w, y + h, active ? 0xFF2A2A3C : hovered ? 0xFF1E1E2C : 0xFF14141C);
        g.renderOutline(x, y, w, h, active ? opaque(rgb) : CLASSIC_BORDER);
    }

    static void scrollbar(GuiGraphics g, int x, int y, int h, int pos, int bar) {
        if (anime()) {
            CodexSkin.scrollbar(g, x, y, h, pos, bar);
            return;
        }
        g.fill(x, y, x + 3, y + h, 0x60404050);
        g.fill(x, y + pos, x + 3, y + pos + bar, CLASSIC_DIM);
    }

    // ------------------------------------------------------------------ the node
    /**
     * The frame of a node: a body tinted by the accent colour (more tint = more owned), a one pixel edge and short corner ticks. Smaller than the toolkit's panel on purpose: a node at
     * low zoom is a dozen pixels wide and the panel's halo and brackets would swallow it.
     */
    static void nodeFrame(GuiGraphics g, int x, int y, int size, int rgb, float body, float edge) {
        int a = rgb & 0xFFFFFF;
        int top = UiMath.withAlpha(UiMath.mix(0xFF0A1226, 0xFF000000 | a, 0.30f), body);
        int bottom = UiMath.withAlpha(UiMath.mix(0xFF060A16, 0xFF000000 | a, 0.12f), body);
        g.fillGradient(x, y, x + size, y + size, top, bottom);
        g.renderOutline(x, y, size, size, UiMath.withAlpha(a, edge));
        int len = Math.max(2, size / 4);
        int c = UiMath.withAlpha(a, Math.min(1f, edge + 0.15f));
        g.fill(x, y, x + len, y + 1, c); g.fill(x, y, x + 1, y + len, c);
        g.fill(x + size - len, y, x + size, y + 1, c); g.fill(x + size - 1, y, x + size, y + len, c);
        g.fill(x, y + size - 1, x + len, y + size, c); g.fill(x, y + size - len, x + 1, y + size, c);
        g.fill(x + size - len, y + size - 1, x + size, y + size, c); g.fill(x + size - 1, y + size - len, x + size, y + size, c);
    }

    /** Four brackets standing a little outside a box: the "selected" marker. */
    static void selection(GuiGraphics g, int x, int y, int size, int argb) {
        int o = 3, len = Math.max(3, size / 3);
        int x0 = x - o, y0 = y - o, x1 = x + size + o, y1 = y + size + o;
        g.fill(x0, y0, x0 + len, y0 + 1, argb); g.fill(x0, y0, x0 + 1, y0 + len, argb);
        g.fill(x1 - len, y0, x1, y0 + 1, argb); g.fill(x1 - 1, y0, x1, y0 + len, argb);
        g.fill(x0, y1 - 1, x0 + len, y1, argb); g.fill(x0, y1 - len, x0 + 1, y1, argb);
        g.fill(x1 - len, y1 - 1, x1, y1, argb); g.fill(x1 - 1, y1 - len, x1, y1, argb);
    }

    // ------------------------------------------------------------------ glyphs
    /** A padlock, 6 x 9 pixels, its top left corner at (x, y). */
    static void padlock(GuiGraphics g, int x, int y, int argb) { padlock(g, x, y, argb, 0xC0000000); }

    /** A padlock whose keyhole has its own colour (the big faint one behind a sealed canvas wants a keyhole that is only a little darker than its body). */
    static void padlock(GuiGraphics g, int x, int y, int argb, int keyhole) {
        g.fill(x + 1, y + 1, x + 2, y + 4, argb);              // the shackle's left post
        g.fill(x + 4, y + 1, x + 5, y + 4, argb);              // and its right post
        g.fill(x + 2, y, x + 4, y + 1, argb);                  // and its top
        g.fill(x, y + 4, x + 6, y + 9, argb);                  // the body
        g.fill(x + 2, y + 5, x + 4, y + 7, keyhole);           // the keyhole
        g.fill(x + 2, y + 7, x + 3, y + 8, keyhole);
    }

    /** A Skill Point gem, 7 x 7 pixels, its top left corner at (x, y). */
    static void gem(GuiGraphics g, int x, int y, int argb) {
        g.fill(x + 3, y, x + 4, y + 1, argb);
        g.fill(x + 2, y + 1, x + 5, y + 2, argb);
        g.fill(x + 1, y + 2, x + 6, y + 3, argb);
        g.fill(x, y + 3, x + 7, y + 4, argb);
        g.fill(x + 1, y + 4, x + 6, y + 5, argb);
        g.fill(x + 2, y + 5, x + 5, y + 6, argb);
        g.fill(x + 3, y + 6, x + 4, y + 7, argb);
        g.fill(x + 3, y + 1, x + 4, y + 3, UiMath.withAlpha(0xFFFFFF, 0.55f));            // a facet
    }
}
