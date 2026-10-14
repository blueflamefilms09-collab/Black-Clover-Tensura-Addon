package com.newuniverse.nusmp.relics.tier;

/**
 * The numbers of tier ascension, with no Minecraft types so they are unit-tested (docs/relic_tiers_and_scaling.md explains every choice).
 * <p>
 * A relic has a base grade (its def) and may carry the data component {@code nusmp:tier}: the ABSOLUTE grade ordinal it has been raised to
 * (0 NORMAL ... 6 GENESIS; 0 or missing means "never raised"). The effective grade is {@code max(base, tier)}. Every multiplier below is a function of
 * the number of STEPS the effective grade stands above the base grade, not of the absolute grade: a Genesis relic is never raised further, and a
 * Normal blade raised to Genesis gets the full six steps. Step 0 is always exactly 1.0, so an unraised relic behaves as it did before.
 */
public final class TierRules {
    private TierRules() {}

    /** The highest grade ordinal (GENESIS). */
    public static final int MAX_TIER = 6;

    /** What a tier scales. */
    public enum Stat { DAMAGE, ATTACK_SPEED, ARMOR, TOUGHNESS, ACT_POWER, COST, COOLDOWN }

    // index = steps above the base grade (0..6)
    private static final double[] DAMAGE = {1.00, 1.08, 1.17, 1.27, 1.38, 1.50, 1.65};
    private static final double[] SPEED = {1.00, 1.02, 1.04, 1.07, 1.10, 1.14, 1.18};
    private static final double[] ARMOR = {1.00, 1.05, 1.10, 1.16, 1.23, 1.31, 1.40};
    private static final double[] TOUGHNESS = {1.00, 1.10, 1.20, 1.32, 1.46, 1.62, 1.80};
    private static final double[] ACT_POWER = {1.00, 1.04, 1.08, 1.13, 1.18, 1.24, 1.30};
    private static final double[] COST = {1.00, 0.97, 0.94, 0.90, 0.86, 0.82, 0.78};
    private static final double[] COOLDOWN = {1.00, 0.98, 0.96, 0.93, 0.90, 0.87, 0.85};

    /** Hard caps on what a raised item may reach (an item already above a cap is left as it is, never lowered). */
    public static final double MAX_WEAPON_DAMAGE = 40.0;      // total attack damage, the player's own 1 included
    public static final double MAX_ATTACK_SPEED = 4.0;        // attacks per second: the player's base, so the modifier never goes above 0
    public static final double MAX_ARMOR_PER_PIECE = 10.0;
    public static final double MAX_TOUGHNESS_PER_PIECE = 5.0;
    /** The floors of the two reductions, so no act becomes free or instant. */
    public static final double MIN_COST_FACTOR = 0.75, MIN_COOLDOWN_FACTOR = 0.80;

    public static int clampTier(int t) { return Math.max(0, Math.min(MAX_TIER, t)); }

    /** The effective grade ordinal: the base, or the tier if that is higher. */
    public static int effective(int baseOrdinal, int tier) { return Math.max(clampTier(baseOrdinal), clampTier(tier)); }

    /** How many grades the effective grade stands above the base (0 for an unraised item). */
    public static int steps(int baseOrdinal, int tier) { return effective(baseOrdinal, tier) - clampTier(baseOrdinal); }

    /** The multiplier of a stat after this many steps (clamped to the table; the reductions respect their floors). */
    public static double multiplier(Stat stat, int steps) {
        int i = Math.max(0, Math.min(MAX_TIER, steps));
        return switch (stat) {
            case DAMAGE -> DAMAGE[i];
            case ATTACK_SPEED -> SPEED[i];
            case ARMOR -> ARMOR[i];
            case TOUGHNESS -> TOUGHNESS[i];
            case ACT_POWER -> ACT_POWER[i];
            case COST -> Math.max(MIN_COST_FACTOR, COST[i]);
            case COOLDOWN -> Math.max(MIN_COOLDOWN_FACTOR, COOLDOWN[i]);
        };
    }

    /**
     * The attack damage MODIFIER value of a weapon after the tier (the modifier is the total damage minus the player's 1). Never below the original;
     * capped so the total stays within {@link #MAX_WEAPON_DAMAGE} (unless the original already was above it).
     */
    public static double scaledDamageModifier(double original, int steps) {
        if (steps <= 0) return original;
        double scaled = original * multiplier(Stat.DAMAGE, steps);
        return Math.max(original, Math.min(scaled, MAX_WEAPON_DAMAGE - 1.0));
    }

