package com.newuniverse.nusmp.devil.mastery;

/**
 * Why: the numbers of the Devil Awakening skill (docs/devil_mastery.md), with no Minecraft type in them so a unit test and the CI self-test pin them. A devil contract no longer gives its power at once:
 * the skill has a MASTERY (percent) that starts at {@link #START} and is the ceiling of the assimilation level; it grows only by use, with diminishing returns, a daily cap and an anti-AFK factor, so
 * full power takes a long time (default numbers: about 30 hours of active play, see {@link #hoursToMax}).
 *
 * <ul>
 * <li><b>Tiers</b> (mastery 10 / 25 / 50 / 75 / 100): the first art and passive; horns (level 20 can be reached) and the second art; wings and flight and the second passive; the fourth art and third passive;
 * the Devil Union and the union passives.</li>
 * <li><b>The awakening</b> is a timed state ({@link #durationSeconds}) with a cooldown ({@link #cooldownSeconds}) and an aftermath ({@link #aftermathSeconds}): a temporary demon-lord-style awakening.</li>
 * <li><b>Gain</b> is counted in "effective seconds" of use; one percent costs {@link #secondsPerPercent} of them, more as the mastery grows ({@link #SLOWDOWN}).</li>
 * </ul>
 */
public final class MasteryRules {
    private MasteryRules() {}

    public static final float START = 10f, MAX = 100f;
    /** The mastery at which each tier opens. */
    public static final float[] TIER_AT = {10f, 25f, 50f, 75f, 100f};
    public static final int TIERS = TIER_AT.length;

    /** Effective seconds one percent costs at the start, and how much dearer it gets at full mastery (1 + SLOWDOWN times). */
    public static final double BASE_SECONDS_PER_PERCENT = 720.0, SLOWDOWN = 3.0;
    /** Default daily cap, in effective minutes (a real day: UTC). */
    public static final int DEFAULT_DAILY_MINUTES = 360;

    // ---- what each kind of use is worth, in effective seconds (the awakened second itself is worth 1)
    public static final double CAST_HIT = 12.0, CAST_MISS = 2.0, KILL = 10.0, HIT = 1.5, TRIAL_WON = 3.0, TRIAL_IMPRESSED = 1.0;
    /** A kill is only counted when the last counted one is this many ticks old, and the victim has at least this much maximum health (a kill of a chicken is nothing). */
    public static final int KILL_GAP = 80;
    public static final double KILL_MIN_HEALTH = 8.0;
    /** The most effective seconds one use can add (a hand-edited rate can not pass it). */
    public static final double MAX_SINGLE = 120.0;

    /** The lowest a mastery can be (a server may start contracts lower than {@link #START}). */
    public static final float FLOOR = 1f;

    public static float clamp(float m) { return Float.isNaN(m) ? START : Math.max(FLOOR, Math.min(MAX, m)); }

    /** The tier of a mastery: 0 from 10 %, 1 from 25 %, 2 from 50 %, 3 from 75 %, 4 at 100 %. */
    public static int tier(float m) {
        int t = 0;
        for (int i = 1; i < TIERS; i++) if (m >= TIER_AT[i]) t = i;
        return t;
    }

    /** The mastery of the next tier, or -1 at the last. */
    public static float nextAt(float m) {
        for (int i = 0; i < TIERS; i++) if (m < TIER_AT[i]) return TIER_AT[i];
        return -1f;
    }

    /** The highest assimilation level the mastery lets through (100 under the legacy setting). */
    public static float ceiling(float m, boolean legacy) { return legacy ? 100f : clamp(m); }

    // ---------------------------------------------------------------- the curve
    /** Effective seconds one percent costs at this mastery ({@code rate}: the server's gain multiplier, 0.1 to 10). */
    public static double secondsPerPercent(double m, double rate) {
        double t = (Math.max(START, Math.min(MAX, m)) - START) / (MAX - START);
        double r = Math.max(0.1, Math.min(10.0, rate));
        return BASE_SECONDS_PER_PERCENT * (1.0 + SLOWDOWN * t * t) / r;
    }

