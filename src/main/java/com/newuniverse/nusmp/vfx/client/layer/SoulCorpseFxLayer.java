package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Soul Corpse Magic (1.93.0 rebuild): a puppeteer's grave: pale violet-blue threads, cross stitches, bone and hands that climb out of the ground. Textures:
 * tools/gen_soulcorpse_rebuild_textures.py (particle/scorpse_*.png). The three shapes of the shape protocol:
 * <ul>
 *   <li>SOUL_CORPSE_FX1, CAST / PROJECTILE, "Marionette Thread". A ring of stitches and a small skeletal hand close round 'from' (charge, 22 percent of the life); then a nail runs
 *       'from' to 'to' (accelerating) paying out a taut thread that stays tied to 'from' behind it (the thread is drawn the whole way, with cross stitches along it) and shedding
 *       bone splinters; at the end the stitch ring snaps shut round 'to' and a hand closes on the spot. power = size (cast shapes are drawn 3 times larger by the manager: 1
 *       usual), duration = flight ticks (default 16).</li>
 *   <li>SOUL_CORPSE_FX2, ZONE / FIELD, "Grave Field". A turning stitched sigil on the ground to the radius with a ring of stitches on the rim, hands rising and sinking at their own
 *       beats, grave markers standing on the rim, threads rising from the ground and swaying, a pale glow. from = centre on the ground, power = radius (zone shapes are drawn 1.5
 *       times larger), duration = life (default 80; in 8, out 12).</li>
 *   <li>SOUL_CORPSE_FX3, IMPACT / SIGNATURE, "Necropolis". A white flash, the sigil flaring on the ground, a crown of hands thrusting up and fanning out, bone splinters flung along
 *       arcs, threads snapping outward like a star, a stitch ring running out. from = centre, to = optional direction hint, power = scale, duration = life (default 28).</li>
 * </ul>
 * The colour tints the light (default slate 0xFF6C7899 mixed to the thread's pale violet). Budget: well under the 400 vertices of an activation.
 */
public class SoulCorpseFxLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation HAND = t("scorpse_hand"), THREAD = t("scorpse_thread"), SIGIL = t("scorpse_sigil"), STITCH = t("scorpse_stitch"),
            SHARD = t("scorpse_shard"), GRAVE = t("scorpse_grave"), GLOW = VfxTextures.GLOW, SPARK = VfxTextures.SPARK;

    private static final int WHITE = 0xFFFFFFFF, PALE = 0xFFB6C0FF;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.SOUL_CORPSE_FX1, VfxShape.SOUL_CORPSE_FX2, VfxShape.SOUL_CORPSE_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case SOUL_CORPSE_FX1 -> 16;
            case SOUL_CORPSE_FX2 -> 80;
            default -> 28;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFF9AA8E8; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case SOUL_CORPSE_FX1 -> cast(inst, ctx, buf);
            case SOUL_CORPSE_FX2 -> zone(inst, ctx, buf);
            case SOUL_CORPSE_FX3 -> necropolis(inst, ctx, buf);
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

    // ------------------------------------------------------------------ FX1: Marionette Thread
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), d = Math.max(6f, inst.duration);
        float u = Mth.clamp(inst.power, 0.5f, 6f) / 3f;
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(from);
        float dist = dir.length();
        boolean still = dist < 0.4f;
        if (still) dir.set(0, 0, 1); else dir.mul(1f / dist);
        int col = hot(inst.color | 0xFF000000, 0.25f);
        float tl = 0.22f * d, ta = 0.78f * d;

        float tc = ramp(age, 0f, tl), cf = tc * (1f - ramp(age, tl, tl * 1.5f + 1f));
        if (still) cf = Math.max(cf, 1f - ramp(age, 0f, d));
        if (cf > 0.02f) {
            VfxPose face = VfxPose.facing(new Vector3f(from), new Vector3f(dir).lerp(new Vector3f(ctx.camRight).cross(ctx.camUp), 0.5f));
            buf.plane(STITCH, VfxBlend.ADD, face.spin(age * 0.25f), (1.1f - 0.5f * tc) * u * 0.9f, a(col, 0.85f * cf));
            buf.billboard(ctx, HAND, VfxBlend.ADD, new Vector3f(from).add(0, -0.1f * u, 0), 0.8f * u * (0.4f + 0.6f * tc), 0f, a(hot(col, 0.4f), 0.85f * cf));
            glow(buf, ctx, from, 0.8f * u, col, 0.55f * cf);
        }
        if (still) return;

        float s = ramp(age, tl, ta), se = 0.4f * s + 0.6f * s * s;
        Vector3f head = new Vector3f(dir).mul(dist * se).add(from);
        if (s > 0f) {                                                                        // the thread stays tied to 'from' the whole way
            float fadeT = 1f - ramp(age, ta + 1f, d);
            buf.beam(ctx, THREAD, VfxBlend.ADD, from, head, 0.16f * u, 0.12f * u, Math.max(1, Math.min(6, (int) (dist * se / 1.5f))), -age * 0.02f, a(PALE, 0.8f * fadeT), a(WHITE, 0.95f * fadeT));
        }
        if (s > 0f && s < 1f) {
            buf.billboard(ctx, SPARK, VfxBlend.ADD, head, 0.45f * u, age * 0.4f, a(WHITE, 0.95f));
            glow(buf, ctx, head, 0.8f * u, col, 0.7f);
            for (int k = 0; k < 3; k++) {
                float f = 0.2f + 0.25f * k;
                Vector3f p = new Vector3f(head).sub(new Vector3f(dir).mul(Math.min(dist * se, 2.5f * u) * f)).add(new Vector3f(ctx.camUp).mul(Mth.sin(age * 0.5f + k * 2f) * 0.15f * u));
                buf.billboard(ctx, SHARD, VfxBlend.ADD, p, 0.3f * u, age * 0.3f + k * 2f, a(col, 0.85f * (1f - f)));
            }
        }
        float ti = ramp(age, ta - 1f, d);
        if (ti > 0f) {
            float k = out(ti);
            buf.plane(STITCH, VfxBlend.ADD, VfxPose.facing(new Vector3f(to), new Vector3f(dir).negate()).spin(age * 0.25f), (1.6f - 0.8f * k) * u, a(col, 0.9f * (1f - ti)));
            buf.billboard(ctx, HAND, VfxBlend.ADD, new Vector3f(to).add(0, 0.2f * u, 0), 1.1f * u * k, 0f, a(hot(col, 0.4f), 0.85f * (1f - ti)));
            glow(buf, ctx, to, 1.1f * u * (1f - 0.4f * ti), col, 0.6f * (1f - ti));
        }
    }

    // ------------------------------------------------------------------ FX2: Grave Field
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float R = Math.max(1.2f, inst.power / 1.5f);
        float fade = life(inst, age, 8f, 12f), open = VfxAnim.easeOutBack(Mth.clamp(age / 10f, 0f, 1f));
        if (fade <= 0.01f) return;
        Vector3f c = ctx.rel(inst.from(ctx));
        int col = hot(inst.color | 0xFF000000, 0.2f);
        float r = R * open;
        ground(buf, GLOW, c, 0.04f, r * 1.05f, 0f, a(col, 0.18f * fade));
        ground(buf, SIGIL, c, 0.05f, r * 1.04f, age * 0.008f, a(col, 0.9f * fade));
        ground(buf, STITCH, c, 0.07f, r * 1.0f, -age * 0.02f, a(hot(col, 0.4f), 0.55f * fade));
        int n = ctx.seg(8, 4);                                                               // hands rising and sinking at their own beats
        for (int i = 0; i < n; i++) {
            float ang = h(inst, i, 1) * Mth.TWO_PI, rr = r * (0.2f + 0.7f * h(inst, i, 2)), beat = 0.5f + 0.5f * Mth.sin(age * (0.12f + 0.05f * h(inst, i, 3)) + i * 2.1f);
            float rise = 0.25f + 0.75f * beat;
            Vector3f p = new Vector3f(Mth.cos(ang) * rr, 0.55f * rise, Mth.sin(ang) * rr).add(c);
            buf.billboard(ctx, HAND, VfxBlend.ADD, p, (0.9f + 0.3f * h(inst, i, 4)) * Math.min(1.5f, 0.6f + R * 0.1f), (h(inst, i, 5) - 0.5f) * 0.4f, a(hot(col, 0.3f), 0.8f * fade * (0.4f + 0.6f * rise)));
        }
        int m = ctx.seg(6, 3);                                                               // grave markers on the rim
        for (int i = 0; i < m; i++) {
            float ang = Mth.TWO_PI * i / m + 0.5f;
            buf.billboard(ctx, GRAVE, VfxBlend.ADD, new Vector3f(Mth.cos(ang) * r * 0.97f, 0.5f, Mth.sin(ang) * r * 0.97f).add(c), 0.9f, 0f, a(col, 0.65f * fade));
        }
        for (int i = 0; i < ctx.seg(6, 3); i++) {                                            // threads rising from the ground, swaying
            float ang = h(inst, i, 6) * Mth.TWO_PI, rr = r * (0.1f + 0.8f * h(inst, i, 7)), sway = Mth.sin(age * 0.08f + i);
            Vector3f foot = new Vector3f(Mth.cos(ang) * rr, 0.05f, Mth.sin(ang) * rr).add(c), top = new Vector3f(foot).add(sway * 0.3f, 2.4f + 0.8f * h(inst, i, 8), 0f);
            buf.beam(ctx, THREAD, VfxBlend.ADD, foot, top, 0.12f, 0.06f, 1, age * 0.01f, a(PALE, 0.7f * fade), a(PALE, 0.05f));
        }
        glow(buf, ctx, new Vector3f(c).add(0, 0.8f, 0), r * 0.7f, col, 0.16f * fade);
    }

    // ------------------------------------------------------------------ FX3: Necropolis
    private void necropolis(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), d = Math.max(8f, inst.duration), t = Mth.clamp(age / d, 0f, 1f);
        float u = Mth.clamp(inst.power, 0.4f, 6f) / 3f;
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f dirHint = ctx.rel(inst.to(ctx)).sub(c);
        int col = hot(inst.color | 0xFF000000, 0.2f);
        float flash = 1f - ramp(t, 0f, 0.25f);
        Vector3f mid = new Vector3f(c).add(0, 0.8f * u, 0);
        glow(buf, ctx, mid, (1.3f + 1.5f * out(t)) * u, col, 0.5f * (1f - t));
        glow(buf, ctx, mid, 1.1f * u * flash, WHITE, 0.9f * flash);
        ground(buf, SIGIL, c, 0.05f, 2.8f * u * out(ramp(t, 0f, 0.7f)), age * 0.05f, a(col, 0.9f * (1f - ramp(t, 0.45f, 1f))));
        ground(buf, STITCH, c, 0.07f, 3.4f * u * out(ramp(t, 0.05f, 0.9f)), -age * 0.08f, a(hot(col, 0.4f), 0.8f * (1f - ramp(t, 0.3f, 1f))));
        int n = ctx.seg(8, 4);                                                               // a crown of hands thrusting up and fanning out
        for (int i = 0; i < n; i++) {
            float ang = Mth.TWO_PI * i / n + h(inst, i, 1) * 0.3f, k = out(ramp(t, 0.02f + 0.1f * h(inst, i, 2), 0.7f));
            Vector3f p = new Vector3f(Mth.cos(ang) * 1.3f * u * k, 0.1f + 1.5f * u * k, Mth.sin(ang) * 1.3f * u * k).add(c);
            buf.billboard(ctx, HAND, VfxBlend.ADD, p, 1.2f * u * (0.5f + 0.5f * k), Mth.cos(ang) * 0.35f, a(hot(col, 0.3f), 0.85f * (1f - ramp(t, 0.55f, 1f))));
        }
        int m = ctx.seg(10, 5);                                                              // bone splinters along arcs
        for (int i = 0; i < m; i++) {
            float ang = h(inst, i, 3) * Mth.TWO_PI, sp = (1.0f + 1.8f * h(inst, i, 4)) * u;
            if (dirHint.lengthSquared() > 0.5f) ang = ang * 0.6f + (float) Math.atan2(dirHint.z, dirHint.x) * 0.4f;
            float f = ramp(t, 0.03f, 1f);
            Vector3f p = new Vector3f(Mth.cos(ang) * sp * out(f), (0.3f + 2.0f * Mth.sin(Mth.PI * f * 0.9f) * (0.5f + h(inst, i, 5))) * u, Mth.sin(ang) * sp * out(f)).add(c);
            buf.billboard(ctx, SHARD, VfxBlend.ADD, p, 0.4f * u, ang + age * 0.3f, a(col, 0.9f * (1f - ramp(f, 0.6f, 1f))));
        }
        for (int i = 0; i < 6; i++) {                                                        // threads snapping outward like a star
            float ang = i * Mth.TWO_PI / 6 + age * 0.03f, k = out(ramp(t, 0f, 0.5f));
            Vector3f p0 = new Vector3f(mid), p1 = new Vector3f(mid).add(Mth.cos(ang) * 3f * u * k, 0.4f * u * k, Mth.sin(ang) * 3f * u * k);
            buf.beam(ctx, THREAD, VfxBlend.ADD, p0, p1, 0.2f * u, 0.1f * u, 1, 0f, a(WHITE, 0.8f * (1f - ramp(t, 0.3f, 0.8f))), a(PALE, 0.2f * (1f - ramp(t, 0.3f, 0.8f))));
        }
    }
}
