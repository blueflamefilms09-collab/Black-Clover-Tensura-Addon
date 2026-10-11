package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/** Layered green mana cuts for Slash Magic: a travelling cut, a crossed impact and a reaping vortex. */
public class SlashLayer extends AbstractVfxLayer {
    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.SLASH_FX1, VfxShape.SLASH_FX2, VfxShape.SLASH_FX3); }
    @Override public int defaultDuration(VfxShape s) { return 30; }
    @Override public int defaultColor(VfxShape s) { return 0xFF6AFF8A; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = Mth.clamp(1f - age / Math.max(1f, inst.duration), 0f, 1f);
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(from);
        if (dir.lengthSquared() < 1.0e-4f) dir.set(0, 0, 1);
        dir.normalize();
        Vector3f side = new Vector3f(dir).cross(new Vector3f(0, 1, 0));
        if (side.lengthSquared() < 1.0e-4f) side.set(1, 0, 0);
        side.normalize();
        float size = Math.max(0.5f, inst.power);
        int green = VfxVertexBuffer.withAlpha(inst.color, fade);
        int core = VfxVertexBuffer.withAlpha(0xFFFFFFFF, fade * 0.9f);

        switch (inst.shape) {
            case SLASH_FX1 -> {
                float progress = Mth.clamp(age / Math.max(1f, inst.duration), 0f, 1f);
                Vector3f tip = new Vector3f(from).lerp(to, progress);
                Vector3f back = new Vector3f(tip).sub(new Vector3f(dir).mul(2.2f * size));
                buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, tip, back, 0.7f * size, 0.04f, 2, 0, green, green);
                buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, tip, back, 0.18f * size, 0.01f, 2, 0, core, core);
                for (int n = -1; n <= 1; n += 2) {
                    Vector3f a = new Vector3f(tip).add(new Vector3f(side).mul(n * 0.25f * size));
                    Vector3f b = new Vector3f(back).add(new Vector3f(side).mul(n * 0.65f * size));
                    buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, a, b, 0.12f * size, 0.01f, 2, 0.2f, green, green);
                }
            }
            case SLASH_FX2 -> {
                Vector3f centre = from.lerp(to, 0.5f);
                for (int n = 0; n < 3; n++) {
                    float angle = age * 0.17f + n * Mth.PI / 3f;
                    Vector3f axis = new Vector3f(side).mul(Mth.cos(angle)).add(0, Mth.sin(angle), 0).normalize();
                    Vector3f a = new Vector3f(centre).add(new Vector3f(axis).mul(size));
                    Vector3f b = new Vector3f(centre).sub(new Vector3f(axis).mul(size));
                    buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, a, b, 0.38f * size, 0.02f, 2, 0, green, green);
                }
                buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, centre, 0.8f * size, age * 0.1f, core);
            }
            case SLASH_FX3 -> {
                Vector3f centre = from.lerp(to, 0.5f);
                for (int n = 0; n < 8; n++) {
                    float angle = Mth.TWO_PI * n / 8f + age * 0.12f;
                    Vector3f radial = new Vector3f(Mth.cos(angle), 0, Mth.sin(angle));
                    Vector3f a = new Vector3f(centre).add(new Vector3f(radial).mul(0.25f * size));
                    Vector3f b = new Vector3f(centre).add(new Vector3f(radial).mul(1.4f * size)).add(0, 0.35f * size, 0);
                    buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, b, a, 0.32f * size, 0.01f, 2, angle, green, green);
                }
                buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, centre, 1.1f * size, age * 0.1f, core);
            }
            default -> { }
        }
    }
}
