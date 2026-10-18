package com.newuniverse.nusmp.heroweapon.weapon.glassworld;

import java.util.Locale;

/**
 * Why: a technique's description tells the player its numbers (damage, blocks, seconds), and a description that says "5 damage" while the code does 6 is a bug
 * nobody finds without reading both. The descriptions are therefore built from the same constants the skills use, and this is the one way they print a number:
 * a whole number without a decimal point, anything else with the fewest decimals that keep it exact to two places, always with a dot (never the machine's comma),
 * and ticks as seconds. No game imports.
 */
public final class Fmt {
    private Fmt() {}

    /** 5.0 prints as {@code 5}, 5.5 as {@code 5.5}, 0.25 as {@code 0.25}, 1.0E-4 as {@code 0}. */
    public static String n(double v) {
        double r = Math.round(v * 100.0) / 100.0;
        if (r == Math.rint(r)) return Long.toString((long) r);
        String s = String.format(Locale.ROOT, "%.2f", r);
        if (s.endsWith("0")) s = s.substring(0, s.length() - 1);
        return s;
    }

    /** Ticks as seconds: 80 prints as {@code 4}, 90 as {@code 4.5}. */
    public static String sec(int ticks) { return n(ticks / 20.0); }

    /** A share as a whole percent: 0.25 prints as {@code 25}. */
    public static String pct(double share) { return n(share * 100.0); }
}
