package com.newuniverse.nusmp.relics.wave;

import java.util.List;
import java.util.Locale;
import java.util.function.IntUnaryOperator;

/**
 * Wave 17 (notebooks, pouches, charms): the pure rules. The dice, the study table of the five Ancient Notebooks, the Life's Emergency store, the
 * Sheriff's bluff, the wild magic table of Martel's sweets, the cupcake's hunger floor and the corrosion of the urn, with no Minecraft types so they can be
 * tested alone. Dice are passed in as {@code roll(sides) -> 0..sides-1} (the way RandomSource.nextInt answers).
 */
public final class Wave17Rules {
    private Wave17Rules() {}

    // ---------------------------------------------------------------- dice
    /** One die of {@code sides} sides, 1..sides. */
    public static int die(IntUnaryOperator roll, int sides) { return roll.applyAsInt(Math.max(1, sides)) + 1; }

    public static int dice(IntUnaryOperator roll, int count, int sides) {
        int t = 0;
        for (int i = 0; i < count; i++) t += die(roll, sides);
        return t;
    }

    // ================================================================ Ancient Notebooks
    /** The card's "1d4-1": 0 to 3, how much the book had to say. */
    public static int studyBonus(IntUnaryOperator roll) { return die(roll, 4) - 1; }

    /** How long a study lasts: two minutes, plus one for every point of the bonus (so two to five minutes). */
    public static int studyTicks(int bonus) { return 2400 + 1200 * clamp(bonus, 0, 3); }

    /** Ticks before the same notebook can be consulted again: longer than the longest study, so no one is ever permanently "studied". */
    public static final int STUDY_COOLDOWN = 7200, STUDY_HOLD = 60;

    /** The five notebooks, the check each one helps with, and what it means in the game. */
    public enum Notebook {
        ARCANIST("arcanists_notebook", "Arcana"),
        BOTANIST("botanists_notebook", "Nature"),
        HISTORIAN("historians_notebook", "History"),
        PHYSICIAN("physicians_notebook", "Medicine"),
        THEOLOGIAN("theologians_notebook", "Religion");

        public final String id, check;

        Notebook(String id, String check) {
            this.id = id;
            this.check = check;
        }

        public static Notebook byId(String id) {
            for (Notebook n : values()) if (n.id.equals(id)) return n;
            return null;
        }
    }

    /** Experience a Historian's Notebook is worth. */
    public static int historianXp(int bonus) { return 5 + 5 * clamp(bonus, 0, 3); }

    /** Hit points a Physician's Notebook tends. */
    public static float physicianHeal(int bonus) { return 4f + 2f * clamp(bonus, 0, 3); }

    /** Hearts-worth of absorption of a Theologian's blessing (in health points). */
    public static float theologianWard(int bonus) { return 2f + 2f * clamp(bonus, 0, 3); }

    /** The Luck amplifier of a Botanist's Notebook, or -1 when the book had nothing to say (bonus 0). */
    public static int botanistLuck(int bonus) { return clamp(bonus, 0, 3) - 1; }

    // ================================================================ Life's Emergency
    /** Stored life at most, by grade tier (the card's table: Uncommon 10, Rare 30, Very rare 50, Very rare 70, Legendary 90; tier 1 = Normal). */
    public static int lifeMax(int gradeTier) {
        return switch (gradeTier) {
            case 1, 2 -> 10;
            case 3 -> 30;
            case 4 -> 50;
            case 5 -> 70;
            default -> 90;
        };
    }

    /** What is absorbed at each dawn. */
    public static final int LIFE_PER_DAWN = 10;
    /** One stored point restores this many health points when it is spent (so 50 stored is a full 20-point heart bar). */
    public static final double LIFE_TO_HEALTH = 0.4;
    /** A relic left in a chest for a month is just full: dawns counted at most this many at a time. */
    public static final int LIFE_CATCHUP_DAYS = 9;

    /** The store after the dawns that passed since {@code lastDay}: +10 each, never above {@code max}, nothing for a clock that runs backwards. */
    public static int lifeRoll(int stored, long lastDay, long today, int max) {
        int s = clamp(stored, 0, max);
        if (today <= lastDay) return s;
        long days = Math.min(LIFE_CATCHUP_DAYS, today - lastDay);
        return (int) Math.min(max, s + days * LIFE_PER_DAWN);
    }

    /** Health points restored when the store is spent at death's door: at least a heart's half, never more than the wearer's maximum. */
    public static float lifeHeal(int stored, float maxHealth) {
        if (stored <= 0) return 0f;
        return (float) Math.min(maxHealth, Math.max(1.0, stored * LIFE_TO_HEALTH));
    }

    // ================================================================ Plastic Sheriff Badge
    public static final int BLUFF_BONUS = 3;

    /** The card's +3 to Bluff: a d20 plus the bonus against the target's resolve. */
    public static boolean bluff(int d20, int dc) { return d20 + BLUFF_BONUS >= dc; }

    /** The resolve of a target: 10 for an ordinary creature, higher for the big ones (up to 18 for a boss with 200 health or more). */
    public static int bluffDc(double maxHealth) { return 10 + clamp((int) (maxHealth / 25.0), 0, 8); }

    /** How long a believed bluff keeps the raiders off (ticks). */
    public static final int BLUFF_CALM_TICKS = 200;

    // ================================================================ Fury of the Fallen
    /** Below this share of the maximum health the badge lights up. */
    public static final double FURY_BELOW = 0.20;

