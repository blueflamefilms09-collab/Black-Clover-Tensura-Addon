package com.newuniverse.nusmp.labyrinth.mega.system;

/**
 * Why: letting the player place their own stat points moves the one rule that matters (how many points may go where) from the dice to a request that comes from
 * the CLIENT, and a client is never trusted: it may ask for a negative amount, for a thousand points it does not have, for a stat above the cap, or for a stat
 * that does not exist. So the whole bookkeeping is here as pure functions over the saved {@link SystemState}, and every one of them can only ever MOVE points
 * between the free pool and the five stats: the sum of the pool and the stats never changes in an allocation or a reset, so no request, however crafted, can create
 * or destroy a point. The server entry ({@link SystemPoints}) calls these and then applies the attribute modifiers; the tests hammer them with hostile numbers. Pure.
 */
public final class PointRules {
    private PointRules() {}

    /** The most points one allocation message may ask for (a bigger number is a damaged or hostile packet and is refused whole). */
    public static final int MAX_AMOUNT = 1000;

    /** The per-stat limit actually in force: the configured cap, never below 0 and never above the hard cap of a save. */
    public static int effectiveCap(int cap) { return Math.max(0, Math.min(SystemState.HARD_CAP, cap)); }

    /** How many points the stat can still take from the pool: the room under its cap, limited by what the pool holds (never negative). */
    public static int maxAddable(SystemState s, Stat stat, int cap) {
        return Math.max(0, Math.min(s.free(), effectiveCap(cap) - s.points(stat)));
    }

    /**
     * How many more points the pool can still hold before it holds more than the stats could ever take: the sum of the room under the cap of each stat, minus
     * what the pool already has. Rewards are paid into the pool only while this is above 0, so a player whose stats are all full does not collect points they
     * could never spend (the core is paid instead, as a capped stat does in random mode).
     */
    public static int room(int[] points, int free, int cap) {
        int c = effectiveCap(cap), open = 0;
        for (int p : points) open += Math.max(0, c - p);
        return Math.max(0, open - Math.max(0, free));
    }

    /**
     * Moves points between the pool and one stat and returns the signed number of points that went INTO the stat. A positive request takes from the pool up to the
     * pool's size and the stat's room under the cap; a negative request (a respec of one stat) gives points back to the pool, and only when {@code allowRespec};
     * zero, a missing stat, an amount past {@link #MAX_AMOUNT} in either direction, or a request that cannot move anything all return 0 and change nothing.
     */
    public static int allocate(SystemState s, Stat stat, int amount, int cap, boolean allowRespec) {
        if (s == null || stat == null || amount == 0 || amount > MAX_AMOUNT || amount < -MAX_AMOUNT) return 0;
        if (amount > 0) {
            int n = Math.min(amount, maxAddable(s, stat, cap));
            if (n <= 0) return 0;
            s.setPoints(stat, s.points(stat) + n);
            s.setFree(s.free() - n);
            return n;
        }
        if (!allowRespec) return 0;
        int n = Math.min(-amount, Math.min(s.points(stat), SystemState.FREE_MAX - s.free()));
        if (n <= 0) return 0;
        s.setPoints(stat, s.points(stat) - n);
        s.setFree(s.free() + n);
        return -n;
    }

    /** Moves every spent point back into the pool (only when {@code allowRespec}) and returns how many moved; 0 when respec is off or nothing was spent. */
    public static int reset(SystemState s, boolean allowRespec) {
        if (s == null || !allowRespec) return 0;
        int moved = 0;
        for (Stat st : Stat.values()) {
            int n = Math.min(s.points(st), SystemState.FREE_MAX - s.free());      // the pool has a ceiling too: a point that would not fit stays where it is
            if (n <= 0) continue;
            s.setPoints(st, s.points(st) - n);
            s.setFree(s.free() + n);
            moved += n;
        }
        return moved;
    }
}
