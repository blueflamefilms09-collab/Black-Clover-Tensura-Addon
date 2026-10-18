package com.newuniverse.nusmp.elite.forge.rules;

/**
 * Why: the minigames must play out IDENTICALLY on the client (which draws them) and on the server (which re-scores what the client reports), so every
 * "random" thing in them (where the target is, how the flow drifts) is a pure function of a seed the server picked, not of a Random object. This is a
 * splitmix64-style mixer: stateless, no allocation, the same on every JVM. Pure Java, no Minecraft classes.
 */
public final class ForgeRng {
    private ForgeRng() {}

    /** The splitmix64 finaliser. */
    public static long mix(long z) {
        z += 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** A hash of a seed and two small coordinates (a purpose and an index). */
    public static long hash(long seed, int a, int b) { return mix(seed ^ mix(((long) a << 32) ^ (b & 0xFFFFFFFFL) ^ 0x632BE59BD9B4E019L)); }

    /** A double in [0, 1) from a seed and two coordinates. */
    public static double unit(long seed, int a, int b) { return (hash(seed, a, b) >>> 11) * 0x1.0p-53; }

    /** A double in [lo, hi). */
    public static double range(long seed, int a, int b, double lo, double hi) { return lo + (hi - lo) * unit(seed, a, b); }
}