    public static boolean furyActive(float health, float maxHealth) { return maxHealth > 0f && health > 0f && health < maxHealth * FURY_BELOW; }

    public static float furyDamage(float amount) { return amount * 2f; }

    // ================================================================ Madama Cupcake
    /** Three Minecraft days of nourishment. */
    public static final long CUPCAKE_TICKS = 72000L;
    /** While it lasts hunger never drops below this: never starving, yet never full, so you can still eat. */
    public static final int CUPCAKE_FLOOR = 17;

    public static int cupcakeFood(int food, long now, long until) { return now < until && food < CUPCAKE_FLOOR ? CUPCAKE_FLOOR : food; }

    // ================================================================ Fast Travel Beads
    public static final int MAX_MARKS = 6, MAX_COMPANIONS = 6;
    public static final double COMPANION_RANGE = 6.0;

    /** "This chain holds 3D10 worth of beads." */
    public static int rollBeads(IntUnaryOperator roll) { return dice(roll, 3, 10); }

    /**
     * The destination picked on the chain: 0 is your bed, 1 the place you last fell, then {@code saved} marks of your own, then one "new mark" slot that
     * saves the place you stand on. Cycles through all of them.
     */
    public static int nextChoice(int current, int saved) {
        int total = saved + 3;
        return ((current % total) + total + 1) % total;
    }

    public static int choiceCount(int saved) { return saved + 3; }

    /** The saved mark an index stands for, or -1 for the bed (0), the grave (1) and the new-mark slot (the last). */
    public static int savedIndex(int choice, int saved) { return choice >= 2 && choice < 2 + saved ? choice - 2 : -1; }

    public static boolean isNewMark(int choice, int saved) { return choice == 2 + saved; }

    /** Where a new mark goes: replaces the nearest old one if one is within 8 blocks, else is added, else drops the oldest. Returns the index it takes in a list of {@code size}. */
    public static int markSlot(int size, int nearestIndex, double nearestDistance) {
        if (nearestIndex >= 0 && nearestIndex < size && nearestDistance <= 8.0) return nearestIndex;
        return size < MAX_MARKS ? size : -1;     // -1: drop the first and add at the end
    }

    // ================================================================ Urn of Black Pudding
    /** The card's 1d8 acid, as health points: one die, times one and a half. */
    public static float urnDamage(IntUnaryOperator roll) { return die(roll, 8) * 1.5f; }

    /** Each blow of the acid eats this share of a nonmagical armour piece's durability. */
    public static final double ACID_SHARE = 0.15;
    public static final int PUDDING_HEALTH = 6, PUDDING_TICKS = 1200;
    public static final double URN_RANGE = 12.0;

    /** The new damage value of an armour piece after one blow; the piece is "destroyed" when it reaches its maximum. */
    public static int corrode(int damage, int maxDamage) {
        if (maxDamage <= 0) return damage;
        return Math.min(maxDamage, damage + Math.max(1, (int) Math.ceil(maxDamage * ACID_SHARE)));
    }

    // ================================================================ Crystal Dragon
    public static final int DRAGON_TICKS = 160, DRAGON_BREATH_EVERY = 20;
    public static final float DRAGON_BREATH_DAMAGE = 4.0f;
    /** Most a player can lose to one dragon in all (30% of the health), so the illusion is safe to use against people. */
    public static final double DRAGON_PLAYER_BUDGET = 0.30;

    // ================================================================ Martel's Magical Bag of Candy
    public static final int SWEETS_PER_BAG = 20;

    /** The entries of the wild magic table (a d20), in order. */
    public enum Surge {
        GLOW, FEATHER, LEAP, LEVITATE, HEAL, SWIFT, QUEASY, VANISH, WARD, LESSON, HOP, SNEEZE, SLUGGISH, SIGHT, MEND, AEGIS, HASTE, SHOVE, FORTUNE, SURGE_TWICE;

        public String label() { return name().substring(0, 1) + name().substring(1).toLowerCase(Locale.ROOT).replace('_', ' '); }
    }

    public static Surge surge(int d20) { return Surge.values()[clamp(d20, 1, 20) - 1]; }

    /** The surges of one sweet: one roll, or two when the roll is the last entry (a second "twice" is not rolled again: it is read as Fortune). */
    public static List<Surge> sweet(IntUnaryOperator roll) {
        Surge first = surge(die(roll, 20));
        if (first != Surge.SURGE_TWICE) return List.of(first);
        Surge a = surge(die(roll, 20)), b = surge(die(roll, 20));
        if (a == Surge.SURGE_TWICE) a = Surge.FORTUNE;
        if (b == Surge.SURGE_TWICE) b = Surge.FORTUNE;
        return List.of(a, b);
    }

    // ================================================================ The Devil's Due
    public static final List<String> DEVIL_TYPES = List.of("tensura:lesser_daemon", "minecraft:vex");
    public static final int DEVIL_TICKS = 6000;

    // ================================================================ pouches
    /** Rows of nine: a pouch is two rows, the shared pouches (a small backpack) three. */
    public static final int POUCH_ROWS = 2, BACKPACK_ROWS = 3;

    public static int slots(int rows) { return clamp(rows, 1, 6) * 9; }

    // ---------------------------------------------------------------- helpers
    public static int clamp(int v, int lo, int hi) { return Math.max(lo, Math.min(hi, v)); }
}
