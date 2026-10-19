package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Vine Magic (1.93.0 rebuild): twisted green vines with ivory thorns, curling tendrils, lime glow. Sprites from tools/gen_vine_rebuild_textures.py.
 * <ul>
 *   <li>VINE_FX1, CAST / PROJECTILE, "Thorn Whip". A spiral-tendril sigil contracts on 'from'; two vine straps wind round the line to 'to' like a cracking whip, thorns bristling off them, a
 *       tendril curling on the head; it lands in a thorn star with leaves and curls. from = origin, to = target, power = size, duration = flight ticks (default 16).</li>
 *   <li>VINE_FX2, ZONE / FIELD, "Thicket". The sigil turns on the ground, vine columns grow out of it round the rim one after another and curl over at the top, thorns line the inner
 *       ring, leaves fall and lime motes rise, a ring pulses out every 26 ticks. from = centre, power = RADIUS, duration = life (default 80).</li>
 *   <li>VINE_FX3, IMPACT / SIGNATURE, "Verdant Maelstrom". A flash and a thorn star, the ground ring runs out, vines spiral up and lean inward over the centre, thorn shards and leaves fly out,
 *       curls rise in the afterglow. from = centre, to = direction hint, power = scale, duration = life (default 28).</li>
 * </ul>
 */
