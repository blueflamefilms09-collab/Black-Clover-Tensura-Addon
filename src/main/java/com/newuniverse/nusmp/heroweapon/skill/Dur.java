package com.newuniverse.nusmp.heroweapon.skill;

import java.util.function.DoubleSupplier;

/**
 * Why: many hero and vassal techniques lasted one to three seconds (a buff, a mark, a barrier, a bind, a field, fire), too short to notice in play. Every length that
 * a player expects to last is therefore multiplied by ONE server setting, {@code heroweapon.durationScale} (default 2.0, 0.25 to 6.0; 1.0 gives the old numbers
 * exactly), and every such length goes through this class, so the factor, the clamp and the hard cap of twenty minutes live in one place and a value is never scaled
 * twice. The mechanical timings (a wind-up, a pulse interval, a parry or riposte window, a cooldown, a step gap) never go through it.
 *
 * <p>No game types here, so it is tested alone. Until {@link #source} is given the factor is 1.0, so a plain unit test (and any code that runs before the core is
 * started) sees the old numbers; {@code HeroCore.init} installs the config reader on both sides, so the client's tooltips follow the synced server setting.
 */
public final class Dur {
    private Dur() {}

    /** The default of the setting, and the range the config allows. */
    public static final double DEFAULT = 2.0, MIN = 0.25, MAX = 6.0;
    /** No scaled length is ever longer than twenty minutes. */
    public static final int CAP = 20 * 60 * 20;
    /** The longest a repeating task of a weapon may run after scaling: twenty seconds, the limit the weapons already had. */
    public static final int TASK_CAP = 400;

    private static volatile DoubleSupplier source = () -> 1.0;

    /** Installs the reader of the setting (called once at start-up; null puts the neutral 1.0 back). */
    public static void source(DoubleSupplier s) { source = s == null ? () -> 1.0 : s; }

    /** The factor now in force, always inside {@link #MIN} to {@link #MAX} (a broken reader gives 1.0). */
    public static double factor() {
        try {
            return clampFactor(source.getAsDouble());
        } catch (RuntimeException e) {
            return 1.0;
        }
    }

    /** {@code f} clamped to the allowed range; NaN becomes 1.0. */
    public static double clampFactor(double f) {
        if (Double.isNaN(f)) return 1.0;
        return Math.max(MIN, Math.min(MAX, f));
    }

    /** The pure function: {@code ticks} times {@code factor} (clamped), rounded, at least 1 tick and at most {@link #CAP}; zero or less is returned as it is. */
    public static int scale(int ticks, double factor) {
        if (ticks <= 0) return ticks;
        long v = Math.round(ticks * clampFactor(factor));
        return (int) Math.max(1L, Math.min(CAP, v));
    }

    /** A length in ticks, scaled by the setting. */
    public static int t(int ticks) { return scale(ticks, factor()); }

    /** The same for a task that repeats (a zone, an aura): scaled and then held to {@link #TASK_CAP}. */
    public static int task(int ticks) { return Math.min(TASK_CAP, t(ticks)); }

    /** Fire seconds, scaled by the setting (at least one). */
    public static int seconds(int seconds) { return Math.max(1, (int) Math.ceil(t(seconds * 20) / 20.0)); }

    /** {@code ticks} as the number of seconds a tooltip shows: whole seconds without a decimal point, otherwise one decimal. */
    public static String secs(int ticks) {
        double s = Math.round(ticks / 2.0) / 10.0;
        return s == Math.rint(s) ? Integer.toString((int) s) : Double.toString(s);
    }
}
