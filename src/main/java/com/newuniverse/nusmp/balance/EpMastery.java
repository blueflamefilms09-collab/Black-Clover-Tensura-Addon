package com.newuniverse.nusmp.balance;

/**
 * The EP part of a skill's damage grows in with the skill's mastery (owner request): x1 at no mastery, the full EP scale once the skill is mastered, a straight line between. Pure; the SERVER switch
 * {@code epNeedsMastery} (off = the old damage, the full EP scale at any mastery) is read by {@link BalanceLaw}. Cost: one multiply per hit.
 */
public final class EpMastery {
    private EpMastery() {}

    /** {@code scale} (the EP scale, 1 or more) taken in by {@code mastery} (0..1); with {@code on} false the scale is returned as it is. */
    public static double ramp(double scale, double mastery, boolean on) {
        if (!on || !(scale > 1.0)) return scale;
        double m = Double.isNaN(mastery) ? 0 : Math.max(0, Math.min(1, mastery));
        return 1.0 + (scale - 1.0) * m;
    }
}
