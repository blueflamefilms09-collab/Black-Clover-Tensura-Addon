package com.newuniverse.nusmp.companion.sample;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.DeferredSpawnEggItem;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;

/**
 * Why: the registrations of the sample companion, on registers of their own so the feature is one self-contained unit (rule 12): one entity type {@code nusmp:arc_companion} (a subclass of the arcs' friendly
 * character, set to Charmy Pappitson) and its creative spawn egg. The egg is the only way to get one (it is not placed by the Hub or by any world generation), and it works only while {@code companions.enabled}
 * is on (a companion that spawns while the system is off simply falls back to the arcs' old chat talk).
 */
public final class CompanionEntities {
    private CompanionEntities() {}

    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, NUSMP.MODID);
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(NUSMP.MODID);

    /** The sample companion: Charmy Pappitson as a companion. */
    public static final DeferredHolder<EntityType<?>, EntityType<ArcCompanionEntity>> CHARMY = TYPES.register("arc_companion",
            () -> EntityType.Builder.of(ArcCompanionEntity::new, MobCategory.MISC).sized(0.6f, 1.95f).clientTrackingRange(10).build("arc_companion"));

    /** Charmy's cream and biscuit colours (the same two as her roster record). */
    public static final DeferredItem<DeferredSpawnEggItem> CHARMY_EGG = ITEMS.register("companion_charmy_spawn_egg",
            () -> new DeferredSpawnEggItem(CHARMY, 0xF4EAD0, 0xF0B070, new Item.Properties()));

    public static void init(IEventBus modBus) {
        TYPES.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(CompanionEntities::attributes);
    }

    private static void attributes(EntityAttributeCreationEvent e) {
        e.put(CHARMY.get(), ArcCompanionEntity.attributes().build());
    }

    /** The eggs for the creative tab. */
    public static List<DeferredItem<DeferredSpawnEggItem>> spawnEggs() { return List.of(CHARMY_EGG); }
}
