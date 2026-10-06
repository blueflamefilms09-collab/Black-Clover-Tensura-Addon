package com.newuniverse.nusmp.vfx.client;

import org.joml.Vector3f;

/**
 * Bloom glow. Real screen-space bloom needs an extra framebuffer + blur shader pass; this uses
 * the cheaper "stacked halo" technique: two additive soft-glow billboards (tight + wide) behind
 * every bright point. Costs 8 vertices per call, so it fits the 400-vertex budget.
 */
public final class VfxBloom {
    private VfxBloom() {}

    /** Glow at a point. size = core size in blocks, intensity 0..1+. */
    public static void glow(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f center, float size, int argb, float intensity) {
        if (intensity <= 0.01f) return;
        buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, center, size * 1.6f, 0,
                VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(argb, 0.35f), 0.55f * intensity));
        buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, center, size * 3.2f, 0,
                VfxVertexBuffer.withAlpha(argb, 0.25f * intensity));
    }

    /** Glow lying flat under a magic circle (lights the "floor" of the effect). */
    public static void planeGlow(VfxVertexBuffer buf, VfxPose pose, float radius, int argb, float intensity) {
        buf.plane(VfxTextures.GLOW, VfxBlend.ADD, pose, radius * 1.5f, VfxVertexBuffer.withAlpha(argb, 0.45f * intensity));
    }
}
