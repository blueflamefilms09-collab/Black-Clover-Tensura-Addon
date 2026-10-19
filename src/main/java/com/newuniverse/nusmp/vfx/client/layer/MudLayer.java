package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Mud Magic (1.93.0 rebuild): wet brown mud with bright highlights, clods, splashes, dust, cracked ground with amber fissures. Sprites from tools/gen_mud_rebuild_textures.py.
 * <ul>
 *   <li>MUD_FX1, CAST / PROJECTILE, "Mud Ball". Amber fissures spread on the ground sigil at 'from' while mud gathers; a heavy glob flies to 'to' on a sagging line of drops and dust and lands in a
 *       splash with clods. from = origin, to = target, power = size, duration = flight ticks (default 16).</li>
 *   <li>MUD_FX2, ZONE / FIELD, "Mud Pit". The cracked sigil sinks into the ground, ripples run out from the middle, blobs of mud rise, swell and burst, clods sink, dust drifts, a ring of light pulses
 *       every 26 ticks. from = centre, power = RADIUS, duration = life (default 80).</li>
 *   <li>MUD_FX3, IMPACT / SIGNATURE, "Mud Eruption". A flash, a column of mud shoots up and crowns in a splash, clods fly, a ring of dust runs out, the ground splits with amber light. from = centre,
 *       to = direction hint, power = scale, duration = life (default 28).</li>
 * </ul>
 */
