package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.client.geo.GeoDraw;
import com.newuniverse.nusmp.client.geo.GeoSpec;
import com.newuniverse.nusmp.client.prop.PropDraw;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;
import org.joml.Matrix3f;
import org.joml.Vector3f;

/**
 * 1.93.0 magic rebuild: the shared plumbing of the five rebuilt temporary armours (Spatial, Gravity, Permeation, Mirror, Slash). One armour is a SET of
 * GeoLite pieces (helm, chest, two arm pieces, two leg pieces, a back piece, a halo piece), each authored on the standing humanoid and drawn on the
 * posed model part it was built for, so it follows every animation. Every piece goes through {@link EquipAnimation}, the Djinn Full Equip's own
 * forge-in / shatter-out: staggered by a per-piece delay (0 to 12 ticks), flying in from its own side while it fades up and grows 85 to 100 percent
 * over 8 ticks, and shattering out over the last 18 ticks. A piece that is fully on is drawn with the solid CUTOUT render type; while it forges or
 * shatters it is drawn TRANSLUCENT (cutout cannot fade); a "ghost" piece (glass, wisps) is always translucent. The glow map is drawn straight after
 * the piece, additive and full bright, faded with the piece. The arm pieces are authored 5 px wide round the 4 px (wide) arm, so they also cover the
 * 3 px (slim) arm with a small gap instead of cutting into it. Also here: the camera-facing glints, flat rings and ribbons the five painters share
 * (own copy of the helpers, like the other magics' draw classes).
 */
public final class SpatialArmourKit {
    private SpatialArmourKit() {}

    public static final int FB = PropDraw.FULL_BRIGHT;

    // ================================================================ pieces
    /** How one piece is drawn: ghost = always translucent at {@code ghostAlpha}; glow = ARGB of the additive glow pass (alpha 0 = none). */
    public record Style(boolean ghost, float ghostAlpha, int glowRgb, float glowK) {
        public static Style solid(int glowRgb, float glowK) { return new Style(false, 1f, glowRgb, glowK); }
        public static Style ghost(float alpha, int glowRgb, float glowK) { return new Style(true, alpha, glowRgb, glowK); }
    }

    /** True for the local player seen from their own eyes (head and shoulder pieces are then left out). */
    public static boolean self1p(AuraContext c) {
        Minecraft mc = Minecraft.getInstance();
        return c.player() == mc.player && mc.options.getCameraType().isFirstPerson();
    }

    /**
     * One piece on one model part. {@code (dx, dy, dz)} is the direction (model pixels, y down, front -z) the piece flies in from, {@code delay} its
     * stagger in ticks (0 to 12), {@code (ox, oy, oz)} an extra offset in model pixels applied in the part's frame (a piece that floats away from the body).
     */
    public static void piece(AuraContext c, ModelPart anchor, GeoSpec spec, String clip, float delay, float dx, float dy, float dz,
                             float ox, float oy, float oz, Style st) {
        float a = EquipAnimation.assembled(c, delay);
        if (a == 0f) return;
        drawPiece(c.pose(), c.buffers(), c.light(), c.age(), a, anchor, spec, clip, dx, dy, dz, ox, oy, oz, st);
    }

    /**
     * The same piece on any posed humanoid model (the Real Doubles wear the Glass Garb): {@code life} ticks since the wearer appeared (the piece forges in through
     * EquipAnimation after its delay and is never shattered: the wearer's own end does that), {@code age} the animation clock in ticks.
     */
    public static void pieceOn(PoseStack pose, MultiBufferSource buffers, int light, float life, float age, ModelPart anchor, GeoSpec spec, float delay,
                               float dx, float dy, float dz, Style st) {
        float a = EquipAnimation.assembled(life, 1.0e9f, delay);
        if (a == 0f) return;
        drawPiece(pose, buffers, light, age, a, anchor, spec, "idle", dx, dy, dz, 0f, 0f, 0f, st);
    }

