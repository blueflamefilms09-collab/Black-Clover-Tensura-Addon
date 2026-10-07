package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Shared pieces of the Ice Magic effects: the textures (tools/gen_icefx_textures.py), the palette (pale cyan, white, deep blue) and the
 * little quad builders the three shapes are made of. Everything goes through VfxVertexBuffer.quad, so the 400-vertex budget of an
 * activation still applies.
 * <p>
 * Texture conventions: the crystal sprites (fang, spike, crystal, shards, wall) are grey-scale where the grey is the facet shade and the
 * alpha is lower inside the ice than on its edges. The ice* builders draw them twice: ALPHA in a blue body tint (clear glass with
 * dark blue shadow facets) and ADD in pale cyan / white (only the lit facets and the edges flare).
 */
final class IceFx {
    private IceFx() {}

    private static ResourceLocation t(String n) { return VfxTextures.byName("icefx_" + n); }

    static final ResourceLocation FANG = t("fang"), SPIKE = t("spike"), CRYSTAL = t("crystal"), SHARDS = t("shards"), BURST = t("burst"),
            FERN = t("fern"), FLAKE = t("flake"), MIST = t("mist"), RIME = t("rime"), GLINT = t("glint"), CRACK = t("crack"),
            TRAIL = t("trail"), WALL = t("wall");

    static final int SHARD_CELLS = 4;
    /** The magic's palette: PALE is the light colour, BLUE the body of the glass, DEEP its shadow, FROST the near-white of the edges. */
    static final int PALE = 0xFF9FE8FF, BLUE = 0xFF5FA8F0, DEEP = 0xFF1E4FA0, FROST = 0xFFE6FAFF, WHITE = 0xFFFFFFFF;

    /** The spell's tint mixed with the palette (30 %): white gives a whiter ice, the default exactly PALE. */
    static int glow(int tint) { return VfxVertexBuffer.lerpColor(PALE, 0xFF000000 | (tint & 0xFFFFFF), 0.3f); }
    /** The glass body: the blue pulled a little toward the light colour. */
    static int body(int glow) { return VfxVertexBuffer.lerpColor(BLUE, glow, 0.3f); }
    static int hot(int glow) { return VfxVertexBuffer.whiten(glow, 0.75f); }
    static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(alpha, 0f, 1f)); }

    static float hash(long seed, int i, int salt) { return ElementFx.hash(seed, i, salt); }
    static float life(VfxInstance inst, float age, float in, float out) { return ElementFx.life(inst, age, in, out); }
    static float ease(float t) { return VfxAnim.easeOutCubic(Mth.clamp(t, 0f, 1f)); }
    static float smooth(float a, float b, float x) { float t = Mth.clamp((x - a) / (b - a), 0f, 1f); return t * t * (3 - 2 * t); }

    /** One cell of a horizontal atlas (n cells), camera facing, rotated by rot like VfxVertexBuffer.billboard. */
    static void cell(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, VfxBlend bl, Vector3f c, float size, float rot, int k, int n, int col) {
        float cs = Mth.cos(rot) * size * 0.5f, sn = Mth.sin(rot) * size * 0.5f;
        Vector3f rx = new Vector3f(ctx.camRight).mul(cs).add(new Vector3f(ctx.camUp).mul(sn));
        Vector3f ry = new Vector3f(ctx.camUp).mul(cs).sub(new Vector3f(ctx.camRight).mul(sn));
        int q = bl.grade(col, 1f);
        buf.quad(tex, bl, new Vector3f(c).sub(rx).sub(ry), new Vector3f(c).add(rx).sub(ry), new Vector3f(c).add(rx).add(ry), new Vector3f(c).sub(rx).add(ry),
                k / (float) n, 0, (k + 1) / (float) n, 1, q, q);
    }

    /** A shard of ice: the glass body and its lit edges. */
    static void iceCell(VfxVertexBuffer buf, VfxRenderContext ctx, Vector3f c, float size, float rot, int k, int glow, float alpha) {
        cell(buf, ctx, SHARDS, VfxBlend.ALPHA, c, size, rot, k, SHARD_CELLS, a(body(glow), 0.9f * alpha));
        cell(buf, ctx, SHARDS, VfxBlend.ADD, c, size, rot, k, SHARD_CELLS, a(hot(glow), 0.85f * alpha));
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

    /** A blade of ice from a (base) to b (tip), camera facing: body then lit edges. */
    static void iceBlade(VfxVertexBuffer buf, ResourceLocation tex, Vector3f a, Vector3f b, float w, int glow, float alpha) {
        strip(buf, tex, VfxBlend.ALPHA, a, b, w, w, a(body(glow), 0.9f * alpha), a(body(glow), 0.9f * alpha));
        strip(buf, tex, VfxBlend.ADD, a, b, w, w, a(glow, 0.55f * alpha), a(hot(glow), 0.9f * alpha));
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

    /** A cluster of ice standing on the ground. */
    static void iceStand(VfxVertexBuffer buf, Vector3f base, float halfW, float height, float lean, int glow, float alpha) {
        stand(buf, CRYSTAL, VfxBlend.ALPHA, base, halfW, height, lean, a(body(glow), 0.9f * alpha));
        stand(buf, CRYSTAL, VfxBlend.ADD, base, halfW, height, lean, a(hot(glow), 0.8f * alpha));
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
