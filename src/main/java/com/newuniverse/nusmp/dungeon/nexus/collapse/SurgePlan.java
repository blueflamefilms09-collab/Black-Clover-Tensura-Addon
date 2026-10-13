package com.newuniverse.nusmp.dungeon.nexus.collapse;

import java.util.List;

/**
 * The Spatial Surge of the Treasury Collapse as rules: while the Dungeon comes down, the space inside it surges. Every 12 to 20 seconds
 * (each player on a clock of their own, so the tears do not open all at once) a tear opens under a player for two seconds and then throws
 * them 8 to 24 blocks across the floor. A surge is a cost in seconds and never a danger, and the rules that keep it so are all here, pure:
 * a throw never ends more than {@link #MAX_FARTHER} blocks farther from the nearest way out than it began, never ends on ground the falling
 * stone has swept or is about to sweep, never in a place the player could not walk back from (a sealed Treasury, an island: {@link #connected}),
 * never more than {@link #MAX_LEVEL} blocks above or below where they stood, never comes within {@link #QUIET} ticks of the end, never reaches
 * a player in creative or spectating or anyone within {@link #GATE_SAFE} blocks of a Root Gate (the last step must never be stolen), and at
 * most one comes in {@link #MIN_GAP} ticks. The world only supplies facts (where the player is, where the stone has got to, which places
 * can be stood on).
 */
public final class SurgePlan {
    private SurgePlan() {}

    /** Ticks between one throw and the next one for the same player: 12 to 20 seconds. */
    public static final int MIN_GAP = 240, MAX_GAP = 400;
    /** A player the collapse sees for the first time is thrown no sooner than this long (and a gap) after: time to read the room. */
    public static final int GRACE = 200;
    /** A tear shows this long (2 seconds) before it throws. */
    public static final int WARNING = 40;
    /** No throw in the last 20 seconds of the clock: the final dash must be clear of it. */
    public static final long QUIET = 400L;
    /** A player this close to a Root Gate (blocks) is never thrown. */
    public static final double GATE_SAFE = 4.0;
    /** A throw lands this far away (the vault's Warp Trap throws the same 8 to 24), at least this far from the old spot, costs at most this many blocks of distance to the nearest exit, and puts the feet at most MAX_LEVEL blocks above or below where they were. */
    public static final int MIN_MOVE = 8, MAX_MOVE = 24, MIN_FROM_OLD = 6, MAX_FARTHER = 10, MAX_LEVEL = 4;
    /** Nausea after a throw: 3 seconds. */
    public static final int NAUSEA = 60;
    /** A tear held back because a Root Gate is at hand is tried again after this many ticks. */
    public static final int RETRY = 20;

    /** What the glue does for a player now: nothing; open the tear; go on showing it; throw; tell them the Anchor held; close the tear unused. */
    public enum Act { NONE, TEAR, SPIRAL, WARP, HELD, CLOSE }

    /** Does the clock say it is too late for a throw (the last {@link #QUIET} ticks, or after the end)? */
    public static boolean quiet(long elapsed) { return elapsed >= CollapsePlan.TOTAL - QUIET; }

    /**
     * The surges of one player: when the next throw is due, whether its tear is open. Deterministic: the same salt and the same looks give the
     * same throws. A throw is only ever due a gap (MIN_GAP to MAX_GAP) after the last one or after the player was first seen, and every way the
     * due time is moved only moves it later, so two throws are never closer than MIN_GAP.
     */
    public static final class Clock {
        private final long salt;
        private long due = Long.MIN_VALUE, lastWarp = Long.MIN_VALUE / 4;
        private int rolls;
        private boolean open;

        /** `salt` makes this player's clock their own (the collapse's number mixed with the player's id). */
        public Clock(long salt) { this.salt = salt; }

        /** The ticks to the next throw after one: MIN_GAP to MAX_GAP, a stable roll of this clock. */
        private long gap() { return MIN_GAP + Math.floorMod(CollapsePlan.mix(salt, rolls++), (long) (MAX_GAP - MIN_GAP + 1)); }

        /** Is a tear open for this player right now? */
        public boolean open() { return open; }

        /** The tick (since the collapse began) the next throw is due at, or Long.MIN_VALUE before the first look. */
        public long due() { return due; }

        /** When the last throw was made, or a very old tick if none was. */
        public long lastWarp() { return lastWarp; }

