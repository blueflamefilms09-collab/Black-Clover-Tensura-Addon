package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import java.util.SplittableRandom;

/**
 * Why: "a per-floor seed (reproducible)": the plan of a floor (which mechanics, in which rooms, with which anchors) must be a pure function of the sector's own seed, so it is
 * the same after a restart, for every player and for the tests, and needs no saved state. Everything random in the planner and in the placement of props and anchors draws from
 * a {@link SplittableRandom} seeded here (java.util.Random would do, but SplittableRandom's sequence is specified, so a JVM update cannot change a floor). Pure.
 */
public final class Seeds {
    private Seeds() {}

    /** A splitmix64 step: a well mixed value from a seed and a salt. */
    public static long mix(long seed, long salt) {
        long z = seed + 0x9E3779B97F4A7C15L * (salt + 1);
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** The seed of one floor of a sector: the sector's seed mixed with the signed floor. */
    public static long floorSeed(long sectorSeed, int floor) { return mix(mix(sectorSeed, 0x4D454348L), floor); }

    /** The seed of one slot (a room's mechanic) of a floor. */
    public static long slotSeed(long floorSeed, int roomId, int slot) { return mix(mix(floorSeed, roomId + 7919L), slot + 31L); }

    public static SplittableRandom rng(long seed) { return new SplittableRandom(seed); }
}
