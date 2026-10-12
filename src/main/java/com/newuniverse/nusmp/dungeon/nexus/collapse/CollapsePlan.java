package com.newuniverse.nusmp.dungeon.nexus.collapse;

/**
 * The clock of the Treasury Collapse (docs/living_dungeon.md section 8) as a pure function of the ticks since the trigger: three minutes
 * from the first crack to the final wipe, the countdown marks, which stage the destruction is in, how fast it may work this tick, how
 * far the falling front has come, and when the exit corridors may be rained on. Nothing here knows Minecraft, so the whole timeline is
 * checked in tests: every curve is monotonic, bounded and deterministic.
 */
public final class CollapsePlan {
    private CollapsePlan() {}

    /** The whole countdown: 3:00 from the trigger to the final wipe. */
    public static final long TOTAL = 3600L;
    /** Cracks only until 2:00 are left; the rolling fall lasts until 0:30; the front reaches the exit corridors at 0:15. */
    public static final long CRACKS_END = 1200L, ROLL_END = 3000L, FRONT_END = 3300L;

    /** CRACKS: dust, rumbles, a block now and then. ROLLING: the far walls and ceilings come down, sweeping toward the exits. FINAL: the last half minute, the corridors rained on a little. WIPE: whatever stands is removed. */
    public enum Stage { CRACKS, ROLLING, FINAL, WIPE }

    public static Stage stageAt(long elapsed) {
        if (elapsed >= TOTAL) return Stage.WIPE;
        if (elapsed >= ROLL_END) return Stage.FINAL;
        if (elapsed >= CRACKS_END) return Stage.ROLLING;
        return Stage.CRACKS;
    }

    public static long remaining(long elapsed) { return Math.max(0L, TOTAL - elapsed); }

    // ---------------------------------------------------------------- the countdown
    /** The ticks left at which each countdown message is sent: 3:00, 2:00, 1:00, 0:30, 0:10, then 0:05 down to 0:01. */
    public static final int[] MARKS = {3600, 2400, 1200, 600, 200, 100, 80, 60, 40, 20};

    /**
     * The newest countdown mark that has come due, among those not sent yet (`next` is the index of the first unsent); -1 if none. After a
     * restart or a stall several may be due at once: only the newest is said, so nobody is told "3:00" with ten seconds left.
     */
    public static int latestDue(int next, long elapsed) {
        int found = -1;
        for (int i = Math.max(0, next); i < MARKS.length; i++) {
            if (elapsed >= TOTAL - MARKS[i]) found = i;
            else break;
        }
        return found;
    }

    // ---------------------------------------------------------------- how fast the stone may fall
    /** What one probe of a block and one removal cost against a tick's budget (a removal is probe + REMOVE). */
    public static final int PROBE = 1, REMOVE = 3;
    /** Units the rolling fall may spend a tick: nothing while cracking, then rising from SWEEP_FROM to SWEEP_TO by the last tick. */
    public static final int SWEEP_FROM = 800, SWEEP_TO = 3600;
    /** Falling-block entities the sweep may start a tick: rising from ENTITIES_FROM to ENTITIES_TO, and never more than MAX_ALIVE alive at once. */
    public static final int ENTITIES_FROM = 3, ENTITIES_TO = 14, MAX_ALIVE = 240;
    /** The final wipe's budget a tick (the same as the Nexus builder's: about 3000 blocks written). */
    public static final int WIPE_UNITS = 12_000;
    /** The most a block of the rolling front hurts when it lands on someone (hearts are 2). The blocks shaken loose by a crack or on a corridor hit for DRIP_DAMAGE at the most. */
    public static final int FRONT_HIT = 2;
    /** A player standing where the front is about to come down is warned this many ticks ahead. */
    public static final int WARN_AHEAD = 80;

