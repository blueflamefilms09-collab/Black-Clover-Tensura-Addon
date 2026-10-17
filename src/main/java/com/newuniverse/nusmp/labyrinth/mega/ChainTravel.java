package com.newuniverse.nusmp.labyrinth.mega;

import net.minecraft.server.level.ServerPlayer;

/**
 * Why: the Hub's polarity gates must be able to say "take this player up (or down)" without importing the generator's travel code, which is written by
 * another builder in parallel. The generator registers ONE implementation at start-up ({@link MegaDungeon#setChainTravel}); the gate only calls
 * {@link MegaDungeon#enterChain}. The implementation wakes the chain's first sector when it does not exist yet, picks the deepest Landing the player ever
 * reached in that chain (or its first one), and teleports through {@code LabyrinthTravel.arrive}, so the way home is remembered the same way as for every
 * other Labyrinth trip.
 */
public interface ChainTravel {
    /** Takes the player into the polarity's chain. False when it cannot (the message says why). */
    boolean enterChain(ServerPlayer player, Polarity polarity);
}
