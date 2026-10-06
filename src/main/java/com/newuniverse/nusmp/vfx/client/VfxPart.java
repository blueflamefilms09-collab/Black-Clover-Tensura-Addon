package com.newuniverse.nusmp.vfx.client;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Reusable building blocks that layers combine. Each part draws itself into the buffer
 * for a given pose / time. Add new parts here and any layer can use them.
 */
public interface VfxPart {
    void draw(VfxRenderContext ctx, VfxVertexBuffer buf, VfxPose pose, float age, float alpha, float scale);

    /** Spinning textured circle + a counter-rotating copy for depth (2 quads). */
    static VfxPart circle(ResourceLocation tex, VfxBlend blend, float radius, float spin, int color) {
        return (ctx, buf, pose, age, alpha, scale) -> {
            float r = radius * scale;
            buf.plane(tex, blend, pose.spin(VfxAnim.magicSpin(age, spin)), r, VfxVertexBuffer.withAlpha(color, alpha));
            buf.plane(tex, blend, pose.lift(0.02f).spin(-VfxAnim.magicSpin(age, spin * 0.6f)), r * 0.72f,
                    VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(color, 0.3f), alpha * 0.8f));
        };
    }

    /** Rune band crawling around the circle. */
    static VfxPart runeBand(float radius, float width, int segments, int color) {
        return (ctx, buf, pose, age, alpha, scale) -> {
            int seg = ctx.seg(segments, 8);
            buf.ring(VfxTextures.RUNE_RING, VfxBlend.ADD, pose.lift(0.04f), (radius - width / 2) * scale, (radius + width / 2) * scale,
                    seg, 6f, age * 0.01f, VfxVertexBuffer.withAlpha(color, alpha));
        };
    }

    /** Glowing orbs orbiting the circle edge (danmaku-style). */
    static VfxPart orbiters(int count, float radius, float speed, float size, int color) {
        return (ctx, buf, pose, age, alpha, scale) -> {
            int n = ctx.seg(count, 3);
            for (int i = 0; i < n; i++) {
                float a = VfxAnim.runeOrbit(i, n, age, speed);
                Vector3f p = pose.lift(0.15f).point(Mth.cos(a) * radius * scale, Mth.sin(a) * radius * scale);
                buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, p, size * scale, age * 0.1f, VfxVertexBuffer.withAlpha(color, alpha));
            }
        };
    }

    /** Soft light pooled on the plane + bloom at the center. */
    static VfxPart glow(float radius, int color) {
        return (ctx, buf, pose, age, alpha, scale) -> {
            VfxBloom.planeGlow(buf, pose, radius * scale, color, alpha);
            VfxBloom.glow(ctx, buf, pose.lift(0.1f).origin(), radius * 0.35f * scale, color, alpha * 0.8f);
        };
    }
}
