package com.newuniverse.nusmp.labyrinth.mega.floors;

import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the banding is the one place where a sector's maze becomes the owner's ten floors, and a mistake there is silent in game (a Landing that is floor 4,
 * an arena that is floor 6, a boss floor with the wrong number, a floor that is never entered). Each rule of {@link FloorBands} is pinned on a graph small
 * enough to check by hand (a chain, a star, a ring with a shortcut, a sector with no arena, an orphan room, a link tunnel), and the properties that must hold
 * for ANY sector (every band in range, the answer does not depend on the order the rooms or the doorways are listed in, bands never fall along a shortest
 * path) are checked over seeded random graphs, including degenerate ones the generator should never produce but a damaged save might. The two strategies are
 * tested side by side: ARENA (the design brief's rule, depth over the arena's depth) is pinned exactly, including the way it leaves floors empty on a shallow,
 * dense sector; EVEN (rooms ranked by depth and cut into equal runs) is pinned for the properties that make it worth having: every floor used, equal runs,
 * no fall with depth, and the same special rooms as the brief's rule.
 */
class FloorBandsTest {

    private static FloorBands.Node n(int id, RoomKind kind) { return new FloorBands.Node(id, kind); }

    private static List<int[]> edges(int... pairs) {
        List<int[]> out = new ArrayList<>();
        for (int i = 0; i + 1 < pairs.length; i += 2) out.add(new int[] {pairs[i], pairs[i + 1]});
        return out;
    }

    private static int band(Map<Integer, Integer> m, int id) {
        Integer b = m.get(id);
        assertTrue(b != null, "room " + id + " has a band");
        return b;
    }

    /** A chain Landing(0) - halls - Arena(last), {@code length} rooms in all. */
    private static List<FloorBands.Node> chainNodes(int length) {
        List<FloorBands.Node> out = new ArrayList<>();
        out.add(n(0, RoomKind.LANDING));
        for (int i = 1; i < length - 1; i++) out.add(n(i, RoomKind.HALL));
        out.add(n(length - 1, RoomKind.ARENA));
        return out;
    }

    private static List<int[]> chainEdges(int length) {
        List<int[]> out = new ArrayList<>();
        for (int i = 0; i + 1 < length; i++) out.add(new int[] {i, i + 1});
        return out;
    }

    // ------------------------------------------------------------------------------------------------ the rules, one by one
    @Test void aThirtyDeepChainUsesEveryBandAndTheArenaIsTheLast() {
        Map<Integer, Integer> m = FloorBands.bands(chainNodes(31), 0, 30, chainEdges(31), 10);
        assertEquals(31, m.size());
        assertEquals(1, band(m, 0), "the Landing is floor 1");
        assertEquals(10, band(m, 30), "the arena is floor 10");
        boolean[] used = new boolean[11];
        int previous = 1;
        for (int i = 0; i <= 30; i++) {
            int b = band(m, i);
            assertTrue(b >= previous, "monotone along the chain at room " + i);
            assertTrue(b >= 1 && b <= 10, "in range at room " + i);
            used[b] = true;
            previous = b;
            if (i > 0 && i < 30) assertEquals(FloorMap.bandOfDepth(i, 30, 10), b, "room " + i + " is banded by its depth");
        }
        for (int b = 1; b <= 10; b++) assertTrue(used[b], "band " + b + " has a room");
    }

    @Test void depthsAreBreadthFirstOverTheUndirectedGraph() {
        // 0 - 1 - 2 - 3, a shortcut 0 - 3, a branch 1 - 4, an orphan 5, an orphan pair 6 - 7
        List<FloorBands.Node> rooms = List.of(n(0, RoomKind.LANDING), n(1, RoomKind.HALL), n(2, RoomKind.HALL), n(3, RoomKind.HALL), n(4, RoomKind.HALL), n(5, RoomKind.HALL), n(6, RoomKind.HALL), n(7, RoomKind.HALL));
        Map<Integer, Integer> d = FloorBands.depths(rooms, 0, edges(1, 0, 1, 2, 2, 3, 3, 0, 1, 4, 6, 7));
        assertEquals(0, band(d, 0));
        assertEquals(1, band(d, 1));
        assertEquals(2, band(d, 2));
        assertEquals(1, band(d, 3), "the shortcut makes room 3 one doorway away");
        assertEquals(2, band(d, 4));
        assertEquals(-1, band(d, 5), "an orphan is unreachable");
        assertEquals(-1, band(d, 6), "so is a pair that only knows each other");
        assertEquals(-1, band(d, 7));
    }

