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
 * 1.93.0 Scale Magic (rebuilt; it used the shared wiki layer before). Teal fish and dragon scales that catch the light: single scales flying, water-like ripple rings, swaying fins, pale gleams. Textures from tools/gen_scale_textures.py (scale_*).
 * <ul>
 *   <li>SCALE_FX1 (cast / projectile, default 16 ticks): the Scale Volley: five scales spiral from -> to round a streak of water-light, each leaving a ripple, and burst into a ripple ring and gleams at the target. from = origin, to = target, power = size (0.6 to 3), colour = tint mixed with the palette.</li>
 *   <li>SCALE_FX2 (zone / field, default 80 ticks): the Shimmering Shoal: ripples run out over the ground inside a rim, a ring of scales turns at chest height, fins sway round the rim, gleams drift up. from = centre on the ground, power = RADIUS in blocks; fades in over 8 ticks and out over the last 12.</li>
 *   <li>SCALE_FX3 (impact / signature, default 28 ticks): the Shedding Tide: a flash, two ripple rings running out, a crown of fins, scales thrown out in arcs that settle, gleams. from = centre, to - from = a direction hint (may be zero), power = scale.</li>
 * </ul>
 * At most 400 vertices per activation (every sprite goes through bb / hasBudget); sprites are layered back to front: dark solid (ALPHA) first, glows (ADD) last.
 */
