package com.newuniverse.nusmp.gnome;

/**
 * Why: the client cost rules of the Gnome, pure so they are unit tested and the doc's numbers are the code's. Each form has three levels of detail (full, half, low poly), a cull distance, and every
 * frame all the Gnomes together share one quad budget: a Gnome that does not fit is drawn at the next lower level, one that fits at none is not drawn, and the owner's own close companion is never
 * dropped (it is counted, but always drawn).
 *
 * <p>The quad counts are those of the generated models (tools/gen_gnome_geo.py {@code --count}); a unit test ({@code GnomeLodTest}) reads the geo files and checks them against {@link #BODY} and
 * {@link #FX}, so a model that grew past its budget fails the build. The glow-only model is drawn on the two nearer levels only.</p>
 */
public final class GnomeLod {
    private GnomeLod() {}

    public static final int COMPANION = 0, FULL = 1;
    public static final int LOD_FULL = 0, LOD_HALF = 1, LOD_LOW = 2, CULL = 3;

    /** Quads of the colour models by form and level (counted from the generated files). */
    public static final int[][] BODY = {{539, 223, 74}, {541, 228, 74}};
    /** Quads of the glow-only model by form and level (level 2 has none). */
    public static final int[][] FX = {{58, 31, 0}, {78, 35, 0}};
    /** Distances (blocks, at the best effect quality) up to which each level is used; beyond the last the Gnome is not drawn. */
    public static final double[][] RANGE = {{12.0, 28.0, 64.0}, {32.0, 64.0, 112.0}};
    /** The quads all Gnomes together may add to one frame. */
    public static final int FRAME_QUADS = 3200;
    /** The most quads of one Gnome (the doc's budget lines: about 800 for a companion, 1000 for the full form at the best level). */
    public static final int COMPANION_BUDGET = 800, FULL_BUDGET = 1000;

    /** The quads one Gnome costs at a level (body and glow-only model); 0 for CULL. */
    public static int cost(int form, int lod) {
        if (lod >= CULL) return 0;
        return BODY[form][lod] + FX[form][lod];
    }

    /**
     * The level for a Gnome at a squared distance: the distance bands scaled by the effect quality (0.5 .. 1: a weaker machine gets the lower levels sooner).
     * @param quality the effect quality's detail factor, clamped to [0.5, 1]
     */
    public static int level(int form, double distSq, float quality) {
        double q = Math.max(0.5f, Math.min(1f, quality));
        double[] r = RANGE[form];
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

        /** Takes the quads if they fit under the cap. */
        public boolean take(long frameKey, int quads, int cap) {
            roll(frameKey);
            if (used + quads > cap) return false;
            used += quads;
            return true;
        }

        /** Takes the quads whatever the cap says (the owner's own close companion). */
        public void force(long frameKey, int quads) {
            roll(frameKey);
            used += quads;
        }

        public int used() { return used; }
    }

    /** The frame key: the game time and the partial tick together. */
    public static long frameKey(long gameTime, float partial) { return gameTime * 1_000_003L + Float.floatToRawIntBits(partial); }

    /**
     * The level a Gnome is drawn at once the frame budget is asked: its own level if it fits, else the next lower that does, else CULL. {@code owner} true (the viewer's own Gnome, within its first
     * two bands) is always drawn at the wanted level and counted.
     */
    public static int fit(int form, int wanted, QuadBudget budget, long frameKey, boolean owner) {
        if (wanted >= CULL) return CULL;
        if (owner && wanted <= LOD_HALF) {
            budget.force(frameKey, cost(form, wanted));
            return wanted;
        }
        for (int l = wanted; l < CULL; l++) if (budget.take(frameKey, cost(form, l), FRAME_QUADS)) return l;
        return CULL;
    }

    /** The draw scale of a form: the full form's model is authored at about 7.5 blocks and drawn a little smaller (a stone giant of about 6.2 blocks with its crown); the companion at its own size. */
    public static final float FULL_SCALE = 0.82f, COMPANION_SCALE = 1.0f;
}
