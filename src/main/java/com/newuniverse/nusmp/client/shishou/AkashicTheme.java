package com.newuniverse.nusmp.client.shishou;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.shishou.ShishouGuiLayout;
import com.newuniverse.nusmp.shishou.ShishouGuiLayout.Region;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Why: the Akashic look of Shishou's own GUIs (the Stomach screen and the HUD, 2.1.0): a deep blue-violet star field, fine prismatic line-work, a glowing cyan frame (the cyan of the blade's fuller lines), a
 * sword emblem, slowly drifting light motes and glowing gauges. Every piece is a GuiGraphics fill / gradient or a blit of the small atlas {@code textures/gui/shishou/akashic.png} (made by
 * {@code tools/shishou/gen_shishou_gui.py}); the numbers are {@link ShishouGuiLayout}. Cost is bounded and stated in the docs: a panel is at most about 12 star-tile blits (clipped), 14 motes, 4 corners, 2 frame
 * outlines and one gradient; a gauge is six fills. Nothing is allocated per frame; the colours are ints, the clock is the theme's. With the classic theme a plain flat panel is drawn instead.
 */
public final class AkashicTheme {
    private AkashicTheme() {}

    public static final ResourceLocation ATLAS = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/gui/shishou/akashic.png");

    private static int argb(int rgb, float a) { return UiMath.withAlpha(rgb & 0xFFFFFF, a); }

    /** Blits a region of the atlas at its own size. */
    public static void blit(GuiGraphics g, Region r, int x, int y) { g.blit(ATLAS, x, y, r.w(), r.h(), r.u(), r.v(), r.w(), r.h(), ShishouGuiLayout.ATLAS, ShishouGuiLayout.ATLAS); }

    /** Blits a region stretched to {@code w x h}. */
    public static void blit(GuiGraphics g, Region r, int x, int y, int w, int h) { g.blit(ATLAS, x, y, w, h, r.u(), r.v(), r.w(), r.h(), ShishouGuiLayout.ATLAS, ShishouGuiLayout.ATLAS); }

    /**
     * The panel: gradient, the star field (slowly scrolling, clipped to the panel), a glowing cyan frame that breathes, corner ornaments and the prismatic line under the title row. {@code flat} draws the classic
     * look instead (one fill and an outline). Motes are drawn by {@link #motes}.
     */
    public static void panel(GuiGraphics g, int x, int y, int w, int h, float now, boolean flat, int prismY) {
        if (flat) {
            g.fill(x, y, x + w, y + h, 0xF010121E);
            g.renderOutline(x, y, w, h, argb(ShishouGuiLayout.CYAN, 0.9f));
            return;
        }
        g.fillGradient(x, y, x + w, y + h, argb(ShishouGuiLayout.INK_TOP, 1f), argb(ShishouGuiLayout.INK_BOT, 1f));
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(1f, 1f, 1f, 0.5f);
        g.enableScissor(x, y, x + w, y + h);
        Region s = ShishouGuiLayout.STAR;
        int ox = (int) (now * 0.08f) % s.w(), oy = (int) (now * 0.03f) % s.h();
        for (int ty = y - oy; ty < y + h; ty += s.h()) for (int tx = x - ox; tx < x + w; tx += s.w()) blit(g, s, tx, ty);
        g.disableScissor();
        g.setColor(1f, 1f, 1f, 1f);
        float breath = UiMath.pulse(now, 120f, 0f);
        g.renderOutline(x - 2, y - 2, w + 4, h + 4, argb(ShishouGuiLayout.CYAN, 0.05f + 0.07f * breath));       // the soft glow around the frame
        g.renderOutline(x - 1, y - 1, w + 2, h + 2, argb(ShishouGuiLayout.CYAN, 0.18f + 0.14f * breath));
        g.renderOutline(x, y, w, h, argb(ShishouGuiLayout.CYAN_HI, 0.75f));
        g.renderOutline(x + 1, y + 1, w - 2, h - 2, argb(ShishouGuiLayout.VIOLET, 0.28f));
        blit(g, ShishouGuiLayout.CORNER_TL, x, y);
        blit(g, ShishouGuiLayout.CORNER_TR, x + w - 16, y);
        blit(g, ShishouGuiLayout.CORNER_BL, x, y + h - 16);
        blit(g, ShishouGuiLayout.CORNER_BR, x + w - 16, y + h - 16);
        diamond(g, x + w / 2, y, 0xFF000000 | ShishouGuiLayout.GOLD);                                  // the gold studs at the middle of the top and the bottom edge
        diamond(g, x + w / 2, y + h - 1, 0xFF000000 | ShishouGuiLayout.GOLD);
        if (prismY > 0) prismLine(g, x + 6, y + prismY, w - 12, now);
    }

