package com.newuniverse.nusmp.labyrinth.seal;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Why: the Peak Altar is the block that CALLS the floor boss, and it is an {@link EntityBlock} because the call needs state that lives
 * in the world: the roster written into the template, which boss it woke, whether that boss is dead. It implements the interface on a
 * plain {@link Block} rather than extending {@code BaseEntityBlock} because that base class forces {@code RenderShape.INVISIBLE} (it expects
 * a block-entity renderer) and demands a codec; the altar is an ordinary JSON model (a stepped plinth) with a ticking block entity behind
 * it. The ticker is handed out on the server only and only for our own type, so the client never runs roster logic and a mismatched
 * block-entity type (a datapack swapping the block under a saved altar) gets no ticker instead of a ClassCastException. Unbreakable and
 * immovable like every block of the Spire; a click tells the player which of its three states it is in.
 */
public class PeakAltarBlock extends Block implements EntityBlock {
    /** A stepped plinth: a full base, a narrower middle, a crown on top. */
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 5, 16), Block.box(2, 5, 2, 14, 10, 14), Block.box(4, 10, 4, 12, 16, 12));

    public PeakAltarBlock(BlockBehaviour.Properties props) { super(props); }

    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new PeakAltarBlockEntity(pos, state); }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != SealRegistry.PEAK_ALTAR_BE.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<PeakAltarBlockEntity>) PeakAltarBlockEntity::serverTick;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof PeakAltarBlockEntity altar) {
            player.displayClientMessage(Component.translatable("message.nusmp.peak_altar." + altar.state().name().toLowerCase()).withStyle(ChatFormatting.DARK_PURPLE), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
