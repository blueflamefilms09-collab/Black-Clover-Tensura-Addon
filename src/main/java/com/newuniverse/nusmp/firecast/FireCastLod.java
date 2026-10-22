package com.newuniverse.nusmp.firecast;

/**
 * Why: the client cost rules of the fire casting props, pure so they are unit tested and the doc's numbers are the code's. Each Salamander form has three levels of detail (full, half, low poly), a
 * cull distance, and every frame all the forms together share one quad budget: a form that does not fit is drawn at the next lower level, and one that fits at none is not drawn.
 *
 * <p>The quad counts are those of the generated models (tools/gen_salamander_geo.py {@code --count}); a unit test reads the geo files and checks them against {@link #SALAMANDER_QUADS} and
 * {@link #DRAGON_QUADS}, so a model that grew past its budget fails the build. The glow-only model ({@link #SALAMANDER_FX_QUADS} / {@link #DRAGON_FX_QUADS} quads) is drawn on the two nearer levels only.</p>
 *
 * <p>Owner-approved raised limits (the redo of the models after the reference stills): a full form may take up to {@link #MAX_FULL_SALAMANDER} / {@link #MAX_FULL_DRAGON} quads, a half form up to
 * {@link #MAX_HALF_SALAMANDER} / {@link #MAX_HALF_DRAGON}, a low form up to {@link #MAX_LOW}, the shared frame cap is {@link #FRAME_QUADS}, the cull distances are 64 / 112 blocks. The caster's OWN Salamander is
 * never dropped: when the frame budget has no room for it, it is drawn at the low level anyway ({@link #fit(boolean, int, QuadBudget, long, boolean)}), a bounded overshoot of one low form.</p>
 */
public final class FireCastLod {
    private FireCastLod() {}

    public static final int FULL = 0, HALF = 1, LOW = 2, CULL = 3;

    /** Quads of the models, full / half / low (the colour pass; counted from the generated files). */
    public static final int[] SALAMANDER_QUADS = {486, 278, 98}, DRAGON_QUADS = {618, 314, 91};
    /** The glow-only models (the slit eyes, the furnace of the mouth, licks of flame on the wing tips and the tail's barb), drawn additive on the full and half levels. */
    public static final int SALAMANDER_FX_QUADS = 13, DRAGON_FX_QUADS = 17;
    /** The larger of the two glow-only models (kept for the callers that only need a bound). */
    public static final int FX_QUADS = Math.max(SALAMANDER_FX_QUADS, DRAGON_FX_QUADS);
    /** The owner-approved ceilings of the models (a unit test holds the generated files under them). */
    public static final int MAX_FULL_SALAMANDER = 700, MAX_FULL_DRAGON = 900, MAX_HALF_SALAMANDER = 300, MAX_HALF_DRAGON = 380, MAX_LOW = 120;
    /** Distances (blocks, at the best effect quality) up to which each level is used; beyond the last the form is not drawn. */
    public static final double[] SALAMANDER_RANGE = {14.0, 30.0, 64.0}, DRAGON_RANGE = {30.0, 60.0, 112.0};
    /** The spiral and the ball: a hard cap on their sprites and the distance they are drawn to. */
    public static final int SPIRAL_MAX_POINTS = 26, BALL_QUADS = 14;
    public static final double SPIRAL_RANGE = 48.0, BALL_RANGE = 64.0;
    /** The quads all fire casting props together may add to one frame. */
    public static final int FRAME_QUADS = 3200;

    /** The quads one form costs at a level (body and glow-only model); 0 for CULL. */
    public static int cost(boolean dragon, int lod) {
        if (lod >= CULL) return 0;
        int[] body = dragon ? DRAGON_QUADS : SALAMANDER_QUADS;
        return body[lod] + (lod <= HALF ? (dragon ? DRAGON_FX_QUADS : SALAMANDER_FX_QUADS) : 0);
    }

    /**
     * The level for a form at a squared distance: the distance bands scaled by the effect quality (0.5 .. 1: a weaker machine gets the lower levels sooner).
     * @param quality the effect quality's detail factor, clamped to [0.5, 1]
     */
    public static int level(boolean dragon, double distSq, float quality) {
        double q = Math.max(0.5f, Math.min(1f, quality));
        double[] r = dragon ? DRAGON_RANGE : SALAMANDER_RANGE;
        for (int l = 0; l < r.length; l++) {
            double d = r[l] * q;
            if (distSq <= d * d) return l;
        }
        return CULL;
    }

    /** The quads taken in the current frame; the key is anything that differs from frame to frame. Render thread only, no allocation. */
    public static final class QuadBudget {
        private long key = Long.MIN_VALUE;
        private int used;

        public boolean take(long frameKey, int quads, int cap) {
            if (frameKey != key) { key = frameKey; used = 0; }
            if (used + quads > cap) return false;
            used += quads;
            return true;
        }

        /** Takes quads past the cap (the caster's own form, at the low level: at most one such form per caster is ever asked). */
        public void force(long frameKey, int quads) {
            if (frameKey != key) { key = frameKey; used = 0; }
            used += quads;
        }

        public int used() { return used; }
    }

    /** The frame key: the game time and the partial tick together. */
    public static long frameKey(long gameTime, float partial) { return gameTime * 1_000_003L + Float.floatToRawIntBits(partial); }

    /** The level a form is drawn at once the frame budget is asked: its own level if it fits, else the next lower that does, else CULL. */
    public static int fit(boolean dragon, int wanted, QuadBudget budget, long frameKey) {
        return fit(dragon, wanted, budget, frameKey, false);
    }

    /**
     * {@link #fit(boolean, int, QuadBudget, long)} for a form that may be the caster's own: the own form is never dropped, when no level fits the frame budget it is forced in at the low level (a bounded
     * overshoot: one low form per frame for each of the caster's two forms, and the caster has one of them at a time).
     */
    public static int fit(boolean dragon, int wanted, QuadBudget budget, long frameKey, boolean own) {
        for (int l = wanted; l < CULL; l++) if (budget.take(frameKey, cost(dragon, l), FRAME_QUADS)) return l;
        if (own && wanted < CULL) {
            budget.force(frameKey, cost(dragon, LOW));
            return LOW;
        }
        return CULL;
    }

    /** How many sprites of the spiral at a charge level (1..4) and a distance: grows with the level, thinned at range, never above {@link #SPIRAL_MAX_POINTS}. */
    public static int spiralPoints(int level, double distSq) {
        int n = 10 + 4 * Math.max(1, Math.min(FireCastRules.MAX_LEVEL, level));
        if (distSq > 24.0 * 24.0) n /= 2;
        return Math.min(SPIRAL_MAX_POINTS, n);
    }
}