    private static void drawPiece(PoseStack pose, MultiBufferSource buffers, int light, float age, float a, ModelPart anchor, GeoSpec spec, String clip,
                                  float dx, float dy, float dz, float ox, float oy, float oz, Style st) {
        pose.pushPose();
        anchor.translateAndRotate(pose);
        pose.scale(1f / 16f, 1f / 16f, 1f / 16f);
        if (ox != 0f || oy != 0f || oz != 0f) pose.translate(ox, oy, oz);
        float alpha = EquipAnimation.begin(pose, a, dx, dy, dz);
        pose.translate(-anchor.x, -anchor.y, -anchor.z);
        pose.scale(16f, 16f, 16f);
        float k = st.ghost ? alpha * st.ghostAlpha : alpha;
        boolean solid = !st.ghost && a >= 1f;
        int argb = ((int) (Mth.clamp(k, 0f, 1f) * 255f) << 24) | 0xFFFFFF;
        if (solid) argb = 0xFFFFFFFF;
        int glow = st.glowRgb == 0 || st.glowK <= 0f ? 0 : argb(Mth.clamp(st.glowK * alpha, 0f, 1f), st.glowRgb);
        GeoDraw.paint(pose, buffers, spec, clip, age / 20f, GeoDraw.Space.PLAYER, solid ? GeoDraw.Layer.CUTOUT : GeoDraw.Layer.TRANSLUCENT, light, argb, glow);
        pose.popPose();
    }

    public static void piece(AuraContext c, ModelPart anchor, GeoSpec spec, float delay, float dx, float dy, float dz, Style st) {
        piece(c, anchor, spec, "idle", delay, dx, dy, dz, 0f, 0f, 0f, st);
    }

    // ================================================================ the shared flat and sprite helpers (own copy)
    private static final Matrix3f INV = new Matrix3f();
    private static final Vector3f TMP = new Vector3f();
    /** The camera's right, up and forward vectors in the space of the pose given to {@link #camera}: {rx, ry, rz, ux, uy, uz, fx, fy, fz}. */
    public static final float[] CAM = {1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, -1f};

    /** Reads the camera into {@link #CAM}, expressed in the current pose space (call once per paint with the pose you draw in). */
    public static void camera(PoseStack pose) {
        try {
            Camera cam = Minecraft.getInstance().gameRenderer.getMainCamera();
            pose.last().normal().transpose(INV);
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

    /** A flat square in the xz plane at the current height (seen from above and below), turned by rotDeg about y. */
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

    /** A streak from the tail point to the head point that turns to face the camera (needs {@link #camera}): the texture's up runs along it, hw = half width. */
    public static void streak(PoseStack pose, VertexConsumer vc, float tx, float ty, float tz, float hx, float hy, float hz, float hw, int argb, int light) {
        float dx = (hx - tx) * 0.5f, dy = (hy - ty) * 0.5f, dz = (hz - tz) * 0.5f;
        float wx = dy * CAM[8] - dz * CAM[7], wy = dz * CAM[6] - dx * CAM[8], wz = dx * CAM[7] - dy * CAM[6];
        float l = Mth.sqrt(wx * wx + wy * wy + wz * wz);
        if (l < 1e-5f) { wx = CAM[0]; wy = CAM[1]; wz = CAM[2]; l = 1f; }
        wx = wx / l * hw; wy = wy / l * hw; wz = wz / l * hw;
        plane(pose, vc, tx + dx, ty + dy, tz + dz, wx, wy, wz, dx, dy, dz, false, argb, light);
    }

    /** A tilted flat ring (a square sprite) centred at (cx,cy,cz), normal tilted by (tiltX, tiltZ) degrees, spun by spin degrees in its own plane. */
    public static void tiltedRing(PoseStack pose, VertexConsumer vc, float cx, float cy, float cz, float half, float tiltX, float tiltZ, float spin, int argb, int light) {
        pose.pushPose();
        pose.translate(cx, cy, cz);
        pose.mulPose(com.mojang.math.Axis.ZP.rotationDegrees(tiltZ));
        pose.mulPose(com.mojang.math.Axis.XP.rotationDegrees(tiltX));
        flat(pose, vc, half, spin, argb, light);
        pose.popPose();
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
