package com.newuniverse.nusmp.dungeon.nexus.relic;

/**
 * The Pheromone Mark of the Mantle of the Scavenger (docs/living_dungeon.md section 7): taking the mantle off leaves the wearer smelling of
 * prey for thirty seconds, and every predator of the roster (tier 2 and up) within {@link #RADIUS} blocks comes for them. Bounded and
 * escapable: it ends by itself, by distance, and on death, relog or a change of dimension. Pure.
 */
public final class Pheromone {
    private Pheromone() {}

    public static final int DURATION = 600, RADIUS = 48, SWEEP = 20, MAX_PER_SWEEP = 24, MIN_TIER = 2;

    /** When a mark begun now runs out. */
    public static long until(long now) { return now + DURATION; }

    /** Is the mark on? An end stamp further off than a whole mark can be (a clock that ran back) is not. */
    public static boolean active(long now, long until) { return now < until && until - now <= DURATION; }

    public static int secondsLeft(long now, long until) { return active(now, until) ? (int) ((until - now + 19) / 20) : 0; }

    /** Does a predator of this roster tier come for the marked? */
    public static boolean aggroes(int tier) { return tier >= MIN_TIER; }
}
