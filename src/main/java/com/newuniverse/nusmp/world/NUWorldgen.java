package com.newuniverse.nusmp.world;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Worldgen features. Placement/rarity live in data/nusmp/worldgen + neoforge/biome_modifier JSON. */
public final class NUWorldgen {
    private NUWorldgen() {}
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(Registries.FEATURE, NUSMP.MODID);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> GRIMOIRE_TOWER = FEATURES.register("grimoire_tower", GrimoireTowerFeature::new);
    public static final DeferredHolder<Feature<?>, Feature<NoneFeatureConfiguration>> LIBRARY_RUINS = FEATURES.register("library_ruins", LibraryRuinsFeature::new);
}
