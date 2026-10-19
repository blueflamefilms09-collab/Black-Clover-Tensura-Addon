package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Soul Magic (1.93.0 rebuild): pale teal soul fire, drifting souls with hollow eyes, rune rings and ribbons of flame. Textures: tools/gen_soul_rebuild_textures.py
 * (particle/soul_*.png). The three shapes of the shape protocol:
 * <ul>
 *   <li>SOUL_FX1, CAST / PROJECTILE, "Soul Flame Bolt". A rune ring and a small soul contract on 'from' (charge, 22 percent of the life); then a soul flame runs 'from' to 'to'
 *       (accelerating) trailing two flame ribbons and three small souls that peel off behind it and fade; at the end a skull flashes and a rune ring opens at 'to' while a
 *       soul climbs out of the hit. power = size (cast shapes are drawn 3 times larger by the manager: 1 usual), duration = flight ticks (default 16).</li>
 *   <li>SOUL_FX2, ZONE / FIELD, "Soul Vortex". A turning five-armed vortex on the ground to the radius under a counter-turning rune ring, souls spiralling inward along the arms
 *       while they rise, a column of flame ribbon standing in the middle, small flames guttering on the rim. from = centre on the ground, power = radius (zone shapes are drawn
 *       1.5 times larger), duration = life (default 80; in 8, out 12).</li>
 *   <li>SOUL_FX3, IMPACT / SIGNATURE, "Soul Release". A white flash, a skull in a ring of fire opening over the middle, a rune ring and a vortex flaring on the ground, souls
 *       leaving upward in spirals and a few flame ribbons rising. from = centre, to = optional direction hint, power = scale, duration = life (default 30).</li>
 * </ul>
 * The colour tints the light (default teal 0xFF72D9CC). Budget: well under the 400 vertices of an activation.
 */
