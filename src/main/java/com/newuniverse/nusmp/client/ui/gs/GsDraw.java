package com.newuniverse.nusmp.client.ui.gs;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.newuniverse.nusmp.client.ui.theme.UiMath;
import com.newuniverse.nusmp.client.ui.theme.UiThemeOptions;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * The drawing kit of the Great Spirit look: the palette of the two skins, the filigree corners, the clover frames, the slime ripple and the
 * alert crest. Static, allocation-free (no object, no string, no location is built in a draw call), every effect has a hard size:
 * a panel gets at most 4 corner quads, a clover frame at most 3 quads (frame, glow, drips), a ripple at most 3 ring quads, the crest 2 quads.
 * Only for the great_spirit theme: callers check {@code UiTheme.greatSpirit()}.
 */
public final class GsDraw {
    private GsDraw() {}

    /** Palette (RGB) per skin, indexed by {@link GsSkin#ordinal()}: B = Parchment and Ink, C = Slime Crest. */
    public static final int[] NEON = {0xDEB252, 0xA65AFF};
    public static final int[] NEON2 = {0xFFDC8C, 0xFF50D6};
    public static final int[] TEXT = {0xF6E8C8, 0xECE2F8};
    public static final int[] DIM = {0xC4A878, 0x9E82B8};
    /** Ink on a skin-B page, and the colour a compiled line settles into; for skin C (dark glass pages) light violet tones. */
    public static final int[] INK = {0x3A2616, 0xCCB0EC};
    public static final int[] SETTLED = {0x1F4E7A, 0xEBDDFF};
    public static final int[] DATA = {0x28AAFF, 0xFF6EE6};
    public static final int[] TITLE = {0x6A3A10, 0xFF8EEA};
    public static final int[] PANEL_TOP = {0x34210F, 0x16092A}, PANEL_BOTTOM = {0x22150A, 0x09040F};
    public static final int[] OK = {0x6FE08A, 0x9BFF72}, WARN = {0xFFB04A, 0xFFA050}, BAD = {0xF0605A, 0xFF5078};

    private static final int CYAN = 0x5CD8FF, VIOLET = 0x9A7CFF;

    /** The anime palette's cyan and violet accents become the skin's two neon colours; everything else (and the alpha byte) stays. */
    public static int tint(int argb, GsSkin skin) {
        int rgb = argb & 0xFFFFFF;
        if (rgb == CYAN) return (argb & 0xFF000000) | NEON[skin.ordinal()];
        if (rgb == VIOLET) return (argb & 0xFF000000) | NEON2[skin.ordinal()];
        return argb;
    }

    // ---------------------------------------------------------------- filigree corners
    /** Corner size in GUI pixels (the 96 px texture is shown at half size and filtered). */
    public static final int CORNER = 30;
    /** Panels smaller than this get no corners (buttons, chips, slots). */
    public static final int CORNER_MIN_W = 96, CORNER_MIN_H = 64;

    /** Four filigree corners on the corners of the rectangle: 4 quads. */
    public static void corners(GuiGraphics g, int x, int y, int w, int h, GsSkin skin, float alpha) {
        if (w < CORNER_MIN_W || h < CORNER_MIN_H || alpha <= 0.02f) return;
        ResourceLocation t = GsTex.CORNER[skin.ordinal()];
        int s = CORNER, o = 2;
        g.setColor(1f, 1f, 1f, Math.min(1f, alpha));
        g.blit(t, x - o, y - o, s, s, 0f, 0f, 96, 96, 96, 96);
        g.blit(t, x + w + o - s, y - o, s, s, 96f, 0f, -96, 96, 96, 96);
        g.blit(t, x - o, y + h + o - s, s, s, 0f, 96f, 96, -96, 96, 96);
        g.blit(t, x + w + o - s, y + h + o - s, s, s, 96f, 96f, -96, -96, 96, 96);
        g.setColor(1f, 1f, 1f, 1f);
    }

