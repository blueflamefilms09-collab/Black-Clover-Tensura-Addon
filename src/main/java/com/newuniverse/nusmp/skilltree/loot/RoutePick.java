package com.newuniverse.nusmp.skilltree.loot;

import com.newuniverse.nusmp.dungeon.sentient.Dice;

/**
 * Why: when a dungeon drop is a route book, WHICH route is the second half of the odds, and the owner wants it weighted by the routes' own {@code loot_weight}
 * (1 is common, a bigger number more common, 0 never) so that a later, rarer magic can be made rarer in its JSON alone, with no code. The pick is a plain
 * weighted choice over an array of integers, with no Minecraft type and no registry, so the glue code builds the array from the route registry and this class can
 * be tested to the last edge: weights of 0 or less are never chosen, an all-zero list (every route switched off, or no route loaded) answers "none" instead of
 * guessing, one dice value maps to exactly one index, and a long run of draws lands on the weights' proportions. The drop adds at most one route book at a time, so
 * "no duplicates within one drop" holds by construction and there is nothing to de-duplicate here.
 */
public final class RoutePick {
    private RoutePick() {}

    /**
     * Picks an index with probability proportional to its weight.
     *
     * @return the index, or -1 when no weight is positive (nothing to pick)
     */
    public static int pick(int[] weights, Dice dice) {
        if (weights == null || dice == null) return -1;
        long total = 0;
        for (int w : weights) if (w > 0) total += w;
        if (total <= 0) return -1;
        long r = Math.min(total - 1, (long) (dice.next() * total));
        int last = -1;
        for (int i = 0; i < weights.length; i++) {
            if (weights[i] <= 0) continue;
            last = i;
            r -= weights[i];
            if (r < 0) return i;
        }
        return last;
    }
}
