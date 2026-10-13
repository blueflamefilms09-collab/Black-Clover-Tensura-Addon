package com.newuniverse.nusmp.dungeon.sentient;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;

/**
 * Reading the world for places to be born into: a standing place (air at the spot and above, a sturdy floor under it), a swimming place
 * (water at the spot, water or air above), and the wall beside it. Every read goes through a chunk that is already loaded: a place whose
 * chunk is not is simply not there, and nothing here ever loads a chunk.
 */
final class Spots {
    private Spots() {}

    /** The four horizontal directions, in the order a wall is looked for. */
    private static final Direction[] SIDES = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    /** The block state at a position, or null if its chunk is not loaded (or it is outside the world). */
    static BlockState stateAt(ServerLevel level, int x, int y, int z) {
        if (y < level.getMinBuildHeight() || y >= level.getMaxBuildHeight()) return null;
        LevelChunk c = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
        return c == null ? null : c.getBlockState(new BlockPos(x, y, z));
    }

    /** Is the chunk holding this column loaded? */
    static boolean loaded(ServerLevel level, int x, int z) { return level.getChunkSource().getChunkNow(x >> 4, z >> 4) != null; }

    /** A standing place: air here and above, a floor under it that can be stood on. */
    static boolean standable(ServerLevel level, int x, int y, int z) {
        BlockState at = stateAt(level, x, y, z), above = stateAt(level, x, y + 1, z), below = stateAt(level, x, y - 1, z);
        if (at == null || above == null || below == null) return false;
        return at.isAir() && above.isAir() && below.isFaceSturdy(level, new BlockPos(x, y - 1, z), Direction.UP);
    }

    /** A swimming place: water here, and water or air above it. */
    static boolean swimmable(ServerLevel level, int x, int y, int z) {
        BlockState at = stateAt(level, x, y, z), above = stateAt(level, x, y + 1, z);
        if (at == null || above == null) return false;
        return at.getFluidState().is(FluidTags.WATER) && (above.getFluidState().is(FluidTags.WATER) || above.isAir());
    }

    /** The direction of a solid wall block beside the position (a block of the Dungeon, not one of its living fixtures), or null. The search starts at a side that depends on the place. */
    static Direction wallBeside(ServerLevel level, int x, int y, int z) {
        int start = Math.floorMod(x * 31 + y * 17 + z * 7, 4);
        for (int i = 0; i < 4; i++) {
            Direction d = SIDES[(start + i) & 3];
            int wx = x + d.getStepX(), wz = z + d.getStepZ();
            BlockState s = stateAt(level, wx, y, wz);
            if (s != null && !s.isAir() && !DungeonGuard.isFixture(s) && s.isFaceSturdy(level, new BlockPos(wx, y, wz), d.getOpposite())) return d;
        }
        return null;
    }
}
