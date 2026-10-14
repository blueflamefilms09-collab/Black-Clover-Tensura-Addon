package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.client.VfxBlend;
import com.newuniverse.nusmp.vfx.client.VfxInstance;
import com.newuniverse.nusmp.vfx.client.VfxPose;
import com.newuniverse.nusmp.vfx.client.VfxRenderContext;
import com.newuniverse.nusmp.vfx.client.VfxTextures;
import com.newuniverse.nusmp.vfx.client.VfxVertexBuffer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * 1.35.0: the shared pieces of the four Zogratis effect layers (ZogratisGravityLayer, ZogratisBloodLayer, ZogratisSpaceLayer,
 * ZogratisDivineLayer): their textures (tools/gen_boss_vfx_textures.py), a few ground / lane / ribbon helpers and the payload flags.
 * Not a layer itself. Every helper spends vertices from the instance's budget (4 per quad) and silently stops when it is used up.
 */
final class ZogratisFx {
    private ZogratisFx() {}

    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    static final ResourceLocation WARP_DISC = t("bfx_warp_disc"), CHEVRON = t("bfx_chevron"), GROUND_CRACK = t("bfx_ground_crack"), LENS = t("bfx_lens"),
            UMBRA = t("bfx_umbra"), BAND = t("bfx_band"), RIBBON = t("bfx_ribbon"), LANCE = t("bfx_lance"), BLOOD_SIGIL = t("bfx_blood_sigil"), GLASS_WEB = t("bfx_glass_web"),
            TEAR = t("bfx_tear"), PORTAL = t("bfx_portal"), PILLAR = t("bfx_pillar"), HALO = t("bfx_halo"), FEATHER = t("bfx_feather"),
            WISP = t("bfx_wisp"), DIVINE_SIGIL = t("bfx_divine_sigil"),
            GLOW = VfxTextures.GLOW, SPARK = VfxTextures.SPARK, SHARD = VfxTextures.SHARD, RUNE_RING = VfxTextures.RUNE_RING,
            DROPLET = t("blood_droplet"), SPLAT = t("blood_splat"), DUST = t("earth_dust");

    static final int WHITE = 0xFFFFFFFF, BLACK = 0xFF05030A;

    /** Payload flag: the telegraph form of a shape that also has a strike form (the seed's lowest bit). */
    static boolean telegraph(VfxInstance inst) { return (inst.seed & 1L) != 0; }

    /** Payload flag: the second variant bit (a pane lying on the ground, a purple form ...). */
    static boolean variant(VfxInstance inst) { return (inst.seed & 2L) != 0; }

