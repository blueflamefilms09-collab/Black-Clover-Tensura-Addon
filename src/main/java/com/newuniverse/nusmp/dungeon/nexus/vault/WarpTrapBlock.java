package com.newuniverse.nusmp.dungeon.nexus.vault;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * The Warp Trap (docs/living_dungeon.md, "Spatial Warping"): a floor block that looks like the rock round it (stone, or deepslate where the
 * floor is deepslate; the rune on its top is barely there). Whoever walks onto it is thrown 8 to 24 blocks away on the same floor,
 * dizzy for a few seconds ({@link WarpTrap}). It never harms anyone, and an attuned Band of the Spatial Anchor ignores it.
 */
public class WarpTrapBlock extends Block {
    /** Looks like deepslate instead of stone. */
    public static final BooleanProperty DEEP = BooleanProperty.create("deep");

    public WarpTrapBlock(BlockBehaviour.Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(DEEP, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> b) { b.add(DEEP); }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (!level.isClientSide && level instanceof ServerLevel sl && entity instanceof ServerPlayer p) WarpTrap.trigger(sl, p, pos);
        super.stepOn(level, pos, state, entity);
    }
}
