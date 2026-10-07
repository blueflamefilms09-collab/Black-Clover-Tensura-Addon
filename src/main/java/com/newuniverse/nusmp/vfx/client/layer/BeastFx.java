package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * Shared pieces of the Beast Magic effects: the textures (tools/gen_beast_textures.py), the palette (the translucent orange beast-fire of
 * the owner's anime still: hot yellow core, orange body, red-orange edge) and the little quad builders the three shapes are made of.
 * Everything goes through VfxVertexBuffer.quad, so the 400-vertex budget of an activation still applies.
 * <p>
 * Texture conventions: FLAME, STREAM, LICKS and SPIRIT carry their own cel-shaded colours (the colour is the point): draw them with ADD
 * and a colour from {@link #flame(int)}. Every other sprite is grey-scale + alpha and takes its colour from {@link #glow(int)}.
 * FLAME / STREAM / LICKS have their tip at the top (u = 0..1 across, v = 0 at the tip), SPIRIT faces right with the mane streaming left,
 * CLAW has its three gashes running from the top to the bottom, PAW has its toes at the top.
 */
final class BeastFx {
    private BeastFx() {}

    private static ResourceLocation t(String n) { return VfxTextures.byName("beast_" + n); }

    static final ResourceLocation FLAME = t("flame"), STREAM = t("stream"), LICKS = t("licks"), SPIRIT = t("spirit"), CLAW = t("claw"),
            PAW = t("paw"), SIGIL = t("sigil"), BURST = t("burst"), RING = t("ring"), FANGS = t("fangs"), HAZE = t("haze"), GLINT = t("glint");
    static final int LICK_CELLS = 4, FANG_CELLS = 4;

    /** The palette: ORANGE is the beast-fire body, RED its darker edge, GOLD the core, HOT the white-yellow heart. */
    static final int ORANGE = 0xFFFF9A2A, RED = 0xFFFF4A10, GOLD = 0xFFFFD060, HOT = 0xFFFFF0B0, SOOT = 0xFF3A0C04;

    /** The spell's tint mixed 30 % into the orange: white gives a pale fire, the default (the owner's light brown-orange) a warm one. */
    static int glow(int tint) { return VfxVertexBuffer.lerpColor(ORANGE, 0xFF000000 | (tint & 0xFFFFFF), 0.30f); }
    /** The multiplier for the baked-colour flame sprites: white with a 35 % pull toward the tint, so the cel colours stay. */
    static int flame(int tint) { return VfxVertexBuffer.lerpColor(0xFFFFFFFF, 0xFF000000 | (tint & 0xFFFFFF), 0.35f); }
    static int red(int glow) { return VfxVertexBuffer.lerpColor(glow, RED, 0.55f); }
    static int hot(int glow) { return VfxVertexBuffer.whiten(glow, 0.7f); }
    static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb, Mth.clamp(alpha, 0f, 1f)); }

    static float hash(long seed, int i, int salt) { return ElementFx.hash(seed, i, salt); }
    static float life(VfxInstance inst, float age, float in, float out) { return ElementFx.life(inst, age, in, out); }
    static float ease(float t) { return VfxAnim.easeOutCubic(Mth.clamp(t, 0f, 1f)); }
    static float smooth(float a, float b, float x) { float t = Mth.clamp((x - a) / (b - a), 0f, 1f); return t * t * (3 - 2 * t); }

    /** Unit vector perpendicular to dir and to the line of sight to 'at' (camera-relative), the sign chosen so it points up on screen. */
    static Vector3f side(VfxRenderContext ctx, Vector3f dir, Vector3f at) {
        Vector3f s = new Vector3f(dir).cross(new Vector3f(at).negate());
        if (s.lengthSquared() < 1e-8f) return new Vector3f(ctx.camUp);
        s.normalize();
        if (s.dot(ctx.camUp) < 0f) s.negate();
        return s;
    }

    /** A quad in an arbitrary plane: centre c, unit axes right / up, half sizes hx / hy. The image stands upright along up. */
    static void panel(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f c, Vector3f right, Vector3f up, float hx, float hy, int col) {
        Vector3f rx = new Vector3f(right).mul(hx), uy = new Vector3f(up).mul(hy);
        int k = bl.grade(col, 1f);
        buf.quad(tex, bl, new Vector3f(c).sub(rx).sub(uy), new Vector3f(c).add(rx).sub(uy), new Vector3f(c).add(rx).add(uy), new Vector3f(c).sub(rx).add(uy), 0, 0, 1, 1, k, k);
    }

    /** A sprite whose image-right runs along dir (a beast facing the way it moves), camera facing and upright. */
    static void along(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, VfxBlend bl, Vector3f c, Vector3f dir, float halfLen, float halfW, int col) {
        panel(buf, tex, bl, c, dir, side(ctx, dir, c), halfLen, halfW, col);
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

    /** Strip with a given unit side vector (a real 3D plane, for a second plane turning round a body so it reads as 3D). */
    static void stripSide(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f a, Vector3f b, Vector3f side, float wa, float wb, int ca, int cb) {
        Vector3f sa = new Vector3f(side).mul(wa * 0.5f), sb = new Vector3f(side).mul(wb * 0.5f);
        buf.quad(tex, bl, new Vector3f(a).sub(sa), new Vector3f(a).add(sa), new Vector3f(b).add(sb), new Vector3f(b).sub(sb), 0, 0, 1, 1, bl.grade(ca, 1f), bl.grade(cb, 1f));
    }

    /** A card standing on the ground at base (image bottom), turning about the vertical axis to face the camera; leans by lean blocks at the top. */
    static void stand(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f base, float halfW, float height, float lean, int col) {
        standCell(buf, tex, bl, base, halfW, height, lean, 0, 1, col);
    }

    /** As {@link #stand}, taking cell k of an atlas of n cells. */
    static void standCell(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f base, float halfW, float height, float lean, int k, int n, int col) {
        Vector3f r = new Vector3f(-base.z, 0, base.x);
        if (r.lengthSquared() < 1e-6f) r.set(1, 0, 0);
        r.normalize();
        Vector3f lo = new Vector3f(r).mul(halfW), top = new Vector3f(base).add(0, height, 0).add(new Vector3f(r).mul(lean));
        int q = bl.grade(col, 1f);
        buf.quad(tex, bl, new Vector3f(base).sub(lo), new Vector3f(base).add(lo), new Vector3f(top).add(lo), new Vector3f(top).sub(lo),
                k / (float) n, 0, (k + 1) / (float) n, 1, q, q);
    }

    /** A flat sprite lying on the ground at c, spun, drawn to the given radius. */
    static void flat(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f c, float lift, float spin, float radius, int col) {
        buf.plane(tex, bl, VfxPose.ground(c).spin(spin).lift(lift), radius, col);
    }

    /** A disc facing along dir. */
    static void facing(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, Vector3f c, Vector3f dir, float spin, float radius, int col) {
        buf.plane(tex, bl, VfxPose.facing(c, dir).spin(spin), radius, col);
    }
}
