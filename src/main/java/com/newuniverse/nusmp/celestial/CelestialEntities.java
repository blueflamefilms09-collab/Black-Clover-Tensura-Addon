package com.newuniverse.nusmp.celestial;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Why: the one entity type of Celestial Spirit Magic, the Celestial Spirit King (the spirits themselves are magic props). Wire it in NUSMP: {@code ENTITIES.register(modEventBus)} and the attributes listener. */
public final class CelestialEntities {
    private CelestialEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, NUSMP.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<SpiritKingEntity>> SPIRIT_KING = ENTITIES.register("spirit_king",
            () -> EntityType.Builder.<SpiritKingEntity>of(SpiritKingEntity::new, MobCategory.MONSTER).sized(1.3f, 3.6f).fireImmune().clientTrackingRange(16).updateInterval(1).build("spirit_king"));

    public static void attributes(EntityAttributeCreationEvent e) { e.put(SPIRIT_KING.get(), SpiritKingEntity.createAttributes().build()); }
}
