package com.newuniverse.nusmp.skilltree.item;

/**
 * Why: the drop shimmer of the 3.26.0 look (a rare or better Skill Book lying on the ground gives off a slow, rarity-coloured particle) must stay cheap on a crowded server, so its numbers are
 * a pure, tested rule and the client class {@code client.skilltree.BookDropFx} only applies them. The worst case is fixed by the constants, whatever the world holds: at most
 * {@link #MAX_TRACKED} books are tracked (they are collected from the entity join / leave events, the world is never scanned), each is considered once every {@link #PERIOD_TICKS} ticks and the
 * considerations are staggered by slot, so at most {@link #maxPerTick()} (4) particles are spawned in any one tick; only books within {@link #RANGE} blocks of the player count. No Minecraft type here.
 */
public final class BookShimmerRules {
    private BookShimmerRules() {}

    /** At most this many dropped books are tracked at once (more are simply not shimmered). */
    public static final int MAX_TRACKED = 24;
    /** A book further than this many blocks from the player does not shimmer. */
    public static final double RANGE = 24.0;
    /** A tracked book spawns one particle every this many ticks. */
    public static final int PERIOD_TICKS = 6;
    /** The hard cap of particles spawned in one tick, whatever else holds. */
    public static final int MAX_PER_TICK = 4;
    /** The lowest rank that shimmers: rare (2). */
    public static final int MIN_RANK = 2;
    /** A newly joined item entity has no stack until its data arrives; it is kept this many ticks while it waits. */
    public static final int UNKNOWN_GRACE_TICKS = 40;

    /** The most particles one tick can spawn for {@code tracked} books: {@code min(MAX_PER_TICK, ceil(tracked / PERIOD_TICKS))}. */
    public static int maxPerTick() { return MAX_PER_TICK; }

    public static int particlesThisTick(int tracked) {
        int n = Math.min(Math.max(0, tracked), MAX_TRACKED);
        return Math.min(MAX_PER_TICK, (n + PERIOD_TICKS - 1) / PERIOD_TICKS);
    }

    /** True when the book in {@code slot} (0 based) is due on game tick {@code tick}: the slots are staggered, so the due ones are spread evenly over the period. */
    public static boolean due(long tick, int slot) {
        return Math.floorMod(tick + slot, (long) PERIOD_TICKS) == 0L;
    }

    public static boolean shimmers(int rank) { return rank >= MIN_RANK; }

    public static boolean inRange(double dx, double dy, double dz) { return dx * dx + dy * dy + dz * dz <= RANGE * RANGE; }

    /** The particle colour (0xRRGGBB) of a rank: the rarity name colours of the books (rare blue, epic violet, mythic orange); white below rare. */
    public static int colorOf(int rank) {
        return switch (rank) {
            case 2 -> 0x55AAFF;
            case 3 -> 0xC855FF;
            case 4 -> 0xFFAA2B;
            default -> 0xFFFFFF;
        };
    }
}
