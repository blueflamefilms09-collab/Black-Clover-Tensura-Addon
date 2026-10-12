package com.newuniverse.nusmp.dungeon.nexus.vault;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The plate puzzle of a Puzzle Vault. */
class PlatePuzzleTest {
    @Test
    void theRightPlatesInOrderSolveIt() {
        PlatePuzzle p = new PlatePuzzle(new int[]{4, 0, 8, 2});
        assertEquals(PlatePuzzle.Result.CORRECT, p.step(4));
        assertEquals(1, p.progress());
        assertEquals(PlatePuzzle.Result.NONE, p.step(-1), "stepping off counts for nothing");
        assertEquals(PlatePuzzle.Result.CORRECT, p.step(0));
        assertEquals(PlatePuzzle.Result.CORRECT, p.step(8));
        assertFalse(p.solved());
        assertEquals(PlatePuzzle.Result.SOLVED, p.step(2));
        assertTrue(p.solved());
        assertEquals(PlatePuzzle.Result.NONE, p.step(5), "a solved puzzle ignores everything");
    }

    @Test
    void aWrongPlateResetsTheRun() {
        PlatePuzzle p = new PlatePuzzle(new int[]{1, 2, 3, 4});
        p.step(1);
        p.step(2);
        assertEquals(PlatePuzzle.Result.WRONG, p.step(7));
        assertEquals(0, p.progress());
        assertEquals(PlatePuzzle.Result.CORRECT, p.step(1), "and it starts again");
        assertFalse(p.done(2));
        assertTrue(p.done(1));
    }

    @Test
    void standingStillCountsOnce() {
        PlatePuzzle p = new PlatePuzzle(new int[]{3, 5, 6, 7});
        assertEquals(PlatePuzzle.Result.CORRECT, p.step(3));
        assertEquals(PlatePuzzle.Result.NONE, p.step(3));
        assertEquals(PlatePuzzle.Result.NONE, p.step(3));
        assertEquals(1, p.progress());
        // stepping back onto the same plate after leaving it is a wrong plate (the next one is 5)
        p.step(-1);
        assertEquals(PlatePuzzle.Result.WRONG, p.step(3));
    }

    @Test
    void aRunCannotSkipOrRepeatAPlateIntoASolution() {
        PlatePuzzle p = new PlatePuzzle(new int[]{0, 1, 2, 3});
        assertEquals(PlatePuzzle.Result.WRONG, p.step(1), "starting in the middle is wrong");
        p.reset();
        for (int round = 0; round < 3; round++) {
            assertEquals(PlatePuzzle.Result.CORRECT, p.step(0));
            assertEquals(PlatePuzzle.Result.WRONG, p.step(2), "skipping one is wrong");
        }
    }

    @Test
    void sequencesAreDistinctStableAndTheRightLength() {
        Set<Integer> lengths = new HashSet<>();
        for (int seed = 0; seed < 500; seed++) {
            int len = PlatePuzzle.lengthFor(seed * 977);
            assertTrue(len >= PlatePuzzle.MIN_LENGTH && len <= PlatePuzzle.MAX_LENGTH, "length " + len);
            lengths.add(len);
            int[] s = PlatePuzzle.sequenceFor(seed * 31L + 7, len);
            assertEquals(len, s.length);
            assertArrayEquals(s, PlatePuzzle.sequenceFor(seed * 31L + 7, len), "stable");
            Set<Integer> seen = new HashSet<>();
            for (int v : s) {
                assertTrue(v >= 0 && v < PlatePuzzle.PLATES);
                assertTrue(seen.add(v), "distinct");
            }
            new PlatePuzzle(s);
        }
        assertEquals(3, lengths.size(), "all three lengths occur");
        Set<String> distinct = new HashSet<>();
        for (int seed = 0; seed < 200; seed++) distinct.add(java.util.Arrays.toString(PlatePuzzle.sequenceFor(seed, 5)));
        assertTrue(distinct.size() > 150, "the seeds give different puzzles");
    }

    @Test
    void badSequencesAreRefused() {
        assertThrows(IllegalArgumentException.class, () -> new PlatePuzzle(new int[]{}));
        assertThrows(IllegalArgumentException.class, () -> new PlatePuzzle(new int[]{1, 1}));
        assertThrows(IllegalArgumentException.class, () -> new PlatePuzzle(new int[]{9}));
        assertThrows(IllegalArgumentException.class, () -> new PlatePuzzle(new int[]{-1}));
    }
}
