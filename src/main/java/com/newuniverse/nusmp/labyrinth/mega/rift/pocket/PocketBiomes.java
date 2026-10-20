package com.newuniverse.nusmp.labyrinth.mega.rift.pocket;

import com.mojang.logging.LogUtils;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the unique biome of each exclusive tomb. A new DIMENSION per tomb cannot be made while the game runs (NeoForge 1.21 reads the dimension registry once, at start), and the Mega-Dungeon's dimension
 * is a flat world of one biome whose special effects already give it thick fog and no sky, so the pockets do what {@code /fillbiome} does instead: every pocket has a real biome in the datapack
 * ({@code data/nusmp/worldgen/biome/pocket_<id>.json}: the fog and sky and water colours, the ambient particles, the ambient loop, the mood and additions sounds and the music), and when a tomb is made the
 * biome of each chunk of its cell is set to it ({@link ChunkAccess#fillBiomesFromNoise}) and the chunks' biomes are sent to the players again. The client then does the rest by itself: the fog colour of the biome
 * is the colour of the tomb's air (the dimension's effects give the biome's fog straight through), the particles drift, the sounds play, the music changes. When the tomb is cleared the cell is set back to
 * {@code nusmp:mega_depths}. A biome the registry does not know (a datapack that dropped the file) is skipped and logged: the tomb keeps the dungeon's biome and loses only its colour.
 */
public final class PocketBiomes {
    private PocketBiomes() {}

    private static final Logger LOG = LogUtils.getLogger();
    public static final String DEFAULT_BIOME = "nusmp:mega_depths";

    /** The biome holder of an id, or null when the registry does not have it. */
    public static Holder<Biome> holder(ServerLevel level, String id) {
        ResourceLocation rl = id == null ? null : ResourceLocation.tryParse(id);
        if (rl == null) return null;
        return level.registryAccess().registryOrThrow(Registries.BIOME).getHolder(ResourceKey.create(Registries.BIOME, rl)).orElse(null);
    }

    /** Sets the biome of one chunk (loading it) and tells the players who see it. Returns false when the biome is unknown. */
    public static boolean applyChunk(ServerLevel level, int chunkX, int chunkZ, String biomeId) {
        Holder<Biome> biome = holder(level, biomeId);
        if (biome == null) {
            LOG.warn("[nusmp] The biome {} is not in the registry: the pocket keeps the dungeon's biome.", biomeId);
            return false;
        }
        LevelChunk chunk = level.getChunk(chunkX, chunkZ);
        chunk.fillBiomesFromNoise((x, y, z, sampler) -> biome, level.getChunkSource().randomState().sampler());
        chunk.setUnsaved(true);
        List<ChunkAccess> one = new ArrayList<>(1);
        one.add(chunk);
        level.getChunkSource().chunkMap.resendBiomesForChunks(one);
        return true;
    }

    /** The chunks of a slot's cell as {x, z} pairs, row by row (what the builder slices over the first ticks). */
    public static List<int[]> chunksOf(int slot) {
        int[] c = PocketCell.chunks(slot);
        List<int[]> out = new ArrayList<>();
        for (int z = c[1]; z <= c[3]; z++) for (int x = c[0]; x <= c[2]; x++) out.add(new int[]{x, z});
        return out;
    }
}
