package com.newuniverse.nusmp.arc.early;

import java.util.List;

/**
 * Why: Revchi Salik's move table and numbers in one pure place (read by the boss class, checked by {@code EarlyTablesTest}). The shape of the fight, after
 * the anime (Magic Knights Entrance Arc): a former Magic Knight turned thief who fights from a few blocks away with Chain Magic. His chains whip and hook
 * (Dance of the Pitless Viper, Scorpio Chain, Heavy Chain), and his signature, Magic Binding, holds a foe and seals their magic. The close moves keep a
 * party that rushes him honest, the mid moves are lines and circles that are locked where they were drawn, and the late moves (40% and up) cage and lock
 * down the whole party. Only the Magic Binding family needs a counter to be learnt: leave the line, strike him to tear the chain, cut it with anti-magic,
 * or break the line of sight.
 */
public final class ChainTable {
    private ChainTable() {}

    public static final String VIPER = "viper_dance", SCORPIO = "scorpio_chain", HEAVY = "heavy_chain", BINDING = "magic_binding", WHIRL = "chain_whirl",
            RUSH = "thief_rush", PILLARS = "creation_chains", FORMATION = "iron_chain_formation", LOCKDOWN = "total_lockdown";

    /** Base damage of the moves (before the framework's damage law: power tuning, victim EP, the balance law's caps). */
    public static final float LASH = 4.5f, SCORPIO_HIT = 8f, HEAVY_HIT = 11f, BINDING_HIT = 6f, WHIRL_HIT = 6.5f, RUSH_HIT = 10f, PILLAR_HIT = 7f,
            CAGE_HIT = 9f, LOCKDOWN_HIT = 12f;
    /** A bound foe takes this much more from him: he feeds on prey that cannot move. */
    public static final float PREY = 1.3f;

    /** A bind: ticks (before the balance law shortens it), links, and the share of his maximum health one link costs (at least {@link #LINK_FLOOR} points). */
    public static final int BIND_TICKS = 70, LINKS = 2;
    public static final double LINK_SHARE = 0.02, LINK_FLOOR = 10;
    /** The share of his maximum health that breaks the Total Lockdown while it is wound up. */
    public static final double LOCKDOWN_BREAK = 0.12;

    /** Reach of the lashes and of the whirl, the cage's radius, the lines' half widths, and the farthest a chain flies. */
    public static final double LASH_REACH = 7.5, LASH_HALF_ANGLE = 0.96, WHIRL_RADIUS = 5.6, CAGE_RADIUS = 6.2, HEAVY_RADIUS = 3.2, PILLAR_RADIUS = 2.1,
            LINE_HALF = 1.0, RUSH_HALF = 1.4, RUSH_LENGTH = 16, CHAIN_RANGE = 22;
    /** The Gates make the cage and the lockdown this much bigger. */
    public static final double GATES_REACH = 1.25;

    private static final List<MoveRow> ROWS = List.of(
            MoveRow.of(VIPER, "Dance of the Pitless Viper", 20, 18, 80, 0, 9, 6),
            MoveRow.of(SCORPIO, "Scorpio Chain", 20, 20, 150, 5, 18, 5),
            MoveRow.of(HEAVY, "Heavy Chain", 20, 24, 130, 3, 20, 4),
            MoveRow.of(BINDING, "Magic Binding", 20, 22, 270, 4, 20, 4),
            MoveRow.crowd(WHIRL, "Chain Whirl", 20, 20, 170, 0, 6, 4),
            MoveRow.of(RUSH, "Thief's Rush", 20, 20, 150, 6, 22, 3),
            MoveRow.crowd(PILLARS, "Creation Magic: Chain Pillars", 40, 32, 260, 0, 24, 3),
            MoveRow.crowd(FORMATION, "Iron Chain Formation", 40, 44, 360, 5, 24, 3),
            MoveRow.ultimate(LOCKDOWN, "Total Lockdown", 60, 56, 1500, 0, 24, 5));

    /** The table, in the order of the boss's move list. */
    public static List<MoveRow> rows() { return ROWS; }

    public static MoveRow byId(String id) { return MoveRow.find(ROWS, id); }

    /** Lashes of the Viper at a power: three, four at the Gates. */
    public static int lashes(boolean gates) { return gates ? 4 : 3; }

    /** Foes the Pillars mark at once: one more than the power's cast slots, at most six. */
    public static int pillarMarks(int castSlots) { return Math.max(2, Math.min(6, castSlots + 1)); }

    /** Foes the Total Lockdown chains: five, eight at the Gates. */
    public static int lockdownTargets(boolean gates) { return gates ? 8 : 5; }
}
