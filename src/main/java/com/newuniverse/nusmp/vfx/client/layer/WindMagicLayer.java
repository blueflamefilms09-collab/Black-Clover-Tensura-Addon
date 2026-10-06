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
 * Wind Magic.
 * WIND_SLASH: a crescent blade (three stacked arcs) that flies from caster to target, leaving
 * a fading afterimage trail and speed streaks.
 * WIND_RING: three gust rings rippling outward + rising spiral streaks around the caster.
 */
public class WindMagicLayer extends AbstractVfxLayer {
    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.WIND_SLASH, VfxShape.WIND_RING); }
    @Override public int defaultDuration(VfxShape s) { return s == VfxShape.WIND_SLASH ? 16 : 30; }
    @Override public int defaultColor(VfxShape s) { return 0xFF8CFFC2; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        if (inst.shape == VfxShape.WIND_SLASH) slash(inst, ctx, buf); else ring(inst, ctx, buf);
    }

    private void slash(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick);
        Vec3 fromW = inst.from(ctx), toW = inst.to(ctx);
        Vec3 dirW = toW.subtract(fromW);
        if (dirW.lengthSqr() < 0.01) return;
        Vector3f a = ctx.rel(fromW), b = ctx.rel(toW);
        Vector3f dir = new Vector3f((float) dirW.x, (float) dirW.y, (float) dirW.z).normalize();
        float travel = VfxAnim.slashHead(t), alpha = VfxAnim.fadeInOut(t, 0.05f, 0.35f);
        float radius = 1.6f * inst.power;
        // The blade plane contains the flight direction; rolled a little per seed for variety.
        Vector3f planeNormal = Math.abs(dir.y) > 0.95f ? new Vector3f(1, 0, 0) : new Vector3f(dir).cross(0, 1, 0).normalize();
        VfxPose base = VfxPose.facing(new Vector3f(b).sub(a).mul(travel).add(a), planeNormal);
        VfxPose blade = base.spin(((inst.seed & 7) - 3.5f) * 0.12f);
        int col = inst.color, edge = VfxVertexBuffer.whiten(col, 0.7f);
        float facingAngle = (float) Math.atan2(dir.dot(blade.up()), dir.dot(blade.right()));
        float sweep = 2.4f;
        int seg = ctx.seg(12, 6);
        // Afterimages (older positions, fainter)
        for (int k = 3; k >= 1; k--) {
            float back = Math.max(0, travel - k * 0.06f);
            VfxPose ghost = new VfxPose(new Vector3f(b).sub(a).mul(back).add(a), blade.right(), blade.up(), blade.normal());
            buf.arc(VfxTextures.WIND_SLASH, VfxBlend.WIND, ghost, radius, 0.5f * inst.power, facingAngle - sweep / 2, sweep, seg,
                    VfxVertexBuffer.withAlpha(col, alpha * (0.35f / k)));
        }
        // Main blade: thick body + bright thin edge
        buf.arc(VfxTextures.WIND_SLASH, VfxBlend.WIND, blade, radius, 0.7f * inst.power, facingAngle - sweep / 2, sweep, seg,
                VfxVertexBuffer.withAlpha(col, alpha));
        buf.arc(VfxTextures.WIND_SLASH, VfxBlend.ADD, blade, radius * 1.02f, 0.18f * inst.power, facingAngle - sweep / 2, sweep, seg,
                VfxVertexBuffer.withAlpha(edge, alpha));
        VfxBloom.glow(ctx, buf, blade.point(Mth.cos(facingAngle) * radius, Mth.sin(facingAngle) * radius), 0.7f * inst.power, col, alpha);
        // Speed streaks
        RandomSource r = inst.random();
        int streaks = ctx.seg(10, 4);
        for (int i = 0; i < streaks; i++) {
            Vector3f off = new Vector3f(r.nextFloat() - 0.5f, r.nextFloat() - 0.5f, r.nextFloat() - 0.5f).mul(radius * 1.5f);
            Vector3f s1 = new Vector3f(blade.origin()).add(off);
            Vector3f s0 = new Vector3f(dir).mul(-(1.5f + r.nextFloat() * 2f) * inst.power).add(s1);
            buf.beam(ctx, VfxTextures.GLOW, VfxBlend.WIND, s0, s1, 0.02f, 0.08f, 1, 0, VfxVertexBuffer.withAlpha(col, 0), VfxVertexBuffer.withAlpha(edge, alpha * 0.8f));
        }
    }

    private void ring(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick);
        Vector3f c = ctx.rel(inst.from(ctx));
        float p = inst.power;
        int col = inst.color, edge = VfxVertexBuffer.whiten(col, 0.6f);
        // Three ripples, staggered
        for (int k = 0; k < 3; k++) {
            float lt = Mth.clamp(t * 1.4f - k * 0.18f, 0, 1);
            if (lt <= 0) continue;
            float r = (0.6f + VfxAnim.easeOutCubic(lt) * 4.5f) * p;
            float alpha = 1 - lt;
            VfxPose pose = VfxPose.ground(new Vector3f(c).add(0, 0.1f + k * 0.35f * p, 0)).spin(age * 0.05f * (k % 2 == 0 ? 1 : -1));
            buf.ring(VfxTextures.WIND_SLASH, VfxBlend.WIND, pose, r * 0.8f, r, ctx.seg(24, 10), 3, age * 0.03f, VfxVertexBuffer.withAlpha(col, alpha));
            buf.ring(VfxTextures.GLOW, VfxBlend.ADD, pose, r * 0.97f, r * 1.02f, ctx.seg(24, 10), 1, 0, VfxVertexBuffer.withAlpha(edge, alpha * 0.8f));
        }
        // Rising spiral streaks (tornado feel)
        int arms = ctx.seg(6, 3);
        float fade = VfxAnim.fadeInOut(t, 0.1f, 0.4f);
        for (int i = 0; i < arms; i++) {
            float a = VfxAnim.runeOrbit(i, arms, age, 3f);
            float rad = 1.4f * p;
            Vector3f lo = new Vector3f(Mth.cos(a) * rad, 0.1f, Mth.sin(a) * rad).add(c);
            Vector3f hi = new Vector3f(Mth.cos(a + 1.2f) * rad * 0.6f, 2.5f * p * VfxAnim.easeOutCubic(t), Mth.sin(a + 1.2f) * rad * 0.6f).add(c);
            buf.beam(ctx, VfxTextures.WIND_SLASH, VfxBlend.WIND, lo, hi, 0.25f * p, 0.05f, 4, age * 0.05f,
                    VfxVertexBuffer.withAlpha(col, fade), VfxVertexBuffer.withAlpha(edge, 0));
        }
        VfxBloom.glow(ctx, buf, new Vector3f(c).add(0, 0.5f * p, 0), 0.8f * p, col, (1 - t) * 0.6f);
    }
}
