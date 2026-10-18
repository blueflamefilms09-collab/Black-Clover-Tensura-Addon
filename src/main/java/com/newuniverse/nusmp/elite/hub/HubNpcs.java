package com.newuniverse.nusmp.elite.hub;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the two NPC entity types of the Hub, registered once in their own deferred register (the pattern of {@code ArcEntities} and {@code GoliathEntities}) with the attributes event and, on the
 * client, a renderer each. {@code nusmp:quest_giver_npc} and {@code nusmp:merchant_npc} are {@code MISC} category creatures (never part of the natural spawn cycle, never counted against a mob
 * cap), human-sized, fire immune, tracked from 10 chunks away, and placed by {@link HubNpcSpawner} (or by their spawn eggs and {@code /summon}).
 */
public final class HubNpcs {
    private HubNpcs() {}

    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, NUSMP.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<QuestGiverNPC>> QUEST_GIVER = TYPES.register("quest_giver_npc",
            () -> EntityType.Builder.of(QuestGiverNPC::new, MobCategory.MISC).sized(0.6f, 1.95f).clientTrackingRange(10).fireImmune().build("quest_giver_npc"));

    public static final DeferredHolder<EntityType<?>, EntityType<MerchantNPC>> MERCHANT = TYPES.register("merchant_npc",
            () -> EntityType.Builder.of(MerchantNPC::new, MobCategory.MISC).sized(0.6f, 1.95f).clientTrackingRange(10).fireImmune().build("merchant_npc"));

    public static void init(IEventBus modBus) {
        TYPES.register(modBus);
        modBus.addListener(HubNpcs::attributes);
    }

    private static void attributes(EntityAttributeCreationEvent e) {
        e.put(QUEST_GIVER.get(), HubNpcBase.attributes().build());
        e.put(MERCHANT.get(), HubNpcBase.attributes().build());
    }
}