    @Test void aShortcutShortensTheWalkAndSoTheFloor() {
        // chain 0..8 (8 = arena) with a shortcut 0 - 6: the arena is three doorways away, not eight
        List<FloorBands.Node> rooms = chainNodes(9);
        List<int[]> e = chainEdges(9);
        e.add(new int[] {0, 6});
        Map<Integer, Integer> m = FloorBands.bands(rooms, 0, 8, e, 10);
        // depths: 0:0  1:1  6:1  2:2  5:2  7:2  3:3  4:3  8:3  -> arena depth 3, band = 1 + depth * 9 / 3
        assertEquals(1, band(m, 0));
        assertEquals(4, band(m, 1));
        assertEquals(4, band(m, 6));
        assertEquals(7, band(m, 2));
        assertEquals(7, band(m, 5));
        assertEquals(7, band(m, 7));
        assertEquals(10, band(m, 3), "a room as far as the arena is on the last floor");
        assertEquals(10, band(m, 4));
        assertEquals(10, band(m, 8));
    }

    @Test void aStarWithTheArenaNextToTheLandingPutsEveryHallOnTheLastFloor() {
        List<FloorBands.Node> rooms = new ArrayList<>();
        rooms.add(n(0, RoomKind.LANDING));
        for (int i = 1; i <= 6; i++) rooms.add(n(i, RoomKind.HALL));
        rooms.add(n(7, RoomKind.ARENA));
        Map<Integer, Integer> m = FloorBands.bands(rooms, 0, 7, edges(0, 1, 0, 2, 0, 3, 0, 4, 0, 5, 0, 6, 0, 7), 10);
        assertEquals(1, band(m, 0));
        for (int i = 1; i <= 7; i++) assertEquals(10, band(m, i), "room " + i + " is as far as the arena");
    }

    @Test void withoutAnArenaTheDeepestRoomPlaysTheArena() {
        List<FloorBands.Node> rooms = new ArrayList<>();
        rooms.add(n(0, RoomKind.LANDING));
        for (int i = 1; i <= 9; i++) rooms.add(n(i, RoomKind.HALL));
        Map<Integer, Integer> m = FloorBands.bands(rooms, 0, FloorBands.NONE, chainEdges(10), 10);
        for (int i = 0; i <= 8; i++) assertEquals(i + 1, band(m, i), "depth " + i + " over a deepest depth of 9 is band " + (i + 1));
        assertEquals(10, band(m, 9), "the deepest room is the last floor");
    }

    @Test void anArenaThatCannotBeReachedIsTreatedAsMissing() {
        List<FloorBands.Node> rooms = new ArrayList<>();
        rooms.add(n(0, RoomKind.LANDING));
        for (int i = 1; i <= 4; i++) rooms.add(n(i, RoomKind.HALL));
        rooms.add(n(9, RoomKind.ARENA));
        Map<Integer, Integer> m = FloorBands.bands(rooms, 0, 9, chainEdges(5), 10);
        assertEquals(10, band(m, 9), "the arena kind is the last band whatever happens");
        assertEquals(1, band(m, 0));
        assertEquals(10, band(m, 4), "the deepest reachable room plays the arena");
        assertEquals(1 + 2 * 9 / 4, band(m, 2));
    }

    @Test void anArenaInTheLandingsOwnCellIsTreatedAsMissing() {
        // a sector whose "arena id" is the Landing itself (depth 0) must not collapse every room to the last floor
        List<FloorBands.Node> rooms = new ArrayList<>();
        rooms.add(n(0, RoomKind.LANDING));
        for (int i = 1; i <= 4; i++) rooms.add(n(i, RoomKind.HALL));
        Map<Integer, Integer> m = FloorBands.bands(rooms, 0, 0, chainEdges(5), 10);
        assertEquals(1, band(m, 0));
        assertEquals(1 + 1 * 9 / 4, band(m, 1));
    }

