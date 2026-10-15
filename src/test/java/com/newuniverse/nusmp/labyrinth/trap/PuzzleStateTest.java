package com.newuniverse.nusmp.labyrinth.trap;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static com.newuniverse.nusmp.labyrinth.trap.PuzzleState.Outcome.*;
import static org.junit.jupiter.api.Assertions.*;

/** The puzzle door's state machine: order, the wrong-order lock, the final solved state, saving and the default riddle. */
class PuzzleStateTest {
    private static final int RESET = 100;

    @Test
    void theRightOrderSolvesIt() {
        PuzzleState s = new PuzzleState(new int[]{0, 2, 1, 3});
        assertEquals(CORRECT, s.press(0, 0, RESET));
        assertEquals(CORRECT, s.press(2, 1, RESET));
        assertEquals(CORRECT, s.press(1, 2, RESET));
        assertEquals(3, s.progress());
        assertFalse(s.solved());
        assertEquals(SOLVED, s.press(3, 3, RESET));
        assertTrue(s.solved());
    }

    @Test
    void aWrongColourResetsTheProgressAndLocksForTheResetTime() {
        PuzzleState s = new PuzzleState(new int[]{0, 2, 1, 3});
        s.press(0, 10, RESET);
        s.press(2, 11, RESET);
        assertEquals(WRONG, s.press(3, 12, RESET));
        assertEquals(0, s.progress(), "the order starts again");
        assertEquals(112, s.lockedUntil());
        assertEquals(LOCKED, s.press(0, 50, RESET), "even the right colour counts for nothing while locked");
        assertEquals(LOCKED, s.press(0, 111, RESET));
        assertEquals(CORRECT, s.press(0, 112, RESET), "free again at the lock time");
    }

    @Test
    void theLockCountsDownAndNeverGoesNegative() {
        PuzzleState s = new PuzzleState(new int[]{1, 0});
        s.press(2, 5, RESET);
        assertEquals(100, s.lockRemaining(5));
        assertEquals(40, s.lockRemaining(65));
        assertEquals(0, s.lockRemaining(105));
        assertEquals(0, s.lockRemaining(500));
    }

    @Test
    void solvedIsFinal() {
        PuzzleState s = new PuzzleState(new int[]{3});
        assertEquals(SOLVED, s.press(3, 0, RESET));
        assertEquals(ALREADY_SOLVED, s.press(0, 1, RESET), "a later wrong press cannot undo it, and costs nothing");
        assertEquals(ALREADY_SOLVED, s.press(3, 2, RESET));
        assertTrue(s.solved());
        assertEquals(0, s.lockedUntil());
    }

    @Test
    void aSequenceMayRepeatAColour() {
        PuzzleState s = new PuzzleState(new int[]{1, 1, 2});
        assertEquals(CORRECT, s.press(1, 0, RESET));
        assertEquals(CORRECT, s.press(1, 1, RESET));
        assertEquals(SOLVED, s.press(2, 2, RESET));
    }

    @Test
    void aZeroResetStillCountsTheWrongPressButNeverLocks() {
        PuzzleState s = new PuzzleState(new int[]{0, 1});
        assertEquals(WRONG, s.press(2, 7, 0));
        assertEquals(CORRECT, s.press(0, 7, 0));
        assertEquals(WRONG, s.press(0, 8, -5), "a negative reset is a zero reset");
        assertEquals(0, s.lockRemaining(8));
    }

    @Test
    void validatesTheSequence() {
        assertTrue(PuzzleState.valid(new int[]{0, 1, 2, 3}));
        assertTrue(PuzzleState.valid(new int[]{3}));
        assertFalse(PuzzleState.valid(null));
        assertFalse(PuzzleState.valid(new int[0]));
        assertFalse(PuzzleState.valid(new int[]{4}));
        assertFalse(PuzzleState.valid(new int[]{-1}));
        assertFalse(PuzzleState.valid(new int[PuzzleState.MAX_LENGTH + 1]), "too long");
        assertTrue(PuzzleState.valid(new int[PuzzleState.MAX_LENGTH]));
        boolean threw = false;
        try { new PuzzleState(new int[]{9}); } catch (IllegalArgumentException e) { threw = true; }
        assertTrue(threw);
    }

    @Test
    void theDefaultRiddleIsAPermutationFixedByTheSeed() {
        for (long seed = -50; seed < 500; seed += 7) {
            int[] d = PuzzleState.defaultSequence(seed);
            int[] sorted = d.clone();
            Arrays.sort(sorted);
            assertTrue(Arrays.equals(new int[]{0, 1, 2, 3}, sorted), "every colour exactly once for seed " + seed);
            assertTrue(Arrays.equals(d, PuzzleState.defaultSequence(seed)), "deterministic");
        }
        boolean differs = false;
        for (long seed = 0; seed < 40; seed++) if (!Arrays.equals(PuzzleState.defaultSequence(seed), PuzzleState.defaultSequence(seed + 1))) differs = true;
        assertTrue(differs, "not the same riddle for every stone");
    }

    @Test
    void sanitizeKeepsAGoodSequenceAndReplacesABadOne() {
        assertTrue(Arrays.equals(new int[]{3, 2}, PuzzleState.sanitize(new int[]{3, 2}, 5)));
        assertTrue(Arrays.equals(PuzzleState.defaultSequence(5), PuzzleState.sanitize(new int[]{7}, 5)));
        assertTrue(Arrays.equals(PuzzleState.defaultSequence(5), PuzzleState.sanitize(null, 5)));
    }

    @Test
    void restoreClampsACorruptSave() {
        PuzzleState s = new PuzzleState(new int[]{0, 1, 2});
        s.restore(99, false, -4);
        assertEquals(2, s.progress(), "cannot skip to the end without the last colour");
        assertEquals(0, s.lockedUntil());
        assertFalse(s.solved());
        s.restore(1, true, 0);
        assertTrue(s.solved());
        assertEquals(3, s.progress());
        assertEquals(ALREADY_SOLVED, s.press(0, 0, RESET));
        PuzzleState t = new PuzzleState(new int[]{0, 1});
        t.restore(-3, false, 40);
        assertEquals(0, t.progress());
        assertEquals(LOCKED, t.press(0, 10, RESET));
    }

    @Test
    void theSequenceIsACopy() {
        int[] raw = {0, 1};
        PuzzleState s = new PuzzleState(raw);
        raw[0] = 3;
        assertEquals(CORRECT, s.press(0, 0, RESET), "changing the input afterwards changes nothing");
        s.sequence()[1] = 2;
        assertEquals(SOLVED, s.press(1, 1, RESET));
    }

    @Test
    void fourColourNamesInOrder() {
        assertEquals(PuzzleState.COLOURS, PuzzleState.COLOUR_NAMES.length);
        assertEquals("red", PuzzleState.COLOUR_NAMES[0]);
        assertEquals("green", PuzzleState.COLOUR_NAMES[3]);
    }
}
