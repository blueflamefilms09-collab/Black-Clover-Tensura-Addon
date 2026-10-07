package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;

/**
 * 0.53: small solid-geometry helpers for props and auras, in block units, y up, straight into a vertex consumer of an ENTITY-format
 * render type (RenderType.entityCutoutNoCull / entityTranslucent / eyes ... : position, colour, uv, overlay, light, normal).
 * Every surface faces OUTWARD; use a no-cull render type when you want to see the inside (a bubble seen from within, a dome).
 * Colours are ARGB. UVs: boxes map the whole texture to each face; spheres map u round (0..1) and v pole to pole; cylinders map u round
 * and v along; tori u round the ring and v round the tube.
 */
public final class PropDraw {
    private PropDraw() {}

    public static final int FULL_BRIGHT = 0xF000F0;

    private static void vert(PoseStack.Pose p, VertexConsumer vc, float x, float y, float z, float u, float v, int argb, int light, float nx, float ny, float nz) {
        vc.addVertex(p, x, y, z).setColor((argb >> 16) & 255, (argb >> 8) & 255, argb & 255, argb >>> 24)
                .setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(p, nx, ny, nz);
    }

    /** A flat quad a, b, c, d (counter-clockwise seen from the front) with UV a=(u0,v1) b=(u1,v1) c=(u1,v0) d=(u0,v0). */
    public static void quad(PoseStack pose, VertexConsumer vc, float ax, float ay, float az, float bx, float by, float bz,
                            float cx, float cy, float cz, float dx, float dy, float dz, float u0, float v0, float u1, float v1, int argb, int light) {
        float ux = bx - ax, uy = by - ay, uz = bz - az, wx = dx - ax, wy = dy - ay, wz = dz - az;
        float nx = uy * wz - uz * wy, ny = uz * wx - ux * wz, nz = ux * wy - uy * wx;
        float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (l < 1e-9f) return;
        nx /= l; ny /= l; nz /= l;
        PoseStack.Pose p = pose.last();
        vert(p, vc, ax, ay, az, u0, v1, argb, light, nx, ny, nz);
        vert(p, vc, bx, by, bz, u1, v1, argb, light, nx, ny, nz);
        vert(p, vc, cx, cy, cz, u1, v0, argb, light, nx, ny, nz);
        vert(p, vc, dx, dy, dz, u0, v0, argb, light, nx, ny, nz);
    }

    /** An axis-aligned box from (x0,y0,z0) to (x1,y1,z1), the whole texture on each face. */
    public static void box(PoseStack pose, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1, int argb, int light) {
        boxUv(pose, vc, x0, y0, z0, x1, y1, z1, 0f, 0f, 1f, 1f, argb, light);
    }

    /** The same, each face showing the texture part (u0,v0)-(u1,v1). */
    public static void boxUv(PoseStack pose, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1,
                             float u0, float v0, float u1, float v1, int argb, int light) {
        quad(pose, vc, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, u0, v0, u1, v1, argb, light);      // up
        quad(pose, vc, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, u0, v0, u1, v1, argb, light);      // down
        quad(pose, vc, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, u0, v0, u1, v1, argb, light);      // north (-z)
        quad(pose, vc, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, u0, v0, u1, v1, argb, light);      // south (+z)
        quad(pose, vc, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, u0, v0, u1, v1, argb, light);      // west (-x)
        quad(pose, vc, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, u0, v0, u1, v1, argb, light);      // east (+x)
    }

    /** A UV sphere with smooth normals. Typical: rings 8, segs 12 (96 quads). */
    public static void sphere(PoseStack pose, VertexConsumer vc, float cx, float cy, float cz, float r, int rings, int segs, int argb, int light) {
        ellipsoid(pose, vc, cx, cy, cz, r, r, r, rings, segs, argb, light);
    }

    public static void ellipsoid(PoseStack pose, VertexConsumer vc, float cx, float cy, float cz, float rx, float ry, float rz, int rings, int segs, int argb, int light) {
        PoseStack.Pose p = pose.last();
        for (int i = 0; i < rings; i++) {
            double t0 = Math.PI * i / rings, t1 = Math.PI * (i + 1) / rings;
            for (int j = 0; j < segs; j++) {
                double f0 = Math.PI * 2 * j / segs, f1 = Math.PI * 2 * (j + 1) / segs;
                float[][] n = {sph(t0, f0), sph(t0, f1), sph(t1, f1), sph(t1, f0)};
                float[] u = {j / (float) segs, (j + 1) / (float) segs, (j + 1) / (float) segs, j / (float) segs}, v = {i / (float) rings, i / (float) rings, (i + 1) / (float) rings, (i + 1) / (float) rings};
                for (int k : new int[]{0, 1, 2, 3}) {                                    // outward counter-clockwise (verified: (b-a) x (d-a) points away from the centre)
                    float[] q = n[k];
                    vert(p, vc, cx + q[0] * rx, cy + q[1] * ry, cz + q[2] * rz, u[k], v[k], argb, light, q[0], q[1], q[2]);
                }
            }
        }
    }

