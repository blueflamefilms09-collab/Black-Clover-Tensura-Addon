package com.newuniverse.nusmp.spiritcontrol;

/**
 * Why: in combat mode every cast of a spirit costs BOTH pools: the spirit's own mana and a share of the user's magicules. This is the split, as pure arithmetic. A spell's price is in mana units; the user's share
 * {@code share} (config {@code combat.userManaShare}, 0.5 = half and half) of those units is paid by the user, the rest by the spirit. The user's units become magicules at {@code unitPercent} percent of
 * their maximum per unit (never below a floor, and times the mana law's cost multiplier), so a strong mage pays more magicules for the same spell and a weak one less in absolute terms, like a page.
 */
public final class ManaSplit {
    private ManaSplit() {}

    /** The share held to 0..1 (a missing or bad number reads as one half). */
    public static double clampShare(double share) {
        if (Double.isNaN(share)) return 0.5;
        return Math.max(0.0, Math.min(1.0, share));
    }

    /** Units the spirit's own pool pays. */
    public static double spiritUnits(double units, double share) { return Math.max(0.0, units) * (1.0 - clampShare(share)); }

    /** Units the user's pool pays. */
    public static double userUnits(double units, double share) { return Math.max(0.0, units) * clampShare(share); }

    /**
     * The magicules the user pays: {@code userUnits} times {@code unitPercent} percent of {@code maxMagicule}, at least {@code floor} (but zero when the user pays no units at all), times {@code costMult}.
     */
    public static double userMagicules(double units, double share, double maxMagicule, double unitPercent, double floor, double costMult) {
        double u = userUnits(units, share);
        if (u <= 0.0) return 0.0;
        double raw = Math.max(0.0, maxMagicule) * Math.max(0.0, unitPercent) / 100.0 * u;
        return Math.max(Math.max(0.0, floor), raw) * Math.max(0.0, costMult);
    }
}
