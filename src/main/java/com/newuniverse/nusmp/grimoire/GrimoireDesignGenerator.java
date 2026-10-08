package com.newuniverse.nusmp.grimoire;

/**
 * Procedural grimoire design: for a magic that has no hand-made palette entry in {@link BookPalette}, the cover leather, glow, trim
 * metal and the frame's stud pattern are generated from the magic's name, so every such grimoire looks its own and always the same.
 * Pure (no game classes). The hue is spread with the golden ratio from a stable FNV-1a hash of the name; lightness, saturation, metal
 * and stud pattern come from other bits of the same hash.
 *
 * @param cover   leather RGB
 * @param trim    frame / ornament metal RGB
 * @param glow    glow while the book is in use, RGB
 * @param studs   0..3: how many studs ring the covers (none, corners, corners + top / bottom, a full ring of eight)
 */
public final class GrimoireDesignGenerator {
    public record Design(int cover, int trim, int glow, int studs) {}

    private static final int[] METALS = {BookPalette.GOLD, BookPalette.SILVER, BookPalette.BRONZE, 0xB8734A};
    private static final double GOLDEN = 0.6180339887498949;

    private GrimoireDesignGenerator() {}

    public static Design forMagic(String magic) {
        long h = fnv(magic == null ? "" : magic);
        double hue = unit(h, 1);
        double sat = 0.40 + unit(h, 2) * 0.42;
        double light = 0.26 + unit(h, 3) * 0.24;
        int cover = hsl(hue, sat, light);
        int glow = hsl(frac(hue + 0.02), 0.70 + unit(h, 6) * 0.28, 0.52 + unit(h, 7) * 0.16);
        int trim = METALS[(int) (unit(h, 4) * 4) & 3];
        int studs = (int) (unit(h, 5) * 4) & 3;
        return new Design(cover, trim, glow, studs);
    }

    /** An independent uniform value in [0, 1) for the k-th property of a hash (splitmix64). */
    private static double unit(long h, int k) {
        long z = h + k * 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        z ^= z >>> 31;
        return (z >>> 11) / (double) (1L << 53);
    }

    private static long fnv(String s) {
        long h = 0xcbf29ce484222325L;
        for (int i = 0; i < s.length(); i++) { h ^= s.charAt(i); h *= 0x100000001b3L; }
        h ^= h >>> 29; h *= 0xbf58476d1ce4e5b9L; h ^= h >>> 32;
        return h & 0x7FFFFFFFFFFFFFFFL;
    }

    private static double frac(double v) { return v - Math.floor(v); }

    static int hsl(double h, double s, double l) {
        double c = (1 - Math.abs(2 * l - 1)) * s, x = c * (1 - Math.abs(h * 6 % 2 - 1)), m = l - c / 2, r, g, b;
        int sector = (int) (h * 6) % 6;
        switch (sector) {
            case 0 -> { r = c; g = x; b = 0; }
            case 1 -> { r = x; g = c; b = 0; }
            case 2 -> { r = 0; g = c; b = x; }
            case 3 -> { r = 0; g = x; b = c; }
            case 4 -> { r = x; g = 0; b = c; }
            default -> { r = c; g = 0; b = x; }
        }
        return (Math.round((float) ((r + m) * 255)) << 16) | (Math.round((float) ((g + m) * 255)) << 8) | Math.round((float) ((b + m) * 255));
    }
}
