package com.newuniverse.nusmp.client.avatar;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.newuniverse.nusmp.avatar.menu.ForgeLayout.Rect;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Soul Forge screen is built from a handful of shapes (a round medallion with an icon in it, a ring that glows when hovered, a soft halo, a button, a
 * bar) drawn many times in many sizes. Drawing them is the same few lines of {@code GuiGraphics} and {@code RenderSystem} every time, and a wrong blend state
 * or a colour left set would tint the rest of the screen, so those lines live here once: the three white sprites of the kit ({@code ring.png}, {@code disc.png},
 * {@code slot.png}) are tinted at draw time, every function restores the colour and blend function it changed, and text helpers cut a string to a width without
 * ever drawing past it. Client only, no state.
 */
final class ForgeUi {
    private ForgeUi() {}

    static final ResourceLocation RING = gui("ring"), DISC = gui("disc"), SLOT = gui("slot");
    private static final String[] CATEGORY = {"cat_identity", "cat_appearance", "cat_traits", "cat_skills", "cat_aptitudes"};

    static ResourceLocation gui(String name) { return ResourceLocation.fromNamespaceAndPath("nusmp", "textures/gui/soulforge/" + name + ".png"); }

    static ResourceLocation category(int page) { return gui(CATEGORY[Math.max(0, Math.min(CATEGORY.length - 1, page))]); }

    static int argb(int rgb, float alpha) { return UiMath.withAlpha(rgb & 0xFFFFFF, alpha); }

    // ================================================================ sprites
    /** Draws one of the kit's sprites into a box with a tint (0xRRGGBB) and alpha; the sprites are white so the tint is the colour. */
    static void sprite(GuiGraphics g, ResourceLocation tex, int x, int y, int w, int h, int texSize, int rgb, float alpha) {
        if (alpha <= 0.003f || w <= 0 || h <= 0) return;
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        g.setColor(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, Math.min(1f, alpha));
        g.blit(tex, x, y, w, h, 0f, 0f, texSize, texSize, texSize, texSize);
        g.setColor(1f, 1f, 1f, 1f);
    }

    static void ring(GuiGraphics g, Rect r, int rgb, float alpha) { sprite(g, RING, r.x(), r.y(), r.w(), r.h(), 64, rgb, alpha); }

    static void disc(GuiGraphics g, Rect r, int rgb, float alpha) { sprite(g, DISC, r.x(), r.y(), r.w(), r.h(), 64, rgb, alpha); }

    static void slotRing(GuiGraphics g, Rect r, int rgb, float alpha) { sprite(g, SLOT, r.x(), r.y(), r.w(), r.h(), 64, rgb, alpha); }

    /** A soft light that ADDS to what is behind it (a halo behind an icon, the glow of a chosen slot). {@code radius} is in GUI pixels. */
    static void glow(GuiGraphics g, float cx, float cy, float radius, int rgb, float alpha) {
        if (alpha <= 0.01f || radius < 2f) return;
        int r = Math.round(radius);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        g.setColor(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f, Math.min(1f, alpha));
        g.blit(DISC, Math.round(cx) - r, Math.round(cy) - r, r * 2, r * 2, 0f, 0f, 64, 64, 64, 64);
        g.setColor(1f, 1f, 1f, 1f);
        RenderSystem.defaultBlendFunc();
    }

