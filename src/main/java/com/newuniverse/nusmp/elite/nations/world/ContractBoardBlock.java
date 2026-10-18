package com.newuniverse.nusmp.elite.nations.world;

import com.newuniverse.nusmp.elite.nations.net.NationNet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Why: the contract board block ("a contract board block/screen"): right click opens the nations screen on the CONTRACTS tab, showing the board of the player's own nation and the open bounties. It holds no
 * data: the boards live in the saved data, so the block can be placed, moved or destroyed freely.
 */
public class ContractBoardBlock extends Block {
    public ContractBoardBlock(Properties props) { super(props); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (player instanceof ServerPlayer sp) NationNet.open(sp, 3);
        return InteractionResult.CONSUME;
    }
}
