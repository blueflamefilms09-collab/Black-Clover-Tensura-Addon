package com.newuniverse.nusmp.dungeon.floor.goliath;

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
 * What the Goliath registers: its entity type (2.4 x 6.2 blocks), its attributes and a spawn egg (through {@link NUItems#ITEMS}). Wire it in
 * NUSMP: call {@link #init()}, {@code ENTITIES.register(modEventBus)} and {@code modEventBus.addListener(GoliathEntities::attributes)}.
 */
public final class GoliathEntities {
    private GoliathEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, NUSMP.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<GoliathBoss>> GOLIATH = ENTITIES.register("goliath",
            () -> EntityType.Builder.<GoliathBoss>of(GoliathBoss::new, MobCategory.MONSTER).sized(2.4f, 6.2f).fireImmune().clientTrackingRange(10).updateInterval(1).build("goliath"));

    /** The spawn egg: pale stone with amber cracks. */
    public static final DeferredItem<DeferredSpawnEggItem> GOLIATH_EGG = NUItems.ITEMS.register("goliath_spawn_egg",
            () -> new DeferredSpawnEggItem(GOLIATH, 0x9C998C, 0xFFAA33, new Item.Properties()));

    /** The egg, for the creative tab. */
    public static List<DeferredItem<DeferredSpawnEggItem>> spawnEggs() { return List.of(GOLIATH_EGG); }

    /** Loads this class (and so registers the egg) before the item register's event; call it from the NUSMP constructor. */
    public static void init() {}

    public static void attributes(EntityAttributeCreationEvent e) { e.put(GOLIATH.get(), GoliathBoss.createAttributes().build()); }

    /** Is this entity a Goliath? */
    public static boolean is(Entity e) { return e != null && e.getType() == GOLIATH.get(); }
}
