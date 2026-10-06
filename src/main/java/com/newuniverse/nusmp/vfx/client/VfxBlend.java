package com.newuniverse.nusmp.vfx.client;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

/**
 * Blend modes. GL only has blend FACTORS, so the elemental modes (FIRE/WATER/WIND) are a
 * GL blend plus a color grade applied to the vertex color in {@link #grade(int, float)}.
 * Draw order: ALPHA -> WATER -> NEGATIVE -> ADD/FIRE/WIND (glows always land on top).
 */
public enum VfxBlend {
    ALPHA(0), WATER(1), NEGATIVE(2), ADD(3), FIRE(3), WIND(3);

    /** Lower draws first. */
    public final int order;

    VfxBlend(int order) { this.order = order; }

    public void apply() {
        RenderSystem.enableBlend();
        switch (this) {
            case ALPHA, WATER -> RenderSystem.defaultBlendFunc();
            // Inverts whatever is behind it -> the "anti-magic" look.
            case NEGATIVE -> RenderSystem.blendFuncSeparate(GlStateManager.SourceFactor.ONE_MINUS_DST_COLOR,
                    GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR, GlStateManager.SourceFactor.ONE, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
            default -> RenderSystem.blendFunc(GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE);
        }
    }

    /**
     * Color grade per mode. heat (0..1) = how close to the "core" this vertex is.
     * FIRE pushes the core toward white-yellow, WATER deepens edges, WIND softens alpha.
     */
    public int grade(int argb, float heat) {
        int a = argb >>> 24, r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255;
        switch (this) {
            case FIRE -> { r = lerp(r, 255, heat * 0.8f); g = lerp(g, 235, heat * 0.7f); b = lerp(b, 160, heat * 0.5f); }
            case WATER -> { r = lerp(r, r / 2, 1 - heat); g = lerp(g, 255, heat * 0.3f); b = lerp(b, 255, heat * 0.5f); }
            case WIND -> a = (int) (a * 0.75f);
            default -> { }
        }
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static int lerp(int a, int b, float t) { return Math.max(0, Math.min(255, (int) (a + (b - a) * t))); }
}
