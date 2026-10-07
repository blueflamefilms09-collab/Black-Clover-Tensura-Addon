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
 * Demon Water Magic (Hydra of Darkneros), drawn after the owner's concept art: pale scaled serpents with spiny ridges and open maws
 * full of fangs, heavy black linework, bubbles drifting up, ink-black water lit teal-green.
 * <ul>
 *   <li>DEMON_WATER_FX1 (cast / projectile): a fanged maw sigil opens at 'from' while bubbles gather, then two scaled water serpents
 *       (a coiling pair, one thicker, one thinner, counter-swaying) race from 'from' to 'to' with a flowing teal afterimage; each
 *       serpent head has an open maw and glowing eyes. Ends in a ripple, an ink splash and a flash at 'to'. power = size scale,
 *       duration = flight ticks (default 16), colour tints the teal light.</li>
 *   <li>DEMON_WATER_FX2 (zone / field): an ink pool with a ragged tar rim on the ground, a fang-ring sigil turning one way with a
 *       smaller one counter-rotating, ripple pulses running outwards, a rim of black thorns, and a crown of hydra necks (4 to 7)
 *       rising around the edge, swaying and snapping their heads inwards, bubbles lifting from the pool. 'from' = centre on the
 *       ground, power = radius in blocks, duration = life ticks (default 80); fades in over 8 and out over 12.</li>
 *   <li>DEMON_WATER_FX3 (impact / burst): white-teal flash, ripples racing over the ground, a camera-facing fang ring, one big
 *       hydra head lunging out of the water along 'to - from' (up when zero) with its neck, ink splashes, thorns and droplets
 *       thrown out, and a lingering teal afterglow with rising bubbles. power = scale, duration = life ticks (default 28).</li>
 * </ul>
 */