    private static float[] sph(double theta, double phi) {
        return new float[]{(float) (Math.sin(theta) * Math.cos(phi)), (float) Math.cos(theta), (float) (Math.sin(theta) * Math.sin(phi))};
    }

    /** A cylinder / cone / frustum from (ax,ay,az) with radius r0 to (bx,by,bz) with radius r1 (r1 = 0 makes a cone), segs sides, optional end caps. */
    public static void cylinder(PoseStack pose, VertexConsumer vc, float ax, float ay, float az, float bx, float by, float bz, float r0, float r1,
                                int segs, boolean caps, int argb, int light) {
        float[] axis = {bx - ax, by - ay, bz - az};
        float len = (float) Math.sqrt(axis[0] * axis[0] + axis[1] * axis[1] + axis[2] * axis[2]);
        if (len < 1e-6f) return;
        float[] a = {axis[0] / len, axis[1] / len, axis[2] / len};
        float[] u = perp(a), w = cross(a, u);
        PoseStack.Pose p = pose.last();
        float slope = (r0 - r1) / len;                                                     // how much the surface leans (for the normal)
        for (int j = 0; j < segs; j++) {
            double f0 = Math.PI * 2 * j / segs, f1 = Math.PI * 2 * (j + 1) / segs;
            float[] d0 = dir(u, w, f0), d1 = dir(u, w, f1);
            float u0 = j / (float) segs, u1 = (j + 1) / (float) segs;
            float[][] pts = {pt(ax, ay, az, d0, r0), pt(ax, ay, az, d1, r0), pt(bx, by, bz, d1, r1), pt(bx, by, bz, d0, r1)};
            float[][] ns = {norm(d0, a, slope), norm(d1, a, slope), norm(d1, a, slope), norm(d0, a, slope)};
            float[] uu = {u0, u1, u1, u0}, vv = {0, 0, 1, 1};
            for (int k : new int[]{0, 1, 2, 3}) vert(p, vc, pts[k][0], pts[k][1], pts[k][2], uu[k], vv[k], argb, light, ns[k][0], ns[k][1], ns[k][2]);
            if (caps) {
                if (r0 > 0) { vert(p, vc, ax, ay, az, 0.5f, 0.5f, argb, light, -a[0], -a[1], -a[2]); vert(p, vc, ax, ay, az, 0.5f, 0.5f, argb, light, -a[0], -a[1], -a[2]);
                    vert(p, vc, pts[1][0], pts[1][1], pts[1][2], 0f, 0f, argb, light, -a[0], -a[1], -a[2]); vert(p, vc, pts[0][0], pts[0][1], pts[0][2], 1f, 0f, argb, light, -a[0], -a[1], -a[2]); }
                if (r1 > 0) { vert(p, vc, bx, by, bz, 0.5f, 0.5f, argb, light, a[0], a[1], a[2]); vert(p, vc, bx, by, bz, 0.5f, 0.5f, argb, light, a[0], a[1], a[2]);
                    vert(p, vc, pts[3][0], pts[3][1], pts[3][2], 0f, 0f, argb, light, a[0], a[1], a[2]); vert(p, vc, pts[2][0], pts[2][1], pts[2][2], 1f, 0f, argb, light, a[0], a[1], a[2]); }
            }
        }
    }

