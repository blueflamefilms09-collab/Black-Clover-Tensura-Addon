package com.newuniverse.nusmp.butoh;

/**
 * Why: the dance makes the dancer a smaller target by adding a transient modifier to the scale attribute (the hitbox shrinks with it), and a
 * modifier left behind would shrink the player for good. This pure gate decides, from "is the dance on" and "which move", whether the modifier
 * must be set to a value or cleared, and remembers what it last asked for, so every exit path (end, death, logout) ends with the modifier
 * gone. The game side only does what the gate says. No game classes, so the add/remove symmetry is unit tested.
 */
public final class DanceScale {
    public static final double MIN_CHANGE = -0.25, MAX_CHANGE = -0.15;

    public enum Kind { NONE, SET, CLEAR }

    /** What the game side must do: SET the modifier to {@code amount}, CLEAR it, or leave it. */
    public record Op(Kind kind, double amount) {
        static final Op NONE = new Op(Kind.NONE, 0);
    }

    private boolean applied;
    private double amount;

    /** The amount a move asks for, clamped to the allowed band. */
    public static double amountFor(DanceMove move) { return Math.max(MIN_CHANGE, Math.min(MAX_CHANGE, move.scaleChange)); }

    public boolean applied() { return applied; }

    public double amount() { return amount; }

    /** One step: the dance is on with this move (or null when it is off). */
    public Op update(boolean dancing, DanceMove move) {
        if (!dancing || move == null) {
            if (!applied) return Op.NONE;
            applied = false;
            amount = 0;
            return new Op(Kind.CLEAR, 0);
        }
        double want = amountFor(move);
        if (applied && Math.abs(want - amount) < 1e-9) return Op.NONE;
        applied = true;
        amount = want;
        return new Op(Kind.SET, want);
    }

}
