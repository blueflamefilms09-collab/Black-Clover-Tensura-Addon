package com.newuniverse.nusmp.skilltree.rarity;

import com.newuniverse.nusmp.dungeon.sentient.Dice;

/**
 * Why: the owner's rarity spec makes the three doors of the skill-book loot three different games (tier 1: the ordinary Abyssal monsters, a very small chance of a mostly common book; tier 2: the decennial
 * Guardians, a guaranteed themed book whose rarity grows with the depth; tier 3: the rift pocket chests, mythic books only) and scales them with Luck and Looting. Numbers like these are exactly what is
 * silently wrong when written inside event handlers (a Luck factor applied to common books, a chance read before the clamp, a NaN attribute that turns every roll into "never"), so they are all here, as pure
 * functions with no Minecraft type: the game glue reads the Luck attribute and the looting level ONCE per drop and passes plain numbers in, and the unit tests draw hundreds of thousands of scripted
 * values to see that the rates land on the weights.
 *
 * <p>The contract (docs/skillbooks_3_26.md section 1): {@link #luckFactor} = {@code min(20, 1 + 0.5 * max(0, luck) * rank)} (rank 0 is common, so common never changes); the tier-1 chance of a book is
 * {@code base * min(10, 1 + 0.25 * looting + 0.05 * max(0, luck))}; its rarity is drawn from the weights common 600, uncommon 300, rare 80, epic 10, mythic 0, each times {@link #luckFactor} (so an Epic is
 * about 0.001% a kill at Luck 0); the Guardian's rarity follows the depth ({@link #guardianRarity}); the rift chance is multiplied by {@code min(3, 1 + 0.1 * max(0, luck))}. Every input is sanitised: NaN
 * and infinity read as 0, a negative Luck or looting level counts as 0 (bad luck never makes a drop rarer than the base), every probability is clamped to 0..1. Nothing here allocates per call.
 */
public final class RarityRules {
    private RarityRules() {}

    /** The ceiling of the Luck factor of one rarity. */
    public static final double MAX_LUCK_FACTOR = 20.0;
    /** The ceiling of the Luck-and-Looting multiplier of the tier-1 chance. */
    public static final double MAX_CHANCE_FACTOR = 10.0;
    /** The ceiling of the Luck multiplier of the rift chance. */
    public static final double MAX_RIFT_FACTOR = 3.0;
    /** The tier-1 weights by rank: common, uncommon, rare, epic, mythic (a monster never drops a mythic book). */
    private static final int[] MONSTER_WEIGHTS = {600, 300, 80, 10, 0};
    /** Floors up to this depth (10 and 20) hand out rare books. */
    public static final int RARE_BAND_END = 20;
    /** Floors above {@link #RARE_BAND_END} up to this depth (30 and 40) hand out rare books with a chance of an epic one; deeper (50 and more) epic. */
    public static final int MIXED_BAND_END = 40;
    /** The chance of an epic book from a Guardian of floor 30 or 40. */
    public static final double MIXED_EPIC_CHANCE = 0.20;

    // ------------------------------------------------------------------ sanitising
    /** A number the rules can use: NaN is 0, infinities are clamped to a large finite value. */
    public static double sane(double v) {
        if (Double.isNaN(v)) return 0.0;
        return Math.max(-1.0e9, Math.min(1.0e9, v));
    }

    /** A probability: NaN and negatives are 0, above 1 is 1. */
    public static double unit(double p) {
        double s = sane(p);
        return s > 0.0 ? Math.min(1.0, s) : 0.0;
    }

    private static double luckOf(double luck) { return Math.max(0.0, sane(luck)); }

    // ------------------------------------------------------------------ the factors
    /** The weight multiplier of a rarity at a Luck: {@code min(20, 1 + 0.5 * max(0, luck) * rank)}; rank is clamped to 0..4, so common (0) is always 1. */
    public static double luckFactor(int rank, double luck) {
        int r = Math.max(0, Math.min(BookRarity.count() - 1, rank));
        return Math.min(MAX_LUCK_FACTOR, 1.0 + 0.5 * luckOf(luck) * r);
    }

