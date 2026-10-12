package com.newuniverse.nusmp.dungeon.nexus.zone;

/**
 * A tiny deterministic generator (SplitMix64), stateless place hashes and smooth value noise, so the garden of a Golden City is the same
 * on every machine and every run. Only exactly rounded arithmetic is used (no trigonometry, no pow), so no two machines can disagree on a
 * block. Pure.
 */
final class GoldenRng {
    private long state;

    GoldenRng(long seed) { state = seed; }

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

    boolean chance(int percent) { return below(100) < percent; }

    /** 0 (included) .. 1 (excluded) */
    double unit() { return (next() >>> 11) * (1.0 / (1L << 53)); }

    /** The seed of a core cell's garden. */
    static long mix(int cellX, int cellZ) {
        long h = cellX * 0x9E3779B97F4A7C15L ^ cellZ * 0xC2B2AE3D27D4EB4FL ^ 0x7F4A7C15D1B54A33L;
        h ^= h >>> 31;
        h *= 0x94D049BB133111EBL;
        h ^= h >>> 29;
        return h;
    }

    /** A stable hash of a place, 0 .. 2^31-1; the same inputs always give the same answer. */
    static int hash(long seed, int x, int z, int salt) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (z * 0xC2B2AE3D27D4EB4FL) ^ (salt * 0x165667B19E3779F9L);
        h ^= h >>> 29;
        h *= 0xBF58476D1CE4E5B9L;
        h ^= h >>> 32;
        return (int) (h & 0x7FFFFFFFL);
    }

    /** A stable number of a place, 0 (included) .. 1 (excluded). */
    static double unit(long seed, int x, int z, int salt) { return hash(seed, x, z, salt) / 2147483648.0; }

    static double smooth(double t) {
        double c = t < 0 ? 0 : Math.min(t, 1.0);
        return c * c * (3.0 - 2.0 * c);
    }

    /** Smooth value noise, 0 .. 1, on a lattice of `scale` blocks. */
    static double noise(long seed, double x, double z, double scale, int salt) {
        double fx = x / scale, fz = z / scale;
        double x0 = Math.floor(fx), z0 = Math.floor(fz);
        int ix = (int) x0, iz = (int) z0;
        double tx = smooth(fx - x0), tz = smooth(fz - z0);
        double a = unit(seed, ix, iz, salt), b = unit(seed, ix + 1, iz, salt), c = unit(seed, ix, iz + 1, salt), d = unit(seed, ix + 1, iz + 1, salt);
        double top = a + (b - a) * tx, bottom = c + (d - c) * tx;
        return top + (bottom - top) * tz;
    }

    /** Two octaves of {@link #noise}: broad shapes with a little roughness, 0 .. 1. */
    static double fbm(long seed, double x, double z, double scale, int salt) {
        return noise(seed, x, z, scale, salt) * 0.7 + noise(seed, x + 31.7, z - 17.3, scale * 0.45, salt + 101) * 0.3;
    }

    /** A pseudo-angle of the offset (u, v), 0 (included) .. 16 (excluded) round a full turn, from divisions only. */
    static double turn16(int u, int v) {
        double au = Math.abs((double) u), av = Math.abs((double) v);
        double s = au + av;
        if (s == 0) return 0;
        double p = av / s;                                     // 0 on the u axis .. 1 on the v axis
        double q = u >= 0 ? (v >= 0 ? p : 4.0 - p) : (v >= 0 ? 2.0 - p : 2.0 + p);
        return (q * 4.0) % 16.0;
    }
}
