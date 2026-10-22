package com.newuniverse.nusmp.spiritcontrol;

/**
 * Why: a ceiling on how many combat decisions the whole server makes in one game tick (each decision may run one target query), so many players in combat mode can never pile their decisions onto one tick.
 * Pure: it counts per tick and resets when the tick changes. Decisions over the budget are simply skipped; the player's next decision tick comes soon enough.
 */
public final class TickBudget {
    private long tick = Long.MIN_VALUE;
    private int used;

    /** Takes one decision slot of tick {@code now}; false when {@code max} were already taken in this tick. */
    public boolean take(long now, int max) {
        if (now != tick) { tick = now; used = 0; }
        if (used >= max) return false;
        used++;
        return true;
    }

    public int used(long now) { return now == tick ? used : 0; }
}
