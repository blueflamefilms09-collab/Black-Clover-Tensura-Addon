package com.newuniverse.nusmp.compat.create;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Why: the trap goes where it cannot be walked round, so the finder must accept a 3-wide corridor and refuse a hall, a junction and a corridor with a doorway in its side. */
class LaneFinderTest {
    /** A corridor 3 wide at x = 10..12 (centre 11), z = 0..29, in walls 3 thick (x = 7..9 and 13..15); an optional doorway through the east wall at z = 12..14. */
    private static LaneFinder.Grid corridor(boolean sideDoor) {
        return new LaneFinder.Grid() {
            @Override public boolean open(int x, int z) { return x >= 10 && x <= 12 && z >= 0 && z <= 29 || sideDoor && x >= 13 && x <= 15 && z >= 12 && z <= 14; }

            @Override public boolean wall(int x, int z) { return !open(x, z) && x >= 7 && x <= 15 && z >= 0 && z <= 29; }
        };
    }

    @Test
    void aCorridorHoldsAStripInTheMiddleOfItsRun() {
        List<LaneFinder.Site> s = LaneFinder.find(corridor(false), 7, 15, 0, 29, 10, false);
        assertFalse(s.isEmpty());
        LaneFinder.Site site = s.get(0);
        assertEquals(11, site.originX());
        assertEquals(2, site.forward(), "south = +z");
        assertEquals(30, site.runLength());
        assertEquals(10, site.originZ(), "the 10-long strip sits in the middle of the 30-long run");
    }

    @Test
    void flippingTurnsTheStripRound() {
        LaneFinder.Site fwd = LaneFinder.find(corridor(false), 7, 15, 0, 29, 10, false).get(0), back = LaneFinder.find(corridor(false), 7, 15, 0, 29, 10, true).get(0);
        assertEquals(0, back.forward());
        assertEquals(fwd.originZ() + 9, back.originZ(), "the origin is the other end of the same strip");
        assertEquals(fwd.originX(), back.originX());
    }

    @Test
    void aDoorwayInTheSideBreaksTheRunInTwo() {
        List<LaneFinder.Site> s = LaneFinder.find(corridor(true), 7, 15, 0, 29, 10, false);
        assertEquals(2, s.size(), "0..11 and 15..29 are left");
        for (LaneFinder.Site site : s) {
            int a = site.originZ(), b = site.originZ() + 9;
            assertFalse(b >= 12 && a <= 14, "the strip must not span the doorway: " + a + ".." + b);
        }
        assertTrue(LaneFinder.find(corridor(true), 7, 15, 0, 29, 17, false).isEmpty(), "no 17-long stretch is left");
    }

    @Test
    void aHallAndATooShortCorridorHoldNothing() {
        LaneFinder.Grid hall = new LaneFinder.Grid() {
            @Override public boolean open(int x, int z) { return x >= 0 && x <= 20 && z >= 0 && z <= 20; }

            @Override public boolean wall(int x, int z) { return !open(x, z); }
        };
        assertTrue(LaneFinder.find(hall, -4, 24, -4, 24, 8, false).isEmpty());
        assertTrue(LaneFinder.find(corridor(false), 7, 15, 0, 29, 31, false).isEmpty());
    }

    @Test
    void aCorridorRunningEastWestIsFoundToo() {
        LaneFinder.Grid ew = new LaneFinder.Grid() {
            @Override public boolean open(int x, int z) { return z >= 10 && z <= 12 && x >= 0 && x <= 29; }

            @Override public boolean wall(int x, int z) { return !open(x, z) && z >= 7 && z <= 15 && x >= 0 && x <= 29; }
        };
        List<LaneFinder.Site> s = LaneFinder.find(ew, 0, 29, 7, 15, 9, false);
        assertFalse(s.isEmpty());
        assertEquals(1, s.get(0).forward(), "east = +x");
        assertEquals(11, s.get(0).originZ());
    }

    @Test
    void theLocalFrameTurnsWithTheDirection() {
        // forward north (-z): right is east (+x); forward east: right is south; forward south: right is west; forward west: right is north
        assertEquals(List.of(6, 7), toList(LaneFinder.toWorld(5, 8, 0, 1, 1)));
        assertEquals(List.of(6, 9), toList(LaneFinder.toWorld(5, 8, 1, 1, 1)));
        assertEquals(List.of(4, 9), toList(LaneFinder.toWorld(5, 8, 2, 1, 1)));
        assertEquals(List.of(4, 7), toList(LaneFinder.toWorld(5, 8, 3, 1, 1)));
        for (int f = 0; f < 4; f++) assertEquals(List.of(5, 8), toList(LaneFinder.toWorld(5, 8, f, 0, 0)));
    }

    private static List<Integer> toList(int[] a) { return List.of(a[0], a[1]); }
}
