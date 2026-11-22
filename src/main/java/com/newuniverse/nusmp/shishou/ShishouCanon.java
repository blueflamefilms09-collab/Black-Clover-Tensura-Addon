package com.newuniverse.nusmp.shishou;

/**
 * Why: the numbers of the 2.x "anime accurate" rework of Shishou in one pure place (no game classes), so the tests pin them and docs/items/shishou_rework.md reads them from here. Canon abilities he did not have
 * (Self-Repair, High-Speed Self-Repair, Self-Evolution, Telekinetic Grasp) and his own original abilities (Akashic Index, Blades of the Record, Akashic Sanctuary). Everything is capped; the worst cases are
 * in the docs. Server config {@code canon.skills} (default true) switches every effect below off again (the skills then do nothing and the numbers are the 3.x ones).
 */
public final class ShishouCanon {
    private ShishouCanon() {}

    // ---------------------------------------------------------------- Self-Repair, High-Speed Self-Repair, Self-Evolution (passives)
    /** Mana regeneration factor: Self-Repair +50%, High-Speed Self-Repair +100% in all (the sword mends itself with mana; he is unbreakable, so the repair is his pool). */
    public static final double REPAIR_BONUS = 0.5, HIGH_SPEED_BONUS = 1.0;

    public static double regenFactor(boolean repair, boolean highSpeed) { return highSpeed ? 1.0 + HIGH_SPEED_BONUS : repair ? 1.0 + REPAIR_BONUS : 1.0; }

    /** Self-Evolution: +10% experience from everything, and one free mana step every {@link #EVOLVE_KILLS} kills (the steps are capped by the rules, so this is bounded too). */
    public static final double EVOLUTION_EXP_BONUS = 0.10;
    public static final int EVOLVE_KILLS = 25;

    public static boolean evolveDue(long kills) { return kills > 0 && kills % EVOLVE_KILLS == 0; }

    public static long evolvedExp(long exp, boolean evolving) { return evolving ? Math.round(exp * (1.0 + EVOLUTION_EXP_BONUS)) : exp; }

    // ---------------------------------------------------------------- Telekinetic Grasp (Nennou): he lifts up to four creatures
    public static final double GRASP_RANGE = 22.0, GRASP_SPLASH = 3.5;
    /** 4.104.0: the hurl after the lift: speed, share of the strike the hurled creature takes, the splash where it lands (a share of the hit, a radius). */
    public static final double GRASP_HURL_SPEED = 1.7, GRASP_HURL_HIT = 2.0, GRASP_IMPACT_RADIUS = 2.5, GRASP_IMPACT_SHARE = 0.5;
    public static final int GRASP_HURL_FLIGHT = 8;
    public static final int GRASP_TARGETS = 4, GRASP_LIFT_TICKS = 30, GRASP_LIFT_AMP = 3, GRASP_SLOW_TICKS = 60;

    // ---------------------------------------------------------------- Akashic Index: the record names its victims
    public static final double INDEX_RADIUS = 24.0, INDEX_BONUS = 0.15;
    public static final int INDEX_TARGETS = 6, INDEX_TICKS = 200, INDEX_GLOW_TICKS = 100, INDEX_TABLE_CAP = 16;

    /** The damage of a sword blow on a creature the Index names: +15%. */
    public static double indexed(double dmg, boolean named) { return named ? dmg * (1.0 + INDEX_BONUS) : dmg; }

    // ---------------------------------------------------------------- Blades of the Record: five waves of spectral blades
    public static final int BLADE_WAVES = 5, BLADE_GAP = 4, BLADE_TARGETS = 6;
    public static final double BLADE_RADIUS = 6.0, BLADE_RANGE = 24.0, BLADE_SHARE = 0.30;

    /** One wave of the blades: 30% of a sword strike (five waves are 1.5 strikes in all), capped. */
    public static double bladeWave(double strike, double cap) { return Math.min(cap, strike * BLADE_SHARE); }

    // ---------------------------------------------------------------- Akashic Sanctuary: a field of the record around the wielder
    public static final double SANCTUARY_RADIUS = 5.0;
    public static final int SANCTUARY_TICKS = 100, SANCTUARY_PULSE = 10, SANCTUARY_TARGETS = 12;

    public static int sanctuaryPulses() { return SANCTUARY_TICKS / SANCTUARY_PULSE; }

    // ---------------------------------------------------------------- bounds for the docs and the tests
    /** Most damage calls one cast of Blades of the Record can make. */
    public static int maxBladeHits() { return BLADE_WAVES * BLADE_TARGETS; }

    /** Most creatures one cast of Akashic Sanctuary touches over its whole life. */
    public static int maxSanctuaryTouches() { return sanctuaryPulses() * SANCTUARY_TARGETS; }
}
