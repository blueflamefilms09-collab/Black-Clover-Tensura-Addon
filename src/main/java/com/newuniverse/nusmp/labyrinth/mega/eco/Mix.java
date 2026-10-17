package com.newuniverse.nusmp.labyrinth.mega.eco;

/**
 * Why: the native faction of a room is drawn from {@code sectorSeed ^ roomId}, and ids of neighbouring rooms differ only in their low bits.
 * {@code java.util.Random} would turn such seeds into nearly identical first draws (its first output moves by about one part in ten
 * thousand per unit of seed), so every room of a region would land on the same faction. A splitmix64 finalizer spreads one flipped bit over all
 * 64, so adjacent ids give unrelated draws, and the arithmetic is plain integer maths that gives the same answer on every JVM, which keeps a
 * regenerated sector identical to the one that was saved. Pure (no Minecraft types).
 */
final class Mix {
    private Mix() {}

    /** The splitmix64 step: a well-mixed 64-bit value for any input, including 0. */
    static long mix64(long z) {
        z += 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** A uniform double in [0, 1) derived from the seed (53 random bits, the precision of a double). */
    static double unit(long seed) {
        return (mix64(seed) >>> 11) * 0x1.0p-53;
    }
}
