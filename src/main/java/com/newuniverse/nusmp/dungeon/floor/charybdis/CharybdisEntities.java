package com.newuniverse.nusmp.dungeon.floor.charybdis;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Everything the Charybdis Basin registers: the Charybdis Spawn (the Deep Waterways' floor boss), the magicule scale it fires, the boss's
 * attributes and its spawn egg (through {@link NUItems#ITEMS}). Wire it in NUSMP: call {@link #init()}, {@code ENTITIES.register(modEventBus)}
 * and {@code modEventBus.addListener(CharybdisEntities::attributes)}. The leviathan breathes water because the entity type tag
 * {@code minecraft:can_breathe_under_water} lists it (data/minecraft/tags/entity_type): {@code canBreatheUnderwater} cannot be overridden.
 */
public final class CharybdisEntities {
    private CharybdisEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, NUSMP.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<CharybdisSpawnBoss>> CHARYBDIS_SPAWN = ENTITIES.register("charybdis_spawn",
            () -> EntityType.Builder.<CharybdisSpawnBoss>of(CharybdisSpawnBoss::new, MobCategory.MONSTER).sized(CharybdisTable.WIDTH, CharybdisTable.HEIGHT)
                    .fireImmune().clientTrackingRange(24).updateInterval(1).build("charybdis_spawn"));

    /** A projectile that is never saved: a scale in the air when the world closes is simply gone. */
    public static final DeferredHolder<EntityType<?>, EntityType<MagiculeScaleEntity>> MAGICULE_SCALE = ENTITIES.register("magicule_scale",
            () -> EntityType.Builder.<MagiculeScaleEntity>of(MagiculeScaleEntity::new, MobCategory.MISC).sized(0.5f, 0.5f)
                    .fireImmune().noSave().clientTrackingRange(10).updateInterval(1).build("magicule_scale"));

    // ---------------------------------------------------------------- spawn egg
    public static final DeferredItem<DeferredSpawnEggItem> CHARYBDIS_SPAWN_EGG = NUItems.ITEMS.register("charybdis_spawn_egg",
            () -> new DeferredSpawnEggItem(CHARYBDIS_SPAWN, 0x0E4A5C, 0x4FF2E6, new Item.Properties()));

    /** The egg, for the creative tab. */
    public static List<DeferredItem<DeferredSpawnEggItem>> spawnEggs() { return List.of(CHARYBDIS_SPAWN_EGG); }

    /** Loads this class (and so registers the egg) before the item register's event; call it from the NUSMP constructor. */
    public static void init() {}

    public static void attributes(EntityAttributeCreationEvent e) {
        e.put(CHARYBDIS_SPAWN.get(), CharybdisSpawnBoss.createAttributes().build());
    }

    /** True for the leviathan and its scales. */
    public static boolean isOurs(Entity e) {
        if (e == null) return false;
        EntityType<?> t = e.getType();
        return t == CHARYBDIS_SPAWN.get() || t == MAGICULE_SCALE.get();
    }
}
