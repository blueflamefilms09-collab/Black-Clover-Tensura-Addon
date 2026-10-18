package com.newuniverse.nusmp.skilltree.shop;

/**
 * Why: prices here run from a few thousand to millions of gold, and a number such as 2347000 is unreadable at a glance, so every place that prints gold (the shop screen, the
 * command feedback, the log) goes through one formatter. It is written by hand instead of with {@code String.format} or {@code NumberFormat} because those depend on the
 * machine's locale (a German server would print dots, a French one narrow spaces) while a shop screen, a chat message and a screenshot in a bug report must all read the same.
 * Pure, so the client screen and the server commands share it and a test pins the exact strings.
 */
public final class GoldFormat {
    private GoldFormat() {}

    /** The whole number with a comma between each three digits: {@code 2,347,000}; a negative number keeps its sign. */
    public static String full(long value) {
        String digits = Long.toString(value);
        boolean negative = digits.startsWith("-");
        if (negative) digits = digits.substring(1);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            if (i > 0 && (digits.length() - i) % 3 == 0) out.append(',');
            out.append(digits.charAt(i));
        }
        return negative ? "-" + out : out.toString();
    }

    /**
     * A short form for narrow places: {@code 950}, {@code 12.3K}, {@code 152K}, {@code 2.35M}, {@code 1.5B}, {@code 4T}. At most three significant digits, truncated and not
     * rounded (a price must never look cheaper than it is by a rounding up of what the buyer has, nor dearer by a rounding up of the cost), no trailing zeros.
     */
    public static String compact(long value) {
        if (value < 0) return "-" + compact(value == Long.MIN_VALUE ? Long.MAX_VALUE : -value);
        if (value < 1_000) return Long.toString(value);
        long unit;
        char suffix;
        if (value < 1_000_000L) { unit = 1_000L; suffix = 'K'; }
        else if (value < 1_000_000_000L) { unit = 1_000_000L; suffix = 'M'; }
        else if (value < 1_000_000_000_000L) { unit = 1_000_000_000L; suffix = 'B'; }
        else { unit = 1_000_000_000_000L; suffix = 'T'; }
        long whole = value / unit;
        long rest = value % unit;
        int decimals = whole >= 100 ? 0 : whole >= 10 ? 1 : 2;
        StringBuilder out = new StringBuilder().append(whole);
        if (decimals > 0) {
            long scale = decimals == 1 ? 10 : 100;
            long frac = rest * scale / unit;
            if (frac > 0) {
                String f = Long.toString(frac);
                if (decimals == 2 && f.length() == 1) f = "0" + f;
                while (f.endsWith("0")) f = f.substring(0, f.length() - 1);
                out.append('.').append(f);
            }
        }
        return out.append(suffix).toString();
    }

    /**
     * A span of game ticks as the shop shows its restock clock, in IN-GAME time because that is what the restock is counted in (a day is 24000 ticks, an hour 1000):
     * {@code 2d 4h}, {@code 5h}, or {@code <1h}; never negative.
     */
    public static String clock(long ticks) {
        long t = Math.max(0, ticks);
        long days = t / 24_000L;
        long hours = t % 24_000L / 1_000L;
        if (days > 0) return days + "d " + hours + "h";
        return hours > 0 ? hours + "h" : "<1h";
    }
}
