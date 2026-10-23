package com.newuniverse.nusmp.book;

/**
 * Why: a grimoire stored which of its family pages are open as ONE int ("Unlocked", bit = 1 << mode), and Java masks a shift by 32, so mode 32 would have aliased mode 0. The "15 spells for every
 * grimoire" project needs more than 32 family pages in one book (the Flame book: 24 old + 9 new = 33), so the mask is now 64 bits: modes 0-31 stay in the old int tag "Unlocked" and modes 32-63 live in a
 * second int tag "Unlocked2" (an old item has no "Unlocked2" = 0, so every save loads unchanged). This class is the pure arithmetic of that 64-bit mask (no Minecraft class, unit tested); the tag access is
 * {@link PageBits}. A mode outside 0-63 is never open and never changes anything.
 */
public final class UnlockMask {
    private UnlockMask() {}

    /** Modes a mask can hold. */
    public static final int BITS = 64;

    public static boolean in(int mode) { return mode >= 0 && mode < BITS; }

    /** The two ints (low = modes 0-31, high = modes 32-63) as one long. */
    public static long join(int lo, int hi) { return (hi & 0xFFFFFFFFL) << 32 | (lo & 0xFFFFFFFFL); }

    public static int lo(long mask) { return (int) mask; }

    public static int hi(long mask) { return (int) (mask >>> 32); }

    public static boolean has(long mask, int mode) { return in(mode) && (mask & (1L << mode)) != 0; }

    public static long with(long mask, int mode) { return in(mode) ? mask | (1L << mode) : mask; }

    public static long without(long mask, int mode) { return in(mode) ? mask & ~(1L << mode) : mask; }

    /** The mask of the first {@code pages} modes (0 pages = nothing, 64 or more = everything). */
    public static long firstN(int pages) {
        if (pages <= 0) return 0L;
        if (pages >= BITS) return -1L;
        return (1L << pages) - 1L;
    }

    /** How many of the first {@code family} modes are set. */
    public static int count(long mask, int family) {
        return Long.bitCount(mask & firstN(Math.min(Math.max(family, 0), BITS)));
    }
}
