package com.newuniverse.nusmp.avatar.party;

import com.newuniverse.nusmp.avatar.naming.NameRules;

import java.util.Collection;
import java.util.Locale;
import java.util.Random;

/**
 * Why: an Avatar that names a wild monster has to choose a name by itself. The names are built from syllables (so they sound like a language, are short, never
 * collide with a named character of an anime, and always pass {@link NameRules}), and a name is never reused inside one party. Pure and seeded, so a test can
 * generate a thousand and check every one.
 */
public final class NameGen {
    private NameGen() {}

    private static final String[] START = {"Ka", "Ro", "Mi", "Zu", "Ta", "Ne", "Vo", "Sha", "Lu", "Dre", "Bo", "Fi", "Gra", "Hel", "Jo", "Kru", "Mar", "Nym", "Or", "Pyr", "Quo", "Rha", "Sil", "Tor", "Ul", "Vex", "Wyn", "Yor", "Zan"};
    private static final String[] MIDDLE = {"", "", "ra", "ni", "ko", "ve", "lo", "mu", "ti", "sa", "dun", "mor", "gal", "ren"};
    private static final String[] END = {"n", "k", "th", "x", "r", "s", "l", "do", "ru", "ka", "mir", "dus", "ton", "ria", "gar", "lin"};

    /** A fresh name that is valid and not in {@code taken} (case-insensitive); after many tries a numbered one is made so it always ends. */
    public static String next(Random rng, Collection<String> taken) {
        for (int i = 0; i < 40; i++) {
            String n = build(rng);
            if (NameRules.check(n, taken) == NameRules.Verdict.OK) return n;
        }
        String base = build(rng);
        for (int k = 2; k < 1000; k++) {
            String n = base + " " + k;
            if (NameRules.check(n, taken) == NameRules.Verdict.OK) return n;
        }
        return base;
    }

    static String build(Random rng) {
        String s = START[rng.nextInt(START.length)] + MIDDLE[rng.nextInt(MIDDLE.length)] + END[rng.nextInt(END.length)];
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase(Locale.ROOT);
    }
}
