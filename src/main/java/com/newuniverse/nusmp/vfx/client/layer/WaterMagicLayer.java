package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/** Water: WATER_SPLASH (burst + droplets at 'to'), WATER_RING (rings rippling out from 'from'). */
public class WaterMagicLayer extends AbstractVfxLayer {
    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.WATER_SPLASH, VfxShape.WATER_RING); }
    @Override public int defaultDuration(VfxShape s) { return s == VfxShape.WATER_SPLASH ? 20 : 30; }
    @Override public int defaultColor(VfxShape s) { return 0xFF4FA8FF; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.power >= 1.5f) VfxShake.add(inst.payload.to(), 0.9f * inst.power, 10);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick);
        float p = inst.power, fade = 1 - VfxAnim.easeInCubic(t);
        int col = inst.color, light = VfxVertexBuffer.whiten(col, 0.6f);
        if (inst.shape == VfxShape.WATER_SPLASH) {
            Vector3f c = ctx.rel(inst.to(ctx));
            VfxBloom.glow(ctx, buf, c, 0.8f * p, col, fade);
            buf.plane(VfxTextures.WATER_SPLASH, VfxBlend.WATER, VfxPose.ground(c).spin(age * 0.05f), 1.6f * p * (0.5f + VfxAnim.easeOutCubic(t)),
                    VfxVertexBuffer.withAlpha(col, fade));
            RandomSource r = inst.random();
            int drops = ctx.seg(20, 6);
            for (int i = 0; i < drops; i++) {
                float yaw = r.nextFloat() * Mth.TWO_PI, up = 0.4f + r.nextFloat();
                float d = 2.2f * p * VfxAnim.easeOutCubic(t);
                Vector3f q = new Vector3f(Mth.cos(yaw) * d, up * d * 1.2f - t * t * 3f, Mth.sin(yaw) * d).add(c);
                buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.WATER, q, 0.25f * p, 0, VfxVertexBuffer.withAlpha(light, fade));
            }
            return;
        }
        Vector3f c = ctx.rel(inst.from(ctx));
        for (int k = 0; k < 3; k++) {
            float lt = Mth.clamp(t * 1.4f - k * 0.2f, 0, 1);
            if (lt <= 0) continue;
            float r = (0.5f + VfxAnim.easeOutCubic(lt) * 3.5f) * p;
            VfxPose pose = VfxPose.ground(new Vector3f(c).add(0, 0.08f + k * 0.04f, 0)).spin(age * 0.03f);
            buf.ring(VfxTextures.WATER_SPLASH, VfxBlend.WATER, pose, r * 0.75f, r, ctx.seg(24, 10), 2, age * 0.02f, VfxVertexBuffer.withAlpha(col, 1 - lt));
            buf.ring(VfxTextures.GLOW, VfxBlend.ADD, pose, r * 0.96f, r * 1.03f, ctx.seg(24, 10), 1, 0, VfxVertexBuffer.withAlpha(light, (1 - lt) * 0.8f));
        }
    }
}
