package com.newuniverse.nusmp.shadow;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Why: Shadow Travel asks the world three things many times in one cast (how bright is this cell, can a body stand here, is there a safe floor),
 * and all of them must answer the same way for the caster, the aimed spot and the search. This class is the one place that reads the level for
 * them; the decisions themselves are in {@link ShadowLight}. It never changes a block, so the Dungeon guard (which only refuses block changes) has
 * nothing to refuse and a move inside a guarded world is allowed.
 */
public final class ShadowWorld {
    private ShadowWorld() {}

    /** Is the standing spot with its feet at {@code feet} in shadow (feet cell and head cell both dark enough)? */
    public static boolean inShadow(ServerLevel level, BlockPos feet, int maxLight) {
        int darken = level.getSkyDarken();
        BlockPos head = feet.above();
        return ShadowLight.spotInShadow(level.getBrightness(LightLayer.BLOCK, feet), level.getBrightness(LightLayer.SKY, feet),
                level.getBrightness(LightLayer.BLOCK, head), level.getBrightness(LightLayer.SKY, head), darken, maxLight);
    }

    /**
     * Can a player stand with the feet at {@code feet}? Two free cells with no fluid, solid ground right below, nothing that burns or stings, inside the
     * world border and the height limits, in a chunk that is already loaded (a cast never loads chunks).
     */
    public static boolean standable(ServerLevel level, BlockPos feet) {
        if (!level.isInWorldBounds(feet) || !level.isInWorldBounds(feet.above()) || !level.getWorldBorder().isWithinBounds(feet)) return false;
        if (feet.getY() < level.getMinBuildHeight() + 2) return false;                          // the void is right under it
        if (!level.hasChunkAt(feet)) return false;
        BlockPos below = feet.below();
        BlockState ground = level.getBlockState(below);
        VoxelShape floor = ground.getCollisionShape(level, below);
        if (floor.isEmpty() || ground.is(Blocks.MAGMA_BLOCK) || ground.is(Blocks.CAMPFIRE) || ground.is(Blocks.SOUL_CAMPFIRE) || ground.is(Blocks.CACTUS)) return false;
        return cellFree(level, feet) && cellFree(level, feet.above());
    }

    private static boolean cellFree(ServerLevel level, BlockPos pos) {
        BlockState s = level.getBlockState(pos);
        if (!s.getCollisionShape(level, pos).isEmpty()) return false;
        if (!level.getFluidState(pos).isEmpty() || level.getFluidState(pos).is(FluidTags.LAVA)) return false;
        return !s.is(BlockTags.FIRE) && !s.is(Blocks.CACTUS) && !s.is(Blocks.POWDER_SNOW) && !s.is(Blocks.SWEET_BERRY_BUSH) && !s.is(Blocks.WITHER_ROSE)
                && !s.is(Blocks.COBWEB);
    }

    /** The height at which feet stand on the ground below {@code feet} (a slab or a snow layer is lower than a full block). */
    public static double standY(ServerLevel level, BlockPos feet) {
        BlockPos below = feet.below();
        VoxelShape floor = level.getBlockState(below).getCollisionShape(level, below);
        return floor.isEmpty() ? feet.getY() : below.getY() + floor.max(Direction.Axis.Y);
    }

    /** Counts the cells one cast has looked at, so the work of a cast is bounded however the terrain looks. */
    public static final class Budget {
        private int left;

        public Budget(int cap) { this.left = Math.max(0, cap); }

        /** Spends one check; false once the budget is gone. */
        public boolean take() {
            if (left <= 0) return false;
            left--;
            return true;
        }

        public int left() { return left; }
    }

    /**
     * A standing spot in shadow near the guessed cell: the cell itself and the ones a step below and above it (so an aimed wall or ceiling face finds the
     * floor). Each cell looked at costs one check of the budget. Null if none.
     */
    public static BlockPos findShadowSpot(ServerLevel level, BlockPos guess, int maxLight, Budget budget) {
        int[] dys = {0, -1, -2, -3, 1, 2};
        for (int dy : dys) {
            if (!budget.take()) return null;
            BlockPos feet = guess.offset(0, dy, 0);
            if (standable(level, feet) && inShadow(level, feet, maxLight)) return feet;
        }
        return null;
    }
}
