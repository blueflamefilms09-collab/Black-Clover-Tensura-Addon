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
 * 1.93.0 Hair Magic (rebuilt; it used the shared wiki layer before). Pink locks of hair with a lit gloss: curling strands, tufts, wrapping tendrils, soft glow. Textures from tools/gen_hair_textures.py (hair_*).
 * <ul>
 *   <li>HAIR_FX1 (cast / projectile, default 16 ticks): the Hair Lash: three strands of hair twist from -> to along a curving arc, each tipped with a tuft, and wrap round the target in a flash and a curl ring. from = origin, to = target, power = size (0.6 to 3), colour = tint mixed with the palette.</li>
 *   <li>HAIR_FX2 (zone / field, default 80 ticks): the Mane Field: a curling seal turns on the ground inside a rim ring, long locks grow out of the ground round the rim and all over the field and sway, tufts rise, a pulse runs out. from = centre on the ground, power = RADIUS in blocks; fades in over 8 ticks and out over the last 12.</li>
 *   <li>HAIR_FX3 (impact / signature, default 28 ticks): the Binding Burst: locks spiral inward round the point and tighten, a pink flash and a curl ring, tufts puff out and settle. from = centre, to - from = a direction hint (may be zero), power = scale.</li>
 * </ul>
 * At most 400 vertices per activation (every sprite goes through bb / hasBudget); sprites are layered back to front: dark solid (ALPHA) first, glows (ADD) last.
 */
