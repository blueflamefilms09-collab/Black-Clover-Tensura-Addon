package com.newuniverse.nusmp.heroweapon.curse;

import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * Why: an Overrun is the weapon taking its wielder's hands, and three of its rules are plain arithmetic that decides how dangerous it is to the people standing
 * next to the wielder: how long it lasts (a roll between the configured seconds), who is hit (the NEAREST living thing in reach, ties broken by order so two runs
 * agree), and how hard a blow may land on another player (a share of that player's health, so a friend who steps away survives). Kept apart from the game so
 * each of them is tested with plain numbers. No Minecraft types.
 */
public final class OverrunMath {
    private OverrunMath() {}

    /**
     * The length of an Overrun in ticks: a whole number of seconds from {@code minSeconds} to {@code maxSeconds} inclusive, picked by {@code roll} in [0, 1).
     * Swapped bounds are put in order and a bound below one second is raised to one, so a bad config never makes a zero-length or negative Overrun.
     */
    public static long ticks(int minSeconds, int maxSeconds, double roll) {
        int lo = Math.max(1, Math.min(minSeconds, maxSeconds));
        int hi = Math.max(lo, Math.max(minSeconds, maxSeconds));
        double r = Double.isNaN(roll) ? 0.0 : Math.max(0.0, Math.min(0.999999999, roll));
        int seconds = lo + (int) Math.floor(r * (hi - lo + 1));
        return Math.min(hi, seconds) * 20L;
    }

    /**
     * The item with the smallest squared distance that is not above {@code maxDistSq}; the first of equals wins; null when the list is empty or all are too far.
     */
    public static <T> T nearest(List<T> items, ToDoubleFunction<T> distSq, double maxDistSq) {
        T best = null;
        double bestD = Double.POSITIVE_INFINITY;
        for (T item : items) {
            double d = distSq.applyAsDouble(item);
            if (d <= maxDistSq && d < bestD) {
                best = item;
                bestD = d;
            }
        }
        return best;
    }

    /** A blow on another player is cut to {@code share} of that player's maximum health (the cap is at least 1 health point; a smaller blow is never raised). */
    public static float playerCap(float damage, float targetMaxHealth, double share) {
        float cap = (float) Math.max(1.0, targetMaxHealth * Math.max(0.0, share));
        return Math.max(0f, Math.min(damage, cap));
    }
}
