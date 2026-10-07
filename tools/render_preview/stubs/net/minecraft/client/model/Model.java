package net.minecraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Function;

/** Preview stub of Model: a render type chosen from a texture, and renderToBuffer. */
public abstract class Model {
    protected final Function<ResourceLocation, RenderType> renderType;

    public Model(Function<ResourceLocation, RenderType> renderType) { this.renderType = renderType; }

    public final RenderType renderType(ResourceLocation location) { return renderType.apply(location); }

    public final void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay) {
        renderToBuffer(poseStack, buffer, packedLight, packedOverlay, -1);
    }

    public abstract void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color);
}
