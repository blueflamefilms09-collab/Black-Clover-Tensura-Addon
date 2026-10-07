package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Shared pieces of the Demon Ice Magic effects: the textures (tools/gen_demon_ice_textures.py), the palette (black glass with
 * violet-blue light) and the little quad builders the three shapes are made of. Everything goes through VfxVertexBuffer.quad, so the
 * 400-vertex budget of an activation still applies.
 * <p>
 * Texture conventions: the crystal sprites (lance, crystal, shards, wall) are grey-scale where the grey is the facet shade. Draw
 * them with ALPHA and BODY for the black glass, then again with ADD and a blue / violet tint: only seams and edges light up.
 */
final class DemonIceFx {
    private DemonIceFx() {}

    private static ResourceLocation t(String n) { return VfxTextures.byName("demon_ice_" + n); }

    static final ResourceLocation LANCE = t("lance"), SPRAY = t("spray"), CRYSTAL = t("crystal"), SHARDS = t("shards"), WALL = t("wall"),
            RUNES = t("runes"), SIGIL = t("sigil"), FLAKE = t("flake"), CRACKS = t("cracks"), MIST = t("mist"), BURST = t("burst"),
            RING = t("ring"), GLINT = t("glint"), INVERT = t("invert");

    /** The magic's palette. GLOW is the owner's light colour (royal blue); the tint of a cast is pulled toward it, never replaces it. */
    static final int GLOW = 0xFF3A6AFF, VIOLET = 0xFF8A50FF, BODY = 0xFF27316E, INK = 0xFF060814, FROST = 0xFFBFD6FF;
    static final int SHARD_CELLS = 4;

