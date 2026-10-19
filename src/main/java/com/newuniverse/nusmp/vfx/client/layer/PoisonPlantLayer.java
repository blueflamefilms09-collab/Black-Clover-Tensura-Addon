package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Poison Plant Magic (1.93.0 rebuild): violet and olive flesh, ivory fangs, acid-green venom and gas. Sprites from tools/gen_poison_plant_rebuild_textures.py.
 * <ul>
 *   <li>POISON_PLANT_FX1, CAST / PROJECTILE, "Venom Thorn". A toxic sigil contracts on 'from' while spores are drawn in; a hooked fang flies to 'to' in a trail of venom drops and gas puffs; it lands
 *       in a spiked star and a cloud of gas. from = origin, to = target, power = size, duration = flight ticks (default 16).</li>
 *   <li>POISON_PLANT_FX2, ZONE / FIELD, "Miasma". The sigil turns on the ground, thorny vines grow round the rim topped with spore pods, great gas puffs drift across the zone, spores rise and venom
 *       drops fall, a ring pulses out every 26 ticks. from = centre, power = RADIUS, duration = life (default 80).</li>
 *   <li>POISON_PLANT_FX3, IMPACT / SIGNATURE, "Toxic Bloom". A flash and a spiked star, a ring of fangs rises round the centre and closes in, a wall of gas runs out, spores and drops fly.
 *       from = centre, to = direction hint, power = scale, duration = life (default 28).</li>
 * </ul>
 */
