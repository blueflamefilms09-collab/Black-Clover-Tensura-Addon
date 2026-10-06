package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * ELF_CIRCLE: green elf rune circle (plant, light).
 * DEVIL_CIRCLE: red-black devil circle that spins up and bursts (five-leaf, triple spade, Devil Union).
 */
public class RuneCircleLayer extends AbstractVfxLayer {
    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.ELF_CIRCLE, VfxShape.DEVIL_CIRCLE); }
    @Override public int defaultDuration(VfxShape s) { return 40; }
    @Override public int defaultColor(VfxShape s) { return s == VfxShape.ELF_CIRCLE ? 0xFF6CFF7A : 0xFFFF2A2A; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.DEVIL_CIRCLE && inst.power >= 1.5f) VfxShake.add(inst.payload.from(), 1.4f, 14);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick);
        Vec3 from = inst.from(ctx), to = inst.to(ctx);
        Vec3 d = to.subtract(from);
        VfxPose pose = VfxPose.facing(ctx.rel(from), d.lengthSqr() < 0.01 ? new Vector3f(0, 1, 0) : new Vector3f((float) d.x, (float) d.y, (float) d.z));
        boolean devil = inst.shape == VfxShape.DEVIL_CIRCLE;
        ResourceLocation tex = devil ? VfxTextures.DEVIL_CIRCLE : VfxTextures.ELF_CIRCLE;
        float radius = 1.5f * inst.power, alpha = VfxAnim.fadeInOut(t, 0.1f, 0.3f), scale = VfxAnim.circleOpen(t);
        int col = inst.color;
        VfxBloom.planeGlow(buf, pose, radius * scale, col, alpha);
        buf.plane(tex, VfxBlend.ADD, pose.spin(VfxAnim.magicSpin(age, devil ? -2.5f : 1f)), radius * scale, VfxVertexBuffer.withAlpha(col, alpha));
        if (devil) {
            // Dark core drawn with the negative blend, then a burst near the end.
            buf.plane(VfxTextures.DEVIL_CIRCLE, VfxBlend.NEGATIVE, pose.lift(0.03f).spin(age * 0.08f), radius * 0.7f * scale, VfxVertexBuffer.withAlpha(0xFF606060, alpha * 0.6f));
            if (t > 0.6f) {
                float b = (t - 0.6f) / 0.4f;
                buf.billboard(ctx, VfxTextures.MAGIC_EXPLOSION, VfxBlend.ADD, pose.lift(0.4f).origin(), radius * 3 * VfxAnim.explosionBurst(b), age * 0.1f,
                        VfxVertexBuffer.withAlpha(col, 1 - b));
            }
        } else {
            VfxPart.runeBand(radius * 1.1f, radius * 0.15f, 28, col).draw(ctx, buf, pose, age, alpha * 0.8f, scale);
            VfxPart.orbiters(5, radius * 1.15f, 1.2f, 0.35f, VfxVertexBuffer.whiten(col, 0.5f)).draw(ctx, buf, pose, age, alpha, scale);
        }
        VfxBloom.glow(ctx, buf, pose.lift(0.2f).origin(), radius * 0.4f, col, alpha * 0.7f);
    }
}