public class SoulFxLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation WISP = t("soul_wisp"), FLAME = t("soul_flame"), VORTEX = t("soul_vortex"), RUNE = t("soul_rune"), RIBBON = t("soul_ribbon_v"),
            SKULL = t("soul_skull"), GLOW = VfxTextures.GLOW, SPARK = VfxTextures.SPARK;

    private static final int WHITE = 0xFFFFFFFF;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.SOUL_FX1, VfxShape.SOUL_FX2, VfxShape.SOUL_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case SOUL_FX1 -> 16;
            case SOUL_FX2 -> 80;
            default -> 30;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFF72D9CC; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case SOUL_FX1 -> cast(inst, ctx, buf);
            case SOUL_FX2 -> zone(inst, ctx, buf);
            case SOUL_FX3 -> release(inst, ctx, buf);
            default -> { }
        }
    }

    // ------------------------------------------------------------------ helpers
    private static int a(int argb, float k) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(k, 0f, 1f)); }
    private static int hot(int argb, float k) { return VfxVertexBuffer.whiten(argb, k); }
    private static float h(VfxInstance inst, int i, int salt) { return ElementFx.hash(inst.seed, i, salt); }
    private static float ramp(float t, float s, float e) { return Mth.clamp((t - s) / Math.max(1e-4f, e - s), 0f, 1f); }
    private static float out(float t) { return VfxAnim.easeOutCubic(Mth.clamp(t, 0f, 1f)); }
    private static float life(VfxInstance inst, float age, float in, float outT) {
        return Math.min(in <= 0 ? 1f : Mth.clamp(age / in, 0f, 1f), outT <= 0 ? 1f : Mth.clamp((inst.duration - age) / outT, 0f, 1f));
    }

    private static void glow(VfxVertexBuffer buf, VfxRenderContext ctx, Vector3f p, float size, int col, float k) {
        if (size > 0.01f && k > 0.01f) buf.billboard(ctx, GLOW, VfxBlend.ADD, p, size, 0f, a(col, k));
    }

    private static void ground(VfxVertexBuffer buf, ResourceLocation tex, Vector3f c, float lift, float half, float spin, int col) {
        if (half > 0.02f) buf.plane(tex, VfxBlend.ADD, VfxPose.ground(new Vector3f(c).add(0, lift, 0)).spin(spin), half, col);
    }

    // ------------------------------------------------------------------ FX1: Soul Flame Bolt
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), d = Math.max(6f, inst.duration);
        float u = Mth.clamp(inst.power, 0.5f, 6f) / 3f;
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(from);
        float dist = dir.length();
        boolean still = dist < 0.4f;
        if (still) dir.set(0, 0, 1); else dir.mul(1f / dist);
        int col = inst.color | 0xFF000000;
        float tl = 0.22f * d, ta = 0.78f * d;

        float tc = ramp(age, 0f, tl), cf = tc * (1f - ramp(age, tl, tl * 1.5f + 1f));
        if (still) cf = Math.max(cf, 1f - ramp(age, 0f, d));
        if (cf > 0.02f) {
            VfxPose face = VfxPose.facing(new Vector3f(from), new Vector3f(dir).lerp(new Vector3f(ctx.camRight).cross(ctx.camUp), 0.5f));
            buf.plane(RUNE, VfxBlend.ADD, face.spin(age * 0.2f), (1.1f - 0.5f * tc) * u * 0.9f, a(col, 0.85f * cf));
            buf.billboard(ctx, WISP, VfxBlend.ADD, new Vector3f(from).add(0, 0.2f * u * tc, 0), 0.8f * u * (0.4f + 0.6f * tc), 0f, a(hot(col, 0.5f), 0.9f * cf));
            glow(buf, ctx, from, 0.9f * u, col, 0.6f * cf);
        }
        if (still) return;

        float s = ramp(age, tl, ta), se = 0.4f * s + 0.6f * s * s;
        Vector3f head = new Vector3f(dir).mul(dist * se).add(from);
        float fly = s > 0f && s < 1f ? 1f : 0f;
        if (fly > 0f) {
            float trail = Math.min(dist * se, 3.0f * u + 0.6f);
            Vector3f tail = new Vector3f(head).sub(new Vector3f(dir).mul(trail));
            buf.beam(ctx, RIBBON, VfxBlend.ADD, tail, head, 0.7f * u, 0.3f * u, 1, 0f, a(col, 0f), a(hot(col, 0.5f), 0.95f));
            Vector3f off = new Vector3f(ctx.camUp).mul(0.12f * u * Mth.sin(age * 0.5f));
            buf.beam(ctx, RIBBON, VfxBlend.ADD, new Vector3f(tail).add(off), new Vector3f(head).sub(off), 0.5f * u, 0.2f * u, 1, 0.3f, a(col, 0f), a(WHITE, 0.7f));
            buf.billboard(ctx, FLAME, VfxBlend.ADD, head, 0.9f * u, Mth.sin(age * 0.6f) * 0.15f, a(hot(col, 0.4f), 0.95f));
            glow(buf, ctx, head, 0.8f * u, col, 0.7f);
            for (int k = 0; k < 3; k++) {
                float f = 0.2f + 0.25f * k;
                Vector3f p = new Vector3f(head).sub(new Vector3f(dir).mul(trail * f)).add(new Vector3f(ctx.camUp).mul(Mth.sin(age * 0.4f + k * 2f) * 0.2f * u));
                buf.billboard(ctx, WISP, VfxBlend.ADD, p, 0.45f * u * (1f - 0.2f * k), Mth.sin(age * 0.3f + k) * 0.3f, a(col, 0.85f * (1f - f)));
            }
        }
        float ti = ramp(age, ta - 1f, d);
        if (ti > 0f) {
            float k = out(ti);
            buf.billboard(ctx, SKULL, VfxBlend.ADD, to, 1.4f * u * k, 0f, a(hot(col, 0.5f), 0.85f * (1f - ti)));
            buf.plane(RUNE, VfxBlend.ADD, VfxPose.facing(new Vector3f(to), new Vector3f(dir).negate()).spin(age * 0.2f), 1.4f * u * k, a(col, 0.8f * (1f - ti)));
            buf.billboard(ctx, WISP, VfxBlend.ADD, new Vector3f(to).add(0, 0.9f * u * k, 0), 0.7f * u, 0f, a(WHITE, 0.8f * (1f - ti)));
            glow(buf, ctx, to, 1.2f * u * (1f - 0.4f * ti), col, 0.65f * (1f - ti));
        }
    }

    // ------------------------------------------------------------------ FX2: Soul Vortex
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float R = Math.max(1.2f, inst.power / 1.5f);
        float fade = life(inst, age, 8f, 12f), open = VfxAnim.easeOutBack(Mth.clamp(age / 10f, 0f, 1f));
        if (fade <= 0.01f) return;
        Vector3f c = ctx.rel(inst.from(ctx));
        int col = inst.color | 0xFF000000;
        float r = R * open;
        ground(buf, GLOW, c, 0.04f, r * 1.05f, 0f, a(col, 0.2f * fade));
        ground(buf, VORTEX, c, 0.05f, r * 1.04f, -age * 0.04f, a(col, 0.9f * fade));
        ground(buf, RUNE, c, 0.07f, r * 1.04f, age * 0.012f, a(hot(col, 0.4f), 0.7f * fade));
        ground(buf, VORTEX, c, 0.06f, r * 0.5f, age * 0.07f, a(hot(col, 0.5f), 0.55f * fade));
        int n = ctx.seg(10, 5);                                                             // souls spiralling inward and up
        for (int i = 0; i < n; i++) {
            float ph = (age * 0.017f + h(inst, i, 1)) % 1f, ang = h(inst, i, 2) * Mth.TWO_PI + ph * 4.5f, rr = r * (0.95f - 0.8f * ph);
            Vector3f p = new Vector3f(Mth.cos(ang) * rr, 0.15f + ph * 2.2f, Mth.sin(ang) * rr).add(c);
            buf.billboard(ctx, WISP, VfxBlend.ADD, p, 0.5f + 0.15f * h(inst, i, 3), Mth.sin(ph * 5f + i) * 0.4f, a(i % 3 == 0 ? hot(col, 0.5f) : col, 0.85f * Mth.sin(ph * Mth.PI) * fade));
        }
        Vector3f foot = new Vector3f(c).add(0, 0.05f, 0), top = new Vector3f(c).add(0, 3.0f * open, 0);        // a column of flame ribbon in the middle
        buf.beam(ctx, RIBBON, VfxBlend.ADD, top, foot, 0.9f, 0.5f, 1, -age * 0.02f, a(col, 0.05f), a(hot(col, 0.4f), 0.6f * fade));
        int m = ctx.seg(8, 4);
        for (int i = 0; i < m; i++) {
            float ang = Mth.TWO_PI * i / m + 0.4f;
            Vector3f p = new Vector3f(Mth.cos(ang) * r * 0.97f, 0.35f + 0.08f * Mth.sin(age * 0.2f + i), Mth.sin(ang) * r * 0.97f).add(c);
            buf.billboard(ctx, FLAME, VfxBlend.ADD, p, 0.6f, Mth.sin(age * 0.17f + i) * 0.2f, a(col, 0.8f * fade));
        }
        glow(buf, ctx, new Vector3f(c).add(0, 0.8f, 0), r * 0.6f, col, 0.2f * fade);
    }

    // ------------------------------------------------------------------ FX3: Soul Release
    private void release(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), d = Math.max(8f, inst.duration), t = Mth.clamp(age / d, 0f, 1f);
        float u = Mth.clamp(inst.power, 0.4f, 6f) / 3f;
        Vector3f c = ctx.rel(inst.from(ctx));
        int col = inst.color | 0xFF000000;
        float flash = 1f - ramp(t, 0f, 0.25f);
        Vector3f mid = new Vector3f(c).add(0, 1.0f * u, 0);
        glow(buf, ctx, mid, (1.5f + 1.5f * out(t)) * u, col, 0.55f * (1f - t));
        glow(buf, ctx, mid, 1.2f * u * flash, WHITE, 0.9f * flash);
        float k = out(ramp(t, 0f, 0.55f));
        buf.billboard(ctx, SKULL, VfxBlend.ADD, mid, (0.5f + 2.4f * k) * u, 0f, a(hot(col, 0.5f), 0.95f * (1f - ramp(t, 0.4f, 1f))));
        ground(buf, RUNE, c, 0.05f, 2.8f * u * out(ramp(t, 0f, 0.8f)), age * 0.06f, a(col, 0.9f * (1f - ramp(t, 0.45f, 1f))));
        ground(buf, VORTEX, c, 0.07f, 2.0f * u * out(ramp(t, 0f, 0.7f)), -age * 0.1f, a(hot(col, 0.4f), 0.8f * (1f - ramp(t, 0.35f, 1f))));
        int n = ctx.seg(10, 5);                                                             // souls leaving upward in spirals
        for (int i = 0; i < n; i++) {
            float st = h(inst, i, 1) * 0.3f, f = ramp(t, st, 1f), ang = h(inst, i, 2) * Mth.TWO_PI + f * 5f, rr = (0.3f + 1.4f * h(inst, i, 3)) * u * out(f);
            Vector3f p = new Vector3f(Mth.cos(ang) * rr, 0.3f * u + 3.4f * u * f, Mth.sin(ang) * rr).add(c);
            buf.billboard(ctx, WISP, VfxBlend.ADD, p, (0.55f + 0.2f * h(inst, i, 4)) * u, Mth.sin(f * 6f + i) * 0.4f, a(i % 3 == 0 ? WHITE : col, 0.9f * Mth.sin(Mth.PI * f)));
        }
        for (int i = 0; i < 3; i++) {
            float ang = i * Mth.TWO_PI / 3 + age * 0.05f;
            Vector3f foot = new Vector3f(Mth.cos(ang) * 0.9f * u, 0.05f, Mth.sin(ang) * 0.9f * u).add(c);
            buf.beam(ctx, RIBBON, VfxBlend.ADD, new Vector3f(foot).add(0, 3.2f * u * out(ramp(t, 0.05f, 0.7f)), 0), foot, 0.55f * u, 0.3f * u, 1, -age * 0.03f, a(col, 0.05f), a(hot(col, 0.4f), 0.7f * (1f - ramp(t, 0.5f, 1f))));
        }
    }
}
