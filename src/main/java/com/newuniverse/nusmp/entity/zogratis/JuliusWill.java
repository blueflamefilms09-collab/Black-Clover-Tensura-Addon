package com.newuniverse.nusmp.entity.zogratis;

/**
 * The Two Souls mechanic (pure): the body of Lucius also holds Julius Novachrono, the Wizard King, asleep. At fixed health thresholds
 * Julius's will surfaces for {@value #DURATION} ticks (eight seconds): the boss stumbles, takes extra damage, the party is blessed. This
 * class is the scheduler of those windows:
 * <ul>
 *   <li>each threshold opens one window, once per fight, however health moves afterwards (healing back up does not re-arm it);</li>
 *   <li>a hit that crosses two thresholds at once opens one window (the lower one is spent too, not queued);</li>
 *   <li>a threshold crossed <i>during</i> a window waits, and opens the next one once the window is over and a pause of
 *       {@value #PAUSE} ticks has passed, so windows never run into each other;</li>
 *   <li>it stays shut while the boss cannot take it (the Gates' transformation, a fight that is not on).</li>
 * </ul>
 */
public final class JuliusWill {
    public enum Event { NONE, SURFACE, END }

    /** Health fractions at which Julius surfaces (each between two steps of the power curve, plus a last stand). */
    public static final float[] THRESHOLDS = {0.75f, 0.55f, 0.35f, 0.12f};
    /** Eight seconds. */
    public static final int DURATION = 160;
    /** Least ticks between the end of one window and the start of the next. */
    public static final int PAUSE = 100;

    private int armed = (1 << THRESHOLDS.length) - 1, pending;
    private long until, shutUntil;
    private boolean active;
    private int surfaced, lastIndex = -1;

    /** A fresh fight: every threshold armed, nothing pending. */
    public void reset() {
        armed = (1 << THRESHOLDS.length) - 1;
        pending = 0;
        until = 0;
        shutUntil = 0;
        active = false;
        surfaced = 0;
        lastIndex = -1;
    }

    /**
     * Looks at the boss's health. Returns SURFACE when a window opens now, END when the open one has just run out, NONE otherwise.
     * 'canSurface' is false while the boss cannot take it (transformation, no fight).
     */
    public Event update(float healthFraction, long now, boolean canSurface) {
        if (active && now >= until) {
            active = false;
            shutUntil = now + PAUSE;
            return Event.END;
        }
        int crossed = 0;
        for (int i = 0; i < THRESHOLDS.length; i++) if ((armed & (1 << i)) != 0 && healthFraction < THRESHOLDS[i]) crossed |= 1 << i;
        if (crossed == 0 && pending == 0) return Event.NONE;
        if (active) {                                  // a window is open: what is crossed now waits
            pending |= crossed;
            armed &= ~crossed;
            return Event.NONE;
        }
        if (!canSurface) {
            pending |= crossed;
            armed &= ~crossed;
            return Event.NONE;
        }
        int ready = pending | crossed;
        armed &= ~crossed;
        if (now < shutUntil) {
            pending = ready;
            return Event.NONE;
        }
        pending = 0;
        // the lowest threshold that was crossed names the window; a skipped higher one is spent with it
        int idx = 0;
        for (int i = 0; i < THRESHOLDS.length; i++) if ((ready & (1 << i)) != 0) idx = i;
        active = true;
        until = now + DURATION;
        surfaced++;
        lastIndex = idx;
        return Event.SURFACE;
    }

    public boolean active(long now) { return active && now < until; }

    /** Ticks left of the open window, 0 if there is none. */
    public int left(long now) { return active ? (int) Math.max(0, until - now) : 0; }

    /** 0..1 of the open window that is still left (for the second boss bar). */
    public float fractionLeft(long now) { return Math.max(0f, Math.min(1f, left(now) / (float) DURATION)); }

    /** How many windows have opened this fight. */
    public int surfaced() { return surfaced; }

    /** Index in {@link #THRESHOLDS} of the window that opened last, or -1. */
    public int lastIndex() { return lastIndex; }

    /** Thresholds still armed (for save data). */
    public int armedMask() { return armed; }

    /** Restores saved state: the armed thresholds and the ticks left of an open window. */
    public void restore(int armedMask, int ticksLeft, long now) {
        armed = armedMask & ((1 << THRESHOLDS.length) - 1);
        pending = 0;
        active = ticksLeft > 0;
        until = now + Math.max(0, ticksLeft);
        shutUntil = 0;
    }
}
