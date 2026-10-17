package com.newuniverse.nusmp.labyrinth.mega.enc;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Why: a wandering boss must walk the rooms the way the dungeon is built, cover the sector, and never enter a safe room; these tests pin the graph (edges both
 * ways, forbidden rooms present but never neighbours), the shape of the walk (it seldom turns back, it avoids rooms it failed to reach and rooms beside a safe room)
 * and where a boss may first appear.
 */
class PatrolGraphTest {
    private static PatrolGraph.Node node(int id, double x, double z, boolean allowed, boolean circulation) {
        return new PatrolGraph.Node(id, x, 65, z, allowed, circulation);
    }

    /** A row of four rooms with a safe room hanging off the second: 0 - 1 - 2 - 3 and 1 - 4(safe). */
    private static PatrolGraph row() {
        return PatrolGraph.build(List.of(node(0, 0, 0, true, false), node(1, 20, 0, true, true), node(2, 40, 0, true, false), node(3, 60, 0, true, false), node(4, 20, 20, false, false)),
                List.of(new int[] {0, 1}, new int[] {1, 2}, new int[] {2, 3}, new int[] {1, 4}));
    }

    /** A ring of six rooms. */
    private static PatrolGraph ring() {
        List<PatrolGraph.Node> nodes = new ArrayList<>();
        List<int[]> edges = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            nodes.add(node(i, Math.cos(i * Math.PI / 3) * 40, Math.sin(i * Math.PI / 3) * 40, true, false));
            edges.add(new int[] {i, (i + 1) % 6});
        }
        return PatrolGraph.build(nodes, edges);
    }

    @Test
    void edgesAreMadeBothWaysAndBadOnesAreIgnored() {
        PatrolGraph g = PatrolGraph.build(List.of(node(0, 0, 0, true, false), node(1, 10, 0, true, false)),
                List.of(new int[] {0, 1}, new int[] {1, 0}, new int[] {0, 0}, new int[] {0, 7}, new int[] {9, 1}, new int[] {1}));
        assertEquals(List.of(1), g.neighbours(0));
        assertEquals(List.of(0), g.neighbours(1));
        assertEquals(2, g.size());
        assertTrue(g.has(1));
        assertFalse(g.has(7));
        assertTrue(g.neighbours(7).isEmpty());
    }

    @Test
    void forbiddenNodesAreInTheGraphButNeverNeighbours() {
        PatrolGraph g = row();
        assertEquals(4, g.size(), "four rooms may be entered, the safe one may not");
        assertEquals(List.of(0, 2), g.neighbours(1));
        assertTrue(g.has(4));
        assertFalse(g.node(4).allowed());
        assertTrue(g.nearForbidden(1), "the corridor beside the safe room");
        assertFalse(g.nearForbidden(0));
        assertFalse(g.nearForbidden(3));
    }

    @Test
    void aWalkNeverEntersAForbiddenRoomAndCoversEveryAllowedOne() {
        PatrolGraph g = row();
        Random r = new Random(3);
        List<Integer> recent = new ArrayList<>();
        Set<Integer> visited = new HashSet<>();
        int at = 0;
        for (int i = 0; i < 400; i++) {
            recent.add(at);
            if (recent.size() > 6) recent.remove(0);
            int nxt = g.next(at, recent, Set.of(), r);
            assertTrue(nxt >= 0, "the walk never gets stuck");
            assertFalse(nxt == 4, "entered the forbidden room");
            visited.add(nxt);
            at = nxt;
        }
        assertEquals(Set.of(0, 1, 2, 3), visited);
    }

    @Test
    void aDeadEndTurnsBackToTheOnlyWayOut() {
        PatrolGraph g = row();
        Random r = new Random(1);
        assertEquals(1, g.next(0, List.of(1, 0), Set.of(), r));
        assertEquals(2, g.next(3, List.of(2, 3), Set.of(), r));
    }

    @Test
    void aNodeWithOnlyForbiddenNeighboursHasNoWay() {
        PatrolGraph g = PatrolGraph.build(List.of(node(0, 0, 0, true, false), node(1, 10, 0, false, false)), List.of(new int[] {0, 1}));
        assertEquals(-1, g.next(0, List.of(), Set.of(), new Random(1)));
        assertEquals(-1, g.next(99, List.of(), Set.of(), new Random(1)));
    }

    @Test
    void theWalkSeldomTurnsStraightBack() {
        PatrolGraph g = ring();
        Random r = new Random(11);
        int back = 0, n = 6000;
        for (int i = 0; i < n; i++) {
            // standing at 0 having come from 1: the choices are 1 (back) and 5 (on)
            int nxt = g.next(0, List.of(1, 0), Set.of(), r);
            if (nxt == 1) back++;
        }
        assertTrue(back > 0, "possible, though rare");
        assertTrue(back < n * 0.20, "turned back " + back + " of " + n);
    }

    @Test
    void recentNodesAreLessLikelyThanFreshOnes() {
        // a node with three neighbours, one visited lately
        PatrolGraph g = PatrolGraph.build(List.of(node(0, 0, 0, true, false), node(1, 20, 0, true, false), node(2, 0, 20, true, false), node(3, -20, 0, true, false)),
                List.of(new int[] {0, 1}, new int[] {0, 2}, new int[] {0, 3}));
        Random r = new Random(5);
        int[] count = new int[4];
        for (int i = 0; i < 9000; i++) count[g.next(0, List.of(3, 0), Set.of(), r)]++;
        assertTrue(count[1] > count[3] * 3 && count[2] > count[3] * 3, "fresh " + count[1] + " " + count[2] + " vs just-left " + count[3]);
        assertTrue(count[3] > 0);
    }

    @Test
    void avoidedNodesAreSkippedUnlessThereIsNothingElse() {
        PatrolGraph g = PatrolGraph.build(List.of(node(0, 0, 0, true, false), node(1, 20, 0, true, false), node(2, 0, 20, true, false)),
                List.of(new int[] {0, 1}, new int[] {0, 2}));
        Random r = new Random(2);
        for (int i = 0; i < 200; i++) assertEquals(2, g.next(0, List.of(), Set.of(1), r));
        int seen1 = 0;
        for (int i = 0; i < 200; i++) if (g.next(0, List.of(), Set.of(1, 2), r) == 1) seen1++;
        assertTrue(seen1 > 0, "when every way is avoided the avoidance is dropped");
    }

    @Test
    void nearestFindsTheClosestAllowedNodeInThePlane() {
        PatrolGraph g = row();
        assertEquals(0, g.nearest(-5, 3));
        assertEquals(2, g.nearest(41, -9));
        assertEquals(1, g.nearest(20, 18), "the safe room at (20, 20) is closer but not allowed");
        assertEquals(-1, PatrolGraph.build(List.of(node(0, 0, 0, false, false)), List.of()).nearest(0, 0));
        assertEquals(-1, PatrolGraph.build(List.of(), List.of()).nearest(0, 0));
    }

    @Test
    void aWandererAppearsFarFromThePartyAndPrefersRooms() {
        PatrolGraph g = row();
        List<double[]> party = new ArrayList<>();
        party.add(new double[] {0, 65, 0});
        Random r = new Random(4);
        int room3 = 0, other = 0;
        for (int i = 0; i < 3000; i++) {
            int id = g.spawnNode(party, 35, r);
            assertTrue(id == 2 || id == 3, "nodes 0 and 1 are within 35 of the party: " + id);
            if (id == 3) room3++;
            else other++;
        }
        assertTrue(room3 > 0 && other > 0);
        assertEquals(3, g.spawnNode(party, 500, r), "when nothing is far enough the farthest node is taken");
        assertEquals(-1, PatrolGraph.build(List.of(node(0, 0, 0, false, false)), List.of()).spawnNode(party, 10, r));
    }

    @Test
    void aCorridorIsDrawnLessOftenThanARoomForASpawn() {
        PatrolGraph g = PatrolGraph.build(List.of(node(0, 100, 0, true, true), node(1, 100, 30, true, false)), List.of(new int[] {0, 1}));
        List<double[]> party = List.of(new double[] {0, 65, 0});
        Random r = new Random(8);
        int corridor = 0, n = 8000;
        for (int i = 0; i < n; i++) if (g.spawnNode(party, 50, r) == 0) corridor++;
        assertTrue(corridor > n * 0.20 && corridor < n * 0.30, "a quarter, got " + corridor + " of " + n);
    }

    @Test
    void roomsNextToAForbiddenRoomAreWalkedToLessOften() {
        // 0 has two equal neighbours; 1 shares a doorway with a safe room (4), 2 does not
        PatrolGraph g = PatrolGraph.build(List.of(node(0, 0, 0, true, false), node(1, 20, 0, true, false), node(2, -20, 0, true, false), node(4, 40, 0, false, false)),
                List.of(new int[] {0, 1}, new int[] {0, 2}, new int[] {1, 4}));
        assertTrue(g.nearForbidden(1));
        assertFalse(g.nearForbidden(2));
        Random r = new Random(23);
        int[] count = new int[3];
        for (int i = 0; i < 9000; i++) count[g.next(0, List.of(), Set.of(), r)]++;
        assertTrue(count[2] > count[1] * 1.5, "far from the safe room " + count[2] + " vs next to it " + count[1]);
        assertTrue(count[1] > 1000, "but it is still visited: " + count[1]);
    }
}
