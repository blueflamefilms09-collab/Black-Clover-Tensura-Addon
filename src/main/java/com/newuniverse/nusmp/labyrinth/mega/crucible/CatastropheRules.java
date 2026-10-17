package com.newuniverse.nusmp.labyrinth.mega.crucible;

/**
 * Why: a catastrophe is three small clocks (a falling-dust ceiling with a warning and a few pulses, a toxic flood that ends early when the vault is
 * empty, a spirit that wakes after a summoning circle and is forgotten when everybody left), and "every one is survivable by running out of the
 * vault" is a promise about those clocks: the warning is long enough to turn round, the pulses are spaced wide enough to cross the hall between
 * two of them, the flood has a visible countdown and stops when nobody is in it. Putting the numbers and the tick arithmetic here, away from the
 * server tick handler, lets {@code CatastropheRulesTest} state the promise as assertions (a warning of at least a second, pulses 30 ticks apart, a
 * flood that ends the moment it is empty after its grace period) instead of leaving it to a stopwatch in game. Pure: ticks and counts in, booleans
 * and numbers out.
 */
public final class CatastropheRules {
    private CatastropheRules() {}

    // ---------------------------------------------------------------- collapse
    /** The ceiling groans this long before the first pulse (two seconds, or a third of a very short collapse). */
    public static final int COLLAPSE_WARN_TICKS = 40;
    /** Ticks between two pulses: a player sprinting 5.6 blocks per second crosses 8 blocks of the hall between two. */
    public static final int COLLAPSE_PULSE_EVERY = 30;

    /** The collapse lasts {@code seconds} but never under three seconds (a warning and one pulse). */
    public static int collapseTotalTicks(int seconds) { return Math.max(60, seconds * 20); }

    public static int collapseWarnTicks(int totalTicks) { return Math.min(COLLAPSE_WARN_TICKS, totalTicks / 3); }

    /** Does a damage pulse fall on this tick of the collapse? The first one comes after the warning, then one every {@link #COLLAPSE_PULSE_EVERY}. */
    public static boolean collapsePulseAt(int elapsed, int totalTicks) {
        int warn = collapseWarnTicks(totalTicks);
        return elapsed >= warn && elapsed < totalTicks && (elapsed - warn) % COLLAPSE_PULSE_EVERY == 0;
    }

    /**
     * A pulse is a fall of masonry, not an instant blow: at the pulse a ring of dust and a column of falling dust mark where a stone will land on each
     * person, and this many ticks later it lands. Seven tenths of a second is long enough to step aside (a walking person covers three blocks in it) and short
     * enough to be a scare, so the collapse is a test of nerve that is won by moving, and a person who keeps moving takes almost nothing.
     */
    public static final int DEBRIS_TELEGRAPH_TICKS = 14;
    /** How far from the point where it lands a stone still hurts (blocks, horizontally): two steps away is out of it. */
    public static final double DEBRIS_RADIUS = 1.8;
    /** How far above or below the landing point a person may stand and still be hit (blocks). */
    public static final double DEBRIS_REACH_Y = 3.0;
    /** Harmless stones that fall round the aimed ones at every pulse, for the look of a ceiling coming down. */
    public static final int DEBRIS_DECOR_PER_PULSE = 3;
    /**
     * The largest share of a person's maximum health the whole collapse may take, however many stones land on them: the collapse is a trial of nerve and
     * never an execution, which is the promise "every catastrophe is survivable by running out of the vault" in one number.
     */
    public static final float COLLAPSE_BUDGET = 0.60f;

    /** Does a stone that landed at the origin hurt somebody standing at this offset (dx, dz horizontally, dy up)? */
    public static boolean debrisHits(double dx, double dy, double dz) {
        return dx * dx + dz * dz <= DEBRIS_RADIUS * DEBRIS_RADIUS && Math.abs(dy) <= DEBRIS_REACH_Y;
    }

    /** How much more damage the collapse may do to a person who has already taken {@code taken} of it: the budget of their maximum health, never below zero. */
    public static float collapseAllowance(float maxHealth, float taken) {
        return Math.max(0f, COLLAPSE_BUDGET * Math.max(1f, maxHealth) - Math.max(0f, taken));
    }

    /** The tick a stone aimed at a pulse of {@code elapsed} lands on. */
    public static int debrisLandsAt(int elapsed) { return elapsed + DEBRIS_TELEGRAPH_TICKS; }

    /** How many pulses the whole collapse holds. */
    public static int collapsePulseCount(int totalTicks) {
        int n = 0;
        for (int t = 0; t < totalTicks; t++) if (collapsePulseAt(t, totalTicks)) n++;
        return n;
    }

    // ---------------------------------------------------------------- flood
    /** The flood does not end early in its first three seconds: the chooser has just turned round and the effect has not been applied yet. */
    public static final int FLOOD_GRACE_TICKS = 60;

    public static int floodTotalTicks(int seconds) { return Math.max(100, seconds * 20); }

    /** The seconds the action bar shows: the time left, rounded up, never below 0. */
    public static int floodSecondsLeft(int elapsed, int totalTicks) { return Math.max(0, (totalTicks - elapsed + 19) / 20); }

    /** The effects are renewed once a second. */
    public static boolean floodPulseAt(int elapsed) { return elapsed % 20 == 0; }

    /** After the grace period, a flood nobody stands in has done its work. */
    public static boolean floodEndsEarly(int elapsed, int playersInside) { return elapsed >= FLOOD_GRACE_TICKS && playersInside <= 0; }

    // ---------------------------------------------------------------- spirit
    /** The Cursed Spirit stands still and cannot be hurt for three seconds (the summoning circle) so the party can run or brace. */
    public static final int SPIRIT_SUMMON_TICKS = 60;

    public static boolean spiritAwake(int elapsed) { return elapsed >= SPIRIT_SUMMON_TICKS; }

    /** No survival player has been in the sector for {@code minutes}: the spirit is let go. */
    public static boolean spiritAbandoned(long now, long lastPlayerTick, int minutes) { return now - lastPlayerTick >= Math.max(1, minutes) * 1200L; }

    /** The spirit entity cannot be found: it died or was removed when its ground is loaded, and may only be asleep in an unloaded chunk otherwise. */
    public static boolean spiritGone(boolean entityPresent, boolean groundLoaded) { return !entityPresent && groundLoaded; }
}
