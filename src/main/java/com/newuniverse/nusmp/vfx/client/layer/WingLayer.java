package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 1.93.0 Wing Magic: light, white and gold, built from feathers. Textures from tools/gen_wing_rebuild.py (textures/particle/wing_*.png). Sizes: VfxInstance
 * multiplies the payload power by 3 for FX1 / FX3 and by 1.5 for FX2, so {@code inst.power} is that product.
 * <ul>
 *   <li>WING_FX1, Feather Shot / Wing Slash / Wing Dash (cast): a pair of small wings unfolds at 'from' (the charge), then a great glowing feather runs to
 *       'to' on a soft streak, shedding down feathers that tumble away and gold glints, with a spread of three flight feathers fanned round it; where it ends
 *       a feather burst opens. 'from' = hand, 'to' = target, default 16 ticks.</li>
 *   <li>WING_FX2, Updraft / the wake of Skyward (zone): a gold halo turns on the ground over the real area, a column of rising light streaks stands on the rim,
 *       feathers climb in a spiral through the whole area, glints twinkle. 'from' = centre, power = radius, fades in over 8 and out over 12 ticks, default 80.</li>
 *   <li>WING_FX3, impact / hawk strike / call (burst): a flash, two great wings that open to both sides in the plane of the camera and fold again, a ring across
 *       the ground, feathers scattered outwards and drifting down. 'from' = centre, default 22 ticks.</li>
 * </ul>
 * Alpha blend first (the feathers), additive last (glows, glints, halo, streaks of light). About 140 vertices of 400.
 */
