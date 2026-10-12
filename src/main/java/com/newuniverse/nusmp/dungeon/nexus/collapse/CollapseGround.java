package com.newuniverse.nusmp.dungeon.nexus.collapse;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;

/** Two questions about the ground the collapse puts things on (Root Gates, mutated guards): is the chunk there, and can a person stand there? */
final class CollapseGround {
    private CollapseGround() {}

    /** Is the chunk of this column loaded? Never loads it. */
    static boolean loaded(ServerLevel level, int x, int z) { return level.getChunkSource().getChunkNow(x >> 4, z >> 4) != null; }

    /** Can feet stand in block (x, y, z): the block and the one above are air, no fluid there or underneath, and the floor is solid on top? */
    static boolean standable(ServerLevel level, int x, int y, int z) {
        if (y < level.getMinBuildHeight() + 1 || y > level.getMaxBuildHeight() - 3 || !loaded(level, x, z)) return false;
        BlockPos p = new BlockPos(x, y, z);
        return level.getBlockState(p).isAir() && level.getBlockState(p.above()).isAir() && level.getFluidState(p).isEmpty() && level.getFluidState(p.below()).isEmpty()
                && level.getBlockState(p.below()).isFaceSturdy(level, p.below(), Direction.UP);
    }

    /** The first standing place in this column from `up` blocks above y down to `down` below it (the nearest to y first on the way down), or null. */
    static int[] standingNear(ServerLevel level, int x, int y, int z, int up, int down) {
        for (int dy = up; dy >= -down; dy--) if (standable(level, x, y + dy, z)) return new int[]{x, y + dy, z};
        return null;
    }
}
