package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.*;

/**
 * 0.56 Seal Magic (the generic sealing spells), drawn to the Time Magic standard from the circle pack in
 * docs/attributes/art_reference/seal_circles_pack.jpg (rune rings, hexagram, star) with the hard white core of seal_inverse_release.
 * Textures: tools/gen_seal_textures.py (seal_*). Colour: the seal's tint (default rose red); the cores are the same tint pushed to white.
 * <ul>
 *   <li>SEALING_FX1, seal cast / projectile: from = hand, to = target, power = size, duration = flight (16).
 *       A seal sigil opens at the hand, then a spinning sigil with a padlock flies to the target dragging a streak, afterimages and
 *       orbiting glints, and ends in a lock flash with a shock ring at the target.</li>
 *   <li>SEALING_FX2, seal field / zone: from = centre on the ground, power = radius in blocks, duration = life (80).
 *       Three counter-rotating sigils on the ground (hexagram, rings, star), a rune band on the ground rim and as a low wall, six light
 *       pillars on the hexagram points, rising glints, a floating padlock and a pulse ring every two seconds.</li>
 *   <li>SEALING_FX3, seal impact / burst: from = centre, to = optional direction hint, power = scale, duration = life (28).
 *       A hard flash, a padlock that slams shut, a sigil stamped round the centre, shock rings, seal crystals and glints flying out.</li>
 * </ul>
 */
