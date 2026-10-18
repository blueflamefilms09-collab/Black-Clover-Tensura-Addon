package com.newuniverse.nusmp.labyrinth.mega.cyclic;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the pure cyclic layer is the part of the owner's requirement that CAN be proved here, so it is proved the hard way: known small graphs for the metrics, the
 * hallway search on hand-drawn maps, and then thousands of random grown dungeons ({@link SynthDungeon}) with every invariant the doc promises checked on every
 * one: the graph stays connected, every old walk survives, no doorway is used twice or belongs to a locked room, every hallway is a chain of free cells whose
 * first cell is fed by door A and last cell feeds door B (so there is never a door without a partner, never a wall behind a door), the dead-end ratio and the cycle
 * share move the right way, the stairs keep their distance, and the same seed gives the same links. The measured means are printed for the doc.
 */
class CyclicLayerTest {
    private static final int SEEDS = 4000;

    // ------------------------------------------------------------------------------------------------------------------------------ metrics

    private static RoomGraph path(int n) {
        RoomGraph g = new RoomGraph();
        for (int i = 0; i < n; i++) g.addNode(RoomCategory.COMBAT, 0);
        for (int i = 0; i + 1 < n; i++) g.addEdge(i, i + 1);
        return g;
    }

    @Test
    void aPathHasTwoDeadEndsAndNoLoop() {
        RoomGraph g = path(6);
        assertEquals(2, g.deadEnds(0).size());
        assertEquals(0, g.cyclomatic());
        assertEquals(0.0, g.cycleShare());
        assertEquals(5, g.bridges().cardinality());
        assertEquals(6, g.danglingOrder(0).size());
        g.addRole(0, Roles.ENTRANCE);
        g.addRole(5, Roles.STAIRS);
        assertEquals(0, g.deadEnds(Roles.TERMINAL).size(), "the entrance and the stairs are destinations, not dead ends");
        assertEquals(0, g.danglingOrder(Roles.TERMINAL).size(), "nothing dangles off a walk between two terminals");
    }

    @Test
    void aCycleHasNoDeadEndAndEveryRoomIsOnIt() {
        RoomGraph g = path(5);
        g.addEdge(4, 0);
        assertEquals(0, g.deadEnds(0).size());
        assertEquals(1, g.cyclomatic());
        assertEquals(1.0, g.cycleShare());
        assertEquals(0, g.bridges().cardinality());
    }

    @Test
    void aTailOnACycleIsADeadEndAndTheCycleStaysOnTheCycle() {
        RoomGraph g = path(4);
        g.addEdge(3, 0);                                   // a 4-cycle
        int tail1 = g.addNode(RoomCategory.LORE, 0);
        int tail2 = g.addNode(RoomCategory.LORE, 0);
        g.addEdge(2, tail1);
        g.addEdge(tail1, tail2);
        assertEquals(List.of(tail2), g.deadEnds(0));
        assertEquals(List.of(tail2, tail1), g.danglingOrder(0), "the leaf falls first, then the room that led only to it");
        BitSet on = g.onCycle();
        for (int i = 0; i < 4; i++) assertTrue(on.get(i));
        assertFalse(on.get(tail1));
        assertEquals(4 / 6.0, g.cycleShare(), 1e-9);
        assertEquals(1, g.cyclomatic());
    }

    @Test
    void theCyclomaticNumberCountsIndependentLoopsOfADisconnectedGraphToo() {
        RoomGraph g = path(3);
        g.addEdge(2, 0);                                   // triangle: 1 loop
        int a = g.addNode(RoomCategory.COMBAT, 0), b = g.addNode(RoomCategory.COMBAT, 0);
        g.addEdge(a, b);                                   // a second component, a tree
        assertEquals(2, g.components());
        assertEquals(1, g.cyclomatic());
    }

    @Test
    void shortestPathAndDistancesAgree() {
        RoomGraph g = path(7);
        g.addEdge(0, 6);
        assertEquals(1, g.distance(0, 6));
        assertEquals(List.of(2, 1, 0, 6), g.shortestPath(2, 6));
        RoomGraph t = path(3);
        int lone = t.addNode(RoomCategory.COMBAT, 0);
        assertEquals(-1, t.distance(0, lone));
        assertTrue(t.shortestPath(0, lone).isEmpty());
    }

    // ------------------------------------------------------------------------------------------------------------------------------ socket rules

