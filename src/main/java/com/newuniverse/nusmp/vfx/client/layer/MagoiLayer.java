package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.BossFx3.*;

/**
 * Why (4.102.0): the look of MAGI (the Labyrinth of Magic): Magoi, Rukh and the Djinn. Sinbad's boss fight and every Djinn of the player used to be dust and the element shapes of other
 * magics; this layer draws what the anime shows instead: a golden vessel sigil that opens under a Djinn Equip while Magoi spirals up the body and white Rukh rise and fly off; a steady Magoi
 * aura of rising motes with two Rukh orbiting; a flock of Rukh flying through a spell's path; the Extreme Magic (a huge sigil in the sky, eight shafts of Magoi closing on the ground, one
 * pillar, a shockwave and a burst of feathers); and the break of a phase (rays, rings, feathers). The colour of the packet is the Djinn's own; the Rukh and feathers are white-gold. Time Magic
 * technique copied (life ramps, eased openings, seeded randomness, camera-relative points), its files untouched. Textures: the Rukh, feather, celestial and seal sprites the mod already has.
 * <ul>
 *   <li>MAGI_EQUIP: from = the feet (follows), power = body height scale (1 = a player), duration about 40. Sigil + counter-turning circle + vessel star on the ground, a column, three Magoi
 *       helices, six Rukh rising, a flash and a ring when the armour closes. At most 62 quads.</li>
 *   <li>MAGI_AURA: from = the feet (follows), power = height scale, duration about 30 (re-sent while it stands). A glow behind the body, a ground ring, eight Magoi motes, two Rukh. At most 24 quads.</li>
 *   <li>MAGI_RUKH: from -> to, power = size. A flock of 5 to 12 Rukh flies the path on curving lines with glowing trails, and a flash and ring end it at {@code to}. At most 52 quads.</li>
 *   <li>MAGI_EXTREME: from = the centre on the ground (to = the same), power = radius, duration about 60. Sigils on the ground and in the sky, eight converging shafts, the pillar, the shockwave,
 *       a burst of Rukh. At most 70 quads.</li>
 *   <li>MAGI_PHASE: from = the body centre (follows), power = size (2 to 4), duration about 36. Twelve rays, a flash, a ground ring, a star and a burst of feathers. At most 40 quads.</li>
 * </ul>
 * <b>Budgets (rule 11).</b> The engine caps an activation at 100 quads; every loop is a constant or a {@code ctx.seg}, every ring and ribbon checks {@code hasBudget}, nothing is kept between
 * frames, and a followed effect whose creature is gone draws from its last place. {@code magi.classicVfx} on the server (Sinbad and the Djinn) decides whether these are sent at all.
 */
