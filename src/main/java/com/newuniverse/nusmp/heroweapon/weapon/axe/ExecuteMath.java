package com.newuniverse.nusmp.heroweapon.weapon.axe;

/**
 * Why: the owner asked for "executes on wounded foes (bounded)". An execution that simply kills would bypass the balance law, so it is a BONUS ON THE HIT: a foe at or under a
 * quarter of its health takes more than double the damage (a player at or under a fifth takes 40 percent more), and the result is clamped to the cap of the technique's kind, so
 * the law, the player cap and the budget all still apply. A boss is never executed. Whether the foe then dies is up to the arithmetic of the hit: a wounded mob usually does, a
 * big one does not. Pure.
 */
public final class ExecuteMath {
    private ExecuteMath() {}

    public static final double MOB_THRESHOLD = 0.25, PLAYER_THRESHOLD = 0.20;
    public static final double MOB_MULT = 2.2, PLAYER_MULT = 1.4;

    /** True when a foe is low enough to be executed: not a boss, and at or under its threshold of health. */
    public static boolean eligible(float health, float maxHealth, boolean boss, boolean player) {
        if (boss || maxHealth <= 0f || health <= 0f) return false;
        return health <= maxHealth * (player ? PLAYER_THRESHOLD : MOB_THRESHOLD);
    }

    public static double multiplier(boolean player) { return player ? PLAYER_MULT : MOB_MULT; }

    /** The raw damage of an executing hit: {@code base} times the other factors, times the execution if the foe is eligible, never above {@code cap}. */
    public static float raw(float base, double otherFactors, boolean eligible, boolean player, float cap) {
        double r = base * otherFactors * (eligible ? multiplier(player) : 1.0);
        return (float) Math.min(cap, r);
    }
}
