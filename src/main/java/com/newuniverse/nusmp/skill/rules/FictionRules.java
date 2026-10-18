package com.newuniverse.nusmp.skill.rules;

/**
 * Why: Book Maker (Kumagawa) pins a target with screws and drags it down to your level; All Fiction, its Ultimate, makes harm "never have
 * happened" (Undo), erases a target (Fiction) and refuses one death. The five-second health memory behind Undo, the pin numbers and the
 * erase-or-hurt decision are pure, so they are tested without a world.
 */
public final class FictionRules {
    private FictionRules() {}

    // ---- Book Maker ----
    public static final int PIN = 0, BARRAGE = 1;
    public static double pinCost() { return 400; }
    public static int pinCooldown() { return 80; }
    public static double pinDamage(Tier t) { return t == Tier.BASE ? 10 : 16; }
    public static int pinSlowTicks() { return 120; }
    public static int pinWeakTicks() { return 200; }
    public static int pinWeakAmp(Tier t) { return t == Tier.BASE ? 1 : 2; }
    public static double pinRange() { return 24; }
    /** Barrage: pins everything in a cone in front of you (mastered only). */
    public static double barrageCost() { return 1200; }
    public static int barrageCooldown() { return 240; }
    public static double barrageRange() { return 10; }
    public static double barrageCone() { return 0.55; }
    public static boolean barrageUnlocked(Tier t) { return t != Tier.BASE; }
    public static int modes() { return 2; }

    // ---- All Fiction ----
    public static final int UNDO = 0, ERASE = 1, FICTION_DEATH = 2;
    public static final int HISTORY_SECONDS = 5;
    public static double undoCost() { return 1500; }
    public static int undoCooldown() { return 400; }
    public static double eraseCost() { return 4000; }
    public static int eraseCooldown() { return 1200; }
    public static double eraseRange() { return 24; }
    /** Share of max health a boss (or protected player) loses instead of being erased. */
    public static double eraseBossShare() { return 0.25; }
    public static int deathCooldown() { return 18000; }

    /** Index into the health history of the sample to restore: the oldest one remembered. */
    public static int oldestIndex() { return HISTORY_SECONDS - 1; }

    /** The health Undo restores: the remembered value if it is higher than now, never above the maximum. */
    public static float undoHealth(float remembered, float current, float max) {
        if (remembered <= current) return current;
        return Math.min(remembered, max);
    }

    /** Shifts a new sample into a history array (newest first), dropping the oldest. */
    public static void push(float[] history, float sample) {
        for (int i = history.length - 1; i > 0; i--) history[i] = history[i - 1];
        if (history.length > 0) history[0] = sample;
    }

    /** Whether the target is erased outright (true) or only weakened (false). */
    public static boolean erases(boolean fullyAffected) { return fullyAffected; }
}
