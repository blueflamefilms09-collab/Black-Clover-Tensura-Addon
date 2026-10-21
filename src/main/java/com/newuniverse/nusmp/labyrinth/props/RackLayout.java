package com.newuniverse.nusmp.labyrinth.props;

/**
 * Why: where the five things on a weapon rack hang, as plain numbers, so the renderer ({@code WeaponRackRenderer}) walks flat arrays every frame and makes no object, and a unit test can pin
 * the arithmetic. The rack is drawn facing SOUTH (its front towards +Z) in block-local space and the renderer turns it by the block's facing. Slots 0..2 are the BAR: three weapons hung from
 * the top rail, side by side; slots 3..4 are the SHELF: two smaller pieces (a helm, a breastplate, a shield) standing on the lower plank. The poses are chosen so that no two slots overlap
 * and every item stays inside the frame of the model. The level of detail follows the distance to the camera: all five slots near, three in the middle, two far, nothing beyond
 * {@link #VIEW_DISTANCE} (the block entity renderer's view distance), which is what keeps a hall of racks cheap.
 */
public final class RackLayout {
    private RackLayout() {}

    public static final int SLOTS = 5, BAR = 3;
    /** The renderer does not draw a rack farther than this (blocks). */
    public static final double VIEW_DISTANCE = 40.0;
    /** Slots drawn up to 14 blocks, up to 28 blocks, and beyond that. */
    public static final double NEAR = 14.0, MID = 28.0;
    public static final int NEAR_SLOTS = 5, MID_SLOTS = 3, FAR_SLOTS = 2;

    /** Block-local position of the centre of each item (block units, 0..1 across, 0 at the floor), the scale on top of the FIXED display transform, and the roll in degrees. */
    private static final float[] X = {0.20f, 0.50f, 0.80f, 0.29f, 0.71f};
    private static final float[] Y = {1.15f, 1.15f, 1.15f, 0.60f, 0.60f};
    private static final float[] Z = {0.60f, 0.60f, 0.60f, 0.63f, 0.63f};
    private static final float[] SCALE = {1.30f, 1.30f, 1.30f, 0.80f, 0.80f};
    private static final float[] ROLL = {-5f, 0f, 5f, 0f, 0f};

    /** The tool-like sprites (swords, axes, bows) are drawn diagonally; this many degrees stand them upright on the rail. */
    public static final float DIAGONAL_FIX = 45f;
    /** The height (blocks) the model and the outline reach: the frame is taller than the block it stands in. */
    public static final float FRAME_HEIGHT = 1.625f;

    public static float x(int slot) { return X[slot]; }

    public static float y(int slot) { return Y[slot]; }

    public static float z(int slot) { return Z[slot]; }

    public static float scale(int slot) { return SCALE[slot]; }

    public static float roll(int slot) { return ROLL[slot]; }

    /**
     * Half the width and half the height (blocks) one item covers at its slot, for the overlap test. The FIXED sprite is half a block across at scale 1; a blade stood upright is thin (about a
     * quarter of the sprite), a shelf piece (helm, plate, shield) fills its sprite.
     */
    public static float halfWidth(int slot) { return slot < BAR ? 0.25f * SCALE[slot] * 0.4f : 0.25f * SCALE[slot]; }

    public static float halfHeight(int slot) { return 0.25f * SCALE[slot]; }

    /** How many slots are drawn at a squared distance from the camera (0 = the rack is not drawn). */
    public static int visibleSlots(double distSq) {
        if (distSq >= VIEW_DISTANCE * VIEW_DISTANCE) return 0;
        if (distSq < NEAR * NEAR) return NEAR_SLOTS;
        if (distSq < MID * MID) return MID_SLOTS;
        return FAR_SLOTS;
    }
}
