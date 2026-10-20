package com.newuniverse.nusmp.labyrinth.mega.decennial.arena;

/**
 * Why: the scatter of a hall (which tombstones lean, where the dead trees stand, which tiles are cracked) must be the same on every server and after every repair, so it never uses
 * {@code java.util.Random} seeded by time: it is a splitmix64 generator seeded from the theme and the size, and every plan builder draws from its own instance in a fixed order.
 */
public final class ArenaRng {
    private long state;

    public ArenaRng(long seed) { this.state = seed * 0x9E3779B97F4A7C15L + 0x1234567L; }

    public static ArenaRng of(String theme, int size) { return new ArenaRng(theme.hashCode() * 31L + size * 7919L); }

    private long next() {
        state += 0x9E3779B97F4A7C15L;
        long z = state;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** An int in [0, n). */
    public int next(int n) { return n <= 1 ? 0 : (int) Math.floorMod(next(), (long) n); }

    /** An int in [lo, hi]. */
    public int range(int lo, int hi) { return lo + next(hi - lo + 1); }

    /** A double in [0, 1). */
    public double d() { return (next() >>> 11) * (1.0 / (1L << 53)); }

    public boolean chance(double p) { return d() < p; }

    public <T> T pick(java.util.List<T> list) { return list.get(next(list.size())); }

    public String pick(String... items) { return items[next(items.length)]; }
}
