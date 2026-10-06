package net.minecraft.client.renderer.entity.layers;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;

public abstract class RenderLayer<T, M> {
    private final RenderLayerParent<T, M> parent;
    protected RenderLayer(RenderLayerParent<T, M> parent) { this.parent = parent; }
    public M getParentModel() { return parent.getModel(); }
    public abstract void render(PoseStack pose, MultiBufferSource buffers, int light, T entity, float limbSwing, float limbSwingAmount,
                                float partial, float ageInTicks, float netHeadYaw, float headPitch);
}
