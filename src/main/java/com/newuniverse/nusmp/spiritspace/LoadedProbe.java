package com.newuniverse.nusmp.spiritspace;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Why: {@link SpaceLanding} asks a probe about single blocks; this is the probe of a real level. It reads blocks of chunks that are ALREADY loaded and never loads or generates one
 * ({@code getChunkNow}); a position in an unloaded chunk answers "not safe" in every question, so a portal can never be aimed into a chunk nobody has loaded. It keeps the last chunk it looked at, so a
 * landing search of a few hundred cells touches the chunk table a handful of times. Server thread only; one instance per search.
 */
final class LoadedProbe implements SpaceLanding.Probe {
    private final ServerLevel level;
    private final BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
    private LevelChunk chunk;
    private int chunkX = Integer.MIN_VALUE, chunkZ = Integer.MIN_VALUE;

    LoadedProbe(ServerLevel level) { this.level = level; }

    /** The block at a cell, or null when its chunk is not loaded or the cell is outside the build height. */
    private BlockState state(int x, int y, int z) {
        if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) return null;
        int cx = x >> 4, cz = z >> 4;
        if (cx != chunkX || cz != chunkZ) {
            chunk = level.getChunkSource().getChunkNow(cx, cz);
            chunkX = cx;
            chunkZ = cz;
        }
        return chunk == null ? null : chunk.getBlockState(at.set(x, y, z));
    }

    @Override
    public boolean solid(int x, int y, int z) {
        BlockState s = state(x, y, z);
        return s != null && s.isFaceSturdy(chunk, at.set(x, y, z), Direction.UP);
    }

    @Override
    public boolean passable(int x, int y, int z) {
        BlockState s = state(x, y, z);
        return s != null && s.getCollisionShape(chunk, at.set(x, y, z)).isEmpty() && s.getFluidState().isEmpty();
    }

    @Override
    public boolean hazard(int x, int y, int z) {
        BlockState s = state(x, y, z);
        if (s == null) return true;
        return s.getFluidState().is(FluidTags.LAVA) || s.is(BlockTags.FIRE) || s.is(BlockTags.CAMPFIRES) || s.is(Blocks.CACTUS) || s.is(Blocks.MAGMA_BLOCK)
                || s.is(Blocks.SWEET_BERRY_BUSH) || s.is(Blocks.WITHER_ROSE) || s.is(Blocks.POWDER_SNOW) || s.is(Blocks.COBWEB);
    }

    @Override public int minY() { return level.getMinBuildHeight(); }

    @Override public int maxY() { return level.getMaxBuildHeight(); }
}