    @Test
    void doorsLineUpOnlyWhenTheyFaceEachOtherAndStandInEachOthersFront() {
        LoopPlanner.Door a = new LoopPlanner.Door(0, new Cell(2, 2), Dir.EAST);
        LoopPlanner.Door b = new LoopPlanner.Door(1, new Cell(3, 2), Dir.WEST);
        assertTrue(SocketRules.aligned(a, b));
        assertFalse(SocketRules.aligned(a, new LoopPlanner.Door(1, new Cell(3, 3), Dir.WEST)), "one module off");
        assertFalse(SocketRules.aligned(a, new LoopPlanner.Door(1, new Cell(3, 2), Dir.EAST)), "not facing");
        assertFalse(SocketRules.aligned(a, new LoopPlanner.Door(1, new Cell(4, 2), Dir.WEST)), "a module apart");
        assertTrue(SocketRules.namesMatch("nusmp:mega_door", "nusmp:mega_door", "nusmp:mega_door", "nusmp:mega_door"));
        assertFalse(SocketRules.namesMatch("nusmp:mega_vault", "nusmp:mega_vault", "nusmp:mega_door", "nusmp:mega_door"), "a boss-chain doorway never meets an ordinary one");
    }

    @Test
    void placementScorePrefersLoopsOverBlindWallsAndLeaves() {
        LoopConfig cfg = LoopConfig.defaults().withBalanceWeight(0);
        double loop = SocketRules.placementScore(new SocketRules.PlacementFacts(1, 0, 0, 1, 2, RoomCategory.COMBAT, false), cfg, null);
        double plain = SocketRules.placementScore(new SocketRules.PlacementFacts(0, 0, 0, 1, 2, RoomCategory.COMBAT, false), cfg, null);
        double blind = SocketRules.placementScore(new SocketRules.PlacementFacts(0, 2, 1, 0, 2, RoomCategory.COMBAT, false), cfg, null);
        double leaf = SocketRules.placementScore(new SocketRules.PlacementFacts(0, 0, 0, 0, 0, RoomCategory.COMBAT, false), cfg, null);
        double allowedLeaf = SocketRules.placementScore(new SocketRules.PlacementFacts(0, 0, 0, 0, 0, RoomCategory.COMBAT, true), cfg, null);
        assertTrue(loop > plain && plain > blind && plain > leaf && allowedLeaf > leaf);
    }

    // ------------------------------------------------------------------------------------------------------------------------------ the hallway search

    @Test
    void aStraightHallwayBetweenTwoDoorsFacingAcrossAGap() {
        LoopPlanner.Door a = new LoopPlanner.Door(0, new Cell(0, 0), Dir.EAST);
        LoopPlanner.Door b = new LoopPlanner.Door(1, new Cell(3, 0), Dir.WEST);       // fronts: (1,0) and (2,0)
        List<LoopPlanner.Hop> h = LoopPlanner.hallway(a, b, 3, c -> true);
        assertNotNull(h);
        assertEquals(2, h.size());
        assertEquals(new Cell(1, 0), h.get(0).cell());
        assertEquals(new Cell(2, 0), h.get(1).cell());
        assertTrue(h.get(0).straight() && h.get(1).straight());
    }

    @Test
    void aBendHallwayWhenTheDoorsAreAtRightAngles() {
        LoopPlanner.Door a = new LoopPlanner.Door(0, new Cell(0, 0), Dir.EAST);       // front (1,0)
        LoopPlanner.Door b = new LoopPlanner.Door(1, new Cell(1, 2), Dir.NORTH);      // front (1,1), enter from the room at (1,2) going... the hallway leaves south
        // the hallway must leave its last cell towards B: B stands at (1,2), the last cell (1,1) leaves SOUTH
        List<LoopPlanner.Hop> h = LoopPlanner.hallway(a, b, 3, c -> true);
        assertNotNull(h);
        assertEquals(2, h.size());
        assertEquals(Dir.EAST, h.get(0).in());
        assertEquals(Dir.SOUTH, h.get(1).out());
    }

    @Test
    void oneCellHallwayWhenTheFrontsCoincide() {
        LoopPlanner.Door a = new LoopPlanner.Door(0, new Cell(0, 1), Dir.EAST);       // front (1,1)
        LoopPlanner.Door b = new LoopPlanner.Door(1, new Cell(1, 2), Dir.NORTH);      // front (1,1)
        List<LoopPlanner.Hop> h = LoopPlanner.hallway(a, b, 3, c -> true);
        assertNotNull(h);
        assertEquals(1, h.size());
        assertEquals(Dir.EAST, h.get(0).in());
        assertEquals(Dir.SOUTH, h.get(0).out());
        assertFalse(h.get(0).straight());
    }

