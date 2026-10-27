package com.newuniverse.nusmp.vfx.client.layer;

import com.newuniverse.nusmp.vfx.VfxShape;
import com.newuniverse.nusmp.vfx.client.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

import java.util.EnumSet;
import java.util.Set;

/**
 * 3.30.0 Veldanava's Absolute Severance (Tensura: a cut that severs space itself, nothing can block it).
 * <ul>
 *   <li>0-3: a tiny circle opens at the start and a hair-thin white line snaps along the whole length (almost invisible at first), then flashes white.</li>
 *   <li>3-6: the space along the line splits from the start to the far end: a black void gap opens between two lips with a thin violet (one side) and teal (other side) rim and a
 *       razor-white core inside; glass-like angular shards are flung off the edges.</li>
 *   <li>6-12: the seam stays wide, its edges flicker, the shards hang and slide along it.</li>
 *   <li>12-16: the lips close (from the start to the far end) and fade, a faint afterimage line is left.</li>
 * </ul>
 * {@code from} = the start (chest height), {@code to} = the far end, {@code inst.power} = the half-width of the open seam (~1.6). Every strip is a straight camera-facing ribbon between
 * shared nodes, so the cut is dead straight; the hard-edged parts sample the solid middle of a texture (the quad edge is the edge), only the glows use a soft texture.
 * Worst case about 355 of the 400 vertices (all quads), constant loop counts, deterministic from seed and age; {@link VfxRenderContext#seg} only thins the shards and glints.
 */
public class VeldSeveranceLayer extends AbstractVfxLayer {
    private static ResourceLocation t(String n) { return VfxTextures.byName(n); }

    private static final ResourceLocation SOLID = t("shard"), UMBRA = t("bfx_umbra"), BEAM = t("judgment_beam"), RING = t("darkfx_photon"), FLARE = t("spike_flare"),
            SHARD_A = t("darkfx_shard_a"), SHARD_B = t("darkfx_shard_b"), GLASS = t("spatial_shard"), GLINT = t("air_glint"), HALO = t("bfx_halo");

    private static final int WHITE = 0xFFFFFFFF, TEAL = 0xFF5EEAD8, VIOLET = 0xFF9A5CFF, VOID = 0xFF07040C;
    private static final int NODES = 12, SHARDS = 14, GLINTS = 5;

    @Override public Set<VfxShape> shapes() { return EnumSet.of(VfxShape.VELD_SEVERANCE); }
    @Override public int defaultDuration(VfxShape s) { return 16; }
    @Override public int defaultColor(VfxShape s) { return 0xFF5EEAD8; }

    private static float hash(long seed, int i, int k) {
        long x = seed * 0x9E3779B97F4A7C15L + i * 0xBF58476D1CE4E5B9L + k * 0x94D049BB133111EBL;
        x ^= x >>> 30; x *= 0xBF58476D1CE4E5B9L; x ^= x >>> 27; x *= 0x94D049BB133111EBL; x ^= x >>> 31;
        return (x >>> 40) / (float) (1 << 24);
    }

    private static int a(int argb, float alpha) { return VfxVertexBuffer.withAlpha(argb | 0xFF000000, Mth.clamp(alpha, 0f, 1f)); }
    private static float cl(float v) { return Mth.clamp(v, 0f, 1f); }
    private static float sub(float v, float s, float e) { return cl((v - s) / Math.max(1e-4f, e - s)); }
    private static float smooth(float t) { t = cl(t); return t * t * (3f - 2f * t); }

    // ---- scratch (the render thread only; nothing is allocated per frame)
    private final float[] nx = new float[NODES + 1], ny = new float[NODES + 1], nz = new float[NODES + 1];
    private final float[] sx = new float[NODES + 1], sy = new float[NODES + 1], sz = new float[NODES + 1];
    private final float[] oL = new float[NODES + 1], oR = new float[NODES + 1];
    private final int[] col = new int[NODES + 1];
    private final float[] gL = new float[NODES + 1], gR = new float[NODES + 1], fl = new float[NODES + 1], vis = new float[NODES + 1];
    private final Vector3f p0 = new Vector3f(), p1 = new Vector3f(), p2 = new Vector3f(), p3 = new Vector3f(), tmp = new Vector3f();

