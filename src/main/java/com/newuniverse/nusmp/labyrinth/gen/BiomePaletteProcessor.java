package com.newuniverse.nusmp.labyrinth.gen;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Map;
import java.util.Optional;

/**
 * Why: the adaptive palette as a structure processor ({@code nusmp:biome_palette}): every template block that is in one of the four swap
 * tags is replaced, at placement, by the block the biome's {@code nusmp:labyrinth_palette} entry names for that slot. Processors run per
 * block, per piece, per chunk, with the template block as input, so A1 and A2 build ONE set of templates in the default look and the
 * same NBT files produce a packed-ice spire over the tundra and a sandstone one over the desert; vanilla's own {@code minecraft:rule}
 * processor cannot do this because its output is fixed in the JSON, not looked up from the biome. The four tags are codec fields with
 * the contract tags as defaults, so the bare {@code {"processor_type": "nusmp:biome_palette"}} works and a datapack can still re-point a
 * slot. Properties are carried over BY NAME (axis, facing, half, shape, waterlogged...) so a rotated pillar or an upside-down stair keeps
 * its orientation in the new material; a property the target lacks is dropped, a value it rejects is left at the default
 * ({@code trySetValue}). Wood goes through {@link PaletteSwap}'s suffix rule and a registry existence check. The processor never throws:
 * a broken palette entry or a missing block keeps the template block, because one exception here would abort the whole piece.
 */
public final class BiomePaletteProcessor extends StructureProcessor {

    public static final MapCodec<BiomePaletteProcessor> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            TagKey.codec(Registries.BLOCK).optionalFieldOf("pillar", GenRegistry.SWAP_PILLAR).forGetter(p -> p.pillar),
            TagKey.codec(Registries.BLOCK).optionalFieldOf("accent", GenRegistry.SWAP_ACCENT).forGetter(p -> p.accent),
            TagKey.codec(Registries.BLOCK).optionalFieldOf("floor", GenRegistry.SWAP_FLOOR).forGetter(p -> p.floor),
            TagKey.codec(Registries.BLOCK).optionalFieldOf("wood", GenRegistry.SWAP_WOOD).forGetter(p -> p.wood)
    ).apply(i, BiomePaletteProcessor::new));

    private final TagKey<Block> pillar, accent, floor, wood;

    public BiomePaletteProcessor(TagKey<Block> pillar, TagKey<Block> accent, TagKey<Block> floor, TagKey<Block> wood) {
        this.pillar = pillar;
        this.accent = accent;
        this.floor = floor;
        this.wood = wood;
    }

    /** The contract defaults: the four {@code nusmp:labyrinth/swap_*} tags. */
    public BiomePaletteProcessor() {
        this(GenRegistry.SWAP_PILLAR, GenRegistry.SWAP_ACCENT, GenRegistry.SWAP_FLOOR, GenRegistry.SWAP_WOOD);
    }

    @Override
    protected StructureProcessorType<?> getType() { return GenRegistry.BIOME_PALETTE.get(); }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos piecePos, BlockPos structureOrigin,
                                                             StructureTemplate.StructureBlockInfo original, StructureTemplate.StructureBlockInfo placed,
                                                             StructurePlaceSettings settings) {
        try {
            if (placed == null) return placed;
            BlockState state = placed.state();
            if (state == null) return placed;
            LabyrinthPalette.Slot slot = slotOf(state);                                                      // cheap tag checks first: most blocks are not swappable
            if (slot == null) return placed;
            LabyrinthPalette palette = BiomeMirror.paletteAt(level, placed.pos());
            if (palette == null) return placed;                                                              // no entry for this biome: the default look
            Block target = targetOf(state, slot, palette);
            if (target == null || target == state.getBlock()) return placed;
            return new StructureTemplate.StructureBlockInfo(placed.pos(), carryProperties(state, target.defaultBlockState()), placed.nbt());
        } catch (RuntimeException e) {
            return placed;
        }
    }

    /** Which palette slot a template block belongs to, or null if it is not in any swap tag. */
    LabyrinthPalette.Slot slotOf(BlockState state) {
        if (state.is(pillar)) return LabyrinthPalette.Slot.PILLAR;
        if (state.is(accent)) return LabyrinthPalette.Slot.ACCENT;
        if (state.is(floor)) return LabyrinthPalette.Slot.FLOOR;
        if (state.is(wood)) return LabyrinthPalette.Slot.WOOD;
        return null;
    }

    /** The replacement block: 1:1 for stone slots, the suffix-mapped family member for wood (null = keep the template block). */
    static Block targetOf(BlockState state, LabyrinthPalette.Slot slot, LabyrinthPalette palette) {
        Optional<Block> entry = palette.block(slot);
        if (entry.isEmpty()) return null;
        if (slot != LabyrinthPalette.Slot.WOOD) return entry.get();
        String templateId = BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        String woodId = BuiltInRegistries.BLOCK.getKey(entry.get()).toString();
        String targetId = PaletteSwap.wood(templateId, woodId);
        if (targetId == null) return null;
        ResourceLocation id = ResourceLocation.tryParse(targetId);
        if (id == null) return null;
        return BuiltInRegistries.BLOCK.getHolder(id).map(h -> h.value()).orElse(null);                    // the family lacks this shape: keep the original
    }

    /** Copies every property of {@code from} whose NAME the target block also has, parsing the value by its serialized name. */
    public static BlockState carryProperties(BlockState from, BlockState to) {
        StateDefinition<Block, BlockState> definition = to.getBlock().getStateDefinition();
        BlockState out = to;
        for (Map.Entry<Property<?>, Comparable<?>> e : from.getValues().entrySet()) {
            Property<?> mine = definition.getProperty(e.getKey().getName());
            if (mine == null) continue;
            out = carry(out, e.getKey(), e.getValue(), mine);
        }
        return out;
    }

    private static <A extends Comparable<A>, B extends Comparable<B>> BlockState carry(BlockState out, Property<A> from, Comparable<?> value, Property<B> to) {
        String name = from.getName(from.getValueClass().cast(value));
        Optional<B> parsed = to.getValue(name);
        return parsed.isPresent() ? out.trySetValue(to, parsed.get()) : out;
    }
}