    static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(alpha, 0f, 1f)); }

    /** 0..1 over [start, end] of the effect's life. */
    static float ramp(float t, float start, float end) { return Mth.clamp((t - start) / Math.max(1e-4f, end - start), 0f, 1f); }

    static float ease(float t) { float k = Mth.clamp(t, 0f, 1f); return k * k * (3f - 2f * k); }

    static float outCubic(float t) { float u = 1f - Mth.clamp(t, 0f, 1f); return 1f - u * u * u; }

    static float hash(long seed, int i, int salt) { return ElementFx.hash(seed, i, salt); }

    /** A tint pushed toward white (hot cores). */
    static int hot(int argb, float k) { return VfxVertexBuffer.whiten(argb, k); }

    /** A square lying flat at 'c' (camera-relative), half size 'half', spun by 'spin'. */
    static void ground(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f c, float lift, float half, float spin, int argb) {
        if (half <= 0.01f) return;
        buf.plane(tex, blend, VfxPose.ground(new Vector3f(c).add(0, lift, 0)).spin(spin), half, argb);
    }

    /** A square on a plane facing 'normal'. */
    static void facing(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f c, Vector3f normal, float half, float spin, int argb) {
        if (half <= 0.01f) return;
        buf.plane(tex, blend, VfxPose.facing(c, normal).spin(spin), half, argb);
    }

    /** Camera-facing glow / sprite. */
    static void glow(VfxVertexBuffer buf, VfxRenderContext ctx, Vector3f p, float size, int argb) {
        if (size <= 0.01f) return;
        buf.billboard(ctx, GLOW, VfxBlend.ADD, p, size, 0f, argb);
    }

    /** A sprite whose texture "up" points along the screen-space direction of the velocity 'v' (camera-relative). */
    static void along(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, VfxBlend blend, Vector3f p, Vector3f v, float size, int argb) {
        float vr = v.dot(ctx.camRight), vu = v.dot(ctx.camUp);
        buf.billboard(ctx, tex, blend, p, size, (float) Math.atan2(-vr, vu), argb);
    }

    /**
     * A lane lying on the ground from a to b, 'width' wide, the texture's V running along it ('tile' repeats per block, 'scroll' moves the
     * pattern toward b); the texture's top points at b.
     */
    static void lane(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f a, Vector3f b, float width, float tile, float scroll, int argb) {
        Vector3f d = new Vector3f(b).sub(a);
        float len = d.length();
        if (len < 0.05f) return;
        Vector3f s = ElementFx.side(new Vector3f(d).div(len)).mul(width * 0.5f);
        float v1 = scroll, v0 = scroll - len * tile;
        buf.quad(tex, blend, new Vector3f(a).sub(s), new Vector3f(a).add(s), new Vector3f(b).add(s), new Vector3f(b).sub(s), 0, v0, 1, v1, argb, argb);
    }

    /** A point on the quadratic bezier p0 - p1 - p2. */
    static Vector3f bez(Vector3f p0, Vector3f p1, Vector3f p2, float s) {
        float u = 1f - s;
        return new Vector3f(p0).mul(u * u).add(new Vector3f(p1).mul(2 * u * s)).add(new Vector3f(p2).mul(s * s));
    }

    /**
     * A camera-facing ribbon along the bezier p0 - p1 - p2 between the fractions s0 and s1 (V along it with the texture's head at the top,
     * U across: RIBBON), tapering to the ends when 'taper'. Segments are spent from the budget.
     */
    static void curve(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f p0, Vector3f p1, Vector3f p2, float s0, float s1, int segs,
                      float width, boolean taper, int cTail, int cHead) {
        if (s1 - s0 < 0.01f || segs < 1) return;
        Vector3f prev = bez(p0, p1, p2, s0);
        for (int i = 1; i <= segs; i++) {
            float f = (float) i / segs, s = s0 + (s1 - s0) * f;
            Vector3f cur = bez(p0, p1, p2, s);
            Vector3f dir = new Vector3f(cur).sub(prev);
            if (dir.lengthSquared() < 1e-8f) { prev = cur; continue; }
            Vector3f mid = new Vector3f(prev).add(cur).mul(0.5f);
            Vector3f side = new Vector3f(dir).cross(new Vector3f(mid).negate());
            if (side.lengthSquared() < 1e-10f) { prev = cur; continue; }
            side.normalize();
            float w0 = width * 0.5f * (taper ? 0.15f + 0.85f * Mth.sin(Mth.PI * (float) (i - 1) / segs) : 1f);
            float w1 = width * 0.5f * (taper ? 0.15f + 0.85f * Mth.sin(Mth.PI * f) : 1f);
            Vector3f s0v = new Vector3f(side).mul(w0), s1v = new Vector3f(side).mul(w1);
            int k0 = blend.grade(VfxVertexBuffer.lerpColor(cTail, cHead, (float) (i - 1) / segs), 0.8f), k1 = blend.grade(VfxVertexBuffer.lerpColor(cTail, cHead, f), 0.8f);
            buf.quad(tex, blend, new Vector3f(prev).sub(s0v), new Vector3f(prev).add(s0v), new Vector3f(cur).add(s1v), new Vector3f(cur).sub(s1v),
                    0, 1f - f, 1, 1f - (float) (i - 1) / segs, k0, k1);
            prev = cur;
        }
    }

    /** A camera-facing spear sprite from 'tail' to 'head' (the texture's top, its head, leads), 'width' wide. */
    static void spear(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f tail, Vector3f head, float width, int argb) {
        Vector3f d = new Vector3f(head).sub(tail);
        if (d.lengthSquared() < 1e-6f) return;
        Vector3f mid = new Vector3f(head).add(tail).mul(0.5f);
        Vector3f s = new Vector3f(d).cross(new Vector3f(mid).negate());
        if (s.lengthSquared() < 1e-10f) return;
        s.normalize().mul(width * 0.5f);
        int col = blend.grade(argb, 0.8f);
        buf.quad(tex, blend, new Vector3f(tail).sub(s), new Vector3f(tail).add(s), new Vector3f(head).add(s), new Vector3f(head).sub(s), 0, 0, 1, 1, col, col);
    }

    /** Unit vector from a to b in camera-relative space, or +Z when they coincide. */
    static Vector3f dir(Vector3f a, Vector3f b) {
        Vector3f d = new Vector3f(b).sub(a);
        if (d.lengthSquared() < 1e-6f) return new Vector3f(0, 0, 1);
        return d.normalize();
    }
}
