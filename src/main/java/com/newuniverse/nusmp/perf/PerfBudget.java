package com.newuniverse.nusmp.perf;

/**
 * Why: a spell, a boss move or a fireworks of relic effects can ask for hundreds of particles, packets or entity scans in one tick, and fifty of them in the same tick is a stutter for the whole
 * server. A budget is the simplest guard: a named allowance per tick. Whoever wants to spend asks {@link #take}; it answers how many it may have this tick (all of them while the allowance
 * lasts, fewer or none after), and the allowance refills the next tick. Spending less than asked thins an effect out instead of dropping the tick. A cap of 0 or less means unlimited
 * (the guard is off), so a server owner can switch a budget off in the config.
 *
 * <p>Pure (no game classes) and single-threaded by design: a budget belongs to one thread (the server thread for the particle and scan budgets, the client thread for the frame budgets);
 * {@link #take} is not synchronized. The counters of denied requests are kept for the {@code /multiverse perf report}.
 */
public final class PerfBudget {
    private final String name;
    private int cap;
    private long tick = Long.MIN_VALUE;
    private int used;
    private long asked, granted, peak;

    public PerfBudget(String name, int cap) {
        this.name = name;
        this.cap = cap;
    }

    public String name() { return name; }

    /** The allowance per tick; 0 or less = unlimited. */
    public int cap() { return cap; }

    public void setCap(int cap) { this.cap = cap; }

    /** What has been spent in the current tick. */
    public int used() { return used; }

    /** How many units were asked for since the last {@link #resetStats}, and how many of them were granted. */
    public long asked() { return asked; }

    public long granted() { return granted; }

    /** The most one single tick has spent since the last {@link #resetStats}. */
    public long peak() { return peak; }

    public long denied() { return asked - granted; }

    public void resetStats() {
        asked = 0;
        granted = 0;
        peak = 0;
    }

    private void roll(long now) {
        if (now != tick) {
            tick = now;
            used = 0;
        }
    }

    /** The units out of {@code wanted} that may be spent in tick {@code now} (0..wanted). The granted units count against this tick's allowance. */
    public int take(long now, int wanted) {
        if (wanted <= 0) return 0;
        roll(now);
        asked += wanted;
        int give = cap <= 0 ? wanted : PerfMath.fit(wanted, cap - used);
        used += give;
        granted += give;
        if (used > peak) peak = used;
        return give;
    }

    /** All or nothing: true (and spends {@code n}) only if the whole of {@code n} fits in this tick. */
    public boolean tryTake(long now, int n) {
        if (n <= 0) return true;
        roll(now);
        asked += n;
        if (cap > 0 && cap - used < n) return false;
        used += n;
        granted += n;
        if (used > peak) peak = used;
        return true;
    }

    /** Units still free in tick {@code now} (Integer.MAX_VALUE when unlimited). */
    public int remaining(long now) {
        roll(now);
        return cap <= 0 ? Integer.MAX_VALUE : Math.max(0, cap - used);
    }
}
