package com.newuniverse.nusmp.client.ui.theme;

import com.newuniverse.nusmp.client.ui.gs.GsDraw;
import com.newuniverse.nusmp.client.ui.gs.GsSkin;
import com.newuniverse.nusmp.client.ui.gs.GsUser;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

/**
 * The panel kit of the anime theme: layered translucent panels with a thin glowing edge, corner brackets and a scan sweep, section headings,
 * hover highlights, tab chips and screen scrims. Everything is plain {@link GuiGraphics} fills (no textures, no allocation), alpha is always
 * scaled by the 'alpha' argument so a panel can fade as a whole.
 */
public final class UiPanel {
    private UiPanel() {}

    // ---------------------------------------------------------------- panels
    /** A translucent panel: soft halo, body gradient, top sheen, inner line, glowing edge and corner brackets in the accent colour. */
    public static void panel(GuiGraphics g, int x, int y, int w, int h, int accent, float glow, float alpha) {
        GsSkin gs = UiTheme.greatSpirit() ? GsUser.skin() : null;                    // 2.x: the great_spirit look re-colours the same panel and adds filigree corners
        if (gs != null) accent = GsDraw.tint(accent, gs);
        int a = accent & 0xFFFFFF;
        g.renderOutline(x - 2, y - 2, w + 4, h + 4, UiMath.withAlpha(a, 0.05f * glow * alpha));
        g.renderOutline(x - 1, y - 1, w + 2, h + 2, UiMath.withAlpha(a, 0.12f * glow * alpha));
        if (gs == null) g.fillGradient(x, y, x + w, y + h, UiMath.scaleAlpha(UiTheme.PANEL_TOP, alpha), UiMath.scaleAlpha(UiTheme.PANEL_BOTTOM, alpha));
        else g.fillGradient(x, y, x + w, y + h, UiMath.withAlpha(GsDraw.PANEL_TOP[gs.ordinal()], 0.85f * alpha), UiMath.withAlpha(GsDraw.PANEL_BOTTOM[gs.ordinal()], 0.88f * alpha));
        g.fillGradient(x + 1, y + 1, x + w - 1, y + 1 + Math.min(h / 3, 26), UiMath.withAlpha(0xFFFFFF, 0.07f * alpha), 0x00FFFFFF);
        g.renderOutline(x + 1, y + 1, w - 2, h - 2, UiMath.withAlpha(a, 0.16f * alpha));
        g.renderOutline(x, y, w, h, UiMath.withAlpha(a, (0.32f + 0.45f * glow) * alpha));
        brackets(g, x, y, w, h, 6, UiMath.withAlpha(a, 0.95f * alpha));
        if (gs != null) GsDraw.corners(g, x, y, w, h, gs, alpha);
    }

    /** {@link #panel} plus a slow scan line sweeping down it (needs the clock: {@link UiTheme#now()}). */
    public static void panelScan(GuiGraphics g, int x, int y, int w, int h, int accent, float glow, float alpha, float time) {
        panel(g, x, y, w, h, accent, glow, alpha);
        sweep(g, x + 1, y + 1, w - 2, h - 2, time, accent, alpha);
    }

    /** A deeper, flatter panel for insets (slots, bars, lists). */
    public static void well(GuiGraphics g, int x, int y, int w, int h, int accent, float alpha) {
        if (UiTheme.greatSpirit()) accent = GsDraw.tint(accent, GsUser.skin());
        g.fill(x, y, x + w, y + h, UiMath.scaleAlpha(UiTheme.PANEL_DEEP, alpha));
        g.renderOutline(x, y, w, h, UiMath.withAlpha(accent & 0xFFFFFF, 0.38f * alpha));
    }

    /** L-shaped brackets at the four corners, 'len' pixels long and two thick, drawn just inside the edge. */
    public static void brackets(GuiGraphics g, int x, int y, int w, int h, int len, int argb) {
        g.fill(x, y, x + len, y + 2, argb);
        g.fill(x, y, x + 2, y + len, argb);
        g.fill(x + w - len, y, x + w, y + 2, argb);
        g.fill(x + w - 2, y, x + w, y + len, argb);
        g.fill(x, y + h - 2, x + len, y + h, argb);
        g.fill(x, y + h - len, x + 2, y + h, argb);
        g.fill(x + w - len, y + h - 2, x + w, y + h, argb);
        g.fill(x + w - 2, y + h - len, x + w, y + h, argb);
    }

