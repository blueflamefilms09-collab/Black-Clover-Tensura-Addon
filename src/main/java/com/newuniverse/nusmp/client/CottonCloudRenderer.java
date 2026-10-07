package com.newuniverse.nusmp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.entity.CottonCloudEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;

/**
 * 0.52: draws Charmy's cotton cloud ({@link CottonCloudModel}). A cumulus for the platform and the ride; a cocoon of three stacked
 * heaps round the foe for Sheep Bondage; a puff that swells and fades for a strike's landing.
 */
public class CottonCloudRenderer extends EntityRenderer<CottonCloudEntity> {
    static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/cotton_cloud.png");
    private final CottonCloudModel model;

    public CottonCloudRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.model = new CottonCloudModel(ctx.bakeLayer(CottonCloudModel.LAYER));
        this.shadowRadius = 0f;
    }

    @Override
    public ResourceLocation getTextureLocation(CottonCloudEntity e) { return TEX; }

    @Override
    public void render(CottonCloudEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        float age = e.tickCount + partial, s = e.cloudScale() * 0.0625f * 1.25f;
        int mode = e.mode();
        int alpha = 255;
        if (mode == CottonCloudEntity.BURST) {
            float t = Math.min(1f, (e.life() + partial) / Math.max(1f, e.maxLife()));
            s *= 0.5f + 1.2f * t;
            alpha = (int) (255 * (1 - t));
        } else if (e.maxLife() - e.life() < 10) alpha = (int) (255 * Math.max(0f, (e.maxLife() - e.life() - partial) / 10f));
        else if (e.life() < 6) alpha = (int) (255 * Math.min(1f, (e.life() + partial) / 6f));
        int color = (alpha << 24) | 0xFFFFFF;
        model.animate(age);
        var vc = buffers.getBuffer(RenderType.entityTranslucent(TEX));
        int heaps = mode == CottonCloudEntity.WRAP ? 3 : 1;
        for (int k = 0; k < heaps; k++) {
            pose.pushPose();
            if (mode == CottonCloudEntity.WRAP) {                                           // a cocoon: three heaps up the body
                float ws = e.cloudScale() * 0.0625f * 0.62f;
                pose.translate(0f, 0.25f + k * 0.62f * e.cloudScale(), 0f);
                pose.scale(-ws, -ws, ws);
                pose.mulPose(com.mojang.math.Axis.YP.rotationDegrees(k * 55f + age * 2f));
            } else {
                pose.translate(0f, 3f * s, 0f);
                pose.scale(-s, -s, s);
            }
            model.render(pose, vc, light, OverlayTexture.NO_OVERLAY, color);
            pose.popPose();
        }
    }
}
