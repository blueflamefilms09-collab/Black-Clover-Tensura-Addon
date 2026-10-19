package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 1.93.0 Vortex Magic: everything turns and draws inward. Textures from tools/gen_vortex_rebuild.py (textures/particle/vortex_*.png). Sizes: VfxInstance
 * multiplies the payload power by 3 for FX1 / FX3 and by 1.5 for FX2, so {@code inst.power} is that product.
 * <ul>
 *   <li>VORTEX_FX1, Vortex Shot / Spiral Lance / Vortex Step / Spiral Gale (cast): a spiral disc turns in open at 'from' facing the way of the shot (the
 *       charge), then a drill runs to 'to': four spiral discs stacked along the way, each facing the flight and turning against its neighbour, a helix of
 *       wind streaks round them, curved gust blades peeling off, and dust whirled behind; where it ends a spiral blooms. 'from' = hand, 'to' = target.</li>
 *   <li>VORTEX_FX2, Whirlpool / Maelstrom pull / Great Vortex trail (zone): two spiral discs turn against each other on the ground over the real area, a
 *       funnel of three smaller discs rises above them narrowing to the middle, streaks wind inward, dust circles and glints twinkle. 'from' = centre,
 *       power = radius, fades in over 8 and out over 12 ticks.</li>
 *   <li>VORTEX_FX3, impact / implosion / call (burst): a spiral shock expands across the ground, a spiral faces the camera and unwinds, gust blades fly out
 *       in a ring, dust and glints. 'from' = centre.</li>
 * </ul>
 * Alpha blend for the dust, additive for the spirals, streaks and glints (drawn last). About 140 vertices of 400.
 */
