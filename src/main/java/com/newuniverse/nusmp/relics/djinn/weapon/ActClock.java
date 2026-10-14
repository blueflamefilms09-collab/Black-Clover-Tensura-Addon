package com.newuniverse.nusmp.relics.djinn.weapon;

/**
 * The cooldowns of one bearer's two weapon acts, in absolute game ticks (pure). A clock whose ready time lies further ahead than any cooldown can be
 * (the world's clock was set back) counts as ready, so a rewound level never locks a weapon.
 */
public final class ActClock {
    /** No cooldown in the table is longer than this; a ready time further ahead than this is a clock that went backwards. */
    public static final int MAX_COOLDOWN = 1200;

    private final long[] readyAt = new long[2];

    public long readyAt(int act) { return readyAt[act]; }

    /** Ticks left of an act's cooldown at {@code now}; 0 when ready. */
    public int left(int act, long now) {
        long d = readyAt[act] - now;
        if (d <= 0 || d > MAX_COOLDOWN) return 0;
        return (int) d;
    }

    public boolean ready(int act, long now) { return left(act, now) == 0; }

    /** Starts an act's cooldown (clamped to 0..MAX_COOLDOWN). */
    public void start(int act, long now, int ticks) { readyAt[act] = now + Math.max(0, Math.min(MAX_COOLDOWN, ticks)); }

    /** Forgets both (the weapon was taken away: a new one starts fresh). */
    public void clear() { readyAt[0] = 0; readyAt[1] = 0; }
}