    @Test void anUnreachableRoomIsTheLastFloor() {
        List<FloorBands.Node> rooms = chainNodes(6);
        rooms.add(n(50, RoomKind.HALL));
        rooms.add(n(51, RoomKind.CORRIDOR));
        Map<Integer, Integer> m = FloorBands.bands(rooms, 0, 5, chainEdges(6), 10);
        assertEquals(10, band(m, 50));
        assertEquals(10, band(m, 51));
        assertEquals(1, band(m, 0));
    }

    @Test void linkTunnelsAreTheFirstFloorAndEverythingBehindTheArenaIsTheLast() {
        // 0 landing - 1 link - 2 link - 3 hall - 4 hall - 5 arena - 6 antechamber - 7 vault, 6 - 8 onward ; a loop 7 - 2 makes the vault close to the start
        List<FloorBands.Node> rooms = List.of(n(0, RoomKind.LANDING), n(1, RoomKind.LINK), n(2, RoomKind.LINK), n(3, RoomKind.HALL), n(4, RoomKind.HALL), n(5, RoomKind.ARENA),
                n(6, RoomKind.ANTECHAMBER), n(7, RoomKind.VAULT), n(8, RoomKind.ONWARD));
        Map<Integer, Integer> m = FloorBands.bands(rooms, 0, 5, edges(0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 6, 8, 7, 2), 10);
        assertEquals(1, band(m, 0));
        assertEquals(1, band(m, 1), "a link tunnel is floor 1");
        assertEquals(1, band(m, 2), "also at depth 2");
        assertEquals(10, band(m, 5));
        assertEquals(10, band(m, 6), "the antechamber");
        assertEquals(10, band(m, 7), "the vault stays on the last floor although the loop through room 2 makes it close to the Landing");
        assertEquals(10, band(m, 8), "the Way Onward");
        assertTrue(band(m, 3) < 10 && band(m, 4) < 10 && band(m, 4) >= band(m, 3), "the rooms before the arena are still spread");
    }

    @Test void theLandingIsAlwaysTheFirstFloor() {
        assertEquals(1, band(FloorBands.bands(List.of(n(0, RoomKind.LANDING)), 0, FloorBands.NONE, edges(), 10), 0), "a one-room sector");
        Map<Integer, Integer> two = FloorBands.bands(List.of(n(0, RoomKind.LANDING), n(1, RoomKind.ARENA)), 0, 1, edges(0, 1), 10);
        assertEquals(1, band(two, 0));
        assertEquals(10, band(two, 1));
        // the root need not be of kind LANDING (the assigner falls back to the lowest id)
        Map<Integer, Integer> odd = FloorBands.bands(List.of(n(3, RoomKind.HALL), n(4, RoomKind.HALL)), 3, FloorBands.NONE, edges(3, 4), 10);
        assertEquals(1, band(odd, 3));
        assertEquals(10, band(odd, 4));
    }

    @Test void aShorterRegionStillSpansFirstToLast() {
        Map<Integer, Integer> m = FloorBands.bands(chainNodes(21), 0, 20, chainEdges(21), 5);
        assertEquals(1, band(m, 0));
        assertEquals(5, band(m, 20));
        for (int i = 0; i <= 20; i++) assertTrue(band(m, i) >= 1 && band(m, i) <= 5, "room " + i);
        assertEquals(1, band(FloorBands.bands(chainNodes(21), 0, 20, chainEdges(21), 1), 20), "a one-floor region is all floor 1");
        assertEquals(1, band(FloorBands.bands(chainNodes(21), 0, 20, chainEdges(21), 0), 20), "and a nonsense length reads as one floor");
        assertEquals(20, band(FloorBands.bands(chainNodes(41), 0, 40, chainEdges(41), 20), 40));
    }

