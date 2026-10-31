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

    // ---- All Fiction rebuilt (3.46.0): ten modes. Undo (0) and Fiction (1) keep their places and numbers; the passive that refuses a death is unchanged. ----
    public static final int ANNUL = 2, REWIND = 3, UNPICK = 4, NULLIFY = 5, UNMAKE = 6, SHARE = 7, STAND_UP = 8, WORLD = 9;
    public static final int ALL_MODES = 10;
    /** The ids of the ten modes (lang keys nusmp.skill.mode.&lt;id&gt;; the first two are the old ones). */
    public static final String[] ALL_IDS = {"fiction.undo", "fiction.erase", "fiction.annul", "fiction.rewind", "fiction.unpick", "fiction.nullify", "fiction.unmake", "fiction.share", "fiction.standup", "fiction.world"};

    /** The number of modes on the wheel: ten when the rebuild is on (setting allFictionRebuilt), the old two when it is off. */
    public static int wheel(boolean rebuilt) { return rebuilt ? ALL_MODES : 2; }

    public static double cost(int mode) {
        return switch (mode) {
            case UNDO -> undoCost(); case ERASE -> eraseCost(); case ANNUL -> 2500; case REWIND -> 2000; case UNPICK -> 1500;
            case NULLIFY -> 3000; case UNMAKE -> 3500; case SHARE -> 3000; case WORLD -> 6000; default -> 0;
        };
    }

    public static int cooldown(int mode) {
        return switch (mode) {
            case UNDO -> undoCooldown(); case ERASE -> eraseCooldown(); case ANNUL -> 900; case REWIND -> 600; case UNPICK -> 300;
            case NULLIFY -> 1200; case UNMAKE -> 900; case SHARE -> 900; case WORLD -> 3600; default -> 0;
        };
    }

    /** Annul Damage: for six seconds, hits are made not to have happened until a pool of half your maximum health is used up. */
    public static int annulTicks() { return 120; }
    public static double annulPool(double maxHealth) { return Math.max(0.0, maxHealth) * 0.5; }

    /** How much of a hit the pool takes (never more than the hit or the pool). */
    public static double annulAbsorb(double pool, double hit) { return Math.max(0.0, Math.min(pool, hit)); }

    /** A window (Annul, Nullify, World) stored as the game time it ends at is open while that time is in the future. */
    public static boolean windowOpen(long until, long now) { return until > now; }

    /** Nullify Strike: the next hit within ten seconds never happened (hits that bypass invulnerability, the void and kill, are not Fiction's to refuse). */
    public static int nullifyTicks() { return 200; }

    /** Rewind Step: the place you stood five seconds ago, if it is between 1.5 and 48 blocks away. */
    public static double rewindMin() { return 1.5; }
    public static double rewindMax() { return 48.0; }
    public static boolean rewindOk(double distance) { return distance >= rewindMin() && distance <= rewindMax(); }

    /** Erase Effects: range of the look. */
    public static double unpickRange() { return 24.0; }

    /** Unmake: hostile projectiles within this radius are erased, at most this many. */
    public static double unmakeRadius() { return 10.0; }
    public static int unmakeMax() { return 32; }

    /** Share the Fiction: allies within this radius (at most eight, you included) are cleansed and healed this share of their maximum health. */
    public static double shareRadius() { return 8.0; }
    public static double shareHeal() { return 0.25; }
    public static int shareMax() { return 8; }

    /** World Fiction: a zone around where you stood, pulsing once a second for six seconds; each pulse cleanses allies in it and erases hostile projectiles (at most 16). */
    public static double worldRadius() { return 8.0; }
    public static int worldTicks() { return 120; }
    public static int worldPulseMaxProjectiles() { return 16; }

    /** True when a pulse of the zone is due: it is open and this is a whole second of the owner's tick count. */
    public static boolean worldPulseDue(long until, long now, int tickCount) { return until > now && tickCount % 20 == 0; }

    /** Seconds until the refused death is ready again (0 = ready now). */
    public static int standUpSeconds(long readyAt, long now) { return readyAt <= now ? 0 : (int) Math.min(Integer.MAX_VALUE, (readyAt - now + 19) / 20); }

    /** Shifts a sample into a history of doubles (newest first), dropping the oldest. */
    public static void pushD(double[] history, double sample) {
        for (int i = history.length - 1; i > 0; i--) history[i] = history[i - 1];
        if (history.length > 0) history[0] = sample;
    }
}
