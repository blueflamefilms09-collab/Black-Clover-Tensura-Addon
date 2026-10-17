package com.newuniverse.nusmp.arc.arena;

import java.util.List;
import java.util.Map;

/**
 * Why: every character of the arcs fights in an arena of its own, and the six looks of those arenas (the ruins of the Midnight Sun, the Spade throne room, the
 * court of the wizard king, the sunlit plain, the pit of the dungeon, the plain ring of the rest) must all answer the same questions for the builder, the
 * director and the tests: what block stands at this offset from the centre, where does the boss stand, where do the travellers appear, where are the ways home.
 * An arena is a FUNCTION from a column (dx, dz) to its cells (the same formula-not-a-file idea as the Final Hour's courtyard): nothing to ship, nothing to
 * keep in step with a template, and a pure object that unit tests can walk without a game. The vertical frame is the walking surface: {@code dy} 0 is the
 * topmost floor block of a column, so a player stands at dy 1 and the foundation lies at -1 and -2; the arena's centre is at world (cx, {@link #FLOOR_Y}, cz).
 */
public interface ArenaShape {
    /** World y of the floor blocks (dy 0). */
    int FLOOR_Y = 64;

    /** The most travellers an arrival ring or row can place. */
    int MAX_ARRIVALS = 16;

    /** The id a character names in {@code ArcCharacter.arena()} ({@code midnight_sun_ruins}). */
    String id();

    /** The name players read ("The Ruins of the Midnight Sun"). */
    String title();

    /** Columns with |dx| or |dz| above this are empty (the half extent of the build, walls included). */
    int radius();

    /** The cells of one column, bottom to top (strictly rising {@code dy}); empty outside the arena. */
    List<ArenaCell> column(int dx, int dz);

    /** Which block text plays each role in this arena (see {@link BlockSpec}); every part a column uses has an entry. */
    Map<ArenaPart, String> palette();

    /** The column the boss stands on, {dx, dz}: the boss's feet are on the surface of that column. */
    int[] bossSpot();

    /** The columns the first {@code n} travellers appear on (at most {@link #MAX_ARRIVALS}), {dx, dz} each, spread so no two share one. */
    int[][] arrivalSpots(int n);

    /** The columns of the ways home (the centre of each pad, where the answering block stands), {dx, dz}. */
    int[][] sigils();

    // ---------------------------------------------------------------- what every shape shares
    /** The {@code dy} of the highest solid cell of the column, or {@link Integer#MIN_VALUE} when it has none. */
    default int surface(int dx, int dz) {
        int best = Integer.MIN_VALUE;
        for (ArenaCell c : column(dx, dz)) if (c.part().solid() && c.dy() > best) best = c.dy();
        return best;
    }

    /** Can a player stand on this column: solid ground with no cell in the three rows above it? */
    default boolean standable(int dx, int dz) {
        List<ArenaCell> cells = column(dx, dz);
        int top = Integer.MIN_VALUE;
        for (ArenaCell c : cells) if (c.part().solid() && c.dy() > top) top = c.dy();
        if (top == Integer.MIN_VALUE) return false;
        for (ArenaCell c : cells) if (c.dy() > top && c.dy() <= top + 3 && c.part().solid()) return false;
        for (ArenaCell c : cells) if (c.dy() > top && c.dy() <= top + 3 && c.part() != ArenaPart.CARPET && c.part() != ArenaPart.FLOWER_A
                && c.part() != ArenaPart.FLOWER_B && c.part() != ArenaPart.TALL_GRASS) return false;
        return true;
    }
}
