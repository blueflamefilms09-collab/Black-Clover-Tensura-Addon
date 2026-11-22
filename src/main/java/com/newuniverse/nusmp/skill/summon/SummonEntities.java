package com.newuniverse.nusmp.skill.summon;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the one entity type of the Summoner's Hell Mode creatures ({@code anime_multiverse:summon_beast}, docs/summoner_creatures.md), in its own small register like the devil's. MISC so it never counts toward
 * the monster cap, fire immune (the Dragon), tracked within 10 chunks, updated every 3 ticks. Registered and given its attributes by {@link #init}; the renderer is {@code client.summon.SummonClient}.
 */
public final class SummonEntities {
    private SummonEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, NUSMP.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<SummonBeast>> BEAST = ENTITIES.register("summon_beast",
            () -> EntityType.Builder.<SummonBeast>of(SummonBeast::new, MobCategory.MISC).sized(0.9f, 1.3f).clientTrackingRange(10).updateInterval(3).fireImmune().build("summon_beast"));

    private static boolean done;

    public static synchronized void init(IEventBus modBus) {
        if (done) return;
        done = true;
        ENTITIES.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent e) -> e.put(BEAST.get(), SummonBeast.createAttributes().build()));
    }
}
