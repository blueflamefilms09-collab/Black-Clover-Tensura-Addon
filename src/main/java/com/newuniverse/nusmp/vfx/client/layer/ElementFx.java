package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Shared geometry for the element spell layers (Fire, Water, Wind, Earth): ribbons standing on circles, camera-facing streaks
 * whose texture runs along their length, and lit 3D stone (spikes and slabs). Everything goes through VfxVertexBuffer.quad, so
 * the per-effect vertex budget still applies.
 */
public final class ElementFx {
    private ElementFx() {}

    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    // fire
    public static final ResourceLocation FIRE_TONGUE = t("fire_tongue");
    public static final ResourceLocation FIRE_BAND = t("fire_band");
    public static final ResourceLocation FIRE_RIBBON = t("fire_ribbon");
    public static final ResourceLocation FIRE_LION = t("fire_lion");
    // water
    public static final ResourceLocation WATER_BAND = t("water_band");
    public static final ResourceLocation WATER_FLOW = t("water_flow");
    public static final ResourceLocation WATER_SPHERE = t("water_sphere");
    public static final ResourceLocation WATER_DROP = t("water_drop");
    public static final ResourceLocation WATER_DRAGON = t("water_dragon");
    // wind
    public static final ResourceLocation WIND_STREAK = t("wind_streak");
    public static final ResourceLocation WIND_BAND = t("wind_band");
    public static final ResourceLocation WIND_SWALLOW = t("wind_swallow");
    public static final ResourceLocation WIND_LEAF = t("wind_leaf");
    // earth
    public static final ResourceLocation EARTH_ROCK = t("earth_rock");
    public static final ResourceLocation EARTH_DUST = t("earth_dust");
    public static final ResourceLocation EARTH_CRACK = t("earth_crack");
    public static final ResourceLocation EARTH_CHUNK = t("earth_chunk");

    public static final int WHITE = 0xFFFFFFFF;
    /** Light direction for shading stone faces (from above, a little to the side). */
    private static final Vector3f LIGHT = new Vector3f(0.35f, 0.85f, -0.4f).normalize();

    /** 1 while running, ramping 0 -> 1 over the first {@code in} ticks and 1 -> 0 over the last {@code out}. */
    public static float life(VfxInstance inst, float age, float in, float out) {
        float a = in <= 0 ? 1 : Mth.clamp(age / in, 0, 1);
        float b = out <= 0 ? 1 : Mth.clamp((inst.duration - age) / out, 0, 1);
        return Math.min(a, b);
    }

    /** Deterministic 0..1 hash (per effect seed and index), for picking fixed per-piece randomness without a RandomSource. */
    public static float hash(long seed, int i, int salt) {
        long x = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L + salt * 0x94D049BB133111EBL;
        x ^= x >>> 31; x *= 0x7FB5D329728EA185L; x ^= x >>> 27; x *= 0x81DADEF4BC2DD44DL; x ^= x >>> 33;
        return (x >>> 40) / (float) (1L << 24);
    }

    /** Unit vector perpendicular to dir (horizontal when possible). */
    public static Vector3f side(Vector3f dir) {
        Vector3f s = new Vector3f(dir).cross(0, 1, 0);
        if (s.lengthSquared() < 1e-5f) s.set(1, 0, 0);
        return s.normalize();
    }

    /**
     * A ribbon standing on a circle (like a ring around a planet): width along the pose normal, U wraps {@code repeats} times
     * around and scrolls by {@code uScroll}; V runs across (v=1 at the bottom edge, v=0 at the top).
     */
    public static void hoop(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, VfxPose pose, float radius, float halfWidth,
                            int segments, float repeats, float uScroll, int argb) {
        hoop(buf, tex, blend, pose, radius, radius, halfWidth, segments, repeats, uScroll, argb);
    }

    /** Hoop whose bottom and top edges can have different radii (a funnel / cone section). */
    public static void hoop(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, VfxPose pose, float rBottom, float rTop, float halfWidth,
                            int segments, float repeats, float uScroll, int argb) {
        Vector3f n = new Vector3f(pose.normal()).mul(halfWidth);
        int col = blend.grade(argb, 0.6f);
        for (int i = 0; i < segments; i++) {
            float a0 = Mth.TWO_PI * i / segments, a1 = Mth.TWO_PI * (i + 1) / segments;
            float c0 = Mth.cos(a0), s0 = Mth.sin(a0), c1 = Mth.cos(a1), s1 = Mth.sin(a1);
            float u0 = uScroll + repeats * i / segments, u1 = uScroll + repeats * (i + 1) / segments;
            buf.quad(tex, blend,
                    pose.point(c0 * rBottom, s0 * rBottom).sub(n), pose.point(c1 * rBottom, s1 * rBottom).sub(n),
                    pose.point(c1 * rTop, s1 * rTop).add(n), pose.point(c0 * rTop, s0 * rTop).add(n),
                    u0, 0, u1, 1, col, col);
        }
    }

