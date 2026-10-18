package com.newuniverse.nusmp.heroweapon.weapon.kit;

import java.util.Locale;

/**
 * Why: the texts of the techniques (the Weapon Book and the tooltip) quote their own numbers, and a text that says "7 damage" while the table says 8 is a bug
 * nobody notices until a player does. The texts are therefore BUILT from the {@code Numbers} constants with these three formatters, so there is one copy of
 * every number; the formatters drop a useless ".0" so "7.0" reads "7" and "3.2" stays "3.2". Always ASCII, always the same in every locale.
 */
public final class Fmt {
    private Fmt() {}

    /** 7.0 to "7", 3.2 to "3.2", 0.15 to "0.15" (at most two decimals, no trailing zeros). */
    public static String num(double v) {
        String s = String.format(Locale.ROOT, "%.2f", v);
        if (s.indexOf('.') >= 0) {
            int end = s.length();
            while (end > 0 && s.charAt(end - 1) == '0') end--;
            if (end > 0 && s.charAt(end - 1) == '.') end--;
            s = s.substring(0, end);
        }
        return s;
    }

    /** A share as a percentage without the percent sign: 0.15 to "15", 0.145 to "14.5". */
    public static String pct(double share) { return num(share * 100.0); }

    /** Ticks as seconds: 160 to "8", 90 to "4.5". */
    public static String secs(int ticks) { return num(ticks / 20.0); }
}