    /** The mastery after {@code eff} effective seconds of use (never above 100, never lower than before; stepped so a big gain still follows the curve). */
    public static float gain(float m, double eff, double rate) { return gain(m, eff, rate, MAX); }

    /** The same, never above {@code max} (the server's highest mastery). */
    public static float gain(float m, double eff, double rate, float max) {
        double top = Math.max(FLOOR, Math.min(MAX, max)), left = Math.max(0.0, Math.min(MAX_SINGLE, eff)), v = clamp(m);
        while (left > 0 && v < top) {
            double step = Math.min(left, 15.0);
            v += step / secondsPerPercent(v, rate);
            left -= step;
        }
        return clamp((float) Math.min(top, v));
    }

    // ---------------------------------------------------------------- anti-AFK and the daily cap
    /** How much of a second of use counts: 1 when the player did something (dealt or took a blow, cast, killed) in the last minute, 0.2 when they only moved, 0 when they stood still. */
    public static double afkFactor(long ticksSinceAction, boolean moved) {
        if (ticksSinceAction >= 0 && ticksSinceAction <= 1200) return 1.0;
        return moved ? 0.2 : 0.0;
    }

    /** The effective seconds of today that may still be earned. */
    public static double capLeft(double usedToday, int dailyMinutes) { return Math.max(0.0, Math.max(0, dailyMinutes) * 60.0 - Math.max(0.0, usedToday)); }

    // ---------------------------------------------------------------- the awakening
    private static double frac(float m) { return Math.max(0.0, (clamp(m) - START) / (MAX - START)); }

    /** How long one awakening lasts: 90 s at the start, 300 s at full mastery. */
    public static int durationSeconds(float m) { return (int) Math.round(90 + 210 * frac(m)); }

    /** The rest after a full awakening: 300 s at the start, 120 s at full mastery. */
    public static int cooldownSeconds(float m) { return (int) Math.round(300 - 180 * frac(m)); }

    /** The rest after pushing the devil back by hand: a third. */
    public static int withdrawCooldownSeconds(float m) { return Math.max(10, cooldownSeconds(m) / 3); }

    /** The fatigue after an awakening that ended by itself (time or magicules): up to 30 s at full level, less with mastery; none under level 20. */
    public static int aftermathSeconds(float level, float m) {
        if (level < 20f) return 0;
        return (int) Math.round(30.0 * Math.min(1f, level / 100f) * (1.0 - 0.5 * frac(m)));
    }

    /** How strong the arts are: 0.30 at the start, 1.0 at full mastery. */
    public static double artScale(float m) { return 0.30 + 0.70 * (clamp(m) - 0f) / MAX; }

    /** How strong a passive is: 0.35 at the start, 1.0 at full mastery. */
    public static double passiveScale(float m) { return 0.35 + 0.65 * frac(m); }

    /** The tenths of a percent (what the packet carries). */
    public static int tenths(float m) { return Math.round(clamp(m) * 10f); }

    // ---------------------------------------------------------------- a model of play, for the tests and the docs
    /**
     * Real hours to reach 100 % for a player who awakens as soon as the rest is over and plays actively in it ({@code castsPerMin} arts, {@code killsPerMin} kills, {@code hitsPerMin} counted blows per awakened
     * minute), each awakened second worth {@code afk}. Stops at 400 h.
     */
    public static double hoursToMax(double rate, double castsPerMin, double killsPerMin, double hitsPerMin, double afk) {
        float m = START;
        double hours = 0;
        while (m < MAX && hours < 400.0) {
            double d = durationSeconds(m), c = cooldownSeconds(m);
            double eff = (d + d / 60.0 * (castsPerMin * CAST_HIT + killsPerMin * KILL + hitsPerMin * HIT)) * afk;
            double left = eff;
            while (left > 0 && m < MAX) {
                double step = Math.min(left, 15.0);
                m = gain(m, step, rate);
                left -= step;
            }
            hours += (d + c) / 3600.0;
        }
        return hours;
    }
}
