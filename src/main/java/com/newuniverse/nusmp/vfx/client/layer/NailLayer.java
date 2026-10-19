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
 * 1.93.0 Nail Magic (rebuilt; it used the shared wiki layer before). Dark iron and pale steel nails with ember-hot heads: driven nails, a falling hammer, sparks. Textures from tools/gen_nail_textures.py (nail_*).
 * <ul>
 *   <li>NAIL_FX1 (cast / projectile, default 16 ticks): the Nail Barrage: three nails in a loose triple fly from -> to point first with hot heads and spark trails and are driven into the target in a flash, a ring and sparks. from = origin, to = target, power = size (0.6 to 3), colour = tint mixed with the palette.</li>
 *   <li>NAIL_FX2 (zone / field, default 80 ticks): the Nail Storm: a nail seal turns on the ground inside a rim ring, nails fall out of the sky all over the field and stay driven in the ground, sparks rise, a pulse runs out. from = centre on the ground, power = RADIUS in blocks; fades in over 8 ticks and out over the last 12.</li>
 *   <li>NAIL_FX3 (impact / signature, default 28 ticks): the Driver: a giant hammer drops onto the point (from the opposite side of the hint, straight down without one), a flash, two shock rings, the ground cracks, nails shoot out of the point in a star and stay for a moment, chips fly. from = centre, to - from = a direction hint (may be zero), power = scale.</li>
 * </ul>
 * At most 400 vertices per activation (every sprite goes through bb / hasBudget); sprites are layered back to front: dark solid (ALPHA) first, glows (ADD) last.
 */
