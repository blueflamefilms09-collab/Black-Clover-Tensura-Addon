package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.hash;
import static com.newuniverse.nusmp.vfx.client.layer.ElementFx.life;

/**
 * Food Magic (Glutton's Banquet), after the owner's manga still: black ink and hatching, a shaggy beast with a fanged maw,
 * a giant fork and knife, a heap of food debris, thick radial speed lines and a spiky impact bubble, lit with the warm gold
 * of a feast. Textures from tools/gen_food_textures.py: the ink layers (maw_ink, fur, speed, burst_ink, plate, swirl, hatch)
 * are drawn dark with ALPHA blend, the light layers (maw_light, burst, beam, disc) in gold / cream with ADD.
 * <ul>
 *   <li>FOOD_FX1 (cast / projectile): crossed fork and knife flash and a speed-line ring collapse at 'from' (first quarter),
 *       then a furred beast maw flies to 'to' with ghost maws behind it, an ink hatch trail with a gold core, a fork and a
 *       knife spinning round it and crumbs shed on the way; a small bubble burst and crumbs where it lands.
 *       power = size of the maw (0.6 to 3), duration = flight ticks, colour tints the gold.</li>
 *   <li>FOOD_FX2 (zone / field / dome): a banquet plate sigil on the ground out to the radius (power = radius in blocks),
 *       a swallowing ink vortex turning against it, a fur rim, a fence of giant forks and knives rising round the rim, a
 *       pulse sweeping outwards, crumbs and sparks rising and the beast's maw looming over the centre. Fades in over 8 ticks
 *       and out over the last 12.</li>
 *   <li>FOOD_FX3 (impact / signature): a bubble burst with its ink outline, radial speed lines, the beast's maw snapping out
 *       in a fur burst, plates opening as shock rings, forks, knives and crumbs flung out (biased along to - from) and
 *       a gold afterglow with rising crumbs. power = scale, duration = life ticks.</li>
 * </ul>
 */
