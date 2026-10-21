package com.newuniverse.nusmp.labyrinth.mega.poise;

/**
 * Why: when a squad captain or elite is nearly dead it should become the most dangerous thing in the room, and the party should SEE it coming. This is the pure rule of that phase: it
 * starts when the health share drops under {@code desperateBelow} (30 percent) and ends only over {@code desperateExit} (a healer's tick cannot make it flicker on and off); while it
 * lasts the bearer attacks faster ({@link #cooldown}, {@link #roll}), winds its telegraphed attacks up a little quicker ({@link #wind}, never under {@link #MIN_WIND} ticks so the cue stays
 * readable) and the foundry mob unlocks its archetype's ultimate. The world side does the visible part (cue particles, the speed buff, the ultimate).
 */
public final class Desperation {
    private Desperation() {}

    /** The shortest wind-up a desperate bearer ever gets: the party must always have this long to read the cue. */
    public static final int MIN_WIND = 8;

    /** The next state: off becomes on under the entry share; on stays on until the share passes the exit share. Dead or non-finite shares never start it. */
    public static boolean next(boolean current, double healthShare, PoiseTuning t) {
        if (Double.isNaN(healthShare) || healthShare <= 0.0) return false;
        return current ? healthShare <= t.desperateExit() : healthShare < t.desperateBelow();
    }

    /** A cooldown in desperation: shorter by the pace factor, at least 1 tick. */
    public static int cooldown(int base, boolean on, PoiseTuning t) {
        if (!on || base <= 1) return Math.max(0, base);
        return Math.max(1, (int) Math.round(base / t.pace()));
    }

    /** The ticks between two ability rolls in desperation. */
    public static int roll(int every, boolean on, PoiseTuning t) { return cooldown(every, on, t); }

    /** A wind-up in desperation: scaled down, but never below {@link #MIN_WIND} (or the base itself, if that is already shorter). */
    public static int wind(int base, boolean on, PoiseTuning t) {
        if (!on) return Math.max(1, base);
        int scaled = (int) Math.round(base * t.windScale());
        return Math.max(Math.min(base, MIN_WIND), scaled);
    }
}
