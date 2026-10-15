package com.newuniverse.nusmp.boss.arc;

/**
 * Why: the Soul Touch's debuff cuts the victim's maximum health, which is the most dangerous kind of debuff to get wrong. The numbers
 * (three levels of 10%, a minute, a floor of four hearts, a refresh that raises the level) are therefore pure and tested, and the effect
 * class only applies them. Max health is never reduced below the floor, so the debuff cannot kill by itself and cannot leave a player
 * with a health bar that one hit empties.
 */
public final class SoulCorruptionMath {
    private SoulCorruptionMath() {}

    public static final int MAX_AMPLIFIER = 2, DURATION_TICKS = 1200;
    public static final double PER_LEVEL = 0.10;
    /** Four hearts. */
    public static final double FLOOR_HEALTH = 8.0;

    /** The amplifier after a new application: 0 when there was none (-1), one more each time, 2 at the most. */
    public static int nextAmplifier(int existing) { return Math.max(0, Math.min(MAX_AMPLIFIER, existing + 1)); }

    /** The share of the base maximum health that level 'amplifier' takes (0.10, 0.20, 0.30). */
    public static double share(int amplifier) { return PER_LEVEL * (Math.max(0, Math.min(MAX_AMPLIFIER, amplifier)) + 1); }

    /** How many health points the debuff removes from a base maximum of 'baseMax': the share, but never into the floor. */
    public static double reduction(double baseMax, int amplifier) {
        if (!(baseMax > 0.0)) return 0.0;
        return Math.max(0.0, Math.min(baseMax * share(amplifier), baseMax - FLOOR_HEALTH));
    }
}