public class DemonWaterLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation HEAD = t("demon_water_head");
    public static final ResourceLocation SCALES = t("demon_water_scales");
    public static final ResourceLocation SCALE_GLOW = t("demon_water_scale_glow");
    public static final ResourceLocation FANGRING = t("demon_water_fangring");
    public static final ResourceLocation BUBBLE = t("demon_water_bubble");
    public static final ResourceLocation RIPPLE = t("demon_water_ripple");
    public static final ResourceLocation POOL = t("demon_water_pool");
    public static final ResourceLocation SPLASH = t("demon_water_splash");
    public static final ResourceLocation SPINE = t("demon_water_spine");
    public static final ResourceLocation STREAK = t("demon_water_streak");

    private static final int TEAL = 0xFF20D0A0;
    private static final int BODY = 0xFF1C4C50;
    private static final int INK = 0xFF16363A;
    private static final int PALE = 0xFFC8FFF0;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.DEMON_WATER_FX1, VfxShape.DEMON_WATER_FX2, VfxShape.DEMON_WATER_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case DEMON_WATER_FX1 -> 16;
            case DEMON_WATER_FX2 -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return TEAL; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case DEMON_WATER_FX1 -> cast(inst, ctx, buf);
            case DEMON_WATER_FX2 -> zone(inst, ctx, buf);
            case DEMON_WATER_FX3 -> impact(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ colours and curves
    /** The magic's light: its teal, pulled a little towards the caster's tint. */
    private static int light(VfxInstance inst) { return VfxVertexBuffer.lerpColor(TEAL, inst.color | 0xFF000000, 0.3f) | 0xFF000000; }

    private static int body(VfxInstance inst) { return VfxVertexBuffer.lerpColor(BODY, inst.color | 0xFF000000, 0.12f) | 0xFF000000; }

    private static float life(VfxInstance inst, float age, float in, float out) {
        float a = in <= 0 ? 1 : Mth.clamp(age / in, 0, 1);
        float b = out <= 0 ? 1 : Mth.clamp((inst.duration - age) / out, 0, 1);
        return Math.min(a, b);
    }

    private static float sat(float v) { return Mth.clamp(v, 0f, 1f); }

    // ------------------------------------------------------------------ primitives
    /** A unit vector in the view plane, perpendicular to axis (the direction serpents sway in). */
    private static Vector3f sway(VfxRenderContext ctx, Vector3f axis) {
        Vector3f fwd = new Vector3f(ctx.camRight).cross(ctx.camUp);
        Vector3f p = new Vector3f(axis).cross(fwd);
        if (p.lengthSquared() < 1e-4f) return new Vector3f(ctx.camRight);
        return p.normalize();
    }

    /** Points of a swaying serpent from base along axis; the sway grows from zero at the base. */
    private static Vector3f[] path(VfxRenderContext ctx, Vector3f base, Vector3f axis, float len, float amp, float phase, float age, float speed, int n) {
        Vector3f perp = sway(ctx, axis);
        Vector3f[] p = new Vector3f[n + 1];
        for (int i = 0; i <= n; i++) {
            float u = (float) i / n;
            float w = amp * Mth.sin(phase + u * 4.6f - age * speed) * sat(u * 1.6f);
            p[i] = new Vector3f(axis).mul(len * u).add(new Vector3f(perp).mul(w)).add(base);
        }
        return p;
    }

    /**
     * A ribbon along the points, widths w0 -> w1, alpha tail -> head, texture running along the length (scale tips point to the tail),
     * scrolling by 'scroll'. Each segment is one quad.
     */
    private static void ribbon(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f[] p, float w0, float w1,
                               int argb, float aTail, float aHead, float scroll) {
        float dist = 0;
        for (int i = 0; i < p.length - 1; i++) {
            Vector3f dir = new Vector3f(p[i + 1]).sub(p[i]);
            float len = dir.length();
            if (len < 1e-5f) continue;
            Vector3f mid = new Vector3f(p[i]).add(p[i + 1]).mul(0.5f);
            Vector3f side = new Vector3f(dir).cross(new Vector3f(mid).negate().normalize());
            if (side.lengthSquared() < 1e-8f) continue;
            side.normalize();
            float t0 = (float) i / (p.length - 1), t1 = (float) (i + 1) / (p.length - 1);
            float h0 = Mth.lerp(t0, w0, w1) * 0.5f, h1 = Mth.lerp(t1, w0, w1) * 0.5f;
            float s0 = scroll - dist * 0.5f / Math.max(0.05f, Mth.lerp(t0, w0, w1));
            dist += len;
            float s1 = scroll - dist * 0.5f / Math.max(0.05f, Mth.lerp(t1, w0, w1));
            int c0 = VfxVertexBuffer.withAlpha(argb, Mth.lerp(t0, aTail, aHead)), c1 = VfxVertexBuffer.withAlpha(argb, Mth.lerp(t1, aTail, aHead));
            buf.quad(tex, blend,
                    new Vector3f(p[i]).sub(new Vector3f(side).mul(h0)), new Vector3f(p[i]).add(new Vector3f(side).mul(h0)),
                    new Vector3f(p[i + 1]).add(new Vector3f(side).mul(h1)), new Vector3f(p[i + 1]).sub(new Vector3f(side).mul(h1)),
                    0, s1, 1, s0, c0, c1);
        }
    }

    /** One hydra head: base of the neck at 'base', pointing along dir, 'len' long (texture is 1:2). Dark skin, then a lit overlay of fangs and eyes. */
    private static void head(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f base, Vector3f dir, float len, int skin, int glow, float alpha) {
        if (alpha <= 0.02f || len <= 0.01f) return;
        Vector3f tip = new Vector3f(dir).normalize(len).add(base);
        buf.beam(ctx, HEAD, VfxBlend.ALPHA, base, tip, len * 0.5f, len * 0.5f, 1, 0, VfxVertexBuffer.withAlpha(skin, alpha), VfxVertexBuffer.withAlpha(skin, alpha));
        buf.beam(ctx, HEAD, VfxBlend.ADD, base, tip, len * 0.5f, len * 0.5f, 1, 0, VfxVertexBuffer.withAlpha(glow, 0.55f * alpha), VfxVertexBuffer.withAlpha(glow, 0.55f * alpha));
    }

    private static void bubble(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f pos, float size, int argb, float alpha) {
        if (alpha <= 0.02f) return;
        buf.billboard(ctx, BUBBLE, VfxBlend.ADD, pos, size, 0, VfxVertexBuffer.withAlpha(argb, alpha));
    }

    private static Vector3f dirOf(VfxInstance inst, VfxRenderContext ctx, Vector3f fallback) {
        Vector3f d = new Vector3f(ctx.rel(inst.to(ctx))).sub(ctx.rel(inst.from(ctx)));
        if (d.lengthSquared() < 1e-4f) return new Vector3f(fallback);
        return d.normalize();
    }

    // ------------------------------------------------------------------ FX1: serpents in flight
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float t = inst.progress(ctx.partialTick);
        float P = Math.max(0.4f, inst.power);
        int lit = light(inst), skin = body(inst);
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f path = new Vector3f(to).sub(from);
        float total = path.length();
        if (total < 0.05f) return;
        Vector3f dir = new Vector3f(path).div(total);

        float charge = VfxAnim.easeOutCubic(sat(t / 0.25f));
        float flyT = sat((t - 0.12f) / 0.72f);
        float fly = VfxAnim.easeInOutSine(flyT) * 0.35f + flyT * 0.65f;
        Vector3f headPos = new Vector3f(dir).mul(total * fly).add(from);

        // 1. the maw sigil opening at the caster, closing as the serpents leave
        float sig = charge * (1f - sat((t - 0.3f) / 0.25f));
        if (sig > 0.02f) {
            VfxPose pose = VfxPose.facing(new Vector3f(dir).mul(0.25f * P).add(from), dir);
            buf.plane(FANGRING, VfxBlend.ADD, pose.spin(age * 0.25f), 0.75f * P * (0.4f + 0.6f * charge), VfxVertexBuffer.withAlpha(lit, 0.9f * sig));
            buf.plane(FANGRING, VfxBlend.ADD, pose.lift(0.04f).spin(-age * 0.4f), 0.4f * P * charge, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(lit, 0.5f), 0.8f * sig));
            VfxBloom.glow(ctx, buf, from, 0.5f * P * charge, lit, sig);
        }
        // bubbles drawn into the sigil
        RandomSource r = inst.random();
        for (int i = 0; i < 4; i++) {
            float a = r.nextFloat() * Mth.TWO_PI, rr = 0.5f + r.nextFloat() * 0.7f;
            float k = sat(t / 0.3f);
            Vector3f side = sway(ctx, dir);
            Vector3f q = new Vector3f(side).mul(Mth.cos(a) * rr * P * (1 - k)).add(new Vector3f(ctx.camUp).mul(Mth.sin(a) * rr * P * (1 - k))).add(from);
            bubble(ctx, buf, q, (0.14f + 0.1f * r.nextFloat()) * P, lit, (1 - k) * 0.9f * sat(t * 8));
        }

        if (flyT > 0f) {
            float trail = Math.min(total * fly, 3.4f * P);
            float fade = 1f - sat((t - 0.88f) / 0.12f);
            // 2. the flowing afterimage along the whole flight so far
            Vector3f tail = new Vector3f(headPos).sub(new Vector3f(dir).mul(Math.min(total * fly, 6f * P)));
            buf.beam(ctx, STREAK, VfxBlend.ADD, tail, headPos, 0.9f * P, 0.35f * P, 3, -age * 0.12f,
                    VfxVertexBuffer.withAlpha(lit, 0f), VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(lit, 0.3f), 0.75f * fade));
            // 3. a coiling pair of serpents: thick and thin, swaying against each other
            for (int k = 0; k < 2; k++) {
                float w1 = (k == 0 ? 0.32f : 0.22f) * P;
                float w0 = w1 * 1.7f;
                Vector3f base = new Vector3f(headPos).sub(new Vector3f(dir).mul(trail));
                Vector3f[] pts = path(ctx, base, dir, trail, (k == 0 ? 0.6f : 0.5f) * P, k * Mth.PI, age, 0.55f, 6);
                // the head sits on the last point
                ribbon(ctx, buf, SCALES, VfxBlend.ALPHA, pts, w0, w1, skin, 0f, 0.95f * fade, age * 0.03f * (k == 0 ? 1 : -1));
                ribbon(ctx, buf, SCALE_GLOW, VfxBlend.ADD, pts, w0, w1, lit, 0f, 0.6f * fade, age * 0.03f * (k == 0 ? 1 : -1));
                Vector3f hd = new Vector3f(pts[6]).sub(pts[5]);
                if (hd.lengthSquared() > 1e-8f) hd.normalize();
                else hd.set(dir);
                float hl = w1 * 5.2f;
                head(ctx, buf, new Vector3f(pts[6]).sub(new Vector3f(hd).mul(hl * 0.18f)), hd, hl, skin, VfxVertexBuffer.whiten(lit, 0.2f), fade);
            }
            // 4. light at the head, thorns and droplets shed from the trail
            VfxBloom.glow(ctx, buf, headPos, 0.38f * P, lit, 0.9f * fade);
            for (int i = 0; i < 4; i++) {
                float back = 0.25f + 0.6f * r.nextFloat();
                float drift = (r.nextFloat() - 0.5f) * 1.4f * P * (0.4f + (1 - back));
                Vector3f q = new Vector3f(headPos).sub(new Vector3f(dir).mul(trail * back)).add(new Vector3f(ctx.camUp).mul(drift - 0.4f * (age % 7) / 7f * P));
                buf.billboard(ctx, SPLASH, VfxBlend.ALPHA, q, (0.2f + 0.16f * r.nextFloat()) * P, r.nextFloat() * 6f, VfxVertexBuffer.withAlpha(skin, 0.8f * fade * back));
            }
        }

        // 5. the impact at 'to': ripple, ink splash, flash
        float hit = sat((t - 0.82f) / 0.18f);
        if (hit > 0f) {
            float e = VfxAnim.easeOutCubic(hit), f = 1f - hit;
            VfxPose ground = VfxPose.ground(new Vector3f(to).add(0, 0.05f, 0));
            buf.plane(RIPPLE, VfxBlend.ADD, ground.spin(1.3f), 1.9f * P * e + 0.2f, VfxVertexBuffer.withAlpha(lit, 0.9f * f));
            buf.billboard(ctx, SPLASH, VfxBlend.ALPHA, to, 1.7f * P * e + 0.2f, age * 0.1f, VfxVertexBuffer.withAlpha(skin, 0.95f * f));
            buf.billboard(ctx, SPLASH, VfxBlend.ADD, to, 1.4f * P * e + 0.2f, -age * 0.14f, VfxVertexBuffer.withAlpha(lit, 0.8f * f));
            VfxBloom.glow(ctx, buf, to, 0.9f * P * (1 - 0.5f * hit), VfxVertexBuffer.whiten(lit, 0.4f), f * 1.2f);
        }
    }

    // ------------------------------------------------------------------ FX2: the hydra's pool
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 8, 12);
        if (fade <= 0.01f) return;
        float R = Math.max(1.2f, inst.power);
        int lit = light(inst), skin = body(inst);
        Vector3f c = ctx.rel(inst.from(ctx));
        float open = VfxAnim.easeOutCubic(sat(age / 12f));
        RandomSource r = inst.random();

        // 1. the ink pool and the glow over it
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, 0.04f, 0));
        buf.plane(POOL, VfxBlend.ALPHA, ground.spin(age * 0.012f), R * 1.08f * open, VfxVertexBuffer.withAlpha(VfxVertexBuffer.lerpColor(INK, skin, 0.35f), 0.92f * fade));
        VfxBloom.planeGlow(buf, ground.lift(0.02f), R * 0.7f, lit, 0.45f * fade);
        // 2. ripple pulses running to the edge, then the two turning fang rings
        for (int k = 0; k < 2; k++) {
            float ph = (age * 0.022f + k * 0.5f) % 1f;
            buf.plane(RIPPLE, VfxBlend.ADD, ground.lift(0.03f).spin(k * 2f), R * (0.25f + 0.78f * ph) * open, VfxVertexBuffer.withAlpha(lit, 0.8f * (1 - ph) * sat(ph * 6) * fade));
        }
        buf.plane(FANGRING, VfxBlend.ADD, ground.lift(0.05f).spin(age * 0.018f), R * 0.98f * open, VfxVertexBuffer.withAlpha(lit, 0.8f * fade));
        buf.plane(FANGRING, VfxBlend.ADD, ground.lift(0.06f).spin(-age * 0.032f), R * 0.52f * open, VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(lit, 0.45f), 0.65f * fade));

        // 3. thorns around the rim
        int thorns = ctx.seg(12, 6);
        float th = Math.min(1.1f, 0.35f + R * 0.1f);
        for (int i = 0; i < thorns; i++) {
            float a = Mth.TWO_PI * i / thorns + 0.2f * Mth.sin(i * 2.3f);
            float rise = VfxAnim.easeOutBack(sat((age - 4 - (i % 5) * 1.5f) / 8f)) * (0.7f + 0.3f * Mth.sin(age * 0.2f + i)) * fade;
            Vector3f foot = new Vector3f(Mth.cos(a) * R * 0.99f, 0, Mth.sin(a) * R * 0.99f).add(c);
            Vector3f top = new Vector3f(foot).add(Mth.cos(a) * 0.15f * th, th * rise, Mth.sin(a) * 0.15f * th);
            buf.beam(ctx, SPINE, VfxBlend.ALPHA, foot, top, th * 0.42f, th * 0.42f, 1, 0, VfxVertexBuffer.withAlpha(skin, 0.95f), VfxVertexBuffer.withAlpha(skin, 0.95f));
        }

        // 4. the crown of hydra necks
        int heads = ctx.seg(Mth.clamp(Math.round(R * 1.4f) + 3, 4, 6), 3);
        float neckLen = Mth.clamp(R * 0.85f, 1.4f, 4.2f);
        float neckW = Mth.clamp(neckLen * 0.17f, 0.25f, 0.7f);
        for (int i = 0; i < heads; i++) {
            float a = Mth.TWO_PI * i / heads + 0.4f;
            float grow = VfxAnim.easeOutBack(sat((age - 2 - i * 2.2f) / 14f)) * fade;
            if (grow <= 0.02f) continue;
            // snap towards the centre on a slow beat, each head on its own phase
            float snap = sat(Mth.sin(age * 0.13f + i * 1.9f) * 2.2f - 0.6f);
            Vector3f foot = new Vector3f(Mth.cos(a) * R * 0.8f, 0, Mth.sin(a) * R * 0.8f).add(c);
            Vector3f axis = new Vector3f(-Mth.cos(a) * (0.35f + 0.25f * snap), 1f, -Mth.sin(a) * (0.35f + 0.25f * snap)).normalize();
            float len = neckLen * grow;
            Vector3f[] pts = path(ctx, foot, axis, len, neckLen * 0.2f, i * 1.7f, age, 0.16f, 4);
            ribbon(ctx, buf, SCALES, VfxBlend.ALPHA, pts, neckW * 1.4f, neckW * 0.8f, skin, 0.9f, 0.95f, age * 0.01f);
            ribbon(ctx, buf, SCALE_GLOW, VfxBlend.ADD, pts, neckW * 1.4f, neckW * 0.8f, lit, 0.3f, 0.55f, age * 0.01f);
            Vector3f hd = new Vector3f(pts[4]).sub(pts[3]);
            // the head tips over towards the middle of the pool
            hd.add(new Vector3f(-Mth.cos(a), -0.35f - 0.3f * snap, -Mth.sin(a)).mul(0.9f)).normalize();
            float hl = neckW * 0.8f * 5.6f * (1f + 0.25f * snap);
            head(ctx, buf, new Vector3f(pts[4]).sub(new Vector3f(hd).mul(hl * 0.18f)), hd, hl, skin, VfxVertexBuffer.whiten(lit, 0.2f), grow);
        }

        // 5. bubbles lifting out of the pool
        int nb = ctx.seg(7, 3);
        for (int i = 0; i < nb; i++) {
            float a = r.nextFloat() * Mth.TWO_PI, d = Mth.sqrt(r.nextFloat()) * R * 0.85f;
            float spd = 0.012f + 0.01f * r.nextFloat();
            float u = (age * spd + r.nextFloat()) % 1f;
            Vector3f q = new Vector3f(Mth.cos(a) * d + 0.1f * Mth.sin(age * 0.2f + i), u * (1.2f + R * 0.3f) + 0.1f, Mth.sin(a) * d).add(c);
            bubble(ctx, buf, q, (0.12f + 0.12f * r.nextFloat()) * (0.7f + 0.3f * Math.min(R, 3f)), lit, 0.9f * Mth.sin(u * Mth.PI) * fade);
        }
    }

    // ------------------------------------------------------------------ FX3: the hydra breaks the surface
    private void impact(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float t = inst.progress(ctx.partialTick);
        float P = Math.max(0.4f, inst.power);
        int lit = light(inst), skin = body(inst);
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f dir = dirOf(inst, ctx, new Vector3f(0, 1, 0));
        RandomSource r = inst.random();
        float out = 1f - sat((t - 0.55f) / 0.45f);

        // 1. ripples racing over the ground, and the ring of fangs facing the camera
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, 0.05f, 0));
        for (int k = 0; k < 3; k++) {
            float p = sat((t - k * 0.07f) / 0.8f);
            if (p <= 0f) continue;
            buf.plane(RIPPLE, VfxBlend.ADD, ground.lift(0.01f * k).spin(k * 1.7f), (0.4f + 3.2f * VfxAnim.easeOutCubic(p)) * P, VfxVertexBuffer.withAlpha(lit, 0.9f * (1 - p)));
        }
        float ringP = sat(t / 0.5f);
        if (ringP < 1f) {
            Vector3f camN = new Vector3f(ctx.camRight).cross(ctx.camUp);
            buf.plane(FANGRING, VfxBlend.ADD, VfxPose.facing(c, camN).spin(age * 0.2f), (0.3f + 2.2f * VfxAnim.easeOutCubic(ringP)) * P,
                    VfxVertexBuffer.withAlpha(lit, 0.85f * (1 - ringP)));
        }

        // 2. the hydra lunges out along dir, bites, and sinks back
        float lunge = VfxAnim.easeOutBack(sat(t / 0.3f)) * (1f - VfxAnim.easeInCubic(sat((t - 0.55f) / 0.4f)));
        if (lunge > 0.03f) {
            float len = 2.2f * P * lunge;
            float w1 = 0.30f * P;
            Vector3f[] pts = path(ctx, c, dir, len, 0.3f * P, 0f, age, 0.5f, 5);
            ribbon(ctx, buf, SCALES, VfxBlend.ALPHA, pts, w1 * 2.1f, w1, skin, 1f, 1f, age * 0.02f);
            ribbon(ctx, buf, SCALE_GLOW, VfxBlend.ADD, pts, w1 * 2.1f, w1, lit, 0.7f, 0.9f, age * 0.02f);
            Vector3f hd = new Vector3f(pts[5]).sub(pts[4]);
            if (hd.lengthSquared() > 1e-8f) hd.normalize();
            else hd.set(dir);
            float hl = w1 * 5.6f * (1f + 0.2f * sat(t / 0.3f));
            head(ctx, buf, new Vector3f(pts[5]).sub(new Vector3f(hd).mul(hl * 0.18f)), hd, hl, skin, VfxVertexBuffer.whiten(lit, 0.3f), 1f);
        }

        // 3. flash and the ink splash
        float flash = 1f - sat(t / 0.3f);
        VfxBloom.glow(ctx, buf, c, (0.9f + 1.4f * (1 - flash)) * P, VfxVertexBuffer.whiten(lit, 0.5f), (0.35f + 0.9f * flash) * out + 0.25f * (1 - out) * (1 - t));
        float sp = VfxAnim.easeOutCubic(sat(t / 0.45f));
        buf.billboard(ctx, SPLASH, VfxBlend.ALPHA, c, (0.6f + 3.4f * sp) * P, 0.4f, VfxVertexBuffer.withAlpha(skin, 0.95f * (1 - sat((t - 0.2f) / 0.5f))));
        buf.billboard(ctx, SPLASH, VfxBlend.ADD, c, (0.5f + 3.0f * sp) * P, -0.6f + age * 0.05f, VfxVertexBuffer.withAlpha(lit, 0.85f * (1 - sat((t - 0.1f) / 0.5f))));

        // 4. thorns and droplets thrown out and falling back
        int thorns = ctx.seg(6, 3);
        for (int i = 0; i < thorns; i++) {
            float a = r.nextFloat() * Mth.TWO_PI, el = 0.4f + r.nextFloat() * 0.9f, v = (1.8f + r.nextFloat() * 2.2f) * P;
            Vector3f vel = new Vector3f(Mth.cos(a) * Mth.cos(el), Mth.sin(el), Mth.sin(a) * Mth.cos(el)).add(new Vector3f(dir).mul(0.5f)).mul(v);
            float tt = t * inst.duration * 0.05f;
            Vector3f q = new Vector3f(vel).mul(tt).add(0, -4.5f * tt * tt, 0).add(c);
            Vector3f d2 = new Vector3f(vel).add(0, -9f * tt, 0);
            if (d2.lengthSquared() < 1e-6f) continue;
            d2.normalize(0.5f * P);
            buf.beam(ctx, SPINE, VfxBlend.ALPHA, q, new Vector3f(q).add(d2), 0.22f * P, 0.22f * P, 1, 0, VfxVertexBuffer.withAlpha(skin, 0.95f * out), VfxVertexBuffer.withAlpha(skin, 0.95f * out));
        }
        int drops = ctx.seg(8, 3);
        for (int i = 0; i < drops; i++) {
            float a = r.nextFloat() * Mth.TWO_PI, el = 0.2f + r.nextFloat() * 1.2f, v = (1.2f + r.nextFloat() * 2.4f) * P;
            Vector3f vel = new Vector3f(Mth.cos(a) * Mth.cos(el), Mth.sin(el), Mth.sin(a) * Mth.cos(el)).mul(v);
            float tt = t * inst.duration * 0.05f;
            Vector3f q = new Vector3f(vel).mul(tt).add(0, -3.5f * tt * tt, 0).add(c);
            float s = (0.12f + 0.16f * r.nextFloat()) * P;
            buf.billboard(ctx, i % 2 == 0 ? SPLASH : BUBBLE, VfxBlend.ADD, q, s * 1.5f, r.nextFloat() * 6f, VfxVertexBuffer.withAlpha(lit, 0.9f * out));
        }

        // 5. afterglow: a teal haze on the water and bubbles drifting up
        float glow = sat((t - 0.2f) / 0.2f) * (1f - sat((t - 0.6f) / 0.4f));
        buf.plane(POOL, VfxBlend.ADD, ground.lift(0.02f), 1.9f * P, VfxVertexBuffer.withAlpha(lit, 0.35f * glow));
        for (int i = 0; i < 3; i++) {
            float u = (t * 1.2f + i * 0.31f) % 1f;
            bubble(ctx, buf, new Vector3f(c).add((r.nextFloat() - 0.5f) * 1.6f * P, 0.2f + u * 1.8f * P, (r.nextFloat() - 0.5f) * 1.6f * P),
                    (0.14f + 0.1f * r.nextFloat()) * P, lit, 0.9f * Mth.sin(u * Mth.PI) * sat(t * 4f) * (1 - 0.4f * t));
        }
    }
}