public class SealLayer extends AbstractVfxLayer {
    static ResourceLocation t(String n) { return VfxTextures.byName(n); }
    static final ResourceLocation SIGIL = t("seal_sigil"), RINGS = t("seal_rings"), STAR = t("seal_star"), BAND = t("seal_band"), LOCK = t("seal_lock"),
            GLINT = t("seal_glint"), CRYSTAL = t("seal_crystal"), BURST = t("seal_burst"), STREAK = t("seal_streak"), RAY = t("seal_ray");

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.SEALING_FX1, VfxShape.SEALING_FX2, VfxShape.SEALING_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case SEALING_FX1 -> 16;
            case SEALING_FX2 -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFFF5A78; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case SEALING_FX1 -> cast(inst, ctx, buf);
            case SEALING_FX2 -> zone(inst, ctx, buf);
            case SEALING_FX3 -> impact(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ helpers
    /** A camera-facing quad of half size hw x hh whose texture "up" points along the screen angle ang + 90 degrees (a billboard with an aspect). */
    static void sprite(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, VfxBlend blend, Vector3f c, float hw, float hh, float ang, int argb) {
        float co = Mth.cos(ang), si = Mth.sin(ang);
        Vector3f r = new Vector3f(ctx.camRight).mul(co).add(new Vector3f(ctx.camUp).mul(si)).mul(hw);
        Vector3f u = new Vector3f(ctx.camUp).mul(co).sub(new Vector3f(ctx.camRight).mul(si)).mul(hh);
        int col = blend.grade(argb, 1f);
        buf.quad(tex, blend, new Vector3f(c).sub(r).sub(u), new Vector3f(c).add(r).sub(u), new Vector3f(c).add(r).add(u), new Vector3f(c).sub(r).add(u),
                0, 0, 1, 1, col, col);
    }

    /** An upright light pillar standing on {@code foot}, always turned to the camera round the vertical. */
    static void pillar(VfxVertexBuffer buf, VfxRenderContext ctx, Vector3f foot, float w, float h, int argb) {
        Vector3f right = new Vector3f(ctx.camRight.x, 0, ctx.camRight.z);
        if (right.lengthSquared() < 1e-4f) right.set(1, 0, 0);
        right.normalize().mul(w * 0.5f);
        Vector3f up = new Vector3f(0, h, 0);
        int col = VfxBlend.ADD.grade(argb, 1f);
        buf.quad(RAY, VfxBlend.ADD, new Vector3f(foot).sub(right), new Vector3f(foot).add(right), new Vector3f(foot).add(right).add(up),
                new Vector3f(foot).sub(right).add(up), 0, 0, 1, 1, col, col);
    }

    private static Vector3f dirOf(Vector3f a, Vector3f b, float[] len) {
        Vector3f d = new Vector3f(b).sub(a);
        len[0] = d.length();
        if (len[0] < 1e-3f) { len[0] = 0; return new Vector3f(0, 0, 1); }
        return d.div(len[0]);
    }

    // ------------------------------------------------------------------ FX1: cast / projectile
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = inst.duration, p = inst.power;
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        float[] len = new float[1];
        Vector3f dir = dirOf(a, b, len);
        int col = inst.color, light = VfxVertexBuffer.whiten(col, 0.5f), hot = VfxVertexBuffer.whiten(col, 0.85f);
        float charge = Mth.clamp(age / (dur * 0.26f), 0, 1);
        float ca = (1 - Mth.clamp((age - dur * 0.2f) / (dur * 0.2f), 0, 1)) * VfxAnim.easeOutCubic(Mth.clamp(age / 3f, 0, 1));
        float fly = Mth.clamp((age - dur * 0.18f) / (dur * 0.6f), 0, 1);
        float imp = Mth.clamp((age - dur * 0.78f) / (dur * 0.22f), 0, 1);

        // the charge: a sigil opens in front of the hand
        if (ca > 0.01f) {
            Vector3f at = new Vector3f(a).add(new Vector3f(dir).mul(0.35f));
            float open = VfxAnim.easeOutBack(charge);
            buf.plane(SIGIL, VfxBlend.ADD, VfxPose.facing(at, dir).spin(age * 0.3f), 0.75f * p * open, VfxVertexBuffer.withAlpha(col, ca));
            buf.plane(RINGS, VfxBlend.ADD, VfxPose.facing(new Vector3f(at).add(new Vector3f(dir).mul(0.05f)), dir).spin(-age * 0.4f), 0.52f * p * open,
                    VfxVertexBuffer.withAlpha(light, ca));
            buf.plane(STAR, VfxBlend.ADD, VfxPose.facing(new Vector3f(at).add(new Vector3f(dir).mul(0.1f)), dir).spin(age * 0.55f), 0.3f * p * open,
                    VfxVertexBuffer.withAlpha(hot, ca));
            VfxBloom.glow(ctx, buf, at, 0.5f * p * charge, col, ca);
        }
        // the flight
        if (fly > 0 && imp < 1) {
            float tt = fly;
            Vector3f h = new Vector3f(a).add(new Vector3f(dir).mul(len[0] * tt));
            float out = 1 - imp;
            float trail = Math.min(len[0] * tt, 3.4f * p);
            Vector3f tail = new Vector3f(h).sub(new Vector3f(dir).mul(trail));
            streak(buf, ctx, STREAK, VfxBlend.ADD, tail, h, 0.46f * p, VfxVertexBuffer.withAlpha(col, out));
            streak(buf, ctx, STREAK, VfxBlend.ADD, new Vector3f(tail).lerp(h, 0.35f), h, 0.2f * p, VfxVertexBuffer.withAlpha(hot, out));
            // afterimages of the sigil
            for (int k = 1; k <= 3; k++) {
                Vector3f q = new Vector3f(h).sub(new Vector3f(dir).mul(0.6f * p * k));
                buf.plane(RINGS, VfxBlend.ADD, VfxPose.facing(q, dir).spin(-age * 0.4f + k), 0.42f * p * (1 - 0.15f * k), VfxVertexBuffer.withAlpha(col, out * (0.55f - 0.15f * k)));
            }
            // the head: sigil, star, padlock
            buf.plane(SIGIL, VfxBlend.ADD, VfxPose.facing(h, dir).spin(age * 0.5f), 0.5f * p, VfxVertexBuffer.withAlpha(light, out));
            buf.plane(STAR, VfxBlend.ADD, VfxPose.facing(new Vector3f(h).add(new Vector3f(dir).mul(0.04f)), dir).spin(-age * 0.7f), 0.34f * p, VfxVertexBuffer.withAlpha(hot, out));
            buf.billboard(ctx, LOCK, VfxBlend.ADD, h, 0.46f * p, 0f, VfxVertexBuffer.withAlpha(hot, out * 0.9f));
            VfxBloom.glow(ctx, buf, h, 0.55f * p, col, out);
            // glints spiralling round the trail
            Vector3f s = side(dir), u = new Vector3f(dir).cross(s).normalize();
            for (int j = 0; j < 4; j++) {
                float w = (j + 1) / 5f, ang = age * 0.8f + j * Mth.HALF_PI;
                Vector3f q = new Vector3f(h).sub(new Vector3f(dir).mul(trail * w)).add(new Vector3f(s).mul(Mth.cos(ang) * 0.3f * p * (0.4f + w)))
                        .add(new Vector3f(u).mul(Mth.sin(ang) * 0.3f * p * (0.4f + w)));
                buf.billboard(ctx, GLINT, VfxBlend.ADD, q, 0.26f * p, age * 0.2f + j, VfxVertexBuffer.withAlpha(WHITE, out * (1 - w * 0.5f)));
            }
        }
        // the lock flash at the target
        if (imp > 0) {
            float e = VfxAnim.easeOutCubic(imp);
            buf.billboard(ctx, BURST, VfxBlend.ADD, b, 2.4f * p * (0.4f + e), age * 0.1f, VfxVertexBuffer.withAlpha(hot, 1 - imp));
            buf.plane(RINGS, VfxBlend.ADD, VfxPose.facing(b, dir).spin(age * 0.3f), (0.3f + 1.1f * e) * p, VfxVertexBuffer.withAlpha(col, (1 - imp) * 0.9f));
            buf.billboard(ctx, LOCK, VfxBlend.ADD, b, (1.0f - 0.35f * e) * p, 0f, VfxVertexBuffer.withAlpha(WHITE, Math.min(1f, imp * 6f) * (1 - imp)));
            VfxBloom.glow(ctx, buf, b, 0.9f * p * (1 - 0.5f * imp), col, 1 - imp);
            for (int j = 0; j < 4; j++) {
                float ang = hash(inst.seed, j, 3) * Mth.TWO_PI;
                Vector3f q = new Vector3f(b).add(new Vector3f(ctx.camRight).mul(Mth.cos(ang) * 0.9f * p * e)).add(new Vector3f(ctx.camUp).mul(Mth.sin(ang) * 0.9f * p * e));
                buf.billboard(ctx, GLINT, VfxBlend.ADD, q, 0.3f * p, ang, VfxVertexBuffer.withAlpha(WHITE, 1 - imp));
            }
        }
    }

