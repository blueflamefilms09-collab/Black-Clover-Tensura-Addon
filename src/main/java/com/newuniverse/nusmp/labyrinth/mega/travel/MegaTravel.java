package com.newuniverse.nusmp.labyrinth.mega.travel;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

/**
 * Why: where a player goes when a Gate, a Maw or the enter command sends them into the Mega-Dungeon. CONTRACT STUB (1.54.0): the core
 * builder replaces the body with the real logic (wake sector 0 on first use, pick the Landing, teleport through
 * {@code LabyrinthTravel.arrive}). {@code LabyrinthTravel.enter} already routes here when {@code mega.layout} is {@code "mega"}, so the
 * stub returning false simply means "the Labyrinth did not open" until the builder's code is in.
 */
public final class MegaTravel {
    private MegaTravel() {}

    /** Takes the player into the Mega-Dungeon from a gate at {@code from}. False when it cannot (the message says why). */
    public static boolean enter(ServerPlayer p, BlockPos from) { return false; }
}
