package com.newuniverse.nusmp.client.gui.bossbar;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.newuniverse.nusmp.boss.bar.BossBarColors;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.LerpingBossEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.BossEvent;

import java.util.Locale;

/**
 * Draws the boss bars: the vanilla look for bars the mod did not style (the same sprites, sizes and order as vanilla's
 * {@code BossHealthOverlay}) and the animated bar of the mod's bosses. Everything is textured blits and a few fills; scrolling is
 * done by tiling a texture with an offset, so no repeat mode or shader is needed, and a bar is a few dozen quads. Nothing here is allocated per frame.
 *
 * <p>The animated bar (all coordinates relative to the top y of the fill's slot, {@code t}):
 * <ul>
 *   <li>the window of the fill is 182x7 at (x, t+3); the frame (210x15) is drawn over it at (x-14, t-1) and has glow lanes in its top and bottom rim;</li>
 *   <li>layers, bottom to top: dark background, grey fill tinted from the primary colour toward the secondary, two scrolling flame layers
 *       and a travelling shine and a bright leading edge (additive), glitch slices, flash, the frame, the glow lanes scrolling in
 *       opposite directions (additive), the five phase gems;</li>
 *   <li>a shatter draws the bar in twelve vertical strips (scissored), each at its own offset.</li>
 * </ul>
 */
final class BossBarPainter {
    private BossBarPainter() {}

    static final int CAP = 14, FRAME_W = 182 + 2 * CAP, FRAME_H = 15, FILL_X_IN_FRAME = CAP, FILL_Y = 3, FILL_H = 7, FRAME_TOP = -1;
    private static final int SEG = 23, PIP_COUNT = 5, PIP_W = 8, PIP_GAP = 2, PIP_Y = 10;
    private static final int WHITE = 0xFFFFFF;

    private static final ResourceLocation FRAME = tex("frame"), BG = tex("bg"), FILL = tex("fill"), FLAME = tex("flame"),
            SHINE = tex("shine"), GLOW = tex("glow"), PIPS = tex("pips"), EDGE = tex("edge");

    private static final ResourceLocation[] BAR_BG = new ResourceLocation[BossEvent.BossBarColor.values().length];
    private static final ResourceLocation[] BAR_PROGRESS = new ResourceLocation[BAR_BG.length];
    private static final ResourceLocation[] OVERLAY_BG = new ResourceLocation[BossEvent.BossBarOverlay.values().length];
    private static final ResourceLocation[] OVERLAY_PROGRESS = new ResourceLocation[OVERLAY_BG.length];

    static {
        for (BossEvent.BossBarColor c : BossEvent.BossBarColor.values()) {
            String n = c.name().toLowerCase(Locale.ROOT);
            BAR_BG[c.ordinal()] = ResourceLocation.withDefaultNamespace("boss_bar/" + n + "_background");
            BAR_PROGRESS[c.ordinal()] = ResourceLocation.withDefaultNamespace("boss_bar/" + n + "_progress");
        }
        for (BossEvent.BossBarOverlay o : BossEvent.BossBarOverlay.values()) {
            if (o == BossEvent.BossBarOverlay.PROGRESS) continue;
            String n = o.name().toLowerCase(Locale.ROOT);
            OVERLAY_BG[o.ordinal()] = ResourceLocation.withDefaultNamespace("boss_bar/" + n + "_background");
            OVERLAY_PROGRESS[o.ordinal()] = ResourceLocation.withDefaultNamespace("boss_bar/" + n + "_progress");
        }
    }

    private static ResourceLocation tex(String name) {
        return ResourceLocation.fromNamespaceAndPath("nusmp", "textures/gui/bossbar/" + name + ".png");
    }

    // ================================================================ vanilla look

    /** A bar exactly as vanilla draws it: background, its notches, then the progress and its notches. (The name is drawn by the caller.) */
    static void paintVanilla(GuiGraphics g, LerpingBossEvent ev, int x, int y) {
        int ci = ev.getColor().ordinal();
        int oi = ev.getOverlay().ordinal();
        boolean notched = ev.getOverlay() != BossEvent.BossBarOverlay.PROGRESS;
        int w = BossBarLayout.fillWidth(ev.getProgress());
        g.blitSprite(BAR_BG[ci], 182, 5, 0, 0, x, y, 182, 5);
        if (notched) {
            RenderSystem.enableBlend();
            g.blitSprite(OVERLAY_BG[oi], 182, 5, 0, 0, x, y, 182, 5);
            RenderSystem.disableBlend();
        }
        if (w > 0) {
            g.blitSprite(BAR_PROGRESS[ci], 182, 5, 0, 0, x, y, w, 5);
            if (notched) {
                RenderSystem.enableBlend();
                g.blitSprite(OVERLAY_PROGRESS[oi], 182, 5, 0, 0, x, y, w, 5);
                RenderSystem.disableBlend();
            }
        }
    }

    // ================================================================ the animated bar