public class HairLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final ResourceLocation SIGIL = t("hair_sigil"), RING = t("hair_ring"), FLARE = t("hair_flare"), STREAK = t("hair_streak"), GLOW = t("hair_glow"),
            CRACK = t("hair_crack"), MOTE = t("hair_mote"), LOCK = t("hair_lock"), LOCK2 = t("hair_lock2"), STRAND = t("hair_strand"), TUFT = t("hair_tuft");
    static final int PINK = 0xFFFF6E9A, DEEP = 0xFFC0305F, PALE = 0xFFFFC4DA, HOT = 0xFFFFF0F6;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.HAIR_FX1, VfxShape.HAIR_FX2, VfxShape.HAIR_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case HAIR_FX1 -> 16;
            case HAIR_FX2 -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFFF6E9A; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.HAIR_FX3) VfxShake.add(inst.payload.from(), 0.15f * Mth.clamp(inst.power, 0.6f, 2f), 7);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case HAIR_FX1 -> cast(inst, ctx, buf);
            case HAIR_FX2 -> zone(inst, ctx, buf);
            case HAIR_FX3 -> impact(inst, ctx, buf);
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

    private static Vector3f bez(Vector3f p0, Vector3f p1, Vector3f p2, float s) {
        float u = 1 - s;
        return new Vector3f(p0).mul(u * u).add(new Vector3f(p1).mul(2 * u * s)).add(new Vector3f(p2).mul(s * s));
    }

    // ------------------------------------------------------------------------------------------------ FX1: the Hair Lash
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1, inst.duration), t = Mth.clamp(age / dur, 0f, 1f);
        float p = Mth.clamp(inst.power, 0.5f, 3.2f);
        Vector3f f = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f d = new Vector3f(to).sub(f);
        float len = d.length();
        if (len < 0.3f) { d.set(0, 0, 1); len = 3f * p; to = new Vector3f(f).add(0, 0, len); } else d.div(len);
        int pink = mix(PINK, inst), deep = mix(DEEP, inst), pale = mix(PALE, inst);
        float cu = Mth.clamp(t / 0.2f, 0f, 1f), u = Mth.clamp((t - 0.1f) / 0.55f, 0f, 1f);
        float e = ease(u), imp = Mth.clamp((t - 0.62f) / 0.38f, 0f, 1f), live = 1f - VfxAnim.easeInCubic(imp);
        float chargeA = cu * (1f - Mth.clamp((t - 0.18f) / 0.2f, 0f, 1f));
        bb(buf, ctx, SIGIL, VfxBlend.ADD, f, (2.0f - 1.3f * ease(cu)) * p, age * 0.15f, pink, chargeA);
        bb(buf, ctx, GLOW, VfxBlend.ADD, f, (0.8f + 0.7f * cu) * p, 0, pink, chargeA * 0.8f);
        Vector3f side = new Vector3f(d).cross(new Vector3f(0, 1, 0));
        if (side.lengthSquared() < 1e-4f) side.set(1, 0, 0);
        side.normalize();
        Vector3f up = new Vector3f(side).cross(d).normalize();
        if (u > 0f) {
            for (int k = 0; k < 3; k++) {                                                          // three strands on curving arcs
                float sgn = k - 1f;
                Vector3f mid = lerp3(f, to, 0.5f).fma(sgn * 0.9f * p, side).fma((0.6f + 0.25f * k) * p, up);
                int segs = ctx.seg(6, 3);
                Vector3f prev = f;
                for (int sIdx = 1; sIdx <= segs; sIdx++) {
                    float s = e * sIdx / segs;
                    Vector3f pos = bez(f, mid, to, s);
                    float wob = 0.12f * p * Mth.sin(s * 9f - age * 0.5f + k * 2f);
                    pos.fma(wob, up);
                    buf.beam(ctx, STREAK, VfxBlend.ADD, prev, pos, 0.28f * p, 0.28f * p, 1, 0f, VfxVertexBuffer.withAlpha(k == 1 ? pale : pink, 0.8f * live * (sIdx / (float) segs)), VfxVertexBuffer.withAlpha(pale, 0.85f * live));
                    if (sIdx % 2 == 0) bb(buf, ctx, STRAND, VfxBlend.ALPHA, pos, 0.45f * p, age * 0.2f + sIdx + k, k % 2 == 0 ? deep : pink, 0.9f * live);
                    prev = pos;
                }
                bb(buf, ctx, TUFT, VfxBlend.ALPHA, prev, 0.55f * p, age * 0.4f + k, pink, 0.95f * live);
                bb(buf, ctx, GLOW, VfxBlend.ADD, prev, 0.9f * p, 0, pale, 0.5f * live);
            }
        }
        if (imp > 0f) {
            float s = ease(imp);
            bb(buf, ctx, FLARE, VfxBlend.ADD, to, (0.8f + 2.0f * s) * p, 0.2f + imp, HOT, 1f - imp);
            bb(buf, ctx, SIGIL, VfxBlend.ADD, to, (0.5f + 2.0f * s) * p, -imp * 2f, pink, 0.9f * (1f - imp));
            RandomSource r = inst.random();
            for (int k = 0; k < 4; k++) {
                float a = r.nextFloat() * Mth.TWO_PI, sp = (0.4f + r.nextFloat() * 0.8f) * p * s;
                Vector3f cp = new Vector3f(to).fma(Mth.cos(a) * sp, ctx.camRight).fma(Mth.sin(a) * sp, ctx.camUp);
                bb(buf, ctx, TUFT, VfxBlend.ALPHA, cp, 0.3f * p, a + age * 0.3f, pink, 1f - imp);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ FX2: the Mane Field
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1, inst.duration);
        float R = Math.max(1f, inst.power), fade = life(inst, age, 8f, 12f);
        if (fade <= 0f) return;
        Vector3f c = ctx.rel(inst.from(ctx));
        int pink = mix(PINK, inst), deep = mix(DEEP, inst), pale = mix(PALE, inst);
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 14f, 0f, 1f)), pulse = VfxAnim.pulse(age, 0.9f);
        VfxPose ground = VfxPose.ground(c);
        buf.plane(SIGIL, VfxBlend.ADD, ground.lift(0.03f).spin(age * 0.01f), R * open, VfxVertexBuffer.withAlpha(pink, (0.5f + 0.2f * pulse) * fade));
        buf.plane(SIGIL, VfxBlend.ADD, ground.lift(0.05f).spin(-age * 0.018f), R * 0.55f * open, VfxVertexBuffer.withAlpha(pale, 0.35f * fade));
        int seg = ctx.seg(16, 10);
        float band = Math.max(0.22f, R * 0.07f);
        buf.ring(RING, VfxBlend.ADD, ground.lift(0.07f), R * open - band, R * open, seg, Math.max(2, Math.round(Mth.TWO_PI * R / (6f * band))), age * 0.005f, VfxVertexBuffer.withAlpha(pink, 0.8f * fade));
        RandomSource rnd = inst.random();
        int n = Mth.clamp(Math.round(R * 3.5f), 8, 14);
        for (int i = 0; i < n; i++) {                                                             // long locks growing out of the ground, swaying
            float a = rnd.nextFloat() * Mth.TWO_PI, rad = (i < n / 2 ? 0.95f : 0.15f + 0.7f * rnd.nextFloat()) * R;
            float ri = VfxAnim.easeOutBack(Mth.clamp((age - 2f - i * 1.1f) / 10f, 0f, 1f)), hh = (1.0f + 0.9f * rnd.nextFloat()) * Mth.clamp(R * 0.5f, 0.9f, 1.8f) * ri;
            float sway = 0.18f * Mth.sin(age * 0.09f + i * 1.7f);
            Vector3f pos = at(c, Mth.cos(a) * rad, 0.04f, Mth.sin(a) * rad);
            stand(buf, (i & 1) == 0 ? LOCK : LOCK2, VfxBlend.ALPHA, pos, a + 1.5708f + sway, 0.55f + 0.15f * rnd.nextFloat(), hh, Mth.cos(a + 1.5708f), Mth.sin(a + 1.5708f), sway * 1.5f, VfxVertexBuffer.withAlpha((i & 1) == 0 ? pink : deep, 0.93f * fade));
            bb(buf, ctx, GLOW, VfxBlend.ADD, at(pos, 0, hh, 0), 0.3f, 0, pale, 0.4f * fade * Mth.clamp(ri, 0f, 1f));
        }
        for (int i = 0; i < 9; i++) {
            float a = rnd.nextFloat() * Mth.TWO_PI, rad = (0.15f + 0.8f * rnd.nextFloat()) * R, sp = 0.012f + 0.012f * rnd.nextFloat();
            float life = ((age * sp * 8f + rnd.nextFloat()) % 1f), y = life * Mth.clamp(R * 0.9f, 1.2f, 3f);
            bb(buf, ctx, TUFT, VfxBlend.ADD, at(c, Mth.cos(a + age * 0.01f) * rad, 0.15f + y, Mth.sin(a + age * 0.01f) * rad), 0.24f, age * 0.1f + i, pale, Mth.sin(life * Mth.PI) * fade);
        }
        float ph = (age % 28f) / 28f;
        buf.plane(RING, VfxBlend.ADD, ground.lift(0.09f), R * (0.25f + 0.8f * ease(ph)), VfxVertexBuffer.withAlpha(pale, (1f - ph) * 0.6f * fade));
    }

    // ------------------------------------------------------------------------------------------------ FX3: the Binding Burst
    private void impact(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1, inst.duration), t = Mth.clamp(age / dur, 0f, 1f);
        float p = Mth.clamp(inst.power, 0.5f, 3.2f);
        Vector3f c = ctx.rel(inst.from(ctx));
        int pink = mix(PINK, inst), deep = mix(DEEP, inst), pale = mix(PALE, inst);
        VfxPose ground = VfxPose.ground(c);
        float s = ease(t), flashA = Mth.clamp(1f - t * 3f, 0f, 1f);
        buf.plane(SIGIL, VfxBlend.ADD, ground.lift(0.04f).spin(t * 2f), (0.5f + 2.2f * s) * p, VfxVertexBuffer.withAlpha(pink, (1f - t) * 0.9f));
        buf.plane(RING, VfxBlend.ADD, ground.lift(0.06f), (0.3f + 2.8f * s) * p, VfxVertexBuffer.withAlpha(pale, (1f - t) * 0.8f));
        bb(buf, ctx, FLARE, VfxBlend.ADD, at(c, 0, 0.5f * p, 0), (1.4f + 4.0f * s) * p, 0.35f + t, HOT, flashA);
        bb(buf, ctx, GLOW, VfxBlend.ADD, at(c, 0, 0.6f * p, 0), (1.4f + 2.2f * s) * p, 0, pink, (1f - t) * 0.8f);
        int n = 7;
        for (int i = 0; i < n; i++) {                                                             // locks spiralling inward and tightening
            float lag = i * 0.03f, k = Mth.clamp((t - lag) * 1.6f, 0f, 1f), a = Mth.TWO_PI * i / n + age * 0.35f * (i % 2 == 0 ? 1f : -1f) * (1f - 0.5f * k);
            float rad = (1.6f - 1.0f * VfxAnim.easeInOutSine(k)) * p, y = (0.2f + 1.3f * k) * p * (0.4f + 0.6f * (i % 3) / 2f);
            Vector3f pos = at(c, Mth.cos(a) * rad, y, Mth.sin(a) * rad);
            float al = Mth.clamp(k * 4f, 0f, 1f) * (1f - VfxAnim.easeInCubic(Mth.clamp((t - 0.6f) / 0.4f, 0f, 1f)));
            bb(buf, ctx, i % 2 == 0 ? LOCK : LOCK2, VfxBlend.ALPHA, pos, 0.9f * p, a + 1.5708f, i % 2 == 0 ? pink : deep, al);
        }
        RandomSource r = inst.random();
        float sec = age / 20f;
        for (int i = 0; i < 8; i++) {
            float az = r.nextFloat() * Mth.TWO_PI, sp = (0.8f + r.nextFloat() * 1.2f) * p, vy = (2.0f + r.nextFloat() * 2.5f) * p, spin = (r.nextFloat() - 0.5f) * 0.5f;
            float hor = sp * (1f - (float) Math.exp(-2.6f * sec)) / 2.6f * 2.0f, y = Math.max(0f, vy * sec - 3.6f * p * sec * sec);
            Vector3f pos = at(c, Mth.cos(az) * hor, 0.2f + y, Mth.sin(az) * hor);
            float al = 1f - VfxAnim.easeInCubic(Mth.clamp((t - 0.5f) / 0.5f, 0f, 1f));
            bb(buf, ctx, TUFT, VfxBlend.ALPHA, pos, 0.3f * p, age * spin + i, i % 2 == 0 ? pink : pale, al);
        }
    }
}
