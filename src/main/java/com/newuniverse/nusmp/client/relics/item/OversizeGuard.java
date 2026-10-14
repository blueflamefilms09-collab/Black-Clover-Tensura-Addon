package com.newuniverse.nusmp.client.relics.item;

/**
 * The render-time safety net of the oversized-weapon rule (tools/relics/display_presets.py is the rule itself and rewrites the model jsons). A
 * weapon whose json has not been through the tool yet is drawn at vanilla's handheld first-person scale, which sends a long mesh through the
 * camera. WeaponRenderer asks {@link #factor} for the first-person contexts and shrinks the mesh about the point the hand holds. A json the tool
 * did process draws at most {@link #APPARENT_MAX} blocks, below the limit here, so the guard then does nothing.
 */
public final class OversizeGuard {
    private OversizeGuard() {}

    /** Longest apparent first-person length (mesh length times display scale) the tool allows; the same number as display_presets.APPARENT_MAX_BLOCKS. */
    public static final float APPARENT_MAX = 1.0f;
    /** The guard only steps in above this many times {@link #APPARENT_MAX} (a preset's rounding must never trigger it). */
    public static final float TOLERANCE = 1.15f;
    /** The guard never shrinks a weapon below this share. */
    public static final float MIN_FACTOR = 0.4f;
    /** The hand holds the weapon this far up from its butt (display_presets.GRIP_FRACTION). */
    public static final float GRIP_FRACTION = 0.25f;

    /** Multiplier (MIN_FACTOR to 1) for a mesh of {@code lengthBlocks} shown with a display scale of {@code displayScale}. */
    public static float factor(float lengthBlocks, float displayScale) {
        if (!(lengthBlocks > 0f) || !(displayScale > 0f)) return 1f;
        float apparent = lengthBlocks * displayScale;
        float limit = APPARENT_MAX * TOLERANCE;
        if (apparent <= limit) return 1f;
        return Math.max(MIN_FACTOR, limit / apparent);
    }
}
