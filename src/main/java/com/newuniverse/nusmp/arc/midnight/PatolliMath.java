package com.newuniverse.nusmp.arc.midnight;

import com.newuniverse.nusmp.entity.zogratis.PowerCurve;

/**
 * Why: every number of Patolli's fight that is not a damage figure (how many swords, how wide a lane, how long a warning, what the Verdict does to a foe
 * who found cover) in one pure class, so the telegraph and the strike read the same constants and a test can prove the fairness rules: a lane that is
 * shown must be leavable in the time it is shown, a flash must only blind those who look at it, and protection must never make a blow worse.
 * The counts rise with the power step (the framework's 20, 40, 60, 80, 100): more swords and more arrows as he draws more of his strength, the Dance of
 * Cuts only at the Gates.
 */
public final class PatolliMath {
    private PatolliMath() {}

    // ---------------------------------------------------------------- counts by power step
    private static int byStage(int power, int... values) { return values[Math.max(0, Math.min(values.length - 1, PowerCurve.stage(power)))]; }

    /** Light swords in one volley. */
    public static int bladeCount(int power) { return byStage(power, 3, 3, 4, 5, 6); }

    /** Orbs of the Heaven-Splitting Flash. */
    public static int orbCount(int power) { return byStage(power, 3, 3, 4, 4, 5); }

    /** Salvos of the Arrows of Judgment. */
    public static int salvoCount(int power) { return byStage(power, 2, 2, 3, 3, 4); }

    /** Swords the Lamp of Avior Gloria drops at each beat. */
    public static int lampBlades(int power) { return byStage(power, 1, 1, 2, 2, 3); }

    /** Cuts of the Dance (the move exists only at the Gates; the count is still defined for every power so a test can read it). */
    public static int danceCuts(int power) { return byStage(power, 3, 3, 4, 4, 5); }

    /** The total spread (radians) of a volley of {@code n} swords: tight for three, wide for six, never more than 1.2. */
    public static double bladeSpread(int n) { return Math.min(1.2, 0.22 * Math.max(0, n - 1)); }

    // ---------------------------------------------------------------- the light sword volley
    /** Blocks a light sword crosses in one tick: faster than anything else in the mod bar the player's own Light Speed; the warning is the lanes, not the flight. */
    public static final double BLADE_SPEED = 4.0, BLADE_RADIUS = 0.7;
    public static final int BLADE_LIFE = 12;
    /** Most light swords of one volley that can hit the same foe. */
    public static final int BLADE_HITS_PER_FOE = 2;
    /** The lanes of the volley follow the target until this many ticks are left, then freeze: the freeze is the cue to step aside. */
    public static final int VOLLEY_LOCK = 8;

    // ---------------------------------------------------------------- the whip, the step, the burst
    public static final double WHIP_REACH = 8.5, WHIP_HALF_ANGLE = Math.toRadians(55);
    /** Ticks between the two lashes; the second lash's direction is drawn the moment the first lands. */
    public static final int WHIP_GAP = 10;
    public static final double STEP_HALF_WIDTH = 1.2, STEP_STRIKE_RADIUS = 3.0, STEP_BEHIND = 2.2;
    public static final double BURST_RADIUS = 6.5;

    // ---------------------------------------------------------------- the flash
    public static final double FLASH_RANGE = 20.0, FLASH_VIEW = Math.toRadians(72);
    public static final int FLASH_BLIND_TICKS = 40;

    // ---------------------------------------------------------------- the orbs
    public static final double ORB_RING = 8.0, ORB_HEIGHT = 3.2, ORB_BEAM_HALF_WIDTH = 0.9;
    /** Ticks between two orbs firing, and the warning each beam's line is drawn before it fires. */
    public static final int ORB_GAP = 7, ORB_WARN = 10;

    // ---------------------------------------------------------------- the shaft
    public static final double SHAFT_HALF_WIDTH = 1.1, SHAFT_LENGTH = 40.0;
    /** The shaft follows the target until this many ticks are left, then freezes. */
    public static final int SHAFT_LOCK = 12;

    // ---------------------------------------------------------------- the arrows and the lamp
    public static final double ARROW_RADIUS = 2.4;
    public static final int ARROW_WARN = 14, ARROW_GAP = 14;
    public static final int LAMP_TICKS = 60, LAMP_EVERY = 6, LAMP_WARN = 8;
    public static final double LAMP_RANGE = 26.0;
    public static final int LAMP_HITS_PER_FOE = 3;

    // ---------------------------------------------------------------- the Dance of Cuts
    public static final double DANCE_RING = 3.4, DANCE_CUT_RADIUS = 2.4;
    public static final int DANCE_GAP = 6;

    // ---------------------------------------------------------------- Heaven's Verdict
    /** A roof or ledge this close above a foe's head shelters them from the falling swords. */
    public static final double COVER_HEIGHT = 4.0;
    /** Radius of the ring that marks each foe, and how high the hanging swords float. */
    public static final double VERDICT_MARK = 1.8, VERDICT_HANG = 7.0;
    /** Foes farther than this from the caster are not marked. */
    public static final double VERDICT_RANGE = 48.0;

    /**
     * The share of the Verdict that a foe takes: a roof near their head cuts it to 35%, a raised shield to 60%, anti-magic in hand to 60%; the cuts add up
     * but never leave less than a fifth of the blow, so protection helps and nothing is a free pass.
     */
    public static float verdictFactor(boolean covered, boolean blocking, boolean antiMagic) {
        double f = 1.0;
        if (covered) f *= 0.35;
        if (blocking) f *= 0.6;
        if (antiMagic) f *= 0.6;
        return (float) Math.max(0.2, f);
    }

    // ---------------------------------------------------------------- overreach (light speed against foresight)
    /** Empty strikes in a row that leave him open, and for how long. */
    public static final int OVERREACH_LIMIT = 3, OVERREACH_OPEN_TICKS = 50;

    // ---------------------------------------------------------------- the open windows
    /** Ticks he is left open when the Verdict is broken. */
    public static final int VERDICT_BROKEN_OPEN_TICKS = 70;
}
