package com.newuniverse.nusmp.client;

import com.newuniverse.nusmp.entity.NUEntities;
import com.newuniverse.nusmp.entity.SpiritLordEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

/** Renders a Spirit Lord with Tensura's own spirit model, texture and animations (read from the Tensura mod). */
public class SpiritLordRenderer extends GeoEntityRenderer<SpiritLordEntity> {
    public SpiritLordRenderer(EntityRendererProvider.Context ctx) {
        super(ctx, new GeoModel<>() {
            @Override public ResourceLocation getModelResource(SpiritLordEntity e) {
                return ResourceLocation.fromNamespaceAndPath("tensura", "geo/entity/" + e.kind().geo + ".geo.json");
            }
            @Override public ResourceLocation getTextureResource(SpiritLordEntity e) {
                return ResourceLocation.fromNamespaceAndPath("tensura", "textures/entity/" + e.kind().texture + ".png");
            }
            @Override public ResourceLocation getAnimationResource(SpiritLordEntity e) {
                return ResourceLocation.fromNamespaceAndPath("tensura", "animations/entity/" + e.kind().geo + ".animation.json");
            }
        });
        this.shadowRadius = 0.6f;
    }

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(NUEntities.SPIRIT_LORD.get(), SpiritLordRenderer::new);
    }
}