public class PoisonPlantLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final ResourceLocation SPORE = t("poison_plant_spore"), MIST = t("poison_plant_mist"), THORN = t("poison_plant_thorn"), SIGIL = t("poison_plant_sigil"), DROP = t("poison_plant_drop"),
            VINE = t("poison_plant_vine"), BURST = t("poison_plant_burst"), MAW = t("poison_plant_maw"), GLOW = VfxTextures.GLOW;
    private static final int ACID = 0xFFB8FF30, PALE = 0xFFE8FF9A;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.POISON_PLANT_FX1, VfxShape.POISON_PLANT_FX2, VfxShape.POISON_PLANT_FX3); }

    @Override
    public int defaultDuration(VfxShape s) { return s == VfxShape.POISON_PLANT_FX1 ? 16 : s == VfxShape.POISON_PLANT_FX2 ? 80 : 28; }

    @Override public int defaultColor(VfxShape s) { return ACID; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case POISON_PLANT_FX1 -> cast(inst, ctx, buf);
            case POISON_PLANT_FX2 -> zone(inst, ctx, buf);
            case POISON_PLANT_FX3 -> signature(inst, ctx, buf);
            default -> { }
        }
    }

    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float D = Math.max(4, inst.duration), age = inst.ageTicks(ctx.partialTick), p = age / D, pw = Mth.clamp(inst.power, 0.6f, 3f);
        TreeFx.Line l = TreeFx.Line.of(ctx.rel(inst.from(ctx)), ctx.rel(inst.to(ctx)));
        int acid = TreeFx.lean(ACID, inst, 0.2f);
        float fade = TreeFx.life(inst, age, 0, D * 0.16f);

        float cp = TreeFx.sm(0f, 0.08f, p) * (1f - TreeFx.sm(0.22f, 0.36f, p));
        if (cp > 0.01f) {
            float shrink = 1.5f - 1.0f * VfxAnim.easeOutCubic(Mth.clamp(p / 0.3f, 0, 1));
            VfxPose sig = VfxPose.facing(l.f(), l.axis()).spin(age * 0.4f);
            buf.plane(SIGIL, VfxBlend.ALPHA, sig, 0.42f * pw * shrink, TreeFx.a(TreeFx.WHITE, cp));
            buf.plane(SIGIL, VfxBlend.ADD, sig.spin(-age * 0.7f).lift(0.01f), 0.5f * pw * shrink, TreeFx.a(acid, cp * 0.5f));
            buf.billboard(ctx, GLOW, VfxBlend.ADD, l.f(), 0.9f * pw * cp, 0f, TreeFx.a(acid, cp * 0.8f));
            for (int i = 0; i < 5; i++) {
                float k = (TreeFx.h(inst, i, 1) + age * 0.07f) % 1f;
                Vector3f d = TreeFx.dirOf(TreeFx.h(inst, i, 2), TreeFx.h(inst, i, 3), false).mul(0.9f * pw * (1f - k));
                buf.billboard(ctx, SPORE, VfxBlend.ADD, new Vector3f(l.f()).add(d), 0.2f * pw, k * 6f, TreeFx.a(acid, cp * Mth.sin(Mth.PI * k)));
            }
        }
        float hp = (float) Math.pow(Mth.clamp((p - 0.2f) / 0.55f, 0f, 1f), 0.8);
        float tail = Mth.clamp((p - 0.55f) / 0.4f, 0f, 1f);
        tail = Math.min(tail * tail, hp - 0.04f);
        if (hp > 0.02f && tail < hp) {
            Vector3f ta = l.at(tail), hd = l.at(hp);
            for (int k = 0; k < 4; k++) {                                                    // gas puffs left behind the fang
                float s = Mth.lerp((k + 0.5f) / 4f, tail, hp);
                buf.billboard(ctx, MIST, VfxBlend.ALPHA, TreeFx.wind(l, s, 0.15f * pw, 1.5f, age * 0.3f, k * 0.3f), (0.5f + 0.2f * k) * pw, age * 0.05f + k, TreeFx.a(TreeFx.WHITE, fade * 0.35f * (k + 1) / 4f));
            }
            for (int k = 0; k < 6; k++) {                                                    // drops of venom falling off the trail
                float s = Mth.lerp((k + 0.3f) / 6f, tail, hp);
                Vector3f pt = TreeFx.wind(l, s, 0.1f * pw, 2f, age * 0.4f, (k % 2) * 0.5f).add(0, -0.1f * pw * (1 - k / 6f) * 2, 0);
                buf.billboard(ctx, DROP, VfxBlend.ALPHA, pt, 0.16f * pw, 0f, TreeFx.a(TreeFx.WHITE, fade));
            }
            buf.beam(ctx, GLOW, VfxBlend.ADD, ta, hd, 0.2f * pw, 0.5f * pw, 4, 0f, TreeFx.a(acid, 0f), TreeFx.a(acid, 0.5f * fade));
            TreeFx.strip(buf, THORN, VfxBlend.ALPHA, new Vector3f(hd).sub(l.axis().x * 0.7f * pw, l.axis().y * 0.7f * pw, l.axis().z * 0.7f * pw), hd, 0.3f * pw, 0.08f * pw, 1f, 0f,
                    TreeFx.a(TreeFx.WHITE, fade), TreeFx.a(TreeFx.WHITE, fade));
            buf.billboard(ctx, GLOW, VfxBlend.ADD, hd, 0.7f * pw, 0f, TreeFx.a(acid, 0.6f * fade));
        }
        float imp = Mth.clamp((p - 0.74f) / 0.26f, 0f, 1f);
        if (imp > 0f) {
            Vector3f T = l.t();
            TreeFx.flash(buf, ctx, BURST, GLOW, T, pw, imp, age, TreeFx.lean(PALE, inst, 0.2f), acid);
            buf.billboard(ctx, MIST, VfxBlend.ALPHA, T, (0.8f + 2.2f * VfxAnim.easeOutCubic(imp)) * pw, age * 0.04f, TreeFx.a(TreeFx.WHITE, (1f - imp) * 0.7f));
            TreeFx.debris(buf, ctx, inst, DROP, T, 6, 1.0f * pw, 0.22f * pw, imp, age, TreeFx.WHITE, 8, true);
            TreeFx.debris(buf, ctx, inst, SPORE, T, 5, 1.1f * pw, 0.22f * pw, imp, age, acid, 20, true);
        }
    }

    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), R = Math.max(1f, inst.power);
        float fade = TreeFx.life(inst, age, 8, 12);
        if (fade <= 0.01f) return;
        Vector3f c = ctx.rel(inst.from(ctx));
        int acid = TreeFx.lean(ACID, inst, 0.2f);
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 8f, 0, 1));
        TreeFx.ground(buf, SIGIL, VfxBlend.ALPHA, c, R * open, age * 0.02f, TreeFx.a(TreeFx.WHITE, 0.75f * fade), 0.02f);
        TreeFx.ground(buf, SIGIL, VfxBlend.ADD, c, R * 0.97f * open, -age * 0.04f, TreeFx.a(acid, 0.5f * fade), 0.04f);
        float pulse = (age % 26f) / 26f;
        if (age > 10f) TreeFx.ground(buf, SIGIL, VfxBlend.ADD, c, R * (0.2f + 0.85f * pulse), age * 0.05f, TreeFx.a(PALE, (1f - pulse) * 0.4f * fade), 0.06f);
        int n = ctx.seg(8, 5);
        TreeFx.ringOfStanding(buf, ctx, inst, VINE, VfxBlend.ALPHA, c, n, R * 0.85f, 0.45f + 0.07f * R, 1.0f + 0.15f * R, age, 3f, 2f, 0.25f, TreeFx.WHITE, fade, 40);
        for (int i = 0; i < n; i++) {
            float ang = Mth.TWO_PI * (i + 0.5f * TreeFx.h(inst, i, 40)) / n;
            float grow = VfxAnim.easeOutBack(Mth.clamp((age - 3f - i * 2f) / 8f, 0, 1));
            if (grow <= 0.3f) continue;
            float jitter = 0.85f + 0.3f * TreeFx.h(inst, i, 41);
            Vector3f top = new Vector3f(c).add(Mth.cos(ang) * R * 0.85f, (1.0f + 0.15f * R) * jitter * grow, Mth.sin(ang) * R * 0.85f);
            buf.billboard(ctx, SPORE, VfxBlend.ADD, top, 0.5f * grow, age * 0.05f, TreeFx.a(acid, fade));
        }
        for (int i = 0; i < ctx.seg(6, 3); i++) {                                          // great gas puffs drifting across the zone
            float k = (TreeFx.h(inst, i, 90) + age * 0.006f) % 1f;
            float ang = TreeFx.h(inst, i, 91) * Mth.TWO_PI + age * 0.015f, r = R * 0.7f * Mth.sqrt(TreeFx.h(inst, i, 92));
            Vector3f at = new Vector3f(c).add(Mth.cos(ang) * r, 0.5f + 0.5f * k, Mth.sin(ang) * r);
            buf.billboard(ctx, MIST, VfxBlend.ALPHA, at, (1.2f + 0.3f * R) * (0.8f + 0.4f * TreeFx.h(inst, i, 93)), age * 0.02f + i, TreeFx.a(TreeFx.WHITE, fade * 0.45f * Mth.sin(Mth.PI * k)));
        }
        TreeFx.drift(buf, ctx, inst, SPORE, VfxBlend.ADD, c, ctx.seg(8, 4), R * 0.9f, 2.0f, 0.2f, age, 0.014f, acid, fade, 60);
        TreeFx.drift(buf, ctx, inst, DROP, VfxBlend.ALPHA, c, ctx.seg(5, 3), R * 0.8f, -(1.8f), 0.16f, age, 0.012f, TreeFx.WHITE, fade, 80);
    }

    private void signature(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float D = Math.max(6, inst.duration), age = inst.ageTicks(ctx.partialTick), p = age / D, pw = Mth.clamp(inst.power, 0.6f, 3f);
        Vector3f c = ctx.rel(inst.from(ctx));
        int acid = TreeFx.lean(ACID, inst, 0.2f);
        float fade = TreeFx.life(inst, age, 0, D * 0.3f);
        TreeFx.flash(buf, ctx, BURST, GLOW, new Vector3f(c).add(0, 0.5f * pw, 0), 1.2f * pw, Mth.clamp(p / 0.3f, 0f, 0.999f), age, TreeFx.lean(PALE, inst, 0.2f), acid);
        float e = VfxAnim.easeOutCubic(Mth.clamp(p / 0.35f, 0, 1));
        TreeFx.ground(buf, SIGIL, VfxBlend.ADD, c, (0.4f + 2.4f * e) * pw, age * 0.2f, TreeFx.a(acid, (1f - TreeFx.sm(0.2f, 1f, p)) * 0.9f), 0.03f);
        TreeFx.ground(buf, SIGIL, VfxBlend.ALPHA, c, (0.3f + 1.2f * e) * pw, -age * 0.1f, TreeFx.a(TreeFx.WHITE, (1f - TreeFx.sm(0.5f, 1f, p)) * 0.8f), 0.02f);
        int n = 8;
        float close = TreeFx.sm(0.35f, 0.7f, p);                                               // the fangs rise round the centre and close in
        for (int i = 0; i < n; i++) {
            float ang = Mth.TWO_PI * i / n;
            float grow = VfxAnim.easeOutBack(Mth.clamp((p - 0.04f - i * 0.015f) / 0.26f, 0, 1));
            if (grow <= 0.01f) continue;
            float r = (1.7f - 1.0f * close) * pw;
            Vector3f base = new Vector3f(c).add(Mth.cos(ang) * r, 0, Mth.sin(ang) * r);
            TreeFx.standing(buf, ctx, THORN, VfxBlend.ALPHA, base, 0.5f * pw, 1.7f * pw * grow, i % 2 == 0, -Mth.cos(ang) * 0.35f * pw * close, TreeFx.a(TreeFx.WHITE, fade));
        }
        for (int i = 0; i < 5; i++) {
            float k = Mth.clamp((p - 0.1f) / 0.9f, 0, 1);
            float ang = Mth.TWO_PI * i / 5f + 0.4f;
            buf.billboard(ctx, MIST, VfxBlend.ALPHA, new Vector3f(c).add(Mth.cos(ang) * (0.5f + 2.2f * k) * pw, 0.5f * pw + 0.4f * k, Mth.sin(ang) * (0.5f + 2.2f * k) * pw),
                    (1.0f + 1.4f * k) * pw, age * 0.03f + i, TreeFx.a(TreeFx.WHITE, (1f - k) * 0.6f));
        }
        TreeFx.debris(buf, ctx, inst, DROP, new Vector3f(c).add(0, 0.5f, 0), 8, 2.2f * pw, 0.24f * pw, Mth.clamp((p - 0.05f) / 0.7f, 0, 1), age, TreeFx.WHITE, 30, true);
        TreeFx.drift(buf, ctx, inst, SPORE, VfxBlend.ADD, c, 8, 1.6f * pw, 2.8f * pw, 0.22f * pw, age, 0.02f, acid, fade, 70);
    }
}
