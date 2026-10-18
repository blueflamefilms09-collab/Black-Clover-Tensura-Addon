package com.newuniverse.nusmp.heroweapon.weapon.gauntlet;

import com.newuniverse.nusmp.heroweapon.weapon.kit.Budget;

import java.util.List;

/**
 * Why: every number of the Vassal Gauntlet in one place without game classes, so a test can judge the whole table against the design's budget. The Gauntlet's
 * techniques get STRONGER with the skill points they spend ({@link PointPool}), so each row of the table is a {@link Rank}: the technique's budget row, how many
 * points it may spend and how much damage each point adds. The strongest value a technique can reach (all its points spent, against an undead foe with the
 * Spirit Bane passive) is written into the budget row as {@code rawMax}, and the test proves the row equals base + points * step + bonus, so the table cannot
 * drift from the mechanics.
 */
public final class GauntletNumbers {
    private GauntletNumbers() {}

    /** A technique with its point rule: it spends up to {@code maxSpend} points and each adds {@code perPoint} raw damage. */
    public record Rank(Budget.Spec spec, int maxSpend, double perPoint) {
        /** Raw damage of one application having spent {@code spent} points (clamped to the rule), plus the Spirit Bane bonus against an undead foe. */
        public double raw(int spent, boolean undeadBonus) {
            int s = Math.max(0, Math.min(maxSpend, spent));
            return spec.rawBase() + perPoint * s + (undeadBonus ? SPIRIT_BONUS : 0.0);
        }

        /** The strongest raw damage the rule can reach. */
        public double strongest() { return raw(maxSpend, true); }
    }

    /** Spirit Bane: extra raw damage against undead foes. */
    public static final double SPIRIT_BONUS = 1.0;

    // ---------------------------------------------------------------- the techniques (the budget table)
    public static final Rank RUSH = new Rank(Budget.damage("gauntlet.rush", 70, 0.01, 6.0, 9.4, 1, 3), 3, 0.8);
    public static final Rank MOONLIGHT_KICK = new Rank(Budget.damage("gauntlet.moonlight_kick", 90, 0.015, 7.0, 10.0, 1, 8), 2, 1.0);
    public static final Rank AFTERIMAGE_PALM = new Rank(Budget.damage("gauntlet.afterimage_palm", 120, 0.02, 8.0, 11.4, 1, 1), 3, 0.8);
    public static final Rank QUAKING_FIST = new Rank(Budget.damage("gauntlet.quaking_fist", 130, 0.02, 7.0, 11.5, 1, 8), 5, 0.7);
    public static final Rank TIGER_LEAP = new Rank(Budget.damage("gauntlet.tiger_leap", 180, 0.02, 7.0, 9.5, 1, 8), 3, 0.5);
    public static final Rank TURTLE_CRUSH = new Rank(Budget.heavy("gauntlet.turtle_crush", 200, 0.03, 14.0, 19.0, 1, 8), 4, 1.0);
    /** The ultimate: needs {@link #DRAGON_MIN} points counted (Dragon Heart counts one more), spends them all, hits for 20 raw at that many and 2 more per point beyond (its rank's points are those beyond the minimum); splash 10 raw. */
    public static final Rank DRAGON_SLAYER = new Rank(Budget.ultimate("gauntlet.dragon_slayer", 900, 0.06, 20.0, 29.0, 1, 7), 4, 2.0);
    /** A stance that takes less damage and answers the first blow with a counter of up to {@link #COUNTER_RAW} raw. */
    public static final Budget.Spec IRON_GUARD = Budget.utilityHit("gauntlet.iron_guard", 240, 0.02, 5.0, 1, 1);
    /** A breath that heals the wielder and gives points. */
    public static final Budget.Spec SECOND_WIND = Budget.utility("gauntlet.second_wind", 400, 0.02, 1).withHeal(0.12);

    public static final List<Rank> RANKS = List.of(RUSH, MOONLIGHT_KICK, AFTERIMAGE_PALM, QUAKING_FIST, TIGER_LEAP, TURTLE_CRUSH, DRAGON_SLAYER);

