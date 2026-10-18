package com.newuniverse.nusmp.heroweapon.weapon.bow;

/**
 * Why: "the draw time sets the power" is the owner's rule for the Legendary Bow, and the whole feel of the weapon is in a few formulas: how much of a full
 * draw a number of ticks is (the curve of the vanilla bow, so it feels like a bow and not like a button), how far the arrow of light flies, how hard it
 * hits and what it costs. They are pure so the edges are tested (a tap that must not shoot, a draw past full that must not exceed it, the budget ceiling
 * that no bonus may break).
 */
public final class DrawMath {
    private DrawMath() {}

    /** Ticks of a full draw (the vanilla bow's). */
    public static final int FULL_DRAW = 20;
    /** The smallest share of a full draw that looses an arrow (below it the string slips, as with any bow). */
    public static final double MIN_FRACTION = 0.1;
    /** How much faster the Quickdraw passive draws. */
    public static final double QUICKDRAW = 1.25;

    /** The share of a full draw (0 to 1) a hold of {@code chargeTicks} ticks gives, by the vanilla curve (c squared plus two c, over three). */
    public static double fraction(int chargeTicks, boolean quick) {
        double c = Math.max(0, chargeTicks) / (double) FULL_DRAW * (quick ? QUICKDRAW : 1.0);
        return Math.min(1.0, (c * c + c * 2.0) / 3.0);
    }

    /** True when the draw is strong enough to loose an arrow. */
    public static boolean enough(double fraction) { return fraction >= MIN_FRACTION - 1e-9; }

    /** How far a plain arrow of light flies: from {@link BowNumbers#RANGE_MIN} to {@link BowNumbers#RANGE_MAX} blocks, farther with Farsight, never past the sniper range. */
    public static double range(double fraction, boolean far) {
        double f = Math.max(0.0, Math.min(1.0, fraction));
        double r = BowNumbers.RANGE_MIN + (BowNumbers.RANGE_MAX - BowNumbers.RANGE_MIN) * f;
        return Math.min(BowNumbers.SNIPER_RANGE, far ? r * BowNumbers.FARSIGHT : r);
    }

    /** The magicules a plain arrow costs, as a share of the maximum: proportional to the draw. */
    public static double cost(double fraction) { return BowNumbers.BASIC_COST * Math.max(0.0, Math.min(1.0, fraction)); }

    /**
     * The raw damage of a plain arrow: the base times the share of the draw, times the form's strength (its attack damage stat, which for a bow means draw
     * strength), times the Law Fanatic and Pride bonuses when they are on, and never above the ordinary budget.
     */
    public static double raw(double fraction, double strength, double lawBonus, double prideBonus) {
        double f = Math.max(0.0, Math.min(1.0, fraction));
        double r = BowNumbers.BASIC_RAW * f * (1.0 + BowNumbers.STRENGTH_SCALE * Math.max(0.0, strength)) * Math.max(1.0, lawBonus) * Math.max(1.0, prideBonus);
        return Math.min(BowNumbers.BASIC_MAX_RAW, r);
    }
}
