package com.newuniverse.nusmp.labyrinth.trap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Why: a plate must find "its" Riddle Stone without a stored link (a link would be a rotated offset again). The stone sits in the same
 * room, so the lookup scans the plate's neighbourhood and takes the nearest stone, which keeps two puzzle rooms that are built side by
 * side apart (each plate picks its own room's stone, the nearest). The box is the same size the stone uses to find its plates (12, 3 below,
 * 5 above), loaded chunks only so a click never forces a chunk to load.
 */
final class RiddleStones {
    private RiddleStones() {}

    static RiddleStoneBlockEntity nearest(ServerLevel level, BlockPos from) {
        BlockPos lo = from.offset(-12, -5, -12), hi = from.offset(12, 3, 12);
        if (!level.hasChunksAt(lo, hi)) return null;
        RiddleStoneBlockEntity best = null;
        double bestD = Double.MAX_VALUE;
        for (BlockPos p : BlockPos.betweenClosed(lo, hi)) {
            if (!level.getBlockState(p).is(TrapRegistry.RIDDLE_STONE.get())) continue;
            BlockEntity be = level.getBlockEntity(p);
            if (!(be instanceof RiddleStoneBlockEntity r)) continue;
            double d = p.distSqr(from);
            if (d < bestD) {
                bestD = d;
                best = r;
            }
        }
        return best;
    }
}
