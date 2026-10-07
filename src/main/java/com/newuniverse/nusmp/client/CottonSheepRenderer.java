package com.newuniverse.nusmp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.entity.CottonSheepEntity;
import com.newuniverse.nusmp.entity.NUEntities;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** 0.52: Charmy's cotton sheep (see {@link CottonSheepModel}); a touch under life size, so a cook fits a kitchen cloud. */
public class CottonSheepRenderer extends MobRenderer<CottonSheepEntity, CottonSheepModel> {
    static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/cotton_sheep.png");

    public CottonSheepRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new CottonSheepModel(ctx.bakeLayer(CottonSheepModel.LAYER)), 0.6f);
    }

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(NUEntities.COTTON_SHEEP.get(), CottonSheepRenderer::new);
        e.registerEntityRenderer(NUEntities.COTTON_CLOUD.get(), CottonCloudRenderer::new);
    }

    public static void layers(EntityRenderersEvent.RegisterLayerDefinitions e) {
        e.registerLayerDefinition(CottonSheepModel.LAYER, CottonSheepModel::createLayer);
        e.registerLayerDefinition(CottonCloudModel.LAYER, CottonCloudModel::createLayer);
    }

    @Override
    public ResourceLocation getTextureLocation(CottonSheepEntity e) { return TEX; }

    @Override
    protected void scale(CottonSheepEntity e, PoseStack pose, float partial) {
        pose.scale(0.8f, 0.8f, 0.8f);
    }

    @Override
    protected boolean shouldShowName(CottonSheepEntity e) { return false; }
}
