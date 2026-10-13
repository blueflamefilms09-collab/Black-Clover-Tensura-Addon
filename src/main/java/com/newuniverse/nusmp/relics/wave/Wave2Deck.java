package com.newuniverse.nusmp.relics.wave;

import java.util.function.IntUnaryOperator;

/**
 * Pact of the Deck: the 54-card pact deck (52 playing cards and 2 jokers) as the 54 low bits of one long, 1 = still in the deck. Pure,
 * no Minecraft imports. Card 0 to 51 is suit * 13 + (rank - 1) with the suits in the order spades, hearts, clubs, diamonds; 52 and 53
 * are the jokers. Jack, queen and king are 11, 12 and 13; the ace is 1.
 */
public final class Wave2Deck {
    private Wave2Deck() {}

    public static final int SIZE = 54;
    public static final long FULL = (1L << SIZE) - 1L;
    public static final int MAX_QUEEN_CHAIN = 2;

    public enum Suit {
        SPADES("minecraft:thrown", "Spades"), HEARTS("minecraft:on_fire", "Hearts"), CLUBS("minecraft:magic", "Clubs"),
        DIAMONDS("minecraft:freeze", "Diamonds"), JOKER("minecraft:magic", "Joker");

        private final String damageType, label;

        Suit(String damageType, String label) {
            this.damageType = damageType;
            this.label = label;
        }

        /** The damage type id of the suit: spades slash, hearts burn, clubs are force, diamonds are cold. */
        public String damageType() { return damageType; }

        public String label() { return label; }
    }

    public enum Kind { NUMBER, ACE, JACK, QUEEN, KING, JOKER }

    public static Suit suit(int card) { return card < 0 || card >= 52 ? Suit.JOKER : Suit.values()[card / 13]; }

    /** 1 to 13, or 0 for a joker. */
    public static int rank(int card) { return card < 0 || card >= 52 ? 0 : card % 13 + 1; }

    public static Kind kind(int card) {
        int r = rank(card);
        return switch (r) {
            case 0 -> Kind.JOKER;
            case 1 -> Kind.ACE;
            case 11 -> Kind.JACK;
            case 12 -> Kind.QUEEN;
            case 13 -> Kind.KING;
            default -> Kind.NUMBER;
        };
    }

    public static String name(int card) {
        Kind k = kind(card);
        return switch (k) {
            case JOKER -> "Joker";
            case ACE -> "Ace of " + suit(card).label();
            case JACK -> "Jack of " + suit(card).label();
            case QUEEN -> "Queen of " + suit(card).label();
            case KING -> "King of " + suit(card).label();
            default -> rank(card) + " of " + suit(card).label();
        };
    }

    public static int remaining(long mask) { return Long.bitCount(mask & FULL); }

    /** Draws a uniformly random card still in the deck; -1 if there is none. {@code rng.applyAsInt(n)} is a number in [0, n). */
    public static int draw(long mask, IntUnaryOperator rng) {
        long m = mask & FULL;
        int n = Long.bitCount(m);
        if (n == 0) return -1;
        int k = Math.floorMod(rng.applyAsInt(n), n);
        for (int card = 0; card < SIZE; card++) {
            if ((m & (1L << card)) == 0) continue;
            if (k-- == 0) return card;
        }
        return -1;
    }

    public static long take(long mask, int card) { return card < 0 || card >= SIZE ? mask : mask & ~(1L << card); }

    /**
     * The deck of a given day: a deck that was never drawn from, or one last used on an earlier day, comes back whole (the card's "shuffle
     * the discard pile back in after each long rest").
     */
    public static long forDay(boolean hasMask, long mask, long storedDay, long today) {
        if (!hasMask || today > storedDay) return FULL;
        return mask & FULL;
    }

    /** A number card is worth half its value in health (a 10 hits for 5, a quarter of a player's 20 HP). */
    public static double damage(int rank) { return rank >= 2 && rank <= 10 ? rank * 0.5 : 0.0; }

    /**
     * Drawing the pact (the deck as a spellcasting focus): three cards are drawn and discarded like a third-level spell, and the pact pays the
     * caster back in magicules, 2% and 6% for each ace, doubled when two or more aces come up (the card's "cast without expending a slot").
     */
    public static double channelFraction(int aces) {
        double f = 0.02 + 0.06 * Math.max(0, aces);
        return aces >= 2 ? f * 2.0 : f;
    }

    public static final int PACT_DRAW = 3;

    // ---------------------------------------------------------------- the wild magic surge
    public enum Surge {
        CONFETTI("A storm of cards bursts around the target."),
        SWAP("The pact trades your place with the target's."),
        MEND("The cards fold into a warm light."),
        SWIFT("The deck hums and you are quick on your feet."),
        LIFT("The target floats up like a dropped card."),
        SPOTLIGHT("Every card turns to face you both: glowing."),
        DEALT_AGAIN("The deck shuffles itself whole."),
        HICCUP("The surge backfires: you are dizzy.");

        private final String line;

        Surge(String line) { this.line = line; }

        public String line() { return line; }
    }

    /** The eight-entry table; any number maps onto it. */
    public static Surge surge(int roll) { return Surge.values()[Math.floorMod(roll, Surge.values().length)]; }
}
