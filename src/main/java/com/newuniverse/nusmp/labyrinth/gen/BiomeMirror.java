package com.newuniverse.nusmp.labyrinth.gen;

import com.newuniverse.nusmp.labyrinth.Labyrinth;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;

/**
 * Why: the Labyrinth dimension has ONE biome ({@code nusmp:labyrinth_sky}), so "the biome at this block" would give every spire the same
 * palette. The owner wants the masonry to adapt to the land the player came from, and the dimension's {@code coordinate_scale} is 1.0:
 * the same x/z in the Overworld is the surface above which the spire floats, so the palette is resolved from the OVERWORLD biome at the
 * block's x/z, sampled at sea level (y 64, the surface band of the 1.18+ multi-noise biomes). The sample goes through
 * {@code BiomeSource.getNoiseBiome(quartX, quartY, quartZ, RandomState.sampler())} and NOT {@code overworld.getBiome(pos)}: structure
 * processors run on worldgen worker threads, and {@code Level.getBiome} fetches the chunk, which from a worker thread goes through the
 * server thread's chunk futures: it would load (or generate) Overworld chunks just to read a biome, and if the server thread is itself
 * waiting on the Labyrinth chunk being generated the two wait on each other. The noise sampler is a pure function of the seed and the
 * climate density functions, loads nothing and is safe from any thread. Outside the Labyrinth (an Overworld gate, a {@code /place} in a
 * test world) the level's own biome is used, so the same processor serves both structures.
 */
public final class BiomeMirror {
    private BiomeMirror() {}

    /** The Overworld y the mirror samples at: sea level, the band where the surface biomes live. */
    public static final int SAMPLE_Y = 64;

    /** The biome that decides the palette at this block: the Overworld's surface biome at the same x/z inside the Labyrinth, else the level's own. Null only if everything failed. */
    public static Holder<Biome> biomeAt(LevelReader level, BlockPos pos) {
        if (level == null || pos == null) return null;
        try {
            if (level instanceof ServerLevelAccessor accessor) {                                           // WorldGenRegion during worldgen, ServerLevel from a command
                ServerLevel self = accessor.getLevel();
                if (self != null && Labyrinth.in(self)) {
                    Holder<Biome> mirrored = overworldSurfaceBiome(self.getServer(), pos.getX(), pos.getZ());
                    if (mirrored != null) return mirrored;
                }
            }
            return level.getBiome(pos);
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** The Overworld's noise biome at sea level over (x, z), from the biome source alone: no chunk is loaded, safe off the server thread. */
    public static Holder<Biome> overworldSurfaceBiome(MinecraftServer server, int x, int z) {
        if (server == null) return null;
        ServerLevel overworld = server.overworld();
        if (overworld == null) return null;
        ServerChunkCache chunks = overworld.getChunkSource();
        return chunks.getGenerator().getBiomeSource().getNoiseBiome(x >> 2, SAMPLE_Y >> 2, z >> 2, chunks.randomState().sampler());   // biome quarts: 4 blocks
    }

    /** The palette entry for the block's deciding biome, or null (no entry = default look). */
    public static LabyrinthPalette paletteAt(LevelReader level, BlockPos pos) {
        Holder<Biome> biome = biomeAt(level, pos);
        return biome == null ? null : palette(biome);
    }

    /** The data-map value of a biome holder; null for a biome without an entry or an unregistered (direct) holder. */
    public static LabyrinthPalette palette(Holder<Biome> biome) {
        if (biome == null) return null;
        try {
            return biome.getData(GenRegistry.PALETTE);                                                       // NeoForge: Holder implements IWithData
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** The biome id for messages ("minecraft:desert"), "?" for a direct holder. */
    public static String name(Holder<Biome> biome) {
        return biome == null ? "?" : biome.unwrapKey().map(k -> k.location().toString()).orElse("?");
    }
}
