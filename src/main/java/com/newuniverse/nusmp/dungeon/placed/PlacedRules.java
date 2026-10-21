package com.newuniverse.nusmp.dungeon.placed;

/**
 * Why: the decisions of the player-placed exemption as plain booleans, so the truth tables are unit tested and the game glue ({@link PlayerPlaced}) only
 * gathers the facts. The rule in one sentence: the Dungeon's own blocks stay unbreakable by any means, and a block a PLAYER placed in the Labyrinth
 * can be taken back, by anything that removes it, until the dungeon's own builders write over it or its mark is pruned. Nothing here changes what the
 * Dungeon Guard holds for the arenas of the Final Hour and of the arcs (their furniture is never player work) or for the Sanctuary (its own rules).
 */
public final class PlacedRules {
    private PlacedRules() {}

    /** Is the exemption in force in this world at all: the switch is on and the level is a Labyrinth dimension (the Mega-Dungeon, or the old Spire). */
    public static boolean active(boolean switchOn, boolean labyrinthLevel) { return switchOn && labyrinthLevel; }

    /** Is a placement remembered: the exemption is active and the one who placed it is a player (a mob, a dispenser or a spell is not). */
    public static boolean remembers(boolean active, boolean byPlayer) { return active && byPlayer; }

    /** May a block change that takes a protected block away go through: only when the position is a marked one. */
    public static boolean letsRemoval(boolean active, boolean marked) { return active && marked; }

    /** Must a mark be dropped when the dungeon's own builder writes here (inside an internal span): the stone is the dungeon's again. */
    public static boolean forgetOnBuild(boolean active, boolean marked, int internalDepth) { return active && marked && internalDepth > 0; }

    /** May the client start the crack animation for this block: it knows the position is a mark (the synced mirror says so). */
    public static boolean clientMayCrack(boolean active, boolean mirrored) { return active && mirrored; }

    /** May the player be told again that the dungeon reclaimed their oldest block: at most once every {@code interval} ticks. */
    public static boolean pruneNoticeDue(long now, long last, long interval) { return last == 0L || now < last || now - last >= interval; }
}
