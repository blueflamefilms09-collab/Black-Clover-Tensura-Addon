package net.minecraft.client.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.function.Function;

/** Preview stub of AgeableListModel: head parts, then body parts (adult proportions only). */
public abstract class AgeableListModel<E extends Entity> extends EntityModel<E> {
    protected AgeableListModel() { this(true, 16.0F, 0.0F); }

    protected AgeableListModel(boolean scaleHead, float babyYHeadOffset, float babyZHeadOffset) { super(RenderType::entityCutoutNoCull); }

    protected AgeableListModel(Function<ResourceLocation, RenderType> renderType, boolean scaleHead, float babyYHeadOffset, float babyZHeadOffset) { super(renderType); }

    protected abstract Iterable<ModelPart> headParts();

    protected abstract Iterable<ModelPart> bodyParts();

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer buffer, int packedLight, int packedOverlay, int color) {
        for (ModelPart part : headParts()) part.render(poseStack, buffer, packedLight, packedOverlay, color);
        for (ModelPart part : bodyParts()) part.render(poseStack, buffer, packedLight, packedOverlay, color);
    }
}
