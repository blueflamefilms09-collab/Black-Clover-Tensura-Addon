package com.newuniverse.nusmp.vfx.deck;

/**
 * Why: where the summon happens. The card leaves the deck slot of the worn duel disk (left forearm) and lands on one of its five card zones; the numbers are the ones of the disk model
 * (tools/visual_upgrade/duel_disk/disk.py: arm frame, x outward from the arm centre, yd pixels down the arm from the shoulder pivot, z forward negative) so the effect and the model agree, and a
 * test reads both. Pure: {@link #world} turns an arm-frame point into a block offset from the player's feet for a body yaw. No Minecraft types.
 */
public final class DeckSummonGeometry {
    private DeckSummonGeometry() {}

    /** Hub of the blade fan (arm frame), the angles of the five blades (degrees, + toward the hand) and the distance of a monster zone from the hub (pixels). */
    public static final float HUB_X = 4.25f, HUB_YD = 5.1f, HUB_Z = 0.5f, ZONE_R = 4.5f;
    public static final float[] ANGLES = {-26f, -13f, 0f, 13f, 26f};
    /** Centre of the deck slot (the top of the stack, arm frame). */
    public static final float SLOT_X = 6.0f, SLOT_YD = 8.35f, SLOT_Z = 0.0f;
    /** The left arm pivot in the layer frame of the body (pixels) and the height of the body top above the feet (pixels). */
    public static final float ARM_X = 5f, ARM_YD = 2f, BODY_TOP = 24f;

    /** Number of card zones. */
    public static final int ZONES = 5;

    /** Arm frame point of the monster zone {@code i} (0..4): {x, yd, z}. Out of range indexes wrap. */
    public static float[] zone(int i) {
        int k = Math.floorMod(i, ZONES);
        double a = Math.toRadians(ANGLES[k]);
        return new float[]{HUB_X + 0.2f, HUB_YD + (float) (Math.sin(a) * ZONE_R), HUB_Z - (float) (Math.cos(a) * ZONE_R)};
    }

    /** Arm frame point of the deck slot: {x, yd, z}. */
    public static float[] slot() { return new float[]{SLOT_X, SLOT_YD, SLOT_Z}; }

    /** The hover point above a zone: a little outward and above (arm frame). */
    public static float[] hover(int i) {
        float[] z = zone(i);
        return new float[]{z[0] + 5.0f, z[1] - 3.0f, z[2]};
    }

    /** Block offset from the player's feet of an arm frame point, for the body yaw in degrees (0 = facing +z; the left of the player is +x there). {x, y, z}. */
    public static double[] world(float[] p, float bodyYawDeg) {
        double yaw = Math.toRadians(bodyYawDeg);
        double lx = Math.cos(yaw), lz = Math.sin(yaw);            // the player's left
        double fx = -Math.sin(yaw), fz = Math.cos(yaw);           // forward
        double left = (ARM_X + p[0]) / 16.0, up = (BODY_TOP - ARM_YD - p[1]) / 16.0, fwd = -p[2] / 16.0;
        return new double[]{lx * left + fx * fwd, up, lz * left + fz * fwd};
    }

    /** Colour (0xRRGGBB) of the column and the rim glow for an alignment: 0 good (green and gold), 1 neutral (aqua), 2 bad (red and purple). */
    public static int alignmentColour(int mood) {
        return switch (mood) {
            case 0 -> 0x6BEA86;
            case 1 -> 0x6AD8FF;
            default -> 0xC8284E;
        };
    }

    /** The second colour of the alignment (the rim of a good card is gold, of a bad one purple). */
    public static int alignmentRim(int mood) {
        return switch (mood) {
            case 0 -> 0xFFD870;
            case 1 -> 0xB8F0FF;
            default -> 0x8A3AD8;
        };
    }

    /** Packs the card ordinal (0..21), the zone (0..4) and the alignment (0..2) into a payload seed; {@link #cardOf} {@link #zoneOf} {@link #moodOf} read it back. */
    public static long pack(int card, int zone, int mood) {
        return ((long) (card & 0xFF)) | ((long) (zone & 0xF) << 8) | ((long) (mood & 0x3) << 12) | (1L << 20);
    }

    public static int cardOf(long seed) { return (int) (seed & 0xFF); }

    public static int zoneOf(long seed) { return (int) ((seed >> 8) & 0xF) % ZONES; }

    public static int moodOf(long seed) { return Math.min(2, (int) ((seed >> 12) & 0x3)); }
}
