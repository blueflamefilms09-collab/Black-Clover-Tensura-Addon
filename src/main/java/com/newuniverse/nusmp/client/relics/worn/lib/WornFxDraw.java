package com.newuniverse.nusmp.client.relics.worn.lib;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The procedural grade effects of a worn relic, drawn in the additive pass over the model library's atlas: orbiting glow motes (LEGEND and up), a turning rune
 * ring (LEGEND and up) and a flowing halo band (GOD and up). Everything is small arithmetic written straight into the vertex consumer through one reusable
 * scratch vector, so a frame allocates nothing. Positions are layer pixels relative to the pivot of the body part the matrix belongs to.
 * <p>
 * Costs (vertices): a mote 32, the ring 128, the halo 128 ({@code WornFxPlan#fxVertices}).
 */
final class WornFxDraw {
    private WornFxDraw() {}

    private static final int FULL = LightTexture.FULL_BRIGHT;
    private static final Vector3f P = new Vector3f();
    /** The centre of the atlas tile "soft" (a round glow), the first texel of the inner area of the tiles "rune" and "flame", and the width of 4 texels. */
    private static final float SOFT_U = 0.875f, SOFT_V = 0.5625f, RUNE_U = 0.53125f, RUNE_V = 0.265625f, FLAME_U = 0.78125f, FLAME_V = 0.265625f, SLICE = 4f / 128f, ROW = 16f / 256f;
    private static final int SEGS = 16;
    /** Per anchor (HEAD, BODY, RIGHT_ARM, LEFT_ARM, RIGHT_LEG, LEFT_LEG): the centre height and radius motes orbit on, the rune ring's height and radius, the halo's height and radius (0 = none). */
    private static final float[][] AURA = {
            {-9f, 6.8f, -9.5f, 6.4f, -12.2f, 5.6f},
            {5f, 9.5f, 12f, 9f, -2.5f, 8.5f},
            {6f, 5.2f, 8f, 5f, 0f, 0f},
            {6f, 5.2f, 8f, 5f, 0f, 0f},
            {9f, 5f, 12.3f, 5.5f, 0f, 0f},
            {9f, 5f, 12.3f, 5.5f, 0f, 0f}};

    private static void vert(VertexConsumer vc, Matrix4f m, float x, float y, float z, float u, float v, int r, int g, int b) {
        m.transformPosition(x, y, z, P);
        vc.addVertex(P.x, P.y, P.z).setColor(r, g, b, 255).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FULL).setNormal(0f, 1f, 0f);
    }

    private static int ch(int rgb, int shift, float k) { return Math.min(255, Math.max(0, (int) (((rgb >> shift) & 255) * k))); }

    /** {@code count} octahedra circling the anchor's aura, bobbing, each pulsing out of step. */
    static void motes(VertexConsumer vc, Matrix4f m, WornAnchor a, int rgb, int count, float age, float pulse) {
        float[] au = AURA[a.ordinal()];
        for (int i = 0; i < count; i++) {
            float th = age * 0.06f * (1f + 0.13f * i) + i * (6.2831855f / count);
            float x = (float) Math.cos(th) * au[1], z = (float) Math.sin(th) * au[1];
            float y = au[0] + (float) Math.sin(age * 0.05f + i * 1.7f) * 2.2f;
            float s = 0.5f + 0.22f * (float) Math.sin(age * 0.11f + i * 2.3f);
            float k = 0.7f + 0.3f * (float) Math.sin(age * 0.09f + i) + pulse * 0.4f;
            octa(vc, m, x, y, z, s, ch(rgb, 16, k), ch(rgb, 8, k), ch(rgb, 0, k));
        }
    }

    private static void octa(VertexConsumer vc, Matrix4f m, float cx, float cy, float cz, float s, int r, int g, int b) {
        float t = cy - s * 1.5f, bt = cy + s * 1.5f;
        float[] ex = {cx + s, cx, cx - s, cx}, ez = {cz, cz + s, cz, cz - s};
        for (int i = 0; i < 4; i++) {
            int j = (i + 1) & 3;
            tri(vc, m, cx, t, cz, ex[i], cy, ez[i], ex[j], cy, ez[j], r, g, b);          // the top, counter-clockwise from outside (y is down)
            tri(vc, m, cx, bt, cz, ex[j], cy, ez[j], ex[i], cy, ez[i], r, g, b);         // the bottom
        }
    }

    private static void tri(VertexConsumer vc, Matrix4f m, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, int r, int g, int b) {
        vert(vc, m, ax, ay, az, SOFT_U, SOFT_V, r, g, b);
        vert(vc, m, bx, by, bz, SOFT_U, SOFT_V, r, g, b);
        vert(vc, m, cx, cy, cz, SOFT_U, SOFT_V, r, g, b);
        vert(vc, m, cx, cy, cz, SOFT_U, SOFT_V, r, g, b);
    }

    /** The rune ring: a flat annulus turning slowly, drawn on both faces, sampling a different slice of the rune tile per segment. */
    static void ring(VertexConsumer vc, Matrix4f m, WornAnchor a, int rgb, float age, float pulse) {
        float[] au = AURA[a.ordinal()];
        float k = 0.55f + 0.25f * (float) Math.sin(age * 0.07f) + pulse * 0.5f;
        int r = ch(rgb, 16, k), g = ch(rgb, 8, k), b = ch(rgb, 0, k);
        float r0 = au[3] - 0.7f, r1 = au[3] + 0.7f, y = au[2], turn = age * 0.024f;
        for (int i = 0; i < SEGS; i++) {
            float t0 = turn + i * (6.2831855f / SEGS), t1 = turn + (i + 1) * (6.2831855f / SEGS);
            float c0 = (float) Math.cos(t0), s0 = (float) Math.sin(t0), c1 = (float) Math.cos(t1), s1 = (float) Math.sin(t1);
            float u0 = RUNE_U + (i % 6) * SLICE, u1 = u0 + SLICE;
            vert(vc, m, c0 * r0, y, s0 * r0, u0, RUNE_V + ROW, r, g, b);
            vert(vc, m, c1 * r0, y, s1 * r0, u1, RUNE_V + ROW, r, g, b);
            vert(vc, m, c1 * r1, y, s1 * r1, u1, RUNE_V, r, g, b);
            vert(vc, m, c0 * r1, y, s0 * r1, u0, RUNE_V, r, g, b);
            vert(vc, m, c0 * r1, y, s0 * r1, u0, RUNE_V, r, g, b);                    // the other face, so the ring shows from above and below
            vert(vc, m, c1 * r1, y, s1 * r1, u1, RUNE_V, r, g, b);
            vert(vc, m, c1 * r0, y, s1 * r0, u1, RUNE_V + ROW, r, g, b);
            vert(vc, m, c0 * r0, y, s0 * r0, u0, RUNE_V + ROW, r, g, b);
        }
    }

    /** The halo: a short upright band that is bright at its foot and fades up, whose pattern slides round it. */
    static void halo(VertexConsumer vc, Matrix4f m, WornAnchor a, int rgb, float age, float pulse) {
        float[] au = AURA[a.ordinal()];
        if (au[5] <= 0f) return;
        float k = 0.6f + 0.2f * (float) Math.sin(age * 0.06f + 1f) + pulse * 0.5f;
        int r = ch(rgb, 16, k), g = ch(rgb, 8, k), b = ch(rgb, 0, k), r2 = ch(rgb, 16, k * 0.15f), g2 = ch(rgb, 8, k * 0.15f), b2 = ch(rgb, 0, k * 0.15f);
        float y0 = au[4], y1 = au[4] - 1.8f, rad = au[5];
        int shift = (int) (age * 0.3f);
        for (int i = 0; i < SEGS; i++) {
            float t0 = i * (6.2831855f / SEGS), t1 = (i + 1) * (6.2831855f / SEGS);
            float c0 = (float) Math.cos(t0) * rad, s0 = (float) Math.sin(t0) * rad, c1 = (float) Math.cos(t1) * rad, s1 = (float) Math.sin(t1) * rad;
            float u0 = FLAME_U + ((i + shift) % 6) * SLICE, u1 = u0 + SLICE;
            vert(vc, m, c0, y0, s0, u0, FLAME_V + ROW, r, g, b);
            vert(vc, m, c1, y0, s1, u1, FLAME_V + ROW, r, g, b);
            vert(vc, m, c1, y1, s1, u1, FLAME_V, r2, g2, b2);
            vert(vc, m, c0, y1, s0, u0, FLAME_V, r2, g2, b2);
            vert(vc, m, c0, y1, s0, u0, FLAME_V, r2, g2, b2);
            vert(vc, m, c1, y1, s1, u1, FLAME_V, r2, g2, b2);
            vert(vc, m, c1, y0, s1, u1, FLAME_V + ROW, r, g, b);
            vert(vc, m, c0, y0, s0, u0, FLAME_V + ROW, r, g, b);
        }
    }
}
