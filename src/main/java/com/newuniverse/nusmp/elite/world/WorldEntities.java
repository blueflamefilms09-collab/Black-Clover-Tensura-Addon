package com.newuniverse.nusmp.elite.world;

import com.newuniverse.nusmp.NUSMP;
import com.newuniverse.nusmp.elite.world.entity.GaiaSpikeEntity;
import com.newuniverse.nusmp.elite.world.entity.GraveboundColossus;
import com.newuniverse.nusmp.elite.world.entity.LichBoss;
import com.newuniverse.nusmp.elite.world.entity.PhylacteryEntity;
import com.newuniverse.nusmp.elite.world.entity.VoidBoltProjectile;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the entity types of the world engine in a register of their own: the two calamity bosses ({@code gravebound_colossus}, {@code lich_boss}), the earth spike ({@code gaia_spike}), the void
 * projectile ({@code void_bolt}) and the Lich's vessel ({@code soul_phylactery}). The bosses are monsters that never spawn by themselves (they are in no biome's spawn list); the calamity
 * director creates them. The spike and the bolt are not saved with the world.
 */
public final class WorldEntities {
    private WorldEntities() {}

    public static final DeferredRegister<EntityType<?>> TYPES = DeferredRegister.create(Registries.ENTITY_TYPE, NUSMP.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<GraveboundColossus>> GRAVEBOUND_COLOSSUS = TYPES.register("gravebound_colossus",
            () -> EntityType.Builder.<GraveboundColossus>of(GraveboundColossus::new, MobCategory.MONSTER).sized(2.4f, 5.4f).fireImmune().clientTrackingRange(16).updateInterval(2).build("gravebound_colossus"));

    public static final DeferredHolder<EntityType<?>, EntityType<LichBoss>> LICH_BOSS = TYPES.register("lich_boss",
            () -> EntityType.Builder.<LichBoss>of(LichBoss::new, MobCategory.MONSTER).sized(0.9f, 2.5f).fireImmune().clientTrackingRange(16).updateInterval(2).build("lich_boss"));

    public static final DeferredHolder<EntityType<?>, EntityType<PhylacteryEntity>> PHYLACTERY = TYPES.register("soul_phylactery",
            () -> EntityType.Builder.<PhylacteryEntity>of(PhylacteryEntity::new, MobCategory.MONSTER).sized(0.7f, 1.2f).fireImmune().clientTrackingRange(12).build("soul_phylactery"));

    public static final DeferredHolder<EntityType<?>, EntityType<GaiaSpikeEntity>> GAIA_SPIKE = TYPES.register("gaia_spike",
            () -> EntityType.Builder.<GaiaSpikeEntity>of(GaiaSpikeEntity::new, MobCategory.MISC).sized(1.0f, 2.0f).fireImmune().noSave().clientTrackingRange(10).updateInterval(20).build("gaia_spike"));

    public static final DeferredHolder<EntityType<?>, EntityType<VoidBoltProjectile>> VOID_BOLT = TYPES.register("void_bolt",
            () -> EntityType.Builder.<VoidBoltProjectile>of(VoidBoltProjectile::new, MobCategory.MISC).sized(0.4f, 0.4f).noSave().clientTrackingRange(10).updateInterval(1).build("void_bolt"));

    public static void attributes(EntityAttributeCreationEvent e) {
        e.put(GRAVEBOUND_COLOSSUS.get(), GraveboundColossus.attributes().build());
        e.put(LICH_BOSS.get(), LichBoss.attributes().build());
        e.put(PHYLACTERY.get(), PhylacteryEntity.attributes().build());
    }
}
