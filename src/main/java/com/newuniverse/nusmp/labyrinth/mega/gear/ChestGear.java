package com.newuniverse.nusmp.labyrinth.mega.gear;

import com.newuniverse.nusmp.dungeon.sentient.Dice;

import java.util.Optional;

/**
 * Why: the chest side of the standard gear is one decision, "does this chest hold a piece, and what", and it is the same arithmetic as a kill's, only with
 * different base chances: a chest is a deliberate reward, found a handful of times a floor, so it pays far more often than one monster does (the defaults are
 * 20 / 8 / 2 percent against 5 / 1.5 / 0.4 for a kill) while the shape stays the same, so the roller is REUSED, not copied: {@link GearRoller#roll} takes the
 * chest's base chances through a {@link GearRates}, applies the same depth curves and caps of {@code tables.json} (a deeper chest holds better grades, a
 * Normal piece gets rarer, Rare keeps rising) and lays the three grades end to end on ONE roll, so a chest never yields more than ONE piece, however
 * generous the config. A TREASURE chest ({@link ChestRules.Kind#TREASURE}) multiplies only the Special and Rare chances by {@code treasureFactor}: a treasure
 * chest should hold something BETTER, not simply more of everything, and the Normal chance stays what it was (the grade caps and the total cap of the table still hold,
 * so a mistyped factor cannot turn every treasure chest into a shower). Luck (a lucky player opening the chest) counts like Looting does for a kill: it adds the
 * configured Looting bonus per level to the TOTAL and keeps the mix. Nothing here rolls a loot table, adds a relic or touches the ledger, so it cannot loop.
 * No Minecraft type, so the distribution is tested the way the kill rolls are (a hundred thousand seeded rolls against the table).
 */
public final class ChestGear {
    private ChestGear() {}

    /** The most Luck levels that count: the bonus is per level and the table's total cap holds anyway, but a silly attribute value must not matter. */
    public static final int MAX_LUCK_LEVELS = 10;

    /**
     * The base chances a chest of this kind rolls with: the configured chest chances, with Special and Rare multiplied for a treasure chest. The depth
     * shift and the Looting bonus are the ones of the kills (they are the same knobs); only the three bases differ.
     *
     * @param base           the chest bases ({@code chestNormal}, {@code chestSpecial}, {@code chestRare}) with the shared shift and Looting bonus
     * @param treasureFactor the multiplier of Special and Rare in a treasure chest (1 or less changes nothing)
     */
    public static GearRates ratesFor(ChestRules.Kind kind, GearRates base, double treasureFactor) {
        if (kind != ChestRules.Kind.TREASURE) return base;
        double f = Math.max(1.0, treasureFactor);
        return new GearRates(base.normal(), base.special() * f, base.rare() * f, base.floorShift(), base.lootingBonus());
    }

    /** The chance of each grade for a chest of this kind at this floor (indexed by {@link GearGrade#ordinal()}); all zero for a table that is no chest. */
    public static double[] chances(ChestRules.Kind kind, GearRates base, double treasureFactor, GearTables t, int floor, int luckLevels) {
        if (kind == null || kind == ChestRules.Kind.NONE) return new double[GearGrade.values().length];
        return GearRoller.chances(ratesFor(kind, base, treasureFactor), t, floor, clampLuck(luckLevels));
    }

    /**
     * Does this chest hold a piece, and which: empty for a table that is no chest, otherwise one roll over the chest's chances (at most one piece) and the
     * ordinary {@link GearRoller#make} for the piece. The same random stream gives the same answer.
     */
    public static Optional<GearSpec> decide(Dice d, ChestRules.Kind kind, GearRates base, double treasureFactor, GearTables t, int floor, int luckLevels) {
        if (kind == null || kind == ChestRules.Kind.NONE) return Optional.empty();
        return GearRoller.roll(d, floor, clampLuck(luckLevels), t, ratesFor(kind, base, treasureFactor));
    }

    /** The Luck of a context (a float, from the opener's Luck attribute and potion) as whole levels: rounded, never negative, never above {@link #MAX_LUCK_LEVELS}. */
    public static int luckLevels(float luck) {
        if (Float.isNaN(luck) || luck <= 0f) return 0;
        return Math.min(MAX_LUCK_LEVELS, Math.round(luck));
    }

    private static int clampLuck(int levels) { return Math.max(0, Math.min(MAX_LUCK_LEVELS, levels)); }
}
