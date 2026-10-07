package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Shared pieces of the Curse Magic effects: the textures (tools/gen_curse_textures.py), the palette and a few geometry helpers.
 * Every texture is read in two passes: ALPHA in near-black (the cursed mass) and ADD in violet (only the bright strokes, cracks
 * and rims light up), so one sprite gives a black body with light in it. Everything goes through VfxVertexBuffer.quad, so the
 * per-effect vertex budget still applies.
 */
final class CurseFx {
    private CurseFx() {}

    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final ResourceLocation SIGIL = t("curse_sigil"), BRAND = t("curse_brand"), GLYPHS = t("curse_glyphs"), BAND = t("curse_band"),
            VEINS = t("curse_veins"), WALL = t("curse_wall"), BOLT = t("curse_bolt"), TENDRIL = t("curse_tendril"), SMOKE = t("curse_smoke"),
            FLAKES = t("curse_flakes"), FLASH = t("curse_flash"), RING = t("curse_ring"), WISP = t("curse_wisp"), SHARDS = t("curse_shards");

    /** The palette: black with a violet cast, the owner's glow violet, an orchid rim, a white-violet core, a magenta accent. */
    static final int INK = 0xFF060209, SHADE = 0xFF2E1A46, DEEP = 0xFF4B158F, VIOLET = 0xFF9A2AFF, ORCHID = 0xFFC864FF,
            HOT = 0xFFF4E4FF, MAGENTA = 0xFFE0309A;

    /** The palette colour pulled a little toward the spell's own tint (so a white tint still reads as this magic). */
    static int mix(int palette, int tint, float amount) {
        return VfxVertexBuffer.lerpColor(palette | 0xFF000000, tint | 0xFF000000, amount);
    }

    /** 0..1 smoothstep. */
    static float sstep(float a, float b, float x) {
        float u = Mth.clamp((x - a) / (b - a), 0f, 1f);
        return u * u * (3f - 2f * u);
    }

    static float clamp01(float x) { return Mth.clamp(x, 0f, 1f); }

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

    /** Fraction part. */
    static float fract(float x) { return x - (float) Math.floor(x); }

    /** A random unit vector (per effect seed, index and salt). */
    static Vector3f unit(long seed, int i, int salt) {
        float u = hash(seed, i, salt) * 2f - 1f, phi = hash(seed, i, salt + 50) * Mth.TWO_PI, s = Mth.sqrt(Math.max(0f, 1f - u * u));
        return new Vector3f(s * Mth.cos(phi), u, s * Mth.sin(phi));
    }

    /** Unit vector perpendicular to dir (horizontal when possible). */
    static Vector3f side(Vector3f dir) {
        Vector3f s = new Vector3f(dir).cross(0, 1, 0);
        if (s.lengthSquared() < 1e-5f) s.set(1, 0, 0);
        return s.normalize();
    }

