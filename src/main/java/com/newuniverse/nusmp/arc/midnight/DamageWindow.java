package com.newuniverse.nusmp.arc.midnight;

import java.util.ArrayDeque;

/**
 * Why: Rades Spirito runs when he is pressed. In the story he made "a tactical retreat through a spatial magic portal" the moment the fight turned against
 * him, and the boss must do the same: when a burst of damage lands in a short window, he opens a gate and leaves. "A burst" means health lost within the
 * last few seconds, which needs a sliding window of the hits; this pure class keeps it (tick and amount of each hit, old ones dropped), so the rule can be
 * tuned and tested without a game and the boss class stays free of bookkeeping.
 */
public final class DamageWindow {
    private final ArrayDeque<long[]> hits = new ArrayDeque<>();
    private static final int MAX_ENTRIES = 256;

    /** Records a hit of {@code amount} (stored in thousandths so a float survives the long) at {@code tick}. */
    public void add(long tick, float amount) {
        if (amount <= 0f) return;
        hits.addLast(new long[]{tick, Math.round(amount * 1000f)});
        while (hits.size() > MAX_ENTRIES) hits.pollFirst();
    }

    /** The total damage of the hits in the last {@code window} ticks up to and including {@code now}. */
    public float sum(long now, int window) {
        long total = 0;
        for (long[] h : hits) if (h[0] > now - window && h[0] <= now) total += h[1];
        return total / 1000f;
    }

    /** Forgets hits older than {@code window} ticks (the boss calls it now and then so the queue stays short). */
    public void prune(long now, int window) {
        while (!hits.isEmpty() && hits.peekFirst()[0] <= now - window) hits.pollFirst();
    }

    public void clear() { hits.clear(); }

    public int size() { return hits.size(); }
}
