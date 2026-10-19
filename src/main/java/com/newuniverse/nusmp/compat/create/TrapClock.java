package com.newuniverse.nusmp.compat.create;

/**
 * Why: the timing of a trap as two small pure pieces, so a test can walk a whole run tick by tick. {@link #phase} answers what a band is doing {@code since} ticks after the
 * run began; {@link Machine} is the trap's own state machine (idle, running, cooling down) fed once per look with whether it was triggered and whether it is disarmed. Time
 * is always passed in (the game tick), never read, so a paused server does not count the pause and a test needs no clock.
 */
public final class TrapClock {
    private TrapClock() {}

    public enum Phase { QUIET, WARN, STRIKE }

    /** What a band does {@code since} ticks after the run began: it starts after its offset, then warns, strikes and is quiet for the rest of its cycle, over and over. */
    public static Phase phase(long since, Blueprint.Band b) {
        long p = since - b.offset();
        if (p < 0 || b.period() <= 0) return Phase.QUIET;
        long c = p % b.period();
        if (c < b.telegraph()) return Phase.WARN;
        if (c < b.telegraph() + b.active()) return Phase.STRIKE;
        return Phase.QUIET;
    }

    /** The number of the cycle a band is in (0 before it has started counting): a hit is dealt at most once per band per cycle to one victim. */
    public static long cycle(long since, Blueprint.Band b) {
        long p = since - b.offset();
        return p < 0 || b.period() <= 0 ? -1 : p / b.period();
    }

    public enum State { IDLE, RUNNING, COOLDOWN, DISARMED }

    /** The state machine of one trap. */
    public static final class Machine {
        private State state = State.IDLE;
        private long since;

        public State state() { return state; }

        /** The tick the current state began. */
        public long since() { return since; }

        /** Ticks into the current run (0 when not running). */
        public long runAge(long now) { return state == State.RUNNING ? now - since : 0; }

        /**
         * Looks at the trap at game tick {@code now}. A disarmed trap stops at once whatever it was doing, and stays DISARMED until the lever goes back (then it is IDLE).
         * An IDLE trap that was triggered starts RUNNING; a run lasts {@code runTicks}, then the trap cools down for {@code cooldownTicks}, then it is IDLE again.
         * A trigger during the cool-down is ignored. Returns the new state.
         */
        public State look(long now, boolean triggered, boolean disarmed, int runTicks, int cooldownTicks) {
            if (disarmed) {
                if (state != State.DISARMED) { state = State.DISARMED; since = now; }
                return state;
            }
            if (state == State.DISARMED) { state = State.IDLE; since = now; }
            switch (state) {
                case IDLE -> { if (triggered) { state = State.RUNNING; since = now; } }
                case RUNNING -> { if (now - since >= runTicks) { state = State.COOLDOWN; since = now; } }
                case COOLDOWN -> { if (now - since >= cooldownTicks) { state = State.IDLE; since = now; } }
                default -> { }
            }
            return state;
        }
    }
}
