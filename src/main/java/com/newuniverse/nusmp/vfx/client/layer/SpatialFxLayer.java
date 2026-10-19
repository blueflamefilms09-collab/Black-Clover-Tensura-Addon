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
 * 1.93.0 Spatial Magic, the own effects (Finral's gates and cuts, Langris's folded space), drawn to the bar of the Time Magic layers: every part is a big
 * textured quad layered back to front (floor glow and sigil, wall, bodies, rims, then the hot additive sprites), animated from the age alone.
 * <ul>
 *   <li>SPATIAL_FX1, CAST: 'from' = hand, 'to' = target, power = width (0.6 to 3). A gate ring snaps open at the hand and charges over the first quarter, then a
 *       rift (a lens-shaped slit of folded space with a white seam, spatial_rift) flies to the target with a thinner afterimage on each side, a lensing rim
 *       riding its head and glass shards shed behind it; it ends in a flash and a fracture at 'to'. Default 16 ticks.</li>
 *   <li>SPATIAL_FX2, FIELD: 'from' = centre on the ground, power = radius. A rune sigil turns on the floor under a counter-turning gate ring; a wall of standing rifts
 *       rings the rim; a lensing wave runs out from the centre every 30 ticks; motes of space fall inwards. In over 8 ticks, out over the last 12. Default 80.</li>
 *   <li>SPATIAL_FX3, BURST: 'from' = centre, 'to' = direction hint, power = scale. A flash, two lensing rings, a gate disc that folds shut, fracture lines on the
 *       ground and glass shards thrown out, and a lingering glow. Default 28.</li>
 * </ul>
 * Budget: FX1 about 150 vertices, FX2 about 250, FX3 about 230 (the preview reports it; the limit is 400).
 */
