package com.newuniverse.nusmp.corpse;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the three named puppets of Rades need their own entity types so each can have its own stats ({@link NamedCorpses}), model and
 * silhouette. They use {@link MobCategory#MISC}: never spawned by the world, not counted against the monster cap, and not removed
 * in Peaceful (a summon of the player must not vanish with the difficulty).
 * <p>
 * Wire it in NUSMP: {@code CorpseEntities.init()}, {@code CorpseEntities.ENTITIES.register(modEventBus)} and
 * {@code modEventBus.addListener(CorpseEntities::attributes)}.
 */
public final class CorpseEntities {
    private CorpseEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, NUSMP.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<NamedCorpseEntity>> JIMMY = named(NamedCorpses.JIMMY);
    public static final DeferredHolder<EntityType<?>, EntityType<NamedCorpseEntity>> ALFREDO = named(NamedCorpses.ALFREDO);
    public static final DeferredHolder<EntityType<?>, EntityType<NamedCorpseEntity>> CARL = named(NamedCorpses.CARL);

    private static DeferredHolder<EntityType<?>, EntityType<NamedCorpseEntity>> named(NamedCorpses k) {
        return ENTITIES.register(k.id, () -> EntityType.Builder.<NamedCorpseEntity>of((t, l) -> new NamedCorpseEntity(t, l, k), MobCategory.MISC)
                .sized(k.width, k.height).clientTrackingRange(10).build(k.id));
    }

    /** Loads this class before the registry event; call it from the NUSMP constructor. */
    public static void init() {}

    public static EntityType<NamedCorpseEntity> type(NamedCorpses k) {
        return switch (k) {
            case JIMMY -> JIMMY.get();
            case ALFREDO -> ALFREDO.get();
            case CARL -> CARL.get();
        };
    }

    public static void attributes(EntityAttributeCreationEvent e) {
        for (NamedCorpses k : NamedCorpses.values()) e.put(type(k), NamedCorpseEntity.createAttributes(k).build());
    }
}
