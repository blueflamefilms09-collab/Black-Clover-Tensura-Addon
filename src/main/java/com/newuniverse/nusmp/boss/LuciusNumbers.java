package com.newuniverse.nusmp.boss;

/**
 * Every number of Lucius's configuration in one place (pure; documented in docs/boss_overhaul.md). The Tensura attributes are
 * resolved by registry id at run time and silently skipped when absent; the amounts here are modifiers on top of whatever base the
 * entity has, and Tensura clamps each attribute to its own registered range (max_magicule and max_aura to the server's energy config).
 */
public final class LuciusNumbers {
    private LuciusNumbers() {}

    // ---- attributes (modifier amounts, ADD_VALUE)
    public static final String ATTR_MAX_MAGICULE = "tensura:max_magicule", ATTR_MAX_AURA = "tensura:max_aura",
            ATTR_SPIRITUAL_REGEN = "tensura:spiritual_health_regeneration", ATTR_DODGE_INVULN = "tensura:dodge_invulnerability";
    /** "Maximized": far above any config ceiling; Tensura clamps to its maximum, so the pool simply fills to the cap. */
    public static final double MAGICULE_BONUS = 50_000_000.0, AURA_BONUS = 50_000_000.0;
    /** "Extreme": the base is 2 per tick-step; +400 refills a spiritual health bar in a few seconds, so spirit damage hardly lasts on him. */
    public static final double SPIRITUAL_REGEN_BONUS = 400.0;
    /** The most dodge invulnerability (ticks) he may have in total, and the most the modifier adds: a dodge never grants more than a second. */
    public static final double DODGE_TOTAL_CAP = 20.0, DODGE_MAX_BONUS = 8.0;

    /** The bonus to add so that base + bonus stays at or under {@link #DODGE_TOTAL_CAP}: never negative, never over {@link #DODGE_MAX_BONUS}. */
    public static double dodgeBonus(double base) {
        double room = DODGE_TOTAL_CAP - Math.max(0, base);
        return Math.max(0, Math.min(DODGE_MAX_BONUS, room));
    }

    // ---- the nullifications as damage reductions (fractions of the hit removed), see LuciusMitigation
    public static final double REDUCE_MAGIC = 0.45, REDUCE_PHYSICAL = 0.30, REDUCE_SPATIAL = 0.60;
    /** No hit is ever reduced by more than this, and what is left is never under a quarter of the hit. */
    public static final double REDUCE_CEILING = 0.75;
    /** Spatial blows are his own element: the only class that is cut harder than magic. */

    // ---- passive skills and effects
    public static final String SKILL_ABSOLUTE_SEVERANCE = "tensura:absolute_severance", EFFECT_SEVERANCE_BLADE = "tensura:severance_blade";

    // ---- Dead End Rainbow
    public static final int RAINBOW_HITS = 7, RAINBOW_WINDOW_TICKS = 240, RAINBOW_COOLDOWN_TICKS = 600, RAINBOW_TELL_TICKS = 16, RAINBOW_GAP_TICKS = 10;
    public static final int RAINBOW_TRACKED = 16;

    // ---- Intangibility: the share of the victim's armour a blow ignores (the framework's pvp cap still bounds the hit)
    public static final float INTANGIBLE_BYPASS = 0.6f;

    // ---- Usurper
    public static final int USURPER_LEND_TICKS = 400, USURPER_COOLDOWN_TICKS = 600;
    public static final double USURPER_DRAIN = 0.03;

    // ---- the three cast spells
    public static final String MEGIDDO = "megiddo", TRUE_DARKNESS = "true_darkness", DIMENSION_CUTTER = "dimension_cutter";
    /** Base damage per hit (before the boss's damage law): the same band as his Tower Storm bolts (11) and Chrono blows (16 to 24). */
    public static final float MEGIDDO_BLAST = 9f, DARKNESS_TOUCH = 4f, CUTTER_BLADE = 14f;
    public static final int MEGIDDO_MAX_MARKS = 6, MEGIDDO_MAX_HITS_PER_FOE = 2, DARKNESS_BLIND_TICKS = 60, DARKNESS_SPIRIT = 10;
    public static final double MEGIDDO_RADIUS = 3.2, DARKNESS_RADIUS = 18, CUTTER_LENGTH = 32, CUTTER_HALF_WIDTH = 1.3;
}
