package com.newuniverse.nusmp.dungeon.sentient;

/**
 * When a born mob is let go: one that has had no player within 96 blocks for three minutes goes back into the walls (its room stays
 * populated in the Dungeon's memory and can be born again). The counter is kept per mob and moves a whole pass at a time. Pure.
 */
public final class LonelyClock {
    private LonelyClock() {}

    /** Three minutes, in ticks, and the distance a player must be within for a mob not to be lonely. */
    public static final int ALONE_TICKS = 3 * 60 * 20;
    public static final double NEAR = 96.0;

    /** The counter after one pass of {@code passTicks} ticks: back to nothing if a player is near, else grown. */
    public static int next(int alone, boolean playerNear, int passTicks) { return playerNear ? 0 : alone + Math.max(0, passTicks); }

    public static boolean expired(int alone) { return alone >= ALONE_TICKS; }
}
