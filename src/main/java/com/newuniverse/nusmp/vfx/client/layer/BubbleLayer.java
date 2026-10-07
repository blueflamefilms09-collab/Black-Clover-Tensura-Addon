package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Bubble Magic (Bubble Refresher), drawn after the anime still: a giant clam shell heaped with soft, cel-shaded white foam (flat white
 * discs, a flat blue-grey crescent shadow, a thin dark-blue outline), small clear soap bubbles with a rim line and a glint floating round it.
 * The colour tint is mixed with the palette (foam stays near white, the film and the glow take the tint), so it reads as bubbles even when the tint is white.
 * <ul>
 *   <li>BUBBLE_FX1 (cast / projectile): a ring of clear bubbles is sucked into 'from' with a glint (first quarter), a foam bubble
 *       pops out and flies to 'to' on a shallow arc, spinning, with three small clear bubbles orbiting it, a soapy streak and a wobbling
 *       string of foam / clear bubbles as the afterimage (it lingers and thins out), then pops at 'to' (pop ring, small foam puff, droplets).
 *       power = bubble size (1 = a melon-sized head); duration = flight ticks.</li>
 *   <li>BUBBLE_FX2 (zone / field): the Refresher shell opens flat on the ground (centre 'from', radius = power) with a counter-rotating
 *       pair of bubble-bead rings at its rim, a heap of foam (a mound in the middle, a lumpy foam wall of 8 puffs on the rim, depth sorted),
 *       clear bubbles rising, a pulse ring every 30 ticks and glints. Fades in over 8 ticks, out over the last 12.</li>
 *   <li>BUBBLE_FX3 (impact / burst / signature): a white flash, a cloud of foam lumps and cel foam balls thrown outwards (biased along
 *       'to - from' when given), two counter-rotating pop rings plus a ground ring, clear bubbles drifting out and up, and a lingering sparkle afterglow.
 *       power = scale; duration = life ticks.</li>
 * </ul>
 */
