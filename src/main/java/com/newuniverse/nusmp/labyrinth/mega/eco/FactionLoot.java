package com.newuniverse.nusmp.labyrinth.mega.eco;

import com.newuniverse.nusmp.dungeon.sentient.Dice;
import com.newuniverse.nusmp.dungeon.sentient.StoneTable;
import com.newuniverse.nusmp.labyrinth.dnd.economy.EconomyMath;

/**
 * Why: the 1.36.0 economy pays every monster the same way whatever it is and wherever it stands; the Mega-Dungeon adds two things that depend on
 * WHICH faction a monster belongs to and HOW DEEP the sector is, and both are plain numbers that can be balanced and tested without a server.
 * (1) The faction's own cut: a beast leaves a haunch, a slime jelly, a fish-thing a fillet (the five raw cuts of {@code DungeonFood}), with the
 * chance the faction file sets, rising a little with the sector's threat and with Looting, and never more than the configured extra count: the
 * point is a reason to cook what each region lives on, not a flood of meat. Creatures with no flesh (skeletons, blazes, golems) still leave none,
 * the same {@link EconomyMath#fleshless} rule as the base economy. (2) The depth bonus: one extra Magic Stone with a chance of {@code perThreat}
 * per threat level (2 % at the default), and in deeper bands the extra item is a crystal instead, but each step up the grade divides the
 * chance, so a deep sector pays better items, not more of them (a medium crystal is a juggernaut's drop in the base economy and must stay rare).
 * Both are player kills only, like the base meat: a monster that died to another monster in a turf war is not a farm. The result reuses
 * {@link StoneTable.Haul}, the record the base economy already turns into item entities. The numbers are a first pass nobody has played.
 * Pure (no Minecraft types); randomness comes in through {@link Dice}.
 */
public final class FactionLoot {
    private FactionLoot() {}

    /** The base economy's item ids ({@code EconomyDrops} keeps them private, so they are repeated here and a test compares the two files). */
    public static final String STONE_PATH = "magic_stone", LOW_PATH = "low_quality_magic_crystal", MEDIUM_PATH = "medium_quality_magic_crystal", HIGH_PATH = "high_quality_magic_crystal";

    /** The most the meat chance can ever reach, however deep the sector and however good the Looting. */
    public static final double MAX_MEAT_CHANCE = 0.90;
    /** Relative meat chance per threat level above 1 (3 percent of itself), and absolute chance per Looting level (the base economy's own 5 points). */
    public static final double MEAT_PER_THREAT = 0.03, MEAT_PER_LOOTING = 0.05;
    /** The stone bonus never passes this chance before the grade divides it. */
    public static final double STONE_BONUS_CAP = 0.50;
    /** Threat from which the bonus item is a low, medium or high quality crystal instead of a stone. */
    public static final int LOW_FROM = 6, MEDIUM_FROM = 14, HIGH_FROM = 26;
    /** What the bonus chance is divided by in each band (stone, low, medium, high): a better item is a rarer one. */
    private static final double[] RARITY = {1.0, 4.0, 16.0, 64.0};

    /** Why: one return value for the whole roll of a death, so the drop listener turns it into item entities in one place: a number of a raw cut ({@link EconomyMath} kind) and a stone haul on top of the base economy. */
    public record Extra(int meatKind, int meat, StoneTable.Haul stones) {
        public static final Extra NONE = new Extra(EconomyMath.PLAIN, 0, StoneTable.Haul.NONE);

        public boolean isEmpty() { return meat <= 0 && stones.isEmpty(); }
    }

    /** The chance of the faction's cut: its base, +3 % of itself per threat level above 1, +5 points per Looting level, at most {@link #MAX_MEAT_CHANCE}; a faction with no base chance never leaves one. */
    public static double meatChance(double base, int threat, int looting) {
        if (base <= 0.0) return 0.0;
        double scaled = base * (1.0 + MEAT_PER_THREAT * Math.max(0, threat - 1)) + MEAT_PER_LOOTING * Math.max(0, looting);
        return Math.min(MAX_MEAT_CHANCE, scaled);
    }

    /** How many cuts: one; one more with a chance that grows from threat 6 (up to one in two); one more with Looting; never above {@code maxExtra} (0 gives none). */
    public static int meatCount(int threat, int looting, int maxExtra, Dice dice) {
        if (maxExtra <= 0) return 0;
        int n = 1;
        if (threat >= 6 && dice.chance(Math.min(0.5, 0.04 * (threat - 5)))) n++;
        if (looting > 0 && dice.chance(Math.min(0.9, 0.25 * looting))) n++;
        return Math.min(n, maxExtra);
    }

    /** 0 stone, 1 low, 2 medium, 3 high quality crystal: the band the sector's threat puts the bonus item in. */
    public static int band(int threat) {
        if (threat >= HIGH_FROM) return 3;
        if (threat >= MEDIUM_FROM) return 2;
        return threat >= LOW_FROM ? 1 : 0;
    }

    /** The chance that a kill in a sector of this threat adds the bonus item: {@code perThreat * threat}, capped, divided by the band's rarity. */
    public static double stoneBonusChance(int threat, double perThreat) {
        if (threat <= 0 || perThreat <= 0.0) return 0.0;
        return Math.min(STONE_BONUS_CAP, perThreat * threat) / RARITY[band(threat)];
    }

    /** The depth bonus of one kill: nothing, or exactly one item of the sector's band. */
    public static StoneTable.Haul stoneBonus(int threat, double perThreat, Dice dice) {
        if (!dice.chance(stoneBonusChance(threat, perThreat))) return StoneTable.Haul.NONE;
        return switch (band(threat)) {
            case 0 -> new StoneTable.Haul(1, 0, 0, 0);
            case 1 -> new StoneTable.Haul(0, 1, 0, 0);
            case 2 -> new StoneTable.Haul(0, 0, 1, 0);
            default -> new StoneTable.Haul(0, 0, 0, 1);
        };
    }

    /**
     * Everything the faction adds to one death. Nothing unless a player did the killing; the cut needs flesh and a successful roll; the stone bonus
     * is independent of the cut. {@code entityPath} is the registry path of the monster ({@code zombie}).
     */
    public static Extra roll(FactionDef def, String entityPath, int threat, int looting, boolean playerKill, int maxExtraMeat, double stonePerThreat, Dice dice) {
        if (!playerKill || def == null) return Extra.NONE;
        int meat = 0;
        if (!EconomyMath.fleshless(entityPath) && dice.chance(meatChance(def.meatChance(), threat, looting))) meat = meatCount(threat, looting, maxExtraMeat, dice);
        return new Extra(def.cutKind(), meat, stoneBonus(threat, stonePerThreat, dice));
    }
}
