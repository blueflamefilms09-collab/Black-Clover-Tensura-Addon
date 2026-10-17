package com.newuniverse.nusmp.arc.early;

/**
 * Why: several early-boss mechanics are "this is true until tick N" (Heath is flustered, Vetto's aura is torn, Mars is staggered, a guard is up), and
 * writing that as a bare {@code long} in five classes invites the same two slips every time: forgetting that a window may be extended but never cut short
 * by a shorter one, and comparing with the wrong sign. One tiny pure class with a test, so each boss states what its window means and nothing else.
 */
public final class Window {
    private long until;

    /** Opens the window for {@code ticks} ticks from {@code now}; a window that is already open keeps the later end. */
    public void open(long now, int ticks) {
        long end = now + Math.max(0, ticks);
        if (end > until) until = end;
    }

    /** Is the window open at {@code now}? */
    public boolean active(long now) { return now < until; }

    /** Ticks left at {@code now} (0 when closed). */
    public int left(long now) { return (int) Math.max(0, until - now); }

    /** Shuts the window at once. */
    public void close() { until = 0; }

    /** The tick the window ends at (0 when it never opened). */
    public long end() { return until; }
}
