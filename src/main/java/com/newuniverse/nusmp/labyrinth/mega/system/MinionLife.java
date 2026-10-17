package com.newuniverse.nusmp.labyrinth.mega.system;

/**
 * Why: a shadow lives until it dies, or until its owner has been gone for {@code minionMinutes} (30 by default). "Gone" means offline OR in another
 * dimension, because the shadows follow the owner through the dungeon but never leave it on their own. The decision is a comparison of two game times, and
 * the awkward part is the bookkeeping: the clock must start when the owner is first seen away, must stop the moment the owner is back, and must still be right
 * after a server restart (the shadow is saved with its chunk, and the world clock keeps counting while nobody is there). Keeping the arithmetic in this tiny
 * pure class makes the sentinel ({@code 0} = not away) and the boundary (exactly the lifetime) testable. Pure.
 */
public final class MinionLife {
    private MinionLife() {}

    /** Game ticks in the given number of minutes (a negative number counts as 0). */
    public static long lifetimeTicks(int minutes) { return Math.max(0, minutes) * 60L * 20L; }

    /** The value to store when the owner is first seen away at game time {@code now}: never 0, which means "not away". */
    public static long awayStamp(long now) { return Math.max(1L, now); }

    /**
     * Has the owner been away for the whole lifetime?
     *
     * @param awaySince the stored stamp (0 = the owner is not away)
     * @param now       the game time now
     * @param lifetime  {@link #lifetimeTicks}
     */
    public static boolean expired(long awaySince, long now, long lifetime) {
        return awaySince > 0 && now - awaySince >= lifetime;
    }
}
