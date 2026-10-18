package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;

/**
 * Why: "picks mechanics by weights from the floor's tier, road, theme, threat budget": these are the numbers, in one pure class so JUnit pins them. A mechanic outside the
 * floor's tier or on the other road has weight 0 (never drawn); a theme it likes triples its weight and a theme it does not like (when it names some) cuts it to a third, so
 * a Necropolis floor mostly draws the grave-and-curse mechanics without ever being forbidden the others; the kind of room it would sit in biases the category (a puzzle room
 * draws puzzles, a trap room hazards, a trial room combat gimmicks, a treasure room risk-and-reward). The threat budget grows with the tier: a deeper floor can carry more, and
 * more expensive, mechanics.
 */
public final class Weights {
    private Weights() {}

    /** Threat budget of one floor, spent by the cost of each mechanic it carries. Tier 1: 12, tier 5: 28. */
    public static int floorBudget(int tier) { return 8 + 4 * Math.max(1, Math.min(Scaling.TIERS, tier)); }

    /** Extra budget the arena of a boss floor may spend on boss mechanics, on top of the floor's. */
    public static int bossBudget(int tier) { return 4 + 2 * Math.max(1, Math.min(Scaling.TIERS, tier)); }

    /** The weight (0 = never) of a mechanic for a floor, road, theme and room-kind category bias. */
    public static double weight(MechanicDef d, int floor, Polarity road, String theme, double bias) {
        if (!d.fitsTier(Scaling.tierOf(floor))) return 0.0;
        if (!d.road().allows(road)) return 0.0;
        double w = d.weight();
        if (theme != null && !d.themes().isEmpty()) w *= d.themes().contains(theme) ? 3.0 : 0.35;
        return w * Math.max(0.0, bias);
    }

    /** The multiplier of a category for a room of the kind (folder name of the RoomKind: hall, trap, puzzle, trial, treasure, cavern, set, corridor ...). */
    public static double roomBias(String kind, MechCategory c) {
        if (kind == null) return 1.0;
        switch (kind) {
            case "puzzle": return c == MechCategory.PUZZLE ? 4.0 : c == MechCategory.HAZARD ? 1.5 : c == MechCategory.PARTY ? 1.3 : 1.0;
            case "trap": return c == MechCategory.HAZARD ? 4.0 : c == MechCategory.TRAVEL ? 1.3 : 0.8;
            case "trial": return c == MechCategory.COMBAT ? 4.0 : c == MechCategory.PARTY ? 1.5 : 0.8;
            case "treasure": return c == MechCategory.RISK ? 4.0 : c == MechCategory.HAZARD ? 1.5 : 0.7;
            case "cavern": return c == MechCategory.HAZARD || c == MechCategory.TRAVEL ? 2.0 : c == MechCategory.COMBAT ? 1.5 : 1.0;
            case "set": return c == MechCategory.PARTY ? 2.0 : c == MechCategory.BOSS ? 0.0 : 1.0;
            case "corridor":
            case "junction": return c == MechCategory.TRAVEL ? 3.0 : c == MechCategory.HAZARD ? 2.0 : c == MechCategory.PARTY ? 0.7 : c == MechCategory.PUZZLE ? 0.5 : 1.0;
            case "arena": return c == MechCategory.BOSS ? 5.0 : 0.0;
            case "hall": return c == MechCategory.TRAVEL ? 0.8 : 1.5;
            default: return 1.0;
        }
    }
}
