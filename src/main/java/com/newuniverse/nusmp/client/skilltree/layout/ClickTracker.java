package com.newuniverse.nusmp.client.skilltree.layout;

/**
 * Why: a single click on a node only selects it (SP is scarce and respec is off by default, so a stray click must never spend it), and the fast way to buy is a double click on the
 * node that is already selected. Minecraft's GUI hands a screen raw mouse presses and no double-click event, so the pairing is done here: two clicks on the same target within a short
 * window. As a pure class with a clock handed in, the window and the "different target resets" rule are pinned by tests instead of being a feel of the moment.
 */
public final class ClickTracker {
    /** Two clicks closer together than this (milliseconds) on the same target are a double click. */
    public static final long WINDOW_MS = 320;

    private int lastTarget = Integer.MIN_VALUE;
    private long lastAt = Long.MIN_VALUE / 2;

    /**
     * Records a click on {@code target} (any int id; -1 and below mean "nothing") and returns true when it completes a double click. A double click is consumed: a third click starts
     * a new pair.
     */
    public boolean click(long nowMs, int target) {
        boolean dbl = target >= 0 && target == lastTarget && nowMs - lastAt <= WINDOW_MS;
        if (dbl) {
            lastTarget = Integer.MIN_VALUE;
            return true;
        }
        lastTarget = target;
        lastAt = nowMs;
        return false;
    }
}
