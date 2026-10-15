package com.newuniverse.nusmp.labyrinth.trap;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Why: the carved stone that states the riddle of a puzzle room. A plain {@link Block} that implements {@link EntityBlock} (the pattern of
 * the Peak Altar) rather than a {@code BaseEntityBlock}: the base class forces an invisible render shape, and this block is a normal
 * baked cube whose four coloured studs are the only clue a player can see without clicking. It has no ticker (the puzzle is entirely
 * event driven by plate clicks) and no menu. A right click calls {@link RiddleStoneBlockEntity#read}, which prints the sequence to be
 * pressed in colour; the state lives in the block entity, the rules in {@link PuzzleState}.
 */
public final class RiddleStoneBlock extends Block implements EntityBlock {
    public RiddleStoneBlock(BlockBehaviour.Properties props) { super(props); }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new RiddleStoneBlockEntity(pos, state); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof RiddleStoneBlockEntity be) be.read(sp);
        return InteractionResult.CONSUME;
    }
}
