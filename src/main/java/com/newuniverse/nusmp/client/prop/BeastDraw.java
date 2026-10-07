package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Vector3f;

/**
 * 0.59 Beast Magic: the camera-facing sprites and small helpers the beast prop painter and the beast aura share (flame tongues, glints, the
 * ground sigil). Everything goes through {@link PropDraw#quad}. The scratch fields are only touched on the render thread.
 */
public final class BeastDraw {
    private BeastDraw() {}

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

    /** A parallelogram centred on (cx,cy,cz) with the half-extent vectors u and v; the front is the side u x v points to. */
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

    /** A flat square in the xz plane at the current height (both sides), half the given size, turned by rotDeg about y. */
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

    /** One quarter-width cell (u0 .. u0 + 0.25) of an atlas, camera facing, half the given size. */
    public static void cell(PoseStack pose, VertexConsumer vc, float cx, float cy, float cz, float half, float u0, int argb, int light) {
        float[] c = CAM;
        PropDraw.quad(pose, vc, cx - c[0] * half - c[3] * half, cy - c[1] * half - c[4] * half, cz - c[2] * half - c[5] * half,
                cx + c[0] * half - c[3] * half, cy + c[1] * half - c[4] * half, cz + c[2] * half - c[5] * half,
                cx + c[0] * half + c[3] * half, cy + c[1] * half + c[4] * half, cz + c[2] * half + c[5] * half,
                cx - c[0] * half + c[3] * half, cy - c[1] * half + c[4] * half, cz - c[2] * half + c[5] * half, u0, 0f, u0 + 0.25f, 1f, argb, light);
    }

    /**
     * A flame tongue standing on (cx, cy, cz): half width hw, height h, turned to the camera about the vertical axis only (it stays upright).
     * dir is +1 in a y-up pose (props) and -1 in the player layer's y-down space.
     */
    public static void tongue(PoseStack pose, VertexConsumer vc, float cx, float cy, float cz, float hw, float h, float dir, int argb, int light) {
        float rx = CAM[0], rz = CAM[2];
        float l = Mth.sqrt(rx * rx + rz * rz);
        if (l < 1e-4f) { rx = 1f; rz = 0f; l = 1f; }
        rx = rx / l * hw; rz = rz / l * hw;
        plane(pose, vc, cx, cy + dir * h * 0.5f, cz, rx, 0f, rz, 0f, dir * h * 0.5f, 0f, false, argb, light);
    }

    /** A cheap hash to 0..1 from a seed and two counters (never allocates). */
    public static float rnd(int seed, int i, int k) {
        int h = seed * 0x9E3779B1 ^ (i * 0x85EBCA6B) ^ (k * 0xC2B2AE35);
        h ^= h >>> 15; h *= 0x2C1B3C6D; h ^= h >>> 12; h *= 0x297A2D39; h ^= h >>> 15;
        return (h & 0xFFFFFF) / (float) 0x1000000;
    }

    /** The light, lifted to at least n (0..15) for block and sky, so the glowing beast stays readable in the dark. */
    public static int lift(int light, int n) {
        int bl = Math.max((light >> 4) & 15, n), sk = Math.max((light >> 20) & 15, n);
        return (bl << 4) | (sk << 20);
    }
}