    @Test
    void aHallwayRoutesAroundBlockedCellsAndRefusesWhenBoxedIn() {
        LoopPlanner.Door a = new LoopPlanner.Door(0, new Cell(0, 0), Dir.EAST);
        LoopPlanner.Door b = new LoopPlanner.Door(1, new Cell(4, 0), Dir.WEST);       // fronts (1,0) and (3,0)
        Set<Cell> blocked = Set.of(new Cell(2, 0));
        assertNull(LoopPlanner.hallway(a, b, 3, c -> !blocked.contains(c)), "a detour needs 5 cells");
        List<LoopPlanner.Hop> h = LoopPlanner.hallway(a, b, 5, c -> !blocked.contains(c));
        assertNotNull(h);
        assertEquals(5, h.size());
        Set<Cell> seen = new HashSet<>();
        for (LoopPlanner.Hop hop : h) assertTrue(seen.add(hop.cell()) && !blocked.contains(hop.cell()));
        assertNull(LoopPlanner.hallway(a, b, 3, c -> false));
        assertNull(LoopPlanner.hallway(a, b, 0, c -> true));
    }

    // ------------------------------------------------------------------------------------------------------------------------------ the properties

    private static final class Stats {
        int runs, linksTotal, hallwayRooms, rejectedTotal, fixedBelowTarget, prunedTotal, roomsBefore, roomsAfter;
        double deadBefore, deadAfterLoops, deadAfterPrune, cycleBefore, cycleAfter, loopsBefore, loopsAfter, shiftMax, shiftSum;
        int atTargetLoopsOnly, atTargetWithPrune, protectedBlocked;
    }

