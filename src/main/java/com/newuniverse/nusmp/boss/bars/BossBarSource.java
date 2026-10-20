package com.newuniverse.nusmp.boss.bars;

import java.util.List;

/**
 * Why: ONE interface through which every boss of the mod tells the custom boss bar what it has to show, so the bar never needs to know a boss class. Every method has a default that means
 * "nothing special": a boss implements only what it has (a phase machine, a lair clock, a wind-up). The interface is pure data (no Minecraft type), so the numbers it answers are unit
 * tested. {@code BossBarSources.of(entity)} gives the source of any living entity: the entity itself if it implements this interface, else the adapter of its family (Zogratis bosses and the
 * arcs, the Labyrinth Guardian and the Decennial layer), else a default one that only reads the entity (style from its type, shield from its absorption, fighters from the players near it).
 * The server samples a source at most five times a second per bar and sends the state only when it changed ({@link BossBarState#differs}).
 */
public interface BossBarSource {
    /** The style id ({@link BarStyleIds}) or null to let the adapter choose from the entity type. */
    default String styleId() { return null; }

    /** The boss's own name for the plate ("" = the name of the vanilla boss event; a text starting with '@' is a lang key). */
    default String name() { return ""; }

    /** The line under the boss's name ("Lich of the Weeping Graves"). */
    default String title() { return ""; }

    /** The current phase, 1-based, and how many phases the boss has (1 = the bar shows no phase). */
    default int phase() { return 1; }

    default int phaseCount() { return 1; }

    /** The percent of health at which phase 2, 3, ... begin, descending ({@code [66, 33]} for a three-phase boss). */
    default List<Integer> phaseThresholds() { return List.of(); }

    default String phaseName() { return ""; }

    /** Absorption as a share of the maximum health, 0..1. */
    default float shield() { return 0f; }

    /** The boss cannot be hurt right now (a phase shift, a refill). */
    default boolean invulnerable() { return false; }

    default boolean enraged() { return false; }

    /** The lair action about to fire ("" = none) and the ticks left; the bar warns during the last {@link BossBarState#LAIR_WARNING_TICKS}. */
    default String lairId() { return ""; }

    default int lairTicks() { return -1; }

    /** Adds alive (-1 = this boss has no add counter). */
    default int adds() { return -1; }

    /** The move being wound up ("" = none) and how far it has got, 0..1. */
    default String castName() { return ""; }

    default float castProgress() { return -1f; }

    /** Players in the fight (-1 = let the hub count the players near the boss). */
    default int fighters() { return -1; }

    /** An empty source: every default. */
    BossBarSource NONE = new BossBarSource() {};
}
