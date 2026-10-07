package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Shared pieces of the Eyeball Magic layer: the textures (tools/gen_eyeball_textures.py), the palette, and the builders for a whole
 * eyeball (sclera, fibrous iris, pupil, wet glint, lids), optic-nerve tendril ribbons, free quads with their own axes, wall hoops and
 * the roving glance of a floating eye. Everything goes through VfxVertexBuffer.quad, so the per-effect vertex budget still applies.
 */
final class EyeballFx {
    private EyeballFx() {}

    private static ResourceLocation t(String n) { return VfxTextures.byName("eyeball_" + n); }

    static final ResourceLocation SCLERA = t("sclera"), IRIS = t("iris"), PUPIL = t("pupil"), SLIT = t("slit"), GLOSS = t("gloss"),
            CORONA = t("corona"), LID = t("lid"), NERVE = t("nerve"), BEAM = t("beam"), WEB = t("vein_web"), BAND = t("vein_band"),
            RETICLE = t("reticle"), SIGIL = t("sigil"), SCAN = t("scan"), RIPPLE = t("ripple"), WARP = t("warp"), DROP = t("drop"),
            FRAG = t("frag"), MINI = t("mini");

    static final int WHITE = 0xFFFFFFFF;
    /** The glow red of the magic (the owner's glow colour), a hot pink-white for cores, and the maroon flesh of the grimoire cover. */
    static final int BLOOD = 0xFFFF3A3A, HOT = 0xFFFFB4A4, MAROON = 0xFF5A1A2A, DARK_FLESH = 0xFF4A1020, WET_RED = 0xFFC8283C;

    /** The effect's tint mixed with the magic's own blood red, so that it reads as Eyeball Magic even when the tint is white. */
    static int tint(VfxInstance inst) { return VfxVertexBuffer.lerpColor(BLOOD, 0xFF000000 | inst.color, 0.4f); }

