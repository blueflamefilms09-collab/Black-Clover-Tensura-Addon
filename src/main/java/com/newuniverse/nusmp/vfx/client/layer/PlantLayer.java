package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Plant Magic (1.93.0 rebuild): soft pink and white petals, golden hearts, green leaves and drifting pollen. Sprites from tools/gen_plant_rebuild_textures.py.
 * <ul>
 *   <li>PLANT_FX1, CAST / PROJECTILE, "Seed Bloom". A flower mandala contracts on 'from'; a golden seed flies to 'to' on a thin green stem, shedding petals in a spiral; where it lands a flower
 *       opens, a ring of petals bursts and petals fall. from = origin, to = target, power = size, duration = flight ticks (default 16).</li>
 *   <li>PLANT_FX2, ZONE / FIELD, "Hundred Flowers". The mandala turns on the ground; blossoms pop up all over it one after another, petals and pollen rise through the air, a soft golden ring
 *       pulses out every 26 ticks. from = centre, power = RADIUS, duration = life (default 80).</li>
 *   <li>PLANT_FX3, IMPACT / SIGNATURE, "Great Bloom". A flash, a giant flower opens above the centre, a ring of petals runs out, petals fly and fall, pollen rises. from = centre, to = direction hint,
 *       power = scale, duration = life (default 28).</li>
 * </ul>
 */