public final class MagoiLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation CIRCLE = t("celestial_circle"), SHAFT = t("celestial_shaft"), GLINT = t("celestial_glint"), STAR = t("seal_star"), BIG_STAR = t("light_star");
    public static final ResourceLocation[] RUKH = {t("light_rukh_0"), t("light_rukh_1"), t("light_rukh_2"), t("light_rukh_3"), t("light_rukh_4"), t("light_rukh_5")};
    public static final ResourceLocation[] FEATHER = {t("feather_white_0"), t("feather_white_1"), t("feather_white_2"), t("feather_white_3"), t("feather_white_4"), t("feather_white_5")};

    /** The white-gold of Rukh and of the vessel star. */
    static final int GOLD = 0xFFE9A8, WHITE = 0xFFFFFF;

    @Override
    public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.MAGI_EQUIP, VfxShape.MAGI_AURA, VfxShape.MAGI_RUKH, VfxShape.MAGI_EXTREME, VfxShape.MAGI_PHASE); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case MAGI_EQUIP -> 40;
            case MAGI_AURA -> 30;
            case MAGI_RUKH -> 24;
            case MAGI_EXTREME -> 60;
            default -> 36;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFF000000 | GOLD; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case MAGI_EQUIP -> equip(inst, ctx, buf);
            case MAGI_AURA -> aura(inst, ctx, buf);
            case MAGI_RUKH -> rukh(inst, ctx, buf);
            case MAGI_EXTREME -> extreme(inst, ctx, buf);
            case MAGI_PHASE -> phase(inst, ctx, buf);
            default -> { }
        }
    }

    private static ResourceLocation bird(int i) { return RUKH[Math.floorMod(i, RUKH.length)]; }

    private static ResourceLocation feather(int i) { return FEATHER[Math.floorMod(i, FEATHER.length)]; }

    // ---------------------------------------------------------------- Djinn Equip
    private void equip(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float prog = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick), P = Mth.clamp(inst.power, 0.6f, 4f), fade = life(age, inst.duration, 3, 8);
        Vector3f f = ctx.rel(inst.from(ctx));
        int rgb = inst.color & 0xFFFFFF;
        float open = ease(win(prog, 0f, 0.3f)), forge = ease(win(prog, 0.15f, 0.7f)), burst = win(prog, 0.62f, 1f);
        buf.plane(CIRCLE, VfxBlend.ADD, VfxPose.ground(f).spin(age * 0.05f).lift(0.03f), 2.4f * P * open, rgbA(rgb, 0.85f * fade));
        buf.plane(VfxTextures.MAGIC_CIRCLE, VfxBlend.ADD, VfxPose.ground(f).spin(-age * 0.07f).lift(0.05f), 1.6f * P * open, rgbA(GOLD, 0.6f * fade));
        buf.plane(STAR, VfxBlend.ADD, VfxPose.ground(f).spin(age * 0.11f).lift(0.07f), 0.9f * P * open, rgbA(WHITE, 0.9f * fade));
        buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, f, new Vector3f(f).add(0f, 7f * P * forge, 0f), 1.8f * P, 0.4f * P, 1, 0f, rgbA(rgb, 0.4f * fade), rgbA(rgb, 0f));
        int segs = ctx.seg(6, 3);
        for (int k = 0; k < 3; k++) {                                                    // three helices of Magoi round the body
            for (int i = 0; i < segs; i++) {
                float tt = (float) i / segs, ang = k * Mth.TWO_PI / 3f + age * 0.22f + tt * 8f, r = (1.15f - 0.55f * tt) * Math.min(1.5f, P);
                Vector3f p = new Vector3f(f).add(Mth.cos(ang) * r, tt * 2.3f * P * forge, Mth.sin(ang) * r);
                buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, p, 0.3f * P * (1f - 0.4f * tt), age * 0.2f + i, rgbA(i % 2 == 0 ? rgb : GOLD, 0.9f * fade * forge));
            }
        }
        for (int i = 0; i < 6; i++) {                                                    // Rukh rise round the body and fly off
            float ph = Mth.frac(age * 0.03f + i / 6f), ang = i * 1.047f + age * 0.08f, r = 1.5f * P * (1f - 0.4f * ph);
            Vector3f p = new Vector3f(f).add(Mth.cos(ang) * r, ph * 3.4f * P, Mth.sin(ang) * r);
            buf.billboard(ctx, bird(i), VfxBlend.ADD, p, 0.55f * P, 0f, rgbA(GOLD, 0.95f * Mth.sin(ph * Mth.PI) * fade * open));
        }
        if (burst > 0f) {                                                                // the armour closes: a flash and a ring
            float e = ease(burst);
            buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, new Vector3f(f).add(0f, 1f * P, 0f), 3.2f * P * e, 0f, rgbA(WHITE, 0.8f * (1f - burst) * fade));
            band(buf, KiLayer.RING, VfxBlend.ADD, f, 1.4f * P * e, 1.9f * P * e + 0.2f, ctx.seg(16, 8), 3f, 0f, 0f, 0.09f, rgbA(rgb, 0.9f * (1f - burst) * fade));
        }
    }

    // ---------------------------------------------------------------- the Magoi aura
    private void aura(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), P = Mth.clamp(inst.power, 0.6f, 4f), fade = life(age, inst.duration, 5, 8);
        Vector3f f = ctx.rel(inst.from(ctx));
        int rgb = inst.color & 0xFFFFFF;
        float pulse = 0.85f + 0.15f * VfxAnim.pulse(age, 1.2f);
        buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, new Vector3f(f).add(0f, 0.95f * P, 0f), 2.6f * P * pulse, 0f, rgbA(rgb, 0.22f * fade));
        band(buf, KiLayer.RING, VfxBlend.ADD, f, 0.85f * P, 1.05f * P, ctx.seg(14, 8), 3f, -age * 0.01f, 0f, 0.06f, rgbA(rgb, 0.55f * fade));
        int n = ctx.seg(8, 4);
        for (int i = 0; i < n; i++) {                                                    // Magoi motes rising off the body
            float ph = Mth.frac(age * 0.025f + h(inst.seed, i)), ang = h(inst.seed, i + 20) * Mth.TWO_PI + age * 0.06f;
            Vector3f p = new Vector3f(f).add(Mth.cos(ang) * 0.75f * P, ph * 2.3f * P, Mth.sin(ang) * 0.75f * P);
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, p, 0.2f * P, age * 0.1f + i, rgbA(i % 3 == 0 ? GOLD : rgb, 0.8f * Mth.sin(ph * Mth.PI) * fade));
        }
        for (int k = 0; k < 2; k++) {                                                    // two Rukh orbit the hips
            float ang = age * 0.07f + k * Mth.PI;
            Vector3f p = new Vector3f(f).add(Mth.cos(ang) * 1.2f * P, 0.9f * P + 0.15f * Mth.sin(age * 0.2f + k), Mth.sin(ang) * 1.2f * P);
            buf.billboard(ctx, bird(k + (int) (inst.seed & 3)), VfxBlend.ADD, p, 0.4f * P, 0f, rgbA(GOLD, 0.75f * fade));
        }
    }

    // ---------------------------------------------------------------- a flock of Rukh
    private void rukh(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float prog = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick), P = Mth.clamp(inst.power, 0.5f, 3f), fade = life(age, inst.duration, 1, 5);
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(b).sub(a);
        float len = dir.length();
        if (len < 0.5f) return;
        dir.div(len);
        Vector3f side = new Vector3f(dir.z, 0f, -dir.x);
        if (side.lengthSquared() < 1e-4f) side.set(1f, 0f, 0f);
        side.normalize();
        int rgb = inst.color & 0xFFFFFF, n = ctx.seg(Mth.clamp(Math.round(5 + 3 * P), 5, 12), 4);
        for (int i = 0; i < n; i++) {
            float lag = 0.35f * i / n, local = Mth.clamp((prog - lag) / 0.65f, 0f, 1f);
            if (local <= 0f || local >= 1f) continue;
            float e = ease(local), sway = Mth.sin(local * Mth.TWO_PI + i) * 0.9f * P, rise = (h(inst.seed, i) - 0.5f) * 1.4f * P + Mth.sin(local * Mth.PI) * 0.8f * P;
            Vector3f p = new Vector3f(a).lerp(b, e).add(new Vector3f(side).mul(sway)).add(0f, rise, 0f);
            float tail = Math.max(0f, e - 0.18f);
            Vector3f q = new Vector3f(a).lerp(b, tail).add(new Vector3f(side).mul(Mth.sin((local - 0.1f) * Mth.TWO_PI + i) * 0.9f * P)).add(0f, rise * 0.8f, 0f);
            buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, q, p, 0.02f, 0.2f * P, 1, 0f, rgbA(rgb, 0f), rgbA(rgb, 0.5f * fade));
            buf.billboard(ctx, bird(i), VfxBlend.ADD, p, 0.55f * P, 0f, rgbA(GOLD, 0.95f * Mth.sin(local * Mth.PI) * fade));
        }
        float end = win(prog, 0.65f, 1f);
        if (end > 0f) {
            buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, b, 2.4f * P * ease(Math.min(1f, end * 1.5f)), 0f, rgbA(WHITE, 0.7f * (1f - end) * fade));
            buf.plane(CIRCLE, VfxBlend.ADD, VfxPose.ground(new Vector3f(b).add(0f, 0.05f, 0f)).spin(age * 0.1f), 1.8f * P * ease(end), rgbA(rgb, 0.8f * (1f - end) * fade));
        }
    }

    // ---------------------------------------------------------------- Extreme Magic
    private void extreme(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float prog = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick), R = Mth.clamp(inst.power, 3f, 40f), fade = life(age, inst.duration, 4, 10);
        Vector3f c = ctx.rel(inst.from(ctx));
        int rgb = inst.color & 0xFFFFFF;
        float charge = ease(win(prog, 0f, 0.5f)), strike = win(prog, 0.5f, 0.64f), after = win(prog, 0.6f, 1f), hit = strike * (1f - after);
        buf.plane(CIRCLE, VfxBlend.ADD, VfxPose.ground(c).spin(age * 0.03f).lift(0.03f), R * charge, rgbA(rgb, 0.8f * fade));
        buf.plane(VfxTextures.MAGIC_CIRCLE, VfxBlend.ADD, VfxPose.ground(c).spin(-age * 0.05f).lift(0.05f), R * 0.7f * charge, rgbA(GOLD, 0.6f * fade));
        buf.plane(STAR, VfxBlend.ADD, VfxPose.ground(c).spin(age * 0.09f).lift(0.07f), R * 0.25f * charge, rgbA(WHITE, 0.9f * fade));
        float sky = Math.min(14f, 6f + R * 0.4f);
        buf.plane(CIRCLE, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0f, sky, 0f)).spin(-age * 0.04f), R * 0.8f * charge, rgbA(rgb, 0.7f * fade * (1f - 0.5f * after)));
        buf.plane(STAR, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0f, sky + 0.1f, 0f)).spin(age * 0.07f), R * 0.3f * charge, rgbA(GOLD, 0.8f * fade));
        for (int i = 0; i < 8; i++) {                                                    // eight shafts of Magoi close on the centre
            float ang = i * Mth.PI / 4f + age * 0.02f, r = R * (0.9f - 0.5f * charge);
            Vector3f top = new Vector3f(c).add(Mth.cos(ang) * r, sky, Mth.sin(ang) * r), foot = new Vector3f(c).add(Mth.cos(ang) * R * 0.55f * charge, 0f, Mth.sin(ang) * R * 0.55f * charge);
            buf.beam(ctx, SHAFT, VfxBlend.ADD, top, foot, 0.5f, 0.12f, 1, age * 0.03f, rgbA(rgb, 0.5f * charge * (1f - strike) * fade), rgbA(GOLD, 0.15f * charge * (1f - strike) * fade));
        }
        if (strike > 0f) {                                                               // the pillar
            float w = R * (0.55f - 0.25f * after);
            buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, c, new Vector3f(c).add(0f, sky + 6f, 0f), w * 1.6f, w * 0.7f, 1, 0f, rgbA(rgb, 0.55f * hit * fade), rgbA(rgb, 0.15f * hit * fade));
            buf.beam(ctx, SHAFT, VfxBlend.ADD, c, new Vector3f(c).add(0f, sky + 6f, 0f), w * 0.8f, w * 0.3f, 1, age * 0.05f, rgbA(WHITE, 0.95f * hit * fade), rgbA(WHITE, 0.3f * hit * fade));
            buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, new Vector3f(c).add(0f, 1.2f, 0f), R * 1.4f * ease(strike), 0f, rgbA(WHITE, 0.8f * hit * fade));
        }
        if (after > 0f) {                                                                // the shockwave and the Rukh
            float e = ease(after);
            band(buf, KiLayer.RING, VfxBlend.ADD, c, R * 0.9f * e, R * 1.1f * e + 0.3f, ctx.seg(24, 10), 4f, 0f, 0f, 0.1f, rgbA(rgb, 0.95f * (1f - after) * fade));
            band(buf, KiLayer.RING, VfxBlend.ADD, c, R * 0.5f * e, R * 0.62f * e + 0.2f, ctx.seg(20, 8), 4f, 0f, 0f, 0.13f, rgbA(GOLD, 0.8f * (1f - after) * fade));
            int n = ctx.seg(10, 4);
            for (int i = 0; i < n; i++) {
                float ang = Mth.TWO_PI * i / n + h(inst.seed, i), d = R * (0.2f + 0.7f * e) * (0.6f + 0.4f * h(inst.seed, i + 9));
                buf.billboard(ctx, bird(i), VfxBlend.ADD, new Vector3f(c).add(Mth.cos(ang) * d, 0.6f + 5f * after * (0.5f + h(inst.seed, i + 30)), Mth.sin(ang) * d), 0.7f, 0f, rgbA(GOLD, 0.9f * (1f - after) * fade));
            }
        }
    }

    // ---------------------------------------------------------------- the break of a phase
    private void phase(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float prog = inst.progress(ctx.partialTick), age = inst.ageTicks(ctx.partialTick), P = Mth.clamp(inst.power, 1.5f, 6f), fade = life(age, inst.duration, 1, 10);
        Vector3f c = ctx.rel(inst.from(ctx));
        int rgb = inst.color & 0xFFFFFF;
        float e = ease(win(prog, 0f, 0.7f)), flash = 1f - win(prog, 0f, 0.35f);
        buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, c, P * 2.8f * (0.4f + 0.6f * e), 0f, rgbA(WHITE, 0.85f * flash * fade));
        buf.billboard(ctx, STAR, VfxBlend.ADD, c, P * 1.6f * e, age * 0.12f, rgbA(GOLD, 0.9f * (1f - prog) * fade));
        buf.plane(CIRCLE, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0f, -P * 0.45f, 0f)).spin(age * 0.06f), P * 1.9f * e, rgbA(rgb, 0.85f * (1f - prog) * fade));
        band(buf, KiLayer.RING, VfxBlend.ADD, new Vector3f(c).add(0f, -P * 0.45f + 0.04f, 0f), P * 2.2f * e, P * 2.45f * e + 0.15f, ctx.seg(20, 8), 4f, 0f, 0f, 0f, rgbA(WHITE, 0.8f * (1f - prog) * fade));
        int rays = ctx.seg(12, 6);
        for (int i = 0; i < rays; i++) {
            float ang = Mth.TWO_PI * i / rays + h(inst.seed, i) * 0.2f, tilt = (h(inst.seed, i + 40) - 0.5f) * 1.2f;
            Vector3f d = new Vector3f(Mth.cos(ang), tilt * 0.6f, Mth.sin(ang));
            Vector3f from = new Vector3f(c).add(new Vector3f(d).mul(0.5f * P)), to = new Vector3f(c).add(new Vector3f(d).mul(P * (1f + 2.6f * e)));
            buf.beam(ctx, SHAFT, VfxBlend.ADD, from, to, 0.35f, 0.04f, 1, 0f, rgbA(i % 2 == 0 ? rgb : GOLD, 0.8f * (1f - prog) * fade), rgbA(WHITE, 0f));
        }
        int n = ctx.seg(8, 4);
        for (int i = 0; i < n; i++) {
            float ang = Mth.TWO_PI * i / n + prog * 2f, d = P * (0.6f + 2.2f * e);
            buf.billboard(ctx, feather(i), VfxBlend.ADD, new Vector3f(c).add(Mth.cos(ang) * d, (h(inst.seed, i) - 0.3f) * P + prog * 1.5f, Mth.sin(ang) * d), 0.55f, ang, rgbA(WHITE, 0.9f * (1f - prog) * fade));
        }
    }
}
