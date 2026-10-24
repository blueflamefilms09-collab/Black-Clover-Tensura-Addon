package com.newuniverse.nusmp.mana.method;

import com.newuniverse.nusmp.mana.method.RuneCharge.Charge;

/**
 * What a Mana Method page costs for the charge it was cast with (pure, tested, no game classes). In the anime a small rune is a small effort and a great array is a great one; the owner
 * asked that the price follows the hold: a tap costs little, a full hold costs the most. The price is the page's normal price (the grimoire's {@code getMagiculeCost}, which already
 * carries the percent of the pool, the floor, the Heart kingdom perk and the other multipliers) times a FRACTION of this class:
 * <ul>
 *   <li>the fraction follows the POWER of the charge ({@link RuneCharge}: x0.5 for a tap, x0.75 rising to x1.6 for a charged hold, x1.6 for a full one), so a spell is paid in proportion to
 *       the strength it gets: {@code fraction = min + (1 - min) * (power - 0.5) / (1.6 - 0.5)}, with {@code min} the setting {@code minCostFraction} (default {@value #DEFAULT_MIN});</li>
 *   <li>at the default that is <b>BASIC 40 %</b>, <b>CHARGED 53.6 % rising to 100 %</b> (77 % half way), <b>FULL 100 %</b>; the fraction never passes 1, so a cast never costs more than the
 *       page's normal price, and the spell without a measured hold (the 3.4.0 cast, or hold-to-charge off) costs the normal price;</li>
 *   <li>the cooldown is NOT scaled: a tap is cheap but not spammable (the grimoire sets the cooldown after the cast, as before).</li>
 * </ul>
 */
public final class RuneCost {
    private RuneCost() {}

    public static final double DEFAULT_MIN = 0.40, MIN_LOW = 0.10, MIN_HIGH = 1.0;

    /** The minimum fraction from the config, kept in range (a NaN gives the default). */
    public static double minFraction(double configured) {
        if (Double.isNaN(configured)) return DEFAULT_MIN;
        return Math.max(MIN_LOW, Math.min(MIN_HIGH, configured));
    }

    /** How strong a charge is, 0 (a tap) to 1 (a full hold), by its power; the charge without a hold (the 3.4.0 spell, power x1) is in the middle. Used for the cost and by the body and brush runes. */
    public static double level(Charge c) {
        if (c == null) return 0.0;
        double t = (c.power() - RuneCharge.BASIC_POWER) / (RuneCharge.FULL_POWER - RuneCharge.BASIC_POWER);
        return Double.isNaN(t) ? 0.0 : Math.max(0.0, Math.min(1.0, t));
    }

    /** The share of the page's normal price this charge pays, in [{@code min}, 1]. 1 for the cast without a measured hold. */
    public static double fraction(Charge c, double configuredMin) {
        if (c == null || c.legacy()) return 1.0;
        double min = minFraction(configuredMin);
        return Math.max(min, Math.min(1.0, min + (1.0 - min) * level(c)));
    }

    /** The price after the charge: {@code base} unchanged when the setting is off, never negative, never above {@code base}. */
    public static double scaled(double base, Charge c, double configuredMin, boolean enabled) {
        if (!(base > 0.0)) return 0.0;
        return enabled ? base * fraction(c, configuredMin) : base;
    }
}
