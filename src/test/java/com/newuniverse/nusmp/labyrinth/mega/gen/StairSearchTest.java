package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MarkerKind;
import com.newuniverse.nusmp.labyrinth.mega.MegaGrid;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: "the forward stairwell stands at the room farthest from the arrival by walking distance" is the owner's description of where the stairs of a floor go, and the
 * brief asks for it to be a SEARCH that is proved in tests. Three proofs: the ranking of the candidates on plain numbers (against a brute-force maximum, whatever order
 * the candidates arrive in), the walking distance itself (loops shorten it; unreachable rooms are -1), and a whole floor whose only possible shape is a line of
 * corridors, where the stairwell can only stand at the end of the line, and does.
 */
class StairSearchTest {

    @Test
    void theRankingIsFarthestFirstAndDoesNotDependOnTheInputOrder() {
        Random rnd = new Random(7);
        for (int round = 0; round < 200; round++) {
            List<StairSearch.Candidate> cs = new ArrayList<>();
            int n = 1 + rnd.nextInt(30);
            for (int i = 0; i < n; i++) cs.add(new StairSearch.Candidate(i, rnd.nextInt(4), rnd.nextInt(9), rnd.nextInt(5)));
            List<StairSearch.Candidate> ranked = StairSearch.rank(cs);
            for (int i = 1; i < ranked.size(); i++) assertTrue(ranked.get(i - 1).dist() >= ranked.get(i).dist(), "non-increasing distance");
            List<StairSearch.Candidate> shuffled = new ArrayList<>(cs);
            Collections.shuffle(shuffled, rnd);
            assertEquals(ranked, StairSearch.rank(shuffled), "the same order whatever order the candidates were listed in");
        }
    }

    @Test
    void theSearchPicksTheFarthestCandidateThatFits() {
        Random rnd = new Random(11);
        for (int round = 0; round < 300; round++) {
            List<StairSearch.Candidate> cs = new ArrayList<>();
            int n = rnd.nextInt(25);
            for (int i = 0; i < n; i++) cs.add(new StairSearch.Candidate(i, 0, rnd.nextInt(12), rnd.nextInt(100)));
            boolean[] fits = new boolean[n];
            int bruteBest = -1;
            for (int i = 0; i < n; i++) {
                fits[i] = rnd.nextInt(3) == 0;
                if (fits[i]) bruteBest = Math.max(bruteBest, cs.get(i).dist());
            }
            StairSearch.Candidate got = StairSearch.pick(cs, c -> fits[c.piece()]);
            if (bruteBest < 0) assertNull(got, "nothing fits, nothing is picked");
            else {
                assertTrue(fits[got.piece()], "the pick fits");
                assertEquals(bruteBest, got.dist(), "and it is as far as any candidate that fits");
            }
        }
    }

    @Test
    void walkingDistancesFollowLoopsAndMarkTheLostAsUnreachable() {
        // 0-1-2-3-4 in a line, a loop 0-3 makes room 4 two doorways away, room 5 has no door
        List<int[]> line = List.of(new int[] {0, 1}, new int[] {1, 2}, new int[] {2, 3}, new int[] {3, 4});
        assertEquals(List.of(0, 1, 2, 3, 4, -1), toList(WalkGraph.distances(6, line, 0)));
        List<int[]> loop = new ArrayList<>(line);
        loop.add(new int[] {0, 3});
        assertEquals(List.of(0, 1, 2, 1, 2, -1), toList(WalkGraph.distances(6, loop, 0)), "the loop is a shortcut");
        assertEquals(List.of(-1, -1), toList(WalkGraph.distances(2, List.of(), 5)), "a source outside the graph reaches nothing");
        assertEquals(List.of(0, 1), toList(WalkGraph.distances(2, List.of(new int[] {0, 1}, new int[] {1, 1}, new int[] {0, 9}, new int[] {-1, 0}), 0)), "bad edges are ignored");
        assertEquals(4, WalkGraph.farthest(WalkGraph.distances(6, line, 0)));
    }

    private static List<Integer> toList(int[] a) {
        List<Integer> out = new ArrayList<>();
        for (int v : a) out.add(v);
        return out;
    }

    /** A catalog whose only rooms are straight corridors (a door at each end) and one-module stairwells: every floor must be a single line. */
    private static SyntheticCatalog lineCatalog() {
        SyntheticCatalog c = new SyntheticCatalog();
        final String M = "nusmp:mega/";
        for (int d = 1; d <= 8; d++) c.cap(M + "cap/alcove_" + d, d);
        c.pool(MegaGrid.P_CAP, 0, MegaGrid.P_EMPTY, M + "cap/alcove_1", 1, M + "cap/alcove_2", 1, M + "cap/alcove_3", 1);
        c.put(c.shape(M + "corridor/line_1x2", RoomKind.CORRIDOR, 1, 2).door('n', 4).door('s', 4));
        c.pool(MegaGrid.P_SIDE, 0, MegaGrid.P_CAP, M + "corridor/line_1x2", 1);
        c.pool(FloorPlan.P_STAIR_DOOR, 0, MegaGrid.P_CAP, M + "corridor/line_1x2", 1);
        for (String k : new String[] {"up", "down"}) {
            c.put(c.shape(M + "stairwell/" + k + "_a", RoomKind.STAIRWELL, 1, 1).door('e', 4, MegaGrid.DOOR, MegaGrid.DOOR, FloorPlan.P_STAIR_DOOR, "minecraft:air")
                    .marker(MarkerKind.ARRIVAL, 5, 9, 4).marker(MarkerKind.STAIR_EXIT, 3, 9, 4, "dir", k));
            c.pool(k.equals("up") ? FloorPlan.P_UP : FloorPlan.P_DOWN, 0, MegaGrid.P_CAP, M + "stairwell/" + k + "_a", 1);
        }
        return c;
    }

    @Test
    void onAFloorThatIsALineTheStairsCanOnlyBeAtTheEndOfTheLine() {
        SyntheticCatalog c = lineCatalog();
        for (Polarity road : Polarity.values()) {
            for (long seed = 1; seed <= 20; seed++) {
                Recipe rc = Recipe.forFloors();
                rc.minLoops = 0;
                rc.extraSafe = 0;
                rc.stairMinWalk = 2;
                rc.maxDepth = 5;                                           // five corridors, so the line ends inside the region and leaves room for the stairwell
                rc.setMin(RoomKind.TREASURE, 0);
                Request q = TestPlans.floor(c, seed, road, 1, 28, rc, TemplateThemes.none(), null);
                Result r = MegaAssembler.assemble(q);
                assertTrue(r.assembled(), r.failure);
                List<Placed> rooms = new ArrayList<>();
                for (Placed p : r.pieces) if (Result.isRoom(p)) rooms.add(p);
                Placed stair = null;
                for (Placed p : rooms) if (p.shape.kind == RoomKind.STAIRWELL && p.index != r.landing) stair = p;
                assertTrue(stair != null, "a forward stairwell exists");
                int[] dist = FloorAssemblerTest.walk(r);
                int longest = 0;
                for (Placed p : rooms) longest = Math.max(longest, dist[p.index]);
                assertEquals(longest, dist[stair.index], "the stairwell is the end of the line (seed " + seed + ", " + rooms.size() + " pieces)");
                assertEquals(6, longest, "five corridors and the stairwell: the stairs stand at the very end (seed " + seed + ")");
            }
        }
    }
}
