package com.newuniverse.nusmp.boss.vfx;

/**
 * 1.35.0: the pure rules of the boss move effects (no Minecraft types, unit tested). The boss shapes ({@code VfxShape.BOSS_*}) carry whether they
 * are the telegraph or the strike of a move in the lowest bit of the payload's seed, and the server spends from a small per-tick budget so a
 * boss with many marks (or several bosses in one room) can never flood the clients: the client keeps 64 effects at most.
 */
public final class BossVfxRules {
    private BossVfxRules() {}

    /** At most this many boss effects leave the server in one game tick, and at most this many may live at once per boss (the client cap is 64 in all). */
    public static final int PER_TICK = 12, PER_BOSS_ACTIVE = 14;
    /** The longest a telegraph effect lives, however long the wind-up (ticks): a wind-up is re-drawn by the particle tells in any case. */
    public static final int MAX_TELEGRAPH_TICKS = 80;

    /** The seed of a boss effect: the random bits with the lowest bit set for a strike and cleared for a telegraph. */
    public static long seed(long random, boolean strike) { return strike ? (random | 1L) : (random & ~1L); }

    /** True if this seed marks a strike. */
    public static boolean isStrike(long seed) { return (seed & 1L) == 1L; }

    /** A telegraph lives as long as the wind-up it announces, bounded to [6, MAX_TELEGRAPH_TICKS]. */
    public static int telegraphTicks(int windUpTicks) { return Math.max(6, Math.min(MAX_TELEGRAPH_TICKS, windUpTicks)); }

    /** A strike lives a short while, scaled by its size: bigger effects last a little longer; bounded to [10, 40] ticks. */
    public static int strikeTicks(double size) { return (int) Math.max(10, Math.min(40, 12 + size * 1.2)); }

    /** The most a radius may be drawn at (blocks): the pits and basins are 28 blocks across, a bigger number is just a bug. */
    public static final double MAX_RADIUS = 36.0;

    public static float clampRadius(double r) { return (float) Math.max(0.5, Math.min(MAX_RADIUS, r)); }

    /** The per-tick budget: counts what was spent in the current game tick. */
    public static final class Throttle {
        private long tick = Long.MIN_VALUE;
        private int spent;

        /** Spends one; false if this game tick's budget is used up. */
        public boolean allow(long gameTime) {
            if (gameTime != tick) {
                tick = gameTime;
                spent = 0;
            }
            if (spent >= PER_TICK) return false;
            spent++;
            return true;
        }

        public int spent() { return spent; }
    }
}
