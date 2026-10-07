package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Vector3f;

/**
 * 0.54 Corundum Magic: the small camera-facing glints, ribbons and flat rings of the Corundum props and the Corundum aura (own copy of the helpers,
 * so the Corundum look never depends on another magic's files). Everything goes through {@link PropDraw#quad}, so it fits entity-format render
 * types. The scratch fields are only touched on the render thread and nothing here allocates per call.
 */
public final class CorundumDraw {
    private CorundumDraw() {}

    private static final Matrix3f INV = new Matrix3f();
    private static final Vector3f TMP = new Vector3f();
    /** The camera's right, up and forward vectors in the space of the pose given to {@link #camera}: {rx, ry, rz, ux, uy, uz, fx, fy, fz}. */
    public static final float[] CAM = {1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, -1f};

    /** Reads the camera into {@link #CAM}, expressed in the current pose space (call it once per paint, with the pose you will draw in). */
    public static void camera(PoseStack pose) {
        try {
            Camera cam = Minecraft.getInstance().gameRenderer.getMainCamera();
            pose.last().normal().transpose(INV);                 // rotation only: the transpose is the inverse
            put(0, TMP.set(cam.getLeftVector()).negate());
            put(3, TMP.set(cam.getUpVector()));
            put(6, TMP.set(cam.getLookVector()));
        } catch (RuntimeException ex) {
            CAM[0] = 1f; CAM[1] = 0f; CAM[2] = 0f; CAM[3] = 0f; CAM[4] = 1f; CAM[5] = 0f; CAM[6] = 0f; CAM[7] = 0f; CAM[8] = -1f;
        }
    }

    private static void put(int at, Vector3f v) {
        INV.transform(v);
        float l = Mth.sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
        if (l < 1e-6f) l = 1f;
        CAM[at] = v.x / l; CAM[at + 1] = v.y / l; CAM[at + 2] = v.z / l;
    }

    /** A parallelogram centred on (cx,cy,cz) with the half-extent vectors u and v (v = the texture's up); the front is the side u x v points to. */
    public static void plane(PoseStack pose, VertexConsumer vc, float cx, float cy, float cz, float ux, float uy, float uz,
                             float vx, float vy, float vz, boolean back, int argb, int light) {
        if (!back) {
            PropDraw.quad(pose, vc, cx - ux - vx, cy - uy - vy, cz - uz - vz, cx + ux - vx, cy + uy - vy, cz + uz - vz,
                    cx + ux + vx, cy + uy + vy, cz + uz + vz, cx - ux + vx, cy - uy + vy, cz - uz + vz, 0f, 0f, 1f, 1f, argb, light);
        } else {
            PropDraw.quad(pose, vc, cx - ux - vx, cy - uy - vy, cz - uz - vz, cx - ux + vx, cy - uy + vy, cz - uz + vz,
                    cx + ux + vx, cy + uy + vy, cz + uz + vz, cx + ux - vx, cy + uy - vy, cz + uz - vz, 0f, 0f, 1f, 1f, argb, light);
        }
    }

    /** A flat square in the xz plane at the current height (seen from above and below), half the given size, turned by rotDeg about y. */
    public static void flat(PoseStack pose, VertexConsumer vc, float half, float rotDeg, int argb, int light) {
        float c = Mth.cos(rotDeg * Mth.DEG_TO_RAD) * half, s = Mth.sin(rotDeg * Mth.DEG_TO_RAD) * half;
        plane(pose, vc, 0f, 0f, 0f, c, 0f, s, -s, 0f, c, false, argb, light);
        plane(pose, vc, 0f, 0f, 0f, c, 0f, s, -s, 0f, c, true, argb, light);
    }

    /** A sprite that always faces the camera (needs {@link #camera} first), turned by rot radians. */
    public static void glow(PoseStack pose, VertexConsumer vc, float cx, float cy, float cz, float half, float rot, int argb, int light) {
        float c = Mth.cos(rot) * half, s = Mth.sin(rot) * half;
        plane(pose, vc, cx, cy, cz, CAM[0] * c + CAM[3] * s, CAM[1] * c + CAM[4] * s, CAM[2] * c + CAM[5] * s,
                -CAM[0] * s + CAM[3] * c, -CAM[1] * s + CAM[4] * c, -CAM[2] * s + CAM[5] * c, false, argb, light);
    }

    /** A ribbon from the tail to the head that turns to face the camera (needs {@link #camera}); texture u runs ut -> uh along it, v across; hw = half width. */
    public static void ribbon(PoseStack pose, VertexConsumer vc, float tx, float ty, float tz, float hx, float hy, float hz, float hw,
                              float ut, float uh, int argb, int light) {
        float dx = hx - tx, dy = hy - ty, dz = hz - tz;
        float wx = dy * CAM[8] - dz * CAM[7], wy = dz * CAM[6] - dx * CAM[8], wz = dx * CAM[7] - dy * CAM[6];
        float l = Mth.sqrt(wx * wx + wy * wy + wz * wz);
        if (l < 1e-5f) { wx = CAM[0]; wy = CAM[1]; wz = CAM[2]; l = 1f; }
        wx = wx / l * hw; wy = wy / l * hw; wz = wz / l * hw;
        PropDraw.quad(pose, vc, tx - wx, ty - wy, tz - wz, hx - wx, hy - wy, hz - wz, hx + wx, hy + wy, hz + wz, tx + wx, ty + wy, tz + wz,
                ut, 0f, uh, 1f, argb, light);
    }

    /**
     * A streak (a tall texture such as corundum_streak) from the tail point to the head point that turns to face the camera (needs {@link #camera}):
     * the texture's up runs along it, hw is the half width.
     */
    public static void streak(PoseStack pose, VertexConsumer vc, float tx, float ty, float tz, float hx, float hy, float hz, float hw, int argb, int light) {
        float dx = (hx - tx) * 0.5f, dy = (hy - ty) * 0.5f, dz = (hz - tz) * 0.5f;
        float wx = dy * CAM[8] - dz * CAM[7], wy = dz * CAM[6] - dx * CAM[8], wz = dx * CAM[7] - dy * CAM[6];
        float l = Mth.sqrt(wx * wx + wy * wy + wz * wz);
        if (l < 1e-5f) { wx = CAM[0]; wy = CAM[1]; wz = CAM[2]; l = 1f; }
        wx = wx / l * hw; wy = wy / l * hw; wz = wz / l * hw;
        plane(pose, vc, tx + dx, ty + dy, tz + dz, wx, wy, wz, dx, dy, dz, false, argb, light);
    }

    public static float smooth(float x) {
        x = Mth.clamp(x, 0f, 1f);
        return x * x * (3f - 2f * x);
    }

    /** ARGB from a 0..1 alpha and an RGB colour. */
    public static int argb(float a, int rgb) { return (Mth.clamp((int) (a * 255f + 0.5f), 0, 255) << 24) | (rgb & 0xFFFFFF); }

    /** Mixes two RGB colours (t = 0 is a). */
    public static int mix(int a, int b, float t) {
        int r = (int) (((a >> 16) & 255) * (1f - t) + ((b >> 16) & 255) * t), g = (int) (((a >> 8) & 255) * (1f - t) + ((b >> 8) & 255) * t),
                bl = (int) ((a & 255) * (1f - t) + (b & 255) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
