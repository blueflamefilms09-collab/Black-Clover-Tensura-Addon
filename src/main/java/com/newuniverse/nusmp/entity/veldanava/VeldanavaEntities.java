package com.newuniverse.nusmp.entity.veldanava;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.item.NUItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Everything Veldanava registers: the boss (one entity that wears three bodies), the Will of the Creator, the spawn egg and the seven relics he leaves. Wire it in
 * NUSMP: call {@link #init()} before the item register, {@code ENTITIES.register(modEventBus)} and {@code modEventBus.addListener(VeldanavaEntities::attributes)}.
 */
public final class VeldanavaEntities {
    private VeldanavaEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, NUSMP.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<VeldanavaBoss>> VELDANAVA = ENTITIES.register("veldanava",
            () -> EntityType.Builder.<VeldanavaBoss>of(VeldanavaBoss::new, MobCategory.MONSTER).sized(VeldanavaRules.width(VeldanavaRules.DRAGON), VeldanavaRules.height(VeldanavaRules.DRAGON))
                    .fireImmune().clientTrackingRange(24).updateInterval(1).build("veldanava"));
    public static final DeferredHolder<EntityType<?>, EntityType<WillOfTheCreator>> WILL = ENTITIES.register("will_of_the_creator",
            () -> EntityType.Builder.<WillOfTheCreator>of(WillOfTheCreator::new, MobCategory.MISC).sized(1.0f, 1.0f).fireImmune().clientTrackingRange(10).updateInterval(10).build("will_of_the_creator"));

    // ---------------------------------------------------------------- the egg
    public static final DeferredItem<DeferredSpawnEggItem> VELDANAVA_EGG = NUItems.ITEMS.register("veldanava_spawn_egg",
            () -> new DeferredSpawnEggItem(VELDANAVA, 0x0A6A78, 0xF2D27A, new Item.Properties()));

    // ---------------------------------------------------------------- the loot
    public static final DeferredItem<VeldanavaItem> STAR_KING_DRAGON_CORE = NUItems.ITEMS.register("star_king_dragon_core",
            () -> new VeldanavaItem(VeldanavaItem.Kind.MATERIAL, Rarity.EPIC, 16));
    public static final DeferredItem<VeldanavaItem> GENESIS_FRAGMENT = NUItems.ITEMS.register("genesis_fragment",
            () -> new VeldanavaItem(VeldanavaItem.Kind.MATERIAL, Rarity.EPIC, 16));
    public static final DeferredItem<VeldanavaItem> CREATORS_ESSENCE = NUItems.ITEMS.register("creators_essence",
            () -> new VeldanavaItem(VeldanavaItem.Kind.ESSENCE, Rarity.EPIC, 16));
    public static final DeferredItem<VeldanavaItem> MICHAEL_FRAGMENT = NUItems.ITEMS.register("justice_king_michael_fragment",
            () -> new VeldanavaItem(VeldanavaItem.Kind.MICHAEL, Rarity.RARE, 16));
    public static final DeferredItem<VeldanavaItem> URIEL_FRAGMENT = NUItems.ITEMS.register("covenant_king_uriel_fragment",
            () -> new VeldanavaItem(VeldanavaItem.Kind.URIEL, Rarity.RARE, 1));
    public static final DeferredItem<VeldanavaItem> NIHILITY_FRAGMENT = NUItems.ITEMS.register("nihility_fragment",
            () -> new VeldanavaItem(VeldanavaItem.Kind.NIHILITY, Rarity.RARE, 1));
    public static final DeferredItem<VeldanavaItem> DIVINE_CORE = NUItems.ITEMS.register("veldanavas_divine_core",
            () -> new VeldanavaItem(VeldanavaItem.Kind.MATERIAL, Rarity.EPIC, 1));

    /** The egg and the seven relics, for the creative tab. */
    public static List<DeferredItem<? extends Item>> creativeItems() {
        return List.of(VELDANAVA_EGG, STAR_KING_DRAGON_CORE, GENESIS_FRAGMENT, CREATORS_ESSENCE, MICHAEL_FRAGMENT, URIEL_FRAGMENT, NIHILITY_FRAGMENT, DIVINE_CORE);
    }

    /** Loads this class (and so registers the egg and the relics) before the item register's event; call it from the NUSMP constructor. */
    public static void init() {}

    public static void attributes(EntityAttributeCreationEvent e) {
        e.put(VELDANAVA.get(), VeldanavaBoss.createAttributes().build());
    }

    /** True for Veldanava and his own: they never hurt each other. */
    public static boolean isOwn(Entity e) { return e != null && (e.getType() == VELDANAVA.get() || e.getType() == WILL.get()); }
}