    /**
     * A round medallion: a dark disc, the icon (or, when there is none, the first letter of the name in the tint), a frame ring in {@code frameRgb} that grows
     * brighter with {@code hover} and with {@code selected}, and a halo while selected. {@code dim} greys the whole thing (a skill the creator cannot carry).
     */
    static void medallion(GuiGraphics g, Font font, Rect r, ResourceLocation icon, String name, int tint, int frameRgb, float hover, boolean selected, boolean dim) {
        float k = dim ? 0.38f : 1f;
        if (selected) glow(g, r.cx(), r.cy(), r.w() * 0.78f, frameRgb, 0.34f);
        else if (hover > 0.02f) glow(g, r.cx(), r.cy(), r.w() * 0.7f, frameRgb, 0.20f * hover);
        disc(g, r, 0x05080F, 0.92f * (dim ? 0.8f : 1f));
        int pad = Math.max(2, Math.round(r.w() * 0.14f));
        int inner = Math.max(4, r.w() - 2 * pad);
        boolean drawn = icon != null && ForgeIcons.draw(g, icon, r.x() + pad, r.y() + pad, inner, 0xFFFFFF, k);
        if (!drawn) {
            String letter = name == null || name.isEmpty() ? "?" : name.substring(0, 1).toUpperCase(java.util.Locale.ROOT);
            g.drawCenteredString(font, letter, r.cx(), r.cy() - font.lineHeight / 2 + 1, argb(tint, Math.max(0.2f, k)));
        }
        float ringA = selected ? 1f : 0.55f + 0.4f * hover;
        ring(g, r, frameRgb, ringA * (dim ? 0.55f : 1f));
    }

