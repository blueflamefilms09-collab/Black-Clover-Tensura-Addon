package com.newuniverse.nusmp.labyrinth.mega.gear;

/**
 * Why: the five numbers a server owner tunes (the base chance of each grade per kill at floor 1, how fast the grade mix moves with depth, and the
 * Looting bonus) are kept apart from the SHAPES in {@code tables.json} (what items, which curve, which caps) because they come from different places
 * and change for different reasons: the shapes are design data that a datapack may replace, the rates are the knobs of one server's config file. The
 * roller takes both, so the tests can pass the defaults without a config and the game reads the config into this record at the moment of a roll
 * ({@code GearConfig.rates()}). The defaults live here and nowhere else: {@code GearConfig} reads them from {@link #DEFAULT}, so the config and the
 * tests cannot drift apart. Pure.
 *
 * @param normal       chance per kill that a Normal piece drops at floor 1 (before Looting)
 * @param special      the same for Special
 * @param rare         the same for Rare
 * @param floorShift   progress along the grade curves per floor above the first: 0.02 reaches the end of the curves (x = 1) at floor 51; 0 turns the shift off
 * @param lootingBonus absolute chance added to the TOTAL per Looting level, shared out in proportion to the grades' own chances
 */
public record GearRates(double normal, double special, double rare, double floorShift, double lootingBonus) {
    /** The shipped defaults: one piece in about fourteen kills at floor 1, mostly Normal. */
    public static final GearRates DEFAULT = new GearRates(0.05, 0.015, 0.004, 0.02, 0.01);

    /**
     * The shipped defaults of a CHEST roll (1.55.0): a chest is a deliberate reward found a handful of times a floor, so it pays about four to five times as often
     * as one kill (22 / 6 / 2 percent against 5 / 1.5 / 0.4): roughly three pieces in ten standard chests, a Rare one in fifty. The three are in the same
     * proportion as the kills' (about 73 / 20 / 7 percent of the pieces at floor 1) on purpose: the depth curves of {@code tables.json} were shaped for that mix, and
     * with it the chance of a piece stays within 3 percent of 30 percent at every depth, so going deeper changes WHAT a chest holds and not how often (a 20 / 8 / 2
     * split drifted by 11 percent, which a test noticed). The depth shift and the Looting bonus (which counts per level of Luck for a chest) are the kills' knobs.
     */
    public static final GearRates CHEST_DEFAULT = new GearRates(0.22, 0.06, 0.02, DEFAULT.floorShift(), DEFAULT.lootingBonus());

    /** What a TREASURE chest multiplies the Special and Rare chances by (the config key {@code chestTreasureFactor}). */
    public static final double DEFAULT_TREASURE_FACTOR = 2.0;

    public double base(GearGrade g) {
        return switch (g) {
            case NORMAL -> normal;
            case SPECIAL -> special;
            case RARE -> rare;
        };
    }
}