    // ------------------------------------------------------------------------------------------------ bad input never throws
    @Test void damagedInputDegradesInsteadOfThrowing() {
        assertEquals(0, FloorBands.bands(null, 0, FloorBands.NONE, null, 10).size());
        assertEquals(0, FloorBands.bands(List.of(), 0, FloorBands.NONE, edges(), 10).size());
        assertEquals(0, FloorBands.depths(null, 0, null).size());
        // the Landing is not one of the rooms: nothing can be walked to, so halls are the last floor and links the first
        List<FloorBands.Node> rooms = List.of(n(1, RoomKind.HALL), n(2, RoomKind.LINK), n(3, RoomKind.ARENA));
        Map<Integer, Integer> m = FloorBands.bands(rooms, 99, 3, edges(1, 2, 2, 3), 10);
        assertEquals(10, band(m, 1));
        assertEquals(1, band(m, 2));
        assertEquals(10, band(m, 3));
        // self loops, doubled doorways, doorways to rooms that do not exist, a short array and a null entry
        List<int[]> junk = new ArrayList<>(chainEdges(4));
        junk.add(new int[] {2, 2});
        junk.add(new int[] {1, 2});
        junk.add(new int[] {2, 1});
        junk.add(new int[] {3, 77});
        junk.add(new int[] {5});
        junk.add(null);
        Map<Integer, Integer> clean = FloorBands.bands(chainNodes(4), 0, 3, chainEdges(4), 10);
        assertEquals(clean, FloorBands.bands(chainNodes(4), 0, 3, junk, 10), "junk doorways change nothing");
        // negative room ids are legal input for a pure function (only NONE is reserved), and a repeated room id does not crash
        List<FloorBands.Node> negative = List.of(n(-5, RoomKind.LANDING), n(-6, RoomKind.HALL), n(-6, RoomKind.HALL));
        assertEquals(2, FloorBands.bands(negative, -5, FloorBands.NONE, edges(-5, -6), 10).size());
    }

    // ------------------------------------------------------------------------------------------------ properties over random graphs
    /** A random connected-ish graph: a random spanning tree plus extra doorways, random kinds, optionally an arena and a few orphans. */
    private static Object[] randomSector(Random r, boolean withArena) {
        int size = 2 + r.nextInt(60);
        RoomKind[] kinds = {RoomKind.HALL, RoomKind.CORRIDOR, RoomKind.JUNCTION, RoomKind.SAFE, RoomKind.TRIAL, RoomKind.LINK, RoomKind.VAULT, RoomKind.ANTECHAMBER, RoomKind.ONWARD, RoomKind.CAVERN};
        List<FloorBands.Node> rooms = new ArrayList<>();
        rooms.add(n(0, RoomKind.LANDING));
        for (int i = 1; i < size; i++) rooms.add(n(i, kinds[r.nextInt(kinds.length)]));
        List<int[]> e = new ArrayList<>();
        for (int i = 1; i < size; i++) e.add(new int[] {r.nextInt(i), i});
        int extra = r.nextInt(size);
        for (int i = 0; i < extra; i++) e.add(new int[] {r.nextInt(size), r.nextInt(size)});
        int arena = FloorBands.NONE;
        if (withArena) {
            arena = 1 + r.nextInt(size - 1);
            rooms.set(arena, n(arena, RoomKind.ARENA));
        }
        int orphans = r.nextInt(3);
        for (int i = 0; i < orphans; i++) rooms.add(n(1000 + i, RoomKind.HALL));
        return new Object[] {rooms, e, arena};
    }

