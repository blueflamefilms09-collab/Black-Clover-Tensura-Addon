package com.newuniverse.nusmp.arc.world;

import com.newuniverse.nusmp.arc.ArcCharacter;

/**
 * Why: a character of the arcs is built for the floors it is allowed to stand on ({@code floorMin..floorMax}); on the lowest of them its numbers are exactly
 * the ones its builder balanced, and on a deeper floor the dungeon around it is tougher (the monsters scale with the floor), so a boss that stands at the end
 * of floor 40 with the health it had on floor 10 would be the easiest thing on its own floor. The Labyrinth Guardian answers this with a steep curve of its own
 * ({@code GuardianScaling}); an arc boss already has a designed health bar and a power curve, so it only GROWS with the depth, gently and with a hard cap, and
 * never shrinks below its design. Both factors are pure functions of the character and the floor, tested, with the growth per floor in the config.
 */
public final class FloorScale {
    private FloorScale() {}

    /** The most a floor can multiply a boss's health, and its damage, however deep it stands. */
    public static final double MAX_HEALTH_FACTOR = 2.5, MAX_DAMAGE_FACTOR = 1.6;

    /** How many floors the boss stands below the first floor it was designed for (0 when it stands on or above it). */
    public static int floorsBelowDesign(ArcCharacter c, int floor) {
        return Math.max(0, Math.abs(floor) - Math.max(0, c.floorMin()));
    }

    /** The factor for its maximum health: 1 on its first floor, plus {@code perFloor} for every floor below it, capped. */
    public static double healthFactor(ArcCharacter c, int floor, double perFloor) {
        return Math.min(MAX_HEALTH_FACTOR, 1.0 + Math.max(0.0, perFloor) * floorsBelowDesign(c, floor));
    }

    /** The factor for its melee damage, the same shape with its own cap. */
    public static double damageFactor(ArcCharacter c, int floor, double perFloor) {
        return Math.min(MAX_DAMAGE_FACTOR, 1.0 + Math.max(0.0, perFloor) * floorsBelowDesign(c, floor));
    }
}
