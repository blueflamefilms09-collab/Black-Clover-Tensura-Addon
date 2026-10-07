package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Shared pieces of the Black Oil Magic effects: the textures (tools/gen_black_oil_textures.py), the palette and a few geometry helpers.
 * The tar textures are dark grey with glossy highlights, so one sprite is drawn twice: an ALPHA pass in near-black (the oil body) and an
 * ADD pass in magenta (only the wet highlights light up, as if the pool were lighting them from below). Everything goes through
 * VfxVertexBuffer.quad, so the per-effect vertex budget still applies.
 */
final class BlackOilFx {
    private BlackOilFx() {}

    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final ResourceLocation POOL = t("black_oil_pool"), RIDGES = t("black_oil_ridges"), RIM = t("black_oil_rim"), FLAME = t("black_oil_flame"),
            CANDLE = t("black_oil_candle"), DROP = t("black_oil_drop"), STREAK = t("black_oil_streak"), TENDRIL = t("black_oil_tendril"),
            SPLAT = t("black_oil_splat"), RIPPLE = t("black_oil_ripple"), BUBBLE = t("black_oil_bubble"), SHEEN = t("black_oil_sheen"),
            BURST = t("black_oil_burst");

    /**
     * The palette (from the owner's still): a glowing magenta pool, lighter pink ridges, black tar with a faint warm cast, white candle flames.
     * TAR multiplies the dark grey tar sprites, so it stays near white; PINK is the owner's glow magenta.
     */
    static final int TAR = 0xFFE6C8D8, POOL_COL = 0xFFFF2E7C, PINK = 0xFFFF2D9A, ROSE = 0xFFFF78BE, HOT = 0xFFFFD6EC, WHITE = 0xFFFFF6FB, VIOLET = 0xFFB040FF;

    /** The palette colour pulled a little toward the spell's own tint (so a white tint still reads as this magic). */
    static int mix(int palette, int tint, float amount) {
        return VfxVertexBuffer.lerpColor(palette | 0xFF000000, tint | 0xFF000000, amount);
    }

    static float clamp01(float x) { return Mth.clamp(x, 0f, 1f); }

    /** 0..1 smoothstep. */
    static float sstep(float a, float b, float x) {
        float u = Mth.clamp((x - a) / (b - a), 0f, 1f);
        return u * u * (3f - 2f * u);
    }

    /** 1 while running, ramping 0 -> 1 over the first {@code in} ticks and 1 -> 0 over the last {@code out}. */
    static float life(VfxInstance inst, float age, float in, float out) {
        float a = in <= 0 ? 1 : clamp01(age / in);
        float b = out <= 0 ? 1 : clamp01((inst.duration - age) / out);
        return Math.min(a, b);
    }

    /** Deterministic 0..1 hash (per effect seed and index), for fixed per-piece randomness. */
    static float hash(long seed, int i, int salt) {
        long x = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L + salt * 0x94D049BB133111EBL;
        x ^= x >>> 31; x *= 0x7FB5D329728EA185L; x ^= x >>> 27; x *= 0x81DADEF4BC2DD44DL; x ^= x >>> 33;
        return (x >>> 40) / (float) (1L << 24);
    }

    static float fract(float x) { return x - (float) Math.floor(x); }

    /** A random unit vector (per effect seed, index and salt). */
    static Vector3f unit(long seed, int i, int salt) {
        float u = hash(seed, i, salt) * 2f - 1f, phi = hash(seed, i, salt + 50) * Mth.TWO_PI, s = Mth.sqrt(Math.max(0f, 1f - u * u));
        return new Vector3f(s * Mth.cos(phi), u, s * Mth.sin(phi));
    }

    // ------------------------------------------------------------------ sprites
    /** A camera-facing rectangle (w x h, rolled by {@code roll}); the UV rectangle may pick a cell of an atlas. */
    static void sprite(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, VfxBlend blend, Vector3f c, float w, float h, float roll,
                       float u0, float v0, float u1, float v1, int argb) {
        float cr = Mth.cos(roll), sr = Mth.sin(roll);
        Vector3f rx = new Vector3f(ctx.camRight).mul(cr * w * 0.5f).add(new Vector3f(ctx.camUp).mul(sr * w * 0.5f));
        Vector3f ry = new Vector3f(ctx.camUp).mul(cr * h * 0.5f).sub(new Vector3f(ctx.camRight).mul(sr * h * 0.5f));
        buf.quad(tex, blend, new Vector3f(c).sub(rx).sub(ry), new Vector3f(c).add(rx).sub(ry), new Vector3f(c).add(rx).add(ry), new Vector3f(c).sub(rx).add(ry),
                u0, v0, u1, v1, argb, argb);
    }

