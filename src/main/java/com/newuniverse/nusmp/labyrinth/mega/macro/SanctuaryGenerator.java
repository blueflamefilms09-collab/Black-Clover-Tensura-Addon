package com.newuniverse.nusmp.labyrinth.mega.macro;

import com.mojang.datafixers.util.Pair;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.newuniverse.nusmp.labyrinth.mega.HubLayout;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.LevelHeightAccessor;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.NoiseColumn;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.BiomeManager;
import net.minecraft.world.level.biome.BiomeSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.chunk.ChunkGeneratorStructureState;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.LegacyRandomSource;
import net.minecraft.world.level.levelgen.RandomState;
import net.minecraft.world.level.levelgen.RandomSupport;
import net.minecraft.world.level.levelgen.WorldgenRandom;
import net.minecraft.world.level.levelgen.blending.Blender;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import org.slf4j.Logger;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

/**
 * Why: this is the "custom {@code ChunkGenerator} utilizing 3D noise" of the owner's brief, for Floor 0, the Sanctuary Hub ({@code nusmp:sanctuary}). It is
 * deliberately THIN: everything about what the Hub looks like is decided by the pure {@link HubModel} (so it is unit tested and previewed without a game); this
 * class only (1) is a valid 1.21.1 {@link ChunkGenerator} with a codec the dimension JSON can name, (2) asks the model for every column of a chunk and places the
 * blocks, keeping the chunk's heightmaps right so vegetation and mobs find the ground, and (3) refuses to produce structures. Why each override:
 * {@code applyCarvers} and {@code buildSurface} do nothing because the surface, the strata and the bedrock are all part of {@code fillFromNoise}; {@code createStructures},
 * {@code createReferences} and {@code findNearestMapStructure} do nothing because the owner wants ABSOLUTELY ZERO structures from any other mod (a vanilla noise
 * generator asks every structure set of the registry, and mods add theirs to that registry; this generator never asks, which is the second of the three layers
 * that keep the Hub clean, after the cloned biome that is in no structure tag and before the hub's self-test that loads chunks and checks); {@code applyBiomeDecoration} is
 * the inherited one on purpose, so the biome's own vegetation features (grass, flowers, trees) run on the plains; {@code spawnOriginalMobs} lets the biome's
 * passive animals exist at the first visit, as in every vanilla generator.
 *
 * <p>The world seed is derived from the {@link RandomState} the game hands every generator ({@code getOrCreateRandomFactory("nusmp:sanctuary")}), which is a
 * function of the world seed alone, so every chunk of a world agrees on the Hub whatever order and on whichever thread it is generated, and the model is
 * cached per seed. SAFETY NET: a bug in the landmark code must never stop chunk generation (a chunk that never finishes freezes the player who is in it), so
 * each chunk's fill is wrapped; on any throwable ONE warning is logged and that chunk is filled with plain plains instead.
 */
public final class SanctuaryGenerator extends ChunkGenerator {
    /** The one field of the codec is the biome source; the dimension JSON is {@code {"type": "nusmp:sanctuary", "biome_source": {...}}}. */
    public static final MapCodec<SanctuaryGenerator> CODEC = RecordCodecBuilder.mapCodec(inst -> inst.group(
            BiomeSource.CODEC.fieldOf("biome_source").forGetter(ChunkGenerator::getBiomeSource)).apply(inst, inst.stable(SanctuaryGenerator::new)));

    private static final Logger LOG = LogUtils.getLogger();
    private static final ResourceLocation SEED_KEY = ResourceLocation.fromNamespaceAndPath("nusmp", "sanctuary");
    private static final AtomicBoolean WARNED = new AtomicBoolean();

    public SanctuaryGenerator(BiomeSource biomeSource) {
        super(biomeSource);
    }

    @Override
    protected MapCodec<? extends ChunkGenerator> codec() { return CODEC; }

    // ---- the model of this world

    /** The seed of the Hub of this world, derived from the generator's random state (a function of the world seed only). */
    public static long hubSeed(RandomState random) { return random.getOrCreateRandomFactory(SEED_KEY).at(0, 0, 0).nextLong(); }