public class WingLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation FEATHER = t("wing_feather"), WING = t("wing_wing"), STREAK = t("wing_streak"), HALO = t("wing_halo"), GLINT = t("wing_glint"), DOWN = t("wing_down");

    private static final int CREAM = 0xFFF6F2FF, GOLD = 0xFFFFDC7A, SILVER = 0xFFD6D8F2;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.WING_FX1, VfxShape.WING_FX2, VfxShape.WING_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case WING_FX1 -> 16;
            case WING_FX2 -> 80;
            default -> 22;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFE6E2F4; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case WING_FX1 -> cast(inst, ctx, buf);
            case WING_FX2 -> zone(inst, ctx, buf);
            default -> burst(inst, ctx, buf);
        }
    }

    private static float hash(long seed, int i, int k) {
        long x = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L + k * 0x94D049BB133111EBL;
        x ^= x >>> 30; x *= 0xBF58476D1CE4E5B9L; x ^= x >>> 27; x *= 0x94D049BB133111EBL; x ^= x >>> 31;
        return (x >>> 40) / (float) (1 << 24);
    }

    private static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(alpha, 0f, 1f)); }
    private static float cl(float v) { return Mth.clamp(v, 0f, 1f); }
    private static float sub(float v, float s, float e) { return cl((v - s) / Math.max(1e-4f, e - s)); }
    private static Vector3f lerp(Vector3f p, Vector3f q, float t) { return new Vector3f(p).lerp(q, t); }

    private static void sprite(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f p, float size, float rot, int col) {
        if (buf.hasBudget(4)) buf.billboard(ctx, tex, bl, p, size, rot, col);
    }

    private static void ground(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f centre, float radius, float spin, int col) {
        if (buf.hasBudget(4)) buf.plane(tex, bl, VfxPose.ground(centre).spin(spin), radius, col);
    }

    /** A wing sprite in the plane of the camera: the shoulder (the bottom left of the image) at 'at', opening by 'open' (0 folded .. 1 spread), side -1 left / +1 right. */
    private static void wing(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f at, float size, float open, int side, int col) {
        if (!buf.hasBudget(4) || open <= 0.01f) return;
        float ang = (1f - open) * 1.2f * side;
        Vector3f r = new Vector3f(ctx.camRight), u = new Vector3f(ctx.camUp);
        Vector3f rr = new Vector3f(r).mul(Mth.cos(ang)).add(new Vector3f(u).mul(Mth.sin(ang) * side));
        Vector3f uu = new Vector3f(u).mul(Mth.cos(ang)).sub(new Vector3f(r).mul(Mth.sin(ang) * side));
        rr.mul(side * size * open);
        uu.mul(size * open);
        Vector3f p0 = new Vector3f(at), p1 = new Vector3f(at).add(rr), p2 = new Vector3f(at).add(rr).add(uu), p3 = new Vector3f(at).add(uu);
        float u0 = side > 0 ? 0f : 1f, u1 = side > 0 ? 1f : 0f;
        buf.quad(WING, VfxBlend.ALPHA, p0, p1, p2, p3, u0, 0f, u1, 1f, col, col);
    }

    // ------------------------------------------------------------------------------------------------ FX1
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.progress(ctx.partialTick);
        float P = Math.max(0.5f, inst.power) * 1.3f;
        long seed = inst.seed;
        Vector3f f = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(f);
        float dist = dir.length();
        if (dist < 0.05f) dir.set(ctx.camRight); else dir.div(dist);
        Vector3f side = new Vector3f(dir).cross(ctx.camUp);
        if (side.lengthSquared() < 1e-4f) side.set(ctx.camRight);
        side.normalize();
        Vector3f up = new Vector3f(side).cross(dir).normalize();
        int cream = VfxVertexBuffer.lerpColor(inst.color | 0xFF000000, CREAM, 0.6f);

        float run = VfxAnim.easeInOutSine(sub(p, 0.12f, 0.85f)) * 0.4f + sub(p, 0.12f, 0.85f) * 0.6f;
        Vector3f head = lerp(f, to, run);
        float live = 1f - sub(p, 0.88f, 1f);
        float cp = sub(p, 0f, 0.25f), cf = 1f - sub(p, 0.2f, 0.45f);

        // 1. down feathers tumbling off the path, the streak (alpha)
        if (run > 0.02f && live > 0f) {
            Vector3f tail = lerp(f, to, Math.max(0f, run - 0.4f));
            if (buf.hasBudget(4)) buf.beam(ctx, STREAK, VfxBlend.ALPHA, tail, head, P * 0.5f, P * 0.18f, 1, 0f, a(cream, 0.0f), a(cream, 0.75f * live));
            for (int k = 0; k < 7; k++) {
                float tk = Math.max(0f, run - 0.05f * (k + 1) * (0.6f + hash(seed, k, 1)));
                float fall = (0.2f + 0.5f * hash(seed, k, 2)) * Math.max(0f, run - tk) * 3f;
                Vector3f sp = lerp(f, to, tk).add(new Vector3f(side).mul((hash(seed, k, 3) - 0.5f) * P * 0.9f)).add(new Vector3f(up).mul(-fall * P * 0.4f));
                sprite(ctx, buf, DOWN, VfxBlend.ALPHA, sp, P * 0.3f, hash(seed, k, 4) * 6.28f + age * 0.12f * (k % 2 == 0 ? 1 : -1), a(cream, 0.9f * live * (1f - 0.08f * k)));
            }
        }
        // 2. the three fanned flight feathers and the great feather
        if (run > 0.01f && live > 0f) {
            for (int k = -1; k <= 1; k++) {
                if (k == 0) continue;
                Vector3f fp = new Vector3f(head).add(new Vector3f(side).mul(k * P * 0.35f)).add(new Vector3f(dir).mul(-P * 0.25f));
                sprite(ctx, buf, FEATHER, VfxBlend.ALPHA, fp, P * 0.6f, (float) Math.atan2(dir.dot(ctx.camUp), dir.dot(ctx.camRight)) - Mth.HALF_PI + k * 0.3f, a(cream, 0.9f * live));
            }
            sprite(ctx, buf, FEATHER, VfxBlend.ALPHA, head, P * 1.15f, (float) Math.atan2(dir.dot(ctx.camUp), dir.dot(ctx.camRight)) - Mth.HALF_PI, a(cream, 0.98f * live));
        }
        // 3. the charge wings at 'from' and the additive glows
        if (cf > 0f) {
            float open = VfxAnim.easeOutBack(cp);
            wing(ctx, buf, f, P * 1.5f, open, -1, a(cream, 0.9f * cf));
            wing(ctx, buf, f, P * 1.5f, open, 1, a(cream, 0.9f * cf));
            sprite(ctx, buf, GLINT, VfxBlend.ADD, f, P * (0.4f + 1.1f * cp), 0f, a(GOLD, 0.9f * cf));
        }
        if (run > 0.01f && live > 0f) {
            sprite(ctx, buf, GLINT, VfxBlend.ADD, head, P * (0.7f + 0.15f * Mth.sin(age * 0.6f)), age * 0.2f, a(GOLD, 0.9f * live));
            sprite(ctx, buf, FEATHER, VfxBlend.ADD, head, P * 1.25f, (float) Math.atan2(dir.dot(ctx.camUp), dir.dot(ctx.camRight)) - Mth.HALF_PI, a(GOLD, 0.3f * live));
        }
        float bp = sub(p, 0.78f, 1f);
        if (bp > 0f) {
            for (int k = 0; k < 5; k++) {
                float ang = Mth.TWO_PI * k / 5f + hash(seed, k, 5), d = P * (0.2f + 0.9f * VfxAnim.easeOutCubic(bp));
                Vector3f fp = new Vector3f(to).add(new Vector3f(ctx.camRight).mul(Mth.cos(ang) * d)).add(new Vector3f(ctx.camUp).mul(Mth.sin(ang) * d));
                sprite(ctx, buf, FEATHER, VfxBlend.ALPHA, fp, P * 0.5f, ang - Mth.HALF_PI, a(cream, 0.9f * (1f - bp)));
            }
            sprite(ctx, buf, GLINT, VfxBlend.ADD, to, P * (0.5f + 1.0f * bp), 0f, a(GOLD, 0.8f * (1f - bp)));
        }
    }

    // ------------------------------------------------------------------------------------------------ FX2
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float R = Math.max(1.2f, inst.power);
        long seed = inst.seed;
        float open = VfxAnim.easeOutCubic(cl(age / 8f));
        float fade = cl(Math.min(age / 8f, (inst.duration - age) / 12f));
        if (fade <= 0.01f) return;
        Vector3f c = ctx.rel(inst.from(ctx));
        int cream = VfxVertexBuffer.lerpColor(inst.color | 0xFF000000, CREAM, 0.6f);

        // 1. feathers climbing in a spiral through the area (alpha)
        int n = ctx.seg(18, 8);
        for (int k = 0; k < n; k++) {
            float ph = (age * 0.014f + hash(seed, k, 1)) % 1f;
            float ang = hash(seed, k, 2) * Mth.TWO_PI + ph * 5f * (k % 2 == 0 ? 1f : -1f), rr = R * (0.25f + 0.7f * hash(seed, k, 3)) * (0.7f + 0.3f * ph);
            Vector3f pos = new Vector3f(c).add(Mth.cos(ang) * rr, 0.1f + ph * R * 1.3f, Mth.sin(ang) * rr);
            sprite(ctx, buf, k % 3 == 0 ? FEATHER : DOWN, VfxBlend.ALPHA, pos, R * (0.16f + 0.14f * hash(seed, k, 4)), ang + age * 0.05f, a(cream, Mth.sin(ph * Mth.PI) * 0.92f * fade));
        }
        // 2. additive: the gold halo on the ground, the rim of rising light, glints
        ground(buf, HALO, VfxBlend.ADD, new Vector3f(c).add(0, 0.06f, 0), R * open, age * 0.02f, a(GOLD, 0.8f * fade));
        ground(buf, HALO, VfxBlend.ADD, new Vector3f(c).add(0, 0.09f, 0), R * 0.6f * open, -age * 0.035f, a(CREAM, 0.55f * fade));
        for (int k = 0; k < 8; k++) {
            float ang = Mth.TWO_PI * k / 8f + age * 0.01f, ph = (age * 0.025f + k * 0.37f) % 1f;
            Vector3f base = new Vector3f(c).add(Mth.cos(ang) * R * 0.95f * open, 0.05f, Mth.sin(ang) * R * 0.95f * open);
            Vector3f top = new Vector3f(base).add(0, R * (0.5f + 0.9f * ph), 0);
            if (buf.hasBudget(4)) buf.beam(ctx, STREAK, VfxBlend.ADD, base, top, R * 0.14f, R * 0.04f, 1, 0f, a(GOLD, 0.6f * fade), a(GOLD, 0.0f));
        }
        for (int k = 0; k < 8; k++) {
            float tw = Mth.sin(age * 0.2f + k * 2.3f) * 0.5f + 0.5f;
            tw *= tw;
            float ang = hash(seed, k, 8) * Mth.TWO_PI, rr = R * 0.85f * hash(seed, k, 9);
            sprite(ctx, buf, GLINT, VfxBlend.ADD, new Vector3f(c).add(Mth.cos(ang) * rr, 0.2f + R * hash(seed, k, 10), Mth.sin(ang) * rr), R * (0.06f + 0.12f * tw), age * 0.03f, a(GOLD, 0.9f * tw * fade));
        }
    }

    // ------------------------------------------------------------------------------------------------ FX3
    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.progress(ctx.partialTick);
        float P = Math.max(0.5f, inst.power) * 1.3f;
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        int cream = VfxVertexBuffer.lerpColor(inst.color | 0xFF000000, CREAM, 0.6f);
        float out = 1f - sub(p, 0.55f, 1f);

        ground(buf, HALO, VfxBlend.ALPHA, new Vector3f(c).add(0, 0.07f, 0), P * (0.4f + 1.5f * VfxAnim.easeOutCubic(sub(p, 0f, 0.7f))), age * 0.05f, a(SILVER, 0.7f * out));
        // the two great wings opening in the plane of the camera, then folding
        float open = VfxAnim.easeOutBack(sub(p, 0.02f, 0.4f)) * (1f - 0.5f * sub(p, 0.6f, 1f));
        Vector3f body = new Vector3f(c).add(0, P * 0.35f, 0);
        wing(ctx, buf, body, P * 1.8f, open, -1, a(cream, 0.95f * out));
        wing(ctx, buf, body, P * 1.8f, open, 1, a(cream, 0.95f * out));
        for (int k = 0; k < 8; k++) {                                                            // scattered feathers drifting down
            float ang = hash(seed, k, 1) * Mth.TWO_PI, d = P * (0.5f + 1.0f * hash(seed, k, 2)) * VfxAnim.easeOutCubic(sub(p, 0f, 0.7f));
            float fall = P * 0.9f * sub(p, 0.2f, 1f) * hash(seed, k, 3);
            Vector3f pos = new Vector3f(c).add(Mth.cos(ang) * d, Math.max(0.05f, P * (0.5f + 0.5f * hash(seed, k, 4)) - fall), Mth.sin(ang) * d);
            sprite(ctx, buf, k % 2 == 0 ? FEATHER : DOWN, VfxBlend.ALPHA, pos, P * (0.3f + 0.2f * hash(seed, k, 5)), hash(seed, k, 6) * 6.28f + age * 0.1f, a(cream, 0.9f * out));
        }
        float fl = 1f - sub(p, 0f, 0.3f);
        if (fl > 0f) sprite(ctx, buf, GLINT, VfxBlend.ADD, body, P * (1.0f + 1.6f * (1f - fl)), 0f, a(GOLD, 0.9f * fl));
        sprite(ctx, buf, FEATHER, VfxBlend.ADD, body, P * (0.8f + 0.8f * sub(p, 0f, 0.5f)), 0f, a(GOLD, 0.35f * out));
        for (int k = 0; k < 7; k++) {
            float ang = hash(seed, k, 8) * Mth.TWO_PI, d = P * (0.3f + 0.9f * hash(seed, k, 9)) * VfxAnim.easeOutCubic(sub(p, 0f, 0.8f));
            float tw = Mth.sin(age * 0.5f + k * 2f) * 0.5f + 0.5f;
            sprite(ctx, buf, GLINT, VfxBlend.ADD, new Vector3f(c).add(Mth.cos(ang) * d, P * (0.2f + 0.8f * hash(seed, k, 10)), Mth.sin(ang) * d), P * (0.08f + 0.12f * tw), 0f, a(GOLD, 0.9f * out * tw));
        }
    }
}
