package com.newuniverse.nusmp.arc.midnight;

import com.newuniverse.nusmp.entity.zogratis.PowerCurve;

/**
 * Why: every number of Fana's fight that is not a damage figure, in one pure class, so the telegraph and the strike read the same constants and a test can
 * prove the fairness rules (a mark must be leavable in the time it is shown; a wave of fire is jumped, not outrun, so it must be shown too). The counts
 * rise with the power step; the Evil Heart exists only at the Gates (100%).
 */
public final class FanaMath {
    private FanaMath() {}

    private static int byStage(int power, int... values) { return values[Math.max(0, Math.min(values.length - 1, PowerCurve.stage(power)))]; }

    /** Fireballs in one cast. */
    public static int fireballCount(int power) { return byStage(power, 1, 1, 2, 2, 3); }

    /** Pillars of Ignis Columna. */
    public static int pillarCount(int power) { return byStage(power, 3, 3, 4, 4, 5); }

    /** Embers of the Rain of Embers. */
    public static int emberCount(int power) { return byStage(power, 5, 5, 6, 7, 9); }

    /** Rings of the Crimson Eruption. */
    public static int eruptionWaves(int power) { return byStage(power, 2, 2, 2, 3, 3); }

    // ---------------------------------------------------------------- the fireball
    /** A fireball flies slowly (a swing can meet it); its blast hits everyone within the radius. */
    public static final double FIREBALL_SPEED = 0.85, FIREBALL_RADIUS = 0.9, FIREBALL_BLAST = 3.4, FIREBALL_SPREAD = 0.35;
    public static final int FIREBALL_LIFE = 55;

    // ---------------------------------------------------------------- breath, claw, charge, spiral
    public static final double BREATH_REACH = 13.0, BREATH_HALF_ANGLE = Math.toRadians(28);
    public static final double CLAW_REACH = 6.0, CLAW_HALF_ANGLE = Math.toRadians(50);
    public static final double CHARGE_HALF_WIDTH = 1.4, CHARGE_MAX = 18.0;
    public static final double SPIRAL_LENGTH = 26.0, SPIRAL_HALF_WIDTH = 1.3;
    /** The spiral follows the target until this many ticks are left, then freezes. */
    public static final int SPIRAL_LOCK = 10;

    // ---------------------------------------------------------------- columns, roar, eruption, arms, embers
    public static final double COLUMN_RADIUS = 2.4, ROAR_RADIUS = 10.0, ARMS_RADIUS = 3.2, EMBER_RADIUS = 2.0;
    public static final int EMBER_WARN = 18, EMBER_GAP = 3;
    /** Reach of eruption ring k (0 based), the ticks between rings, and the blocks a ring travels in a tick. */
    public static double eruptionReach(int k) { return 5.0 + 3.2 * k; }
    public static final int ERUPTION_GAP = 10;
    public static final double ERUPTION_SPEED = 1.0, ERUPTION_BAND = 1.4, ERUPTION_JUMP = 0.75;

    /** Ticks a ring of reach {@code reach} takes to run out. */
    public static int eruptionTicks(double reach) { return (int) Math.ceil(reach / ERUPTION_SPEED) + 2; }

    // ---------------------------------------------------------------- the Evil Heart
    /** The Evil Heart is the move of the open Gates; how long she is left open when it is broken, and when it is spent. */
    public static final int HEART_BROKEN_OPEN_TICKS = 100, HEART_SPENT_OPEN_TICKS = 80;
    /** Foes farther than this from the sphere's centre are never hit, whatever the radius. */
    public static final double HEART_REACH = EvilHeart.MAX_RADIUS + 2.0;
}
