package com.newuniverse.nusmp.client.heroweapon.carriage;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.client.relics.item.VariantRules;
import com.newuniverse.nusmp.heroweapon.weapon.carriage.CarriageEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.List;

/**
 * Why: this is how the Vassal Carriage looks in the world. It draws the baked model of {@link CarriageMesh} (the same mesh as the small carriage in the hand,
 * three times larger) turned to the way the carriage faces: the skin as a cut-out body that takes the world's light, and the glow layer (the lantern gem and
 * the runes) added on top at full brightness with a slow pulse in the mesh's own colour, exactly as the weapon renderer does for the hand-held model. The
 * riders are drawn by the player renderer, seated by the entity's passenger attachment points. Nothing else is drawn: the barrier, the gusts and the gates
 * are the effect system's.
 */
public final class CarriageRenderer extends EntityRenderer<CarriageEntity> {
    public CarriageRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 1.1f;
    }

    @Override
    public ResourceLocation getTextureLocation(CarriageEntity entity) { return CarriageMesh.SKIN; }

    @Override
    public void render(CarriageEntity entity, float entityYaw, float partialTick, PoseStack pose, MultiBufferSource buffers, int light) {
        MeshBake.Baked mesh = CarriageMesh.get();
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-entityYaw));                        // the model's +z (its nose) turns to the way the carriage faces
        PoseStack.Pose p = pose.last();
        emit(p, buffers.getBuffer(RenderType.entityCutoutNoCull(mesh.fallback ? CarriageMesh.FALLBACK_SKIN : CarriageMesh.SKIN)), mesh.body, 0xFFFFFFFF, light, OverlayTexture.NO_OVERLAY);
        if (!mesh.glow.isEmpty()) {
            float time = entity.tickCount + partialTick;
            float k = VariantRules.pulse(time, mesh.glowSpeed, mesh.glowMin, mesh.glowMax, 1.0f);
            int r = VariantRules.channel(mesh.glowRgb >> 16 & 255, k), g = VariantRules.channel(mesh.glowRgb >> 8 & 255, k), b = VariantRules.channel(mesh.glowRgb & 255, k);
            emit(p, buffers.getBuffer(RenderType.eyes(CarriageMesh.GLOW)), mesh.glow, 0xFF000000 | r << 16 | g << 8 | b, 0xF000F0, OverlayTexture.NO_OVERLAY);
        }
        pose.popPose();
        super.render(entity, entityYaw, partialTick, pose, buffers, light);
    }

    /** Writes the quads through the pose (position and normal), the same way the weapon renderer writes the hand-held model. */
    private static void emit(PoseStack.Pose p, VertexConsumer vc, List<float[]> verts, int argb, int light, int overlay) {
        Matrix4f m = p.pose();
        Matrix3f nm = p.normal();
        int a = argb >>> 24, r = argb >> 16 & 255, g = argb >> 8 & 255, b = argb & 255;
        Vector3f q = new Vector3f(), n = new Vector3f();                         // reused for every vertex: the model has thousands
        for (float[] v : verts) {
            m.transformPosition(v[0], v[1], v[2], q);
            nm.transform(v[5], v[6], v[7], n).normalize();
            vc.addVertex(q.x, q.y, q.z).setColor(r, g, b, a).setUv(v[3] / 128f, v[4] / 128f).setOverlay(overlay).setLight(light).setNormal(n.x, n.y, n.z);
        }
    }
}
