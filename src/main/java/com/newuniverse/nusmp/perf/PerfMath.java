package com.newuniverse.nusmp.perf;

/**
 * Why: the small decisions every throttle, stagger and level-of-detail rule of this pass makes live here as pure functions (no game classes), so they are unit-tested once and every
 * caller says the same thing: "is it my tick", "which tick is mine", "how detailed at this distance".
 */
public final class PerfMath {
    private PerfMath() {}

    /** The smallest and largest scan or refresh period (ticks) a config value may ask for. */
    public static final int MIN_INTERVAL = 1, MAX_INTERVAL = 1200;

    /** {@code v} limited to [lo, hi]. */
    public static int clamp(int v, int lo, int hi) { return v < lo ? lo : Math.min(v, hi); }

    /** A period from a config value, kept in a sane range. */
    public static int interval(int configured) { return clamp(configured, MIN_INTERVAL, MAX_INTERVAL); }

    /** The tick of the period an id owns: the same id always lands on the same phase, ids spread over the period. */
    public static int phaseOf(int id, int period) {
        int p = Math.max(1, period);
        return Math.floorMod(id, p);
    }

    /** Is {@code tick} the one tick of every {@code period} that belongs to {@code phase}? A period of 1 or less is every tick. */
    public static boolean due(long tick, int period, int phase) {
        if (period <= 1) return true;
        return Math.floorMod(tick, (long) period) == Math.floorMod(phase, period);
    }

    /** Is it this entity's turn: every {@code period} ticks, staggered by the entity's id so a crowd does not all work on the same tick. */
    public static boolean dueFor(long tick, int period, int entityId) {
        return due(tick, period, phaseOf(entityId, period));
    }

    /** How many of {@code wanted} things fit in {@code remaining} (never negative, never above wanted). A negative remaining counts as none. */
    public static int fit(int wanted, int remaining) {
        if (wanted <= 0 || remaining <= 0) return 0;
        return Math.min(wanted, remaining);
    }

    /**
     * The level of detail at a distance: 1 up to {@code near}, 0.75 up to the middle of near and far, 0.5 up to {@code far}, 0 beyond (cull). Stepped on purpose: a smooth ramp would
     * change the segment counts every frame and make an effect shimmer. {@code far <= near} means a single cut at {@code far}.
     */
    public static float lodDetail(double distance, double near, double far) {
        if (distance > far) return 0f;
        if (distance <= near) return 1f;
        if (far <= near) return 1f;
        return distance <= (near + far) * 0.5 ? 0.75f : 0.5f;
    }

    /** The same on a squared distance (cheaper: no square root per effect per frame). */
    public static float lodDetailSq(double distanceSq, double near, double far) {
        if (distanceSq > far * far) return 0f;
        if (distanceSq <= near * near) return 1f;
        if (far <= near) return 1f;
        double mid = (near + far) * 0.5;
        return distanceSq <= mid * mid ? 0.75f : 0.5f;
    }

    /**
     * How many auras of a wearer to paint at a distance: all of them near, at most {@code mid} between near and far, none beyond far. {@code count} is how many are running.
     */
    public static int auraCount(int count, double distanceSq, double near, double far, int mid) {
        if (count <= 0) return 0;
        if (distanceSq > far * far) return 0;
        if (distanceSq <= near * near) return count;
        return Math.min(count, Math.max(0, mid));
    }

    /**
     * The start tick that selects the {@code n} most recently started live windows: every window that started at or after it is kept. A window is {@code {start, end, ...}} in game ticks
     * and is live while {@code now < end}. Windows that started on the same tick are kept or dropped together, so a tie can let a few more than {@code n} through. With no more than n live
     * windows the result is the oldest live start (so every live window is kept); with none it is Long.MIN_VALUE. Allocation free apart from the iterators; n is small.
     */
    public static long nthNewestStart(Iterable<long[]> windows, float now, int n) {
        long bound = Long.MAX_VALUE;                                   // only starts below this are still to be taken
        long result = Long.MIN_VALUE;
        int taken = 0;
        while (taken < n) {
            long best = Long.MIN_VALUE;
            boolean found = false;
            for (long[] w : windows) {
                if (now >= w[1] || w[0] >= bound) continue;
                if (!found || w[0] > best) {
                    best = w[0];
                    found = true;
                }
            }
            if (!found) break;
            int same = 0;
            for (long[] w : windows) if (now < w[1] && w[0] == best) same++;
            result = best;
            taken += same;
            bound = best;
        }
        return result;
    }

    /** Estimated cost of a periodic job as a share of the ticks it runs on: calls per second times the cost of one call (ns), in milliseconds per second. */
    public static double msPerSecond(double callsPerSecond, double nanosPerCall) {
        return callsPerSecond * nanosPerCall / 1.0e6;
    }
}
