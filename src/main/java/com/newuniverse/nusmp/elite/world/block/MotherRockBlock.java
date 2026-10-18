package com.newuniverse.nusmp.elite.world.block;

import com.mojang.serialization.MapCodec;
import com.newuniverse.nusmp.elite.world.MagiculeApi;
import com.newuniverse.nusmp.elite.world.calamity.CalamityManager;
import com.newuniverse.nusmp.elite.world.rules.CrystalGrowth;
import com.newuniverse.nusmp.particle.rukh.RukhParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Why: the last stage of the Magic Ore growth and the heart of a grove. A Mother Rock has a CHARGE (0..{@value CrystalGrowth#MAX_CHARGE}) that fills with the chunk's density, spends one charge to
 * seed a new Small Bud on a free face nearby ({@link CrystalLogic#motherTick}), raises the density of its chunk while it stands (the engine counts the rocks and their charge), and is the
 * altar of the ritual that calls a calamity back (a Calamity Core used on a charged rock). The chunk remembers its Mother Rocks by position so that counting them needs no scan.
 */
public class MotherRockBlock extends Block {
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, CrystalGrowth.MAX_CHARGE);
    public static final MapCodec<MotherRockBlock> CODEC = simpleCodec(MotherRockBlock::new);

    public MotherRockBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(CHARGE, 0));
    }

    @Override
    protected MapCodec<? extends Block> codec() { return CODEC; }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(CHARGE); }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        CrystalLogic.motherTick(this, state, level, pos, random);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!oldState.is(this)) MagiculeApi.addMother(level, pos);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!newState.is(this)) {
            MagiculeApi.removeMother(level, pos);
            if (level instanceof ServerLevel sl) MagiculeApi.bump(sl, pos, -0.06, 1);          // a broken Mother Rock thins the chunk for a while
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }

    /** The ritual: a Calamity Core used on a charged rock calls the calamity it holds. Anything else behaves as an ordinary block. */
    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (level.isClientSide() || !(player instanceof ServerPlayer sp) || !(level instanceof ServerLevel sl)) {
            return stack.getItem() instanceof com.newuniverse.nusmp.elite.world.item.CalamityCoreItem ? ItemInteractionResult.SUCCESS : ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (!(stack.getItem() instanceof com.newuniverse.nusmp.elite.world.item.CalamityCoreItem)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        return CalamityManager.ritual(sp, sl, pos, state, stack) ? ItemInteractionResult.SUCCESS : ItemInteractionResult.CONSUME;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        int charge = state.getValue(CHARGE);
        if (charge <= 0 || random.nextInt(8 - charge * 2) != 0) return;
        level.addParticle(RukhParticles.light(), pos.getX() + random.nextDouble(), pos.getY() + 1.0 + random.nextDouble() * 0.4, pos.getZ() + random.nextDouble(), 0.0, 0.03, 0.0);
    }
}
