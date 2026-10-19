package com.newuniverse.nusmp.client.playermodel;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;

/**
 * Why: the drawing half of the hair system, with no entity and no render layer in it so the Avatar creation screen's preview and the in-world layer ({@link HairLayer}) draw the same hair: the
 * scalp cap and the other static boxes with the part they hang from, and the chains with the angles of their {@link HairSpring} nested down the segments. Boxes are drawn as vanilla draws a
 * cube (the same corner order and UV layout as {@code ModelPart.Cube}, 6 faces, the texture a 64x64 sheet), tinted by the tone of the box from the {@link HairPalette.Tint}, inflated so every
 * layer sits on its own surface (the cap is 0.7 outside the head, the skin's hat layer is 0.5, so nothing shares a plane and nothing z-fights).
 *
 * <p>The pose stack must be at the entity's model origin as a render layer's is (vanilla layer space, y down, units of blocks); the part each box hangs from is posed with {@code translateAndRotate}.
 * Under a helmet the cap and the boxes marked {@code hide} are skipped and a chain draws only the segments that hang out of the helmet ({@link HairStyleData#segmentShownUnderHelmet}).
 */
public final class HairDraw {
    private HairDraw() {}

    private static final float PX = 1f / 16f;

    /**
     * Draws a style.
     *
     * @param head    the model's head part (posed by the entity)
     * @param body    the model's body part
     * @param state   the springs of this entity (null: every chain at rest)
     * @param tint    the colours of the hair
     * @param helmet  a helmet is worn
     * @param argb    a multiplier on every colour (alpha included), 0xFFFFFFFF = none
     */
    public static void draw(PoseStack pose, VertexConsumer vc, HairStyleData.Style style, ModelPart head, ModelPart body, HairState state, HairPalette.Tint tint, boolean helmet, int light,
                            int overlay, int argb) {
        for (HairStyleData.Box b : style.boxes()) {
            if (helmet && !HairStyleData.boxShownUnderHelmet(b)) continue;
            pose.pushPose();
            (b.part() == HairStyleData.Part.BODY ? body : head).translateAndRotate(pose);
            if (b.rotated()) {
                float[] p = b.pivot() != null ? b.pivot() : new float[]{b.from()[0] + b.size()[0] / 2f, b.from()[1] + b.size()[1], b.from()[2] + b.size()[2] / 2f};
                pose.translate(p[0] * PX, p[1] * PX, p[2] * PX);
                pose.mulPose(new org.joml.Quaternionf().rotationZYX(b.rot()[2] * DEG, b.rot()[1] * DEG, b.rot()[0] * DEG));
                pose.translate(-p[0] * PX, -p[1] * PX, -p[2] * PX);
            }
            box(pose.last(), vc, b.from()[0], b.from()[1], b.from()[2], b.size()[0], b.size()[1], b.size()[2], b.inflate(), b.u(), b.v(), b.uvSize(), mul(argb, tint.tone(b.tone())), light, overlay);
            pose.popPose();
        }
        for (HairStyleData.Chain c : style.chains()) chain(pose, vc, c, head, body, state == null ? null : state.spring(c), tint, helmet, light, overlay, argb);
    }

    private static final float DEG = (float) (Math.PI / 180.0);

    private static void chain(PoseStack pose, VertexConsumer vc, HairStyleData.Chain c, ModelPart head, ModelPart body, HairSpring spring, HairPalette.Tint tint, boolean helmet, int light,
                              int overlay, int argb) {
        HairStyleData.Part run = null;
        int pushes = 0;
        for (int i = 0; i < c.segments().size(); i++) {
            HairStyleData.Segment s = c.segments().get(i);
            if (s.part() != run) {                                      // a new run of segments: its own part's pose, its own nest of rotations
                while (pushes > 0) {
                    pose.popPose();
                    pushes--;
                }
                pose.pushPose();
                pushes++;
                (s.part() == HairStyleData.Part.BODY ? body : head).translateAndRotate(pose);
                run = s.part();
            }
            float[] p = HairStyleData.pivotOf(c, i);
            float pitch = spring != null && i < spring.segments() ? spring.pitch(i) : 0f, roll = spring != null && i < spring.segments() ? spring.roll(i) : 0f;
            pose.translate(p[0] * PX, p[1] * PX, p[2] * PX);
            if (pitch != 0f || roll != 0f) pose.mulPose(new org.joml.Quaternionf().rotationZYX(roll, 0f, pitch));
            pose.translate(-p[0] * PX, -p[1] * PX, -p[2] * PX);
            if (helmet && !HairStyleData.segmentShownUnderHelmet(c, i)) continue;
            float w = s.size()[0], len = s.size()[1], d = s.size()[2];
            box(pose.last(), vc, p[0] - w / 2f, p[1], p[2] - d / 2f, w, len, d, s.inflate(), s.u(), s.v(), s.uvSize(), mul(argb, tint.tone(s.tone())), light, overlay);
        }
        while (pushes > 0) {
            pose.popPose();
            pushes--;
        }
    }

