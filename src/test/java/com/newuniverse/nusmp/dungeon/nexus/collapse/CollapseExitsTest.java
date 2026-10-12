package com.newuniverse.nusmp.dungeon.nexus.collapse;

import com.newuniverse.nusmp.dungeon.nexus.Footprint;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Where the ways out of a collapsing Nexus are taken from. */
class CollapseExitsTest {
    private static final Footprint FP = Footprint.oval(0, 0, 47, 47);

    private static int[] spot(int x, int y, int z) { return new int[]{x, y, z}; }

    @Test
    void theEntrancesOfThePlanAreTheExits() {
        List<int[]> in = List.of(spot(43, -51, 0), spot(-43, -51, 0), spot(0, -51, 43));
        List<int[]> out = CollapseExits.fromPlan(in, List.of(spot(0, -51, -30)), spot(1, 2, 3), FP, -52);
        assertEquals(3, out.size());
        for (int i = 0; i < 3; i++) assertArrayEquals(in.get(i), out.get(i));
        out.get(0)[0] = 999;
        assertEquals(43, in.get(0)[0], "the answer is a copy");
    }

    @Test
    void closeEntrancesAreOneWayOut() {
        List<int[]> in = List.of(spot(43, -51, 0), spot(45, -51, 2), spot(-43, -51, 0), spot(43, -51, 5));
        List<int[]> out = CollapseExits.fromPlan(in, null, null, FP, -52);
        assertEquals(2, out.size(), "(43,0) with (45,2) and (43,5) is one doorway, the far side is the other");
        assertEquals(43, out.get(0)[0]);
        assertEquals(-43, out.get(1)[0]);
    }

    @Test
    void thereAreNeverMoreThanTheMostAndNeverNone() {
        List<int[]> many = new ArrayList<>();
        for (int i = 0; i < 40; i++) many.add(spot(i * 10, -51, 0));
        assertEquals(CollapseExits.MAX, CollapseExits.fromPlan(many, null, null, FP, -52).size());
        List<int[]> a = CollapseExits.fromPlan(List.of(), List.of(spot(5, -51, 5)), null, FP, -52);
        assertEquals(1, a.size());
        assertArrayEquals(spot(5, -51, 5), a.get(0), "no entrances: the arrival spot");
        List<int[]> r = CollapseExits.fromPlan(null, null, spot(7, -51, 8), FP, -52);
        assertArrayEquals(spot(7, -51, 8), r.get(0), "no arrival either: the rescue point");
        List<int[]> c = CollapseExits.fromPlan(null, null, null, FP, -52);
        assertEquals(1, c.size());
        assertArrayEquals(spot(0, -51, 0), c.get(0), "nothing at all: the middle of the floor");
        assertEquals(1, CollapseExits.fromPlan(List.of(new int[]{1}), List.of(new int[0]), new int[]{4, 5}, FP, -52).size(), "malformed spots are ignored");
    }

    @Test
    void theGapRuleIsInclusiveOfExactlyTheMinimum() {
        List<int[]> taken = List.of(spot(0, 0, 0));
        assertFalse(CollapseExits.farEnough(taken, 5, 0, CollapseExits.MIN_GAP));
        assertTrue(CollapseExits.farEnough(taken, 6, 0, CollapseExits.MIN_GAP));
        assertTrue(CollapseExits.farEnough(taken, 0, -6, CollapseExits.MIN_GAP));
        assertFalse(CollapseExits.farEnough(taken, 4, 4, CollapseExits.MIN_GAP));
        assertTrue(CollapseExits.farEnough(List.of(), 0, 0, 100));
        assertFalse(CollapseExits.farEnough(taken, 0, 0, 1));
    }

    @Test
    void aRingIsEvenAtTheRightRadiusAndAlwaysTheSame() {
        List<int[]> ring = CollapseExits.ring(100, -50, 14, 8, 3);
        assertEquals(8, ring.size());
        for (int[] c : ring) {
            double d = Math.hypot(c[0] - 100, c[1] + 50);
            assertEquals(14.0, d, 1.0, "on the ring");
        }
        for (int i = 0; i < ring.size(); i++) {
            for (int j = i + 1; j < ring.size(); j++) assertFalse(ring.get(i)[0] == ring.get(j)[0] && ring.get(i)[1] == ring.get(j)[1], "all different");
        }
        List<int[]> again = CollapseExits.ring(100, -50, 14, 8, 3);
        for (int i = 0; i < ring.size(); i++) assertArrayEquals(ring.get(i), again.get(i));
        List<int[]> other = CollapseExits.ring(100, -50, 14, 8, 4);
        boolean differs = false;
        for (int i = 0; i < ring.size(); i++) differs |= ring.get(i)[0] != other.get(i)[0] || ring.get(i)[1] != other.get(i)[1];
        assertTrue(differs, "another phase turns the ring");
        assertTrue(CollapseExits.ring(0, 0, 14, 0, 0).isEmpty());
        assertTrue(CollapseExits.ring(0, 0, 0, 8, 0).isEmpty());
        assertEquals(8, CollapseExits.ring(0, 0, 14, 8, Integer.MIN_VALUE).size(), "any phase number works");
        assertEquals(8, CollapseExits.ring(0, 0, 14, 8, -7).size());
    }

    @Test
    void theNearestSpotIsFoundInTheFlatPlane() {
        List<int[]> spots = List.of(spot(43, -51, 0), spot(-43, 0, 0), spot(0, 99, 43));
        assertEquals(0, CollapseExits.nearest(spots, 30, 2));
        assertEquals(1, CollapseExits.nearest(spots, -20, -1));
        assertEquals(2, CollapseExits.nearest(spots, 3, 30));
        assertEquals(-1, CollapseExits.nearest(List.of(), 0, 0));
        assertEquals(0, CollapseExits.nearest(List.of(spot(5, 5, 5)), 1e9, -1e9));
    }
}
