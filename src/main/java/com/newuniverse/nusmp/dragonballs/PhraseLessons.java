package com.newuniverse.nusmp.dragonballs;

import java.util.ArrayList;
import java.util.List;

/**
 * Why: the owner wants a player to be taught the words that call the dragon one piece at a time, as they find each ball, and to be handed the words ready to paste into chat. This is the pure part: how a
 * phrase is cut into pieces, one piece per ball, and what a player knows after N balls (docs/dragon_ball_words.md). No Minecraft type, so it is unit-tested.
 * <p>
 * The first N-1 balls teach the phrase (a short phrase gives one word per ball and the later balls give none; a long phrase is cut into N-1 even groups); the last ball always completes it, so a
 * player who holds every ball knows every word. The order is the order in which the player finds the balls, not the stars.
 */
final class PhraseLessons {
    private PhraseLessons() {}

    /** The words of a phrase (whitespace separated, empty parts dropped). */
    static List<String> words(String phrase) {
        List<String> out = new ArrayList<>();
        if (phrase == null) return out;
        for (String w : phrase.trim().split("\\s+")) if (!w.isEmpty()) out.add(w);
        return out;
    }

    /** The words ball number {@code ordinal} (1-based: the ordinal-th ball the player has found) adds; empty when it adds none (and always for the last ball). */
    static List<String> fresh(List<String> words, int balls, int ordinal) {
        int groups = balls - 1;
        if (groups < 1 || ordinal < 1 || ordinal > groups || words.isEmpty()) return List.of();
        int n = words.size();
        if (n <= groups) return ordinal <= n ? List.of(words.get(ordinal - 1)) : List.of();
        return words.subList((ordinal - 1) * n / groups, ordinal * n / groups);
    }

    /** Everything the player knows after finding {@code learned} balls of a set of {@code balls}: the words so far, in order (the whole phrase once all balls are found). */
    static List<String> known(List<String> words, int balls, int learned) {
        if (learned >= balls) return words;
        List<String> out = new ArrayList<>();
        for (int i = 1; i <= learned; i++) out.addAll(fresh(words, balls, i));
        return out;
    }

    /** Words joined into one chat line, first letter capital ("Takkaradoppo pakkoradoppo yaikanaan"). Case is ignored by the summoning check, so this is safe to paste. */
    static String line(List<String> words) {
        if (words.isEmpty()) return "";
        String s = String.join(" ", words);
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** How many balls the player has learned, from a bit mask with one bit per star (stars 1..8). */
    static int count(int mask) { return Integer.bitCount(mask & 0xFF); }

    /** The mask with the star added (stars outside 1..8 change nothing). */
    static int with(int mask, int star) { return star < 1 || star > 8 ? mask : mask | (1 << (star - 1)); }

    static boolean has(int mask, int star) { return star >= 1 && star <= 8 && (mask & (1 << (star - 1))) != 0; }
}
