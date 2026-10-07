package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Shared look of Body Magic (textures from tools/gen_body_textures.py): the palette, the sprite names and the few primitives the
 * three shapes of {@link BodyLayer} are built from. Every primitive goes through VfxVertexBuffer.quad, so the per-effect vertex
 * budget still applies.
 *
 * Texture layouts: body_steam is a 2 x 2 atlas of puffs; body_steam_jet, body_strand, body_fibre and body_slug run along V (the far
 * end is the top of the image, V = 0); body_wall (hot base at the bottom) and body_band tile in U; body_ring / body_pulse / body_veins /
 * body_flesh are plates.
 */
public final class BodyFx {
    private BodyFx() {}

    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    public static final ResourceLocation STEAM = t("body_steam"), JET = t("body_steam_jet"), VEINS = t("body_veins"), FLESH = t("body_flesh"),
            RING = t("body_ring"), PULSE = t("body_pulse"), WALL = t("body_wall"), BAND = t("body_band"), FIBRE = t("body_fibre"), SLUG = t("body_slug"),
            MUZZLE = t("body_muzzle"), RIFLING = t("body_rifling"), STRAND = t("body_strand"), BURST = t("body_burst"), SPARK = t("body_spark");

    /** Palette: bronze-orange light, hot white-gold core, vein red, muscle tissue, dark flesh, steam. */
    public static final int ORANGE = 0xFFFF8A4A, AMBER = 0xFFFFB46A, HOT = 0xFFFFE8CC, VEIN = 0xFFFF5A2A, FLESH_MID = 0xFFC07450,
            FLESH_DEEP = 0xFF4E1C14, STEAM_WHITE = 0xFFFFF2E8, STEAM_SHADE = 0xFFD9BEAC;

    // ------------------------------------------------------------------ small helpers
    /** The magic's own orange, pulled a little toward the spell's tint (never all the way: it must read as Body Magic even when the tint is white). */
    static int glow(VfxInstance inst) { return VfxVertexBuffer.lerpColor(ORANGE, inst.color | 0xFF000000, 0.35f) | 0xFF000000; }

