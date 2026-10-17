package com.newuniverse.nusmp.arc.sds;

/**
 * Why: {@link SunClock} says how high Escanor's sun is; this pure class says what that MEANS for the fight, in one place, so the numbers can be
 * read, tuned and tested together. A high sun makes him hit harder, move faster, take less, burn whoever stands close, and unlocks the moves that
 * need daylight; a low sun does the reverse. The power he may draw is the lower of the power his health has reached and the power the sun allows
 * ({@link #powerCap}), which is how the framework's own move gating (each move has a minimum power) becomes "needs daylight" without a second
 * system. When the Gates open the sun cannot set below {@link #GATES_FLOOR}: Rhitta, the axe that stores light, pours out what it kept, so the
 * last quarter of the fight is never a walkover just because the party arrived at midnight.
 */
public final class Sunshine {
    private Sunshine() {}

    /** The lowest Sunshine once the Gates are open (the framework's 100% mode). */
    public static final double GATES_FLOOR = 0.60;
    /** The heat of his body burns those who stand close from this Sunshine up. */
    public static final double AURA_FROM = 0.45;

    /** The Sunshine the fight uses: the clock's, but never under the floor once the Gates are open. */
    public static double effective(double sun, boolean gatesOpen, double gatesFloor) {
        double s = Math.max(0.0, Math.min(1.0, sun));
        return gatesOpen ? Math.max(s, Math.max(0.0, Math.min(1.0, gatesFloor))) : s;
    }

    /** His own damage factor: 0.74 at the darkest hour up to 1.40 at noon (on top of the framework's power tuning). */
    public static double damageScale(double sun) { return 0.60 + 0.80 * clamp(sun); }

    /** The factor on every blow he takes: 1.03 in the dark, 0.72 at noon (the sun shields him; it never makes him immune). */
    public static double taken(double sun) { return 1.10 - 0.38 * clamp(sun); }

    /** A fraction added to his movement speed: up to +10% at noon. */
    public static double speedBonus(double sun) { return 0.10 * clamp(sun); }

    /** The most power the sun lets him draw: 20 at night, 40 at dawn, 60 by mid-morning, 80 in the afternoon sun, 100 at noon (The One). */
    public static int powerCap(double sun) {
        double s = clamp(sun);
        return s < 0.30 ? 20 : s < 0.50 ? 40 : s < 0.72 ? 60 : s < SunClock.THE_ONE ? 80 : 100;
    }

    /** The power the scheduler is shown: what his health has reached, held down by what the sun allows. */
    public static int effectivePower(int power, double sun) { return Math.min(power, powerCap(sun)); }

    /** Radius of the heat round his body (0 when the sun is too low to burn): 4.9 blocks at mid-morning, 6.5 at noon. */
    public static double auraRadius(double sun) { return clamp(sun) < AURA_FROM ? 0.0 : 3.5 + 3.0 * clamp(sun); }

    /** Base damage of one pulse of the heat (every second), before the boss's damage law: 1.7 at mid-morning, 2.8 at noon. */
    public static float auraDamage(double sun) { return (float) (0.8 + 2.0 * clamp(sun)); }

    /** 0..1 brightness for the glow and the boss bar: 0 at the darkest hour, 1 at noon. */
    public static double glow(double sun) { return clamp((sun - SunClock.NIGHT_FLOOR) / (1 - SunClock.NIGHT_FLOOR)); }

    /**
     * The form of the model for a Sunshine, with a little hysteresis so it does not flicker at a threshold: 0 the frail man (below about 0.45),
     * 1 Pride in the daylight (to about 0.9), 2 The One. {@code current} is the form it has now.
     */
    public static int form(int current, double sun) {
        double s = clamp(sun);
        int f = Math.max(0, Math.min(2, current));
        if (f == 0 && s >= 0.47) f = 1;
        if (f == 1 && s >= 0.92) f = 2;
        if (f == 2 && s < 0.88) f = 1;
        if (f == 1 && s < 0.43) f = 0;
        return f;
    }

    private static double clamp(double v) { return Math.max(0.0, Math.min(1.0, v)); }
}