public class VineLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final ResourceLocation MOTE = t("vine_mote"), LEAF = t("vine_leaf"), SIGIL = t("vine_sigil"), STRAP = t("vine_strap"), THORN = t("vine_thorn"), CURL = t("vine_curl"),
            BUD = t("vine_bud"), BURST = t("vine_burst"), GLOW = VfxTextures.GLOW;
    private static final int LIME = 0xFFB6FF5A, GREEN = 0xFFFFFFFF, PALE = 0xFFEFFFB0;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.VINE_FX1, VfxShape.VINE_FX2, VfxShape.VINE_FX3); }

    @Override
    public int defaultDuration(VfxShape s) { return s == VfxShape.VINE_FX1 ? 16 : s == VfxShape.VINE_FX2 ? 80 : 28; }

    @Override public int defaultColor(VfxShape s) { return LIME; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case VINE_FX1 -> cast(inst, ctx, buf);
            case VINE_FX2 -> zone(inst, ctx, buf);
            case VINE_FX3 -> signature(inst, ctx, buf);
            default -> { }
        }
    }

    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float D = Math.max(4, inst.duration), age = inst.ageTicks(ctx.partialTick), p = age / D, pw = Mth.clamp(inst.power, 0.6f, 3f);
        TreeFx.Line l = TreeFx.Line.of(ctx.rel(inst.from(ctx)), ctx.rel(inst.to(ctx)));
        int glow = TreeFx.lean(LIME, inst, 0.2f);
        float fade = TreeFx.life(inst, age, 0, D * 0.16f);

        float cp = TreeFx.sm(0f, 0.08f, p) * (1f - TreeFx.sm(0.22f, 0.36f, p));
        if (cp > 0.01f) {
            float shrink = 1.5f - 1.0f * VfxAnim.easeOutCubic(Mth.clamp(p / 0.3f, 0, 1));
            VfxPose sig = VfxPose.facing(l.f(), l.axis()).spin(age * 0.33f);
            buf.plane(SIGIL, VfxBlend.ALPHA, sig, 0.42f * pw * shrink, TreeFx.a(TreeFx.WHITE, cp));
            buf.plane(SIGIL, VfxBlend.ADD, sig.spin(-age * 0.6f).lift(0.01f), 0.5f * pw * shrink, TreeFx.a(glow, cp * 0.55f));
            buf.billboard(ctx, GLOW, VfxBlend.ADD, l.f(), 0.9f * pw * cp, 0f, TreeFx.a(glow, cp * 0.8f));
            buf.billboard(ctx, BUD, VfxBlend.ALPHA, l.f(), 0.3f * pw * VfxAnim.easeOutBack(Mth.clamp(p / 0.22f, 0, 1)), age * 0.12f, TreeFx.a(TreeFx.WHITE, cp));
            for (int i = 0; i < 5; i++) {
                float k = (TreeFx.h(inst, i, 1) + age * 0.07f) % 1f;
                Vector3f d = TreeFx.dirOf(TreeFx.h(inst, i, 2), TreeFx.h(inst, i, 3), false).mul(0.9f * pw * (1f - k));
                buf.billboard(ctx, MOTE, VfxBlend.ADD, new Vector3f(l.f()).add(d), 0.18f * pw, k * 6f, TreeFx.a(glow, cp * Mth.sin(Mth.PI * k)));
            }
        }
        float hp = (float) Math.pow(Mth.clamp((p - 0.2f) / 0.55f, 0f, 1f), 0.8);
        float tail = Mth.clamp((p - 0.55f) / 0.4f, 0f, 1f);
        tail = Math.min(tail * tail, hp - 0.04f);
        if (hp > 0.02f && tail < hp) {
            TreeFx.strands(buf, STRAP, VfxBlend.ALPHA, l, tail, hp, 2, ctx.seg(10, 5), 0.3f * pw, 2.5f, age * 0.7f, 0.34f * pw, TreeFx.WHITE, fade);
            for (int k = 0; k < 7; k++) {
                float s = Mth.lerp((k + 0.5f) / 7f, tail, hp);
                Vector3f pt = TreeFx.wind(l, s, 0.3f * pw, 2.5f, age * 0.7f, (k % 2) * 0.5f);
                Vector3f out = new Vector3f(pt).sub(l.at(s));
                if (out.lengthSquared() < 1e-6f) out.set(l.u());
                out.normalize().mul(0.22f * pw);
                TreeFx.strip(buf, THORN, VfxBlend.ALPHA, pt, new Vector3f(pt).add(out), 0.09f * pw, 0.05f * pw, 1f, 0f, TreeFx.a(TreeFx.WHITE, fade), TreeFx.a(TreeFx.WHITE, fade));
            }
            Vector3f ta = l.at(tail), hd = l.at(hp);
            buf.beam(ctx, GLOW, VfxBlend.ADD, ta, hd, 0.22f * pw, 0.6f * pw, 4, 0f, TreeFx.a(glow, 0f), TreeFx.a(glow, 0.5f * fade));
            float bloom = VfxAnim.easeOutBack(Mth.clamp((hp - 0.02f) / 0.2f, 0, 1));
            buf.billboard(ctx, GLOW, VfxBlend.ADD, hd, 0.8f * pw * bloom, 0f, TreeFx.a(glow, 0.55f * fade));
            buf.billboard(ctx, CURL, VfxBlend.ALPHA, hd, 0.5f * pw * bloom, age * 0.18f, TreeFx.a(TreeFx.WHITE, fade));
        }
        float imp = Mth.clamp((p - 0.74f) / 0.26f, 0f, 1f);
        if (imp > 0f) {
            Vector3f T = l.t();
            TreeFx.flash(buf, ctx, BURST, GLOW, T, pw, imp, age, TreeFx.lean(PALE, inst, 0.2f), glow);
            TreeFx.ground(buf, SIGIL, VfxBlend.ADD, T, (0.3f + 1.6f * VfxAnim.easeOutCubic(imp)) * pw, age * 0.2f, TreeFx.a(glow, (1f - imp) * 0.8f), 0f);
            TreeFx.debris(buf, ctx, inst, THORN, T, 6, 1.0f * pw, 0.24f * pw, imp, age, TreeFx.WHITE, 8, false);
            TreeFx.debris(buf, ctx, inst, LEAF, T, 4, 0.9f * pw, 0.24f * pw, imp, age, GREEN, 20, true);
        }
    }

    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), R = Math.max(1f, inst.power);
        float fade = TreeFx.life(inst, age, 8, 12);
        if (fade <= 0.01f) return;
        Vector3f c = ctx.rel(inst.from(ctx));
        int glow = TreeFx.lean(LIME, inst, 0.2f);
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 8f, 0, 1));
        TreeFx.ground(buf, SIGIL, VfxBlend.ALPHA, c, R * open, age * 0.025f, TreeFx.a(TreeFx.WHITE, 0.8f * fade), 0.02f);
        TreeFx.ground(buf, SIGIL, VfxBlend.ADD, c, R * 0.97f * open, -age * 0.04f, TreeFx.a(glow, 0.5f * fade), 0.04f);
        float pulse = (age % 26f) / 26f;
        if (age > 10f) TreeFx.ground(buf, SIGIL, VfxBlend.ADD, c, R * (0.2f + 0.85f * pulse), age * 0.05f, TreeFx.a(PALE, (1f - pulse) * 0.4f * fade), 0.06f);
        int n = ctx.seg(9, 5);
        TreeFx.ringOfStanding(buf, ctx, inst, STRAP, VfxBlend.ALPHA, c, n, R * 0.84f, 0.5f + 0.08f * R, 1.3f + 0.2f * R, age, 3f, 2f, 0.3f, TreeFx.WHITE, fade, 40);
        for (int i = 0; i < n; i++) {
            float ang = Mth.TWO_PI * (i + 0.5f * TreeFx.h(inst, i, 40)) / n;
            float grow = VfxAnim.easeOutBack(Mth.clamp((age - 3f - i * 2f) / 8f, 0, 1));
            if (grow <= 0.3f) continue;
            float jitter = 0.85f + 0.3f * TreeFx.h(inst, i, 41);
            Vector3f top = new Vector3f(c).add(Mth.cos(ang) * R * 0.84f, (1.3f + 0.2f * R) * jitter * grow, Mth.sin(ang) * R * 0.84f);
            buf.billboard(ctx, CURL, VfxBlend.ALPHA, top, 0.55f * (0.7f + 0.15f * R) * grow, Mth.sin(age * 0.07f + i) * 0.3f, TreeFx.a(TreeFx.WHITE, fade));
        }
        for (int i = 0; i < ctx.seg(6, 3); i++) {                                         // thorns along the inner ring
            float ang = Mth.TWO_PI * i / 6f + 0.3f;
            float grow = VfxAnim.easeOutCubic(Mth.clamp((age - 8f - i) / 8f, 0, 1));
            Vector3f base = new Vector3f(c).add(Mth.cos(ang) * R * 0.45f, 0, Mth.sin(ang) * R * 0.45f);
            TreeFx.standing(buf, ctx, THORN, VfxBlend.ALPHA, base, 0.3f, (0.5f + 0.1f * R) * grow, false, 0f, TreeFx.a(TreeFx.WHITE, fade));
        }
        TreeFx.drift(buf, ctx, inst, LEAF, VfxBlend.ALPHA, c, ctx.seg(7, 4), R * 0.9f, -(1.2f + 0.2f * R), 0.2f, age, 0.012f, GREEN, fade, 60);
        TreeFx.drift(buf, ctx, inst, MOTE, VfxBlend.ADD, c, ctx.seg(8, 4), R * 0.85f, 1.6f, 0.2f, age, 0.016f, glow, fade, 80);
    }

    private void signature(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float D = Math.max(6, inst.duration), age = inst.ageTicks(ctx.partialTick), p = age / D, pw = Mth.clamp(inst.power, 0.6f, 3f);
        Vector3f c = ctx.rel(inst.from(ctx));
        int glow = TreeFx.lean(LIME, inst, 0.2f);
        float fade = TreeFx.life(inst, age, 0, D * 0.3f);
        TreeFx.flash(buf, ctx, BURST, GLOW, new Vector3f(c).add(0, 0.5f * pw, 0), 1.2f * pw, Mth.clamp(p / 0.3f, 0f, 0.999f), age, TreeFx.lean(PALE, inst, 0.2f), glow);
        float e = VfxAnim.easeOutCubic(Mth.clamp(p / 0.35f, 0, 1));
        TreeFx.ground(buf, SIGIL, VfxBlend.ADD, c, (0.4f + 2.4f * e) * pw, age * 0.2f, TreeFx.a(glow, (1f - TreeFx.sm(0.2f, 1f, p)) * 0.9f), 0.03f);
        TreeFx.ground(buf, SIGIL, VfxBlend.ALPHA, c, (0.3f + 1.3f * e) * pw, -age * 0.12f, TreeFx.a(TreeFx.WHITE, (1f - TreeFx.sm(0.5f, 1f, p)) * 0.8f), 0.02f);
        int n = 7;
        for (int i = 0; i < n; i++) {                                                       // vines spiral up and lean inwards over the centre
            float ang = Mth.TWO_PI * i / n + age * 0.04f;
            float grow = VfxAnim.easeOutBack(Mth.clamp((p - 0.04f - i * 0.02f) / 0.28f, 0, 1));
            if (grow <= 0.01f) continue;
            float r = (0.9f + 0.4f * (i % 2)) * pw;
            Vector3f base = new Vector3f(c).add(Mth.cos(ang) * r, 0, Mth.sin(ang) * r);
            float hgt = (2.0f + 1.0f * TreeFx.h(inst, i, 4)) * pw * grow, w = 0.6f * pw;
            TreeFx.standing(buf, ctx, STRAP, VfxBlend.ALPHA, base, w, hgt, i % 2 == 0, -0.5f * w * Mth.cos(ang + 0.5f), TreeFx.a(TreeFx.WHITE, fade));
            if (grow > 0.7f) buf.billboard(ctx, CURL, VfxBlend.ALPHA, new Vector3f(base).add(0, hgt, 0), 0.5f * pw, age * 0.08f + i, TreeFx.a(TreeFx.WHITE, fade));
        }
        TreeFx.debris(buf, ctx, inst, THORN, new Vector3f(c).add(0, 0.4f, 0), 8, 2.2f * pw, 0.26f * pw, Mth.clamp((p - 0.05f) / 0.7f, 0, 1), age, TreeFx.WHITE, 30, true);
        TreeFx.debris(buf, ctx, inst, LEAF, new Vector3f(c).add(0, 1.4f * pw, 0), 8, 2.0f * pw, 0.26f * pw, Mth.clamp((p - 0.2f) / 0.8f, 0, 1), age, GREEN, 50, true);
        TreeFx.drift(buf, ctx, inst, MOTE, VfxBlend.ADD, c, 6, 1.4f * pw, 2.6f * pw, 0.22f * pw, age, 0.02f, glow, fade, 70);
    }
}
