package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 1.35.0: the Charybdis Spawn's look (whirlpool spirals, water spouts, tentacle sweeps, tide rings). Each shape has a TELEGRAPH voice (seed
 * bit 0 = 0: pale, steady, closing) and a STRIKE voice (seed bit 0 = 1: bright, moving). 'color' is the tide (teal by default; the Frenzy of
 * the Gates makes it a colder white-blue). At most about 300 vertices each.
 * <ul>
 *   <li>BOSS_TIDE_VORTEX: 'from' = centre, power = radius. Three spiral arms of water turning into a funnel; the telegraph shows the rim and
 *       a faint spiral, the strike (a zone: pass the length of the whirlpool as the duration) the full turning vortex with foam.</li>
 *   <li>BOSS_TIDE_SPOUT: 'from' = base, power = height. A column of water with spiralling foam, a crown at the top and a ring of splash at
 *       the base; the telegraph is a ring of rising bubbles and a thin column, the strike shoots the column up.</li>
 *   <li>BOSS_TIDE_SWEEP: 'from' = the beast's middle, 'to' = a point in the direction of the sweep, power = reach. The telegraph draws the
 *       arc's two edges and its rim; the strike sweeps three tentacles across the 200 degrees with a ribbon of water behind them.</li>
 *   <li>BOSS_TIDE_RING: 'from' = centre, power = reach. Telegraph: the outline with a ring that closes in. Strike: three tide rings of
 *       foam running out one behind the other.</li>
 * </ul>
 */
