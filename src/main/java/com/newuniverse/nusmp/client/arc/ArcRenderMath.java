package com.newuniverse.nusmp.client.arc;

/**
 * Why: the few numbers the arc renderers compute, kept apart from the game classes so they can be tested: how big a boss is drawn against its hitbox, how many blocks of
 * ground one cycle of a walk clip covers (so the feet do not slide, whatever the build of the character), and when the optional spawn clip plays.
 */
public final class ArcRenderMath {
    private ArcRenderMath() {}

    /** The smallest and largest scale a model is drawn at (a corrupt hitbox must not blow a boss up to the sky or shrink it to nothing). */
    public static final float MIN_SCALE = 0.25f, MAX_SCALE = 4f;
    /** Used when a model has no walk clip to measure. */
    public static final float DEFAULT_WALK_CYCLE = 1.3f, DEFAULT_RUN_CYCLE = 2.2f;

    /**
     * How a boss is scaled: its hitbox height over the height its model was made for ({@code ArcLook.height}). A boss whose hitbox grows with a phase grows on screen;
     * a boss whose hitbox is exactly its model's height is drawn at 1.
     */
    public static float scale(float hitboxHeight, float lookHeight) {
        if (!(lookHeight > 0f) || !(hitboxHeight > 0f)) return 1f;
        return Math.max(MIN_SCALE, Math.min(MAX_SCALE, hitboxHeight / lookHeight));
    }

    /**
     * Blocks of ground one full cycle (two steps) of a leg swing of {@code swingDegrees} covers, for legs whose hip is {@code hipPixels} model pixels above the floor:
     * each step is a chord of 2 L sin(a), the cycle has two of them, and 16 pixels are a block. 0 when either number is not positive.
     */
    public static float strideBlocks(float hipPixels, float swingDegrees) {
        if (!(hipPixels > 0f) || !(swingDegrees > 0f)) return 0f;
        double a = Math.toRadians(Math.min(80f, swingDegrees));
        return (float) (4.0 * hipPixels * Math.sin(a) / 16.0);
    }

    /** The stride to use: the measured one when it is positive, else the default. */
    public static float strideOr(float measured, float fallback) {
        return measured > 0.05f ? measured : fallback;
    }

    /**
     * Seconds into the spawn clip for an entity that has existed for {@code ageTicks} client ticks, or -1 when it does not play: the model has no such clip (length 0),
     * the body is dying, or the clip is over. The clip plays once, when the body first appears.
     */
    public static float spawnSeconds(float ageTicks, float clipSeconds, boolean dead) {
        if (dead || !(clipSeconds > 0f) || ageTicks < 0f) return -1f;
        float s = ageTicks / 20f;
        return s < clipSeconds ? s : -1f;
    }
}