    /** A torus: centre, axis (the ring lies in the plane perpendicular to it), major radius R, tube radius r. */
    public static void torus(PoseStack pose, VertexConsumer vc, float cx, float cy, float cz, float nx, float ny, float nz, float R, float r,
                             int ringSegs, int tubeSegs, int argb, int light) {
        float l = (float) Math.sqrt(nx * nx + ny * ny + nz * nz);
        if (l < 1e-6f) return;
        float[] n = {nx / l, ny / l, nz / l}, u = perp(n), w = cross(n, u);
        PoseStack.Pose p = pose.last();
        for (int i = 0; i < ringSegs; i++) {
            double a0 = Math.PI * 2 * i / ringSegs, a1 = Math.PI * 2 * (i + 1) / ringSegs;
            float[] d0 = dir(u, w, a0), d1 = dir(u, w, a1);
            for (int j = 0; j < tubeSegs; j++) {
                double b0 = Math.PI * 2 * j / tubeSegs, b1 = Math.PI * 2 * (j + 1) / tubeSegs;
                float[][] q = {tp(d0, n, b0), tp(d1, n, b0), tp(d1, n, b1), tp(d0, n, b1)};
                float[][] c = {{d0[0], d0[1], d0[2]}, {d1[0], d1[1], d1[2]}, {d1[0], d1[1], d1[2]}, {d0[0], d0[1], d0[2]}};
                float[] uu = {i / (float) ringSegs, (i + 1) / (float) ringSegs, (i + 1) / (float) ringSegs, i / (float) ringSegs}, vv = {j / (float) tubeSegs, j / (float) tubeSegs, (j + 1) / (float) tubeSegs, (j + 1) / (float) tubeSegs};
                for (int k : new int[]{0, 1, 2, 3}) {
                    float[] o = q[k];                                                       // o = unit outward normal of the tube surface
                    vert(p, vc, cx + c[k][0] * R + o[0] * r, cy + c[k][1] * R + o[1] * r, cz + c[k][2] * R + o[2] * r, uu[k], vv[k], argb, light, o[0], o[1], o[2]);
                }
            }
        }
    }

    /** A hexagonal crystal: a prism from the base centre up to height h, with a pointed tip of height tip, radius r. Lean it with the pose. */
    public static void crystal(PoseStack pose, VertexConsumer vc, float bx, float by, float bz, float r, float h, float tip, int argb, int light) {
        cylinder(pose, vc, bx, by, bz, bx, by + h, bz, r, r, 6, false, argb, light);
        if (tip > 0) cylinder(pose, vc, bx, by + h, bz, bx, by + h + tip, bz, r, 0f, 6, false, argb, light);
        cylinder(pose, vc, bx, by, bz, bx, by - 0.001f, bz, r * 0.001f, r, 6, false, argb, light);
    }

    /** A tube of radius r0 (start) to r1 (end) along a polyline of points {x,y,z}: chains, vines, ropes, tentacles. */
    public static void tube(PoseStack pose, VertexConsumer vc, float[][] pts, float r0, float r1, int segs, int argb, int light) {
        for (int i = 0; i + 1 < pts.length; i++) {
            float t0 = i / (float) (pts.length - 1), t1 = (i + 1) / (float) (pts.length - 1);
            cylinder(pose, vc, pts[i][0], pts[i][1], pts[i][2], pts[i + 1][0], pts[i + 1][1], pts[i + 1][2], r0 + (r1 - r0) * t0, r0 + (r1 - r0) * t1, segs, false, argb, light);
        }
    }

    // ---------------------------------------------------------------- vector bits
    private static float[] perp(float[] a) {
        float[] t = Math.abs(a[1]) < 0.9f ? new float[]{0, 1, 0} : new float[]{1, 0, 0};
        float[] u = cross(a, t);
        float l = (float) Math.sqrt(u[0] * u[0] + u[1] * u[1] + u[2] * u[2]);
        return new float[]{u[0] / l, u[1] / l, u[2] / l};
    }

    private static float[] cross(float[] a, float[] b) { return new float[]{a[1] * b[2] - a[2] * b[1], a[2] * b[0] - a[0] * b[2], a[0] * b[1] - a[1] * b[0]}; }

    private static float[] dir(float[] u, float[] w, double f) {
        float c = (float) Math.cos(f), s = (float) Math.sin(f);
        return new float[]{u[0] * c + w[0] * s, u[1] * c + w[1] * s, u[2] * c + w[2] * s};
    }

    private static float[] pt(float x, float y, float z, float[] d, float r) { return new float[]{x + d[0] * r, y + d[1] * r, z + d[2] * r}; }

    /** The outward normal of a cylinder side: the radial direction tilted along the axis by the slope. */
    private static float[] norm(float[] d, float[] a, float slope) {
        float x = d[0] + a[0] * slope, y = d[1] + a[1] * slope, z = d[2] + a[2] * slope;
        float l = (float) Math.sqrt(x * x + y * y + z * z);
        return new float[]{x / l, y / l, z / l};
    }

    /** A point of the unit tube cross-section: radial direction d, ring axis n, angle b. */
    private static float[] tp(float[] d, float[] n, double b) {
        float c = (float) Math.cos(b), s = (float) Math.sin(b);
        return new float[]{d[0] * c + n[0] * s, d[1] * c + n[1] * s, d[2] * c + n[2] * s};
    }
}
