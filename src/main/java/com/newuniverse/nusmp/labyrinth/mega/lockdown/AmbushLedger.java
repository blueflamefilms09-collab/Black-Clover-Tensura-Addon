package com.newuniverse.nusmp.labyrinth.mega.lockdown;

import java.util.ArrayDeque;

/**
 * Why: the memory of the ambush director about ONE player, kept as plain numbers so its arithmetic (heat that rises per signal and cools with time, a cooldown after an
 * ambush, a budget of so many ambushes per window, the count of earlier strikes that raises the next squad's band) is tested without a game. The game class holds one
 * ledger per player in a map and forgets it at logout. Time is a parameter (game ticks), never read from a clock. Pure.
 */
public final class AmbushLedger {
    private double heat;
    private long heatAt;
    private long cooldownUntil;
    private final ArrayDeque<Long> fired = new ArrayDeque<>();
    /** The last tick each kind of signal was accepted: one signal of a kind per {@link #SIGNAL_GAP} ticks, so a long run is one signal a second, not twenty. */
    private final long[] lastSignal = new long[AmbushRules.Signal.values().length];
    /** What the most recent signal was, for the status line. */
    private AmbushRules.Signal lastKind;
    /** Ticks between two accepted signals of the same kind. */
    public static final long SIGNAL_GAP = 20L;

    public AmbushLedger() {
        java.util.Arrays.fill(lastSignal, Long.MIN_VALUE / 2);
    }

    /** The heat now (cooled up to {@code now}). */
    public double heat(long now, AmbushRules.Config c) {
        heat = AmbushRules.cooled(heat, now - heatAt, c);
        heatAt = now;
        return heat;
    }

    /**
     * Adds a signal's points. Returns the points added (0 when the same kind of signal was accepted less than {@link #SIGNAL_GAP} ticks ago). The heat is capped at
     * twice the threshold, so one hour of running does not store a day of ambushes.
     */
    public double add(AmbushRules.Signal s, int liveMobs, long now, AmbushRules.Config c) {
        heat(now, c);
        int i = s.ordinal();
        if (now - lastSignal[i] < SIGNAL_GAP) return 0.0;
        lastSignal[i] = now;
        double p = AmbushRules.points(s, liveMobs, c);
        heat = Math.min(heat + p, 2.0 * c.threshold());
        lastKind = s;
        return p;
    }

    public AmbushRules.Signal lastSignal() { return lastKind; }

    public long cooldownLeft(long now) { return Math.max(0L, cooldownUntil - now); }

    /** How many ambushes were fired inside the budget window ending at {@code now}. */
    public int firedInWindow(long now, AmbushRules.Config c) {
        prune(now, c);
        int n = 0;
        for (long t : fired) if (now - t <= c.budgetWindowTicks()) n++;
        return n;
    }

    /** How many ambushes were fired inside the strike window: the earlier strikes that raise the next band. */
    public int strikes(long now, AmbushRules.Config c) {
        int n = 0;
        for (long t : fired) if (now - t <= c.strikeWindowTicks()) n++;
        return n;
    }

    /** An ambush was fired: the heat is spent, the cooldown starts, the budget is charged. */
    public void fired(long now, AmbushRules.Config c) {
        heat = 0.0;
        heatAt = now;
        cooldownUntil = now + c.cooldownTicks();
        fired.addLast(now);
        prune(now, c);
        while (fired.size() > 64) fired.removeFirst();
    }

    /** An ambush was due but no place could be found: part of the heat stays, so the next suspicious act tries again soon (half the threshold is kept). */
    public void noPlace(long now, AmbushRules.Config c) {
        heat = Math.min(heat(now, c), c.threshold() * 0.5);
    }

    private void prune(long now, AmbushRules.Config c) {
        long keep = Math.max(c.budgetWindowTicks(), c.strikeWindowTicks());
        while (!fired.isEmpty() && now - fired.peekFirst() > keep) fired.removeFirst();          // the two windows are counted separately; only what is older than both goes
    }
}
