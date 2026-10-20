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

    // 1.99.0 (performance pass, docs/performance_1_99.md): this is the draw routine of every geo model of the mod (the aura armours, the props, the arc and Zogratis bodies, the foundry mobs, the
    // player-model extras), so it is where the per-frame garbage was: a Quaternionf per turned bone, a float[3] per sampled track, and about forty arrays per cube. The scratch objects below are
    // used by the render thread only, and every value is consumed before the next one is written (the bone recursion copies what it needs into locals first); the cube's own numbers are worked out
    // once and kept on the cube (GeoModelData.Cube#drawCorners). The floats that reach the vertex consumer are bit for bit the ones the allocating version produced.
    private static final Quaternionf TURN = new Quaternionf();
    private static final float[] ROT = new float[3], POS = new float[3], SCL = new float[3];

    // ================================================================ bones
    private static void bone(PoseStack pose, VertexConsumer vc, GeoModelData m, GeoModelData.Bone b, GeoModelData.Bone parent, GeoAnim.Clip clip,
                             float t, int light, int overlay, int argb) {
        // vanilla space: y is the file's y negated; the part sits at its pivot relative to the parent's pivot
        float x = b.px - (parent == null ? 0f : parent.px), y = -b.py + (parent == null ? 0f : parent.py), z = b.pz - (parent == null ? 0f : parent.pz);
        float rx = b.rx, ry = b.ry, rz = b.rz, sx = 1f, sy = 1f, sz = 1f;
        if (clip != null) {
            GeoAnim.BoneAnim a = clip.bones.get(b.name);
            if (a != null) {
                if (a.rotation != null && a.rotation.sampleInto(t, ROT)) { rx += ROT[0]; ry += ROT[1]; rz += ROT[2]; }
                if (a.position != null && a.position.sampleInto(t, POS)) { x += POS[0]; y -= POS[1]; z += POS[2]; }
                if (a.scale != null && a.scale.sampleInto(t, SCL)) { sx = SCL[0]; sy = SCL[1]; sz = SCL[2]; }
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
        if (rx != 0f || ry != 0f || rz != 0f) pose.mulPose(TURN.rotationZYX(-rz * DEG, ry * DEG, -rx * DEG));
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
        if (c.drawCorners == null) c.prepareDraw(m, b);
        float[] v = c.drawCorners, uv = c.drawUv;
        int mask = c.drawMask;
        PoseStack.Pose p = pose.last();
        boolean mirror = c.mirror;
        float mx = mirror ? -1f : 1f;
        int r = (argb >> 16) & 255, g = (argb >> 8) & 255, bl = argb & 255, a = argb >>> 24;
        // face, its four corners (indices into the eight: x = bit 0, y = bit 1, z = bit 2; vanilla's order), its outward normal
        if ((mask & 1) != 0)  face(p, vc, v, 0, 4, 6, 2, uv, 0,  mirror, -mx, 0f, 0f, light, overlay, r, g, bl, a);   // WEST
        if ((mask & 2) != 0)  face(p, vc, v, 1, 0, 2, 3, uv, 4,  mirror, 0f, 0f, -1f, light, overlay, r, g, bl, a);   // NORTH
        if ((mask & 4) != 0)  face(p, vc, v, 5, 1, 3, 7, uv, 8,  mirror, mx, 0f, 0f, light, overlay, r, g, bl, a);    // EAST
        if ((mask & 8) != 0)  face(p, vc, v, 4, 5, 7, 6, uv, 12, mirror, 0f, 0f, 1f, light, overlay, r, g, bl, a);    // SOUTH
        if ((mask & 16) != 0) face(p, vc, v, 5, 4, 0, 1, uv, 16, mirror, 0f, -1f, 0f, light, overlay, r, g, bl, a);   // TOP
        if ((mask & 32) != 0) face(p, vc, v, 3, 2, 6, 7, uv, 20, mirror, 0f, 1f, 0f, light, overlay, r, g, bl, a);    // BOTTOM
        if (turned) pose.popPose();
    }

    /**
     * One polygon: vanilla gives its four corners the uvs (u2,v1) (u1,v1) (u1,v2) (u2,v2) in this order, and a mirrored cube reverses them. {@code a..d} are the corner indices of the four
     * corners; {@code at} is where the face's {u1, v1, u2, v2} start in {@code uv}.
     */
    private static void face(PoseStack.Pose p, VertexConsumer vc, float[] v, int a, int b, int c, int d, float[] uv, int at, boolean mirror,
                             float nx, float ny, float nz, int light, int overlay, int r, int g, int bl, int alpha) {
        float u1 = uv[at], v1 = uv[at + 1], u2 = uv[at + 2], v2 = uv[at + 3];
        for (int k = 0; k < 4; k++) {
            int i = mirror ? 3 - k : k;
            int corner = (i == 0 ? a : i == 1 ? b : i == 2 ? c : d) * 3;
            float u = i == 0 || i == 3 ? u2 : u1, vv = i < 2 ? v1 : v2;
            vc.addVertex(p, v[corner], v[corner + 1], v[corner + 2]).setColor(r, g, bl, alpha).setUv(u, vv)
                    .setOverlay(overlay).setLight(light).setNormal(p, nx, ny, nz);
        }
    }
}
