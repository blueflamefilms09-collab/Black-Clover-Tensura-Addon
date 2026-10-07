package com.newuniverse.nusmp.client.geo;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.client.aura.AuraRender;
import com.newuniverse.nusmp.client.prop.PropDraw;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Quaternionf;

/**
 * 0.53: draws the geo.json models (see {@link GeoModelData}) with their keyframe animations ({@link GeoAnim}) and an emissive glow layer.
 * It reproduces vanilla's ModelPart.Cube (box UV, mirror, inflate, vertex order, normals) and ModelPart.translateAndRotate (pivot,
 * rotation in ZYX order), so a model looks in game as it does in Blockbench's vanilla-style preview.
 *
 * <p><b>Where a model goes ({@link Space}).</b> A model is authored y up, origin at the feet, front on the -z side, +x the model's left.
 * {@link Space#PROP}: in a prop painter's pose (feet at the origin, y up, blocks): the model stands on the prop's feet and its FRONT looks
 * along the prop's forward (+z at yaw 0). {@link Space#PLAYER}: in an aura painter's layer pose (vanilla layer space, y down, feet at
 * y = 1.5): the model stands on the player's feet, turned with the player, front where the player's face is, so a model built to a
 * humanoid's proportions (head origin y = 24..32, arms at x = +-5..8) lines up with the player's own body.</p>
 *
 * <p><b>Rotations.</b> Bone and cube rotations are degrees, applied in vanilla order (Z, then Y, then X on the point), converted from the
 * file's y-up frame, and animation rotation is added to the bone's own rotation. If a part turns the wrong way, flip the sign in the
 * file (tools/geo_builder.py documents the sign of each axis and the preview shows it).</p>
 */
public final class GeoDraw {
    private GeoDraw() {}

    public enum Space { PROP, PLAYER }

    /** The render type of the model's main texture: solid (alpha-tested), alpha-blended, or additive. */
    public enum Layer { CUTOUT, TRANSLUCENT, ADDITIVE }

    private static final float PX = 1f / 16f, DEG = (float) (Math.PI / 180.0);

    /**
     * The usual call: the model with its texture on {@code layer}, then (when glowArgb has any alpha) the glow texture additive and full
     * bright, tinted by glowArgb. {@code anim} is a clip name of the spec's animation file (null = rest pose); {@code seconds} the clock.
     */
    public static void paint(PoseStack pose, MultiBufferSource buffers, GeoSpec spec, String anim, float seconds, Space space, Layer layer,
                             int light, int argb, int glowArgb) {
        GeoModelData m = GeoModels.model(spec.model());
        GeoAnim.Clip clip = null;
        if (anim != null) {
            GeoAnim a = GeoModels.animations(spec.animations());
            if (a != null) clip = a.clip(anim);
        }
        ResourceLocation tex = spec.texture();
        RenderType rt = switch (layer) {
            case TRANSLUCENT -> AuraRender.translucent(tex);
            case ADDITIVE -> AuraRender.additive(tex);
            default -> AuraRender.cutout(tex);
        };
        draw(pose, buffers.getBuffer(rt), m, clip, seconds, space, light, OverlayTexture.NO_OVERLAY, argb);
        if (spec.glow() != null && (glowArgb >>> 24) != 0)
            draw(pose, buffers.getBuffer(AuraRender.additive(spec.glow())), m, clip, seconds, space, PropDraw.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, glowArgb);
    }

    /** One pass of a model into a vertex consumer (an entity-format render type). */
    public static void draw(PoseStack pose, VertexConsumer vc, GeoModelData m, GeoAnim.Clip clip, float seconds, Space space,
                            int light, int overlay, int argb) {
        if (m == null || m.roots.isEmpty()) return;
        pose.pushPose();
        if (space == Space.PROP) {
            pose.scale(PX, -PX, -PX);                         // vanilla model space (y down, front -z) -> world (y up, front +z)
        } else {
            pose.translate(0f, 24f * PX, 0f);                 // the feet are 24 px below the neck line of vanilla's layer space
            pose.scale(PX, PX, PX);
        }
        float t = clip == null ? 0f : clip.time(seconds);
        for (GeoModelData.Bone b : m.roots) bone(pose, vc, m, b, null, clip, t, light, overlay, argb);
        pose.popPose();
    }

    // ================================================================ bones
    private static void bone(PoseStack pose, VertexConsumer vc, GeoModelData m, GeoModelData.Bone b, GeoModelData.Bone parent, GeoAnim.Clip clip,
                             float t, int light, int overlay, int argb) {
        // vanilla space: y is the file's y negated; the part sits at its pivot relative to the parent's pivot
        float x = b.px - (parent == null ? 0f : parent.px), y = -b.py + (parent == null ? 0f : parent.py), z = b.pz - (parent == null ? 0f : parent.pz);
        float rx = b.rx, ry = b.ry, rz = b.rz, sx = 1f, sy = 1f, sz = 1f;
        if (clip != null) {
            GeoAnim.BoneAnim a = clip.bones.get(b.name);
            if (a != null) {
                float[] r = a.rotation == null ? null : a.rotation.sample(t);
                float[] p = a.position == null ? null : a.position.sample(t);
                float[] s = a.scale == null ? null : a.scale.sample(t);
                if (r != null) { rx += r[0]; ry += r[1]; rz += r[2]; }
                if (p != null) { x += p[0]; y -= p[1]; z += p[2]; }
                if (s != null) { sx = s[0]; sy = s[1]; sz = s[2]; }
            }
        }
        pose.pushPose();
        pose.translate(x, y, z);
        rotate(pose, rx, ry, rz);
        if (sx != 1f || sy != 1f || sz != 1f) pose.scale(sx, sy, sz);
        for (GeoModelData.Cube c : b.cubes) cube(pose, vc, m, b, c, light, overlay, argb);
        for (GeoModelData.Bone child : b.children) bone(pose, vc, m, child, b, clip, t, light, overlay, argb);
        pose.popPose();
    }

