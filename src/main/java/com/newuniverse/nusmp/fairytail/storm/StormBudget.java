package com.newuniverse.nusmp.fairytail.storm;

import com.newuniverse.nusmp.perf.PerfBudget;

/**
 * Why: NO LAG on a public server, ever (CLAUDE.md rule 11). However many storms are alive, the work they may do in one server tick is bounded here, with the shared per-tick budgets of
 * the performance pass ({@link PerfBudget}): at most {@link #SCANS_PER_TICK} entity scans over all storms in one tick, {@link #HAIL_PER_TICK} hail volleys, {@link #BOLTS_PER_TICK} bolt and
 * {@link #BOLTS_PER_SECOND} bolts in a second over the whole server. A storm that is refused simply does that piece of work on its next due tick (a scan, a volley) or skips the bolt (the
 * cloud mutters); nothing is queued, so a crowd of storms can never build a backlog. Pure (no game class, the tick is passed in) and owned by the server thread.
 */
public final class StormBudget {
    private StormBudget() {}

    /** The allowances. */
    public static final int SCANS_PER_TICK = 6, HAIL_PER_TICK = 3, BOLTS_PER_TICK = 1, BOLTS_PER_SECOND = 6;

    private static final PerfBudget SCANS = new PerfBudget("storm-scans", SCANS_PER_TICK);
    private static final PerfBudget HAIL = new PerfBudget("storm-hail", HAIL_PER_TICK);
    private static final PerfBudget BOLTS = new PerfBudget("storm-bolts", BOLTS_PER_TICK);
    private static long boltSecond = Long.MIN_VALUE;
    private static int boltsThisSecond;

    /** May one more entity scan run in this tick? Counts it when yes. */
    public static boolean scan(long tick) { return SCANS.take(tick, 1) > 0; }

    /** May one more hail volley land in this tick? */
    public static boolean hail(long tick) { return HAIL.take(tick, 1) > 0; }

    /** May one more bolt strike now: one per tick and {@link #BOLTS_PER_SECOND} per second over the whole server. Counts it when yes. */
    public static boolean bolt(long tick) {
        long sec = tick / 20;
        if (sec != boltSecond) {
            boltSecond = sec;
            boltsThisSecond = 0;
        }
        if (boltsThisSecond >= BOLTS_PER_SECOND) return false;
        if (BOLTS.take(tick, 1) <= 0) return false;
        boltsThisSecond++;
        return true;
    }

    /** Forgets everything (a server stop, the self-test). */
    public static void reset() {
        SCANS.resetStats();
        HAIL.resetStats();
        BOLTS.resetStats();
        boltSecond = Long.MIN_VALUE;
        boltsThisSecond = 0;
    }

    /**
     * The most a storm system can cost in ONE server tick, as a count of the operations that matter: entity scans, hail volleys, bolts (each with its own few entity queries and packets).
     * Used by the doc and the test; the numbers above are what holds it.
     */
    public static int worstCaseOperationsPerTick() { return SCANS_PER_TICK + HAIL_PER_TICK + BOLTS_PER_TICK; }
}
