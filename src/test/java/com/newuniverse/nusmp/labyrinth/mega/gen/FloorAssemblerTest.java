package com.newuniverse.nusmp.labyrinth.mega.gen;

import com.newuniverse.nusmp.labyrinth.mega.MarkerKind;
import com.newuniverse.nusmp.labyrinth.mega.Polarity;
import com.newuniverse.nusmp.labyrinth.mega.RoomKind;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Why: the floor model (1.58.0) replaces the Landing, the tunnel and the Way Onward's seal by two stairwells, and the promises it makes are the owner's: you start
 * at the foot of a stair, the other stair is at the far end of a floor of about 28 rooms, an ordinary floor has no boss chain, a boss floor has the whole chain with
 * the stair behind it. Each is checked here on the stand-in catalog with the stairwells, over many seeds and both roads, by looking at the plan itself and not at the
 * audit that judged it (an independent walking-distance computation, an independent look at where the pieces stand).
 */
class FloorAssemblerTest {
    private static final SyntheticCatalog CAT = SyntheticCatalog.floors();

    /** The floor the game would place: the solver's winner over up to 24 seeds (a single try may fail the audit; see {@link #mostFloorsPassTheFullAuditOnTheFirstTry}). */
    private static Result plan(Polarity road, int chain, long seed) {
        Request q = TestPlans.floor(CAT, seed, road, chain);
        SectorSolver.Solved s = SectorSolver.solve(q, 24);
        assertTrue(s.ok(), "the floor must be solved: " + SectorSolver.summary(s.failures(), 3));
        assertFalse(s.fallback(), "without the fallback layout");
        return s.result();
    }

    private static List<Placed> kind(Result r, RoomKind k) {
        List<Placed> out = new ArrayList<>();
        for (Placed p : r.pieces) if (p.shape.kind == k) out.add(p);
        return out;
    }

    /** The walking distance (doorways) from the arrival to every piece, worked out here from the plan alone. */
    static int[] walk(Result r) {
        List<int[]> e = new ArrayList<>();
        for (Placed p : r.pieces) if (p.parent >= 0 && !p.shape.isCap() && !r.pieces.get(p.parent).shape.isCap()) e.add(new int[] {p.parent, p.index});
        for (Result.Loop l : r.loops) e.add(new int[] {l.pieceA(), l.pieceB()});
        return WalkGraph.distances(r.pieces.size(), e, r.landing);
    }

    @Test
    void anOrdinaryFloorHasTwoStairwellsAndNoBossChain() {
        for (Polarity road : Polarity.values()) {
            for (long seed = 1; seed <= 60; seed++) {
                Result r = plan(road, 2, seed);                           // floor 3: not a boss floor
                assertEquals(2, kind(r, RoomKind.STAIRWELL).size(), "two stairwells, seed " + seed);
                for (RoomKind k : new RoomKind[] {RoomKind.ARENA, RoomKind.ANTECHAMBER, RoomKind.VAULT, RoomKind.ONWARD, RoomKind.LANDING, RoomKind.LINK}) {
                    assertEquals(0, kind(r, k).size(), "no " + k + " on an ordinary floor, seed " + seed);
                }
                assertEquals(RoomKind.STAIRWELL, r.landingPiece().shape.kind, "the first room is the arrival stairwell");
                assertEquals(null, r.expansion, "no Way Onward seal on an ordinary floor");
            }
        }
    }

    @Test
    void theRoadDecidesWhichWayEachStairGoes() {
        // Holy road: the stairs that lead back drop, the stairs that lead on climb; Abyssal: the other way round
        for (Polarity road : Polarity.values()) {
            Result r = plan(road, 4, 7);
            Placed arrival = r.landingPiece();
            Placed forward = kind(r, RoomKind.STAIRWELL).stream().filter(p -> p.index != arrival.index).findFirst().orElseThrow();
            String back = arrival.shape.id, on = forward.shape.id;
            if (road == Polarity.HOLY) {
                assertTrue(back.contains("/down_"), "Holy arrival drops back: " + back);
                assertTrue(on.contains("/up_"), "Holy forward climbs: " + on);
            } else {
                assertTrue(back.contains("/up_"), "Abyssal arrival climbs back: " + back);
                assertTrue(on.contains("/down_"), "Abyssal forward drops: " + on);
            }
            assertEquals(FloorPlan.forwardDir(road), forward.shape.markers.stream().filter(m -> m.spec().kind() == MarkerKind.STAIR_EXIT).findFirst().orElseThrow().spec().arg("dir", ""));
        }
    }

