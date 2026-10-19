package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 1.93.0 Mirror Magic (Gauche Adlai's reflecting glass), the own effects, to the bar of the Time Magic layers: big textured quads layered back to front, moved from the
 * age alone, in silver-white and the grimoire's violet (0xB89CFF) with the light split into colour where it passes through glass. The older MirrorLayer (MIRROR_RAY,
 * MIRROR_FRAME, MIRROR_ARRAY ...) is untouched and still draws its own shapes.
 * <ul>
 *   <li>MIRROR_FX1, CAST (a reflected ray): 'from' = the mirror's heart / hand, 'to' = target, power = width (0.6 to 3). A halo ring and a pane of glass open at 'from'
 *       over the first quarter; the ray grows to the target as a white core with a red and a blue beam either side (the light split by the glass), prism fans spread
 *       where it leaves the mirror; it lands in a glint, a ripple facing back along the ray and shards. Default 14 ticks.</li>
 *   <li>MIRROR_FX2, FIELD (a mirror hall): 'from' = centre on the ground, power = radius. A sigil of mirrored triangles and an eight-point star turns on the floor under a
 *       halo ring; upright panes ring the rim, each with a glint running over it; a ripple runs out every 30 ticks and light shafts stand between the panes.
 *       In over 8 ticks, out over the last 12. Default 80.</li>
 *   <li>MIRROR_FX3, BURST (a shattering mirror): 'from' = centre, 'to' = direction hint, power = scale. A flash and a long glint, a prism star of colour streaks, two
 *       ripples, a halo ring, and glass shards thrown out that spin and fade. Default 28.</li>
 * </ul>
 * Budget: FX1 about 140 vertices, FX2 about 220, FX3 about 230 (the preview reports it; the limit is 400).
 */
