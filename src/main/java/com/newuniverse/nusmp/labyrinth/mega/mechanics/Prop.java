package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import net.minecraft.core.BlockPos;

/**
 * Why: a mechanic that asks the players to DO something (pull a lever, step on a plate, give an offering, repeat a pattern) puts small blocks into its room. A prop is one of
 * them: where it is, what kind it is (plate, lever, pedestal, lectern, button, chest) and its index in the order the puzzle wants them used. The props are placed through the
 * instance's prop journal, so they vanish with the mechanic.
 */
public record Prop(BlockPos pos, String kind, int index) {}