    /** {@code argb} times an 0xRRGGBB tone: the result's alpha is the multiplier's. */
    static int mul(int argb, int rgb) {
        int r = ((argb >> 16) & 255) * ((rgb >> 16) & 255) / 255, g = ((argb >> 8) & 255) * ((rgb >> 8) & 255) / 255, b = (argb & 255) * (rgb & 255) / 255;
        return (argb & 0xFF000000) | (r << 16) | (g << 8) | b;
    }

    // ================================================================ a box, as vanilla's ModelPart.Cube draws it
    /**
     * One box: its minimum corner and size in model pixels (relative to the pose), inflated on every side, textured by the vanilla box layout from (u, v) of a 64x64 sheet with the UV box
     * {@code uvSize} (w, h, d texels, which may differ from the geometry when the box is fractional).
     */
    static void box(PoseStack.Pose p, VertexConsumer vc, float fx, float fy, float fz, float sx, float sy, float sz, float inflate, int u, int v, int[] uvSize, int argb, int light, int overlay) {
        float x0 = (fx - inflate) * PX, y0 = (fy - inflate) * PX, z0 = (fz - inflate) * PX;
        float x1 = (fx + sx + inflate) * PX, y1 = (fy + sy + inflate) * PX, z1 = (fz + sz + inflate) * PX;
        int w = uvSize[0], h = uvSize[1], d = uvSize[2];
        // the eight corners: index = x (0 min / 1 max) + 2 * y + 4 * z, y DOWN as in vanilla's model space
        float[][] c = new float[8][];
        for (int i = 0; i < 8; i++) c[i] = new float[]{(i & 1) == 0 ? x0 : x1, (i & 2) == 0 ? y0 : y1, (i & 4) == 0 ? z0 : z1};
        float s = HairStyleData.SHEET;
        //            corners (vanilla order)           u1       v1        u2          v2             outward normal
        face(p, vc, new float[][]{c[0], c[4], c[6], c[2]}, u / s, (v + d) / s, (u + d) / s, (v + d + h) / s, -1f, 0f, 0f, argb, light, overlay);                      // west (-x)
        face(p, vc, new float[][]{c[1], c[0], c[2], c[3]}, (u + d) / s, (v + d) / s, (u + d + w) / s, (v + d + h) / s, 0f, 0f, -1f, argb, light, overlay);          // north (front)
        face(p, vc, new float[][]{c[5], c[1], c[3], c[7]}, (u + d + w) / s, (v + d) / s, (u + 2 * d + w) / s, (v + d + h) / s, 1f, 0f, 0f, argb, light, overlay);    // east (+x)
        face(p, vc, new float[][]{c[4], c[5], c[7], c[6]}, (u + 2 * d + w) / s, (v + d) / s, (u + 2 * d + 2 * w) / s, (v + d + h) / s, 0f, 0f, 1f, argb, light, overlay); // south (back)
        face(p, vc, new float[][]{c[5], c[4], c[0], c[1]}, (u + d) / s, v / s, (u + d + w) / s, (v + d) / s, 0f, -1f, 0f, argb, light, overlay);                      // top
        face(p, vc, new float[][]{c[3], c[2], c[6], c[7]}, (u + d + w) / s, (v + d) / s, (u + d + 2 * w) / s, v / s, 0f, 1f, 0f, argb, light, overlay);               // bottom
    }

    /** One polygon: vanilla gives its four corners the uvs (u2,v1) (u1,v1) (u1,v2) (u2,v2) in this order. */
    private static void face(PoseStack.Pose p, VertexConsumer vc, float[][] c, float u1, float v1, float u2, float v2, float nx, float ny, float nz, int argb, int light, int overlay) {
        float[][] t = {{u2, v1}, {u1, v1}, {u1, v2}, {u2, v2}};
        int r = (argb >> 16) & 255, g = (argb >> 8) & 255, b = argb & 255, a = argb >>> 24;
        for (int k = 0; k < 4; k++)
            vc.addVertex(p, c[k][0], c[k][1], c[k][2]).setColor(r, g, b, a).setUv(t[k][0], t[k][1]).setOverlay(overlay).setLight(light).setNormal(p, nx, ny, nz);
    }
}
