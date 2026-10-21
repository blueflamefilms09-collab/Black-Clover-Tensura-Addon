package com.newuniverse.nusmp.labyrinth.loot.backpack;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Why: inside the dungeon every generated block is unbreakable, and the owner wants one exception: what a PLAYER placed (a torch, a sleeping bag, a crafting station, a backpack) can
 * be broken and picked up again. The record of "a player placed this block here" is the dungeon guard's job (it has a log of player-placed positions per level), and the backpack
 * must not depend on how that log is kept or edit the guard. So this is the tiny seam between the two. The BACKPACK side calls {@link #placed} the moment a player sets one down,
 * so the guard can mark the position even if its own block-place listener did not see this placement; the GUARD side installs a listener with {@link #install} (for example {@code PlayerPlaced::mark}) and may ask {@link #isBackpack} when it decides whether
 * a block it never saw placed should still be picked up. With nothing installed the call does nothing, so the backpack works on its own, and an installed listener that throws is
 * ignored: a placement must never fail because of a log. Server side.
 */
public interface PlacedHook {
    /** A player placed a backpack at {@code pos}. {@code by} is null when something other than a player did (a structure, a command). */
    void placed(Level level, BlockPos pos, Player by);

    /** The listener, or nothing. */
    final class Holder {
        private Holder() {}

        private static volatile PlacedHook hook;
    }

    /** Installs the guard's listener (replacing an earlier one); null removes it. */
    static void install(PlacedHook listener) { Holder.hook = listener; }

    /** Is a listener installed? */
    static boolean installed() { return Holder.hook != null; }

    /** Tells the listener, if any; never throws. */
    static void fire(Level level, BlockPos pos, Player by) {
        PlacedHook h = Holder.hook;
        if (h == null || level == null || level.isClientSide) return;
        try {
            h.placed(level, pos, by);
        } catch (RuntimeException ignored) {
            // a log of placed blocks must never be able to stop a placement
        }
    }

    /** Is this block state a backpack? For the guard's "may this be picked up" question. */
    static boolean isBackpack(BlockState state) { return state != null && state.getBlock() instanceof BackpackBlock; }
}
