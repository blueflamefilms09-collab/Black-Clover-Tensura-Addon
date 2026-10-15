package com.newuniverse.nusmp.labyrinth.gen;

/**
 * Why: the breadcrumb maths of the Magicule Veins as PURE functions (unit-tested in VeinMathTest, no Minecraft classes): the chance that a
 * masonry block glows, and a deterministic positional roll. The chance is keyed to the STRUCTURE origin (the bottom centre of the Spire's
 * base piece, which Jigsaw hands every processor as the third BlockPos), not to the piece: a side chamber hanging off the fourth ascent
 * gets the density of the fourth ascent, so the veins read as one gradient over the whole spire, thickening up and inward towards the
 * Boss Peak and the Vault, which is the owner's "breadcrumb" brief. The roll hashes (world seed, x, y, z) with SplitMix64 instead of
 * drawing from the piece's RandomSource because Jigsaw re-runs every processor over the WHOLE piece for every chunk the piece touches:
 * a stream draw would depend on the iteration order of that run, while the hash gives the same answer for a block however often it is
 * processed, keeps the other processors' random stream untouched, and makes the pattern reproducible from the seed.
 */
public final class VeinMath {
    private VeinMath() {}

    /** A floor added to every chance so even the rim of the base shows a vein now and then (1 in 500 masonry blocks). */
    public static final double FLOOR = 0.002;
    /** Horizontal distance from the axis at which the inward pull is fully applied (the base piece is 41 wide, side rooms reach ~64). */
    public static final double REACH = 64.0;

    /**
     * Chance in [0,1] that a veinable block at {@code dy} blocks above the structure origin and {@code dxz} blocks from its axis glows:
     * {@code lerp(base, peak, clamp(dy / rise)) * (1 - pull * clamp(dxz / REACH)) + FLOOR}. Below the origin it is the base chance.
     */
    public static double chance(int dy, double dxz, double baseChance, double peakChance, int rise, double pull) {
        double t = rise <= 0 ? 1.0 : clamp01(dy / (double) rise);
        double r = clamp01(dxz / REACH);
        double c = lerp(clamp01(baseChance), clamp01(peakChance), t) * (1.0 - clamp01(pull) * r) + FLOOR;
        return clamp01(c);
    }

    /** SplitMix64 of the seed mixed with the position: every bit of the output depends on every input. */
    public static long hash(long seed, int x, int y, int z) {
        long h = seed ^ (x * 0x9E3779B97F4A7C15L) ^ (y * 0xC2B2AE3D27D4EB4FL) ^ (z * 0x165667B19E3779F9L);
        h += 0x9E3779B97F4A7C15L;
        h = (h ^ (h >>> 30)) * 0xBF58476D1CE4E5B9L;
        h = (h ^ (h >>> 27)) * 0x94D049BB133111EBL;
        return h ^ (h >>> 31);
    }

    /** The hash as a uniform double in [0,1). */
    public static double unit(long seed, int x, int y, int z) {
        return (hash(seed, x, y, z) >>> 11) * 0x1.0p-53;
    }

    /** Does the block at this world position glow? Deterministic in (seed, x, y, z); never true at chance 0, always true at chance 1. */
    public static boolean roll(long seed, int x, int y, int z, double chance) {
        if (!(chance > 0.0)) return false;
        if (chance >= 1.0) return true;
        return unit(seed, x, y, z) < chance;
    }

    static double clamp01(double v) {
        if (v != v) return 0.0;                                                                                // NaN
        return v < 0.0 ? 0.0 : (v > 1.0 ? 1.0 : v);
    }

    static double lerp(double a, double b, double t) { return a + (b - a) * t; }
}
