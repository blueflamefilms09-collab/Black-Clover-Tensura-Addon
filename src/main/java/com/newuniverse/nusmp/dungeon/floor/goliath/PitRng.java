package com.newuniverse.nusmp.dungeon.floor.goliath;

/**
 * A tiny deterministic generator (SplitMix64) and a stateless place hash, so the Goliath's Pit of a cell is the same on every machine and
 * every run and its pieces can be tested with fixed seeds. Pure.
 */
final class PitRng {
    private long state;

    PitRng(long seed) { state = seed; }

    long next() {
        long z = (state += 0x9E3779B97F4A7C15L);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** 0 .. n-1 */
    int below(int n) { return (int) ((next() >>> 33) % n); }

    /** lo .. hi, both included */
    int range(int lo, int hi) { return lo + below(hi - lo + 1); }

    /** 0 (included) .. 1 (excluded) */
    double unit() { return (next() >>> 11) * (1.0 / (1L << 53)); }

    boolean chance(int percent) { return below(100) < percent; }

    /** A stable hash of a place, 0 .. 2^31-1; the same inputs always give the same answer. */
    static int hash(long seed, int x, int z, int salt) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL) ^ (salt * 0x165667B19E3779F9L);
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (int) (h & 0x7FFFFFFFL);
    }
}
