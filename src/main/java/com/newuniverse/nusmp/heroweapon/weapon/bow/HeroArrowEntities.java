package com.newuniverse.nusmp.heroweapon.weapon.bow;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Why: the Legendary Bow's real arrow is a small mod entity type of its own, and it must not be tangled into the registers of other features, so it gets one
 * tiny register. It is sized and tracked exactly like the vanilla arrow (half a block, tracked by players within 64 blocks, so only nearby players are ever
 * told about it), never saved with the world and not summonable by command. It is listed in the {@code minecraft:arrows} entity-type tag
 * ({@code data/minecraft/tags/entity_type/arrows.json}), so vanilla and the mods that treat arrows specially treat it as an arrow. Wire it in the NUSMP
 * constructor: {@code ENTITIES.register(modEventBus)}; the client renderer is {@code client.heroweapon.bow.BowArrowClient}.
 */
public final class HeroArrowEntities {
    private HeroArrowEntities() {}

    public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE, NUSMP.MODID);

    public static final DeferredHolder<EntityType<?>, EntityType<HeroArrow>> HERO_ARROW = ENTITIES.register("hero_arrow",
            () -> EntityType.Builder.<HeroArrow>of(HeroArrow::new, MobCategory.MISC).sized(0.5f, 0.5f).eyeHeight(0.13f).clientTrackingRange(4).updateInterval(20)
                    .noSave().noSummon().build("hero_arrow"));

    /** Loads this class before the registry event. */
    public static void init() {}
}
