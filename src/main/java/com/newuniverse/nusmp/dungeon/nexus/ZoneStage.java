package com.newuniverse.nusmp.dungeon.nexus;

import net.minecraft.server.level.ServerLevel;

/**
 * Extra staged work a zone asks for after its plan's own ops (see {@link ZoneSite#stages}). The Golden City hands its whole structure to
 * the old city builder this way. The builder calls {@link #begin} and {@link StageSequence.Stage#run} inside {@code DungeonGuard.internal},
 * only while every chunk of the footprint is loaded for {@code run}, a slice of about 12000 units a tick (a block written costs
 * {@link NexusCursor#WRITE}, a block that is already right {@link NexusCursor#READ}), and saves (stage, op, cell) after each slice.
 * A stage that only waits for something returns 0 from {@code run} without being done; that ends the slice and it is asked again next tick.
 */
public interface ZoneStage {
    /** A name for the log. */
    String name();

    /**
     * Opens the stage. (0, 0) means it starts fresh; anything else is a position this stage reported earlier ({@code op()} and
     * {@code cell()} of the stage it returned), so a restart picks it up: rewind a few ops inside the same phase if the work is not
     * idempotent op by op, as {@link NexusCursor#resume} does.
     */
    StageSequence.Stage begin(ServerLevel level, int savedOp, long savedCell);
}
