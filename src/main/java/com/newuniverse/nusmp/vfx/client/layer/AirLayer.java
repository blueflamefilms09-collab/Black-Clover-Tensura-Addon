package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 1.93.0 Air Magic: air made visible, never solid: thin ripples, speed lines, loose swirls and cut crescents, all additive. Textures from
 * tools/gen_air_rebuild.py (textures/particle/air_*.png). Sizes: VfxInstance multiplies the payload power by 3 for FX1 / FX3 and by 1.5 for FX2, so
 * {@code inst.power} is that product.
 * <ul>
 *   <li>AIR_FX1, Air Bullet / Blade / Step (cast): at 'from' a swirl winds up facing the shot (the charge), then a lance of compressed air runs to 'to': a stack
 *       of six ripple rings facing the flight (the shock cone of the bullet), a bright core, long speed lines racing past it and a pair of cut crescents; where it
 *       ends a ripple bursts. 'from' = hand, 'to' = target.</li>
 *   <li>AIR_FX2, Dead Air / Vacuum Seal (zone): ripples pulse outward from the middle of the real area on the ground, two vertical swirls turn inside it,
 *       speed lines race inward and up, glints twinkle. 'from' = centre, power = radius, fades in over 8 and out over 12 ticks.</li>
 *   <li>AIR_FX3, impact / burst / pressure burst (burst): a bright flash, three rings running out across the ground and one facing the camera, speed lines
 *       flying radially, cut crescents thrown out, glints. 'from' = centre.</li>
 * </ul>
 * Everything is additive (RenderType-free: VfxBlend.ADD), so nothing hides the target. About 110 vertices of 400.
 */
