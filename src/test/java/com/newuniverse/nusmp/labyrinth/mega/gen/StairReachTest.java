package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.cyclic.LoopConfig;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the quietest way a dungeon can lock somebody in is a room that has no way out: a pocket the generator closed behind a wall, a room whose only doorway was capped, a
 * stairwell nobody can walk to. The owner's rule is that EVERY placed room has a path to the stairs, and this proves it on the plans themselves: hundreds of floors of both
 * roads, the old generator and the cyclic (loop-making) one, each judged by an independent flood fill over the plan's doorways. The flood fill's own behaviour is pinned on
 * small hand-made graphs first (a pocket is found, a pinch room is found, the nearest safe room is the nearest by doorways), and a deliberately broken plan (all the doorways
 * of one room removed) must be reported, so the check is known to be able to fail.
 */
class StairReachTest {
    private static final SyntheticCatalog CAT = SyntheticCatalog.floors();

    private static List<int[]> e(int... pairs) {
        List<int[]> out = new ArrayList<>();
        for (int i = 0; i < pairs.length; i += 2) out.add(new int[] {pairs[i], pairs[i + 1]});
        return out;
    }

    // ---------------------------------------------------------------- the flood fill on small graphs
    @Test void aPathGraphIsFine() {
        StairReach.Report r = StairReach.analyse(4, e(0, 1, 1, 2, 2, 3), 0, List.of(0, 3));
        assertTrue(r.ok(), r.toString());
        assertEquals(3, r.farthest());
        assertTrue(r.stairsJoined());
    }

    @Test void aClosedPocketIsFound() {
        // rooms 3 and 4 are joined to each other and to nothing else
        StairReach.Report r = StairReach.analyse(5, e(0, 1, 1, 2, 3, 4), 0, List.of(0, 2));
        assertFalse(r.ok());
        assertEquals(List.of(3, 4), r.cutOff());
        assertEquals(List.of(3, 4), r.noStairs());
        assertTrue(r.stairsJoined());
    }

    @Test void aRoomWithNoDoorwayIsAPocketOfOne() {
        StairReach.Report r = StairReach.analyse(3, e(0, 1), 0, List.of(0, 1));
        assertEquals(List.of(2), r.cutOff());
    }

    @Test void stairsThatCannotBeWalkedToFailEvenWhenEveryRoomReachesSomeStairs() {
        // two components, each with a stairwell: every room reaches a stairwell, but the forward stairs are cut off from the arrival
        StairReach.Report r = StairReach.analyse(4, e(0, 1, 2, 3), 0, List.of(0, 3));
        assertFalse(r.stairsJoined());
        assertEquals(List.of(), r.noStairs());
        assertFalse(r.ok());
    }

    @Test void badEdgesAreIgnoredAndAnEmptyGraphIsFine() {
        assertTrue(StairReach.analyse(0, List.of(), -1, List.of()).ok());
        StairReach.Report r = StairReach.analyse(3, e(0, 1, 1, 2, 5, 6, 2, 2, -1, 0), 0, List.of(2));
        assertTrue(r.ok(), r.toString());
        assertEquals(2, StairReach.cleanEdges(e(0, 1, 5, 6, 2, 2, -1, 0, 1, 2), 3).size());
    }

    @Test void theNearestRoomIsTheNearestByDoorwaysAndTiesGoToTheLowerId() {
        List<int[]> edges = e(0, 1, 1, 2, 2, 3, 3, 4, 0, 5);
        assertEquals(5, StairReach.nearest(6, edges, 0, i -> i == 5 || i == 4));
        assertEquals(0, StairReach.nearest(6, edges, 0, i -> i == 0), "the start counts at distance 0");
        assertEquals(1, StairReach.nearest(6, e(0, 1, 0, 2), 0, i -> i == 1 || i == 2), "a tie goes to the lower id");
        assertEquals(-1, StairReach.nearest(4, e(0, 1), 0, i -> i == 3), "unreachable");
    }

    @Test void pinchRoomsAreTheOnesEveryWalkMustUse() {
        // 0 - 1 - 2 - 3 with a bypass 1 - 4 - 3: room 2 is bypassed, room 1 is not
        assertEquals(List.of(1), StairReach.pinchRooms(5, e(0, 1, 1, 2, 2, 3, 1, 4, 4, 3), 0, 3));
        assertEquals(List.of(1, 2), StairReach.pinchRooms(4, e(0, 1, 1, 2, 2, 3), 0, 3));
        assertEquals(List.of(), StairReach.pinchRooms(4, e(0, 1, 1, 2, 2, 3, 3, 0), 0, 2));
        assertEquals(List.of(), StairReach.pinchRooms(3, e(0, 1), 0, 2), "no walk at all, nothing to pinch");
    }