    /** Scales the RGB of a colour (for NEGATIVE blending, where alpha does not fade anything). */
    static int dim(int argb, float f) {
        int r = Mth.clamp((int) (((argb >> 16) & 255) * f), 0, 255), g = Mth.clamp((int) (((argb >> 8) & 255) * f), 0, 255);
        int b = Mth.clamp((int) ((argb & 255) * f), 0, 255);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    // ------------------------------------------------------------------ sprites
    /** A camera-facing rectangle (w x h, rolled by {@code roll}); the UV rectangle picks a cell of an atlas. */
    static void sprite(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, VfxBlend blend, Vector3f c, float w, float h, float roll,
                       float u0, float v0, float u1, float v1, int argb) {
        float cr = Mth.cos(roll), sr = Mth.sin(roll);
        Vector3f rx = new Vector3f(ctx.camRight).mul(cr * w * 0.5f).add(new Vector3f(ctx.camUp).mul(sr * w * 0.5f));
        Vector3f ry = new Vector3f(ctx.camUp).mul(cr * h * 0.5f).sub(new Vector3f(ctx.camRight).mul(sr * h * 0.5f));
        buf.quad(tex, blend, new Vector3f(c).sub(rx).sub(ry), new Vector3f(c).add(rx).sub(ry), new Vector3f(c).add(rx).add(ry), new Vector3f(c).sub(rx).add(ry),
                u0, v0, u1, v1, argb, argb);
    }

    /** A tall (or wide) camera-facing sprite using the whole texture. */
    static void tall(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, VfxBlend blend, Vector3f c, float w, float h, float roll, int argb) {
        sprite(buf, ctx, tex, blend, c, w, h, roll, 0, 0, 1, 1, argb);
    }

    /** One cell of a square atlas with {@code cols} x {@code cols} cells, as a camera-facing square. */
    static void cell(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, int cols, int index, VfxBlend blend, Vector3f c, float size,
                     float roll, int argb) {
        float cs = 1f / cols, inset = 0.004f;
        float u0 = (index % cols) * cs + inset, v0 = (index / cols) * cs + inset;
        sprite(buf, ctx, tex, blend, c, size, size, roll, u0, v0, u0 + cs - 2 * inset, v0 + cs - 2 * inset, argb);
    }

    /**
     * A vertical card standing on the ground at {@code base} that always turns toward the camera (a cylindrical billboard): thorns,
     * pillars. {@code cBase} colours the foot, {@code cTop} the tip. The UV rectangle may be one cell of an atlas.
     */
    static void standing(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f base, float height, float halfWidth,
                         float u0, float v0, float u1, float v1, int cBase, int cTop) {
        Vector3f f = new Vector3f(base.x, 0, base.z);
        if (f.lengthSquared() < 1e-6f) f.set(0, 0, 1);
        f.normalize();
        Vector3f r = new Vector3f(f.z, 0, -f.x).mul(halfWidth);
        Vector3f lo0 = new Vector3f(base).sub(r), lo1 = new Vector3f(base).add(r);
        buf.quad(tex, blend, lo0, lo1, new Vector3f(lo1).add(0, height, 0), new Vector3f(lo0).add(0, height, 0), u0, v0, u1, v1, cBase, cTop);
    }

    /** A thorn / fang card from the shard atlas (cell 0 = tall thorn, 1 = fang cluster). */
    static void thorn(VfxVertexBuffer buf, VfxBlend blend, int cell, Vector3f base, float height, int cBase, int cTop) {
        float cs = 0.5f, inset = 0.004f;
        float u0 = (cell % 2) * cs + inset, v0 = (cell / 2) * cs + inset;
        standing(buf, SHARDS, blend, base, height * 1.03f, height * 0.52f, u0, v0, u0 + cs - 2 * inset, v0 + cs - 2 * inset, cBase, cTop);
    }

    // ------------------------------------------------------------------ ribbons and rings
    /**
     * A ribbon through points (camera-relative), turned to face the camera. The texture runs along it: v = 1 (image bottom) at the first
     * point, v = 0 (image top) at the last, so the bolt / tendril sprites sit tail- / base-first. {@code widths} and {@code cols} per point.
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

    /**
     * A ribbon standing on a circle (a wall): width along the pose normal, U wraps {@code repeats} times around and scrolls by {@code uScroll};
     * the bottom edge has radius {@code rBottom}, the top edge {@code rTop} (a leaning wall).
     */
    static void hoop(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, VfxPose pose, float rBottom, float rTop, float halfWidth,
                     int segments, float repeats, float uScroll, int argb) {
        Vector3f n = new Vector3f(pose.normal()).mul(halfWidth);
        for (int i = 0; i < segments; i++) {
            float a0 = Mth.TWO_PI * i / segments, a1 = Mth.TWO_PI * (i + 1) / segments;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            float u0 = uScroll + repeats * i / segments, u1 = uScroll + repeats * (i + 1) / segments;
            buf.quad(tex, blend,
                    pose.point(c0 * rBottom, s0 * rBottom).sub(n), pose.point(c1 * rBottom, s1 * rBottom).sub(n),
                    pose.point(c1 * rTop, s1 * rTop).add(n), pose.point(c0 * rTop, s0 * rTop).add(n),
                    u0, 0, u1, 1, argb, argb);
        }
    }

    /** A plane's half size that puts the jagged ring of curse_ring (drawn at 0.80 of the half size) at radius {@code r}. */
    static float ringHalf(float r) { return r / 0.80f; }

    /** A dark pass then a light pass of the same card (billboard). */
    static void inkGlow(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, Vector3f c, float size, float roll, int ink, int glow) {
        buf.billboard(ctx, tex, VfxBlend.ALPHA, c, size * 1.04f, roll, ink);
        buf.billboard(ctx, tex, VfxBlend.ADD, c, size, roll, glow);
    }

    /** A dark pass then a light pass of the same card lying on a plane. */
    static void inkGlow(VfxVertexBuffer buf, ResourceLocation tex, VfxPose pose, float half, int ink, int glow) {
        buf.plane(tex, VfxBlend.ALPHA, pose, half * 1.03f, ink);
        buf.plane(tex, VfxBlend.ADD, pose.lift(0.004f), half, glow);
    }
}