    @Test
    void theArrivalStairwellStandsAtTheNearEdgeAndItsDoorLeadsIn() {
        for (Polarity road : Polarity.values()) {
            for (int chain = 0; chain < 6; chain++) {
                Request q = TestPlans.floor(CAT, 99, road, chain);
                Result r = MegaAssembler.assemble(q);
                assertTrue(r.assembled(), r.failure);
                Placed a = r.landingPiece();
                assertEquals(q.region.minX(), a.box.minX(), "against the near edge of the region");
                assertTrue(a.box.inside(q.region, 0));
                assertEquals(1, a.shape.sockets.size(), "a stairwell has one door");
                assertEquals(Facing.EAST, a.socketFacing(0), "the only door faces into the floor");
                assertEquals(q.start.frontCell().z(), a.socketCell(0).z(), "centred on the lane axis");
                Placed first = r.pieces.stream().filter(p -> p.parent == a.index).findFirst().orElse(null);
                assertNotNull(first, "something hangs from the arrival stairwell's door");
                assertTrue(first.shape.kind != RoomKind.TREASURE && first.shape.sockets.size() >= 2, "the first room has a way on: " + first.shape.id);
            }
        }
    }

    @Test
    void aFloorAimsAtItsRoomCountAndStaysInsideItsRegion() {
        int total = 0, n = 0;
        for (long seed = 1; seed <= 40; seed++) {
            Result r = plan(Polarity.HOLY, 1, seed);
            Request q = TestPlans.floor(CAT, seed, Polarity.HOLY, 1);
            int rooms = (int) r.pieces.stream().filter(Result::isRoom).count();
            assertTrue(rooms >= q.floor.minRooms(), "at least the audit's share: " + rooms + " < " + q.floor.minRooms());
            assertTrue(rooms <= q.floor.rooms() + 3, "no more than the aim plus the required pieces: " + rooms + " > " + q.floor.rooms());
            total += rooms;
            n++;
            assertEquals(List.of(), TestPlans.brute(r, q), "sound plan, seed " + seed);
        }
        double avg = total / (double) n;
        assertTrue(avg >= 22 && avg <= 31, "about 28 rooms on average, got " + avg);
    }

    @Test
    void theForwardStairwellIsAtTheFarEndByWalkingDistance() {
        int atBest = 0, plans = 0;
        for (Polarity road : Polarity.values()) {
            for (long seed = 1; seed <= 80; seed++) {
                Result r = plan(road, 2, seed);
                int[] dist = walk(r);
                Placed arrival = r.landingPiece();
                Placed forward = kind(r, RoomKind.STAIRWELL).stream().filter(p -> p.index != arrival.index).findFirst().orElseThrow();
                Placed host = r.pieces.get(forward.parent);
                int hostDist = dist[host.index];
                assertTrue(hostDist >= 3, "the stairs are not at the doorstep: host at " + hostDist + " (seed " + seed + ")");
                assertEquals(hostDist + 1, dist[forward.index], "the stairwell is one doorway beyond its host");
                // every other ROOM that could still have hosted it: no room farther than the host has a door that is still free and has room for the stairwell
                // (the assembler tried the farthest first; this checks the claim from the outside: the host is among the farthest rooms of the plan)
                int farthest = 0;                                          // the farthest ordinary room (the stairwell itself is always the farthest piece)
                for (Placed p : r.pieces) if (Result.isRoom(p) && p.shape.kind != RoomKind.STAIRWELL) farthest = Math.max(farthest, dist[p.index]);
                assertTrue(hostDist >= farthest - 2, "the host is within two doorways of the farthest room (" + hostDist + " of " + farthest + ", seed " + seed + ")");
                assertTrue(hostDist <= farthest, "and never beyond it");
                plans++;
                if (r.stats.getOrDefault("stairs.skipped", 0) == 0) atBest++;
                assertEquals(hostDist, (int) r.stats.getOrDefault("stairs.hostDist", -1), "the recorded host distance is the real one");
                assertTrue(r.stats.getOrDefault("stairs.bestDist", 0) >= hostDist, "no candidate is closer than the host unless the farther ones did not fit");
            }
        }
        assertTrue(atBest >= plans * 0.4, "many floors put the stairs on the very farthest free doorway (the others had a farther doorway that faced a wall): " + atBest + " of " + plans);
    }

