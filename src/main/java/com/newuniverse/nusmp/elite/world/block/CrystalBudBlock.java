package com.newuniverse.nusmp.elite.world.block;

import com.newuniverse.nusmp.elite.world.MagiculeApi;
import com.newuniverse.nusmp.elite.world.rules.CrystalStage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Why: Small Bud, Medium Bud, Large Bud and Cluster of the Magic Ore growth are one class with a stage, built on vanilla's amethyst cluster (the same directional, waterloggable crystal that
 * sits on a face, with the same shapes by size) so they behave like the crystal players already know. What it adds: it takes random ticks and ripens to the next stage by the chunk's magicule
 * density ({@link CrystalLogic#ripen}), and it keeps the chunk's crystal score (the density bonus of a grove) with {@code onPlace} / {@code onRemove}.
 */
public class CrystalBudBlock extends AmethystClusterBlock {
    private final CrystalStage stage;

    public CrystalBudBlock(CrystalStage stage, float height, float aabbOffset, BlockBehaviour.Properties properties) {
        super(height, aabbOffset, properties);
        this.stage = stage;
    }

    public CrystalStage stage() { return stage; }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        CrystalLogic.ripen(this, state, level, pos, random);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this)) MagiculeApi.addScore(level, pos, stage.weight);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!newState.is(this)) MagiculeApi.addScore(level, pos, -stage.weight);
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
