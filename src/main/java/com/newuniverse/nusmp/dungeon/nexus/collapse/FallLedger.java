package com.newuniverse.nusmp.dungeon.nexus.collapse;

import java.util.Arrays;

/**
 * Counts the falling blocks a collapse has in the air without ever scanning for them: a block that was started in the last WINDOW ticks
 * is taken to be still falling, so the number alive is the sum over a sliding window. That bounds the entities of a collapse to
 * {@link CollapsePlan#MAX_ALIVE} whatever the sweep wants. Pure.
 */
public final class FallLedger {
    /** About how long a block takes to fall the height of a Nexus, and the most a falling block entity lives anyway (they are discarded on landing). */
    public static final int WINDOW = 80;

    private final int[] counts = new int[WINDOW];
    private final long[] stamps = new long[WINDOW];

    public FallLedger() { reset(); }

    public void reset() {
        Arrays.fill(counts, 0);
        Arrays.fill(stamps, Long.MIN_VALUE / 4);
    }

    /** Notes that n blocks were started at this tick. */
    public void add(long tick, int n) {
        if (n <= 0) return;
        int i = (int) Math.floorMod(tick, (long) WINDOW);
        if (stamps[i] != tick) {
            stamps[i] = tick;
            counts[i] = 0;
        }
        counts[i] += n;
    }

    /** How many started blocks are taken to be still in the air at this tick. */
    public int alive(long tick) {
        int sum = 0;
        for (int i = 0; i < WINDOW; i++) if (stamps[i] <= tick && tick - stamps[i] < WINDOW) sum += counts[i];
        return sum;
    }

    /** How many more may be started at this tick without passing `cap` alive. */
    public int room(long tick, int cap) { return Math.max(0, cap - alive(tick)); }
}
