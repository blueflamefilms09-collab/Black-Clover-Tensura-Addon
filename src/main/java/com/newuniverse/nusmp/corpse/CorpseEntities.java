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

    /** The zombie of Soul Corpse Magic (a thin subclass of the vanilla zombie: owned, no sun burn, no drops). Appended; the ids above stay. */
    public static final DeferredHolder<EntityType<?>, EntityType<SoulZombieEntity>> SOUL_ZOMBIE = ENTITIES.register("soul_zombie",
            () -> EntityType.Builder.<SoulZombieEntity>of(SoulZombieEntity::new, MobCategory.MISC).sized(0.6f, 1.95f).clientTrackingRange(8).build("soul_zombie"));

    /** No.3 David and No.0 Michael Caesal (appended after the three old puppets; same entity class, their own rows in {@link NamedCorpses}). */
    public static final DeferredHolder<EntityType<?>, EntityType<NamedCorpseEntity>> DAVID = named(NamedCorpses.DAVID);
    public static final DeferredHolder<EntityType<?>, EntityType<NamedCorpseEntity>> MICHAEL_CAESAL = named(NamedCorpses.MICHAEL_CAESAL);

    private static DeferredHolder<EntityType<?>, EntityType<NamedCorpseEntity>> named(NamedCorpses k) {
        return ENTITIES.register(k.id, () -> EntityType.Builder.<NamedCorpseEntity>of((t, l) -> new NamedCorpseEntity(t, l, k), MobCategory.MISC)
                .sized(k.width, k.height).clientTrackingRange(8).build(k.id));
    }

    /** Loads this class before the registry event; call it from the NUSMP constructor. */
    public static void init() {}

    public static EntityType<NamedCorpseEntity> type(NamedCorpses k) {
        return switch (k) {
            case JIMMY -> JIMMY.get();
            case ALFREDO -> ALFREDO.get();
            case CARL -> CARL.get();
            case DAVID -> DAVID.get();
            case MICHAEL_CAESAL -> MICHAEL_CAESAL.get();
        };
    }

    public static void attributes(EntityAttributeCreationEvent e) {
        for (NamedCorpses k : NamedCorpses.values()) e.put(type(k), NamedCorpseEntity.createAttributes(k).build());
        e.put(SOUL_ZOMBIE.get(), SoulZombieEntity.createAttributes().build());
    }
}
