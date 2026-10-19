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
 * 1.93.0 Water Magic (the Sea Dragon: Noelle Silva), drawn toward the Time Magic layers' quality: layered, eased, every sprite from textures/particle/waterfx_*.png
 * (tools/gen_water_fx_textures.py).
 * <ul>
 *   <li>WATERFX_CAST: the sea sigil and a ripple open at 'from' (first quarter), then a head of spray runs to 'to' trailing three strands of water winding round a straight stream,
 *       shedding drops; where it lands a crest of water and a ripple ring. 'from' = origin, 'to' = target, power = size.</li>
 *   <li>WATERFX_ZONE: the sea sigil turning on the ground at 'from', three ripples running out in turn, water spouts curling up round the rim and drops falling. power = radius.</li>
 *   <li>WATERFX_BURST: a ring of ripples, eight wave crests thrown outward, a spray cloud and drops on arcs. power = size.</li>
 * </ul>
 * Render types: the water itself is VfxBlend.WATER (tinted translucent), the glints and rings are additive (VfxBlend.ADD, glows land on top). At most 400 vertices an activation.
 */
public class WaterFxLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    private static final ResourceLocation STREAM = t("waterfx_stream"), RIPPLE = t("waterfx_ripple"), DROP = t("waterfx_drop"), SIGIL = t("waterfx_sigil"), WAVE = t("waterfx_wave"),
            SPRAY = t("waterfx_spray");

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.WATERFX_CAST, VfxShape.WATERFX_ZONE, VfxShape.WATERFX_BURST); }

    @Override
    public int defaultDuration(VfxShape s) { return s == VfxShape.WATERFX_ZONE ? 80 : s == VfxShape.WATERFX_BURST ? 28 : 22; }

    @Override public int defaultColor(VfxShape s) { return 0xFF4FA8FF; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.WATERFX_BURST && inst.power >= 1.2f) VfxShake.add(inst.payload.from(), 0.8f * inst.power, 10);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case WATERFX_CAST -> cast(inst, ctx, buf);
            case WATERFX_ZONE -> zone(inst, ctx, buf);
            case WATERFX_BURST -> burst(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ helpers
    private static float life(VfxInstance inst, float age, float in, float out) {
        float a = in <= 0 ? 1 : Mth.clamp(age / in, 0, 1), b = out <= 0 ? 1 : Mth.clamp((inst.duration - age) / out, 0, 1);
        return Math.min(a, b);
    }

    /** A ribbon from a to b (camera facing), the texture lying along u (tail a = 0, head b = 1), cut in n pieces so the colour can fade from ca to cb along it. */
    private static void strip(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f a, Vector3f b, float wa, float wb, int n, int ca, int cb) {
        Vector3f dir = new Vector3f(b).sub(a);
        if (dir.lengthSquared() < 1e-6f) return;
        Vector3f toCam = new Vector3f(a).add(b).mul(0.5f).negate().normalize();
        Vector3f side = new Vector3f(dir).cross(toCam);
        if (side.lengthSquared() < 1e-8f) return;
        side.normalize();
        for (int i = 0; i < n; i++) {
            float t0 = (float) i / n, t1 = (float) (i + 1) / n;
            Vector3f c0 = new Vector3f(dir).mul(t0).add(a), c1 = new Vector3f(dir).mul(t1).add(a);
            Vector3f s0 = new Vector3f(side).mul(Mth.lerp(t0, wa, wb) * 0.5f), s1 = new Vector3f(side).mul(Mth.lerp(t1, wa, wb) * 0.5f);
            int col = blend.grade(VfxVertexBuffer.lerpColor(ca, cb, (t0 + t1) * 0.5f), 1f - t0);
            buf.quad(tex, blend, new Vector3f(c0).sub(s0), new Vector3f(c1).sub(s1), new Vector3f(c1).add(s1), new Vector3f(c0).add(s0), t0, 0, t1, 1, col, col);
        }
    }

    private static void upright(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f base, float w, float h, int argb) {
        Vector3f r = new Vector3f(ctx.camRight.x, 0, ctx.camRight.z);
        if (r.lengthSquared() < 1e-6f) r.set(1, 0, 0);
        r.normalize().mul(w * 0.5f);
        int col = blend.grade(argb, 1f);
        buf.quad(tex, blend, new Vector3f(base).sub(r), new Vector3f(base).add(r), new Vector3f(base).add(r).add(0, h, 0), new Vector3f(base).sub(r).add(0, h, 0), 0, 0, 1, 1, col, col);
    }

    private static Vector3f ease(Vector3f a, Vector3f b, float t) { return new Vector3f(a).lerp(b, t); }

    // ------------------------------------------------------------------ WATERFX_CAST
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), p = inst.power, fade = life(inst, age, 0, 6);
        int col = inst.color, pale = VfxVertexBuffer.whiten(col, 0.65f);
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(from);
        float len = dir.length();
        if (len < 0.05f) { dir.set(0, 0, 1); len = 0.05f; }
        dir.div(len);
        float charge = Mth.clamp(age / (inst.duration * 0.25f), 0, 1), run = Mth.clamp((age - inst.duration * 0.12f) / (inst.duration * 0.7f), 0, 1);
        float head = VfxAnim.easeOutCubic(run), tail = Math.max(0f, head - 0.6f);
        float open = VfxAnim.easeOutBack(charge) * (1f - 0.6f * Mth.clamp((age - inst.duration * 0.3f) / (inst.duration * 0.3f), 0, 1));
        VfxPose face = VfxPose.facing(new Vector3f(from).add(new Vector3f(dir).mul(0.25f * p)), dir);
        if (open > 0.02f) {
            buf.plane(SIGIL, VfxBlend.ADD, face.spin(-age * 0.07f), 0.7f * p * open, VfxVertexBuffer.withAlpha(col, 0.9f * fade));
            buf.plane(RIPPLE, VfxBlend.ADD, face.lift(0.04f).spin(age * 0.09f), 0.55f * p * open, VfxVertexBuffer.withAlpha(pale, 0.8f * fade));
            VfxBloom.glow(ctx, buf, from, 0.5f * p * open, col, 0.7f * fade * (1f - run * 0.6f));
        }
        if (head <= 0.001f) return;
        Vector3f up = Math.abs(dir.y) > 0.95f ? new Vector3f(1, 0, 0) : new Vector3f(0, 1, 0);
        Vector3f side = new Vector3f(dir).cross(up).normalize(), up2 = new Vector3f(side).cross(dir).normalize();
        Vector3f hp = ease(from, to, head), tp = ease(from, to, tail);
        float width = 0.8f * p;
        strip(ctx, buf, STREAM, VfxBlend.WATER, tp, hp, width * 0.4f, width, ctx.seg(8, 4), VfxVertexBuffer.withAlpha(col, 0f), VfxVertexBuffer.withAlpha(col, 0.95f * fade));
        strip(ctx, buf, STREAM, VfxBlend.ADD, tp, hp, width * 0.15f, width * 0.35f, ctx.seg(6, 3), VfxVertexBuffer.withAlpha(pale, 0f), VfxVertexBuffer.withAlpha(pale, 0.7f * fade));
        for (int k = 0; k < 3; k++) {                                                           // three strands winding round the stream
            Vector3f prev = null;
            int pieces = ctx.seg(6, 3);
            for (int i = 0; i <= pieces; i++) {
                float s = Mth.lerp(i / (float) pieces, tail, head), ang = s * 12f - age * 0.3f + k * Mth.TWO_PI / 3f, rad = 0.3f * p * (0.4f + 0.6f * (i / (float) pieces));
                Vector3f pos = ease(from, to, s).add(new Vector3f(side).mul(Mth.cos(ang) * rad)).add(new Vector3f(up2).mul(Mth.sin(ang) * rad));
                if (prev != null) strip(ctx, buf, STREAM, VfxBlend.WATER, prev, pos, width * 0.3f, width * 0.35f, 1,
                        VfxVertexBuffer.withAlpha(col, 0.4f * fade * (i / (float) pieces)), VfxVertexBuffer.withAlpha(col, 0.75f * fade));
                prev = pos;
            }
        }
        buf.billboard(ctx, SPRAY, VfxBlend.ADD, hp, 1.5f * p, age * 0.1f, VfxVertexBuffer.withAlpha(pale, 0.75f * fade));
        VfxBloom.glow(ctx, buf, hp, 0.7f * p, col, 0.8f * fade);
        RandomSource r = inst.random();
        int n = ctx.seg(10, 4);
        for (int i = 0; i < n; i++) {                                                           // drops shed along the stream, thrown up and falling
            float s = tail + (head - tail) * r.nextFloat(), life = Mth.frac(age * 0.06f + r.nextFloat());
            Vector3f q = ease(from, to, s).add(new Vector3f(side).mul((r.nextFloat() - 0.5f) * 0.9f * p)).add(new Vector3f(up2).mul((r.nextFloat() - 0.3f) * 0.5f * p + 0.6f * Mth.sin(life * Mth.PI) * p - 0.4f * life * life * p));
            buf.billboard(ctx, DROP, VfxBlend.ADD, q, 0.22f * p, 0f, VfxVertexBuffer.withAlpha(pale, Mth.sin(life * Mth.PI) * 0.9f * fade));
        }
        if (head > 0.88f) {                                                                     // the landing: a crest and a ripple
            float k = (head - 0.88f) / 0.12f;
            upright(ctx, buf, WAVE, VfxBlend.WATER, new Vector3f(to).add(0, -0.1f, 0), 2.2f * p * k, 1.1f * p * k, VfxVertexBuffer.withAlpha(col, (1f - 0.5f * k) * fade));
            buf.plane(RIPPLE, VfxBlend.ADD, VfxPose.ground(new Vector3f(to).add(0, 0.08f, 0)), 0.5f * p + 1.1f * p * k, VfxVertexBuffer.withAlpha(pale, (1f - k) * 0.8f * fade));
        }
    }

    // ------------------------------------------------------------------ WATERFX_ZONE
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), R = inst.power, fade = life(inst, age, 8, 12);
        if (fade <= 0.01f) return;
        int col = inst.color, pale = VfxVertexBuffer.whiten(col, 0.65f);
        Vector3f c = ctx.rel(inst.from(ctx));
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 10f, 0, 1));
        VfxPose ground = VfxPose.ground(new Vector3f(c).add(0, 0.06f, 0));
        VfxBloom.planeGlow(buf, ground.lift(-0.02f), R * open, col, 0.4f * fade);
        buf.plane(SIGIL, VfxBlend.ADD, ground.spin(-age * 0.02f), R * 0.9f * open, VfxVertexBuffer.withAlpha(col, 0.8f * fade));
        for (int k = 0; k < 3; k++) {                                                           // ripples running out in turn
            float ph = Mth.frac(age * 0.015f + k / 3f);
            buf.plane(RIPPLE, VfxBlend.ADD, ground.lift(0.02f + 0.01f * k).spin(age * 0.02f + k), R * (0.3f + 0.75f * ph) * open, VfxVertexBuffer.withAlpha(k == 1 ? pale : col, (1f - ph) * 0.9f * fade));
        }
        RandomSource r = inst.random();
        int n = ctx.seg(10, 5);
        for (int i = 0; i < n; i++) {                                                           // spouts of water curling up round the rim
            float a = i * Mth.TWO_PI / n + r.nextFloat() * 0.3f, ph = r.nextFloat() * Mth.TWO_PI, rad = R * (0.9f + 0.1f * r.nextFloat());
            float h = (0.9f + 0.7f * r.nextFloat()) * Math.max(1f, R * 0.4f) * (0.8f + 0.2f * Mth.sin(age * 0.2f + ph)) * open;
            Vector3f base = new Vector3f(c).add(Mth.cos(a) * rad, 0.05f, Mth.sin(a) * rad);
            Vector3f mid = new Vector3f(base).add(-Mth.cos(a) * 0.12f * h, h * 0.55f, -Mth.sin(a) * 0.12f * h);
            Vector3f top = new Vector3f(base).add(-Mth.cos(a) * 0.38f * h, h, -Mth.sin(a) * 0.38f * h);
            strip(ctx, buf, STREAM, VfxBlend.WATER, base, mid, 0.34f, 0.3f, 1, VfxVertexBuffer.withAlpha(col, 0.85f * fade), VfxVertexBuffer.withAlpha(col, 0.85f * fade));
            strip(ctx, buf, STREAM, VfxBlend.WATER, mid, top, 0.3f, 0.12f, 1, VfxVertexBuffer.withAlpha(col, 0.85f * fade), VfxVertexBuffer.withAlpha(pale, 0.4f * fade));
        }
        int dn = ctx.seg(14, 6);
        for (int i = 0; i < dn; i++) {                                                          // drops falling back
            float life = Mth.frac(age * 0.03f * (1f + 0.4f * r.nextFloat()) + r.nextFloat()), a = r.nextFloat() * Mth.TWO_PI, d = Mth.sqrt(r.nextFloat()) * R;
            buf.billboard(ctx, DROP, VfxBlend.ADD, new Vector3f(c).add(Mth.cos(a) * d, 0.2f + (1f - life) * (0.8f + R * 0.3f), Mth.sin(a) * d), 0.2f, 0f,
                    VfxVertexBuffer.withAlpha(pale, Mth.sin(life * Mth.PI) * fade));
        }
    }

    // ------------------------------------------------------------------ WATERFX_BURST
    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), t = inst.progress(ctx.partialTick), p = inst.power, fade = 1f - VfxAnim.easeInCubic(t);
        int col = inst.color, pale = VfxVertexBuffer.whiten(col, 0.65f);
        Vector3f c = ctx.rel(inst.from(ctx));
        float flash = Mth.clamp(1f - age / 6f, 0, 1);
        VfxBloom.glow(ctx, buf, c, 0.8f * p * (0.5f + VfxAnim.easeOutCubic(t)), col, fade * 0.8f);
        buf.billboard(ctx, SPRAY, VfxBlend.ADD, c, (1.4f + 2.2f * VfxAnim.easeOutCubic(Mth.clamp(t * 2f, 0, 1))) * p, age * 0.04f, VfxVertexBuffer.withAlpha(pale, Math.min(1f, 0.3f + flash) * fade));
        for (int k = 0; k < 3; k++) {                                                           // three ripples
            float lt = Mth.clamp(t * 1.5f - k * 0.18f, 0, 1);
            if (lt <= 0f) continue;
            float rr = (0.4f + 2.6f * VfxAnim.easeOutCubic(lt)) * p;
            buf.plane(RIPPLE, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, 0.07f + 0.01f * k, 0)).spin(age * 0.05f), rr, VfxVertexBuffer.withAlpha(k == 1 ? pale : col, (1f - lt) * 0.9f));
        }
        RandomSource r = inst.random();
        int n = ctx.seg(8, 4);
        for (int i = 0; i < n; i++) {                                                           // wave crests thrown outward
            float a = i * Mth.TWO_PI / n + r.nextFloat() * 0.3f, reach = (0.8f + 1.0f * r.nextFloat()) * p * VfxAnim.easeOutCubic(Mth.clamp(t * 1.4f, 0, 1));
            float h = (0.6f + 0.5f * r.nextFloat()) * p * Mth.sin(Mth.clamp(t * 1.3f, 0, 1) * Mth.PI);
            upright(ctx, buf, WAVE, VfxBlend.WATER, new Vector3f(c).add(Mth.cos(a) * reach, 0.05f, Mth.sin(a) * reach), 1.6f * p, h, VfxVertexBuffer.withAlpha(col, 0.9f * fade));
        }
        int e = ctx.seg(16, 6);
        for (int i = 0; i < e; i++) {                                                           // drops on arcs
            float a = r.nextFloat() * Mth.TWO_PI, v = (1.0f + 2.4f * r.nextFloat()) * p, vy = (1.6f + 2.2f * r.nextFloat()) * p, tt = t * 1.2f;
            Vector3f q = new Vector3f(c).add(Mth.cos(a) * v * tt * 0.7f, 0.1f + vy * tt - 2.8f * tt * tt * p, Mth.sin(a) * v * tt * 0.7f);
            buf.billboard(ctx, DROP, VfxBlend.ADD, q, 0.24f * p * (1f - 0.5f * t), 0f, VfxVertexBuffer.withAlpha(pale, fade));
        }
    }
}
