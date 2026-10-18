package com.newuniverse.nusmp.skilltree.shop;

/**
 * Why: the owner wants the price of a route book to move with "market risk" (2 to 3 million gold), the same for every player of a world, and still the same after a restart.
 * Storing a rolled price per shelf would need migration and could drift from the data file; instead every shelf number is a PURE FUNCTION of four inputs, the world seed, the
 * restock period, a restock counter (bumped by the operator's {@code restock} command), and the line's slot id, so any server thread, any restart and any test rebuild exactly
 * the same shelf. The mixer is SplitMix64, written out here (about six lines) rather than taken from {@code java.util.Random} or {@code SplittableRandom}, so the shelf of a
 * world cannot change when a Java version changes an implementation detail. Prices are rounded to "market" numbers (a trader says 2.35 million, not 2,347,812), the flagship's
 * band floor is honoured exactly through {@link StockEntry#bandLow}, and the result is always inside the line's own band.
 */
public final class StockRoll {
    private StockRoll() {}

    /** What one line rolled to at one restock: whether it is on the shelf, its price (unscaled) and how many copies. */
    public record Roll(boolean present, long price, int count) {}

    private static final long GOLDEN = 0x9E3779B97F4A7C15L;

    /** Rolls one line. {@code slotId} is the shelf slot (the line's id, or line and route for the all-routes line), so two slots of one line roll independently. */
    public static Roll roll(long seed, long period, int epoch, String slotId, StockEntry e) {
        long h = mix(seed ^ mix(period * GOLDEN + epoch) ^ fnv(slotId));
        h = next(h);
        double uAppear = unit(h);
        h = next(h);
        double uPrice = unit(h);
        h = next(h);
        double uCount = unit(h);

        boolean present = e.weight() >= 100 || (e.weight() > 0 && uAppear * 100.0 < e.weight());

        long lo = e.priceMin() + (long) Math.ceil((e.priceMax() - e.priceMin()) * e.bandLow());       // ceil: the roll never lands below the exact band start
        long span = e.priceMax() - lo;
        long raw = lo + (long) Math.floor(span * uPrice);
        long price = Math.min(e.priceMax(), Math.max(ceilNice(lo), nice(raw)));

        int count = e.stockMin() + (int) Math.floor(uCount * (e.stockMax() - e.stockMin() + 1));
        count = Math.min(e.stockMax(), Math.max(e.stockMin(), count));
        return new Roll(present, price, count);
    }

    /** The step prices are rounded to at this size: 1 under 1,000, 10 under 10,000, 100 under 100,000, 1,000 from there (three or four significant digits). */
    public static long step(long v) {
        return v >= 100_000 ? 1_000 : v >= 10_000 ? 100 : v >= 1_000 ? 10 : 1;
    }

    /** {@code v} rounded to the nearest market number (see {@link #step}); idempotent. */
    public static long nice(long v) {
        if (v <= 0) return 0;
        long s = step(v);
        return (v + s / 2) / s * s;
    }

    /** {@code v} rounded UP to the next market number, so a band floor is never rounded below itself. */
    public static long ceilNice(long v) {
        if (v <= 0) return 0;
        long s = step(v);
        return (v + s - 1) / s * s;
    }

    // ---------------------------------------------------------------- SplitMix64
    static long mix(long z) {
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    static long next(long h) {
        return mix(h + GOLDEN);
    }

    /** A number in [0, 1) from the top 53 bits. */
    static double unit(long h) {
        return (h >>> 11) * 0x1.0p-53;
    }

    /** FNV-1a over the characters of the id: a stable 64-bit number for a name. */
    static long fnv(String s) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++) {
            h ^= s.charAt(i);
            h *= 0x100000001b3L;
        }
        return h;
    }
}
