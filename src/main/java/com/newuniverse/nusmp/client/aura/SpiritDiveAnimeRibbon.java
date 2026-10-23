package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.book.SpiritDiveAnimeGeometry;
import com.newuniverse.nusmp.book.SpiritDiveAnimeGeometry.Shape;
import com.newuniverse.nusmp.client.prop.SealDraw;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Spirit Dive, ANIME look: draws a smooth strip along a spine (the plume sheets, the locks of hair, the trails) as subdivided quads that SHARE their edge vertices, so the strip is one continuous
 * curved surface, never a row of separate cards. The maths is {@link SpiritDiveAnimeGeometry}; this class owns the scratch arrays and writes the vertices. Per vertex alpha gives the soft fade at the
 * root; the UV runs 0..1 across the strip and from the root (v = 0) to the tip (v = 1). The winding follows {@link SpiritDiveAnimeGeometry#facing}, so the culling additive types show the strip's
 * front. No allocation: the scratch arrays are statics (the render thread is the only user; a strip is finished before the next starts).
 */
final class SpiritDiveAnimeRibbon {
    private SpiritDiveAnimeRibbon() {}

    private static final int N = SpiritDiveAnimeGeometry.MAX_SEGS + 1;
    /** The spine of the strip being built (3 floats per point): the caller fills it, then calls {@link #strip}. */
    static final float[] SPINE = new float[N * 3];
    private static final float[] LEFT = new float[N * 3], RIGHT = new float[N * 3], WIDTH = new float[N];
    private static final float[] LOOK = new float[3];

    /** Reads the camera's look vector in the space of the current pose (call after the pose is set for the strips, before the first strip). */
    static void camera(PoseStack pose) {
        SealDraw.camera(pose);
        LOOK[0] = SealDraw.CAM[6]; LOOK[1] = SealDraw.CAM[7]; LOOK[2] = SealDraw.CAM[8];
    }

    /**
     * Writes one strip of {@code segs} quads along {@link #SPINE} (segs + 1 points) into {@code vc}.
     *
     * @param width   the half width at the widest point, px
     * @param shape   the profile
     * @param ph      the phase of the wobble of a flame's edge
     * @param blend   0..1 how much the strip turns to face the viewer (0 a fixed sheet, 1 a billboard)
     * @param n       the sheet's own normal (any sign; it is turned to face the viewer)
     * @param argb    the colour; its alpha is the strip's alpha before the fades
     * @param fadeIn  s below which the alpha fades in from 0 (the root grows out of the body)
     */
    static void strip(PoseStack pose, VertexConsumer vc, int segs, float width, Shape shape, float ph, float blend, float nx, float ny, float nz, int argb, float fadeIn, int light) {
        int pts = segs + 1;
        if (LOOK[0] * nx + LOOK[1] * ny + LOOK[2] * nz > 0f) { nx = -nx; ny = -ny; nz = -nz; }          // the normal faces the viewer
        for (int k = 0; k < pts; k++) WIDTH[k] = width * SpiritDiveAnimeGeometry.profile(shape, k / (float) segs, ph);
        SpiritDiveAnimeGeometry.edges(SPINE, pts, WIDTH, nx, ny, nz, LOOK, blend, LEFT, RIGHT);
        boolean front = SpiritDiveAnimeGeometry.facing(LEFT, RIGHT, pts, LOOK);
        PoseStack.Pose p = pose.last();
        int a0 = argb >>> 24, rgb = argb & 0xFFFFFF;
        for (int k = 0; k < segs; k++) {
            float s0 = k / (float) segs, s1 = (k + 1) / (float) segs;
            int c0 = (Math.round(a0 * fade(s0, fadeIn)) << 24) | rgb, c1 = (Math.round(a0 * fade(s1, fadeIn)) << 24) | rgb;
            int i = k * 3, j = i + 3;
            if (front) {
                v(p, vc, LEFT, i, 0f, s0, c0, light, nx, ny, nz);
                v(p, vc, RIGHT, i, 1f, s0, c0, light, nx, ny, nz);
                v(p, vc, RIGHT, j, 1f, s1, c1, light, nx, ny, nz);
                v(p, vc, LEFT, j, 0f, s1, c1, light, nx, ny, nz);
            } else {
                v(p, vc, LEFT, i, 0f, s0, c0, light, nx, ny, nz);
                v(p, vc, LEFT, j, 0f, s1, c1, light, nx, ny, nz);
                v(p, vc, RIGHT, j, 1f, s1, c1, light, nx, ny, nz);
                v(p, vc, RIGHT, i, 1f, s0, c0, light, nx, ny, nz);
            }
        }
    }

    private static float fade(float s, float fadeIn) { return fadeIn <= 0f || s >= fadeIn ? 1f : s / fadeIn; }

    private static void v(PoseStack.Pose p, VertexConsumer vc, float[] a, int i, float u, float vv, int argb, int light, float nx, float ny, float nz) {
        vc.addVertex(p, a[i], a[i + 1], a[i + 2]).setColor((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, argb >>> 24)
                .setUv(u, vv).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
    }
}