    /** A sprite using the whole texture. */
    static void sprite(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, VfxBlend blend, Vector3f c, float w, float h, float roll, int argb) {
        sprite(buf, ctx, tex, blend, c, w, h, roll, 0, 0, 1, 1, argb);
    }

    /** The roll that turns a sprite's "up" (the tip of a drop, the top of a tendril) toward the world direction {@code dir} on screen. */
    static float rollToward(VfxRenderContext ctx, Vector3f dir) {
        float dr = dir.dot(ctx.camRight), du = dir.dot(ctx.camUp);
        if (dr * dr + du * du < 1e-8f) return 0f;
        return (float) Math.atan2(-dr, du);
    }

    /** One candle flame (cell 0 slender, 1 swayed) standing on {@code base}, {@code height} tall, drawn as a camera-facing card. */
    static void flame(VfxVertexBuffer buf, VfxRenderContext ctx, VfxBlend blend, int cell, Vector3f base, float height, float roll, int argb) {
        Vector3f c = new Vector3f(base).add(0, height * 0.46f, 0);
        float u0 = cell * 0.5f + 0.002f;
        sprite(buf, ctx, FLAME, blend, c, height * 0.5f, height, roll, u0, 0f, u0 + 0.496f, 1f, argb);
    }

    /** A flat sprite on a pose plane, spun by {@code spin}. */
    static void flat(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, VfxPose pose, float half, float spin, int argb) {
        buf.plane(tex, blend, pose.spin(spin), half, argb);
    }

    /**
     * A card standing on {@code base} along {@code up} (unit), turned toward the camera (cylindrical billboard); the top is shifted by
     * {@code lean} (the sway of viscous oil). Texture: foot at the image bottom, tip at the top.
     */
    static void standing(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f base, Vector3f up, float height, float halfWidth,
                         Vector3f lean, int cBase, int cTop) {
        Vector3f side = new Vector3f(up).cross(base);
        if (side.lengthSquared() < 1e-8f) side.set(1, 0, 0);
        side.normalize().mul(halfWidth);
        Vector3f top = new Vector3f(up).mul(height).add(lean).add(base);
        buf.quad(tex, blend, new Vector3f(base).sub(side), new Vector3f(base).add(side), new Vector3f(top).add(side), new Vector3f(top).sub(side),
                0, 0, 1, 1, cBase, cTop);
    }

    /**
     * A ribbon through points (camera-relative), turned to face the camera. The texture runs along it: v = 1 (image bottom) at the first
     * point, v = 0 (image top) at the last. {@code widths} and {@code cols} per point.
     */
    static void ribbon(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f[] pts, float[] widths, int[] cols) {
        int n = pts.length;
        if (n < 2) return;
        Vector3f[] s = new Vector3f[n];
        for (int i = 0; i < n; i++) {
            Vector3f tan = new Vector3f(pts[Math.min(i + 1, n - 1)]).sub(pts[Math.max(i - 1, 0)]);
            Vector3f sd = new Vector3f(tan).cross(pts[i]);
            if (sd.lengthSquared() < 1e-9f) sd.set(1, 0, 0);
            s[i] = sd.normalize().mul(widths[i] * 0.5f);
        }
        for (int i = 0; i < n - 1; i++) {
            float vA = 1f - (float) i / (n - 1), vB = 1f - (float) (i + 1) / (n - 1);
            buf.quad(tex, blend, new Vector3f(pts[i]).sub(s[i]), new Vector3f(pts[i]).add(s[i]), new Vector3f(pts[i + 1]).add(s[i + 1]),
                    new Vector3f(pts[i + 1]).sub(s[i + 1]), 0, vB, 1, vA, cols[i], cols[i + 1]);
        }
    }
}