    // ================================================================ text
    /** "anti_magic" -> "Anti Magic": a readable form of a snake_case id. */
    static String titleCase(String snake) {
        if (snake == null || snake.isEmpty()) return "";
        StringBuilder out = new StringBuilder();
        for (String part : snake.split("_")) {
            if (part.isEmpty()) continue;
            if (out.length() > 0) out.append(' ');
            out.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return out.toString();
    }

    /** The text cut to fit {@code width} pixels, with an ellipsis when something was cut. */
    static String fit(Font font, String text, int width) {
        if (text == null) return "";
        if (width <= 0) return "";
        if (font.width(text) <= width) return text;
        int ell = font.width("...");
        if (width <= ell) return font.plainSubstrByWidth(text, width);
        return font.plainSubstrByWidth(text, width - ell) + "...";
    }

    static void text(GuiGraphics g, Font font, String s, int x, int y, int rgb) { g.drawString(font, s, x, y, 0xFF000000 | (rgb & 0xFFFFFF), false); }

    static void textShadow(GuiGraphics g, Font font, String s, int x, int y, int rgb) { g.drawString(font, s, x, y, 0xFF000000 | (rgb & 0xFFFFFF), true); }

    static void centered(GuiGraphics g, Font font, String s, int cx, int y, int rgb) { g.drawString(font, s, cx - font.width(s) / 2, y, 0xFF000000 | (rgb & 0xFFFFFF), false); }

    /** Draws {@code text} word-wrapped in a box of {@code width}, at most {@code maxLines} lines (the last one gets an ellipsis if text remains); returns the y below it. */
    static int wrapped(GuiGraphics g, Font font, String text, int x, int y, int width, int rgb, int maxLines) {
        List<String> lines = wrapLines(font, text, width, maxLines);
        for (String l : lines) {
            text(g, font, l, x, y, rgb);
            y += font.lineHeight + 1;
        }
        return y;
    }

    /** The lines a text breaks into at {@code width} pixels (words are not split unless a single word is wider than the box). */
    static List<String> wrapLines(Font font, String text, int width, int maxLines) {
        List<String> out = new ArrayList<>();
        if (text == null || text.isEmpty() || width < 8 || maxLines <= 0) return out;
        String[] words = text.split(" ");
        StringBuilder cur = new StringBuilder();
        for (int i = 0; i < words.length; i++) {
            String w = words[i];
            String trial = cur.length() == 0 ? w : cur + " " + w;
            if (font.width(trial) <= width) { cur = new StringBuilder(trial); continue; }
            if (cur.length() > 0) {
                out.add(cur.toString());
                cur = new StringBuilder();
                if (out.size() >= maxLines) { return ellipsisOnLast(font, out, words, i, width); }
            }
            if (font.width(w) > width) w = fit(font, w, width);
            cur.append(w);
        }
        if (cur.length() > 0 && out.size() < maxLines) out.add(cur.toString());
        return out;
    }

    private static List<String> ellipsisOnLast(Font font, List<String> out, String[] words, int from, int width) {
        int last = out.size() - 1;
        out.set(last, fit(font, out.get(last) + " " + words[from] + "...", width));
        return out;
    }

    // ================================================================ buttons and bars
    /** A flat button in the glass look: dark body, a coloured edge that lights with {@code hover}, centred label; {@code enabled=false} greys it. */
    static void button(GuiGraphics g, Font font, Rect r, String label, boolean enabled, float hover, int rgb) {
        int a = enabled ? rgb : 0x5C6E88;
        g.fill(r.x(), r.y(), r.right(), r.bottom(), argb(0x050810, 0.82f));
        g.fillGradient(r.x(), r.y(), r.right(), r.bottom(), argb(a, 0.10f + 0.22f * hover), argb(a, 0.03f + 0.10f * hover));
        g.renderOutline(r.x(), r.y(), r.w(), r.h(), argb(a, enabled ? 0.45f + 0.5f * hover : 0.35f));
        if (enabled && hover > 0.05f) g.fill(r.x() + 1, r.bottom() - 2, r.right() - 1, r.bottom() - 1, argb(a, 0.5f * hover));
        String t = fit(font, label, r.w() - 6);
        g.drawString(font, t, r.cx() - font.width(t) / 2, r.cy() - font.lineHeight / 2 + 1, 0xFF000000 | (enabled ? (hover > 0.5f ? 0xFFFFFF : 0xE6F2FF) : 0x5C6E88), false);
    }

    /** A small selectable tag (a personality, a sub-tab): filled and bright when selected. */
    static void chip(GuiGraphics g, Font font, Rect r, String label, boolean selected, float hover, int rgb) {
        float k = selected ? 1f : 0.25f + 0.6f * hover;
        g.fillGradient(r.x(), r.y(), r.right(), r.bottom(), argb(rgb, 0.10f + 0.30f * k), argb(rgb, 0.04f + 0.14f * k));
        g.renderOutline(r.x(), r.y(), r.w(), r.h(), argb(rgb, 0.30f + 0.65f * k));
        String t = fit(font, label, r.w() - 4);
        g.drawString(font, t, r.cx() - font.width(t) / 2, r.cy() - font.lineHeight / 2 + 1, 0xFF000000 | (selected ? 0xFFFFFF : 0xB8C8E0), false);
    }

    /** A thin scroll bar for a grid or list: the track, and the thumb sized by how much of the content is visible. */
    static void scrollbar(GuiGraphics g, Rect track, int scroll, int maxScroll, int visibleRows, int totalRows, int rgb) {
        if (maxScroll <= 0 || totalRows <= 0) return;
        g.fill(track.x(), track.y(), track.right(), track.bottom(), argb(0x000000, 0.35f));
        int thumbH = Math.max(8, Math.round(track.h() * (visibleRows / (float) totalRows)));
        int travel = Math.max(0, track.h() - thumbH);
        int ty = track.y() + Math.round(travel * (scroll / (float) maxScroll));
        g.fill(track.x(), ty, track.right(), ty + thumbH, argb(rgb, 0.75f));
    }

    /** A horizontal gauge: a dark track, {@code frac} of it filled, and an optional second mark (the cap) drawn as a bright line at {@code mark} (0..1, negative = none). */
    static void gauge(GuiGraphics g, Rect r, float frac, float mark, int rgb) {
        g.fill(r.x(), r.y(), r.right(), r.bottom(), argb(0x03050A, 0.8f));
        g.renderOutline(r.x() - 1, r.y() - 1, r.w() + 2, r.h() + 2, argb(rgb, 0.4f));
        int fw = Math.round(r.w() * Math.max(0f, Math.min(1f, frac)));
        if (fw > 0) g.fillGradient(r.x(), r.y(), r.x() + fw, r.bottom(), argb(rgb, 0.95f), argb(UiMath.mix(0xFF000000 | rgb, 0xFF000000, 0.45f), 0.95f));
        if (mark >= 0f) {
            int mx = r.x() + Math.round(r.w() * Math.min(1f, mark));
            g.fill(mx, r.y() - 2, mx + 1, r.bottom() + 2, argb(0xFFFFFF, 0.9f));
        }
    }
}
