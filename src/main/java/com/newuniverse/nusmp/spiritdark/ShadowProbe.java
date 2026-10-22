package com.newuniverse.nusmp.spiritdark;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Why: the world behind {@link ShadowStep.Probe}: answers the destination search from the server level without loading a chunk ({@code hasChunkAt}), reading block states through one reused
 * mutable position (nothing is allocated per cell). Created once per Shadow Step (one per player per nine seconds at most), never kept.
 */
final class ShadowProbe implements ShadowStep.Probe {
    private final ServerLevel level;
    private final Entity who;
    private final BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

    ShadowProbe(ServerLevel level, Entity who) {
        this.level = level;
        this.who = who;
    }

    private BlockState at(int x, int y, int z) { return level.getBlockState(pos.set(x, y, z)); }

    @Override public boolean loaded(int x, int y, int z) { return level.hasChunkAt(pos.set(x, y, z)); }

    @Override public boolean passable(int x, int y, int z) {
        BlockState s = at(x, y, z);
        return s.getCollisionShape(level, pos).isEmpty() && !s.getFluidState().is(FluidTags.LAVA);
    }

    @Override public boolean hazard(int x, int y, int z) {
        BlockState s = at(x, y, z);
        return s.is(BlockTags.FIRE) || s.is(BlockTags.CAMPFIRES) || s.is(Blocks.MAGMA_BLOCK) || s.is(Blocks.CACTUS) || s.is(Blocks.SWEET_BERRY_BUSH) || s.is(Blocks.WITHER_ROSE)
                || s.is(Blocks.POWDER_SNOW) || s.is(Blocks.COBWEB) || s.is(Blocks.LAVA) || s.getFluidState().is(FluidTags.LAVA);
    }

    @Override public boolean floor(int x, int y, int z) {
        BlockState s = at(x, y, z);
        return s.isFaceSturdy(level, pos, Direction.UP) && s.getFluidState().isEmpty();
    }

    @Override public int light(int x, int y, int z) { return level.getMaxLocalRawBrightness(pos.set(x, y, z)); }

    @Override public boolean lineClear(double x0, double y0, double z0, double x1, double y1, double z1) {
        HitResult r = level.clip(new ClipContext(new Vec3(x0, y0, z0), new Vec3(x1, y1, z1), ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, who));
        return r.getType() == HitResult.Type.MISS;
    }

    @Override public int minY() { return level.getMinBuildHeight(); }

    @Override public int maxY() { return level.getMaxBuildHeight(); }
}
