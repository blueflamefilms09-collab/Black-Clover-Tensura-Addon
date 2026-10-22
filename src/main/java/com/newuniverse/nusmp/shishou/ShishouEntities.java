package com.newuniverse.nusmp.shishou;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Why: the entity type of Shishou's body ({@code nusmp:shishou}): one type for the wild sword, the dormant sword and the thrown minion. Its own register, attached to the mod bus by {@link ShishouBoot}. */
public final class ShishouEntities {
    private ShishouEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, NUSMP.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<ShishouEntity>> SHISHOU = ENTITIES.register("shishou",
            () -> EntityType.Builder.of(ShishouEntity::new, MobCategory.MISC).sized(0.9f, 0.6f).fireImmune().clientTrackingRange(10).updateInterval(2).build("shishou"));

    public static void attributes(EntityAttributeCreationEvent e) { e.put(SHISHOU.get(), ShishouEntity.createAttributes().build()); }
}