public class NailLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final ResourceLocation SIGIL = t("nail_sigil"), RING = t("nail_ring"), FLARE = t("nail_flare"), STREAK = t("nail_streak"), GLOW = t("nail_glow"),
            CRACK = t("nail_crack"), MOTE = t("nail_mote"), NAIL = t("nail_nail"), SMALL = t("nail_nail_small"), HAMMER = t("nail_hammer"), SPARK = t("nail_spark");
    static final int IRON = 0xFFB8AFA0, DARK = 0xFF3A322E, EMBER = 0xFFFFA050, HOT = 0xFFFFE6C0;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.NAIL_FX1, VfxShape.NAIL_FX2, VfxShape.NAIL_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case NAIL_FX1 -> 16;
            case NAIL_FX2 -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFC7B9A6; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.NAIL_FX3) VfxShake.add(inst.payload.from(), 0.4f * Mth.clamp(inst.power, 0.6f, 2f), 7);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case NAIL_FX1 -> cast(inst, ctx, buf);
            case NAIL_FX2 -> zone(inst, ctx, buf);
            case NAIL_FX3 -> impact(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------------------------------------ helpers
    /** A palette colour pulled a quarter of the way toward the spell tint. */
    private static int mix(int palette, VfxInstance inst) { return VfxVertexBuffer.lerpColor(palette, inst.color | 0xFF000000, 0.25f); }

    private static void bb(VfxVertexBuffer b, VfxRenderContext ctx, ResourceLocation tex, VfxBlend bl, Vector3f p, float size, float rot, int col, float a) {
        if (a <= 0.01f || size <= 0.001f || !b.hasBudget(4)) return;
        b.billboard(ctx, tex, bl, p, size, rot, VfxVertexBuffer.withAlpha(col, Mth.clamp(a, 0, 1)));
    }

    private static Vector3f at(Vector3f o, float x, float y, float z) { return new Vector3f(o).add(x, y, z); }

    private static Vector3f lerp3(Vector3f a, Vector3f b, float t) { return new Vector3f(a).lerp(b, t); }

    /** Screen-plane angle of a world direction, so a billboard's x axis points along it. */
    private static float screenAngle(VfxRenderContext ctx, Vector3f d) { return (float) Math.atan2(d.dot(ctx.camUp), d.dot(ctx.camRight)); }

    private static float life(VfxInstance inst, float age, float in, float out) {
        float a = in <= 0 ? 1 : Mth.clamp(age / in, 0, 1), b = out <= 0 ? 1 : Mth.clamp((inst.duration - age) / out, 0, 1);
        return Math.min(a, b);
    }

    /** A vertical sprite standing on base (its bottom edge), facing the direction yaw, half width w, height h, leaning toward (lx, lz) by 'lean' of its height. */
    private static void stand(VfxVertexBuffer b, ResourceLocation tex, VfxBlend bl, Vector3f base, float yaw, float w, float h, float lx, float lz, float lean, int col) {
        if (h <= 0.01f || !b.hasBudget(4)) return;
        float ux = Mth.cos(yaw) * w, uz = Mth.sin(yaw) * w;
        Vector3f l = new Vector3f(base).add(-ux, 0, -uz), r = new Vector3f(base).add(ux, 0, uz);
        Vector3f top = new Vector3f(lx * lean * h, h, lz * lean * h);
        int c = bl.grade(col, 1f);
        b.quad(tex, bl, l, r, new Vector3f(r).add(top), new Vector3f(l).add(top), 0, 0, 1, 1, c, c);
    }

    private static float ease(float x) { return VfxAnim.easeOutCubic(Mth.clamp(x, 0f, 1f)); }

    // ------------------------------------------------------------------------------------------------ FX1: the Nail Barrage
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1, inst.duration), t = Mth.clamp(age / dur, 0f, 1f);
        float p = Mth.clamp(inst.power, 0.5f, 3.2f);
        Vector3f f = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f d = new Vector3f(to).sub(f);
        float len = d.length();
        if (len < 0.3f) { d.set(0, 0, 1); len = 3f * p; to = new Vector3f(f).add(0, 0, len); } else d.div(len);
        int iron = mix(IRON, inst), ember = mix(EMBER, inst);
        float rot = screenAngle(ctx, d) - 1.5708f;
        float cu = Mth.clamp(t / 0.2f, 0f, 1f), u = Mth.clamp((t - 0.1f) / 0.62f, 0f, 1f);
        float e = 0.8f * ease(u) + 0.2f * u, imp = Mth.clamp((t - 0.74f) / 0.26f, 0f, 1f), live = 1f - VfxAnim.easeInCubic(imp);
        float chargeA = cu * (1f - Mth.clamp((t - 0.18f) / 0.2f, 0f, 1f));
        bb(buf, ctx, SIGIL, VfxBlend.ADD, f, (2.0f - 1.3f * ease(cu)) * p, -age * 0.15f, ember, chargeA);
        bb(buf, ctx, GLOW, VfxBlend.ADD, f, (0.8f + 0.6f * cu) * p, 0, ember, chargeA * 0.8f);
        bb(buf, ctx, FLARE, VfxBlend.ADD, f, (0.5f + 1.2f * cu) * p, 0.4f, HOT, chargeA * 0.9f);
        Vector3f side = new Vector3f(d).cross(new Vector3f(0, 1, 0));
        if (side.lengthSquared() < 1e-4f) side.set(1, 0, 0);
        side.normalize();
        Vector3f up = new Vector3f(side).cross(d).normalize();
        float[] ox = {0f, -0.45f, 0.45f}, oy = {0f, 0.3f, 0.25f}, lagk = {0f, 0.05f, 0.09f};
        for (int k = 0; k < 3; k++) {
            float uk = Mth.clamp(u - lagk[k], 0f, 1f), ek = 0.8f * ease(uk) + 0.2f * uk;
            if (uk <= 0f) continue;
            float fan = Mth.clamp(1f - ek * 1.4f, 0.1f, 1f);
            Vector3f pos = lerp3(f, to, ek).fma(ox[k] * p * fan, side).fma(oy[k] * p * fan, up);
            Vector3f tail = new Vector3f(pos).fma(-Math.min(ek * len, 1.8f * p), d);
            buf.beam(ctx, STREAK, VfxBlend.ADD, tail, pos, 0.1f * p, 0.35f * p, 2, -age * 0.15f, VfxVertexBuffer.withAlpha(ember, 0f), VfxVertexBuffer.withAlpha(HOT, 0.8f * live));
            bb(buf, ctx, NAIL, VfxBlend.ALPHA, pos, 0.9f * p, rot, iron, 0.98f * live);
            bb(buf, ctx, SPARK, VfxBlend.ADD, new Vector3f(pos).fma(-0.4f * p, d), 0.35f * p, age * 0.3f + k, ember, 0.9f * live);
        }
        if (imp > 0f) {
            float s = ease(imp);
            bb(buf, ctx, FLARE, VfxBlend.ADD, to, (0.8f + 2.0f * s) * p, 0.2f + imp, HOT, 1f - imp);
            bb(buf, ctx, RING, VfxBlend.ADD, to, (0.5f + 2.2f * s) * p, 0f, ember, 0.9f * (1f - imp));
            bb(buf, ctx, NAIL, VfxBlend.ALPHA, new Vector3f(to).fma(-0.5f * p, d), 0.8f * p, rot, iron, 0.9f * (1f - imp * 0.6f));
            RandomSource r = inst.random();
            for (int k = 0; k < 5; k++) {
                float a = r.nextFloat() * Mth.TWO_PI, sp = (0.5f + r.nextFloat() * 0.9f) * p * s;
                Vector3f cp = new Vector3f(to).fma(Mth.cos(a) * sp, ctx.camRight).fma(Mth.sin(a) * sp, ctx.camUp);
                bb(buf, ctx, SPARK, VfxBlend.ADD, cp, 0.3f * p, a + age * 0.4f, ember, 1f - imp);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ FX2: the Nail Storm
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1, inst.duration);
        float R = Math.max(1f, inst.power), fade = life(inst, age, 8f, 12f);
        if (fade <= 0f) return;
        Vector3f c = ctx.rel(inst.from(ctx));
        int iron = mix(IRON, inst), ember = mix(EMBER, inst), dark = mix(DARK, inst);
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 14f, 0f, 1f)), pulse = VfxAnim.pulse(age, 1.2f);
        VfxPose ground = VfxPose.ground(c);
        buf.plane(CRACK, VfxBlend.ALPHA, ground.lift(0.015f).spin(0.9f), R * 1.02f * open, VfxVertexBuffer.withAlpha(dark, 0.8f * fade));
        buf.plane(SIGIL, VfxBlend.ADD, ground.lift(0.03f).spin(-age * 0.014f), R * open, VfxVertexBuffer.withAlpha(ember, (0.5f + 0.2f * pulse) * fade));
        int seg = ctx.seg(16, 10);
        float band = Math.max(0.22f, R * 0.07f);
        buf.ring(RING, VfxBlend.ADD, ground.lift(0.07f), R * open - band, R * open, seg, Math.max(2, Math.round(Mth.TWO_PI * R / (6f * band))), age * 0.005f, VfxVertexBuffer.withAlpha(ember, 0.75f * fade));
        RandomSource rnd = inst.random();
        float top = Mth.clamp(R * 1.2f, 3f, 5f);
        int n = Mth.clamp(Math.round(R * 3f), 7, 12);
        for (int i = 0; i < n; i++) {                                                             // nails falling, driven in, and standing there
            float a = rnd.nextFloat() * Mth.TWO_PI, rad = Mth.sqrt(rnd.nextFloat()) * R * 0.95f, beat = 22f + 10f * rnd.nextFloat();
            float ph = ((age + rnd.nextFloat() * beat * 2f) % (beat * 2f)) / beat;             // 0..1 falling, 1..2 standing in the ground
            Vector3f pos = at(c, Mth.cos(a) * rad, 0f, Mth.sin(a) * rad);
            if (ph < 1f) {
                float y = top * (1f - VfxAnim.easeInCubic(ph));
                Vector3f p0 = at(pos, 0, y + 0.8f, 0), p1 = at(pos, 0, y, 0);
                buf.beam(ctx, STREAK, VfxBlend.ADD, at(p1, 0, 1.6f, 0), p1, 0.12f, 0.12f, 1, 0f, VfxVertexBuffer.withAlpha(ember, 0f), VfxVertexBuffer.withAlpha(HOT, 0.6f * fade));
                stand(buf, NAIL, VfxBlend.ALPHA, at(pos, 0, y, 0), 0f, 0.3f, 1.0f, 0, 0, 0f, VfxVertexBuffer.withAlpha(iron, 0.95f * fade));
            } else {
                float land = ph - 1f;
                stand(buf, NAIL, VfxBlend.ALPHA, at(pos, 0, -0.1f, 0), (float) a, 0.3f, 0.85f, 0, 0, 0f, VfxVertexBuffer.withAlpha(iron, 0.95f * fade * (1f - land * 0.7f)));
                if (land < 0.25f) bb(buf, ctx, FLARE, VfxBlend.ADD, at(pos, 0, 0.1f, 0), 0.7f * (1f - land * 4f), land * 6f, HOT, fade);
            }
        }
        for (int i = 0; i < 7; i++) {
            float a = rnd.nextFloat() * Mth.TWO_PI, rad = (0.15f + 0.8f * rnd.nextFloat()) * R, sp = 0.012f + 0.012f * rnd.nextFloat();
            float life = ((age * sp * 8f + rnd.nextFloat()) % 1f), y = life * Mth.clamp(R * 0.9f, 1.2f, 3f);
            bb(buf, ctx, SPARK, VfxBlend.ADD, at(c, Mth.cos(a + age * 0.01f) * rad, 0.15f + y, Mth.sin(a + age * 0.01f) * rad), 0.25f, age * 0.1f + i, ember, Mth.sin(life * Mth.PI) * fade);
        }
        float ph = (age % 26f) / 26f;
        buf.plane(RING, VfxBlend.ADD, ground.lift(0.09f), R * (0.25f + 0.8f * ease(ph)), VfxVertexBuffer.withAlpha(HOT, (1f - ph) * 0.6f * fade));
    }

    // ------------------------------------------------------------------------------------------------ FX3: the Driver
    private void impact(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1, inst.duration), t = Mth.clamp(age / dur, 0f, 1f);
        float p = Mth.clamp(inst.power, 0.5f, 3.2f);
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f hint = new Vector3f(ctx.rel(inst.to(ctx))).sub(c);
        Vector3f axis = hint.lengthSquared() < 0.04f ? new Vector3f(0, -1, 0) : hint.normalize();
        int iron = mix(IRON, inst), ember = mix(EMBER, inst), dark = mix(DARK, inst);
        VfxPose ground = VfxPose.ground(c);
        float hitT = 0.2f, after = Mth.clamp((t - hitT) / (1f - hitT), 0f, 1f), s = ease(after), flashA = Mth.clamp(1f - after * 3.2f, 0f, 1f);
        if (t >= hitT) {
            float fadeC = 1f - VfxAnim.easeInCubic(Mth.clamp((after - 0.4f) / 0.6f, 0f, 1f));
            buf.plane(CRACK, VfxBlend.ALPHA, ground.lift(0.02f).spin(0.6f), 2.2f * p * (0.3f + 0.7f * s), VfxVertexBuffer.withAlpha(dark, 0.85f * fadeC));
            buf.plane(CRACK, VfxBlend.ADD, ground.lift(0.04f).spin(0.6f), 2.2f * p * (0.3f + 0.7f * s), VfxVertexBuffer.withAlpha(ember, (1f - after) * 0.9f));
            buf.plane(RING, VfxBlend.ADD, ground.lift(0.07f), (0.3f + 3.0f * s) * p, VfxVertexBuffer.withAlpha(ember, (1f - after) * 0.95f));
            buf.plane(RING, VfxBlend.ADD, ground.lift(0.08f).spin(1f), (0.2f + 1.9f * ease(after * 1.5f)) * p, VfxVertexBuffer.withAlpha(HOT, flashA));
            buf.plane(SIGIL, VfxBlend.ADD, ground.lift(0.05f).spin(after), (0.5f + 1.5f * s) * p, VfxVertexBuffer.withAlpha(ember, (1f - after) * 0.6f));
        }
        if (t < hitT + 0.08f) {                                                                    // the hammer drops
            float fall = Mth.clamp(t / hitT, 0f, 1f), fe = VfxAnim.easeInCubic(fall);
            Vector3f from = new Vector3f(c).fma(-4.2f * p, axis), hp = lerp3(from, c, fe);
            float vis = t < hitT ? 1f : 1f - (t - hitT) / 0.08f, rot = screenAngle(ctx, axis) + 3.1416f;
            buf.beam(ctx, STREAK, VfxBlend.ADD, from, hp, 0.2f * p, 1.5f * p, 3, -age * 0.2f, VfxVertexBuffer.withAlpha(ember, 0f), VfxVertexBuffer.withAlpha(HOT, 0.7f * Mth.clamp(fall * 3f, 0, 1) * vis));
            bb(buf, ctx, HAMMER, VfxBlend.ALPHA, hp, 2.4f * p, rot, iron, vis * Mth.clamp(fall * 4f, 0, 1));
            bb(buf, ctx, GLOW, VfxBlend.ADD, hp, 2.8f * p, 0, ember, 0.45f * fall * vis);
        }
        if (t >= hitT) {
            bb(buf, ctx, FLARE, VfxBlend.ADD, at(c, 0, 0.3f * p, 0), (1.5f + 5.0f * s) * p, 0.35f + after, HOT, flashA);
            bb(buf, ctx, GLOW, VfxBlend.ADD, at(c, 0, 0.5f * p, 0), (1.6f + 2.4f * s) * p, 0, ember, (1f - after) * 0.8f);
            int n = 9;
            for (int i = 0; i < n; i++) {                                                          // nails shot out of the point, standing for a moment
                float a = Mth.TWO_PI * i / n + 0.3f, el = 0.3f + 0.5f * (i % 3) / 2f, out = (0.5f + 1.2f * s) * p, len = (0.7f + 0.3f * (i % 3)) * p * ease(after * 2.5f) * (1f - VfxAnim.easeInCubic(Mth.clamp((after - 0.5f) / 0.5f, 0f, 1f)));
                Vector3f dir = new Vector3f(Mth.cos(a) * Mth.cos(el), Mth.sin(el), Mth.sin(a) * Mth.cos(el));
                float rot = screenAngle(ctx, dir) - 1.5708f;
                bb(buf, ctx, SMALL, VfxBlend.ALPHA, at(c, dir.x * out, 0.1f + dir.y * out, dir.z * out), len, rot, iron, 0.95f * (1f - after * 0.5f));
            }
            RandomSource r = inst.random();
            float sec = (age - hitT * dur) / 20f;
            for (int i = 0; i < 8; i++) {
                float az = r.nextFloat() * Mth.TWO_PI, sp = (1.0f + r.nextFloat() * 1.5f) * p, vy = (3.0f + r.nextFloat() * 3.0f) * p, spin = (r.nextFloat() - 0.5f) * 0.6f;
                float tt = Math.max(0f, sec), hor = sp * (1f - (float) Math.exp(-3.2f * tt)) / 3.2f * 2.2f, y = Math.max(0f, vy * tt - 5.5f * p * tt * tt);
                Vector3f pos = at(c, Mth.cos(az) * hor, 0.15f + y, Mth.sin(az) * hor);
                float al = 1f - VfxAnim.easeInCubic(Mth.clamp((after - 0.55f) / 0.45f, 0f, 1f));
                bb(buf, ctx, SPARK, VfxBlend.ADD, pos, 0.32f * p, age * spin + i, i % 2 == 0 ? ember : HOT, al * 0.9f);
            }
        }
    }
}
