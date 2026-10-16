package com.newuniverse.nusmp.butoh;

/**
 * Why: the dance has to look unpredictable to everyone watching, yet the server (which rolls evasion by the current move) and every client
 * (which plays the clips) must agree on which move is on at which tick without a packet per move. So the move list is a pure function of one
 * 64-bit seed: the server sends the seed once when the dance starts, and both sides walk the same sequence from it. A move never repeats the
 * one before it, and lasts 12 to 30 ticks. No game classes, so it is unit tested.
 */
public final class DanceChoreographer {
    private DanceChoreographer() {}

    public static final int MIN_TICKS = 12, MAX_TICKS = 30;

    /** One move of the sequence: which move, which step number, the tick (since the dance began) it starts on, and how long it lasts. */
    public record Step(int index, DanceMove move, int startTick, int length) {
        public int endTick() { return startTick + length; }
    }

    /** SplitMix64: a well mixed 64-bit value from a counter, the same on every machine. */
    static long mix(long z) {
        z += 0x9E3779B97F4A7C15L;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /** The sequence seed for a player's dance: from the player's id halves and the game tick it started on. */
    public static long seedFor(long idMost, long idLeast, long startTick) {
        return mix(idMost ^ mix(idLeast ^ mix(startTick)));
    }

    /** The move of step {@code index}, given the move of step index-1 (the first step has no predecessor). */
    static DanceMove pick(long seed, int index, DanceMove previous) {
        long r = mix(seed ^ (index * 0x632BE59BD9B4E019L));
        int n = DanceMove.count();
        if (previous == null) return DanceMove.byIndex((int) Long.remainderUnsigned(r, n));
        // choose among the other n-1 moves, so the same move can never come twice in a row
        int k = (int) Long.remainderUnsigned(r, n - 1);
        int idx = k >= previous.ordinal() ? k + 1 : k;
        return DanceMove.byIndex(idx);
    }

    /** The length in ticks of step {@code index}: 12 to 30, from its own mix of the seed. */
    static int lengthOf(long seed, int index) {
        long r = mix(seed ^ mix(index + 0x5851F42D4C957F2DL));
        return MIN_TICKS + (int) Long.remainderUnsigned(r, MAX_TICKS - MIN_TICKS + 1);
    }

    /**
     * A walker over one dance: ask for the step at any tick since the dance began. Moving forward costs one step at a time (no recomputing
     * the whole sequence per tick). Asking for an earlier tick starts over from step 0, which stays correct.
     */
    public static final class Cursor {
        private final long seed;
        private Step cur;

        public Cursor(long seed) {
            this.seed = seed;
            this.cur = first();
        }

        private Step first() { return new Step(0, pick(seed, 0, null), 0, lengthOf(seed, 0)); }

        public long seed() { return seed; }

        public Step at(int tick) {
            if (tick < 0) tick = 0;
            if (tick < cur.startTick()) cur = first();
            while (tick >= cur.endTick()) {
                int next = cur.index() + 1;
                cur = new Step(next, pick(seed, next, cur.move()), cur.endTick(), lengthOf(seed, next));
            }
            return cur;
        }
    }
}
