package com.newuniverse.nusmp.client.elite.hub;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Why: the quest dialogue, the shop and the map share one small look (a dark panel with a gold hairline, gold headings, muted body text, one accent per state) and a few drawing chores (a panel, a
 * button, a progress bar, a clipped single line). They live here once so the three screens and the overlay cannot drift apart, and so a colour change is one line. Plain {@code fill} calls
 * on the GuiGraphics: no textures, no shaders, no state; client only.
 */
final class HubGui {
    private HubGui() {}

    static final int PANEL = 0xE6141218, PANEL_EDGE = 0xFFB08A3A, ROW = 0xB0201C28, ROW_HOVER = 0xC0302A3C, GOLD = 0xFFE8C060, TEXT = 0xFFE8E4DC, MUTED = 0xFF9A96A2, GOOD = 0xFF6FD08C,
            WARN = 0xFFE07A5F, BAR_BG = 0xFF2A2630, BAR_FG = 0xFF5C9CE0, BUTTON = 0xFF3A3450, BUTTON_HOVER = 0xFF54486E, BUTTON_OFF = 0xFF2A2630;

    /** A dark panel with a one-pixel gold edge. */
    static void panel(GuiGraphics g, int x, int y, int w, int h) {
        g.fill(x, y, x + w, y + h, PANEL);
        g.fill(x, y, x + w, y + 1, PANEL_EDGE);
        g.fill(x, y + h - 1, x + w, y + h, PANEL_EDGE);
        g.fill(x, y, x + 1, y + h, PANEL_EDGE);
        g.fill(x + w - 1, y, x + w, y + h, PANEL_EDGE);
    }

    static boolean inside(double mx, double my, int x, int y, int w, int h) { return mx >= x && mx < x + w && my >= y && my < y + h; }

    /** A button with a centred label; returns nothing: the screen tests the same rectangle on click. */
    static void button(GuiGraphics g, net.minecraft.client.gui.Font font, int x, int y, int w, int h, String label, boolean enabled, boolean hover) {
        g.fill(x, y, x + w, y + h, !enabled ? BUTTON_OFF : hover ? BUTTON_HOVER : BUTTON);
        g.fill(x, y, x + w, y + 1, enabled ? PANEL_EDGE : MUTED);
        g.drawString(font, label, x + (w - font.width(label)) / 2, y + (h - 8) / 2 + 1, enabled ? TEXT : MUTED, false);
    }

    /** A progress bar. */
    static void bar(GuiGraphics g, int x, int y, int w, int h, double fraction, int colour) {
        g.fill(x, y, x + w, y + h, BAR_BG);
        int filled = (int) Math.round(w * Math.max(0.0, Math.min(1.0, fraction)));
        if (filled > 0) g.fill(x, y, x + filled, y + h, colour);
    }

    /** A line cut to {@code maxWidth} pixels with an ellipsis. */
    static String clip(net.minecraft.client.gui.Font font, String s, int maxWidth) {
        if (font.width(s) <= maxWidth) return s;
        String ell = "...";
        int n = s.length();
        while (n > 0 && font.width(s.substring(0, n)) + font.width(ell) > maxWidth) n--;
        return s.substring(0, n) + ell;
    }
}
