package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 1.93.0 Lightning Magic (Luck Voltia: Raging Lightning), drawn toward the Time Magic layers' quality: layered, eased, every sprite from textures/particle/lightningfx_*.png
 * (tools/gen_lightning_fx_textures.py). Lightning never holds still: every bolt is re-rolled every two ticks from the effect's seed and the tick, so it flickers.
 * <ul>
 *   <li>LIGHTNINGFX_CAST: a flash and the electric ring open at 'from', then a jagged bolt (ten legs, a wide sheath and a hot core, three forks) strikes through to 'to' and holds
 *       there, flickering; a flash, a ring on the ground and sparks where it lands. 'from' = origin, 'to' = target, power = size.</li>
 *   <li>LIGHTNINGFX_ZONE: the electric ring and the drum sigil turning on the ground at 'from', bolts striking down from the sky at random places inside it in turn, sparks. power = radius.</li>
 *   <li>LIGHTNINGFX_BURST: a flash star, a shock ring running out, eight arcs thrown across the ground and sparks on arcs. power = size.</li>
 * </ul>
 * Everything is additive (VfxBlend.ADD, glows land on top). At most 400 vertices an activation.
 */
public class LightningFxLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    private static final ResourceLocation BOLT = t("lightningfx_bolt"), ARC = t("lightningfx_arc"), RING = t("lightningfx_ring"), FLASH = t("lightningfx_flash"),
            SIGIL = t("lightningfx_sigil"), SPARK = t("lightningfx_spark");

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.LIGHTNINGFX_CAST, VfxShape.LIGHTNINGFX_ZONE, VfxShape.LIGHTNINGFX_BURST); }

    @Override
    public int defaultDuration(VfxShape s) { return s == VfxShape.LIGHTNINGFX_ZONE ? 80 : s == VfxShape.LIGHTNINGFX_BURST ? 24 : 16; }

    @Override public int defaultColor(VfxShape s) { return 0xFF6AB4FF; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape != VfxShape.LIGHTNINGFX_ZONE && inst.power >= 1.0f) VfxShake.add(inst.payload.from(), 0.8f * inst.power, 8);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case LIGHTNINGFX_CAST -> cast(inst, ctx, buf);
            case LIGHTNINGFX_ZONE -> zone(inst, ctx, buf);
            case LIGHTNINGFX_BURST -> burst(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ helpers
    private static float life(VfxInstance inst, float age, float in, float out) {
        float a = in <= 0 ? 1 : Mth.clamp(age / in, 0, 1), b = out <= 0 ? 1 : Mth.clamp((inst.duration - age) / out, 0, 1);
        return Math.min(a, b);
    }

    /** A hash of the effect's seed, the roll (changes every two ticks) and two counters, in 0..1. */
    private static float h(VfxInstance inst, int roll, int i, int k) {
        int x = (int) (inst.seed ^ (inst.seed >>> 32)) * 0x9E3779B1 ^ roll * 0x85EBCA6B ^ i * 0xC2B2AE35 ^ k * 0x27D4EB2F;
        x ^= x >>> 15; x *= 0x2C1B3C6D; x ^= x >>> 12; x *= 0x297A2D39; x ^= x >>> 15;
        return (x & 0xFFFFFF) / (float) 0x1000000;
    }

    private static void strip(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, Vector3f a, Vector3f b, float wa, float wb, int ca, int cb) {
        Vector3f dir = new Vector3f(b).sub(a);
        if (dir.lengthSquared() < 1e-6f) return;
        Vector3f toCam = new Vector3f(a).add(b).mul(0.5f).negate().normalize();
        Vector3f side = new Vector3f(dir).cross(toCam);
        if (side.lengthSquared() < 1e-8f) return;
        side.normalize();
        Vector3f s0 = new Vector3f(side).mul(wa * 0.5f), s1 = new Vector3f(side).mul(wb * 0.5f);
        int c0 = VfxBlend.ADD.grade(ca, 1f), c1 = VfxBlend.ADD.grade(cb, 1f);
        buf.quad(tex, VfxBlend.ADD, new Vector3f(a).sub(s0), new Vector3f(b).sub(s1), new Vector3f(b).add(s1), new Vector3f(a).add(s0), 0, 0, 1, 1, c0, c0);
    }

    /** A bolt from a to b: n legs jittered sideways (the jitter is zero at both ends), drawn twice (a wide soft sheath and a narrow hot core). Returns the points for forks. */
    private static Vector3f[] bolt(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f a, Vector3f b, int roll, int salt, int n, float amp, float width, int col, int hot, float alpha) {
        Vector3f dir = new Vector3f(b).sub(a);
        Vector3f up = Math.abs(dir.y) > 0.9f * dir.length() ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        Vector3f s1 = new Vector3f(dir).cross(up).normalize(), s2 = new Vector3f(s1).cross(dir).normalize();
        Vector3f[] pts = new Vector3f[n + 1];
        for (int i = 0; i <= n; i++) {
            float t = i / (float) n, k = amp * Mth.sin(t * Mth.PI);
            pts[i] = new Vector3f(a).add(new Vector3f(dir).mul(t)).add(new Vector3f(s1).mul((h(inst, roll, i, salt) - 0.5f) * 2f * k)).add(new Vector3f(s2).mul((h(inst, roll, i, salt + 31) - 0.5f) * 2f * k));
        }
        for (int i = 0; i < n; i++) {
            strip(ctx, buf, ARC, pts[i], pts[i + 1], width * 2.2f, width * 2.2f, VfxVertexBuffer.withAlpha(col, 0.55f * alpha), VfxVertexBuffer.withAlpha(col, 0.55f * alpha));
            strip(ctx, buf, ARC, pts[i], pts[i + 1], width, width, VfxVertexBuffer.withAlpha(hot, alpha), VfxVertexBuffer.withAlpha(hot, alpha));
        }
        return pts;
    }

    // ------------------------------------------------------------------ LIGHTNINGFX_CAST
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power, fade = life(inst, age, 0, 5);
        int col = inst.color, hot = VfxVertexBuffer.whiten(col, 0.8f), roll = (int) (age / 2f);
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(from);
        float len = dir.length();
        if (len < 0.05f) return;
        dir.div(len);
        float charge = Mth.clamp(age / (inst.duration * 0.22f), 0, 1), strike = Mth.clamp((age - inst.duration * 0.12f) / (inst.duration * 0.2f), 0, 1);
        VfxPose face = VfxPose.facing(from, dir);
        float open = VfxAnim.easeOutBack(charge) * (1f - 0.5f * strike);
        buf.plane(RING, VfxBlend.ADD, face.spin(age * 0.3f), 0.6f * p * open, VfxVertexBuffer.withAlpha(col, 0.9f * fade));
        buf.billboard(ctx, FLASH, VfxBlend.ADD, from, 1.0f * p * open, age * 0.2f, VfxVertexBuffer.withAlpha(hot, 0.9f * fade));
        VfxBloom.glow(ctx, buf, from, 0.5f * p * open, col, 0.8f * fade);
        if (strike <= 0f) return;
        float flick = 0.65f + 0.35f * h(inst, roll, 7, 7);
        Vector3f tip = new Vector3f(from).lerp(to, VfxAnim.easeOutCubic(strike));
        Vector3f[] pts = bolt(inst, ctx, buf, from, tip, roll, 1, ctx.seg(10, 5), 0.45f * p * Math.min(1f, len / 6f + 0.4f), 0.2f * p, col, hot, flick * fade);
        for (int k = 0; k < 3; k++) {                                                           // forks off the bolt
            int i = 2 + (int) (h(inst, 0, k, 9) * (pts.length - 4));
            if (i >= pts.length - 1) continue;
            float a = (h(inst, roll, k, 11) - 0.5f) * 2f;
            Vector3f end = new Vector3f(pts[i]).add(new Vector3f(dir).mul(len * 0.18f)).add(new Vector3f(0, (h(inst, roll, k, 12) - 0.5f) * 1.5f * p, 0)).add(a * 0.8f * p, 0, a * 0.8f * p);
            strip(ctx, buf, ARC, pts[i], end, 0.14f * p, 0.04f * p, VfxVertexBuffer.withAlpha(hot, 0.8f * flick * fade), VfxVertexBuffer.withAlpha(col, 0.4f * flick * fade));
        }
        if (strike > 0.95f) {                                                                   // where it lands
            float k = Mth.clamp((age - inst.duration * 0.32f) / (inst.duration * 0.5f), 0, 1);
            buf.billboard(ctx, FLASH, VfxBlend.ADD, to, (1.2f + 1.3f * k) * p, age * 0.4f, VfxVertexBuffer.withAlpha(hot, (1f - k) * fade));
            buf.plane(RING, VfxBlend.ADD, VfxPose.ground(new Vector3f(to).add(0, 0.08f, 0)).spin(age * 0.4f), (0.4f + 1.6f * k) * p, VfxVertexBuffer.withAlpha(col, (1f - k) * 0.9f * fade));
            int n = ctx.seg(8, 3);
            for (int i = 0; i < n; i++) {
                float a = h(inst, 0, i, 21) * Mth.TWO_PI, v = (0.6f + 1.6f * h(inst, 0, i, 22)) * p;
                buf.billboard(ctx, SPARK, VfxBlend.ADD, new Vector3f(to).add(Mth.cos(a) * v * k, 0.2f + 1.0f * p * Mth.sin(k * Mth.PI) * h(inst, 0, i, 23), Mth.sin(a) * v * k), 0.22f * p,
                        age * 0.5f + i, VfxVertexBuffer.withAlpha(i % 2 == 0 ? hot : col, (1f - k) * fade));
            }
        }
    }

    // ------------------------------------------------------------------ LIGHTNINGFX_ZONE
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), R = inst.power, fade = life(inst, age, 8, 12);
        if (fade <= 0.01f) return;
        int col = inst.color, hot = VfxVertexBuffer.whiten(col, 0.8f), roll = (int) (age / 3f);
        Vector3f c = ctx.rel(inst.from(ctx));
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 10f, 0, 1));
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, 0.06f, 0));
        VfxBloom.planeGlow(buf, ground.lift(-0.02f), R * open, col, 0.4f * fade);
        buf.plane(SIGIL, VfxBlend.ADD, ground.spin(-age * 0.05f), R * 0.88f * open, VfxVertexBuffer.withAlpha(col, 0.75f * fade));
        buf.plane(RING, VfxBlend.ADD, ground.lift(0.03f).spin(age * 0.12f), R * 1.04f * open, VfxVertexBuffer.withAlpha(hot, (0.75f + 0.25f * h(inst, roll, 1, 1)) * fade));
        int n = ctx.seg(7, 3);
        for (int i = 0; i < n; i++) {                                                           // bolts from the sky into the zone, each on for a few ticks, then another place
            int slot = (int) (age / 5f) + i * 3;
            float on = Mth.frac(age / 5f + i * 0.37f);
            if (on > 0.55f) continue;
            float a = h(inst, slot, i, 3) * Mth.TWO_PI, d = Mth.sqrt(h(inst, slot, i, 4)) * R * 0.95f, hgt = 5f + 3f * h(inst, slot, i, 5);
            Vector3f ground0 = new Vector3f(c).add(Mth.cos(a) * d, 0f, Mth.sin(a) * d);
            Vector3f top = new Vector3f(ground0).add((h(inst, slot, i, 6) - 0.5f) * 1.5f, hgt, (h(inst, slot, i, 7) - 0.5f) * 1.5f);
            float flick = 0.6f + 0.4f * h(inst, roll, i, 8);
            bolt(inst, ctx, buf, top, ground0, slot, i * 5, ctx.seg(6, 3), 0.5f, 0.2f, col, hot, flick * fade);
            buf.billboard(ctx, FLASH, VfxBlend.ADD, new Vector3f(ground0).add(0, 0.15f, 0), 0.9f * flick, age * 0.5f + i, VfxVertexBuffer.withAlpha(hot, 0.9f * fade));
        }
        int sp = ctx.seg(10, 4);
        for (int i = 0; i < sp; i++) {
            float life = Mth.frac(age * 0.04f * (1f + 0.5f * h(inst, 0, i, 31)) + h(inst, 0, i, 32)), a = h(inst, 0, i, 33) * Mth.TWO_PI, d = Mth.sqrt(h(inst, 0, i, 34)) * R;
            buf.billboard(ctx, SPARK, VfxBlend.ADD, new Vector3f(c).add(Mth.cos(a) * d, 0.2f + life * 1.5f, Mth.sin(a) * d), 0.16f * (1f - life) + 0.05f, age * 0.4f + i,
                    VfxVertexBuffer.withAlpha(i % 2 == 0 ? hot : col, Mth.sin(life * Mth.PI) * fade));
        }
    }

    // ------------------------------------------------------------------ LIGHTNINGFX_BURST
    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = inst.progress(ctx.partialTick), p = inst.power, fade = 1f - VfxAnim.easeInCubic(t);
        int col = inst.color, hot = VfxVertexBuffer.whiten(col, 0.8f), roll = (int) (age / 2f);
        Vector3f c = ctx.rel(inst.from(ctx));
        float flash = Mth.clamp(1f - age / 5f, 0, 1);
        VfxBloom.glow(ctx, buf, c, 0.9f * p * (0.5f + VfxAnim.easeOutCubic(t)), col, fade);
        buf.billboard(ctx, FLASH, VfxBlend.ADD, c, (1.4f + 2.2f * VfxAnim.easeOutCubic(Mth.clamp(t * 2f, 0, 1))) * p, age * 0.15f, VfxVertexBuffer.withAlpha(hot, Math.min(1f, 0.35f + flash) * fade));
        float rr = (0.4f + 2.6f * VfxAnim.easeOutCubic(t)) * p;
        buf.plane(RING, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, 0.07f, 0)).spin(age * 0.3f), rr, VfxVertexBuffer.withAlpha(col, 0.95f * fade));
        buf.plane(RING, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, 0.09f, 0)).spin(-age * 0.4f), rr * 0.6f, VfxVertexBuffer.withAlpha(hot, 0.7f * fade));
        int n = ctx.seg(8, 4);
        for (int i = 0; i < n; i++) {                                                           // arcs thrown across the ground, each re-rolled
            float a = i * Mth.TWO_PI / n + h(inst, 0, i, 41) * 0.4f, reach = (1.0f + 1.0f * h(inst, 0, i, 42)) * p * VfxAnim.easeOutCubic(Mth.clamp(t * 1.6f, 0, 1));
            Vector3f end = new Vector3f(c).add(Mth.cos(a) * reach, 0.25f + 0.2f * h(inst, roll, i, 43), Mth.sin(a) * reach);
            bolt(inst, ctx, buf, new Vector3f(c).add(0, 0.2f, 0), end, roll, i * 3, 3, 0.25f * p, 0.14f * p, col, hot, fade * (0.6f + 0.4f * h(inst, roll, i, 44)));
        }
        int e = ctx.seg(10, 4);
        for (int i = 0; i < e; i++) {
            float a = h(inst, 0, i, 51) * Mth.TWO_PI, v = (1.0f + 2.2f * h(inst, 0, i, 52)) * p, vy = (1.4f + 2.0f * h(inst, 0, i, 53)) * p, tt = t * 1.2f;
            buf.billboard(ctx, SPARK, VfxBlend.ADD, new Vector3f(c).add(Mth.cos(a) * v * tt * 0.7f, 0.1f + vy * tt - 2.6f * tt * tt * p, Mth.sin(a) * v * tt * 0.7f), 0.2f * p * (1f - t) + 0.04f,
                    age * 0.5f + i, VfxVertexBuffer.withAlpha(i % 2 == 0 ? hot : col, fade));
        }
    }
}
