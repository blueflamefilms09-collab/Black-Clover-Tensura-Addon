package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * Sound Magic (1.93.0 rebuild): the look of a sound you can see, cyan and white on the dark steel of the speakers: shock fronts, waveform ribbons, speaker cones, equalizer bars
 * and rings that roll outward. Textures: tools/gen_sound_rebuild_textures.py (particle/sound_*.png). The three shapes of the shape protocol:
 * <ul>
 *   <li>SOUND_FX1, CAST / PROJECTILE, "Sonic Bolt". A speaker cone and a ring of waves contract on 'from' (charge, 22 percent of the life); then a shock-front head runs 'from' to
 *       'to' (accelerating) with a waveform ribbon behind it, four wave rings left along its path that shrink as they age, and a white core; at the end a shock ring and a
 *       cone flash open at 'to'. power = size (cast shapes are drawn 3 times larger by the manager: 1 usual), duration = flight ticks (default 14).</li>
 *   <li>SOUND_FX2, ZONE / FIELD, "Resonance Field". A turning ripple ring on the ground to the radius, a pulsing cone in the middle, wave rings rolling out every 20 ticks, a row of
 *       equalizer bars standing on the rim (each bar dancing to its own beat), cyan glints rising. from = centre on the ground, power = radius (zone shapes are drawn 1.5 times
 *       larger), duration = life (default 80; in 8, out 12).</li>
 *   <li>SOUND_FX3, IMPACT / SIGNATURE, "Sonic Boom". A white flash, three shock rings (flat on the ground, upright and tilted) expanding at different speeds, a pale ring
 *       standing for the bend in the air, a cone disc blooming, sparks flung out and a cyan afterglow. from = centre, to = optional direction hint, power = scale,
 *       duration = life (default 26).</li>
 * </ul>
 * The colour tints the light (default cyan 0xFF70C8F0). Budget: well under the 400 vertices of an activation.
 */
