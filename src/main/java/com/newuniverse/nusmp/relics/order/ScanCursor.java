package com.newuniverse.nusmp.relics.order;

/**
 * Walks a list of stacks to examine a few per tick (pure, unit tested). Every item has a cost (1 for a stack, 1 plus the stacks inside for a
 * shulker box or a bundle); a tick may spend {@link #PER_TICK} of them, but always takes at least one item so a big one cannot starve the scan.
 */
public final class ScanCursor {
    /** Most stacks examined per player per tick. */
    public static final int PER_TICK = 16;
    /** Ticks between two scans of one player's inventory (five seconds). */
    public static final int PLAYER_INTERVAL = 100;

    private final int[] cost;
    private int next;

    public ScanCursor(int[] cost) { this.cost = cost == null ? new int[0] : cost; }

    public int size() { return cost.length; }

    public int position() { return next; }

    public boolean done() { return next >= cost.length; }

    /**
     * How many items to examine this tick, starting at {@link #position()}: as many as fit into the budget, at least one while any is left.
     * Advances the cursor by that many.
     */
    public int take(int budget) {
        int n = 0, spent = 0;
        while (next < cost.length) {
            int c = Math.max(1, cost[next]);
            if (n > 0 && spent + c > budget) break;
            spent += c;
            next++;
            n++;
            if (spent >= budget) break;
        }
        return n;
    }

    /** True on the tick when the entity with this id starts its scan: staggered by id so that players never scan in the same tick. */
    public static boolean shouldStart(long tick, int entityId, int interval) {
        int i = Math.max(1, interval);
        return Math.floorMod(tick + entityId, (long) i) == 0L;
    }
}