public class PlantLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final ResourceLocation PETAL = t("plant_petal"), FLOWER = t("plant_flower"), MOTE = t("plant_mote"), SIGIL = t("plant_sigil"), LEAF = t("plant_leaf"), STEM = t("plant_stem"),
            BURST = t("plant_burst"), HEART = t("plant_heart"), GLOW = VfxTextures.GLOW;
    private static final int GOLD = 0xFFFFE690, PINK = 0xFFFFFFFF, SOFT = 0xFFFFC8E0;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.PLANT_FX1, VfxShape.PLANT_FX2, VfxShape.PLANT_FX3); }

    @Override
    public int defaultDuration(VfxShape s) { return s == VfxShape.PLANT_FX1 ? 16 : s == VfxShape.PLANT_FX2 ? 80 : 28; }

    @Override public int defaultColor(VfxShape s) { return GOLD; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case PLANT_FX1 -> cast(inst, ctx, buf);
            case PLANT_FX2 -> zone(inst, ctx, buf);
            case PLANT_FX3 -> signature(inst, ctx, buf);
            default -> { }
        }
    }

    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float D = Math.max(4, inst.duration), age = inst.ageTicks(ctx.partialTick), p = age / D, pw = Mth.clamp(inst.power, 0.6f, 3f);
        TreeFx.Line l = TreeFx.Line.of(ctx.rel(inst.from(ctx)), ctx.rel(inst.to(ctx)));
        int gold = TreeFx.lean(GOLD, inst, 0.2f);
        float fade = TreeFx.life(inst, age, 0, D * 0.16f);

        float cp = TreeFx.sm(0f, 0.08f, p) * (1f - TreeFx.sm(0.22f, 0.36f, p));
        if (cp > 0.01f) {
            float shrink = 1.5f - 1.0f * VfxAnim.easeOutCubic(Mth.clamp(p / 0.3f, 0, 1));
            VfxPose sig = VfxPose.facing(l.f(), l.axis()).spin(age * 0.25f);
            buf.plane(SIGIL, VfxBlend.ALPHA, sig, 0.44f * pw * shrink, TreeFx.a(TreeFx.WHITE, cp));
            buf.plane(SIGIL, VfxBlend.ADD, sig.spin(-age * 0.5f).lift(0.01f), 0.5f * pw * shrink, TreeFx.a(gold, cp * 0.5f));
            buf.billboard(ctx, HEART, VfxBlend.ADD, l.f(), 0.9f * pw * cp, age * 0.05f, TreeFx.a(gold, cp * 0.9f));
            for (int i = 0; i < 5; i++) {
                float k = (TreeFx.h(inst, i, 1) + age * 0.07f) % 1f;
                Vector3f d = TreeFx.dirOf(TreeFx.h(inst, i, 2), TreeFx.h(inst, i, 3), false).mul(0.9f * pw * (1f - k));
                buf.billboard(ctx, PETAL, VfxBlend.ALPHA, new Vector3f(l.f()).add(d), 0.2f * pw, k * 6f, TreeFx.a(PINK, cp * Mth.sin(Mth.PI * k)));
            }
        }
        float hp = (float) Math.pow(Mth.clamp((p - 0.2f) / 0.55f, 0f, 1f), 0.8);
        float tail = Mth.clamp((p - 0.55f) / 0.4f, 0f, 1f);
        tail = Math.min(tail * tail, hp - 0.04f);
        if (hp > 0.02f && tail < hp) {
            Vector3f ta = l.at(tail), hd = l.at(hp);
            TreeFx.strip(buf, STEM, VfxBlend.ALPHA, ta, hd, 0.2f * pw, 0.2f * pw, tail * l.len() * 0.6f, hp * l.len() * 0.6f, TreeFx.a(TreeFx.WHITE, fade * 0.3f), TreeFx.a(TreeFx.WHITE, fade));
            for (int k = 0; k < 9; k++) {                                                    // petals shed in a spiral round the line
                float s = Mth.lerp((k + 0.5f) / 9f, tail, hp);
                Vector3f pt = TreeFx.wind(l, s, 0.3f * pw, 2.0f, age * 0.5f, (k % 3) / 3f);
                buf.billboard(ctx, PETAL, VfxBlend.ALPHA, pt, 0.26f * pw, age * 0.2f + k * 1.4f, TreeFx.a(k % 2 == 0 ? PINK : SOFT, fade * (0.5f + 0.5f * (k / 9f))));
            }
            buf.beam(ctx, GLOW, VfxBlend.ADD, ta, hd, 0.2f * pw, 0.55f * pw, 4, 0f, TreeFx.a(gold, 0f), TreeFx.a(gold, 0.45f * fade));
            float bloom = VfxAnim.easeOutBack(Mth.clamp((hp - 0.02f) / 0.2f, 0, 1));
            buf.billboard(ctx, GLOW, VfxBlend.ADD, hd, 0.8f * pw * bloom, 0f, TreeFx.a(gold, 0.6f * fade));
            buf.billboard(ctx, HEART, VfxBlend.ADD, hd, 0.5f * pw * bloom, age * 0.1f, TreeFx.a(TreeFx.WHITE, fade));
        }
        float imp = Mth.clamp((p - 0.74f) / 0.26f, 0f, 1f);
        if (imp > 0f) {
            Vector3f T = l.t();
            float e = VfxAnim.easeOutBack(Mth.clamp(imp * 1.6f, 0, 1));
            buf.billboard(ctx, FLOWER, VfxBlend.ALPHA, T, (0.4f + 1.4f * e) * pw, age * 0.08f, TreeFx.a(TreeFx.WHITE, 1f - imp * 0.8f));
            buf.billboard(ctx, BURST, VfxBlend.ALPHA, T, (0.5f + 2.0f * VfxAnim.easeOutCubic(imp)) * pw, -age * 0.1f, TreeFx.a(PINK, (1f - imp) * 0.9f));
            buf.billboard(ctx, GLOW, VfxBlend.ADD, T, (0.7f + 1.2f * e) * pw, 0f, TreeFx.a(gold, (1f - imp) * 0.7f));
            TreeFx.debris(buf, ctx, inst, PETAL, T, 8, 1.1f * pw, 0.24f * pw, imp, age, PINK, 8, true);
            TreeFx.debris(buf, ctx, inst, MOTE, T, 4, 0.8f * pw, 0.2f * pw, imp, age, gold, 20, true);
        }
    }

    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), R = Math.max(1f, inst.power);
        float fade = TreeFx.life(inst, age, 8, 12);
        if (fade <= 0.01f) return;
        Vector3f c = ctx.rel(inst.from(ctx));
        int gold = TreeFx.lean(GOLD, inst, 0.2f);
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 8f, 0, 1));
        TreeFx.ground(buf, SIGIL, VfxBlend.ALPHA, c, R * open, age * 0.015f, TreeFx.a(TreeFx.WHITE, 0.8f * fade), 0.02f);
        TreeFx.ground(buf, SIGIL, VfxBlend.ADD, c, R * 0.97f * open, -age * 0.03f, TreeFx.a(gold, 0.4f * fade), 0.04f);
        TreeFx.ground(buf, GLOW, VfxBlend.ADD, c, R * 1.1f, 0f, TreeFx.a(SOFT, 0.25f * fade), 0.01f);
        float pulse = (age % 26f) / 26f;
        if (age > 10f) TreeFx.ground(buf, SIGIL, VfxBlend.ADD, c, R * (0.2f + 0.85f * pulse), age * 0.05f, TreeFx.a(gold, (1f - pulse) * 0.4f * fade), 0.06f);
        int n = ctx.seg(12, 6);
        for (int i = 0; i < n; i++) {                                                         // blossoms pop up all over the ground, on the spot where they stand
            float ang = Mth.TWO_PI * TreeFx.h(inst, i, 5), r = R * 0.9f * Mth.sqrt(TreeFx.h(inst, i, 6));
            float grow = VfxAnim.easeOutBack(Mth.clamp((age - 3f - i * 2.2f) / 8f, 0, 1));
            if (grow <= 0.01f) continue;
            float s = (0.45f + 0.35f * TreeFx.h(inst, i, 7)) * (0.8f + 0.1f * R);
            Vector3f base = new Vector3f(c).add(Mth.cos(ang) * r, 0.25f * s * grow, Mth.sin(ang) * r);
            buf.billboard(ctx, FLOWER, VfxBlend.ALPHA, base, s * grow, Mth.sin(age * 0.05f + i) * 0.2f, TreeFx.a(TreeFx.WHITE, fade));
        }
        TreeFx.drift(buf, ctx, inst, PETAL, VfxBlend.ALPHA, c, ctx.seg(8, 4), R * 0.9f, 2.2f, 0.22f, age, 0.012f, TreeFx.WHITE, fade, 60);
        TreeFx.drift(buf, ctx, inst, MOTE, VfxBlend.ADD, c, ctx.seg(8, 4), R * 0.85f, 1.8f, 0.2f, age, 0.016f, gold, fade, 80);
    }

    private void signature(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float D = Math.max(6, inst.duration), age = inst.ageTicks(ctx.partialTick), p = age / D, pw = Mth.clamp(inst.power, 0.6f, 3f);
        Vector3f c = ctx.rel(inst.from(ctx));
        int gold = TreeFx.lean(GOLD, inst, 0.2f);
        float fade = TreeFx.life(inst, age, 0, D * 0.3f);
        float e = VfxAnim.easeOutCubic(Mth.clamp(p / 0.35f, 0, 1));
        TreeFx.ground(buf, SIGIL, VfxBlend.ADD, c, (0.4f + 2.4f * e) * pw, age * 0.12f, TreeFx.a(gold, (1f - TreeFx.sm(0.2f, 1f, p)) * 0.9f), 0.03f);
        TreeFx.ground(buf, SIGIL, VfxBlend.ALPHA, c, (0.3f + 1.2f * e) * pw, -age * 0.08f, TreeFx.a(TreeFx.WHITE, (1f - TreeFx.sm(0.5f, 1f, p)) * 0.8f), 0.02f);
        Vector3f top = new Vector3f(c).add(0, 1.6f * pw, 0);
        float open = VfxAnim.easeOutBack(Mth.clamp((p - 0.04f) / 0.3f, 0, 1));
        buf.billboard(ctx, GLOW, VfxBlend.ADD, top, 3.2f * pw * open, 0f, TreeFx.a(gold, 0.5f * fade));
        buf.billboard(ctx, FLOWER, VfxBlend.ALPHA, top, 2.6f * pw * open, age * 0.04f, TreeFx.a(TreeFx.WHITE, fade));
        buf.billboard(ctx, HEART, VfxBlend.ADD, top, 1.3f * pw * open, -age * 0.07f, TreeFx.a(gold, fade));
        buf.billboard(ctx, BURST, VfxBlend.ALPHA, top, (0.6f + 4.0f * e) * pw, age * 0.05f, TreeFx.a(TreeFx.WHITE, (1f - TreeFx.sm(0.1f, 0.8f, p)) * 0.9f));
        TreeFx.debris(buf, ctx, inst, PETAL, top, 12, 2.6f * pw, 0.3f * pw, Mth.clamp((p - 0.1f) / 0.9f, 0, 1), age, TreeFx.WHITE, 30, true);
        TreeFx.debris(buf, ctx, inst, LEAF, new Vector3f(c).add(0, 0.8f * pw, 0), 6, 2.0f * pw, 0.26f * pw, Mth.clamp((p - 0.15f) / 0.85f, 0, 1), age, TreeFx.WHITE, 50, true);
        TreeFx.drift(buf, ctx, inst, MOTE, VfxBlend.ADD, c, 8, 1.6f * pw, 3.0f * pw, 0.22f * pw, age, 0.02f, gold, fade, 70);
    }
}
