package com.newuniverse.nusmp.relics.wave;

/**
 * Wave 4: the rules of the Beast Masters Amulet. The card sizes what fits by the creature's challenge rating (ten of the weakest, five,
 * two, one); here the same ladder is read off the creature's maximum health, and the amulet holds ten points. A willing creature (one
 * the wearer owns) goes in freely; an unwilling one must first be hurt to half its health, which stands in for the card's Wisdom
 * save. Pure Java.
 */
final class Wave4Beasts {
    private Wave4Beasts() {}

    static final int CAPACITY = 10;
    static final int MAX_STORED = 10;
    /** The most text one creature's saved data may take (so the amulet's data stays small to sync). */
    static final int MAX_DATA_CHARS = 6000;
    static final double RANGE = 6.0;

    /** Points a creature takes: 1 (CR 0 to 1/8), 2 (1/4 to 1/2), 5 (1 to 2), 10 (3 to 4); 0 means too mighty to store. */
    static int weight(float maxHealth) {
        if (maxHealth <= 0f || Float.isNaN(maxHealth)) return 0;
        if (maxHealth <= 8f) return 1;
        if (maxHealth <= 16f) return 2;
        if (maxHealth <= 40f) return 5;
        if (maxHealth <= 80f) return 10;
        return 0;
    }

    static boolean fits(int used, int weight) { return weight > 0 && used + weight <= CAPACITY; }

    /** Unwilling creatures must be at half health or less; a creature the wearer owns is always willing. */
    static boolean willing(boolean ownedByWearer, float health, float maxHealth) { return ownedByWearer || health <= maxHealth * 0.5f; }
}
