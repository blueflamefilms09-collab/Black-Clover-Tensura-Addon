package com.newuniverse.nusmp.client.aura;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.book.SpiritDiveWind3Mesh;
import com.newuniverse.nusmp.book.SpiritDiveWind3Rules;
import com.newuniverse.nusmp.client.SafeAdditive;
import com.newuniverse.nusmp.client.prop.PropDraw;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Spirit Dive WIND remake: draws the real 3D pieces ({@link SpiritDiveWind3Mesh}) into the dive's vertex consumers, in the frame the painter has set up. Two passes of a piece: the SOLID layers (body and
 * ornaments) through the emissive translucent type, the GLOW layer through the additive type; both use the one atlas {@code aura/sdw3_skin}. Render thread only; every scratch object is a static, the
 * mesh arrays are shared, so a piece costs its vertex count and nothing else (no allocation per frame).
 */
final class SpiritDiveWind3Draw {
    private SpiritDiveWind3Draw() {}

    private static final ResourceLocation SKIN = AuraRender.tex(SpiritDiveWind3Rules.SKIN);
    private static final int FB = PropDraw.FULL_BRIGHT;
    private static final Quaternionf Q = new Quaternionf();
    private static final Matrix3f M3 = new Matrix3f();
    private static final Vector3f P = new Vector3f(), N = new Vector3f();

    /**
     * Moves the pose to {@code (ox, oy, oz)}, turns the mesh's +y axis to {@code (yx, yy, yz)} (any length) and its flat face across {@code (hx, hy, hz)} (the mesh's x axis ends up as nearly this as the
     * y axis allows), then scales. The caller pushes and pops.
     */
    static void basis(PoseStack pose, float ox, float oy, float oz, float yx, float yy, float yz, float hx, float hy, float hz, float sx, float sy, float sz) {
        float yl = (float) Math.sqrt(yx * yx + yy * yy + yz * yz);
        if (yl < 1e-5f) { yx = 0f; yy = 1f; yz = 0f; yl = 1f; }
        yx /= yl; yy /= yl; yz /= yl;
        float zx = hy * yz - hz * yy, zy = hz * yx - hx * yz, zz = hx * yy - hy * yx, zl = (float) Math.sqrt(zx * zx + zy * zy + zz * zz);
        if (zl < 1e-4f) {                                                                                       // the hint is parallel to the axis: any perpendicular
            zx = yy; zy = -yx; zz = 0f; zl = (float) Math.sqrt(zx * zx + zy * zy);
            if (zl < 1e-4f) { zx = 0f; zy = 0f; zz = 1f; zl = 1f; }
        }
        zx /= zl; zy /= zl; zz /= zl;
        float xx = yy * zz - yz * zy, xy = yz * zx - yx * zz, xz = yx * zy - yy * zx;                            // x = y cross z
        M3.set(xx, xy, xz, yx, yy, yz, zx, zy, zz);
        pose.translate(ox, oy, oz);
        pose.mulPose(Q.setFromNormalized(M3));
        pose.scale(sx, sy, sz);
    }

    /** Draws the layers of {@code m} whose bit is set in {@code mask}: solid ones with {@code alpha}, the glow layer additive with {@code glowAlpha} in the colour {@code glowRgb}. */
    static void mesh(AuraContext c, SpiritDiveWind3Mesh m, int mask, float alpha, float glowAlpha, int glowRgb) {
        if (m.totalVerts() == 0) return;
        PoseStack.Pose pose = c.pose().last();
        if (alpha > 0.01f) {
            int a = Math.min(255, (int) (alpha * 255f + 0.5f));
            if ((mask & 1) != 0 && m.quads(SpiritDiveWind3Mesh.SOLID) > 0) emit(pose, c.buffers().getBuffer(SafeAdditive.emissive(SKIN)), m, SpiritDiveWind3Mesh.SOLID, a, 0xFFFFFF);
            if ((mask & 4) != 0 && m.quads(SpiritDiveWind3Mesh.ORNAMENT) > 0) emit(pose, c.buffers().getBuffer(SafeAdditive.emissive(SKIN)), m, SpiritDiveWind3Mesh.ORNAMENT, a, 0xFFFFFF);
        }
        if ((mask & 2) != 0 && glowAlpha > 0.01f && m.quads(SpiritDiveWind3Mesh.GLOW) > 0)
            emit(pose, c.buffers().getBuffer(SafeAdditive.additive(SKIN)), m, SpiritDiveWind3Mesh.GLOW, Math.min(255, (int) (glowAlpha * 255f + 0.5f)), glowRgb);
    }

    private static void emit(PoseStack.Pose pose, VertexConsumer vc, SpiritDiveWind3Mesh m, int layer, int a, int mulRgb) {
        Matrix4f mat = pose.pose();
        float[] pos = m.pos[layer], uv = m.uv[layer], nrm = m.nrm[layer];
        int[] tint = m.tint[layer];
        int mr = mulRgb >> 16 & 255, mg = mulRgb >> 8 & 255, mb = mulRgb & 255;
        for (int q = 0; q < tint.length; q++) {
            int t = tint[q];
            int r = (t >> 16 & 255) * mr / 255, g = (t >> 8 & 255) * mg / 255, b = (t & 255) * mb / 255;
            pose.transformNormal(nrm[q * 3], nrm[q * 3 + 1], nrm[q * 3 + 2], N);
            for (int k = 0; k < 4; k++) {
                int i = q * 12 + k * 3;
                mat.transformPosition(pos[i], pos[i + 1], pos[i + 2], P);
                vc.addVertex(P.x, P.y, P.z).setColor(r, g, b, a).setUv(uv[q * 8 + k * 2], uv[q * 8 + k * 2 + 1]).setOverlay(OverlayTexture.NO_OVERLAY).setLight(FB).setNormal(N.x, N.y, N.z);
            }
        }
    }
}
