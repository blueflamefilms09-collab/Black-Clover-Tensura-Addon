package com.newuniverse.nusmp.labyrinth.mega.floors;

import com.newuniverse.nusmp.labyrinth.mega.FloorMap;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
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
import static org.junit.jupiter.api.Assertions.assertNull;
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

    @Test void aRoomCalledMinusOneIsNotTheMissingArena() {
        // NONE means "no arena", so a room that happens to have the id -1 is an ordinary hall: the deepest room (6, depth 3) plays the arena
        List<FloorBands.Node> rooms = List.of(n(0, RoomKind.LANDING), n(-1, RoomKind.HALL), n(5, RoomKind.HALL), n(6, RoomKind.HALL));
        Map<Integer, Integer> m = FloorBands.bands(rooms, 0, FloorBands.NONE, edges(0, -1, -1, 5, 5, 6), 10);
        assertEquals(1, band(m, 0));
        assertEquals(FloorMap.bandOfDepth(1, 3, 10), band(m, -1));
        assertEquals(FloorMap.bandOfDepth(2, 3, 10), band(m, 5));
        assertEquals(10, band(m, 6));
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

    // ------------------------------------------------------------------------------------------------ the EVEN strategy
    private static Map<Integer, Integer> even(List<FloorBands.Node> rooms, int landing, int arena, List<int[]> e, int per) {
        return FloorBands.bands(rooms, landing, arena, e, per, FloorBands.Strategy.EVEN);
    }

    private static Map<Integer, Integer> arenaRule(List<FloorBands.Node> rooms, int landing, int arena, List<int[]> e, int per) {
        return FloorBands.bands(rooms, landing, arena, e, per, FloorBands.Strategy.ARENA);
    }

    /** Why: the arena rule and the even rule differ most on the shape the assembler really produces (wide layers of halls, a shallow arena, the antechamber, the vault and the Way Onward behind it), so the tests build that shape on purpose. */
    private record Layered(List<FloorBands.Node> rooms, List<int[]> edges, int arena) {}

    /** The Landing, then one layer of halls per entry of {@code widths} (each hall hangs from a room of the layer before), the arena off the first room of layer {@code arenaLayer} (1-based). */
    private static Layered layered(int[] widths, int arenaLayer) {
        List<FloorBands.Node> rooms = new ArrayList<>();
        List<int[]> edges = new ArrayList<>();
        rooms.add(n(0, RoomKind.LANDING));
        List<Integer> previous = List.of(0);
        List<List<Integer>> layers = new ArrayList<>();
        int next = 1;
        for (int w : widths) {
            List<Integer> layer = new ArrayList<>();
            for (int i = 0; i < w; i++) {
                int id = next++;
                rooms.add(n(id, RoomKind.HALL));
                edges.add(new int[] {previous.get(i % previous.size()), id});
                layer.add(id);
            }
            layers.add(layer);
            previous = layer;
        }
        int arena = next++, ante = next++, vault = next++, onward = next++;
        rooms.add(n(arena, RoomKind.ARENA));
        rooms.add(n(ante, RoomKind.ANTECHAMBER));
        rooms.add(n(vault, RoomKind.VAULT));
        rooms.add(n(onward, RoomKind.ONWARD));
        edges.add(new int[] {layers.get(arenaLayer - 1).get(0), arena});
        edges.add(new int[] {arena, ante});
        edges.add(new int[] {ante, vault});
        edges.add(new int[] {ante, onward});
        return new Layered(rooms, edges, arena);
    }

    private static int[] histogram(Map<Integer, Integer> m, int per) {
        int[] count = new int[per + 1];
        for (int b : m.values()) count[b]++;
        return count;
    }

    @Test void evenSmallExampleIsCutIntoRunsByRank() {
        // Landing 0, halls 1..7 in a chain, arena 8; 5 floors, so the 7 halls are cut into 4 runs: ranks 0..6 -> floor((2i+1)*4/14)+1
        Map<Integer, Integer> m = even(chainNodes(9), 0, 8, chainEdges(9), 5);
        int[] expected = {1, 1, 1, 2, 3, 3, 4, 4, 5};
        for (int i = 0; i < expected.length; i++) assertEquals(expected[i], band(m, i), "room " + i);
    }

    @Test void evenGivesEveryFloorItsOwnHallWhenThereAreExactlyAsManyAsFloors() {
        // no arena: Landing + 9 halls in a chain, 10 floors: the nine halls take floors 1..9, floor 10 stays empty (only a boss chain would use it)
        List<FloorBands.Node> rooms = new ArrayList<>();
        rooms.add(n(0, RoomKind.LANDING));
        for (int i = 1; i <= 9; i++) rooms.add(n(i, RoomKind.HALL));
        Map<Integer, Integer> m = even(rooms, 0, FloorBands.NONE, chainEdges(10), 10);
        assertEquals(1, band(m, 0));
        for (int i = 1; i <= 9; i++) assertEquals(i, band(m, i), "hall " + i);
    }

    @Test void evenSpreadsAFewRoomsOverTheWholeSector() {
        // three halls and 10 floors: they must not huddle on floors 1..3, they are spread over the nine early floors
        List<FloorBands.Node> rooms = List.of(n(0, RoomKind.LANDING), n(1, RoomKind.HALL), n(2, RoomKind.HALL), n(3, RoomKind.HALL), n(4, RoomKind.ARENA));
        Map<Integer, Integer> m = even(rooms, 0, 4, chainEdges(5), 10);
        assertEquals(2, band(m, 1));
        assertEquals(5, band(m, 2));
        assertEquals(8, band(m, 3));
        assertEquals(10, band(m, 4));
    }

    @Test void evenFillsEveryEarlyFloorWhereTheArenaRuleLeavesHalfEmpty() {
        // the shape the assembler really produces: a shallow arena (depth 4) and wide layers beyond it
        Layered s = layered(new int[] {7, 14, 18, 18, 15, 12, 8}, 3);
        Map<Integer, Integer> a = arenaRule(s.rooms(), 0, s.arena(), s.edges(), 10);
        int[] ca = histogram(a, 10);
        int usedEarly = 0;
        for (int b = 1; b <= 9; b++) if (ca[b] > 0) usedEarly++;
        assertEquals(4, usedEarly, "the arena rule uses bands 1, 3, 5 and 7 only");
        assertEquals(53 + 4, ca[10], "and piles the 53 halls beyond the arena depth and the 4 boss rooms on the last floor");

        Map<Integer, Integer> e = even(s.rooms(), 0, s.arena(), s.edges(), 10);
        int[] ce = histogram(e, 10);
        int sum = 0;
        for (int b = 1; b <= 9; b++) {
            sum += ce[b];
            int hallsHere = ce[b] - (b == 1 ? 1 : 0);                                  // the Landing is on floor 1 as well
            assertTrue(hallsHere == 10 || hallsHere == 11, "floor " + b + " holds " + hallsHere + " halls, expected 10 or 11");
        }
        assertEquals(1 + 92, sum, "the Landing and the 92 halls are on floors 1..9");
        assertEquals(4, ce[10], "only the arena, the antechamber, the vault and the Way Onward are on the last floor");
        assertEquals(1, band(e, 0));
        assertEquals(10, band(e, s.arena()));
    }

    @Test void evenKeepsTheSpecialRoomsOfTheCommonRules() {
        // the sector of the link-tunnel test, plus an orphan hall and an orphan link
        List<FloorBands.Node> rooms = new ArrayList<>(List.of(n(0, RoomKind.LANDING), n(1, RoomKind.LINK), n(2, RoomKind.LINK), n(3, RoomKind.HALL), n(4, RoomKind.HALL), n(5, RoomKind.ARENA),
                n(6, RoomKind.ANTECHAMBER), n(7, RoomKind.VAULT), n(8, RoomKind.ONWARD), n(20, RoomKind.HALL), n(21, RoomKind.LINK)));
        Map<Integer, Integer> m = even(rooms, 0, 5, edges(0, 1, 1, 2, 2, 3, 3, 4, 4, 5, 5, 6, 6, 7, 6, 8, 7, 2), 10);
        assertEquals(1, band(m, 0), "the Landing");
        assertEquals(1, band(m, 1), "a link tunnel is floor 1");
        assertEquals(1, band(m, 2));
        assertEquals(1, band(m, 21), "also an unreachable one");
        assertEquals(10, band(m, 5), "the arena");
        assertEquals(10, band(m, 6), "the antechamber");
        assertEquals(10, band(m, 7), "the vault, although the loop through room 2 puts it close to the Landing");
        assertEquals(10, band(m, 8), "the Way Onward");
        assertEquals(10, band(m, 20), "an unreachable hall is the last floor");
        assertTrue(band(m, 3) <= band(m, 4) && band(m, 4) < 10, "the two reachable halls are spread, the nearer never above the farther");
    }

    @Test void evenTreatsAnUnreachableArenaAsTheLastFloorAndRanksTheRest() {
        List<FloorBands.Node> rooms = new ArrayList<>();
        rooms.add(n(0, RoomKind.LANDING));
        for (int i = 1; i <= 4; i++) rooms.add(n(i, RoomKind.HALL));
        rooms.add(n(9, RoomKind.ARENA));
        Map<Integer, Integer> m = even(rooms, 0, 9, chainEdges(5), 10);
        assertEquals(10, band(m, 9));
        assertEquals(1, band(m, 0));
        for (int i = 1; i <= 4; i++) assertTrue(band(m, i) >= 1 && band(m, i) <= 9, "hall " + i);
        for (int i = 1; i < 4; i++) assertTrue(band(m, i) <= band(m, i + 1), "the chain never falls at hall " + i);
    }

    @Test void evenWorksForShortAndSingleFloorSectors() {
        List<FloorBands.Node> rooms = chainNodes(8);
        Map<Integer, Integer> two = even(rooms, 0, 7, chainEdges(8), 2);
        for (int i = 0; i <= 6; i++) assertEquals(1, band(two, i), "two floors: everything before the arena is floor 1 (room " + i + ")");
        assertEquals(2, band(two, 7));
        Map<Integer, Integer> one = even(rooms, 0, 7, chainEdges(8), 1);
        for (int i = 0; i <= 7; i++) assertEquals(1, band(one, i), "one floor: all floor 1 (room " + i + ")");
        Map<Integer, Integer> zero = even(rooms, 0, 7, chainEdges(8), 0);
        assertEquals(one, zero, "a nonsense length reads as one floor");
        assertEquals(0, even(null, 0, FloorBands.NONE, null, 10).size(), "no rooms, no bands");
    }

    @SuppressWarnings("unchecked")
    @Test void evenNeverFallsWithDepthAndCutsEqualRunsOnAnySector() {
        Random r = new Random(4242);
        int[] lengths = {2, 3, 5, 10, 20};
        for (int t = 0; t < 300; t++) {
            Object[] s = randomSector(r, t % 3 != 0);
            List<FloorBands.Node> rooms = (List<FloorBands.Node>) s[0];
            List<int[]> e = (List<int[]>) s[1];
            int arena = (Integer) s[2];
            Map<Integer, Integer> depth = FloorBands.depths(rooms, 0, e);
            for (int per : lengths) {
                Map<Integer, Integer> m = even(rooms, 0, arena, e, per);
                int open = per - 1;
                int[] count = new int[per + 2];
                List<FloorBands.Node> plain = new ArrayList<>();
                for (FloorBands.Node node : rooms) {
                    int b = band(m, node.id());
                    assertTrue(b >= 1 && b <= per, "band " + b + " outside 1.." + per);
                    if (!evenPlain(node, depth)) continue;
                    plain.add(node);
                    assertTrue(b <= open, "a plain room is never on the last floor (room " + node.id() + " band " + b + " of " + per + ")");
                    count[b]++;
                }
                assertEquals(1, band(m, 0), "the Landing is floor 1");
                for (FloorBands.Node x : plain) {
                    for (FloorBands.Node y : plain) {
                        if (depth.get(x.id()) < depth.get(y.id())) assertTrue(band(m, x.id()) <= band(m, y.id()), "a deeper room is never on a lower floor (" + x.id() + " depth " + depth.get(x.id()) + " vs " + y.id() + " depth " + depth.get(y.id()) + ")");
                    }
                }
                if (plain.isEmpty()) continue;
                int lo = Integer.MAX_VALUE, hi = 0;
                for (int b = 1; b <= open; b++) {
                    lo = Math.min(lo, count[b]);
                    hi = Math.max(hi, count[b]);
                }
                assertTrue(hi - lo <= 1, "runs differ by more than one room: " + java.util.Arrays.toString(count) + " for " + plain.size() + " rooms on " + open + " floors");
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Test void evenDoesNotDependOnTheOrderOfRoomsOrDoorways() {
        Random r = new Random(31337);
        for (int t = 0; t < 200; t++) {
            Object[] s = randomSector(r, t % 4 != 0);
            List<FloorBands.Node> rooms = new ArrayList<>((List<FloorBands.Node>) s[0]);
            List<int[]> e = new ArrayList<>((List<int[]>) s[1]);
            int arena = (Integer) s[2];
            Map<Integer, Integer> first = even(rooms, 0, arena, e, 10);
            Collections.shuffle(rooms, r);
            Collections.shuffle(e, r);
            for (int[] pair : e) if (r.nextBoolean()) { int x = pair[0]; pair[0] = pair[1]; pair[1] = x; }
            assertEquals(first, even(rooms, 0, arena, e, 10), "same bands for the same graph, listed in another order");
        }
    }

    /** A room whose EVEN band comes from its rank: not the Landing, a link, the boss chain, and reachable. */
    private static boolean evenPlain(FloorBands.Node n, Map<Integer, Integer> depth) {
        return n.id() != 0 && n.kind() != RoomKind.LINK && !FloorBands.isBeyondArena(n.kind()) && depth.get(n.id()) >= 0;
    }

    // ------------------------------------------------------------------------------------------------ the config words and the length of a sector
    @Test void strategyIdsAreReadWithoutRegardToCaseOrSpaces() {
        assertEquals(FloorBands.Strategy.EVEN, FloorBands.Strategy.fromId("even"));
        assertEquals(FloorBands.Strategy.EVEN, FloorBands.Strategy.fromId("EVEN"));
        assertEquals(FloorBands.Strategy.ARENA, FloorBands.Strategy.fromId(" Arena "));
        assertNull(FloorBands.Strategy.fromId("sideways"));
        assertNull(FloorBands.Strategy.fromId(null));
        assertEquals("even", FloorBands.Strategy.EVEN.id());
        assertEquals("arena", FloorBands.Strategy.ARENA.id());
    }

    @Test void theDefaultOverloadIsTheBriefsRule() {
        Layered s = layered(new int[] {5, 9, 9, 6}, 2);
        assertEquals(arenaRule(s.rooms(), 0, s.arena(), s.edges(), 10), FloorBands.bands(s.rooms(), 0, s.arena(), s.edges(), 10));
        assertFalse(even(s.rooms(), 0, s.arena(), s.edges(), 10).equals(arenaRule(s.rooms(), 0, s.arena(), s.edges(), 10)), "the two strategies really differ on a shallow sector");
    }

    // ------------------------------------------------------------------------------------------------ bands become signed floors
    @Test void theHolyRoadCountsUpAndTheAbyssalRoadCountsDown() {
        Map<Integer, Integer> holy = FloorBands.floors(chainNodes(31), 0, 30, chainEdges(31), Polarity.HOLY, 0, 10, FloorBands.Strategy.ARENA);
        assertEquals(1, band(holy, 0), "the Landing of the first Holy sector is +1");
        assertEquals(10, band(holy, 30), "its arena is +10");
        Map<Integer, Integer> abyss = FloorBands.floors(chainNodes(31), 0, 30, chainEdges(31), Polarity.ABYSSAL, 0, 10, FloorBands.Strategy.ARENA);
        assertEquals(-1, band(abyss, 0), "the Landing of the first Abyssal sector is -1");
        assertEquals(-10, band(abyss, 30), "its arena is -10");
        for (int i = 0; i <= 30; i++) assertEquals(-band(holy, i), band(abyss, i), "the same sector numbered on the other road is the mirror image (room " + i + ")");
    }

    @Test void laterSectorsOfAChainContinueWhereTheLastOneEnded() {
        Map<Integer, Integer> third = FloorBands.floors(chainNodes(31), 0, 30, chainEdges(31), Polarity.HOLY, 2, 10, FloorBands.Strategy.EVEN);
        assertEquals(21, band(third, 0), "the Landing of the third Holy sector");
        assertEquals(30, band(third, 30), "and its arena, the third boss floor");
        Map<Integer, Integer> second = FloorBands.floors(chainNodes(31), 0, 30, chainEdges(31), Polarity.ABYSSAL, 1, 5, FloorBands.Strategy.EVEN);
        assertEquals(-6, band(second, 0), "five floors per sector: the second Abyssal sector starts at -6");
        assertEquals(-10, band(second, 30), "and ends at -10");
        for (int i = 0; i <= 30; i++) assertTrue(band(second, i) <= -6 && band(second, i) >= -10, "room " + i + " is inside the sector's floors");
    }

    @Test void aSectorWithoutALandingIsWalkedFromItsLowestRoom() {
        List<FloorBands.Node> rooms = List.of(n(9, RoomKind.HALL), n(5, RoomKind.HALL), n(3, RoomKind.HALL));
        Map<Integer, Integer> m = FloorBands.floors(rooms, FloorBands.NONE, FloorBands.NONE, edges(3, 5, 5, 9), Polarity.HOLY, 0, 10, FloorBands.Strategy.ARENA);
        assertEquals(1, band(m, 3), "room 3 is the root");
        assertEquals(FloorMap.bandOfDepth(1, 2, 10), band(m, 5));
        assertEquals(10, band(m, 9), "the deepest room plays the arena");
        assertEquals(0, FloorBands.floors(null, FloorBands.NONE, FloorBands.NONE, null, Polarity.HOLY, 0, 10, FloorBands.Strategy.EVEN).size());
        assertEquals(band(m, 3), band(FloorBands.floors(rooms, FloorBands.NONE, FloorBands.NONE, edges(3, 5, 5, 9), null, 0, 10, FloorBands.Strategy.ARENA), 3), "a missing road reads as the Holy one");
    }

    @Test void theLengthOfASectorFollowsItsOrder() {
        int d = FloorMap.DEFAULT_PER_REGION;
        assertEquals(7, FloorBands.lengthFor(d, 7, 5), "another numbered sector of the road wins: the chain stays consistent");
        assertEquals(7, FloorBands.lengthFor(3, 7, 5), "even over a length the sector carries");
        assertEquals(5, FloorBands.lengthFor(d, 0, 5), "a sector that carries only the constructor's default takes the config");
        assertEquals(d, FloorBands.lengthFor(d, 0, d));
        assertEquals(3, FloorBands.lengthFor(3, 0, 5), "a regenerated sector keeps the length it was numbered with");
        assertEquals(1, FloorBands.lengthFor(d, 0, 0), "never below one");
        assertEquals(1, FloorBands.lengthFor(0, 0, -4));
        assertEquals(5, FloorBands.lengthFor(-2, 0, 5), "a damaged stored length is ignored");
        assertEquals(1, FloorBands.lengthFor(d, -3, 0), "a damaged length of another sector is ignored");
    }
}
