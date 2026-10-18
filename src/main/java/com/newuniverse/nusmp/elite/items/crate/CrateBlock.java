package com.newuniverse.nusmp.elite.items.crate;

import com.newuniverse.nusmp.elite.items.EliteItemsConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Why: the crate you place in a hub or a dungeon and open with a key. ONE block class for the three tiers (the tier decides the key, the table and the glow). It has no block entity: the state is
 * {@code facing} (so it sits the way it was placed) and {@code open} (the lid is up for the opening animation, set by the opening and reset by a scheduled block tick, so a lid never stays up after a
 * crash or a reload). Using it with the matching key in the hand opens it ({@link CrateOpening}); with anything else it only says which key it wants. It breaks like a wooden chest and drops itself, so
 * a crate someone placed is not lost, and it is not a container (nothing can be stored in it), so it cannot be looted without the key.
 */
public class CrateBlock extends Block {
    public static final DirectionProperty FACING = HorizontalDirectionalBlock.FACING;
    public static final BooleanProperty OPEN = BlockStateProperties.OPEN;
    private static final VoxelShape SHAPE = Block.box(1, 0, 1, 15, 13, 15);

    private final CrateTier tier;

    public CrateBlock(CrateTier tier) {
        super(Properties.of().mapColor(switch (tier) {
            case COMMON -> MapColor.WOOD;
            case RARE -> MapColor.COLOR_BLUE;
            case ELITE -> MapColor.COLOR_PURPLE;
        }).strength(3.0f, 12.0f).sound(SoundType.WOOD).noOcclusion().lightLevel(s -> s.getValue(OPEN) ? 12 : tier == CrateTier.ELITE ? 6 : tier == CrateTier.RARE ? 3 : 0));
        this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(OPEN, false));
    }

    public CrateTier tier() { return tier; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(FACING, OPEN); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, ctx.getHorizontalDirection().getOpposite()).setValue(OPEN, false);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return SHAPE; }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (!(stack.getItem() instanceof CrateKeyItem key) || key.tier() != tier) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide || !(player instanceof ServerPlayer p)) return ItemInteractionResult.SUCCESS;
        CrateOpening.open(p, (ServerLevel) level, pos, state, tier, stack);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide || !(player instanceof ServerPlayer p)) return InteractionResult.SUCCESS;
        p.displayClientMessage(Component.translatableWithFallback("message.nusmp.crate.locked", "This crate is locked. It needs a %s.",
                Component.translatableWithFallback("item.nusmp." + tier.keyId(), CrateKeyItem.cap(tier.id()) + " Crate Key")).withStyle(ChatFormatting.GRAY), true);
        level.playSound(null, pos, com.newuniverse.nusmp.elite.items.EliteSounds.CRATE_LOCKED.get(), net.minecraft.sounds.SoundSource.BLOCKS, 0.7f, 1.0f);
        return InteractionResult.CONSUME;
    }

    /** The lid comes down when the animation is over (a scheduled tick, so it survives a reload). */
    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(OPEN)) level.setBlock(pos, state.setValue(OPEN, false), 3);
    }

    /** How long the lid stays up. */
    static int animationTicks() { return EliteItemsConfig.get(EliteItemsConfig.CRATE_ANIMATION_TICKS); }
}
