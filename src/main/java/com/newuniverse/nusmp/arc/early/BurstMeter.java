package com.newuniverse.nusmp.arc.early;

import java.util.ArrayDeque;

/**
 * Why: "dealt a tenth of its health in three seconds" is the one test several mechanics need (Vetto's aura overloads, Mars's stone cracks open), and it must
 * not be confused by a single slow trickle: only blows inside a sliding window count. This pure class keeps the blows of the last {@code windowTicks}
 * ticks and their sum, drops the old ones as time passes, and never grows beyond a few dozen entries because it merges blows of the same tick.
 */
public final class BurstMeter {
    private record Blow(long tick, double amount) {}

    private final int windowTicks;
    private final ArrayDeque<Blow> blows = new ArrayDeque<>();
    private double total;

    public BurstMeter(int windowTicks) { this.windowTicks = Math.max(1, windowTicks); }

    /** Notes a blow at {@code now}. Blows of no size are ignored. */
    public void record(long now, double amount) {
        if (amount <= 0) return;
        expire(now);
        Blow last = blows.peekLast();
        if (last != null && last.tick() == now) {
            blows.pollLast();
            blows.addLast(new Blow(now, last.amount() + amount));
        } else {
            blows.addLast(new Blow(now, amount));
        }
        total += amount;
    }

    /** The damage dealt in the window that ends at {@code now}. */
    public double total(long now) {
        expire(now);
        return total;
    }

    /** Forgets everything. */
    public void clear() {
        blows.clear();
        total = 0;
    }

    private void expire(long now) {
        while (!blows.isEmpty() && now - blows.peekFirst().tick() >= windowTicks) total -= blows.pollFirst().amount();
        if (blows.isEmpty()) total = 0;
    }
}
