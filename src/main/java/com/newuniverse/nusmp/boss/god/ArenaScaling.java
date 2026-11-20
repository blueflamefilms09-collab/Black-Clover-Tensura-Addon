package com.newuniverse.nusmp.boss.god;

/**
 * Why: Lucius's health resets at every stage, so a fight that is fair for a party of four is a wall for one player and a joke for a
 * crowd if his numbers do not follow who is in the arena. This pure class (no Minecraft types, unit tested) is that one table. It
 * scales two things by the number of players inside the arena at the moment a stage begins: his <i>maximum health</i> (so a duo
 * and a squad both get a fight of about ten to fifteen minutes) and the <i>true damage</i> he deals (more players spread the pressure
 * over more targets, so it grows more slowly than his health). 2 players is the reference (100%).
 * <pre>
 *   players   health   true damage
 *      1       70%        80%
 *      2      100%       100%
 *      3      115%       106%
 *      4+     130%       112%
 * </pre>
 * The factor is read once per stage start (not every tick), so a player who walks in or out mid-stage does not move his health bar.
 */
public final class ArenaScaling {
    private ArenaScaling() {}

    public static final int MAX_COUNTED = 4;

    private static final double[] HEALTH = {0.70, 0.70, 1.00, 1.15, 1.30};
    private static final double[] DAMAGE = {0.80, 0.80, 1.00, 1.06, 1.12};

    private static int idx(int players) { return Math.max(1, Math.min(MAX_COUNTED, players)); }

    /** The factor on his maximum health for this many players (zero or fewer count as one). */
    public static double health(int players) { return HEALTH[idx(players)]; }

    /** The factor on his true damage for this many players. */
    public static double damage(int players) { return DAMAGE[idx(players)]; }

    /** The attribute modifier amount (factor minus one) for ADD_MULTIPLIED_TOTAL on the maximum health. */
    public static double healthModifier(int players) { return health(players) - 1.0; }

    // ---- Leon study fix 5: the same table for every boss of the framework, under one combined cap
    /**
     * The most the whole health stack (Black Clover x3 times the floor scale (at most x2.5) times the party factor) may reach, and the same for damage (x3 times the floor scale (at most
     * x1.6) times the party factor). A party factor above 1 is cut back so the product stays under the maximum (never below 1); a factor below 1 (a small party) is never touched.
     * x3 * x2.5 = 7.5 and x3 * x1.6 = 4.8 leave the full table x1.30 / x1.12 only a little room on the deepest floors, which is the point.
     */
    public static final double MAX_TOTAL_HEALTH = 8.0, MAX_TOTAL_DAMAGE = 5.0;

    /** The party factor on health for 'players' fighters when 'others' (the product of every other health factor already on the boss) is already there. */
    public static double healthCombined(int players, double others) { return combined(health(players), others, MAX_TOTAL_HEALTH); }

    /** The party factor on damage, the same rule with its own maximum. */
    public static double damageCombined(int players, double others) { return combined(damage(players), others, MAX_TOTAL_DAMAGE); }

    private static double combined(double raw, double others, double max) {
        if (!(raw > 1.0)) return raw;
        double o = Double.isNaN(others) || others < 1.0 ? 1.0 : others;
        return Math.max(1.0, Math.min(raw, max / o));
    }
}
