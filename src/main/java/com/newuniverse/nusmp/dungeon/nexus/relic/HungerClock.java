package com.newuniverse.nusmp.dungeon.nexus.relic;

/**
 * The Glutton's Chalice is alive (docs/living_dungeon.md section 7, Symbiotic Hunger): fed no biomass for three in-game days, it drinks from
 * its wearer, hunger first and magicules with it, until it is fed. This is its clock. Pure: times are game ticks.
 */
public final class HungerClock {
    private HungerClock() {}

    /** Three in-game days. */
    public static final long STARVE_AFTER = 72000L;
    /** Two days: the chalice stirs. Two and a half: it is hungry (a warning). Three: it feeds on its wearer. */
    public static final long STIRS_AT = 48000L, HUNGRY_AT = 60000L;
    /** The wearer is looked at every {@code SWEEP} ticks; once starving it takes a food point every fourth sweep and a share of the pool every sweep. */
    public static final int SWEEP = 20, FOOD_EVERY = 4;
    public static final double MAGICULE_SHARE = 0.005, MAGICULE_SHARE_LATE = 0.01;
    /** A day after it starts feeding it feeds harder. */
    public static final long LATE_AFTER = 24000L;

    public enum Phase { FED, STIRRING, HUNGRY, STARVING }

    /** Ticks since it was last fed; a stamp from a clock that ran later counts as just fed. */
    public static long since(long now, long fedAt) { return fedAt > now ? 0L : now - fedAt; }

    public static Phase phase(long since) {
        if (since >= STARVE_AFTER) return Phase.STARVING;
        if (since >= HUNGRY_AT) return Phase.HUNGRY;
        if (since >= STIRS_AT) return Phase.STIRRING;
        return Phase.FED;
    }

    public static Phase phase(long now, long fedAt) { return phase(since(now, fedAt)); }

    /** Hunger points taken on this sweep. */
    public static int foodLoss(long since) {
        return since >= STARVE_AFTER && (since / SWEEP) % FOOD_EVERY == 0 ? 1 : 0;
    }

    /** The share of the maximum magicule pool taken on this sweep. */
    public static double magiculeLoss(long since) {
        if (since < STARVE_AFTER) return 0.0;
        return since - STARVE_AFTER >= LATE_AFTER ? MAGICULE_SHARE_LATE : MAGICULE_SHARE;
    }

    /** Feeding resets the clock: it is the moment it was fed. */
    public static long fed(long now) { return now; }

    /** Whole in-game days (24000 ticks) since it was fed, one decimal, for the tooltip. */
    public static double days(long since) { return Math.round(since / 2400.0) / 10.0; }
}
