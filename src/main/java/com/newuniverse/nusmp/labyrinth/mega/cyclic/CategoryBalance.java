package com.newuniverse.nusmp.labyrinth.mega.cyclic;

/**
 * Why: the owner wants the five functional categories (Combat, Puzzles / Hazards, Lore, Transitions, Safe Rooms) balanced, and a loop pass that adds
 * hallways could quietly turn a dungeon into a maze of corridors (hallways ARE Transitions). So the balance is a number the pass can see: a target share per
 * category, the distance of a set of room counts from those targets, and two nudges built on it: a placement that would add a room of an over-represented
 * category scores lower than one that adds an under-represented one, and when the pruner has to drop a dangling room it drops one of the over-represented
 * category first. The targets are the shape of an ordinary floor of the Mega-Dungeon (a third of it circulation, a quarter fighting, a fifth lore, the rest
 * puzzles and rest); they are advice, never a hard rule, because the recipe and the template pools decide which rooms exist at all. Pure.
 */
public final class CategoryBalance {
    private CategoryBalance() {}

    /** Target share of each category, in {@link RoomCategory} order (COMBAT, PUZZLE_HAZARD, LORE, TRANSITION, SAFE); sums to 1. */
    public static final double[] TARGET = {0.24, 0.14, 0.20, 0.34, 0.08};

    /** The shares (0..1) of a count array in {@link RoomCategory} order; all zero for no rooms. */
    public static double[] shares(int[] counts) {
        double total = 0;
        for (int c : counts) total += c;
        double[] s = new double[counts.length];
        if (total > 0) for (int i = 0; i < s.length; i++) s[i] = counts[i] / total;
        return s;
    }

    /** The largest absolute difference between two share arrays: how far one dungeon's mix is from another's. */
    public static double maxShift(double[] a, double[] b) {
        double m = 0;
        for (int i = 0; i < a.length; i++) m = Math.max(m, Math.abs(a[i] - b[i]));
        return m;
    }

    /** The total variation distance (half the sum of absolute differences, 0..1) between the mix and the targets. */
    public static double distanceToTarget(int[] counts) {
        double[] s = shares(counts);
        double d = 0;
        for (int i = 0; i < s.length; i++) d += Math.abs(s[i] - TARGET[i]);
        return d / 2.0;
    }

    /**
     * How much the dungeon wants one more room of this category: the target share minus the current share, in -1..1 (positive = under-represented). With no rooms
     * yet it is the target itself.
     */
    public static double want(RoomCategory c, int[] counts) {
        double total = 0;
        for (int k : counts) total += k;
        double share = total <= 0 ? 0.0 : counts[c.ordinal()] / total;
        return TARGET[c.ordinal()] - share;
    }
}
