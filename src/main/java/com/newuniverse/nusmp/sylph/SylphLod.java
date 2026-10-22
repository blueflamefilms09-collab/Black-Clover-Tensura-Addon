package com.newuniverse.nusmp.sylph;

/**
 * Why: the client cost rules of the Sylph, pure so they are unit tested and the doc's numbers are the code's. Each form (the small fairy, the powered form) has three levels of detail (full,
 * half, low poly), a cull distance, and every frame all the Sylphs together share one quad budget: a Sylph that does not fit is drawn at the next lower level, and one that fits at none is not
 * drawn. The owner's own Sylph is never dropped to nothing: it is drawn at least at the lowest level whatever the budget says ({@link #fit}).
 *
 * <p>The quad counts are those of the generated models (tools/gen_sylph_geo.py {@code --count}); {@code SylphLodTest} reads the geo files and checks them against {@link #SMALL_BODY},
 * {@link #POWERED_BODY}, {@link #WINGS_QUADS} and {@link #FX_QUADS}, so a model that grew past its budget fails the build. The glow-only model is drawn on the two nearer levels only; the wings
 * (four plates, or the segments of them) on all.</p>
 *
 * <p>The small fairy is only about half a block tall, so she covers few pixels: her distance bands are shorter than the powered form's (a smaller thing needs its detail only close up), the
 * powered form (about 1.7 blocks tall, 2.8 wide) keeps its detail further out.</p>
 */
public final class SylphLod {
    private SylphLod() {}

    public static final int FULL = 0, HALF = 1, LOW = 2, CULL = 3;
    public static final int SMALL = 0, POWERED = 1;

    /** Quads of the body model (the colour pass) per level, per form; counted from the generated files. */
    public static final int[] SMALL_BODY = {377, 211, 66}, POWERED_BODY = {385, 233, 70};
    /**
     * The wings model (all levels) and the glow-only model, per form. The wings are four dragonfly wings, each a chain of 4 (small) or 5 (powered) cards cut out of one painted wing picture, two
     * faces a card (2 x 4 x 4 = 32 quads), plus for the powered form the six petal wings of the skirt (2 x 5 x 4 + 12 = 52).
     */
    public static final int[] WINGS_QUADS = {32, 52}, FX_QUADS = {29, 25};
    /**
     * The wing sheen: one additive sheet holds this many phases of an iridescent pastel gradient stacked top to bottom; the client shows the phase that the clock asks for and, on the full level,
     * blends it with the next one (two passes), so the sheen sweeps slowly over the membrane. One phase lasts this many ticks (a whole sweep of all phases is about 8 seconds).
     */
    public static final int SHEEN_PHASES = 6;
    public static final float SHEEN_TICKS_PER_PHASE = 27f;
    /** How many times the wings model is drawn per level: the membrane, and the sheen once (half level: the nearest phase) or twice (full level: two phases blended); none at the lowest level. */
    public static int wingPasses(int lod) { return lod <= FULL ? 3 : lod == HALF ? 2 : 1; }
    /** The halo behind the Sylph: one camera-facing square on every level but the lowest. */
    public static final int HALO_QUADS = 2;
    /** Distances (blocks, at the best effect quality) up to which each level is used; beyond the last the form is not drawn (64 blocks small, 112 powered). */
    public static final double[] SMALL_RANGE = {12.0, 28.0, 64.0}, POWERED_RANGE = {32.0, 64.0, 112.0};
    /** The quads all Sylphs together may add to one frame. */
    public static final int FRAME_QUADS = 3200;

    public static int[] body(int form) { return form == POWERED ? POWERED_BODY : SMALL_BODY; }

    /** The quads one Sylph costs at a level (body, the wings once per pass, glow-only model, halo); 0 for CULL. */
    public static int cost(int form, int lod) {
        if (lod >= CULL) return 0;
        return body(form)[lod] + WINGS_QUADS[form] * wingPasses(lod) + (lod <= HALF ? FX_QUADS[form] + HALO_QUADS : 0);
    }

    /**
     * The level for a Sylph at a squared distance: the distance bands scaled by the effect quality (0.5 .. 1: a weaker machine gets the lower levels sooner).
     * @param quality the effect quality's detail factor, clamped to [0.5, 1]
     */
    public static int level(int form, double distSq, float quality) {
        double q = Math.max(0.5f, Math.min(1f, quality));
        double[] r = form == POWERED ? POWERED_RANGE : SMALL_RANGE;
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

        /** Takes {@code quads} if they fit under {@code cap}; false (nothing taken) otherwise. */
        public boolean take(long frameKey, int quads, int cap) {
            if (frameKey != key) { key = frameKey; used = 0; }
            if (used + quads > cap) return false;
            used += quads;
            return true;
        }

        /** Takes {@code quads} even past the cap (the owner's own Sylph). */
        public void force(long frameKey, int quads) {
            if (frameKey != key) { key = frameKey; used = 0; }
            used += quads;
        }

        public int used() { return used; }
    }

    /** The frame key: the game time and the partial tick together. */
    public static long frameKey(long gameTime, float partial) { return gameTime * 1_000_003L + Float.floatToRawIntBits(partial); }

    /**
     * The level a Sylph is drawn at once the frame budget is asked: its own level if it fits, else the next lower that does, else CULL. A Sylph that {@code own} (the viewer's own) is
     * never dropped: when nothing fits it is drawn at the lowest level and the cost is booked anyway.
     */
    public static int fit(int form, int wanted, QuadBudget budget, long frameKey, boolean own) {
        for (int l = wanted; l < CULL; l++) if (budget.take(frameKey, cost(form, l), FRAME_QUADS)) return l;
        if (own && wanted < CULL) { budget.force(frameKey, cost(form, LOW)); return LOW; }
        return CULL;
    }
}
