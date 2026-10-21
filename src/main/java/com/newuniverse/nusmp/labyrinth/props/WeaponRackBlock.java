package com.newuniverse.nusmp.labyrinth.props;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Why: the dungeon's weapons and armour are shown on racks, not on armour stands. An armour stand is an entity: it is created, ticked, synced to every client in range and saved with its chunk, and a
 * hall with forty of them is paid for in lag. A rack is a block with a block entity and a renderer: nothing ticks, nothing is an entity, the renderer draws five items only when the player is near.
 * The block is a free-standing frame (two posts, a top rail to hang weapons from, a lower plank for a helm and a breastplate), a little taller than a block (the model and the outline reach 1.625
 * blocks; the collision stops at 14 pixels so it can be walked round and jumped over), facing the way the stand it replaces faced. Hands: see {@link RackMode}. In the dungeon the rack is a block like
 * every other and is held by the Dungeon Guard (nothing of the generated dungeon is breakable); outside it, it breaks in two seconds with an axe and gives back the rack and what was on it
 * (a DECOR rack gives back only itself: its contents are scenery).
 */
public final class WeaponRackBlock extends BaseEntityBlock {
    public static final MapCodec<WeaponRackBlock> CODEC = simpleCodec(WeaponRackBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;

    /** Outline: the whole frame, 26 pixels tall (1.625 blocks). Collision: the lower 14 pixels. Both thin front to back (6 pixels, centred). */
    private static final VoxelShape OUTLINE_NS = Block.box(0, 0, 5, 16, 26, 11), OUTLINE_EW = Block.box(5, 0, 0, 11, 26, 16);
    private static final VoxelShape COLLISION_NS = Block.box(0, 0, 5, 16, 14, 11), COLLISION_EW = Block.box(5, 0, 0, 11, 14, 16);

    public WeaponRackBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.SOUTH));
    }

    @Override
    protected MapCodec<WeaponRackBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) { return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()); }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) { return state.setValue(FACING, rot.rotate(state.getValue(FACING))); }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return northSouth(state) ? OUTLINE_NS : OUTLINE_EW;
    }

    /** Does the rack face north or south (then it is wide along x)? */
    private static boolean northSouth(BlockState state) {
        Direction d = state.getValue(FACING);
        return d == Direction.NORTH || d == Direction.SOUTH;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return northSouth(state) ? COLLISION_NS : COLLISION_EW;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new WeaponRackBlockEntity(pos, state); }

    /** Hang the held item on the first free hook (an OPEN rack only). */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof WeaponRackBlockEntity be) || !be.mode().canPut() || held.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (be.firstEmpty() < 0) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        be.put(held);
        if (!player.getAbilities().instabuild) held.shrink(1);
        return ItemInteractionResult.CONSUME;
    }

    /** An empty hand takes the last item off (an OPEN or LOOT rack); a DECOR rack says it is only for show. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof WeaponRackBlockEntity be)) return InteractionResult.PASS;
        if (!be.mode().canTake()) {
            if (!level.isClientSide && be.count() > 0) player.displayClientMessage(Component.translatableWithFallback("nusmp.weapon_rack.decor", "These are only for show."), true);
            return InteractionResult.PASS;
        }
        if (be.count() == 0) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        ItemStack taken = be.take();
        if (!taken.isEmpty() && !player.getInventory().add(taken)) player.drop(taken, false);
        return InteractionResult.CONSUME;
    }

    /** What was on the rack falls out when the rack is broken or replaced, unless it was scenery. */
    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!state.is(newState.getBlock()) && !level.isClientSide && level.getBlockEntity(pos) instanceof WeaponRackBlockEntity be && be.mode() != RackMode.DECOR) {
            for (int i = 0; i < RackLayout.SLOTS; i++) {
                ItemStack s = be.get(i);
                if (!s.isEmpty()) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, s.copy());
            }
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
