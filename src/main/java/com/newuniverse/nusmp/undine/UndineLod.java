package com.newuniverse.nusmp.undine;

/**
 * Why: the client cost rules of the Undine, pure so they are unit tested and the doc's numbers are the code's. The Undine has three levels of detail (full, half, low poly) in each of her two forms
 * (the companion and the full form), a cull distance per form, and every frame all the Undines together share one quad budget: a form that does not fit is drawn at the next lower level, and one that
 * fits at none is not drawn, except the owner's own close one, which is never dropped (it degrades to the lowest level that fits, but it is always drawn).
 *
 * <p>The quad counts are those of the generated models (tools/gen_undine_geo.py {@code --count}); a unit test reads the geo files and checks them against the tables here, so a model that grew
 * past its budget fails the build. A level's cost is the sum of its passes: the body (the cel-shaded figure), the translucent water pass, the soft additive foam pass, and for the full form the
 * wave base.</p>
 */
public final class UndineLod {
    private UndineLod() {}

    public static final int FULL = 0, HALF = 1, LOW = 2, CULL = 3;

    /** Quads of each pass at each level (index = level), counted from the generated files; a pass a level does not draw has 0. */
    public static final int[] BODY_QUADS = {209, 88, 66}, WATER_QUADS = {136, 54, 0}, FX_QUADS = {26, 26, 0}, BASE_QUADS = {42, 30, 0};
    /** The expression card (one quad) drawn over the calm face on the two nearer levels while a face other than calm is shown; counted in the cost always, so the budget is the worst case. */
    public static final int[] FACE_QUADS = {1, 1, 0};
    /** The two soft ripple rings of the full form (two flat quads each, drawn on the two nearer levels only). */
    public static final int RING_QUADS = 4;
    /** The most quads one form may have at each level (the owner's budget: companion ~900 / 400 / 150, full ~1100 / 500 / 150). */
    public static final int[] COMPANION_CAP = {900, 400, 150}, FULL_CAP = {1100, 500, 150};
    /** Distances (blocks, at the best effect quality) up to which each level is used; beyond the last the form is not drawn (64 blocks for the companion, 112 for the full form). */
    public static final double[] COMPANION_RANGE = {16.0, 36.0, 64.0}, FULL_RANGE = {32.0, 64.0, 112.0};
    /** The quads all Undines together may add to one frame. */
    public static final int FRAME_QUADS = 3200;
    /** An owner's own form closer than this (blocks) is never dropped by the budget. */
    public static final double OWNER_PRIORITY_RANGE = 12.0;

    /** The quads one form costs at a level; 0 for CULL. The full form adds the wave base on the two nearer levels. */
    public static int cost(boolean full, int lod) {
        if (lod >= CULL) return 0;
        return BODY_QUADS[lod] + WATER_QUADS[lod] + FX_QUADS[lod] + FACE_QUADS[lod] + (full ? BASE_QUADS[lod] + (lod <= HALF ? RING_QUADS : 0) : 0);
    }

    /**
     * The level for a form at a squared distance: the distance bands scaled by the effect quality (0.5 .. 1: a weaker machine gets the lower levels sooner).
     * @param quality the effect quality's detail factor, clamped to [0.5, 1]
     */
    public static int level(boolean full, double distSq, float quality) {
        double q = Math.max(0.5f, Math.min(1f, quality));
        double[] r = full ? FULL_RANGE : COMPANION_RANGE;
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

        /** Takes the quads if they fit under the cap. */
        public boolean take(long frameKey, int quads, int cap) {
            if (frameKey != key) { key = frameKey; used = 0; }
            if (used + quads > cap) return false;
            used += quads;
            return true;
        }

        /** Takes the quads whatever the cap says (the owner's own form). */
        public void force(long frameKey, int quads) {
            if (frameKey != key) { key = frameKey; used = 0; }
            used += quads;
        }

        public int used() { return used; }
    }

    /** The frame key: the game time and the partial tick together. */
    public static long frameKey(long gameTime, float partial) { return gameTime * 1_000_003L + Float.floatToRawIntBits(partial); }

    /**
     * The level a form is drawn at once the frame budget is asked: its own level if it fits, else the next lower that does, else CULL. A form with {@code priority} (the owner's own, close) is never
     * culled: if no level fits it takes the lowest one anyway.
     */
    public static int fit(boolean full, int wanted, QuadBudget budget, long frameKey, boolean priority) {
        for (int l = wanted; l < CULL; l++) if (budget.take(frameKey, cost(full, l), FRAME_QUADS)) return l;
        if (!priority) return CULL;
        budget.force(frameKey, cost(full, LOW));
        return LOW;
    }

    /** Is a form at this squared distance of its owner's camera close enough to count as the owner's own close one? */
    public static boolean ownerPriority(boolean isOwner, double distSq) { return isOwner && distSq <= OWNER_PRIORITY_RANGE * OWNER_PRIORITY_RANGE; }
}