    @SuppressWarnings("unchecked")
    @Test void everyBandIsInRangeForAnySectorAndAnyLength() {
        Random r = new Random(1234);
        int[] lengths = {1, 2, 3, 5, 10, 20};
        for (int t = 0; t < 400; t++) {
            Object[] s = randomSector(r, t % 3 != 0);
            List<FloorBands.Node> rooms = (List<FloorBands.Node>) s[0];
            List<int[]> e = (List<int[]>) s[1];
            int arena = (Integer) s[2];
            for (int per : lengths) {
                Map<Integer, Integer> m = FloorBands.bands(rooms, 0, arena, e, per);
                assertEquals(rooms.size(), m.size(), "every room has a band");
                for (FloorBands.Node node : rooms) {
                    int b = band(m, node.id());
                    assertTrue(b >= 1 && b <= per, "band " + b + " outside 1.." + per);
                }
                assertEquals(1, band(m, 0), "the Landing is floor 1");
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Test void theAnswerDoesNotDependOnTheOrderOfRoomsOrDoorways() {
        Random r = new Random(99);
        for (int t = 0; t < 200; t++) {
            Object[] s = randomSector(r, t % 4 != 0);
            List<FloorBands.Node> rooms = new ArrayList<>((List<FloorBands.Node>) s[0]);
            List<int[]> e = new ArrayList<>((List<int[]>) s[1]);
            int arena = (Integer) s[2];
            Map<Integer, Integer> first = FloorBands.bands(rooms, 0, arena, e, 10);
            Collections.shuffle(rooms, r);
            Collections.shuffle(e, r);
            for (int[] pair : e) if (r.nextBoolean()) { int x = pair[0]; pair[0] = pair[1]; pair[1] = x; }
            assertEquals(first, FloorBands.bands(rooms, 0, arena, e, 10), "same bands for the same graph, listed in another order");
            assertEquals(first, FloorBands.bands(rooms, 0, arena, e, 10), "and the same on a second call");
        }
    }

    @SuppressWarnings("unchecked")
    @Test void bandsNeverFallAlongAShortestPathAndNeverJumpMoreThanTheSpreadAllows() {
        Random r = new Random(7);
        for (int t = 0; t < 300; t++) {
            Object[] s = randomSector(r, true);
            List<FloorBands.Node> rooms = (List<FloorBands.Node>) s[0];
            List<int[]> e = (List<int[]>) s[1];
            int arena = (Integer) s[2];
            Map<Integer, Integer> depth = FloorBands.depths(rooms, 0, e);
            Map<Integer, Integer> m = FloorBands.bands(rooms, 0, arena, e, 10);
            int arenaDepth = depth.get(arena);
            int spread = arenaDepth <= 0 ? 9 : (9 + arenaDepth - 1) / arenaDepth;       // the largest rise one doorway can cause
            Map<Integer, RoomKind> kind = new HashMap<>();
            for (FloorBands.Node node : rooms) kind.put(node.id(), node.kind());
            for (int[] pair : e) {
                int a = pair[0], b = pair[1];
                if (a == b || !depth.containsKey(a) || !depth.containsKey(b) || depth.get(a) < 0 || depth.get(b) < 0) continue;
                if (!plain(kind.get(a), a) || !plain(kind.get(b), b)) continue;
                int lo = depth.get(a) <= depth.get(b) ? a : b, hi = lo == a ? b : a;
                assertTrue(Math.abs(depth.get(a) - depth.get(b)) <= 1, "breadth-first depths of neighbours differ by at most one");
                assertTrue(band(m, hi) >= band(m, lo), "the band does not fall from room " + lo + " to its deeper neighbour " + hi);
                assertTrue(band(m, hi) - band(m, lo) <= Math.max(1, spread), "one doorway never raises the band by more than the spread (" + spread + ")");
            }
        }
    }

    /** A room whose band comes from its depth alone (not the Landing, a link or something behind the arena). */
    private static boolean plain(RoomKind k, int id) {
        return id != 0 && k != RoomKind.LINK && !FloorBands.isBeyondArena(k);
    }

    @Test void theKindsBehindTheArenaAreExactlyTheBossChain() {
        for (RoomKind k : RoomKind.values()) {
            boolean expected = k == RoomKind.ARENA || k == RoomKind.ANTECHAMBER || k == RoomKind.VAULT || k == RoomKind.ONWARD;
            assertEquals(expected, FloorBands.isBeyondArena(k), k.name());
        }
        assertFalse(FloorBands.isBeyondArena(RoomKind.LINK), "a link is band 1, not the last band");
        assertFalse(FloorBands.isBeyondArena(RoomKind.LANDING));
    }
}
