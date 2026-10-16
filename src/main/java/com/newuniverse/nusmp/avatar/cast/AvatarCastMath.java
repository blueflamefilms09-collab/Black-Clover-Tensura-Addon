package com.newuniverse.nusmp.avatar.cast;

/**
 * Why: how long an Avatar waits between casts depends on five things (the page's own cooldown, its personality, INT and WIS, the entity-level
 * multiplier that a Yandere frenzy sets to 0.1, and a minimum gap). One pure function keeps that arithmetic testable.
 */
public final class AvatarCastMath {
    private AvatarCastMath() {}

    /** A frenzy never goes below this multiplier, and a normal Avatar never below the traits' own. */
    public static final double FRENZY_MULT = 0.1;

    /**
     * Ticks until the same page may be cast again.
     * @param pageTicks   the page's own cooldown in ticks
     * @param mental      {@code AvatarPersonalityTraits.mentalCooldownMult(INT, WIS)}
     * @param personality the personality's cooldown multiplier (LAZY 0.7)
     * @param entityMult  {@code AvatarEntity.cooldownMultiplier()}
     * @param frenzied    whether the Avatar is in a Yandere frenzy (applied once, even if entityMult already carries it)
     */
    public static int pageCooldownTicks(int pageTicks, double mental, double personality, double entityMult, boolean frenzied) {
        double m = mental * personality * (frenzied ? Math.min(entityMult, FRENZY_MULT) : entityMult);
        return (int) Math.max(0, Math.round(pageTicks * m));
    }

    /** Ticks until the Avatar may cast ANY page again: the configured gap scaled the same way, but never below 8 ticks. */
    public static int gapTicks(int baseGap, double mental, double personality, double entityMult, boolean frenzied) {
        double m = mental * personality * (frenzied ? Math.min(entityMult, FRENZY_MULT) : entityMult);
        return (int) Math.max(8, Math.round(baseGap * m));
    }
}
