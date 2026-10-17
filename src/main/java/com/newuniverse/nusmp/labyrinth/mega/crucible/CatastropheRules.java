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
