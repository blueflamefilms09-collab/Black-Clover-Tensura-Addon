package com.newuniverse.nusmp.avatar.naming;

/**
 * Why: a formal name gives the Avatar a surge of EP (+50% to +200% by default) that the owner wants scaled by the creator's luck and the Avatar's own
 * intelligence, and a permanent tax on the creator. The formulas are pure arithmetic so they are tested, and the config only supplies the bounds.
 */
public final class NamingMath {
    private NamingMath() {}

    /**
     * The EP surge as a fraction (0.5 = +50%). A random part, the luck and the INT score each pull the result up inside [min, max]:
     * 50% dice, 30% luck (20 luck is full), 20% INT (a score of 18 is full).
     */
    public static double epBoost(double luck, int intScore, double rng01, double min, double max) {
        double lo = Math.min(min, max), hi = Math.max(min, max);
        double l = clamp((luck) / 20.0), i = clamp((intScore - 3) / 15.0), r = clamp(rng01);
        double t = 0.5 * r + 0.3 * l + 0.2 * i;
        return lo + (hi - lo) * clamp(t);
    }

    /** The multiplier on the Avatar's EP: 1 + boost. */
    public static double epMultiplier(double boost) { return 1.0 + Math.max(0.0, boost); }

    /**
     * The native-naming floor. Tensura's own naming has already raised the Avatar's EP from {@code epBefore} to {@code epNow}; the owner's rule is that a named Avatar
     * also gets the surge ({@code multiplier} on the EP it had before). The Avatar ends with the LARGER of the two, never the sum: the factor to scale the EP by now,
     * 1.0 when Tensura's gain already beats the surge or when a number is unknown (not above zero).
     */
    public static double nativeTopUp(double epBefore, double epNow, double multiplier) {
        if (!(epBefore > 0.0) || !(epNow > 0.0) || !(multiplier > 1.0)) return 1.0;
        double wanted = epBefore * multiplier;
        return wanted > epNow ? wanted / epNow : 1.0;
    }

    /** The creator's permanent magicule tax as a fraction of their maximum: the config percent, never the whole pool. */
    public static double taxFraction(double percent) { return clamp(percent / 100.0, 0.0, 0.9); }

    /** Seconds of Naming Sleep the creator suffers: the config value, doubled for an evolution (a bigger rebirth costs more). */
    public static int sleepTicks(int configSeconds, boolean evolves) { return Math.max(0, configSeconds) * 20 * (evolves ? 2 : 1); }

    private static double clamp(double v) { return clamp(v, 0.0, 1.0); }

    private static double clamp(double v, double lo, double hi) { return v < lo ? lo : Math.min(v, hi); }
}
