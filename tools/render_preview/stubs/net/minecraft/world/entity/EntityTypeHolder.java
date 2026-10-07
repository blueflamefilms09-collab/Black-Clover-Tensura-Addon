package net.minecraft.world.entity;

/** Preview helper (not in the game): stands in for NeoForge's DeferredHolder, which has get(). */
public class EntityTypeHolder<T extends Entity> {
    private final EntityType<? extends T> type = new EntityType<>();

    public EntityType<? extends T> get() { return type; }
}
