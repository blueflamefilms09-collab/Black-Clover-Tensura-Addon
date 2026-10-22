package com.newuniverse.nusmp.compass3d;

import com.newuniverse.nusmp.perf.PerfBudget;
import com.newuniverse.nusmp.perf.VfxQuality;

/**
 * Why: the 3D Compass Magic is about 1100 quads when a compass is drawn whole (rim, dial, rose, needle, glass and the additive glow passes), and an arrow-needle is about 130. One caster is
 * nothing, twenty casters in view with volleys in the air are 20 000 quads and a dozen buffer switches each. Rule 11 of CLAUDE.md asks for budgets, levels of detail and culling; this class
 * is the pure half of them (no Minecraft class, unit-tested; the client half is {@code client.compass.CompassFrame}).
 * <ul>
 *   <li>{@link #level}: by distance from the camera a compass is drawn FULL, MEDIUM (a third of the segments, only the dial lines glow), LOW (the dial disc and a ring card) or CULLED. The
 *       distances are for the effect quality HIGH and shrink with {@link VfxQuality#detail} (0.75 and 0.5 for MEDIUM and LOW).</li>
 *   <li>{@link #behind}: a compass wholly behind the camera is not drawn (props are never culled by the frustum, see {@code MagicPropRenderer}).</li>
 *   <li>{@link #admit}: a per-frame budget of quads ({@link #frameQuads}) shared by every compass, arrow and route of the frame; what does not fit is drawn one level lower, and a compass
 *       nearer than {@link #KEEP_RADIUS} blocks is never dropped (at worst it is drawn LOW).</li>
 * </ul>
 */
public final class CompassLod {
    private CompassLod() {}

    public static final int FULL = 0, MEDIUM = 1, LOW = 2, CULLED = 3;

    /** The distances (blocks, quality HIGH) up to which a level is used: {FULL, MEDIUM, LOW}; further away nothing is drawn. */
    public static final double[] COMPASS = {18.0, 36.0, 56.0}, ARROW = {24.0, 40.0, 64.0}, ROUTE = {24.0, 48.0, 72.0};

    /** A compass this close to the camera is always drawn, whatever the budget says. */
    public static final double KEEP_RADIUS = 6.0;

    /** How far behind the camera plane (blocks) something must lie to be skipped: the size of the biggest thing (a compass with its aura card, scale 1.4 and a margin). */
    public static final double BEHIND_MARGIN = 4.0;

    /** The level for a squared distance. {@code detail} is {@link VfxQuality#detail} (1 HIGH, 0.75 MEDIUM, 0.5 LOW): it scales the bands, so a weak machine switches earlier. */
    public static int level(double distSq, float detail, double[] bands) {
        double k = Math.max(0.25, Math.min(1.0, detail));
        if (distSq <= sq(bands[0] * k)) return FULL;
        if (distSq <= sq(bands[1] * k)) return MEDIUM;
        if (distSq <= sq(bands[2] * k)) return LOW;
        return CULLED;
    }

    private static double sq(double d) { return d * d; }

    /** True when the thing lies wholly behind the camera: {@code dot} is (position - camera) . look direction. */
    public static boolean behind(double dot) { return dot < -BEHIND_MARGIN; }

    /** The quads the frame budget allows all compass work together: a tenth of the effect quality's vertex budget (4000 quads on HIGH, 1400 on MEDIUM, 600 on LOW). */
    public static int frameQuads(VfxQuality q) { return Math.max(200, q.frameVertices / 10); }

    /**
     * The level a compass gets once the frame budget is asked: {@code wanted} if its cost fits, else the next lower ones in turn; a compass within {@link #KEEP_RADIUS} blocks is drawn LOW
     * even when nothing fits; anything else that does not fit is CULLED. {@code cost} is the quads of each level ({@code cost[level]}, three entries); {@code frame} is the frame counter the
     * budget rolls on.
     */
    public static int admit(PerfBudget budget, long frame, int wanted, double distSq, int[] cost) {
        if (wanted >= CULLED) return CULLED;
        for (int l = Math.max(FULL, wanted); l <= LOW; l++) if (budget.tryTake(frame, cost[l])) return l;
        return distSq <= KEEP_RADIUS * KEEP_RADIUS ? LOW : CULLED;
    }

    /** The cost of each level of a whole compass (solid passes plus the additive glow passes plus the cards), for {@link #admit}. */
    public static int[] compassCost(boolean needle, boolean glass) {
        int[] c = new int[3];
        for (int l = 0; l < 3; l++) c[l] = CompassMesh.compassQuads(l, needle, glass) + CompassMesh.glowQuads(l, needle) + (l == 0 ? 6 : l == 1 ? 4 : 4);
        return c;
    }

    /** The cost of each level of an arrow-needle: the model, its additive twin, the streak and the glow card (FULL); the model and the streak (MEDIUM); the streak alone (LOW). */
    public static int[] arrowCost() {
        return new int[]{2 * CompassMesh.arrowQuads(0) + 3, CompassMesh.arrowQuads(1) + 3, 3};
    }

    /** The cost of each level of a route of {@code dashes} dashes: pieces with their twins (FULL), half the dashes without twins (MEDIUM), the streak alone (LOW). */
    public static int[] routeCost(int dashes) {
        int n = Math.max(1, dashes);
        return new int[]{n * 22 + 2, (n / 2 + 1) * 11 + 2, 2};
    }

    /** How many dashes of a route of {@code dashes} are drawn at a level. */
    public static int routeDashes(int dashes, int level) {
        if (level <= FULL) return dashes;
        if (level == MEDIUM) return Math.max(2, dashes / 2);
        return 0;
    }
}
