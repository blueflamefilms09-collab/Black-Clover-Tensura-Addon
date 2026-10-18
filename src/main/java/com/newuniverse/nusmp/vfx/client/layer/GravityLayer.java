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
 * 1.93.0 Gravity Magic (Dante Zogratis's weight), the own effects, built to the bar of the Time Magic layers: big layered quads back to front (the black
 * pool and the floor first, alpha blended; then the rims, lanes and sparks, additive), moved by the age alone. The palette is a violet (0xB45AFF) against black.
 * <ul>
 *   <li>GRAVITY_FX1, CAST: 'from' = hand, 'to' = target, power = size (0.6 to 3). A pressure ring squeezes shut at the hand over the first quarter; then a
 *       singularity flies to the target: a black disc with a bright rim and a lensing ring, a lane of compressing chevrons pointing into its head and a
 *       dark trail; it ends in a crushing flash with a ring of cracks at 'to'. Default 16 ticks.</li>
 *   <li>GRAVITY_FX2, FIELD (a well): 'from' = centre on the ground, power = radius. A black pool and a sigil of down arrows on the floor, three pressure rings
 *       that crowd in towards the centre one after the other, fractures, pressure streaks falling from above and motes dragged inwards and down. In over
 *       8 ticks, out over the last 12. Default 80.</li>
 *   <li>GRAVITY_FX3, BURST (a crush): 'from' = centre, 'to' = direction hint, power = scale. A flash, a shock ring and a second warped ring running out, cracks
 *       spreading over the ground, a ring of dust, and rocks thrown up that fall back. Default 28.</li>
 * </ul>
 * Budget: FX1 about 150 vertices, FX2 about 300, FX3 about 260 (the preview reports it; the limit is 400).
 */
