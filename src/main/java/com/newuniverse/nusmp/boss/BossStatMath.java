package com.newuniverse.nusmp.boss;

/**
 * Why: the owner asked for the Black Clover bosses to be three times as strong (3.64.0). The numbers live here as pure arithmetic (unit tested, no Minecraft types): which character
 * counts (a boss of the roster whose source work is "Black Clover"), what the multiplier may be, the maximum-health modifier it becomes, and the damage a boss deals after it. The glue
 * ({@link BossStatBuff}) only reads the game and applies these answers.
 */
public final class BossStatMath {
    private BossStatMath() {}

    /** The work whose bosses are buffed: the {@code source} of their {@code ArcCharacter}. */
    public static final String SOURCE = "Black Clover";
    /** Highest multiplier the config may ask for; 1 = off. */
    public static final double MAX = 10.0;
    /** A buffed hit never takes more than this share of the victim's maximum health, unless the unbuffed hit already did (nothing is made a one-shot from full health). */
    public static final double HIT_CAP = 0.5;

    /** The multiplier really used: NaN or under 1 = 1 (off), never above {@link #MAX}. */
    public static double clamp(double m) {
        if (Double.isNaN(m) || m < 1.0) return 1.0;
        return Math.min(MAX, m);
    }

    /** Is this roster character one the buff applies to? */
    public static boolean applies(String source, boolean isBoss) { return isBoss && SOURCE.equalsIgnoreCase(source); }

    /** The amount of the ADD_MULTIPLIED_TOTAL modifier on maximum health: x3 is +2.0 (the modifiers of other systems multiply with it). */
    public static double healthModifier(double m) { return clamp(m) - 1.0; }

    /** The damage of one hit after the multiplier: times m, never above {@link #HIT_CAP} of the victim's maximum health unless it was already, never below the unbuffed hit. */
    public static float damage(float amount, double m, float victimMax) {
        double k = clamp(m);
        if (!(amount > 0f) || k <= 1.0) return amount;
        double boosted = amount * k;
        if (victimMax > 0f) boosted = Math.min(boosted, Math.max(amount, victimMax * HIT_CAP));
        return (float) boosted;
    }

    /** The health to set after the maximum moved from {@code before} to {@code after}: the same share of the bar (a spawn stays full, a wounded boss stays wounded). */
    public static float keepShare(float health, float before, float after) {
        if (!(before > 0f) || !(after > 0f)) return health;
        return Math.min(after, health * after / before);
    }
}
