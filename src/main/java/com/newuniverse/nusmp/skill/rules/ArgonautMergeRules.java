package com.newuniverse.nusmp.skill.rules;

/**
 * Why: Firebolt was folded into Argonaut (docs/argonaut_rework.md). Argonaut (and Hero's Argonaut) gain a Firebolt mode next to the charged
 * strike, and a charged release carries fire. Which mode index means what, how many modes there are, how long a charged strike burns and when a
 * saved Firebolt holder is migrated are pure functions, so they are unit-tested instead of discovered by playing. The old mode numbers never move:
 * STRIKE 0 and (Hero) BEAM 1 stay, FIREBOLT is appended (Argonaut: index 1, Hero's Argonaut: index 2).
 */
public final class ArgonautMergeRules {
    private ArgonautMergeRules() {}

    /** Charges needed before a release also sets the struck creatures alight (a single bell is still a plain strike). */
    public static final int FIRE_MIN_CHARGE = 2;
    /** The most seconds a charged strike can burn a target. */
    public static final int MAX_IGNITE_SECONDS = 12;
    /** Mastery a Firebolt that hit teaches Argonaut: half of what the standalone Firebolt taught, so rapid casting cannot outrun the charged strike. */
    public static final double FIREBOLT_MASTERY_PER_HIT = 0.5;

    /** How many modes: the Unique has Strike (+ Firebolt); the Ultimate has Strike, Beam (+ Firebolt). {@code merged} is the server setting. */
    public static int modes(boolean ultimate, boolean merged) { return (ultimate ? 2 : 1) + (merged ? 1 : 0); }

    /** What a mode index does. An index outside the live range (a saved mode after the setting went off) is the plain Strike. */
    public static ChargeRules.Strike kind(int mode, boolean ultimate, boolean merged) {
        if (mode < 0 || mode >= modes(ultimate, merged)) return ChargeRules.Strike.STRIKE;
        if (ultimate) return mode == 0 ? ChargeRules.Strike.STRIKE : mode == 1 ? ChargeRules.Strike.BEAM : ChargeRules.Strike.FIREBOLT;
        return mode == 0 ? ChargeRules.Strike.STRIKE : ChargeRules.Strike.FIREBOLT;
    }

    /** The mode after {@code mode} (or before, reversed), wrapping around the live modes. */
    public static int nextMode(int mode, boolean reverse, boolean ultimate, boolean merged) {
        int n = modes(ultimate, merged);
        int cur = Math.max(0, Math.min(n - 1, mode));
        return reverse ? (cur - 1 + n) % n : (cur + 1) % n;
    }

    /** Seconds a charged strike burns what it hits: 0 below {@link #FIRE_MIN_CHARGE} charges, then 2 + the charge, capped. */
    public static int chargedIgniteSeconds(int charge) {
        return charge < FIRE_MIN_CHARGE ? 0 : Math.min(MAX_IGNITE_SECONDS, 2 + charge);
    }

    /** Mastery a Firebolt cast teaches Argonaut: only a shot that hit something. */
    public static double fireboltMastery(boolean hit) { return hit ? FIREBOLT_MASTERY_PER_HIT : 0.0; }

    /** A saved Firebolt holder is migrated (Firebolt forgotten, Argonaut kept or granted) only while the merge is on. */
    public static boolean migrates(boolean merged, boolean holdsFirebolt) { return merged && holdsFirebolt; }

    /** Argonaut is granted only to a holder who has neither Argonaut nor its Ultimate (the Ultimate already carries the Firebolt mode). */
    public static boolean grantsArgonaut(boolean holdsArgonaut, boolean holdsHeros) { return !holdsArgonaut && !holdsHeros; }

    /** With the merge turned off again, a holder whose Firebolt was migrated gets the old standalone Firebolt back. */
    public static boolean restores(boolean merged, boolean wasMigrated, boolean holdsFirebolt) { return !merged && wasMigrated && !holdsFirebolt; }
}
