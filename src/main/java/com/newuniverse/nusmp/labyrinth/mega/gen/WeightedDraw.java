package com.newuniverse.nusmp.labyrinth.mega.gen;

/**
 * Why: vanilla Jigsaw picks the next template of a pool by repeating each element {@code weight} times and shuffling the list; the first time an element
 * comes up decides its place in the order, so the chance that a given element is tried FIRST is proportional to its weight, the chance it is second is
 * proportional to its weight among the rest, and so on. This reproduces exactly that distribution without building the repeated list: each call returns
 * an element with probability proportional to its weight among the elements not yet returned (a draw without replacement). The pool's "empty element" is
 * one more slot in the draw, with the weight of the empty elements: when it comes up the primary candidates are over and every later call returns null (the
 * assembler then moves on to the fallback pool, which vanilla would not). {@code ignoreEmpty} drops that slot, for the pools the boss chain forces, where
 * "nothing" is not an answer. The draw is lazy, so a socket that is satisfied by the first candidate costs one random number.
 */
final class WeightedDraw {
    private final PoolEntry[] entries;
    private final int[] weights;
    private int n;
    private int total;
    private final int emptyWeight;
    private final Rng rng;
    private boolean done;

    WeightedDraw(PoolDef pool, boolean ignoreEmpty, Rng rng) {
        this.entries = pool.entries().toArray(new PoolEntry[0]);
        this.n = entries.length;
        this.weights = new int[n];
        int t = 0;
        for (int i = 0; i < n; i++) {
            weights[i] = entries[i].weight();
            t += weights[i];
        }
        this.total = t;
        this.emptyWeight = ignoreEmpty ? 0 : pool.emptyWeight();
        this.rng = rng;
    }

    /** The next element, or null when the elements are used up or the empty element came up. */
    PoolEntry next() {
        if (done) return null;
        int sum = total + emptyWeight;
        if (sum <= 0) {
            done = true;
            return null;
        }
        int r = rng.nextInt(sum);
        if (r >= total) {
            done = true;
            return null;
        }
        int i = 0;
        while (r >= weights[i]) {
            r -= weights[i];
            i++;
        }
        PoolEntry e = entries[i];
        total -= weights[i];
        entries[i] = entries[n - 1];
        weights[i] = weights[n - 1];
        n--;
        return e;
    }
}
