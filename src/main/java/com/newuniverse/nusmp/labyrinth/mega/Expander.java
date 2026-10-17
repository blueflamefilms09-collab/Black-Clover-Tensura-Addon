package com.newuniverse.nusmp.labyrinth.mega;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * Why: the facade ({@link MegaDungeon}) must be able to say "grow the dungeon" without importing the generator, or the guard builder's
 * Gatekeeper door would depend on the whole assembler. The generator registers ONE implementation at start-up
 * ({@link MegaDungeon#setExpander}); everything else only calls the interface. Calls are cheap: they queue a job and return, the job runs
 * over the following ticks.
 */
public interface Expander {
    /** Queues sector 0 if it does not exist yet. False when it exists already or a job is running. */
    boolean requestFirst(MinecraftServer server);

    /** Queues the sector that attaches to {@code from}'s Way Onward. False when refused (already generated, queued, expansion off, limit reached). */
    boolean requestNext(ServerLevel level, Sector from);

    /** Progress of the running job in 0..1, or -1 when idle (for the "the Labyrinth is waking" action bar). */
    float progress();

    /** Is a generation job queued or running? */
    default boolean busy() { return progress() >= 0f; }
}
