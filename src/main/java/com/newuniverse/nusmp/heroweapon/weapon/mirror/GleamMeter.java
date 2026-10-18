package com.newuniverse.nusmp.heroweapon.weapon.mirror;

/**
 * Why: the Mirror turns defence into offence: every shot a pane turns back and every spell the rebound returns leaves light in the glass (gleam), and the next
 * Prism Ray is worth more for it. The gleam is a small counter with a cap that fades when it is not fed, and it must be computed from the game clock when it is
 * asked for (nothing ticks it), so the rule lives here as plain arithmetic with tests: a gain stops the fading for a while, then one point is lost every
 * {@code decayEvery} ticks, and spending takes everything.
 */
public final class GleamMeter {
    private final int max, decayAfter, decayEvery;
    private int stored;
    private long lastGain = Long.MIN_VALUE / 4;

    public GleamMeter(int max, int decayAfter, int decayEvery) {
        this.max = Math.max(1, max);
        this.decayAfter = Math.max(0, decayAfter);
        this.decayEvery = Math.max(1, decayEvery);
    }

    /** The gleam at game time {@code now}, after the fading. */
    public int value(long now) {
        long idle = now - lastGain - decayAfter;
        long lost = idle > 0 ? idle / decayEvery : 0;
        return (int) Math.max(0L, stored - lost);
    }

    /** Adds {@code n} (a negative or zero amount changes nothing); the total never passes the cap. Returns the gleam now. */
    public int add(long now, int n) {
        if (n <= 0) return value(now);
        stored = Math.min(max, value(now) + n);
        lastGain = now;
        return stored;
    }

    /** Takes all the gleam and returns how much there was. */
    public int spendAll(long now) {
        int v = value(now);
        stored = 0;
        return v;
    }

    /** Empties the meter (the weapon was put away). */
    public void reset() {
        stored = 0;
    }
}
