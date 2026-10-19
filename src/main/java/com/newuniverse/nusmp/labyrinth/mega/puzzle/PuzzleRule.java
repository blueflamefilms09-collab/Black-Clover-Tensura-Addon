package com.newuniverse.nusmp.labyrinth.mega.puzzle;

import java.util.List;

/**
 * Why: the pure half of every puzzle. A rule is a deterministic state machine: built from {@link Params} (seed, party, tier) it fixes its board, then it answers actions ({@link #act}) and
 * the passing of time ({@link #tick}) with {@link Fx} lists, shows its board as numbers ({@link #face}) the world turns into blocks through {@link Palettes}, and can always say what the next
 * CORRECT action is ({@link #suggest}). That last method is the heart of the safety story: the tests drive every rule with its own suggestions (from any state, after any mix of wrong
 * actions) to prove "solvable for every seed, resets safe, no soft lock", and the world uses it for the anti-frustration hint (after a few mistakes the next correct cell flashes). A rule
 * never talks to Minecraft, never reads the clock (the tick is passed in) and never uses an unseeded random.
 */
public interface PuzzleRule {
    String id();

    /** What the world must build. Fixed for the life of the rule. */
    Layout layout();

    /** The party size and tier the board was built for. */
    Params params();

    /** The puzzle is armed at {@code tick}: the first effects (a pattern is shown, a melody plays). */
    List<Fx> begin(long tick);

    /** A delver did something. */
    List<Fx> act(Act a);

    /** Time passed (the world calls this every tick the instance is armed; tests call it with any increasing tick). */
    List<Fx> tick(long tick);

    /** What cell {@code cell} shows at layer {@code layer} (0 is the bottom block of a column), as a number of its group's palette. */
    int face(int cell, int layer);

    /** The height of a cell's column in blocks (1 for most). */
    int height(int cell);

    boolean solved();

    /** How many mistakes were made so far (the reward is better with none). */
    int fails();

    /**
     * The next correct action from the CURRENT state (never null while the puzzle is unsolved, except while the rule is busy showing something: then the action's tick says when). Its
     * {@code tick} is the earliest tick at which it is valid, never earlier than the last tick the rule saw.
     */
    Act suggest(long tick);

    /** Back to the starting board (same seed, same solution). Never leaves the puzzle unsolvable. */
    void reset(long tick);

    /** The pairs of cells joined by a drawn thread right now (constellations); empty for most puzzles. */
    default List<int[]> links() { return List.of(); }

    /** The open question again (a question puzzle re-asks on request); empty for most puzzles. */
    default List<Fx> question() { return List.of(); }

    /** Ticks without any action after which the world gives the party a nudge (the hint line). */
    default int nudgeTicks() { return 600; }
}
