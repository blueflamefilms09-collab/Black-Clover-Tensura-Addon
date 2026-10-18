package com.newuniverse.nusmp.client.skilltree.layout;

import java.util.Locale;

/**
 * Why: the screen shows text that comes from data nobody typed for the screen: a family id such as {@code fairy_tail} for a heading nobody translated, a class id for the "Class:"
 * line, the per-level numbers of a node ({@code 6.0, 7.5, 9.0}) and the seconds left of the class-switch cooldown, which the core hands over as ticks. Turning those into something a
 * player should read is plain string and number work, so it lives in a pure class with its own tests instead of being repeated, slightly differently, in the header, the list and the
 * card. Numbers drop a meaningless ".0" and are cut to two decimals, because a table of damage values that reads {@code 6.0 7.0 8.0} looks like an error and one that reads
 * {@code 0.3333333} overflows the card; the countdown rounds up, so "1s" stays on the button until the time is really over and the button never says "0s" while it is still disabled.
 */
public final class TreeText {
    private TreeText() {}

    /** {@code fire_dragon_slayer} to {@code Fire Dragon Slayer}; a namespace before a colon is dropped; null and blank give the empty string. */
    public static String prettify(String id) {
        if (id == null) return "";
        String s = id.indexOf(':') >= 0 ? id.substring(id.indexOf(':') + 1) : id;
        StringBuilder b = new StringBuilder();
        for (String part : s.split("[_\\s]+")) {
            if (part.isEmpty()) continue;
            if (b.length() > 0) b.append(' ');
            b.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
        }
        return b.toString();
    }

    /** Whole seconds to show for a number of ticks, rounded up; 0 for zero or less. */
    public static int secondsUp(long ticks) { return (int) ((Math.max(0, ticks) + 19) / 20); }

    /** A table value for display: whole numbers without a decimal point, others with at most two decimals and no trailing zeros; NaN and infinity give "?". */
    public static String number(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return "?";
        if (Math.abs(v - Math.rint(v)) < 1e-9) return Long.toString(Math.round(v));
        String s = String.format(Locale.ROOT, "%.2f", v);
        int end = s.length();
        while (end > 0 && s.charAt(end - 1) == '0') end--;
        if (end > 0 && s.charAt(end - 1) == '.') end--;
        return s.substring(0, end);
    }
}