    /** Runs the pass on a grown dungeon; checks every invariant; returns the links. */
    private static List<LoopPlanner.Link> check(long seed, LoopConfig cfg, boolean prune, Stats st) {
        Rng r = new Rng(seed);
        int w = 13 + r.nextInt(7), h = 13 + r.nextInt(7), rooms = 18 + r.nextInt(23);
        SynthDungeon d = SynthDungeon.grow(seed, w, h, rooms);
        RoomGraph g = d.graph;
        assertTrue(g.connected(), "a grown dungeon is connected");
        int n0 = g.size();
        int[] distBefore = g.distances(d.entrance);
        RoomGraph.Report before = g.report(cfg.terminalMask());
        int[] shareBefore = g.categoryCounts();
        List<int[]> keep = new ArrayList<>();
        if (d.stairs != d.entrance && distBefore[d.stairs] >= 4) keep.add(new int[] {d.entrance, d.stairs, 4});
        Set<Cell> hallCells = new HashSet<>();
        List<LoopPlanner.Link> seen = new ArrayList<>();
        LoopPlanner.Host host = link -> {
            seen.add(link);
            return true;
        };
        LoopPlanner.Outcome out = LoopPlanner.run(g, d.open, d::free, cfg, seed, keep, host);

        // ---- connectivity and monotonicity
        assertTrue(g.connected(), "seed " + seed + ": the graph stays connected");
        for (int i = 0; i < n0; i++) assertTrue(g.distance(d.entrance, i) <= distBefore[i], "seed " + seed + ": walks only get shorter");
        assertTrue(g.size() >= n0 && g.edgeCount() >= before.edges());
        if (!keep.isEmpty()) assertTrue(g.distance(d.entrance, d.stairs) >= 4, "seed " + seed + ": the stairs keep their distance from the entrance");
        // ---- doors and hallways
        Set<LoopPlanner.Door> usedDoors = new HashSet<>();
        for (LoopPlanner.Link l : out.links()) {
            assertTrue(usedDoors.add(l.a()) && usedDoors.add(l.b()), "seed " + seed + ": a doorway is used once");
            assertTrue(d.open.contains(l.a()) && d.open.contains(l.b()), "seed " + seed + ": only open doorways are joined");
            assertFalse(g.hasRole(l.a().room(), cfg.lockedMask()) || g.hasRole(l.b().room(), cfg.lockedMask()), "seed " + seed + ": locked rooms get no new doorway");
            assertTrue(l.a().room() != l.b().room());
            if (l.direct()) {
                assertTrue(SocketRules.aligned(l.a(), l.b()), "seed " + seed + ": a direct link joins doorways that line up");
                continue;
            }
            List<LoopPlanner.Hop> hops = l.hops();
            assertTrue(hops.size() >= 1 && hops.size() <= cfg.maxHallway());
            assertEquals(l.a().front(), hops.get(0).cell(), "seed " + seed + ": the first hallway cell is fed by door A");
            assertEquals(l.a().dir(), hops.get(0).in());
            assertEquals(l.b().dir().opposite(), hops.get(hops.size() - 1).out(), "seed " + seed + ": the last hallway cell feeds door B");
            assertEquals(l.b().front(), hops.get(hops.size() - 1).cell());
            for (int i = 0; i < hops.size(); i++) {
                LoopPlanner.Hop hop = hops.get(i);
                assertTrue(d.free(hop.cell()), "seed " + seed + ": the hallway stands on free ground");
                assertTrue(hallCells.add(hop.cell()), "seed " + seed + ": two hallways never share a cell");
                assertTrue(hop.out() != hop.in().opposite(), "seed " + seed + ": a hallway piece has two different doorways");
                if (i + 1 < hops.size()) {
                    assertEquals(hop.cell().step(hop.out()), hops.get(i + 1).cell());
                    assertEquals(hop.out(), hops.get(i + 1).in());
                }
            }
        }
        // ---- the report
        RoomGraph.Report after = g.report(cfg.terminalMask());
        assertTrue(after.deadEnds() <= before.deadEnds(), "seed " + seed + ": links never create a dead end");
        assertTrue(after.cyclomatic() >= before.cyclomatic());
        assertTrue(out.hallwayRooms() <= cfg.hallwayShare() * g.size() + 1e-9, "seed " + seed + ": the hallway share holds");
        // ---- determinism
        SynthDungeon d2 = SynthDungeon.grow(seed, w, h, rooms);
        List<LoopPlanner.Link> seen2 = new ArrayList<>();
        LoopPlanner.run(d2.graph, d2.open, d2::free, cfg, seed, keep, l -> {
            seen2.add(l);
            return true;
        });
        assertEquals(seen, seen2, "seed " + seed + ": the same seed gives the same links");

        st.runs++;
        st.linksTotal += out.links().size();
        st.hallwayRooms += out.hallwayRooms();
        st.deadBefore += before.deadEndRatio();
        st.deadAfterLoops += after.deadEndRatio();
        st.cycleBefore += before.cycleShare();
        st.cycleAfter += after.cycleShare();
        st.loopsBefore += before.cyclomatic();
        st.loopsAfter += after.cyclomatic();
        st.roomsBefore += before.rooms();
        if (after.deadEndRatio() <= cfg.deadEndTarget() + 1e-9) st.atTargetLoopsOnly++;
        if (!prune) return seen;

        // ---- the pruner
        List<Integer> gone = DeadEndPruner.plan(g, cfg, Math.max(8, rooms / 2), d.entrance);
        BitSet rm = new BitSet();
        for (int x : gone) {
            assertFalse(g.hasRole(x, cfg.protectedMask()), "seed " + seed + ": a protected room is never pruned");
            rm.set(x);
        }
        RoomGraph.Pruned p = g.without(rm);
        RoomGraph pg = p.graph();
        assertTrue(pg.connected(), "seed " + seed + ": pruning keeps the dungeon connected");
        int[] dBefore = g.distances(d.entrance);
        int[] dAfter = pg.distances(p.oldToNew()[d.entrance]);
        for (int i = 0; i < g.size(); i++) if (p.oldToNew()[i] >= 0) assertEquals(dBefore[i], dAfter[p.oldToNew()[i]], "seed " + seed + ": pruning a leaf never lengthens a walk");
        RoomGraph.Report fin = pg.report(cfg.terminalMask());
        boolean blockedByProtected = true;
        for (int de : pg.deadEnds(cfg.terminalMask())) if (!pg.hasRole(de, cfg.protectedMask())) blockedByProtected = false;
        assertTrue(fin.deadEndRatio() <= cfg.deadEndTarget() + 1e-9 || blockedByProtected || pg.size() <= Math.max(8, rooms / 2),
                "seed " + seed + ": after pruning the ratio is at the target (" + fin + ")");
        if (fin.deadEndRatio() <= cfg.deadEndTarget() + 1e-9) st.atTargetWithPrune++;
        st.deadAfterPrune += fin.deadEndRatio();
        st.prunedTotal += gone.size();
        st.roomsAfter += fin.rooms();
        double[] sb = CategoryBalance.shares(shareBefore), sa = CategoryBalance.shares(pg.categoryCounts());
        st.shiftSum += CategoryBalance.maxShift(sb, sa);
        st.shiftMax = Math.max(st.shiftMax, CategoryBalance.maxShift(sb, sa));
        return seen;
    }

