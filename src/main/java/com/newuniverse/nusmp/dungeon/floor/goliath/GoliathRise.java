package com.newuniverse.nusmp.dungeon.floor.goliath;

/**
 * The numbers of the Goliath's rising (pure, unit tested): who wakes it, how long the seal cracks for, how long the climb takes, how the
 * climb is eased, and how hard the new body shoves players off the seal. The body ({@link GoliathBoss}) plays the cracking and climbs and the renderer sinks the model by the same eased share.
 */
public final class GoliathRise {
    private GoliathRise() {}

    /** Ticks the seal cracks (dust, grinding) before the body appears: three seconds. */
    public static final int CRACK_TICKS = 60;
    /** Ticks the body takes to climb out of the floor, invulnerable and not looking for anyone. */
    public static final int CLIMB_TICKS = 80;
    /** A player this near the seal (blocks along the floor) and this near its height wakes the Goliath. */
    public static final double TRIGGER_RANGE = 18.0, TRIGGER_HEIGHT = 12.0;
    /** The body shoves players within this many blocks of the seal's middle when it appears. */
    public static final double SHOVE_RANGE = 6.5;
    /** The most a shove carries a player (a knockback strength): enough to clear the seal, never enough to hurt. */
    public static final double SHOVE_MAX = 0.9;

    /** Does a (survival) player at this offset from the middle of the seal wake the Goliath? */
    public static boolean triggers(double dx, double dy, double dz) {
        return dx * dx + dz * dz <= TRIGGER_RANGE * TRIGGER_RANGE && Math.abs(dy) <= TRIGGER_HEIGHT;
    }

    /** The knockback strength the appearing body gives someone `dist` blocks from the seal's middle: strongest at the middle, none at {@link #SHOVE_RANGE}. */
    public static double shove(double dist) {
        if (!(dist >= 0.0) || dist >= SHOVE_RANGE) return 0.0;
        return SHOVE_MAX * (1.0 - dist / SHOVE_RANGE) + 0.15;
    }

    /** The share (0 to 1) of the climb done `ticks` ticks into it: slow at first (the stone gives), quick in the middle, slow again at the end. */
    public static float progress(int ticks) {
        if (ticks <= 0) return 0f;
        if (ticks >= CLIMB_TICKS) return 1f;
        float t = ticks / (float) CLIMB_TICKS;
        return t * t * (3f - 2f * t);
    }

    /** How many bits of stone fly from the seal at one step of the cracking, `age` ticks into it (more as the seal gives way). */
    public static int crackBits(int age) {
        int a = Math.max(0, Math.min(CRACK_TICKS, age));
        return 3 + 12 * a / CRACK_TICKS;
    }

    /** The pitch of a grinding sound `age` ticks into the cracking: it rises as the seal gives way, from 0.5 to 0.8. */
    public static float crackPitch(int age) {
        int a = Math.max(0, Math.min(CRACK_TICKS, age));
        return 0.5f + 0.3f * a / CRACK_TICKS;
    }

    /** The synched value the client reads while the body climbs: 1 once it stands (or if it never climbed). */
    public static float clampRise(float rise) { return Float.isNaN(rise) ? 1f : rise < 0f ? 0f : rise > 1f ? 1f : rise; }
}