        /**
         * One look at the player, `elapsed` ticks after the collapse began. `exempt`: creative, spectating or dead (the clock starts over when
         * they are back). `anchored`: the Band of the Spatial Anchor is worn. `nearGate`: a Root Gate is within {@link #GATE_SAFE} blocks.
         */
        public Act step(long elapsed, boolean exempt, boolean anchored, boolean nearGate) {
            if (exempt || quiet(elapsed)) {
                boolean was = open;
                open = false;
                if (exempt) due = Long.MIN_VALUE;
                return was ? Act.CLOSE : Act.NONE;
            }
            if (due == Long.MIN_VALUE) {                                              // the first look: a grace, then a gap of their own
                due = elapsed + GRACE + gap();
                return Act.NONE;
            }
            if (open) {
                if (anchored || nearGate) {
                    open = false;
                    due = elapsed + WARNING + (anchored ? gap() : RETRY);
                    return anchored ? Act.HELD : Act.CLOSE;
                }
                if (elapsed < due) return Act.SPIRAL;
                open = false;
                lastWarp = elapsed;
                due = elapsed + gap();
                return Act.WARP;
            }
            if (elapsed + WARNING < due) return Act.NONE;
            if (anchored) {
                due = elapsed + WARNING + gap();
                return Act.HELD;
            }
            if (nearGate) {
                due = elapsed + WARNING + RETRY;
                return Act.NONE;
            }
            due = Math.max(due, elapsed + WARNING);                                   // a full warning, however late this look comes
            if (quiet(due)) {                                                         // the throw would fall in the last seconds: there is no tear at all
                due = Long.MAX_VALUE;
                return Act.NONE;
            }
            open = true;
            return Act.TEAR;
        }
    }

    // ---------------------------------------------------------------- where a throw may end
    /** The flat distance (blocks) from (x, z) to the nearest of the spots {x, y, z} (the middle of the block); positive infinity if there are none. */
    public static double exitDistance(List<int[]> spots, double x, double z) {
        double best = Double.POSITIVE_INFINITY;
        if (spots == null) return best;
        for (int[] s : spots) {
            double dx = s[0] + 0.5 - x, dz = s[2] + 0.5 - z;
            best = Math.min(best, Math.sqrt(dx * dx + dz * dz));
        }
        return best;
    }

    /** Is a Root Gate (a spot {x, y, z}) within GATE_SAFE blocks of the player? */
    public static boolean nearGate(List<int[]> gates, double x, double y, double z) {
        if (gates == null) return false;
        for (int[] g : gates) {
            double dx = g[0] + 0.5 - x, dy = g[1] + 0.5 - y, dz = g[2] + 0.5 - z;
            if (dx * dx + dy * dy + dz * dz <= GATE_SAFE * GATE_SAFE) return true;
        }
        return false;
    }

    /** The fairness rule: the nearest way out may be at most MAX_FARTHER blocks farther after the throw than before it (NaN is refused). */
    public static boolean costsLittle(double exitBefore, double exitAfter) { return exitAfter <= exitBefore + MAX_FARTHER; }

    /**
     * May a throw end in the column at `index` of the sweep order? Not one the falling stone has swept (below the cursor) or will have swept
     * within the warning ahead of the front (below `frontLimit`, see {@link CollapsePlan#warnLimit}); a corridor (`index` at or past `open`)
     * stands until the wipe; a column the collapse does not have (-1) is refused.
     */
    public static boolean columnOk(int index, int cursor, int frontLimit, int open) {
        if (index < 0) return false;
        if (index >= open) return true;
        return index >= Math.max(cursor, frontLimit);
    }

    /** Everything about a column that can be known before the world is searched for a place to stand: the sweep rule and the fairness rule. */
    public static boolean columnAllowed(int index, int cursor, int frontLimit, int open, double exitBefore, double exitAfter) {
        return columnOk(index, cursor, frontLimit, open) && costsLittle(exitBefore, exitAfter);
    }

    /** Are the feet above the floor that stays and the head under the roof (two cells of room between floorY and topY)? */
    public static boolean heightOk(int y, int floorY, int topY) { return y > floorY && y + 1 <= topY; }

