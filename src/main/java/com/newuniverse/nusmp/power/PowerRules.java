package com.newuniverse.nusmp.power;

/**
 * Why: the owner's 3.74.0 power raise ("Djinns, Beast Wills and the Vassal Weapons 25 times stronger") as pure arithmetic, so the numbers can be tested without a server. A wielder who
 * has several of the three out at once gets the LARGEST factor, never their product (25 x 25 x 25 would be 15 625). Against a player the raised blow is held at a share of the victim's
 * maximum health (the mod's own PvP hit cap), so the raise makes no one-shots; mobs and bosses take it whole (a boss's own damage cap, where it has one, still runs after this).
 */
public final class PowerRules {
    private PowerRules() {}

    /** The factor a blow is multiplied by: the largest of the active systems' factors, never under 1. A system that is not active passes 1. */
    public static double factor(double djinn, double beast, double vassal) {
        return Math.max(1.0, Math.max(clean(djinn), Math.max(clean(beast), clean(vassal))));
    }

    /** The smallest lead the four Legendary Weapons keep over the Seven Star and Vassal Weapons, whatever the settings say. */
    public static final double MIN_LEGENDARY_MARGIN = 1.1;

    /**
     * 3.78.1, the owner's rule "the four Legendary Weapons always stay on top": a Legendary Weapon's factor is the Vassal factor times the margin, and the margin is never under
     * {@link #MIN_LEGENDARY_MARGIN}. With the Vassal raise switched off (1) the Legendary Weapons get no raise either: the hero weapon buff's own Holy factor already keeps them ahead.
     */
    public static double legendary(double vassal, double margin) {
        double v = clean(vassal);
        if (v <= 1.0) return 1.0;
        return v * Math.max(MIN_LEGENDARY_MARGIN, Double.isNaN(margin) ? MIN_LEGENDARY_MARGIN : Math.min(margin, 100.0));      // no upper clamp at the Vassal cap: even at vassalFactor 1000 the lead holds
    }

    private static double clean(double f) { return Double.isNaN(f) || f < 1.0 ? 1.0 : Math.min(f, 1000.0); }

    /** The blow after the raise. {@code pvpShare}: the most of a player's maximum health one raised blow may take (0.4 = 40 %); a blow that was already larger is left as it was. */
    public static float dealt(float amount, double factor, boolean targetIsPlayer, float targetMaxHealth, double pvpShare) {
        if (!(amount > 0f) || factor <= 1.0) return amount;
        double raised = amount * factor;
        if (targetIsPlayer) raised = Math.max(amount, Math.min(raised, Math.max(0.0, targetMaxHealth) * Math.max(0.0, Math.min(1.0, pvpShare))));
        return (float) Math.min(raised, Float.MAX_VALUE / 4);
    }

    /** The blow a boosted wielder takes: times {@code takenFactor} (0.5 = half), never negative, never more than it was. */
    public static float taken(float amount, double takenFactor) {
        if (!(amount > 0f)) return amount;
        return (float) (amount * Math.max(0.0, Math.min(1.0, takenFactor)));
    }
}