public class AirLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation RING = t("air_ring"), STREAK = t("air_streak"), SWIRL = t("air_swirl"), ORB = t("air_orb"), SLASH = t("air_slash"), GLINT = t("air_glint");

    private static final int CYAN = 0xFF9FE8FF, WHITE = 0xFFF4FFFF, DEEP = 0xFF54B8E0;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.AIR_FX1, VfxShape.AIR_FX2, VfxShape.AIR_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case AIR_FX1 -> 16;
            case AIR_FX2 -> 80;
            default -> 22;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFF9FE8FF; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case AIR_FX1 -> cast(inst, ctx, buf);
            case AIR_FX2 -> zone(inst, ctx, buf);
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

    private static void sprite(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, Vector3f p, float size, float rot, int col) {
        if (buf.hasBudget(4)) buf.billboard(ctx, tex, VfxBlend.ADD, p, size, rot, col);
    }

    private static void disc(VfxVertexBuffer buf, ResourceLocation tex, Vector3f centre, Vector3f normal, float radius, float spin, int col) {
        if (buf.hasBudget(4)) buf.plane(tex, VfxBlend.ADD, VfxPose.facing(centre, normal).spin(spin), radius, col);
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
        int cyan = VfxVertexBuffer.lerpColor(inst.color | 0xFF000000, CYAN, 0.5f);

        float run = VfxAnim.easeInOutSine(sub(p, 0.1f, 0.85f)) * 0.3f + sub(p, 0.1f, 0.85f) * 0.7f;
        Vector3f head = lerp(f, to, run);
        float live = 1f - sub(p, 0.88f, 1f);
        float cp = sub(p, 0f, 0.22f), cf = 1f - sub(p, 0.18f, 0.4f);

        if (cf > 0f) {                                                                    // the charge: a swirl winding up facing the shot
            disc(buf, SWIRL, f, dir, P * (1.3f - 0.5f * VfxAnim.easeOutCubic(cp)), age * 0.7f, a(cyan, 0.85f * cf));
            sprite(ctx, buf, ORB, f, P * (0.4f + 0.9f * cp), 0f, a(WHITE, 0.8f * cf));
        }
        if (run > 0.01f && live > 0f) {
            for (int k = 0; k < 6; k++) {                                                 // the shock cone: six ripple rings, widening behind the head
                float tk = Math.max(0f, run - 0.04f * k);
                disc(buf, RING, lerp(f, to, tk), dir, P * (0.35f + 0.16f * k), 0f, a(k % 2 == 0 ? cyan : WHITE, (0.85f - 0.12f * k) * live));
            }
            sprite(ctx, buf, ORB, head, P * 0.85f, 0f, a(WHITE, 0.9f * live));
            for (int k = 0; k < 4; k++) {                                                 // speed lines racing past
                float off = (hash(seed, k, 1) - 0.5f) * 2f, ang = hash(seed, k, 2) * Mth.TWO_PI;
                Vector3f o = new Vector3f(side).mul(Mth.cos(ang) * P * 0.7f * (0.4f + Math.abs(off))).add(new Vector3f(up).mul(Mth.sin(ang) * P * 0.7f * (0.4f + Math.abs(off))));
                Vector3f p0 = lerp(f, to, Math.max(0f, run - 0.5f)).add(o), p1 = lerp(f, to, run + 0.05f).add(o);
                if (buf.hasBudget(4)) buf.beam(ctx, STREAK, VfxBlend.ADD, p0, p1, P * 0.18f, P * 0.04f, 1, 0f, a(cyan, 0.0f), a(WHITE, 0.8f * live));
            }
            for (int k = -1; k <= 1; k += 2) {                                            // a pair of cut crescents either side
                Vector3f cp2 = new Vector3f(head).add(new Vector3f(side).mul(k * P * 0.7f)).add(new Vector3f(dir).mul(-P * 0.3f));
                sprite(ctx, buf, SLASH, cp2, P * 0.9f, (float) Math.atan2(dir.dot(ctx.camUp), dir.dot(ctx.camRight)) + k * 0.5f, a(cyan, 0.7f * live));
            }
        }
        float bp = sub(p, 0.78f, 1f);
        if (bp > 0f) {
            disc(buf, RING, to, dir, P * (0.4f + 1.6f * VfxAnim.easeOutCubic(bp)), 0f, a(WHITE, 0.8f * (1f - bp)));
            disc(buf, SWIRL, to, dir, P * (0.3f + 1.2f * bp), age * 0.5f, a(cyan, 0.6f * (1f - bp)));
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
        int cyan = VfxVertexBuffer.lerpColor(inst.color | 0xFF000000, CYAN, 0.5f);
        Vector3f upv = new Vector3f(0, 1, 0);

        for (int k = 0; k < 3; k++) {                                                      // ripples pulsing out from the middle
            float ph = (age * 0.022f + k / 3f) % 1f;
            disc(buf, RING, new Vector3f(c).add(0, 0.05f + 0.02f * k, 0), upv, R * (0.15f + 0.85f * ph) * open, 0f, a(k == 1 ? WHITE : cyan, (1f - ph) * 0.8f * fade));
        }
        disc(buf, RING, new Vector3f(c).add(0, 0.1f, 0), upv, R * open, 0f, a(DEEP, 0.7f * fade));
        for (int k = 0; k < 2; k++) {                                                      // two vertical swirls turning inside the area
            float ang = age * 0.04f * (k == 0 ? 1f : -1f) + k * Mth.HALF_PI;
            Vector3f nrm = new Vector3f(Mth.cos(ang), 0f, Mth.sin(ang));
            disc(buf, SWIRL, new Vector3f(c).add(0, R * 0.55f * open, 0), nrm, R * 0.75f * open, age * 0.06f * (k == 0 ? 1f : -1f), a(k == 0 ? cyan : WHITE, 0.45f * fade));
        }
        for (int k = 0; k < 8; k++) {                                                      // speed lines racing inward and up
            float ph = (age * 0.03f + k / 8f) % 1f, ang = Mth.TWO_PI * k / 8f + age * 0.01f;
            float r0 = R * (1f - 0.9f * ph), r1 = R * (1f - 0.9f * Math.min(1f, ph + 0.15f));
            Vector3f p0 = new Vector3f(c).add(Mth.cos(ang) * r0, 0.2f + R * 0.6f * ph, Mth.sin(ang) * r0);
            Vector3f p1 = new Vector3f(c).add(Mth.cos(ang) * r1, 0.2f + R * 0.6f * Math.min(1f, ph + 0.15f), Mth.sin(ang) * r1);
            if (buf.hasBudget(4)) buf.beam(ctx, STREAK, VfxBlend.ADD, p0, p1, R * 0.16f, R * 0.04f, 1, 0f, a(cyan, 0.0f), a(WHITE, 0.8f * Mth.sin(ph * Mth.PI) * fade));
        }
        for (int k = 0; k < 6; k++) {
            float tw = Mth.sin(age * 0.2f + k * 2.3f) * 0.5f + 0.5f;
            tw *= tw;
            float ang = hash(seed, k, 8) * Mth.TWO_PI, rr = R * 0.85f * hash(seed, k, 9);
            sprite(ctx, buf, GLINT, new Vector3f(c).add(Mth.cos(ang) * rr, 0.2f + R * 0.8f * hash(seed, k, 10), Mth.sin(ang) * rr), R * (0.05f + 0.1f * tw), 0f, a(WHITE, 0.9f * tw * fade));
        }
    }

    // ------------------------------------------------------------------------------------------------ FX3
    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.progress(ctx.partialTick);
        float P = Math.max(0.5f, inst.power) * 1.2f;
        long seed = inst.seed;
        Vector3f c = ctx.rel(inst.from(ctx));
        int cyan = VfxVertexBuffer.lerpColor(inst.color | 0xFF000000, CYAN, 0.5f);
        float out = 1f - sub(p, 0.5f, 1f);
        Vector3f upv = new Vector3f(0, 1, 0);
        Vector3f mid = new Vector3f(c).add(0, P * 0.35f, 0);

        for (int k = 0; k < 3; k++) {                                                      // three rings running out across the ground
            float rp = VfxAnim.easeOutCubic(sub(p, 0.06f * k, 0.7f + 0.06f * k));
            disc(buf, RING, new Vector3f(c).add(0, 0.05f + 0.02f * k, 0), upv, P * (0.3f + 1.9f * rp), 0f, a(k == 1 ? WHITE : cyan, (1f - rp * 0.85f) * 0.85f * out));
        }
        Vector3f toCam = new Vector3f(ctx.camRight).cross(ctx.camUp);
        disc(buf, RING, mid, toCam, P * (0.3f + 1.5f * VfxAnim.easeOutCubic(sub(p, 0f, 0.6f))), 0f, a(WHITE, 0.3f * out));
        disc(buf, SWIRL, mid, toCam, P * (0.4f + 1.0f * sub(p, 0f, 0.7f)), age * 0.4f, a(cyan, 0.55f * out));
        for (int k = 0; k < 8; k++) {                                                      // speed lines flying radially
            float ang = Mth.TWO_PI * k / 8f + hash(seed, k, 1) * 0.5f, e0 = VfxAnim.easeOutCubic(sub(p, 0f, 0.7f));
            Vector3f d = new Vector3f(Mth.cos(ang), 0.15f * (hash(seed, k, 2) - 0.3f), Mth.sin(ang));
            Vector3f p0 = new Vector3f(mid).add(new Vector3f(d).mul(P * (0.2f + 0.9f * e0))), p1 = new Vector3f(mid).add(new Vector3f(d).mul(P * (0.5f + 1.5f * e0)));
            if (buf.hasBudget(4)) buf.beam(ctx, STREAK, VfxBlend.ADD, p0, p1, P * 0.14f, P * 0.03f, 1, 0f, a(cyan, 0.0f), a(WHITE, 0.8f * out));
        }
        for (int k = 0; k < 4; k++) {                                                      // cut crescents thrown out
            float ang = Mth.TWO_PI * k / 4f + hash(seed, k, 3), d = P * (0.4f + 1.1f * VfxAnim.easeOutCubic(sub(p, 0f, 0.8f)));
            sprite(ctx, buf, SLASH, new Vector3f(mid).add(Mth.cos(ang) * d, 0.1f * P, Mth.sin(ang) * d), P * 0.8f, -ang + 1.2f, a(cyan, 0.7f * out));
        }
        float fl = 1f - sub(p, 0f, 0.25f);
        if (fl > 0f) sprite(ctx, buf, ORB, mid, P * (0.8f + 1.4f * (1f - fl)), 0f, a(WHITE, 0.9f * fl));
        for (int k = 0; k < 5; k++) {
            float ang = hash(seed, k, 8) * Mth.TWO_PI, d = P * (0.3f + 0.9f * hash(seed, k, 9)) * VfxAnim.easeOutCubic(sub(p, 0f, 0.8f));
            float tw = Mth.sin(age * 0.5f + k * 2f) * 0.5f + 0.5f;
            sprite(ctx, buf, GLINT, new Vector3f(mid).add(Mth.cos(ang) * d, P * (hash(seed, k, 10) - 0.3f), Mth.sin(ang) * d), P * (0.07f + 0.1f * tw), 0f, a(WHITE, 0.9f * out * tw));
        }
    }
}
