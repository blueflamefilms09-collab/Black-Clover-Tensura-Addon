package com.newuniverse.nusmp.scalecast;

import com.newuniverse.nusmp.perf.PerfBudget;
import com.newuniverse.nusmp.perf.VfxQuality;

/**
 * Why: the Scale model is about 1400 quads drawn whole (beam, hooks, stand, pans, chains, orbs and the glow pass) and every Scale Magic caster in view has one. Rule 11 of CLAUDE.md asks for
 * budgets, levels of detail and culling; this class is the pure half of them (no Minecraft class, unit-tested; the client half is {@code client.scalecast.ScaleFrame}).
 * <ul>
 *   <li>{@link #level}: by distance from the camera a Scale is drawn FULL, MEDIUM (about a quarter of the quads, no glow pass, no gauge), LOW (a bar, a stand, two discs, three lines) or CULLED. The
 *       distances are for the effect quality HIGH and shrink with {@link VfxQuality#detail} (0.75 and 0.5 for MEDIUM and LOW).</li>
 *   <li>{@link #behind}: a Scale wholly behind the camera is not drawn (props are never culled by the frustum).</li>
 *   <li>{@link #admit}: a per-frame budget of quads ({@link #frameQuads}) shared by every Scale of the frame; what does not fit is drawn one level lower, and a Scale nearer than
 *       {@link #KEEP_RADIUS} blocks is never dropped (at worst it is drawn LOW).</li>
 * </ul>
 */
public final class ScaleLod {
    private ScaleLod() {}

    public static final int FULL = 0, MEDIUM = 1, LOW = 2, CULLED = 3;

    /** The distances (blocks, quality HIGH) up to which a level is used: {FULL, MEDIUM, LOW}; further away nothing is drawn. */
    public static final double[] BANDS = {16.0, 32.0, 48.0};

    /** A Scale this close to the camera is always drawn, whatever the budget says. */
    public static final double KEEP_RADIUS = 5.0;

    /** How far behind the camera plane (blocks) something must lie to be skipped: the size of the biggest thing (the Scale with its halo card) and a margin. */
    public static final double BEHIND_MARGIN = 3.0;

    /** The level for a squared distance. {@code detail} is {@link VfxQuality#detail} (1 HIGH, 0.75 MEDIUM, 0.5 LOW): it scales the bands, so a weak machine switches earlier. */
    public static int level(double distSq, float detail) {
        double k = Math.max(0.25, Math.min(1.0, detail));
        if (distSq <= sq(BANDS[0] * k)) return FULL;
        if (distSq <= sq(BANDS[1] * k)) return MEDIUM;
        if (distSq <= sq(BANDS[2] * k)) return LOW;
        return CULLED;
    }

    private static double sq(double d) { return d * d; }

    /** True when the thing lies wholly behind the camera: {@code dot} is (position - camera) . look direction. */
    public static boolean behind(double dot) { return dot < -BEHIND_MARGIN; }

    /** The quads the frame budget allows all Scales together: a tenth of the effect quality's vertex budget (4000 quads on HIGH, 1400 on MEDIUM, 600 on LOW), never under 200. */
    public static int frameQuads(VfxQuality q) { return Math.max(200, q.frameVertices / 10); }

    /**
     * The level a Scale gets once the frame budget is asked: {@code wanted} if its cost fits, else the next lower ones in turn; a Scale within {@link #KEEP_RADIUS} blocks is drawn LOW even
     * when nothing fits; anything else that does not fit is CULLED. {@code cost} is the quads of each level ({@code cost[level]}, three entries); {@code frame} is the frame counter the budget
     * rolls on.
     */
    public static int admit(PerfBudget budget, long frame, int wanted, double distSq, int[] cost) {
        if (wanted >= CULLED) return CULLED;
        for (int l = Math.max(FULL, wanted); l <= LOW; l++) if (budget.tryTake(frame, cost[l])) return l;
        return distSq <= KEEP_RADIUS * KEEP_RADIUS ? LOW : CULLED;
    }

    /** The cost of each level of a whole Scale: the model with its glow pass plus the additive cards (halo, jewel and pan glows, the weighing line), for {@link #admit}. */
    public static int[] cost() {
        return new int[]{ScaleMesh.quads(0) + 7, ScaleMesh.quads(1) + 4, ScaleMesh.quads(2) + 1};
    }
}
