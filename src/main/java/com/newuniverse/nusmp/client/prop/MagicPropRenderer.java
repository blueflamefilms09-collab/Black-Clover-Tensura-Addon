package com.newuniverse.nusmp.client.prop;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.newuniverse.nusmp.entity.NUEntities;
import com.newuniverse.nusmp.prop.MagicPropEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** 0.53: draws {@link MagicPropEntity} through the painter of its kind. */
public class MagicPropRenderer extends EntityRenderer<MagicPropEntity> {
    private static final ResourceLocation NONE = ResourceLocation.fromNamespaceAndPath("nusmp", "textures/particle/glow.png");

    public MagicPropRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
        this.shadowRadius = 0f;
    }

    public static void register(EntityRenderersEvent.RegisterRenderers e) {
        PropPainters.init();
        e.registerEntityRenderer(NUEntities.MAGIC_PROP.get(), MagicPropRenderer::new);
    }

    @Override public ResourceLocation getTextureLocation(MagicPropEntity e) { return NONE; }

    /** Props can be big and oddly shaped (a dome, a chain): never cull them by their hitbox. */
    @Override public boolean shouldRender(MagicPropEntity e, Frustum frustum, double x, double y, double z) { return true; }

    @Override
    public void render(MagicPropEntity e, float yaw, float partial, PoseStack pose, MultiBufferSource buffers, int light) {
        PropPainter painter = PropPainters.get(e.kind());
        if (painter == null) return;
        pose.pushPose();
        pose.mulPose(Axis.YP.rotationDegrees(-Mth.rotLerp(partial, e.yRotO, e.getYRot())));
        try {
            painter.paint(e, partial, e.tickCount + partial, pose, buffers, light);
        } finally {
            pose.popPose();
        }
    }
}
