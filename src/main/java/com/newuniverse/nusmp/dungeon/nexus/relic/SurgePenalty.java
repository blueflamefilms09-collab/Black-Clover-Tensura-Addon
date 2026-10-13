package com.newuniverse.nusmp.dungeon.nexus.relic;

/**
 * How long the cost of a Wild Magic Surge lasts (the Gambler's Page, docs/living_dungeon.md section 7): a tenth of the user's mana pool for a
 * day ({@link WildSurge#PENALTY_TICKS}). It is kept as the game time it ends, in the player's persistent data ({@link SurgeHandler}), so a
 * relog, a death or a server restart does not wash it away, and it is over by itself when that time comes. Pure.
 */
public final class SurgePenalty {
    private SurgePenalty() {}

    /** A whole in-game day. */
    public static final int TICKS = WildSurge.PENALTY_TICKS;

    /** The game time a penalty that starts now ends. */
    public static long until(long now) { return now + TICKS; }

    /**
     * Is the penalty on at game time {@code now}? A stamp more than a day ahead came from a clock that ran later (another world, a reset): it is
     * over, so no stamp can hold the penalty for longer than a day.
     */
    public static boolean active(long now, long until) { return until > now && until - now <= TICKS; }

    /** Ticks still to go (0 when it is over). */
    public static long remaining(long now, long until) { return active(now, until) ? until - now : 0L; }

    /** Whole minutes still to go, rounded up (0 when it is over): for the message the page shows. */
    public static int minutesLeft(long now, long until) { return (int) ((remaining(now, until) + 1199L) / 1200L); }
}
