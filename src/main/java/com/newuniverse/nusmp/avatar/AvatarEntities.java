package com.newuniverse.nusmp.avatar;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the entity type of the player-made Avatar ({@code nusmp:player_avatar}) needs its own register, kept apart from the Devil Avatar boss's
 * ({@code entity.avatar.AvatarEntities}, a different package and a different thing). Registered by {@link AvatarSystem#init}.
 */
public final class AvatarEntities {
    private AvatarEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, NUSMP.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<AvatarEntity>> AVATAR = ENTITIES.register("player_avatar",
            () -> EntityType.Builder.<AvatarEntity>of(AvatarEntity::new, MobCategory.MISC).sized(0.6f, 1.8f).eyeHeight(1.62f).clientTrackingRange(10).updateInterval(2)
                    .build("player_avatar"));

    public static void attributes(EntityAttributeCreationEvent e) { e.put(AVATAR.get(), AvatarEntity.createAttributes().build()); }
}
