package com.newuniverse.nusmp.client.veldanava;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.newuniverse.nusmp.client.SafeAdditive;
import com.newuniverse.nusmp.entity.veldanava.WillOfTheCreator;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;

/**
 * The Will of the Creator: a glowing orb of light with a gold-white heart, a teal halo and six motes of light circling it, bobbing above the ground. It beats faster the
 * fewer fighters have still to choose their boon. Six quads plus two; no allocation per frame.
 */
public class VeldanavaOrbRenderer extends EntityRenderer<WillOfTheCreator> {
    private static final ResourceLocation ORB = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/spirit_orb.png");
    private static final ResourceLocation GLOW = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/particle/glow.png");
    private static final float CORE = 0.9f, HALO = 2.6f, MOTE = 0.2f;

    public VeldanavaOrbRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0f;
    }

    @Override
    public void render(WillOfTheCreator e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = e.tickCount + partial;
        float beat = 0.10f + 0.05f * Math.max(0, Math.min(4, e.waiting()));
        float pulse = 1f + 0.09f * Mth.sin(age * beat);
        pose.pushPose();
        pose.translate(0, 0.5 + 0.18 * Mth.sin(age * 0.07f), 0);
        for (int i = 0; i < 6; i++) {
            float a = age * 0.06f + i * Mth.TWO_PI / 6f;
            pose.pushPose();
            pose.translate(Mth.cos(a) * 1.1, 0.35 * Mth.sin(age * 0.09f + i * 1.7f), Mth.sin(a) * 1.1);
            pose.mulPose(entityRenderDispatcher.cameraOrientation());
            quad(pose, buffers.getBuffer(SafeAdditive.emissive(GLOW)), MOTE, 0.95f, 0.9f, 0.6f, 0.9f);
            pose.popPose();
        }
        pose.mulPose(entityRenderDispatcher.cameraOrientation());
        quad(pose, buffers.getBuffer(SafeAdditive.emissive(GLOW)), HALO * pulse, 0.25f, 0.88f, 0.82f, 0.55f);
        quad(pose, buffers.getBuffer(SafeAdditive.emissive(ORB)), CORE * pulse, 1f, 0.96f, 0.78f, 1f);
        pose.popPose();
        super.render(e, yaw, partial, pose, buffers, light);
    }

    /** A camera-facing square of half-size {@code s}, full bright. */
    private static void quad(PoseStack pose, VertexConsumer vc, float s, float r, float g, float b, float a) {
        Matrix4f m = pose.last().pose();
        PoseStack.Pose last = pose.last();
        int light = 0xF000F0;
        vc.addVertex(m, -s, -s, 0).setColor(r, g, b, a).setUv(0, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0, 1, 0);
        vc.addVertex(m, s, -s, 0).setColor(r, g, b, a).setUv(1, 1).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0, 1, 0);
        vc.addVertex(m, s, s, 0).setColor(r, g, b, a).setUv(1, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0, 1, 0);
        vc.addVertex(m, -s, s, 0).setColor(r, g, b, a).setUv(0, 0).setOverlay(OverlayTexture.NO_OVERLAY).setLight(light).setNormal(last, 0, 1, 0);
    }

    @Override
    public ResourceLocation getTextureLocation(WillOfTheCreator e) { return ORB; }
}
