package net.minecraft.client.renderer.entity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;

/** Preview stub of RenderLayerParent: the renderer a layer belongs to. */
public interface RenderLayerParent<T extends Entity, M extends EntityModel<T>> {
    M getModel();

    ResourceLocation getTextureLocation(T entity);
}