    /** The camera-facing side vector of the line at a point (unit, perpendicular to the line and to the view ray). */
    private static void side(Vector3f out, float dx, float dy, float dz, float px, float py, float pz) {
        out.set(dy * -pz - dz * -py, dz * -px - dx * -pz, dx * -py - dy * -px);
        if (out.lengthSquared() > 1e-8f) out.normalize(); else out.set(0f, 0f, 0f);
    }

    /** Nodes n+1 points of the line from s0 to s1 (fractions of A->D), with their camera-facing sides. */
    private void nodes(Vector3f A, float dx, float dy, float dz, float s0, float s1, int n) {
        for (int k = 0; k <= n; k++) {
            float s = Mth.lerp((float) k / n, s0, s1);
            nx[k] = A.x + dx * s; ny[k] = A.y + dy * s; nz[k] = A.z + dz * s;
            side(tmp, dx, dy, dz, nx[k], ny[k], nz[k]);
            if (tmp.lengthSquared() < 0.5f && k > 0) tmp.set(sx[k - 1], sy[k - 1], sz[k - 1]);
            sx[k] = tmp.x; sy[k] = tmp.y; sz[k] = tmp.z;
        }
    }

    /** A ribbon through the current nodes: left edge at oL, right edge at oR (along the side vector), colour per node, texture rectangle u0,v0 - u1,v1. */
    private void strip(VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, int n, float u0, float v0, float u1, float v1) {
        if (!buf.hasBudget(4 * n)) return;
        for (int k = 0; k < n; k++) {
            if (((col[k] | col[k + 1]) >>> 24) == 0) continue;
            p0.set(nx[k] + sx[k] * oL[k], ny[k] + sy[k] * oL[k], nz[k] + sz[k] * oL[k]);
            p1.set(nx[k] + sx[k] * oR[k], ny[k] + sy[k] * oR[k], nz[k] + sz[k] * oR[k]);
            p2.set(nx[k + 1] + sx[k + 1] * oR[k + 1], ny[k + 1] + sy[k + 1] * oR[k + 1], nz[k + 1] + sz[k + 1] * oR[k + 1]);
            p3.set(nx[k + 1] + sx[k + 1] * oL[k + 1], ny[k + 1] + sy[k + 1] * oL[k + 1], nz[k + 1] + sz[k + 1] * oL[k + 1]);
            buf.quad(tex, bl, p0, p1, p2, p3, u0, v0, u1, v1, col[k], col[k + 1]);
        }
    }

    private void sprite(VfxRenderContext ctx, VfxVertexBuffer buf, ResourceLocation tex, VfxBlend bl, float x, float y, float z, float size, float rot, int c) {
        if (size > 0.01f && ((c >>> 24) > 2) && buf.hasBudget(4)) buf.billboard(ctx, tex, bl, tmp.set(x, y, z), size, rot, c);
    }