public class SoundFxLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation WAVE = t("sound_wave"), CONE = t("sound_cone"), SHOCK = t("sound_shock"), BEAM = t("sound_beam_v"), BARS = t("sound_bars"),
            RIPPLE = t("sound_ripple"), GLOW = VfxTextures.GLOW, SPARK = VfxTextures.SPARK;

    private static final int WHITE = 0xFFFFFFFF;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.SOUND_FX1, VfxShape.SOUND_FX2, VfxShape.SOUND_FX3); }

    @Override
    public int defaultDuration(VfxShape s) {
        return switch (s) {
            case SOUND_FX1 -> 14;
            case SOUND_FX2 -> 80;
            default -> 26;
        };
    }

    @Override public int defaultColor(VfxShape s) { return 0xFF70C8F0; }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case SOUND_FX1 -> cast(inst, ctx, buf);
            case SOUND_FX2 -> zone(inst, ctx, buf);
            case SOUND_FX3 -> boom(inst, ctx, buf);
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

    // ------------------------------------------------------------------ FX1: Sonic Bolt
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
            buf.plane(CONE, VfxBlend.ADD, face.spin(age * 0.3f), (0.9f - 0.4f * tc) * u, a(col, 0.8f * cf));
            buf.plane(WAVE, VfxBlend.ADD, face.lift(0.04f).spin(-age * 0.2f), (1.4f - 0.7f * tc) * u, a(hot(col, 0.4f), 0.8f * cf));
            glow(buf, ctx, from, 0.8f * u, col, 0.6f * cf);
        }
        if (still) return;

        float s = ramp(age, tl, ta), se = 0.4f * s + 0.6f * s * s;
        Vector3f head = new Vector3f(dir).mul(dist * se).add(from);
        float fly = s > 0f && s < 1f ? 1f : 0f;
        if (fly > 0f) {
            float trail = Math.min(dist * se, 3.0f * u + 0.6f);
            Vector3f tail = new Vector3f(head).sub(new Vector3f(dir).mul(trail));
            buf.beam(ctx, BEAM, VfxBlend.ADD, head, tail, 0.9f * u, 0.5f * u, 1, -age * 0.1f, a(hot(col, 0.5f), 0.95f), a(col, 0f));
            VfxPose face = VfxPose.facing(new Vector3f(head), dir);
            buf.plane(SHOCK, VfxBlend.ADD, face.spin(age * 0.4f), 0.75f * u, a(hot(col, 0.5f), 0.95f));
            for (int k = 1; k <= 4; k++) {                                                  // wave rings left behind, shrinking as they age
                float f = k * 0.2f;
                Vector3f p = new Vector3f(head).sub(new Vector3f(dir).mul(trail * f));
                buf.plane(WAVE, VfxBlend.ADD, VfxPose.facing(p, dir).spin(age * 0.2f * k), (0.75f - 0.13f * k) * u, a(col, 0.7f * (1f - f)));
            }
            glow(buf, ctx, head, 0.9f * u, col, 0.75f);
            glow(buf, ctx, head, 0.35f * u, WHITE, 0.95f);
        }
        float ti = ramp(age, ta - 1f, d);
        if (ti > 0f) {
            float k = out(ti);
            VfxPose face = VfxPose.facing(new Vector3f(to), new Vector3f(dir).negate());
            buf.plane(SHOCK, VfxBlend.ADD, face.spin(age * 0.3f), 1.5f * u * k, a(hot(col, 0.4f), 0.9f * (1f - ti)));
            buf.plane(CONE, VfxBlend.ADD, face.lift(0.05f), 0.9f * u * (0.4f + 0.6f * k), a(col, 0.8f * (1f - ti)));
            glow(buf, ctx, to, 1.3f * u * (1f - 0.4f * ti), col, 0.7f * (1f - ti));
            for (int i = 0; i < 5; i++) {
                float ang = h(inst, i, 1) * Mth.TWO_PI;
                Vector3f p = new Vector3f(to).add(new Vector3f(ctx.camRight).mul(Mth.cos(ang) * k * 1.2f * u)).add(new Vector3f(ctx.camUp).mul(Mth.sin(ang) * k * 1.2f * u));
                buf.billboard(ctx, SPARK, VfxBlend.ADD, p, 0.3f * u, ang, a(i % 2 == 0 ? col : WHITE, 0.9f * (1f - ti)));
            }
        }
    }

    // ------------------------------------------------------------------ FX2: Resonance Field
    private void zone(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float R = Math.max(1.2f, inst.power / 1.5f);
        float fade = life(inst, age, 8f, 12f), open = VfxAnim.easeOutBack(Mth.clamp(age / 10f, 0f, 1f));
        if (fade <= 0.01f) return;
        Vector3f c = ctx.rel(inst.from(ctx));
        int col = inst.color | 0xFF000000;
        float r = R * open;
        ground(buf, GLOW, c, 0.04f, r * 1.05f, 0f, a(col, 0.2f * fade));
        ground(buf, RIPPLE, c, 0.05f, r * 1.04f, age * 0.015f, a(col, 0.9f * fade));
        ground(buf, RIPPLE, c, 0.06f, r * 0.66f, -age * 0.025f, a(hot(col, 0.4f), 0.55f * fade));
        float beat = 0.5f + 0.5f * Mth.sin(age * 0.63f);
        ground(buf, CONE, c, 0.07f, r * (0.28f + 0.05f * beat), age * 0.02f, a(col, (0.55f + 0.3f * beat) * fade));
        for (int k = 0; k < 2; k++) {                                                       // rings rolling out every 20 ticks
            float ph = ((age + k * 10f) % 20f) / 20f;
            ground(buf, WAVE, c, 0.09f, r * (0.2f + 0.85f * out(ph)), age * 0.03f * (k == 0 ? 1 : -1), a(col, 0.8f * (1f - ph) * fade));
        }
        int n = ctx.seg(10, 5);                                                             // equalizer bars standing on the rim, dancing
        for (int i = 0; i < n; i++) {
            float ang = Mth.TWO_PI * i / n + age * 0.004f, hgt = (0.6f + 1.1f * (0.5f + 0.5f * Mth.sin(age * 0.45f + i * 1.7f))) * Math.min(1.5f, 0.6f + R * 0.1f) * open;
            Vector3f foot = new Vector3f(Mth.cos(ang) * r * 0.96f, 0.05f, Mth.sin(ang) * r * 0.96f).add(c);
            Vector3f nrm = new Vector3f(-Mth.cos(ang), 0, -Mth.sin(ang));
            VfxPose face = VfxPose.facing(new Vector3f(foot).add(0, hgt * 0.5f, 0), nrm);
            buf.plane(BARS, VfxBlend.ADD, face, hgt * 0.5f + 0.1f, a(i % 3 == 0 ? hot(col, 0.5f) : col, 0.75f * fade));
        }
        int m = ctx.seg(10, 4);
        for (int i = 0; i < m; i++) {
            float ph = (age * 0.016f + h(inst, i, 3)) % 1f, ang = h(inst, i, 4) * Mth.TWO_PI, rr = r * (0.15f + 0.8f * h(inst, i, 5));
            Vector3f p = new Vector3f(Mth.cos(ang) * rr, 0.1f + ph * 2.2f, Mth.sin(ang) * rr).add(c);
            buf.billboard(ctx, SPARK, VfxBlend.ADD, p, 0.2f + 0.08f * h(inst, i, 6), age * 0.1f + i, a(i % 3 == 0 ? WHITE : col, 0.9f * Mth.sin(ph * Mth.PI) * fade));
        }
        glow(buf, ctx, new Vector3f(c).add(0, 0.6f, 0), r * 0.9f, col, 0.18f * fade * (0.6f + 0.4f * beat));
    }

    // ------------------------------------------------------------------ FX3: Sonic Boom
    private void boom(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), d = Math.max(8f, inst.duration), t = Mth.clamp(age / d, 0f, 1f);
        float u = Mth.clamp(inst.power, 0.4f, 6f) / 3f;
        Vector3f c = ctx.rel(inst.from(ctx));
        Vector3f dirHint = ctx.rel(inst.to(ctx)).sub(c);
        int col = inst.color | 0xFF000000;
        float flash = 1f - ramp(t, 0f, 0.28f);
        Vector3f mid = new Vector3f(c).add(0, 0.9f * u, 0);
        glow(buf, ctx, mid, (1.4f + 1.6f * out(t)) * u, col, 0.55f * (1f - t));
        glow(buf, ctx, mid, 1.2f * u * flash, WHITE, 0.95f * flash);
        // a pale ring that stands for the bend in the air
        float kn = out(ramp(t, 0f, 0.75f));
        if (kn > 0.02f) buf.plane(SHOCK, VfxBlend.ADD, VfxPose.facing(new Vector3f(mid), new Vector3f(ctx.camRight).cross(ctx.camUp)).spin(age * 0.05f), 2.6f * u * kn, a(WHITE, 0.4f * (1f - ramp(t, 0.4f, 1f))));
        // three shock rings: flat, upright, tilted
        ground(buf, SHOCK, c, 0.06f, 3.0f * u * out(ramp(t, 0f, 0.8f)), age * 0.1f, a(hot(col, 0.4f), 0.95f * (1f - ramp(t, 0.35f, 1f))));
        buf.plane(SHOCK, VfxBlend.ADD, VfxPose.facing(new Vector3f(mid), new Vector3f(ctx.camRight).cross(ctx.camUp)).spin(-age * 0.08f), 2.1f * u * out(ramp(t, 0.05f, 0.9f)), a(col, 0.85f * (1f - ramp(t, 0.3f, 1f))));
        Vector3f tilt = new Vector3f(0.5f, 1f, 0.3f);
        buf.plane(WAVE, VfxBlend.ADD, VfxPose.facing(new Vector3f(mid), tilt).spin(age * 0.12f), 1.7f * u * out(ramp(t, 0.1f, 0.95f)), a(hot(col, 0.3f), 0.8f * (1f - ramp(t, 0.4f, 1f))));
        buf.plane(CONE, VfxBlend.ADD, VfxPose.facing(new Vector3f(mid), new Vector3f(ctx.camRight).cross(ctx.camUp)), 1.0f * u * out(ramp(t, 0f, 0.5f)), a(col, 0.7f * (1f - ramp(t, 0.25f, 0.8f))));
        int n = ctx.seg(14, 6);
        for (int i = 0; i < n; i++) {
            float ang = h(inst, i, 1) * Mth.TWO_PI, el = (h(inst, i, 2) - 0.3f) * 1.3f, sp = (1.0f + 1.8f * h(inst, i, 3)) * u;
            if (dirHint.lengthSquared() > 0.5f) ang = ang * 0.6f + (float) Math.atan2(dirHint.z, dirHint.x) * 0.4f;
            float f = out(ramp(t, 0.02f, 0.9f));
            Vector3f p = new Vector3f(Mth.cos(ang) * Mth.cos(el), Mth.sin(el) + 0.2f, Mth.sin(ang) * Mth.cos(el)).mul(sp * f).add(mid);
            buf.billboard(ctx, SPARK, VfxBlend.ADD, p, 0.3f * u * (1f - 0.5f * f), ang + age * 0.2f, a(i % 3 == 0 ? WHITE : col, 0.9f * (1f - ramp(t, 0.5f, 1f))));
        }
    }
}