public class FoodLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final ResourceLocation MAW_INK = t("food_maw_ink"), MAW_LIGHT = t("food_maw_light"), FUR = t("food_fur"), SPEED = t("food_speed"),
            BURST = t("food_burst"), BURST_INK = t("food_burst_ink"), HATCH = t("food_hatch"), BEAM = t("food_beam"), PLATE = t("food_plate"),
            DISC = t("food_disc"), SWIRL = t("food_swirl"), FORK = t("food_fork"), KNIFE = t("food_knife");
    static final ResourceLocation[] CRUMB = {t("food_crumb0"), t("food_crumb1"), t("food_crumb2")};

    static final int INK = 0xFF1A120C, GOLD = 0xFFFFC04A, AMBER = 0xFFC8803A, CREAM = 0xFFFFF0D2, STEEL = 0xFFE8E4DC, WHITE = 0xFFFFFFFF;

    @Override
    public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.FOOD_FX1, VfxShape.FOOD_FX2, VfxShape.FOOD_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case FOOD_FX1 -> 16;
            case FOOD_FX2 -> 80;
            default -> 28;
        };
    }

    @Override
    public int defaultColor(VfxShape s) { return GOLD; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case FOOD_FX1 -> cast(inst, ctx, buf);
            case FOOD_FX2 -> zone(inst, ctx, buf);
            case FOOD_FX3 -> impact(inst, ctx, buf);
            default -> { }
        }
    }

    /** An oriented quad centred at c spanning +-hw along right and +-hh along up. */
    private static void panel(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f c, Vector3f right, Vector3f up, float hw, float hh, int argb) {
        Vector3f r = new Vector3f(right).mul(hw), u = new Vector3f(up).mul(hh);
        buf.quad(tex, blend, new Vector3f(c).sub(r).sub(u), new Vector3f(c).add(r).sub(u), new Vector3f(c).add(r).add(u), new Vector3f(c).sub(r).add(u),
                0, 0, 1, 1, argb, argb);
    }

    /** An upright figure standing on foot, turned to the camera horizontally. */
    private static void stand(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f foot, float w, float h, int argb) {
        Vector3f right = new Vector3f(ctx.camRight.x, 0, ctx.camRight.z);
        if (right.lengthSquared() < 1e-4f) right.set(1, 0, 0);
        right.normalize();
        panel(buf, tex, blend, new Vector3f(foot).add(0, h / 2, 0), right, new Vector3f(0, 1, 0), w / 2, h / 2, argb);
    }

    private static int col(int c, float a) { return VfxVertexBuffer.withAlpha(c, a); }

    /** Own gold mixed with the spell tint (the effect stays gold even when the tint is white). */
    private static int tint(VfxInstance inst) { return 0xFF000000 | (VfxVertexBuffer.lerpColor(GOLD, inst.color | 0xFF000000, 0.3f) & 0xFFFFFF); }

    private static float clamp01(float v) { return Mth.clamp(v, 0f, 1f); }

    /** The beast's head at a point: fur burst behind (ink), dark maw (ink), teeth and eyes (cream light), gold halo. */
    private static void maw(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, float size, float rot, float a, int gold, boolean halo) {
        if (a <= 0.01f) return;
        if (halo) VfxBloom.glow(ctx, buf, c, size * 0.8f, gold, 0.8f * a);
        buf.billboard(ctx, FUR, VfxBlend.ALPHA, c, size * 1.7f, rot * 0.5f, col(INK, 0.95f * a));
        buf.billboard(ctx, MAW_INK, VfxBlend.ALPHA, c, size, rot, col(INK, a));
        buf.billboard(ctx, MAW_LIGHT, VfxBlend.ADD, c, size, rot, col(VfxVertexBuffer.lerpColor(CREAM, gold, 0.25f), 0.95f * a));
    }

    // ------------------------------------------------------------------------------------------------------ FX1
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), d = Math.max(1f, inst.duration), t = age / d, p = Mth.clamp(inst.power, 0.4f, 4f);
        int gold = tint(inst);
        Vector3f a = ctx.rel(inst.from(ctx)), b = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(b).sub(a);
        if (dir.lengthSquared() < 1e-4f) dir.set(0, 0, 1);
        float dist = dir.length();
        dir.normalize();
        float fade = clamp01((d + 6 - age) / 8f);

        // 1) charge at the hand: a speed-line ring collapsing, crossed fork and knife, motes pulled in
        float tc = clamp01(t / 0.3f);
        if (tc < 1f || t < 0.45f) {
            float k = clamp01(1f - Math.max(0f, t - 0.3f) / 0.15f);
            float pop = VfxAnim.easeOutBack(tc);
            buf.billboard(ctx, SPEED, VfxBlend.ALPHA, a, 1.8f * p * (1.3f - 0.8f * VfxAnim.easeOutCubic(tc)), age * 0.2f, col(INK, 0.9f * k));
            VfxBloom.glow(ctx, buf, a, 0.6f * p * (0.4f + tc), gold, 0.9f * k);
            Vector3f r = ctx.camRight, u = ctx.camUp;
            float hh = 0.55f * p * pop * (0.5f + 0.5f * k);
            Vector3f cr = new Vector3f(r).mul(0.5f).add(new Vector3f(u).mul(0.87f)), cu = new Vector3f(r).mul(-0.87f).add(new Vector3f(u).mul(0.5f));
            panel(buf, FORK, VfxBlend.ALPHA, a, cu, cr, hh * 0.5f, hh, col(STEEL, k));
            panel(buf, KNIFE, VfxBlend.ALPHA, a, cr, cu, hh * 0.5f, hh, col(STEEL, k));
            for (int i = 0; i < 4; i++) {
                float ang = hash(inst.seed, i, 1) * Mth.TWO_PI + age * 0.3f, rr = (1.1f - 0.9f * tc) * p;
                Vector3f q = new Vector3f(a).add(new Vector3f(r).mul(Mth.cos(ang) * rr)).add(new Vector3f(u).mul(Mth.sin(ang) * rr));
                buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, q, 0.2f * p, age, col(gold, k * tc));
            }
        }

        // 2) flight: the maw runs from -> to
        float tf = clamp01((t - 0.18f) / 0.67f), e = VfxAnim.easeInOutSine(tf);
        if (t > 0.18f && t < 0.97f) {
            Vector3f head = new Vector3f(a).lerp(b, e);
            head.y += 0.25f * p * Mth.sin(tf * Mth.PI);
            float ms = 1.0f * p * (0.6f + 0.4f * VfxAnim.easeOutBack(clamp01(tf * 4f)));
            float wob = 0.12f * Mth.sin(age * 0.9f);
            float trailLen = Math.min(dist * e, 3.2f * p + 0.6f);
            Vector3f tail = new Vector3f(head).sub(new Vector3f(dir).mul(trailLen));
            // trail: ink hatch strip under a gold core beam, then ghost maws
            if (trailLen > 0.05f) {
                buf.beam(ctx, HATCH, VfxBlend.ALPHA, head, tail, 0.9f * p, 0.25f * p, 3, -age * 0.15f, col(INK, 0.9f * fade), col(INK, 0f));
                buf.beam(ctx, BEAM, VfxBlend.ADD, head, tail, 0.5f * p, 0.08f * p, 3, 0f, col(gold, 0.85f * fade), col(AMBER, 0f));
            }
            for (int g = 3; g >= 1; g--) {
                float eg = VfxAnim.easeInOutSine(clamp01(tf - g * 0.07f));
                Vector3f gp = new Vector3f(a).lerp(b, eg);
                gp.y += 0.25f * p * Mth.sin(clamp01(tf - g * 0.07f) * Mth.PI);
                buf.billboard(ctx, MAW_LIGHT, VfxBlend.ADD, gp, ms * (1f - 0.1f * g), wob, col(gold, 0.32f / g * fade));
            }
            maw(ctx, buf, head, ms, wob, fade, gold, true);
            // cutlery spinning round the head
            Vector3f r = ctx.camRight, u = ctx.camUp;
            for (int i = 0; i < 2; i++) {
                float ang = age * 0.55f + i * Mth.PI;
                Vector3f q = new Vector3f(head).add(new Vector3f(r).mul(Mth.cos(ang) * 0.85f * ms)).add(new Vector3f(u).mul(Mth.sin(ang) * 0.5f * ms));
                buf.billboard(ctx, i == 0 ? FORK : KNIFE, VfxBlend.ALPHA, q, 0.7f * p, ang + 1.2f, col(STEEL, fade));
            }
            // crumbs shed behind, falling
            for (int i = 0; i < 6; i++) {
                float life = (tf * 3f + hash(inst.seed, i, 2)) % 1f;
                float at = clamp01(tf - life * 0.25f);
                Vector3f q = new Vector3f(a).lerp(b, VfxAnim.easeInOutSine(at));
                q.add(new Vector3f(r).mul((hash(inst.seed, i, 3) - 0.5f) * 0.9f * p)).add(0, (hash(inst.seed, i, 4) - 0.6f) * 0.5f * p - 1.2f * life * life, 0);
                buf.billboard(ctx, CRUMB[i % 3], VfxBlend.ALPHA, q, 0.28f * p, age * 0.4f + i, col(WHITE, (1f - life) * fade));
            }
        }

        // 3) landing: a small manga bubble and a spray of crumbs at 'to'
        float ti = clamp01((t - 0.82f) / 0.18f);
        if (t > 0.82f) {
            float k = 1f - ti;
            float sz = 1.7f * p * VfxAnim.easeOutCubic(clamp01(ti * 2.2f) + 0.15f);
            buf.billboard(ctx, BURST_INK, VfxBlend.ALPHA, b, sz * 1.08f, 0.2f, col(INK, k));
            buf.billboard(ctx, BURST, VfxBlend.ADD, b, sz, 0f, col(CREAM, 0.9f * k));
            VfxBloom.glow(ctx, buf, b, 0.9f * p, gold, k);
            Vector3f r = ctx.camRight, u = ctx.camUp;
            for (int i = 0; i < 6; i++) {
                float ang = hash(inst.seed, i, 5) * Mth.TWO_PI, rr = (0.3f + 1.2f * VfxAnim.easeOutCubic(ti)) * p;
                Vector3f q = new Vector3f(b).add(new Vector3f(r).mul(Mth.cos(ang) * rr)).add(new Vector3f(u).mul(Mth.sin(ang) * rr - 0.5f * ti * ti));
                buf.billboard(ctx, CRUMB[i % 3], VfxBlend.ALPHA, q, 0.3f * p, age * 0.5f + i * 2, col(WHITE, k));
            }
        }
    }

    // ------------------------------------------------------------------------------------------------------ FX2
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), R = Math.max(1.5f, inst.power);
        float fade = life(inst, age, 8, 12), open = VfxAnim.easeOutCubic(clamp01(age / 14f));
        int gold = tint(inst);
        Vector3f g = ctx.rel(inst.from(ctx));
        float rad = R * (0.35f + 0.65f * open);
        VfxPose floor = VfxPose.ground(new Vector3f(g).add(0, 0.04f, 0));

        // ground, back to front: warm disc, swallowing vortex, ink plate under a gold plate, fur rim, travelling pulse
        buf.plane(DISC, VfxBlend.ADD, floor, rad * 1.02f, col(AMBER, 0.55f * fade));
        buf.plane(SWIRL, VfxBlend.ALPHA, floor.lift(0.01f).spin(-age * 0.035f), rad * 0.98f, col(INK, 0.8f * fade));
        buf.plane(PLATE, VfxBlend.ALPHA, floor.lift(0.02f).spin(age * 0.012f), rad * 1.06f, col(INK, 0.95f * fade));
        buf.plane(PLATE, VfxBlend.ADD, floor.lift(0.03f).spin(age * 0.012f), rad * 1.05f, col(gold, 0.85f * fade));
        float breathe = 1f + 0.03f * Mth.sin(age * 0.25f);
        buf.plane(FUR, VfxBlend.ALPHA, floor.lift(0.02f).spin(age * 0.01f), rad * 1.22f * breathe, col(INK, 0.85f * fade));
        float pulse = (age % 32f) / 32f;
        buf.plane(DISC, VfxBlend.ADD, floor.lift(0.05f), rad * (0.2f + 0.85f * VfxAnim.easeOutCubic(pulse)), col(CREAM, 0.5f * (1f - pulse) * fade));
        VfxBloom.glow(ctx, buf, new Vector3f(g).add(0, 0.2f, 0), 0.9f + 0.1f * R, gold, 0.5f * fade);

        // the beast looms over the centre (ink and light, behind the cutlery fence)
        float loom = VfxAnim.easeOutCubic(clamp01((age - 6f) / 22f));
        float ms = Math.min(2.6f + 0.4f * R, 2f + 0.8f * R) * loom;
        Vector3f head = new Vector3f(g).add(0, 0.6f * ms + 0.5f, 0);
        if (loom > 0.02f) {
            buf.billboard(ctx, FUR, VfxBlend.ALPHA, head, ms * 1.7f, age * 0.01f, col(INK, 0.55f * fade));
            buf.billboard(ctx, MAW_INK, VfxBlend.ALPHA, head, ms, 0f, col(INK, 0.6f * fade));
            buf.billboard(ctx, MAW_LIGHT, VfxBlend.ADD, head, ms, 0f, col(gold, 0.5f * fade * (0.8f + 0.2f * Mth.sin(age * 0.3f))));
        }

        // the fence: giant forks and knives stand up round the rim, one after another
        int n = ctx.seg(12, 6);
        Vector3f camR = ctx.camRight;
        float h = Mth.clamp(0.8f + 0.14f * R, 1.1f, 2.8f);
        for (int i = 0; i < n; i++) {
            float ang = Mth.TWO_PI * i / n + age * 0.01f;
            float rise = VfxAnim.easeOutBack(clamp01((age - 3f - i * 0.8f) / 10f));
            if (rise <= 0.01f) continue;
            Vector3f foot = new Vector3f(g).add(Mth.cos(ang) * rad * 0.98f, 0.03f, Mth.sin(ang) * rad * 0.98f);
            float sway = 0.04f * Mth.sin(age * 0.2f + i);
            boolean fork = i % 2 == 0;
            stand(ctx, buf, fork ? FORK : KNIFE, VfxBlend.ALPHA, foot, h * 0.4f * (1 + sway), h * rise, col(STEEL, fade));
        }

        // rising crumbs and sparks
        int m = ctx.seg(8, 4);
        for (int i = 0; i < m; i++) {
            float lt = (age * 0.02f + hash(inst.seed, i, 1)) % 1f, ang = hash(inst.seed, i, 2) * Mth.TWO_PI;
            float rr = R * (0.15f + 0.8f * hash(inst.seed, i, 3));
            Vector3f q = new Vector3f(g).add(Mth.cos(ang + age * 0.01f) * rr, 0.2f + lt * (1.5f + 0.2f * R), Mth.sin(ang + age * 0.01f) * rr);
            float al = Mth.sin(lt * Mth.PI) * fade;
            buf.billboard(ctx, CRUMB[i % 3], VfxBlend.ALPHA, q, 0.26f + 0.04f * R, age * 0.12f + i, col(WHITE, al));
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, new Vector3f(q).add(camR.x * 0.3f, 0.2f, camR.z * 0.3f), 0.2f, age * 0.1f, col(gold, al));
        }
    }

    // ------------------------------------------------------------------------------------------------------ FX3
    private void impact(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), d = Math.max(1f, inst.duration), t = age / d, s = Math.max(0.4f, inst.power);
        int gold = tint(inst);
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f dir = new Vector3f(ctx.rel(inst.to(ctx))).sub(c);
        boolean hasDir = dir.lengthSquared() > 1e-3f;
        if (hasDir) dir.normalize();
        float fade = clamp01((d + 4 - age) / 10f), e = VfxAnim.easeOutCubic(clamp01(t * 1.5f));

        // afterglow (long) and the opening flash
        VfxBloom.glow(ctx, buf, c, 1.8f * s * (0.5f + 0.5f * e), gold, 0.6f * (1f - t));
        float flash = clamp01(1f - age / 6f);
        if (flash > 0f) buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, c, 5.5f * s * (0.4f + 0.6f * flash), 0f, col(CREAM, flash));

        // the manga bubble: light body, ink outline, speed lines turning in two directions
        float bub = VfxAnim.easeOutBack(clamp01(age / 5f)) * (1f + 0.15f * t);
        float bk = clamp01(1.25f - t * 1.25f);
        buf.billboard(ctx, SPEED, VfxBlend.ALPHA, c, 7f * s * (0.6f + 0.5f * e), age * 0.02f, col(INK, 0.85f * bk));
        buf.billboard(ctx, SPEED, VfxBlend.ALPHA, c, 5f * s * (0.6f + 0.7f * e), -age * 0.03f + 0.5f, col(INK, 0.7f * bk));
        buf.billboard(ctx, BURST, VfxBlend.ADD, c, 4.2f * s * bub, age * 0.01f, col(gold, 0.85f * bk));
        buf.billboard(ctx, BURST_INK, VfxBlend.ALPHA, c, 4.4f * s * bub, age * 0.01f, col(INK, bk));

        // the beast snaps out of the bubble
        float snap = VfxAnim.easeOutBack(clamp01((age - 2f) / 7f));
        float mk = clamp01(1.6f - t * 1.6f) * clamp01((age - 1f) / 2f);
        if (snap > 0.02f && mk > 0.01f) maw(ctx, buf, c, 2.6f * s * snap, 0.08f * Mth.sin(age * 0.8f) * (1f - t), mk, gold, false);

        // plates open outwards as shock rings (one on the ground, one facing the camera)
        float r1 = VfxAnim.easeOutCubic(clamp01(t * 1.3f)), r2 = VfxAnim.easeOutCubic(clamp01((t - 0.12f) * 1.3f));
        buf.plane(PLATE, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, -0.4f * s, 0)), 4.5f * s * r1, col(gold, 0.8f * (1f - r1)));
        buf.billboard(ctx, PLATE, VfxBlend.ADD, c, 6f * s * r2, age * 0.05f, col(CREAM, 0.6f * (1f - r2)));

        // cutlery and crumbs flung out, biased along the direction hint, falling
        int n = ctx.seg(14, 7);
        Vector3f r = ctx.camRight, u = ctx.camUp;
        for (int i = 0; i < n; i++) {
            float ang = hash(inst.seed, i, 1) * Mth.TWO_PI, rs = 0.55f + 0.7f * hash(inst.seed, i, 2);
            float dd = s * rs * 3.6f * VfxAnim.easeOutCubic(clamp01(t * 1.25f));
            Vector3f q = new Vector3f(c).add(new Vector3f(r).mul(Mth.cos(ang) * dd)).add(new Vector3f(u).mul(Mth.sin(ang) * dd * 0.8f));
            if (hasDir) q.add(new Vector3f(dir).mul(dd * 0.6f));
            q.y -= 1.4f * t * t * s;
            int type = i % 5;
            boolean tool = type < 2;
            ResourceLocation tex = type == 0 ? FORK : type == 1 ? KNIFE : CRUMB[type - 2];
            float sz = (tool ? 0.95f : 0.4f) * s * (0.7f + 0.5f * hash(inst.seed, i, 3));
            float spin = age * 0.35f * (hash(inst.seed, i, 4) - 0.5f) + i * 1.7f;
            buf.billboard(ctx, tex, VfxBlend.ALPHA, q, sz, tool ? (float) Mth.atan2(Mth.sin(ang), Mth.cos(ang)) - 1.57f + spin * 0.3f : spin, col(tool ? STEEL : WHITE, fade));
        }
        // gold sparks and, late, crumbs drifting up out of the afterglow
        int m = ctx.seg(8, 4);
        for (int i = 0; i < m; i++) {
            float ang = hash(inst.seed, i, 7) * Mth.TWO_PI, dd = s * (0.6f + 2.6f * hash(inst.seed, i, 8)) * e;
            Vector3f q = new Vector3f(c).add(new Vector3f(r).mul(Mth.cos(ang) * dd)).add(new Vector3f(u).mul(Mth.sin(ang) * dd)).add(0, 0.5f * t * s, 0);
            buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, q, 0.28f * s, age * 0.2f + i, col(i % 2 == 0 ? gold : CREAM, fade * (1f - t * 0.5f)));
        }
    }
}