    @Override
    public void render(VfxInstance inst, VfxRenderContext ctx, VfxVertexBuffer buf) {
        float age = inst.ageTicks(ctx.partialTick);
        if (age >= 16f) return;
        long seed = inst.seed;
        float W = Math.max(0.5f, inst.power), ws = W / 1.6f;
        Vector3f A = ctx.rel(inst.from(ctx)), B = ctx.rel(inst.to(ctx));
        float dx = B.x - A.x, dy = B.y - A.y, dz = B.z - A.z;
        float len = Mth.sqrt(dx * dx + dy * dy + dz * dz);
        if (len < 0.5f) return;
        float ux = dx / len, uy = dy / len, uz = dz / len;
        int teal = (inst.color & 0xFFFFFF) | 0xFF000000;

        // ---- timeline
        float extent = VfxAnim.easeOutCubic(sub(age, 0.15f, 1.7f));                       // how much of the hair line is drawn (it snaps out from the start)
        float flashK = smooth(sub(age, 1.5f, 2.3f)) * (1f - 0.65f * smooth(sub(age, 3.0f, 5.0f)));
        float bump = Mth.sin(Mth.PI * sub(age, 1.7f, 3.6f));                               // the white flash of the snap
        float afterglow = 1f - sub(age, 12f, 14.4f);
        float lineA = 0.3f + 0.7f * flashK;                                                // faint hair -> full white
        lineA *= 0.25f + 0.75f * afterglow;
        lineA *= 1f - sub(age, 15.2f, 16f);
        float fin = 1f - sub(age, 15.2f, 16f);

        // ---- the void gap and its lips: per node, from the start to the end, opens at 3-5.4 and closes at 11.6-15.6
        boolean opening = age > 3.0f;
        if (opening) {
            for (int k = 0; k <= NODES; k++) {
                float s = (float) k / NODES;
                float open = VfxAnim.easeOutCubic(sub(age, 3.0f + 1.8f * s, 5.4f + 1.8f * s));
                float close = smooth(sub(age, 10.6f + 1.5f * s, 13.0f + 1.8f * s));
                float prof = smooth(s / 0.14f) * smooth((1f - s) / 0.16f) * (0.72f + 0.28f * Mth.sin(Mth.PI * s));
                float jl = 0.62f + 0.38f * hash(seed, k, 1), jr = 0.62f + 0.38f * hash(seed, k, 2);
                int tick = (int) Math.floor(age * 1.6f);
                float wob = age > 6f && age < 12.5f ? 1f + 0.14f * (hash(seed, k, 10 + tick) - 0.5f) * 2f : 1f;
                float g = W * 1.1f * prof * open * (1f - close) * wob;
                gL[k] = g * jl; gR[k] = g * jr;
                float flick = age > 6f ? 0.4f + 0.6f * hash(seed, k, 40 + (int) Math.floor(age * 2.2f)) : 1f;
                fl[k] = flick;
                vis[k] = open * (1f - close);
            }
            nodes(A, dx, dy, dz, 0f, 1f, NODES);
            // the black void gap (hard edges)
            for (int k = 0; k <= NODES; k++) { oL[k] = -gL[k]; oR[k] = gR[k]; col[k] = a(VOID, 0.97f * cl(vis[k] * 4f)); }
            strip(buf, UMBRA, VfxBlend.ALPHA, NODES, 0.3f, 0.3f, 0.7f, 0.7f);
            // the thin glowing rims (violet on one lip, teal on the other), hard edged
            float rw = 0.1f + 0.05f * ws;
            for (int k = 0; k <= NODES; k++) {
                int cv = VfxVertexBuffer.lerpColor(VIOLET, TEAL, 0.25f * k / NODES), ct = VfxVertexBuffer.lerpColor(teal, VIOLET, 0.25f * k / NODES);
                oL[k] = -gL[k] - rw; oR[k] = -gL[k]; col[k] = a(cv, 0.95f * fl[k] * cl(vis[k] * 3f) * fin);
            }
            strip(buf, SOLID, VfxBlend.ADD, NODES, 0.42f, 0.4f, 0.58f, 0.6f);
            for (int k = 0; k <= NODES; k++) {
                int ct = VfxVertexBuffer.lerpColor(teal, VIOLET, 0.25f * k / NODES);
                oL[k] = gR[k]; oR[k] = gR[k] + rw; col[k] = a(ct, 0.95f * fl[(k + 3) % (NODES + 1)] * cl(vis[k] * 3f) * fin);
            }
            strip(buf, SOLID, VfxBlend.ADD, NODES, 0.42f, 0.4f, 0.58f, 0.6f);
            // the cold glow outside the rims
            for (int k = 0; k <= NODES; k++) { oL[k] = -gL[k] - rw - 0.85f * W * cl(vis[k] * 3f); oR[k] = -gL[k] - rw * 0.5f; col[k] = a(VIOLET, 0.85f * fl[k] * cl(vis[k] * 3f) * fin); }
            strip(buf, BEAM, VfxBlend.ADD, NODES, 0.02f, 0.2f, 0.5f, 0.8f);
            for (int k = 0; k <= NODES; k++) { oL[k] = gR[k] + rw * 0.5f; oR[k] = gR[k] + rw + 0.85f * W * cl(vis[k] * 3f); col[k] = a(teal, 0.85f * fl[(k + 3) % (NODES + 1)] * cl(vis[k] * 3f) * fin); }
            strip(buf, BEAM, VfxBlend.ADD, NODES, 0.5f, 0.2f, 0.98f, 0.8f);
        }

        // ---- the razor-white core: a hard hair line plus a soft aura (n = 2 is enough, it is straight), drawn from the start to the snapping head
        nodes(A, dx, dy, dz, 0f, extent, 2);
        float coreW = 0.03f + 0.05f * flashK * (1f - 0.5f * smooth(sub(age, 3f, 6f))) + 0.02f * bump;
        coreW *= 0.5f + 0.5f * afterglow;
        float auraW = 0.12f + (0.2f + 1.5f * bump) * ws + 0.25f * ws * smooth(sub(age, 3f, 6f)) * afterglow;
        int auraC = VfxVertexBuffer.lerpColor(teal, WHITE, 0.55f);
        for (int k = 0; k <= 2; k++) { oL[k] = -auraW; oR[k] = auraW; col[k] = a(auraC, (0.18f + 0.55f * flashK + 0.4f * bump) * lineA * (k == 2 ? 0.8f + 0.2f * extent : 1f)); }
        strip(buf, BEAM, VfxBlend.ADD, 2, 0.02f, 0.2f, 0.98f, 0.8f);
        for (int k = 0; k <= 2; k++) { oL[k] = -coreW; oR[k] = coreW; col[k] = a(WHITE, lineA); }
        strip(buf, SOLID, VfxBlend.ADD, 2, 0.42f, 0.4f, 0.58f, 0.6f);

        // ---- the circle at the start, and the head of the snap
        float circ = smooth(sub(age, 0f, 0.8f)) * (1f - smooth(sub(age, 5f, 8f)));
        if (circ > 0.01f) {
            float r = (0.16f + 0.22f * VfxAnim.easeOutBack(sub(age, 0f, 1.0f)) + 0.45f * bump) * ws * (1f - 0.4f * smooth(sub(age, 5f, 8f)));
            VfxPose pose = VfxPose.facing(A, new Vector3f(ux, uy, uz));
            if (buf.hasBudget(4)) buf.plane(RING, VfxBlend.ADD, pose.spin(age * 0.25f), r * 1.15f, a(WHITE, 0.95f * circ));
            if (buf.hasBudget(4)) buf.plane(RING, VfxBlend.ADD, pose.spin(-age * 0.12f), r * 0.62f, a(teal, 0.8f * circ));
            sprite(ctx, buf, FLARE, VfxBlend.ADD, A.x, A.y, A.z, (0.7f + 1.6f * bump) * ws, age * 0.1f, a(WHITE, (0.35f + 0.65f * bump) * circ));
        }
        if (extent < 0.999f && extent > 0.01f) {
            sprite(ctx, buf, FLARE, VfxBlend.ADD, A.x + dx * extent, A.y + dy * extent, A.z + dz * extent, 0.9f * ws + 0.002f * len, age * 0.5f, a(WHITE, 0.9f));
        }
        if (bump > 0.02f) {                                                                // the snap lands at the far end
            float endK = bump * sub(age, 1.9f, 2.6f);
            sprite(ctx, buf, HALO, VfxBlend.ADD, B.x, B.y, B.z, (1.2f + 2.4f * bump) * ws, 0f, a(auraC, 0.7f * endK));
        }

        // ---- glass-like shards flung off the edges, hanging and sliding along the seam afterwards
        if (age > 3.0f) {
            int n = ctx.seg(SHARDS, 6);
            for (int i = 0; i < n; i++) {
                float s = 0.07f + 0.86f * hash(seed, i, 20);
                float birth = 3.0f + 1.8f * s + 0.5f * hash(seed, i, 21);
                float ts = age - birth;
                if (ts <= 0f) continue;
                float fade = sub(ts, 0f, 0.5f) * (1f - sub(age, 12.2f, 15.6f));
                if (fade <= 0.01f) continue;
                float sg = (i & 1) == 0 ? -1f : 1f;
                float fly = VfxAnim.easeOutCubic(sub(ts, 0f, 2.2f));
                float dist = (0.5f + 3.2f * hash(seed, i, 22)) * ws * fly + 0.04f * ws * ts;
                float slide = (hash(seed, i, 23) - 0.5f) * 5f * ws * sub(ts, 1.5f, 9f) + (hash(seed, i, 24) - 0.5f) * 1.5f * ws * fly;
                float ss = cl(s + slide / len);
                float px = A.x + dx * ss, py = A.y + dy * ss, pz = A.z + dz * ss;
                side(tmp, dx, dy, dz, px, py, pz);
                float gw = W * 1.1f * (0.5f + 0.5f * Mth.sin(Mth.PI * ss)) * 0.8f;
                float off = sg * (gw + dist);
                float bob = 0.18f * ws * Mth.sin(age * 0.3f + i * 1.7f) * sub(ts, 1f, 3f);
                float x = px + tmp.x * off, y = py + tmp.y * off + bob * 0.5f, z = pz + tmp.z * off;
                float size = (0.7f + 1.0f * hash(seed, i, 25)) * ws;
                float rot = hash(seed, i, 26) * Mth.TWO_PI + (hash(seed, i, 27) - 0.5f) * 0.25f * ts;
                if ((i & 1) == 1) sprite(ctx, buf, GLASS, VfxBlend.ALPHA, x, y, z, size * 0.9f, rot, a((i & 2) == 0 ? 0xFFCFF6F2 : 0xFFD9C8FF, 0.85f * fade));
                else sprite(ctx, buf, (i & 3) == 0 ? SHARD_A : SHARD_B, VfxBlend.ALPHA, x, y, z, size, rot, a(WHITE, fade));
            }
            int gn = ctx.seg(GLINTS, 2);
            for (int i = 0; i < gn; i++) {
                float s = 0.1f + 0.8f * hash(seed, i, 30);
                float tw = Mth.sin(age * (0.9f + 0.5f * hash(seed, i, 31)) + i * 2.3f);
                float on = sub(age, 3.4f + 1.6f * s, 4.6f + 1.6f * s) * (1f - sub(age, 12f, 15.4f)) * cl(tw * 1.5f);
                if (on <= 0.02f) continue;
                float px = A.x + dx * s, py = A.y + dy * s, pz = A.z + dz * s;
                side(tmp, dx, dy, dz, px, py, pz);
                float off = ((i & 1) == 0 ? -1f : 1f) * W * (0.8f + 0.5f * hash(seed, i, 32));
                sprite(ctx, buf, GLINT, VfxBlend.ADD, px + tmp.x * off, py + tmp.y * off, pz + tmp.z * off, (0.55f + 0.5f * hash(seed, i, 33)) * ws, i * 0.6f, a((i & 1) == 0 ? VIOLET : teal, 0.95f * on));
            }
        }
    }
}
