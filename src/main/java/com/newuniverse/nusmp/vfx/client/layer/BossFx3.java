package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.client.VfxBlend;
import com.newuniverse.nusmp.vfx.client.VfxPose;
import com.newuniverse.nusmp.vfx.client.VfxRenderContext;
import com.newuniverse.nusmp.vfx.client.VfxVertexBuffer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * 1.35.0: flat-on-the-floor and envelope helpers shared by the boss move layers (Zagred, the Commander, the Juggernaut, the refill flash).
 * Pure drawing maths on top of {@link VfxVertexBuffer}: nothing here places or breaks a block, and every call is skipped whole when the
 * activation's vertex budget cannot hold it.
 */
final class BossFx3 {
    private BossFx3() {}

    /** Rises over `in` ticks, falls over the last `out` ticks, 1 between. */
    static float life(float age, float dur, float in, float out) {
        float a = in <= 0 ? 1f : Mth.clamp(age / in, 0f, 1f), b = out <= 0 ? 1f : Mth.clamp((dur - age) / out, 0f, 1f);
        return a * b;
    }

    /** 0..1 progress of a window [from, to] of the effect's own 0..1 progress. */
    static float win(float prog, float from, float to) { return Mth.clamp((prog - from) / Math.max(1e-4f, to - from), 0f, 1f); }

    static float ease(float x) { return x * x * (3f - 2f * x); }

    /** A flat strip lying on the floor from a to b, `half` either side (a and b are camera-relative), full texture across it. */
    static void strip(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f a, Vector3f b, float half, float uRepeat, int c0, int c1) {
        Vector3f dir = new Vector3f(b).sub(a);
        dir.y = 0f;
        if (dir.lengthSquared() < 1e-6f) return;
        dir.normalize();
        Vector3f side = new Vector3f(-dir.z, 0f, dir.x).mul(half);
        buf.quad(tex, blend, new Vector3f(a).sub(side), new Vector3f(a).add(side), new Vector3f(b).add(side), new Vector3f(b).sub(side), 0f, 0f, uRepeat, 1f, c0, c1);
    }

    /** A disc lying on the floor, `lift` above the pose origin. */
    static void disc(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f c, float radius, float spin, float lift, int argb) {
        buf.plane(tex, blend, VfxPose.ground(c).spin(spin).lift(lift), radius, argb);
    }

    /** A ring band lying on the floor. */
    static void band(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f c, float inner, float outer, int segs, float repeat, float scroll, float spin, float lift, int argb) {
        buf.ring(tex, blend, VfxPose.ground(c).spin(spin).lift(lift), inner, outer, segs, repeat, scroll, argb);
    }

    /** A vertical pillar (two crossed camera-independent quads would cost 8 vertices; a camera-facing ribbon costs 4): bottom to top, `w` wide. */
    static void pillar(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend blend, Vector3f base, float height, float w, int cBottom, int cTop) {
        Vector3f top = new Vector3f(base).add(0f, height, 0f);
        buf.beam(ctx, tex, blend, base, top, w, w * 0.6f, 1, 0f, cBottom, cTop);
    }

    static int rgbA(int rgb, float a) { return VfxVertexBuffer.withAlpha(0xFF000000 | (rgb & 0xFFFFFF), Mth.clamp(a, 0f, 1f)); }

    static float h(long seed, int i) { return DarkMagicFx.hash(seed, i); }
}
