package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/** ANTI_MAGIC_SLASH: a black-violet crescent drawn with the NEGATIVE blend, then shards shatter outward. */
public class AntiMagicLayer extends AbstractVfxLayer {
    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.ANTI_MAGIC_SLASH); }
    @Override public int defaultDuration(VfxShape s) { return 18; }
    @Override public int defaultColor(VfxShape s) { return 0xFF2A0A30; }

    @Override
    public void onSpawn(VfxInstance inst) { if (inst.power >= 1.5f) VfxShake.add(inst.payload.from(), 1.0f, 8); }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick);
        Vec3 a = inst.from(ctx), b = inst.to(ctx), d = b.subtract(a);
        if (d.lengthSqr() < 0.01) return;
        Vector3f dir = new Vector3f((float) d.x, (float) d.y, (float) d.z).normalize();
        Vector3f n = Math.abs(dir.y) > 0.95f ? new Vector3f(1, 0, 0) : new Vector3f(dir).cross(0, 1, 0).normalize();
        Vector3f mid = ctx.rel(a.lerp(b, VfxAnim.slashHead(t)));
        VfxPose blade = VfxPose.facing(mid, n);
        float ang = (float) Math.atan2(dir.dot(blade.up()), dir.dot(blade.right()));
        float alpha = VfxAnim.fadeInOut(t, 0.05f, 0.35f), r = 1.6f * inst.power;
        int seg = ctx.seg(12, 6);
        buf.arc(VfxTextures.ANTI_MAGIC_SLASH, VfxBlend.NEGATIVE, blade, r, 0.9f * inst.power, ang - 1.2f, 2.4f, seg, VfxVertexBuffer.withAlpha(0xFFFFFFFF, alpha));
        buf.arc(VfxTextures.ANTI_MAGIC_SLASH, VfxBlend.ADD, blade, r * 1.03f, 0.25f * inst.power, ang - 1.2f, 2.4f, seg, VfxVertexBuffer.withAlpha(0xFF8A2BE2, alpha));
        RandomSource rnd = inst.random();
        int shards = ctx.seg(14, 5);
        for (int i = 0; i < shards; i++) {
            float yaw = rnd.nextFloat() * Mth.TWO_PI, up = rnd.nextFloat() - 0.3f;
            float dist = r * VfxAnim.easeOutCubic(t) * (0.8f + rnd.nextFloat());
            Vector3f q = new Vector3f(Mth.cos(yaw) * dist, up * dist, Mth.sin(yaw) * dist).add(mid);
            buf.billboard(ctx, VfxTextures.SHARD, VfxBlend.ADD, q, 0.3f * inst.power, yaw + age * 0.3f, VfxVertexBuffer.withAlpha(0xFF9B59D0, 1 - t));
        }
        VfxBloom.glow(ctx, buf, mid, 0.8f * inst.power, 0xFF6A1B9A, alpha * 0.7f);
    }
}