    static HubModel modelOf(RandomState random) { return HubModel.of(hubSeed(random), MacroConfig.params()); }

    private static void warn(Throwable t) {
        if (WARNED.compareAndSet(false, true)) {
            StackTraceElement top = t.getStackTrace().length > 0 ? t.getStackTrace()[0] : null;
            LOG.warn("[nusmp] The Sanctuary generator failed ({}: {} at {}); the affected chunks are drawn as plain plains. This is logged once.", t.getClass().getSimpleName(), t.getMessage(), top);
        }
    }

    // ---- the chunk

    @Override
    public CompletableFuture<ChunkAccess> fillFromNoise(Blender blender, RandomState randomState, StructureManager structureManager, ChunkAccess chunk) {
        try {
            fill(chunk, modelOf(randomState));
        } catch (Throwable t) {
            warn(t);
            plainFill(chunk);
        }
        return CompletableFuture.completedFuture(chunk);
    }

    /** Places the whole chunk from the model. Heightmaps are kept the way the vanilla generators keep them: vegetation and spawning read the _WG ones. */
    private static void fill(ChunkAccess chunk, HubModel model) {
        ChunkPos cp = chunk.getPos();
        int x0 = cp.getMinBlockX(), z0 = cp.getMinBlockZ();
        int top = Math.min(chunk.getMaxBuildHeight() - 1, HubModel.MAX_Y);
        BlockKind[] col = new BlockKind[HubModel.HEIGHT];
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int lz = 0; lz < 16; lz++) {
            for (int lx = 0; lx < 16; lx++) {
                int x = x0 + lx, z = z0 + lz;
                model.fillColumn(x, z, col);
                for (int y = Math.max(chunk.getMinBuildHeight(), HubModel.MIN_Y); y <= top; y++) {
                    BlockKind k = col[y - HubModel.MIN_Y];
                    if (k == BlockKind.AIR) continue;
                    BlockState state = MacroBlocks.stateAt(model, k, x, y, z);
                    if (state.isAir()) continue;
                    chunk.setBlockState(pos.set(x, y, z), state, false);
                    oceanFloor.update(lx, y, lz, state);
                    surface.update(lx, y, lz, state);
                }
            }
        }
    }

    /** The safety net: grass over dirt over stone at the plains' height, nothing else, written over whatever a failed fill left behind. */
    private static void plainFill(ChunkAccess chunk) {
        ChunkPos cp = chunk.getPos();
        int x0 = cp.getMinBlockX(), z0 = cp.getMinBlockZ();
        BlockState air = Blocks.AIR.defaultBlockState(), grass = Blocks.GRASS_BLOCK.defaultBlockState(), dirt = Blocks.DIRT.defaultBlockState(), stone = Blocks.STONE.defaultBlockState();
        BlockState deepslate = Blocks.DEEPSLATE.defaultBlockState(), bedrock = Blocks.BEDROCK.defaultBlockState();
        Heightmap oceanFloor = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.OCEAN_FLOOR_WG);
        Heightmap surface = chunk.getOrCreateHeightmapUnprimed(Heightmap.Types.WORLD_SURFACE_WG);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int h = HubLayout.SURFACE_Y;
        for (int lz = 0; lz < 16; lz++)
            for (int lx = 0; lx < 16; lx++)
                for (int y = chunk.getMinBuildHeight(); y < chunk.getMaxBuildHeight(); y++) {
                    BlockState s = y >= h ? air : y == h - 1 ? grass : y >= h - 4 ? dirt : y == chunk.getMinBuildHeight() ? bedrock : y < -8 ? deepslate : stone;
                    chunk.setBlockState(pos.set(x0 + lx, y, z0 + lz), s, false);
                    if (!s.isAir()) {
                        oceanFloor.update(lx, y, lz, s);
                        surface.update(lx, y, lz, s);
                    }
                }
    }

    @Override
    public void buildSurface(WorldGenRegion level, StructureManager structureManager, RandomState random, ChunkAccess chunk) {
        // the surface is part of fillFromNoise
    }

    @Override
    public void applyCarvers(WorldGenRegion level, long seed, RandomState random, BiomeManager biomeManager, StructureManager structureManager, ChunkAccess chunk, GenerationStep.Carving step) {
        // no carvers: the Chasm is part of the model, nothing else may cut the landmarks
    }

    @Override
    public void spawnOriginalMobs(WorldGenRegion level) {
        ChunkPos chunkpos = level.getCenter();
        Holder<Biome> biome = level.getBiome(chunkpos.getWorldPosition().atY(level.getMaxBuildHeight() - 1));
        WorldgenRandom random = new WorldgenRandom(new LegacyRandomSource(RandomSupport.generateUniqueSeed()));
        random.setDecorationSeed(level.getSeed(), chunkpos.getMinBlockX(), chunkpos.getMinBlockZ());
        NaturalSpawner.spawnMobsForChunkGeneration(level, biome, chunkpos, random);
    }

    // ---- no structures, whatever other mods add to the registry

    @Override
    public void createStructures(RegistryAccess registryAccess, ChunkGeneratorStructureState structureState, StructureManager structureManager, ChunkAccess chunk, StructureTemplateManager templateManager) {
        // absolutely zero structures
    }

    @Override
    public void createReferences(WorldGenLevel level, StructureManager structureManager, ChunkAccess chunk) {
        // there are no structure starts to refer to
    }

    @Nullable
    @Override
    public Pair<BlockPos, Holder<Structure>> findNearestMapStructure(ServerLevel level, HolderSet<Structure> structures, BlockPos pos, int searchRadius, boolean skipKnownStructures) {
        return null;
    }

    // ---- the numbers of the dimension

    @Override
    public int getGenDepth() { return HubModel.HEIGHT; }

    @Override
    public int getSeaLevel() { return 63; }

    @Override
    public int getMinY() { return HubModel.MIN_Y; }

    @Override
    public int getSpawnHeight(LevelHeightAccessor level) { return HubLayout.SURFACE_Y; }

    /** The height the given heightmap type would report for this column: one above the highest block the type counts as opaque (as {@code FlatLevelSource} does). */
    @Override
    public int getBaseHeight(int x, int z, Heightmap.Types type, LevelHeightAccessor level, RandomState random) {
        int min = level.getMinBuildHeight();
        try {
            BlockKind[] col = new BlockKind[HubModel.HEIGHT];
            modelOf(random).fillColumn(x, z, col);
            Predicate<BlockState> opaque = type.isOpaque();
            for (int y = Math.min(level.getMaxBuildHeight() - 1, HubModel.MAX_Y); y >= Math.max(min, HubModel.MIN_Y); y--)
                if (col[y - HubModel.MIN_Y] != BlockKind.AIR && opaque.test(MacroBlocks.state(col[y - HubModel.MIN_Y]))) return y + 1;
            return min;
        } catch (Throwable t) {
            warn(t);
            return HubLayout.SURFACE_Y;
        }
    }

    @Override
    public NoiseColumn getBaseColumn(int x, int z, LevelHeightAccessor level, RandomState random) {
        int min = level.getMinBuildHeight(), height = level.getHeight();
        BlockState[] states = new BlockState[height];
        BlockState air = Blocks.AIR.defaultBlockState();
        Arrays.fill(states, air);
        try {
            HubModel model = modelOf(random);
            BlockKind[] col = new BlockKind[HubModel.HEIGHT];
            model.fillColumn(x, z, col);
            for (int i = 0; i < height; i++) {
                int y = min + i;
                if (y >= HubModel.MIN_Y && y <= HubModel.MAX_Y) states[i] = MacroBlocks.state(col[y - HubModel.MIN_Y]);
            }
        } catch (Throwable t) {
            warn(t);
        }
        return new NoiseColumn(min, states);
    }

    @Override
    public void addDebugScreenInfo(List<String> info, RandomState random, BlockPos pos) {
        try {
            HubModel m = modelOf(random);
            info.add("Sanctuary Hub: seed " + m.seed() + ", plains height " + m.surfaceHeight(pos.getX(), pos.getZ()) + (MacroConfig.get(MacroConfig.ENABLED) ? "" : " (landmarks off)"));
        } catch (Throwable t) {
            warn(t);
        }
    }
}