    /** A faint horizontal scan line with a soft tail moving down through the box (one cycle every ~(h+16)/0.7 ticks). */
    public static void sweep(GuiGraphics g, int x, int y, int w, int h, float time, int accent, float alpha) {
        if (h < 8 || w < 8) return;
        int yy = y + (int) ((time * 0.7f) % (h + 16)) - 8;
        int a = accent & 0xFFFFFF;
        int top = Math.max(y, yy - 7), bot = Math.min(y + h, yy);
        if (bot > top) g.fillGradient(x, top, x + w, bot, UiMath.withAlpha(a, 0f), UiMath.withAlpha(a, 0.10f * alpha));
        if (yy >= y && yy < y + h) g.fill(x, yy, x + w, yy + 1, UiMath.withAlpha(a, 0.22f * alpha));
    }

    // ---------------------------------------------------------------- text furniture
    /** A section heading: the text in the accent colour, a rule under it that is bright at the start and fades, and a small end mark. Returns the y below it. */
    public static int heading(GuiGraphics g, Font font, String text, int x, int y, int w, int rgb) {
        int a = rgb & 0xFFFFFF;
        g.drawString(font, text, x, y, 0xFF000000 | a, false);
        int ry = y + font.lineHeight + 1;
        g.fill(x, ry, x + w, ry + 1, UiMath.withAlpha(a, 0.22f));
        g.fill(x, ry, x + Math.min(w, 30), ry + 1, UiMath.withAlpha(a, 0.9f));
        g.fill(x + w - 3, ry - 1, x + w, ry + 2, UiMath.withAlpha(a, 0.7f));
        return y + font.lineHeight + 5;
    }

    /** Hover highlight over a row: a tinted wash, a bright bar on the left and a hairline under it. 'amount' is 0..1. */
    public static void hover(GuiGraphics g, int x, int y, int w, int h, float amount, int rgb) {
        if (amount <= 0.01f) return;
        if (UiTheme.greatSpirit()) rgb = GsDraw.tint(rgb, GsUser.skin());
        int a = rgb & 0xFFFFFF;
        g.fill(x, y, x + w, y + h, UiMath.withAlpha(a, 0.13f * amount));
        g.fill(x, y, x + 2, y + h, UiMath.withAlpha(a, 0.9f * amount));
        g.fill(x, y + h - 1, x + w, y + h, UiMath.withAlpha(a, 0.25f * amount));
    }

    /** A tab chip (the Codex's side tabs): translucent, accent edge, brighter when active or hovered. */
    public static void chip(GuiGraphics g, int x, int y, int w, int h, boolean active, boolean hovered, int rgb) {
        if (UiTheme.greatSpirit()) rgb = GsDraw.tint(rgb, GsUser.skin());
        int a = rgb & 0xFFFFFF;
        float k = active ? 1f : hovered ? 0.6f : 0.25f;
        g.fillGradient(x, y, x + w, y + h, UiMath.withAlpha(a, 0.10f + 0.28f * k), UiMath.withAlpha(a, 0.04f + 0.14f * k));
        g.renderOutline(x, y, w, h, UiMath.withAlpha(a, 0.35f + 0.6f * k));
        if (active) g.fill(x + w - 2, y + 2, x + w, y + h - 2, UiMath.withAlpha(0xFFFFFF, 0.85f));
    }

    // ---------------------------------------------------------------- screen furniture
    /** A scrim for the whole screen (darker toward the bottom) with a soft vignette at the edges. */
    public static void scrim(GuiGraphics g, int w, int h, float alpha) {
        g.fillGradient(0, 0, w, h, UiMath.withAlpha(0x050812, 0.62f * alpha), UiMath.withAlpha(0x020409, 0.82f * alpha));
        int e = Math.max(24, Math.min(w, h) / 5);
        int edge = UiMath.withAlpha(0x000000, 0.45f * alpha), none = 0x00000000;
        g.fillGradient(0, 0, w, e, edge, none);
        g.fillGradient(0, h - e, w, h, none, edge);
    }
}
