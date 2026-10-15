package com.newuniverse.nusmp.labyrinth.gen;

import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.datamaps.DataMapType;
import net.neoforged.neoforge.registries.datamaps.RegisterDataMapTypesEvent;

/**
 * Why: the Labyrinth's generation layer (1.35.0, docs/labyrinth/b_generation.md) is two structure PROCESSOR TYPES and one DATA MAP, and
 * this is the one class that registers them, in the pattern of {@link com.newuniverse.nusmp.dungeon.DungeonBlocks} (one DeferredRegister
 * per package, one {@code init(IEventBus)} the system class calls). Processors instead of pre-baked template variants: the Spire's pieces
 * are built ONCE from the default masonry and the processors rewrite single blocks at placement time, so one template set yields a
 * different-looking spire in every biome and the veins get denser towards the peak without A2 drawing a second set of NBT files.
 * {@link StructureProcessorType} is a functional interface returning the MapCodec, hence the {@code () -> () -> CODEC} registration: the
 * registry entry IS the codec and {@code processor_type} in a processor list JSON names it. The palette is a NeoForge data map on the biome
 * registry rather than a hard-coded {@code Map<ResourceKey<Biome>, ...>} because a data map is a datapack file keyed by biome id OR biome
 * tag, merged across packs: a modpack adds its own biomes' palettes (or a wood mod's planks) in a JSON next to ours without touching this
 * jar, which vanilla has no equivalent for (biome tags can only say yes/no, not carry a value). The data map is not synced to clients:
 * it is read only server-side, during placement and by {@code /multiverse labyrinth palette}.
 */
public final class GenRegistry {
    private GenRegistry() {}

    public static final DeferredRegister<StructureProcessorType<?>> PROCESSORS = DeferredRegister.create(Registries.STRUCTURE_PROCESSOR, NUSMP.MODID);

    /** {@code nusmp:biome_palette}: swaps the tagged template blocks for the biome palette's blocks (see BiomePaletteProcessor). */
    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<BiomePaletteProcessor>> BIOME_PALETTE =
            PROCESSORS.register("biome_palette", () -> (StructureProcessorType<BiomePaletteProcessor>) () -> BiomePaletteProcessor.CODEC);
    /** {@code nusmp:magicule_veins}: the glowing veins whose density rises up and inward (see MagiculeVeinProcessor). */
    public static final DeferredHolder<StructureProcessorType<?>, StructureProcessorType<MagiculeVeinProcessor>> MAGICULE_VEINS =
            PROCESSORS.register("magicule_veins", () -> (StructureProcessorType<MagiculeVeinProcessor>) () -> MagiculeVeinProcessor.CODEC);

    /** Data map {@code nusmp:labyrinth_palette} over {@code minecraft:worldgen/biome}: data/nusmp/data_maps/worldgen/biome/labyrinth_palette.json. */
    public static final DataMapType<Biome, LabyrinthPalette> PALETTE =
            DataMapType.builder(id("labyrinth_palette"), Registries.BIOME, LabyrinthPalette.CODEC).build();

    /** The template blocks the palette processor may replace, one tag per palette slot (data/nusmp/tags/block/labyrinth/). */
    public static final TagKey<Block> SWAP_PILLAR = blockTag("labyrinth/swap_pillar");
    public static final TagKey<Block> SWAP_ACCENT = blockTag("labyrinth/swap_accent");
    public static final TagKey<Block> SWAP_FLOOR = blockTag("labyrinth/swap_floor");
    public static final TagKey<Block> SWAP_WOOD = blockTag("labyrinth/swap_wood");
    /** The masonry the vein processor may turn into {@code nusmp:magicule_vein}. */
    public static final TagKey<Block> VEINABLE = blockTag("labyrinth/veinable");

    public static void init(IEventBus modBus) {
        PROCESSORS.register(modBus);
        modBus.addListener(GenRegistry::registerDataMaps);                                                 // RegisterDataMapTypesEvent is a mod-bus event
    }

    private static void registerDataMaps(RegisterDataMapTypesEvent event) {
        event.register(PALETTE);
    }

    static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, path); }

    private static TagKey<Block> blockTag(String path) { return TagKey.create(Registries.BLOCK, id(path)); }
}
