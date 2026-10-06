package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Grimoire magic circles.
 * MAGIC_CIRCLE: opens with overshoot, spins, rune band crawls, orbs orbit, fades out.
 * MAGIC_CIRCLE_EXPLOSION: same circle charges (spins faster, brightens) then shatters into a
 * radial burst + flash + expanding shock ring + danmaku spark spray, with screen shake.
 */
public class MagicCircleLayer extends AbstractVfxLayer {
    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.MAGIC_CIRCLE, VfxShape.MAGIC_CIRCLE_EXPLOSION); }
    @Override public int defaultDuration(VfxShape s) { return s == VfxShape.MAGIC_CIRCLE ? 40 : 34; }
    @Override public int defaultColor(VfxShape s) { return 0xFFFFD86B; }

    @Override
    public void onTick(VfxInstance inst) {
        // Shake at the moment of the burst (60% through the effect).
        if (inst.shape == VfxShape.MAGIC_CIRCLE_EXPLOSION && inst.power >= 1.5f && inst.age() == (int) (inst.duration * 0.6f)) {
            VfxShake.add(inst.payload.to(), 1.2f * inst.power, 10);
        }
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick);
        Vec3 from = inst.from(ctx), to = inst.to(ctx);
        Vec3 dir = to.subtract(from);
        Vector3f facing = dir.lengthSqr() < 0.01 ? new Vector3f(0, 1, 0) : new Vector3f((float) dir.x, (float) dir.y, (float) dir.z);
        VfxPose pose = VfxPose.facing(ctx.rel(from), facing);
        float radius = 1.6f * inst.power;
        int col = inst.color;

        if (inst.shape == VfxShape.MAGIC_CIRCLE) {
            float alpha = VfxAnim.fadeInOut(t, 0.12f, 0.25f), scale = VfxAnim.circleOpen(t);
            drawCircle(ctx, buf, pose, age, alpha, scale, radius, col, 1f);
            return;
        }

        // ---- explosion variant ----
        float burstAt = 0.6f;
        if (t < burstAt) {
            float charge = t / burstAt;
            float scale = VfxAnim.circleOpen(charge) * (1 + 0.1f * charge);
            drawCircle(ctx, buf, pose, age * (1 + charge * 2), Math.min(1, charge * 3), scale, radius,
                    VfxVertexBuffer.whiten(col, charge * 0.5f), 1 + charge * 2);
            return;
        }
        float b = (t - burstAt) / (1 - burstAt);   // 0..1 after the burst
        float fade = 1 - b;
        Vector3f center = pose.lift(0.2f).origin();
        // Flash
        VfxBloom.glow(ctx, buf, center, radius * 1.4f * (1 + b), VfxVertexBuffer.whiten(col, 0.6f), fade * 1.5f);
        // Radial burst texture (camera-facing + flat)
        float bs = VfxAnim.explosionBurst(b) * radius * 3.2f;
        buf.billboard(ctx, VfxTextures.MAGIC_EXPLOSION, VfxBlend.ADD, center, bs, (float) inst.seed, VfxVertexBuffer.withAlpha(col, fade));
        buf.plane(VfxTextures.MAGIC_EXPLOSION, VfxBlend.ADD, pose.spin(age * 0.02f), bs * 0.6f, VfxVertexBuffer.withAlpha(col, fade * 0.8f));
        // Shock ring expanding outward
        float rr = radius * (1 + b * 3.5f);
        buf.ring(VfxTextures.GLOW, VfxBlend.ADD, pose, rr * 0.92f, rr, ctx.seg(28, 12), 1, 0,
                VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(col, 0.4f), fade));
        // Broken circle fragments spinning away (the circle "shatters")
        buf.plane(VfxTextures.MAGIC_CIRCLE, VfxBlend.ADD, pose.lift(b * 0.6f).spin(age * 0.2f), radius * (1 + b * 0.8f),
                VfxVertexBuffer.withAlpha(col, fade * 0.6f));
        // Danmaku spark spray: deterministic per seed
        RandomSource r = inst.random();
        int sparks = ctx.seg(36, 10);
        for (int i = 0; i < sparks; i++) {
            float a = Mth.TWO_PI * i / sparks + r.nextFloat() * 0.2f;
            float tilt = (r.nextFloat() - 0.3f) * 1.2f;
            float dist = radius * (0.5f + VfxAnim.easeOutCubic(b) * (2.5f + r.nextFloat() * 2.5f));
            Vector3f p = pose.point(Mth.cos(a) * dist, Mth.sin(a) * dist).add(new Vector3f(pose.normal()).mul(tilt * dist * 0.5f));
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, p, 0.35f * inst.power * (1 - b * 0.6f), a,
                    VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(col, r.nextFloat() * 0.5f), fade));
        }
    }

    /** The standard Black Clover grimoire circle. */
    static void drawCircle(VfxRenderContext ctx, VfxVertexBuffer buf, VfxPose pose, float age, float alpha, float scale,
                           float radius, int col, float spinSpeed) {
        VfxPart.glow(radius, col).draw(ctx, buf, pose, age, alpha * 0.8f, scale);
        VfxPart.circle(VfxTextures.MAGIC_CIRCLE, VfxBlend.ADD, radius, spinSpeed, col).draw(ctx, buf, pose, age, alpha, scale);
        VfxPart.runeBand(radius * 1.12f, radius * 0.16f, 32, col).draw(ctx, buf, pose, age, alpha * 0.9f, scale);
        VfxPart.orbiters(6, radius * 1.18f, 1.6f * spinSpeed, 0.4f, VfxVertexBuffer.whiten(col, 0.5f)).draw(ctx, buf, pose, age, alpha, scale);
    }
}