public class MudLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final ResourceLocation GLOB = t("mud_glob"), SPLAT = t("mud_splat"), DRIP = t("mud_drip"), SIGIL = t("mud_sigil"), CHUNK = t("mud_chunk"), MIST = t("mud_mist"),
            BURST = t("mud_burst"), RIPPLE = t("mud_ripple"), GLOW = VfxTextures.GLOW;
    private static final int AMBER = 0xFFFF9A3C, PALE = 0xFFFFD79A;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.MUD_FX1, VfxShape.MUD_FX2, VfxShape.MUD_FX3); }

    @Override
    public int defaultDuration(VfxShape s) { return s == VfxShape.MUD_FX1 ? 16 : s == VfxShape.MUD_FX2 ? 80 : 28; }

    @Override public int defaultColor(VfxShape s) { return AMBER; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case MUD_FX1 -> cast(inst, ctx, buf);
            case MUD_FX2 -> zone(inst, ctx, buf);
            case MUD_FX3 -> signature(inst, ctx, buf);
            default -> { }
        }
    }

    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float D = Math.max(4, inst.duration), age = inst.ageTicks(ctx.partialTick), p = age / D, pw = Mth.clamp(inst.power, 0.6f, 3f);
        TreeFx.Line l = TreeFx.Line.of(ctx.rel(inst.from(ctx)), ctx.rel(inst.to(ctx)));
        int amber = TreeFx.lean(AMBER, inst, 0.2f);
        float fade = TreeFx.life(inst, age, 0, D * 0.16f);

        float cp = TreeFx.sm(0f, 0.08f, p) * (1f - TreeFx.sm(0.22f, 0.36f, p));
        if (cp > 0.01f) {
            float shrink = 1.5f - 1.0f * VfxAnim.easeOutCubic(Mth.clamp(p / 0.3f, 0, 1));
            VfxPose sig = VfxPose.facing(l.f(), l.axis()).spin(age * 0.22f);
            buf.plane(SIGIL, VfxBlend.ALPHA, sig, 0.44f * pw * shrink, TreeFx.a(TreeFx.WHITE, cp));
            buf.plane(SIGIL, VfxBlend.ADD, sig.spin(-age * 0.4f).lift(0.01f), 0.5f * pw * shrink, TreeFx.a(amber, cp * 0.45f));
            buf.billboard(ctx, GLOB, VfxBlend.ALPHA, l.f(), 0.7f * pw * cp * VfxAnim.easeOutBack(Mth.clamp(p / 0.25f, 0, 1)), age * 0.1f, TreeFx.a(TreeFx.WHITE, cp));
            for (int i = 0; i < 5; i++) {
                float k = (TreeFx.h(inst, i, 1) + age * 0.07f) % 1f;
                Vector3f d = TreeFx.dirOf(TreeFx.h(inst, i, 2), TreeFx.h(inst, i, 3), false).mul(0.9f * pw * (1f - k));
                buf.billboard(ctx, CHUNK, VfxBlend.ALPHA, new Vector3f(l.f()).add(d), 0.18f * pw, k * 6f, TreeFx.a(TreeFx.WHITE, cp * Mth.sin(Mth.PI * k)));
            }
        }
        float hp = (float) Math.pow(Mth.clamp((p - 0.2f) / 0.55f, 0f, 1f), 0.8);
        float tail = Mth.clamp((p - 0.55f) / 0.4f, 0f, 1f);
        tail = Math.min(tail * tail, hp - 0.04f);
        if (hp > 0.02f && tail < hp) {
            Vector3f hd = l.at(hp);
            for (int k = 0; k < 6; k++) {                                                    // a sagging line of drops and dust behind the glob
                float s = Mth.lerp((k + 0.5f) / 6f, tail, hp);
                Vector3f pt = l.at(s).add(0, -0.25f * pw * Mth.sin(Mth.PI * s) - 0.1f * pw * (6 - k) / 6f, 0);
                buf.billboard(ctx, k % 2 == 0 ? DRIP : MIST, VfxBlend.ALPHA, pt, (k % 2 == 0 ? 0.24f : 0.5f) * pw, k * 1.3f, TreeFx.a(TreeFx.WHITE, fade * (0.3f + 0.7f * k / 6f) * (k % 2 == 0 ? 1f : 0.5f)));
            }
            buf.billboard(ctx, GLOW, VfxBlend.ADD, hd, 0.7f * pw, 0f, TreeFx.a(amber, 0.3f * fade));
            float grow = VfxAnim.easeOutBack(Mth.clamp((hp - 0.02f) / 0.2f, 0, 1));
            buf.billboard(ctx, GLOB, VfxBlend.ALPHA, hd, (0.9f + 0.08f * Mth.sin(age * 0.8f)) * pw * grow, age * 0.15f, TreeFx.a(TreeFx.WHITE, fade));
        }
        float imp = Mth.clamp((p - 0.74f) / 0.26f, 0f, 1f);
        if (imp > 0f) {
            Vector3f T = l.t();
            buf.billboard(ctx, BURST, VfxBlend.ALPHA, T, (0.5f + 2.0f * VfxAnim.easeOutCubic(imp)) * pw, age * 0.1f, TreeFx.a(TreeFx.WHITE, 1f - imp));
            buf.billboard(ctx, GLOW, VfxBlend.ADD, T, (0.6f + 1.0f * imp) * pw, 0f, TreeFx.a(amber, (1f - imp) * 0.5f));
            TreeFx.ground(buf, SPLAT, VfxBlend.ALPHA, T, (0.3f + 1.2f * VfxAnim.easeOutCubic(imp)) * pw, age * 0.04f, TreeFx.a(TreeFx.WHITE, 1f - imp * 0.7f), 0.01f);
            TreeFx.debris(buf, ctx, inst, CHUNK, T, 6, 1.0f * pw, 0.24f * pw, imp, age, TreeFx.WHITE, 8, false);
            TreeFx.debris(buf, ctx, inst, DRIP, T, 5, 0.9f * pw, 0.2f * pw, imp, age, TreeFx.WHITE, 20, true);
        }
    }

    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), R = Math.max(1f, inst.power);
        float fade = TreeFx.life(inst, age, 8, 12);
        if (fade <= 0.01f) return;
        Vector3f c = ctx.rel(inst.from(ctx));
        int amber = TreeFx.lean(AMBER, inst, 0.2f);
        float open = VfxAnim.easeOutBack(Mth.clamp(age / 8f, 0, 1));
        TreeFx.ground(buf, SPLAT, VfxBlend.ALPHA, c, R * 1.05f * open, age * 0.004f, TreeFx.a(TreeFx.WHITE, 0.7f * fade), 0.015f);          // the pit itself, a wet dark pool
        TreeFx.ground(buf, SIGIL, VfxBlend.ALPHA, c, R * open, age * 0.01f, TreeFx.a(TreeFx.WHITE, 0.85f * fade), 0.025f);
        TreeFx.ground(buf, SIGIL, VfxBlend.ADD, c, R * 0.97f * open, -age * 0.02f, TreeFx.a(amber, 0.35f * fade), 0.04f);
        for (int k = 0; k < 2; k++) {                                                                                                         // ripples run out from the middle
            float ph = ((age + k * 13f) % 26f) / 26f;
            TreeFx.ground(buf, RIPPLE, VfxBlend.ALPHA, c, R * (0.15f + 0.85f * ph), 0f, TreeFx.a(TreeFx.WHITE, (1f - ph) * 0.8f * fade), 0.05f + 0.01f * k);
        }
        for (int i = 0; i < ctx.seg(8, 4); i++) {                                                                                             // blobs rise, swell and burst
            float k = (TreeFx.h(inst, i, 5) + age * 0.018f) % 1f;
            float ang = TreeFx.h(inst, i, 6) * Mth.TWO_PI, r = R * 0.8f * Mth.sqrt(TreeFx.h(inst, i, 7));
            float s = (0.3f + 0.4f * TreeFx.h(inst, i, 8)) * (0.8f + 0.1f * R);
            float sw = k < 0.8f ? k / 0.8f : 1f + (k - 0.8f) * 3f;
            buf.billboard(ctx, GLOB, VfxBlend.ALPHA, new Vector3f(c).add(Mth.cos(ang) * r, 0.1f + 0.35f * s * Math.min(1f, k * 1.25f), Mth.sin(ang) * r), s * sw * (k > 0.9f ? 1.3f : 1f), i + age * 0.02f,
                    TreeFx.a(TreeFx.WHITE, fade * (k < 0.9f ? 1f : (1f - k) * 10f)));
        }
        TreeFx.drift(buf, ctx, inst, MIST, VfxBlend.ALPHA, c, ctx.seg(5, 3), R * 0.9f, 1.4f, 0.55f, age, 0.008f, TreeFx.WHITE, fade * 0.5f, 60);
        TreeFx.drift(buf, ctx, inst, CHUNK, VfxBlend.ALPHA, c, ctx.seg(5, 3), R * 0.8f, -0.6f, 0.2f, age, 0.01f, TreeFx.WHITE, fade, 80);
    }

    private void signature(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float D = Math.max(6, inst.duration), age = inst.ageTicks(ctx.partialTick), p = age / D, pw = Mth.clamp(inst.power, 0.6f, 3f);
        Vector3f c = ctx.rel(inst.from(ctx));
        int amber = TreeFx.lean(AMBER, inst, 0.2f);
        float fade = TreeFx.life(inst, age, 0, D * 0.3f);
        float e = VfxAnim.easeOutCubic(Mth.clamp(p / 0.35f, 0, 1));
        TreeFx.ground(buf, SIGIL, VfxBlend.ALPHA, c, (0.4f + 2.2f * e) * pw, age * 0.02f, TreeFx.a(TreeFx.WHITE, (1f - TreeFx.sm(0.6f, 1f, p)) * 0.9f), 0.02f);
        TreeFx.ground(buf, SIGIL, VfxBlend.ADD, c, (0.4f + 2.2f * e) * pw, age * 0.02f, TreeFx.a(amber, (1f - TreeFx.sm(0.2f, 1f, p)) * 0.9f), 0.03f);
        TreeFx.ground(buf, RIPPLE, VfxBlend.ALPHA, c, (0.3f + 2.8f * e) * pw, 0f, TreeFx.a(TreeFx.WHITE, (1f - e) * 0.9f), 0.04f);
        TreeFx.flash(buf, ctx, BURST, GLOW, new Vector3f(c).add(0, 0.4f * pw, 0), 1.1f * pw, Mth.clamp(p / 0.3f, 0f, 0.999f), age, TreeFx.lean(PALE, inst, 0.2f), amber);
        float rise = VfxAnim.easeOutBack(Mth.clamp((p - 0.03f) / 0.3f, 0, 1));
        for (int k = 0; k < 5; k++) {                                                         // the column: stacked blobs that rise, the top one crowned by a splash
            float h = (0.35f + 0.55f * k) * pw * rise, s = (1.5f - 0.17f * k) * pw;
            buf.billboard(ctx, GLOB, VfxBlend.ALPHA, new Vector3f(c).add(0.12f * pw * Mth.sin(age * 0.5f + k), h, 0), s, k * 1.1f + age * 0.03f, TreeFx.a(TreeFx.WHITE, fade));
        }
        TreeFx.standing(buf, ctx, SPLAT, VfxBlend.ALPHA, new Vector3f(c).add(0, 2.6f * pw * rise, 0), 2.2f * pw, 0.01f, false, 0f, 0);
        buf.billboard(ctx, SPLAT, VfxBlend.ALPHA, new Vector3f(c).add(0, 2.7f * pw * rise, 0), 1.8f * pw * rise, age * 0.05f, TreeFx.a(TreeFx.WHITE, fade));
        TreeFx.debris(buf, ctx, inst, CHUNK, new Vector3f(c).add(0, 1.0f * pw, 0), 10, 2.6f * pw, 0.3f * pw, Mth.clamp((p - 0.06f) / 0.7f, 0, 1), age, TreeFx.WHITE, 30, true);
        TreeFx.debris(buf, ctx, inst, DRIP, new Vector3f(c).add(0, 2.2f * pw, 0), 8, 2.0f * pw, 0.24f * pw, Mth.clamp((p - 0.15f) / 0.85f, 0, 1), age, TreeFx.WHITE, 50, true);
        TreeFx.drift(buf, ctx, inst, MIST, VfxBlend.ALPHA, c, 4, 1.8f * pw, 2.0f * pw, 0.9f * pw, age, 0.015f, TreeFx.WHITE, fade * 0.5f, 70);
    }
}