    /** File rotation (y-up frame, degrees) -> vanilla's ZYX rotation: the file's x and z angles change sign under the y flip. */
    private static void rotate(PoseStack pose, float rx, float ry, float rz) {
        if (rx != 0f || ry != 0f || rz != 0f) pose.mulPose(new Quaternionf().rotationZYX(-rz * DEG, ry * DEG, -rx * DEG));
    }

    // ================================================================ cubes (vanilla ModelPart.Cube)
    private static void cube(PoseStack pose, VertexConsumer vc, GeoModelData m, GeoModelData.Bone b, GeoModelData.Cube c, int light, int overlay, int argb) {
        if (c.sx <= 0f && c.sy <= 0f && c.sz <= 0f) return;
        boolean turned = c.hasRot && (c.rx != 0f || c.ry != 0f || c.rz != 0f);
        if (turned) {
            float cx = c.px - b.px, cy = -c.py + b.py, cz = c.pz - b.pz;
            pose.pushPose();
            pose.translate(cx, cy, cz);
            rotate(pose, c.rx, c.ry, c.rz);
            pose.translate(-cx, -cy, -cz);
        }
        float g = c.inflate;
        float minX = c.ox - b.px - g, maxX = c.ox - b.px + c.sx + g;
        float minY = -(c.oy + c.sy) + b.py - g, maxY = -c.oy + b.py + g;
        float minZ = c.oz - b.pz - g, maxZ = c.oz - b.pz + c.sz + g;
        if (c.mirror) { float tmp = maxX; maxX = minX; minX = tmp; }
        // the eight corners (names after their vanilla counterparts): index = x(0 min / 1 max) + 2 * y(0 min / 1 max) + 4 * z(0 min / 1 max)
        float[][] v = new float[8][];
        for (int i = 0; i < 8; i++) v[i] = new float[]{(i & 1) == 0 ? minX : maxX, (i & 2) == 0 ? minY : maxY, (i & 4) == 0 ? minZ : maxZ};
        PoseStack.Pose p = pose.last();
        float mx = c.mirror ? -1f : 1f;
        //            face            corners in vanilla order                                   outward normal
        face(p, vc, m, c.uv[GeoModelData.WEST],   new float[][]{v[0], v[4], v[6], v[2]}, c.mirror, -mx, 0f, 0f, light, overlay, argb);
        face(p, vc, m, c.uv[GeoModelData.NORTH],  new float[][]{v[1], v[0], v[2], v[3]}, c.mirror, 0f, 0f, -1f, light, overlay, argb);
        face(p, vc, m, c.uv[GeoModelData.EAST],   new float[][]{v[5], v[1], v[3], v[7]}, c.mirror, mx, 0f, 0f, light, overlay, argb);
        face(p, vc, m, c.uv[GeoModelData.SOUTH],  new float[][]{v[4], v[5], v[7], v[6]}, c.mirror, 0f, 0f, 1f, light, overlay, argb);
        face(p, vc, m, c.uv[GeoModelData.TOP],    new float[][]{v[5], v[4], v[0], v[1]}, c.mirror, 0f, -1f, 0f, light, overlay, argb);
        face(p, vc, m, c.uv[GeoModelData.BOTTOM], new float[][]{v[3], v[2], v[6], v[7]}, c.mirror, 0f, 1f, 0f, light, overlay, argb);
        if (turned) pose.popPose();
    }

    /** One polygon: vanilla gives its four corners the uvs (u2,v1) (u1,v1) (u1,v2) (u2,v2) in this order, and a mirrored cube reverses them. */
    private static void face(PoseStack.Pose p, VertexConsumer vc, GeoModelData m, float[] uv, float[][] c, boolean mirror,
                             float nx, float ny, float nz, int light, int overlay, int argb) {
        if (uv == null) return;
        float u1 = uv[0] / m.texW, v1 = uv[1] / m.texH, u2 = uv[2] / m.texW, v2 = uv[3] / m.texH;
        float[][] t = {{u2, v1}, {u1, v1}, {u1, v2}, {u2, v2}};
        int r = (argb >> 16) & 255, g = (argb >> 8) & 255, bl = argb & 255, a = argb >>> 24;
        for (int k = 0; k < 4; k++) {
            int i = mirror ? 3 - k : k;
            vc.addVertex(p, c[i][0], c[i][1], c[i][2]).setColor(r, g, bl, a).setUv(t[i][0], t[i][1])
                    .setOverlay(overlay).setLight(light).setNormal(p, nx, ny, nz);
        }
    }
}