public class MirrorFxLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation PANE = t("mirror_reflex_pane"), RING = t("mirror_reflex_ring"), BEAM = t("mirror_reflex_beam"), SHARD = t("mirror_reflex_shard"),
            GLINT = t("mirror_reflex_glint"), SIGIL = t("mirror_reflex_sigil"), PRISM = t("mirror_reflex_prism"), RIPPLE = t("mirror_reflex_ripple");

    private static final int WHITE = 0xFFFFFFFF, RED = 0xFFFF8E9A, BLUE = 0xFF8EB4FF;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.MIRROR_FX1, VfxShape.MIRROR_FX2, VfxShape.MIRROR_FX3); }

    @Override
    public int defaultDuration(VfxShape s) { return switch (s) { case MIRROR_FX1 -> 14; case MIRROR_FX2 -> 80; default -> 28; }; }

    @Override public int defaultColor(VfxShape s) { return 0xFFB89CFF; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.MIRROR_FX3) VfxShake.add(inst.payload.from(), 0.4f * Math.min(2f, inst.power), 6);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case MIRROR_FX1 -> ray(inst, ctx, buf);
            case MIRROR_FX2 -> hall(inst, ctx, buf);
            case MIRROR_FX3 -> shatter(inst, ctx, buf);
            default -> { }
        }
    }

    private static float life(VfxInstance inst, float age, float in, float out) {
        float a = in <= 0 ? 1 : Mth.clamp(age / in, 0, 1);
        float b = out <= 0 ? 1 : Mth.clamp((inst.duration - age) / out, 0, 1);
        return Math.min(a, b);
    }

    private static int col(int argb, float a) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(a, 0f, 1f)); }

    // ------------------------------------------------------------------ CAST
    private void ray(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1f, inst.duration), prog = Mth.clamp(age / dur, 0, 1);
        float size = Math.max(0.6f, inst.power);
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(from);
        float len = dir.length();
        if (len < 1.0e-3f) dir.set(0, 0, 1); else dir.div(len);
        int c = inst.color, hot = VfxVertexBuffer.whiten(c, 0.7f);
        float open = VfxAnim.easeOutBack(Mth.clamp(age / (dur * 0.25f), 0, 1)) * (1f - VfxAnim.easeInCubic(Mth.clamp((prog - 0.6f) / 0.4f, 0, 1)));

        // the halo ring and the pane of glass the ray comes out of
        VfxPose gate = VfxPose.facing(from, dir);
        if (open > 0.02f) {
            VfxBloom.glow(ctx, buf, from, 0.35f * size * open, c, 0.6f);
            buf.plane(RING, VfxBlend.ADD, gate.spin(age * 0.18f), 0.55f * size * open, col(c, 0.9f * open));
            buf.plane(PANE, VfxBlend.ADD, gate.lift(0.02f), 0.38f * size * open, col(hot, 0.5f * open));
        }
        if (prog < 0.15f) return;
        float grow = VfxAnim.easeOutCubic(Mth.clamp((prog - 0.15f) / 0.3f, 0, 1));
        float fade = 1f - VfxAnim.easeInCubic(Mth.clamp((prog - 0.7f) / 0.3f, 0, 1));
        Vector3f end = new Vector3f(from).lerp(to, grow);
        Vector3f side = new Vector3f(dir).cross(ctx.camUp);
        if (side.lengthSquared() < 1.0e-4f) side.set(ctx.camRight);
        side.normalize();
        float split = 0.16f * size * (0.4f + 0.6f * grow);
        // the light split by the glass: a red and a blue beam either side of the white core
        buf.beam(ctx, BEAM, VfxBlend.ADD, new Vector3f(from).add(new Vector3f(side).mul(split)), new Vector3f(end).add(new Vector3f(side).mul(split * 2.4f)), 0.35f * size, 0.3f * size, 1, 0, col(RED, 0.55f * fade), col(RED, 0.4f * fade));
        buf.beam(ctx, BEAM, VfxBlend.ADD, new Vector3f(from).sub(new Vector3f(side).mul(split)), new Vector3f(end).sub(new Vector3f(side).mul(split * 2.4f)), 0.35f * size, 0.3f * size, 1, 0, col(BLUE, 0.55f * fade), col(BLUE, 0.4f * fade));
        buf.beam(ctx, BEAM, VfxBlend.ADD, from, end, 0.55f * size, 0.45f * size, 1, 0, col(hot, 0.9f * fade), col(WHITE, 0.9f * fade));
        buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, from, end, 0.14f * size, 0.1f * size, 1, 0, col(WHITE, 0.95f * fade), col(WHITE, 0.95f * fade));
        // prism fans where the ray leaves the mirror
        for (int k = -1; k <= 1; k += 2) {
            Vector3f tip = new Vector3f(from).add(new Vector3f(dir).mul(1.3f * size)).add(new Vector3f(side).mul(k * 0.7f * size));
            buf.beam(ctx, PRISM, VfxBlend.ALPHA, from, tip, 0.3f * size, 0.18f * size, 1, 0, col(WHITE, 0.55f * fade * grow), col(WHITE, 0.0f));
        }
        // the landing
        if (grow > 0.95f) {
            float p = Mth.clamp((prog - 0.45f) / 0.55f, 0, 1);
            VfxBloom.glow(ctx, buf, to, 0.5f * size * (1f - 0.4f * p), c, 0.9f * (1f - p * 0.7f));
            buf.billboard(ctx, GLINT, VfxBlend.ADD, to, 1.4f * size * (1f - 0.5f * p), age * 0.06f, col(WHITE, 0.95f * (1f - p)));
            buf.plane(RIPPLE, VfxBlend.ADD, VfxPose.facing(to, dir), 1.2f * size * VfxAnim.easeOutCubic(p), col(hot, 0.8f * (1f - p)));
            RandomSource r = inst.random();
            for (int i = 0; i < 4; i++) {
                float a = Mth.TWO_PI * i / 4 + r.nextFloat(), sp = (0.6f + r.nextFloat() * 0.6f) * size * VfxAnim.easeOutCubic(p);
                buf.billboard(ctx, SHARD, VfxBlend.ADD, new Vector3f(to).add(Mth.cos(a) * sp, Mth.sin(a) * sp, 0), 0.25f * size, a + age * 0.3f, col(hot, 0.8f * (1f - p)));
            }
        }
    }

    // ------------------------------------------------------------------ FIELD (a mirror hall)
    private void hall(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 8, 12), grow = VfxAnim.easeOutCubic(Mth.clamp(age / 10f, 0, 1));
        float R = Math.max(1f, inst.power) * grow;
        if (R <= 0.05f || fade <= 0.01f) return;
        Vector3f c0 = ctx.rel(inst.from(ctx));
        int c = inst.color, hot = VfxVertexBuffer.whiten(c, 0.65f);
        RandomSource r = inst.random();

        VfxPose floor = VfxPose.ground(new Vector3f(c0).add(0, 0.05f, 0));
        VfxBloom.planeGlow(buf, floor, R * 0.9f, c, 0.4f * fade);
        buf.plane(SIGIL, VfxBlend.ADD, floor.spin(-age * 0.012f), R, col(c, 0.85f * fade));
        buf.plane(RING, VfxBlend.ADD, floor.lift(0.02f).spin(age * 0.03f), R * 0.98f, col(hot, 0.8f * fade));
        float wave = (age % 30f) / 30f;
        buf.plane(RIPPLE, VfxBlend.ADD, floor.lift(0.04f), R * (0.15f + 0.85f * wave), col(WHITE, 0.6f * fade * (1f - wave)));
        // upright panes round the rim, each facing the centre, with a glint running over it
        int n = ctx.seg(8, 4);
        float h = Math.min(3.4f, 1.4f + 0.3f * R);
        for (int i = 0; i < n; i++) {
            float a = Mth.TWO_PI * i / n + age * 0.004f;
            Vector3f base = new Vector3f(Mth.cos(a) * R * 0.94f, 0f, Mth.sin(a) * R * 0.94f).add(c0);
            Vector3f inward = new Vector3f(-Mth.cos(a), 0, -Mth.sin(a));
            float grow2 = VfxAnim.easeOutCubic(Mth.clamp((age - i * 0.8f) / 10f, 0, 1));
            VfxPose pane = new VfxPose(new Vector3f(base).add(0, h * 0.5f + 0.05f, 0), new Vector3f(-Mth.sin(a), 0, Mth.cos(a)), new Vector3f(0, 1, 0), inward);
            float hw = Math.min(1.1f, 0.5f + 0.1f * R) * grow2;
            buf.quad(PANE, VfxBlend.ADD, pane.point(-hw, -h * 0.5f * grow2), pane.point(hw, -h * 0.5f * grow2), pane.point(hw, h * 0.5f * grow2), pane.point(-hw, h * 0.5f * grow2),
                    0, 0, 1, 1, col(c, 0.42f * fade), col(hot, 0.42f * fade));
            float ph = (age * 0.03f + i * 0.17f) % 1f;
            buf.billboard(ctx, GLINT, VfxBlend.ADD, pane.point((ph - 0.5f) * hw, (0.5f - ph) * h * 0.8f), 0.45f * Mth.sin(ph * Mth.PI) * grow2, age * 0.04f, col(WHITE, 0.9f * fade));
        }
        // motes of light drifting up between the panes
        int m = ctx.seg(8, 4);
        for (int i = 0; i < m; i++) {
            float ph = (age * 0.02f + r.nextFloat()) % 1f, a = r.nextFloat() * Mth.TWO_PI, d = Mth.sqrt(r.nextFloat()) * R * 0.85f;
            buf.billboard(ctx, GLINT, VfxBlend.ADD, new Vector3f(Mth.cos(a) * d, 0.2f + ph * h, Mth.sin(a) * d).add(c0), 0.14f + 0.08f * Mth.sin(ph * Mth.PI), age * 0.05f + i, col(i % 2 == 0 ? c : WHITE, 0.8f * fade * Mth.sin(ph * Mth.PI)));
        }
    }

    // ------------------------------------------------------------------ BURST (a shattering mirror)
    private void shatter(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1f, inst.duration), p = Mth.clamp(age / dur, 0, 1);
        float size = Math.max(0.5f, inst.power);
        Vector3f c0 = ctx.rel(inst.from(ctx));
        Vector3f dir = new Vector3f(ctx.rel(inst.to(ctx))).sub(c0);
        if (dir.lengthSquared() < 1.0e-3f) dir.set(0, 0, 1);
        dir.normalize();
        int c = inst.color, hot = VfxVertexBuffer.whiten(c, 0.7f);
        float out = 1f - VfxAnim.easeInCubic(p);
        RandomSource r = inst.random();

        float flash = Mth.clamp(1f - age / 7f, 0, 1);
        VfxBloom.glow(ctx, buf, c0, 0.7f * size * (0.5f + flash), c, 0.4f + 0.7f * flash);
        buf.billboard(ctx, GLINT, VfxBlend.ADD, c0, 2.4f * size * (0.5f + 0.5f * flash), 0.2f, col(WHITE, flash));
        // the prism star: colour streaks radiating from the break
        int rays = ctx.seg(8, 4);
        int[] tints = {RED, 0xFFFFD28E, 0xFF9EFFB0, BLUE, 0xFFC89EFF};
        float spread = VfxAnim.easeOutCubic(Mth.clamp(p * 2.2f, 0, 1));
        for (int i = 0; i < rays; i++) {
            float a = Mth.TWO_PI * i / rays + r.nextFloat() * 0.3f;
            Vector3f tip = new Vector3f(c0).add(ctx.camRight.x * Mth.cos(a) * 1.8f * size * spread + ctx.camUp.x * Mth.sin(a) * 1.8f * size * spread,
                    ctx.camRight.y * Mth.cos(a) * 1.8f * size * spread + ctx.camUp.y * Mth.sin(a) * 1.8f * size * spread,
                    ctx.camRight.z * Mth.cos(a) * 1.8f * size * spread + ctx.camUp.z * Mth.sin(a) * 1.8f * size * spread);
            buf.beam(ctx, PRISM, VfxBlend.ALPHA, c0, tip, 0.34f * size, 0.12f * size, 1, 0, col(tints[i % tints.length], 0.7f * out), col(tints[i % tints.length], 0f));
        }
        for (int k = 0; k < 2; k++) {
            float q = Mth.clamp((p - 0.07f * k) / (0.9f - 0.07f * k), 0, 1), s = VfxAnim.easeOutCubic(q);
            buf.plane(RIPPLE, VfxBlend.ADD, VfxPose.facing(c0, dir).lift(0.02f * k), size * (0.3f + 2.2f * s), col(k == 0 ? hot : c, 0.85f * (1f - q)));
        }
        buf.plane(RING, VfxBlend.ADD, VfxPose.facing(c0, dir).spin(age * 0.2f), size * (0.5f + 1.1f * spread), col(hot, 0.7f * out));
        // glass shards thrown out
        int n = ctx.seg(10, 5);
        for (int i = 0; i < n; i++) {
            float a = Mth.TWO_PI * i / n + r.nextFloat() * 0.6f, up = (r.nextFloat() - 0.3f) * 1.2f * size, sp = (0.9f + r.nextFloat() * 1.3f) * size;
            float e = VfxAnim.easeOutCubic(Mth.clamp(p * 1.3f, 0, 1));
            Vector3f q = new Vector3f(Mth.cos(a) * sp * e, up * e - 0.7f * size * p * p, Mth.sin(a) * sp * e).add(c0);
            buf.billboard(ctx, SHARD, VfxBlend.ADD, q, 0.34f * size * (1f - 0.3f * p), a + age * 0.3f, col(i % 2 == 0 ? hot : WHITE, 0.9f * out));
        }
    }
}
