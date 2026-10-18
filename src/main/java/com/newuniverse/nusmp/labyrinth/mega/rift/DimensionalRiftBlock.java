package com.newuniverse.nusmp.labyrinth.mega.rift;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
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

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new DimensionalRiftBlockEntity(pos, state); }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, RiftRegistry.RIFT_BE.get(), DimensionalRiftBlockEntity::tick);
    }
}
