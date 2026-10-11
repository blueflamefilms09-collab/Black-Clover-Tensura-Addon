package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.AbstractVfxLayer;
import com.newuniverse.nusmp.vfx.client.VfxBlend;
import com.newuniverse.nusmp.vfx.client.VfxInstance;
import com.newuniverse.nusmp.vfx.client.VfxRenderContext;
import com.newuniverse.nusmp.vfx.client.VfxTextures;
import com.newuniverse.nusmp.vfx.client.VfxVertexBuffer;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/** Distinct layered effects for Star, Sand, and Mist Magic. */
public final class ElementIdentityLayer extends AbstractVfxLayer {
    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.STAR_MAGIC, VfxShape.SAND_MAGIC, VfxShape.MIST_MAGIC); }
    @Override public int defaultDuration(VfxShape shape) { return 24; }
    @Override public int defaultColor(VfxShape shape) {
        return switch (shape) {
            case STAR_MAGIC -> 0xFFFFE8A0;
            case SAND_MAGIC -> 0xFFD8B878;
            case MIST_MAGIC -> 0xFFC8D2E0;
            default -> 0xFFFFFFFF;
        };
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = Mth.clamp(1f - age / Math.max(1f, inst.duration), 0f, 1f);
        float size = Math.max(0.35f, inst.power);
        Vector3f from = ctx.rel(inst.from(ctx));
        Vector3f to = ctx.rel(inst.to(ctx));
        int tint = VfxVertexBuffer.withAlpha(inst.color, fade);

        switch (inst.shape) {
            case STAR_MAGIC -> {
                Vector3f tip = new Vector3f(from).lerp(to, Mth.clamp(age / Math.max(1f, inst.duration), 0f, 1f));
                buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, tip, from, 0.16f * size, 0.01f, 2, age * 0.08f, tint, tint);
                int count = ctx.seg(12, 5);
                Vector3f side = new Vector3f(ctx.camRight), up = new Vector3f(ctx.camUp);
                for (int n = 0; n < count; n++) {
                    float t = (n + 0.5f) / count;
                    Vector3f centre = new Vector3f(from).lerp(to, t);
                    float angle = age * 0.17f + n * 2.4f;
                    centre.add(new Vector3f(side).mul(Mth.cos(angle) * 0.55f * size));
                    centre.add(new Vector3f(up).mul(Mth.sin(angle) * 0.35f * size));
                    float glint = 0.45f + 0.55f * (0.5f + 0.5f * Mth.sin(age * 0.35f + n));
                    buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, centre, 0.45f * size, angle, VfxVertexBuffer.withAlpha(0xFFFFFFFF, fade * glint));
                    buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, centre, 0.7f * size, -angle, VfxVertexBuffer.withAlpha(inst.color, fade * glint));
                }
            }
            case SAND_MAGIC -> {
                int count = ctx.seg(14, 6);
                Vector3f side = new Vector3f(ctx.camRight), up = new Vector3f(ctx.camUp);
                for (int n = 0; n < count; n++) {
                    float t = (n + 0.5f) / count;
                    float angle = age * 0.21f + n * 2.399f;
                    float radius = (0.25f + 0.65f * Mth.sin(t * Mth.PI)) * size;
                    Vector3f centre = new Vector3f(from).lerp(to, t)
                            .add(new Vector3f(side).mul(Mth.cos(angle) * radius))
                            .add(new Vector3f(up).mul(Mth.sin(angle) * radius * 0.45f));
                    buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ALPHA, centre, 0.7f * size, angle, VfxVertexBuffer.withAlpha(inst.color, fade * 0.42f));
                    buf.billboard(ctx, VfxTextures.SHARD, VfxBlend.ADD, centre, 0.28f * size, angle * 1.7f, VfxVertexBuffer.withAlpha(0xFFFFE4A1, fade * 0.72f));
                }
                buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ALPHA, to, from, 0.32f * size, 0.08f, 2, age * 0.12f, VfxVertexBuffer.withAlpha(inst.color, fade * 0.35f), tint);
            }
            case MIST_MAGIC -> {
                int count = ctx.seg(10, 5);
                Vector3f side = new Vector3f(ctx.camRight), up = new Vector3f(ctx.camUp);
                for (int n = 0; n < count; n++) {
                    float t = (n + 0.5f) / count;
                    float angle = age * 0.075f + n * 2.399f;
                    Vector3f centre = new Vector3f(from).lerp(to, t)
                            .add(new Vector3f(side).mul(Mth.cos(angle) * size * 0.55f))
                            .add(new Vector3f(up).mul(Mth.sin(angle) * size * 0.42f));
                    float pulse = 0.7f + 0.3f * Mth.sin(age * 0.11f + n);
                    buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ALPHA, centre, 1.15f * size, angle, VfxVertexBuffer.withAlpha(inst.color, fade * pulse * 0.32f));
                    buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, centre, 0.38f * size, -angle, VfxVertexBuffer.withAlpha(0xFFEAF4FF, fade * pulse * 0.35f));
                }
                buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ALPHA, to, from, 0.5f * size, 0.18f * size, 2, 0,
                        VfxVertexBuffer.withAlpha(inst.color, fade * 0.2f), VfxVertexBuffer.withAlpha(inst.color, fade * 0.2f));
            }
            default -> { }
        }
    }
}
