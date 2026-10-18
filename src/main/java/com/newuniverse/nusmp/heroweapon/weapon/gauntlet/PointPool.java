package com.newuniverse.nusmp.heroweapon.weapon.gauntlet;

/**
 * Why: the Vassal Gauntlet's power-up method in the series is Skill Power-up Points (points are spent to raise a skill's rank, which makes the skill stronger
 * and can add effects). In play that becomes a small pool of skill points that FILLS by fighting (every blow, every technique that lands, every blow the guard
 * takes) and is SPENT by the next technique, which hits harder the more it spends, so a fighter who keeps hitting lands bigger techniques than one who waits.
 * The pool also fades when the fighter stops (a point every second after five idle seconds), so it cannot be banked for ever. This class is only the
 * arithmetic on the game clock, with no game classes, so a test can pin it: the cap, the spend, the fade and the re-anchoring after each change.
 * One instance per wielder; server thread only.
 */
public final class PointPool {
    /** The most points the pool holds. */
    public static final int MAX = 10;
    /** Ticks without a gain or a spend before the points start to fade, and ticks per point faded after that. */
    public static final int IDLE = 100, FADE_EVERY = 20;

    private int stored;
    private long lastActive = Long.MIN_VALUE / 4;

    /** The points held at the game time {@code now}, after the fade. */
    public int points(long now) {
        if (stored <= 0) return 0;
        long idle = Math.max(0L, now - lastActive - IDLE);
        long faded = idle / FADE_EVERY;
        return (int) Math.max(0L, stored - faded);
    }

    /** Adds points (up to {@link #MAX}); a gain counts as activity, so it restarts the idle time. */
    public void gain(int n, long now) {
        if (n <= 0) return;
        stored = Math.min(MAX, points(now) + n);
        lastActive = now;
    }

    /** Spends up to {@code max} points and returns how many were spent (0 when the pool is empty); a spend counts as activity. */
    public int spend(int max, long now) {
        int have = points(now);
        int spent = Math.max(0, Math.min(have, max));
        stored = have - spent;
        lastActive = now;
        return spent;
    }

    public void clear() {
        stored = 0;
        lastActive = Long.MIN_VALUE / 4;
    }
}
