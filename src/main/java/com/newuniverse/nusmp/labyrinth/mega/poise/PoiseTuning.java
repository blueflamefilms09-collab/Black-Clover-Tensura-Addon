package com.newuniverse.nusmp.labyrinth.mega.poise;

/**
 * Why: every number of the poise, super-armor and desperation rules in one immutable record, so the pure rules ({@link PoiseMath}, {@link PoiseState}, {@link Desperation}) take their
 * numbers as an argument and are tested with the same values the game uses, while the world side builds one from the server config ({@code [foundry.poise]} of
 * {@code FoundryConfig}) once per server tick. Nothing here knows Minecraft. {@link #DEFAULT} is the shipped balance.
 *
 * @param healthShare     the share of the bearer's maximum health that becomes poise capacity (a capacity, never a damage: no hit is ever a percentage of anyone's health)
 * @param perTier         extra capacity per foundry tier (1 for a mob that has no tier)
 * @param captainMult     a squad captain's capacity multiplier
 * @param minMax          the least capacity a bearer can have
 * @param maxMax          the most capacity a bearer can have
 * @param regenDelay      ticks without a hit before the bar refills
 * @param regenFrac       the share of the capacity the bar refills per second once it starts
 * @param armorReduction  the share of every blow that super armor takes off (the blow still hurts, but less, and never staggers or pushes)
 * @param staggerTicks    how long a broken bar leaves the bearer helpless
 * @param recoverFrac     the share of the bar that is back the moment the stagger ends
 * @param immuneTicks     ticks after the stagger during which the armor holds without draining (no chain-stagger)
 * @param projectileMult  a projectile drains the bar this much less than a melee blow of the same damage
 * @param desperateBelow  the health share under which desperation starts
 * @param desperateExit   the health share over which it ends again (a healer cannot make it flicker)
 * @param pace            the attack-speed factor of desperation (cooldowns and ability rolls are divided by it)
 * @param windScale       the factor of the wind-up of a telegraphed attack in desperation (shorter, but never under {@link Desperation#MIN_WIND})
 * @param eliteTier       a foundry mob of this tier or above is an elite
 * @param armorTail       ticks the armor outlasts the wind-up (the swing itself)
 */
public record PoiseTuning(double healthShare, double perTier, double captainMult, double minMax, double maxMax, int regenDelay, double regenFrac, double armorReduction, int staggerTicks,
                          double recoverFrac, int immuneTicks, double projectileMult, double desperateBelow, double desperateExit, double pace, double windScale, int eliteTier, int armorTail) {

    public static final PoiseTuning DEFAULT = new PoiseTuning(0.40, 8.0, 1.3, 20.0, 600.0, 80, 0.12, 0.35, 50, 0.5, 100, 0.5, 0.30, 0.45, 1.4, 0.8, 3, 10);

    /** The same numbers with the config's four knobs applied (every one clamped into a sane range; the exit share is always at least 5 percent over the entry share). */
    public PoiseTuning with(double desperateBelow, int eliteTier, double armorReduction, int staggerTicks) {
        double below = clamp(desperateBelow, 0.05, 0.6);
        return new PoiseTuning(healthShare, perTier, captainMult, minMax, maxMax, regenDelay, regenFrac, clamp(armorReduction, 0.0, 0.9), Math.max(10, Math.min(200, staggerTicks)), recoverFrac,
                immuneTicks, projectileMult, below, Math.min(0.95, below + 0.15), pace, windScale, Math.max(1, Math.min(6, eliteTier)), armorTail);
    }

    private static double clamp(double v, double lo, double hi) { return Double.isNaN(v) ? lo : Math.max(lo, Math.min(hi, v)); }
}
