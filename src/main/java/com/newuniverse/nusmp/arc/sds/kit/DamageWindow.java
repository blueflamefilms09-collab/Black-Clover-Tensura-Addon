package com.newuniverse.nusmp.arc.sds.kit;

import java.util.ArrayDeque;

/**
 * Why: several of the Sins' rules are "how much damage did I take in the last few seconds?" (a burst breaks Merlin's focus, a burst breaks Gowther's
 * nightmare, a burst stops Diane's dance and cracks King's pollen). A sliding window of (tick, amount) pairs answers it with no allocation per tick and no
 * per-boss copy of the same loop. It is pure (a game tick is just a long) so the rule can be tested at any speed. Amounts are floats because damage is.
 */
public final class DamageWindow {
    private final int span;
    private final ArrayDeque<long[]> ticks = new ArrayDeque<>();
    private final ArrayDeque<Float> amounts = new ArrayDeque<>();
    private float sum;

    /** A window of {@code spanTicks} ticks (at least 1). */
    public DamageWindow(int spanTicks) { this.span = Math.max(1, spanTicks); }

    public int span() { return span; }

    private void prune(long now) {
        while (!ticks.isEmpty() && now - ticks.peekFirst()[0] >= span) {
            ticks.pollFirst();
            sum -= amounts.pollFirst();
        }
        if (ticks.isEmpty()) sum = 0f;
    }

    /** Adds a blow and returns what the window holds now, the blow included. Negative or non-finite amounts count as nothing. */
    public float add(long now, float amount) {
        prune(now);
        float a = Float.isFinite(amount) && amount > 0f ? amount : 0f;
        if (a > 0f) {
            ticks.addLast(new long[]{now});
            amounts.addLast(a);
            sum += a;
        }
        return sum;
    }

    /** What the window holds at {@code now}. */
    public float total(long now) {
        prune(now);
        return sum;
    }

    public void clear() {
        ticks.clear();
        amounts.clear();
        sum = 0f;
    }
}