    /**
     * The animated bar with its name and percentage. 'x' is the left of the 182-wide window, 'top' the y of the vanilla bar's slot (the
     * name sits 9 above it). 'effects' = shake, glitch, flash and shatter allowed; 'shatterOk' = this bar may use the costly shatter this frame.
     */
    static void paintMod(GuiGraphics g, Font font, LerpingBossEvent ev, BossBarClientState.Entry en, int x, int top, double time,
                         boolean effects, boolean showPercent, boolean shatterOk) {
        BossBarAnim an = en.anim;
        int sx = effects ? Math.round(an.shakeX()) : 0;
        int sy = effects ? Math.round(an.shakeY()) : 0;
        float progress = ev.getProgress();
        int fw = BossBarLayout.fillWidth(progress);
        try {
            RenderSystem.enableBlend();
            if (effects && shatterOk && an.shattering()) {
                paintShattered(g, an, x + sx, top + sy, fw, time);
            } else {
                paintBody(g, an, x + sx, top + sy, fw, time, 1f, false, effects);
            }
        } finally {
            g.setColor(1f, 1f, 1f, 1f);
            RenderSystem.defaultBlendFunc();
        }
        Component name = ev.getName();
        g.drawString(font, name, x + 91 - font.width(name) / 2 + sx, top - 9 + sy, WHITE);
        if (showPercent) {
            String pct = en.percentText(progress);
            g.drawString(font, pct, x + BossBarLayout.BAR_W - font.width(pct) + sx, top - 9 + sy, WHITE);
        }
    }

    /** The bar in twelve strips that each have their own offset (the scissor moves with the strip). */
    private static void paintShattered(GuiGraphics g, BossBarAnim an, int x, int t, int fw, double time) {
        int left = x - CAP;
        for (int c = 0; c < BossBarAnim.COLUMNS; c++) {
            int x0 = left + FRAME_W * c / BossBarAnim.COLUMNS, x1 = left + FRAME_W * (c + 1) / BossBarAnim.COLUMNS;
            int dx = Math.round(an.colDx(c)), dy = Math.round(an.colDy(c));
            g.enableScissor(x0 + dx, t + FRAME_TOP + dy, x1 + dx, t + FRAME_TOP + FRAME_H + 4 + dy);
            try {
                paintBody(g, an, x + dx, t + dy, fw, time, an.shatterAlpha(), true, false);
            } finally {
                g.disableScissor();
            }
        }
    }

    /**
     * One copy of the bar. 'lite' = the cheap version used per strip of a shatter (no shine, one flame layer, no glitch);
     * 'effects' = glitch slices and flash are drawn.
     */
    private static void paintBody(GuiGraphics g, BossBarAnim an, int x, int t, int fw, double time, float alpha, boolean lite, boolean effects) {
        int p = an.primary(), s = an.secondary();
        boolean rage = an.rage();
        float speed = rage ? 1.8f : 1f;
        int fy = t + FILL_Y;

        // inside of the window
        RenderSystem.defaultBlendFunc();
        colour(g, p, 0.30f, alpha);
        g.blit(BG, x, fy, 0, 0, BossBarLayout.BAR_W, FILL_H, BossBarLayout.BAR_W, FILL_H);

        if (fw > 0) {
            // the grey fill, tinted from the primary colour toward the secondary along the bar
            for (int sx = 0; sx < fw; sx += SEG) {
                int w = Math.min(SEG, fw - sx);
                colour(g, BossBarColors.lerp(p, s, 0.35f * (sx + SEG / 2) / BossBarLayout.BAR_W), 1f, alpha);
                g.blit(FILL, x + sx, fy, sx, 0, w, FILL_H, BossBarLayout.BAR_W, FILL_H);
            }
            // glitch slices: rows of the fill moved sideways over a dark gap
            if (effects && !lite) {
                for (int i = 0; i < an.sliceCount(); i++) slice(g, an, x, fy, fw, i, p, s, alpha);
            }
            // additive layers: flames, the travelling shine, the bright leading edge
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            float flick = 0.85f + 0.15f * (float) Math.sin(time * 0.35);
            colour(g, s, 1f, 0.55f * flick * alpha * (rage ? 1.25f : 1f));
            scroll(g, FLAME, 64, FILL_H, x, fy, fw, -(int) Math.floor(time * 0.9 * speed));
            if (!lite) {
                colour(g, BossBarColors.lerp(s, WHITE, 0.3f), 1f, 0.35f * alpha * (rage ? 1.25f : 1f));
                scroll(g, FLAME, 64, FILL_H, x, fy, fw, (int) Math.floor(time * 0.55 * speed) + 32);
                shine(g, x, fy, fw, time, alpha);
            }
            if (fw > 2) {
                int ew = Math.min(4, fw);
                colour(g, s, 1f, 0.8f * alpha);
                g.blit(EDGE, x + fw - ew, fy, 4 - ew, 0, ew, FILL_H, 4, FILL_H);
            }
            RenderSystem.defaultBlendFunc();
            if (effects && an.flash() > 0f) {
                int a = Math.min(255, (int) (255 * an.flash() * alpha));
                g.fill(x, fy, x + fw, fy + FILL_H, (a << 24) | WHITE);
            }
        }

        // the frame, tinted by the primary colour
        colour(g, BossBarColors.lerp(WHITE, p, 0.5f), 1f, alpha);
        g.blit(FRAME, x - FILL_X_IN_FRAME, t + FRAME_TOP, 0, 0, FRAME_W, FRAME_H, FRAME_W, FRAME_H);

        // glow lanes in the rim: top runs one way, bottom the other (additive)
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        float pulse = 0.8f + 0.2f * (float) Math.sin(time * 0.2);
        colour(g, s, 1f, 0.85f * pulse * alpha);
        int lane = (int) Math.floor(time * 0.7 * (rage ? 1.6 : 1.0));
        scroll(g, GLOW, 64, 3, x, t, BossBarLayout.BAR_W, -lane);
        scroll(g, GLOW, 64, 3, x, t + 10, BossBarLayout.BAR_W, lane + 21);
        RenderSystem.defaultBlendFunc();

        pips(g, an, x, t, time, alpha);
    }

