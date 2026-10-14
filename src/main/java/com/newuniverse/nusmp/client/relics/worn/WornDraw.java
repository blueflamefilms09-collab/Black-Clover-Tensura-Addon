package com.newuniverse.nusmp.client.relics.worn;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * Small solid shapes for the worn boss relics, written straight into an entity-format vertex consumer (RenderType.entityCutoutNoCull,
 * entityTranslucentEmissive, eyes: position, colour, uv, overlay, light, normal) over the one-pixel white texture. Units are model units (pixels);
 * the caller scales the pose by 1/16 first ({@link #PX}). Normals are worked out from each polygon's corners, so every shape is lit like a solid.
 * Colours are 0xAARRGGBB.
 */
final class WornDraw {
    private WornDraw() {}

    static final float PX = 1f / 16f;

    static int argb(int rgb, float alpha) { return (Math.max(0, Math.min(255, (int) (alpha * 255f))) << 24) | (rgb & 0xFFFFFF); }

    /** Blends two 0xRRGGBB colours, {@code t} 0 = a, 1 = b. */
    static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1f - t) + ((b >> 16) & 255) * t), g = (int) (((a >> 8) & 255) * (1f - t) + ((b >> 8) & 255) * t),
                bl = (int) ((a & 255) * (1f - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }

    private static void vert(PoseStack.Pose p, VertexConsumer vc, int light, int argb, float x, float y, float z, float nx, float ny, float nz) {
        vc.addVertex(p, x, y, z).setColor((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, argb >>> 24).setUv(0.5f, 0.5f)
                .setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
    }

    /** One quad a-b-c-d (a triangle when c == d); the normal comes from the corners' winding. */
    static void quad(PoseStack.Pose p, VertexConsumer vc, int light, int argb,
                     float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz, float dx, float dy, float dz) {
        float ux = bx - ax, uy = by - ay, uz = bz - az, wx = cx - ax, wy = cy - ay, wz = cz - az;
        float nx = uy * wz - uz * wy, ny = uz * wx - ux * wz, nz = ux * wy - uy * wx;
        float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (l < 1e-9f) {                                                            // a sliver: use the other diagonal
            wx = dx - ax; wy = dy - ay; wz = dz - az;
            nx = uy * wz - uz * wy; ny = uz * wx - ux * wz; nz = ux * wy - uy * wx;
            l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
            if (l < 1e-9f) return;
        }
        nx /= l; ny /= l; nz /= l;
        vert(p, vc, light, argb, ax, ay, az, nx, ny, nz);
        vert(p, vc, light, argb, bx, by, bz, nx, ny, nz);
        vert(p, vc, light, argb, cx, cy, cz, nx, ny, nz);
        vert(p, vc, light, argb, dx, dy, dz, nx, ny, nz);
    }

    static void tri(PoseStack.Pose p, VertexConsumer vc, int light, int argb, float ax, float ay, float az, float bx, float by, float bz, float cx, float cy, float cz) {
        quad(p, vc, light, argb, ax, ay, az, bx, by, bz, cx, cy, cz, cx, cy, cz);
    }

    /** An axis-aligned box. */
    static void box(PoseStack.Pose p, VertexConsumer vc, int light, int argb, float x0, float y0, float z0, float x1, float y1, float z1) {
        quad(p, vc, light, argb, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0);      // bottom (y is down in model space)
        quad(p, vc, light, argb, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1);      // top
        quad(p, vc, light, argb, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0);      // front (-z)
        quad(p, vc, light, argb, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1);      // back
        quad(p, vc, light, argb, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0);      // -x side
        quad(p, vc, light, argb, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1);      // +x side
    }

    /** A four-sided pyramid: the square base of half-sizes (hx, hz) centred at (cx, cy, cz) and the tip at (tx, ty, tz). */
    static void pyramid(PoseStack.Pose p, VertexConsumer vc, int light, int argb, float cx, float cy, float cz, float hx, float hz, float tx, float ty, float tz) {
        float x0 = cx - hx, x1 = cx + hx, z0 = cz - hz, z1 = cz + hz;
        tri(p, vc, light, argb, x0, cy, z0, x1, cy, z0, tx, ty, tz);
        tri(p, vc, light, argb, x1, cy, z0, x1, cy, z1, tx, ty, tz);
        tri(p, vc, light, argb, x1, cy, z1, x0, cy, z1, tx, ty, tz);
        tri(p, vc, light, argb, x0, cy, z1, x0, cy, z0, tx, ty, tz);
        quad(p, vc, light, argb, x0, cy, z1, x1, cy, z1, x1, cy, z0, x0, cy, z0);       // the underside
    }

    /** An elongated octahedron (a crystal shard): half-sizes (rx, ry, rz), turned {@code yawDeg} about the vertical axis. */
    static void diamond(PoseStack.Pose p, VertexConsumer vc, int light, int argb, float cx, float cy, float cz, float rx, float ry, float rz, float yawDeg) {
        double a = Math.toRadians(yawDeg);
        float c = (float) Math.cos(a), s = (float) Math.sin(a);
        float[][] eq = new float[4][];
        float[][] base = {{rx, 0}, {0, rz}, {-rx, 0}, {0, -rz}};
        for (int i = 0; i < 4; i++) eq[i] = new float[]{cx + base[i][0] * c - base[i][1] * s, cy, cz + base[i][0] * s + base[i][1] * c};
        for (int i = 0; i < 4; i++) {
            float[] a0 = eq[i], a1 = eq[(i + 1) % 4];
            tri(p, vc, light, argb, a0[0], a0[1], a0[2], a1[0], a1[1], a1[2], cx, cy - ry, cz);
            tri(p, vc, light, argb, a1[0], a1[1], a1[2], a0[0], a0[1], a0[2], cx, cy + ry, cz);
        }
    }

    /**
     * A flat ring in the x-z plane at y = 0 (turn the pose to tilt it), between radii r0 and r1, in {@code n} segments, with the alpha of the inner
     * edge and of the outer edge given separately (a soft rim). {@code rgb} is 0xRRGGBB.
     */
    static void annulus(PoseStack.Pose p, VertexConsumer vc, int light, int rgb, float aInner, float aOuter, float r0, float r1, int n, float phaseDeg) {
        int segs = Math.max(3, n);
        for (int i = 0; i < segs; i++) {
            double t0 = Math.toRadians(phaseDeg + 360.0 * i / segs), t1 = Math.toRadians(phaseDeg + 360.0 * (i + 1) / segs);
            float c0 = (float) Math.cos(t0), s0 = (float) Math.sin(t0), c1 = (float) Math.cos(t1), s1 = (float) Math.sin(t1);
            int in = argb(rgb, aInner), out = argb(rgb, aOuter);
            float[] xs = {c0 * r0, c1 * r0, c1 * r1, c0 * r1}, zs = {s0 * r0, s1 * r0, s1 * r1, s0 * r1};
            int[] col = {in, in, out, out};
            for (int k = 0; k < 4; k++) vert(p, vc, light, col[k], xs[k], 0f, zs[k], 0f, -1f, 0f);
            for (int k = 3; k >= 0; k--) vert(p, vc, light, col[k], xs[k], 0f, zs[k], 0f, 1f, 0f);      // and the other face, so it shows from below
        }
    }
}
