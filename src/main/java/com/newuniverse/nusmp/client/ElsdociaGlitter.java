package com.newuniverse.nusmp.client;

/**
 * 1.29.0: the maths of Elsdocia's in-hand glitter, kept free of Minecraft types so it can be tested. The blade is a 1.25 m longsword: about
 * 0.30 m of hilt and 0.95 m of crystal (docs/elsdocia_rebuild.md). Particles are spawned along the blade (not at one point), at a rate that falls
 * with distance, and capped per client per tick so a crowd of Elsdocia holders can never flood the particle engine.
 */
public final class ElsdociaGlitter {
    private ElsdociaGlitter() {}

    /** Where the crystal begins and ends along the weapon, in metres from the pommel (reference measurement table). */
    public static final float BLADE_START = 0.57f, BLADE_END = 1.25f;
    /** Hard cap of glitter particles one client may spawn per tick, and the farthest holder that glitters. */
    public static final int MAX_PER_TICK = 6;
    public static final double MAX_DISTANCE = 24.0;

    /** How many glitter particles a holder at this squared distance may spawn this tick (0 or 1 for most ticks), given a random roll 0..1. */
    public static int count(double distSq, float roll) {
        if (distSq > MAX_DISTANCE * MAX_DISTANCE) return 0;
        double near = 1.0 - Math.sqrt(distSq) / MAX_DISTANCE;                                // 1 at the holder's feet, 0 at the limit
        double chance = 0.25 + 0.55 * near;
        return roll < chance ? (roll < chance * 0.25 ? 2 : 1) : 0;
    }

    /** A point on the crystal, in metres along the weapon from the pommel, for a roll 0..1 (biased a little toward the point, where it burns white). */
    public static float along(float roll) {
        float t = roll * roll * 0.35f + roll * 0.65f;
        return BLADE_START + (BLADE_END - BLADE_START) * t;
    }

    /** The share of the per-tick budget left after `spent` particles this tick: spawning stops at the cap. */
    public static boolean budgetLeft(int spent) {
        return spent < MAX_PER_TICK;
    }

    /** The tint (0xRRGGBB) of a learned ability's mote, by the soul type of its magic; white for an unknown soul. */
    public static int moteColour(String soulType) {
        if (soulType == null) return 0xFFFFFF;
        return switch (soulType) {
            case "FLAME" -> 0xFF7A33;
            case "WATER" -> 0x4DA6FF;
            case "WIND" -> 0xA6FFD9;
            case "EARTH" -> 0xB8864B;
            case "LIGHT" -> 0xFFF2A0;
            case "DARKNESS" -> 0x8A4DCC;
            case "SPACE" -> 0xC9A0FF;
            case "TIME" -> 0xFFD966;
            case "BATTLE" -> 0xFF5555;
            case "FANTASY" -> 0xFF99DD;
            default -> 0xFFFFFF;
        };
    }
}
