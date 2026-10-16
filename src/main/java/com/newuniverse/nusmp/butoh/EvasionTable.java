package com.newuniverse.nusmp.butoh;

/**
 * Why: "massive but not immunity" is a balance rule, so the odds of slipping a blow live in one pure table: a base chance per move, a flat
 * 20 point penalty against bosses, a hard cap from the config (default 0.65), and the list of damage that can never be dodged (anything that
 * bypasses invulnerability: /kill, the void, out of world; and anything with no attacker, like fall, fire or poison). No game classes.
 */
public final class EvasionTable {
    private EvasionTable() {}

    /** The default cap on the chance (butoh.evadeMax). */
    public static final double DEFAULT_CAP = 0.65;
    /** What a boss attacker takes off the chance, in absolute points (0.20 = 20 points). */
    public static final double BOSS_PENALTY = 0.20;
    /** The cap can be raised by the config at most this high: it must stay a chance, never a guarantee. */
    public static final double HARD_CEILING = 0.95;

    /** Whether a hit may be dodged at all. */
    public static boolean eligible(boolean bypassesInvulnerability, boolean outOfWorld, boolean hasAttacker) {
        return hasAttacker && !bypassesInvulnerability && !outOfWorld;
    }

    /** The chance to slip a hit while {@code move} is held: the move's base, minus the boss penalty, never above the cap, never below 0. */
    public static double chance(DanceMove move, boolean attackerIsBoss, double cap) {
        double c = move.evasion - (attackerIsBoss ? BOSS_PENALTY : 0.0);
        double top = Math.max(0.0, Math.min(HARD_CEILING, cap));
        return Math.max(0.0, Math.min(top, c));
    }

    /** The roll: {@code roll} in [0, 1) dodges when it is below the chance. */
    public static boolean dodges(double chance, double roll) { return roll < chance; }
}