    @Test
    void mostFloorsPassTheFullAuditOnTheFirstTry() {
        int ok = 0, n = 0;
        for (Polarity road : Polarity.values()) {
            for (int chain : new int[] {2, 9}) {
                for (long seed = 1; seed <= 50; seed++) {
                    Request q = TestPlans.floor(CAT, seed, road, chain);
                    Result r = MegaAssembler.assemble(q);
                    n++;
                    if (r.assembled() && SectorAudit.audit(r, q, true).isEmpty()) ok++;
                }
            }
        }
        assertTrue(ok >= n * 0.9, "the solver is a safety net, not the plan: " + ok + " of " + n + " passed at once");
    }

    @Test
    void aBossFloorHasTheWholeChainWithTheGrandStairBehindIt() {
        for (Polarity road : Polarity.values()) {
            for (long seed = 1; seed <= 25; seed++) {
                Request q = TestPlans.floor(CAT, seed, road, 9);          // floor 10
                assertTrue(q.floor.boss());
                assertEquals(19, (q.region.maxZ() - q.region.minZ() + 1) / 9, "a boss floor gets the full region");
                SectorSolver.Solved sv = SectorSolver.solve(q, 24);
                assertTrue(sv.ok(), SectorSolver.summary(sv.failures(), 3));
                Result r = sv.result();
                assertEquals(1, kind(r, RoomKind.ARENA).size());
                assertEquals(1, kind(r, RoomKind.ANTECHAMBER).size());
                assertEquals(1, kind(r, RoomKind.VAULT).size());
                Placed onward = kind(r, RoomKind.ONWARD).get(0);
                List<Placed> stairs = kind(r, RoomKind.STAIRWELL);
                assertEquals(2, stairs.size());
                Placed grand = stairs.stream().filter(p -> p.index != r.landing).findFirst().orElseThrow();
                assertEquals(onward.index, grand.parent, "the forward stairwell hangs from the Way Onward");
                assertTrue(grand.shape.id.contains("grand_"), "and it is a grand one: " + grand.shape.id);
                assertNotNull(r.expansion, "the rubble seal is kept: it opens when the Crucible is resolved");
                assertEquals(onward.index, r.expansion.piece());
                List<String> audit = SectorAudit.audit(r, q, false);
                assertEquals(List.of(), audit.stream().filter(a -> a.contains("stairwell") || a.contains("Way Onward")).toList());
            }
        }
    }

    @Test
    void aSameSeedGivesTheSameFloorAndAnotherSeedAnotherOne() {
        Result a = plan(Polarity.HOLY, 3, 5), b = plan(Polarity.HOLY, 3, 5), c = plan(Polarity.HOLY, 3, 6);
        assertEquals(TestPlans.fingerprint(a), TestPlans.fingerprint(b));
        assertFalse(TestPlans.fingerprint(a).equals(TestPlans.fingerprint(c)));
    }

    @Test
    void roomsReservedForAnotherThemeAreNeverDrawn() {
        TemplateThemes only = TemplateThemes.of(java.util.Map.of("nusmp:mega/hall/hall_2x2", List.of("frost"), "nusmp:mega/corridor/ribs_1x3", List.of("frost", "storm")));
        int frostDrew = 0;
        for (long seed = 1; seed <= 40; seed++) {
            for (String theme : new String[] {"gravity", "frost", "blood"}) {
                Request q = TestPlans.floor(CAT, seed, Polarity.HOLY, 3, 28, Recipe.forFloors(), only, null);
                q.themeId = theme;
                Result r = MegaAssembler.assemble(q);
                if (!r.assembled()) continue;
                boolean hasHall = r.pieces.stream().anyMatch(p -> p.shape.id.equals("nusmp:mega/hall/hall_2x2"));
                boolean hasRibs = r.pieces.stream().anyMatch(p -> p.shape.id.equals("nusmp:mega/corridor/ribs_1x3"));
                if (!theme.equals("frost")) assertFalse(hasHall, theme + " never draws the frost hall");
                if (!theme.equals("frost") && !theme.equals("storm")) assertFalse(hasRibs, theme + " never draws the corridor of frost and storm");
                if (theme.equals("frost") && hasHall) frostDrew++;
            }
        }
        assertTrue(frostDrew > 0, "the theme's own room is drawn in the theme");
    }
}