    private static long ramp(long elapsed, int from, int to) {
        long t = Math.max(0L, Math.min(elapsed, TOTAL) - CRACKS_END);
        return from + (long) (to - from) * t / (TOTAL - CRACKS_END);
    }

    public static int sweepUnits(long elapsed) { return elapsed < CRACKS_END ? 0 : (int) ramp(elapsed, SWEEP_FROM, SWEEP_TO); }

    public static int entityBudget(long elapsed) { return elapsed < CRACKS_END ? 0 : (int) ramp(elapsed, ENTITIES_FROM, ENTITIES_TO); }

    /**
     * How many columns of the sweep order (far side first) are due by now, out of `open` that may be swept: none while cracking, then a
     * curve that starts slow and speeds up (0.3 p + 0.7 p squared) until the front has covered them all at FRONT_END.
     */
    public static int frontColumns(long elapsed, int open) {
        if (open <= 0 || elapsed <= CRACKS_END) return 0;
        if (elapsed >= FRONT_END) return open;
        long p = (elapsed - CRACKS_END) * 1000L / (FRONT_END - CRACKS_END);          // 0 to 1000
        long f = (3L * p + 7L * p * p / 1000L) / 10L;                                   // 0 to 1000
        return (int) Math.min(open, (long) open * f / 1000L);
    }

    /** How many columns of the order the front will have reached WARN_AHEAD ticks from now: whoever stands in a column below this (and above the cursor) is about to be hit. */
    public static int warnLimit(long elapsed, int open) { return frontColumns(elapsed + WARN_AHEAD, open); }

    // ---------------------------------------------------------------- cracks
    /** Ticks between two cracks: every 14 at first, every 5 once the rolling fall begins. */
    public static int crackInterval(long elapsed) { return (int) Math.max(5L, 14L - 9L * Math.min(elapsed, CRACKS_END) / CRACKS_END); }

    /** Does a crack open this tick (cracks go on until the last half minute, when the fall itself is the show)? */
    public static boolean crackDue(long elapsed) { return elapsed >= 0 && elapsed < ROLL_END && elapsed % crackInterval(elapsed) == 0; }

    /** Does this crack also shake one block loose? A third of them do, while the stage is still CRACKS. */
    public static boolean crackDrips(long id, long elapsed) { return elapsed < CRACKS_END && Math.floorMod(mix(id, elapsed), 3L) == 0L; }

    // ---------------------------------------------------------------- the exit corridors
    /** A column this close to an exit (blocks, flat) is part of its corridor: the sweep leaves it for the wipe. */
    public static final int CORRIDOR_RADIUS = 7;
    /** In the last half minute, except the last five seconds, a block drops on a corridor every 10 ticks, one second after its dust shows (the last one lands 5 seconds before the end); each hits for 3 at the most. */
    public static final long DRIP_FROM = ROLL_END, DRIP_UNTIL = TOTAL - 120L;
    public static final int DRIP_EVERY = 10, DRIP_LEAD = 20, DRIP_DAMAGE = 3;
    /** The most blocks that can ever be dripped on the corridors in one collapse. */
    public static final int MAX_CORRIDOR_DRIPS = (int) ((DRIP_UNTIL - DRIP_FROM) / DRIP_EVERY) + 1;

    public static boolean dripDue(long elapsed) { return elapsed >= DRIP_FROM && elapsed < DRIP_UNTIL && elapsed % DRIP_EVERY == 0; }

    // ---------------------------------------------------------------- determinism
    /** A stable scramble of two numbers (splitmix): the same collapse gives the same cracks and drips on every run and after a restart. */
    public static long mix(long a, long b) {
        long z = a * 0x9E3779B97F4A7C15L + b * 0xC2B2AE3D27D4EB4FL + 0x165667B19E3779F9L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** `mix` reduced to 0..n-1 (n of at least 1). */
    public static int pick(long a, long b, int n) { return n <= 1 ? 0 : (int) Math.floorMod(mix(a, b), (long) n); }
}
