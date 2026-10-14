package com.newuniverse.nusmp.client.relics.djinn;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.relics.djinn.Djinn;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * Draws a baked armour piece ({@link DjinnMeshes}) at the pose's current transform: its lit body from the painted skin of the set the piece came from
 * (the shared rig, the Djinn's unique look or its Dark Djinn look), and its additive glow on top, full bright. No allocation per frame: the vertex maths
 * reuses two vectors (the render thread is one thread).
 */
final class DjinnDraw {
    private DjinnDraw() {}

    private static final Vector3f POS = new Vector3f(), NRM = new Vector3f();
    static final int FULL = LightTexture.FULL_BRIGHT;

    static void piece(PoseStack pose, MultiBufferSource buffers, Djinn d, DjinnMeshes.Piece piece, int light, float alpha, float glow) {
        piece(pose, buffers, d, piece, false, light, alpha, glow);
    }

    /**
     * @param dark   draw the Dark Djinn's set (falls back to the unique look, then to the shared rig, when a set is missing)
     * @param alpha  0..1, below 1 the body is drawn translucent (forging in, shattering out)
     * @param glow   brightness of the additive pass, 0..1.2
     */
    static void piece(PoseStack pose, MultiBufferSource buffers, Djinn d, DjinnMeshes.Piece piece, boolean dark, int light, float alpha, float glow) {
        DjinnMeshes.Data m = DjinnMeshes.get(d, piece, dark);
        if (m.isEmpty()) return;
        if (alpha > 0.02f && m.body.length > 0) {
            RenderType type = alpha >= 0.98f ? RenderType.entityCutoutNoCull(DjinnMeshes.skin(d, m.kind)) : RenderType.entityTranslucent(DjinnMeshes.skin(d, m.kind));
            emit(buffers.getBuffer(type), pose.last(), m.body, light, 255, 255, 255, (int) (255 * Math.min(1f, alpha)));
        }
        if (glow > 0.02f && alpha > 0.02f && m.glow.length > 0) {
            int v = (int) (255 * Math.min(1f, glow * alpha));
            emit(buffers.getBuffer(RenderType.eyes(DjinnMeshes.glowSkin(d, m.kind))), pose.last(), m.glow, FULL, v, v, v, 255);
        }
    }

    /** The glow quads only, in a tint (the weapon sheath, a ghost of the weapon). */
    static void glowOnly(PoseStack pose, MultiBufferSource buffers, Djinn d, DjinnMeshes.Piece piece, int rgb, float strength) {
        glowOnly(pose, buffers, d, piece, false, rgb, strength);
    }

    static void glowOnly(PoseStack pose, MultiBufferSource buffers, Djinn d, DjinnMeshes.Piece piece, boolean dark, int rgb, float strength) {
        DjinnMeshes.Data m = DjinnMeshes.get(d, piece, dark);
        if (m.glow.length == 0) return;
        int r = (int) (((rgb >> 16) & 255) * strength), g = (int) (((rgb >> 8) & 255) * strength), b = (int) ((rgb & 255) * strength);
        emit(buffers.getBuffer(RenderType.eyes(DjinnMeshes.glowSkin(d, m.kind))), pose.last(), m.glow, FULL, Math.min(255, r), Math.min(255, g), Math.min(255, b), 255);
    }

    private static void emit(VertexConsumer vc, PoseStack.Pose last, float[] q, int light, int r, int g, int b, int a) {
        Matrix4f pm = last.pose();
        Matrix3f nm = last.normal();
        for (int i = 0; i < q.length; i += 8) {
            pm.transformPosition(q[i], q[i + 1], q[i + 2], POS);
            nm.transform(q[i + 5], q[i + 6], q[i + 7], NRM).normalize();
            vc.addVertex(POS.x, POS.y, POS.z).setColor(r, g, b, a).setUv(q[i + 3], q[i + 4]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light)
                    .setNormal(NRM.x, NRM.y, NRM.z);
        }
    }
}
