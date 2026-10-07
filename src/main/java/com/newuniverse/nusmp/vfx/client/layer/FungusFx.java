package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Shared textures, palette and geometry of the Fungus Magic effects (FungusLayer): sprites cut from the atlases, mushrooms that stand
 * upright or lean, a haze wall, camera-distance fading. Everything goes through VfxVertexBuffer.quad, so the per-effect vertex budget applies.
 */
final class FungusFx {
    private FungusFx() {}

    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    // spore clouds and light
    static final ResourceLocation PUFF = t("fungus_puff"), PUFF2 = t("fungus_puff2"), SPORES = t("fungus_spores"), FLARE = t("fungus_flare"),
            SHOCK = t("fungus_shock"), HAZE = t("fungus_haze"), TRAIL = t("fungus_trail");
    // the ground: mycelium web, cap gills, fairy ring
    static final ResourceLocation MYCELIUM = t("fungus_mycelium"), GILLS = t("fungus_gills"), RING = t("fungus_ring");
    // bodies
    static final ResourceLocation PUFFBALL = t("fungus_puffball"), HYPHA = t("fungus_hypha"), SHROOMS = t("fungus_shrooms"),
            DEBRIS = t("fungus_debris"), MR = t("fungus_mr_mushroom"), MR_HEAVY = t("fungus_mr_heavy");

    // palette (ARGB)
    static final int GOLD = 0xFFF2CC62, AMBER = 0xFFD89A38, CREAM = 0xFFF7ECCB, OCHRE = 0xFFC0A04A, OLIVE = 0xFF9AA544, TOXIC = 0xFFC4DC54,
            SOIL = 0xFF2A2114, DUST = 0xFFB8A060, WHITE = 0xFFFFFFFF;

    /** The effect's tint mixed into a palette colour (the palette always stays visible, even for a white tint). */
    static int mix(int palette, int tint, float k) { return VfxVertexBuffer.lerpColor(palette, 0xFF000000 | tint, k); }

