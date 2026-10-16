package com.newuniverse.nusmp.avatar.luck;

/**
 * Why: the creator's luck at the moment of creation decides the dice (5d6 above 10) and tilts the three-day outcome, and the owner listed four separate
 * sources of luck. The sum and the per-source caps are plain arithmetic, so they live here without Minecraft (unit tested); {@link LuckPoller} only reads
 * the four numbers off the player.
 */
public final class LuckMath {
    private LuckMath() {}

    /** Luck a Tensura probability / fortune skill adds, each, and the most the skills can add together. */
    public static final double SKILL_EACH = 2.0, SKILL_MAX = 6.0;
    /** Luck of the Red Thread of Fate (Vanessa's Rouge armed or out). */
    public static final double FATE_BONUS = 3.0;
    /** Luck of an active Loaded Dice buff (Dice Magic). */
    public static final double DICE_BONUS = 2.0;
    /** Luck is kept inside this range so a modified attribute cannot break the weights. */
    public static final double MIN = -20.0, MAX = 60.0;

    public static double skillBonus(int probabilitySkills) { return Math.min(SKILL_MAX, Math.max(0, probabilitySkills) * SKILL_EACH); }

    public static double fateBonus(boolean rougeArmed, boolean rougeOut) { return rougeArmed || rougeOut ? FATE_BONUS : 0.0; }

    public static double diceBonus(boolean loadedDice) { return loadedDice ? DICE_BONUS : 0.0; }

    /** The total: vanilla LUCK attribute + skills + fate + dice, clamped. */
    public static double total(double vanillaLuck, int probabilitySkills, boolean rougeArmed, boolean rougeOut, boolean loadedDice) {
        double t = vanillaLuck + skillBonus(probabilitySkills) + fateBonus(rougeArmed, rougeOut) + diceBonus(loadedDice);
        if (Double.isNaN(t)) return 0.0;
        return Math.max(MIN, Math.min(MAX, t));
    }

    /** True when the id of a registered skill looks like a luck / probability skill (used to find Tensura's without hard-coding an id that may change). */
    public static boolean looksLikeLuckSkill(String path) {
        if (path == null) return false;
        String p = path.toLowerCase();
        return p.contains("probability") || p.contains("luck") || p.contains("fortune");
    }
}
