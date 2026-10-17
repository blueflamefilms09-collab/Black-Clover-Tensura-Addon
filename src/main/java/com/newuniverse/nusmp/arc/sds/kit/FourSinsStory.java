package com.newuniverse.nusmp.arc.sds.kit;

import java.util.List;

/**
 * Why: the owner's story order puts the Seven Deadly Sins after Saga 1 ("the Eye of the Midnight Sun bosses"), and Merlin, Gowther, King and Diane all
 * unlock together, in any order, once those have fallen. The ids of those bosses belong to another builder's files, so the list is written ONCE here (not
 * four times in four records) and a single edit follows if an id is renamed when the characters are merged. An id that is not in the roster is skipped by
 * the story code (a chain with one unmerged link can still be opened), so a stale entry can never lock the Sins out.
 */
public final class FourSinsStory {
    private FourSinsStory() {}

    /** The characters whose defeat opens the four Sins of this builder: the bosses of the Eye of the Midnight Sun (Saga 1, part two). */
    public static final List<String> PREREQUISITES = List.of("fana", "patolli", "rades_spirito", "third_eye");

    /** The array form the record builder takes. */
    public static String[] prerequisites() { return PREREQUISITES.toArray(new String[0]); }
}
