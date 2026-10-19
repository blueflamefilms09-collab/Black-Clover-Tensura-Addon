package com.newuniverse.nusmp.labyrinth.mega.rift;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Why: the Dimensional Rift is one block whose whole look is drawn by its block entity renderer (a vertical tear in the air with arcs and a window onto another place), so the block itself is
 * invisible ({@link RenderShape#INVISIBLE}), has no collision (a player walks into the tear), cannot be mined or pushed (strength -1, like every block of the dungeon, so a pickaxe is no way to
 * "steal" a rift and a piston cannot move it), and gives light. The block entity is what knows what kind of rift it is, where it leads and how long it has left; it ticks on both sides (the
 * client for the animation and the aura, the server for the lifetime, the destination and the players who step in).
 */
public final class DimensionalRiftBlock extends BaseEntityBlock {
    public static final MapCodec<DimensionalRiftBlock> CODEC = simpleCodec(DimensionalRiftBlock::new);
    private static final VoxelShape OUTLINE = net.minecraft.world.level.block.Block.box(2, 0, 2, 14, 16, 14);

    public DimensionalRiftBlock(BlockBehaviour.Properties props) { super(props); }

    @Override
    protected MapCodec<DimensionalRiftBlock> codec() { return CODEC; }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.INVISIBLE; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return OUTLINE; }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return Shapes.empty(); }

    /**
     * 1.94.0: a right click on the tear steps through it. Walking in always worked, but a delver who stands at the edge of the tear (a ledge, a doorway) or whose box misses the tear by a hair
     * had no other way; the outline of the block is what the crosshair meets, so it is a click on the shimmer.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp && level instanceof ServerLevel sl && !sp.isSpectator() && level.getBlockEntity(pos) instanceof DimensionalRiftBlockEntity be) RiftService.enter(sl, sp, be);
        return InteractionResult.CONSUME;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new DimensionalRiftBlockEntity(pos, state); }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, RiftRegistry.RIFT_BE.get(), DimensionalRiftBlockEntity::tick);
    }
}
