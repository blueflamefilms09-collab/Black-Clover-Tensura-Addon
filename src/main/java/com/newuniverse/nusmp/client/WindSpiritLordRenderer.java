package com.newuniverse.nusmp.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.newuniverse.nusmp.entity.NUEntities;
import com.newuniverse.nusmp.entity.WindSpiritLordEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/**
 * 0.53 Sylph, the Wind Spirit Lord: STUB (draws nothing yet). The real renderer draws her geo.json models with client.geo.GeoDraw
 * (body cutout, wings translucent and iridescent, blade additive, glow pass) and emits her wind trails.
 */
public class WindSpiritLordRenderer extends EntityRenderer<WindSpiritLordEntity> {
    private static final ResourceLocation TEX = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/entity/sylph_body.png");

    public WindSpiritLordRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0.5f;
    }

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        e.registerEntityRenderer(NUEntities.WIND_SPIRIT_LORD.get(), WindSpiritLordRenderer::new);
    }

    @Override
    public ResourceLocation getTextureLocation(WindSpiritLordEntity e) { return TEX; }

    @Override
    public void render(WindSpiritLordEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        super.render(e, yaw, partial, pose, buffers, light);
    }
}