    /** The spell's tint mixed with the palette (35 % tint): white gives a pale blue, red a violet, the default exactly GLOW. */
    static int glow(int tint) { return VfxVertexBuffer.lerpColor(GLOW, 0xFF000000 | (tint & 0xFFFFFF), 0.35f); }
    /** The seam / edge light: the glow pushed toward violet. */
    static int edge(int glow) { return VfxVertexBuffer.lerpColor(glow, VIOLET, 0.35f); }
    static int hot(int glow) { return VfxVertexBuffer.whiten(glow, 0.7f); }
    static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(alpha, 0f, 1f)); }
    /** A colour scaled in brightness only (for the NEGATIVE mask, whose RGB is the strength). */
    static int dim(int argb, float k) {
        int r = Mth.clamp((int) (((argb >> 16) & 255) * k), 0, 255), g = Mth.clamp((int) (((argb >> 8) & 255) * k), 0, 255), b = Mth.clamp((int) ((argb & 255) * k), 0, 255);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    static float hash(long seed, int i, int salt) { return ElementFx.hash(seed, i, salt); }
    static float life(VfxInstance inst, float age, float in, float out) { return ElementFx.life(inst, age, in, out); }
    static float ease(float t) { return VfxAnim.easeOutCubic(Mth.clamp(t, 0f, 1f)); }
    static float smooth(float a, float b, float x) { float t = Mth.clamp((x - a) / (b - a), 0f, 1f); return t * t * (3 - 2 * t); }

    /** A quad in an arbitrary plane: centre c, unit axes right / up, half sizes hx / hy. The image stands upright along up. */
    static void panel(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f c, Vector3f right, Vector3f up, float hx, float hy, int col) {
        Vector3f rx = new Vector3f(right).mul(hx), uy = new Vector3f(up).mul(hy);
        int k = bl.grade(col, 1f);
        buf.quad(tex, bl, new Vector3f(c).sub(rx).sub(uy), new Vector3f(c).add(rx).sub(uy), new Vector3f(c).add(rx).add(uy), new Vector3f(c).sub(rx).add(uy), 0, 0, 1, 1, k, k);
    }

    /** One cell of a horizontal atlas (n cells), camera facing, rotated by rot like VfxVertexBuffer.billboard. */
    static void cell(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, VfxBlend bl, Vector3f c, float size, float rot, int k, int n, int col) {
        float cs = Mth.cos(rot) * size * 0.5f, sn = Mth.sin(rot) * size * 0.5f;
        Vector3f rx = new Vector3f(ctx.camRight).mul(cs).add(new Vector3f(ctx.camUp).mul(sn));
        Vector3f ry = new Vector3f(ctx.camUp).mul(cs).sub(new Vector3f(ctx.camRight).mul(sn));
        int q = bl.grade(col, 1f);
        buf.quad(tex, bl, new Vector3f(c).sub(rx).sub(ry), new Vector3f(c).add(rx).sub(ry), new Vector3f(c).add(rx).add(ry), new Vector3f(c).sub(rx).add(ry),
                k / (float) n, 0, (k + 1) / (float) n, 1, q, q);
    }

    /** Camera-facing strip from a (image bottom, width wa) to b (image top, width wb). */
    static void strip(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f a, Vector3f b, float wa, float wb, int ca, int cb) {
        Vector3f dir = new Vector3f(b).sub(a);
        if (dir.lengthSquared() < 1e-8f) return;
        Vector3f mid = new Vector3f(a).add(b).mul(0.5f);
        Vector3f side = new Vector3f(dir).cross(new Vector3f(mid).negate());
        if (side.lengthSquared() < 1e-10f) return;
        stripSide(buf, tex, bl, a, b, side.normalize(), wa, wb, ca, cb);
    }

    /** Strip with a given unit side vector (a real 3D plane, for the crossing second plane of a spinning body). */
    static void stripSide(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f a, Vector3f b, Vector3f side, float wa, float wb, int ca, int cb) {
        Vector3f sa = new Vector3f(side).mul(wa * 0.5f), sb = new Vector3f(side).mul(wb * 0.5f);
        buf.quad(tex, bl, new Vector3f(a).sub(sa), new Vector3f(a).add(sa), new Vector3f(b).add(sb), new Vector3f(b).sub(sb), 0, 0, 1, 1, bl.grade(ca, 1f), bl.grade(cb, 1f));
    }

    /** A card standing on the ground at base (image bottom), turning about the vertical axis to face the camera; leans by lean blocks at the top. */
    static void stand(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f base, float halfW, float height, float lean, int col) {
        Vector3f r = new Vector3f(-base.z, 0, base.x);
        if (r.lengthSquared() < 1e-6f) r.set(1, 0, 0);
        r.normalize();
        Vector3f lo = new Vector3f(r).mul(halfW), top = new Vector3f(base).add(0, height, 0).add(new Vector3f(r).mul(lean));
        int k = bl.grade(col, 1f);
        buf.quad(tex, bl, new Vector3f(base).sub(lo), new Vector3f(base).add(lo), new Vector3f(top).add(lo), new Vector3f(top).sub(lo), 0, 0, 1, 1, k, k);
    }

    /** A wall standing on a circle round c: bottom edge at y0, height h, the texture wraps repeats times and scrolls by scroll. */
    static void hoop(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f c, float radius, float y0, float h, int segs, float repeats, float scroll, int col, int colTop) {
        int k0 = bl.grade(col, 0.6f), k1 = bl.grade(colTop, 0.6f);
        for (int i = 0; i < segs; i++) {
            float a0 = Mth.TWO_PI * i / segs, a1 = Mth.TWO_PI * (i + 1) / segs;
            float u0 = scroll + repeats * i / segs, u1 = scroll + repeats * (i + 1) / segs;
            Vector3f b0 = new Vector3f(c).add(Mth.cos(a0) * radius, y0, Mth.sin(a0) * radius), b1 = new Vector3f(c).add(Mth.cos(a1) * radius, y0, Mth.sin(a1) * radius);
            buf.quad(tex, bl, b0, b1, new Vector3f(b1).add(0, h, 0), new Vector3f(b0).add(0, h, 0), u0, 0, u1, 1, k0, k1);
        }
    }

    /** A flat sprite lying on the ground (or any facing) at c, spun, drawn to the given radius. */
    static void flat(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f c, float lift, float spin, float radius, int col) {
        buf.plane(tex, bl, VfxPose.ground(c).spin(spin).lift(lift), radius, col);
    }

    /** A disc facing along dir. */
    static void facing(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f c, Vector3f dir, float spin, float radius, int col) {
        buf.plane(tex, bl, VfxPose.facing(c, dir).spin(spin), radius, col);
    }
}
