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
            () -> EntityType.Builder.of(SpiritLordEntity::new, MobCategory.CREATURE).sized(0.6f, 1.4f).clientTrackingRange(10).build("spirit_lord"));

    /** 0.42: Mirror Magic's Real Double (a tangible mirror clone of its owner). */
    public static final DeferredHolder<EntityType<?>, EntityType<MirrorDoubleEntity>> MIRROR_DOUBLE = ENTITIES.register("mirror_double",
            () -> EntityType.Builder.of(MirrorDoubleEntity::new, MobCategory.MISC).sized(0.6f, 1.8f).clientTrackingRange(10).build("mirror_double"));

    /** 0.44: Painting Magic's living illustrations (painted beast / einherjar / giant). */
    public static final DeferredHolder<EntityType<?>, EntityType<PaintedConstructEntity>> PAINTED_CONSTRUCT = ENTITIES.register("painted_construct",
            () -> EntityType.Builder.of(PaintedConstructEntity::new, MobCategory.MISC).sized(0.6f, 1.95f).clientTrackingRange(10).build("painted_construct"));

    public static void attributes(EntityAttributeCreationEvent e) {
        e.put(SPIRIT_LORD.get(), SpiritLordEntity.createAttributes().build());
        e.put(MIRROR_DOUBLE.get(), MirrorDoubleEntity.createAttributes().build());
        e.put(PAINTED_CONSTRUCT.get(), PaintedConstructEntity.createAttributes().build());
    }
}