    /** A 5 x 3 diamond stud centred on (cx, cy): three fills. */
    private static void diamond(GuiGraphics g, int cx, int cy, int argb) {
        g.fill(cx - 2, cy, cx + 3, cy + 1, argb);
        g.fill(cx - 1, cy - 1, cx + 2, cy, argb);
        g.fill(cx - 1, cy + 1, cx + 2, cy + 2, argb);
    }

    /** The prismatic line (cyan, violet, gold), with a pale shimmer that runs along it. */
    public static void prismLine(GuiGraphics g, int x, int y, int w, float now) {
        Region r = ShishouGuiLayout.PRISM;
        RenderSystem.enableBlend();
        g.setColor(1f, 1f, 1f, 0.9f);
        blit(g, r, x, y, w, r.h());
        g.setColor(1f, 1f, 1f, 1f);
        int sx = x + (int) ((now * 0.9f) % (w + 40)) - 20;
        int s0 = Math.max(x, sx), s1 = Math.min(x + w, sx + 16);
        if (s1 > s0) g.fill(s0, y, s1, y + 1, argb(0xFFFFFF, 0.55f));
    }

    /** The drifting light motes inside a clipped rectangle (additive, tiny): {@code n} of them, never more than {@link ShishouGuiLayout#MOTES_PANEL}. */
    public static void motes(GuiGraphics g, int x, int y, int w, int h, float now, int n) {
        int count = Math.min(n, ShishouGuiLayout.MOTES_PANEL);
        Region m = ShishouGuiLayout.MOTE;
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        for (int i = 0; i < count; i++) {
            float a = ShishouGuiLayout.moteAlpha(i, now);
            g.setColor(1f, 1f, 1f, a);
            int mx = x + (int) ShishouGuiLayout.moteX(i, now, w), my = y + (int) ShishouGuiLayout.moteY(i, now, h);
            if (mx > x + w - 8 || my > y + h - 8) continue;                                              // keep them inside: no scissor needed
            blit(g, m, mx, my);
        }
        g.setColor(1f, 1f, 1f, 1f);
        RenderSystem.defaultBlendFunc();
    }

    /** A glowing gauge: the glass tube from the atlas, the fill as a gradient between two tones with a travelling shimmer and a bright leading edge. {@code frac} is 0..1. */
    public static void gauge(GuiGraphics g, int x, int y, int w, int h, float frac, int lowRgb, int highRgb, float now, boolean flat) {
        if (flat) {
            g.fill(x, y, x + w, y + h, 0xC0000000);
            g.fill(x, y, x + ShishouGuiLayout.gaugePixels(frac, w), y + h, argb(highRgb, 1f));
            return;
        }
        g.fill(x - 1, y - 1, x + w + 1, y + h + 1, argb(ShishouGuiLayout.CYAN, 0.28f));
        g.fill(x, y, x + w, y + h, 0xFF03050F);
        int fw = ShishouGuiLayout.gaugePixels(frac, w);
        if (fw > 0) {
            g.fillGradient(x, y, x + fw, y + h, argb(highRgb, 1f), argb(lowRgb, 1f));
            int sx = x + (int) ((now * 1.1f) % (fw + 30)) - 14;
            int s0 = Math.max(x, sx), s1 = Math.min(x + fw, sx + 10);
            if (s1 > s0) g.fill(s0, y, s1, y + h, argb(0xFFFFFF, 0.30f));
            g.fill(x + fw - 1, y - 1, x + fw, y + h + 1, argb(0xFFFFFF, 0.85f));                          // the bright leading edge
            g.fill(x + fw, y, x + fw + 1, y + h, argb(highRgb, 0.45f));
            g.fill(x, y, x + fw, y + 1, argb(0xFFFFFF, 0.28f));
        }
        for (int k = 1; k < 10; k++) {
            int tx = x + w * k / 10;
            g.fill(tx, y, tx + 1, y + h, tx <= x + fw ? argb(0x000000, 0.25f) : argb(0xFFFFFF, 0.08f));
        }
    }

    /** The sword emblem at {@code size} px. */
    public static void emblem(GuiGraphics g, int x, int y, int size) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(1f, 1f, 1f, 1f);
        blit(g, ShishouGuiLayout.EMBLEM, x, y, size, size);
    }

    /** One row of nine slot cells (162 x 18) at the top left of the first cell's frame. */
    public static void slotRow(GuiGraphics g, int x, int y) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(1f, 1f, 1f, 1f);
        blit(g, ShishouGuiLayout.SLOTROW, x, y);
    }
}
