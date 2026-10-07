package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.client.VfxBlend;
import com.newuniverse.nusmp.vfx.client.VfxRenderContext;
import com.newuniverse.nusmp.vfx.client.VfxVertexBuffer;
import net.minecraft.resources.ResourceLocation;
import org.joml.Vector3f;

/** 0.56: ribbon and hash helpers of {@link DarkSlashLayer}. */
final class DarkSlashStrip {
    private DarkSlashStrip() {}

    /**
     * A camera-facing ribbon through the points: U runs across it (0..1), V along it from vA at the first point to vB at the last.
     * w = full width and col = ARGB per point. Costs 4 vertices per segment and is skipped whole if the budget cannot hold it.
     */
    static void ribbon(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, VfxBlend blend, Vector3f[] p, float[] w, int[] col, float vA, float vB) {
        int n = p.length;
        if (n < 2 || !buf.hasBudget(4 * (n - 1))) return;
        Vector3f[] l = new Vector3f[n], r = new Vector3f[n];
        Vector3f tan = new Vector3f(), cam = new Vector3f(), side = new Vector3f();
        for (int i = 0; i < n; i++) {
            tan.set(p[Math.min(i + 1, n - 1)]).sub(p[Math.max(i - 1, 0)]);
            cam.set(p[i]).negate();
            tan.cross(cam, side);
            if (side.lengthSquared() < 1e-8f) side.set(ctx.camRight); else side.normalize();
            side.mul(w[i] * 0.5f);
            l[i] = new Vector3f(p[i]).sub(side);
            r[i] = new Vector3f(p[i]).add(side);
        }
        for (int i = 0; i < n - 1; i++) {
            float t0 = vA + (vB - vA) * i / (n - 1), t1 = vA + (vB - vA) * (i + 1) / (n - 1);
            buf.quad(tex, blend, l[i], r[i], r[i + 1], l[i + 1], 0f, t1, 1f, t0, col[i], col[i + 1]);
        }
    }

    /** A straight ribbon a -> b in n points (n - 1 segments) with a width profile and one colour. */
    static void line(VfxVertexBuffer buf, VfxRenderContext ctx, ResourceLocation tex, VfxBlend blend, Vector3f a, Vector3f b, int n, float width, int argb) {
        Vector3f[] p = new Vector3f[n];
        float[] w = new float[n];
        int[] c = new int[n];
        for (int i = 0; i < n; i++) { p[i] = new Vector3f(a).lerp(b, (float) i / (n - 1)); w[i] = width; c[i] = argb; }
        ribbon(buf, ctx, tex, blend, p, w, c, 0f, 1f);
    }

    /** Deterministic 0..1 value from a seed and an index (same on every client). */
    static float hash(long seed, int i) {
        long h = seed * 0x9E3779B97F4A7C15L + (i + 1) * 0xBF58476D1CE4E5B9L;
        h ^= h >>> 31;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 29;
        return (h >>> 40) / (float) (1L << 24);
    }

    static float saturate(float x) { return x < 0f ? 0f : (x > 1f ? 1f : x); }

    static float frac(float x) { return x - (float) Math.floor(x); }
}