public class GravityLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation RING = t("gravity_ring"), DISC = t("gravity_disc"), CHEVRON = t("gravity_chevron"), CRACK = t("gravity_crack"),
            MOTE = t("gravity_mote"), SIGIL = t("gravity_sigil"), SHARD = t("gravity_shard"), STREAK = t("gravity_streak"), LENS = t("gravity_lens");

    private static final int WHITE = 0xFFFFFFFF, BLACK = 0xFFFFFFFF;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.GRAVITY_FX1, VfxShape.GRAVITY_FX2, VfxShape.GRAVITY_FX3); }

    @Override
    public int defaultDuration(VfxShape s) { return switch (s) { case GRAVITY_FX1 -> 16; case GRAVITY_FX2 -> 80; default -> 28; }; }

    @Override public int defaultColor(VfxShape s) { return 0xFFB45AFF; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.GRAVITY_FX3) VfxShake.add(inst.payload.from(), 1.0f * Math.min(2f, inst.power), 10);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case GRAVITY_FX1 -> cast(inst, ctx, buf);
            case GRAVITY_FX2 -> well(inst, ctx, buf);
            case GRAVITY_FX3 -> crush(inst, ctx, buf);
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
    private void cast(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1f, inst.duration), prog = Mth.clamp(age / dur, 0, 1);
        float size = Math.max(0.6f, inst.power);
        Vector3f from = ctx.rel(inst.from(ctx)), to = ctx.rel(inst.to(ctx));
        Vector3f dir = new Vector3f(to).sub(from);
        float len = dir.length();
        if (len < 1.0e-3f) dir.set(0, 0, 1); else dir.div(len);
        int c = inst.color, hot = VfxVertexBuffer.whiten(c, 0.55f);
        float charge = Mth.clamp(age / (dur * 0.25f), 0, 1);

        // the pressure ring that squeezes shut at the hand
        VfxPose gate = VfxPose.facing(from, dir);
        float squeeze = 1.3f - 0.9f * VfxAnim.easeOutCubic(charge);
        float ringFade = 1f - VfxAnim.easeInCubic(Mth.clamp((prog - 0.4f) / 0.6f, 0, 1));
        buf.plane(RING, VfxBlend.ADD, gate.spin(-age * 0.3f), 0.55f * size * squeeze, col(c, 0.9f * ringFade));
        buf.plane(RING, VfxBlend.ADD, gate.lift(0.02f).spin(age * 0.2f), 0.35f * size * squeeze, col(hot, 0.7f * ringFade));
        VfxBloom.glow(ctx, buf, from, 0.3f * size * charge, c, 0.5f * ringFade);
        if (prog < 0.2f) return;

        float e = VfxAnim.easeOutCubic(Mth.clamp((prog - 0.2f) / 0.8f, 0, 1));
        Vector3f head = new Vector3f(from).lerp(to, e), tail = new Vector3f(from).lerp(to, Math.max(0f, e - 0.5f));
        float fade = 1f - VfxAnim.easeInCubic(Mth.clamp((prog - 0.78f) / 0.22f, 0, 1));
        // the dark trail, then the lane of chevrons pointing into the head (beam starts at the head: its texture bottom, the chevron tips, is there)
        buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ALPHA, tail, head, 0.7f * size, 0.5f * size, 1, 0, col(0xFF0A0414, 0.7f * fade), col(0xFF0A0414, 0.7f * fade));
        buf.beam(ctx, CHEVRON, VfxBlend.ADD, head, tail, 0.9f * size, 0.9f * size, 1, 0, col(hot, 0.9f * fade), col(c, 0.2f * fade));
        // the singularity: black disc (alpha) with its rim, and the lens rings (additive)
        buf.billboard(ctx, DISC, VfxBlend.ALPHA, head, 0.95f * size, 0, col(WHITE, fade));
        buf.plane(LENS, VfxBlend.ADD, VfxPose.facing(head, dir).spin(age * 0.2f), 0.75f * size, col(c, 0.7f * fade));
        buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ADD, head, 1.8f * size, 0, col(c, 0.25f * fade));
        // motes dragged into the head
        RandomSource r = inst.random();
        Vector3f side = new Vector3f(dir).cross(ctx.camUp);
        if (side.lengthSquared() < 1.0e-4f) side.set(ctx.camRight);
        side.normalize();
        for (int i = 0; i < 5; i++) {
            float ph = (age * 0.09f + r.nextFloat()) % 1f;
            Vector3f off = new Vector3f(side).mul((r.nextFloat() - 0.5f) * 1.6f * size).add(0, (r.nextFloat() - 0.5f) * 1.2f * size, 0);
            Vector3f p = new Vector3f(head).add(off.mul(1f - ph)).sub(new Vector3f(dir).mul(0.3f * size * (1f - ph)));
            buf.billboard(ctx, MOTE, VfxBlend.ALPHA, p, 0.22f * size, 0, col(WHITE, fade * Mth.sin(ph * Mth.PI)));
        }
        // the landing
        if (prog > 0.78f) {
            float p = (prog - 0.78f) / 0.22f;
            VfxBloom.glow(ctx, buf, to, 0.7f * size * (1f - 0.4f * p), c, 0.9f * (1f - p));
            buf.plane(RING, VfxBlend.ADD, VfxPose.facing(to, dir).spin(age * 0.2f), 1.4f * size * (1.2f - VfxAnim.easeOutCubic(p)), col(c, 0.9f * (1f - p)));
            buf.plane(CRACK, VfxBlend.ADD, VfxPose.ground(new Vector3f(to).add(0, 0.04f, 0)), 1.4f * size * VfxAnim.easeOutCubic(p), col(c, 0.8f * (1f - p)));
        }
    }

    // ------------------------------------------------------------------ FIELD (a well)
    private void well(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 8, 12), grow = VfxAnim.easeOutCubic(Mth.clamp(age / 10f, 0, 1));
        float R = Math.max(1f, inst.power) * grow;
        if (R <= 0.05f || fade <= 0.01f) return;
        Vector3f c0 = ctx.rel(inst.from(ctx));
        int c = inst.color, hot = VfxVertexBuffer.whiten(c, 0.5f);
        RandomSource r = inst.random();

        VfxPose floor = VfxPose.ground(new Vector3f(c0).add(0, 0.04f, 0));
        buf.plane(DISC, VfxBlend.ALPHA, floor, R * 1.02f, col(WHITE, 0.6f * fade));                            // the black pool
        buf.plane(SIGIL, VfxBlend.ADD, floor.lift(0.02f).spin(age * 0.01f), R, col(c, 0.75f * fade));
        buf.plane(CRACK, VfxBlend.ADD, floor.lift(0.03f).spin(r.nextFloat() * Mth.TWO_PI), R * 0.95f, col(c, 0.55f * fade));
        for (int k = 0; k < 3; k++) {                                                                           // rings crowding inwards, one after the other
            float ph = ((age * 0.025f) + k / 3f) % 1f;
            buf.plane(RING, VfxBlend.ADD, floor.lift(0.05f + 0.01f * k).spin(age * (k % 2 == 0 ? 0.02f : -0.03f)), R * (1.05f - 0.9f * ph), col(k == 0 ? hot : c, 0.85f * fade * Mth.sin(ph * Mth.PI)));
        }
        VfxBloom.planeGlow(buf, floor.lift(0.01f), R * 0.5f, c, 0.35f * fade);
        // pressure streaks falling from above
        int n = ctx.seg(10, 4);
        float h = Math.min(5f, 2.5f + 0.4f * R);
        for (int i = 0; i < n; i++) {
            float ph = (age * 0.05f + r.nextFloat()) % 1f, a = r.nextFloat() * Mth.TWO_PI, d = Mth.sqrt(r.nextFloat()) * R * 0.9f;
            float top = h * (1f - ph);
            Vector3f foot = new Vector3f(Mth.cos(a) * d, 0.06f, Mth.sin(a) * d).add(c0);
            buf.beam(ctx, STREAK, VfxBlend.ADD, new Vector3f(foot).add(0, top + 1.1f, 0), new Vector3f(foot).add(0, top, 0), 0.22f, 0.12f, 1, 0, col(c, 0.0f), col(hot, 0.75f * fade * Mth.sin(ph * Mth.PI)));
        }
        // motes dragged inwards and down
        int m = ctx.seg(10, 4);
        for (int i = 0; i < m; i++) {
            float ph = (age * 0.022f + r.nextFloat()) % 1f, a = r.nextFloat() * Mth.TWO_PI + age * 0.03f * (1f + ph);
            float rad = R * (0.95f - 0.85f * ph);
            Vector3f p = new Vector3f(Mth.cos(a) * rad, 0.15f + (1f - ph) * 0.9f, Mth.sin(a) * rad).add(c0);
            buf.billboard(ctx, MOTE, VfxBlend.ALPHA, p, 0.2f + 0.08f * (1f - ph), 0, col(WHITE, 0.9f * fade * Mth.sin(ph * Mth.PI)));
        }
    }

    // ------------------------------------------------------------------ BURST (a crush)
    private void crush(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1f, inst.duration), p = Mth.clamp(age / dur, 0, 1);
        float size = Math.max(0.5f, inst.power);
        Vector3f c0 = ctx.rel(inst.from(ctx));
        int c = inst.color, hot = VfxVertexBuffer.whiten(c, 0.55f);
        float out = 1f - VfxAnim.easeInCubic(p);
        RandomSource r = inst.random();

        VfxPose floor = VfxPose.ground(new Vector3f(c0).add(0, 0.05f, 0));
        buf.plane(DISC, VfxBlend.ALPHA, floor, 1.4f * size * VfxAnim.easeOutCubic(Mth.clamp(p * 5f, 0, 1)), col(WHITE, 0.65f * out));
        buf.plane(CRACK, VfxBlend.ADD, floor.lift(0.02f).spin(r.nextFloat() * Mth.TWO_PI), 2.0f * size * VfxAnim.easeOutCubic(Mth.clamp(p * 3f, 0, 1)), col(c, 0.9f * out));
        float flash = Mth.clamp(1f - age / 7f, 0, 1);
        VfxBloom.glow(ctx, buf, new Vector3f(c0).add(0, 0.3f, 0), 0.8f * size * (0.5f + flash), c, 0.4f + 0.7f * flash);
        // the shock rings run out along the ground
        for (int k = 0; k < 2; k++) {
            float q = Mth.clamp((p - 0.06f * k) / (0.85f - 0.06f * k), 0, 1), s = VfxAnim.easeOutCubic(q);
            buf.plane(RING, VfxBlend.ADD, floor.lift(0.04f + 0.01f * k), size * (0.4f + 2.6f * s), col(k == 0 ? hot : c, 0.9f * (1f - q)));
            buf.plane(LENS, VfxBlend.ADD, floor.lift(0.06f + 0.01f * k), size * (0.3f + 2.2f * s), col(WHITE, 0.55f * (1f - q)));
        }
        // a ring of dust
        int n = ctx.seg(8, 4);
        for (int i = 0; i < n; i++) {
            float a = Mth.TWO_PI * i / n + r.nextFloat() * 0.4f, s = VfxAnim.easeOutCubic(Mth.clamp(p * 1.3f, 0, 1));
            Vector3f q = new Vector3f(Mth.cos(a) * size * (0.5f + 2.0f * s), 0.25f + 0.5f * s, Mth.sin(a) * size * (0.5f + 2.0f * s)).add(c0);
            buf.billboard(ctx, MOTE, VfxBlend.ALPHA, q, size * (0.5f + 0.6f * s), a, col(WHITE, 0.55f * out));
        }
        // rocks thrown up that fall back
        int m = ctx.seg(8, 4);
        for (int i = 0; i < m; i++) {
            float a = Mth.TWO_PI * i / m + r.nextFloat(), sp = (0.6f + r.nextFloat() * 1.2f) * size, up = (1.2f + r.nextFloat() * 1.6f) * size;
            float tt = Mth.clamp(p * 1.25f, 0, 1);
            Vector3f q = new Vector3f(Mth.cos(a) * sp * tt, up * tt - 2.4f * size * tt * tt + 0.2f, Mth.sin(a) * sp * tt).add(c0);
            if (q.y < c0.y) continue;
            buf.billboard(ctx, SHARD, VfxBlend.ALPHA, q, 0.3f * size, age * 0.2f + i * 2f, col(0xFF9A8AAA, 0.95f * out));
        }
    }
}
