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
 * 1.93.0 Shakudo Magic (rebuilt; it used the shared wiki layer before). Black-purple patinated alloy with fine gold inlay: dark slabs and plates, gold lines that glow as if the metal were poured, drifting gold leaf. Textures from tools/gen_shakudo_textures.py (shakudo_*).
 * <ul>
 *   <li>SHAKUDO_FX1 (cast / projectile, default 16 ticks): the Gilded Inlay Lance: a seal contracts on the hand, then a dark alloy lance with a glowing inlay line and three trailing plates flies from -> to shedding gold leaf, and ends in a flash, a ring and chips at the target. from = origin, to = target, power = size (0.6 to 3), colour = tint mixed with the palette.</li>
 *   <li>SHAKUDO_FX2 (zone / field, default 80 ticks): the alloy hall: a gold seal turns on cracked ground inside a rim ring, six inlaid slabs stand up round the rim with gold running up them, a second ring turns overhead, leaf rises, a shock ring pulses. from = centre on the ground, power = RADIUS in blocks; fades in over 8 ticks and out over the last 12.</li>
 *   <li>SHAKUDO_FX3 (impact / signature, default 28 ticks): the golem fist: a giant dark slab drops onto the point (from the opposite side of the hint, straight down without one), a gold flash and two shock rings, the ground cracks with gold in the cracks, chips and leaf fly out. from = centre, to - from = a direction hint (may be zero), power = scale.</li>
 * </ul>
 * At most 400 vertices per activation (every sprite goes through bb / hasBudget); sprites are layered back to front: dark solid (ALPHA) first, glows (ADD) last.
 */
