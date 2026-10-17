package com.newuniverse.nusmp.labyrinth.mega.guard;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
 * Why: the Guardian Altar is the block that CALLS the sector's boss and later answers for its Mana Core, and it is an {@link EntityBlock}
 * because that needs state that lives in the world (which boss it woke, whether that boss is dead, when a core was last forged). It
 * implements the interface on a plain {@link Block} rather than extending {@code BaseEntityBlock} for the reason the Peak Altar does:
 * the base class forces an invisible render shape and demands a codec, while the altar is an ordinary JSON model (a tall plinth with four
 * rune pillars) with a ticking block entity behind it. The ticker is handed out on the server only and only for our own block-entity type,
 * so the client never runs the machine and a mismatched type gets no ticker instead of a ClassCastException. Unbreakable and immovable
 * (see {@link GuardRegistry}); a click tells the player which of three states it is in, and on a spent altar with an empty hand it is
 * the way to re-forge a lost core.
 */
public class GuardianAltarBlock extends Block implements EntityBlock {
    /** A base slab, a plinth, a crown and four rune pillars at the corners: the model's silhouette. */
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 4, 16), Block.box(3, 4, 3, 13, 9, 13), Block.box(5, 9, 5, 11, 13, 11),
            Block.box(0, 4, 0, 3, 16, 3), Block.box(13, 4, 0, 16, 16, 3), Block.box(0, 4, 13, 3, 16, 16), Block.box(13, 4, 13, 16, 16, 16));

    public GuardianAltarBlock(BlockBehaviour.Properties props) { super(props); }

    @Override protected VoxelShape getShape(BlockState s, BlockGetter l, BlockPos p, CollisionContext c) { return SHAPE; }

    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new GuardianAltarBlockEntity(pos, state); }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide || type != GuardRegistry.ALTAR_BE.get()) return null;
        return (BlockEntityTicker<T>) (BlockEntityTicker<GuardianAltarBlockEntity>) GuardianAltarBlockEntity::serverTick;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level instanceof ServerLevel sl && player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof GuardianAltarBlockEntity altar) {
            altar.interact(sl, sp);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