public class BubbleLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation FOAM_BALL = t("bubble_foam_ball");
    public static final ResourceLocation FOAM_CLUSTER = t("bubble_foam_cluster");
    public static final ResourceLocation FOAM_PUFF = t("bubble_foam_puff");
    public static final ResourceLocation CLEAR = t("bubble_clear");
    public static final ResourceLocation SHELL = t("bubble_shell");
    public static final ResourceLocation BAND = t("bubble_band");
    public static final ResourceLocation POP = t("bubble_pop");
    public static final ResourceLocation GLINT = t("bubble_glint");
    public static final ResourceLocation TRAIL = t("bubble_trail");
    public static final ResourceLocation HALO = t("bubble_halo");

    private static final int WHITE = 0xFFFFFFFF;
    private static final int PALE = 0xFFA0D8FF;
    private static final int DEEP = 0xFF6A9AE0;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.BUBBLE_FX1, VfxShape.BUBBLE_FX2, VfxShape.BUBBLE_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case BUBBLE_FX1 -> 16;
            case BUBBLE_FX2 -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFA0D8FF; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case BUBBLE_FX1 -> cast(inst, ctx, buf);
            case BUBBLE_FX2 -> zone(inst, ctx, buf);
            case BUBBLE_FX3 -> burst(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ colours and small helpers
    private static int opaque(int c) { return c | 0xFF000000; }
    private static int foam(VfxInstance inst, float a) { return VfxVertexBuffer.withAlpha(VfxVertexBuffer.lerpColor(WHITE, opaque(inst.color), 0.16f), a); }
    private static int film(VfxInstance inst, float a) { return VfxVertexBuffer.withAlpha(VfxVertexBuffer.lerpColor(WHITE, opaque(inst.color), 0.30f), a); }
    private static int glow(VfxInstance inst, float a) { return VfxVertexBuffer.withAlpha(VfxVertexBuffer.lerpColor(PALE, opaque(inst.color), 0.35f), a); }
    private static int deep(VfxInstance inst, float a) { return VfxVertexBuffer.withAlpha(VfxVertexBuffer.lerpColor(DEEP, opaque(inst.color), 0.35f), a); }

    private static float sat(float x) { return Mth.clamp(x, 0f, 1f); }

    /** Random unit vector. */
    private static Vector3f dir(RandomSource r) {
        float u = r.nextFloat() * 2 - 1, p = r.nextFloat() * Mth.TWO_PI, s = Mth.sqrt(1 - u * u);
        return new Vector3f(s * Mth.cos(p), u, s * Mth.sin(p));
    }

    private static Vector3f lerp3(Vector3f a, Vector3f b, float t) { return new Vector3f(a).lerp(b, t); }

    /** A clear soap bubble with its glint, drawn as one quad. */
    private static void clearBubble(VfxRenderContext ctx, VfxVertexBuffer buf, VfxInstance inst, Vector3f p, float size, float rot, float a) {
        if (a <= 0.02f || size <= 0.005f) return;
        buf.billboard(ctx, CLEAR, VfxBlend.ALPHA, p, size, rot, film(inst, a));
    }

    // ------------------------------------------------------------------ FX1: cast / projectile
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = inst.duration;
        float pw = Mth.clamp(inst.power, 0.4f, 3.5f);
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        float dist = a.distance(b);
        float chargeEnd = dur * 0.25f, flightEnd = dur * 0.82f;
        float q = sat(age / chargeEnd);                                   // charge progress
        float h = VfxAnim.easeInOutSine(sat((age - chargeEnd) / (flightEnd - chargeEnd)));   // head progress
        float ip = sat((age - dur * 0.74f) / (dur * 0.26f + 0.001f));    // impact progress
        float outFade = 1f - sat((age - dur * 0.86f) / (dur * 0.14f + 0.001f));
        RandomSource r = inst.random();
        float spin = age * 0.25f;

        // 1. charge: clear bubbles are sucked into the hand, a glint blooms, the foam bubble swells
        if (age < dur * 0.62f) {
            float cf = 1f - sat((age - chargeEnd) / (dur * 0.37f));
            int n = ctx.seg(6, 4);
            for (int i = 0; i < n; i++) {
                float ph = r.nextFloat() * Mth.TWO_PI, rad = 0.55f + 0.5f * r.nextFloat(), lift = (r.nextFloat() - 0.5f) * 0.6f;
                float k = VfxAnim.easeInCubic(q);
                float ang = ph + age * 0.22f;
                Vector3f p = new Vector3f(Mth.cos(ang) * rad * pw * (1 - k), lift * pw * (1 - k) + 0.1f * pw * k, Mth.sin(ang) * rad * pw * (1 - k)).add(a);
                clearBubble(ctx, buf, inst, p, (0.16f + 0.1f * r.nextFloat()) * pw * (1 - 0.4f * k), ang, cf);
            }
            VfxBloom.glow(ctx, buf, a, 0.35f * pw * (0.4f + q), glow(inst, 1f), 0.9f * cf * (0.4f + q));
            buf.billboard(ctx, GLINT, VfxBlend.ADD, a, 0.9f * pw * q, spin * 0.5f, VfxVertexBuffer.withAlpha(WHITE, cf * q));
        }

        // 2. afterimage: the streak and the wobbling string of bubbles left on the path (it thins out after the head has landed)
        float headS = h;
        Vector3f head = lerp3(a, b, headS).add(0, 0.12f * dist * Mth.sin(Mth.PI * headS), 0);
        if (h > 0.01f) {
            Vector3f tail = lerp3(a, b, Math.max(0f, h - 0.4f)).add(0, 0.12f * dist * Mth.sin(Mth.PI * Math.max(0f, h - 0.4f)), 0);
            buf.beam(ctx, TRAIL, VfxBlend.ADD, tail, head, 0.05f * pw, 0.6f * pw, 3, -age * 0.05f,
                    glow(inst, 0.35f * outFade), glow(inst, 0.8f * outFade));
            int n = ctx.seg(12, 6);
            for (int i = 0; i < n; i++) {
                float s = h - (i + 1) * 0.04f - 0.02f;
                if (s < 0) break;
                float fall = 1f - i / (float) n;
                float wob = Mth.sin(i * 1.7f + age * 0.3f) * 0.2f * pw * (0.4f + i / (float) n);
                float settle = ip * 0.5f;
                Vector3f p = lerp3(a, b, s).add(0, 0.12f * dist * Mth.sin(Mth.PI * s) + settle * (i + 1) * 0.03f * pw, 0)
                        .add(new Vector3f(ctx.camRight).mul(wob)).add(new Vector3f(ctx.camUp).mul(Mth.cos(i * 1.3f + age * 0.27f) * 0.12f * pw * fall));
                float sz = (0.07f + 0.17f * fall) * pw;
                if ((i & 1) == 0) buf.billboard(ctx, FOAM_BALL, VfxBlend.ALPHA, p, sz, i * 0.9f, foam(inst, fall * outFade));
                else clearBubble(ctx, buf, inst, p, sz * 1.15f, i * 0.7f, (0.35f + 0.65f * fall) * outFade);
            }
        }

        // 3. the projectile: a foam bubble with a deeper puff behind it, a halo and orbiting clear bubbles
        float born = VfxAnim.easeOutBack(sat((age - chargeEnd * 0.4f) / (chargeEnd * 1.4f)));
        float alive = 1f - sat((age - flightEnd) / (dur * 0.08f + 0.001f));
        if (born > 0.02f && alive > 0.02f) {
            float sz = 0.62f * pw * born * (0.9f + 0.1f * Mth.sin(age * 0.8f));
            Vector3f pos = born < 1f && h < 0.02f ? new Vector3f(a) : head;
            buf.billboard(ctx, HALO, VfxBlend.ADD, pos, sz * 2.4f, 0f, glow(inst, 0.55f * alive));
            buf.billboard(ctx, FOAM_PUFF, VfxBlend.ALPHA, pos, sz * 1.25f, spin * 0.3f, foam(inst, alive));
            buf.billboard(ctx, FOAM_BALL, VfxBlend.ALPHA, pos, sz * 0.8f, -spin * 0.4f, foam(inst, alive));
            for (int i = 0; i < 3; i++) {
                float ang = spin * 0.7f + i * Mth.TWO_PI / 3f;
                Vector3f p = new Vector3f(pos).add(new Vector3f(ctx.camRight).mul(Mth.cos(ang) * sz * 0.85f))
                        .add(new Vector3f(ctx.camUp).mul(Mth.sin(ang) * sz * 0.5f));
                clearBubble(ctx, buf, inst, p, sz * 0.34f, ang, alive);
            }
            buf.billboard(ctx, GLINT, VfxBlend.ADD, pos, sz * 0.7f, spin, VfxVertexBuffer.withAlpha(WHITE, 0.8f * alive));
        }

        // 4. impact at 'to': flash, pop ring, a small foam puff, droplets
        if (ip > 0.01f) {
            float e = VfxAnim.easeOutCubic(ip), f = 1f - ip;
            VfxBloom.glow(ctx, buf, b, 0.4f * pw * (0.5f + e), glow(inst, 1f), 1.0f * f);
            buf.billboard(ctx, POP, VfxBlend.ADD, b, (0.5f + 1.7f * e) * pw, spin * 0.2f, glow(inst, 0.9f * f));
            buf.billboard(ctx, FOAM_PUFF, VfxBlend.ALPHA, b, (0.35f + 0.55f * e) * pw, 0.3f, foam(inst, sat(f * 1.4f)));
            int n = ctx.seg(6, 3);
            for (int i = 0; i < n; i++) {
                Vector3f d = dir(r);
                Vector3f p = new Vector3f(b).add(d.mul((0.3f + 0.9f * e) * pw)).add(0, 0.25f * ip * pw, 0);
                clearBubble(ctx, buf, inst, p, (0.12f + 0.1f * r.nextFloat()) * pw * (1f - 0.4f * ip), i, f);
            }
            buf.billboard(ctx, GLINT, VfxBlend.ADD, b, 1.1f * pw * f, ip * 2f, VfxVertexBuffer.withAlpha(WHITE, f));
        }
    }

    // ------------------------------------------------------------------ FX2: zone / field
    private record Blob(Vector3f p, float size, float rot, ResourceLocation tex, float alpha) {}

    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = inst.duration;
        float R = Math.max(0.6f, inst.power);
        float fade = Math.min(sat(age / 8f), sat((dur - age) / 12f));
        if (fade <= 0.01f) return;
        float open = VfxAnim.easeOutCubic(sat(age / 14f));
        RandomSource r = inst.random();
        float yaw = r.nextFloat() * Mth.TWO_PI;
        Vector3f c = ctx.rel(inst.from(ctx));
        VfxPose ground = VfxPose.ground(c).lift(0.03f);

        // ground: soft glow, the Refresher shell opened flat, the pulse
        buf.plane(HALO, VfxBlend.ADD, ground, R * 1.15f * open, glow(inst, 0.45f * fade));
        buf.plane(SHELL, VfxBlend.ALPHA, ground.lift(0.01f).spin(yaw), R * 0.92f * open, foam(inst, 0.92f * fade));

        // two counter-rotating rings of bubble beads at the rim and inside
        float w = Mth.clamp(0.08f * R, 0.22f, 0.6f);
        int rep = Math.max(2, Math.round(Mth.TWO_PI * R / (8f * w)));
        buf.ring(BAND, VfxBlend.ALPHA, ground.lift(0.02f).spin(age * 0.012f), R * 0.99f * open - w, R * 0.99f * open + w * 0.2f, ctx.seg(20, 10), rep, age * 0.004f, foam(inst, fade));
        int rep2 = Math.max(2, Math.round(Mth.TWO_PI * R * 0.62f / (8f * w * 0.8f)));
        buf.ring(BAND, VfxBlend.ALPHA, ground.lift(0.02f).spin(-age * 0.02f), R * 0.62f * open - w * 0.8f, R * 0.62f * open, ctx.seg(14, 8), rep2, -age * 0.006f, film(inst, 0.8f * fade));

        // foam heap: a mound in the middle plus a lumpy wall on the rim, sorted far to near
        List<Blob> blobs = new ArrayList<>();
        float bob = Mth.sin(age * 0.09f);
        blobs.add(new Blob(new Vector3f(c).add(0, R * 0.2f * open + 0.02f * bob, 0), R * 1.15f * open, 0, FOAM_CLUSTER, 1f));
        blobs.add(new Blob(new Vector3f(c).add(R * 0.12f, R * 0.34f * open, R * 0.06f), R * 0.7f * open * (1 + 0.03f * bob), 0.2f, FOAM_PUFF, 1f));
        blobs.add(new Blob(new Vector3f(c).add(-R * 0.14f, R * 0.30f * open, -R * 0.08f), R * 0.6f * open * (1 - 0.03f * bob), -0.3f, FOAM_CLUSTER, 1f));
        int wall = 8;
        for (int i = 0; i < wall; i++) {
            float ang = yaw + Mth.TWO_PI * i / wall + 0.15f * Mth.sin(i * 2.1f);
            float sz = R * (0.36f + 0.06f * Mth.sin(i * 3.7f)) * open * (1f + 0.05f * Mth.sin(age * 0.11f + i));
            Vector3f p = new Vector3f(Mth.cos(ang) * R * 0.86f * open, sz * 0.36f, Mth.sin(ang) * R * 0.86f * open).add(c);
            blobs.add(new Blob(p, sz, i * 0.8f, (i & 1) == 0 ? FOAM_PUFF : FOAM_CLUSTER, 1f));
        }
        blobs.sort((x, y) -> Float.compare(y.p().lengthSquared(), x.p().lengthSquared()));
        for (Blob bl : blobs) buf.billboard(ctx, bl.tex(), VfxBlend.ALPHA, bl.p(), bl.size(), bl.rot() * 0.15f, foam(inst, fade));

        // clear bubbles rising out of the foam
        int n = ctx.seg(22, 10);
        for (int i = 0; i < n; i++) {
            float ang = r.nextFloat() * Mth.TWO_PI, rad = Mth.sqrt(r.nextFloat()) * R * 0.95f, sp = 0.006f + 0.008f * r.nextFloat(), off = r.nextFloat();
            float sz = (0.14f + 0.22f * r.nextFloat()) * Mth.clamp(R * 0.45f, 0.8f, 2f);
            float life = (age * sp + off) % 1f;
            float vis = Math.min(sat(life * 6f), sat((1f - life) * 3f)) * fade;
            Vector3f p = new Vector3f(Mth.cos(ang) * rad + 0.12f * Mth.sin(age * 0.13f + i), life * R * 1.1f + R * 0.1f, Mth.sin(ang) * rad + 0.12f * Mth.cos(age * 0.11f + i)).add(c);
            clearBubble(ctx, buf, inst, p, sz, i * 0.6f + age * 0.02f, vis);
        }

        // pulse every 30 ticks, and glints on the foam
        float pu = (age % 30f) / 30f;
        buf.plane(POP, VfxBlend.ADD, ground.lift(0.03f).spin(age * 0.03f), R * (0.2f + 1.0f * VfxAnim.easeOutCubic(pu)), glow(inst, 0.7f * (1f - pu) * fade * open));
        int g = ctx.seg(4, 2);
        for (int i = 0; i < g; i++) {
            float ang = r.nextFloat() * Mth.TWO_PI, rad = R * (0.2f + 0.6f * r.nextFloat());
            float tw = Mth.sin(age * (0.14f + 0.08f * r.nextFloat()) + r.nextFloat() * 6f);
            tw = tw * tw * tw;
            if (tw <= 0.05f) continue;
            Vector3f p = new Vector3f(Mth.cos(ang) * rad, R * (0.25f + 0.25f * r.nextFloat()) * open, Mth.sin(ang) * rad).add(c);
            buf.billboard(ctx, GLINT, VfxBlend.ADD, p, R * 0.3f * (0.6f + tw), age * 0.03f, VfxVertexBuffer.withAlpha(WHITE, tw * fade));
        }
    }

    // ------------------------------------------------------------------ FX3: impact / burst
    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = inst.duration;
        float t = sat(age / dur);
        float S = Mth.clamp(inst.power, 0.4f, 4f);
        RandomSource r = inst.random();
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f hint = ctx.rel(inst.to(ctx)).sub(c);
        boolean has = hint.lengthSquared() > 0.04f;
        if (has) hint.normalize();
        float e = VfxAnim.easeOutCubic(sat(t * 1.5f));
        float out = 1f - sat((t - 0.55f) / 0.45f);       // foam leaves slowly
        float flash = 1f - sat(age / 6f);
        VfxPose ground = VfxPose.ground(c).lift(0.03f);

        // 1. ground ring and the glow behind everything
        buf.plane(POP, VfxBlend.ADD, ground.spin(age * 0.05f), S * (0.4f + 2.0f * e), glow(inst, 0.8f * (1f - t)));
        buf.billboard(ctx, HALO, VfxBlend.ADD, c, S * (1.2f + 1.8f * e), 0f, glow(inst, 0.5f * (1f - t * t)));

        // 2. foam thrown out: big lumps first, then small cel balls flying further
        List<Blob> blobs = new ArrayList<>();
        int lumps = ctx.seg(6, 3);
        for (int i = 0; i < lumps; i++) {
            Vector3f d = dir(r);
            if (has) d.add(new Vector3f(hint).mul(0.9f)).normalize();
            d.y = d.y * 0.6f + 0.15f;
            float sp = (0.7f + 0.6f * r.nextFloat()) * S * 0.75f;
            Vector3f p = new Vector3f(d).mul(sp * e).add(c).add(0, 0.5f * S * e * t, 0);
            float sz = S * (0.8f + 0.5f * r.nextFloat()) * (0.4f + 0.6f * e) * (1f - 0.25f * t);
            blobs.add(new Blob(p, sz, r.nextFloat() * 6f, (i & 1) == 0 ? FOAM_CLUSTER : FOAM_PUFF, 1f));
        }
        blobs.sort((x, y) -> Float.compare(y.p().lengthSquared(), x.p().lengthSquared()));
        for (Blob bl : blobs) buf.billboard(ctx, bl.tex(), VfxBlend.ALPHA, bl.p(), bl.size(), bl.rot() * 0.2f + age * 0.01f, foam(inst, out));

        int balls = ctx.seg(8, 4);
        for (int i = 0; i < balls; i++) {
            Vector3f d = dir(r);
            if (has) d.add(new Vector3f(hint).mul(1.1f)).normalize();
            float sp = (1.0f + 0.9f * r.nextFloat()) * S;
            Vector3f p = new Vector3f(d).mul(sp * e).add(c).add(0, 0.35f * S * t, 0);
            float sz = S * (0.12f + 0.2f * r.nextFloat()) * (1f - 0.3f * t);
            buf.billboard(ctx, FOAM_BALL, VfxBlend.ALPHA, p, sz, r.nextFloat() * 6f, foam(inst, out));
        }

        // 3. clear bubbles drifting out and up
        int cb = ctx.seg(12, 5);
        for (int i = 0; i < cb; i++) {
            Vector3f d = dir(r);
            float sp = (0.6f + 1.4f * r.nextFloat()) * S;
            Vector3f p = new Vector3f(d).mul(sp * e).add(c).add(0, S * 0.9f * t * t + 0.1f * Mth.sin(age * 0.2f + i), 0);
            float sz = S * (0.12f + 0.2f * r.nextFloat());
            clearBubble(ctx, buf, inst, p, sz, i, 1f - sat((t - 0.6f) / 0.4f));
        }

        // 4. the big payoff: flash and two counter-rotating pop rings
        if (flash > 0.01f) VfxBloom.glow(ctx, buf, c, S * 0.9f, glow(inst, 1f), flash);
        buf.billboard(ctx, POP, VfxBlend.ADD, c, S * (0.7f + 3.0f * e), age * 0.04f, VfxVertexBuffer.withAlpha(WHITE, 0.9f * (1f - sat(t * 1.3f))));
        buf.billboard(ctx, POP, VfxBlend.ADD, c, S * (0.4f + 2.0f * e), -age * 0.07f, deep(inst, 0.8f * (1f - sat(t * 1.6f))));

        // 5. afterglow: lingering glints
        int g = ctx.seg(5, 3);
        for (int i = 0; i < g; i++) {
            Vector3f d = dir(r);
            float rad = S * (0.4f + 0.9f * r.nextFloat());
            float tw = Mth.sin(age * (0.18f + 0.1f * r.nextFloat()) + r.nextFloat() * 6f);
            tw = tw * tw * tw;
            if (tw <= 0.05f || t < 0.25f) continue;
            Vector3f p = new Vector3f(d).mul(rad).add(c).add(0, S * 0.4f * t, 0);
            buf.billboard(ctx, GLINT, VfxBlend.ADD, p, S * 0.4f * (0.5f + tw), age * 0.05f, VfxVertexBuffer.withAlpha(WHITE, tw * (1f - t)));
        }
    }
}
