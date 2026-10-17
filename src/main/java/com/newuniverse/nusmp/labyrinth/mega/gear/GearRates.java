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

    public double base(GearGrade g) {
        return switch (g) {
            case NORMAL -> normal;
            case SPECIAL -> special;
            case RARE -> rare;
        };
    }
}