    /** Camera-facing streak from a to b whose texture's U runs along the length (a = u0, b = u1). */
    public static void streak(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, VfxBlend blend, Vector3f a, Vector3f b,
                              float width, int argb) {
        Vector3f dir = new Vector3f(b).sub(a);
        if (dir.lengthSquared() < 1e-6f) return;
        Vector3f mid = new Vector3f(a).add(b).mul(0.5f);
        Vector3f toCam = new Vector3f(mid).negate();
        Vector3f s = new Vector3f(dir).cross(toCam);
        if (s.lengthSquared() < 1e-8f) return;
        s.normalize().mul(width * 0.5f);
        int col = blend.grade(argb, 0.8f);
        buf.quad(tex, blend, new Vector3f(a).sub(s), new Vector3f(b).sub(s), new Vector3f(b).add(s), new Vector3f(a).add(s), 0, 0, 1, 1, col, col);
    }

    /** A flat strip lying on the ground from a to b (cracks), U along its length. */
    public static void groundStrip(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f a, Vector3f b, float width,
                                   float u0, float u1, int argb) {
        Vector3f dir = new Vector3f(b).sub(a);
        if (dir.lengthSquared() < 1e-6f) return;
        Vector3f s = side(dir.normalize()).mul(width * 0.5f);
        buf.quad(tex, blend, new Vector3f(a).sub(s), new Vector3f(b).sub(s), new Vector3f(b).add(s), new Vector3f(a).add(s), u0, 0, u1, 1, argb, argb);
    }

    /** Billboard whose texture "up" points along screen angle {@code angle} (0 = screen right, PI/2 = up). */
    public static void pointed(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, VfxBlend blend, Vector3f center,
                               float size, float angle, int argb) {
        buf.billboard(ctx, tex, blend, center, size, angle - Mth.HALF_PI, argb);
    }

    /** Shade factor (0.5..1) for a stone face with this outward normal. */
    public static float shade(Vector3f normal) {
        float d = new Vector3f(normal).normalize().dot(LIGHT);
        return 0.5f + 0.5f * Math.max(0f, d);
    }

    public static int mulRgb(int argb, float f) {
        int r = Mth.clamp((int) (((argb >> 16) & 255) * f), 0, 255), g = Mth.clamp((int) (((argb >> 8) & 255) * f), 0, 255);
        int b = Mth.clamp((int) ((argb & 255) * f), 0, 255);
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    /** Four-sided stone spike: square base (yawed) on the ground at {@code base}, apex at {@code tip}. 16 vertices. */
    public static void spike(VfxVertexBuffer buf, Vector3f base, Vector3f tip, float radius, float yaw, int argb) {
        Vector3f[] c = new Vector3f[4];
        for (int i = 0; i < 4; i++) {
            float a = yaw + Mth.HALF_PI * i;
            c[i] = new Vector3f(base).add(Mth.cos(a) * radius, 0, Mth.sin(a) * radius);
        }
        float h = new Vector3f(tip).sub(base).length();
        for (int i = 0; i < 4; i++) {
            Vector3f p0 = c[i], p1 = c[(i + 1) % 4];
            Vector3f n = new Vector3f(p1).sub(p0).cross(new Vector3f(tip).sub(p0));
            if (n.dot(new Vector3f(p0).add(p1).mul(0.5f).sub(base)) < 0) n.negate();
            int col = mulRgb(argb, shade(n));
            float v = Math.min(1f, h * 0.5f);
            buf.quad(EARTH_ROCK, VfxBlend.ALPHA, p0, p1, new Vector3f(tip), new Vector3f(tip), i * 0.25f, 0, i * 0.25f + 0.5f, v, col, col);
        }
    }

    /**
     * A stone slab pushed out of the ground: {@code along} x {@code out} footprint, height {@code h}, leaning outward by
     * {@code tilt} radians. Draws the top, both long sides and both ends (20 vertices).
     */
    public static void slab(VfxVertexBuffer buf, Vector3f base, Vector3f along, Vector3f out, float halfLen, float halfWid, float h, float tilt, int argb) {
        Vector3f up = new Vector3f(0, Mth.cos(tilt), 0).add(new Vector3f(out).mul(Mth.sin(tilt)));
        Vector3f o = new Vector3f(out).mul(Mth.cos(tilt)).add(0, -Mth.sin(tilt), 0);
        Vector3f[] p = new Vector3f[8];
        int k = 0;
        for (int y = 0; y < 2; y++) for (int w = -1; w <= 1; w += 2) for (int l = -1; l <= 1; l += 2) {
            p[k++] = new Vector3f(base).add(new Vector3f(along).mul(l * halfLen)).add(new Vector3f(o).mul(w * halfWid)).add(new Vector3f(up).mul(y * h));
        }
        // indices: y*4 + (w==1)*2 + (l==1)
        face(buf, p[4], p[5], p[7], p[6], up, argb, 1f);                 // top
        face(buf, p[2], p[3], p[7], p[6], o, argb, h);                   // outer side
        face(buf, p[1], p[0], p[4], p[5], new Vector3f(o).negate(), argb, h);   // inner side
        face(buf, p[3], p[1], p[5], p[7], along, argb, h);               // +end
        face(buf, p[0], p[2], p[6], p[4], new Vector3f(along).negate(), argb, h);  // -end
    }

    private static void face(VfxVertexBuffer buf, Vector3f a, Vector3f b, Vector3f c, Vector3f d, Vector3f normal, int argb, float v) {
        int col = mulRgb(argb, shade(normal));
        buf.quad(EARTH_ROCK, VfxBlend.ALPHA, a, b, c, d, 0, 0, 1, Math.min(1f, v * 0.5f), col, col);
    }
}
