package com.newuniverse.nusmp.heroweapon.weapon.claw;

/**
 * Why: the canon power-up method of the Vassal Claw is Skill Mastery: the more a skill is used, the more its levels and abilities improve (from a search summary of the fandom
 * wiki). Here every damaging technique of the Claw has its own mastery that grows with its uses (Predator's Instinct does no damage and has none): five levels, each worth 6 percent more damage and 4 percent less cooldown, so a technique
 * used a hundred times hits 30 percent harder and comes back 20 percent sooner than a new one. The uses are kept in the bond (they survive death), the arithmetic is here, pure.
 * The budget stays true at the best level: {@link ClawNumbers} rows are checked at {@link #MAX_DAMAGE_MULT}, and the cooldown can never go under the budget's floor
 * ({@code StrikeSkill} clamps it).
 */
public final class ClawMastery {
    private ClawMastery() {}

    /** The uses a technique needs to reach level 1, 2, 3, 4 and 5. */
    public static final int[] THRESHOLDS = {6, 16, 32, 56, 90};
    public static final int MAX_LEVEL = THRESHOLDS.length;
    public static final double DAMAGE_STEP = 0.06, COOLDOWN_STEP = 0.04;
    /** The damage factor at the top level. */
    public static final double MAX_DAMAGE_MULT = 1.0 + DAMAGE_STEP * MAX_LEVEL;

    /** The level a number of uses has reached, 0 to {@link #MAX_LEVEL}. */
    public static int level(int uses) {
        int level = 0;
        for (int need : THRESHOLDS) if (uses >= need) level++;
        return level;
    }

    /** The damage factor of a level. */
    public static double damageMult(int level) { return 1.0 + DAMAGE_STEP * Math.max(0, Math.min(MAX_LEVEL, level)); }

    /** The cooldown factor of a level. */
    public static double cooldownMult(int level) { return 1.0 - COOLDOWN_STEP * Math.max(0, Math.min(MAX_LEVEL, level)); }

    /** True when the use that took the count from {@code before} to {@code before + 1} reached a new level. */
    public static boolean leveledUp(int before) { return before >= 0 && level(before + 1) > level(before); }
}
