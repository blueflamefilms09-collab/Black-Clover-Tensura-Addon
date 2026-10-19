package com.newuniverse.nusmp.prop;

/**
 * 1.93.0: the pure numbers of {@link WaterPropFlight} (no game classes, unit-tested by WaterRebuildTest): the layout of the prop's param word, the yaw and pitch that face a direction, the
 * stop tick. Param layout: bits 0 to 3 = the painter's variant, bit 4 = follow the owner, bit 5 = bob, bit 6 = hold still, bits 8 to 17 = stop moving after this many ticks.
 */
public final class WaterPropMath {
    private WaterPropMath() {}

    public static final int FOLLOW = 16, BOB = 32, HOLD = 64;

    public static int variant(int param) { return param & 15; }

    /** The param bits for 'stop moving after n ticks' (0 to 1023). */
    public static int stopAfter(int ticks) { return Math.max(0, Math.min(1023, ticks)) << 8; }

    /** The stop tick stored in a param (0 = never). */
    public static int stopTick(int param) { return (param >> 8) & 1023; }

    /** Yaw in degrees (the game's convention: 0 faces +z, 90 faces -x) of a horizontal direction. */
    public static float yawOf(double dx, double dz) { return (float) (Math.atan2(-dx, dz) * 57.29577951308232); }

    /** Pitch in degrees (positive = looking down) of a direction. */
    public static float pitchOf(double dx, double dy, double dz) { return (float) (-Math.atan2(dy, Math.sqrt(dx * dx + dz * dz)) * 57.29577951308232); }

    /** The model centre heights (blocks above the feet at scale 1) of the eight props, by kind name and variant. */
    public static float centre(String kind, int param) {
        int v = variant(param);
        return switch (kind) {
            case "WATER_1" -> 1.125f;
            case "WATER_2", "DARK_1", "LIGHTNING_2" -> 1.0f;
            case "DARK_2" -> v == 0 ? 1.5f : 1.25f;
            case "LIGHTNING_1", "FLAME_2" -> 1.375f;
            default -> 0f;
        };
    }
}
