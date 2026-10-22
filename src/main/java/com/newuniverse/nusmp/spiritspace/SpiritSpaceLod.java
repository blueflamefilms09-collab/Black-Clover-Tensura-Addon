package com.newuniverse.nusmp.spiritspace;

/**
 * Why: the client cost rules of the Spirit of Space, pure so they are unit tested and the doc's numbers are the code's. Each form (the orb, the wisp) has three levels of detail and a cull distance,
 * and every frame all spirits, wisps and portal rings together share one quad budget: a form that does not fit is drawn at the next lower level, and one that fits at none is not drawn. The owner's own
 * spirit that is close is never degraded by the budget (the budget still counts it, so the others give way), because a spirit that visibly drops its detail beside its owner is the one effect that is
 * noticed.
 *
 * <p>The quad counts come from {@link SpiritSpaceMesh}; a unit test reads the geo files and checks them against the constants there, so a model that grew past its budget fails the build.</p>
 */
public final class SpiritSpaceLod {
    private SpiritSpaceLod() {}

    public static final int FULL = SpiritSpaceMesh.FULL, HALF = SpiritSpaceMesh.HALF, LOW = SpiritSpaceMesh.LOW, CULL = 3;

    /** Distances (blocks, at the best effect quality) up to which each level is used; beyond the last the form is not drawn (cull 64 for the orb, 112 for the wisp). */
    public static final double[] ORB_RANGE = {16.0, 32.0, 64.0}, WISP_RANGE = {32.0, 64.0, 112.0};
    /** One powered form per player, 6 on the server and one linked spirit per player, 48 on the server (the server caps; the doc's worst case multiplies them out). */
    public static final int FRAME_QUADS = 3200;
    /** The portal rings are drawn up to this distance (blocks). */
    public static final double PORTAL_RANGE = 64.0;

    /** The quads one form costs at a level; 0 for CULL. */
    public static int cost(boolean wisp, int lod) {
        if (lod >= CULL) return 0;
        return wisp ? SpiritSpaceMesh.wispQuads(lod) : SpiritSpaceMesh.orbQuads(lod);
    }

    /**
     * The level for a form at a squared distance: the distance bands scaled by the effect quality (0.5 .. 1: a weaker machine gets the lower levels sooner).
     * @param quality the effect quality's detail factor, clamped to [0.5, 1]
     */
    public static int level(boolean wisp, double distSq, float quality) {
        double q = Math.max(0.5f, Math.min(1f, quality));
        double[] r = wisp ? WISP_RANGE : ORB_RANGE;
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

        private void roll(long frameKey) { if (frameKey != key) { key = frameKey; used = 0; } }

        /** Takes {@code quads} if they fit under {@code cap}. */
        public boolean take(long frameKey, int quads, int cap) {
            roll(frameKey);
            if (used + quads > cap) return false;
            used += quads;
            return true;
        }

        /** Takes {@code quads} whatever the cap says (the owner's own close spirit): the others see less room. */
        public void charge(long frameKey, int quads) {
            roll(frameKey);
            used += quads;
        }

        public int used() { return used; }
    }

    /** The frame key: the game time and the partial tick together. */
    public static long frameKey(long gameTime, float partial) { return gameTime * 1_000_003L + Float.floatToRawIntBits(partial); }

    /**
     * The level a form is drawn at once the frame budget is asked: its own level if it fits, else the next lower that does, else CULL. A form that is {@code own} (the viewer's own spirit) and within
     * its half-detail band keeps its level whatever the budget says (and is charged to it).
     */
    public static int fit(boolean wisp, int wanted, boolean own, QuadBudget budget, long frameKey) {
        if (wanted >= CULL) return CULL;
        if (own && wanted <= HALF) { budget.charge(frameKey, cost(wisp, wanted)); return wanted; }
        for (int l = wanted; l < CULL; l++) if (budget.take(frameKey, cost(wisp, l), FRAME_QUADS)) return l;
        return CULL;
    }

    /** The worst case of one frame: the shared budget, plus what the owner's own forms can add above it (one orb or one wisp at full detail). */
    public static int worstFrameQuads() { return FRAME_QUADS + Math.max(cost(true, FULL), cost(false, FULL)); }
}
