package com.newuniverse.nusmp.labyrinth.vault;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
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
 * Why: the Relic Pedestal is the anime vault reveal: one relic floating over a dark stone column, taken with a right click. It is a
 * {@link BaseEntityBlock} because the held stack and the loot table must live in a block entity (the template places the block with
 * {@code {LootTable:"nusmp:chests/labyrinth_pedestal"}} exactly like a chest, and the client needs the stack to draw it). The render shape is
 * {@code MODEL}, not the BaseEntityBlock default {@code INVISIBLE}: the stone is the baked JSON model, only the floating relic and its glow are
 * drawn by the block entity renderer. There is no menu, no container and no hopper access: a pedestal is a one-time pick, not storage.
 * The ticker exists only on the client (a float phase for the bob and the turn); the server never ticks a pedestal.
 * <p>
 * Strength 50 / 1200 (obsidian-like): inside the dungeon the guard makes it unbreakable anyway; outside it is a hard decorative block that
 * drops itself. When it is removed with a relic still floating, the relic drops: the ledger already counts that copy as alive, so losing it
 * in a broken block would leave a relic no one can ever find again.
 */
public final class RelicPedestalBlock extends BaseEntityBlock {
    public static final MapCodec<RelicPedestalBlock> CODEC = simpleCodec(RelicPedestalBlock::new);

    /** The model's three parts: a 16x2 base, a 10-wide column to y 11, a 12-wide top plate to y 13. */
    private static final VoxelShape SHAPE = Shapes.or(Block.box(0, 0, 0, 16, 2, 16), Block.box(3, 2, 3, 13, 11, 13), Block.box(2, 11, 2, 14, 13, 14));

    public RelicPedestalBlock(BlockBehaviour.Properties props) { super(props); }

    @Override
    protected MapCodec<RelicPedestalBlock> codec() { return CODEC; }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return SHAPE; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new RelicPedestalBlockEntity(pos, state); }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? createTickerHelper(type, VaultRegistry.PEDESTAL_BE.get(), RelicPedestalBlockEntity::clientTick) : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp && level.getBlockEntity(pos) instanceof RelicPedestalBlockEntity be) be.take(sp);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof RelicPedestalBlockEntity be) {
            ItemStack held = be.getHeld();
            if (!held.isEmpty()) Block.popResource(level, pos, held.copy());                    // never lose an issued relic with the block
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
