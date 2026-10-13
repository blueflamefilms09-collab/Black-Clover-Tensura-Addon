package com.newuniverse.nusmp.dungeon.nexus.vault;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.material.FluidState;

/**
 * The Dungeon as the vault planner sees it ({@link VaultTerrain}): one block at a time, classified. A chunk that is not loaded answers
 * UNKNOWN, unless this adapter was told it may load chunks (the admin command and the Descent Gate do). It keeps the chunk it read last, so
 * the long runs of probes along one chunk cost one lookup.
 */
final class WorldTerrain implements VaultTerrain {
    private final ServerLevel level;
    private final boolean load;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
    private int chunkX = Integer.MIN_VALUE, chunkZ;
    private LevelChunk chunk;

    WorldTerrain(ServerLevel level, boolean load) {
        this.level = level;
        this.load = load;
    }

    @Override
    public int at(int x, int y, int z) {
        if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) return OTHER;               // above and below the world is bedrock
        int cx = x >> 4, cz = z >> 4;
        if (chunk == null || cx != chunkX || cz != chunkZ) {
            LevelChunk c = load ? level.getChunk(cx, cz) : level.getChunkSource().getChunkNow(cx, cz);
            if (c == null) return UNKNOWN;
            chunk = c;
            chunkX = cx;
            chunkZ = cz;
        }
        return classify(chunk.getBlockState(pos.set(x, y, z)));
    }

    /** Which {@link VaultTerrain} code a block state is. Shared with the sink, so what the planner called rock is what the builder treats as rock. */
    static int classify(BlockState s) {
        if (s.isAir()) return AIR;
        FluidState f = s.getFluidState();
        boolean motion = s.blocksMotion();
        if (!f.isEmpty()) {
            if (f.is(FluidTags.LAVA)) return LAVA;
            if (f.is(FluidTags.WATER) && !motion) return WATER;                                          // water, kelp, bubble columns
        }
        if (s.is(Blocks.STONE)) return STONE;
        if (s.is(Blocks.DEEPSLATE)) return DEEPSLATE;
        return motion ? OTHER : AIR;                                                                      // plants, carpets and veils give way
    }
}