    /** Is the new spot at least MIN_FROM_OLD blocks from the old one (offsets in blocks)? */
    public static boolean farFromOld(int dx, int dy, int dz) {
        long d2 = (long) dx * dx + (long) dy * dy + (long) dz * dz;
        return d2 >= (long) MIN_FROM_OLD * MIN_FROM_OLD;
    }

    /** What is left to check once a place to stand has been found: the height, the level it is on and the distance from the old spot. */
    public static boolean spotAllowed(int y, int floorY, int topY, int dx, int dy, int dz) {
        return heightOk(y, floorY, topY) && Math.abs(dy) <= MAX_LEVEL && farFromOld(dx, dy, dz);
    }

    // ---------------------------------------------------------------- a place the player can walk back from
    /** The world as a walk sees it: can a person stand with their feet in this block (two cells of air over something solid)? */
    @FunctionalInterface
    public interface Ground {
        boolean stand(int x, int y, int z);
    }

    /** A walk looks at this many cells at the most, steps down at most WALK_DROP blocks, counts as there within WALK_NEAR blocks (and 3 up or down) of its goal, and stays within WALK_BOX blocks flat and WALK_HEIGHT high of where it began. */
    public static final int WALK_BUDGET = 1200, WALK_DROP = 3, WALK_NEAR = 2, WALK_BOX = 40, WALK_HEIGHT = 12;
    private static final int[][] WALK_DIRS = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
    private static final int[] WALK_STEPS = {0, 1, -1, -2, -3};

    private static long walkKey(int x, int y, int z, int fx, int fy, int fz) {
        return ((long) (x - fx + 512) << 40) | ((long) (z - fz + 512) << 20) | (long) (y - fy + 512);
    }

    /**
     * Can someone standing at (fx, fy, fz) walk to within a couple of blocks of (gx, gy, gz): stepping to a neighbouring column at the same level,
     * one block up or up to WALK_DROP blocks down, through places {@link Ground} says can be stood in? A search that heads for the goal and gives
     * up after WALK_BUDGET cells, so a sealed room, a closed gate or an island is found out cheaply and a big open hall costs little. False if
     * the start itself cannot be stood in. This is what keeps a throw from leaving anyone in a place they cannot get out of.
     */
    public static boolean connected(Ground g, int fx, int fy, int fz, int gx, int gy, int gz) {
        if (g == null || !g.stand(fx, fy, fz)) return false;
        java.util.PriorityQueue<long[]> open = new java.util.PriorityQueue<>((a, b) -> Long.compare(a[0], b[0]));      // {priority, steps, x, y, z}
        java.util.Set<Long> seen = new java.util.HashSet<>();
        open.add(new long[]{0L, 0L, fx, fy, fz});
        seen.add(walkKey(fx, fy, fz, fx, fy, fz));
        int looked = 0;
        while (!open.isEmpty() && looked < WALK_BUDGET) {
            long[] cur = open.poll();
            looked++;
            int x = (int) cur[2], y = (int) cur[3], z = (int) cur[4];
            if (Math.abs(x - gx) <= WALK_NEAR && Math.abs(z - gz) <= WALK_NEAR && Math.abs(y - gy) <= 3) return true;
            for (int[] d : WALK_DIRS) {
                int nx = x + d[0], nz = z + d[1];
                if (Math.abs(nx - fx) > WALK_BOX || Math.abs(nz - fz) > WALK_BOX) continue;
                for (int step : WALK_STEPS) {                                                                       // the level first, then one up, then down
                    int ny = y + step;
                    if (Math.abs(ny - fy) > WALK_HEIGHT || !g.stand(nx, ny, nz)) continue;
                    if (seen.add(walkKey(nx, ny, nz, fx, fy, fz))) {
                        long steps = cur[1] + 1;
                        open.add(new long[]{steps + Math.abs(nx - gx) + Math.abs(nz - gz) + 2L * Math.abs(ny - gy), steps, nx, ny, nz});
                    }
                    break;
                }
            }
        }
        return false;
    }

    /** Is the distance of a throw (blocks, flat) one the Dungeon makes: MIN_MOVE to MAX_MOVE? */
    public static boolean moveOk(int dx, int dz) {
        long d2 = (long) dx * dx + (long) dz * dz;
        return d2 >= (long) MIN_MOVE * MIN_MOVE && d2 <= (long) MAX_MOVE * MAX_MOVE;
    }
}
