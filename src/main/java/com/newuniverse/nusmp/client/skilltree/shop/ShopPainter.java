package com.newuniverse.nusmp.client.skilltree.shop;

import com.mojang.blaze3d.systems.RenderSystem;
import com.newuniverse.nusmp.client.skilltree.shop.ShopLayout.Rect;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Why: the shop screen is dusty and dim on purpose (an old witch's back room: plum-black glass, amber lamplight, cobwebs in the corners, motes of dust in the air), and that look
 * is a dozen small drawing routines that have nothing to do with the screen's state or its clicks. They live here, stateless and in one place, so the screen class stays about
 * what it shows and does, and so the preview tool can mirror them one for one. Everything is plain {@link GuiGraphics} fills and one texture (no shaders, no allocation per frame
 * beyond what the fills need), the amber/plum palette sits on top of the mod's own UI theme (the same panel kit, brackets and wells as the other screens), and every effect that
 * moves is a function of the shared clock, so it stands still when the player turned UI motion off.
 */
final class ShopPainter {
    private ShopPainter() {}

    /** The shop's palette (ARGB): plum for the witch's voice, amber for lamplight and gold, paper for text, and the three states of a price. */
    static final int PLUM = 0xFFB878E0, AMBER = 0xFFF0B650, PAPER = 0xFFEADFCB, DIM = 0xFFA09080, FAINT = 0xFF6A6070, GOOD = 0xFF9CE8A8, BAD = 0xFFE87868;

    /** The texture sheet of four cobwebs (96x96: one 48x48 web per corner, top-left, top-right, bottom-left, bottom-right). */
    static final ResourceLocation WEBS = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/gui/black_market/cobwebs.png");
    /** The witch's portrait (40x42). */
    static final ResourceLocation PORTRAIT = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/gui/black_market/witch.png");
    static final int PORTRAIT_W = 40, PORTRAIT_H = 42;

    /** The four webs, one in each corner of the rectangle, drawn over its body. The webs are mostly see-through, so blending is switched on first (nothing before this draw promises it is). */
    static void cobwebs(GuiGraphics g, Rect r) {
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        int s = 48;
        g.blit(WEBS, r.x(), r.y(), s, s, 0, 0, s, s, 96, 96);
        g.blit(WEBS, r.right() - s, r.y(), s, s, 48, 0, s, s, 96, 96);
        g.blit(WEBS, r.x(), r.bottom() - s, s, s, 0, 48, s, s, 96, 96);
        g.blit(WEBS, r.right() - s, r.bottom() - s, s, s, 48, 48, s, s, 96, 96);
    }

    /**
     * Motes of dust turning slowly in the lamplight: {@code count} one-pixel specks whose places are fixed numbers of their index and whose drift is the clock, so there is no state
     * and no allocation. Kept inside the rectangle.
     */
    static void dust(GuiGraphics g, Rect r, float now, int count) {
        for (int i = 0; i < count; i++) {
            float fx = frac(i * 0.6180339f + 0.13f);
            float fy = frac(i * 0.7548776f + 0.41f);
            float speed = 0.012f + 0.02f * frac(i * 0.3247f);
            int x = r.x() + 2 + (int) (fx * (r.w() - 4) + Math.sin(now * 0.02f + i * 1.7f) * 5.0);
            int y = r.y() + 2 + (int) (frac(fy - now * speed * 0.01f) * (r.h() - 4));
            if (x < r.x() + 1 || x >= r.right() - 1) continue;
            float tw = 0.5f + 0.5f * (float) Math.sin(now * 0.07f + i * 2.3f);
            g.fill(x, y, x + 1, y + 1, UiMath.withAlpha(0xF4E6C0, 0.10f + 0.28f * tw));
        }
    }

    private static float frac(float v) {
        return v - (float) Math.floor(v);
    }

    /** A coin, 7x7, in the shop's gold. */
    static void coin(GuiGraphics g, int x, int y, boolean lit) {
        int body = lit ? 0xFFE8B040 : 0xFF8A7A60, edge = lit ? 0xFFA06820 : 0xFF5A5040, shine = lit ? 0xFFFFF0A0 : 0xFFB0A488;
        g.fill(x + 2, y, x + 5, y + 1, edge);
        g.fill(x + 1, y + 1, x + 6, y + 2, body);
        g.fill(x, y + 2, x + 7, y + 5, body);
        g.fill(x + 1, y + 5, x + 6, y + 6, body);
        g.fill(x + 2, y + 6, x + 5, y + 7, edge);
        g.fill(x, y + 2, x + 1, y + 5, edge);
        g.fill(x + 6, y + 2, x + 7, y + 5, edge);
        g.fill(x + 2, y + 2, x + 4, y + 3, shine);
        g.fill(x + 3, y + 3, x + 4, y + 5, edge);
    }

    /** A small button in the shop's style: dark glass, an edge in the accent colour that brightens under the mouse, and a dimmed, flat look when it cannot be used. */
    static void button(GuiGraphics g, Font font, Rect r, String label, boolean enabled, boolean hovered, int accent) {
        int a = enabled ? accent : 0xFF5C5668;
        g.fill(r.x(), r.y(), r.right(), r.bottom(), UiMath.withAlpha(0x0A0710, 0.88f));
        g.fillGradient(r.x() + 1, r.y() + 1, r.right() - 1, r.bottom() - 1, UiMath.withAlpha(a, enabled && hovered ? 0.38f : 0.17f), UiMath.withAlpha(a, 0.05f));
        g.renderOutline(r.x(), r.y(), r.w(), r.h(), UiMath.withAlpha(a, enabled ? (hovered ? 1.0f : 0.75f) : 0.35f));
        int color = enabled ? (hovered ? 0xFFFFFFFF : PAPER) : 0xFF7A7080;
        g.drawCenteredString(font, label, r.x() + r.w() / 2, r.y() + (r.h() - 8) / 2, color);
    }

    /** A row's backing: a faint plank, lit when the mouse is over it, with a gold frame for the showpiece. */
    static void row(GuiGraphics g, Rect r, boolean hovered, boolean flagship, float now) {
        g.fill(r.x(), r.y(), r.right(), r.bottom(), UiMath.withAlpha(0x1A1222, hovered ? 0.92f : 0.70f));
        g.fill(r.x(), r.bottom() - 1, r.right(), r.bottom(), UiMath.withAlpha(0x6A4A2A, 0.55f));
        if (hovered) {
            g.fill(r.x(), r.y(), r.x() + 2, r.bottom(), UiMath.withAlpha(AMBER, 0.9f));
            g.fill(r.x() + 2, r.y(), r.right(), r.bottom() - 1, UiMath.withAlpha(AMBER, 0.07f));
        }
        if (flagship) {
            float glow = 0.55f + 0.25f * (float) Math.sin(now * 0.08f);
            g.renderOutline(r.x(), r.y(), r.w(), r.h() - 1, UiMath.withAlpha(AMBER, glow));
        }
    }

    /** A thin scrollbar: a dark track and a thumb. {@code thumb} is {@code {offset, height}} from {@link ShopLayout#thumb}, or null when nothing scrolls. */
    static void scrollbar(GuiGraphics g, Rect track, int[] thumb) {
        g.fill(track.x(), track.y(), track.right(), track.bottom(), UiMath.withAlpha(0x000000, 0.35f));
        if (thumb != null) g.fill(track.x(), track.y() + thumb[0], track.right(), track.y() + thumb[0] + thumb[1], UiMath.withAlpha(AMBER, 0.65f));
    }

    /** The witch's speech bubble in the footer: dark glass with a plum edge and a small tail pointing up-left at her portrait. */
    static void bubble(GuiGraphics g, Rect r) {
        g.fill(r.x(), r.y(), r.right(), r.bottom(), UiMath.withAlpha(0x120A1A, 0.82f));
        g.renderOutline(r.x(), r.y(), r.w(), r.h(), UiMath.withAlpha(PLUM, 0.55f));
        g.fill(r.x() + 6, r.y() - 3, r.x() + 7, r.y(), UiMath.withAlpha(PLUM, 0.55f));
        g.fill(r.x() + 7, r.y() - 2, r.x() + 8, r.y(), UiMath.withAlpha(PLUM, 0.55f));
        g.fill(r.x() + 8, r.y() - 1, r.x() + 9, r.y(), UiMath.withAlpha(PLUM, 0.55f));
    }
}