    /** Every technique of the gauntlet, in the order the logic lists them. */
    public static final List<Budget.Spec> ALL = List.of(RUSH.spec(), MOONLIGHT_KICK.spec(), AFTERIMAGE_PALM.spec(), QUAKING_FIST.spec(), TIGER_LEAP.spec(), TURTLE_CRUSH.spec(),
            IRON_GUARD, SECOND_WIND, DRAGON_SLAYER.spec());

    // ---------------------------------------------------------------- points
    /** Points a blow gives, a full swing gives, a technique that hit gives, a kill gives, a blow absorbed in the guard gives (at most {@link #GUARD_POINTS_MAX} per stance). */
    public static final int POINTS_HIT = 1, POINTS_FULL_SWING = 2, POINTS_TECHNIQUE = 1, POINTS_KILL = 2, POINTS_GUARD = 1, GUARD_POINTS_MAX = 3, POINTS_WIND = 3;
    /** The ultimate needs this many points (spent: all it can); Dragon Heart makes every held point worth one more. */
    public static final int DRAGON_MIN = 6;
    public static final int DRAGON_HEART_BONUS = 1;

    // ---------------------------------------------------------------- the techniques' shapes
    public static final double RUSH_BLOCKS = 6.0, RUSH_WIDTH = 1.3;
    public static final int RUSH_DELAY = 4;

    public static final double KICK_RANGE = 3.8, KICK_HALF_ANGLE = 55.0, KICK_LIFT = 0.55, KICK_LIFT_PER_POINT = 0.1;
    /** The kick shows the two edges of its arc and lands this many ticks later (its strongest form is above 8 raw, so it needs a tell). */
    public static final int KICK_DELAY = 4;

    public static final double PALM_RANGE = 9.0, PALM_HALF_ANGLE = 70.0, PALM_BEHIND = 1.4;
    public static final int PALM_DELAY = 3;

    public static final double FIST_LENGTH = 7.0, FIST_WIDTH = 1.4, FIST_PUSH = 1.1;
    public static final int FIST_DELAY = 8, FIST_SLOW_TICKS = 20;

    public static final double LEAP_BLOCKS = 7.0, LEAP_LIFT = 0.55, LEAP_RADIUS = 3.2, LEAP_PUSH = 0.9;
    public static final int LEAP_DELAY = 11;

    public static final double CRUSH_AHEAD = 2.2, CRUSH_RADIUS = 3.4;
    public static final int CRUSH_DELAY = 14, CRUSH_WEAK_TICKS = 60;

    public static final double DRAGON_LENGTH = 7.0, DRAGON_WIDTH = 1.6, DRAGON_SPLASH_RADIUS = 3.5, DRAGON_SPLASH_RAW = 10.0;
    public static final int DRAGON_DELAY = 20;

    public static final int GUARD_TICKS = 60;
    public static final double GUARD_TAKEN = 0.6, COUNTER_RAW = 5.0;

    /** Hardened Fist: damage taken is multiplied by this at all times. */
    public static final double HARDENED_TAKEN = 0.92;
    /** Blossom Step: the cooldown of Afterimage Palm and Tiger Leap is multiplied by this. */
    public static final double BLOSSOM_COOLDOWN = 0.85;

    /** The skill points the ultimate can count: the pool plus the Dragon Heart bonus, never above the pool's cap. */
    public static int dragonPoints(int held, boolean dragonHeart) { return Math.min(PointPool.MAX, Math.max(0, held) + (dragonHeart ? DRAGON_HEART_BONUS : 0)); }

    /** The raw damage of the ultimate's main blow with {@code counted} points (the pool's plus the bonus): 20 at the minimum, 2 more per point to the cap. */
    public static double dragonRaw(int counted, boolean undeadBonus) {
        int beyond = Math.max(0, Math.min(DRAGON_SLAYER.maxSpend(), counted - DRAGON_MIN));
        return DRAGON_SLAYER.spec().rawBase() + DRAGON_SLAYER.perPoint() * beyond + (undeadBonus ? SPIRIT_BONUS : 0.0);
    }
}
