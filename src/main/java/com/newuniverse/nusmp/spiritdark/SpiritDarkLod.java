package com.newuniverse.nusmp.spiritdark;

/**
 * Why: the client cost rules of the Spirit of Darkness, pure so they are unit tested and the doc's numbers are the code's. Each form has three levels of detail (full, half, low poly), a cull
 * distance, and every frame all the spirits together share one quad budget: a spirit that does not fit is drawn at the next lower level, and one that fits at none is not drawn. The OWNER's own
 * spirit while it is close is never degraded and never culled by the budget (it counts against the budget, so everyone else gives way), so a player always sees their own spirit at full detail.
 *
 * <p>The quad counts are those of the generated models (tools/gen_spirit_dark_geo.py {@code --count}); a unit test reads the geo files and checks them against the tables here, so a model that
 * grew past its budget fails the build. The glow-only model ({@link #FX_QUADS}) is drawn on the two nearer levels only.</p>
 */
public final class SpiritDarkLod {
    private SpiritDarkLod() {}

    public static final int FULL = 0, HALF = 1, LOW = 2, CULL = 3;

    /** Quads of the body models, full / half / low (counted from the generated files). */
    public static final int[] COMPANION_QUADS = {374, 286, 40}, POWERED_QUADS = {637, 347, 109};
    /** The glow-only models (runes, wing seams, smoke wisps, the ring), drawn additive on the full and half levels. */
    public static final int COMPANION_FX_QUADS = 34, POWERED_FX_QUADS = 46;
    /** The face model (the head's plates with the sheet of the mood; drawn on every level) and its glow copy (the two nearer levels): the north faces the body models leave out. */
    public static final int COMPANION_FACE_QUADS = 1, POWERED_FACE_QUADS = 2;
    /** Distances (blocks, at the best effect quality) up to which each level is used; beyond the last the form is not drawn: the companion is culled at 64 blocks, the powered form at 112. */
    public static final double[] COMPANION_RANGE = {16.0, 36.0, 64.0}, POWERED_RANGE = {28.0, 64.0, 112.0};
    /** The quads all spirits together may add to one frame. */
    public static final int FRAME_QUADS = 3200;
    /** The owner's own spirit is exempt from degrading while it is within this many blocks of the camera. */
    public static final double OWNER_PROTECT = 12.0;

    /** The quads one form costs at a level (body and glow-only model); 0 for CULL. */
    public static int cost(boolean powered, int lod) {
        if (lod >= CULL) return 0;
        int[] body = powered ? POWERED_QUADS : COMPANION_QUADS;
        int face = powered ? POWERED_FACE_QUADS : COMPANION_FACE_QUADS;
        return body[lod] + face + (lod <= HALF ? (powered ? POWERED_FX_QUADS : COMPANION_FX_QUADS) + face : 0);
    }

    /**
     * The level for a form at a squared distance: the distance bands scaled by the effect quality (0.5 .. 1: a weaker machine gets the lower levels sooner).
     * @param quality the effect quality's detail factor, clamped to [0.5, 1]
     */
    public static int level(boolean powered, double distSq, float quality) {
        double q = Math.max(0.5f, Math.min(1f, quality));
        double[] r = powered ? POWERED_RANGE : COMPANION_RANGE;
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

        private void roll(long frameKey) {
            if (frameKey != key) { key = frameKey; used = 0; }
        }

        public boolean take(long frameKey, int quads, int cap) {
            roll(frameKey);
            if (used + quads > cap) return false;
            used += quads;
            return true;
        }

        /** Spends quads without asking (the owner's own spirit): it may take the budget past the cap, the others then give way. */
        public void force(long frameKey, int quads) {
            roll(frameKey);
            used += quads;
        }

        public int used() { return used; }
    }

    /** The frame key: the game time and the partial tick together. */
    public static long frameKey(long gameTime, float partial) { return gameTime * 1_000_003L + Float.floatToRawIntBits(partial); }

    /**
     * The level a form is drawn at once the frame budget is asked: its own level if it fits, else the next lower that does, else CULL. The owner's own form within
     * {@link #OWNER_PROTECT} blocks keeps its own level whatever the budget says.
     */
    public static int fit(boolean powered, int wanted, boolean ownerClose, QuadBudget budget, long frameKey) {
        if (wanted >= CULL) return CULL;
        if (ownerClose) {
            budget.force(frameKey, cost(powered, wanted));
            return wanted;
        }
        for (int l = wanted; l < CULL; l++) if (budget.take(frameKey, cost(powered, l), FRAME_QUADS)) return l;
        return CULL;
    }

    /** Does the owner's protection apply at this squared distance? */
    public static boolean ownerClose(boolean isOwner, double distSq) { return isOwner && distSq <= OWNER_PROTECT * OWNER_PROTECT; }

    /** Worst case quads one frame can ever spend: the cap, plus what the owner's own (at most two: companion and powered, hidden companion aside) can force past it. */
    public static int worstCaseFrameQuads() { return FRAME_QUADS + cost(true, FULL) + cost(false, FULL); }
}
