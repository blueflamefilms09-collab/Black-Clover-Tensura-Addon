package com.newuniverse.nusmp.compat.create;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

/**
 * Why: shows the real world to {@link LaneFinder} as a grid of columns at a given floor row. A column is a LANE cell when the floor is plain solid ground and the cells above
 * it are air up to {@link TrapRules#MIN_HEADROOM}; it is WALL when the floor row and the two cells above it are solid blocks the machinery may replace (no block entity, nothing
 * unbreakable by nature). Answers are cached per column, because the finder asks about each column several times. Only loaded chunks are ever read ({@code getBlockState} on
 * an unloaded chunk would load it; the caller checks {@code hasChunksAt} first).
 */
public final class WorldGrid implements LaneFinder.Grid {
    private final ServerLevel level;
    private final int floorY;
    private final Map<Long, Boolean> open = new HashMap<>(), wall = new HashMap<>();

    public WorldGrid(ServerLevel level, int floorY) {
        this.level = level;
        this.floorY = floorY;
    }

    private static long key(int x, int z) { return ((long) x << 32) ^ (z & 0xFFFFFFFFL); }

    @Override
    public boolean open(int x, int z) {
        return open.computeIfAbsent(key(x, z), k -> {
            BlockPos floor = new BlockPos(x, floorY, z);
            BlockState fs = level.getBlockState(floor);
            if (!fs.isFaceSturdy(level, floor, Direction.UP) || fs.hasBlockEntity()) return false;
            for (int y = 1; y <= TrapRules.MIN_HEADROOM; y++) if (!level.getBlockState(floor.above(y)).isAir()) return false;
            return true;
        });
    }

    @Override
    public boolean wall(int x, int z) {
        return wall.computeIfAbsent(key(x, z), k -> {
            for (int y = 0; y <= 2; y++) {
                BlockPos p = new BlockPos(x, floorY + y, z);
                BlockState s = level.getBlockState(p);
                if (s.isAir() || !s.getFluidState().isEmpty() || !s.blocksMotion() || !TrapBuilder.replaceable(level, p, s)) return false;
            }
            return true;
        });
    }

    /** How many free cells stand above the floor row in this column, up to {@link TrapRules#MAX_HEADROOM}. */
    public int headroom(int x, int z) {
        int n = 0;
        while (n < TrapRules.MAX_HEADROOM && level.getBlockState(new BlockPos(x, floorY + 1 + n, z)).isAir()) n++;
        return n;
    }

    /** The lowest headroom over the three columns of every cell of a strip of the given site (the drawing's headroom must not exceed it). */
    public int stripHeadroom(LaneFinder.Site site) {
        int min = TrapRules.MAX_HEADROOM;
        for (int dz = 0; dz < site.length(); dz++) {
            for (int dx = -1; dx <= 1; dx++) {
                int[] xz = LaneFinder.toWorld(site.originX(), site.originZ(), site.forward(), dx, dz);
                min = Math.min(min, headroom(xz[0], xz[1]));
            }
        }
        return min;
    }
}
