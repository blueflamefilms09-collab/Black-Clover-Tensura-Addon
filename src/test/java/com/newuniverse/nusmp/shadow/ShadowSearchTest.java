package com.newuniverse.nusmp.shadow;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The bounded, nearest-first search for a shadow spot. */
class ShadowSearchTest {
    @Test
    void neverReturnsMoreThanTheCap() {
        assertEquals(ShadowSearch.DEFAULT_CAP, ShadowSearch.candidates(0, 0, 1, 48, ShadowSearch.DEFAULT_CAP).size());
        assertEquals(10, ShadowSearch.candidates(1, 0, 0, 48, 10).size());
        assertTrue(ShadowSearch.candidates(1, 0, 0, 48, 0).isEmpty());
        assertTrue(ShadowSearch.candidates(1, 0, 0, 1, 400).isEmpty(), "a range below the first ring has nothing to check");
    }

    @Test
    void ringsGetFartherAndStayInRange() {
        List<int[]> c = ShadowSearch.candidates(0.3, -0.1, 0.9, 48, 400);
        double last = 0;
        for (int[] o : c) {
            double d = Math.sqrt(o[0] * o[0] + o[1] * o[1] + o[2] * o[2]);
            assertTrue(d <= 48.6, "within range, got " + d);
            assertTrue(d >= last - 1.6, "rings never jump back by more than rounding: " + d + " after " + last);
            last = Math.max(last, d);
        }
        int[] first = c.get(0);
        assertEquals(2.0, Math.sqrt(first[0] * first[0] + first[1] * first[1] + first[2] * first[2]), 0.6, "starts at the nearest ring");
    }

    @Test
    void theStraightAheadCellComesFirstInEachRing() {
        List<int[]> c = ShadowSearch.candidates(0, 0, 1, 10, 400);       // looking +z
        assertEquals(0, c.get(0)[0]);
        assertEquals(0, c.get(0)[1]);
        assertEquals(2, c.get(0)[2]);
        boolean ahead = false;
        for (int[] o : c) if (o[2] == 10 && o[0] == 0 && o[1] == 0) { ahead = true; break; }
        assertTrue(ahead, "the ring at 10 contains the straight cell");
    }

    @Test
    void cellsAreUniqueAndFollowTheLookDirection() {
        List<int[]> c = ShadowSearch.candidates(-1, 0, 0, 20, 400);      // looking -x
        Set<String> seen = new HashSet<>();
        for (int[] o : c) {
            assertTrue(seen.add(o[0] + "," + o[1] + "," + o[2]), "unique");
            assertTrue(o[0] < 0, "every cell is in front: " + o[0]);
        }
    }

    @Test
    void aZeroLookVectorStillWorks() {
        assertFalse(ShadowSearch.candidates(0, 0, 0, 12, 50).isEmpty());
    }

    @Test
    void extraManaGrowsWithDistanceAndIsCapped() {
        assertEquals(0.0, ShadowSearch.extraManaPercent(0, 0.1), 1e-9);
        assertEquals(2.0, ShadowSearch.extraManaPercent(20, 0.1), 1e-9);
        assertEquals(8.0, ShadowSearch.extraManaPercent(500, 0.1), 1e-9);
        assertEquals(0.0, ShadowSearch.extraManaPercent(-4, 0.1), 1e-9);
        assertEquals(0.0, ShadowSearch.extraManaPercent(20, -1), 1e-9);
    }
}
