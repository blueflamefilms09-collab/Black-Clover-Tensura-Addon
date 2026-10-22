package com.newuniverse.nusmp.spiritlight;

/**
 * Why: the client cost rules of the Spirit of Light, pure so they are unit tested and the doc's numbers are the code's. Each form (the lion cub, the seraph) has three levels of detail (full, half,
 * low poly), a cull distance, and every frame all the spirits together share one quad budget: a form that does not fit is drawn at the next lower level, and one that fits at none is not drawn.
 * The one thing the budget never takes away is the detail of the local player's own spirit when it is close ({@link #OWN_CLOSE} blocks): its quads are counted (so the others give way) but it is
 * always drawn at the level its distance asks for.
 *
 * <p>The quad counts are those of the generated models (tools/gen_spirit_light_geo.py {@code --count}); a unit test reads the geo files and checks them against the tables below, so a model that grew
 * past its budget fails the build. The glow-only model ({@code CUB_FX_QUADS}, {@code SERAPH_FX_QUADS} quads) is drawn on the two nearer levels only.</p>
 */
public final class SpiritLightLod {
    private SpiritLightLod() {}

    public static final int FULL = 0, HALF = 1, LOW = 2, CULL = 3;

    /** Quads of the colour models, full / half / low (counted from the generated files). */
    public static final int[] CUB_QUADS = {229, 183, 84}, SERAPH_QUADS = {215, 155, 56};
    /** The glow-only model drawn additive on the full and half levels (halo, eyes, mane glints / rings, wing eyes, core, prism). */
    public static final int CUB_FX_QUADS = 15, SERAPH_FX_QUADS = 90;
    /** The most quads a form may cost at its full level, colour and glow together (the owner's budget: cub about 600, seraph about 1000). */
    public static final int CUB_MAX_QUADS = 600, SERAPH_MAX_QUADS = 1000;
    /** Distances (blocks, at the best effect quality) up to which each level is used; beyond the last the form is not drawn (cub 64, seraph 112). */
    public static final double[] CUB_RANGE = {16.0, 36.0, 64.0}, SERAPH_RANGE = {32.0, 64.0, 112.0};
    /** A beam and a pulse ring: a few quads each, drawn within these distances. */
    public static final int BEAM_QUADS = 6, PULSE_QUADS = 8;
    public static final double BEAM_RANGE = 64.0, PULSE_RANGE = 64.0;
    /** The quads all Spirit of Light things together may add to one frame (the local player's own close spirit is counted but never refused). */
    public static final int FRAME_QUADS = 3200;
    /** The owner's own spirit within this many blocks is never degraded or culled by the budget. */
    public static final double OWN_CLOSE = 8.0;

    /** The quads one form costs at a level (colour model and glow-only model); 0 for CULL. */
    public static int cost(boolean seraph, int lod) {
        if (lod >= CULL) return 0;
        int[] body = seraph ? SERAPH_QUADS : CUB_QUADS;
        return body[lod] + (lod <= HALF ? (seraph ? SERAPH_FX_QUADS : CUB_FX_QUADS) : 0);
    }

    /** The quads of a form at its full level. */
    public static int fullCost(boolean seraph) { return cost(seraph, FULL); }

    /**
     * The level for a form at a squared distance: the distance bands scaled by the effect quality (0.5 .. 1: a weaker machine gets the lower levels sooner).
     * @param quality the effect quality's detail factor, clamped to [0.5, 1]
     */
    public static int level(boolean seraph, double distSq, float quality) {
        double q = Math.max(0.5f, Math.min(1f, quality));
        double[] r = seraph ? SERAPH_RANGE : CUB_RANGE;
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

        /** Takes {@code quads} if they fit under {@code cap}. */
        public boolean take(long frameKey, int quads, int cap) {
            roll(frameKey);
            if (used + quads > cap) return false;
            used += quads;
            return true;
        }

        /** Counts {@code quads} whatever is left (the owner's own spirit). */
        public void force(long frameKey, int quads) {
            roll(frameKey);
            used += quads;
        }

        public int used() { return used; }
    }

    /** The frame key: the game time and the partial tick together. */
    public static long frameKey(long gameTime, float partial) { return gameTime * 1_000_003L + Float.floatToRawIntBits(partial); }

    /**
     * The level a form is drawn at once the frame budget is asked: its own level if it fits, else the next lower that does, else CULL. The local player's own close spirit ({@code own}) is always
     * drawn at its wanted level and counted against the budget.
     */
    public static int fit(boolean seraph, int wanted, QuadBudget budget, long frameKey, boolean own) {
        if (wanted >= CULL) return CULL;
        if (own) { budget.force(frameKey, cost(seraph, wanted)); return wanted; }
        for (int l = wanted; l < CULL; l++) if (budget.take(frameKey, cost(seraph, l), FRAME_QUADS)) return l;
        return CULL;
    }

    /** Is a spirit the local player's own and close enough to be exempt from the frame budget? */
    public static boolean ownClose(boolean isLocalOwner, double distSq) { return isLocalOwner && distSq <= OWN_CLOSE * OWN_CLOSE; }

    /** May a beam or a pulse ring (a few quads) be drawn this frame? */
    public static boolean fitSmall(QuadBudget budget, long frameKey, int quads) { return budget.take(frameKey, quads, FRAME_QUADS); }
}
