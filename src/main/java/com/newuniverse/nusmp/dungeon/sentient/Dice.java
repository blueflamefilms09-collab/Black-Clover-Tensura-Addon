package com.newuniverse.nusmp.dungeon.sentient;

/**
 * A uniform random source in [0, 1): the tables of the sentient Dungeon take one of these, so they are tested with a seeded generator and
 * played with the game's own ({@code Dice d = level.getRandom()::nextDouble}). Pure.
 */
@FunctionalInterface
public interface Dice {
    double next();

    /** An integer in [0, bound); 0 when the bound is 1 or less. */
    default int nextInt(int bound) { return bound <= 1 ? 0 : Math.min(bound - 1, (int) (next() * bound)); }

    default boolean chance(double probability) { return next() < probability; }
}
