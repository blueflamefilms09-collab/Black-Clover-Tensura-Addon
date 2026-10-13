package com.newuniverse.nusmp.dungeon.sentient;

/**
 * The immune system's decisions, apart from the world: what the Dungeon does when a player's threat has been raised (nothing, a warning at
 * half and at four fifths of the threshold, a Juggernaut at the threshold), and whether a Juggernaut may be sent now (not before the player's
 * cooldown is over, not while the last attempt to find a wall is fresh, not with three alive or coming). Pure.
 */
public final class ImmunePolicy {
    private ImmunePolicy() {}

    /** Ticks to wait before trying again after a Juggernaut could not be placed (the search for a wall is not free). */
    public static final int RETRY = 100;
    /** A threat that could not be answered is held at this share of the threshold, so it does not stay over the line for good. */
    public static final double HOLD = 0.95;

    public enum Step { NONE, WARN_FIRST, WARN_SECOND, SEND }

    /** The step after the threat rose to {@code value}; {@code warned} is the warning level already given (0, 1 or 2). */
    public static Step after(double value, double threshold, int warned) {
        if (value >= threshold) return Step.SEND;
        int level = ThreatMeter.warning(value, threshold);
        if (level <= warned) return Step.NONE;
        return level >= 2 ? Step.WARN_SECOND : Step.WARN_FIRST;
    }

    /** The warning level to remember after the threat is at {@code value} (it falls back when the threat does, so the next rise warns again). */
    public static int warned(double value, double threshold) { return ThreatMeter.warning(value, threshold); }

    public enum Verdict { GO, WAIT_RETRY, COOLDOWN, CAP }

    /** May a Juggernaut be sent to this player now? {@code force} (the admin command) skips the retry wait and the cooldown, never the cap. */
    public static Verdict mayTry(boolean force, long now, long retryAt, long cooldownUntil, int alive, int pending) {
        if (!force && now < retryAt) return Verdict.WAIT_RETRY;
        if (!force && now < cooldownUntil) return Verdict.COOLDOWN;
        return JuggernautPlan.mayBirth(alive, pending, now, 0L) ? Verdict.GO : Verdict.CAP;
    }

    /** When to try again after no wall could be found. */
    public static long retryAt(long now) { return now + RETRY; }

    /** The value a threat that was over the threshold but could not be answered is held at. */
    public static double held(double threshold) { return threshold * HOLD; }
}
