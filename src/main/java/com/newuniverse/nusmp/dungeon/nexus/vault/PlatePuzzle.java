package com.newuniverse.nusmp.dungeon.nexus.vault;

/**
 * The plate puzzle of a Puzzle Vault as a plain state machine: nine plates in a 3 x 3 floor (0 to 8, row by row), a sequence of them to be
 * stepped in order. The room hints the sequence by lighting the plates in turn; a wrong plate sends the visitor back to the start (and
 * zaps them lightly, which the glue does). A step is an edge: standing on a plate counts once, and nothing counts until the visitor
 * has stepped off it. Pure.
 */
public final class PlatePuzzle {
    public static final int PLATES = 9, MIN_LENGTH = 4, MAX_LENGTH = 6;

    /** What a step did. */
    public enum Result { NONE, CORRECT, WRONG, SOLVED }

    private final int[] sequence;
    private int progress;
    private int standing = -1;
    private boolean solved;

    public PlatePuzzle(int[] sequence) {
        if (sequence.length < 1 || sequence.length > PLATES) throw new IllegalArgumentException("a sequence needs 1 to 9 plates");
        boolean[] seen = new boolean[PLATES];
        for (int p : sequence) {
            if (p < 0 || p >= PLATES || seen[p]) throw new IllegalArgumentException("a sequence needs distinct plates 0 to 8");
            seen[p] = true;
        }
        this.sequence = sequence.clone();
    }

    /** A stable sequence of `length` distinct plates for a seed (a Fisher-Yates shuffle of the nine, cut short). */
    public static int[] sequenceFor(long seed, int length) {
        int n = Math.max(1, Math.min(PLATES, length));
        int[] all = new int[PLATES];
        for (int i = 0; i < PLATES; i++) all[i] = i;
        long h = seed;
        for (int i = PLATES - 1; i > 0; i--) {
            h = h * 6364136223846793005L + 1442695040888963407L;
            int j = (int) Math.floorMod(h >>> 33, (long) (i + 1));
            int t = all[i];
            all[i] = all[j];
            all[j] = t;
        }
        int[] out = new int[n];
        System.arraycopy(all, 0, out, 0, n);
        return out;
    }

    /** The length a vault's puzzle has: 4 to 6, from its number. */
    public static int lengthFor(int variant) { return MIN_LENGTH + Math.floorMod(variant >>> 5, MAX_LENGTH - MIN_LENGTH + 1); }

    /** The visitor now stands on this plate (or on none: -1). Returns what that did. */
    public Result step(int plate) {
        if (plate == standing) return Result.NONE;
        standing = plate;
        if (solved || plate < 0) return Result.NONE;
        if (plate == sequence[progress]) {
            progress++;
            if (progress >= sequence.length) {
                solved = true;
                return Result.SOLVED;
            }
            return Result.CORRECT;
        }
        progress = 0;
        return Result.WRONG;
    }

    /** Forgets the progress (the room was left, or the puzzle was reset). */
    public void reset() {
        progress = 0;
        standing = -1;
    }

    public int progress() { return progress; }

    public boolean solved() { return solved; }

    public int length() { return sequence.length; }

    /** The plate at position i of the sequence. */
    public int plateAt(int i) { return sequence[i]; }

    /** Has this plate been stepped correctly in the current run (is it before the next one in the sequence)? */
    public boolean done(int plate) {
        for (int i = 0; i < progress; i++) if (sequence[i] == plate) return true;
        return solved;
    }
}
