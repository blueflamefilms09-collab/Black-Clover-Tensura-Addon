package com.newuniverse.nusmp.labyrinth.mega.gen;

import java.util.List;

/**
 * Why: the dungeon must be the same dungeon on every machine and after every restart, because a sector that was half placed when the server
 * stopped is repaired by planning it AGAIN from its saved seed, and {@code /multiverse mega regen} promises the same rooms. So the random numbers
 * come from a generator whose algorithm is fixed right here (SplitMix64, a few lines of integer arithmetic) instead of {@code java.util.Random}
 * or {@code SplittableRandom}, whose exact sequences are not guaranteed across JDK versions, and instead of the game's {@code RandomSource}, which
 * the pure code cannot see. Nothing here reads a clock or a hash code; shuffles are Fisher-Yates over a list in its given order.
 */
public final class Rng {
    private long state;

    public Rng(long seed) { this.state = seed; }

    /** One SplitMix64 round over {@code z}: the mixing function used for seeds and for hashing small integers. */
    public static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** A seed derived from two longs, never equal for swapped arguments (so (seed, 3) and (3, seed) differ). */
    public static long mix(long a, long b) { return mix(mix(a + 0x9E3779B97F4A7C15L) ^ (b * 0xD1B54A32D192ED03L + 0x2545F4914F6CDD1DL)); }

    public long nextLong() {
        state += 0x9E3779B97F4A7C15L;
        return mix(state);
    }

    /** A uniform int in {@code [0, bound)}; {@code bound} must be positive. */
    public int nextInt(int bound) {
        if (bound <= 0) throw new IllegalArgumentException("bound must be positive: " + bound);
        return (int) Long.remainderUnsigned(nextLong() >>> 1, bound);
    }

    /** A uniform double in {@code [0, 1)}. */
    public double nextDouble() { return (nextLong() >>> 11) * 0x1.0p-53; }

    /** Shuffles the list in place (Fisher-Yates). */
    public <T> void shuffle(List<T> list) {
        for (int i = list.size() - 1; i > 0; i--) {
            int j = nextInt(i + 1);
            T t = list.get(i);
            list.set(i, list.get(j));
            list.set(j, t);
        }
    }

    /** A generator seeded from this one's next value and a salt: an independent stream for a sub-task that must not disturb this one. */
    public Rng fork(long salt) { return new Rng(mix(nextLong(), salt)); }
}
