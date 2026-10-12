package com.newuniverse.nusmp.entity.zogratis;

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
 * Everything the Zogratis family registers: the four bosses, the Dark Disciple, the Blood Beast and the Zogratis clone, their
 * attributes, and a spawn egg for each boss (registered through {@link NUItems#ITEMS}). Wire it in NUSMP: call {@link #init()},
 * {@code ENTITIES.register(modEventBus)} and {@code modEventBus.addListener(ZogratisEntities::attributes)}.
 */
public final class ZogratisEntities {
    private ZogratisEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, NUSMP.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<DanteBoss>> DANTE = ENTITIES.register("dante_zogratis",
            () -> EntityType.Builder.<DanteBoss>of(DanteBoss::new, MobCategory.MONSTER).sized(0.7f, 2.0f).fireImmune().clientTrackingRange(20).updateInterval(1).build("dante_zogratis"));
    public static final DeferredHolder<EntityType<?>, EntityType<VanicaBoss>> VANICA = ENTITIES.register("vanica_zogratis",
            () -> EntityType.Builder.<VanicaBoss>of(VanicaBoss::new, MobCategory.MONSTER).sized(0.7f, 2.0f).fireImmune().clientTrackingRange(20).updateInterval(1).build("vanica_zogratis"));
    public static final DeferredHolder<EntityType<?>, EntityType<ZenonBoss>> ZENON = ENTITIES.register("zenon_zogratis",
            () -> EntityType.Builder.<ZenonBoss>of(ZenonBoss::new, MobCategory.MONSTER).sized(0.7f, 2.0f).fireImmune().clientTrackingRange(20).updateInterval(1).build("zenon_zogratis"));
    public static final DeferredHolder<EntityType<?>, EntityType<LuciusBoss>> LUCIUS = ENTITIES.register("lucius_zogratis",
            () -> EntityType.Builder.<LuciusBoss>of(LuciusBoss::new, MobCategory.MONSTER).sized(0.7f, 2.0f).fireImmune().clientTrackingRange(24).updateInterval(1).build("lucius_zogratis"));

    public static final DeferredHolder<EntityType<?>, EntityType<DarkDiscipleEntity>> DARK_DISCIPLE = ENTITIES.register("dark_disciple",
            () -> EntityType.Builder.<DarkDiscipleEntity>of(DarkDiscipleEntity::new, MobCategory.MONSTER).sized(0.6f, 1.95f).fireImmune().clientTrackingRange(12).build("dark_disciple"));
    public static final DeferredHolder<EntityType<?>, EntityType<BloodBeastEntity>> BLOOD_BEAST = ENTITIES.register("blood_beast",
            () -> EntityType.Builder.<BloodBeastEntity>of(BloodBeastEntity::new, MobCategory.MONSTER).sized(1.2f, 1.0f).fireImmune().clientTrackingRange(12).build("blood_beast"));
    public static final DeferredHolder<EntityType<?>, EntityType<ZogratisCloneEntity>> ZOGRATIS_CLONE = ENTITIES.register("zogratis_clone",
            () -> EntityType.Builder.<ZogratisCloneEntity>of(ZogratisCloneEntity::new, MobCategory.MONSTER).sized(0.7f, 2.0f).fireImmune().clientTrackingRange(16).updateInterval(1).build("zogratis_clone"));

    // ---------------------------------------------------------------- spawn eggs (bosses only)
    public static final DeferredItem<DeferredSpawnEggItem> DANTE_EGG = NUItems.ITEMS.register("dante_zogratis_spawn_egg",
            () -> new DeferredSpawnEggItem(DANTE, 0x16141A, 0xD9A81E, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> VANICA_EGG = NUItems.ITEMS.register("vanica_zogratis_spawn_egg",
            () -> new DeferredSpawnEggItem(VANICA, 0x2A0A0E, 0xC4161C, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> ZENON_EGG = NUItems.ITEMS.register("zenon_zogratis_spawn_egg",
            () -> new DeferredSpawnEggItem(ZENON, 0xCFC8B6, 0x2B3B8F, new Item.Properties()));
    public static final DeferredItem<DeferredSpawnEggItem> LUCIUS_EGG = NUItems.ITEMS.register("lucius_zogratis_spawn_egg",
            () -> new DeferredSpawnEggItem(LUCIUS, 0xF2E6B0, 0xC8960C, new Item.Properties()));

    /** The four eggs, for the creative tab. */
    public static List<DeferredItem<DeferredSpawnEggItem>> spawnEggs() { return List.of(DANTE_EGG, VANICA_EGG, ZENON_EGG, LUCIUS_EGG); }

    /** Loads this class (and so registers the eggs) before the item register's event; call it from the NUSMP constructor. */
    public static void init() {}

    public static void attributes(EntityAttributeCreationEvent e) {
        e.put(DANTE.get(), DanteBoss.createAttributes().build());
        e.put(VANICA.get(), VanicaBoss.createAttributes().build());
        e.put(ZENON.get(), ZenonBoss.createAttributes().build());
        e.put(LUCIUS.get(), LuciusBoss.createAttributes().build());
        e.put(DARK_DISCIPLE.get(), DarkDiscipleEntity.createAttributes().build());
        e.put(BLOOD_BEAST.get(), BloodBeastEntity.createAttributes().build());
        e.put(ZOGRATIS_CLONE.get(), ZogratisCloneEntity.createAttributes().build());
    }

    /** True for the four bosses and their followers: they never hurt each other. */
    public static boolean isFamily(Entity e) {
        if (e == null) return false;
        EntityType<?> t = e.getType();
        return t == DANTE.get() || t == VANICA.get() || t == ZENON.get() || t == LUCIUS.get()
                || t == DARK_DISCIPLE.get() || t == BLOOD_BEAST.get() || t == ZOGRATIS_CLONE.get();
    }
}
