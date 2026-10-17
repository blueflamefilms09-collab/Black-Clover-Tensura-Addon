package com.newuniverse.nusmp.avatar.menu;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The slot-machine reel: always ends exactly on the server's winner, never overshoots, the same seed shows the same reel, decoys are not repeated side by side. */
class SlotReelTest {
    private static final List<String> POOL = List.of("fire", "water", "wind", "earth", "plant", "ice", "lightning", "light", "dark", "time", "anti_magic");

    @Test
    void theReelEndsExactlyOnTheWinnerAtTheLastTick() {
        for (String winner : POOL) {
            for (int seed = -3; seed < 30; seed++) {
                SlotReel r = SlotReel.build(winner, POOL, seed, 32, 60);
                assertEquals(winner, r.winner());
                assertEquals(winner, r.idAt((int) r.position(60)), "winner at the end for seed " + seed);
                assertEquals(r.winnerIndex(), r.position(60), 0f);
                assertEquals(r.winnerIndex(), r.position(9999), 0f, "stays on the winner");
                assertEquals(0f, r.position(0), 0f);
            }
        }
    }

    @Test
    void theReelNeverGoesBackwardsOrPastTheWinner() {
        SlotReel r = SlotReel.build("time", POOL, 7, 40, 80);
        float prev = -1f;
        for (int t = 0; t <= 100; t++) {
            float p = r.position(t);
            assertTrue(p >= prev, "monotone at " + t);
            assertTrue(p <= r.winnerIndex(), "never past the winner at " + t);
            prev = p;
        }
    }

    @Test
    void theReelStartsFastAndEndsSlow() {
        SlotReel r = SlotReel.build("time", POOL, 7, 40, 80);
        float first = r.position(5) - r.position(0), last = r.position(80) - r.position(75);
        assertTrue(first > 4f * last, "first 5 ticks cover much more than the last 5: " + first + " vs " + last);
    }

    @Test
    void theSameSeedMakesTheSameReelAndAnotherSeedADifferentOne() {
        SlotReel a = SlotReel.build("dark", POOL, 11, 30, 50), b = SlotReel.build("dark", POOL, 11, 30, 50), c = SlotReel.build("dark", POOL, 12, 30, 50);
        List<String> la = new ArrayList<>(), lb = new ArrayList<>(), lc = new ArrayList<>();
        for (int i = 0; i < a.cells(); i++) { la.add(a.idAt(i)); lb.add(b.idAt(i)); lc.add(c.idAt(i)); }
        assertEquals(la, lb);
        assertNotEquals(la, lc);
    }

    @Test
    void noDecoyRepeatsItsNeighbourAndTheWinnerOnlyAppearsAtTheEnd() {
        for (int seed = 0; seed < 50; seed++) {
            SlotReel r = SlotReel.build("time", POOL, seed, 36, 60);
            for (int i = 1; i < r.cells(); i++) assertFalse(r.idAt(i - 1).equals(r.idAt(i)), "no repeats at " + i + " seed " + seed);
            for (int i = 0; i < r.cells() - 1; i++) assertFalse("time".equals(r.idAt(i)), "the winner shows only on the last cell");
        }
    }

    @Test
    void aTinyOrOddPoolStillWorks() {
        SlotReel one = SlotReel.build("fire", List.of("fire"), 1, 10, 20);
        assertEquals("fire", one.idAt(one.winnerIndex()));
        SlotReel none = SlotReel.build("fire", List.of(), 1, 10, 20);
        assertEquals("fire", none.winner());
        SlotReel nul = SlotReel.build("fire", null, 1, 10, 20);
        assertEquals("fire", nul.winner());
        SlotReel two = SlotReel.build("fire", List.of("fire", "water"), 5, 10, 20);
        assertEquals("fire", two.winner());
        assertEquals(10, two.cells());
    }

    @Test
    void cellsAreClampedIntoTheStripAndLengthsHaveAFloor() {
        SlotReel r = SlotReel.build("time", POOL, 3, 2, 1);
        assertTrue(r.cells() >= 8);
        assertTrue(r.totalTicks() >= 10);
        assertEquals(r.idAt(0), r.idAt(-5));
        assertEquals(r.winner(), r.idAt(500));
    }

    @Test
    void theTickSoundCountMatchesTheCellsPassed() {
        SlotReel r = SlotReel.build("time", POOL, 4, 30, 60);
        int total = 0;
        for (int t = 0; t < 60; t++) total += r.cellsCrossed(t, t + 1);
        assertEquals(r.winnerIndex(), total, "every cell boundary is counted exactly once");
        assertEquals(0, r.cellsCrossed(60, 70));
        assertEquals(0, r.cellsCrossed(10, 5), "never negative");
    }
}
