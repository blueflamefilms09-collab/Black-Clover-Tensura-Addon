package com.newuniverse.nusmp.elite.skills.evolution;

import java.util.UUID;

/**
 * Why: a requirement trigger must be a small pure class, but what it asks is spread over the whole game: the skills a player owns, their EP, their kills, their inventory and, through
 * {@code EliteHooks}, the forge, the records and titles and the reset count that other workers own. This interface is the one window the triggers look through. In game it is
 * implemented by {@code PlayerEvolutionContext} (which reads the player and the late-bound hooks); in tests by a fake with plain fields. Every answer is a safe default when the thing asked
 * about does not exist.
 */
public interface EvolutionContext {
    UUID id();

    boolean owns(String skillId);

    boolean ownsMastered(String skillId);

    /** The player's EP (existence points). */
    double ep();

    /** Kills made while owning {@code skillId}, of the filter ("any", "boss", "monster", "undead"). */
    int killsWith(String skillId, String filter);

    /** All kills of the filter. */
    int killsTotal(String filter);

    int itemCount(String itemId);

    /** Removes the items; false (and nothing removed) if they are not all there. */
    boolean consumeItems(String itemId, int count);

    /** Has the player awakened of this kind ("demon_lord", "hero", or "any")? */
    boolean awakened(String kind);

    // ---- late-bound, owned by other subsystems (EliteHooks)
    int forgeCrafted();

    /** 0 none, 1 common ... 6 mythical. */
    int forgeQuality();

    boolean record(String record);

    boolean hasTitle(String title);

    String activeTitle();

    int heldTitles();

    /** Owned by the skills subsystem itself: skill and class resets. */
    int resets();
}
