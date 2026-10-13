package com.newuniverse.nusmp.relics.order;

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
 * Everything the Order registers: the God of Order (entity type, attributes, spawn egg) and the Notice of Order (items go through
 * {@link NUItems#ITEMS}). Wire it in NUSMP: call {@link #init()}, {@code ENTITIES.register(modEventBus)} and
 * {@code modEventBus.addListener(OrderEntities::attributes)}; the client registers its renderer with {@code OrderClient::register}.
 */
public final class OrderEntities {
    private OrderEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, NUSMP.MODID);

    /** Tall (3.4 blocks with its halo below the top), slender, not a swimmer or a flier; it never spawns by itself. */
    public static final DeferredHolder<EntityType<?>, EntityType<GodOfOrder>> GOD_OF_ORDER = ENTITIES.register("god_of_order",
            () -> EntityType.Builder.<GodOfOrder>of(GodOfOrder::new, MobCategory.MONSTER).sized(0.8f, 3.4f).fireImmune().clientTrackingRange(24).updateInterval(1)
                    .build("god_of_order"));

    public static final DeferredItem<DeferredSpawnEggItem> GOD_OF_ORDER_EGG = NUItems.ITEMS.register("god_of_order_spawn_egg",
            () -> new DeferredSpawnEggItem(GOD_OF_ORDER, 0xF3EBD2, 0xC9A227, new Item.Properties()));

    /** The paper the God hands over. */
    public static final DeferredItem<NoticeOfOrderItem> NOTICE = NUItems.ITEMS.register("notice_of_order",
            () -> new NoticeOfOrderItem(new Item.Properties().stacksTo(16)));

    /** The egg, for the creative tab. */
    public static List<DeferredItem<DeferredSpawnEggItem>> spawnEggs() { return List.of(GOD_OF_ORDER_EGG); }

    /** Loads this class (and so registers the egg and the notice) before the item register's event; call it from the NUSMP constructor. */
    public static void init() {}

    public static void attributes(EntityAttributeCreationEvent e) {
        e.put(GOD_OF_ORDER.get(), GodOfOrder.createAttributes().build());
    }

    public static boolean isGod(Entity e) { return e != null && e.getType() == GOD_OF_ORDER.get(); }
}
