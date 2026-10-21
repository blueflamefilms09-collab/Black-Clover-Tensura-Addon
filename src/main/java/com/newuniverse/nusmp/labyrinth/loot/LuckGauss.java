package com.newuniverse.nusmp.labyrinth.loot;

import com.newuniverse.nusmp.dungeon.sentient.Dice;

/**
 * Why: the owner wants the number of things a kill or a chest gives to follow a bell curve whose middle moves up with the killer's Luck and with how deep the floor is, instead of a
 * flat table. A bell curve (a Gaussian) is the right shape because it makes the ordinary result the common one and the great result a rare, believable one: a lucky player does not
 * get a fixed bonus, they get a better MIDDLE, and the spread widens with it so a very lucky haul can be much better than the average without the average being absurd. Every rule
 * that keeps this safe is here, in one pure class a unit test can pin: (1) the curve only ADDS to what the table already gave (the extra is clamped at zero, so Luck can never
 * take an item away; add the new, keep the old); (2) with no Luck and no depth the middle is zero, the spread is zero and NO random number is drawn, so the old results are
 * bit-for-bit what they were; (3) Luck is clamped to {@link #MAX_LUCK} and negative Luck counts as zero (a cursed player is not punished, they just are not rewarded); (4) the
 * extra is clamped to {@code maxFactor - 1} times the base and the result to the caller's cap (a stack never goes over its size); (5) the depth term is the saturating
 * {@code d / (d + 200)}, so deeper is richer but never without end (floor scaling stays the saturating curve on purpose). The same maths gives the coin multiplier (Luck only, the
 * floor already scales coins) and the chance of a rolled piece of armour. Hunger is not a concept here. Pure: no Minecraft type.
 */
public final class LuckGauss {
    private LuckGauss() {}

    /** The most Luck that counts (a few potions, a relic and a heritage stack together stay far below). */
    public static final double MAX_LUCK = 20.0;

    /** The floor at which the depth term is one half. */
    public static final double DEPTH_HALF = 200.0;

    /**
     * The tunable numbers. {@code luckGain} is the middle's rise per point of Luck and {@code depthGain} its rise from the deepest floors (both as a FRACTION of the base count),
     * {@code sigmaRatio} makes the spread a fraction of the middle, and {@code maxFactor} is the most the count may be multiplied by (1 = Luck does nothing).
     */
    public record Params(double luckGain, double depthGain, double sigmaRatio, double maxFactor) {
        public Params {
            luckGain = clamp(luckGain, 0.0, 1.0);
            depthGain = clamp(depthGain, 0.0, 5.0);
            sigmaRatio = clamp(sigmaRatio, 0.0, 3.0);
            maxFactor = clamp(maxFactor, 1.0, 10.0);
        }
    }

    /** Item counts: +6% of the base per point of Luck, up to +50% from depth, a spread of half the middle, never more than triple. */
    public static final Params ITEMS = new Params(0.06, 0.5, 0.5, 3.0);

    /** Coin amounts: +5% per point of Luck, no depth term (the payout already scales with the floor), never more than 2.5 times. */
    public static final Params COINS = new Params(0.05, 0.0, 0.5, 2.5);

    private static double clamp(double v, double lo, double hi) {
        return Double.isNaN(v) ? lo : Math.max(lo, Math.min(hi, v));
    }

    /** The Luck that counts: zero for a negative or missing value, at most {@link #MAX_LUCK}. */
    public static double luck(double raw) { return clamp(raw, 0.0, MAX_LUCK); }

    /** How deep a floor is, 0..1: {@code d / (d + 200)} on the absolute floor number, zero for the Hub or an unknown floor. */
    public static double depthFactor(int floor) {
        double d = Math.abs((double) floor);
        return d / (d + DEPTH_HALF);
    }

    // ---------------------------------------------------------------- the curve
    /** One standard normal draw (Box-Muller; the unit draw is kept off zero so the logarithm is finite). */
    public static double gauss(Dice dice) {
        double u1 = Math.max(1.0e-12, dice.next());
        double u2 = dice.next();
        return Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
    }

    /** The middle of the extra, as a fraction of the base count. */
    public static double mean(double luck, int floor, Params p) {
        return p.luckGain() * luck(luck) + p.depthGain() * depthFactor(floor);
    }

    /** The spread of the extra, as a fraction of the base count. */
    public static double sigma(double luck, int floor, Params p) { return p.sigmaRatio() * mean(luck, floor, p); }

    /**
     * The extra fraction of one draw: a Gaussian around {@link #mean}, clamped to {@code [0, maxFactor - 1]}. Draws NOTHING when the middle is zero, so the old results do not move.
     */
    public static double extra(double luck, int floor, Dice dice, Params p) {
        double mean = mean(luck, floor, p);
        if (mean <= 0.0) return 0.0;
        double v = mean + sigma(luck, floor, p) * gauss(dice);
        return clamp(v, 0.0, p.maxFactor() - 1.0);
    }

    /**
     * The count after Luck: the base plus the Gaussian extra of the base, rounded without bias (the fraction is the chance of one more), never below the base and never above
     * {@code max(base, cap)}.
     */
    public static int count(int base, double luck, int floor, Dice dice, Params p, int cap) {
        if (base <= 0) return Math.max(0, base);
        double extra = extra(luck, floor, dice, p) * base;
        if (extra <= 0.0) return base;
        int whole = (int) Math.floor(extra);
        double frac = extra - whole;
        int add = whole + (dice.next() < frac ? 1 : 0);
        return Math.min(Math.max(base, cap), base + add);
    }

    /** The multiplier of an amount (coins): one plus the extra of {@link #extra} at the floor-less curve. Always at least 1. */
    public static double factor(double luck, Dice dice, Params p) { return 1.0 + extra(luck, 0, dice, p); }

    /** The same as {@link #factor} for an amount: the rounded product, at least the amount itself. */
    public static long scale(long amount, double luck, Dice dice, Params p) {
        if (amount <= 0) return Math.max(0L, amount);
        double f = factor(luck, dice, p);
        return f <= 1.0 ? amount : Math.max(amount, Math.round(amount * f));
    }

    // ---------------------------------------------------------------- chances
    /** Extra chance that a coin-paying mob pays: 1 point per point of Luck, at most 15 points. */
    public static double coinChanceBonus(double luck) { return Math.min(0.15, 0.01 * luck(luck)); }

    /**
     * The chance that a kill or a chest also leaves a rolled piece of dungeon armour (on top of what the monster's own table gave): a base, +1.5 points per point of Luck, more
     * for a boss or a chest, a little more with depth, never above {@code MAX_ARMOUR}.
     */
    public static double armourChance(double luck, int floor, Kind kind) {
        double base = switch (kind) {
            case KILL -> 0.01;
            case BOSS -> 0.25;
            case CHEST -> 0.06;
        };
        double c = (base + 0.015 * luck(luck)) * (1.0 + depthFactor(floor));
        return Math.min(MAX_ARMOUR, c);
    }

    /** The hard ceiling on the armour chance. */
    public static final double MAX_ARMOUR = 0.5;

    /** What the chance is for: an ordinary kill, a boss kill, a chest. */
    public enum Kind { KILL, BOSS, CHEST }
}
