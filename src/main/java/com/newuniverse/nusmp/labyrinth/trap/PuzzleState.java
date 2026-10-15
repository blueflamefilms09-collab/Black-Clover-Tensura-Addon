package com.newuniverse.nusmp.labyrinth.trap;

import java.util.Arrays;

/**
 * Why: the logic of the puzzle door as a pure state machine, the pattern of {@code TrialState}: a Riddle Stone holds one of these, a Sigil
 * Plate click feeds it a colour and the answer says what the world has to do (light the plate, open the door, hurt the player, say
 * "wait"). Keeping it free of Minecraft classes means the order rule, the reset lock and the "already solved is final" rule are tested
 * line by line, and the block entity is only glue. The state is what the player cannot fake: the progress index and the lock time live
 * with the stone (saved in its NBT), never in the plates, so two players pressing plates at once share one sequence, a plate moved by
 * an admin changes nothing and a reload mid-puzzle keeps the progress. Time is the game tick passed in, not a clock read here, so tests
 * drive it by hand and a server that was paused does not count the pause against the lock.
 */
public final class PuzzleState {
    /** The four sigil colours: 0 red, 1 blue, 2 gold, 3 green. */
    public static final int COLOURS = 4;
    /** The longest sequence a Riddle Stone accepts. */
    public static final int MAX_LENGTH = 8;
    /** Lang-key suffixes and wording of the colours, in colour order. */
    public static final String[] COLOUR_NAMES = {"red", "blue", "gold", "green"};

    /** What one press did. */
    public enum Outcome { CORRECT, SOLVED, WRONG, LOCKED, ALREADY_SOLVED }

    private final int[] sequence;
    private int progress;
    private boolean solved;
    private long lockedUntil;

    public PuzzleState(int[] sequence) {
        if (!valid(sequence)) throw new IllegalArgumentException("a puzzle sequence is 1.." + MAX_LENGTH + " colours of 0.." + (COLOURS - 1) + ": " + Arrays.toString(sequence));
        this.sequence = sequence.clone();
    }

    /** Is this a usable sequence? */
    public static boolean valid(int[] s) {
        if (s == null || s.length < 1 || s.length > MAX_LENGTH) return false;
        for (int c : s) if (c < 0 || c >= COLOURS) return false;
        return true;
    }

    /**
     * The sequence of a Riddle Stone whose template carried none (or a broken one): a permutation of the four colours derived only from the
     * seed (the block position), so the same stone always asks the same riddle and two stones rarely agree. A Fisher-Yates shuffle over a
     * 64-bit linear congruential stream; the constants are Knuth's MMIX, so there is no dependency on {@code java.util.Random}'s
     * algorithm.
     */
    public static int[] defaultSequence(long seed) {
        int[] s = {0, 1, 2, 3};
        long x = seed * 0x9E3779B97F4A7C15L + 0x1234567L;
        for (int i = s.length - 1; i > 0; i--) {
            x = x * 6364136223846793005L + 1442695040888963407L;
            int j = (int) ((x >>> 33) % (i + 1));
            int t = s[i];
            s[i] = s[j];
            s[j] = t;
        }
        return s;
    }

    /** The given sequence if it is valid, else the default for the seed. */
    public static int[] sanitize(int[] raw, long seed) { return valid(raw) ? raw.clone() : defaultSequence(seed); }

    /**
     * One press of a plate of this colour at game time {@code now}. While locked nothing counts (LOCKED); once solved nothing changes
     * (ALREADY_SOLVED); the next colour of the sequence advances (CORRECT, or SOLVED on the last); any other colour is WRONG: progress goes
     * back to zero and the stone locks for {@code resetTicks}.
     */
    public Outcome press(int colour, long now, int resetTicks) {
        if (solved) return Outcome.ALREADY_SOLVED;
        if (now < lockedUntil) return Outcome.LOCKED;
        if (colour == sequence[progress]) {
            progress++;
            if (progress >= sequence.length) {
                solved = true;
                progress = sequence.length;
                return Outcome.SOLVED;
            }
            return Outcome.CORRECT;
        }
        progress = 0;
        lockedUntil = now + Math.max(0, resetTicks);
        return Outcome.WRONG;
    }

    /** Ticks left on the lock at {@code now}, 0 when free. */
    public long lockRemaining(long now) { return Math.max(0L, lockedUntil - now); }

    public boolean solved() { return solved; }

    public int progress() { return progress; }

    public int length() { return sequence.length; }

    public long lockedUntil() { return lockedUntil; }

    /** A copy of the target sequence. */
    public int[] sequence() { return sequence.clone(); }

    /** Restores saved progress (clamped: a corrupt save can neither skip the puzzle nor crash it). */
    public void restore(int progress, boolean solved, long lockedUntil) {
        this.solved = solved;
        this.progress = solved ? sequence.length : Math.max(0, Math.min(progress, sequence.length - 1));
        this.lockedUntil = Math.max(0L, lockedUntil);
    }
}
