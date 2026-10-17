package com.newuniverse.nusmp.labyrinth.mega.hub;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.world.BiomeGenerationSettingsBuilder;
import net.neoforged.neoforge.common.world.BiomeModifier;
import net.neoforged.neoforge.common.world.MobSpawnSettingsBuilder;
import net.neoforged.neoforge.common.world.ModifiableBiomeInfo;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import java.util.List;

/**
 * Why: the data half of the Sanctuary Rules. NeoForge's {@code BiomeModifier}s rewrite what a biome offers (its spawns, its features) AFTER every mod has
 * had its say, and they are data, so they are the cleanest place to say "this biome is peaceful and plain" once for every spawn and feature another mod
 * adds. The stock {@code neoforge:remove_spawns} removes by entity TYPE, which can never be complete (a new mod's monster is not on any list), so this
 * modifier removes by CATEGORY: in the phase that runs last ({@link Phase#AFTER_EVERYTHING}) it empties every spawner list of the biome except the
 * creature list, and from that list any type registered as a monster; and it removes every placed feature whose id is not in one of the kept
 * namespaces (default only {@code minecraft}: the plains' own grass, flowers and trees), which is what keeps a mod's ruins, towers and ores, added to
 * "every overworld biome", out of Floor 0 when they arrive as features. It is registered under {@code nusmp:sanctuary_peace} and used by
 * {@code data/nusmp/neoforge/biome_modifier/sanctuary_peace.json} on {@code nusmp:sanctuary_plains} only. It is a belt, not the braces: the event
 * handlers in {@link HubRules} stay the guarantee for the mobs (they see spawns this data never lists), and the chunk generator is the guarantee for the
 * structures.
 */
public record SanctuaryPeaceModifier(HolderSet<Biome> biomes, List<String> keepNamespaces) implements BiomeModifier {
    public static final MapCodec<SanctuaryPeaceModifier> CODEC = RecordCodecBuilder.mapCodec(b -> b.group(
            Biome.LIST_CODEC.fieldOf("biomes").forGetter(SanctuaryPeaceModifier::biomes),
            Codec.STRING.listOf().optionalFieldOf("keep_namespaces", List.of("minecraft")).forGetter(SanctuaryPeaceModifier::keepNamespaces)
    ).apply(b, SanctuaryPeaceModifier::new));

    private static final DeferredRegister<MapCodec<? extends BiomeModifier>> SERIALIZERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.BIOME_MODIFIER_SERIALIZERS, NUSMP.MODID);
    public static final DeferredHolder<MapCodec<? extends BiomeModifier>, MapCodec<SanctuaryPeaceModifier>> TYPE = SERIALIZERS.register("sanctuary_peace", () -> CODEC);

    /** Registers the serializer; called from {@code HubRegistry.init} on the mod bus. */
    public static void init(IEventBus modBus) { SERIALIZERS.register(modBus); }

    @Override
    public void modify(Holder<Biome> biome, Phase phase, ModifiableBiomeInfo.BiomeInfo.Builder builder) {
        if (phase != Phase.AFTER_EVERYTHING || !biomes.contains(biome)) return;
        MobSpawnSettingsBuilder spawns = builder.getMobSpawnSettings();
        for (MobCategory category : MobCategory.values()) {
            if (category == MobCategory.CREATURE) spawns.getSpawner(category).removeIf(data -> data.type.getCategory() == MobCategory.MONSTER);
            else spawns.getSpawner(category).clear();
        }
        BiomeGenerationSettingsBuilder generation = builder.getGenerationSettings();
        for (GenerationStep.Decoration step : GenerationStep.Decoration.values()) generation.getFeatures(step).removeIf(feature -> !kept(feature));
    }

    /** Does this placed feature's id sit in one of the kept namespaces? A feature written inline in a biome (no id) is not kept. */
    boolean kept(Holder<?> feature) {
        return feature.unwrapKey().map(key -> keepNamespaces.contains(key.location().getNamespace())).orElse(false);
    }

    @Override
    public MapCodec<? extends BiomeModifier> codec() { return TYPE.get(); }
}
