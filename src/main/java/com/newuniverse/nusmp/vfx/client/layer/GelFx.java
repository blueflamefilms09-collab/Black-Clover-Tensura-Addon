package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Shared pieces of the Gel Magic effects: the textures (tools/gen_gel_textures.py), the palette and a few geometry helpers.
 * Every jelly body sprite has a twin "_hi" sprite: the body is drawn ALPHA in the jelly tint (denser and more opaque at the rim,
 * clear in the middle) and the twin is drawn ADD on top (rim light, window highlight, caustic net, bubble glints), so one jelly is
 * two quads. Everything goes through VfxVertexBuffer.quad, so the per-effect vertex budget still applies.
 */
final class GelFx {
    private GelFx() {}

    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final ResourceLocation BLOB = t("gel_blob"), BLOB_HI = t("gel_blob_hi"), PUDDLE = t("gel_puddle"), PUDDLE_HI = t("gel_puddle_hi"),
            SPLAT = t("gel_splat"), SPLAT_HI = t("gel_splat_hi"), SALA = t("gel_salamander"), SALA_HI = t("gel_salamander_hi"),
            RING = t("gel_ring"), STRAND = t("gel_strand"), DROPS = t("gel_drops"), BUBBLES = t("gel_bubbles"), WALL = t("gel_wall"),
            GLINT = t("gel_glint");

    /** The palette: a mint jelly with a teal depth, a glass-white light and a faint lime for the sparks inside it. */
    static final int JELLY = 0xFF62E4CC, DEEP = 0xFF1C8C94, MINT = 0xFF7AFFD8, GLASS = 0xFFDCFFF4, LIME = 0xFFC4FF9C;

    /** The palette colour pulled a little toward the spell's own tint (so a white tint still reads as this magic). */
    static int mix(int palette, int tint, float amount) {
        return VfxVertexBuffer.lerpColor(palette | 0xFF000000, tint | 0xFF000000, amount);
    }

    /** The colours of one instance. */
    static final class Pal {
        final int body, hi, glow, deep, lime;
        Pal(VfxInstance inst) {
            body = mix(JELLY, inst.color, 0.30f);
            hi = mix(GLASS, inst.color, 0.18f);
            glow = mix(MINT, inst.color, 0.30f);
            deep = mix(DEEP, inst.color, 0.30f);
            lime = mix(LIME, inst.color, 0.15f);
        }
    }

    static int al(int argb, float a) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(a, 0f, 1f)); }

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

    /** Jelly jiggle: a damped-looking wobble in -1..1 (two beats that do not line up). */
    static float jig(float age, float phase) {
        return 0.65f * Mth.sin(age * 1.35f + phase) + 0.35f * Mth.sin(age * 2.3f + phase * 1.7f + 1f);
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

    /** A jelly: the body ALPHA in {@code body}, its light ADD in {@code hi} (2 quads). */
    static void jelly(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, ResourceLocation texHi, Vector3f c, float w, float h, float roll,
                      int body, int hi) {
        sprite(buf, ctx, tex, VfxBlend.ALPHA, c, w, h, roll, 0, 0, 1, 1, body);
        sprite(buf, ctx, texHi, VfxBlend.ADD, c, w, h, roll, 0, 0, 1, 1, hi);
    }

    /** A rectangle lying on a pose plane (hw x hh half sizes), picture "up" along the pose's up. */
    static void flat(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, VfxPose pose, float hw, float hh, int argb) {
        buf.quad(tex, blend, pose.point(-hw, -hh), pose.point(hw, -hh), pose.point(hw, hh), pose.point(-hw, hh), 0, 0, 1, 1, argb, argb);
    }

    /** A jelly lying on a pose plane (2 quads). */
    static void jellyFlat(VfxVertexBuffer buf, ResourceLocation tex, ResourceLocation texHi, VfxPose pose, float hw, float hh, int body, int hi) {
        flat(buf, tex, VfxBlend.ALPHA, pose, hw, hh, body);
        flat(buf, texHi, VfxBlend.ADD, pose.lift(0.004f), hw, hh, hi);
    }

    /** One drop of the drops atlas (4 cells of 64 x 128: teardrop, bead, falling drop, double drop; tips point up); {@code size} = its height. */
    static void drop(VfxVertexBuffer buf, VfxRenderContext ctx, VfxBlend blend, int cell, Vector3f c, float size, float roll, int argb) {
        float u0 = (cell & 3) * 0.25f + 0.004f;
        sprite(buf, ctx, DROPS, blend, c, size * 0.5f, size, roll, u0, 0.004f, u0 + 0.25f - 0.008f, 0.996f, argb);
    }

    /** One bubble cell (2 x 2 atlas of 128). */
    static void bubble(VfxVertexBuffer buf, VfxRenderContext ctx, VfxBlend blend, int cell, Vector3f c, float size, float roll, int argb) {
        float u0 = (cell & 1) * 0.5f + 0.004f, v0 = ((cell >> 1) & 1) * 0.5f + 0.004f;
        sprite(buf, ctx, BUBBLES, blend, c, size, size, roll, u0, v0, u0 + 0.492f, v0 + 0.492f, argb);
    }

    /** A drop that flies along screen direction (vx, vy): its tip trails behind it. */
    static float tipRoll(VfxRenderContext ctx, Vector3f vel) {
        float vx = vel.dot(ctx.camRight), vy = vel.dot(ctx.camUp);
        if (vx * vx + vy * vy < 1e-6f) return 0f;
        return (float) Math.atan2(-vy, -vx) - Mth.HALF_PI;
    }

    // ------------------------------------------------------------------ ribbons and hoops
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

    /**
     * A ribbon standing on a circle (a wall): width along the pose normal, U wraps {@code repeats} times around and scrolls by {@code uScroll};
     * the bottom edge has radius {@code rBottom}, the top edge {@code rTop} (a leaning wall). The picture's bottom is at the pose's -normal side.
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

    /** A plane's half size that puts the ridge of gel_ring (drawn at 0.80 of the half size) at radius {@code r}. */
    static float ringHalf(float r) { return r / 0.80f; }

    /** A pose lying on the ground at {@code c} whose right axis points along the horizontal direction (dx, dz); right x up = up-normal. */
    static VfxPose groundHeading(Vector3f c, float dx, float dz) {
        float l = Mth.sqrt(dx * dx + dz * dz);
        if (l < 1e-5f) return VfxPose.ground(c);
        Vector3f right = new Vector3f(dx / l, 0, dz / l);
        Vector3f up = new Vector3f(dz / l, 0, -dx / l);
        return new VfxPose(new Vector3f(c), right, up, new Vector3f(0, 1, 0));
    }
}
