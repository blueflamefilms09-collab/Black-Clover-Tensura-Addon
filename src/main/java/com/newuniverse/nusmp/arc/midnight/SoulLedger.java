package com.newuniverse.nusmp.arc.midnight;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * Why: the Soul Abductor is the spell in the story that calls a sacrificed soul back to its body and takes the wounds of its death with it. In the fight it
 * calls Rades's FALLEN corpses back, once, in the place where they fell. To do that the boss must remember where and when each corpse fell, forget the ones
 * that fell too long ago, and hand each back only once; that bookkeeping is this pure class, so the rule (a window of fifteen seconds, the newest first, a
 * corpse revived is a corpse forgotten, a bounded memory) is proven by a test and the boss class only asks "who can come back?".
 */
public final class SoulLedger {
    private static final int MAX_ENTRIES = 32;
    private final ArrayDeque<double[]> fallen = new ArrayDeque<>();

    /** Notes a corpse that fell at (x, y, z) at {@code tick}. The oldest memory is forgotten when there are too many. */
    public void fell(long tick, double x, double y, double z) {
        fallen.addLast(new double[]{tick, x, y, z});
        while (fallen.size() > MAX_ENTRIES) fallen.pollFirst();
    }

    /** How many could come back now: fell within the last {@code window} ticks. */
    public int count(long now, int window) {
        int n = 0;
        for (double[] f : fallen) if (f[0] > now - window && f[0] <= now) n++;
        return n;
    }

    /**
     * Takes up to {@code max} of the corpses that fell within the window, newest first, forgets them, and returns where they fell (x, y, z each).
     * Entries older than the window are dropped on the way.
     */
    public List<double[]> take(long now, int window, int max) {
        fallen.removeIf(f -> f[0] <= now - window);
        List<double[]> out = new ArrayList<>();
        while (out.size() < Math.max(0, max) && !fallen.isEmpty()) {
            double[] f = fallen.pollLast();
            out.add(new double[]{f[1], f[2], f[3]});
        }
        return out;
    }

    public void clear() { fallen.clear(); }

    public int size() { return fallen.size(); }
}
