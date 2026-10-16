package com.newuniverse.nusmp.avatar.outcome;

/**
 * Why: the three-day roll has three results and two reasons to skip it; one enum names them so the save format, the command, the engine and the tests agree.
 * Stored by name; constants only appended.
 */
public enum Outcome {
    /** Not decided yet (the timer still runs). */
    PENDING,
    BETRAYAL, SOUL_AWAKENING, ORDINARY,
    /** The Avatar was named: it stays loyal and the timer is cancelled (Soul-Bond Lock). */
    SKIPPED_NAMED,
    /** The Avatar holds a Unique / Ultimate skill: it counts as independent already, the roll is skipped and it stays loyal. */
    SKIPPED_UNIQUE;

    public static Outcome byName(String s) {
        for (Outcome o : values()) if (o.name().equalsIgnoreCase(s)) return o;
        return PENDING;
    }

    /** True when the outcome is final (nothing will roll again). */
    public boolean decided() { return this != PENDING; }
}
