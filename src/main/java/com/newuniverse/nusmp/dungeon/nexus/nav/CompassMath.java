package com.newuniverse.nusmp.dungeon.nexus.nav;

/**
 * The needle of the Anti-Bird Compass (docs/living_dungeon.md section 3), as plain maths for the client item property {@code nusmp:angle}:
 * a number from 0 to 1 that is the turn of the needle round the holder (0 = the target is dead ahead, 0.25 = to the right, increasing
 * clockwise: the order of the 32 sprite frames). Where the target is, how a steady needle swings and settles, how it spins wildly in
 * warped space, and how it idles when there is no target. Pure.
 */
public final class CompassMath {
    private CompassMath() {}

    /** A number wrapped into [0, 1). */
    public static double wrap01(double v) {
        double r = v - Math.floor(v);
        return r >= 1.0 ? 0.0 : r;
    }

    /** The direction from one position to another in turns: atan2(dz, dx) / 2pi (the same measure as vanilla's compass). */
    public static double bearing(double fromX, double fromZ, double toX, double toZ) {
        return Math.atan2(toZ - fromZ, toX - fromX) / (Math.PI * 2);
    }

    /** What the needle has to chase for a holder facing {@code yawDegrees}: it comes to rest at this plus the bearing. */
    public static double wobbleTarget(double yawDegrees) { return 0.5 - (wrap01(yawDegrees / 360.0) - 0.25); }

    /**
     * The needle for a target at {@code bearing} seen by a holder facing {@code yawDegrees}. With a {@code smooth} spring (the local player's
     * own compass) the needle swings and settles, the spring being stepped once per game tick; without one it is exact.
     */
    public static double needle(double yawDegrees, double bearing, NeedleWobble smooth, long gameTime) {
        if (smooth != null) {
            if (smooth.shouldUpdate(gameTime)) smooth.update(gameTime, wobbleTarget(yawDegrees));
            return wrap01(bearing + smooth.rotation());
        }
        return wrap01(wobbleTarget(yawDegrees) + bearing);
    }

    private static double seedTurn(int seed) {
        int h = seed * 0x9E3779B1;
        h ^= h >>> 15;
        h *= 0x85EBCA6B;
        h ^= h >>> 13;
        return (h & 0xFFFFFF) / (double) 0x1000000;
    }

    /** Warped space: the needle runs round several turns a second and jitters, a little differently for each stack (the seed). {@code time} is in ticks, with the partial tick. */
    public static double spin(double time, int seed) {
        double s = seedTurn(seed);
        return wrap01(time * 0.21 + 0.13 * Math.sin(time * 1.7 + s * 6.283185307) + s);
    }

    /** No target (outside the Dungeon, or no life near): the needle searches, turning slowly round, one turn in twenty seconds. */
    public static double idle(double time, int seed) { return wrap01(time / 400.0 + seedTurn(seed)); }
}