    // ---------------------------------------------------------------- generated floors
    private static Result plan(Polarity road, int chain, long seed, LoopConfig cyclic) {
        Request q = TestPlans.floor(CAT, seed, road, chain);
        q.cyclic = cyclic;
        SectorSolver.Solved s = SectorSolver.solve(q, 24);
        assertTrue(s.ok(), road + " chain " + chain + " seed " + seed + " could not be solved: " + SectorSolver.summary(s.failures(), 3));
        return s.result();
    }

    private static void everyRoomHasAPathToTheStairs(LoopConfig cyclic, String label) {
        int floors = 0, rooms = 0, pinched = 0, deadDoors = 0;
        for (Polarity road : Polarity.values()) {
            for (int chain = 0; chain < 5; chain++) {
                for (long seed = 1; seed <= 40; seed++) {
                    Result r = plan(road, chain, seed, cyclic);
                    String where = label + " " + road + " chain " + chain + " seed " + seed;
                    assertEquals(List.of(), StairReach.problems(r), where);
                    StairReach.Report rep = StairReach.of(r);
                    assertTrue(rep.ok(), where + ": " + rep);
                    assertTrue(rep.arrival() >= 0 && rep.stairs().size() >= 2, where + ": two stairwells");
                    assertTrue(rep.farthest() >= 2, where + ": the floor is more than a doorstep wide");
                    // every room has at least one doorway: a room with none is a closed pocket even when it is the only room we have not walked to
                    int[] ids = r.roomIds();
                    int n = rep.rooms();
                    int[] degree = new int[n];
                    for (int[] edge : r.edges()) {
                        degree[edge[0]]++;
                        degree[edge[1]]++;
                    }
                    for (int i = 0; i < n; i++) {
                        if (degree[i] == 0) deadDoors++;
                        assertTrue(degree[i] >= 1, where + ": room " + i + " has no doorway");
                    }
                    pinched += StairReach.lockablePinch(r).isEmpty() ? 0 : 1;
                    floors++;
                    rooms += n;
                    assertNotEquals(0, ids.length);
                }
            }
        }
        assertEquals(0, deadDoors);
        System.out.println("StairReachTest " + label + ": " + floors + " floors, " + rooms + " rooms, every room walks to the stairs; " + pinched + " floor(s) have a trial or puzzle room every walk must pass through");
    }

    @Test void everyRoomOfEveryGeneratedFloorHasAPathToTheStairs() {
        everyRoomHasAPathToTheStairs(null, "old generator");
    }

    @Test void everyRoomOfEveryCyclicFloorHasAPathToTheStairsToo() {
        everyRoomHasAPathToTheStairs(LoopConfig.defaults(), "cyclic generator");
    }

    @Test void theAuditNowSaysSoWhenAPlanHasAPocket() {
        // a plan whose graph is cut: take a real plan, remove every doorway of one room from the edge list and ask the pure flood fill
        Result r = plan(Polarity.HOLY, 1, 7, null);
        int n = StairReach.of(r).rooms();
        List<int[]> edges = r.edges();
        int victim = -1;
        List<Integer> stairs = StairReach.stairRooms(r);
        for (int i = 0; i < n; i++) if (!stairs.contains(i)) {
            victim = i;
            break;
        }
        assertTrue(victim >= 0);
        List<int[]> cut = new ArrayList<>();
        for (int[] edge : edges) if (edge[0] != victim && edge[1] != victim) cut.add(edge);
        StairReach.Report broken = StairReach.analyse(n, cut, StairReach.of(r).arrival(), stairs);
        assertFalse(broken.ok());
        assertTrue(broken.cutOff().contains(victim));
        assertTrue(broken.noStairs().contains(victim));
        // and the same plan is fine and the audit has nothing to say
        Request q = TestPlans.floor(CAT, 7, Polarity.HOLY, 1);
        SectorSolver.Solved s = SectorSolver.solve(q, 24);
        assertEquals(List.of(), SectorAudit.audit(s.result(), q.withSeed(s.result().seed, s.fallback()), !s.fallback()));
    }
}
