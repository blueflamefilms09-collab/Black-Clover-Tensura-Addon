package com.newuniverse.nusmp.charset;

/**
 * Why: an acquisition rule ({@link SpellRule}) must be decidable without the game, so tests can prove "Spirit of Zephyr only after Spirit Dive". This is everything a rule may ask about
 * the player and the grimoire; the game side ({@code CharacterSets}) answers from the grimoire instance and the player, the tests answer from a map.
 */
public interface RuleFacts {
    /** The grimoire's mastery bar as 0..1. */
    double mastery();

    /** How many times this page has been cast successfully (only pages that some rule names are counted). */
    int uses(String pageId);

    /** Is the page of this id open in the grimoire right now? */
    boolean pageOpen(String pageId);

    /** The bonded spirit's trust, 0..100 (0 without a bond). */
    int trust();

    /** A named stage flag, see {@link SpellRule#FLAGS}. */
    boolean flag(String name);
}