    /** Colour with its alpha scaled (clamped to 0..1). */
    static int col(int argb, float k) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(k, 0f, 1f)); }

    static float sq(float x) { return x * x; }

    static int mix(int c0, int c1, float t) { return VfxVertexBuffer.lerpColor(c0, c1, Mth.clamp(t, 0f, 1f)); }

    /** Grey for the NEGATIVE blend (which ignores alpha and works on RGB). */
    static int grey(float v) { int g = Mth.clamp((int) (v * 255f), 0, 255); return 0xFF000000 | (g << 16) | (g << 8) | g; }

    /** Deterministic 0..1 hash (per effect seed and index). */
    static float hash(long seed, int i, int salt) {
        long x = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L + salt * 0x94D049BB133111EBL;
        x ^= x >>> 31; x *= 0x7FB5D329728EA185L; x ^= x >>> 27; x *= 0x81DADEF4BC2DD44DL; x ^= x >>> 33;
        return (x >>> 40) / (float) (1L << 24);
    }

    /** 1 while running, ramping 0 -> 1 over the first {@code in} ticks and 1 -> 0 over the last {@code out}. */
    static float life(VfxInstance inst, float age, float in, float out) {
        float a = in <= 0 ? 1 : Mth.clamp(age / in, 0, 1);
        float b = out <= 0 ? 1 : Mth.clamp((inst.duration - age) / out, 0, 1);
        return Math.min(a, b);
    }

    static float sstep(float e0, float e1, float x) {
        float t = Mth.clamp((x - e0) / (e1 - e0), 0f, 1f);
        return t * t * (3 - 2 * t);
    }

    static float easeOut(float t) { return VfxAnim.easeOutCubic(Mth.clamp(t, 0f, 1f)); }

    static float easeIn(float t) { return VfxAnim.easeInCubic(Mth.clamp(t, 0f, 1f)); }

    /** A heartbeat: two thumps per period, 0..1. */
    static float thump(float age, float period) {
        float ph = (age % period) / period;
        float x1 = (ph - 0.05f) / 0.05f, x2 = (ph - 0.23f) / 0.06f;
        return Math.min(1f, (float) Math.exp(-x1 * x1) + 0.65f * (float) Math.exp(-x2 * x2));
    }

    /** Unit vector perpendicular to dir (horizontal when possible). */
    static Vector3f side(Vector3f dir) {
        Vector3f s = new Vector3f(dir).cross(0, 1, 0);
        if (s.lengthSquared() < 1e-5f) s.set(1, 0, 0);
        return s.normalize();
    }

    /** Unit vector at random on the sphere (from three hashes of one piece). */
    static Vector3f spread(long seed, int i, int salt) {
        float u = hash(seed, i, salt) * 2f - 1f, ph = hash(seed, i, salt + 1) * Mth.TWO_PI, s = Mth.sqrt(Math.max(0f, 1f - u * u));
        return new Vector3f(s * Mth.cos(ph), u, s * Mth.sin(ph));
    }

    // ------------------------------------------------------------------ quads with their own axes

    /** A quad round c spanned by the unit axes right / up with half sizes hw / hh; the texture stands upright along 'up'. */
    static void panel(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f c, Vector3f right, Vector3f up, float hw, float hh, int argb) {
        Vector3f rx = new Vector3f(right).mul(hw), uy = new Vector3f(up).mul(hh);
        int col = blend.grade(argb, 1f);
        buf.quad(tex, blend, new Vector3f(c).sub(rx).sub(uy), new Vector3f(c).add(rx).sub(uy), new Vector3f(c).add(rx).add(uy), new Vector3f(c).sub(rx).add(uy),
                0, 0, 1, 1, col, col);
    }

    /** A camera-facing ribbon through pts (camera-relative): width w0 -> w1, colour c0 -> c1, texture V v0 -> v1 along it, U across. */
    static void ribbon(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f[] pts, float w0, float w1, int c0, int c1, float v0, float v1) {
        int n = pts.length;
        if (n < 2) return;
        Vector3f[] l = new Vector3f[n], r = new Vector3f[n];
        for (int i = 0; i < n; i++) {
            Vector3f dir = new Vector3f(pts[Math.min(n - 1, i + 1)]).sub(pts[Math.max(0, i - 1)]);
            Vector3f toCam = new Vector3f(pts[i]).negate();
            Vector3f s = new Vector3f(dir).cross(toCam);
            if (s.lengthSquared() < 1e-10f) s.set(0, 1, 0);
            s.normalize().mul(0.5f * Mth.lerp((float) i / (n - 1), w0, w1));
            l[i] = new Vector3f(pts[i]).sub(s);
            r[i] = new Vector3f(pts[i]).add(s);
        }
        for (int i = 0; i < n - 1; i++) {
            float f0 = (float) i / (n - 1), f1 = (float) (i + 1) / (n - 1);
            buf.quad(tex, blend, l[i], r[i], r[i + 1], l[i + 1], 0, Mth.lerp(f1, v0, v1), 1, Mth.lerp(f0, v0, v1),
                    blend.grade(VfxVertexBuffer.lerpColor(c0, c1, f0), 0.7f), blend.grade(VfxVertexBuffer.lerpColor(c0, c1, f1), 0.7f));
        }
    }

    /** A hairline from a to b (camera-relative) whose texture repeats every {@code period} blocks and slides by {@code scroll} periods. */
    static void strip(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f a, Vector3f b, float w0, float w1, float period, float scroll, int c0, int c1) {
        Vector3f dir = new Vector3f(b).sub(a);
        float len = dir.length();
        if (len < 1e-4f) return;
        Vector3f toCam = new Vector3f(a).add(b).mul(-0.5f);
        Vector3f s = new Vector3f(dir).cross(toCam);
        if (s.lengthSquared() < 1e-10f) return;
        s.normalize();
        Vector3f s0 = new Vector3f(s).mul(0.5f * w0), s1 = new Vector3f(s).mul(0.5f * w1);
        buf.quad(tex, blend, new Vector3f(a).sub(s0), new Vector3f(a).add(s0), new Vector3f(b).add(s1), new Vector3f(b).sub(s1),
                0, scroll + len / period, 1, scroll, blend.grade(c0, 1f), blend.grade(c1, 1f));
    }

    /**
     * A ribbon standing on a circle (a wall): width along the pose normal, U wraps {@code repeats} times round and scrolls by
     * {@code uScroll}; V runs up (the image bottom at the base).
     */
    static void hoop(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, VfxPose pose, float radius, float halfHeight, int segments,
                     float repeats, float uScroll, int argb) {
        Vector3f n = new Vector3f(pose.normal()).mul(halfHeight);
        int col = blend.grade(argb, 0.6f);
        for (int i = 0; i < segments; i++) {
            float a0 = Mth.TWO_PI * i / segments, a1 = Mth.TWO_PI * (i + 1) / segments;
            float u0 = uScroll + repeats * i / segments, u1 = uScroll + repeats * (i + 1) / segments;
            buf.quad(tex, blend, pose.point(Mth.cos(a0) * radius, Mth.sin(a0) * radius).sub(n), pose.point(Mth.cos(a1) * radius, Mth.sin(a1) * radius).sub(n),
                    pose.point(Mth.cos(a1) * radius, Mth.sin(a1) * radius).add(n), pose.point(Mth.cos(a0) * radius, Mth.sin(a0) * radius).add(n),
                    u0, 0, u1, 1, col, col);
        }
    }

    /** A flat ring sprite: facing the camera when {@code axis} is null, otherwise lying across that axis (a ring the beam passes through). */
    static void ring(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, VfxBlend blend, Vector3f c, Vector3f axis, float size, float rot, int argb) {
        if (axis == null || axis.lengthSquared() < 1e-6f) buf.billboard(ctx, tex, blend, c, size, rot, argb);
        else buf.plane(tex, blend, VfxPose.facing(c, axis).spin(rot), size * 0.5f, argb);
    }

    // ------------------------------------------------------------------ the eyeball

    /**
     * One whole eyeball facing the camera, drawn as layers (back to front): sclera, iris, pupil, optional eyelids (ALPHA), then the
     * wet glint and a hollow corona (ADD).
     *
     * @param c      centre (camera-relative)
     * @param d      diameter of the ball in blocks
     * @param gaze   the direction it looks along (any length; null or zero = straight at the viewer). The iris slides over the ball
     *               to where the gaze meets it and is foreshortened there, so the pupil really roves.
     * @param roll   the turn of the sclera in radians (veins slide round as it rolls); ignored with lids
     * @param dilate pupil size 0 (pin-prick) .. 1 (the whole inner iris)
     * @param slit   true = vertical predator slit, false = round pupil
     * @param open   lid opening 0..1 (only with lids: the aperture and the ball squash with it)
     * @param tint   the iris colour
     * @param a      overall alpha 0..1
     * @param lids   draw the fleshy eyelids round it (an eye opening in the air)
     * @param flare  0..1 extra light: whitens the iris and lights the corona
     */
    static void eye(VfxVertexBuffer buf, VfxRenderContext ctx, Vector3f c, float d, Vector3f gaze, float roll, float dilate, boolean slit, float open,
                    int tint, float a, boolean lids, float flare) {
        if (a <= 0.01f || d <= 0.001f || (lids && open <= 0.03f)) return;
        Vector3f right = ctx.camRight, up = ctx.camUp;
        float R = d * 0.5f, sq = lids ? Math.max(0.10f, open) : 1f;
        float ox = 0, oy = 0;
        if (gaze != null && gaze.lengthSquared() > 1e-8f) {
            Vector3f g = new Vector3f(gaze).normalize();
            ox = g.dot(right);
            oy = g.dot(up);
            float l0 = Mth.sqrt(ox * ox + oy * oy);
            if (l0 > 0.78f) { ox *= 0.78f / l0; oy *= 0.78f / l0; }
        }
        float l = Mth.sqrt(ox * ox + oy * oy), f = Mth.sqrt(Math.max(0.05f, 1f - l * l));
        Vector3f er = l > 1e-4f ? new Vector3f(right).mul(ox / l).add(new Vector3f(up).mul(oy / l)) : new Vector3f(right);
        // the screen axes with the direction the iris slid along foreshortened: the iris becomes an ellipse, the slit leans with it
        Vector3f rp = new Vector3f(right).sub(new Vector3f(er).mul((1 - f) * er.dot(right)));
        Vector3f up2 = new Vector3f(up).sub(new Vector3f(er).mul((1 - f) * er.dot(up)));
        Vector3f ic = new Vector3f(c).add(new Vector3f(right).mul(ox * R)).add(new Vector3f(up).mul(oy * R * sq));

        // sclera: almost white with a breath of the tint
        int sc = VfxVertexBuffer.withAlpha(VfxVertexBuffer.lerpColor(WHITE, tint | 0xFF000000, 0.10f), a);
        if (lids || Math.abs(roll) < 1e-4f) panel(buf, SCLERA, VfxBlend.ALPHA, c, right, up, R, R * sq, sc);
        else {
            Vector3f rr = new Vector3f(right).mul(Mth.cos(roll)).add(new Vector3f(up).mul(Mth.sin(roll)));
            Vector3f ru = new Vector3f(up).mul(Mth.cos(roll)).sub(new Vector3f(right).mul(Mth.sin(roll)));
            panel(buf, SCLERA, VfxBlend.ALPHA, c, rr, ru, R, R, sc);
        }
        // iris
        float hi = d * 0.265f;
        int ir = VfxVertexBuffer.withAlpha(VfxVertexBuffer.whiten(tint | 0xFF000000, Mth.clamp(flare * 0.55f, 0f, 0.8f)), a);
        panel(buf, IRIS, VfxBlend.ALPHA, ic, rp, up2, hi, hi * sq, ir);
        // pupil
        int pc = VfxVertexBuffer.withAlpha(0xFF000000, a);
        if (slit) {
            float hh = hi * (0.34f + 0.60f * dilate), hw = hh * (0.17f + 0.26f * dilate);
            panel(buf, SLIT, VfxBlend.ALPHA, ic, rp, up2, hw, hh * sq, pc);
        } else {
            float hr = hi * (0.20f + 0.50f * dilate);
            panel(buf, PUPIL, VfxBlend.ALPHA, ic, rp, up2, hr, hr * sq, pc);
        }
        // the lids close over everything
        if (lids) panel(buf, LID, VfxBlend.ALPHA, c, right, up, d * 0.94f, d * 0.705f * sq, VfxVertexBuffer.withAlpha(WHITE, a));
        // the wet glint, and the glow round the rim
        panel(buf, GLOSS, VfxBlend.ADD, c, right, up, R, R * sq, VfxVertexBuffer.withAlpha(WHITE, a * (0.62f + 0.3f * flare)));
        if (flare > 0.02f) panel(buf, CORONA, VfxBlend.ADD, c, right, up, d * 1.05f, d * 1.05f * Math.max(0.5f, sq), VfxVertexBuffer.withAlpha(tint | 0xFF000000, a * Mth.clamp(flare, 0f, 1f) * 0.8f));
    }

    /**
     * An optic-nerve tendril hanging or trailing from an eyeball: a ribbon of {@code n} points starting at {@code root}, running along
     * {@code dir} for {@code len} blocks, swaying sideways (camera-right) with a wave that grows towards the tip. 'width' is at the root.
     */
    static void tendril(VfxVertexBuffer buf, VfxRenderContext ctx, Vector3f root, Vector3f dir, float len, float width, int n, float sway, float phase, float age, int argb) {
        Vector3f[] pts = new Vector3f[n];
        for (int i = 0; i < n; i++) {
            float q = (float) i / (n - 1);
            Vector3f p = new Vector3f(root).add(new Vector3f(dir).mul(len * q));
            p.add(new Vector3f(ctx.camRight).mul(sway * len * q * Mth.sin(phase + age * 0.17f + q * 3.6f)));
            p.add(new Vector3f(ctx.camUp).mul(0.5f * sway * len * q * Mth.sin(phase * 1.7f + age * 0.13f + q * 2.8f)));
            pts[i] = p;
        }
        ribbon(buf, NERVE, VfxBlend.ALPHA, pts, width, width * 0.12f, argb, argb, 0f, 1f);
    }

    /**
     * Where a floating eye looks at {@code age}: it glances from one spot on or round the field to the next every 12 to 28 ticks,
     * easing over a few ticks (the roving pupil). Returns a point (camera-relative).
     */
    static Vector3f glance(long seed, int i, float age, Vector3f centre, float radius) {
        float period = 12f + 16f * hash(seed, i, 71), ph = hash(seed, i, 72) * period;
        float x = (age + ph) / period;
        int q = Mth.floor(x);
        float f = sstep(0f, 0.2f, x - q);
        Vector3f s0 = spot(seed, i, q - 1, centre, radius), s1 = spot(seed, i, q, centre, radius);
        return s0.lerp(s1, f);
    }

    private static Vector3f spot(long seed, int i, int q, Vector3f centre, float radius) {
        float ang = hash(seed, i * 131 + q, 73) * Mth.TWO_PI;
        float rr = radius * (hash(seed, i * 131 + q, 74) < 0.45f ? 0.12f : 0.25f + 0.75f * hash(seed, i * 131 + q, 75));
        return new Vector3f(centre).add(Mth.cos(ang) * rr, 0.4f, Mth.sin(ang) * rr);
    }
}
