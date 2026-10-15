package com.newuniverse.nusmp.labyrinth.gen;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.newuniverse.nusmp.NUSMP;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureProcessorType;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;

import java.util.Optional;

/**
 * Why: the Magicule Veins ({@code nusmp:magicule_veins}) are the breadcrumbs of the Spire: glowing veins in the masonry that grow denser
 * the higher and the closer to the axis a block is, so a climber reads "up and inward" from the walls themselves. A processor rather than
 * veins drawn into the templates because the density must depend on where a piece ended up in the WHOLE spire: Jigsaw hands every
 * processor the structure origin (the bottom centre of the start piece, from {@code StructureStart.placeInChunk}), and the same ascent
 * template placed on tier 2 and on tier 6 gets two different densities from one NBT file. The chance comes from {@link VeinMath#chance}
 * and the roll from a hash of (world seed, world position), see {@link VeinMath} for why not the piece's RandomSource. Only blocks in the
 * {@code targets} tag (default {@code nusmp:labyrinth/veinable}) are touched and the test is on the block AS PLACED SO FAR, so when the
 * palette processor runs first a swapped floor keeps its new material and only the remaining masonry veins. The vein block defaults to
 * {@code nusmp:magicule_vein} resolved lazily by id: a block-state codec default would need the block at class-load, and a missing block
 * must make the processor a no-op, never a datapack load error. The replaced block's NBT is dropped: the vein has no block entity.
 */
public final class MagiculeVeinProcessor extends StructureProcessor {

    public static final ResourceLocation DEFAULT_VEIN = ResourceLocation.fromNamespaceAndPath(NUSMP.MODID, "magicule_vein");
    public static final double DEFAULT_BASE = 0.01, DEFAULT_PEAK = 0.12, DEFAULT_PULL = 0.6;
    public static final int DEFAULT_RISE = 112;

    public static final MapCodec<MagiculeVeinProcessor> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            TagKey.codec(Registries.BLOCK).optionalFieldOf("targets", GenRegistry.VEINABLE).forGetter(p -> p.targets),
            BlockState.CODEC.optionalFieldOf("vein").forGetter(p -> p.vein),
            Codec.doubleRange(0.0, 1.0).optionalFieldOf("base_chance", DEFAULT_BASE).forGetter(p -> p.baseChance),
            Codec.doubleRange(0.0, 1.0).optionalFieldOf("peak_chance", DEFAULT_PEAK).forGetter(p -> p.peakChance),
            Codec.intRange(1, 4096).optionalFieldOf("rise", DEFAULT_RISE).forGetter(p -> p.rise),
            Codec.doubleRange(0.0, 1.0).optionalFieldOf("pull", DEFAULT_PULL).forGetter(p -> p.pull)
    ).apply(i, MagiculeVeinProcessor::new));

    private final TagKey<Block> targets;
    private final Optional<BlockState> vein;
    private final double baseChance, peakChance, pull;
    private final int rise;
    private volatile BlockState resolvedVein;                                                               // the lazily resolved default

    public MagiculeVeinProcessor(TagKey<Block> targets, Optional<BlockState> vein, double baseChance, double peakChance, int rise, double pull) {
        this.targets = targets;
        this.vein = vein;
        this.baseChance = baseChance;
        this.peakChance = peakChance;
        this.rise = rise;
        this.pull = pull;
    }

    /** The contract defaults: veinable masonry -> nusmp:magicule_vein, 1% at the base rim rising to 12% on the axis of the peak. */
    public MagiculeVeinProcessor() {
        this(GenRegistry.VEINABLE, Optional.empty(), DEFAULT_BASE, DEFAULT_PEAK, DEFAULT_RISE, DEFAULT_PULL);
    }

    @Override
    protected StructureProcessorType<?> getType() { return GenRegistry.MAGICULE_VEINS.get(); }

    @Override
    public StructureTemplate.StructureBlockInfo processBlock(LevelReader level, BlockPos piecePos, BlockPos structureOrigin,
                                                             StructureTemplate.StructureBlockInfo original, StructureTemplate.StructureBlockInfo placed,
                                                             StructurePlaceSettings settings) {
        try {
            if (placed == null || structureOrigin == null) return placed;
            BlockState state = placed.state();
            if (state == null || !state.is(targets)) return placed;
            BlockState veinState = vein();
            if (veinState == null) return placed;                                                            // A1's block is not registered: no-op
            BlockPos pos = placed.pos();
            int dy = pos.getY() - structureOrigin.getY();                                                   // height above the base's bottom centre
            double dx = pos.getX() - structureOrigin.getX(), dz = pos.getZ() - structureOrigin.getZ();       // distance from the spire axis
            double chance = VeinMath.chance(dy, Math.sqrt(dx * dx + dz * dz), baseChance, peakChance, rise, pull);
            long seed = level instanceof WorldGenLevel gen ? gen.getSeed() : 0L;
            if (!VeinMath.roll(seed, pos.getX(), pos.getY(), pos.getZ(), chance)) return placed;
            return new StructureTemplate.StructureBlockInfo(pos, BiomePaletteProcessor.carryProperties(state, veinState), null);
        } catch (RuntimeException e) {
            return placed;
        }
    }

    /** The configured vein state, else {@code nusmp:magicule_vein}'s default state looked up once; null while that block is unregistered. */
    private BlockState vein() {
        if (vein.isPresent()) return vein.get();
        BlockState resolved = resolvedVein;
        if (resolved == null) {
            Optional<Holder.Reference<Block>> holder = BuiltInRegistries.BLOCK.getHolder(DEFAULT_VEIN);
            resolved = holder.map(h -> h.value().defaultBlockState()).orElse(null);
            resolvedVein = resolved;
        }
        return resolved;
    }
}
