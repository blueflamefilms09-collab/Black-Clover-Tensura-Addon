package com.newuniverse.nusmp.labyrinth.loot;

import java.util.Locale;

/**
 * Why: the rarity of a piece of loot gear is the one number that decides how many stat lines it has (two to four), how strong they are and where in its grade's damage
 * range a weapon sits, so it is a pure enum that the roll, the drop tables, the tooltip and the sell price all read. Five tiers (common, uncommon, rare, epic, legendary);
 * the weapon's damage GRADE follows the tier through the owner's damage table ({@code relics/GradeDamage}: normal 20-30, special 35-55, rare 60-85): common and uncommon
 * weapons are normal grade, rare and epic are special grade, legendary is rare grade, so the base damage of a dungeon weapon is always inside the range its grade owns.
 * Saved by {@link #id()} (never by ordinal); entries are only ever appended.
 */
public enum LootRarity {
    COMMON("common", 1.0, 2, 2, 0, 1.0, "WHITE"),
    UNCOMMON("uncommon", 1.3, 2, 3, 0, 2.0, "GREEN"),
    RARE("rare", 1.7, 3, 3, 1, 4.0, "AQUA"),
    EPIC("epic", 2.3, 3, 4, 1, 8.0, "LIGHT_PURPLE"),
    LEGENDARY("legendary", 3.2, 4, 4, 2, 18.0, "GOLD");

    private final String id;
    private final double magnitude;
    private final int minLines, maxLines, grade;
    private final double sellFactor;
    private final String colour;

    LootRarity(String id, double magnitude, int minLines, int maxLines, int grade, double sellFactor, String colour) {
        this.id = id;
        this.magnitude = magnitude;
        this.minLines = minLines;
        this.maxLines = maxLines;
        this.grade = grade;
        this.sellFactor = sellFactor;
        this.colour = colour;
    }

    public String id() { return id; }

    /** How much stronger than a common roll every stat line of this tier is. */
    public double magnitude() { return magnitude; }

    public int minLines() { return minLines; }

    public int maxLines() { return maxLines; }

    /** The ordinal of the damage grade (0 normal, 1 special, 2 rare) a weapon of this tier belongs to, for {@code GradeDamage}. */
    public int grade() { return grade; }

    /** What a piece of this tier sells for, as a multiple of the item's base value. */
    public double sellFactor() { return sellFactor; }

    /** The name of the {@code ChatFormatting} colour of the tier's name (kept as text so this class has no Minecraft import). */
    public String colour() { return colour; }

    public static LootRarity fromId(String id) {
        if (id == null) return null;
        String s = id.trim().toLowerCase(Locale.ROOT);
        for (LootRarity r : values()) if (r.id.equals(s)) return r;
        return null;
    }

    // ---------------------------------------------------------------- the weights
    /** Weights of the five tiers (common first) by depth band 0..4: going deeper moves weight from common to the tiers above. */
    private static final double[][] BAND_WEIGHTS = {
            {72, 24, 4, 0, 0},
            {55, 30, 12, 3, 0},
            {38, 32, 21, 8, 1},
            {22, 32, 28, 15, 3},
            {10, 25, 32, 25, 8}};
    private static final double[] LUCK_SHIFT = {-3.0, 1.0, 1.0, 0.7, 0.3};

    /** The weights of the tiers at a band (clamped to 0..4) with this much luck: each point of luck moves three weight from common to the tiers above (common never below 2). */
    public static double[] weights(int band, double luck) {
        double[] w = BAND_WEIGHTS[Math.max(0, Math.min(4, band))].clone();
        double l = Math.max(0.0, Math.min(10.0, luck));
        double common = Math.max(2.0, w[0] + LUCK_SHIFT[0] * l);
        double moved = w[0] - common;
        w[0] = common;
        double share = LUCK_SHIFT[1] + LUCK_SHIFT[2] + LUCK_SHIFT[3] + LUCK_SHIFT[4];
        for (int i = 1; i < 5; i++) w[i] += moved * LUCK_SHIFT[i] / share;
        return w;
    }

    /** The tier a roll {@code u} in [0, 1) picks at this band and luck. */
    public static LootRarity pick(double u, int band, double luck) {
        double[] w = weights(band, luck);
        double sum = 0.0;
        for (double x : w) sum += x;
        double edge = 0.0, target = Math.max(0.0, Math.min(0.999999999, u)) * sum;
        for (int i = 0; i < 5; i++) {
            edge += w[i];
            if (target < edge) return values()[i];
        }
        return COMMON;
    }
}
