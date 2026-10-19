package com.newuniverse.nusmp.item;

/**
 * 1.97.0: the pure numbers of Elsdocia (no Minecraft types, so JUnit can run them); {@link ElsdociaRules} is the public face.
 */
public final class ElsdociaMath {
    private ElsdociaMath() {}

    /** Base damage of the first owner (the explicit exception to the generic Genesis range). */
    public static final int BASE_DAMAGE = 400;
    /** Damage gained each time it is passed to a new, distinct Grand Magic rank (or higher) owner. */
    public static final int GRAND_STEP_DAMAGE = 25;
    /** {@code KnightRank.step}: Grand Magic Knight = 15, Wizard King = 16. */
    public static final int GRAND_RANK_STEP = 15;
    /** Black Clover magic types there are (one learned ability each at most); the tooltip says "N of 105 magics". */
    public static final int TOTAL_MAGICS = 105;
    /** Most motes the held weapon shows (one per learned ability, capped). */
    public static final int MAX_MOTES = 8;
    /** Ability slots every Elsdocia has before it learns any: 0 = Legacy Release, 1 = Legacy Gate. */
    public static final int BASE_SLOTS = 2;
    /** Item cooldown after a learned ability, ticks (the ability's own cooldown is per magic). */
    public static final int USE_LOCK_TICKS = 10;
    /** Share of the page's cost (percent of the pools) and of its floor that a learned ability spends: the sword does the shaping. */
    public static final double COST_SHARE = 0.6, FLOOR_SHARE = 0.25;
    /** Share of the page's own cooldown. */
    public static final double COOLDOWN_SHARE = 0.75;

    /** Base damage after {@code bonuses} passes to distinct Grand Magic rank owners: 400, 425, 450 ... */
    public static int damage(int bonuses) { return BASE_DAMAGE + GRAND_STEP_DAMAGE * Math.max(0, bonuses); }

    /** The attack-damage modifier that shows this damage (the player's own fist is 1). */
    public static double attributeAmount(int bonuses) { return damage(bonuses) - 1.0; }

    /** Grand Magic Knight or above. */
    public static boolean isGrand(int rankStep) { return rankStep >= GRAND_RANK_STEP; }
}
