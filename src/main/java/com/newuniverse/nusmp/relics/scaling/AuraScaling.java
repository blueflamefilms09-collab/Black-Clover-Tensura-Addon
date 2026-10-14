package com.newuniverse.nusmp.relics.scaling;

/**
 * The numbers of the Aura system, with no Minecraft types so they are unit-tested (docs/relic_tiers_and_scaling.md explains every choice).
 * A relic weapon in the main hand hits harder in the hands of an end-game player: a bonus that grows with the logarithm of the player's maximum
 * magicule pool (so it keeps rising smoothly but never runs away), a bonus for an awakened state, and a share of the target's armour that is ignored.
 * Everything is capped, and bosses get tighter caps so the boss fights keep their step-by-step pacing.
 */
public final class AuraScaling {
    private AuraScaling() {}

    /** The pool (maximum magicules) under which there is no pool bonus. */
    public static final double BASE_POOL = 10_000.0;
    /** The bonus for each doubling of the pool above {@link #BASE_POOL}, and its cap (reached at 1024 times the base pool). */
    public static final double PER_DOUBLING = 0.035, POOL_BONUS_CAP = 0.35;
    /** A Mana Zone around the wielder. */
    public static final double ZONE_DAMAGE = 1.08, ZONE_IGNORE = 0.05;
    /** The total caps on the damage multiplier, and on the share of armour ignored; bosses are held to the second pair. */
    public static final double MAX_DAMAGE_MULT = 1.60, MAX_IGNORE = 0.25;
    public static final double BOSS_MAX_DAMAGE_MULT = 1.35, BOSS_MAX_IGNORE = 0.12;

    /** What the wielder is: nothing special, one step from awakening (a Demon Lord seed, a Hero egg), or awakened (true demon lord, true hero, the crown). */
    public enum Awakening {
        NONE(1.00, 0.00), SEEDED(1.05, 0.00), AWAKENED(1.20, 0.15);

        final double damage, ignore;

        Awakening(double damage, double ignore) {
            this.damage = damage;
            this.ignore = ignore;
        }
    }

    /** A damage multiplier (1.0 = no change) and the share of the target's armour reduction that is ignored (0..1). */
    public record Aura(double damageMult, double armourIgnore) {
        public static final Aura NONE = new Aura(1.0, 0.0);

        public boolean none() { return damageMult <= 1.0 && armourIgnore <= 0.0; }
    }

    /** The bonus (0 up to {@link #POOL_BONUS_CAP}) of a maximum magicule pool: PER_DOUBLING for every doubling above the base pool. */
    public static double poolBonus(double maxMagicule) {
        if (Double.isNaN(maxMagicule) || maxMagicule <= BASE_POOL) return 0.0;
        if (Double.isInfinite(maxMagicule)) return POOL_BONUS_CAP;
        double doublings = Math.log(maxMagicule / BASE_POOL) / Math.log(2.0);
        return Math.min(POOL_BONUS_CAP, PER_DOUBLING * doublings);
    }

    /** The aura of a wielder against an ordinary target or against a boss. */
    public static Aura compute(Awakening awakening, boolean manaZone, double maxMagicule, boolean bossTarget) {
        Awakening a = awakening == null ? Awakening.NONE : awakening;
        double dmg = (1.0 + poolBonus(maxMagicule)) * a.damage * (manaZone ? ZONE_DAMAGE : 1.0);
        double ignore = a.ignore + (manaZone ? ZONE_IGNORE : 0.0);
        dmg = Math.max(1.0, Math.min(bossTarget ? BOSS_MAX_DAMAGE_MULT : MAX_DAMAGE_MULT, dmg));
        ignore = Math.max(0.0, Math.min(bossTarget ? BOSS_MAX_IGNORE : MAX_IGNORE, ignore));
        return new Aura(dmg, ignore);
    }

    /** The armour reduction that is left after ignoring a share of it (the factor the reduction is multiplied by). */
    public static float armourKeep(double ignore) { return (float) (1.0 - Math.max(0.0, Math.min(1.0, ignore))); }

    /**
     * The damage of one hit after the aura: never below the original, and where a hard limit exists (a player target: the per-hit cap) never above
     * it unless the original already was.
     */
    public static float apply(float original, double damageMult, float limit) {
        float scaled = (float) (original * Math.max(1.0, damageMult));
        if (limit > 0f) scaled = Math.min(scaled, limit);
        return Math.max(original, scaled);
    }
}
