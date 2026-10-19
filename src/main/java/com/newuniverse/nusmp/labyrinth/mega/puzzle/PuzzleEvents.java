package com.newuniverse.nusmp.labyrinth.mega.puzzle;

import com.newuniverse.nusmp.labyrinth.mega.MegaDungeon;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.server.ServerStoppingEvent;

/**
 * Why: the only hooks into the game the puzzles need, both existing events and both cheap when no puzzle is live (an empty-map check first): a right click on a block (a cell of a puzzle, or
 * its lectern; the click is cancelled so a pillar is never "used" like a block, a chest never opens and a held block is never placed on a tile) and the server stopping (every puzzle ends and
 * puts its blocks back). Everything else, the steps on tiles, is read from the players' positions in the tick, so no movement event is needed.
 */
public final class PuzzleEvents {
    private PuzzleEvents() {}

    public static void onRightBlock(PlayerInteractEvent.RightClickBlock e) {
        if (e.getHand() != InteractionHand.MAIN_HAND || !(e.getEntity() instanceof ServerPlayer p) || PuzzleDirector.ACTIVE.isEmpty() || !MegaDungeon.in(p.level())) return;
        long now = p.level().getGameTime();
        for (PuzzleInstance in : PuzzleDirector.active()) {
            if (in.state == PuzzleInstance.State.ENDED || p.level() != in.level || !in.area.contains(e.getPos())) continue;
            if (in.onClick(p, e.getPos(), e.getItemStack(), p.isShiftKeyDown(), now)) {
                e.setCanceled(true);
                e.setCancellationResult(InteractionResult.SUCCESS);
                return;
            }
        }
    }

    public static void onStopping(ServerStoppingEvent e) { PuzzleDirector.onServerStopping(); }
}