public class VortexLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation SPIRAL = t("vortex_spiral"), RING = t("vortex_ring"), STREAK = t("vortex_streak"), CORE = t("vortex_core"), DUST = t("vortex_dust"), ARC = t("vortex_arc");

    private static final int TEAL = 0xFF59B8C9, PALE = 0xFFCFFAFF, DEEP = 0xFF2E86A0;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.VORTEX_FX1, VfxShape.VORTEX_FX2, VfxShape.VORTEX_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case VORTEX_FX1 -> 16;
            case VORTEX_FX2 -> 80;
            default -> 22;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFF59B8C9; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case VORTEX_FX1 -> cast(inst, ctx, buf);
            case VORTEX_FX2 -> zone(inst, ctx, buf);
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

    private static void disc(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f centre, Vector3f normal, float radius, float spin, int col) {
        if (buf.hasBudget(4)) buf.plane(tex, bl, VfxPose.facing(centre, normal).spin(spin), radius, col);
    }

    // ------------------------------------------------------------------------------------------------ FX1
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.progress(ctx.partialTick);
        float P = Math.max(0.5f, inst.power) * 1.2f;
        long seed = inst.seed;
        Vector3f f = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(f);
        float dist = dir.length();
        if (dist < 0.05f) dir.set(ctx.camRight); else dir.div(dist);
        Vector3f side = new Vector3f(dir).cross(ctx.camUp);
        if (side.lengthSquared() < 1e-4f) side.set(ctx.camRight);
        side.normalize();
        Vector3f up = new Vector3f(side).cross(dir).normalize();
        int teal = VfxVertexBuffer.lerpColor(inst.color | 0xFF000000, TEAL, 0.5f);

        float run = VfxAnim.easeInOutSine(sub(p, 0.12f, 0.85f)) * 0.35f + sub(p, 0.12f, 0.85f) * 0.65f;
        Vector3f head = lerp(f, to, run);
        float live = 1f - sub(p, 0.88f, 1f);
        float cp = sub(p, 0f, 0.25f), cf = 1f - sub(p, 0.2f, 0.45f);

        // 1. dust whirled up behind the drill (alpha)
        if (run > 0.02f && live > 0f) {
            for (int k = 0; k < 6; k++) {
                float tk = Math.max(0f, run - 0.06f * (k + 1));
                float ang = age * 0.4f + k * 1.3f, r = P * (0.2f + 0.08f * k);
                Vector3f sp = lerp(f, to, tk).add(new Vector3f(side).mul(Mth.cos(ang) * r)).add(new Vector3f(up).mul(Mth.sin(ang) * r));
                sprite(ctx, buf, DUST, VfxBlend.ALPHA, sp, P * (0.35f + 0.06f * k), age * 0.05f + k, a(PALE, 0.5f * live * (1f - 0.12f * k)));
            }
        }
        // 2. the drill: four spiral discs facing the flight, turning against each other (additive)
        if (run > 0.01f && live > 0f) {
            for (int k = 0; k < 4; k++) {
                float tk = Math.max(0f, run - 0.045f * k);
                Vector3f pos = lerp(f, to, tk);
                disc(buf, SPIRAL, VfxBlend.ADD, pos, dir, P * (0.85f - 0.14f * k), age * (0.5f + 0.1f * k) * (k % 2 == 0 ? 1f : -1f), a(k % 2 == 0 ? teal : PALE, (0.95f - 0.15f * k) * live));
            }
            sprite(ctx, buf, CORE, VfxBlend.ADD, head, P * 0.9f, 0f, a(PALE, 0.8f * live));
            for (int k = 0; k < 3; k++) {                                              // wind streaks winding round the drill
                float ph = age * 0.35f + k * 2.094f;
                Vector3f tail = lerp(f, to, Math.max(0f, run - 0.35f));
                Vector3f o0 = new Vector3f(tail).add(new Vector3f(side).mul(Mth.cos(ph + 2f) * P * 0.5f)).add(new Vector3f(up).mul(Mth.sin(ph + 2f) * P * 0.5f));
                Vector3f o1 = new Vector3f(head).add(new Vector3f(side).mul(Mth.cos(ph) * P * 0.4f)).add(new Vector3f(up).mul(Mth.sin(ph) * P * 0.4f));
                if (buf.hasBudget(4)) buf.beam(ctx, STREAK, VfxBlend.ADD, o0, o1, P * 0.3f, P * 0.1f, 1, 0f, a(teal, 0.0f), a(PALE, 0.8f * live));
            }
            for (int k = 0; k < 3; k++) {                                              // curved gust blades peeling off
                float tk = Math.max(0f, run - 0.08f * (k + 1));
                float ang = Mth.TWO_PI * hash(seed, k, 1);
                Vector3f bp = lerp(f, to, tk).add(new Vector3f(side).mul(Mth.cos(ang) * P * 0.8f)).add(new Vector3f(up).mul(Mth.sin(ang) * P * 0.8f));
                sprite(ctx, buf, ARC, VfxBlend.ADD, bp, P * 0.55f, ang + age * 0.3f, a(teal, 0.8f * live));
            }
        }
        // 3. the charge disc at 'from' and the bloom at 'to'
        if (cf > 0f) {
            disc(buf, SPIRAL, VfxBlend.ADD, f, dir, P * (1.5f - 0.6f * VfxAnim.easeOutCubic(cp)), age * 0.6f, a(teal, 0.9f * cf));
            disc(buf, SPIRAL, VfxBlend.ADD, f, dir, P * (0.9f - 0.3f * cp), -age * 0.9f, a(PALE, 0.8f * cf));
        }
        float bp = sub(p, 0.78f, 1f);
        if (bp > 0f) {
            disc(buf, SPIRAL, VfxBlend.ADD, to, dir, P * (0.5f + 1.7f * VfxAnim.easeOutCubic(bp)), age * 0.5f, a(teal, 0.85f * (1f - bp)));
            sprite(ctx, buf, CORE, VfxBlend.ADD, to, P * (0.6f + 0.8f * bp), 0f, a(PALE, 0.6f * (1f - bp)));
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
        int teal = VfxVertexBuffer.lerpColor(inst.color | 0xFF000000, TEAL, 0.5f);
        Vector3f upv = new Vector3f(0, 1, 0);

        // 1. dust circling low (alpha)
        int n = ctx.seg(10, 5);
        for (int k = 0; k < n; k++) {
            float ang = hash(seed, k, 1) * Mth.TWO_PI + age * (0.07f + 0.04f * hash(seed, k, 2)), rr = R * (0.3f + 0.65f * hash(seed, k, 3));
            float ph = (age * 0.015f + hash(seed, k, 4)) % 1f;
            sprite(ctx, buf, DUST, VfxBlend.ALPHA, new Vector3f(c).add(Mth.cos(ang) * rr, 0.2f + ph * R * 0.8f, Mth.sin(ang) * rr), R * (0.25f + 0.2f * ph), ang, a(PALE, Mth.sin(ph * Mth.PI) * 0.45f * fade));
        }
        // 2. the spirals: two on the ground turning against each other, a funnel of three above them (additive)
        disc(buf, SPIRAL, VfxBlend.ADD, new Vector3f(c).add(0, 0.05f, 0), upv, R * open, age * 0.09f, a(teal, 0.85f * fade));
        disc(buf, SPIRAL, VfxBlend.ADD, new Vector3f(c).add(0, 0.08f, 0), upv, R * 0.66f * open, -age * 0.14f, a(PALE, 0.6f * fade));
        for (int k = 0; k < 3; k++) {
            float h = R * (0.35f + 0.38f * k) * open, rad = R * (0.62f - 0.17f * k) * open;
            disc(buf, SPIRAL, VfxBlend.ADD, new Vector3f(c).add(0, h, 0), upv, rad, age * (0.12f + 0.04f * k) * (k % 2 == 0 ? 1f : -1f), a(k % 2 == 0 ? PALE : teal, (0.5f - 0.1f * k) * fade));
        }
        disc(buf, RING, VfxBlend.ADD, new Vector3f(c).add(0, 0.1f, 0), upv, R * open, age * 0.02f, a(teal, 0.6f * fade));
        // 3. streaks winding inward and glints
        for (int k = 0; k < 6; k++) {
            float ph = (age * 0.02f + k / 6f) % 1f, ang = Mth.TWO_PI * k / 6f + ph * 3.2f;
            float r0 = R * (1.0f - 0.85f * ph), r1 = R * (1.0f - 0.85f * Math.min(1f, ph + 0.18f)), a1 = ang + 0.55f;
            Vector3f p0 = new Vector3f(c).add(Mth.cos(ang) * r0, 0.25f + R * 0.5f * ph, Mth.sin(ang) * r0);
            Vector3f p1 = new Vector3f(c).add(Mth.cos(a1) * r1, 0.25f + R * 0.5f * Math.min(1f, ph + 0.18f), Mth.sin(a1) * r1);
            if (buf.hasBudget(4)) buf.beam(ctx, STREAK, VfxBlend.ADD, p0, p1, R * 0.22f, R * 0.06f, 1, 0f, a(teal, 0.0f), a(PALE, 0.8f * Mth.sin(ph * Mth.PI) * fade));
        }
        for (int k = 0; k < 6; k++) {
            float tw = Mth.sin(age * 0.2f + k * 2.3f) * 0.5f + 0.5f;
            tw *= tw;
            float ang = hash(seed, k, 8) * Mth.TWO_PI, rr = R * 0.8f * hash(seed, k, 9);
            sprite(ctx, buf, CORE, VfxBlend.ADD, new Vector3f(c).add(Mth.cos(ang) * rr, 0.2f + R * 0.7f * hash(seed, k, 10), Mth.sin(ang) * rr), R * (0.06f + 0.1f * tw), 0f, a(PALE, 0.9f * tw * fade));
        }
    }

    // ------------------------------------------------------------------------------------------------ FX3
    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.progress(ctx.partialTick);
        float P = Math.max(0.5f, inst.power) * 1.2f;
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        int teal = VfxVertexBuffer.lerpColor(inst.color | 0xFF000000, TEAL, 0.5f);
        float out = 1f - sub(p, 0.5f, 1f);
        Vector3f upv = new Vector3f(0, 1, 0);

        for (int k = 0; k < 6; k++) {                                                           // dust thrown out (alpha)
            float ang = hash(seed, k, 1) * Mth.TWO_PI, d = P * (0.3f + 0.9f * hash(seed, k, 2)) * VfxAnim.easeOutCubic(sub(p, 0f, 0.8f));
            sprite(ctx, buf, DUST, VfxBlend.ALPHA, new Vector3f(c).add(Mth.cos(ang) * d, P * 0.2f + P * 0.5f * sub(p, 0f, 1f), Mth.sin(ang) * d), P * (0.4f + 0.4f * hash(seed, k, 3)), ang, a(PALE, 0.5f * out));
        }
        float rp = VfxAnim.easeOutCubic(sub(p, 0f, 0.75f));
        disc(buf, SPIRAL, VfxBlend.ADD, new Vector3f(c).add(0, 0.06f, 0), upv, P * (0.3f + 1.7f * rp), age * 0.3f, a(teal, 0.9f * (1f - rp * 0.7f) * out));
        disc(buf, RING, VfxBlend.ADD, new Vector3f(c).add(0, 0.1f, 0), upv, P * (0.3f + 2.0f * rp), 0f, a(PALE, 0.7f * (1f - rp) * out));
        Vector3f mid = new Vector3f(c).add(0, P * 0.4f, 0);
        sprite(ctx, buf, SPIRAL, VfxBlend.ADD, mid, P * (0.6f + 1.6f * VfxAnim.easeOutCubic(sub(p, 0f, 0.6f))), -age * 0.35f, a(PALE, 0.8f * out));
        for (int k = 0; k < 6; k++) {                                                           // gust blades flying out in a ring
            float ang = Mth.TWO_PI * k / 6f + hash(seed, k, 4), d = P * (0.4f + 1.0f * VfxAnim.easeOutCubic(sub(p, 0f, 0.8f)));
            sprite(ctx, buf, ARC, VfxBlend.ADD, new Vector3f(c).add(Mth.cos(ang) * d, P * 0.35f, Mth.sin(ang) * d), P * 0.55f, -ang + 1.2f, a(teal, 0.85f * out));
        }
        float fl = 1f - sub(p, 0f, 0.25f);
        if (fl > 0f) sprite(ctx, buf, CORE, VfxBlend.ADD, mid, P * (0.8f + 1.4f * (1f - fl)), 0f, a(PALE, 0.9f * fl));
    }
}