public class TideBossLayer extends AbstractVfxLayer {
    private static final int TEAL = 0xFF2FD6C8, FOAM = 0xFFE6FFFF, DEEP = 0xFF0E5C78;
    /** Half of the sweep's arc (100 degrees, as CharybdisMath.SWEEP_HALF_ARC). */
    private static final float HALF_ARC = (float) Math.toRadians(100);

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.BOSS_TIDE_VORTEX, VfxShape.BOSS_TIDE_SPOUT, VfxShape.BOSS_TIDE_SWEEP, VfxShape.BOSS_TIDE_RING); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case BOSS_TIDE_VORTEX -> 60;
            case BOSS_TIDE_SPOUT -> 28;
            case BOSS_TIDE_SWEEP -> 18;
            default -> 32;
        };
    }

    @Override public int defaultColor(VfxShape s) { return TEAL; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case BOSS_TIDE_VORTEX -> vortex(inst, ctx, buf);
            case BOSS_TIDE_SPOUT -> spout(inst, ctx, buf);
            case BOSS_TIDE_SWEEP -> sweep(inst, ctx, buf);
            case BOSS_TIDE_RING -> tide(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ vortex
    private void vortex(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick), r = Math.max(1.5f, inst.power);
        boolean strike = BossFx.strike(inst);
        int col = inst.color, foam = BossFx.hot(col, 0.7f);
        Vector3f c = ctx.rel(inst.from(ctx).add(0, 0.12, 0));
        float alpha = strike ? VfxAnim.fadeInOut(t, 0.08f, 0.2f) : VfxAnim.fadeInOut(t, 0.12f, 0.15f) * 0.75f;
        float spin = age * (strike ? 0.16f : 0.07f);
        BossFx.groundRing(ctx, buf, c, VfxTextures.WATER_SPLASH, VfxBlend.WATER, r, Math.min(1.2f, 0.35f + r * 0.08f), 20, 0f, 7f, age * 0.02f, BossFx.a(col, alpha));
        if (!strike) BossFx.groundRing(ctx, buf, c, VfxTextures.GLOW, VfxBlend.ADD, Math.max(0.5f, r * (1f - 0.8f * t)), 0.25f, 18, 0f, 1f, 0f, BossFx.a(foam, alpha));
        int arms = strike ? 3 : 2, segs = ctx.seg(8, 4);
        for (int arm = 0; arm < arms; arm++) {
            Vector3f prev = null;
            for (int i = 0; i <= segs; i++) {
                float s = (float) i / segs;                                                      // 0 = the rim, 1 = the eye
                float rad = r * (1f - 0.88f * s), ang = spin + Mth.TWO_PI * arm / arms + s * 3.4f;
                Vector3f p = BossFx.onCircle(c, rad, ang, -0.7f * s);
                if (prev != null) {
                    float w = 0.7f * (1f - s * 0.5f) + 0.1f;
                    buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, prev, p, w * 1.6f, w * 1.6f, 1, 0, BossFx.a(col, alpha * 0.5f), BossFx.a(col, alpha * 0.5f));
                    buf.beam(ctx, VfxTextures.WATER_SPLASH, VfxBlend.WATER, prev, p, w, w, 1, 0,
                            BossFx.a(BossFx.hot(col, 0.2f * s), alpha * (0.5f + 0.5f * s)), BossFx.a(foam, alpha));
                }
                prev = p;
            }
        }
        if (strike) {
            for (int i = 0; i < 4; i++) {                                                         // the funnel: rings sinking into the eye
                float s = (i + 1) / 5f;
                buf.plane(VfxTextures.GLOW, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, -0.9f * s, 0)), r * (1f - 0.8f * s) * 0.5f, BossFx.a(DEEP, alpha * 0.5f));
            }
            int n = ctx.seg(8, 3);
            for (int i = 0; i < n; i++) {
                float k = (age * 0.05f + BossFx.rnd(inst.seed, i)) % 1f, ang = spin * 1.3f + i * 2.4f + k * 3f;
                BossFx.spark(ctx, buf, BossFx.onCircle(c, r * (1f - k) * 0.9f, ang, 0.1f - 0.7f * k), 0.35f, ang, BossFx.a(foam, alpha * (1f - k)));
            }
        }
        VfxBloom.glow(ctx, buf, new Vector3f(c).add(0, -0.3f, 0), r * 0.2f, col, alpha * 0.7f);
    }

    // ------------------------------------------------------------------ spout
    private void spout(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick), h = Math.max(2f, inst.power);
        boolean strike = BossFx.strike(inst);
        int col = inst.color, foam = BossFx.hot(col, 0.7f);
        Vector3f c = ctx.rel(inst.from(ctx).add(0, 0.1, 0));
        float rad = h * 0.14f;
        float alpha = strike ? 1f - VfxAnim.easeInCubic(Math.max(0f, (t - 0.5f) / 0.5f)) : VfxAnim.fadeInOut(t, 0.15f, 0.2f) * 0.7f;
        float top = h * (strike ? VfxAnim.easeOutCubic(Math.min(1f, t * 3.5f)) : 0.25f + 0.2f * t);
        BossFx.groundRing(ctx, buf, c, VfxTextures.WATER_SPLASH, VfxBlend.WATER, rad * (strike ? 2f + t : 1.4f), 0.5f, 18, 0f, 5f, age * 0.03f, BossFx.a(col, alpha));
        buf.beam(ctx, VfxTextures.WATER_SPLASH, VfxBlend.WATER, new Vector3f(c), new Vector3f(c).add(0, top, 0), rad * 2f, rad * 1.5f, 4, -age * 0.1f,
                BossFx.a(col, alpha * 0.8f), BossFx.a(foam, alpha * 0.5f));
        buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, new Vector3f(c), new Vector3f(c).add(0, top, 0), rad * 0.7f, rad * 0.5f, 2, 0, BossFx.a(foam, alpha * 0.5f), BossFx.a(foam, alpha * 0.2f));
        int n = ctx.seg(10, 4);
        for (int i = 0; i < n; i++) {
            float k = (float) i / n, a = age * (strike ? 0.4f : 0.2f) + i * 1.9f;
            Vector3f p = new Vector3f(c).add(Mth.cos(a) * rad * 1.1f, top * k, Mth.sin(a) * rad * 1.1f);
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, p, (strike ? 0.5f : 0.3f) * (0.6f + k), a, BossFx.a(foam, alpha * (0.5f + 0.5f * k)));
        }
        if (strike) {
            BossFx.puff(ctx, buf, new Vector3f(c).add(0, top, 0), rad * 4f, age * 0.05f, BossFx.a(foam, alpha * 0.45f));      // the crown
            VfxBloom.glow(ctx, buf, new Vector3f(c).add(0, 0.5f, 0), rad * 2f, col, alpha);
        }
    }

    // ------------------------------------------------------------------ sweep
    private void sweep(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick), reach = Math.max(2f, inst.power);
        boolean strike = BossFx.strike(inst);
        int col = inst.color, foam = BossFx.hot(col, 0.7f);
        Vec3 from = inst.from(ctx), to = inst.to(ctx);
        Vec3 d = to.subtract(from).multiply(1, 0, 1);
        float yaw = d.lengthSqr() < 1e-4 ? 0f : (float) Math.atan2(d.z, d.x);
        Vector3f c = ctx.rel(from);
        if (!strike) {
            float alpha = VfxAnim.fadeInOut(t, 0.12f, 0.15f) * 0.8f;
            for (int side = -1; side <= 1; side += 2) BossFx.glowLine(ctx, buf, c, BossFx.onCircle(c, reach, yaw + side * HALF_ARC, 0f), 0.06f, col, alpha);
            arcRibbon(ctx, buf, c, reach, yaw - HALF_ARC, yaw + HALF_ARC, ctx.seg(16, 6), 0.35f, col, alpha, VfxTextures.GLOW, VfxBlend.ADD);
            BossFx.spark(ctx, buf, BossFx.onCircle(c, reach * (0.3f + 0.7f * t), yaw, 0f), 0.6f, age, BossFx.a(foam, alpha));
            return;
        }
        float head = Mth.clamp(t / 0.55f, 0f, 1f), alpha = 1f - VfxAnim.easeInCubic(Math.max(0f, (t - 0.5f) / 0.5f));
        float cur = yaw - HALF_ARC + 2f * HALF_ARC * VfxAnim.easeInOutSine(head);
        arcRibbon(ctx, buf, c, reach * 0.85f, yaw - HALF_ARC, cur, ctx.seg(14, 5), 1.3f, col, alpha * 0.8f, VfxTextures.WATER_SPLASH, VfxBlend.WATER);
        arcRibbon(ctx, buf, c, reach * 0.85f, yaw - HALF_ARC, cur, ctx.seg(14, 5), 0.3f, foam, alpha, VfxTextures.GLOW, VfxBlend.ADD);
        for (int k = 0; k < 3; k++) {                                                              // three tentacles, one behind the other
            float lag = k * 0.16f, a = cur - lag * HALF_ARC;
            if (a < yaw - HALF_ARC) continue;
            Vector3f prev = new Vector3f(c);
            for (int i = 1; i <= 5; i++) {
                float s = i / 5f, curl = (float) Math.sin(s * 3f + age * 0.3f + k) * 0.18f;
                Vector3f p = BossFx.onCircle(c, reach * s, a - curl * (1f - s) - (1f - s) * 0.25f, Mth.sin(Mth.PI * s) * 0.5f);
                buf.beam(ctx, VfxTextures.WATER_SPLASH, VfxBlend.WATER, prev, p, 0.8f * (1.1f - s), 0.8f * (1.1f - s + 0.2f), 1, 0, BossFx.a(DEEP, alpha), BossFx.a(col, alpha));
                prev = p;
            }
            BossFx.spark(ctx, buf, prev, 0.9f, age + k, BossFx.a(foam, alpha));
        }
    }

    /** A ribbon of water along the arc of radius r from world angle a0 to a1 on the floor plane of c: 'segs' quads. */
    private static void arcRibbon(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, float r, float a0, float a1, int segs, float width,
                                  int argb, float alpha, net.minecraft.resources.ResourceLocation tex, VfxBlend blend) {
        if (a1 <= a0 + 0.01f) return;
        Vector3f prev = BossFx.onCircle(c, r, a0, 0.1f);
        for (int i = 1; i <= segs; i++) {
            float k = (float) i / segs;
            Vector3f p = BossFx.onCircle(c, r, a0 + (a1 - a0) * k, 0.1f);
            buf.beam(ctx, tex, blend, prev, p, width * (0.3f + 0.7f * k), width * (0.3f + 0.7f * k), 1, 0, BossFx.a(argb, alpha * (0.2f + 0.8f * k)), BossFx.a(argb, alpha * (0.2f + 0.8f * k)));
            prev = p;
        }
    }

    // ------------------------------------------------------------------ tide rings
    private void tide(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float t = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick), reach = Math.max(1.5f, inst.power);
        boolean strike = BossFx.strike(inst);
        int col = inst.color, foam = BossFx.hot(col, 0.7f);
        Vector3f c = ctx.rel(inst.from(ctx).add(0, 0.12, 0));
        if (!strike) {
            float alpha = VfxAnim.fadeInOut(t, 0.1f, 0.15f);
            BossFx.groundRing(ctx, buf, c, VfxTextures.WATER_SPLASH, VfxBlend.WATER, reach, 0.5f, 34, 0f, Math.max(6f, reach), age * 0.02f, BossFx.a(col, alpha * 0.85f));
            BossFx.groundRing(ctx, buf, c, VfxTextures.GLOW, VfxBlend.ADD, Math.max(0.4f, reach * (1f - t)), 0.3f, 26, 0f, 1f, 0f, BossFx.a(foam, alpha));
            return;
        }
        for (int k = 0; k < 3; k++) {
            float local = Mth.clamp((t - k * 0.14f) / 0.7f, 0f, 1f);
            if (local <= 0f) continue;
            float rr = reach * VfxAnim.easeOutCubic(local), alpha = (1f - local) * (1f - 0.2f * k);
            if (k == 0) BossFx.groundRing(ctx, buf, c, VfxTextures.WATER_SPLASH, VfxBlend.WATER, rr, 0.9f, 16, 0f, 8f, age * 0.05f, BossFx.a(col, alpha));
            if (k < 2) BossFx.groundRing(ctx, buf, c, VfxTextures.GLOW, VfxBlend.ADD, rr, 1.6f, 14, 0f, 1f, 0f, BossFx.a(col, alpha * 0.55f));
            BossFx.groundRing(ctx, buf, c, VfxTextures.GLOW, VfxBlend.ADD, rr, 0.3f, 12, 0f, 1f, 0f, BossFx.a(foam, alpha));
            if (k == 0) {
                int n = ctx.seg(10, 5);
                for (int i = 0; i < n; i++) {
                    float a = Mth.TWO_PI * i / n + BossFx.rnd(inst.seed, i) * 0.4f;
                    BossFx.spark(ctx, buf, BossFx.onCircle(c, rr, a, 0.2f + 0.5f * Mth.sin(Mth.PI * local)), 0.4f, a + age * 0.2f, BossFx.a(foam, alpha));
                }
            }
        }
        if (t < 0.3f) VfxBloom.glow(ctx, buf, new Vector3f(c).add(0, 0.5f, 0), Math.min(3f, reach * 0.2f + 1f), col, 1f - t * 3f);
    }
}
