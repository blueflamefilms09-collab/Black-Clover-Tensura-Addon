package com.newuniverse.nusmp.entity.zogratis;

/**
 * The numbers of Lucius's Time Magic, the contract with the Supreme Time Devil Astaroth (pure): how long a time stop may last and
 * what protects against it, when the blows of a stopped second land, how far a rewind goes and how much it may heal, how fast
 * Chrono Acceleration is, and the geometry of a step beyond the speed of light (a line, a width, a stopping point).
 * <p>
 * The rules that keep it fair: a stop is never longer than {@value #HARD_STOP_CAP} ticks (three seconds), costs no damage by itself,
 * is followed by a grace of {@value #GRACE} ticks in which nobody can be stopped again, and every blow of the combo that follows is
 * marked on the ground {@value #MARK_LEAD} ticks before it lands.
 */
public final class ChronoMath {
    private ChronoMath() {}

    /** The longest anyone is held in stopped time. */
    public static final int HARD_STOP_CAP = 60;
    /** After a stop nobody can be stopped for this long (six seconds). */
    public static final int GRACE = 120;
    /** Ticks a blow is marked on the ground before it lands. */
    public static final int MARK_LEAD = 14;
    /** The party's blows needed to break the stop of an ally (the existing TimeStop breaks at 8 health of damage). */
    public static final float BREAK_DAMAGE = 8f;

    // ---------------------------------------------------------------- time stop
    /** Ticks the single-target Chrono Stasis holds: 30 at the opening, 40 from 80% power. */
    public static int haltTicks(int power) { return power >= 80 ? 40 : 30; }

    /** Ticks the Time Stop ultimate holds: 50, 60 (three seconds) at the Gates; never over the hard cap. */
    public static int stopTicks(int power) { return Math.min(HARD_STOP_CAP, power >= 100 ? 60 : 50); }

    /** Most foes held at once. */
    public static final int MAX_HELD = 8;

    /** Number of blows in the Time Stop combo: 2, 3 from 100%. */
    public static int comboBlows(int power) { return power >= 100 ? 3 : 2; }

    /** Ticks after the release at which blow 'k' (0-based) lands: 18, 32, 46 ... (14 apart; the first gives 18 to leave). */
    public static int blowDelay(int k) { return 18 + 14 * Math.max(0, k); }

    /** The tick at which blow 'k' is marked: {@value #MARK_LEAD} before it lands. */
    public static int markDelay(int k) { return Math.max(0, blowDelay(k) - MARK_LEAD); }

    /** Radius of one blow's mark. */
    public static final double BLOW_RADIUS = 2.3;

    // ---------------------------------------------------------------- time reversal
    /** Ticks he rewinds: 60 at the opening, 100 at the Gates. */
    public static int rewindTicks(int power) { return 60 + 40 * Math.max(0, Math.min(80, power - 20)) / 80; }

    /** The most health a rewind returns, as a fraction of his maximum: 6% at 60%, 10% at 100%. */
    public static double rewindHealCap(int power) { return power >= 100 ? 0.10 : power >= 80 ? 0.08 : 0.06; }

    /** Snapshots are taken every this many ticks. */
    public static final int SNAP_EVERY = 2;

    /** The ring is big enough to remember 6 seconds. */
    public static int ringCapacity() { return 120 / SNAP_EVERY + 4; }

    // ---------------------------------------------------------------- acceleration
    public static final int ACCEL_TICKS = 160;
    /** Movement speed bonus while accelerated (multiplied-total modifier). */
    public static final double ACCEL_SPEED = 0.45;
    /** His plain blows hit this much harder while accelerated. */
    public static final float ACCEL_MELEE = 1.2f;

    // ---------------------------------------------------------------- light step
    /** Half-width of the damage line of a step beyond light. */
    public static final double STEP_WIDTH = 1.5;
    /** Longest step. */
    public static final double STEP_MAX = 32;
    /** Ticks a step's line is shown before he moves. */
    public static final int STEP_LEAD = 14;
    /** Steps in a Beyond Light combo: 2, 3 from 80%, 4 at the Gates. */
    public static int comboSteps(int power) { return power >= 100 ? 4 : power >= 80 ? 3 : 2; }

    /** Where a step ends: 'overshoot' blocks past the target on the line from the origin through it, no farther than {@link #STEP_MAX} from the origin. Returns {x, z}. */
    public static double[] stepEnd(double ox, double oz, double tx, double tz, double overshoot) {
        double dx = tx - ox, dz = tz - oz;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 1e-6) return new double[]{ox, oz};
        double want = Math.min(STEP_MAX, len + overshoot);
        return new double[]{ox + dx / len * want, oz + dz / len * want};
    }

    /** Distance from the point (px, pz) to the segment (ax, az)-(bx, bz). */
    public static double distToSegment(double px, double pz, double ax, double az, double bx, double bz) {
        double abx = bx - ax, abz = bz - az;
        double l2 = abx * abx + abz * abz;
        double t = l2 < 1e-9 ? 0 : ((px - ax) * abx + (pz - az) * abz) / l2;
        t = Math.max(0, Math.min(1, t));
        double cx = ax + abx * t - px, cz = az + abz * t - pz;
        return Math.sqrt(cx * cx + cz * cz);
    }

    /** True if (px, pz) is in the damage line of a step from (ax, az) to (bx, bz), with 'margin' for the victim's own width. */
    public static boolean inLine(double px, double pz, double ax, double az, double bx, double bz, double margin) {
        return distToSegment(px, pz, ax, az, bx, bz) <= STEP_WIDTH + Math.max(0, margin);
    }

    // ---------------------------------------------------------------- the fall of a stopped foe
    /** Is a freeze breakable by an ally: damage taken while stopped reaches {@link #BREAK_DAMAGE}. */
    public static boolean breaks(float damageTaken) { return damageTaken >= BREAK_DAMAGE; }
}
