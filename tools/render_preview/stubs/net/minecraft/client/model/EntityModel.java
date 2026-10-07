package net.minecraft.client.model;

import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

import java.util.function.Function;

/** Preview stub of EntityModel. */
public abstract class EntityModel<T extends Entity> extends Model {
    public float attackTime;
    public boolean riding;
    public boolean young = true;

    protected EntityModel() { this(RenderType::entityCutoutNoCull); }

    protected EntityModel(Function<ResourceLocation, RenderType> renderType) { super(renderType); }

    public abstract void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch);

    public void prepareMobModel(T entity, float limbSwing, float limbSwingAmount, float partialTick) {}
}
