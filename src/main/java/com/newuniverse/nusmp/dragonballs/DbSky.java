package com.newuniverse.nusmp.dragonballs;

/**
 * The CLIENT's view of a summoning: how many more ticks the sky stays dark, which tint, and a smoothed 0..1 darkness. Plain numbers, no client class (the packet handler in common code writes it, the
 * client overlay in client/dragonballs reads it). On a dedicated server nothing ever writes it. One client has one sky, so a static is right here.
 */
public final class DbSky {
    private DbSky() {}

    private static int ticksLeft, set;
    private static float level;

    public static void set(int s, int ticks) {
        set = s;
        ticksLeft = ticks;
    }

    /** Once per client tick: counts down and eases the darkness (about 2 seconds in, 2 seconds out). */
    public static void clientTick() {
        if (ticksLeft > 0) ticksLeft--;
        float target = ticksLeft > 0 ? 1f : 0f;
        level += Math.max(-0.025f, Math.min(0.025f, target - level));
    }

    public static float level() { return level; }

    public static int set() { return set; }

    public static void reset() {
        ticksLeft = 0;
        level = 0f;
    }
}