    @Test
    void thousandsOfRandomDungeonsKeepEveryInvariantAndLoseTheirDeadEnds() {
        LoopConfig cfg = LoopConfig.defaults();
        Stats st = new Stats();
        for (int i = 0; i < SEEDS; i++) check(0x1000 + i * 7919L, cfg, true, st);
        double n = st.runs;
        System.out.println(String.format(Locale.ROOT,
                "CYCLIC pure %d dungeons (rooms %.1f): dead ends %.1f%% -> %.1f%% after loops -> %.1f%% after prune; rooms on a cycle %.1f%% -> %.1f%%; independent loops %.2f -> %.2f; "
                        + "%.1f links and %.2f hallway rooms per dungeon; %d reach the 8%% target by loops alone, %d with the pruner; pruned %.2f rooms per dungeon; "
                        + "category shift (max share change) mean %.3f, worst %.3f",
                st.runs, st.roomsBefore / n, st.deadBefore / n * 100, st.deadAfterLoops / n * 100, st.deadAfterPrune / n * 100, st.cycleBefore / n * 100, st.cycleAfter / n * 100,
                st.loopsBefore / n, st.loopsAfter / n, st.linksTotal / n, st.hallwayRooms / n, st.atTargetLoopsOnly, st.atTargetWithPrune, st.prunedTotal / n, st.shiftSum / n, st.shiftMax));
        assertTrue(st.deadAfterLoops < st.deadBefore * 0.8, "the loop pass alone removes a good part of the dead ends");
        assertTrue(st.cycleAfter > st.cycleBefore + 0.1 * n, "the share of rooms on a cycle rises");
        assertTrue(st.loopsAfter > st.loopsBefore, "more independent loops");
        assertTrue(st.deadAfterPrune / n <= cfg.deadEndTarget() + 0.01, "on average the dead-end ratio is at the target");
    }

    @Test
    void theLoopPassAloneStaysUnderTheCapsItWasGiven() {
        LoopConfig cfg = LoopConfig.defaults().withMaxLinks(3).withHallwayShare(0.05);
        for (int i = 0; i < 300; i++) {
            long seed = 77 + i * 31L;
            SynthDungeon d = SynthDungeon.grow(seed, 15, 15, 30);
            LoopPlanner.Outcome o = LoopPlanner.run(d.graph, d.open, d::free, cfg, seed, null, l -> true);
            assertTrue(o.links().size() <= 3);
            assertTrue(o.hallwayRooms() <= 0.05 * d.graph.size() + 1e-9);
        }
    }

    @Test
    void withHallwaysSwitchedOffOnlyDirectDoorwaysAreAdded() {
        LoopConfig cfg = LoopConfig.defaults().withMaxHallway(0);
        for (int i = 0; i < 300; i++) {
            long seed = 5 + i * 13L;
            SynthDungeon d = SynthDungeon.grow(seed, 15, 15, 30);
            int n = d.graph.size();
            LoopPlanner.run(d.graph, d.open, d::free, cfg, seed, null, l -> {
                assertTrue(l.direct());
                return true;
            });
            assertEquals(n, d.graph.size(), "no hallway rooms");
        }
    }

    @Test
    void aHostThatRefusesEverythingLeavesTheGraphAlone() {
        LoopConfig cfg = LoopConfig.defaults();
        SynthDungeon d = SynthDungeon.grow(99, 15, 15, 30);
        int e = d.graph.edgeCount(), n = d.graph.size();
        LoopPlanner.Outcome o = LoopPlanner.run(d.graph, d.open, d::free, cfg, 99, null, l -> false);
        assertEquals(0, o.links().size());
        assertEquals(e, d.graph.edgeCount());
        assertEquals(n, d.graph.size());
        assertTrue(o.rejected() >= 0);
    }

    @Test
    void prunerRespectsMinimumRoomsAndProtectedRooms() {
        RoomGraph g = path(10);
        g.addRole(0, Roles.ENTRANCE);
        g.addRole(9, Roles.REQUIRED);                                    // a required dead end
        for (int i = 1; i < 9; i++) {
            int leaf = g.addNode(RoomCategory.TRANSITION, 0);
            g.addEdge(i, leaf);
        }
        LoopConfig cfg = LoopConfig.defaults().withDeadEndTarget(0.0);
        List<Integer> gone = DeadEndPruner.plan(g, cfg, 1, 0);
        assertFalse(gone.contains(9), "the required room stays");
        assertFalse(gone.contains(0));
        assertEquals(8, gone.size(), "the eight stub leaves go, the walk and the required room stay");
        List<Integer> few = DeadEndPruner.plan(g, cfg, 15, 0);
        assertTrue(g.size() - few.size() >= 15, "never under the minimum");
        assertTrue(DeadEndPruner.plan(g, cfg.withPrune(false), 1, 0).isEmpty());
    }
}
