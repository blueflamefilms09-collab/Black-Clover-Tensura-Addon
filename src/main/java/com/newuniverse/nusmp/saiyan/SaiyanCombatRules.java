package com.newuniverse.nusmp.saiyan;

/**
 * Why: the numbers of the Saiyan combat passives (3.65.0, Stage 2) with no Minecraft type, so they are unit tested: Warrior's Reflex (a dodge chance that grows with Battle Power and can never reach
 * invulnerability), Battle Adaptation (a resistance to the kind of damage taken again and again, at most fifteen percent, gone after the fight), and Zenkai (permanent growth for surviving a dangerous fight
 * while nearly dead, with a cool-down, a daily cap and diminishing returns). The same dodge arithmetic serves the Ultra Instinct forms ({@link SaiyanForms.Dodge}).
 */
public final class SaiyanCombatRules {
    private SaiyanCombatRules() {}

    /** No dodge chance, from any source, can pass this: a Saiyan is never permanently untouchable. */
    public static final double HARD_DODGE_CAP = 0.75;
    /** Adaptation can never pass this share, whatever the setting says. */
    public static final double HARD_ADAPT_CAP = 0.5;

    /** The kinds of damage Battle Adaptation tells apart. */
    public enum Kind { MELEE, PROJECTILE, MAGIC, EXPLOSION, FIRE, OTHER }

    // ------------------------------------------------------------------ Warrior's Reflex
    /** The dodge chance of the race: {@code base} at the lowest Battle Power, growing to {@code max} at the highest rank, never over {@link #HARD_DODGE_CAP}. */
    public static double reflexChance(double base, double max, double bp) {
        double b = SaiyanRules.clamp(base, 0.0, HARD_DODGE_CAP), m = SaiyanRules.clamp(max, b, HARD_DODGE_CAP);
        return Math.min(m, b + (m - b) * SaiyanRules.growth(bp));
    }

    /** A chance plus a bonus (a form's flow stacks), never over the hard cap. */
    public static double capped(double chance, double bonus) { return SaiyanRules.clamp(chance + bonus, 0.0, HARD_DODGE_CAP); }

    /** True when {@code roll} (0..1) is under the chance. */
    public static boolean dodged(double roll, double chance) { return roll < SaiyanRules.clamp(chance, 0.0, HARD_DODGE_CAP); }

    /** Another dodge is allowed in the current window: the budget is not used up, or the window is over. */
    public static boolean budgetOk(int used, long windowStart, long now, int windowTicks, int max) { return now - windowStart > windowTicks || now < windowStart || used < max; }

    /** The flow stacks after a successful dodge or counter: one more inside the window of the last one (at most {@code max}), else the first. */
    public static int flowAfter(int stacks, long now, long lastAt, int windowTicks, int max) { return (now - lastAt <= windowTicks && now >= lastAt) ? Math.min(Math.max(1, max), stacks + 1) : 1; }

    /** The stacks still counting: none once the window has passed. */
    public static int flowNow(int stacks, long now, long lastAt, int windowTicks) { return (now - lastAt <= windowTicks && now >= lastAt) ? Math.max(0, stacks) : 0; }

    // ------------------------------------------------------------------ Battle Adaptation
    /** The resistance after {@code exposures} hits of one kind: a straight line from nothing to {@code max} over {@code hitsToMax} hits. */
    public static double adaptation(int exposures, double max, int hitsToMax) {
        double cap = SaiyanRules.clamp(max, 0.0, HARD_ADAPT_CAP);
        return cap * Math.min(1.0, Math.max(0, exposures) / (double) Math.max(1, hitsToMax));
    }

    /** True when the fight is over: no damage taken for {@code resetTicks}. */
    public static boolean adaptationOver(long now, long lastHit, int resetTicks) { return now - lastHit > resetTicks || now < lastHit; }

    // ------------------------------------------------------------------ Zenkai
    /** After this blow the player is alive but at or under {@code fraction} of the maximum health: a near-death. */
    public static boolean nearDeath(double healthAfter, double max, double fraction) { return max > 0.0 && healthAfter > 0.0 && healthAfter <= max * SaiyanRules.clamp(fraction, 0.01, 0.9); }

    /** The player has recovered to at least {@code fraction} of the maximum health. */
    public static boolean recovered(double health, double max, double fraction) { return max > 0.0 && health >= max * SaiyanRules.clamp(fraction, 0.05, 1.0); }

    /** The attacker was worth the name of a dangerous fight: its power is at least {@code minRatio} of the Saiyan's Battle Power. */
    public static boolean worthy(double attackerPower, double bp, double minRatio) { return bp > 0.0 && attackerPower >= bp * SaiyanRules.clamp(minRatio, 0.0, 10.0); }

    /** A Zenkai may be granted: not within the cool-down of the last one and under the day's limit. */
    public static boolean zenkaiAllowed(long now, long lastAt, int cooldownTicks, int today, int perDay) { return (lastAt < 0 || now - lastAt >= cooldownTicks || now < lastAt) && today < Math.max(0, perDay); }

    /** The Battle Power one Zenkai gives: a share of the current value, shrinking with every Zenkai already received. */
    public static double zenkaiGain(double bp, double fraction, int received, double decay) {
        return Math.max(0.0, bp) * SaiyanRules.clamp(fraction, 0.0, 1.0) / (1.0 + Math.max(0, received) * Math.max(0.0, decay));
    }
}