    // ------------------------------------------------------------------ FX2: zone / field
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power;
        float f = life(inst, age, 8, 12);
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 10f, 0, 1));
        Vector3f c = ctx.rel(inst.from(ctx)).add(0, 0.05f, 0);
        int col = inst.color, light = VfxVertexBuffer.whiten(col, 0.5f), hot = VfxVertexBuffer.whiten(col, 0.85f);
        VfxPose ground = VfxPose.ground(c);
        VfxBloom.planeGlow(buf, ground, p, col, 0.7f * f);
        // three counter-rotating sigils
        VfxPose sig = ground.spin(age * 0.02f);
        buf.plane(SIGIL, VfxBlend.ADD, sig, p * open, VfxVertexBuffer.withAlpha(col, f * 0.95f));
        buf.plane(RINGS, VfxBlend.ADD, ground.lift(0.02f).spin(-age * 0.035f), 0.8f * p * open, VfxVertexBuffer.withAlpha(light, f * 0.9f));
        buf.plane(STAR, VfxBlend.ADD, ground.lift(0.03f).spin(age * 0.055f), 0.5f * p * open, VfxVertexBuffer.withAlpha(hot, f * 0.85f));
        buf.plane(SIGIL, VfxBlend.ADD, ground.lift(0.04f).spin(-age * 0.09f), 0.24f * p * open, VfxVertexBuffer.withAlpha(WHITE, f * 0.7f));
        // the rune band on the rim and as a low wall
        buf.ring(BAND, VfxBlend.ADD, ground.lift(0.05f), p * 1.0f, p * 1.12f, ctx.seg(16, 10), 6, age * 0.012f, VfxVertexBuffer.withAlpha(light, f * 0.85f));
        hoop(buf, BAND, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, 0.45f, 0)), p * 1.02f, 0.4f, ctx.seg(16, 10), 6, -age * 0.015f,
                VfxVertexBuffer.withAlpha(col, f * (0.5f + 0.2f * Mth.sin(age * 0.2f))));
        // six pillars on the hexagram points
        for (int k = 0; k < 6; k++) {
            float ang = -Mth.HALF_PI + k * Mth.PI / 3;
            Vector3f v = sig.point(Mth.cos(ang) * 0.78f * p * open, Mth.sin(ang) * 0.78f * p * open);
            float h = (1.5f + 0.5f * Mth.sin(age * 0.12f + k * 1.7f)) * Math.min(1.6f, 0.6f + 0.4f * p) * Math.min(1f, age / 12f);
            pillar(buf, ctx, v, 0.5f, h, VfxVertexBuffer.withAlpha(light, f * 0.8f));
        }
        // glints rising
        for (int i = 0; i < 10; i++) {
            float ang = hash(inst.seed, i, 1) * Mth.TWO_PI, rad = Mth.sqrt(hash(inst.seed, i, 2)) * p * 0.92f;
            float ph = (age * (0.012f + 0.014f * hash(inst.seed, i, 3)) + hash(inst.seed, i, 4)) % 1f;
            Vector3f q = new Vector3f(c).add(Mth.cos(ang) * rad, ph * 2.4f, Mth.sin(ang) * rad);
            buf.billboard(ctx, GLINT, VfxBlend.ADD, q, 0.2f + 0.14f * hash(inst.seed, i, 5), age * 0.1f + i, VfxVertexBuffer.withAlpha(WHITE, f * Mth.sin(ph * Mth.PI)));
        }
        // the padlock hanging over the middle, and the pulse
        Vector3f lk = new Vector3f(c).add(0, 1.15f + 0.08f * Mth.sin(age * 0.15f), 0);
        float ls = 0.7f + 0.12f * Math.min(p, 4f);
        buf.billboard(ctx, LOCK, VfxBlend.ADD, lk, ls, 0f, VfxVertexBuffer.withAlpha(hot, f * 0.9f));
        VfxBloom.glow(ctx, buf, lk, 0.5f * ls, col, f * 0.8f);
        float ph = (age % 40f) / 40f;
        buf.plane(RINGS, VfxBlend.ADD, ground.lift(0.06f).spin(age * 0.1f), p * (0.2f + 0.95f * VfxAnim.easeOutCubic(ph)), VfxVertexBuffer.withAlpha(col, f * (1 - ph) * 0.6f));
    }

    // ------------------------------------------------------------------ FX3: impact / burst
    private void impact(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power, t = inst.progress(ctx.partialTick);
        Vector3f c = ctx.rel(inst.from(ctx));
        int col = inst.color, light = VfxVertexBuffer.whiten(col, 0.5f), hot = VfxVertexBuffer.whiten(col, 0.85f);
        Vector3f hint = new Vector3f(ctx.rel(inst.to(ctx))).sub(c);
        boolean hasDir = hint.lengthSquared() > 0.01f;
        float e = VfxAnim.easeOutCubic(Mth.clamp(age / 14f, 0, 1)), fade = 1 - t;
        float linger = 1 - Mth.clamp((t - 0.5f) / 0.5f, 0, 1);
        // the flash
        buf.billboard(ctx, BURST, VfxBlend.ADD, c, 3.6f * p * (0.35f + 0.65f * VfxAnim.easeOutCubic(Mth.clamp(age / 5f, 0, 1))), age * 0.06f,
                VfxVertexBuffer.withAlpha(hot, Mth.clamp(fade * 1.6f, 0, 1)));
        VfxBloom.glow(ctx, buf, c, 1.3f * p * fade, col, fade);
        // the sigil stamped round the centre (flat if no direction)
        VfxPose stamp = hasDir ? VfxPose.facing(c, hint) : VfxPose.ground(c);
        float st = VfxAnim.easeOutBack(Mth.clamp(age / 6f, 0, 1));
        buf.plane(SIGIL, VfxBlend.ADD, stamp.spin(age * 0.12f), 1.5f * p * st, VfxVertexBuffer.withAlpha(col, linger * 0.9f));
        buf.plane(STAR, VfxBlend.ADD, stamp.lift(0.03f).spin(-age * 0.2f), 0.9f * p * st, VfxVertexBuffer.withAlpha(light, linger * 0.9f));
        // the padlock slamming shut
        float slam = VfxAnim.easeOutCubic(Mth.clamp(age / 6f, 0, 1));
        buf.billboard(ctx, LOCK, VfxBlend.ADD, c, (2.8f - 1.9f * slam) * p, 0f, VfxVertexBuffer.withAlpha(WHITE, Math.min(1f, age / 2f) * linger));
        // shock rings
        buf.plane(RINGS, VfxBlend.ADD, VfxPose.ground(c).spin(age * 0.2f), (0.4f + 2.8f * e) * p, VfxVertexBuffer.withAlpha(col, (1 - e) * fade));
        buf.billboard(ctx, RINGS, VfxBlend.ADD, c, 5.2f * p * e, age * 0.18f, VfxVertexBuffer.withAlpha(light, (1 - e) * 0.9f));
        // crystals, glints and streaks flying out
        for (int i = 0; i < 8; i++) {
            Vector3f d = new Vector3f(hash(inst.seed, i, 1) - 0.5f, hash(inst.seed, i, 2) - 0.1f, hash(inst.seed, i, 3) - 0.5f);
            if (hasDir) d.add(new Vector3f(hint).normalize().mul(0.6f));
            if (d.lengthSquared() < 1e-4f) d.set(0, 1, 0);
            d.normalize();
            float dist = (0.5f + 2.0f * hash(inst.seed, i, 4)) * p * e;
            Vector3f q = new Vector3f(c).add(new Vector3f(d).mul(dist)).add(0, -0.6f * p * t * t * hash(inst.seed, i, 5), 0);
            float ang = (float) Mth.atan2(d.dot(ctx.camUp), d.dot(ctx.camRight)) - Mth.HALF_PI;
            float sz = (0.4f + 0.3f * hash(inst.seed, i, 6)) * p;
            sprite(buf, ctx, CRYSTAL, VfxBlend.ADD, q, sz * 0.25f, sz * 0.5f, ang + t * 2f * (i % 2 == 0 ? 1 : -1), VfxVertexBuffer.withAlpha(i % 2 == 0 ? light : hot, fade * 0.9f));
            if (i < 6) {
                Vector3f g = new Vector3f(c).add(new Vector3f(d).mul(dist * 1.3f));
                buf.billboard(ctx, GLINT, VfxBlend.ADD, g, 0.3f * p, age * 0.2f + i, VfxVertexBuffer.withAlpha(WHITE, fade));
            }
            if (i < 8) {
                Vector3f s0 = new Vector3f(c).add(new Vector3f(d).mul(dist * 0.55f)), s1 = new Vector3f(c).add(new Vector3f(d).mul(dist * 0.95f + 0.2f * p));
                streak(buf, ctx, STREAK, VfxBlend.ADD, s0, s1, 0.18f * p, VfxVertexBuffer.withAlpha(WHITE, Mth.clamp(1 - t * 1.4f, 0, 1)));
            }
        }
    }
}
