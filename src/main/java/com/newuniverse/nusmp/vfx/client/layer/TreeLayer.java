package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Tree Magic (1.93.0 rebuild): heartwood and bark, amber sap, growth rings and green leaves. Three shapes, sprites from tools/gen_tree_rebuild_textures.py:
 * <ul>
 *   <li>TREE_FX1, CAST / PROJECTILE, "Root Lance". A growth-ring sigil contracts on 'from' while sap is drawn in; then a lance of two twisted root straps flies to 'to' (bark shards trailing,
 *       a sap glow running along it, a splinter star on its head) and lands in a splinter burst with bark and leaves. from = origin, to = target, power = size, duration = flight ticks (default 16).</li>
 *   <li>TREE_FX2, ZONE / FIELD, "Grove". A growth-ring sigil turns on the ground; branches grow out of the ground round the rim one after another and put on leaves, leaves fall and sap rises
 *       through the grove, a ring of light pulses out every 26 ticks. from = centre on the ground, power = RADIUS, duration = life (default 80; fades in over 8, out over 12).</li>
 *   <li>TREE_FX3, IMPACT / SIGNATURE, "Great Tree Rising". A flash and a ring on the ground, a crown of root masses erupts upward (they lean outward), bark and splinters fly, leaves fall in
 *       the afterglow. from = centre, to = direction hint (to - from leans the roots), power = scale, duration = life (default 28).</li>
 * </ul>
 */