    /** argb with its alpha scaled (0..1). */
    static int alpha(int argb, float amount) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(amount, 0f, 1f)); }

    /** 1 while running, ramping 0 -> 1 over the first {@code in} ticks and 1 -> 0 over the last {@code out}. */
    static float life(VfxInstance inst, float age, float in, float out) {
        float a = in <= 0 ? 1 : Mth.clamp(age / in, 0, 1);
        float b = out <= 0 ? 1 : Mth.clamp((inst.duration - age) / out, 0, 1);
        return Math.min(a, b);
    }

    /** Deterministic 0..1 hash (effect seed, index, salt). */
    static float hash(long seed, int i, int salt) {
        long x = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L + salt * 0x94D049BB133111EBL;
        x ^= x >>> 31; x *= 0x7FB5D329728EA185L; x ^= x >>> 27; x *= 0x81DADEF4BC2DD44DL; x ^= x >>> 33;
        return (x >>> 40) / (float) (1L << 24);
    }

    /** Ease-out over t = 0..1 (clamped). */
    static float ease(float t) { return VfxAnim.easeOutCubic(Mth.clamp(t, 0f, 1f)); }

    /** Smoothstep. */
    static float step(float a, float b, float x) { float t = Mth.clamp((x - a) / (b - a), 0f, 1f); return t * t * (3f - 2f * t); }

    /** 0 right at the camera, 1 from about two blocks away: puffs and steam fade out near the eye so they never white out first person. */
    static float near(Vector3f camRel) { return Mth.clamp((camRel.length() - 0.6f) / 1.6f, 0f, 1f); }

    /** Horizontal unit vector perpendicular to dir. */
    static Vector3f side(Vector3f dir) {
        Vector3f s = new Vector3f(dir).cross(0f, 1f, 0f);
        if (s.lengthSquared() < 1e-5f) s.set(1f, 0f, 0f);
        return s.normalize();
    }

    /** The third axis of (dir, side, up). */
    static Vector3f upOf(Vector3f dir, Vector3f side) { return new Vector3f(side).cross(dir).normalize(); }

    /** The double heartbeat: a hard thump at phase 0 and a softer one 7 ticks later, every 24 ticks. 0..1. */
    static float beat(float age) {
        float ph = age % 24f;
        return Math.max(thump(ph), 0.7f * thump(ph - 7f));
    }

    private static float thump(float x) {
        if (x < 0f || x > 13f) return 0f;
        return Math.min(1f, 1.3f * Mth.clamp(x / 0.8f, 0f, 1f) * (float) Math.exp(-x * 0.30f));
    }

    // ------------------------------------------------------------------ primitives
    /** Camera-facing w x h sprite turned by rot, sampling the texture rectangle (u0, v0) - (u1, v1). */
    static void sprite(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, VfxBlend blend, Vector3f c, float w, float h, float rot,
                       float u0, float v0, float u1, float v1, int argb) {
        float cs = Mth.cos(rot), sn = Mth.sin(rot);
        Vector3f rx = new Vector3f(ctx.camRight).mul(cs * w * 0.5f).add(new Vector3f(ctx.camUp).mul(sn * w * 0.5f));
        Vector3f ry = new Vector3f(ctx.camUp).mul(cs * h * 0.5f).sub(new Vector3f(ctx.camRight).mul(sn * h * 0.5f));
        int col = blend.grade(argb, 1f);
        buf.quad(tex, blend, new Vector3f(c).sub(rx).sub(ry), new Vector3f(c).add(rx).sub(ry), new Vector3f(c).add(rx).add(ry), new Vector3f(c).sub(rx).add(ry),
                u0, v0, u1, v1, col, col);
    }

    /** One puff of the steam atlas (cell 0..3). */
    static void puff(VfxVertexBuffer buf, VfxRenderContext ctx, int cell, VfxBlend blend, Vector3f c, float size, float rot, int argb) {
        float u0 = (cell & 1) * 0.5f, v0 = ((cell >> 1) & 1) * 0.5f;
        sprite(buf, ctx, STEAM, blend, c, size, size, rot, u0, v0, u0 + 0.5f, v0 + 0.5f, argb);
    }

    /** A flying ember: the spark sprite turned so its streak lies along the screen direction of vel. */
    static void ember(VfxVertexBuffer buf, VfxRenderContext ctx, Vector3f c, Vector3f vel, float size, int argb) {
        float rot = (float) Math.atan2(vel.dot(ctx.camUp), vel.dot(ctx.camRight)) - Mth.HALF_PI;
        buf.billboard(ctx, SPARK, VfxBlend.ADD, c, size, rot, argb);
    }

    /**
     * Camera-facing quad from a to b. U runs across (0..1), V runs along: vA at a, vB at b (so V = 1 at a and 0 at b puts the bottom of the
     * image at a and the top at b). wA / wB are the widths at the two ends, cA / cB their colours.
     */
    static void strip(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f a, Vector3f b, float wA, float wB, float vA, float vB, int cA, int cB) {
        Vector3f dir = new Vector3f(b).sub(a);
        if (dir.lengthSquared() < 1e-8f) return;
        Vector3f mid = new Vector3f(a).add(b).mul(0.5f);
        Vector3f s = new Vector3f(dir).cross(new Vector3f(mid).negate());
        if (s.lengthSquared() < 1e-10f) return;
        s.normalize();
        Vector3f sa = new Vector3f(s).mul(wA * 0.5f), sb = new Vector3f(s).mul(wB * 0.5f);
        buf.quad(tex, blend, new Vector3f(a).sub(sa), new Vector3f(a).add(sa), new Vector3f(b).add(sb), new Vector3f(b).sub(sb),
                0f, vB, 1f, vA, blend.grade(cA, 1f), blend.grade(cB, 1f));
    }

    /** An open cylinder along the pose normal (a barrel, a wall): U runs round it, V along it (V = vRepeat at the bottom end, 0 at the top end). */
    static void tube(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, VfxPose pose, float radius, float halfLen, int segs,
                     float uRepeat, float uScroll, float vRepeat, int colBottom, int colTop) {
        Vector3f n = new Vector3f(pose.normal()).mul(halfLen);
        int cb = blend.grade(colBottom, 0.6f), ct = blend.grade(colTop, 0.6f);
        for (int i = 0; i < segs; i++) {
            float a0 = Mth.TWO_PI * i / segs, a1 = Mth.TWO_PI * (i + 1) / segs;
            Vector3f m0 = pose.point(Mth.cos(a0) * radius, Mth.sin(a0) * radius), m1 = pose.point(Mth.cos(a1) * radius, Mth.sin(a1) * radius);
            float u0 = uScroll + uRepeat * i / segs, u1 = uScroll + uRepeat * (i + 1) / segs;
            buf.quad(tex, blend, new Vector3f(m0).sub(n), new Vector3f(m1).sub(n), new Vector3f(m1).add(n), new Vector3f(m0).add(n), u0, 0f, u1, vRepeat, cb, ct);
        }
    }

    /** A wall standing on the ground at {@code base}: radius, height, bottom and top colours (fade the top to nothing for a rim of light). */
    static void wall(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f base, float radius, float height, int segs,
                     float repeats, float scroll, int colBottom, int colTop) {
        VfxPose mid = VfxPose.ground(new Vector3f(base).add(0f, height * 0.5f, 0f));
        tube(buf, tex, blend, mid, radius, height * 0.5f, segs, repeats, scroll, 1f, colBottom, colTop);
    }
}
