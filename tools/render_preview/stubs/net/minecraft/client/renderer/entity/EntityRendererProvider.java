package net.minecraft.client.renderer.entity;

import net.minecraft.world.entity.Entity;

/** Preview stub of EntityRendererProvider. */
@FunctionalInterface
public interface EntityRendererProvider<T extends Entity> {
    EntityRenderer<T> create(Context context);

    class Context {
        public Context() {}
    }
}