public class TreeLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final ResourceLocation MOTE = t("tree_fx_mote"), LEAF = t("tree_fx_leaf"), RING = t("tree_fx_ring"), BARK = t("tree_fx_bark"), ROOT = t("tree_fx_root"),
            SPLINTER = t("tree_fx_splinter"), SEED = t("tree_fx_seed"), BRANCH = t("tree_fx_branch"), GLOW = VfxTextures.GLOW;
    private static final int SAP = 0xFFFFC85A, BARKC = 0xFFFFFFFF, LEAFC = 0xFFFFFFFF, PALE = 0xFFFFF0B8;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.TREE_FX1, VfxShape.TREE_FX2, VfxShape.TREE_FX3); }

    @Override
    public int defaultDuration(VfxShape s) { return s == VfxShape.TREE_FX1 ? 16 : s == VfxShape.TREE_FX2 ? 80 : 28; }

    @Override public int defaultColor(VfxShape s) { return SAP; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case TREE_FX1 -> cast(inst, ctx, buf);
            case TREE_FX2 -> zone(inst, ctx, buf);
            case TREE_FX3 -> signature(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ FX1: Root Lance
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float D = Math.max(4, inst.duration), age = inst.ageTicks(ctx.partialTick), p = age / D, pw = Mth.clamp(inst.power, 0.6f, 3f);
        TreeFx.Line l = TreeFx.Line.of(ctx.rel(inst.from(ctx)), ctx.rel(inst.to(ctx)));
        int sap = TreeFx.lean(SAP, inst, 0.2f);
        float fade = TreeFx.life(inst, age, 0, D * 0.16f);

        // 1. charge: the ring contracts on 'from', sap motes are drawn in
        float cp = TreeFx.sm(0f, 0.08f, p) * (1f - TreeFx.sm(0.22f, 0.36f, p));
        if (cp > 0.01f) {
            float shrink = 1.5f - 1.0f * VfxAnim.easeOutCubic(Mth.clamp(p / 0.3f, 0, 1));
            VfxPose sig = VfxPose.facing(l.f(), l.axis()).spin(age * 0.3f);
            buf.plane(RING, VfxBlend.ALPHA, sig, 0.42f * pw * shrink, TreeFx.a(TreeFx.WHITE, cp));
            buf.plane(RING, VfxBlend.ADD, sig.spin(-age * 0.55f).lift(0.01f), 0.5f * pw * shrink, TreeFx.a(sap, cp * 0.55f));
            buf.billboard(ctx, GLOW, VfxBlend.ADD, l.f(), 0.9f * pw * cp, 0f, TreeFx.a(sap, cp * 0.8f));
            for (int i = 0; i < 5; i++) {
                float k = (TreeFx.h(inst, i, 1) + age * 0.07f) % 1f;
                Vector3f d = TreeFx.dirOf(TreeFx.h(inst, i, 2), TreeFx.h(inst, i, 3), false).mul(0.9f * pw * (1f - k));
                buf.billboard(ctx, MOTE, VfxBlend.ADD, new Vector3f(l.f()).add(d), 0.2f * pw, k * 6f, TreeFx.a(sap, cp * Mth.sin(Mth.PI * k)));
            }
        }

        // 2. the lance: two twisted straps, bark shards, the sap glow, the splinter head
        float hp = (float) Math.pow(Mth.clamp((p - 0.2f) / 0.55f, 0f, 1f), 0.8);
        float tail = Mth.clamp((p - 0.55f) / 0.4f, 0f, 1f);
        tail = Math.min(tail * tail, hp - 0.04f);
        if (hp > 0.02f && tail < hp) {
            TreeFx.strands(buf, ROOT, VfxBlend.ALPHA, l, tail, hp, 2, ctx.seg(8, 4), 0.2f * pw, 2.2f, age * 0.6f, 0.34f * pw, BARKC, fade);
            for (int k = 0; k < 6; k++) {
                float s = Mth.lerp((k + 0.5f) / 6f, tail, hp);
                Vector3f pt = TreeFx.wind(l, s, 0.2f * pw, 2.2f, age * 0.6f, (k % 2) * 0.5f);
                Vector3f out = new Vector3f(pt).sub(l.at(s));
                if (out.lengthSquared() < 1e-6f) out.set(l.u());
                buf.billboard(ctx, BARK, VfxBlend.ALPHA, pt.add(out.normalize().mul(0.14f * pw)), 0.2f * pw, age * 0.3f + k, TreeFx.a(BARKC, fade));
            }
            Vector3f ta = l.at(tail), hd = l.at(hp);
            buf.beam(ctx, GLOW, VfxBlend.ADD, ta, hd, 0.25f * pw, 0.6f * pw, 4, 0f, TreeFx.a(sap, 0f), TreeFx.a(sap, 0.55f * fade));
            float bloom = VfxAnim.easeOutBack(Mth.clamp((hp - 0.02f) / 0.2f, 0, 1));
            buf.billboard(ctx, GLOW, VfxBlend.ADD, hd, 0.8f * pw * bloom, 0f, TreeFx.a(sap, 0.6f * fade));
            buf.billboard(ctx, SPLINTER, VfxBlend.ALPHA, hd, 0.5f * pw * bloom, age * 0.2f, TreeFx.a(TreeFx.WHITE, fade));
        }

        // 3. impact at 'to'
        float imp = Mth.clamp((p - 0.74f) / 0.26f, 0f, 1f);
        if (imp > 0f) {
            Vector3f T = l.t();
            TreeFx.flash(buf, ctx, SPLINTER, GLOW, T, pw, imp, age, TreeFx.lean(PALE, inst, 0.2f), sap);
            TreeFx.ground(buf, RING, VfxBlend.ADD, T, (0.3f + 1.7f * VfxAnim.easeOutCubic(imp)) * pw, age * 0.2f, TreeFx.a(sap, (1f - imp) * 0.8f), 0f);
            TreeFx.debris(buf, ctx, inst, BARK, T, 6, 1.0f * pw, 0.22f * pw, imp, age, BARKC, 8, false);
            TreeFx.debris(buf, ctx, inst, LEAF, T, 4, 0.9f * pw, 0.24f * pw, imp, age, LEAFC, 20, true);
        }
    }

    // ------------------------------------------------------------------ FX2: Grove
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), R = Math.max(1f, inst.power);
        float fade = TreeFx.life(inst, age, 8, 12);
        if (fade <= 0.01f) return;
        Vector3f c = ctx.rel(inst.from(ctx));
        int sap = TreeFx.lean(SAP, inst, 0.2f);
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 8f, 0, 1));

        // the sigil: a solid ring of bark-brown under a turning glow copy
        TreeFx.ground(buf, RING, VfxBlend.ALPHA, c, R * open, age * 0.02f, TreeFx.a(TreeFx.WHITE, 0.85f * fade), 0.02f);
        TreeFx.ground(buf, RING, VfxBlend.ADD, c, R * 0.97f * open, -age * 0.035f, TreeFx.a(sap, 0.5f * fade), 0.04f);
        // the pulse: a ring of light running out every 26 ticks
        float pulse = (age % 26f) / 26f;
        if (age > 10f) TreeFx.ground(buf, RING, VfxBlend.ADD, c, R * (0.2f + 0.85f * pulse), age * 0.05f, TreeFx.a(PALE, (1f - pulse) * 0.45f * fade), 0.06f);
        // the grove: branches grow out of the ground round the rim and put on leaves
        int n = ctx.seg(8, 5);
        TreeFx.ringOfStanding(buf, ctx, inst, BRANCH, VfxBlend.ALPHA, c, n, R * 0.82f, 0.5f + 0.1f * R, 0.9f + 0.18f * R, age, 4f, 2f, 0.12f, TreeFx.WHITE, fade, 40);
        for (int i = 0; i < n; i++) {
            float ang = Mth.TWO_PI * (i + 0.5f * TreeFx.h(inst, i, 40)) / n;
            float grow = VfxAnim.easeOutBack(Mth.clamp((age - 4f - i * 2f) / 8f, 0, 1));
            if (grow <= 0.3f) continue;
            float jitter = 0.85f + 0.3f * TreeFx.h(inst, i, 41);
            Vector3f top = new Vector3f(c).add(Mth.cos(ang) * R * 0.82f, (0.9f + 0.18f * R) * jitter * grow, Mth.sin(ang) * R * 0.82f);
            buf.billboard(ctx, LEAF, VfxBlend.ALPHA, top, 0.42f * (0.7f + 0.2f * R) * grow, age * 0.05f + i, TreeFx.a(LEAFC, fade));
        }
        // leaves fall, sap rises
        TreeFx.drift(buf, ctx, inst, LEAF, VfxBlend.ALPHA, c, ctx.seg(8, 4), R * 0.9f, -(1.2f + 0.2f * R), 0.2f, age, 0.012f, LEAFC, fade, 60);
        TreeFx.drift(buf, ctx, inst, MOTE, VfxBlend.ADD, c, ctx.seg(8, 4), R * 0.85f, 1.6f, 0.2f, age, 0.016f, sap, fade, 80);
    }

    // ------------------------------------------------------------------ FX3: Great Tree Rising
    private void signature(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float D = Math.max(6, inst.duration), age = inst.ageTicks(ctx.partialTick), p = age / D, pw = Mth.clamp(inst.power, 0.6f, 3f);
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f hint = ctx.rel(inst.to(ctx)).sub(c);
        float lean = hint.lengthSquared() > 0.04f ? Mth.clamp(hint.length() * 0.05f, 0f, 0.4f) : 0f;
        int sap = TreeFx.lean(SAP, inst, 0.2f);
        float fade = TreeFx.life(inst, age, 0, D * 0.3f);

        TreeFx.flash(buf, ctx, SPLINTER, GLOW, new Vector3f(c).add(0, 0.5f * pw, 0), 1.2f * pw, Mth.clamp(p / 0.3f, 0f, 0.999f), age, TreeFx.lean(PALE, inst, 0.2f), sap);
        float e = VfxAnim.easeOutCubic(Mth.clamp(p / 0.35f, 0, 1));
        TreeFx.ground(buf, RING, VfxBlend.ADD, c, (0.4f + 2.4f * e) * pw, age * 0.15f, TreeFx.a(sap, (1f - TreeFx.sm(0.2f, 1f, p)) * 0.9f), 0.03f);
        TreeFx.ground(buf, RING, VfxBlend.ALPHA, c, (0.3f + 1.2f * e) * pw, -age * 0.1f, TreeFx.a(TreeFx.WHITE, (1f - TreeFx.sm(0.5f, 1f, p)) * 0.8f), 0.02f);

        // the root masses erupt in a crown, leaning out, taller at the middle
        int n = 6;
        for (int i = 0; i < n; i++) {
            float ang = Mth.TWO_PI * (i + TreeFx.h(inst, i, 3) * 0.4f) / n;
            float grow = VfxAnim.easeOutBack(Mth.clamp((p - 0.04f - i * 0.02f) / 0.28f, 0, 1));
            if (grow <= 0.01f) continue;
            float r = (0.35f + 0.55f * (i % 2)) * pw;
            Vector3f base = new Vector3f(c).add(Mth.cos(ang) * r, 0, Mth.sin(ang) * r);
            float hgt = (1.9f + 1.1f * TreeFx.h(inst, i, 4)) * pw * grow, w = 0.75f * pw;
            TreeFx.standing(buf, ctx, ROOT, VfxBlend.ALPHA, base, w, hgt, i % 2 == 0, (0.3f + lean) * w * (i % 2 == 0 ? 1 : -1), TreeFx.a(TreeFx.WHITE, fade));
            if (grow > 0.7f) buf.billboard(ctx, LEAF, VfxBlend.ALPHA, new Vector3f(base).add(0, hgt, 0), 0.4f * pw, age * 0.06f + i, TreeFx.a(LEAFC, fade));
        }
        TreeFx.debris(buf, ctx, inst, BARK, new Vector3f(c).add(0, 0.4f, 0), 8, 2.2f * pw, 0.26f * pw, Mth.clamp((p - 0.05f) / 0.7f, 0, 1), age, BARKC, 30, true);
        TreeFx.debris(buf, ctx, inst, LEAF, new Vector3f(c).add(0, 1.4f * pw, 0), 8, 2.0f * pw, 0.26f * pw, Mth.clamp((p - 0.2f) / 0.8f, 0, 1), age, LEAFC, 50, true);
        TreeFx.drift(buf, ctx, inst, MOTE, VfxBlend.ADD, c, 6, 1.4f * pw, 2.6f * pw, 0.22f * pw, age, 0.02f, sap, fade, 70);
    }
}
