package com.newuniverse.nusmp.elite.forge.rules;

/**
 * Why: this is the quality roll of the forge, kept as one pure class so it can be tested to the number and so the station screen can show the player what
 * a roll is made of. The original addon's rule was not available, so the numbers are designed here:
 * <pre>
 * points = recipe base (0..30) + 50 x minigame score (0..1) + station bonus (0/4/8/13) + premium materials (cap 8)
 *        + skill bonus (cap 16) + specialization (0 or 6) + 4 x luck (-1..1)
 * </pre>
 * and the quality is the highest whose threshold the points reach (30 Uncommon, 45 Rare, 60 Epic, 75 Legendary, 92 Mythical), then CAPPED by the station's ceiling
 * (Basic Rare, Magisteel Epic, Mythril Legendary, Dragonforge Mythical). Playing the minigames badly (score under 0.10) can never go above Common, so the
 * flat bonuses cannot carry an item the player did not work on. Playing them perfectly at a Basic station without any help gives 50 + the recipe base (an Epic
 * number) which the Basic ceiling holds at Rare: the player has a reason to climb the station tiers. The luck input is passed in (not drawn here) so the class is
 * deterministic. No Minecraft classes.
 */
public final class QualityRoller {
    private QualityRoller() {}

    /** Most points the premium materials can add. */
    public static final int MATERIAL_CAP = 8;
    /** Most points the player's skills can add (the table in {@link SkillBonusTable} applies its own cap too). */
    public static final int SKILL_CAP = 16;
    /** Points a perfect minigame score is worth. */
    public static final double SCORE_POINTS = 50.0;
    /** Points a luck of +1 (or -1) is worth. */
    public static final double LUCK_POINTS = 4.0;
    /** Below this minigame score the result is Common whatever the bonuses. */
    public static final double EFFORT_FLOOR = 0.10;

    /** Everything one roll depends on. {@code score} is 0..1; {@code luck} is -1..1. */
    public record Inputs(double score, StationTier tier, int baseQuality, int materialPoints, int skillPoints, int specPoints, double luck) {}

    /** The outcome: the quality, the points it came from, the quality before the station's cap and whether the cap held it back. */
    public record Result(Quality quality, double points, Quality uncapped, boolean cappedByStation) {}

    /** The roll total (before the quality lookup and the cap). */
    public static double points(Inputs in) {
        double score = clamp(in.score(), 0.0, 1.0);
        double luck = clamp(in.luck(), -1.0, 1.0);
        double base = clamp(in.baseQuality(), 0, 30);
        double p = base + SCORE_POINTS * score + in.tier().qualityBonus()
                + clamp(in.materialPoints(), 0, MATERIAL_CAP) + clamp(in.skillPoints(), 0, SKILL_CAP) + clamp(in.specPoints(), 0, 12) + LUCK_POINTS * luck;
        if (score < EFFORT_FLOOR) p = Math.min(p, Quality.UNCOMMON.minPoints() - 1);
        return Math.max(0.0, p);
    }

    public static Result roll(Inputs in) {
        double p = points(in);
        Quality raw = Quality.byPoints(p);
        Quality capped = Quality.min(raw, in.tier().ceiling());
        return new Result(capped, p, raw, capped != raw);
    }

    /** Maps a uniform random number in [0, 1) to a luck in [-1, 1). */
    public static double luckFrom(double unit) { return clamp(unit, 0.0, 1.0) * 2.0 - 1.0; }

    private static double clamp(double v, double lo, double hi) { return v < lo ? lo : Math.min(v, hi); }
}
