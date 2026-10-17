package com.newuniverse.nusmp.arc.midnight;

/**
 * Why: each of the four Midnight Sun fights has a way for the party to earn a window in which the boss is open (Patolli overreaches and falls, Fana's
 * Evil Eye breaks, Rhya's copy is turned against him, a reflected beam strikes back). Those windows must be real but never endless: a party that can
 * trigger one every few seconds would never meet the rest of the fight. This pure clock owns the rule: a stagger lasts a given number of ticks, and after
 * it ends the boss cannot be staggered again for a lockout, so the reward is a beat, not a lock. It reads game time as a plain number so it is testable.
 */
public final class StaggerClock {
    private long until = Long.MIN_VALUE, lockedUntil = Long.MIN_VALUE;

    /** Starts a stagger of {@code ticks} unless the lockout of the last one is still running. Returns whether it started. */
    public boolean tryStart(long now, int ticks, int lockout) {
        if (ticks <= 0 || now < lockedUntil) return false;
        until = now + ticks;
        lockedUntil = until + Math.max(0, lockout);
        return true;
    }

    /** Is the boss open at {@code now}? */
    public boolean active(long now) { return now < until; }

    /** Ticks of the stagger left at {@code now} (0 when none). */
    public int left(long now) { return (int) Math.max(0, until - now); }

    /** Ends the stagger and the lockout (a new fight, or the boss was reset). */
    public void clear() {
        until = Long.MIN_VALUE;
        lockedUntil = Long.MIN_VALUE;
    }

    /** The damage factor on a hit taken while open: a quarter more, the same bonus the framework gives the exposed phase after an ultimate. */
    public static final float OPEN_BONUS = 1.25f;
}