    // ---------------------------------------------------------------- clover frames
    /**
     * A clover frame of 'size' GUI pixels centred on (cx, cy): the frame, for four and five leaves an additive glow (breathing when
     * 'animate'), for five leaves the swaying drips. The caller draws the icon (the old 24 x 24 picture) first, inside the frame's round hole.
     * The five-leaf frame always wears skin C. 'phase' offsets the breathing per icon. At most 3 quads.
     */
    public static void clover(GuiGraphics g, int cx, int cy, int size, int leaves, GsSkin userSkin, float time, boolean animate, int phase) {
        int n = leaves >= 5 ? 5 : leaves == 4 ? 4 : 3;
        GsSkin skin = GsClover.skinFor(n, userSkin);
        int s = skin.ordinal();
        int x = cx - size / 2, y = cy - size / 2;
        g.blit(GsTex.CLOVER[s][n - 3], x, y, size, size, 0f, 0f, 96, 96, 96, 96);
        if (n >= 4 && UiThemeOptions.highQuality()) {
            float k = animate ? 0.35f + 0.65f * GsClover.pulse(time, phase) : 0.6f;
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            g.setColor(1f, 1f, 1f, k);
            g.blit(GsTex.GLOW[s][n - 4], x, y, size, size, 0f, 0f, 96, 96, 96, 96);
            g.setColor(1f, 1f, 1f, 1f);
            RenderSystem.defaultBlendFunc();
        }
        if (n == 5) {
            float sway = animate ? (float) Math.sin(time * 0.08f + phase) * 0.8f : 0f;
            var pose = g.pose();
            pose.pushPose();
            pose.translate(sway, 0f, 0f);
            g.blit(GsTex.DRIPS[s], x, y, size, size, 0f, 0f, 96, 96, 96, 96);
            pose.popPose();
        }
    }

    // ---------------------------------------------------------------- slime ripple
    /**
     * The hover ripple of a button (x, y, w, h): up to three expanding rings from the point the pointer entered ('px', 'py'), clipped to the
     * button. 'enteredAt' is the clock value of the entry. Nothing is drawn once the last ring ended or on quality LOW. At most 3 quads.
     */
    public static void ripple(GuiGraphics g, int x, int y, int w, int h, float px, float py, float now, float enteredAt, GsSkin skin) {
        if (!GsRipple.anyAlive(now, enteredAt) || !UiThemeOptions.highQuality()) return;
        ResourceLocation t = GsTex.RIPPLE[skin.ordinal()];
        g.enableScissor(x, y, x + w, y + h);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        for (int i = 0; i < GsRipple.MAX_RINGS; i++) {
            float age = GsRipple.age(now, enteredAt, i);
            if (!GsRipple.alive(age)) continue;
            int r = Math.max(2, Math.round(GsRipple.radius(age)));
            g.setColor(1f, 1f, 1f, GsRipple.alpha(age) * 0.85f);
            g.blit(t, Math.round(px) - r, Math.round(py) - r, r * 2, r * 2, 0f, 0f, 64, 64, 64, 64);
        }
        g.setColor(1f, 1f, 1f, 1f);
        RenderSystem.defaultBlendFunc();
        g.disableScissor();
    }

    // ---------------------------------------------------------------- the alert crest
    public static final int CREST = 32;

    /** The crest (2 quads: the seal and, on quality HIGH, a slowly breathing additive copy). 'ping' 0..1 brightens it when a notice arrives. */
    public static void crest(GuiGraphics g, int x, int y, GsSkin skin, float alpha, float time, float ping) {
        if (alpha <= 0.02f) return;
        ResourceLocation t = GsTex.CREST[skin.ordinal()];
        g.setColor(1f, 1f, 1f, Math.min(1f, alpha));
        g.blit(t, x, y, CREST, CREST, 0f, 0f, 96, 96, 96, 96);
        if (UiThemeOptions.highQuality()) {
            float k = (0.10f + 0.12f * UiMath.pulse(time, 70f, 0f) + 0.55f * ping) * alpha;
            RenderSystem.enableBlend();
            RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
            g.setColor(1f, 1f, 1f, Math.min(1f, k));
            g.blit(t, x, y, CREST, CREST, 0f, 0f, 96, 96, 96, 96);
            RenderSystem.defaultBlendFunc();
        }
        g.setColor(1f, 1f, 1f, 1f);
    }
}
