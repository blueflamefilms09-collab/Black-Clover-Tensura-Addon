package com.newuniverse.nusmp.saiyan;

/**
 * Why: the numbers of the four Saiyan techniques of 3.71.0 with no Minecraft type, so they are unit tested. DRAGON FIST: a close-range finisher in a few timed steps, unlocked by martial training.
 * GOD BIND: Son Goku holds one foe still for a short time; a boss or a player holds out for less of it. KI DEFLECTION: for a few seconds the Saiyan turns away hostile projectiles near him, a few per pulse.
 * ENERGY TRANSFER: Ki given to an ally player, with a loss, a cap per use and a cool-down. Every count is bounded here, so a technique can never cost more than a fixed amount of work.
 */
public final class SaiyanTech3Rules {
    private SaiyanTech3Rules() {}

    /** The most projectiles one deflection pulse turns (one query, at most this many handled). */
    public static final int MAX_DEFLECTS = 8;
    /** The most steps of a Dragon Fist, whatever the setting says. */
    public static final int MAX_DRAGON_STEPS = 8;
    /** The longest a bind can hold, in ticks (30 seconds), whatever the settings say. */
    public static final int MAX_BIND_TICKS = 600;
    /** The fastest a deflected projectile may be sent (blocks per tick). */
    public static final double MAX_DEFLECT_SPEED = 3.0;

    // ------------------------------------------------------------------ Dragon Fist
    /** True once martial training reaches the unlock (a setting; the awakening already asks for martial victories). */
    public static boolean dragonUnlocked(double martialTrained, double need) { return martialTrained >= Math.max(0.0, need); }

    /** The number of steps (rise of the dragon), bounded. */
    public static int dragonSteps(int setting) { return Math.max(2, Math.min(setting, MAX_DRAGON_STEPS)); }

    /** The damage multiplier of step i (0-based) of n: the rise of the dragon does no damage, the last step is the blow (three times the base damage). */
    public static double dragonStepFactor(int i, int n) { return i >= n - 1 ? 3.0 : 0.0; }

    /** Height of the dragon's body at step i of n, in blocks above the foe's feet (0..{@code top}). */
    public static double dragonHeight(int i, int n, double top) { return Math.max(0.0, top) * Math.min(1.0, (i + 1) / (double) Math.max(1, n)); }

    // ------------------------------------------------------------------ God Bind
    /** Ticks a bind holds: the base seconds, cut to a share for a boss and another for a player (the smaller of the two if both), at most {@link #MAX_BIND_TICKS}. */
    public static int bindTicks(double baseSeconds, boolean boss, boolean player, double bossShare, double playerShare) {
        double share = 1.0;
        if (boss) share = Math.min(share, clamp01(bossShare));
        if (player) share = Math.min(share, clamp01(playerShare));
        return (int) Math.max(0, Math.min(MAX_BIND_TICKS, Math.round(Math.max(0.0, baseSeconds) * 20.0 * share)));
    }

    /** True while the bind still holds. */
    public static boolean bindHolds(long now, long until) { return now < until; }

    // ------------------------------------------------------------------ Ki Deflection
    /** The speed of a turned projectile: at least a fast arrow's, boosted, never past {@link #MAX_DEFLECT_SPEED}. */
    public static double deflectSpeed(double current, double boost) { return Math.max(0.3, Math.min(MAX_DEFLECT_SPEED, Math.max(0.6, Math.max(0.0, current)) * Math.max(0.5, boost))); }

    /** True on the ticks a pulse runs (every second tick, so the work is half of one query a tick). */
    public static boolean pulseTick(long now) { return (now & 1L) == 1L; }

    // ------------------------------------------------------------------ Energy Transfer
    /** The Ki the receiver gets, in percent of the receiver's maximum: the giver's spent percent of the giver's maximum, less the loss, in absolute Ki, over the receiver's maximum, capped. 0 if the receiver has no Ki. */
    public static double receivedPercent(double giverMax, double spentPercent, double efficiency, double receiverMax, double capPercent) {
        if (!(receiverMax > 0.0) || !(giverMax > 0.0)) return 0.0;
        double ki = giverMax * Math.max(0.0, spentPercent) / 100.0 * clamp01(efficiency);
        return Math.min(Math.max(0.0, capPercent), ki / receiverMax * 100.0);
    }

    static double clamp01(double x) { return x < 0.0 ? 0.0 : (x > 1.0 ? 1.0 : x); }
}
