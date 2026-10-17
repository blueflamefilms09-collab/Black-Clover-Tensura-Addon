package com.newuniverse.nusmp.arc;

/**
 * Why: the single list of characters of the arcs, in story order. Every character is a file of its own (a boss class with a {@code CHARACTER} constant, or an
 * NPC definition) and this class is the one place that names them all, so the roster, the registry and the saved ids agree on one order. The lead adds one line
 * per character here when a builder's work is merged; a character that is not listed here does not exist in the game.
 */
public final class ArcCharacters {
    private ArcCharacters() {}

    private static boolean done;

    /** Registers every character and loads every boss class (so its entity type is registered before the registry events). Safe to call twice. */
    public static synchronized void bootstrap() {
        if (done) return;
        done = true;
        // Saga 1 ... (lines are appended here as the builders are merged, e.g. ArcRoster.register(HeathGriceBoss.CHARACTER);)
        com.newuniverse.nusmp.arc.knights.KnightsCast.registerAll();   // 1.61.0: sixteen friendly Magic Knights, captains and court members of the Hub (NPC-B)
    }
}
