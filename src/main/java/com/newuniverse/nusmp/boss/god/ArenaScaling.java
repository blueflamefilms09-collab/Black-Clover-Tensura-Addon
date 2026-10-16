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
}
