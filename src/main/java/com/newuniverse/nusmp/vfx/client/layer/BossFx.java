package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * 1.35.0: the small drawing kit the four boss layers share ({@link DevilBossLayer}, {@link StoneBossLayer}, {@link TideBossLayer},
 * {@link OrderBossLayer}). Everything is built from the repo's existing particle textures and the vertex buffer's own primitives, so there is
 * no new texture, and every helper spends a known number of quads (4 vertices each) so a layer can keep inside the 400-vertex budget.
 * Convention of all boss shapes: 'from' = ground centre, 'to' = a second point (target, tip, anchor), power = radius or length in blocks,
 * duration = life in ticks, and the lowest bit of the seed = 1 for the STRIKE of a move, 0 for its TELEGRAPH (the wind-up).
 */
final class BossFx {
    private BossFx() {}

    /** True when this instance is the strike (the move lands), false when it is the telegraph. */
    static boolean strike(VfxInstance inst) { return (inst.seed & 1L) == 1L; }

    /** A deterministic number in [0, 1) for (seed, i): the same on every client, no allocation. */
    static float rnd(long seed, int i) {
        long h = (seed >>> 1) * 0x9E3779B97F4A7C15L + (i + 1L) * 0xBF58476D1CE4E5B9L;
        h ^= h >>> 31;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 29;
        return (h >>> 40) / (float) (1 << 24);
    }

    static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(alpha, 0f, 1f)); }

    static int hot(int argb, float t) { return VfxVertexBuffer.whiten(argb, t); }

    static float clamp01(float v) { return Mth.clamp(v, 0f, 1f); }

    /** A flat band on the floor: ring of texture 'tex' between r - w/2 and r + w/2, 'seg' quads. */
    static void groundRing(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f centre, net.minecraft.resources.ResourceLocation tex,
                           VfxBlend blend, float r, float w, int seg, float spin, float uRepeat, float uScroll, int argb) {
        if (r <= 0.02f) return;
        VfxPose pose = VfxPose.ground(centre).spin(spin);
        if (tex == VfxTextures.GLOW) {          // a soft band: sample one vertical line of the glow (its falloff runs across the band), never the whole disc round the ring
            uRepeat = 0f;
            uScroll = 0.5f;
        }
        buf.ring(tex, blend, pose, Math.max(0.01f, r - w * 0.5f), r + w * 0.5f, ctx.seg(seg, 8), uRepeat, uScroll, argb);
    }

    /** Shorter call: a floor band with default repeat and no scroll. */
    static void band(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f centre, net.minecraft.resources.ResourceLocation tex, VfxBlend blend,
                     float r, float w, int seg, float spin, int argb) {
        groundRing(ctx, buf, centre, tex, blend, r, w, seg, spin, 1f, 0f, argb);
    }

    /** A bright line with a soft outer glow (2 quads). */
    static void glowLine(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f p, Vector3f q, float width, int argb, float alpha) {
        buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, p, q, width * 2.4f, width * 2.4f, 1, 0, a(argb, alpha * 0.45f), a(argb, alpha * 0.45f));
        buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, p, q, width * 0.8f, width * 0.8f, 1, 0, a(hot(argb, 0.7f), alpha), a(hot(argb, 0.7f), alpha));
    }

    /** A zig-zag line of 'segs' segments between p and q, jittered by 'jitter' blocks, one quad per segment. */
    static void jagged(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f p, Vector3f q, int segs, float jitter, long seed, int salt,
                       float width, int argb, float alpha) {
        Vector3f prev = new Vector3f(p);
        for (int i = 1; i <= segs; i++) {
            Vector3f next = new Vector3f(q).sub(p).mul((float) i / segs).add(p);
            if (i < segs) next.add((rnd(seed, salt + i * 3) - 0.5f) * jitter, (rnd(seed, salt + i * 3 + 1) - 0.5f) * jitter * 0.3f, (rnd(seed, salt + i * 3 + 2) - 0.5f) * jitter);
            buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, prev, next, width, width, 1, 0, a(argb, alpha), a(hot(argb, 0.4f), alpha));
            prev = next;
        }
    }

    /** Camera-facing spark (1 quad). */
    static void spark(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f at, float size, float rot, int argb) {
        buf.billboard(ctx, VfxTextures.SPARK, VfxBlend.ADD, at, size, rot, argb);
    }

    /** A soft puff of dust or foam (1 quad, alpha blend so it can be dark). */
    static void puff(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f at, float size, float rot, int argb) {
        buf.billboard(ctx, VfxTextures.GLOW, VfxBlend.ALPHA, at, size, rot, argb);
    }

    /** A point on a circle on the floor round 'c' (camera-relative c). */
    static Vector3f onCircle(Vector3f c, float r, float ang, float y) {
        return new Vector3f(c.x + Mth.cos(ang) * r, c.y + y, c.z + Mth.sin(ang) * r);
    }

    /** The camera-relative point of a world position lifted by dy. */
    static Vector3f rel(VfxRenderContext ctx, Vec3 world, double dy) { return ctx.rel(world.add(0, dy, 0)); }

    /** A pentagram / star polygon with 'n' points (n odd, step 2): n quads of line, on the floor round c, radius r, rotated by 'rot'. */
    static void star(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f c, int n, float r, float rot, float width, int argb, float alpha) {
        for (int i = 0; i < n; i++) {
            float a0 = rot + Mth.TWO_PI * i / n, a1 = rot + Mth.TWO_PI * ((i + 2) % n) / n;
            glowLineFlat(ctx, buf, onCircle(c, r, a0, 0.06f), onCircle(c, r, a1, 0.06f), width, argb, alpha);
        }
    }

    /** One quad of line (no outer glow), for dense patterns. */
    static void glowLineFlat(VfxRenderContext ctx, VfxVertexBuffer buf, Vector3f p, Vector3f q, float width, int argb, float alpha) {
        buf.beam(ctx, VfxTextures.GLOW, VfxBlend.ADD, p, q, width, width, 1, 0, a(argb, alpha), a(argb, alpha));
    }
}
