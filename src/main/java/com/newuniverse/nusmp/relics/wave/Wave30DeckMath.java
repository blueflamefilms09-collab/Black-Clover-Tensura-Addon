package com.newuniverse.nusmp.relics.wave;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.SplittableRandom;

/**
 * Why: the deck's fairness rules (the twenty-two without replacement, at most three a day, the chain of the Fool and the Jester that cannot run away,
 * the crumbling at the end) are a contract with the player, so they are checked by tests rather than by playing.
 * <p>
 * Wave 30: the Deck of Many Things as pure maths. A deck is a seed plus a count of cards drawn: the seed fixes one shuffle of the twenty-two cards
 * (a weighted shuffle, so the great and the terrible cards tend to sit deeper in the deck, but every card is somewhere), the count says how far the
 * drawing has come. Nothing here knows Minecraft; the effects of the cards are in {@link Wave30Deck}.
 */
final class Wave30DeckMath {
    private Wave30DeckMath() {}

    /** The cards of the 22-card deck of the rules, in the order of the table. {@code weight} shapes the shuffle (low: deeper in the deck). */
    enum Card {
        BALANCE("Balance", 1.0, Mood.NEUTRAL), COMET("Comet", 0.9, Mood.GOOD), DONJON("The Donjon", 0.6, Mood.BAD), EURYALE("Euryale", 0.8, Mood.BAD),
        FATES("The Fates", 0.9, Mood.GOOD), FLAMES("Flames", 0.7, Mood.BAD), FOOL("The Fool", 1.2, Mood.BAD), GEM("Gem", 1.1, Mood.GOOD),
        IDIOT("Idiot", 1.1, Mood.BAD), JESTER("Jester", 1.2, Mood.GOOD), KEY("Key", 0.8, Mood.GOOD), KNIGHT("Knight", 0.9, Mood.GOOD),
        MOON("Moon", 0.5, Mood.GOOD), ROGUE("Rogue", 0.9, Mood.BAD), RUIN("Ruin", 0.7, Mood.BAD), SKULL("Skull", 0.6, Mood.BAD),
        STAR("Star", 0.5, Mood.GOOD), SUN("Sun", 0.4, Mood.GOOD), TALONS("Talons", 0.7, Mood.BAD), THRONE("Throne", 0.8, Mood.GOOD),
        VIZIER("Vizier", 1.0, Mood.GOOD), VOID("The Void", 0.5, Mood.BAD);

        final String title;
        final double weight;
        final Mood mood;

        Card(String title, double weight, Mood mood) {
            this.title = title;
            this.weight = weight;
            this.mood = mood;
        }
    }

    enum Mood { GOOD, NEUTRAL, BAD }

    static final int SIZE = Card.values().length;
    /** Cards a player may draw from one deck in a game day, and the most extra draws the Fool and the Jester can add in one go. */
    static final int MAX_PER_DAY = 3, MAX_EXTRA = 3;

    /** The shuffle of this seed: a weighted random permutation of all twenty-two cards (each card once). Deterministic. */
    static Card[] order(long seed) {
        SplittableRandom rng = new SplittableRandom(seed);
        Card[] cards = Card.values();
        double[] key = new double[cards.length];
        Integer[] idx = new Integer[cards.length];
        for (int i = 0; i < cards.length; i++) {
            double u = rng.nextDouble();
            if (u <= 1.0e-12) u = 1.0e-12;
            key[i] = -Math.log(u) / cards[i].weight;          // exponential clocks: the smaller the key, the earlier the card; weight is the rate
            idx[i] = i;
        }
        Arrays.sort(idx, Comparator.comparingDouble((Integer i) -> key[i]));
        Card[] out = new Card[cards.length];
        for (int i = 0; i < cards.length; i++) out[i] = cards[idx[i]];
        return out;
    }

    /** Cards left in a deck of which {@code drawn} have been drawn. */
    static int remaining(int drawn) { return Math.max(0, SIZE - Math.max(0, drawn)); }

    /** Has the deck run out? A deck that has given its twenty-two crumbles to dust. */
    static boolean crumbles(int drawn) { return drawn >= SIZE; }

    /**
     * How many cards a request may draw: what was asked (1 at least), no more than are left, and no more than the day's allowance still holds
     * ({@code usedToday} already drawn today). Zero when nothing may be drawn.
     */
    static int allowed(int requested, int drawn, int usedToday) {
        int want = Math.max(1, requested);
        return Math.max(0, Math.min(want, Math.min(remaining(drawn), MAX_PER_DAY - Math.max(0, usedToday))));
    }

    /** The result of a drawing: the cards in the order they came up, how many the deck has given in all now, and how many of them were extra draws. */
    record Draw(List<Card> cards, int drawnAfter, int extra) {}

    /**
     * Draws {@code count} cards from the position {@code drawn} of this seed's shuffle. A Fool says "draw again" (one extra card) and a Jester, on the
     * side of the seeded coin that favours drawing, "two more": the extras are never more than {@link #MAX_EXTRA} in one drawing and never more than
     * the deck holds.
     */
    static Draw draw(long seed, int drawn, int count) {
        Card[] shuffle = order(seed);
        List<Card> got = new ArrayList<>();
        int pos = Math.max(0, drawn), want = Math.max(0, count), extra = 0;
        while (want > 0 && pos < SIZE) {
            Card c = shuffle[pos];
            got.add(c);
            pos++;
            want--;
            int add = 0;
            if (c == Card.FOOL) add = 1;
            else if (c == Card.JESTER && jesterDraws(seed, pos - 1)) add = 2;
            add = Math.min(add, MAX_EXTRA - extra);
            add = Math.min(add, SIZE - pos - want);
            if (add > 0) {
                want += add;
                extra += add;
            }
        }
        return new Draw(List.copyOf(got), pos, extra);
    }

    /** The Jester's coin, fixed by the seed and the card's place: true means "two more draws", false "gain experience". */
    static boolean jesterDraws(long seed, int index) { return (mix(seed, index, 0x4A455354L) & 1L) == 0L; }

    /** The Moon grants one to three boons, fixed by the seed and the card's place. */
    static int moonBoons(long seed, int index) { return 1 + (int) Math.floorMod(mix(seed, index, 0x4D4F4F4EL), 3L); }

    /** A seed from two longs (the player's nanotime and the world time, say): well mixed, never zero. */
    static long freshSeed(long a, long b) {
        long s = mix(a, 0x9E3779B97F4A7C15L, b);
        return s == 0L ? 0x5DEECE66DL : s;
    }

    private static long mix(long a, long b, long c) {
        long z = a * 0x9E3779B97F4A7C15L + b * 0xBF58476D1CE4E5B9L + c * 0x94D049BB133111EBL;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** Names of the cards drawn so far, in order (for the deck's tooltip and the "look at the deck" act). */
    static List<String> names(long seed, int drawn) {
        Card[] shuffle = order(seed);
        List<String> out = new ArrayList<>();
        for (int i = 0; i < Math.min(drawn, SIZE); i++) out.add(shuffle[i].title);
        return out;
    }
}
