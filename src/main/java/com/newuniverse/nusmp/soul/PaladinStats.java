package com.newuniverse.nusmp.soul;

/**
 * Why: a converted creature is "heavily buffed", and the owner gave the numbers: maximum health doubled, attack up 60%, eight points
 * of armour, four of toughness, 15% faster, hard to knock back. The numbers, the one scalar that turns them all up or down
 * ({@code soul.paladinStrength}) and the two safety caps on what a Paladin may deal (a share of a player's or a boss's health per
 * blow, so a converted Ravager cannot one-shot a player or shave a boss) live here as plain arithmetic with no Minecraft types, so
 * they are tested on their own and the glue only applies them.
 */
public final class PaladinStats {
    private PaladinStats() {}

    /** Maximum health: +100% of the base (x2). */
    public static final double HEALTH_MUL = 1.0;
    /** Attack damage: +60% of the base. */
    public static final double ATTACK_MUL = 0.6;
    /** Armour and toughness added flat. */
    public static final double ARMOR = 8.0, TOUGHNESS = 4.0;
    /** Movement speed: +15% of the base. */
    public static final double SPEED_MUL = 0.15;
    /** Knockback resistance added flat (1.0 is immune). */
    public static final double KNOCKBACK = 0.6;
    /** The strength scalar is held to this range so a bad config cannot make a creature immortal or a feather. */
    public static final double MIN_STRENGTH = 0.0, MAX_STRENGTH = 3.0;
    /** A Paladin's blow on a player is at most this share of the player's maximum health. */
    public static final double PLAYER_HIT_CAP = 0.25;
    /** A Paladin's blow on a boss is at most this share of the boss's maximum health. */
    public static final double BOSS_HIT_CAP = 0.02;
    /** A mob with no attack attribute at all (a cow) strikes for this many health points before the bonus. */
    public static final double FALLBACK_ATTACK = 3.0;

    /** The modifiers to add, each already multiplied by the strength. */
    public record Mods(double health, double attack, double armor, double toughness, double speed, double knockback) {}

    public static double clampStrength(double strength) {
        if (Double.isNaN(strength)) return 1.0;
        return Math.max(MIN_STRENGTH, Math.min(MAX_STRENGTH, strength));
    }

    public static Mods mods(double strength) {
        double s = clampStrength(strength);
        return new Mods(HEALTH_MUL * s, ATTACK_MUL * s, ARMOR * s, TOUGHNESS * s, SPEED_MUL * s, Math.min(1.0, KNOCKBACK * s));
    }

    /** The blow of a Paladin that has no attack attribute: the fallback plus the attack bonus. */
    public static double fallbackDamage(double strength) { return FALLBACK_ATTACK * (1.0 + ATTACK_MUL * clampStrength(strength)); }

    /** What a Paladin's blow is held to: a player (a quarter of their health), a boss (2%), anything else untouched. */
    public static float capHit(float amount, double victimMaxHealth, boolean victimIsPlayer, boolean victimIsBoss) {
        double cap = Double.MAX_VALUE;
        if (victimIsPlayer) cap = Math.min(cap, victimMaxHealth * PLAYER_HIT_CAP);
        if (victimIsBoss) cap = Math.min(cap, victimMaxHealth * BOSS_HIT_CAP);
        return (float) Math.max(0.0, Math.min(amount, cap));
    }
}
