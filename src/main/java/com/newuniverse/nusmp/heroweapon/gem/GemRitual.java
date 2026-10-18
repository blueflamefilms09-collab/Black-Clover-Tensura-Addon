package com.newuniverse.nusmp.heroweapon.gem;

/**
 * Why: the Choosing is a channel the player holds for a few seconds, and every way it can end (it completes, a blow lands, the player walks off, the sky
 * above is covered, the player lets go) must be decided the same way on every run and be provable without a game. This is that state machine and nothing
 * else: no player, no world, no item, only counters. The glue ({@code GemChoosing}) reads the facts of each server tick (how far the player stands from
 * where the circle was drawn, whether anything hurt them, whether the space above is still open) and asks this class what happens; the class answers
 * {@link Step#CONTINUE}, {@link Step#COMPLETE} or {@link Step#INTERRUPT}, and once it has answered either of the last two it never changes its mind, so a
 * late event (a hit in the very tick the gem answers) cannot undo or double a result. The order of the checks is a rule: a blow ends the circle before a
 * step away does, a step away before a covered sky, and none of them can end a ritual that has already completed.
 */
public final class GemRitual {
    /** Where the ritual stands. */
    public enum State { CHANNELING, COMPLETED, INTERRUPTED }

    /** Why a ritual ended early; {@link #key} is the suffix of the message that tells the player. */
    public enum Interrupt {
        /** The player stood further than the tolerance from the centre of the circle. */
        MOVED("moved"),
        /** Something hurt the player. */
        HURT("hurt"),
        /** The space above the player was covered. */
        CLOSED("closed"),
        /** The player let go of the use key. */
        RELEASED("released"),
        /** The gem left the player's hand (another slot, a drop, a death) without the use key being released. */
        LOST("lost");

        private final String key;

        Interrupt(String key) { this.key = key; }

        /** The message key suffix: {@code moved}, {@code hurt}, {@code closed}, {@code released} or {@code lost}. */
        public String key() { return key; }
    }

    /** What one tick (or one event) did to the ritual. */
    public enum Step { CONTINUE, COMPLETE, INTERRUPT }

    private final int total;
    private final double tolerance;
    private int elapsed;
    private State state = State.CHANNELING;
    private Interrupt reason;

    /**
     * @param totalTicks    how many channelled ticks complete the ritual (at least one)
     * @param moveTolerance how far, in blocks, the player may stand from the centre of the circle before the ritual breaks (not negative)
     */
    public GemRitual(int totalTicks, double moveTolerance) {
        this.total = Math.max(1, totalTicks);
        this.tolerance = Math.max(0.0, moveTolerance);
    }

    /**
     * One channelled server tick.
     *
     * @param moved how far the player is from the centre of the circle now, in blocks
     * @param hurt  whether anything hurt the player since the last tick
     * @param open  whether the space above the player is still clear
     */
    public Step tick(double moved, boolean hurt, boolean open) {
        if (state == State.COMPLETED) return Step.COMPLETE;
        if (state == State.INTERRUPTED) return Step.INTERRUPT;
        if (hurt) return stop(Interrupt.HURT);
        if (moved > tolerance) return stop(Interrupt.MOVED);
        if (!open) return stop(Interrupt.CLOSED);
        elapsed++;
        if (elapsed >= total) {
            state = State.COMPLETED;
            return Step.COMPLETE;
        }
        return Step.CONTINUE;
    }

    /** Ends a ritual that is still channelling for the given reason (the player let go, the gem was lost); a ritual that already ended keeps its result. */
    public Step interrupt(Interrupt why) {
        if (state == State.CHANNELING) return stop(why);
        return state == State.COMPLETED ? Step.COMPLETE : Step.INTERRUPT;
    }

    private Step stop(Interrupt why) {
        state = State.INTERRUPTED;
        reason = why;
        return Step.INTERRUPT;
    }

    public State state() { return state; }

    /** Why the ritual was interrupted, or null while it channels or after it completed. */
    public Interrupt reason() { return reason; }

    /** Channelled ticks so far. */
    public int elapsed() { return elapsed; }

    /** Channelled ticks that complete the ritual. */
    public int total() { return total; }

    /** Ticks still to channel (zero once completed). */
    public int remaining() { return Math.max(0, total - elapsed); }

    /** The whole seconds left, rounded up, for the countdown ({@code 5, 4, 3 ...}); zero once completed. */
    public int secondsLeft() { return (remaining() + 19) / 20; }

    /** How far along the channel is, 0 to 1. */
    public double progress() { return Math.min(1.0, elapsed / (double) total); }

    /** True once the ritual has completed or been interrupted. */
    public boolean ended() { return state != State.CHANNELING; }

    /** The centre-of-circle distance that breaks the ritual, in blocks. */
    public double tolerance() { return tolerance; }
}