public class ShakudoLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final ResourceLocation SIGIL = t("shakudo_sigil"), RING = t("shakudo_ring"), FLARE = t("shakudo_flare"), STREAK = t("shakudo_streak"), GLOW = t("shakudo_glow"),
            CRACK = t("shakudo_crack"), MOTE = t("shakudo_mote"), PLATE = t("shakudo_plate"), FLAKE = t("shakudo_flake"), SLAB = t("shakudo_slab"), CHIP = t("shakudo_chip");
    static final int GOLD = 0xFFE6B450, PALE = 0xFFFFE9A8, DARK = 0xFF2A2030, MID = 0xFF4A3A58, HOT = 0xFFFFF6D8;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.SHAKUDO_FX1, VfxShape.SHAKUDO_FX2, VfxShape.SHAKUDO_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case SHAKUDO_FX1 -> 16;
            case SHAKUDO_FX2 -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFFE6B450; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.SHAKUDO_FX3) VfxShake.add(inst.payload.from(), 0.3f * Mth.clamp(inst.power, 0.6f, 2f), 7);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case SHAKUDO_FX1 -> cast(inst, ctx, buf);
            case SHAKUDO_FX2 -> zone(inst, ctx, buf);
            case SHAKUDO_FX3 -> impact(inst, ctx, buf);
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

    // ------------------------------------------------------------------------------------------------ FX1: the Gilded Inlay Lance
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1, inst.duration), t = Mth.clamp(age / dur, 0f, 1f);
        float p = Mth.clamp(inst.power, 0.5f, 3.2f);
        Vector3f f = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f d = new Vector3f(to).sub(f);
        float len = d.length();
        if (len < 0.3f) { d.set(0, 0, 1); len = 3f * p; to = new Vector3f(f).add(0, 0, len); } else d.div(len);
        int gold = mix(GOLD, inst), pale = mix(PALE, inst), dark = mix(DARK, inst), mid = mix(MID, inst);
        float rot = screenAngle(ctx, d);
        float cu = Mth.clamp(t / 0.22f, 0f, 1f), u = Mth.clamp((t - 0.14f) / 0.6f, 0f, 1f);
        float e = 0.7f * ease(u) + 0.3f * u, imp = Mth.clamp((t - 0.72f) / 0.28f, 0f, 1f), live = 1f - VfxAnim.easeInCubic(imp);
        Vector3f head = lerp3(f, to, e);

        float chargeA = cu * (1f - Mth.clamp((t - 0.2f) / 0.2f, 0f, 1f));                          // the seal contracting on the hand
        bb(buf, ctx, GLOW, VfxBlend.ADD, f, (0.9f + 0.7f * cu) * p, 0, gold, chargeA * 0.8f);
        bb(buf, ctx, SIGIL, VfxBlend.ADD, f, (2.3f - 1.6f * ease(cu)) * p, age * 0.14f, gold, chargeA);
        bb(buf, ctx, FLARE, VfxBlend.ADD, f, (0.6f + 1.4f * cu) * p, 0.4f, pale, chargeA * 0.9f);
        if (u > 0f || cu >= 1f) {
            float grow = VfxAnim.easeOutBack(Mth.clamp(t / 0.2f, 0f, 1f));
            float body = Math.min(e * len, 2.8f * p);
            Vector3f tail = new Vector3f(head).fma(-body, d);
            buf.beam(ctx, STREAK, VfxBlend.ALPHA, tail, head, 0.5f * p, 0.5f * p, 2, 0f, VfxVertexBuffer.withAlpha(dark, 0.95f * live), VfxVertexBuffer.withAlpha(mid, 0.95f * live));   // the dark body
            buf.beam(ctx, STREAK, VfxBlend.ADD, tail, head, 0.22f * p, 0.3f * p, 2, -age * 0.08f, VfxVertexBuffer.withAlpha(gold, 0f), VfxVertexBuffer.withAlpha(pale, 0.95f * live));   // the inlay line
            for (int k = 3; k >= 1; k--) {                                                          // plates left behind
                float ek = 0.7f * ease(Math.max(0, u - 0.08f * k)) + 0.3f * Math.max(0, u - 0.08f * k);
                bb(buf, ctx, PLATE, VfxBlend.ALPHA, lerp3(f, to, ek), 0.8f * p * grow, rot - 1.5708f, VfxVertexBuffer.lerpColor(mid, gold, k / 4f), (0.5f - 0.1f * k) * live);
            }
            RandomSource r = inst.random();
            for (int k = 0; k < 4; k++) {                                                           // gold leaf shed behind
                float lag = 0.06f + 0.05f * k + r.nextFloat() * 0.03f, ek = 0.7f * ease(Math.max(0, u - lag)) + 0.3f * Math.max(0, u - lag);
                Vector3f cp = lerp3(f, to, ek);
                float off = (r.nextFloat() - 0.5f) * 0.8f * p, fall = (age - lag * dur) * 0.03f * p;
                cp.add(ctx.camRight.x * off, ctx.camRight.y * off - fall, ctx.camRight.z * off);
                bb(buf, ctx, FLAKE, VfxBlend.ADD, cp, (0.2f + 0.1f * r.nextFloat()) * p, age * (0.3f + r.nextFloat() * 0.3f), pale, 0.9f * live);
            }
            bb(buf, ctx, GLOW, VfxBlend.ADD, head, 2.0f * p * grow, 0, gold, 0.55f * live);
            bb(buf, ctx, PLATE, VfxBlend.ALPHA, head, 1.1f * p * grow, rot - 1.5708f, mid, 0.98f * live);
            bb(buf, ctx, FLARE, VfxBlend.ADD, head, 1.5f * p * grow, age * 0.3f, pale, 0.85f * live);
        }
        if (imp > 0f) {
            float s = ease(imp);
            bb(buf, ctx, FLARE, VfxBlend.ADD, to, (0.8f + 2.4f * s) * p, 0.2f + imp, HOT, 1f - imp);
            bb(buf, ctx, RING, VfxBlend.ADD, to, (0.5f + 2.6f * s) * p, 0f, gold, 0.95f * (1f - imp));
            RandomSource r = inst.random();
            for (int k = 0; k < 4; k++) {
                float a = r.nextFloat() * Mth.TWO_PI, sp = (0.6f + r.nextFloat() * 0.8f) * p * s;
                Vector3f cp = new Vector3f(to).fma(Mth.cos(a) * sp, ctx.camRight).fma(Mth.sin(a) * sp, ctx.camUp);
                bb(buf, ctx, CHIP, VfxBlend.ALPHA, cp, 0.2f * p, a + age * 0.4f, mid, 1f - imp);
            }
        }
    }

    // ------------------------------------------------------------------------------------------------ FX2: the alloy hall
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1, inst.duration);
        float R = Math.max(1f, inst.power), fade = life(inst, age, 8f, 12f);
        if (fade <= 0f) return;
        Vector3f c = ctx.rel(inst.from(ctx));
        int gold = mix(GOLD, inst), pale = mix(PALE, inst), dark = mix(DARK, inst), mid = mix(MID, inst);
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 14f, 0f, 1f)), pulse = VfxAnim.pulse(age, 1.0f);
        VfxPose ground = VfxPose.ground(c);
        buf.plane(CRACK, VfxBlend.ALPHA, ground.lift(0.015f).spin(1.1f), R * 1.02f * open, VfxVertexBuffer.withAlpha(dark, 0.8f * fade));
        buf.plane(SIGIL, VfxBlend.ADD, ground.lift(0.03f).spin(age * 0.012f), R * open, VfxVertexBuffer.withAlpha(gold, (0.55f + 0.2f * pulse) * fade));
        buf.plane(SIGIL, VfxBlend.ADD, ground.lift(0.05f).spin(-age * 0.02f), R * 0.58f * open, VfxVertexBuffer.withAlpha(pale, 0.4f * fade));
        buf.plane(CRACK, VfxBlend.ADD, ground.lift(0.06f).spin(1.1f), R * 0.9f * open, VfxVertexBuffer.withAlpha(gold, (0.2f + 0.25f * pulse) * fade));
        int seg = ctx.seg(16, 10);
        float band = Math.max(0.25f, R * 0.08f);
        buf.ring(RING, VfxBlend.ADD, ground.lift(0.07f), R * open - band, R * open, seg, Math.max(2, Math.round(Mth.TWO_PI * R / (6f * band))), age * 0.004f, VfxVertexBuffer.withAlpha(gold, 0.85f * fade));

        RandomSource rnd = inst.random();
        int n = 6;
        float hMax = Mth.clamp(R * 0.55f, 1.0f, 2.6f), w = Math.min(1.2f, Mth.TWO_PI * R / n * 0.3f);
        for (int i = 0; i < n; i++) {                                                          // inlaid slabs standing round the rim, leaning in
            float a = Mth.TWO_PI * i / n + 0.3f;
            float ri = VfxAnim.easeOutBack(Mth.clamp((age - 3f - i * 1.5f) / 9f, 0f, 1f)), hh = hMax * (0.85f + 0.15f * rnd.nextFloat()) * ri;
            Vector3f pos = at(c, Mth.cos(a) * R * 0.94f, 0.04f, Mth.sin(a) * R * 0.94f);
            stand(buf, SLAB, VfxBlend.ALPHA, pos, a + 1.5708f, w, hh, -Mth.cos(a), -Mth.sin(a), 0.12f, VfxVertexBuffer.withAlpha(mid, 0.96f * fade));
            float run = ((age * 0.03f + i * 0.17f) % 1f);                                       // the gold running up the inlay
            bb(buf, ctx, FLARE, VfxBlend.ADD, at(pos, -Mth.cos(a) * 0.12f * hh * run, hh * run, -Mth.sin(a) * 0.12f * hh * run), 0.5f * w * ri, i + age * 0.1f, pale, fade * (0.3f + 0.6f * (1f - run)));
        }
        float oh = Mth.clamp(R * 0.8f, 1.6f, 3.0f) * open;                                      // a ring turning overhead
        buf.ring(RING, VfxBlend.ADD, VfxPose.ground(at(c, 0, oh, 0)).spin(age * 0.03f), R * 0.5f, R * 0.56f, seg, 3, 0f, VfxVertexBuffer.withAlpha(pale, 0.5f * fade));
        for (int i = 0; i < 10; i++) {                                                          // gold leaf rising
            float a = rnd.nextFloat() * Mth.TWO_PI, rad = (0.15f + 0.8f * rnd.nextFloat()) * R, sp = 0.012f + 0.012f * rnd.nextFloat();
            float life = ((age * sp * 8f + rnd.nextFloat()) % 1f), y = life * Mth.clamp(R * 0.9f, 1.2f, 3f);
            bb(buf, ctx, FLAKE, VfxBlend.ADD, at(c, Mth.cos(a + age * 0.01f) * rad, 0.15f + y, Mth.sin(a + age * 0.01f) * rad), 0.22f, age * 0.1f + i, pale, Mth.sin(life * Mth.PI) * fade);
        }
        float ph = (age % 26f) / 26f;
        buf.plane(RING, VfxBlend.ADD, ground.lift(0.09f), R * (0.25f + 0.8f * ease(ph)), VfxVertexBuffer.withAlpha(pale, (1f - ph) * 0.7f * fade));
    }

    // ------------------------------------------------------------------------------------------------ FX3: the golem fist
    private void impact(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1, inst.duration), t = Mth.clamp(age / dur, 0f, 1f);
        float p = Mth.clamp(inst.power, 0.5f, 3.2f);
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f hint = new Vector3f(ctx.rel(inst.to(ctx))).sub(c);
        Vector3f axis = hint.lengthSquared() < 0.04f ? new Vector3f(0, -1, 0) : hint.normalize();
        int gold = mix(GOLD, inst), pale = mix(PALE, inst), dark = mix(DARK, inst), mid = mix(MID, inst);
        VfxPose ground = VfxPose.ground(c);
        float hitT = 0.2f, after = Mth.clamp((t - hitT) / (1f - hitT), 0f, 1f), s = ease(after), flashA = Mth.clamp(1f - after * 3.2f, 0f, 1f);
        if (t >= hitT) {
            float fadeC = 1f - VfxAnim.easeInCubic(Mth.clamp((after - 0.4f) / 0.6f, 0f, 1f));
            buf.plane(CRACK, VfxBlend.ALPHA, ground.lift(0.02f).spin(0.6f), 2.3f * p * (0.3f + 0.7f * s), VfxVertexBuffer.withAlpha(dark, 0.85f * fadeC));
            buf.plane(CRACK, VfxBlend.ADD, ground.lift(0.04f).spin(0.6f), 2.3f * p * (0.3f + 0.7f * s), VfxVertexBuffer.withAlpha(gold, (1f - after) * 0.9f));
            buf.plane(RING, VfxBlend.ADD, ground.lift(0.07f), (0.3f + 3.0f * s) * p, VfxVertexBuffer.withAlpha(gold, (1f - after) * 0.95f));
            buf.plane(RING, VfxBlend.ADD, ground.lift(0.08f).spin(1f), (0.2f + 1.9f * ease(after * 1.5f)) * p, VfxVertexBuffer.withAlpha(HOT, flashA));
        }
        if (t < hitT + 0.08f) {                                                                   // the giant slab drops along the hit axis
            float fall = Mth.clamp(t / hitT, 0f, 1f), fe = VfxAnim.easeInCubic(fall);
            Vector3f from = new Vector3f(c).fma(-4.2f * p, axis), hp = lerp3(from, c, fe);
            float vis = t < hitT ? 1f : 1f - (t - hitT) / 0.08f, rot = screenAngle(ctx, axis) + 0.5f;
            buf.beam(ctx, STREAK, VfxBlend.ADD, from, hp, 0.2f * p, 1.6f * p, 3, -age * 0.2f, VfxVertexBuffer.withAlpha(gold, 0f), VfxVertexBuffer.withAlpha(pale, 0.7f * Mth.clamp(fall * 3f, 0, 1) * vis));
            bb(buf, ctx, SLAB, VfxBlend.ALPHA, hp, 2.2f * p, rot, mid, vis * Mth.clamp(fall * 4f, 0, 1));
            bb(buf, ctx, GLOW, VfxBlend.ADD, hp, 3.0f * p, 0, gold, 0.45f * fall * vis);
        }
        if (t >= hitT) {
            bb(buf, ctx, FLARE, VfxBlend.ADD, at(c, 0, 0.3f * p, 0), (1.5f + 5.2f * s) * p, 0.35f + after, HOT, flashA);
            bb(buf, ctx, GLOW, VfxBlend.ADD, at(c, 0, 0.5f * p, 0), (1.6f + 2.4f * s) * p, 0, gold, (1f - after) * 0.8f);
            RandomSource r = inst.random();
            float sec = (age - hitT * dur) / 20f;
            for (int i = 0; i < 10; i++) {
                float az = r.nextFloat() * Mth.TWO_PI, sp = (1.0f + r.nextFloat() * 1.5f) * p, vy = (3.0f + r.nextFloat() * 3.0f) * p, spin = (r.nextFloat() - 0.5f) * 0.6f;
                float tt = Math.max(0f, sec), hor = sp * (1f - (float) Math.exp(-3.2f * tt)) / 3.2f * 2.2f, y = Math.max(0f, vy * tt - 5.5f * p * tt * tt);
                Vector3f pos = at(c, Mth.cos(az) * hor, 0.15f + y, Mth.sin(az) * hor);
                float al = 1f - VfxAnim.easeInCubic(Mth.clamp((after - 0.55f) / 0.45f, 0f, 1f));
                if (i < 6) bb(buf, ctx, CHIP, VfxBlend.ALPHA, pos, 0.34f * p, age * spin + i, mid, al);
                else bb(buf, ctx, FLAKE, VfxBlend.ADD, pos, 0.3f * p, age * spin + i, pale, al * 0.9f);
            }
            for (int i = 0; i < 3; i++) {
                float a = i * 2.1f + 0.5f, y = after * (1.0f + 0.4f * i) * p;
                bb(buf, ctx, MOTE, VfxBlend.ADD, at(c, Mth.cos(a) * 0.6f * p, 0.3f + y, Mth.sin(a) * 0.6f * p), 0.5f * p, 0, gold, Mth.clamp(after * 4f, 0, 1) * (1f - after) * 0.8f);
            }
        }
    }
}
