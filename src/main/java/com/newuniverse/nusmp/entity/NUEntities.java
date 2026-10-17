package com.newuniverse.nusmp.entity;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class NUEntities {
    private NUEntities() {}
    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, NUSMP.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<SpiritLordEntity>> SPIRIT_LORD = ENTITIES.register("spirit_lord",
            () -> EntityType.Builder.of(SpiritLordEntity::new, MobCategory.CREATURE).sized(0.6f, 1.4f).clientTrackingRange(10).build("spirit_lord"));

    /** 0.102: the four Black Clover Spirit Lords in their true bodies (exclusive: one mage per element), see ElementalSpiritLordEntity. */
    public static final DeferredHolder<EntityType<?>, EntityType<ElementalSpiritLordEntity>> ELEMENTAL_SPIRIT_LORD = ENTITIES.register("elemental_spirit_lord",
            () -> EntityType.Builder.of(ElementalSpiritLordEntity::new, MobCategory.CREATURE).sized(1.0f, 2.0f).clientTrackingRange(12).build("elemental_spirit_lord"));

    /** 0.42: Mirror Magic's Real Double (a tangible mirror clone of its owner). */
    public static final DeferredHolder<EntityType<?>, EntityType<MirrorDoubleEntity>> MIRROR_DOUBLE = ENTITIES.register("mirror_double",
            () -> EntityType.Builder.of(MirrorDoubleEntity::new, MobCategory.MISC).sized(0.6f, 1.8f).clientTrackingRange(10).build("mirror_double"));

    /** 0.44: Painting Magic's living illustrations (painted beast / einherjar / giant). */
    public static final DeferredHolder<EntityType<?>, EntityType<PaintedConstructEntity>> PAINTED_CONSTRUCT = ENTITIES.register("painted_construct",
            () -> EntityType.Builder.of(PaintedConstructEntity::new, MobCategory.MISC).sized(0.6f, 1.95f).clientTrackingRange(10).build("painted_construct"));

    /**
     * 0.47: Zagred, the Kotodama devil, as a 4-phase boss. 1.60.0: this is the LEGACY Zagred. The id {@code nusmp:zagred} now belongs to the remade boss
     * ({@code arc/zagred/ZagredBoss}, registered through {@code ArcEntities}); the old class stays (no egg, no command, no floor) under {@code nusmp:zagred_legacy}.
     * The Java name {@code ZAGRED} is kept because the legacy renderer and the legacy summon still use it.
     */
    public static final DeferredHolder<EntityType<?>, EntityType<ZagredBossEntity>> ZAGRED = ENTITIES.register("zagred_legacy",
            () -> EntityType.Builder.of(ZagredBossEntity::new, MobCategory.MONSTER).sized(0.6f, 1.95f).fireImmune().clientTrackingRange(16).build("zagred_legacy"));

    /** 0.52: Zagred's Grimoire Daemons (lesser, greater, arch, and the Overwrite rule stones). */
    public static final DeferredHolder<EntityType<?>, EntityType<GrimoireDaemonEntity>> GRIMOIRE_DAEMON = ENTITIES.register("grimoire_daemon",
            () -> EntityType.Builder.of(GrimoireDaemonEntity::new, MobCategory.MONSTER).sized(0.8f, 1.9f).fireImmune().clientTrackingRange(12).build("grimoire_daemon"));

    /** 0.52: Charmy's cotton sheep and cotton cloud (Cotton Magic). */
    public static final DeferredHolder<EntityType<?>, EntityType<CottonSheepEntity>> COTTON_SHEEP = ENTITIES.register("cotton_sheep",
            () -> EntityType.Builder.of(CottonSheepEntity::new, MobCategory.MISC).sized(0.9f, 1.9f).clientTrackingRange(10).updateInterval(1).build("cotton_sheep"));
    public static final DeferredHolder<EntityType<?>, EntityType<CottonCloudEntity>> COTTON_CLOUD = ENTITIES.register("cotton_cloud",
            () -> EntityType.Builder.<CottonCloudEntity>of(CottonCloudEntity::new, MobCategory.MISC).sized(2.6f, 0.6f).clientTrackingRange(10).updateInterval(1).build("cotton_cloud"));

    /** 0.53: Sylph, the Wind Spirit Lord, as a boss (command only: /multiverse boss sylph). */
    public static final DeferredHolder<EntityType<?>, EntityType<WindSpiritLordEntity>> WIND_SPIRIT_LORD = ENTITIES.register("wind_spirit_lord",
            () -> EntityType.Builder.of(WindSpiritLordEntity::new, MobCategory.MONSTER).sized(1.0f, 2.2f).fireImmune().clientTrackingRange(16).updateInterval(1).build("wind_spirit_lord"));

    /** 0.53: every real 3D thing of the Black Clover Magic and VFX expansion (floating eyes, keys, chains, food, soldiers, bubbles ...): see prop.MagicProps. */
    public static final DeferredHolder<EntityType<?>, EntityType<com.newuniverse.nusmp.prop.MagicPropEntity>> MAGIC_PROP = ENTITIES.register("magic_prop",
            () -> EntityType.Builder.<com.newuniverse.nusmp.prop.MagicPropEntity>of(com.newuniverse.nusmp.prop.MagicPropEntity::new, MobCategory.MISC)
                    .sized(1.0f, 1.0f).clientTrackingRange(12).updateInterval(1).build("magic_prop"));

    /** 1.6.0: the keeper of a Grimoire Tower (see wizard.TowerWizardEntity). */
    public static final DeferredHolder<EntityType<?>, EntityType<com.newuniverse.nusmp.wizard.TowerWizardEntity>> TOWER_WIZARD = ENTITIES.register("tower_wizard",
            () -> EntityType.Builder.of(com.newuniverse.nusmp.wizard.TowerWizardEntity::new, MobCategory.MISC).sized(0.6f, 1.95f).clientTrackingRange(10).build("tower_wizard"));

    public static void attributes(EntityAttributeCreationEvent e) {
        e.put(TOWER_WIZARD.get(), net.minecraft.world.entity.monster.Witch.createAttributes().build());
        e.put(SPIRIT_LORD.get(), SpiritLordEntity.createAttributes().build());
        e.put(ELEMENTAL_SPIRIT_LORD.get(), ElementalSpiritLordEntity.createAttributes().build());
        e.put(MIRROR_DOUBLE.get(), MirrorDoubleEntity.createAttributes().build());
        e.put(PAINTED_CONSTRUCT.get(), PaintedConstructEntity.createAttributes().build());
        e.put(ZAGRED.get(), ZagredBossEntity.createAttributes().build());
        e.put(GRIMOIRE_DAEMON.get(), GrimoireDaemonEntity.createAttributes().build());
        e.put(COTTON_SHEEP.get(), CottonSheepEntity.createAttributes().build());
        e.put(WIND_SPIRIT_LORD.get(), WindSpiritLordEntity.createAttributes().build());
    }
}
