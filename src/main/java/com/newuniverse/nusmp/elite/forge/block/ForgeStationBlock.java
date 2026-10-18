package com.newuniverse.nusmp.elite.forge.block;

import com.mojang.serialization.MapCodec;
import com.newuniverse.nusmp.elite.forge.ForgeConfig;
import com.newuniverse.nusmp.elite.forge.item.UpgradeKitItem;
import com.newuniverse.nusmp.elite.forge.net.ForgeServer;
import com.newuniverse.nusmp.elite.forge.rules.StationTier;
import com.newuniverse.nusmp.elite.forge.session.ForgeSessions;
import com.newuniverse.nusmp.elite.forge.station.StationUpgrade;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Why: the forge station, one block class for the four tiers (Basic, Magisteel, Mythril, Dragonforge); the tier is a final field because each tier is its own
 * registered block (so a loot table, a texture set and a world full of stations stay simple, and an upgrade is a block change). Right click opens the station
 * screen on the client through {@link ForgeServer#open}; right click holding an upgrade kit of the next tier upgrades the station in place
 * ({@link StationUpgrade}). The {@code lit} property glows the station while somebody forges at it: it is set when a session starts, cleared when it ends, and a
 * scheduled tick clears it again if the session was lost (a crash, a logout), so a station cannot stay lit forever. A {@link BaseEntityBlock} because the station
 * counts what has been forged at it. Pickaxe-mineable (the tag file lists the four ids), hard like an anvil.
 */
public class ForgeStationBlock extends BaseEntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 13, 16);
    /** Ticks after which a lit station checks that somebody is still forging at it. */
    public static final int LIT_RECHECK = 20 * 60 * 2;

    private final StationTier tier;

    public static final MapCodec<ForgeStationBlock> CODEC = simpleCodec(p -> new ForgeStationBlock(StationTier.BASIC, p));

    public ForgeStationBlock(StationTier tier, BlockBehaviour.Properties props) {
        super(props);
        this.tier = tier;
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    public StationTier tier() { return tier; }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(LIT); }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) { return defaultBlockState().setValue(LIT, false); }

    @Override
    protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) { return SHAPE; }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ForgeStationBlockEntity(pos, state); }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.getItem() instanceof UpgradeKitItem) {
            if (level.isClientSide) return ItemInteractionResult.SUCCESS;
            if (player instanceof ServerPlayer sp) StationUpgrade.tryUpgrade(sp, (ServerLevel) level, pos, stack);
            return ItemInteractionResult.CONSUME;
        }
        return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        if (!ForgeConfig.enabled()) {
            sp.displayClientMessage(Component.translatable("nusmp.eforge.msg.disabled"), true);
            return InteractionResult.CONSUME;
        }
        ForgeServer.open(sp, pos);
        return InteractionResult.CONSUME;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(LIT)) {
            if (ForgeSessions.activeAt(level, pos)) level.scheduleTick(pos, this, LIT_RECHECK);
            else level.setBlock(pos, state.setValue(LIT, false), Block.UPDATE_ALL);
        }
    }

    /** Lights or darkens the station at a position (server). */
    public static void setLit(ServerLevel level, BlockPos pos, boolean lit) {
        BlockState s = level.getBlockState(pos);
        if (!(s.getBlock() instanceof ForgeStationBlock b) || s.getValue(LIT) == lit) return;
        level.setBlock(pos, s.setValue(LIT, lit), Block.UPDATE_ALL);
        if (lit) level.scheduleTick(pos, b, LIT_RECHECK);
    }

    /** The tier of the station at a position, or null when there is none. */
    public static StationTier tierAt(Level level, BlockPos pos) {
        return level.getBlockState(pos).getBlock() instanceof ForgeStationBlock b ? b.tier : null;
    }

    /** True for the registry's four blocks (for the creative tab and the self-test). */
    public static boolean isStation(Block b) { return b instanceof ForgeStationBlock; }
}
