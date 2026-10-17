package com.newuniverse.nusmp.labyrinth.mega.enc;

import java.util.Random;

/**
 * Why: encounter tables are written for people, and "2-4 goblins" is how a game master writes a group, so a mob's count is a small range
 * written as {@code "2-4"} (or {@code "3"} for exactly three). It is parsed once when the table loads, rolled when the encounter is built, and
 * scaled by the party factor so a table written for one player still fits four (the budget then trims whatever the factor made too big).
 * Pure.
 */
public record CountRange(int min, int max) {
    public static final CountRange ONE = new CountRange(1, 1);
    /** No table needs more than this many of one kind; it also bounds a hand-edited file that asks for a thousand. */
    public static final int LIMIT = 32;

    public CountRange {
        if (min < 0) min = 0;
        if (max < min) max = min;
        if (max > LIMIT) max = LIMIT;
        if (min > max) min = max;
    }

    /** Parses {@code "3"} or {@code "2-4"}; null for anything else (a negative number, a third number, words). */
    public static CountRange parse(String text) {
        if (text == null) return null;
        String t = text.trim();
        if (t.isEmpty()) return null;
        try {
            int dash = t.indexOf('-');
            if (dash < 0) {
                int n = Integer.parseInt(t);
                return n < 0 || n > LIMIT ? null : new CountRange(n, n);
            }
            if (dash == 0 || t.indexOf('-', dash + 1) >= 0) return null;
            int a = Integer.parseInt(t.substring(0, dash).trim()), b = Integer.parseInt(t.substring(dash + 1).trim());
            if (a < 0 || b < a || b > LIMIT) return null;
            return new CountRange(a, b);
        } catch (NumberFormatException notANumber) {
            return null;
        }
    }

    /** A number in [min, max], each equally likely. */
    public int roll(Random r) { return max <= min ? min : min + r.nextInt(max - min + 1); }

    /**
     * Both ends multiplied by {@code factor} and rounded. A range that asked for at least one still asks for at least one (a pack for a party of
     * four is bigger, never empty); a range that asked for none stays at none.
     */
    public CountRange scaled(double factor) {
        if (!(factor > 0.0) || factor == 1.0) return this;
        int lo = (int) Math.round(min * factor), hi = (int) Math.round(max * factor);
        if (min >= 1) lo = Math.max(1, lo);
        return new CountRange(lo, Math.max(lo, hi));
    }

    public String text() { return min == max ? Integer.toString(min) : min + "-" + max; }
}
