package com.newuniverse.nusmp.labyrinth.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.block.Block;

import java.util.Optional;

/**
 * Why: the value of the {@code nusmp:labyrinth_palette} data map: which block stands in for each of the four swap slots of the templates
 * (pillar, accent, floor, wood) in a given biome. A record with a RecordCodecBuilder codec over {@code BuiltInRegistries.BLOCK.byNameCodec()}
 * so a datapack writes plain block ids ({@code "pillar": "minecraft:packed_ice"}) and an unknown id is a load-time error in the log instead
 * of a silent air block. Every slot is OPTIONAL: a biome that only wants another wood leaves the stone alone, and "absent" means "keep the
 * template's block" (the default look), which is why the fields are {@code Optional<Block>} and not blocks defaulting to air. The wood slot
 * holds the family's PLANKS block; fences, stairs, slabs and gates are derived from it by id suffix ({@link PaletteSwap}), so one id per
 * biome covers every wooden part of a bridge.
 */
public record LabyrinthPalette(Optional<Block> pillar, Optional<Block> accent, Optional<Block> floor, Optional<Block> wood) {

    public static final Codec<LabyrinthPalette> CODEC = RecordCodecBuilder.create(i -> i.group(
            BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("pillar").forGetter(LabyrinthPalette::pillar),
            BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("accent").forGetter(LabyrinthPalette::accent),
            BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("floor").forGetter(LabyrinthPalette::floor),
            BuiltInRegistries.BLOCK.byNameCodec().optionalFieldOf("wood").forGetter(LabyrinthPalette::wood)
    ).apply(i, LabyrinthPalette::new));

    /** The four swap slots of a template, each backed by one block tag (GenRegistry.SWAP_*). */
    public enum Slot { PILLAR, ACCENT, FLOOR, WOOD }

    /** The palette's block for a slot; empty = the template block stays. */
    public Optional<Block> block(Slot slot) {
        return switch (slot) {
            case PILLAR -> pillar;
            case ACCENT -> accent;
            case FLOOR -> floor;
            case WOOD -> wood;
        };
    }

    /** One line for the command output: "pillar=minecraft:packed_ice accent=- floor=minecraft:snow_block wood=minecraft:spruce_planks". */
    public String describe() {
        return "pillar=" + name(pillar) + " accent=" + name(accent) + " floor=" + name(floor) + " wood=" + name(wood);
    }

    private static String name(Optional<Block> b) {
        return b.map(x -> BuiltInRegistries.BLOCK.getKey(x).toString()).orElse("-");
    }
}
