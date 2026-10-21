package com.newuniverse.nusmp.labyrinth.loot.backpack;

import com.newuniverse.nusmp.dungeon.DungeonGuard;
import com.newuniverse.nusmp.labyrinth.loot.LootConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Why: the set-down backpack is a small container block that is soft (a hand breaks it), looks like what it is (a 3D model per tier, turned to face the player who set it down), and
 * is never lost: the block state carries the tier (so the model and the size are the same fact), the block entity carries the items, and both ride back onto the item when the pack
 * is broken (the loot table copies the components) or picked up. Right-click opens it. Sneak and right-click with an empty hand picks it up WITH its contents, but only a pack a
 * player placed (the flag on the block entity), so a pack a structure generated is not a prop to carry off; the removal goes through the dungeon guard's own internal door because
 * taking back what you set down is not breaking the dungeon. An upgrade item used on it grows it in place, items kept. When a player sets one down, {@link PlacedHook} is told so the
 * dungeon guard's placed-block record includes it. Server decisions only; the client just swings.
 */
public class BackpackBlock extends Block implements EntityBlock {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final IntegerProperty TIER = IntegerProperty.create("tier", 0, BackpackTier.values().length - 1);

    private static final VoxelShape NORTH_SOUTH = Block.box(3.0, 0.0, 4.0, 13.0, 14.0, 12.0);
    private static final VoxelShape EAST_WEST = Block.box(4.0, 0.0, 3.0, 12.0, 14.0, 13.0);

    public BackpackBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(TIER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING, TIER); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        BackpackTier tier = BackpackItem.tierOf(ctx.getItemInHand());
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()).setValue(TIER, tier.ordinal());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rot) { return state.setValue(FACING, rot.rotate(state.getValue(FACING))); }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) { return state.rotate(mirror.getRotation(state.getValue(FACING))); }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        Direction d = state.getValue(FACING);
        return d == Direction.EAST || d == Direction.WEST ? EAST_WEST : NORTH_SOUTH;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new BackpackBlockEntity(pos, state); }

    @Override
    public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
        return BackpackItem.of(BackpackTier.of(state.getValue(TIER)));
    }

    // ---------------------------------------------------------------- placing
    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.isClientSide) return;
        if (level.getBlockEntity(pos) instanceof BackpackBlockEntity be) {
            if (placer instanceof Player player) {
                be.setPlacedByPlayer(true);
                PlacedHook.fire(level, pos, player);
            }
        }
    }

    // ---------------------------------------------------------------- using
    @Override
    protected ItemInteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (held.getItem() instanceof BackpackUpgradeItem upgrade) {
            BackpackTier current = BackpackTier.of(state.getValue(TIER));
            BackpackTier result = BackpackTier.upgraded(current, upgrade.tier());
            if (result == current) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;     // not an upgrade for this pack: nothing is spent, the pack just opens
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            if (level.getBlockEntity(pos) instanceof BackpackBlockEntity be) {
                level.setBlock(pos, state.setValue(TIER, result.ordinal()), Block.UPDATE_ALL);
                if (level.getBlockEntity(pos) instanceof BackpackBlockEntity grown) grown.resize(result);
                else be.resize(result);
                if (!player.getAbilities().instabuild) held.shrink(1);
                level.playSound(null, pos, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 0.6f, 1.4f);
                return ItemInteractionResult.CONSUME;
            }
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof BackpackBlockEntity be)) return InteractionResult.PASS;
        if (player.isShiftKeyDown() && player.getMainHandItem().isEmpty() && LootConfig.get(LootConfig.BACKPACK_PICKUP) && be.placedByPlayer()) {
            pickUp(level, pos, player, be);
            return InteractionResult.CONSUME;
        }
        if (player instanceof ServerPlayer sp && be.canOpen(sp)) {
            BackpackTier tier = BackpackTier.fitting(be.getContainerSize());
            sp.openMenu(be, buf -> {
                buf.writeVarInt(tier.ordinal());
                buf.writeVarInt(-1);
            });
        }
        return InteractionResult.CONSUME;
    }

    /** Takes a set-down backpack back into the player's hands, tier and contents with it. */
    static void pickUp(Level level, BlockPos pos, Player player, BackpackBlockEntity be) {
        ItemStack stack = new ItemStack(BackpackRegistry.ITEM.get());
        stack.applyComponents(be.collectComponents());
        Boolean removed = DungeonGuard.internalGet(() -> level.removeBlock(pos, false));
        if (removed == null || !removed) return;                       // nothing was taken, so nothing is given: no copy of the pack
        if (!player.getInventory().add(stack)) player.drop(stack, false);
        level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.BLOCKS, 0.8f, 1.0f);
    }
}
