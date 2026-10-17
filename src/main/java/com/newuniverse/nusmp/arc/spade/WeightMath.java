package com.newuniverse.nusmp.arc.spade;

/**
 * Why: Lucifero's fight is gravity on the scale of a room, and every number of it decides whether the fight is fair: how wide the lanes of the Gravity
 * Comb are and how much floor between them is safe, how hard the Event Horizon drags and whether a player can always walk out of it, how long a kneel lasts
 * against the Balance Law's three-second cap, how many pockets of still air a Kneel Before the King offers. They are plain arithmetic, so they live here
 * and are pinned by tests (the pull never beats walking, the comb always has a gap, a kneel never outlasts the law) instead of being buried in the world
 * code where only a game could check them. Distinct from Dante's {@code GravityMath}: his fields are circles and spheres on marks; the King's are lanes,
 * one great well round himself, and rules about who is on their knees.
 */
public final class WeightMath {
    private WeightMath() {}

    // ---------------------------------------------------------------- the Gravity Comb
    /** Lane width, length and the distance between two lane centres (blocks). */
    public static final double LANE_WIDTH = 3.2, LANE_LENGTH = 24, LANE_SPACING = 7.2;

    /** Lanes in a comb: three, five from 80% power. */
    public static int laneCount(int power) { return power >= 80 ? 5 : 3; }

    /** Lateral offsets of the lane centres from the aim line, centred on 0 ({@code n} lanes {@link #LANE_SPACING} apart). */
    public static double[] laneOffsets(int n) {
        int k = Math.max(1, n);
        double[] out = new double[k];
        for (int i = 0; i < k; i++) out[i] = (i - (k - 1) / 2.0) * LANE_SPACING;
        return out;
    }

    /** Safe floor between two lanes (blocks). */
    public static double laneGap() { return LANE_SPACING - LANE_WIDTH; }

    /** Is a point at {@code lateral} from the aim line and {@code along} it from the comb centre inside one of the {@code n} lanes? */
    public static boolean inComb(double lateral, double along, int n) {
        if (Math.abs(along) > LANE_LENGTH / 2) return false;
        for (double o : laneOffsets(n)) if (Math.abs(lateral - o) <= LANE_WIDTH / 2) return true;
        return false;
    }

    // ---------------------------------------------------------------- the Event Horizon
    /** How far the pull of the great well reaches. */
    public static final double PULL_RADIUS = 30;
    /** A player walks 0.216 blocks a tick. */
    public static final double WALK = 0.216;
    /** The radius of the collapse at the middle of the well (blocks), the ticks before the collapse in which the well pulls, and its strongest pull (blocks per tick). */
    public static final double CORE_RADIUS = 9;
    public static final int PULL_TICKS = 24;
    public static final double PULL_MAX = 0.09;

    /**
     * Blocks per tick the well drags something at {@code dist} blocks from its middle: 0.03 at the rim growing to {@link #PULL_MAX} near the core, nothing
     * beyond {@link #PULL_RADIUS} and nothing within two blocks of the middle (it holds you, it does not shake you). Always far under {@link #WALK}.
     */
    public static double pullSpeed(double dist) {
        if (dist > PULL_RADIUS || dist < 2) return 0;
        double t = 1.0 - Math.max(0, Math.min(1, dist / PULL_RADIUS));
        return 0.03 + (PULL_MAX - 0.03) * t;
    }

    /** Is something at {@code dist} blocks from the middle inside the collapse? */
    public static boolean inCore(double dist) { return dist <= CORE_RADIUS; }

    /**
     * How far from the very middle a player who starts walking straight out the moment the wind-up begins is when the well collapses, with the pull
     * fighting them for the last {@link #PULL_TICKS} ticks at its strongest. The tests pin that this is more than {@link #CORE_RADIUS} at every power,
     * so the one thing the ultimate asks, to walk away from the King, is always possible without sprinting.
     */
    public static double escapeDistance(int windTicks) {
        int free = Math.max(0, windTicks - PULL_TICKS), pulled = Math.min(PULL_TICKS, Math.max(0, windTicks));
        return free * WALK + pulled * (WALK - PULL_MAX);
    }

    // ---------------------------------------------------------------- Flattened Air
    /** Ticks the air stays flattened: 140 at 40%, ten more for each step after. */
    public static int flattenTicks(int power) {
        int stage = power >= 100 ? 4 : power >= 80 ? 3 : power >= 60 ? 2 : power >= 40 ? 1 : 0;
        return 140 + 10 * Math.max(0, stage - 1);
    }

    // ---------------------------------------------------------------- kneeling
    /** The Balance Law's longest hard control on a player, ticks (3 s). */
    public static final int LAW_CAP = 60;

    /** Ticks a foe is made to kneel: the configured seconds, never above the law's cap or the cap handed in (the law's diminishing returns), at least 5. */
    public static int kneelTicks(double configuredSeconds, int allowed) {
        int want = (int) Math.round(Math.max(0.25, configuredSeconds) * 20);
        return Math.max(5, Math.min(Math.min(want, LAW_CAP), Math.max(5, allowed)));
    }

    // ---------------------------------------------------------------- the pockets of still air
    /** Radius of a pocket of still air (blocks). */
    public static final double POCKET_RADIUS = 2.4;

    /** How many pockets a Kneel Before the King offers for {@code foes} foes: one each, at least two, at most five. */
    public static int pocketCount(int foes) { return Math.max(2, Math.min(5, foes)); }

    /** Is the foe at (x, z) inside the pocket at (px, pz)? */
    public static boolean inPocket(double x, double z, double px, double pz) {
        double dx = x - px, dz = z - pz;
        return dx * dx + dz * dz <= POCKET_RADIUS * POCKET_RADIUS;
    }

    // ---------------------------------------------------------------- the six arms
    /** The angles (radians) of {@code count} strikes spread evenly round the circle, starting at {@code phase}. */
    public static double[] armAngles(int count, double phase) {
        int n = Math.max(1, count);
        double[] out = new double[n];
        for (int i = 0; i < n; i++) out[i] = phase + i * Math.PI * 2 / n;
        return out;
    }

    /** Half-angle of one arm's strike, radians (24 degrees) and its reach (blocks). */
    public static final double ARM_HALF = Math.toRadians(24), ARM_REACH = 5.8;

    /** Is a foe at {@code angle} (radians) inside the strike of the arm pointing at {@code arm}, at distance {@code dist}? */
    public static boolean inArm(double angle, double arm, double dist) {
        return dist <= ARM_REACH && CageMath.angularDistance(angle, arm) <= ARM_HALF;
    }
}
