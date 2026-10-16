package com.newuniverse.nusmp.boss.god;

import java.util.HashMap;
import java.util.Map;

/**
 * Why: Spatial Disorientation swaps Lucius with a player right as they launch a devastating attack, so that the attack "lands on its
 * owner". What the game lets us do reliably is not to steer a spell in flight but to remember <i>who</i> was swapped: for a short window
 * after the swap, a blow from that player on Lucius is absorbed and half of it is returned to the player ({@link CounterRules#swapMirror}).
 * This pure class is that book: who is inside a window and until when, with the window length, so the rule is tested.
 */
public final class ReflectWindow {
    /** Two seconds after the swap: long enough for the spell to resolve, short enough that ordinary blows are not swallowed. */
    public static final int WINDOW_TICKS = 40;

    private final Map<Object, Long> until = new HashMap<>();

    public void open(Object player, long now) {
        until.put(player, now + WINDOW_TICKS);
        if (until.size() > 64) until.values().removeIf(t -> t <= now);
    }

    public boolean reflecting(Object player, long now) {
        Long t = until.get(player);
        return t != null && now < t;
    }

    public void close(Object player) { until.remove(player); }

    public void clear() { until.clear(); }
}
