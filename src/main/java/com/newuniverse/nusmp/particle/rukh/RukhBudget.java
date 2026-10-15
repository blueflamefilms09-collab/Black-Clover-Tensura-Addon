package com.newuniverse.nusmp.particle.rukh;

/**
 * Why: relics and abilities fire from dozens of call sites and several of them every tick for every wearer, so the number of Rukh particles
 * has to be capped in ONE place or a crowded fight makes the client (and the packet stream) lag, which is exactly what the owner's brief
 * wants to avoid. This is the cap as a tiny pure state machine (no Minecraft classes, so it is unit tested): a counter that belongs to one
 * level, resets when the game tick changes, and grants at most {@link #cap()} particles per tick. {@code RukhFx} keeps one per
 * {@code ServerLevel} (a level has its own clock and its own players, so the budgets must not share) and {@code RukhClientFx} one for the
 * client. Excess requests are dropped, never queued: a missing particle is invisible, a delayed one is a visual bug.
 */
public final class RukhBudget {
    /** Particles per level per game tick, the default. */
    public static final int DEFAULT_CAP = 96;

    private int cap;
    private long tick = Long.MIN_VALUE;
    private int used;

    public RukhBudget(int cap) {
        this.cap = Math.max(0, cap);
    }

    public synchronized int cap() {
        return cap;
    }

    public synchronized void setCap(int cap) {
        this.cap = Math.max(0, cap);
    }

    /** Asks for {@code want} particles at game tick {@code now}; returns how many are granted (0..want). A clock that went backwards simply starts a new tick. */
    public synchronized int take(long now, int want) {
        if (want <= 0) return 0;
        if (now != tick) {
            tick = now;
            used = 0;
        }
        int give = Math.min(want, cap - used);
        if (give <= 0) return 0;
        used += give;
        return give;
    }

    /** What is still free at tick {@code now} (does not consume). */
    public synchronized int remaining(long now) {
        return now != tick ? cap : Math.max(0, cap - used);
    }
}
