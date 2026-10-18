package com.newuniverse.nusmp.skill.rules;

/**
 * Why: Liaris Freese (Bell Cranel) lets kills raise the mastery of all your skills. Without limits a mob farm would master everything in
 * minutes, so who counts as a kill, how often it can pay out and how much a boss is worth are decided here, in unit-tested numbers.
 */
public final class GrowthRules {
    private GrowthRules() {}

    /** At most one payout per this many ticks (a one-second guard against farms that kill dozens of mobs in one tick). */
    public static final int MIN_GAP_TICKS = 20;

    /** Kills of yourself, your own pets or summons, and armor stands never count. */
    public static boolean countsAsKill(boolean selfKill, boolean ownCreature, boolean armorStand) {
        return !selfKill && !ownCreature && !armorStand;
    }

    /** Whether the roll (0..1) pays out under the chance and the guard. */
    public static boolean pays(double chance, double roll, long now, long lastPayout) {
        if (now - lastPayout < MIN_GAP_TICKS && now >= lastPayout) return false;
        return roll < SkillMath.clamp(chance, 0.0, 1.0);
    }

    /** Mastery points per payout: the configured amount, tripled for a boss. */
    public static int amount(int configured, boolean boss) {
        if (configured <= 0) return 0;
        return boss ? configured * 3 : configured;
    }
}
