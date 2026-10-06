package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/** Reusable element shapes: lightning spear, earth spikes, spatial rift, mirror pane, thread line. */
public class ElementShapesLayer extends AbstractVfxLayer {
    @Override public Set<VfxShape> shapes() {
        return EnumSet.of(VfxShape.LIGHTNING_SPEAR, VfxShape.EARTH_SPIKES, VfxShape.SPATIAL_RIFT, VfxShape.MIRROR_PANE, VfxShape.THREAD_LINE);
    }
    @Override public int defaultDuration(VfxShape s) { return 20; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case LIGHTNING_SPEAR -> lightning(inst, ctx, buf);
            case EARTH_SPIKES -> spikes(inst, ctx, buf);
            case SPATIAL_RIFT -> rift(inst, ctx, buf);
            case MIRROR_PANE -> pane(inst, ctx, buf);
            default -> thread(inst, ctx, buf);
        }
    }

    /** Jagged bolt; re-rolls its zigzag every 2 ticks. */
    private void lightning(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), alpha = 1 - VfxAnim.easeInCubic(t);
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        RandomSource r = RandomSource.create(inst.seed + inst.age() / 2);
        int segs = ctx.seg(8, 4);
        Vector3f prev = new Vector3f(a);
        float jitter = a.distance(b) * 0.06f;
        int col = inst.color, core = VfxVertexBuffer.whiten(col, 0.8f);
        for (int i = 1; i <= segs; i++) {
            Vector3f next = new Vector3f(b).sub(a).mul((float) i / segs).add(a);
            if (i < segs) next.add((r.nextFloat() - 0.5f) * jitter, (r.nextFloat() - 0.5f) * jitter, (r.nextFloat() - 0.5f) * jitter);
            buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, prev, next, 0.35f * inst.power, 0.35f * inst.power, 1, 0,
                    VfxVertexBuffer.withAlpha(col, alpha), VfxVertexBuffer.withAlpha(col, alpha));
            buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, prev, next, 0.12f * inst.power, 0.12f * inst.power, 1, 0,
                    VfxVertexBuffer.withAlpha(core, alpha), VfxVertexBuffer.withAlpha(core, alpha));
            prev = next;
        }
        VfxBloom.glow(ctx, buf, b, 0.7f * inst.power, col, alpha);
    }

    /** Spikes erupt one after another along the line, then sink. */
    private void spikes(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick);
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        int n = ctx.seg(10, 5);
        int col = inst.color;
        RandomSource r = inst.random();
        for (int i = 0; i < n; i++) {
            float at = (float) i / Math.max(1, n - 1);
            float local = Mth.clamp(t * 2.2f - at, 0, 1);
            if (local <= 0) continue;
            float rise = local < 0.5f ? VfxAnim.easeOutBack(local * 2) : 1 - (local - 0.5f) * 2;
            Vector3f base = new Vector3f(b).sub(a).mul(at).add(a).add((r.nextFloat() - 0.5f) * 0.6f, 0, (r.nextFloat() - 0.5f) * 0.6f);
            Vector3f tip = new Vector3f(base).add(0, (1.2f + r.nextFloat()) * inst.power * rise, 0);
            buf.beam(ctx, VfxTextures.SHARD, VfxBlend.ALPHA, base, tip, 0.7f * inst.power, 0.05f, 1, 0,
                    VfxVertexBuffer.withAlpha(col, 1), VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(col, 0.4f), 1));
        }
        VfxBloom.glow(ctx, buf, new Vector3f(a).lerp(b, Math.min(1, t * 2)), 0.6f * inst.power, col, 1 - t);
    }

    /** A spinning rift disc with a rune band; lasts the payload duration. */
    private void rift(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick);
        Vec3 from = inst.from(ctx), to = inst.to(ctx), d = to.subtract(from);
        VfxPose pose = VfxPose.facing(ctx.rel(from.add(0, 1, 0)), d.lengthSqr() < 0.01 ? new Vector3f(0, 1, 0) : new Vector3f((float) d.x, (float) d.y, (float) d.z));
        float alpha = VfxAnim.fadeInOut(t, 0.08f, 0.15f), r = 1.1f * inst.power * VfxAnim.circleOpen(t);
        int col = inst.color;
        buf.plane(VfxTextures.GLOW, VfxBlend.NEGATIVE, pose, r * 0.8f, VfxVertexBuffer.withAlpha(0xFF404040, alpha));
        buf.plane(VfxTextures.MAGIC_CIRCLE, VfxBlend.ADD, pose.spin(age * 0.15f), r, VfxVertexBuffer.withAlpha(col, alpha));
        VfxPart.runeBand(r * 1.1f, r * 0.18f, 24, col).draw(ctx, buf, pose, age * 3, alpha, 1);
        VfxBloom.glow(ctx, buf, pose.origin(), r * 0.6f, col, alpha);
    }

    /** A glowing pane at 'from' facing 'to'. */
    private void pane(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick);
        Vec3 from = inst.from(ctx), to = inst.to(ctx), d = to.subtract(from);
        VfxPose pose = VfxPose.facing(ctx.rel(from), d.lengthSqr() < 0.01 ? new Vector3f(0, 0, 1) : new Vector3f((float) d.x, (float) d.y, (float) d.z));
        float alpha = VfxAnim.fadeInOut(t, 0.1f, 0.2f) * (0.8f + 0.2f * VfxAnim.pulse(age, 2)), s = 1.2f * inst.power;
        int col = inst.color;
        buf.plane(VfxTextures.GLOW, VfxBlend.ADD, pose, s * 1.2f, VfxVertexBuffer.withAlpha(col, alpha * 0.4f));
        buf.ring(VfxTextures.GLOW, VfxBlend.ADD, pose, s * 0.92f, s, 4, 1, 0, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(col, 0.5f), alpha));
        buf.plane(VfxTextures.SPARK, VfxBlend.ADD, pose.spin(age * 0.05f), s * 0.5f, VfxVertexBuffer.withAlpha(0xFFFFFFFF, alpha * 0.5f));
    }

    /** A thin glowing thread with a bright core. */
    private void thread(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), alpha = VfxAnim.fadeInOut(t, 0.05f, 0.4f);
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        int col = inst.color;
        buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, a, b, 0.18f * inst.power, 0.18f * inst.power, 3, 0,
                VfxVertexBuffer.withAlpha(col, alpha), VfxVertexBuffer.withAlpha(col, alpha));
        buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, a, b, 0.05f * inst.power, 0.05f * inst.power, 3, 0,
                VfxVertexBuffer.withAlpha(0xFFFFFFFF, alpha), VfxVertexBuffer.withAlpha(0xFFFFFFFF, alpha));
        VfxBloom.glow(ctx, buf, b, 0.4f * inst.power, col, alpha);
    }
}
