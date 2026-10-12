package com.newuniverse.nusmp.dungeon;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** The creatures of the Living Dungeon's food web. */
public final class DungeonEntities {
    private DungeonEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, NUSMP.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<DungeonSlimeEntity>> SLIME = ENTITIES.register("dungeon_slime",
            () -> EntityType.Builder.of(DungeonSlimeEntity::new, MobCategory.CREATURE).sized(0.5f, 0.5f).clientTrackingRange(8).build("dungeon_slime"));

    public static final DeferredHolder<EntityType<?>, EntityType<LivingArmorEntity>> LIVING_ARMOR = ENTITIES.register("living_armor",
            () -> EntityType.Builder.of(LivingArmorEntity::new, MobCategory.MONSTER).sized(0.6f, 1.95f).clientTrackingRange(10).build("living_armor"));

    public static final DeferredHolder<EntityType<?>, EntityType<DirewolfEntity>> DIREWOLF = ENTITIES.register("dungeon_direwolf",
            () -> EntityType.Builder.of(DirewolfEntity::new, MobCategory.MONSTER).sized(1.3f, 1.3f).clientTrackingRange(10).build("dungeon_direwolf"));

    public static final DeferredHolder<EntityType<?>, EntityType<BasiliskEntity>> BASILISK = ENTITIES.register("basilisk",
            () -> EntityType.Builder.of(BasiliskEntity::new, MobCategory.MONSTER).sized(1.6f, 1.0f).clientTrackingRange(10).build("basilisk"));

    public static void attributes(EntityAttributeCreationEvent e) {
        e.put(SLIME.get(), DungeonSlimeEntity.createAttributes().build());
        e.put(LIVING_ARMOR.get(), LivingArmorEntity.createAttributes().build());
        e.put(DIREWOLF.get(), DirewolfEntity.createAttributes().build());
        e.put(BASILISK.get(), BasiliskEntity.createAttributes().build());
    }
}