    static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, alpha); }

    /** 1 while running, ramping 0 -> 1 over the first {@code in} ticks and 1 -> 0 over the last {@code out}. */
    static float life(VfxInstance inst, float age, float in, float out) {
        float a = in <= 0 ? 1 : Mth.clamp(age / in, 0, 1);
        float b = out <= 0 ? 1 : Mth.clamp((inst.duration - age) / out, 0, 1);
        return Math.min(a, b);
    }

    /** Deterministic 0..1 hash (per effect seed and index). */
    static float hash(long seed, int i, int salt) {
        long x = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L + salt * 0x94D049BB133111EBL;
        x ^= x >>> 31; x *= 0x7FB5D329728EA185L; x ^= x >>> 27; x *= 0x81DADEF4BC2DD44DL; x ^= x >>> 33;
        return (x >>> 40) / (float) (1L << 24);
    }

    static float smooth(float x) { x = Mth.clamp(x, 0, 1); return x * x * (3 - 2 * x); }
    static float c01(float x) { return Mth.clamp(x, 0, 1); }
    static float fract(float x) { return x - (float) Math.floor(x); }

    /** Fades things that are close to the camera so a cloud never fills the screen (0 at {@code start} blocks, 1 at {@code full}). */
    static float near(Vector3f p, float start, float full) { return smooth((p.length() - start) / Math.max(0.01f, full - start)); }

    /** A unit vector perpendicular to {@code dir}, horizontal when possible. */
    static Vector3f side(Vector3f dir) {
        Vector3f s = new Vector3f(dir).cross(0, 1, 0);
        if (s.lengthSquared() < 1e-5f) s.set(1, 0, 0);
        return s.normalize();
    }

    // ------------------------------------------------------------------ atlas cells
    /** Cell k of a 2 x 2 atlas (spores, debris): u0, v0, u1, v1. */
    static float[] cell2(int k) {
        float u = (k & 1) * 0.5f, v = (k >> 1) * 0.5f;
        return new float[]{u + 0.012f, v + 0.012f, u + 0.488f, v + 0.488f};
    }

    /** Cell k of the 4-wide mushroom atlas. */
    static float[] cell4(int k) { return new float[]{k * 0.25f + 0.008f, 0.008f, (k + 1) * 0.25f - 0.008f, 0.992f}; }

    /** A camera-facing sprite of size {@code size} (blocks) cut from an atlas cell. */
    static void sprite(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, VfxBlend blend, float[] uv, Vector3f centre, float size, float rot, int argb) {
        float c = Mth.cos(rot) * size * 0.5f, s = Mth.sin(rot) * size * 0.5f;
        Vector3f rx = new Vector3f(ctx.camRight).mul(c).add(new Vector3f(ctx.camUp).mul(s));
        Vector3f ry = new Vector3f(ctx.camUp).mul(c).sub(new Vector3f(ctx.camRight).mul(s));
        int col = blend.grade(argb, 1f);
        buf.quad(tex, blend, new Vector3f(centre).sub(rx).sub(ry), new Vector3f(centre).add(rx).sub(ry),
                new Vector3f(centre).add(rx).add(ry), new Vector3f(centre).sub(rx).add(ry), uv[0], uv[1], uv[2], uv[3], col, col);
    }

    /** A glittering spore (cells of the spore atlas: 0 grains, 1 bubble, 2 pod, 3 twinkle), drawn additively. */
    static void mote(VfxVertexBuffer buf, VfxRenderContext ctx, int cell, Vector3f centre, float size, float rot, int argb) {
        sprite(buf, ctx, SPORES, VfxBlend.ADD, cell2(cell), centre, size, rot, argb);
    }

    /**
     * A sprite standing along {@code dir} (its texture "up"): base at {@code base}, {@code length} long, {@code halfW} wide each side,
     * turned to face the camera round its own axis. Mushrooms grow with it.
     */
    static void along(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, VfxBlend blend, float[] uv, Vector3f base, Vector3f dir,
                      float length, float halfW, int argb) {
        if (length < 0.01f || halfW < 0.003f) return;
        Vector3f mid = new Vector3f(base).fma(length * 0.5f, dir);
        Vector3f s = new Vector3f(dir).cross(mid);                     // dir x (mid - camera at the origin)
        if (s.lengthSquared() < 1e-8f) s.set(ctx.camRight);
        s.normalize();
        if (s.dot(ctx.camRight) < 0) s.negate();
        s.mul(halfW);
        Vector3f tip = new Vector3f(base).fma(length, dir);
        int col = blend.grade(argb, 1f);
        buf.quad(tex, blend, new Vector3f(base).sub(s), new Vector3f(base).add(s), new Vector3f(tip).add(s), new Vector3f(tip).sub(s),
                uv[0], uv[1], uv[2], uv[3], col, col);
    }

    /** A sprite standing straight up (a mushroom on the ground). */
    static void upright(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, VfxBlend blend, float[] uv, Vector3f base, float height, float halfW, int argb) {
        Vector3f r = new Vector3f(ctx.camRight.x, 0, ctx.camRight.z);
        if (r.lengthSquared() < 1e-6f) r.set(1, 0, 0);
        r.normalize().mul(halfW);
        int col = blend.grade(argb, 1f);
        Vector3f top = new Vector3f(base).add(0, height, 0);
        buf.quad(tex, blend, new Vector3f(base).sub(r), new Vector3f(base).add(r), new Vector3f(top).add(r), new Vector3f(top).sub(r),
                uv[0], uv[1], uv[2], uv[3], col, col);
    }

    /**
     * A ribbon standing on a circle (a wall): width along the pose normal, U wraps {@code repeats} times and scrolls, V = 1 at the bottom edge.
     * Each segment is faded by its distance from the camera so the near side of a big wall never blinds the player standing inside it.
     */
    static void wall(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, VfxPose pose, float radius, float halfWidth, int segments,
                     float repeats, float uScroll, int argb) {
        Vector3f n = new Vector3f(pose.normal()).mul(halfWidth);
        for (int i = 0; i < segments; i++) {
            float a0 = Mth.TWO_PI * i / segments, a1 = Mth.TWO_PI * (i + 1) / segments;
            Vector3f m0 = pose.point(Mth.cos(a0) * radius, Mth.sin(a0) * radius), m1 = pose.point(Mth.cos(a1) * radius, Mth.sin(a1) * radius);
            float k = near(new Vector3f(m0).add(m1).mul(0.5f), 0.6f, 3.2f);
            if (k <= 0.01f) continue;
            int col = blend.grade(a(argb, k), 0.6f);
            float u0 = uScroll + repeats * i / segments, u1 = uScroll + repeats * (i + 1) / segments;
            buf.quad(tex, blend, new Vector3f(m0).sub(n), new Vector3f(m1).sub(n), new Vector3f(m1).add(n), new Vector3f(m0).add(n), u0, 0, u1, 1, col, col);
        }
    }
}