public class ScaleLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final ResourceLocation SIGIL = t("scale_sigil"), RING = t("scale_ring"), FLARE = t("scale_flare"), STREAK = t("scale_streak"), GLOW = t("scale_glow"),
            CRACK = t("scale_crack"), MOTE = t("scale_mote"), SCALE = t("scale_scale"), RIPPLE = t("scale_ripple"), FIN = t("scale_fin"), GLEAM = t("scale_gleam");
    static final int TEAL = 0xFF3CC2BC, DEEP = 0xFF14707A, PALE = 0xFFC0FFF2, HOT = 0xFFEFFFFA;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.SCALE_FX1, VfxShape.SCALE_FX2, VfxShape.SCALE_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case SCALE_FX1 -> 16;
            case SCALE_FX2 -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFF63BDB5; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.SCALE_FX3) VfxShake.add(inst.payload.from(), 0.2f * Mth.clamp(inst.power, 0.6f, 2f), 7);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case SCALE_FX1 -> cast(inst, ctx, buf);
            case SCALE_FX2 -> zone(inst, ctx, buf);
            case SCALE_FX3 -> impact(inst, ctx, buf);
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

    // ------------------------------------------------------------------------------------------------ FX1: the Scale Volley
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1, inst.duration), t = Mth.clamp(age / dur, 0f, 1f);
        float p = Mth.clamp(inst.power, 0.5f, 3.2f);
        Vector3f f = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f d = new Vector3f(to).sub(f);
        float len = d.length();
        if (len < 0.3f) { d.set(0, 0, 1); len = 3f * p; to = new Vector3f(f).add(0, 0, len); } else d.div(len);
        int teal = mix(TEAL, inst), deep = mix(DEEP, inst), pale = mix(PALE, inst);
        float cu = Mth.clamp(t / 0.22f, 0f, 1f), u = Mth.clamp((t - 0.12f) / 0.62f, 0f, 1f);
        float e = 0.75f * ease(u) + 0.25f * u, imp = Mth.clamp((t - 0.74f) / 0.26f, 0f, 1f), live = 1f - VfxAnim.easeInCubic(imp);
        Vector3f head = lerp3(f, to, e);
        Vector3f side = new Vector3f(d).cross(new Vector3f(0, 1, 0));
        if (side.lengthSquared() < 1e-4f) side.set(1, 0, 0);
        side.normalize();
        Vector3f up = new Vector3f(side).cross(d).normalize();
        float chargeA = cu * (1f - Mth.clamp((t - 0.2f) / 0.2f, 0f, 1f));
        bb(buf, ctx, RIPPLE, VfxBlend.ADD, f, (2.0f - 1.2f * ease(cu)) * p, age * 0.1f, teal, chargeA);
        bb(buf, ctx, GLOW, VfxBlend.ADD, f, (0.8f + 0.7f * cu) * p, 0, teal, chargeA * 0.8f);
        if (u > 0f || cu >= 1f) {
            float grow = VfxAnim.easeOutBack(Mth.clamp(t / 0.2f, 0f, 1f));
            Vector3f tail = lerp3(f, head, Mth.clamp(1f - 0.5f * (1f - u) - 0.2f, 0f, 0.9f));
            buf.beam(ctx, STREAK, VfxBlend.ADD, tail, head, 0.2f * p, 0.9f * p, 3, -age * 0.12f, VfxVertexBuffer.withAlpha(teal, 0f), VfxVertexBuffer.withAlpha(pale, 0.8f * live));
            for (int k = 0; k < 5; k++) {                                                          // five scales on a helix round the streak
                float lag = 0.05f * k, uk = Mth.clamp(u - lag, 0f, 1f), ek = 0.75f * ease(uk) + 0.25f * uk;
                float ang = age * 0.5f + k * 1.257f, rad = 0.35f * p * (0.4f + 0.6f * Mth.sin(Mth.clamp(uk, 0f, 1f) * Mth.PI));
                Vector3f pos = lerp3(f, to, ek).fma(Mth.cos(ang) * rad, side).fma(Mth.sin(ang) * rad, up);
                bb(buf, ctx, SCALE, VfxBlend.ALPHA, pos, 0.5f * p * grow, age * 0.3f + k, k % 2 == 0 ? teal : deep, 0.95f * live);
                bb(buf, ctx, GLEAM, VfxBlend.ADD, pos, 0.4f * p * grow, k, pale, 0.7f * live);
            }
            bb(buf, ctx, GLOW, VfxBlend.ADD, head, 1.6f * p * grow, 0, teal, 0.5f * live);
            bb(buf, ctx, RIPPLE, VfxBlend.ADD, head, 1.3f * p * grow, -age * 0.2f, pale, 0.5f * live);
        }
        if (imp > 0f) {
            float s = ease(imp);
            bb(buf, ctx, FLARE, VfxBlend.ADD, to, (0.8f + 2.0f * s) * p, 0.2f + imp, HOT, 1f - imp);
            bb(buf, ctx, RIPPLE, VfxBlend.ADD, to, (0.6f + 2.8f * s) * p, 0f, teal, 0.95f * (1f - imp));
            RandomSource r = inst.random();
            for (int k = 0; k < 4; k++) {
                float a = r.nextFloat() * Mth.TWO_PI, sp = (0.5f + r.nextFloat() * 0.8f) * p * s;
                Vector3f cp = new Vector3f(to).fma(Mth.cos(a) * sp, ctx.camRight).fma(Mth.sin(a) * sp, ctx.camUp);
                bb(buf, ctx, (k & 1) == 0 ? SCALE : GLEAM, (k & 1) == 0 ? VfxBlend.ALPHA : VfxBlend.ADD, cp, 0.28f * p, a + age * 0.4f, k % 2 == 0 ? teal : pale, 1f - imp);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ FX2: the Shimmering Shoal
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1, inst.duration);
        float R = Math.max(1f, inst.power), fade = life(inst, age, 8f, 12f);
        if (fade <= 0f) return;
        Vector3f c = ctx.rel(inst.from(ctx));
        int teal = mix(TEAL, inst), deep = mix(DEEP, inst), pale = mix(PALE, inst);
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 14f, 0f, 1f)), pulse = VfxAnim.pulse(age, 0.9f);
        VfxPose ground = VfxPose.ground(c);
        buf.plane(SIGIL, VfxBlend.ADD, ground.lift(0.02f).spin(-age * 0.01f), R * open, VfxVertexBuffer.withAlpha(teal, (0.45f + 0.2f * pulse) * fade));
        for (int k = 0; k < 3; k++) {                                                            // ripples running out from the centre
            float ph = ((age * 0.025f + k / 3f) % 1f);
            buf.plane(RIPPLE, VfxBlend.ADD, ground.lift(0.04f + 0.01f * k), R * (0.2f + 0.85f * ph) * open, VfxVertexBuffer.withAlpha(k == 1 ? pale : teal, (1f - ph) * 0.8f * fade));
        }
        int seg = ctx.seg(16, 10);
        float band = Math.max(0.22f, R * 0.07f);
        buf.ring(RING, VfxBlend.ADD, ground.lift(0.07f), R * open - band, R * open, seg, Math.max(2, Math.round(Mth.TWO_PI * R / (6f * band))), age * 0.005f, VfxVertexBuffer.withAlpha(teal, 0.8f * fade));
        RandomSource rnd = inst.random();
        int nf = Mth.clamp(Math.round(R * 1.6f), 5, 9);
        for (int i = 0; i < nf; i++) {                                                           // fins swaying round the rim
            float a = Mth.TWO_PI * i / nf, ri = VfxAnim.easeOutBack(Mth.clamp((age - 2f - i * 1.2f) / 9f, 0f, 1f));
            float hh = Mth.clamp(R * 0.45f, 0.8f, 2.0f) * (0.8f + 0.2f * rnd.nextFloat()) * ri, sway = 0.1f * Mth.sin(age * 0.12f + i);
            Vector3f pos = at(c, Mth.cos(a) * R * 0.95f, 0.04f, Mth.sin(a) * R * 0.95f);
            stand(buf, FIN, VfxBlend.ALPHA, pos, a + 1.5708f, Math.min(0.7f, hh * 0.45f), hh, -Mth.cos(a), -Mth.sin(a), 0.1f + sway, VfxVertexBuffer.withAlpha(teal, 0.92f * fade));
        }
        float hy = Mth.clamp(R * 0.5f, 0.9f, 1.6f);
        int ns = 9;
        for (int i = 0; i < ns; i++) {                                                           // a ring of scales turning at chest height
            float a = age * 0.04f + i * Mth.TWO_PI / ns, rr = R * 0.62f * open, bob = 0.15f * Mth.sin(age * 0.1f + i * 1.3f);
            Vector3f pos = at(c, Mth.cos(a) * rr, hy + bob, Mth.sin(a) * rr);
            bb(buf, ctx, SCALE, VfxBlend.ALPHA, pos, 0.4f, a + age * 0.05f, i % 2 == 0 ? teal : deep, 0.9f * fade);
            bb(buf, ctx, GLEAM, VfxBlend.ADD, pos, 0.35f, i, pale, 0.5f * fade * (0.5f + 0.5f * Mth.sin(age * 0.2f + i)));
        }
        for (int i = 0; i < 8; i++) {
            float a = rnd.nextFloat() * Mth.TWO_PI, rad = (0.15f + 0.8f * rnd.nextFloat()) * R, sp = 0.012f + 0.012f * rnd.nextFloat();
            float life = ((age * sp * 8f + rnd.nextFloat()) % 1f), y = life * Mth.clamp(R * 0.9f, 1.2f, 3f);
            bb(buf, ctx, GLEAM, VfxBlend.ADD, at(c, Mth.cos(a + age * 0.01f) * rad, 0.15f + y, Mth.sin(a + age * 0.01f) * rad), 0.3f, age * 0.1f + i, pale, Mth.sin(life * Mth.PI) * fade);
        }
    }

    // ------------------------------------------------------------------------------------------------ FX3: the Shedding Tide
    private void impact(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1, inst.duration), t = Mth.clamp(age / dur, 0f, 1f);
        float p = Mth.clamp(inst.power, 0.5f, 3.2f);
        Vector3f c = ctx.rel(inst.from(ctx));
        int teal = mix(TEAL, inst), deep = mix(DEEP, inst), pale = mix(PALE, inst);
        VfxPose ground = VfxPose.ground(c);
        float s = ease(t), flashA = Mth.clamp(1f - t * 3.4f, 0f, 1f);
        buf.plane(RIPPLE, VfxBlend.ADD, ground.lift(0.04f), (0.3f + 3.2f * s) * p, VfxVertexBuffer.withAlpha(teal, (1f - t) * 0.95f));
        buf.plane(RIPPLE, VfxBlend.ADD, ground.lift(0.05f).spin(0.7f), (0.2f + 2.0f * ease(t * 1.4f)) * p, VfxVertexBuffer.withAlpha(pale, Mth.clamp(1f - t * 1.6f, 0f, 1f)));
        buf.plane(SIGIL, VfxBlend.ADD, ground.lift(0.03f).spin(t), (0.6f + 1.6f * s) * p, VfxVertexBuffer.withAlpha(teal, (1f - t) * 0.6f));
        bb(buf, ctx, FLARE, VfxBlend.ADD, at(c, 0, 0.4f * p, 0), (1.4f + 4.4f * s) * p, 0.35f + t, HOT, flashA);
        bb(buf, ctx, GLOW, VfxBlend.ADD, at(c, 0, 0.5f * p, 0), (1.4f + 2.4f * s) * p, 0, teal, (1f - t) * 0.8f);
        int n = 8;
        for (int i = 0; i < n; i++) {                                                            // a crown of fins thrown up round the point
            float a = Mth.TWO_PI * i / n + 0.2f, ri = ease(t * 2.2f) * (1f - VfxAnim.easeInCubic(Mth.clamp((t - 0.5f) / 0.5f, 0f, 1f)));
            Vector3f pos = at(c, Mth.cos(a) * (0.5f + 1.2f * s) * p, 0.05f, Mth.sin(a) * (0.5f + 1.2f * s) * p);
            stand(buf, FIN, VfxBlend.ALPHA, pos, a + 1.5708f, 0.3f * p, 1.2f * p * ri, -Mth.cos(a), -Mth.sin(a), -0.25f, VfxVertexBuffer.withAlpha(teal, 0.9f * (1f - t)));
        }
        RandomSource r = inst.random();
        float sec = age / 20f;
        for (int i = 0; i < 9; i++) {                                                            // scales thrown out in arcs
            float az = r.nextFloat() * Mth.TWO_PI, sp = (1.0f + r.nextFloat() * 1.6f) * p, vy = (2.6f + r.nextFloat() * 3.0f) * p, spin = (r.nextFloat() - 0.5f) * 0.6f;
            float hor = sp * (1f - (float) Math.exp(-3.0f * sec)) / 3.0f * 2.2f, y = Math.max(0f, vy * sec - 5.0f * p * sec * sec);
            Vector3f pos = at(c, Mth.cos(az) * hor, 0.15f + y, Mth.sin(az) * hor);
            float al = 1f - VfxAnim.easeInCubic(Mth.clamp((t - 0.55f) / 0.45f, 0f, 1f));
            bb(buf, ctx, SCALE, VfxBlend.ALPHA, pos, 0.34f * p, age * spin + i, i % 2 == 0 ? teal : deep, al);
            if (i % 3 == 0) bb(buf, ctx, GLEAM, VfxBlend.ADD, pos, 0.4f * p, i, pale, al * 0.8f);
        }
    }
}
