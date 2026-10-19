package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Mucus Magic (1.93.0 rebuild): translucent teal gel with bright rims, bubbles, long sticky strands and a glowing nucleus. Sprites from tools/gen_mucus_rebuild_textures.py.
 * <ul>
 *   <li>MUCUS_FX1, CAST / PROJECTILE, "Mucus Shot". A membrane sigil contracts on 'from' while a bubble swells; a wobbling blob flies to 'to' pulling a sticky strand and shedding bubbles; it lands
 *       in a splat and a burst of droplets. from = origin, to = target, power = size, duration = flight ticks (default 16).</li>
 *   <li>MUCUS_FX2, ZONE / FIELD, "Sticky Field". The membrane mandala turns on the ground, ripples of gel run out from the middle, sticky strands rise and sway round the rim, bubbles float up and
 *       pop, a ring pulses out every 26 ticks. from = centre, power = RADIUS, duration = life (default 80).</li>
 *   <li>MUCUS_FX3, IMPACT / SIGNATURE, "Engulfing Tide". A giant bubble swells round the centre with a glowing blob inside, strands arc across it, then it bursts in droplets and a ring of gel.
 *       from = centre, to = direction hint, power = scale, duration = life (default 28).</li>
 * </ul>
 */
public class MucusLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final ResourceLocation BUBBLE = t("mucus_bubble"), BLOB = t("mucus_blob"), STRAND = t("mucus_strand"), SIGIL = t("mucus_sigil"), SPLAT = t("mucus_splat"), DRIP = t("mucus_drip"),
            BURST = t("mucus_burst"), RING = t("mucus_ring"), GLOW = VfxTextures.GLOW;
    private static final int TEAL = 0xFF72D6B4, PALE = 0xFFDDFFF4;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.MUCUS_FX1, VfxShape.MUCUS_FX2, VfxShape.MUCUS_FX3); }

    @Override
    public int defaultDuration(VfxShape s) { return s == VfxShape.MUCUS_FX1 ? 16 : s == VfxShape.MUCUS_FX2 ? 80 : 28; }

    @Override public int defaultColor(VfxShape s) { return TEAL; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case MUCUS_FX1 -> cast(inst, ctx, buf);
            case MUCUS_FX2 -> zone(inst, ctx, buf);
            case MUCUS_FX3 -> signature(inst, ctx, buf);
            default -> { }
        }
    }

    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float D = Math.max(4, inst.duration), age = inst.ageTicks(ctx.partialTick), p = age / D, pw = Mth.clamp(inst.power, 0.6f, 3f);
        TreeFx.Line l = TreeFx.Line.of(ctx.rel(inst.from(ctx)), ctx.rel(inst.to(ctx)));
        int teal = TreeFx.lean(TEAL, inst, 0.2f);
        float fade = TreeFx.life(inst, age, 0, D * 0.16f);

        float cp = TreeFx.sm(0f, 0.08f, p) * (1f - TreeFx.sm(0.22f, 0.36f, p));
        if (cp > 0.01f) {
            float shrink = 1.5f - 1.0f * VfxAnim.easeOutCubic(Mth.clamp(p / 0.3f, 0, 1));
            VfxPose sig = VfxPose.facing(l.f(), l.axis()).spin(age * 0.2f);
            buf.plane(SIGIL, VfxBlend.ALPHA, sig, 0.44f * pw * shrink, TreeFx.a(TreeFx.WHITE, cp));
            buf.plane(SIGIL, VfxBlend.ADD, sig.spin(-age * 0.4f).lift(0.01f), 0.5f * pw * shrink, TreeFx.a(teal, cp * 0.45f));
            buf.billboard(ctx, BUBBLE, VfxBlend.ALPHA, l.f(), 0.8f * pw * cp * VfxAnim.easeOutBack(Mth.clamp(p / 0.25f, 0, 1)), 0f, TreeFx.a(TreeFx.WHITE, cp));
            buf.billboard(ctx, GLOW, VfxBlend.ADD, l.f(), 0.9f * pw * cp, 0f, TreeFx.a(teal, cp * 0.6f));
            for (int i = 0; i < 4; i++) {
                float k = (TreeFx.h(inst, i, 1) + age * 0.07f) % 1f;
                Vector3f d = TreeFx.dirOf(TreeFx.h(inst, i, 2), TreeFx.h(inst, i, 3), false).mul(0.9f * pw * (1f - k));
                buf.billboard(ctx, BUBBLE, VfxBlend.ALPHA, new Vector3f(l.f()).add(d), 0.16f * pw, 0f, TreeFx.a(TreeFx.WHITE, cp * Mth.sin(Mth.PI * k)));
            }
        }
        float hp = (float) Math.pow(Mth.clamp((p - 0.2f) / 0.55f, 0f, 1f), 0.8);
        float tail = Mth.clamp((p - 0.55f) / 0.4f, 0f, 1f);
        tail = Math.min(tail * tail, hp - 0.04f);
        if (hp > 0.02f && tail < hp) {
            Vector3f ta = l.at(tail), hd = l.at(hp);
            TreeFx.strands(buf, STRAND, VfxBlend.ALPHA, l, tail, hp, 2, ctx.seg(8, 4), 0.12f * pw, 1.6f, age * 0.5f, 0.3f * pw, TreeFx.WHITE, fade);
            for (int k = 0; k < 5; k++) {                                                    // bubbles shed behind the blob
                float s = Mth.lerp((k + 0.5f) / 5f, tail, hp);
                Vector3f pt = TreeFx.wind(l, s, 0.25f * pw, 1.3f, age * 0.35f, (k % 2) * 0.5f).add(0, 0.06f * pw * (5 - k), 0);
                buf.billboard(ctx, BUBBLE, VfxBlend.ALPHA, pt, (0.14f + 0.05f * (k % 3)) * pw, 0f, TreeFx.a(TreeFx.WHITE, fade * (0.4f + 0.6f * k / 5f)));
            }
            buf.billboard(ctx, GLOW, VfxBlend.ADD, hd, 0.8f * pw, 0f, TreeFx.a(teal, 0.45f * fade));
            float grow = VfxAnim.easeOutBack(Mth.clamp((hp - 0.02f) / 0.2f, 0, 1));
            float wob = 1f + 0.1f * Mth.sin(age * 0.9f);
            buf.billboard(ctx, BLOB, VfxBlend.ALPHA, hd, 0.95f * pw * grow * wob, age * 0.12f, TreeFx.a(TreeFx.WHITE, fade));
        }
        float imp = Mth.clamp((p - 0.74f) / 0.26f, 0f, 1f);
        if (imp > 0f) {
            Vector3f T = l.t();
            buf.billboard(ctx, BURST, VfxBlend.ALPHA, T, (0.5f + 2.0f * VfxAnim.easeOutCubic(imp)) * pw, age * 0.06f, TreeFx.a(TreeFx.WHITE, 1f - imp));
            buf.billboard(ctx, GLOW, VfxBlend.ADD, T, (0.6f + 1.0f * imp) * pw, 0f, TreeFx.a(teal, (1f - imp) * 0.5f));
            TreeFx.ground(buf, SPLAT, VfxBlend.ALPHA, T, (0.3f + 1.2f * VfxAnim.easeOutCubic(imp)) * pw, age * 0.03f, TreeFx.a(TreeFx.WHITE, 1f - imp * 0.7f), 0.01f);
            TreeFx.debris(buf, ctx, inst, DRIP, T, 5, 1.0f * pw, 0.22f * pw, imp, age, TreeFx.WHITE, 8, true);
            TreeFx.debris(buf, ctx, inst, BUBBLE, T, 6, 1.1f * pw, 0.2f * pw, imp, age, TreeFx.WHITE, 20, true);
        }
    }

    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), R = Math.max(1f, inst.power);
        float fade = TreeFx.life(inst, age, 8, 12);
        if (fade <= 0.01f) return;
        Vector3f c = ctx.rel(inst.from(ctx));
        int teal = TreeFx.lean(TEAL, inst, 0.2f);
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 8f, 0, 1));
        TreeFx.ground(buf, SPLAT, VfxBlend.ALPHA, c, R * 1.05f * open, age * 0.004f, TreeFx.a(TreeFx.WHITE, 0.5f * fade), 0.015f);
        TreeFx.ground(buf, SIGIL, VfxBlend.ALPHA, c, R * open, age * 0.012f, TreeFx.a(TreeFx.WHITE, 0.85f * fade), 0.025f);
        TreeFx.ground(buf, SIGIL, VfxBlend.ADD, c, R * 0.97f * open, -age * 0.022f, TreeFx.a(teal, 0.35f * fade), 0.04f);
        for (int k = 0; k < 2; k++) {
            float ph = ((age + k * 13f) % 26f) / 26f;
            TreeFx.ground(buf, RING, VfxBlend.ALPHA, c, R * (0.15f + 0.85f * ph), 0f, TreeFx.a(TreeFx.WHITE, (1f - ph) * 0.8f * fade), 0.05f + 0.01f * k);
        }
        int n = ctx.seg(8, 5);
        TreeFx.ringOfStanding(buf, ctx, inst, STRAND, VfxBlend.ALPHA, c, n, R * 0.8f, 0.4f + 0.05f * R, 1.2f + 0.18f * R, age, 3f, 2f, 0.4f, TreeFx.WHITE, fade * 0.9f, 40);
        for (int i = 0; i < ctx.seg(6, 3); i++) {                                            // fat bubbles floating up from the middle of the field
            float k = (TreeFx.h(inst, i, 5) + age * 0.012f) % 1f;
            float ang = TreeFx.h(inst, i, 6) * Mth.TWO_PI, r = R * 0.75f * Mth.sqrt(TreeFx.h(inst, i, 7));
            float s = (0.4f + 0.5f * TreeFx.h(inst, i, 8)) * (0.8f + 0.1f * R);
            buf.billboard(ctx, BUBBLE, VfxBlend.ALPHA, new Vector3f(c).add(Mth.cos(ang) * r, 0.2f + 1.8f * k, Mth.sin(ang) * r), s * (k > 0.92f ? 1.4f : 1f), 0f,
                    TreeFx.a(TreeFx.WHITE, fade * (k < 0.92f ? 1f : (1f - k) * 12f)));
        }
        TreeFx.drift(buf, ctx, inst, BUBBLE, VfxBlend.ALPHA, c, ctx.seg(8, 4), R * 0.9f, 2.0f, 0.18f, age, 0.016f, TreeFx.WHITE, fade, 60);
        TreeFx.drift(buf, ctx, inst, DRIP, VfxBlend.ALPHA, c, ctx.seg(4, 2), R * 0.8f, -1.6f, 0.16f, age, 0.012f, TreeFx.WHITE, fade, 80);
    }

    private void signature(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float D = Math.max(6, inst.duration), age = inst.ageTicks(ctx.partialTick), p = age / D, pw = Mth.clamp(inst.power, 0.6f, 3f);
        Vector3f c = ctx.rel(inst.from(ctx));
        int teal = TreeFx.lean(TEAL, inst, 0.2f);
        float fade = TreeFx.life(inst, age, 0, D * 0.2f);
        Vector3f mid = new Vector3f(c).add(0, 1.2f * pw, 0);
        float swell = VfxAnim.easeOutBack(Mth.clamp(p / 0.4f, 0, 1)), pop = TreeFx.sm(0.62f, 0.74f, p);
        if (pop < 1f) {
            float s = (3.2f * swell + 0.1f * Mth.sin(age * 0.7f)) * pw;
            buf.billboard(ctx, BUBBLE, VfxBlend.ALPHA, mid, s, 0f, TreeFx.a(TreeFx.WHITE, (1f - pop) * 0.9f));
            buf.billboard(ctx, BLOB, VfxBlend.ALPHA, mid, 1.5f * pw * swell, age * 0.1f, TreeFx.a(TreeFx.WHITE, (1f - pop)));
            buf.billboard(ctx, GLOW, VfxBlend.ADD, mid, 2.4f * pw * swell, 0f, TreeFx.a(teal, 0.45f * (1f - pop)));
            for (int i = 0; i < 3; i++) {
                float ang = age * 0.15f + i * 2.094f;
                Vector3f a = new Vector3f(mid).add(Mth.cos(ang) * 1.5f * pw * swell, -0.9f * pw * swell, Mth.sin(ang) * 1.5f * pw * swell);
                TreeFx.strip(buf, STRAND, VfxBlend.ALPHA, a, new Vector3f(mid).add(0, 0.9f * pw * swell, 0), 0.3f * pw, 0.2f * pw, 0f, 1f, TreeFx.a(TreeFx.WHITE, 0.8f * (1f - pop)), TreeFx.a(TreeFx.WHITE, 0.8f * (1f - pop)));
            }
        }
        float e = VfxAnim.easeOutCubic(Mth.clamp((p - 0.62f) / 0.38f, 0, 1));
        if (p > 0.62f) {
            buf.billboard(ctx, BURST, VfxBlend.ALPHA, mid, (1.0f + 3.6f * e) * pw, age * 0.05f, TreeFx.a(TreeFx.WHITE, (1f - e) * 0.9f));
            TreeFx.ground(buf, RING, VfxBlend.ALPHA, c, (0.4f + 2.6f * e) * pw, 0f, TreeFx.a(TreeFx.WHITE, (1f - e) * 0.9f), 0.03f);
            TreeFx.ground(buf, SPLAT, VfxBlend.ALPHA, c, (0.4f + 1.8f * e) * pw, age * 0.03f, TreeFx.a(TreeFx.WHITE, (1f - e * 0.8f) * 0.8f), 0.02f);
            TreeFx.debris(buf, ctx, inst, DRIP, mid, 8, 2.4f * pw, 0.26f * pw, e, age, TreeFx.WHITE, 30, true);
            TreeFx.debris(buf, ctx, inst, BUBBLE, mid, 8, 2.6f * pw, 0.24f * pw, e, age, TreeFx.WHITE, 50, true);
        }
        TreeFx.ground(buf, SIGIL, VfxBlend.ADD, c, (0.5f + 1.8f * swell) * pw, age * 0.05f, TreeFx.a(teal, 0.5f * fade), 0.02f);
    }
}