    /** The attack speed MODIFIER (4 + modifier = attacks per second) after the tier: faster, never above {@link #MAX_ATTACK_SPEED}, never slower than before. */
    public static double scaledSpeedModifier(double original, int steps) {
        if (steps <= 0) return original;
        double attacksPerSecond = (4.0 + original) * multiplier(Stat.ATTACK_SPEED, steps);
        double scaled = Math.min(MAX_ATTACK_SPEED, attacksPerSecond) - 4.0;
        return Math.max(original, scaled);
    }

    public static double scaledArmor(double original, int steps) {
        if (steps <= 0) return original;
        return Math.max(original, Math.min(original * multiplier(Stat.ARMOR, steps), MAX_ARMOR_PER_PIECE));
    }

    public static double scaledToughness(double original, int steps) {
        if (steps <= 0) return original;
        return Math.max(original, Math.min(original * multiplier(Stat.TOUGHNESS, steps), MAX_TOUGHNESS_PER_PIECE));
    }

    // ---------------------------------------------------------------- the upgrade ritual
    /** One accepted catalyst: an item id ("tensura:mithril_ingot") and how many are consumed. */
    public record Catalyst(String itemId, int count) {}

    private static final Catalyst[][] CATALYSTS = {
            {},                                                                                           // 0 NORMAL: nothing raises to it
            {new Catalyst("tensura:low_magisteel_ingot", 2)},                                              // to SPECIAL
            {new Catalyst("tensura:high_magisteel_ingot", 2)},                                             // to RARE
            {new Catalyst("tensura:mithril_ingot", 3)},                                                    // to UNIQUE
            {new Catalyst("tensura:adamantite_ingot", 3)},                                                 // to LEGEND
            {new Catalyst("tensura:hihiirokane_ingot", 4), new Catalyst("nusmp:devils_core", 1)},         // to GOD
            {new Catalyst("nusmp:genesis_seed", 1)},                                                       // to GENESIS
    };

    /** The catalysts that raise an item to this grade ordinal (any ONE of them does it); empty for NORMAL or out of range. */
    public static Catalyst[] catalystsTo(int targetOrdinal) {
        return targetOrdinal < 1 || targetOrdinal > MAX_TIER ? new Catalyst[0] : CATALYSTS[targetOrdinal].clone();
    }

    /** Chance (0..1) that the jump INTO this grade ordinal succeeds. */
    private static final double[] SUCCESS = {1.0, 1.00, 0.95, 0.85, 0.70, 0.55, 0.40};

    public static double successChance(int targetOrdinal) { return targetOrdinal < 1 || targetOrdinal > MAX_TIER ? 0.0 : SUCCESS[targetOrdinal]; }

    /** Percent of the maximum magicule pool (aura pays what magicules cannot) the jump INTO this grade costs. */
    private static final double[] COST_PERCENT = {0, 2, 4, 6, 10, 15, 22};

    public static double costPercent(int targetOrdinal) { return targetOrdinal < 1 || targetOrdinal > MAX_TIER ? 0.0 : COST_PERCENT[targetOrdinal]; }

    /** Ticks the player must wait after a FAILED attempt into this grade (a successful one waits a short fixed time). */
    private static final int[] FAIL_COOLDOWN = {0, 100, 200, 400, 800, 1600, 2400};

    public static int failCooldownTicks(int targetOrdinal) { return targetOrdinal < 1 || targetOrdinal > MAX_TIER ? 0 : FAIL_COOLDOWN[targetOrdinal]; }

    /** From this target grade up, a failed attempt also costs the catalysts (below it a failure only sets the cooldown). */
    public static final int CATALYST_LOST_FROM = 3;

    public static boolean losesCatalystOnFail(int targetOrdinal) { return targetOrdinal >= CATALYST_LOST_FROM; }

    /** Ticks between two attempts after a success (also stops a held button from repeating the ritual). */
    public static final int SUCCESS_COOLDOWN_TICKS = 40;

    /** The result of a roll in [0,1): success if it falls under the chance. The relic is never destroyed either way. */
    public static boolean succeeds(int targetOrdinal, double roll) { return roll < successChance(targetOrdinal); }

    /** The grade ordinal a ritual on an item of this effective grade aims for, or -1 if it is already at the top. */
    public static int nextTarget(int effectiveOrdinal) { return effectiveOrdinal >= MAX_TIER ? -1 : clampTier(effectiveOrdinal) + 1; }
}
