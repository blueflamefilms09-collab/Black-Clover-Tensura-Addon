package com.newuniverse.nusmp.meteor;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The one entity type of the shared meteor. Never saved ({@code noSave}), never summoned by command ({@code noSummon}), fire-immune. The movement of a meteor is a pure function of its
 * synced numbers, so it is tracked with a long update interval: after the packet that spawns it the server sends it no position packets at all.
 */
public final class MeteorEntities {
    private MeteorEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, NUSMP.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<MeteorEntity>> METEOR = ENTITIES.register("meteor",
            () -> EntityType.Builder.<MeteorEntity>of(MeteorEntity::new, MobCategory.MISC).sized(1.0f, 1.0f).fireImmune().noSave().noSummon()
                    .clientTrackingRange(8).updateInterval(200).build("meteor"));
}
