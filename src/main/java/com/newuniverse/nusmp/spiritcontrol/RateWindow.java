package com.newuniverse.nusmp.spiritcontrol;

import java.util.Arrays;

/**
 * Why: a hard ceiling on how many things may happen in any window of game ticks, with no allocation and no scan: a ring of the last {@code max} times. At most {@code max} acquires succeed in any span of
 * {@code windowTicks} ticks. The server-wide cap on spirit casts per second is one of these; so is the gap between two casts of one spirit (max 1).
 */
public final class RateWindow {
    private final long[] stamps;
    private final long window;
    private int next;

    public RateWindow(int max, int windowTicks) {
        this.stamps = new long[Math.max(1, max)];
        this.window = Math.max(1, windowTicks);
        Arrays.fill(stamps, Long.MIN_VALUE / 4);
    }

    public int max() { return stamps.length; }

    public int windowTicks() { return (int) window; }

    /** Takes one slot at game tick {@code now}; false when {@code max} were already taken within the window. */
    public boolean tryAcquire(long now) {
        if (now - stamps[next] < window && now >= stamps[next]) return false;
        stamps[next] = now;
        next = (next + 1) % stamps.length;
        return true;
    }

    /** How many slots are taken within the window ending at {@code now}. */
    public int used(long now) {
        int n = 0;
        for (long s : stamps) if (now >= s && now - s < window) n++;
        return n;
    }
}