public class SpatialFxLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation RING = t("spatial_ring"), SIGIL = t("spatial_sigil"), STAR = t("spatial_star"), MOTE = t("spatial_mote"), RIFT = t("spatial_rift"),
            SHARD = t("spatial_shard"), FOLD = t("spatial_fold"), CRACK = t("spatial_crack"), LENS = t("spatial_lens");

    private static final int WHITE = 0xFFFFFFFF;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.SPATIAL_FX1, VfxShape.SPATIAL_FX2, VfxShape.SPATIAL_FX3); }

    @Override
    public int defaultDuration(VfxShape s) { return switch (s) { case SPATIAL_FX1 -> 16; case SPATIAL_FX2 -> 80; default -> 28; }; }

    @Override public int defaultColor(VfxShape s) { return 0xFFB088FF; }

    @Override
    public void onSpawn(VfxInstance inst) {
        if (inst.shape == VfxShape.SPATIAL_FX3) VfxShake.add(inst.payload.from(), 0.7f * Math.min(2f, inst.power), 8);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        switch (inst.shape) {
            case SPATIAL_FX1 -> cast(inst, ctx, buf);
            case SPATIAL_FX2 -> field(inst, ctx, buf);
            case SPATIAL_FX3 -> burst(inst, ctx, buf);
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
        int c = inst.color, hot = VfxVertexBuffer.whiten(c, 0.6f);
        float charge = Mth.clamp(age / (dur * 0.25f), 0, 1);

        // the gate ring that opens at the hand (faces along the throw), then folds shut
        float open = VfxAnim.easeOutBack(charge) * (1f - VfxAnim.easeInCubic(Mth.clamp((prog - 0.45f) / 0.55f, 0, 1)));
        VfxPose gate = VfxPose.facing(from, dir);
        if (open > 0.02f) {
            VfxBloom.glow(ctx, buf, from, 0.35f * size * open, c, 0.6f);
            buf.plane(RING, VfxBlend.ADD, gate.spin(age * 0.25f), 0.5f * size * open, col(c, 0.95f * open));
            buf.plane(FOLD, VfxBlend.ADD, gate.lift(0.02f).spin(-age * 0.3f), 0.38f * size * open, col(c, 0.55f * open));
        }
        if (prog < 0.2f) return;

        // the rift in flight: tail drags 40 percent of the way behind the head
        float e = VfxAnim.easeOutCubic(Mth.clamp((prog - 0.2f) / 0.8f, 0, 1));
        Vector3f head = new Vector3f(from).lerp(to, e), tail = new Vector3f(from).lerp(to, Math.max(0f, e - 0.45f));
        float fade = 1f - VfxAnim.easeInCubic(Mth.clamp((prog - 0.75f) / 0.25f, 0, 1));
        Vector3f side = new Vector3f(dir).cross(ctx.camUp);
        if (side.lengthSquared() < 1.0e-4f) side.set(ctx.camRight);
        side.normalize();
        for (int k = -1; k <= 1; k += 2) {                                                  // afterimages
            Vector3f off = new Vector3f(side).mul(k * 0.22f * size);
            buf.beam(ctx, RIFT, VfxBlend.ADD, new Vector3f(tail).add(off).sub(new Vector3f(dir).mul(0.3f)), new Vector3f(head).add(off).sub(new Vector3f(dir).mul(0.3f)),
                    0.45f * size, 0.2f * size, 1, 0, col(c, 0.28f * fade), col(c, 0.28f * fade));
        }
        buf.beam(ctx, RIFT, VfxBlend.ADD, tail, head, 0.95f * size, 0.45f * size, 1, 0, col(c, 0.9f * fade), col(hot, 0.9f * fade));
        buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, tail, head, 0.22f * size, 0.06f * size, 1, 0, col(WHITE, 0.85f * fade), col(WHITE, 0.85f * fade));
        buf.plane(LENS, VfxBlend.ADD, VfxPose.facing(head, dir).spin(age * 0.2f), 0.5f * size, col(c, 0.8f * fade));
        VfxBloom.glow(ctx, buf, head, 0.3f * size, hot, 0.8f * fade);

        // glass shards shed behind the head
        RandomSource r = inst.random();
        for (int i = 0; i < 5; i++) {
            float lag = 0.12f + 0.1f * i + r.nextFloat() * 0.06f;
            Vector3f p = new Vector3f(from).lerp(to, Math.max(0f, e - lag));
            p.add(new Vector3f(side).mul((r.nextFloat() - 0.5f) * 0.9f * size)).add(0, (r.nextFloat() - 0.5f) * 0.5f * size - (age * 0.012f * i), 0);
            buf.billboard(ctx, SHARD, VfxBlend.ADD, p, 0.2f * size, age * 0.3f + i, col(c, 0.7f * fade * (1f - 0.12f * i)));
        }

        // the landing: flash and fracture at 'to'
        if (prog > 0.8f) {
            float p = (prog - 0.8f) / 0.2f;
            VfxBloom.glow(ctx, buf, to, 0.6f * size * (1f - p * 0.4f), c, 0.9f * (1f - p));
            buf.billboard(ctx, CRACK, VfxBlend.ADD, to, 1.4f * size * VfxAnim.easeOutCubic(p), age * 0.05f, col(c, 0.8f * (1f - p)));
            buf.billboard(ctx, STAR, VfxBlend.ADD, to, 0.9f * size * (1f - p), 0f, col(WHITE, 0.9f * (1f - p)));
        }
    }

    // ------------------------------------------------------------------ FIELD
    private void field(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        float fade = life(inst, age, 8, 12), grow = VfxAnim.easeOutCubic(Mth.clamp(age / 10f, 0, 1));
        float R = Math.max(1f, inst.power) * grow;
        if (R <= 0.05f || fade <= 0.01f) return;
        Vector3f c0 = ctx.rel(inst.from(ctx));
        int c = inst.color, hot = VfxVertexBuffer.whiten(c, 0.6f);
        RandomSource r = inst.random();

        VfxPose floor = VfxPose.ground(new Vector3f(c0).add(0, 0.05f, 0));
        VfxBloom.planeGlow(buf, floor, R, c, 0.55f * fade);
        buf.plane(SIGIL, VfxBlend.ADD, floor.spin(age * 0.012f), R, col(c, 0.85f * fade));
        buf.plane(RING, VfxBlend.ADD, floor.lift(0.02f).spin(-age * 0.03f), R * 0.98f, col(hot, 0.8f * fade));
        buf.plane(RING, VfxBlend.ADD, floor.lift(0.03f).spin(age * 0.05f), R * 0.55f, col(c, 0.6f * fade));

        // the wall of standing rifts round the rim
        int n = ctx.seg(8, 4);
        float h = Math.min(4.2f, 1.6f + 0.35f * R);
        for (int i = 0; i < n; i++) {
            float a = Mth.TWO_PI * i / n + age * 0.006f;
            Vector3f base = new Vector3f(Mth.cos(a) * R * 0.97f, 0.05f, Mth.sin(a) * R * 0.97f).add(c0);
            float flick = 0.6f + 0.4f * Mth.sin(age * 0.21f + i * 1.7f);
            float grow2 = VfxAnim.easeOutCubic(Mth.clamp((age - i * 0.8f) / 10f, 0, 1));
            buf.beam(ctx, RIFT, VfxBlend.ADD, base, new Vector3f(base).add(0, h * grow2, 0), 0.8f + 0.1f * R, 0.5f + 0.06f * R, 1, 0, col(c, 0.6f * fade * flick), col(hot, 0.6f * fade * flick));
        }
        // the lensing wave that runs out from the centre
        float wave = (age % 30f) / 30f;
        buf.plane(LENS, VfxBlend.ADD, floor.lift(0.06f), R * (0.15f + 0.85f * wave), col(WHITE, 0.55f * fade * (1f - wave)));

        // motes of space falling inwards
        int m = ctx.seg(12, 5);
        for (int i = 0; i < m; i++) {
            float ph = (age * 0.02f + r.nextFloat()) % 1f, a = r.nextFloat() * Mth.TWO_PI + age * 0.01f;
            float rad = R * (0.95f - 0.8f * ph);
            Vector3f p = new Vector3f(Mth.cos(a) * rad, 0.2f + (1f - ph) * h * 0.8f, Mth.sin(a) * rad).add(c0);
            buf.billboard(ctx, MOTE, VfxBlend.ADD, p, 0.16f + 0.06f * (1f - ph), 0, col(i % 2 == 0 ? c : WHITE, 0.85f * fade * Mth.sin(ph * Mth.PI)));
        }
    }

    // ------------------------------------------------------------------ BURST
    private void burst(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick), dur = Math.max(1f, inst.duration), p = Mth.clamp(age / dur, 0, 1);
        float size = Math.max(0.5f, inst.power);
        Vector3f c0 = ctx.rel(inst.from(ctx));
        Vector3f dir = new Vector3f(ctx.rel(inst.to(ctx))).sub(c0);
        if (dir.lengthSquared() < 1.0e-3f) dir.set(0, 1, 0);
        dir.normalize();
        int c = inst.color, hot = VfxVertexBuffer.whiten(c, 0.6f);
        float out = 1f - VfxAnim.easeInCubic(p);
        RandomSource r = inst.random();

        float flash = Mth.clamp(1f - age / 8f, 0, 1);
        VfxBloom.glow(ctx, buf, c0, 0.7f * size * (0.5f + flash), c, 0.5f + 0.6f * flash);
        if (flash > 0.02f) buf.billboard(ctx, STAR, VfxBlend.ADD, c0, 1.8f * size * (0.6f + 0.4f * flash), age * 0.05f, col(WHITE, flash));

        // the gate disc that folds shut
        float shut = 1f - VfxAnim.easeInCubic(Mth.clamp(p / 0.6f, 0, 1));
        if (shut > 0.02f) {
            buf.plane(FOLD, VfxBlend.ADD, VfxPose.facing(c0, dir).spin(-age * 0.4f), 0.85f * size * shut, col(c, 0.85f * shut));
            buf.plane(RING, VfxBlend.ADD, VfxPose.facing(c0, dir).lift(0.03f).spin(age * 0.3f), 1.0f * size * (0.4f + 0.6f * shut), col(hot, 0.8f * shut));
        }
        // two lensing rings running out
        for (int k = 0; k < 2; k++) {
            float q = Mth.clamp((p - 0.08f * k) / (0.9f - 0.08f * k), 0, 1), s = VfxAnim.easeOutCubic(q);
            buf.plane(LENS, VfxBlend.ADD, VfxPose.facing(c0, dir).lift(0.04f * k), size * (0.3f + 2.0f * s), col(k == 0 ? hot : c, 0.8f * (1f - q)));
        }
        // fractures on the ground, shards thrown out
        VfxPose floor = VfxPose.ground(new Vector3f(c0).add(0, 0.04f, 0));
        buf.plane(CRACK, VfxBlend.ADD, floor.spin(r.nextFloat() * Mth.TWO_PI), 1.5f * size * VfxAnim.easeOutCubic(Mth.clamp(p * 4f, 0, 1)), col(c, 0.85f * out));
        int n = ctx.seg(9, 4);
        for (int i = 0; i < n; i++) {
            float a = Mth.TWO_PI * i / n + r.nextFloat() * 0.5f, up = 0.4f + r.nextFloat() * 0.9f, sp = (1.0f + r.nextFloat() * 1.2f) * size;
            float e = VfxAnim.easeOutCubic(Mth.clamp(p * 1.4f, 0, 1));
            Vector3f q = new Vector3f(Mth.cos(a) * sp * e, up * size * e - 0.9f * size * p * p, Mth.sin(a) * sp * e).add(c0);
            buf.billboard(ctx, SHARD, VfxBlend.ADD, q, 0.28f * size * (1f - 0.4f * p), age * 0.25f + i * 2f, col(i % 2 == 0 ? hot : c, 0.9f * out));
        }
        VfxBloom.planeGlow(buf, floor.lift(0.01f), size * 1.2f, c, 0.4f * out);
    }
}
