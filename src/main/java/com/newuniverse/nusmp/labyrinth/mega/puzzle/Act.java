package com.newuniverse.nusmp.labyrinth.mega.puzzle;

/**
 * Why: a puzzle rule must be testable without a game, so everything a delver can do to a puzzle is reduced to ONE small value: click a cell (a right click on a pedestal, a tile or a chest,
 * optionally with a token item in hand or while sneaking), step onto or off a floor tile, or pick one answer of a question. The world layer ({@link PuzzleInstance}) turns the real events
 * into these values and the JUnit tests and the in-game self-tests turn the rule's own {@link PuzzleRule#suggest suggestion} into them, so the rule that is proven solvable is the rule
 * that is played. {@code actor} is a small number the instance gives each player of the party (0 for the first), {@code tick} is the game tick the action happens at, and {@code hold}
 * is only used by a suggestion ("step on it and stay this many ticks"). Pure (no Minecraft types).
 */
public record Act(Kind kind, int actor, int cell, int arg, String item, long tick, int hold) {
    public enum Kind { CLICK, STEP, CHOOSE }

    /** A plain right click on a cell. */
    public static Act click(int actor, int cell, long tick) { return new Act(Kind.CLICK, actor, cell, 0, null, tick, 0); }

    /** A right click with a token item in hand (an offering, a key, a shard). */
    public static Act clickWith(int actor, int cell, String item, long tick) { return new Act(Kind.CLICK, actor, cell, 0, item, tick, 0); }

    /** A right click while sneaking (the "commit" gesture of a puzzle that also has a "look" gesture; also the "turn the other way" gesture of a dial). */
    public static Act sneakClick(int actor, int cell, long tick) { return new Act(Kind.CLICK, actor, cell, 1, null, tick, 0); }

    /** The actor steps onto a floor tile (suggestion: stay {@code hold} ticks). */
    public static Act stepOn(int actor, int cell, long tick, int hold) { return new Act(Kind.STEP, actor, cell, 1, null, tick, hold); }

    /** The actor leaves a floor tile. */
    public static Act stepOff(int actor, int cell, long tick) { return new Act(Kind.STEP, actor, cell, 0, null, tick, 0); }

    /** The actor picks answer number {@code answer} of the open question. */
    public static Act choose(int actor, int answer, long tick) { return new Act(Kind.CHOOSE, actor, -1, answer, null, tick, 0); }

    public boolean sneaking() { return kind == Kind.CLICK && arg == 1; }

    public boolean stepsOn() { return kind == Kind.STEP && arg == 1; }

    /** The same action at another tick (the harness moves a suggestion to the moment it is valid). */
    public Act at(long t) { return new Act(kind, actor, cell, arg, item, t, hold); }
}
