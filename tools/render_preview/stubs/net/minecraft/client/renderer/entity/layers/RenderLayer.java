package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/** Preview stub of RenderLayer: the base class of the mod's player layers (render is called after the model has been drawn). */
public abstract class RenderLayer<T extends Entity, M extends EntityModel<T>> {
    private final RenderLayerParent<T, M> renderer;

    public RenderLayer(RenderLayerParent<T, M> renderer) { this.renderer = renderer; }

    protected ResourceLocation getTextureLocation(T entity) { return renderer.getTextureLocation(entity); }

    public abstract void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, T livingEntity, float limbSwing, float limbSwingAmount,
                                float partialTick, float ageInTicks, float netHeadYaw, float headPitch);

    public M getParentModel() { return renderer.getModel(); }
}
