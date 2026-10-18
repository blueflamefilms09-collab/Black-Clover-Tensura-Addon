package com.newuniverse.nusmp.labyrinth.mega.mechanics;

import java.util.Locale;

/**
 * Why: the owner asked for about a hundred mechanics in each of eight families, and the config switches, the admin command, the room-kind biases of the Director and the
 * data files all key on the same eight words, so the list lives in one pure enum (no Minecraft import: JUnit and the Python generator both repeat these ids). Entries are
 * only ever appended; the id (never the ordinal) is what data and saves use.
 */
public enum MechCategory {
    /** Gravity shifts, silence, mana tides, rukh flow, time dilation, shifting walls, darkness, weather, burning floors. */
    FLOOR("floor"),
    /** Sequence plates, light and mirror routing, weights, levers, notes, colours, memory, carrying. */
    PUZZLE("puzzle"),
    /** Timed, proximity and line-of-sight traps, chains, falling ceilings, flooding, spores, collapsing floors, glyphs. */
    HAZARD("hazard"),
    /** Arena rules: kill order, shielded foes, reflective phases, waves, cursed weapons, ambushes. */
    COMBAT("combat"),
    /** Cursed chests, bargains, gambling altars, sacrifice shrines, mimics, vault timers. */
    RISK("risk"),
    /** Phase rules, lair actions, arena transformations, minion tethers, anti-cheese rules. */
    BOSS("boss"),
    /** Moving platforms, wind lifts, one-way doors, teleport networks, vertical shafts. */
    TRAVEL("travel"),
    /** Shared burdens, split-party rooms, revive rules, cooperative locks. */
    PARTY("party");

    private final String id;

    MechCategory(String id) { this.id = id; }

    public String id() { return id; }

    /** The category with this id (case-insensitive), or null. */
    public static MechCategory fromId(String id) {
        if (id == null) return null;
        String s = id.toLowerCase(Locale.ROOT);
        for (MechCategory c : values()) if (c.id.equals(s)) return c;
        return null;
    }
}
