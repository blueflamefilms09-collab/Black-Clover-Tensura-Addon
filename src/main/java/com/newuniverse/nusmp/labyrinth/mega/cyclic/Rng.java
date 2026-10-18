package com.newuniverse.nusmp.labyrinth.mega.cyclic;

/**
 * Why: the pass must give the same links for the same sector seed on every machine and after every restart (a sector that was half placed is repaired by
 * planning it again). {@code java.util.Random} and {@code SplittableRandom} do not promise their exact sequences across JDK versions, so this is SplitMix64
 * written out: a few lines of integer arithmetic. It is the same algorithm as the generator's {@code gen.Rng}; it is repeated here only so this package depends
 * on nothing else of the mod.
 */
public final class Rng {
    private long state;

    public Rng(long seed) { this.state = seed; }

    public static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    public long nextLong() {
        state += 0x9E3779B97F4A7C15L;
        return mix(state);
    }

    /** A uniform int in {@code [0, bound)}. */
    public int nextInt(int bound) {
        if (bound <= 0) throw new IllegalArgumentException("bound must be positive: " + bound);
        return (int) Long.remainderUnsigned(nextLong() >>> 1, bound);
    }

    /** A uniform double in {@code [0, 1)}. */
    public double nextDouble() { return (nextLong() >>> 11) * 0x1.0p-53; }
}
