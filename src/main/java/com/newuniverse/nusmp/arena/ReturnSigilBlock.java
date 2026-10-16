package com.newuniverse.nusmp.arena;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * Why: the way home from the arena must not depend on the hourglass (it may have been dropped with a death, or left in a chest), so four
 * Return Sigils stand at the edge of the courtyard. A right click sends the player back to where the hourglass took them from
 * ({@link ArenaTravel#leave}). While Lucius fights the sigil is dormant (leaving mid-fight costs the ten-second channel of the hourglass,
 * so the sigil is not a free exit), and it wakes when he is defeated or the fight is abandoned. It is unbreakable (the arena is guarded) and has no
 * block entity: the state it reads is the arena's saved data. Outside the arena it does nothing, so a creative-mode copy placed elsewhere is harmless.
 */
public class ReturnSigilBlock extends Block {
    public ReturnSigilBlock(Properties props) { super(props); }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
        if (!ZogratisArena.in(level)) {
            sp.displayClientMessage(Component.literal("The sigil is cold: it only answers inside the arena of the Final Hour.").withStyle(ChatFormatting.GRAY), true);
            return InteractionResult.CONSUME;
        }
        if (ArenaDirector.fightRunning(sp.server)) {
            sp.displayClientMessage(Component.literal("The sigil is dormant while Lucius fights. Use the Hourglass of the Final Hour (ten seconds).").withStyle(ChatFormatting.GOLD), true);
            return InteractionResult.CONSUME;
        }
        ArenaTravel.leave(sp);
        return InteractionResult.CONSUME;
    }
}
