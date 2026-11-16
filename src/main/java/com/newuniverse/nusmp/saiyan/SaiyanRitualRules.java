package com.newuniverse.nusmp.saiyan;

/**
 * Why: the Super Saiyan God ritual (Dragon Ball rework, phase 4; the spec's "Battle of Gods" scene) as pure arithmetic: how many helpers stand in the ring, whether the caster is ready, and when the ritual is done.
 * The caster needs the mastered Super Saiyan and a power threshold (Battle Power by default, Tensura Existence Points if the server chooses, owner decision "EP numbers"), and {@code helpers} OTHER players (the spec
 * says five besides the caster) must stand between {@link #MIN_RING} and {@link #MAX_RING} blocks away and sneak for {@code ticks} ticks. Every number has a hard bound, so a ritual can never read more than
 * {@link #MAX_HELPERS} players and never last more than {@link #MAX_TICKS}.
 */
public final class SaiyanRitualRules {
    private SaiyanRitualRules() {}

    public static final double MIN_RING = 2.5, MAX_RING = 7.5;
    public static final int MAX_HELPERS = 8, MAX_TICKS = 600, CHECK_EVERY = 10;

    static int clampInt(int x, int lo, int hi) { return Math.max(lo, Math.min(hi, x)); }

    public static int helpers(int setting) { return clampInt(setting, 1, MAX_HELPERS); }

    public static int ticks(int setting) { return clampInt(setting, 40, MAX_TICKS); }

    /** True if a helper at this horizontal distance from the caster stands in the ring. */
    public static boolean inRing(double dx, double dz) {
        double d = Math.sqrt(dx * dx + dz * dz);
        return d >= MIN_RING && d <= MAX_RING;
    }

    /** What the caster needs before the circle can even start: the power (Battle Power, or Existence Points if {@code useEp}) and the mastered Super Saiyan. */
    public static boolean casterReady(boolean useEp, double bp, double bpNeed, double ep, double epNeed, boolean masteredForm, boolean alreadyDone) {
        if (alreadyDone || !masteredForm) return false;
        return useEp ? ep >= epNeed : bp >= bpNeed;
    }

    /** True when the number of helpers in the ring is enough. */
    public static boolean circleHolds(int inRing, int need) { return inRing >= need; }

    /** True once the ritual has run its whole length. */
    public static boolean done(long now, long started, int ticks) { return now - started >= ticks; }

    /** Percent complete, for the message the caster sees. */
    public static int percent(long now, long started, int ticks) { return (int) Math.max(0, Math.min(100, (now - started) * 100L / Math.max(1, ticks))); }

    /** True on the ticks the circle is checked (every {@link #CHECK_EVERY} ticks, so the work is a tenth of a scan a tick). */
    public static boolean checkTick(long now, long started) { return (now - started) % CHECK_EVERY == 0; }
}
