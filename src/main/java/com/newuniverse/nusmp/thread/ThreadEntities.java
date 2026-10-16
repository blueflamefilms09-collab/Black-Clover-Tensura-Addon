package com.newuniverse.nusmp.thread;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: Vanessa's Thread Magic now owns two entity types of its own (Rouge, the red thread cat, and the thread trap node) and they
 * must not be tangled into the registers of other features, so they get one small register. Wire it in NUSMP: call {@link #init()},
 * {@code ENTITIES.register(modEventBus)} and {@code modEventBus.addListener(ThreadEntities::attributes)}; the client renderers are
 * registered by {@code client.thread.ThreadClient}.
 */
public final class ThreadEntities {
    private ThreadEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, NUSMP.MODID);

    /** Rouge: 0.35 m tall, never saved, tracked by anyone within 64 blocks, updated every tick so her leaps look right. */
    public static final DeferredHolder<EntityType<?>, EntityType<RougeEntity>> ROUGE = ENTITIES.register("rouge",
            () -> EntityType.Builder.<RougeEntity>of(RougeEntity::new, MobCategory.MISC).sized(0.35f, 0.4f).eyeHeight(0.3f).fireImmune()
                    .clientTrackingRange(8).updateInterval(1).build("rouge"));

    /** One node of a thread trap: a short, nearly invisible tripwire (see {@link ThreadTrapEntity}). */
    public static final DeferredHolder<EntityType<?>, EntityType<ThreadTrapEntity>> THREAD_TRAP = ENTITIES.register("thread_trap",
            () -> EntityType.Builder.<ThreadTrapEntity>of(ThreadTrapEntity::new, MobCategory.MISC).sized(1.4f, 0.9f).fireImmune()
                    .clientTrackingRange(6).updateInterval(20).noSummon().build("thread_trap"));

    /** Loads this class before the registry event; call it from the NUSMP constructor. */
    public static void init() {}

    public static void attributes(EntityAttributeCreationEvent e) {
        e.put(ROUGE.get(), RougeEntity.createAttributes().build());
    }
}
