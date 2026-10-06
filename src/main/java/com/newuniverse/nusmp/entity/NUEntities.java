package com.newuniverse.nusmp.entity;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NUEntities {
    private NUEntities() {}
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, NUSMP.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<SpiritLordEntity>> SPIRIT_LORD = ENTITIES.register("spirit_lord",
            () -> EntityType.Builder.of(SpiritLordEntity::new, MobCategory.CREATURE).sized(0.8f, 2.2f).clientTrackingRange(10).build("spirit_lord"));

    public static void attributes(EntityAttributeCreationEvent e) { e.put(SPIRIT_LORD.get(), SpiritLordEntity.createAttributes().build()); }
}
