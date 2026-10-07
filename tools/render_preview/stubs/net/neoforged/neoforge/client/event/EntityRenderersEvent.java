package net.neoforged.neoforge.client.event;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/** Preview stub of EntityRenderersEvent (only RegisterRenderers.registerEntityRenderer, which MagicPropRenderer.register calls). */
public abstract class EntityRenderersEvent {
    public static class RegisterRenderers extends EntityRenderersEvent {
        public <T extends Entity> void registerEntityRenderer(EntityType<? extends T> entityType, EntityRendererProvider<T> provider) {}
    }
}