    /** The multiplier of the tier-1 chance: {@code min(10, 1 + 0.25 * looting + 0.05 * max(0, luck))}; a negative looting level counts as 0. */
    public static double abyssalChanceFactor(int looting, double luck) {
        return Math.min(MAX_CHANCE_FACTOR, 1.0 + 0.25 * Math.max(0, looting) + 0.05 * luckOf(luck));
    }

    /** The chance that an ordinary Abyssal monster adds a book: the base chance times {@link #abyssalChanceFactor}, clamped to 0..1. */
    public static double abyssalBookChance(double base, int looting, double luck) {
        return unit(unit(base) * abyssalChanceFactor(looting, luck));
    }

    /** The multiplier of a rift chest's chance: {@code min(3, 1 + 0.1 * max(0, luck))}. */
    public static double riftFactor(double luck) {
        return Math.min(MAX_RIFT_FACTOR, 1.0 + 0.1 * luckOf(luck));
    }

    /** The chance of a rift chest (already scaled by the table and the server scale) at a Luck, clamped to 0..1. */
    public static double riftChance(double scaledChance, double luck) {
        return unit(unit(scaledChance) * riftFactor(luck));
    }

    // ------------------------------------------------------------------ tier 1: the monsters
    /** The base weight of a rarity in tier 1 (600 / 300 / 80 / 10 / 0). */
    public static int monsterWeight(BookRarity r) { return MONSTER_WEIGHTS[r.rank()]; }

    /** The weight of a rarity in tier 1 at a Luck: its base weight times {@link #luckFactor}. */
    public static double monsterWeight(BookRarity r, double luck) { return MONSTER_WEIGHTS[r.rank()] * luckFactor(r.rank(), luck); }

    /** The total of the tier-1 weights at a Luck. */
    public static double monsterTotal(double luck) {
        double t = 0.0;
        for (BookRarity r : BookRarity.values()) t += monsterWeight(r, luck);
        return t;
    }

    /** The probability of a rarity among the books an Abyssal monster drops, at a Luck (0 for mythic). */
    public static double monsterShare(BookRarity r, double luck) { return monsterWeight(r, luck) / monsterTotal(luck); }

    /** Draws the rarity of a monster's book at a Luck: one dice value; never mythic. */
    public static BookRarity drawMonsterRarity(double luck, Dice dice) {
        double total = monsterTotal(luck);
        double pick = (dice == null ? 0.0 : unit(dice.next())) * total;
        BookRarity last = BookRarity.COMMON;
        for (BookRarity r : BookRarity.values()) {
            double w = monsterWeight(r, luck);
            if (w <= 0.0) continue;
            last = r;
            pick -= w;
            if (pick < 0.0) return r;
        }
        return last;
    }

    // ------------------------------------------------------------------ tier 2: the Guardians
    /**
     * The rarity of the book a Guardian of this floor drops: floor 10 and 20 rare; 30 and 40 rare, with a 20% chance of an epic; 50 and deeper epic. The sign of the floor (the road) does not matter. One dice
     * value is used only in the mixed band.
     */
    public static BookRarity guardianRarity(int floor, Dice dice) {
        int depth = floor == Integer.MIN_VALUE ? Integer.MAX_VALUE : Math.abs(floor);
        if (depth <= RARE_BAND_END) return BookRarity.RARE;
        if (depth <= MIXED_BAND_END) return dice != null && dice.chance(MIXED_EPIC_CHANCE) ? BookRarity.EPIC : BookRarity.RARE;
        return BookRarity.EPIC;
    }

    // ------------------------------------------------------------------ tier 3: the rifts
    /** The rarity a rift chest hands out: mythic while the opener has a mythic book left to learn, else epic. */
    public static BookRarity riftRarity(boolean mythicLeft) { return mythicLeft ? BookRarity.MYTHIC : BookRarity.EPIC; }

    /** Can a book of this rarity come out of a monster or a Guardian? (mythic only comes from the rifts, the shop's flagship, the command and the creative tab) */
    public static boolean dropsFromMonsters(BookRarity r) { return r != BookRarity.MYTHIC; }
}
