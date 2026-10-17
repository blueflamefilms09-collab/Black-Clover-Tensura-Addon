package com.newuniverse.nusmp.labyrinth.mega.system;

/**
 * Why: a room counts as "cleared" for a quest only when the player has really been IN it for a moment (40 ticks) and nothing hostile is left alive there, not
 * when they sprint through a doorway. That is a small timer over "which room am I in, for how long", and it is fed a few times a second by the player tick, so
 * it must be allocation-free and testable without a game: this is that timer. It tells the caller exactly once when a room has been stood in long enough;
 * the caller then looks for hostiles (an entity query, which is the expensive part and so happens only now) and, if some are alive, asks to be told again a
 * second later. Moving to another room, or into none, starts the timer again. Pure.
 */
public final class RoomDwell {
    /** The "no room" value. */
    public static final int NONE = Integer.MIN_VALUE;
    /** Ticks a player must stand in a room before it can count (the brief's 40). */
    public static final int DWELL_TICKS = 40;

    private int room = NONE;
    private int ticks;
    private boolean offered;

    /**
     * Time passes by {@code dt} ticks while the player stands in {@code roomId} ({@link #NONE} for a place that does not count). Returns the room id the
     * first time it has been stood in for {@link #DWELL_TICKS}, else {@link #NONE}.
     */
    public int step(int roomId, int dt) {
        if (roomId == NONE) {
            reset();
            return NONE;
        }
        if (roomId != room) {
            room = roomId;
            ticks = 0;
            offered = false;
        }
        ticks += Math.max(0, dt);
        if (!offered && ticks >= DWELL_TICKS) {
            offered = true;
            return room;
        }
        return NONE;
    }

    /** The offered room could not be counted yet (a hostile is still alive in it): offer it again after about {@code inTicks} more ticks. */
    public void retry(int inTicks) {
        offered = false;
        ticks = Math.max(0, DWELL_TICKS - Math.max(1, inTicks));
    }

    public void reset() {
        room = NONE;
        ticks = 0;
        offered = false;
    }

    public int room() { return room; }

    public int ticks() { return ticks; }
}