    private static void slice(GuiGraphics g, BossBarAnim an, int x, int fy, int fw, int i, int p, int s, float alpha) {
        int y = an.sliceY(i), h = an.sliceH(i), dx = an.sliceDx(i);
        if (Math.abs(dx) >= fw) return;
        colour(g, p, 0.22f, alpha);
        g.blit(BG, x, fy + y, 0, y, fw, h, BossBarLayout.BAR_W, FILL_H);
        colour(g, BossBarColors.lerp(p, s, 0.6f), 1.25f, alpha);
        if (dx > 0) g.blit(FILL, x + dx, fy + y, 0, y, fw - dx, h, BossBarLayout.BAR_W, FILL_H);
        else g.blit(FILL, x, fy + y, -dx, y, fw + dx, h, BossBarLayout.BAR_W, FILL_H);
    }

    private static void shine(GuiGraphics g, int x, int fy, int fw, double time, float alpha) {
        double cycle = (time % 80.0) / 80.0;
        int pos = (int) Math.floor(-28 + cycle * (fw + 56));
        int cut = Math.max(0, -pos);                                          // columns of the streak left of the fill
        int w = Math.min(28 - cut, fw - (pos + cut));
        if (w <= 0) return;
        colour(g, WHITE, 1f, 0.7f * alpha);
        g.blit(SHINE, x + pos + cut, fy, cut, 0, w, FILL_H, 28, FILL_H);
    }

    private static void pips(GuiGraphics g, BossBarAnim an, int x, int t, double time, float alpha) {
        int total = PIP_COUNT * PIP_W + (PIP_COUNT - 1) * PIP_GAP;
        int px = x + BossBarLayout.BAR_W / 2 - total / 2, py = t + PIP_Y;
        int phase = an.phase();
        for (int i = 0; i < PIP_COUNT; i++) {
            int gx = px + i * (PIP_W + PIP_GAP);
            if (i <= phase) {
                colour(g, an.secondary(), 1f, alpha);
                g.blit(PIPS, gx, py, PIP_W, 0, PIP_W, PIP_W, 16, PIP_W);
            } else {
                colour(g, WHITE, 1f, alpha);
                g.blit(PIPS, gx, py, 0, 0, PIP_W, PIP_W, 16, PIP_W);
            }
        }
        if (phase >= 0 && phase < PIP_COUNT) {                                // the current gem breathes
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            colour(g, an.secondary(), 1f, (0.35f + 0.3f * (float) Math.sin(time * 0.25)) * alpha);
            g.blit(PIPS, px + phase * (PIP_W + PIP_GAP), py, PIP_W, 0, PIP_W, PIP_W, 16, PIP_W);
            RenderSystem.defaultBlendFunc();
        }
    }

    /** Draws 'width' pixels of a horizontally tiling texture starting at u = offset (mod the tile width), in at most a few quads. */
    private static void scroll(GuiGraphics g, ResourceLocation tex, int tw, int th, int x, int y, int width, int offset) {
        int u = Math.floorMod(offset, tw);
        int drawn = 0;
        while (drawn < width) {
            int w = Math.min(tw - u, width - drawn);
            g.blit(tex, x + drawn, y, u, 0, w, th, tw, th);
            drawn += w;
            u = 0;
        }
    }

    /** The shader colour: the rgb scaled by 'k' (clamped) and an opacity. */
    private static void colour(GuiGraphics g, int rgb, float k, float alpha) {
        g.setColor(Math.min(1f, BossBarColors.red(rgb) * k), Math.min(1f, BossBarColors.green(rgb) * k),
                Math.min(1f, BossBarColors.blue(rgb) * k), Math.max(0f, Math.min(1f, alpha)));
    }
}
