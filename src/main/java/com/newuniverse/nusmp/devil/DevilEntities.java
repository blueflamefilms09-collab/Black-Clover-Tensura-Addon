package com.newuniverse.nusmp.devil;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the entity type of the devil's figure ({@code anime_multiverse:devil_npc}, docs/devil_forms.md) in its own small register, as the other systems do. It is a plain entity (no health, no AI, no attributes),
 * never saved ({@code noSave}), fire immune, tracked within a short range (8 chunks), updated every 10 ticks (it never moves). Registered by {@link DevilBoot}.
 */
public final class DevilEntities {
    private DevilEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, NUSMP.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<DevilNpc>> NPC = ENTITIES.register("devil_npc",
            () -> EntityType.Builder.<DevilNpc>of(DevilNpc::new, MobCategory.MISC).sized(0.7f, 1.9f).clientTrackingRange(8).updateInterval(10).fireImmune().noSave().build("devil_npc"));

    /** Phase 2 (docs/devil_contract_phase2.md): the devil that fights before it agrees. Never saved, never counts toward a mob cap (MISC); attributes are registered by {@link DevilBoot}. */
    public static final DeferredHolder<EntityType<?>, EntityType<com.newuniverse.nusmp.devil.phase2.DevilTrialEntity>> TRIAL = ENTITIES.register("devil_trial",
            () -> EntityType.Builder.<com.newuniverse.nusmp.devil.phase2.DevilTrialEntity>of(com.newuniverse.nusmp.devil.phase2.DevilTrialEntity::new, MobCategory.MISC).sized(0.7f, 1.95f).clientTrackingRange(8).updateInterval(2).fireImmune().noSave().build("devil_trial"));

    public static void init(IEventBus modBus) { ENTITIES.register(modBus); }
}
